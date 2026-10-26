package com.example.keylogin;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Updater {
    public static void checkForUpdate(final Context context, final String updateUrl) {
        if (updateUrl == null || updateUrl.isEmpty()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Update Available");
        builder.setMessage("A new version is available. Update now?");
        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                downloadAndInstall(context, updateUrl);
            }
        });
        builder.setNegativeButton("Later", null);
        builder.show();
    }

    private static void downloadAndInstall(final Context context, final String urlString) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(urlString);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.connect();

                    File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk");
                    FileOutputStream fos = new FileOutputStream(file);
                    InputStream is = connection.getInputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                    is.close();

                    final File finalFile = file;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.fromFile(finalFile), "application/vnd.android.package-archive");
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
