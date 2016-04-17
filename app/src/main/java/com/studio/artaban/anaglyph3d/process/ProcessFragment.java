package com.studio.artaban.anaglyph3d.process;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.studio.artaban.anaglyph3d.R;

/**
 * Created by pascal on 12/04/16.
 * Process fragment to...
 */
public class ProcessFragment extends Fragment {

    public static final String TAG = "process";

    public static final String PICTURE_SIZE_WIDTH = "width";
    public static final String PICTURE_SIZE_HEIGHT = "height";
    public static final String PICTURE_RAW_BUFFER = "raw";

    //////
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Enable sound that is played when recording
        ((AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE)).
                setStreamMute(AudioManager.STREAM_SYSTEM, true);




        //getArguments()





        return inflater.inflate(R.layout.fragment_process, container, false);
    }
}
