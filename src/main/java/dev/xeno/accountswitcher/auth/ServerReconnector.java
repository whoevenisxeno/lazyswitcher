package dev.xeno.accountswitcher.auth;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
//? if <1.20.5 {
/*import net.minecraft.client.gui.screen.ConnectScreen;
*///?} else {
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
//?}
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

public final class ServerReconnector {

    /** The server the client is currently on, or null if on a menu / singleplayer. */
    public static ServerInfo currentServer(MinecraftClient mc) {
        if (mc.isInSingleplayer()) return null;
        ServerInfo info = mc.getCurrentServerEntry();
        if (info == null && mc.getNetworkHandler() != null) info = mc.getNetworkHandler().getServerInfo();
        //? if <1.20.5 {
        /*if (info == null || info.address == null || info.address.isBlank()) return null;
        *///?} else {
        if (info == null || info.isRealm() || info.address == null || info.address.isBlank()) return null;
        //?}
        return ServerInfo.fromNbt(info.toNbt());
    }

    public static void reconnect(MinecraftClient mc, ServerInfo server) {
        if (mc.world != null || mc.getNetworkHandler() != null)
            //? if <1.20.5 {
            /*mc.disconnect(new TitleScreen());
            *///?} else {
            mc.disconnect(new TitleScreen(), false);
            //?}
        Screen parent = new MultiplayerScreen(new TitleScreen());
        //? if <1.20.5 {
        /*ConnectScreen.connect(parent, mc, ServerAddress.parse(server.address), server, false);
        *///?} else {
        ConnectScreen.connect(parent, mc, ServerAddress.parse(server.address), server, false, null);
        //?}
    }
}
