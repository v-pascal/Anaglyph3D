package com.studio.artaban.anaglyph3d;

import android.animation.AnimatorInflater;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
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
public class ConnectActivity extends AppCompatActivity {

    private ProgressBar mProgressBar; // Download or connection progress bar
    private ImageView mLeftDevice; // Left device image
    private ImageView mRightDevice; // Right device image
    private ImageView mImageInfo; // Download or no bluetooth available image
    private TextView mTextInfo; // Text info to display

    private Menu mMenuOptions; // Activity menu

    //
    private void setDeviceAnimation(boolean right) {

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
    private void displayError() { // Display no bluetooth UI components

        mProgressBar.setVisibility(View.GONE);
        mLeftDevice.setVisibility(View.GONE);
        mRightDevice.setVisibility(View.GONE);

        mTextInfo.setText(R.string.no_bluetooth);

        mImageInfo.setImageDrawable(getResources().getDrawable(R.drawable.warning));
        mImageInfo.setVisibility(View.VISIBLE);
    }

    ////// Download videos
    private boolean mBluetoothAvailable; // Bluetooth available flag
    private boolean mDownloading; // Downloading videos flag

    private void displayDownload(boolean enable) { // Enable/Disable download videos UI components
        if (enable) { // Enable download components

            if (mBluetoothAvailable) {

                mLeftDevice.clearAnimation();
                mLeftDevice.setVisibility(View.GONE);
                mRightDevice.clearAnimation();
                mRightDevice.setVisibility(View.GONE);
            }
            mImageInfo.setVisibility(View.VISIBLE);
            mImageInfo.setImageDrawable(getResources().getDrawable(R.drawable.clap_anim));
            ((AnimationDrawable)mImageInfo.getDrawable()).start();

            mTextInfo.setText(getString(R.string.downloading_videos));

            ((RelativeLayout.LayoutParams)mProgressBar.getLayoutParams()).height =
                    getResources().getDimensionPixelSize(R.dimen.progress_height);
            ((RelativeLayout.LayoutParams)mProgressBar.getLayoutParams()).width =
                    ViewGroup.LayoutParams.MATCH_PARENT;
            mProgressBar.setMax(1);
            mProgressBar.setProgress(0);
            mProgressBar.setIndeterminate(false);
            mProgressBar.setVisibility(View.VISIBLE);
        }
        else { // Disable download components

            if (!mBluetoothAvailable)
                displayError();

            else {

                mLeftDevice.setVisibility(View.VISIBLE);
                mRightDevice.setVisibility(View.VISIBLE);
                mImageInfo.setVisibility(View.GONE);

                setDeviceAnimation(!Connectivity.getInstance().mListenDevice);

                mTextInfo.setText(getString(R.string.find_camera));

                ((RelativeLayout.LayoutParams)mProgressBar.getLayoutParams()).height =
                        ViewGroup.LayoutParams.WRAP_CONTENT;
                ((RelativeLayout.LayoutParams)mProgressBar.getLayoutParams()).width =
                        ViewGroup.LayoutParams.WRAP_CONTENT;
                mProgressBar.setIndeterminate(true);
            }
        }
        assert mMenuOptions.getItem(0).getItemId() == R.id.menu_album;
        mMenuOptions.getItem(0).setEnabled(!enable);
        assert mMenuOptions.getItem(1).getItemId() == R.id.menu_download;
        mMenuOptions.getItem(1).setEnabled(!enable);
    }

    private DownloadVideosTask mDownloadTask;
    private class DownloadVideosTask extends AsyncTask<Void, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {











            return Boolean.TRUE;
        }

        @Override
        protected void onPreExecute() {

            mDownloading = true;
            displayDownload(true);








        }

        @Override
        protected void onPostExecute(Boolean result) {

            mDownloading = false;
            displayDownload(false);
            if (!result) {








            }
            else // Display videos album immediately
                ActivityWrapper.startActivity(VideoListActivity.class, null, 0);
        }
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

        mProgressBar = (ProgressBar)findViewById(R.id.progress_bar);
        mTextInfo = (TextView)findViewById(R.id.text_info);
        mLeftDevice = (ImageView)findViewById(R.id.left_device);
        mRightDevice = (ImageView)findViewById(R.id.right_device);
        mImageInfo = (ImageView)findViewById(R.id.image_info);

        // Start connectivity
        mBluetoothAvailable = Connectivity.getInstance().start(this);
        if (!mBluetoothAvailable) {

            // Failed to enable Bluetooth connectivity
            displayError();
            return;
        }

        // Searching right device (default)
        setDeviceAnimation(true);
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
    public void onBackPressed() {
        if (mDownloading) {

            moveTaskToBack(true); // Put application into background (paused)
            return;
        }
        // Finish application
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.activity_connect, menu);
        mMenuOptions = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_album: {

                // Stop connectivity (do not attempt to connect when video album is displayed)
                Connectivity.getInstance().stop();

                // Display album activity
                Intent intent = new Intent(this, VideoListActivity.class);
                startActivityForResult(intent, 0);
                return true;
            }
            case R.id.menu_download: {

                // Stop connectivity (do not attempt to connect when video album is displayed)
                Connectivity.getInstance().stop();

                mDownloadTask = new DownloadVideosTask();
                mDownloadTask.execute();
                return true;
            }
            case R.id.menu_quit: {

                if (mDownloading) {

                    // Confirm by user to cancel download
                    DisplayMessage.getInstance().alert(R.string.title_warning,
                            R.string.confirm_cancel_download, null, true,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            });
                }
                else
                    finish();

                return true;
            }
        }
        return super.onOptionsItemSelected(item);
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
        if (mDownloading)
            mDownloadTask.cancel(true);
        Storage.removeTempFiles();

        // Free GStreamer library dependencies (if any)
        System.exit(0);
    }
}
