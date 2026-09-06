package com.cfmc.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.HudRenderCallback;

import java.awt.Color;

/**
 * ============================================================================
 * HUD 覆盖层 — 显示 CFMC 连接状态 / 区域 / 延迟
 * ============================================================================
 * 绘制在左上角 (避免遮挡原版聊天/血量)。
 */
public class CFMCHudOverlay implements HudRenderCallback {

    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        var state = com.cfmc.client.network.CFMCNetworkManager.getInstance().getState();
        if (state == com.cfmc.client.network.CFMCNetworkManager.State.DISCONNECTED) return;

        var textRenderer = client.textRenderer;
        String line = "CFMC [" + state + "] 区域 "
                + com.cfmc.client.network.CFMCNetworkManager.getInstance().getCurrentRegion();

        int x = 4;
        int y = 4;
        // 半透明背景提高可读性
        drawContext.fill(x - 2, y - 2, x + textRenderer.getWidth(line) + 2, y + 10, 0x80000000);
        drawContext.drawTextWithShadow(textRenderer, line, x, y,
                new Color(120, 220, 120).getRGB());
    }
}
