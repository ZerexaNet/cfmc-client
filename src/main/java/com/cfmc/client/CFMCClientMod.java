package com.cfmc.client;

import com.cfmc.client.network.CFMCNetworkManager;
import com.cfmc.client.network.CFMCReconnectHandler;
import com.cfmc.client.render.CFMCHudOverlay;
import com.cfmc.client.util.CFMCLogger;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * ============================================================================
 * CFMC Client Mod 主类
 * ============================================================================
 * 入口职责 (fabric.mod.json entrypoint: CFMCClientMod::init):
 *   1. 加载配置
 *   2. 注册 HUD 覆盖层 + "P" 键快捷连接
 *   3. 提供 onClientTick 供 Mixin 调用 (每 tick 位置上报)
 *
 * Phase 1 定位: 协议/网络/认证链路验证 — "按 P 连接后能聊天即验收通过"。
 * 3D 世界渲染接入是 Phase 2 主题。
 */
public class CFMCClientMod {

    private static KeyBinding connectKey;

    /** Mod 入口 (静态方法, fabric.mod.json 引用) */
    public static void init() {
        CFMCConfig.get().load();

        // ---- HUD 覆盖层 ----
        HudRenderCallback.EVENT.register(new CFMCHudOverlay());

        // ---- 快捷键: P = 打开 CFMC 登录界面 ----
        connectKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cfmc.connect",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "category.cfmc.main"
        ));

        // ---- 客户端 tick: 快捷键处理 + 位置上报 ----
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (connectKey.wasPressed()) {
                String url = CFMCConfig.get().defaultServerAddress;
                client.setScreen(new com.cfmc.client.auth.CFMCAuthScreen(url));
                CFMCReconnectHandler.getInstance().remember(url);
            }
            onClientTick(client);
        });

        CFMCLogger.info("CFMC Client 初始化完成 (按 P 键打开连接界面)");
    }

    /**
     * 每 tick 回调 (Mixin: MinecraftClientMixin 调用)
     * IN_GAME 状态下以 20Hz 上报位置 (服务端 Tick 与此对齐)。
     */
    public static void onClientTick(MinecraftClient client) {
        var nm = CFMCNetworkManager.getInstance();
        if (nm.getState() != CFMCNetworkManager.State.IN_GAME) return;
        if (client.player == null) return;

        // TODO(Phase 2): 位置差量阈值 (位移>0.001 或视角变化才发包, 省带宽)
        nm.sendPacket(new com.cfmc.client.protocol.packets.serverbound.PlayerPositionLookPacket(
                client.player.getX(),
                client.player.getY(),
                client.player.getZ(),
                client.player.getYaw(),
                client.player.getPitch(),
                client.player.isOnGround()
        ));
    }
}
