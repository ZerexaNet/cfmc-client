package com.cfmc.neoforge.screen;

import com.cfmc.common.config.CFMCConfig;
import com.cfmc.common.network.CFMCNetworkManager;
import com.cfmc.common.util.CFMCLogger;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * CFMC 认证/连接界面 (NeoForge, Mojang 官方映射)
 *
 * 与 fabric/screen/CFMCAuthScreen 平行 — 使用 1.20.4~1.21.x 稳定的
 * Screen/GuiGraphics/Button/EditBox API。
 *
 * v0.1: 服务器地址输入 + 连接按钮 (认证模式走配置文件的默认值 hybrid);
 * 完整认证 UI (在线/离线/皮肤站下拉切换 + 密码框) Phase 2。
 */
public class CFMCAuthScreen extends Screen {

    private final String serverUrl;
    private EditBox addressBox;

    public CFMCAuthScreen(String serverUrl) {
        super(Component.literal("CFMC 连接"));
        this.serverUrl = serverUrl;
    }

    @Override
    protected void init() {
        // 地址输入框
        addressBox = new EditBox(this.font, this.width / 2 - 100, this.height / 2 - 30, 200, 20,
                Component.literal("服务器地址"));
        addressBox.setMaxLength(256);
        addressBox.setValue(CFMCConfig.get().defaultServerAddress);
        addRenderableWidget(addressBox);

        // 连接按钮
        addRenderableWidget(Button.builder(Component.literal("连接"), b -> connect())
                .bounds(this.width / 2 - 100, this.height / 2 + 4, 98, 20)
                .build());
        // 取消按钮
        addRenderableWidget(Button.builder(Component.literal("取消"), b -> onClose())
                .bounds(this.width / 2 + 2, this.height / 2 + 4, 98, 20)
                .build());
    }

    private void connect() {
        String url = addressBox.getValue().trim();
        if (url.isEmpty()) return;
        CFMCConfig.get().defaultServerAddress = url;
        CFMCConfig.get().save();
        CFMCLogger.info("发起连接: " + url + " (认证模式 " + CFMCConfig.get().authMode + ")");
        // v0.1: 匿名连接 (JWT 由 CFMCAuthService 流程补上, Phase 2 接入本界面)
        CFMCNetworkManager.getInstance().connect(url, null);
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xC0101010);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 50, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
