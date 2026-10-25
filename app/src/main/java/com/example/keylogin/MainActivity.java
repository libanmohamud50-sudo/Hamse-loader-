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

        // Shizuku Setup
        try {
            android.widget.Button btnConnect = findViewById(R.id.btn_connect_shizuku);
            if (btnConnect != null) {
                btnConnect.setOnClickListener(v -> {
                    if (Shizuku.isPreV11() || Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                        android.widget.Toast.makeText(this, "Shizuku already connected", android.widget.Toast.LENGTH_SHORT).show();
                    } else if (Shizuku.isPreV11() || Shizuku.checkSelfPermission() == PackageManager.PERMISSION_DENIED) {
                        Shizuku.requestPermission(0);
                    }
                });
            }
        } catch (Exception e) {
            // Haddii btn_connect_shizuku uusan jirin, iska dhaaf
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
