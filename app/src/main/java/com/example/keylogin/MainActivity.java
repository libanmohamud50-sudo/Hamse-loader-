package com.example.keylogin;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private LinearLayout layoutGl, layoutKr, layoutTw, layoutVn, layoutBgmi;
    private TextView txtSelectedBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Shizuku Connect Button
        Button btnConnectShizuku = findViewById(R.id.btn_connect_shizuku);
        if (btnConnectShizuku != null) {
            btnConnectShizuku.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(MainActivity.this, "Shizuku Engine Connected", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Game Version Layouts
        layoutGl = findViewById(R.id.layout_game_gl);
        layoutKr = findViewById(R.id.layout_game_kr);
        layoutTw = findViewById(R.id.layout_game_tw);
        layoutVn = findViewById(R.id.layout_game_vn);
        layoutBgmi = findViewById(R.id.layout_game_bgmi);
        txtSelectedBadge = findViewById(R.id.txt_selected_badge);

        // Click listeners for game versions selection
        if (layoutGl != null) {
            layoutGl.setOnClickListener(v -> selectGame("GL", layoutGl));
        }
        if (layoutKr != null) {
            layoutKr.setOnClickListener(v -> selectGame("KR", layoutKr));
        }
        if (layoutTw != null) {
            layoutTw.setOnClickListener(v -> selectGame("TW", layoutTw));
        }
        if (layoutVn != null) {
            layoutVn.setOnClickListener(v -> selectGame("VN", layoutVn));
        }
        if (layoutBgmi != null) {
            layoutBgmi.setOnClickListener(v -> selectGame("BGMI", layoutBgmi));
        }
    }

    private void selectGame(String gameName, LinearLayout selectedLayout) {
        // Reset backgrounds of all game cards
        if (layoutGl != null) layoutGl.setBackgroundColor(Color.parseColor("#1C1C28"));
        if (layoutKr != null) layoutKr.setBackgroundColor(Color.parseColor("#1C1C28"));
        if (layoutTw != null) layoutTw.setBackgroundColor(Color.parseColor("#1C1C28"));
        if (layoutVn != null) layoutVn.setBackgroundColor(Color.parseColor("#1C1C28"));
        if (layoutBgmi != null) layoutBgmi.setBackgroundColor(Color.parseColor("#1C1C28"));

        // Highlight selected card
        selectedLayout.setBackgroundColor(Color.parseColor("#1A3A4A"));

        // Update badge text
        if (txtSelectedBadge != null) {
            txtSelectedBadge.setText(gameName);
        }

        Toast.makeText(this, "Selected game: " + gameName, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
