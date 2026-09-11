package com.cfmc.neoforge.mixin;

import com.cfmc.common.network.CFMCNetworkManager;
import com.cfmc.neoforge.CFMCNeoForgeClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraft 主类 Mixin (Mojang 映射: net.minecraft.client.Minecraft)
 *
 * 每帧 tick 尾部注入 — 作用:
 *   1. NeoForge 侧快捷键/位置上报驱动 (1.20.4 与 1.21.x 的 Tick 事件
 *      签名不同, Mixin 是唯一跨版本稳定的注入点)
 *   2. IN_GAME 时每帧调 CFMCNeoForgeHooks.onClientTick (20Hz 上报)
 *
 * 与 yarn 版 (fabric/MinecraftClientMixin) 等价, 仅类名/方法名映射不同。
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void cfmc$onTick(CallbackInfo ci) {
        CFMCNeoForgeClient.onClientTick(Minecraft.getInstance());
        // 网络层状态保持 (心跳/重连计时由 common 自线程驱动, 此处仅占位)
        CFMCNetworkManager.getInstance();
    }
}
