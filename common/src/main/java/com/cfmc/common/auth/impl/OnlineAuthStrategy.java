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
 * 正版认证 — 通过服务端 /auth/login + Mojang sessionserver 校验
 * ============================================================================
 *
 * 流程:
 *   1. 客户端从 Minecraft 会话获取 UUID + 玩家名
 *   2. POST {server}/auth/login { username, mode: "online" }
 *   3. 服务端调 Mojang hasJoined 校验 → 签发 JWT
 *   4. 返回 accessToken + profile
 *
 * v0.1 简化: 服务端 online 模式需要 client 先在 Mojang 执行 join,
 * 然后传 serverId 给服务端验证。当前先走服务端 hybrid 兜底 (离线)。
 */
public class OnlineAuthStrategy implements CFMCAuthService {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final Gson GSON = new Gson();

    private final String sessionName;

    public OnlineAuthStrategy(String sessionName) {
        this.sessionName = sessionName;
    }

    @Override
    public AuthResult authenticate(String username, String password, String serverUrl) throws AuthException {
        if (sessionName == null) {
            throw new AuthException("未检测到正版会话, 请先启动正版登录");
        }
        if (!sessionName.equalsIgnoreCase(username)) {
            throw new AuthException("正版会话 (" + sessionName + ") 与输入名不匹配");
        }

        String httpUrl = toHttpUrl(serverUrl);
        CFMCLogger.info("正版认证: " + sessionName + " → " + httpUrl + "/auth/login");

        try {
            JsonObject body = new JsonObject();
            body.addProperty("username", username);
            body.addProperty("mode", "online");
            // TODO(Phase 3): 传 serverId 给服务端做 hasJoined 验证
            // body.addProperty("serverId", serverId);

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
            String mode = profile.has("mode") ? profile.get("mode").getAsString() : "online";

            CFMCLogger.info("正版认证成功: " + name + " (" + uuid + ", mode=" + mode + ")");
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