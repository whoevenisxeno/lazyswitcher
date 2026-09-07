package dev.xeno.accountswitcher.auth;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

public class OfflineAuthFlow {
    public static AuthResult authenticate(Map<String, String> params) {
        String username = params.get("username");
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Username required");

        UUID uuid = UUID.nameUUIDFromBytes(
            ("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8)
        );
        return new AuthResult(username, uuid.toString(), "", null, 0, null);
    }
}
