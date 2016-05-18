package com.studio.artaban.anaglyph3d.album;

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
    public void setOnDetailListener() {



        final Button testage = (Button) findViewById(R.id.button_testage);
        testage.setOnClickListener(this);



    }

    //////
    @Override
    public void onClick(View v) {




        Logs.add(Logs.Type.E, "testage");




    }
}
