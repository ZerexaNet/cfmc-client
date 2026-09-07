package com.cfmc.fabric.screen;

import com.cfmc.common.config.CFMCConfig;
import com.cfmc.common.auth.impl.HybridAuthStrategy;
import com.cfmc.common.auth.impl.OfflineAuthStrategy;
import com.cfmc.common.auth.impl.SkinServerAuthStrategy;
import com.cfmc.common.network.CFMCNetworkManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * ============================================================================
 * CFMC 登录界面 — 用户名/密码输入 + 认证模式选择 + 连接
 * ============================================================================
 * 认证模式由 CFMCConfig.authMode 决定 (配置文件修改):
 *   online / offline / skin_server / hybrid
 * v0.1 简化: 界面提供 用户名 + 密码 + 连接按钮; 模式切换走配置文件。
 */
public class CFMCAuthScreen extends Screen {

    private TextFieldWidget usernameField;
    private TextFieldWidget passwordField;
    private ButtonWidget connectButton;

    private final String serverUrl;

    public CFMCAuthScreen(String serverUrl) {
        super(Text.literal("CFMC 登录"));
        this.serverUrl = serverUrl;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;

        // 标题
        // 用户名输入框
        usernameField = new TextFieldWidget(this.textRenderer, cx - 100, this.height / 2 - 40, 200, 20, Text.literal("用户名"));
        usernameField.setMaxLength(16);
        usernameField.setText(net.minecraft.client.MinecraftClient.getInstance().getSession().getUsername());
        addSelectableChild(usernameField);

        // 密码输入框 (皮肤站用; 离线模式留空)
        passwordField = new TextFieldWidget(this.textRenderer, cx - 100, this.height / 2 - 10, 200, 20, Text.literal("密码"));
        passwordField.setMaxLength(64);
        addSelectableChild(passwordField);

        // 连接按钮
        connectButton = ButtonWidget.builder(Text.literal("连接"), b -> onConnectClicked())
                .dimensions(cx - 100, this.height / 2 + 30, 200, 20)
                .build();
        addDrawableChild(connectButton);
    }

    private void onConnectClicked() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        if (username.isEmpty()) return;

        connectButton.setMessage(Text.literal("认证中..."));
        connectButton.active = false;

        // 认证是阻塞 IO — 放后台线程, 完成后回主线程连 WS
        new Thread(() -> {
            try {
                CFMCAuthService service = createStrategy();
                AuthResult result = service.authenticate(username, password);

                // 回主线程: 建 WS 连接 + 关闭界面
                net.minecraft.client.MinecraftClient.getInstance().execute(() -> {
                    CFMCNetworkManager.getInstance().connect(serverUrl, result);
                    close();
                });
            } catch (CFMCAuthService.AuthException e) {
                net.minecraft.client.MinecraftClient.getInstance().execute(() -> {
                    connectButton.setMessage(Text.literal("失败: " + e.getMessage()));
                    connectButton.active = true;
                });
            }
        }, "CFMC-Auth").start();
    }

    /** 按配置选择认证策略 (与服务端四种模式对应) */
    private CFMCAuthService createStrategy() {
        return switch (CFMCConfig.get().authMode) {
            case "offline" -> new OfflineAuthStrategy();
            case "skin_server" -> new SkinServerAuthStrategy(null);
            case "online" -> new com.cfmc.common.auth.impl.OnlineAuthStrategy(
                    net.minecraft.client.MinecraftClient.getInstance().getSession().getUsername(), null);
            default -> new HybridAuthStrategy(null, new SkinServerAuthStrategy(null));
        };
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.width / 2, this.height / 2 - 70, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "用户名:", this.width / 2 - 100, this.height / 2 - 52, 0xA0A0A0);
        context.drawTextWithShadow(this.textRenderer, "密码 (皮肤站, 可留空):", this.width / 2 - 100, this.height / 2 - 22, 0xA0A0A0);
        usernameField.render(context, mouseX, mouseY, delta);
        passwordField.render(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
    }
}
