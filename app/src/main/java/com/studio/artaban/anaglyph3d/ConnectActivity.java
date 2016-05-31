package com.studio.artaban.anaglyph3d;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Parcelable;
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.studio.artaban.anaglyph3d.album.AlbumActivity;
import com.studio.artaban.anaglyph3d.album.VideoListActivity;
import com.studio.artaban.anaglyph3d.data.AlbumTable;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;
import com.studio.artaban.anaglyph3d.helpers.Internet;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.helpers.Storage;
import com.studio.artaban.anaglyph3d.transfer.Connectivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Created by pascal on 19/03/16.
 * Connect activity (launcher)
 */
public class ConnectActivity extends AppCompatActivity {

    private static final String PREFERENCE_NAME = "AnaglyphPreferences";
    private static final String PREFERENCE_DATA_DOWNLOADED = "downloadedData";

    //////
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
    private boolean mDownloaded; // Downloaded videos flag (persistent data)

    private void displayDownload(boolean enable) { // Enable/Disable download videos UI components
        if (enable) { // Enable download components

            if (mBluetoothAvailable) {

                mLeftDevice.clearAnimation();
                mLeftDevice.setVisibility(View.GONE);
                mRightDevice.clearAnimation();
                mRightDevice.setVisibility(View.GONE);
            }
            mImageInfo.setVisibility(View.VISIBLE);
            mImageInfo.setImageDrawable(getResources().getDrawable(R.drawable.download_anim));
            ((AnimationDrawable)mImageInfo.getDrawable()).start();

            mTextInfo.setText(getString(R.string.downloading_videos, "..."));

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
    private class DownloadVideosTask extends AsyncTask<Void, Integer, Integer> implements
            Internet.OnDownloadListener {

        private static final String WEBSERVICE_URL = "http://studio-artaban.com/Anaglyph3D/videos.php";

        private static final String JSON_VIDEOS = "videos";
        private static final String JSON_VIDEO = "video";
        private static final String JSON_THUMBNAIL = "thumbnail";
        private static final String JSON_URL = "url";
        private static final String JSON_SIZE = "size";

        //
        private boolean mPublishProgress; // Publish progress flag
        private Parcelable[] mDownloadedVideos; // Downloaded videos DB info

        private int mTotalSize; // Total files size to download (in byte)
        private short mTotalVideos; // Total videos to download
        private short mCurVideo; // Current downloading video

        private void getTotalSize(JSONArray list) throws JSONException {

            mTotalVideos = 0;
            mTotalSize = 0;
            for (short i = 0; i < list.length(); ++i) {

                // Add video file size
                JSONObject video = list.getJSONObject(i).getJSONObject(JSON_VIDEO);
                mTotalSize += video.getInt(JSON_SIZE);

                // Add thumbnail file size
                JSONObject thumbnail = list.getJSONObject(i).getJSONObject(JSON_THUMBNAIL);
                mTotalSize += thumbnail.getInt(JSON_SIZE);

                ++mTotalVideos;
            }
        }
        private int getResultId(Internet.DownloadResult result) {

            // Return error string ID or Constants.NO_DATA if succeeded
            switch (result) {

                case WRONG_URL:
                case CONNECTION_FAILED:
                    return R.string.webservice_unavailable;

                case CANCELLED:
                    return R.string.download_cancelled;
            }
            return Constants.NO_DATA; // No error
        }

        //////
        @Override
        protected Integer doInBackground(Void... params) {

            // Empty downloads folder
            Storage.removeTempFiles(true);

            // Download JSON videos attributes under a web service
            mPublishProgress = false;
            int resultId = getResultId(Internet.downloadHttpFile(WEBSERVICE_URL,
                    ActivityWrapper.DOCUMENTS_FOLDER + Storage.FOLDER_DOWNLOAD +
                            Storage.FILENAME_DOWNLOAD_JSON, this));
            if (resultId != Constants.NO_DATA)
                return resultId; // Error or cancelled

            try {

                // Extract videos attributes from JSON file...
                String attributes = Storage.readFile(new File(ActivityWrapper.DOCUMENTS_FOLDER +
                    Storage.FOLDER_DOWNLOAD + Storage.FILENAME_DOWNLOAD_JSON));
                JSONObject videos = new JSONObject(attributes);

                // {
                //     "videos": [
                //         {
                //             "video": {
                //                 "url": "http://studio-artaban.com/Anaglyph3D/YYYY-MM-DD%20HH:MM:SS.000.webm",
                //                 "size": 2000000
                //             },
                //             "thumbnail": {
                //                 "url": "http://studio-artaban.com/Anaglyph3D/YYYY-MM-DD0%20HH:MM:SS.000.jpg",
                //                 "size": 20000
                //             },
                //             "Album": {
                //                 "title": "Titre",
                //                 "description": "Description",
                //                 "date": "YYYY-MM-DD HH:MM:SS.000",
                //                 "duration": 30,
                //                 "location": true,
                //                 "latitude": 0.393489,
                //                 "longitude": -22.104523,
                //                 "thumbnailWidth": 640,
                //                 "thumbnailHeight": 480
                //             }
                //         },
                //
                //         ...
                //
                //     ]
                // }
                JSONArray videoList = videos.getJSONArray(JSON_VIDEOS);

                // Get total bytes to download
                getTotalSize(videoList);
                if ((mTotalSize == 0) || (mTotalVideos == 0))
                    return R.string.wrong_videos_attr;

                mCurVideo = 1;
                mPublishProgress = true; // Ready to update progress bar
                mDownloadedVideos = new Parcelable[mTotalVideos];

                for (short i = 0; i < videoList.length(); ++i) {

                    JSONObject album = videoList.getJSONObject(i).getJSONObject(AlbumTable.TABLE_NAME);
                    String fileName = album.getString(AlbumTable.COLUMN_DATE); // Date is used as filename

                    // Download video file
                    JSONObject video = videoList.getJSONObject(i).getJSONObject(JSON_VIDEO);
                    resultId = getResultId(Internet.downloadHttpFile(video.getString(JSON_URL),
                            ActivityWrapper.DOCUMENTS_FOLDER + Storage.FOLDER_DOWNLOAD +
                                    fileName + Constants.EXTENSION_WEBM, this));
                    if (resultId != Constants.NO_DATA)
                        return resultId; // Error or cancelled

                    // Download thumbnail file
                    JSONObject thumbnail = videoList.getJSONObject(i).getJSONObject(JSON_THUMBNAIL);
                    resultId = getResultId(Internet.downloadHttpFile(thumbnail.getString(JSON_URL),
                            ActivityWrapper.DOCUMENTS_FOLDER + Storage.FOLDER_DOWNLOAD +
                                    fileName + Constants.EXTENSION_JPEG, this));
                    if (resultId != Constants.NO_DATA)
                        return resultId; // Error or cancelled

                    // Fill videos DB info array
                    SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATABASE_DATE_FORMAT);
                    mDownloadedVideos[i] = new AlbumTable.Video(0,
                            album.getString(AlbumTable.COLUMN_TITLE), // Title
                            album.getString(AlbumTable.COLUMN_DESCRIPTION), // Description
                            dateFormat.parse(fileName), // Date
                            (short)album.getInt(AlbumTable.COLUMN_DURATION), // Duration
                            album.getBoolean(AlbumTable.COLUMN_LOCATION), // Location flag
                            album.getDouble(AlbumTable.COLUMN_LATITUDE), // Latitude
                            album.getDouble(AlbumTable.COLUMN_LONGITUDE), // Longitude
                            album.getInt(AlbumTable.COLUMN_THUMBNAIL_WIDTH), // Thumbnail width
                            album.getInt(AlbumTable.COLUMN_THUMBNAIL_HEIGHT)); // Thumbnail height

                    ++mCurVideo;
                }
            }
            catch (JSONException e) {

                Logs.add(Logs.Type.F, "JSON Exception: " + e.getMessage());
                return R.string.wrong_videos_attr;
            }
            catch (IOException e) {

                Logs.add(Logs.Type.F, "IO Exception: " + e.getMessage());
                return R.string.wrong_videos_attr;
            }
            catch (ParseException e) {

                Logs.add(Logs.Type.F, "Parse exception: " + e.getMessage());
                return R.string.wrong_videos_attr;
            }
            return Constants.NO_DATA; // Ok
        }

        @Override
        protected void onPreExecute() {
            mDownloading = true;
            displayDownload(true);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {

            mProgressBar.setMax(mTotalSize);
            mProgressBar.setProgress(mProgressBar.getProgress() + values[0]);

            mTextInfo.setText(getString(R.string.downloading_videos, ": " +
                    mCurVideo + "/" + mTotalVideos));
        }

        @Override
        protected void onPostExecute(Integer result) {
            mDownloading = false;
            displayDownload(false);

            if (result != Constants.NO_DATA) { // Display error message

                DisplayMessage.getInstance().toast(result, Toast.LENGTH_LONG);
                Connectivity.getInstance().start(ConnectActivity.this);
            }
            else {

                mDownloaded = true;
                assert mMenuOptions.getItem(1).getItemId() == R.id.menu_download;
                mMenuOptions.getItem(1).setEnabled(false);

                // Display videos album to add video entries into DB
                Intent intent = new Intent(ConnectActivity.this, VideoListActivity.class);
                intent.putExtra(AlbumActivity.DATA_VIDEOS_DOWNLOADED, mDownloadedVideos);

                startActivityForResult(intent, 0);
            }
        }

        //////
        @Override public boolean onCheckCancelled() { return isCancelled(); }
        @Override
        public void onPublishProgress(int read) {

            if (mPublishProgress)
                publishProgress(read);
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

        // Restore persistent data (downloaded videos flag)
        SharedPreferences settings = getSharedPreferences(PREFERENCE_NAME, 0);
        mDownloaded = settings.getBoolean(PREFERENCE_DATA_DOWNLOADED, false);

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

                mDownloaded = false; // Allow user to download videos if already done
                assert mMenuOptions.getItem(1).getItemId() == R.id.menu_download;
                mMenuOptions.getItem(1).setEnabled(true);

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

                // Stop connectivity (do not attempt to connect when video album is displayed)
                Connectivity.getInstance().stop();

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

                                // Stop connectivity (do not attempt to connect when video album is displayed)
                                Connectivity.getInstance().stop();

                                mDownloadTask = new DownloadVideosTask();
                                mDownloadTask.execute();
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

        Connectivity.getInstance().destroy();
        if (mDownloading)
            mDownloadTask.cancel(true);
        Storage.removeTempFiles(false);

        // Free GStreamer library dependencies (if any)
        System.exit(0);
    }
}
