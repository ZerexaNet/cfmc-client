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
 * 离线认证 — 通过服务端 /auth/login 获取 JWT
 * ============================================================================
 *
 * 流程:
 *   1. POST {server}/auth/login { username, mode: "offline" }
 *   2. 服务端生成离线 UUID + 签发 JWT
 *   3. 返回 accessToken + profile (uuid/name)
 *
 * UUID 一致性: 服务端 offlineUuid(username) 用 MD5("OfflinePlayer:"+name),
 * 与 Java UUID.nameUUIDFromBytes 算法逐字节一致。由服务端统一生成,
 * 客户端不再本地计算, 消除两端 UUID 不一致的风险。
 */
public class OfflineAuthStrategy implements CFMCAuthService {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final Gson GSON = new Gson();

    @Override
    public AuthResult authenticate(String username, String password, String serverUrl) throws AuthException {
        if (username == null || username.isBlank() || username.length() > 16) {
            throw new AuthException("用户名必须为 1-16 个字符");
        }

        String httpUrl = toHttpUrl(serverUrl);
        CFMCLogger.info("离线认证: " + username + " → " + httpUrl + "/auth/login");

        try {
            JsonObject body = new JsonObject();
            body.addProperty("username", username);
            body.addProperty("mode", "offline");

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

            CFMCLogger.info("离线认证成功: " + name + " (" + uuid + ")");
            return new AuthResult(uuid, name, accessToken, "offline");

        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException("无法连接认证服务器: " + e.getMessage(), e);
        }
    }

    /** ws:// → http://, wss:// → https:// */
    private static String toHttpUrl(String wsUrl) {
        if (wsUrl == null) throw new IllegalArgumentException("服务器地址为空");
        String url = wsUrl.replaceAll("/+$", "");
        if (url.startsWith("wss://")) return "https://" + url.substring(6);
        if (url.startsWith("ws://")) return "http://" + url.substring(5);
        if (url.startsWith("https://") || url.startsWith("http://")) return url;
        return "http://" + url;
    }
}