package dev.xeno.accountswitcher.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AccountStorage {
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
        .getConfigDir().resolve("accountswitcher").resolve("accounts.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static class AccountFile {
        List<SavedAccount> accounts = new ArrayList<>();
    }

    public static List<SavedAccount> load() {
        try {
            if (!Files.exists(CONFIG_PATH)) return new ArrayList<>();
            String json = Files.readString(CONFIG_PATH);
            AccountFile file = GSON.fromJson(json, AccountFile.class);
            return (file != null && file.accounts != null) ? file.accounts : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static void save(List<SavedAccount> accounts) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            AccountFile file = new AccountFile();
            file.accounts = accounts;
            Files.writeString(CONFIG_PATH, GSON.toJson(file));
        } catch (IOException e) {
            // log silently — non-fatal
        }
    }
}
