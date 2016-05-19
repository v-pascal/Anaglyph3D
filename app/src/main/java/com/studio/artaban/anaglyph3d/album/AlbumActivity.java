package com.studio.artaban.anaglyph3d.album;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.album.details.DetailEditFragment;
import com.studio.artaban.anaglyph3d.album.details.DetailLocationFragment;
import com.studio.artaban.anaglyph3d.album.details.DetailPlayerFragment;
import com.studio.artaban.anaglyph3d.data.Constants;

/**
 * Created by pascal on 16/05/16.
 * Parent album activities (details selection)
 */
public abstract class AlbumActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String DATA_VIDEO_POSITION = "position";
    public static final String DATA_NEW_VIDEO_ADDED = "added";
    public static final String DATA_NEW_VIDEO_SAVED = "saved";

    public static final String DATA_VIDEO_DETAIL = "detail";

    //////
    protected int mVideoSelected = Constants.NO_DATA; // Selected video position (or video to select)
    protected boolean mNewVideoAdded = false; // Flag to know if the new video has been added into the DB
    protected boolean mNewVideoSaved = false; // Flag to know if the new video has been saved or deleted (geolocation)

    protected String mDetailTag = DetailPlayerFragment.TAG; // Fragment detail displayed (tag)

    //
    protected void restoreVideosAlbum(Bundle state) { // Restore album (manage video selection)
        if (state != null) {

            mVideoSelected = state.getInt(DATA_VIDEO_POSITION);
            mNewVideoAdded = state.getBoolean(DATA_NEW_VIDEO_ADDED);
            mNewVideoSaved = state.getBoolean(DATA_NEW_VIDEO_SAVED);

            mDetailTag = state.getString(DATA_VIDEO_DETAIL);
        }
        else if (getIntent().getExtras() != null) {

            if (getIntent().getExtras().containsKey(DATA_VIDEO_POSITION))
                mVideoSelected = getIntent().getIntExtra(DATA_VIDEO_POSITION, 0);
            if (getIntent().getExtras().containsKey(DATA_VIDEO_DETAIL))
                mDetailTag = getIntent().getStringExtra(DATA_VIDEO_DETAIL);

            // Needed with detail activity child
        }
    }
    protected void addDetailClickListener() { // Add click events listener for detail commands

        ImageButton command = (ImageButton) findViewById(R.id.detail_player);
        assert command != null;
        command.setOnClickListener(this);
        command = (ImageButton) findViewById(R.id.detail_edit);
        assert command != null;
        command.setOnClickListener(this);
        command = (ImageButton) findViewById(R.id.detail_location);
        assert command != null;
        command.setOnClickListener(this);
        command = (ImageButton) findViewById(R.id.detail_share);
        assert command != null;
        command.setOnClickListener(this);
        command = (ImageButton) findViewById(R.id.detail_trash);
        assert command != null;
        command.setOnClickListener(this);
    }
    protected void displayVideoDetail() { // Display video detail fragment

        Bundle arguments = new Bundle();
        arguments.putInt(AlbumActivity.DATA_VIDEO_POSITION, mVideoSelected);
        Fragment fragment;

        // Display expected video detail
        if (mDetailTag.equals(DetailPlayerFragment.TAG)) // Player
            fragment = new DetailPlayerFragment();
        else if (mDetailTag.equals(DetailEditFragment.TAG)) // Edit
            fragment = new DetailEditFragment();
        else // Location
            fragment = new DetailLocationFragment();

        fragment.setArguments(arguments);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.video_detail_container, fragment)
                .commit();
    }

    //////
    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putInt(DATA_VIDEO_POSITION, mVideoSelected);
        outState.putBoolean(DATA_NEW_VIDEO_ADDED, mNewVideoAdded);
        outState.putBoolean(DATA_NEW_VIDEO_SAVED, mNewVideoSaved);

        outState.putString(DATA_VIDEO_DETAIL, mDetailTag);

        super.onSaveInstanceState(outState);
    }

    //////
    @Override
    public void onClick(View v) { // Manage change detail command events
        switch (v.getId()) {

            case R.id.detail_player:
                mDetailTag = DetailPlayerFragment.TAG;
                break;

            case R.id.detail_edit:
                mDetailTag = DetailEditFragment.TAG;
                break;

            case R.id.detail_location:
                mDetailTag = DetailLocationFragment.TAG;
                break;

            case R.id.detail_share:
            case R.id.detail_trash:











                break;
        }
        displayVideoDetail();
    }
}
