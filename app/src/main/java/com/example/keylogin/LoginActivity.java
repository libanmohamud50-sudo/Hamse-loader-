package com.example.keylogin;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
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

    // TODO: replace with your own license API and "get key" page
    private static final String API_URL = "https://example.com/api/verify";
    private static final String GET_KEY_URL = "https://example.com";

    private EditText keyInput;
    private TextView status;
    private Button loginBtn;
    private boolean visible = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_login);

        keyInput = findViewById(R.id.keyInput);
        status = findViewById(R.id.status);
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
            String[] langs = {"English", "Soomaali", "العربية"};
            new AlertDialog.Builder(this)
                    .setTitle("Select language")
                    .setItems(langs, (d, i) ->
                            Toast.makeText(this, langs[i], Toast.LENGTH_SHORT).show())
                    .show();
        });

        findViewById(R.id.getKeyBtn).setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(GET_KEY_URL))));

        loginBtn.setOnClickListener(v -> doLogin());
    }

    private void doLogin() {
        final String key = keyInput.getText().toString().trim();
        if (key.isEmpty()) {
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

        HttpURLConnection c = (HttpURLConnection) new URL(API_URL).openConnection();
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
        // Expected server reply: {"valid": true}
        return new JSONObject(sb.toString()).optBoolean("valid", false);
    }
}
