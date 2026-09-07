package com.cfmc.fabric.mixin;

import com.cfmc.common.network.CFMCNetworkManager;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ============================================================================
 * Mixin: 拦截原生包处理
 * ============================================================================
 * CFMC 会话激活时, 原版 S2C 包 (区块/位置) 不应再驱动游戏世界
 * (数据来源已换成我们的 ChunkDataPacket / EntityMovePacket)。
 *
 * ⚠️ 1.20.4 yarn 方法名: onChunkData / onPlayerPositionLook;
 *    若映射变动请以实际反编译结果为准调整。
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {

    /**
     * v0.1: CFMC 会话中原版区块包直接取消 (防止双数据源冲突);
     * 我们的数据由 CFMCChunkLoader.handleChunkData 灌入。
     */
    @Inject(method = "onChunkData", at = @At("HEAD"), cancellable = true)
    private void onChunkData(net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket packet, CallbackInfo ci) {
        if (CFMCNetworkManager.getInstance().isActive()) {
            // CFMC 模式下原版区块包不合法 (服务端不发送), 防御性丢弃
            ci.cancel();
        }
    }
}
