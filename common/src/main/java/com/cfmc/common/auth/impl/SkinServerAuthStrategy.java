package com.cfmc.common.auth.impl;

import com.cfmc.common.auth.AuthResult;
import com.cfmc.common.auth.CFMCAuthService;
import com.cfmc.common.util.CFMCLogger;
import com.cfmc.common.config.CFMCConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * ============================================================================
 * 皮肤站认证 — Yggdrasil /authenticate (ely.by / littleskin.cn / 自建)
 * ============================================================================
 *
 * 端点: POST {base}/api/authserver/authenticate
 * 请求: { agent:{name:"Minecraft",version:1}, username, password, clientToken }
 * 响应: { accessToken, clientToken, selectedProfile: { id, name } }
 *
 * ⚠️ 服务端拿到的是"服务端视角"的皮肤站会话; v0.1 信任皮肤站返回的
 *    profile (uuid/name), 服务端 skin_server 模式会再次调皮肤站核验 (Phase 3)。
 */
public class SkinServerAuthStrategy implements CFMCAuthService {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private final Gson gson = new Gson();

    private final String baseUrl;
    private final String clientToken = java.util.UUID.randomUUID().toString();

    public SkinServerAuthStrategy(String baseUrl) {
        this.baseUrl = baseUrl == null ? CFMCConfig.get().skinServerUrl
                : baseUrl.replaceAll("/+$", "");
    }

    @Override
    public AuthResult authenticate(String username, String password) throws AuthException {
        if (password == null || password.isBlank()) {
            throw new AuthException("皮肤站认证需要密码");
        }

        try {
            JsonObject body = new JsonObject();
            JsonObject agent = new JsonObject();
            agent.addProperty("name", "Minecraft");
            agent.addProperty("version", 1);
            body.add("agent", agent);
            body.addProperty("username", username);
            body.addProperty("password", password);
            body.addProperty("clientToken", clientToken);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/authserver/authenticate"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 403) {
                throw new AuthException("皮肤站账号或密码错误");
            }
            if (resp.statusCode() != 200) {
                throw new AuthException("皮肤站错误 (HTTP " + resp.statusCode() + ")");
            }

            JsonObject data = gson.fromJson(resp.body(), JsonObject.class);
            if (!data.has("selectedProfile")) {
                throw new AuthException("皮肤站账号没有角色档案");
            }
            JsonObject profile = data.getAsJsonObject("selectedProfile");

            CFMCLogger.info("皮肤站认证成功: " + profile.get("name").getAsString());
            return new AuthResult(
                    profile.get("id").getAsString(),       // 皮肤站 UUID (无横杠)
                    profile.get("name").getAsString(),
                    data.get("accessToken").getAsString(), // 皮肤站 Token (供服务端核验)
                    "skin_server"
            );
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException("皮肤站不可达: " + baseUrl, e);
        }
    }
}
