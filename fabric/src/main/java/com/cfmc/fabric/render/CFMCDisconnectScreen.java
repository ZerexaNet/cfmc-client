package com.cfmc.fabric.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * 断线界面 — 显示原因 + 重连按钮
 */
public class CFMCDisconnectScreen extends Screen {

    private final String reason;
    private final String serverUrl;

    public CFMCDisconnectScreen(String reason, String serverUrl) {
        super(Text.literal("连接已断开"));
        this.reason = reason;
        this.serverUrl = serverUrl;
    }

    @Override
    protected void init() {
        // 重连按钮 (居中) — builder 链以 .build() 收尾后整体作为 addDrawableChild 实参
        addDrawableChild(ButtonWidget.builder(Text.literal("重新连接"), b -> {
                    com.cfmc.common.network.CFMCNetworkManager.getInstance()
                            .connect(serverUrl, null); // TODO: 复用认证结果
                    close();
                })
                .dimensions(this.width / 2 - 100, this.height / 2 + 20, 200, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.width / 2, this.height / 2 - 30, 0xFF5555);
        context.drawCenteredTextWithShadow(this.textRenderer, "原因: " + reason,
                this.width / 2, this.height / 2 - 10, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true; // Esc 返回主菜单
    }
}
