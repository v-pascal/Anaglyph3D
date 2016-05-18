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

    public static final String ARG_VIDEO_POSITION = "position";
    protected int mVideoSelected = Constants.NO_DATA; // Selected video position (or video to select)

    //
    protected void restoreVideosAlbum(Bundle state) { // Restore album (manage video selection)
        if (state != null)
            mVideoSelected = state.getInt(ARG_VIDEO_POSITION);
    }
    protected void setOnDetailListener() { // Add click events listener for detail commands






        final Button testage = (Button) findViewById(R.id.button_testage);
        testage.setOnClickListener(this);






    }

    //////
    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putInt(ARG_VIDEO_POSITION, mVideoSelected);
        super.onSaveInstanceState(outState);
    }

    //////
    @Override
    public void onClick(View v) { // Manage change detail command events






        Logs.add(Logs.Type.E, "testage");





    }
}
