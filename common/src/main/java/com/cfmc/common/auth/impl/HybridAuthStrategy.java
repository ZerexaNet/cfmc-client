package com.cfmc.common.auth.impl;

import com.cfmc.common.auth.AuthResult;
import com.cfmc.common.auth.CFMCAuthService;
import com.cfmc.common.util.CFMCLogger;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * ============================================================================
 * 混合认证 — 直接调服务端 /auth/login (mode=hybrid)
 * ============================================================================
 *
 * 服务端 hybrid 模式的降级链:
 *   1. 有 serverId → online (Mojang hasJoined)
 *   2. 有 password → skin_server (Yggdrasil)
 *   3. 都没有     → offline (离线 UUID)
 *
 * 客户端只需传 username + password (可选), 服务端自动选最优路径。
 * 这是最推荐的认证模式 —— 保证"总能进服"。
 */
public class HybridAuthStrategy implements CFMCAuthService {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final Gson GSON = new Gson();

    @Override
    public AuthResult authenticate(String username, String password, String serverUrl) throws AuthException {
        String httpUrl = toHttpUrl(serverUrl);
        CFMCLogger.info("混合认证: " + username + " → " + httpUrl + "/auth/login");

        try {
            JsonObject body = new JsonObject();
            body.addProperty("username", username);
            body.addProperty("mode", "hybrid");
            if (password != null && !password.isBlank()) {
                body.addProperty("password", password);
            }

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(httpUrl + "/auth/login"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                    .build();

            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                JsonObject err = GSON.fromJson(resp.body(), JsonObject.class);
                String msg = err.has("message") ? err.get("message").getAsString() : "HTTP " + resp.statusCode();
                throw new AuthException("认证失败: " + msg);
            }

            JsonObject data = GSON.fromJson(resp.body(), JsonObject.class);
            if (!data.has("ok") || !data.get("ok").getAsBoolean()) {
                throw new AuthException("认证失败: " + data.get("message").getAsString());
            }

            String accessToken = data.get("accessToken").getAsString();
            JsonObject profile = data.getAsJsonObject("profile");
            String uuid = profile.get("uuid").getAsString();
            String name = profile.get("name").getAsString();
            String mode = profile.has("mode") ? profile.get("mode").getAsString() : "hybrid";

            CFMCLogger.info("混合认证成功: " + name + " (" + uuid + ", mode=" + mode + ")");
            return new AuthResult(uuid, name, accessToken, mode);

        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException("无法连接认证服务器: " + e.getMessage(), e);
        }
    }

    private static String toHttpUrl(String wsUrl) {
        if (wsUrl == null) throw new IllegalArgumentException("服务器地址为空");
        String url = wsUrl.replaceAll("/+$", "");
        if (url.startsWith("wss://")) return "https://" + url.substring(6);
        if (url.startsWith("ws://")) return "http://" + url.substring(5);
        if (url.startsWith("https://") || url.startsWith("http://")) return url;
        return "http://" + url;
    }
}