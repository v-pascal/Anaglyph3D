package com.studio.artaban.anaglyph3d;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.studio.artaban.anaglyph3d.album.VideoListActivity;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;
import com.studio.artaban.anaglyph3d.helpers.Internet;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.helpers.Storage;

import java.io.File;

public class ChoiceActivity extends AppCompatActivity {

    private static final String PREFERENCE_NAME = "AnaglyphPreferences";
    private static final String PREFERENCE_DATA_DOWNLOADED = "downloadedData";

    //
    private Menu mMenuOptions; // Activity menu
    private boolean mDownloading; // Downloading videos flag
    private boolean mDownloaded; // Downloaded videos flag (persistent data)

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

                mDownloaded = false; // Allow user to download videos if already done
                assert mMenuOptions.getItem(1).getItemId() == R.id.menu_download;
                mMenuOptions.getItem(1).setEnabled(true);

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

        if (mDownloaded) { // Check if videos have been already downloaded

            assert mMenuOptions.getItem(1).getItemId() == R.id.menu_download;
            mMenuOptions.getItem(1).setEnabled(false);
        }
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
                if (!Internet.isOnline(1500)) {

                    DisplayMessage.getInstance().toast(R.string.no_internet, Toast.LENGTH_LONG);
                    return true;
                }

                // Confirm by the user to download
                DisplayMessage.getInstance().alert(R.string.title_confirm, R.string.confirm_download,
                        null, true, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {









                                //mDownloadTask = new DownloadVideosTask();
                                //mDownloadTask.execute();








                            }
                        });

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
    protected void onDestroy() {
        super.onDestroy();
        Storage.removeTempFiles(false);






        //if (mDownloading)
        //    mDownloadTask.cancel(true);







        // Free GStreamer library dependencies (if any)
        System.exit(0);
    }
}
