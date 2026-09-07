package dev.xeno.accountswitcher.mixin;

import dev.xeno.accountswitcher.gui.AccountSwitcherScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public class GameMenuScreenMixin extends net.minecraft.client.gui.screen.Screen {
    protected GameMenuScreenMixin() { super(Text.empty()); }

    @Inject(method = "initWidgets", at = @At("TAIL"))
    private void onInitWidgets(CallbackInfo ci) {
        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("accountswitcher.button.switch"),
            btn -> this.client.setScreen(new AccountSwitcherScreen(this))
        ).dimensions(this.width / 2 - 100, this.height / 4 + 152, 200, 20).build());
    }
}
