package dev.xeno.accountswitcher.auth;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import dev.xeno.accountswitcher.AccountSwitcherMod;
import dev.xeno.accountswitcher.account.AccountType;
import dev.xeno.accountswitcher.account.SavedAccount;
import dev.xeno.accountswitcher.mixin.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
//? if <1.20.5 {
/*import net.minecraft.client.util.ProfileKeys;
import net.minecraft.client.util.Session;
*///?} else {
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
//?}

import java.util.Optional;
import java.util.UUID;

/** Applies a session to the running client, including the auth-bound services used for chat signing. */
public final class SessionSwapper {

    public static void apply(MinecraftClient mc, SavedAccount acc, String token) {
        String access = acc.type == AccountType.OFFLINE || token == null ? "" : token;
        //? if <1.20.5 {
        /*Session session = new Session(
            acc.username, acc.uuid, access, Optional.empty(), Optional.empty(),
            acc.type == AccountType.MICROSOFT ? Session.AccountType.MSA : Session.AccountType.LEGACY);
        *///?} else if <1.21.11 {
        /*Session session = new Session(
            acc.username, UUID.fromString(acc.uuid), access, Optional.empty(), Optional.empty(),
            acc.type == AccountType.MICROSOFT ? Session.AccountType.MSA : Session.AccountType.LEGACY);
        *///?} else {
        Session session = new Session(
            acc.username, UUID.fromString(acc.uuid), access, Optional.empty(), Optional.empty());
        //?}
        applySession(mc, session);
        AccountSwitcherMod.LOGGER.info("Now playing as {} ({})", acc.username, acc.uuid);
    }

    /** Restore whatever account the game launched with. */
    public static void revert(MinecraftClient mc) {
        applySession(mc, AccountSwitcherMod.originalSession);
        AccountSwitcherMod.overrideSession = null;
        AccountSwitcherMod.LOGGER.info("Reverted to {}", AccountSwitcherMod.originalSession.getUsername());
    }

    private static void applySession(MinecraftClient mc, Session session) {
        MinecraftClientAccessor client = (MinecraftClientAccessor) mc;
        client.setSession(session);
        AccountSwitcherMod.overrideSession = session;

        String access = session.getAccessToken();
        if (access == null || access.isBlank()) return;
        try {
            YggdrasilAuthenticationService yggdrasil =
                new YggdrasilAuthenticationService(mc.getNetworkProxy());
            UserApiService userApi = yggdrasil.createUserApiService(access);
            client.setUserApiService(userApi);
            client.setProfileKeys(ProfileKeys.create(userApi, session, mc.runDirectory.toPath()));
        } catch (Throwable t) {
            // Non-fatal: joining still works; only chat signing on secure servers may lag until restart.
            AccountSwitcherMod.LOGGER.warn("Could not refresh chat-signing keys: {}", t.toString());
        }
    }
}
