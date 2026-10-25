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
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class LoginActivity extends AppCompatActivity {

    private static final String DEF_API = "https://a.hamse.workers.dev/verify";
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

    private static class Result {
        boolean ok;
        String msg = "Invalid Key";
        String expire = "";
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
            Result r = new Result();
            try {
                r = verify(key);
            } catch (UnknownHostException | ConnectException e) {
                r.msg = "No internet connection";
            } catch (SocketTimeoutException e) {
                r.msg = "Connection timed out";
            } catch (Exception e) {
                r.msg = "Server error, try again";
            }
            final Result fr = r;
            runOnUiThread(() -> {
                loginBtn.setEnabled(true);
                loginBtn.setText("LOG IN");
                if (fr.ok) {
                    Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(this, MainActivity.class);
                    i.putExtra("expire", fr.expire);
                    startActivity(i);
                    finish();
                } else {
                    status.setTextColor(Color.parseColor("#FF6B6B"));
                    status.setText(fr.msg);
                }
            });
        }).start();
    }

    private Result verify(String key) throws Exception {
        String base = cfg.optString("api_url", DEF_API);
        String url = base + (base.contains("?") ? "&" : "?")
                + "key=" + URLEncoder.encode(key, "UTF-8");

        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("GET");
        c.setConnectTimeout(10000);
        c.setReadTimeout(10000);
        c.setRequestProperty("Accept", "application/json");

        int code = c.getResponseCode();
        InputStream is = code < 400 ? c.getInputStream() : c.getErrorStream();
        StringBuilder sb = new StringBuilder();
        if (is != null) {
            try (BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = rd.readLine()) != null) sb.append(line);
            }
        }

        Result res = new Result();
        JSONObject j;
        try {
            j = new JSONObject(sb.toString());
        } catch (Exception e) {
            res.msg = "Server error, try again";
            return res;
        }
        res.ok = j.optBoolean("valid", false);
        res.expire = j.optString("expire", "");
        if (!res.ok) {
            String reason = j.optString("reason", "");
            res.msg = reason.isEmpty() ? "Invalid Key" : reason;
        }
        return res;
    }
}
