package com.example.keylogin;
import com.example.keylogin.R;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import rikka.shizuku.Shizuku;
import android.content.pm.PackageManager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (rikka.shizuku.Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                android.widget.TextView txt = findViewById(R.id.txt_shizuku_detail);
                if (txt != null) {
                    txt.setText("Shizuku Connected");
                    txt.setTextColor(android.graphics.Color.GREEN);
                }
                android.widget.Button btn = findViewById(R.id.btn_connect_shizuku);
                if (btn != null) {
                    btn.setText("CONNECTED");
                    btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.GREEN));
                }
            }
        } catch (Exception e) {}
    }


    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private LinearLayout content;
    private TextView titleView;
    private WebView web;
    private String webUrl = "";
    private String expire = "";
    private String maskedKey = "";

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        expire = nz(getIntent().getStringExtra("expire"));
        String k = nz(getIntent().getStringExtra("key"));
        maskedKey = k.length() > 4 ? k.substring(0, 4) + "****" : "****";

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0A0A0F"));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(20), dp(16), dp(12), dp(8));

        titleView = new TextView(this);
        titleView.setText("Dashboard");
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(24);
        titleView.setTypeface(null, Typeface.BOLD);
        bar.addView(titleView, new LinearLayout.LayoutParams(0, WRAP, 1f));

        TextView out = new TextView(this);
        out.setText("LOG OUT");
        out.setTextColor(Color.parseColor("#FF6B6B"));
        out.setTextSize(14);
        out.setTypeface(null, Typeface.BOLD);
        out.setPadding(dp(12), dp(8), dp(12), dp(8));
        out.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
        bar.addView(out);
        root.addView(bar);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content, new LinearLayout.LayoutParams(MATCH, 0, 1f));

        setContentView(R.layout.activity_main);


// Shizuku integration: official Binder and permission API.
final int shizukuRequestCode = 4201;
final android.widget.Button shizukuButton =
        findViewById(R.id.btn_connect_shizuku);
final android.widget.TextView shizukuStatus =
        findViewById(R.id.txt_shizuku_detail);

final Runnable refreshShizuku = () -> {
    try {
        if (!rikka.shizuku.Shizuku.pingBinder()) {
            shizukuButton.setText("CONNECT");
            shizukuButton.setTextColor(0xFF000000);
            shizukuStatus.setText("Shizuku is not running. Start it first.");
            shizukuStatus.setTextColor(0xFFFFA500);
            return;
        }

        if (rikka.shizuku.Shizuku.checkSelfPermission()
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            shizukuButton.setText("CONNECTED");
            shizukuButton.setTextColor(0xFF00C853);
            shizukuStatus.setText("Permission granted");
            shizukuStatus.setTextColor(0xFF00C853);
        } else {
            shizukuButton.setText("GRANT PERMISSION");
            shizukuButton.setTextColor(0xFF000000);
            shizukuStatus.setText("Shizuku is running. Permission required.");
            shizukuStatus.setTextColor(0xFFFFA500);
        }
    } catch (Throwable error) {
        shizukuButton.setText("CONNECT");
        shizukuStatus.setText("Shizuku unavailable. Open Shizuku and retry.");
        shizukuStatus.setTextColor(0xFFFF5252);
        android.util.Log.e("ShizukuIntegration", "Status check failed", error);
    }
};

try {
    rikka.shizuku.Shizuku.addBinderReceivedListenerSticky(
            () -> runOnUiThread(refreshShizuku));
    rikka.shizuku.Shizuku.addBinderDeadListener(() -> runOnUiThread(() -> {
        shizukuButton.setText("CONNECT");
        shizukuButton.setTextColor(0xFF000000);
        shizukuStatus.setText("Shizuku connection lost. Restart Shizuku.");
        shizukuStatus.setTextColor(0xFFFF5252);
    }));

    rikka.shizuku.Shizuku.addRequestPermissionResultListener(
            (requestCode, grantResult) -> {
                if (requestCode == shizukuRequestCode) {
                    runOnUiThread(() -> {
                        if (grantResult ==
                                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            refreshShizuku.run();
                        } else {
                            shizukuButton.setText("CONNECT");
                            shizukuStatus.setText("Shizuku permission denied.");
                            shizukuStatus.setTextColor(0xFFFF5252);
                        }
                    });
                }
            });

    shizukuButton.setOnClickListener(view -> {
        try {
            if (!rikka.shizuku.Shizuku.pingBinder()) {
                shizukuStatus.setText(
                        "Start Shizuku using wireless debugging or ADB first.");
                shizukuStatus.setTextColor(0xFFFFA500);
                return;
            }

            if (rikka.shizuku.Shizuku.checkSelfPermission()
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                refreshShizuku.run();
            } else {
                rikka.shizuku.Shizuku.requestPermission(shizukuRequestCode);
                shizukuStatus.setText("Waiting for Shizuku permission...");
                shizukuStatus.setTextColor(0xFFFFA500);
            }
        } catch (Throwable error) {
            android.util.Log.e("ShizukuIntegration", "Connect failed", error);
            shizukuStatus.setText("Unable to connect. Check Shizuku status.");
            shizukuStatus.setTextColor(0xFFFF5252);
        }
    });

    refreshShizuku.run();
} catch (Throwable error) {
    android.util.Log.e("ShizukuIntegration", "Initialization failed", error);
    shizukuStatus.setText("Shizuku initialization failed. Check logcat.");
    shizukuStatus.setTextColor(0xFFFF5252);
}

