package com.studio.artaban.anaglyph3d;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.os.Parcelable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.studio.artaban.anaglyph3d.album.AlbumActivity;
import com.studio.artaban.anaglyph3d.album.VideoListActivity;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;
import com.studio.artaban.anaglyph3d.helpers.Internet;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.helpers.Storage;

import java.io.File;

public class ChoiceActivity extends AppCompatActivity implements DownloadFragment.OnInteractionListener {

    private static final String PREFERENCE_NAME = "AnaglyphPreferences";
    private static final String PREFERENCE_DATA_DOWNLOADED = "downloadedData";

    private static final String DATA_KEY_PROGRESS = "progress";
    private static final String DATA_KEY_PROGRESS_MAX = "progressMax";
    private static final String DATA_KEY_INFO = "info";

    private static final int REQUEST_DOWNLOAD = 1;

    //
    private Menu mMenuOptions; // Activity menu
    private ProgressBar mProgressBar; // Download progress bar
    private TextView mTextInfo; // Download text info

    private boolean mDownloaded; // Downloaded videos flag (persistent data)
    private DownloadFragment mDownloadTask; // Download fragment

    public void onReal3D(View sender) {

        Settings.getInstance().mSimulated = false;

        ////// Start connect activity
        Intent intent = new Intent(this, ConnectActivity.class);
        startActivityForResult(intent, 0);
    }
    public void onSimulated3D(View sender) {

        Settings.getInstance().mSimulated = true;

        // Initialize camera settings
        if (!Settings.getInstance().initialize())
            return; // Error: failed to initialize camera (will quit application)

        ////// Start main activity
        Intent intent = new Intent(this, MainActivity.class);
        startActivityForResult(intent, 0);
    }

    ////// Download videos
    @Override public void onPreExecute() {

        displayDownload(true);
        displayMenu();

        mProgressBar.setMax(1);
        mProgressBar.setProgress(0);
        mTextInfo.setText(getString(R.string.downloading_videos, "..."));
    }
    @Override
    public void onProgressUpdate(int progress, int totalSize, short video, short totalVideos) {

        mProgressBar.setMax(totalSize);
        mProgressBar.setProgress(mProgressBar.getProgress() + progress);
        mTextInfo.setText(getString(R.string.downloading_videos, ": " + video + "/" + totalVideos));
    }
    @Override
    public void onPostExecute(int result, Parcelable[] videos) {

        if (result != Constants.NO_DATA) { // Display error message

            displayDownload(false);
            displayMenu();
            DisplayMessage.getInstance().toast(result, Toast.LENGTH_LONG);
        }
        else {

            mDownloaded = true;
            displayMenu();

            // Display videos album to add video entries into DB
            Intent intent = new Intent(ChoiceActivity.this, VideoListActivity.class);
            intent.putExtra(AlbumActivity.DATA_VIDEOS_DOWNLOADED, videos);

            startActivityForResult(intent, REQUEST_DOWNLOAD);
        }
    }

    //
    private void displayDownload(boolean enable) { // Enable/Disable download videos UI components
        if (enable) { // Enable download components

            final LinearLayout choice = (LinearLayout)findViewById(R.id.container_choice);
            assert choice != null;
            choice.setVisibility(View.GONE);
            final LinearLayout download = (LinearLayout)findViewById(R.id.container_download);
            assert download != null;
            download.setVisibility(View.VISIBLE);
            final ImageView image = (ImageView)findViewById(R.id.image_download);
            ((AnimationDrawable)image.getDrawable()).start();
        }
        else { // Disable download components

            final ImageView image = (ImageView)findViewById(R.id.image_download);
            ((AnimationDrawable)image.getDrawable()).stop();
            final LinearLayout download = (LinearLayout)findViewById(R.id.container_download);
            assert download != null;
            download.setVisibility(View.GONE);
            final LinearLayout choice = (LinearLayout)findViewById(R.id.container_choice);
            assert choice != null;
            choice.setVisibility(View.VISIBLE);
        }
    }
    public void displayMenu() { // Enable/Disable menu item according data

        assert mMenuOptions.getItem(0).getItemId() == R.id.menu_album;
        mMenuOptions.getItem(0).setEnabled(!mDownloadTask.isDownloading());
        assert mMenuOptions.getItem(1).getItemId() == R.id.menu_download;
        mMenuOptions.getItem(1).setEnabled(!mDownloadTask.isDownloading() & !mDownloaded);
    }

    //////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choice);

        // Set current activity
        ActivityWrapper.set(this);

