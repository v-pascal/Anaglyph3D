package com.studio.artaban.anaglyph3d;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Parcelable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class ChoiceActivity extends AppCompatActivity {

    private static final String PREFERENCE_NAME = "AnaglyphPreferences";
    private static final String PREFERENCE_DATA_DOWNLOADED = "downloadedData";

    //
    private Menu mMenuOptions; // Activity menu
    private ProgressBar mProgressBar; // Download progress bar
    private TextView mTextInfo; // Download text info

    ////// Download videos
    private boolean mDownloading; // Downloading videos flag
    private boolean mDownloaded; // Downloaded videos flag (persistent data)

    private void displayDownload(boolean enable) { // Enable/Disable download videos UI components
        if (enable) { // Enable download components

            mTextInfo.setText(getString(R.string.downloading_videos, "..."));
            mProgressBar.setMax(1);
            mProgressBar.setProgress(0);

            //
            final LinearLayout choice = (LinearLayout)findViewById(R.id.container_choice);
            assert choice != null;
            choice.setVisibility(View.GONE);
            final LinearLayout download = (LinearLayout)findViewById(R.id.container_download);
            assert download != null;
            download.setVisibility(View.VISIBLE);
        }
        else { // Disable download components

            final LinearLayout download = (LinearLayout)findViewById(R.id.container_download);
            assert download != null;
            download.setVisibility(View.GONE);
            final LinearLayout choice = (LinearLayout)findViewById(R.id.container_choice);
            assert choice != null;
            choice.setVisibility(View.VISIBLE);
        }
        assert mMenuOptions.getItem(0).getItemId() == R.id.menu_album;
        mMenuOptions.getItem(0).setEnabled(!enable);
        assert mMenuOptions.getItem(1).getItemId() == R.id.menu_download;
        mMenuOptions.getItem(1).setEnabled(!enable);
    }

    private DownloadVideosTask mDownloadTask;
    private class DownloadVideosTask extends AsyncTask<Void, Integer, Integer> implements
            Internet.OnDownloadListener {

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
            int resultId = getResultId(Internet.downloadHttpFile(Constants.DOWNLOAD_URL,
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

            if (result != Constants.NO_DATA) // Display error message
                DisplayMessage.getInstance().toast(result, Toast.LENGTH_LONG);

            else {

                mDownloaded = true;
                assert mMenuOptions.getItem(1).getItemId() == R.id.menu_download;
                mMenuOptions.getItem(1).setEnabled(false);

                // Display videos album to add video entries into DB
                Intent intent = new Intent(ChoiceActivity.this, VideoListActivity.class);
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

        //
        mProgressBar = (ProgressBar)findViewById(R.id.progress_download);
        mTextInfo = (TextView)findViewById(R.id.text_download);
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
        if (isFinishing()) {

            Storage.removeTempFiles(false);
            if (mDownloading)
                mDownloadTask.cancel(true);

            // Free GStreamer library dependencies (if any)
            System.exit(0);
        }
    }
}
