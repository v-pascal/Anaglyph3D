package com.studio.artaban.anaglyph3d.album;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.widget.Toast;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;

/**
 * Created by pascal on 16/05/16.
 * Display video detail (only)
 */
public class VideoDetailActivity extends AlbumActivity implements AlbumActivity.OnVideoAlbumListener {

    private boolean mDetailSaved = false; // Flag to know if video details have changed
    private boolean mNewVideoLocated = false; // Flag to know if new video has been located

    //////
    @Override
    public void onSave(int videoPosition) {

        // Check if geolocation is available for a none located new video
        if ((isVideoCreation()) && (!mNewVideoLocated)) {

            Location videoLocation = getGeolocation();
            if (videoLocation != null) {

                mNewVideoLocated = true;

                // Locate new video
                VideoListActivity.mVideos.get(mVideoSelected).setLocation(videoLocation.getLatitude(),
                        videoLocation.getLongitude());
                updateDetailUI(); // Enable location detail
            }
        }
        mDetailSaved = true;
        DisplayMessage.getInstance().toast(R.string.info_saved, Toast.LENGTH_SHORT);
    }
    @Override public void onStore(String title, String description) { super.onStore(title, description);}
    @Override public boolean isVideoCreation() { return super.isVideoCreation(); }
    @Override public boolean isVideoSaved() { return mDetailSaved; }
    @Override public void setEditFlag(boolean flag) { mEditFlag = flag; }
    @Override public boolean getEditFlag() { return mEditFlag; }

    //
    @Override
    public void onDelete() {

        setResult(Constants.RESULT_DELETE_VIDEO);
        finish();
    }
    @Override
    protected void onClose() {

        saveEditingInfo();

        // Check if video details have been updated and must be updated into DB as well
        if (mDetailSaved)
            setResult(Constants.RESULT_SAVE_VIDEO);

        finish();
    }

    //////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_detail);

        // Set current activity
        ActivityWrapper.set(this);

        ActionBar appBar = getSupportActionBar();
        if (appBar != null) {
            if (Build.VERSION.SDK_INT >= 21) {
                getWindow().setNavigationBarColor(Color.BLACK);
                getWindow().setStatusBarColor(Color.BLACK);
            }
            else // Default status bar color (API < 21)
                appBar.setBackgroundDrawable(getResources().getDrawable(R.color.api_16_black));

            appBar.setDisplayHomeAsUpEnabled(true);
        }

        // Restore videos album (manage video selection & info)
        restoreVideosAlbum(savedInstanceState);

        // Check if orientation has changed with a large screen (check two panels expected)
        final Point screenSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(screenSize);
        if ((getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) &&
                (screenSize.x >= Constants.LARGE_SCREEN_WIDTH)) {

            // Force to display two panels in same activity (list & details)
            Intent intent = new Intent();
            intent.putExtra(AlbumActivity.DATA_VIDEO_POSITION, mVideoSelected);
            intent.putExtra(AlbumActivity.DATA_VIDEO_DETAIL, mDetailTag);

            setResult(Constants.RESULT_SELECT_VIDEO, intent);
            finish();
            return;
        }

        // Initialize detail UI
        initializeDetailUI();

        // Create the detail fragment and add it to the activity (if needed)
        if (getSupportFragmentManager().findFragmentById(R.id.video_detail_container) == null)
            displayVideoDetail();
    }
}
