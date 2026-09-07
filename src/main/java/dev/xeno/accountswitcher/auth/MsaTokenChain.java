package dev.xeno.accountswitcher.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.xeno.accountswitcher.account.PrismAccountReader;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Microsoft -> Xbox Live -> XSTS -> Minecraft token chain, plus refresh-token renewal.
 * Deliberately free of msal4j so it loads even when that (compile-only) library is absent.
 */
public final class MsaTokenChain {
    private static final String[] TOKEN_URLS = {
        "https://login.live.com/oauth20_token.srf",
        "https://login.microsoftonline.com/consumers/oauth2/v2.0/token"
    };

    public record RefreshedToken(String username, String uuid, String mcToken,
                                 String refreshToken, long expiryMs) {}

    public record McToken(String token, long expiryMs) {}

    public static RefreshedToken refresh(String refreshToken, String clientId) throws Exception {
        if (refreshToken == null || refreshToken.isBlank())
            throw new IllegalStateException("No refresh token stored for this account — re-add it");
        if (clientId == null || clientId.isBlank())
            clientId = PrismAccountReader.DEFAULT_CLIENT_ID;

        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        String form = "client_id=" + enc(clientId)
            + "&grant_type=refresh_token"
            + "&scope=" + enc("XboxLive.signin offline_access")
            + "&refresh_token=" + enc(refreshToken);

        HttpResponse<String> resp = null;
        for (String url : TOKEN_URLS) {
            resp = http.send(
                HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(form)).build(),
                HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) break;
        }
        if (resp == null || resp.statusCode() != 200) {
            String body = resp == null ? "" : resp.body();
            if (body != null && body.contains("invalid_grant"))
                throw new Exception("Refresh token out of sync with Prism — launch this account once in Prism, then reopen this menu");
            throw new Exception("Microsoft refresh failed (HTTP " + (resp == null ? "?" : resp.statusCode()) + ")");
        }

        JsonObject obj = JsonParser.parseString(resp.body()).getAsJsonObject();
        String msAccessToken = obj.get("access_token").getAsString();
        String newRefresh = obj.has("refresh_token") ? obj.get("refresh_token").getAsString() : refreshToken;

        McToken mc = exchangeForMcToken(msAccessToken);
        JsonObject profile = fetchProfile(mc.token());
        if (!profile.has("id")) throw new Exception("Account does not own Minecraft");
        return new RefreshedToken(
            profile.get("name").getAsString(),
            AltServiceAuthFlow.normalizeUuid(profile.get("id").getAsString()),
            mc.token(), newRefresh, mc.expiryMs());
    }

    public static McToken exchangeForMcToken(String msAccessToken) throws Exception {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

        JsonObject xblProps = new JsonObject();
        xblProps.addProperty("AuthMethod", "RPS");
        xblProps.addProperty("SiteName", "user.auth.xboxlive.com");
        xblProps.addProperty("RpsTicket", "d=" + msAccessToken);
        JsonObject xblBody = new JsonObject();
        xblBody.add("Properties", xblProps);
        xblBody.addProperty("RelyingParty", "http://auth.xboxlive.com");
        xblBody.addProperty("TokenType", "JWT");
        JsonObject xblResp = postJson(http, "https://user.auth.xboxlive.com/user/authenticate", xblBody);
        String xblToken = xblResp.get("Token").getAsString();
        String userHash = xblResp.getAsJsonObject("DisplayClaims")
            .getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();

        JsonArray tokens = new JsonArray();
        tokens.add(xblToken);
        JsonObject xstsProps = new JsonObject();
        xstsProps.addProperty("SandboxId", "RETAIL");
        xstsProps.add("UserTokens", tokens);
        JsonObject xstsBody = new JsonObject();
        xstsBody.add("Properties", xstsProps);
        xstsBody.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        xstsBody.addProperty("TokenType", "JWT");
        JsonObject xstsResp = postJson(http, "https://xsts.auth.xboxlive.com/xsts/authorize", xstsBody);
        if (xstsResp.has("XErr")) throw new Exception(xstsError(xstsResp.get("XErr").getAsLong()));
        String xstsToken = xstsResp.get("Token").getAsString();

        JsonObject mcBody = new JsonObject();
        mcBody.addProperty("identityToken", "XBL3.0 x=" + userHash + ";" + xstsToken);
        JsonObject mcResp = postJson(http, "https://api.minecraftservices.com/authentication/login_with_xbox", mcBody);
        long expiryMs = mcResp.has("expires_in")
            ? System.currentTimeMillis() + mcResp.get("expires_in").getAsLong() * 1000L
            : 0;
        return new McToken(mcResp.get("access_token").getAsString(), expiryMs);
    }

    public static JsonObject fetchProfile(String mcToken) throws Exception {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        return JsonParser.parseString(http.send(
            HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                .header("Authorization", "Bearer " + mcToken).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        ).body()).getAsJsonObject();
    }

    private static String xstsError(long xerr) {
        return switch ((int) (xerr - 2148916230L)) {
            case 3  -> "This Microsoft account has no Xbox account";
            case 5  -> "Xbox Live is not available in this account's region";
            case 7  -> "This account is a child and needs to be added to a Family";
            default -> "Xbox authorization failed (XErr " + xerr + ")";
        };
    }

    private static JsonObject postJson(HttpClient http, String url, JsonObject body) throws Exception {
        return JsonParser.parseString(http.send(
            HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(),
            HttpResponse.BodyHandlers.ofString()
        ).body()).getAsJsonObject();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
