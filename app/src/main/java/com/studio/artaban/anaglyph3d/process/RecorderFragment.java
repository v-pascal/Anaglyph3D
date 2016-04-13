package com.studio.artaban.anaglyph3d.process;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.studio.artaban.anaglyph3d.R;

/**
 * Created by pascal on 12/04/16.
 * Recorder fragment to save video
 */
public class RecorderFragment extends Fragment {

    public static final String TAG = "recorder";

    //////
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recorder, container, false);
    }
}
