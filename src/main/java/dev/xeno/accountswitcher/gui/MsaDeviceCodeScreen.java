package dev.xeno.accountswitcher.gui;

import dev.xeno.accountswitcher.auth.MicrosoftAuthFlow.DeviceCodeState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

public class MsaDeviceCodeScreen extends Screen {
    private final Screen parent;

    public MsaDeviceCodeScreen(Screen parent) {
        super(Text.translatable("accountswitcher.msa.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("accountswitcher.msa.open_browser"),
            btn -> {
                if (DeviceCodeState.verificationUrl != null)
                    Util.getOperatingSystem().open(DeviceCodeState.verificationUrl);
            }
        ).dimensions(cx - 100, cy + 24, 200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("accountswitcher.msa.copy_code"),
            btn -> {
                if (DeviceCodeState.userCode != null)
                    this.client.keyboard.setClipboard(DeviceCodeState.userCode);
            }
        ).dimensions(cx - 100, cy + 46, 200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("accountswitcher.button.cancel"),
            btn -> this.client.setScreen(parent)
        ).dimensions(cx - 100, cy + 72, 200, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int cx = this.width / 2;
        int cy = this.height / 2;
        ctx.fill(0, 0, this.width, this.height, Palette.BG);

        ctx.drawCenteredTextWithShadow(this.textRenderer, "LazySwitcher", cx, cy - 68, Palette.TITLE);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, cy - 56, Palette.TEXT_DIM);

        String url = DeviceCodeState.verificationUrl != null ? DeviceCodeState.verificationUrl
            : "microsoft.com/link";
        ctx.drawCenteredTextWithShadow(this.textRenderer, "Go to  " + url, cx, cy - 32, Palette.TEXT);

        String code = DeviceCodeState.userCode != null ? DeviceCodeState.userCode : "…";
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(code), cx, cy - 12, Palette.TITLE);

        ctx.drawCenteredTextWithShadow(this.textRenderer,
            Text.translatable("accountswitcher.msa.waiting"), cx, cy + 6, Palette.TEXT_DIM);

        super.render(ctx, mouseX, mouseY, delta);
    }
}
