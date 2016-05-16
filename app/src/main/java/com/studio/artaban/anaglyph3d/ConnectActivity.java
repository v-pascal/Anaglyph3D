package com.studio.artaban.anaglyph3d;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.studio.artaban.anaglyph3d.album.VideoListActivity;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.helpers.Storage;
import com.studio.artaban.anaglyph3d.transfer.Connectivity;

import java.io.File;

/**
 * Created by pascal on 19/03/16.
 * Connect activity (launcher)
 */
public class ConnectActivity extends AppCompatActivity implements View.OnClickListener {

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
        setContentView(R.layout.activity_connect);

        // Set current activity
        ActivityWrapper.set(this);

        // Get documents folder of the application
        File documents = getExternalFilesDir(null);
        if (documents != null)
            ActivityWrapper.DOCUMENTS_FOLDER = documents.getAbsolutePath();
        else
            Logs.add(Logs.Type.F, "Failed to get documents folder");

        // Get action bar height
        TypedValue typedValue = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true))
            ActivityWrapper.ACTION_BAR_HEIGHT = TypedValue.complexToDimensionPixelSize(typedValue.data,
                    getResources().getDisplayMetrics());
        else {

            ActivityWrapper.ACTION_BAR_HEIGHT = 0;
            Logs.add(Logs.Type.W, "'android.R.attr.actionBarSize' attribute not found");
        }

        // Add and set app bar
        final Toolbar appBar = (Toolbar)findViewById(R.id.appBar);
        setSupportActionBar(appBar);
        assert appBar != null;
        if (Build.VERSION.SDK_INT >= 21) {
            appBar.setBackgroundColor(Color.BLACK);
            getWindow().setNavigationBarColor(Color.BLACK);
            getWindow().setStatusBarColor(Color.BLACK);
        }
        else // Default status bar color (API < 21)
            appBar.setBackgroundColor(Color.argb(255, 30, 30, 30));

        // Add toolbar image menu click listener
        final ImageView albumMenu = (ImageView)findViewById(R.id.album_menu);
        assert albumMenu != null;
        albumMenu.setOnClickListener(this);

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
    }

    @Override
    protected void onPause() {
        super.onPause();
        Connectivity.getInstance().pause(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        ActivityWrapper.set(this); // Set current activity

        if (requestCode != 0) {
            Logs.add(Logs.Type.F, "Unexpected request code");
            return;
        }
        switch (resultCode) {
            case Constants.RESULT_NO_VIDEO: {
                DisplayMessage.getInstance().toast(R.string.no_video, Toast.LENGTH_LONG);
                //break; // Always restart connectivity thread in this case
            }
            case Constants.RESULT_RESTART_CONNECTION: {
                Connectivity.getInstance().start(this); // Restart connectivity
                break;
            }
            case Constants.RESULT_QUIT_APPLICATION: {
                finish(); // Quit application
                break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Connectivity.getInstance().destroy();
        Storage.removeTempFiles();
    }

    @Override
    public void onClick(View v) { // Album menu

        // Stop connectivity (do not attempt to connect when video album is displayed)
        Connectivity.getInstance().stop();

        // Display album activity
        Intent intent = new Intent(this, VideoListActivity.class);
        startActivityForResult(intent, 0);
    }
}
