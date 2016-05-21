package com.studio.artaban.anaglyph3d.album;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.album.details.DetailEditFragment;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;

/**
 * Created by pascal on 16/05/16.
 * Display video detail (only)
 */
public class VideoDetailActivity extends AlbumActivity implements DetailEditFragment.OnEditVideoListener {

    public static final String DATA_VIDEO_TITLE = "title";
    public static final String DATA_VIDEO_DESCRIPTION = "description";

    //////
    @Override
    public void onSave(String title, String description) {









    }
    @Override
    public boolean onDelete() {









        return false;
    }
    @Override
    protected void onClose() {












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

        // Check if orientation has changed with a large screen (check two panel needed)
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

        // Add click events listener for detail commands
        addDetailClickListener();
    }
}
