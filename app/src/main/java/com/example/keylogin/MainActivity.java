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
        root.setBackgroundColor(Color.parseColor("#0A0A0F"));

        TextView t = new TextView(this);
        t.setText("Welcome! Login successful.");
        t.setTextColor(Color.WHITE);
        t.setTextSize(22);
        root.addView(t);

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
