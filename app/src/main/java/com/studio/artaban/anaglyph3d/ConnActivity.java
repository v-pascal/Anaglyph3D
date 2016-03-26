package com.studio.artaban.anaglyph3d;

import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;
import com.studio.artaban.anaglyph3d.transfer.Connectivity;

public class ConnActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conn);

        // Add and set app bar
        final Toolbar appBar = (Toolbar)findViewById(R.id.appBar);
        setSupportActionBar(appBar);
        if (Build.VERSION.SDK_INT >= 21) {
            appBar.setBackgroundColor(Color.BLACK);
            getWindow().setNavigationBarColor(Color.BLACK);
            getWindow().setStatusBarColor(Color.BLACK);
        }
        else
            appBar.setBackgroundColor(Color.argb(255,30,30,30)); // Default status bar color (API < 21)

        // Add toolbar album image click listener
        final ImageView albumMenu = (ImageView)findViewById(R.id.albumMenu);
        albumMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {




                // Display album activity





            }
        });

        // Start connectivity
        if (!Connectivity.getInstance().start(this)) {

            // Failed to enable Bluetooth connectivity
            final ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBar);
            progressBar.setVisibility(View.GONE);
            final TextView textView = (TextView)findViewById(R.id.textView);
            textView.setText(R.string.no_bluetooth);
            final ImageView imgDevices = (ImageView)findViewById(R.id.imgDevices);
            imgDevices.setImageDrawable(getResources().getDrawable(R.drawable.warning));
            return;
        }

        // Animate wait second devices image
        final ImageView imgDevices = (ImageView)findViewById(R.id.imgDevices);
        imgDevices.setImageDrawable(getResources().getDrawable(R.drawable.devices_anim));
        final AnimationDrawable animDevices = (AnimationDrawable)imgDevices.getDrawable();
        imgDevices.post(new Runnable() {

            public void run() {
                animDevices.start();
            }
        });

        // Set current activity to be able to display message
        DisplayMessage.getInstance().setActivity(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Connectivity.getInstance().resume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Connectivity.getInstance().pause(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Connectivity.getInstance().destroy();
    }
}
