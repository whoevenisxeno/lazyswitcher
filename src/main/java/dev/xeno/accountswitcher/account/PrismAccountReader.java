package dev.xeno.accountswitcher.account;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PrismAccountReader {
    public static final String DEFAULT_CLIENT_ID = "c36a9fb6-4f2a-41ff-90bd-ae7cc92031eb";

    private static Path resolveAccountsFile() {
        String override = System.getProperty("lazyswitcher.accountsFile");
        if (override != null && !override.isBlank()) return Paths.get(override);

        String home = System.getProperty("user.home", ".");
        String os = System.getProperty("os.name", "").toLowerCase();
        String appData = System.getenv("APPDATA");
        String xdgData = System.getenv("XDG_DATA_HOME");

        Set<Path> roots = new LinkedHashSet<>();
        // launcher working dir is <root>/instances/<name>/minecraft — walk up to find the root
        Path cwd = Paths.get("").toAbsolutePath();
        for (Path p = cwd; p != null; p = p.getParent()) roots.add(p);

        for (String launcher : new String[]{"PrismLauncher", "PolyMC", "MultiMC", "ManyMC"}) {
            if (appData != null) roots.add(Paths.get(appData, launcher));
            if (os.contains("mac")) roots.add(Paths.get(home, "Library", "Application Support", launcher));
            if (xdgData != null) roots.add(Paths.get(xdgData, launcher));
            roots.add(Paths.get(home, ".local", "share", launcher));
            roots.add(Paths.get(home, ".var", "app", "org.prismlauncher.PrismLauncher", "data", launcher));
            roots.add(Paths.get(home, "." + launcher.toLowerCase()));
        }
        for (Path root : roots) {
            if (root == null) continue;
            Path f = root.resolve("accounts.json");
            if (Files.isRegularFile(f)) return f;
        }
        return null;
    }

    public static List<SavedAccount> load() {
        List<SavedAccount> result = new ArrayList<>();
        Path file = resolveAccountsFile();
        if (file == null) return result;
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            JsonArray accounts = root.getAsJsonArray("accounts");
            if (accounts == null) return result;
            for (JsonElement el : accounts) {
                JsonObject acc = el.getAsJsonObject();
                JsonObject profile = acc.has("profile") ? acc.getAsJsonObject("profile") : null;
                if (profile == null || !profile.has("name") || !profile.has("id")) continue;
                String name = profile.get("name").getAsString();
                String rawUuid = profile.get("id").getAsString();
                if (name.isBlank() || rawUuid.isBlank()) continue;
                // "ygg" is the actual Minecraft access token used to join servers.
                // "xrp-mc" is only the XSTS (Xbox) token — servers reject it.
                JsonObject ygg = acc.has("ygg") ? acc.getAsJsonObject("ygg") : null;
                String token = (ygg != null && ygg.has("token")) ? ygg.get("token").getAsString() : "";
                long expSec = (ygg != null && ygg.has("exp")) ? ygg.get("exp").getAsLong() : 0;

                JsonObject msa = acc.has("msa") ? acc.getAsJsonObject("msa") : null;
                String refresh = (msa != null && msa.has("refresh_token")) ? msa.get("refresh_token").getAsString() : null;
                String clientId = acc.has("msa-client-id") ? acc.get("msa-client-id").getAsString() : DEFAULT_CLIENT_ID;

                SavedAccount sa = new SavedAccount();
                sa.type = AccountType.MICROSOFT;
                sa.alias = "[Prism] " + name;
                sa.username = name;
                sa.uuid = formatUuid(rawUuid);
                sa.accessToken = token;
                sa.refreshToken = refresh;
                sa.msaClientId = clientId;
                sa.tokenExpiry = expSec > 0 ? expSec * 1000L : 0;
                result.add(sa);
            }
        } catch (Exception ignored) {}
        return result;
    }

    private static String formatUuid(String raw) {
        if (raw.contains("-") || raw.length() != 32) return raw;
        return raw.substring(0,8) + "-" + raw.substring(8,12) + "-"
             + raw.substring(12,16) + "-" + raw.substring(16,20) + "-" + raw.substring(20);
    }
}
