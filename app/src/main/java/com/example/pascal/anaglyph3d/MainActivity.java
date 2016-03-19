package com.example.pascal.anaglyph3d;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Animate wait second devices image
        final ImageView imgDevices = (ImageView)findViewById(R.id.imgDevices);
        if (Build.VERSION.SDK_INT >= 22)
            imgDevices.setImageDrawable(getResources().getDrawable(R.drawable.devices_anim, this.getTheme()));
        else
            imgDevices.setImageDrawable(getResources().getDrawable(R.drawable.devices_anim));
        final AnimationDrawable animDevices = (AnimationDrawable)imgDevices.getDrawable();
        imgDevices.post(new Runnable() {

            public void run() {
                animDevices.start();
            }
        });

        // Add and set app bar
        final Toolbar appBar = (Toolbar) findViewById(R.id.appBar);
        setSupportActionBar(appBar);
        if (Build.VERSION.SDK_INT >= 21) {
            appBar.setBackgroundColor(Color.BLACK);
            getWindow().setNavigationBarColor(Color.BLACK);
            getWindow().setStatusBarColor(Color.BLACK);
        }
    }
}
