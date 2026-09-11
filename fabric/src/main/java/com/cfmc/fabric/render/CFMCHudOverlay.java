package com.cfmc.fabric.render;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.lang.reflect.Proxy;

/**
 * ============================================================================
 * HUD 覆盖层 — 显示 CFMC 连接状态 / 区域
 * ============================================================================
 * 绘制在左上角 (避免遮挡原版聊天/血量)。
 *
 * ⚠️ 版本兼容设计 (全版本支持):
 *   HudRenderCallback.onHudRender 的第二参在 1.20.x 是 float (tickDelta),
 *   1.21+ 改为 RenderTickCounter —— 接口方法签名跨版本不同, 无法用单一
 *   源码直接 implements。这里用 **JDK 动态代理** 注册回调:
 *   编译期只依赖接口类名 (跨版本稳定) 与 DrawContext, 实际方法分派走反射,
 *   两代签名通吃。
 */
public class CFMCHudOverlay {

    /** 版本兼容注册入口 (CFMCFabricClient 初始化时调用一次) */
    public static void register() {
        HudRenderCallback.EVENT.register((HudRenderCallback) Proxy.newProxyInstance(
                CFMCHudOverlay.class.getClassLoader(),
                new Class<?>[]{HudRenderCallback.class},
                (proxy, method, args) -> {
                    if ("onHudRender".equals(method.getName()) && args != null && args.length >= 1
                            && args[0] instanceof DrawContext ctx) {
                        new CFMCHudOverlay().render(ctx);
                    }
                    return null; // void 回调
                }));
    }

    /** 绘制状态行 (所有版本共用) */
    public void render(DrawContext drawContext) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        var state = com.cfmc.common.network.CFMCNetworkManager.getInstance().getState();
        if (state == com.cfmc.common.network.CFMCNetworkManager.State.DISCONNECTED) return;

        var textRenderer = client.textRenderer;
        String line = "CFMC [" + state + "] 区域 "
                + com.cfmc.common.network.CFMCNetworkManager.getInstance().getCurrentRegion();

        int x = 4;
        int y = 4;
        // 半透明背景提高可读性
        drawContext.fill(x - 2, y - 2, x + textRenderer.getWidth(line) + 2, y + 10, 0x80000000);
        drawContext.drawTextWithShadow(textRenderer, line, x, y, 0x78DC78);
    }
}
