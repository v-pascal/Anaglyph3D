package com.studio.artaban.anaglyph3d;

import android.content.Intent;
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

import com.studio.artaban.anaglyph3d.album.VideoListActivity;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.transfer.Connectivity;

public class ConnectActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conn);

        // Set current activity
        ActivityWrapper.set(this);

        // Add and set app bar
        final Toolbar appBar = (Toolbar)findViewById(R.id.appBar);
        setSupportActionBar(appBar);
        if (appBar != null) {
            if (Build.VERSION.SDK_INT >= 21) {
                appBar.setBackgroundColor(Color.BLACK);
                getWindow().setNavigationBarColor(Color.BLACK);
                getWindow().setStatusBarColor(Color.BLACK);
            } else // Default status bar color (API < 21)
                appBar.setBackgroundColor(Color.argb(255, 30, 30, 30));
        }

        // Add toolbar image menu click listener
        final ImageView albumMenu = (ImageView)findViewById(R.id.albumMenu);
        if (albumMenu != null) {
            albumMenu.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    // Stop connectivity (do not attempt to connect when video album is displayed)
                    Connectivity.getInstance().stop();

                    // Display album activity
                    Intent intent = new Intent(getApplicationContext(), VideoListActivity.class);
                    intent.putExtra(Constants.DATA_CONNECTION_ESTABLISHED, false);
                    startActivityForResult(intent, 0);
                }
            });
        }
        final ImageView closeMenu = (ImageView)findViewById(R.id.closeMenu);
        if (closeMenu != null) {
            closeMenu.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    finish(); // Quit application
                }
            });
        }

        // Start connectivity
        if (!Connectivity.getInstance().start()) {

            // Failed to enable Bluetooth connectivity
            final ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBar);
            if (progressBar != null)
                progressBar.setVisibility(View.GONE);
            final TextView textView = (TextView)findViewById(R.id.textView);
            if (textView != null)
                textView.setText(R.string.no_bluetooth);
            final ImageView imgDevices = (ImageView)findViewById(R.id.imgDevices);
            if (imgDevices != null)
                imgDevices.setImageDrawable(getResources().getDrawable(R.drawable.warning));
            return;
        }

        // Animate wait second devices image
        final ImageView imgDevices = (ImageView)findViewById(R.id.imgDevices);
        if (imgDevices != null) {
            imgDevices.setImageDrawable(getResources().getDrawable(R.drawable.devices_anim));
            final AnimationDrawable animDevices = (AnimationDrawable) imgDevices.getDrawable();
            imgDevices.post(new Runnable() {

                @Override
                public void run() {
                    animDevices.start();
                }
            });
        }
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Set current activity
        ActivityWrapper.set(this);

        if ((requestCode == 0) && (resultCode == Constants.RESULT_RESTART_CONNECTION))
            Connectivity.getInstance().start(); // Restart connectivity
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true); // Put application into background (paused)
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Connectivity.getInstance().destroy();
    }
}
