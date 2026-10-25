package com.example.keylogin;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LoginActivity extends AppCompatActivity {

    private static final String DEF_API = "https://example.com/api/verify";
    private static final String DEF_GET = "https://example.com";

    private EditText keyInput;
    private TextView status, title, subtitle;
    private Button loginBtn;
    private boolean visible = false;
    private boolean maintenance = false;
    private boolean updateShown = false;
    private JSONObject cfg = new JSONObject();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_login);

        keyInput = findViewById(R.id.keyInput);
        status = findViewById(R.id.status);
        title = findViewById(R.id.title);
        subtitle = findViewById(R.id.subtitle);
        loginBtn = findViewById(R.id.loginBtn);

        findViewById(R.id.pasteBtn).setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip()) {
                ClipData clip = cm.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0) {
                    keyInput.setText(clip.getItemAt(0).coerceToText(this).toString().trim());
                }
            }
        });

        findViewById(R.id.eyeBtn).setOnClickListener(v -> {
            visible = !visible;
            keyInput.setInputType(InputType.TYPE_CLASS_TEXT | (visible
                    ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_TEXT_VARIATION_PASSWORD));
            keyInput.setSelection(keyInput.getText().length());
        });

        findViewById(R.id.langBtn).setOnClickListener(v -> {
            String[] langs = {"English", "Soomaali", "Arabic"};
            new AlertDialog.Builder(this)
                    .setTitle("Select language")
                    .setItems(langs, (d, i) ->
                            Toast.makeText(this, langs[i], Toast.LENGTH_SHORT).show())
                    .show();
        });

        findViewById(R.id.getKeyBtn).setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(cfg.optString("get_key_url", DEF_GET)))));

        loginBtn.setOnClickListener(v -> doLogin());

        // 1) apply saved config instantly, 2) refresh from the internet
        applyConfig(RemoteConfig.cached(this));
        RemoteConfig.fetch(this, this::applyConfig);
    }

    @Override
    protected void onResume() {
        super.onResume();
        RemoteConfig.fetch(this, this::applyConfig);
    }

    private void applyConfig(JSONObject c) {
        cfg = c;
        title.setText(c.optString("title", "Log in"));
        subtitle.setText(c.optString("subtitle", ""));
        try {
            loginBtn.setBackgroundTintList(ColorStateList.valueOf(
                    Color.parseColor(c.optString("accent", "#00DDF8"))));
        } catch (Exception ignored) { }

        maintenance = c.optBoolean("maintenance", false);
        if (maintenance) {
            status.setTextColor(Color.parseColor("#FFB020"));
            status.setText(c.optString("maintenance_msg", "Under maintenance"));
        } else {
            String n = c.optString("notice", "");
            status.setTextColor(Color.parseColor("#9AA0AE"));
            status.setText(n);
        }
        checkUpdate(c);
    }

    private void checkUpdate(JSONObject c) {
        if (updateShown) return;
        int latest = c.optInt("latest_version", 0);
        int current = 0;
        try {
            current = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (Exception ignored) { }
        final String url = c.optString("update_url", "");
        if (latest <= current || url.isEmpty()) return;
        updateShown = true;
        final boolean force = c.optBoolean("force_update", false);
        AlertDialog.Builder d = new AlertDialog.Builder(this)
                .setTitle("Update")
                .setMessage(c.optString("update_msg", "A new version is available"))
                .setCancelable(!force)
                .setPositiveButton("UPDATE", (x, i) ->
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))));
        if (!force) d.setNegativeButton("LATER", null);
        d.show();
    }

    private void doLogin() {
        if (maintenance) return;
        final String key = keyInput.getText().toString().trim();
        if (key.isEmpty()) {
            status.setTextColor(Color.parseColor("#FF6B6B"));
            status.setText("Please enter a key");
            return;
        }
        status.setText("");
        loginBtn.setEnabled(false);
        loginBtn.setText("CHECKING...");
        new Thread(() -> {
            String err = null;
            boolean ok = false;
            try {
                ok = verify(key);
                if (!ok) err = "Invalid or expired key";
            } catch (Exception e) {
                err = "Network or server error";
            }
            final boolean fOk = ok;
            final String fErr = err;
            runOnUiThread(() -> {
                loginBtn.setEnabled(true);
                loginBtn.setText("LOG IN");
                if (fOk) {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                } else {
                    status.setTextColor(Color.parseColor("#FF6B6B"));
                    status.setText(fErr);
                }
            });
        }).start();
    }

    private boolean verify(String key) throws Exception {
        String device = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        JSONObject body = new JSONObject();
        body.put("key", key);
        body.put("device", device);

        HttpURLConnection c = (HttpURLConnection)
                new URL(cfg.optString("api_url", DEF_API)).openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(10000);
        c.setReadTimeout(10000);
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json");
        try (OutputStream os = c.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
        InputStream is = c.getResponseCode() < 400 ? c.getInputStream() : c.getErrorStream();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
        }
        return new JSONObject(sb.toString()).optBoolean("valid", false);
    }
}
