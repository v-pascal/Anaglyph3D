package com.studio.artaban.anaglyph3d;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.transfer.Connectivity;

/**
 * Created by pascal on 19/03/16.
 * Connect activity (launcher)
 */
public class ConnectActivity extends AppCompatActivity {

    private void setDeviceAnimation(boolean right) {

        Logs.add(Logs.Type.V, "right: " + right);

        // Display alpha animation on device glass image (according position)
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
        Logs.add(Logs.Type.V, "savedInstanceState: " + ((savedInstanceState != null) ?
                savedInstanceState.toString() : "null"));
        setContentView(R.layout.activity_connect);

        // Set current activity
        ActivityWrapper.set(this);

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

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        // Start connectivity
        if (!Connectivity.getInstance().start(this)) {

            // Failed to enable Bluetooth connectivity
            final ProgressBar progressBar = (ProgressBar)findViewById(R.id.progress_bar);
            if (progressBar != null)
                progressBar.setVisibility(View.GONE);
            final TextView textView = (TextView) findViewById(R.id.text_info);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        Logs.add(Logs.Type.V, "requestCode: " + requestCode + ", resultCode: " + resultCode);
        ActivityWrapper.set(this); // Set current activity

        if (requestCode != 0) {
            Logs.add(Logs.Type.F, "Unexpected request code");
            return;
        }
        switch (resultCode) {
            case Constants.RESULT_QUIT_APPLICATION: {

                // Quit application
                setResult(Constants.RESULT_QUIT_APPLICATION);
                finish();
                break;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Logs.add(Logs.Type.V, "item: " + ((item != null)? item.getItemId():"null"));
        if (item.getItemId() == android.R.id.home) {

            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Logs.add(Logs.Type.V, null);
        Connectivity.getInstance().resume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Logs.add(Logs.Type.V, null);
        Connectivity.getInstance().pause(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Logs.add(Logs.Type.V, null);
        Connectivity.getInstance().destroy();
    }
}
