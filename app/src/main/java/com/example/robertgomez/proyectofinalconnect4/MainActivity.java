package com.example.robertgomez.proyectofinalconnect4;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView logoConnect4ImageView = findViewById(R.id.logoConnect4ImageView);
        Button playWithFriendButton = findViewById(R.id.playWithFriendButton);
        Button settingsButton = findViewById(R.id.settingsButton);

        // Using Glide to show a GIF
        Glide.with(this)
                .load(R.drawable.logo_connect4)
                .into(logoConnect4ImageView);

        playWithFriendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GameActivity.class);
                startActivity(intent);
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
    }

}
