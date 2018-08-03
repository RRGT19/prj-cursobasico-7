package com.example.robertgomez.proyectofinalconnect4;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView logoConnect4ImageView = findViewById(R.id.logoConnect4ImageView);
        Button playWithFriendButton = findViewById(R.id.playWithFriendButton);
        Button playWithMachineButton = findViewById(R.id.playWithMachineButton);
        Button settingsButton = findViewById(R.id.settingsButton);
        Button exitButton = findViewById(R.id.exitButton);
        TextView versionNameTextView = findViewById(R.id.versionName);

        // Using Glide to show a GIF
        Glide.with(this)
                .load(R.drawable.logo_connect4)
                .into(logoConnect4ImageView);

        playWithFriendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GameActivity.class);
                intent.putExtra("withMachine", false);
                startActivity(intent);
            }
        });

        playWithMachineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GameActivity.class);
                intent.putExtra("withMachine", true);
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

        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishAffinity();
            }
        });

        // Version Name
        PackageInfo packageInfo;
        String versionName;
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = packageInfo.versionName;
            versionNameTextView.setText("v" + versionName);
        } catch (PackageManager.NameNotFoundException e) {
            versionNameTextView.setText(getString(R.string.error_loading_version_name));
        }
    }

}
