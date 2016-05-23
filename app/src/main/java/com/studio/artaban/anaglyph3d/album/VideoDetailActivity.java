package com.studio.artaban.anaglyph3d.album;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
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

    public static final String DATA_VIDEO_TITLE = "title";
    public static final String DATA_VIDEO_DESCRIPTION = "description";

    //
    private boolean mDetailSaved = false; // Flag to know if video details have changed

    private String mDetailTitle;
    private String mDetailDescription;
    // Editable video detail info

    //////
    @Override
    public void onSave(int videoPosition, String title, String description) {

        mDetailTitle = title;
        mDetailDescription = description;

        mDetailSaved = true;
        DisplayMessage.getInstance().toast(R.string.info_saved, Toast.LENGTH_SHORT);
    }
    @Override public boolean isVideoCreation() { return (mNewVideoAdded && !mNewVideoSaved); }
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

        // Check if video details have been updated and must be updated into the database as well
        if (mDetailSaved) {

            Intent intent = new Intent();
            intent.putExtra(DATA_VIDEO_TITLE, mDetailTitle);
            intent.putExtra(DATA_VIDEO_DESCRIPTION, mDetailDescription);

            setResult(Constants.RESULT_SAVE_VIDEO, intent);
        }
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

        // Create the detail fragment and add it to the activity (if needed)
        if (getSupportFragmentManager().findFragmentById(R.id.video_detail_container) == null)
            displayVideoDetail();

        // Initialize detail UI
        initializeDetailUI();
    }
}
