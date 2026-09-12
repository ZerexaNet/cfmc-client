package com.cfmc.fabric;

import com.cfmc.common.config.CFMCConfig;
import com.cfmc.common.network.CFMCNetworkManager;
import com.cfmc.common.network.CFMCReconnectHandler;
import com.cfmc.common.platform.CFMCPlatform;
import com.cfmc.common.platform.CFMCPlatformHolder;
import com.cfmc.common.util.CFMCLogger;
import com.cfmc.common.version.CFMCVersionRegistry;
import com.cfmc.fabric.render.CFMCHudOverlay;
import com.cfmc.fabric.screen.CFMCAuthScreen;
import com.cfmc.fabric.world.CFMCWorldInjector;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;

/**
 * ============================================================================
 * CFMC Fabric 客户端入口 (全版本支持架构: Fabric 侧薄适配层)
 * ============================================================================
 * 本类是 common 模块 (网络/协议/认证, 零 MC 依赖) 与 Fabric/Minecraft 的
 * 粘合点 —— 所有版本相关的东西只有: 平台信息注入 + UI/键位注册。
 *
 * 入口职责 (fabric.mod.json entrypoint: CFMCFabricClient::init):
 *   1. 【最优先】注入 CFMCPlatform (common 全链路依赖它取 MC 版本/配置目录)
 *   2. 加载配置 + 注册 HUD + "P" 键快捷连接
 *   3. 提供 onClientTick 供 Mixin 调用 (每 tick 位置上报)
 *
 * 全版本矩阵: 本源码集配合 versions.gradle 的版本表, 由 CI 对
 * 1.20.1 ~ 1.21.x 逐版本构建 (yarn 映射差异在此层内消化, common 不动)。
 */
public class CFMCFabricClient {

    private static KeyBinding connectKey;

    /** Fabric 平台实现 — common 获取 MC 版本/配置目录的唯一途径 */
    private static final class FabricPlatform implements CFMCPlatform {
        @Override public String mcVersion() {
            return FabricLoader.getInstance().getModContainer("minecraft")
                    .map(c -> c.getMetadata().getVersion().getFriendlyString())
                    .orElse("unknown");
        }
        @Override public String loaderName() { return "fabric"; }
        @Override public Path configDir() { return FabricLoader.getInstance().getConfigDir(); }
    }

    /** Mod 入口 (静态方法, fabric.mod.json 引用) */
    public static void init() {
        // ---- 1. 平台注入必须最先 (配置/握手/版本注册表都依赖它) ----
        CFMCPlatformHolder.set(new FabricPlatform());

        // ---- 2. 配置 ----
        CFMCConfig.get().load();

        // ---- 3. HUD 覆盖层 ----
        // 版本兼容注册: 1.20.x 与 1.21.x 的 HudRenderCallback 方法签名不同 (见该类注释)
        CFMCHudOverlay.register();

        // ---- 3.5 世界注入桥 [Phase 2 核心] ----
        // 服务端区块/方块/JoinGame → 灌入客户端世界 (传送 + 地形覆盖 + 实时方块)
        CFMCWorldInjector.register();

        // ---- 4. 快捷键: P = 打开 CFMC 登录界面 ----
        connectKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cfmc.connect",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "category.cfmc.main"
        ));

        // ---- 5. 客户端 tick: 快捷键处理 + 服务器区块灌入 + 位置上报 ----
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (connectKey.wasPressed()) {
                String url = CFMCConfig.get().defaultServerAddress;
                client.setScreen(new CFMCAuthScreen(url));
                CFMCReconnectHandler.getInstance().remember(url);
            }
            CFMCWorldInjector.drain(client); // 灌入积压的服务器区块 (每 tick ≤4 个)
            onClientTick(client);
        });

        CFMCLogger.info("CFMC Client 初始化完成 (Fabric @ MC "
                + CFMCPlatformHolder.get().mcVersion()
                + ", 协议号 " + CFMCVersionRegistry.currentProtocol()
                + ", CFMC v" + com.cfmc.common.util.CFMCConstants.PROTOCOL_VERSION
                + ") — 按 P 键打开连接界面");
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
        nm.sendPacket(new com.cfmc.common.protocol.packets.serverbound.PlayerPositionLookPacket(
                client.player.getX(),
                client.player.getY(),
                client.player.getZ(),
                client.player.getYaw(),
                client.player.getPitch(),
                client.player.isOnGround()
        ));
    }
}
