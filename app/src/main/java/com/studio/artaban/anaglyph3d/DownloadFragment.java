package com.studio.artaban.anaglyph3d;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.LinearLayout;
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

public class DownloadFragment extends Fragment {

    private OnInteractionListener mListener;
    public interface OnInteractionListener { ///////////////////////////////////////////////////////

        void onPreExecute();
        void onProgressUpdate(int progess);
        void onPostExecute(int result);
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    //
    private boolean mDownloading; // Downloading videos flag

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

    public void start() {

    }
    public void stop() {

    }
    public boolean isDownloading() {
        return mDownloading;
    }

    //////
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnInteractionListener)
            mListener = (OnInteractionListener)context;
        else
            throw new RuntimeException(context.toString() + " must implement 'OnInteractionListener'");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
