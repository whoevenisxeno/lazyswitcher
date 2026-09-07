package dev.xeno.accountswitcher.mixin;

import dev.xeno.accountswitcher.AccountSwitcherMod;
import net.minecraft.client.MinecraftClient;
//? if <1.20.5 {
/*import net.minecraft.client.util.Session;
*///?} else {
import net.minecraft.client.session.Session;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "getSession", at = @At("RETURN"), cancellable = true)
    private void overrideSession(CallbackInfoReturnable<Session> cir) {
        if (AccountSwitcherMod.overrideSession != null) {
            cir.setReturnValue(AccountSwitcherMod.overrideSession);
        }
    }
}
