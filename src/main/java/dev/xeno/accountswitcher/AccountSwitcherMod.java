package dev.xeno.accountswitcher;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountSwitcherMod implements ClientModInitializer {
    public static final String MOD_ID = "accountswitcher";
    public static final String NAME = "LazySwitcher";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    public static Session originalSession;
    public static volatile Session overrideSession = null;

    @Override
    public void onInitializeClient() {
        originalSession = MinecraftClient.getInstance().getSession();
        LOGGER.info("{} ready - signed in as {}", NAME, originalSession.getUsername());
    }
}
