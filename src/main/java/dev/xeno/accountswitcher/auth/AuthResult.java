package dev.xeno.accountswitcher.auth;

public record AuthResult(
    String username,
    String uuid,
    String accessToken,
    String refreshToken,
    long tokenExpiry,
    String authServer
) {}
