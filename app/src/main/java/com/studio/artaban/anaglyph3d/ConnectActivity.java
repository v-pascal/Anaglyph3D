package com.studio.artaban.anaglyph3d;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.studio.artaban.anaglyph3d.album.VideoListActivity;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.transfer.Connectivity;

import java.io.File;

public class ConnectActivity extends AppCompatActivity {

    private void setDeviceAnimation(boolean right) {

        final ImageView deviceA = (ImageView)findViewById((right)? R.id.left_device:R.id.right_device);
        if (deviceA != null)
            deviceA.clearAnimation();

        final AlphaAnimation anim = new AlphaAnimation(1.0f, 0.f);
        anim.setDuration(800);
        anim.setRepeatCount(Animation.INFINITE);
        anim.setRepeatMode(Animation.REVERSE);
        final ImageView deviceB = (ImageView)findViewById((right)? R.id.right_device:R.id.left_device);
        if (deviceB != null)
            deviceB.startAnimation(anim);

        Connectivity.getInstance().mListenDevice = !right;
    }

    //
    public void onLeftDeviceClick(View sender) { setDeviceAnimation(false); }
    public void onRightDeviceClick(View sender) { setDeviceAnimation(true); }

    //////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conn);

        // Set current activity
        ActivityWrapper.set(this);

        // Get documents folder of the application
        File documents = getExternalFilesDir(null);
        if (documents != null)
            ActivityWrapper.DOCUMENTS_FOLDER = documents.getAbsolutePath();
        else
            Logs.add(Logs.Type.F, "Failed to get documents folder");

        // Add and set app bar
        final Toolbar appBar = (Toolbar)findViewById(R.id.appBar);
        setSupportActionBar(appBar);
        if (appBar != null) {
            if (Build.VERSION.SDK_INT >= 21) {
                appBar.setBackgroundColor(Color.BLACK);
                getWindow().setNavigationBarColor(Color.BLACK);
                getWindow().setStatusBarColor(Color.BLACK);
            }
            else // Default status bar color (API < 21)
                appBar.setBackgroundColor(Color.argb(255, 30, 30, 30));
        }

        // Add toolbar image menu click listener
        final ImageView albumMenu = (ImageView)findViewById(R.id.album_menu);
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

        // Start connectivity
        if (!Connectivity.getInstance().start(this)) {

            // Failed to enable Bluetooth connectivity
            final ProgressBar progressBar = (ProgressBar)findViewById(R.id.progress_bar);
            if (progressBar != null)
                progressBar.setVisibility(View.GONE);
            final TextView textView = (TextView)findViewById(R.id.text_info);
            if (textView != null)
                textView.setText(R.string.no_bluetooth);
            final ImageView devLeft = (ImageView)findViewById(R.id.left_device);
            if (devLeft != null)
                devLeft.setVisibility(View.GONE);
            final ImageView devRight = (ImageView)findViewById(R.id.right_device);
            if (devRight != null)
                devRight.setVisibility(View.GONE);
            final ImageView imgWarning = (ImageView)findViewById(R.id.image_warning);
            if (imgWarning != null)
                imgWarning.setVisibility(View.VISIBLE);
            return;
        }

        // Searching right device (default)
        setDeviceAnimation(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Connectivity.getInstance().resume(this);

        if (ActivityWrapper.isQuitAppRequested())
            finish(); // Force to quit application (see declaration)
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

        if (requestCode != 0) {
            Logs.add(Logs.Type.F, "Unexpected request code");
            return;
        }
        if (resultCode == Constants.RESULT_RESTART_CONNECTION)
            Connectivity.getInstance().start(this); // Restart connectivity

        //else if (resultCode == Constants.RESULT_QUIT_APPLICATION)
        //    finish(); // Quit application
        // BUG: Not working! See 'isQuitAppRequested' method call in 'onResume' method
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Connectivity.getInstance().destroy();
    }
}
