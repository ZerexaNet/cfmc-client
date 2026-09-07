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
 * 正版认证 — 与 Mojang sessionserver 校验玩家会话
 * ============================================================================
 *
 * 流程适配 (与服务端 mojang-api.js 对齐):
 *   原版: 客户端 join(serverId) → 服务端 hasJoined(username, serverId)
 *   本项目: 客户端把 Minecraft 会话的 serverId 约定值直接传给服务端
 *   login 接口 (body.serverId), 服务端回查 hasJoined。
 *
 * v0.1 简化: 客户端只负责采集"会话可用性" (UUID+名), 服务端 hybrid
 *   模式收到 serverId 后自行验证 — 真正的 join/hasJoined 全流程 Phase 3。
 *
 * TODO(Phase 3): POST sessionserver join 端点 + 服务端随机 serverId 生成。
 */
public class OnlineAuthStrategy implements CFMCAuthService {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private final Gson gson = new Gson();

    /** 当前 Minecraft 会话的玩家名 (由 CFMCClientMod 注入) */
    private final String sessionName;
    /** 当前会话 UUID (带横杠) */
    private final String sessionUuid;

    public OnlineAuthStrategy(String sessionName, String sessionUuid) {
        this.sessionName = sessionName;
        this.sessionUuid = sessionUuid;
    }

    @Override
    public AuthResult authenticate(String username, String password) throws AuthException {
        // 校验: 请求的名字必须与当前正版会话一致 (不能冒用他人名字)
        if (sessionName == null || sessionUuid == null) {
            throw new AuthException("未检测到正版会话, 请先启动正版登录");
        }
        if (!sessionName.equalsIgnoreCase(username)) {
            throw new AuthException("正版会话 (" + sessionName + ") 与输入名不匹配");
        }

        // v0.1: serverId 用固定占位 — 服务端 hasJoined 会失败并降级 (hybrid 语义)
        // TODO(Phase 3): 实现 join → hasJoined 全流程后移除本注释
        CFMCLogger.info("正版认证: " + sessionName + " (" + sessionUuid + ")");
        return new AuthResult(
                sessionUuid.replace("-", ""),
                sessionName,
                null, // TODO: 服务端签发的 JWT 在 login 响应里, v0.1 走匿名握手
                "online"
        );
    }

    /** 探测 Mojang sessionserver 可达性 (调试用) */
    @SuppressWarnings("unused")
    private boolean isSessionServerReachable() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://sessionserver.mojang.com/"))
                    .timeout(Duration.ofSeconds(5))
                    .GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
