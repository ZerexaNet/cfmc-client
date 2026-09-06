package com.cfmc.client.mixin;

import com.cfmc.client.CFMCClientMod;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin: 客户端 tick 循环挂钩
 * 用于 CFMC 会话中的周期任务:
 *   - IN_GAME 状态每 tick 发送 PlayerPositionLook (20Hz, 与服务端 TPS 对齐)
 *   - HUD 状态刷新
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        CFMCClientMod.onClientTick((MinecraftClient) (Object) this);
    }
}
