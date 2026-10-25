package com.example.keylogin;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class RemoteConfig {
    public static final String URL =
            "https://raw.githubusercontent.com/libanmohamud50-sudo/Hamse-loader-/main/config.json";

    public interface Callback { void onConfig(JSONObject cfg); }

    public static JSONObject cached(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences("cfg", Context.MODE_PRIVATE);
        try { return new JSONObject(sp.getString("json", "{}")); }
        catch (Exception e) { return new JSONObject(); }
    }

    public static void fetch(Context ctx, Callback cb) {
        final Context app = ctx.getApplicationContext();
        new Thread(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection)
                        new URL(URL + "?t=" + System.currentTimeMillis()).openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                c.setUseCaches(false);
                StringBuilder sb = new StringBuilder();
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                }
                final JSONObject j = new JSONObject(sb.toString());
                app.getSharedPreferences("cfg", Context.MODE_PRIVATE)
                        .edit().putString("json", j.toString()).apply();
                new Handler(Looper.getMainLooper()).post(() -> cb.onConfig(j));
            } catch (Exception ignored) { }
        }).start();
    }
}
