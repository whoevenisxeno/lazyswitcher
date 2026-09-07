package dev.xeno.accountswitcher.auth;

import dev.xeno.accountswitcher.account.AccountStorage;
import dev.xeno.accountswitcher.account.AccountType;
import dev.xeno.accountswitcher.account.SavedAccount;
import net.minecraft.client.MinecraftClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class AuthManager {
    private static final long SKEW_MS = 300_000;

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "AccountSwitcher-Auth");
        t.setDaemon(true);
        return t;
    });

    public static void applySession(SavedAccount account) {
        applySession(account, () -> {}, err ->
            dev.xeno.accountswitcher.AccountSwitcherMod.LOGGER.error("Session switch failed: {}", err));
    }

    public static void applySession(SavedAccount account, Runnable onSuccess, Consumer<String> onError) {
        if (account.type != AccountType.MICROSOFT) {
            finish(account, account.accessToken, onSuccess);
            return;
        }
        EXECUTOR.submit(() -> {
            try {
                String token = resolveMicrosoftToken(account);
                MinecraftClient.getInstance().execute(() -> finish(account, token, onSuccess));
            } catch (Throwable e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                String msg = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
                dev.xeno.accountswitcher.AccountSwitcherMod.LOGGER.error("Auth failed for {}", account.username, e);
                MinecraftClient.getInstance().execute(() -> onError.accept(msg));
            }
        });
    }

    private static String resolveMicrosoftToken(SavedAccount acc) throws Exception {
        TokenStore.Entry cached = TokenStore.get(acc.uuid);
        long now = System.currentTimeMillis();

        if (cached != null && cached.accessToken != null && cached.expiryMs - SKEW_MS > now)
            return cached.accessToken;

        // Prefer the token Prism wrote to disk while it is still valid — Prism rotates the
        // single-use refresh token on its own schedule, so only refresh when actually stale.
        if (acc.accessToken != null && !acc.accessToken.isBlank() && acc.tokenExpiry - SKEW_MS > now)
            return acc.accessToken;

        String refreshToken = (cached != null && cached.refreshToken != null)
            ? cached.refreshToken : acc.refreshToken;
        if (refreshToken == null || refreshToken.isBlank()) {
            if (acc.accessToken != null && !acc.accessToken.isBlank()) return acc.accessToken;
            throw new IllegalStateException("No usable token — launch this account once in Prism, then reopen this menu");
        }
        dev.xeno.accountswitcher.AccountSwitcherMod.LOGGER.info("Refreshing Microsoft token for {}", acc.username);
        MsaTokenChain.RefreshedToken r = MsaTokenChain.refresh(refreshToken, acc.msaClientId);

        acc.username = r.username();
        acc.uuid = r.uuid();
        acc.accessToken = r.mcToken();
        acc.refreshToken = r.refreshToken();
        acc.tokenExpiry = r.expiryMs();
        TokenStore.put(r.uuid(), r.refreshToken(), r.mcToken(), r.expiryMs());
        persistIfStored(acc);
        return r.mcToken();
    }

    private static void persistIfStored(SavedAccount acc) {
        try {
            List<SavedAccount> stored = AccountStorage.load();
            boolean changed = false;
            for (SavedAccount s : stored) {
                if (acc.uuid.equalsIgnoreCase(s.uuid) && s.type == AccountType.MICROSOFT) {
                    s.accessToken = acc.accessToken;
                    s.refreshToken = acc.refreshToken;
                    s.tokenExpiry = acc.tokenExpiry;
                    s.msaClientId = acc.msaClientId;
                    changed = true;
                }
            }
            if (changed) AccountStorage.save(stored);
        } catch (Exception ignored) {}
    }

    private static void finish(SavedAccount acc, String token, Runnable onSuccess) {
        SessionSwapper.apply(MinecraftClient.getInstance(), acc, token);
        onSuccess.run();
    }

    public static void authenticateAsync(
        AccountType type,
        Map<String, String> params,
        Consumer<AuthResult> onSuccess,
        Consumer<String> onError,
        Runnable onDeviceCodeReady
    ) {
        EXECUTOR.submit(() -> {
            try {
                AuthResult result = switch (type) {
                    case MICROSOFT   -> MicrosoftAuthFlow.authenticate(params, onDeviceCodeReady);
                    case OFFLINE     -> OfflineAuthFlow.authenticate(params);
                    case ALT_SERVICE -> AltServiceAuthFlow.authenticate(params);
                };
                MinecraftClient.getInstance().execute(() -> onSuccess.accept(result));
            } catch (Throwable e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                String msg = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
                dev.xeno.accountswitcher.AccountSwitcherMod.LOGGER.error("Auth failed", e);
                MinecraftClient.getInstance().execute(() -> onError.accept(msg));
            }
        });
    }
}
