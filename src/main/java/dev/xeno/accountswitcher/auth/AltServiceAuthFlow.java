package dev.xeno.accountswitcher.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class AltServiceAuthFlow {
    public static final Map<String, String> KNOWN_SERVICES = Map.of(
        "TheAltening", "https://authserver.thealtening.com",
        "EasyMC",      "https://api.easymc.io/v1"
    );

    public static AuthResult authenticate(Map<String, String> params) throws Exception {
        String serviceToken = params.get("serviceToken");
        String authServer   = params.get("authServer");
        String serviceName  = params.get("serviceName");

        return "EasyMC".equals(serviceName)
            ? fetchEasyMC(serviceToken, authServer)
            : fetchTheAltening(serviceToken, authServer);
    }

    private static AuthResult fetchTheAltening(String token, String base) throws Exception {
        String url = base + "/generate?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        JsonObject obj = httpGetJson(url);
        return new AuthResult(
            obj.get("username").getAsString(),
            normalizeUuid(obj.get("uuid").getAsString()),
            obj.get("accessToken").getAsString(),
            null, 0, base
        );
    }

    private static AuthResult fetchEasyMC(String token, String base) throws Exception {
        String url = base + "/token/redeem?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        JsonObject obj = httpGetJson(url);
        return new AuthResult(
            obj.get("mcName").getAsString(),
            normalizeUuid(obj.get("uuid").getAsString()),
            obj.get("token").getAsString(),
            null, 0, base
        );
    }

    private static JsonObject httpGetJson(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("User-Agent", "AccountSwitcher/1.0");
        if (conn.getResponseCode() != 200)
            throw new IOException("HTTP " + conn.getResponseCode());
        try (InputStream is = conn.getInputStream();
             Reader r = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    static String normalizeUuid(String raw) {
        if (raw.contains("-")) return raw;
        return raw.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
    }
}
