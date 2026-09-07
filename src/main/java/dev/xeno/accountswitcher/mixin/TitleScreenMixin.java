package dev.xeno.accountswitcher.mixin;

import dev.xeno.accountswitcher.gui.AccountSwitcherScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends net.minecraft.client.gui.screen.Screen {
    protected TitleScreenMixin() { super(Text.empty()); }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("accountswitcher.button.switch"),
            btn -> this.client.setScreen(new AccountSwitcherScreen(this))
        ).dimensions(this.width / 2 - 100, this.height / 4 + 156, 200, 20).build());
    }
}
