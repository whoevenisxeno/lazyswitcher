package dev.xeno.accountswitcher.auth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Caches the rotated Microsoft refresh token per account uuid so switching keeps working
 * after Prism's own copy has rotated away. Keyed by uuid, survives restarts.
 */
public final class TokenStore {
    public static class Entry {
        public String refreshToken;
        public String accessToken;
        public long expiryMs;
    }

    private static final Path PATH = FabricLoader.getInstance()
        .getConfigDir().resolve("accountswitcher").resolve("tokens.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Map<String, Entry> cache;

    private static synchronized Map<String, Entry> all() {
        if (cache == null) {
            try {
                cache = Files.exists(PATH)
                    ? GSON.fromJson(Files.readString(PATH),
                        new TypeToken<HashMap<String, Entry>>() {}.getType())
                    : new HashMap<>();
            } catch (Exception e) {
                cache = new HashMap<>();
            }
            if (cache == null) cache = new HashMap<>();
        }
        return cache;
    }

    public static synchronized Entry get(String uuid) {
        return all().get(uuid);
    }

    public static synchronized void put(String uuid, String refreshToken, String accessToken, long expiryMs) {
        Entry e = new Entry();
        e.refreshToken = refreshToken;
        e.accessToken = accessToken;
        e.expiryMs = expiryMs;
        all().put(uuid, e);
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(all()));
        } catch (Exception ignored) {}
    }
}
