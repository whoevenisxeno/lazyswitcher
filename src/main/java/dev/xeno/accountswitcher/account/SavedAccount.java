package dev.xeno.accountswitcher.account;

public class SavedAccount {
    public AccountType type;
    public String alias;
    public String username;
    public String uuid;
    public String accessToken;
    public String refreshToken;
    public String msaClientId;
    public long tokenExpiry;
    public String serviceToken;
    public String authServer;

    public String displayName() {
        return (alias != null && !alias.isBlank()) ? alias : username;
    }
}
