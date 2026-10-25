package com.example.keylogin;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(48, 48, 48, 48);
        root.setBackgroundColor(Color.parseColor("#0A0A0F"));

        TextView t = new TextView(this);
        t.setText("Login Successful");
        t.setTextColor(Color.parseColor("#00DDF8"));
        t.setTextSize(26);
        t.setGravity(Gravity.CENTER);
        root.addView(t);

        String exp = getIntent().getStringExtra("expire");
        if (exp != null && !exp.isEmpty()) {
            TextView e = new TextView(this);
            e.setText("Expires: " + exp);
            e.setTextColor(Color.parseColor("#9AA0AE"));
            e.setTextSize(16);
            e.setGravity(Gravity.CENTER);
            e.setPadding(0, 24, 0, 48);
            root.addView(e);
        }

        Button out = new Button(this);
        out.setText("LOG OUT");
        out.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
        root.addView(out);

        setContentView(root);
    }
}
