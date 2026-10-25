package com.example.keylogin;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Updater {
    private static String pending = null;
    private static boolean busy = false;

    private static boolean canInstall(Activity a) {
        return Build.VERSION.SDK_INT < 26 || a.getPackageManager().canRequestPackageInstalls();
    }

    public static void resumePending(Activity a) {
        if (pending != null && canInstall(a)) {
            String u = pending;
            pending = null;
            start(a, u);
        }
    }

    public static void start(final Activity a, final String url) {
        if (busy) return;

        if (!canInstall(a)) {
            pending = url;
            new AlertDialog.Builder(a)
                    .setTitle("Permission needed")
                    .setMessage("Allow this app to install updates, then come back.")
                    .setCancelable(false)
                    .setPositiveButton("OK", (d, w) -> {
                        Intent i = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:" + a.getPackageName()));
                        a.startActivity(i);
                    })
                    .show();
            return;
        }
        busy = true;

        final float dp = a.getResources().getDisplayMetrics().density;
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (24 * dp);
        box.setPadding(pad, pad, pad, pad / 2);

        final TextView msg = new TextView(a);
        msg.setText("Updating... 0%");
        msg.setTextSize(16);
        box.addView(msg);

        final ProgressBar bar = new ProgressBar(a, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (16 * dp);
        box.addView(bar, lp);

        final AlertDialog dlg = new AlertDialog.Builder(a)
                .setTitle("Update")
                .setView(box)
                .setCancelable(false)
                .create();
        dlg.show();

        new Thread(() -> {
            String err = null;
            File out = null;
            try {
                File dir = new File(a.getCacheDir(), "updates");
                if (!dir.exists()) dir.mkdirs();
                out = new File(dir, "update.apk");

                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setConnectTimeout(15000);
                c.setReadTimeout(30000);
                c.setInstanceFollowRedirects(true);
                if (c.getResponseCode() >= 400) throw new Exception("http " + c.getResponseCode());
                final long total = c.getContentLengthLong();
                if (total <= 0) a.runOnUiThread(() -> bar.setIndeterminate(true));

                try (InputStream in = c.getInputStream(); FileOutputStream fo = new FileOutputStream(out)) {
                    byte[] buf = new byte[16384];
                    long done = 0;
                    int n;
                    int last = -1;
                    while ((n = in.read(buf)) > 0) {
                        fo.write(buf, 0, n);
                        done += n;
                        if (total > 0) {
                            final int p = (int) (done * 100 / total);
                            if (p != last) {
                                last = p;
                                a.runOnUiThread(() -> {
                                    bar.setProgress(p);
                                    msg.setText("Updating... " + p + "%");
                                });
                            }
                        }
                    }
                }
            } catch (Exception e) {
                err = "Update failed. Check your internet and try again.";
            }

            final String fErr = err;
            final File fOut = out;
            a.runOnUiThread(() -> {
                busy = false;
                if (fErr != null) {
                    msg.setText(fErr);
                    dlg.setCancelable(true);
                    return;
                }
                bar.setIndeterminate(false);
                bar.setProgress(100);
                msg.setText("Done");
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        Uri uri = FileProvider.getUriForFile(a, a.getPackageName() + ".fileprovider", fOut);
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setDataAndType(uri, "application/vnd.android.package-archive");
                        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        a.startActivity(i);
                    } catch (Exception e) {
                        msg.setText("Update failed, try again.");
                        dlg.setCancelable(true);
                        return;
                    }
                    dlg.dismiss();
                }, 800);
            });
        }).start();
    }
}