        // Get documents folder of the application
        File documents = getExternalFilesDir(null);
        if (documents != null) {

            ActivityWrapper.DOCUMENTS_FOLDER = documents.getAbsolutePath();
            Storage.createFolder(ActivityWrapper.DOCUMENTS_FOLDER + Storage.FOLDER_DOWNLOAD);
        }
        else {

            Logs.add(Logs.Type.F, "Failed to get documents folder");
            DisplayMessage.getInstance().toast(R.string.no_storage, Toast.LENGTH_LONG);
            finish();
            return;
        }

        // Get action bar height
        TypedValue typedValue = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true))
            ActivityWrapper.ACTION_BAR_HEIGHT = TypedValue.complexToDimensionPixelSize(typedValue.data,
                    getResources().getDisplayMetrics());
        else {

            ActivityWrapper.ACTION_BAR_HEIGHT = 0;
            Logs.add(Logs.Type.W, "'android.R.attr.actionBarSize' attribute not found");
        }

        // Update toolbar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            if (Build.VERSION.SDK_INT >= 21) {
                actionBar.setBackgroundDrawable(getResources().getDrawable(android.R.color.black));
                getWindow().setNavigationBarColor(Color.BLACK);
                getWindow().setStatusBarColor(Color.BLACK);
            }
            else // Default status bar color (API < 21)
                actionBar.setBackgroundDrawable(getResources().getDrawable(R.color.api_16_black));
        }

        // Restore persistent data (downloaded videos flag)
        SharedPreferences settings = getSharedPreferences(PREFERENCE_NAME, 0);
        mDownloaded = settings.getBoolean(PREFERENCE_DATA_DOWNLOADED, false);

        // Restore saved data (if any)
        mProgressBar = (ProgressBar)findViewById(R.id.progress_download);
        mTextInfo = (TextView)findViewById(R.id.text_download);
        if (savedInstanceState != null) {

            mProgressBar.setMax(savedInstanceState.getInt(DATA_KEY_PROGRESS_MAX));
            mProgressBar.setProgress(savedInstanceState.getInt(DATA_KEY_PROGRESS));
            mTextInfo.setText(savedInstanceState.getString(DATA_KEY_INFO));
        }

        FragmentManager manager = getSupportFragmentManager();
        mDownloadTask = (DownloadFragment)manager.findFragmentByTag(DownloadFragment.TAG);
        if (mDownloadTask == null) {

            mDownloadTask = new DownloadFragment();
            manager.beginTransaction().add(mDownloadTask, DownloadFragment.TAG).commit();
            manager.executePendingTransactions();
        }
        displayDownload(mDownloadTask.isDownloading());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        ActivityWrapper.set(this); // Set current activity

        if (requestCode == REQUEST_DOWNLOAD)
            displayDownload(false);

        else if (requestCode != 0) {
            Logs.add(Logs.Type.F, "Unexpected request code");
            return;
        }
        switch (resultCode) {
            case Constants.RESULT_NO_VIDEO: {

                mDownloaded = false; // Allow user to download videos if already done
                DisplayMessage.getInstance().toast(R.string.no_video, Toast.LENGTH_LONG);
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
        if (mDownloadTask.isDownloading()) {

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

        displayMenu();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_album: {

                // Display album activity
                Intent intent = new Intent(this, VideoListActivity.class);
                startActivityForResult(intent, 0);
                return true;
            }
            case R.id.menu_download: {

                // Check Internet connection
                if (Internet.isOnline(this)) {

                    DisplayMessage.getInstance().toast(R.string.no_internet, Toast.LENGTH_LONG);
                    return true;
                }

                // Confirm by the user to download
                DisplayMessage.getInstance().alert(R.string.title_confirm, R.string.confirm_download,
                        null, true, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == DialogInterface.BUTTON_POSITIVE)
                                    mDownloadTask.start();
                            }
                        });

                return true;
            }
            case R.id.menu_quit: {

                if (mDownloadTask.isDownloading()) {

                    // Confirm by user to cancel download
                    DisplayMessage.getInstance().alert(R.string.title_warning,
                            R.string.confirm_cancel_download, null, true,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which == DialogInterface.BUTTON_POSITIVE)
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
    protected void onStop(){
        super.onStop();

        SharedPreferences prefs = getSharedPreferences(PREFERENCE_NAME, 0);
        if (prefs != null) {

            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(PREFERENCE_DATA_DOWNLOADED, mDownloaded);
            editor.apply();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putInt(DATA_KEY_PROGRESS, mProgressBar.getProgress());
        outState.putInt(DATA_KEY_PROGRESS_MAX, mProgressBar.getMax());
        outState.putString(DATA_KEY_INFO, mTextInfo.getText().toString());

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        if (isFinishing()) {

            Storage.removeTempFiles(false);
            if (mDownloadTask.isDownloading())
                mDownloadTask.stop();

            // Free GStreamer library dependencies (if any)
            System.exit(0);
        }
    }
}
