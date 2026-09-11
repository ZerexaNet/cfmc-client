package com.cfmc.neoforge;

import com.cfmc.common.config.CFMCConfig;
import com.cfmc.common.platform.CFMCPlatform;
import com.cfmc.common.platform.CFMCPlatformHolder;
import com.cfmc.common.util.CFMCLogger;
import com.cfmc.common.version.CFMCVersionRegistry;
import com.cfmc.neoforge.screen.CFMCAuthScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;

/**
 * ============================================================================
 * CFMC NeoForge 客户端入口 (全版本支持架构: NeoForge 侧薄适配层)
 * ============================================================================
 * 与 fabric/CFMCFabricClient 平行 — common 模块 (零 MC 依赖) 的第二个挂载点。
 * 代码使用 Mojang 官方映射名 (NeoForge 规范), 与 Fabric 侧的 yarn 名不同,
 * 但 common 层完全一致 —— 这就是"全版本一份核心"的意义。
 *
 * 版本矩阵: 本源码集只使用 1.20.4~1.21.x 全系稳定的 API:
 *   - @Mod 无参构造 + FMLJavaModLoadingContext.get().getModEventBus()
 *   - KeyMapping / RegisterKeyMappingsEvent (跨版本稳定)
 *   - tick 驱动与 HUD 不走 NeoForge 事件 (两版本签名不同) —— 走 Mixin!
 *     见 mixin/MinecraftMixin (Minecraft#tick 尾部注入, 跨版本稳定)
 */
@Mod(CFMCNeoForgeClient.MODID)
public class CFMCNeoForgeClient {
    public static final String MODID = "cfmcclient";

    /** 快捷键: P = 打开 CFMC 登录界面 (NeoForge 侧注册) */
    public static KeyMapping connectKey;

    /** NeoForge 平台实现 — common 获取 MC 版本/配置目录的唯一途径 */
    private static final class NeoForgePlatform implements CFMCPlatform {
        @Override public String mcVersion() {
            // net.neoforged.fml.loading.FMLLoader.versionInfo() 全版本可用
            return net.neoforged.fml.loading.FMLLoader.versionInfo().mcVersion();
        }
        @Override public String loaderName() { return "neoforge"; }
        @Override public Path configDir() { return net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get(); }
    }

    public CFMCNeoForgeClient() {
        // ---- 1. 平台注入必须最先 (配置/握手/版本注册表都依赖它) ----
        CFMCPlatformHolder.set(new NeoForgePlatform());

        IEventBus modBus = net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::onClientSetup);
        modBus.addListener(this::onRegisterKeys);
    }

    private void onRegisterKeys(RegisterKeyMappingsEvent event) {
        connectKey = new KeyMapping(
                "key.cfmc.connect",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "category.cfmc.main"
        );
        event.register(connectKey);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        CFMCConfig.get().load();
        CFMCLogger.info("CFMC Client 初始化完成 (NeoForge @ MC "
                + CFMCPlatformHolder.get().mcVersion()
                + ", 协议号 " + CFMCVersionRegistry.currentProtocol()
                + ", CFMC v" + com.cfmc.common.util.CFMCConstants.PROTOCOL_VERSION + ")");
    }

    /** Mixin (MinecraftMixin) 每帧调用: P 键处理 + 位置上报 */
    public static void onClientTick(Minecraft mc) {
        // 快捷键: 游戏内按 P 打开连接界面
        while (connectKey != null && connectKey.consumeClick()) {
            String url = CFMCConfig.get().defaultServerAddress;
            mc.setScreen(new CFMCAuthScreen(url));
            com.cfmc.common.network.CFMCReconnectHandler.getInstance().remember(url);
        }

        var nm = com.cfmc.common.network.CFMCNetworkManager.getInstance();
        if (nm.getState() != com.cfmc.common.network.CFMCNetworkManager.State.IN_GAME) return;
        if (mc.player == null) return;

        // TODO(Phase 2): 位置差量阈值 (位移>0.001 或视角变化才发包)
        nm.sendPacket(new com.cfmc.common.protocol.packets.serverbound.PlayerPositionLookPacket(
                mc.player.getX(),
                mc.player.getY(),
                mc.player.getZ(),
                mc.player.getYRot(),   // Mojmap: yaw = getYRot()
                mc.player.getXRot(),   // Mojmap: pitch = getXRot()
                mc.player.onGround()
        ));
    }
}