render(RemoteConfig.cached(this));

        RemoteConfig.fetch(this, this::render);
    }

    private String left(String exp) {
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            Date d = f.parse(exp);
            long ms = d.getTime() - System.currentTimeMillis();
            if (ms <= 0) return "Expired";
            return (ms / 86400000L) + " days left";
        } catch (Exception e) {
            return "";
        }
    }

    private void render(JSONObject c) {
        titleView.setText(c.optString("dashboard_title", "Dashboard"));
        int accent = Color.parseColor("#00DDF8");
        try {
            accent = Color.parseColor(c.optString("accent", "#00DDF8"));
        } catch (Exception ignored) { }

        String url = c.optString("dashboard_url", "");
        if (url.startsWith("https://")) {
            if (web == null || !url.equals(webUrl)) {
                content.removeAllViews();
                web = new WebView(this);
                web.setBackgroundColor(Color.parseColor("#0A0A0F"));
                WebSettings s = web.getSettings();
                s.setJavaScriptEnabled(true);
                s.setDomStorageEnabled(true);
                web.setWebViewClient(new WebViewClient());
                web.loadUrl(url);
                webUrl = url;
                content.addView(web, new LinearLayout.LayoutParams(MATCH, MATCH));
            }
            return;
        }

        if (web != null) {
            web.destroy();
            web = null;
            webUrl = "";
        }
        content.removeAllViews();

        ScrollView sv = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(20), dp(4), dp(20), dp(32));
        sv.addView(list);
        content.addView(sv, new LinearLayout.LayoutParams(MATCH, MATCH));

        String lic = "Status: Active\nKey: " + maskedKey;
        if (!expire.isEmpty()) {
            lic += "\nExpires: " + expire;
            String d = left(expire);
            if (!d.isEmpty()) lic += " (" + d + ")";
        }
        list.addView(card("LICENSE", lic, "", "", accent));

        String notice = c.optString("notice", "");
        if (!notice.isEmpty()) list.addView(card("NOTICE", notice, "", "", accent));

        JSONArray arr = c.optJSONArray("cards");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                list.addView(card(o.optString("title", ""), o.optString("text", ""),
                        o.optString("button", ""), o.optString("url", ""), accent));
            }
        }
    }

    private View card(String title, String text, String btn, String url, int accent) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundResource(R.drawable.bg_input);
        box.setPadding(dp(20), dp(18), dp(20), dp(18));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.topMargin = dp(14);
        box.setLayoutParams(lp);

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(accent);
        t.setTextSize(13);
        t.setTypeface(null, Typeface.BOLD);
        box.addView(t);

        if (!text.isEmpty()) {
            TextView d = new TextView(this);
            d.setText(text);
            d.setTextColor(Color.WHITE);
            d.setTextSize(16);
            d.setPadding(0, dp(8), 0, 0);
            box.addView(d);
        }

        if (!btn.isEmpty() && url.startsWith("https://")) {
            MaterialButton mb = new MaterialButton(this);
            mb.setText(btn);
            mb.setTextColor(Color.BLACK);
            mb.setBackgroundTintList(ColorStateList.valueOf(accent));
            mb.setCornerRadius(dp(24));
            LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(MATCH, dp(52));
            bl.topMargin = dp(14);
            mb.setLayoutParams(bl);
            mb.setOnClickListener(v ->
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))));
            box.addView(mb);
        }
        return box;
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
