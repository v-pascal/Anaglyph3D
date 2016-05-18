package com.studio.artaban.anaglyph3d.album;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.helpers.Logs;

/**
 * Created by pascal on 16/05/16.
 * Parent album activity (details selection)
 */
public abstract class AlbumActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String DATA_VIDEO_POSITION = "position";
    public static final String DATA_NEW_VIDEO_ADDED = "added";
    public static final String DATA_NEW_VIDEO_SAVED = "saved";

    protected int mVideoSelected = Constants.NO_DATA; // Selected video position (or video to select)
    protected boolean mNewVideoAdded = false; // Flag to know if the new video has been added into the DB
    protected boolean mNewVideoSaved = false; // Flag to know if the new video has been saved or deleted (geolocation)

    //
    protected void restoreVideosAlbum(Bundle state) { // Restore album (manage video selection)
        if (state != null) {

            mVideoSelected = state.getInt(DATA_VIDEO_POSITION);
            mNewVideoAdded = state.getBoolean(DATA_NEW_VIDEO_ADDED);
            mNewVideoSaved = state.getBoolean(DATA_NEW_VIDEO_SAVED);
        }
        else
        if ((getIntent().getExtras() != null) && (getIntent().getExtras().containsKey(DATA_VIDEO_POSITION)))
            mVideoSelected = getIntent().getIntExtra(DATA_VIDEO_POSITION, 0);
            // Needed with detail activity child
    }
    protected void setOnDetailListener() { // Add click events listener for detail commands






        final Button testage = (Button) findViewById(R.id.button_testage);
        testage.setOnClickListener(this);






    }

    //////
    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putInt(DATA_VIDEO_POSITION, mVideoSelected);
        outState.putBoolean(DATA_NEW_VIDEO_ADDED, mNewVideoAdded);
        outState.putBoolean(DATA_NEW_VIDEO_SAVED, mNewVideoSaved);
        super.onSaveInstanceState(outState);
    }

    //////
    @Override
    public void onClick(View v) { // Manage change detail command events






        Logs.add(Logs.Type.E, "testage");






    }
}
