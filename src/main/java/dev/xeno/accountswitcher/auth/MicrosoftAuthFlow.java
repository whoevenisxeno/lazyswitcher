package dev.xeno.accountswitcher.auth;

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
import java.util.Map;

/** OAuth 2.0 device-code flow against consumer Microsoft accounts. No external SDK. */
public class MicrosoftAuthFlow {
    private static final String DEVICE_CODE_URL =
        "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode";
    private static final String TOKEN_URL =
        "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    private static final String SCOPE = "XboxLive.signin offline_access";

    public static AuthResult authenticate(Map<String, String> params, Runnable onDeviceCodeReady)
        throws Exception {

        String clientId = params.getOrDefault("clientId", "");
        if (clientId.isBlank()) clientId = PrismAccountReader.DEFAULT_CLIENT_ID;

        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

        JsonObject dc = postForm(http, DEVICE_CODE_URL,
            "client_id=" + enc(clientId) + "&scope=" + enc(SCOPE));
        String deviceCode = dc.get("device_code").getAsString();
        DeviceCodeState.userCode = dc.get("user_code").getAsString();
        DeviceCodeState.verificationUrl = dc.get("verification_uri").getAsString();
        DeviceCodeState.message = dc.has("message") ? dc.get("message").getAsString() : null;
        int interval = dc.has("interval") ? dc.get("interval").getAsInt() : 5;
        long deadline = System.currentTimeMillis() + dc.get("expires_in").getAsLong() * 1000L;
        if (onDeviceCodeReady != null) onDeviceCodeReady.run();

        String msRefresh = null, msAccess = null;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(interval * 1000L);
            HttpResponse<String> resp = http.send(
                HttpRequest.newBuilder(URI.create(TOKEN_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                        "client_id=" + enc(clientId)
                        + "&grant_type=" + enc("urn:ietf:params:oauth:grant-type:device_code")
                        + "&device_code=" + enc(deviceCode)))
                    .build(),
                HttpResponse.BodyHandlers.ofString());
            JsonObject body = JsonParser.parseString(resp.body()).getAsJsonObject();
            if (resp.statusCode() == 200) {
                msAccess = body.get("access_token").getAsString();
                msRefresh = body.get("refresh_token").getAsString();
                break;
            }
            String err = body.has("error") ? body.get("error").getAsString() : "unknown";
            switch (err) {
                case "authorization_pending" -> {}
                case "slow_down" -> interval += 5;
                case "expired_token", "code_expired" -> throw new Exception("Sign-in code expired, try again");
                case "authorization_declined" -> throw new Exception("Sign-in was declined");
                default -> throw new Exception("Microsoft sign-in failed: " + err);
            }
        }
        if (msAccess == null) throw new Exception("Timed out waiting for sign-in");

        MsaTokenChain.McToken mc = MsaTokenChain.exchangeForMcToken(msAccess);
        JsonObject profile = MsaTokenChain.fetchProfile(mc.token());
        if (!profile.has("id")) throw new Exception("This account does not own Minecraft");

        String uuid = AltServiceAuthFlow.normalizeUuid(profile.get("id").getAsString());
        long expiryMs = mc.expiryMs();
        TokenStore.put(uuid, msRefresh, mc.token(), expiryMs);
        return new AuthResult(profile.get("name").getAsString(), uuid, mc.token(), msRefresh, expiryMs, null);
    }

    private static JsonObject postForm(HttpClient http, String url, String form) throws Exception {
        HttpResponse<String> resp = http.send(
            HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build(),
            HttpResponse.BodyHandlers.ofString());
        JsonObject obj = JsonParser.parseString(resp.body()).getAsJsonObject();
        if (resp.statusCode() != 200) {
            String err = obj.has("error_description") ? obj.get("error_description").getAsString()
                : obj.has("error") ? obj.get("error").getAsString() : "HTTP " + resp.statusCode();
            throw new Exception(err);
        }
        return obj;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    public static final class DeviceCodeState {
        public static volatile String userCode;
        public static volatile String verificationUrl;
        public static volatile String message;
    }
}
