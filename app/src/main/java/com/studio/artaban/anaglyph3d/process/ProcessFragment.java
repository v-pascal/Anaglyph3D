package com.studio.artaban.anaglyph3d.process;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.studio.artaban.anaglyph3d.R;

/**
 * Created by pascal on 12/04/16.
 * Process fragment to...
 */
public class ProcessFragment extends Fragment {

    public static final String TAG = "process";

    // Update progress bar, status text and step displayed
    public void updateProgress(String status, int progress, int max,
                               ProcessThread.Step step, boolean heavy) {

        switch (step) {
            case MAKE:
                mMakeChecked.setVisibility(View.VISIBLE);
            case FRAMES:
                mFramesChecked.setVisibility(View.VISIBLE);
            case VIDEO:
                mVideoChecked.setVisibility(View.VISIBLE);
            default: // VIDEO
                break;
        }
        mProgressBar.setMax(max);
        mProgressBar.setProgress(progress);
        mProgressBar.setIndeterminate(heavy);
        mProgressText.setText(status);
    }

    //
    private ImageView mVideoChecked;
    private ImageView mFramesChecked;
    private ImageView mMakeChecked;

    private ProgressBar mProgressBar;
    private TextView mProgressText;

    private ImageView mClapPortrait, mClapLandscape;
    private void displayClapImage(int orientation) { // Display clap image according orientation

        if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) { // Portrait

            mClapLandscape.setVisibility(View.GONE);
            mClapPortrait.setVisibility(View.VISIBLE);
        }
        else { // Landscape

            mClapPortrait.setVisibility(View.GONE);
            mClapLandscape.setVisibility(View.VISIBLE);
        }
    }

    //////
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Restore system sound (disabled to avoid sound when start and stop recording)
        ((AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE)).
                setStreamMute(AudioManager.STREAM_SYSTEM, false);

        final View rootView = inflater.inflate(R.layout.fragment_process, container, false);
        mVideoChecked = (ImageView)rootView.findViewById(R.id.video_checked);
        mFramesChecked = (ImageView)rootView.findViewById(R.id.frames_checked);
        mMakeChecked = (ImageView)rootView.findViewById(R.id.anaglyph_checked);

        mProgressText = (TextView)rootView.findViewById(R.id.status_text);
        mProgressBar = (ProgressBar)rootView.findViewById(R.id.status_progress);
        mProgressBar.setIndeterminateDrawable(getResources().getDrawable(R.drawable.heavy_progress));

        // Display 3D clap image animation
        mClapPortrait = (ImageView)rootView.findViewById(R.id.clap_image_top);
        mClapPortrait.post(new Runnable() {

            @Override
            public void run() {
                ((AnimationDrawable) mClapPortrait.getDrawable()).start();
            }
        });
        mClapLandscape = (ImageView)rootView.findViewById(R.id.clap_image_right);
        mClapLandscape.post(new Runnable() {

            @Override
            public void run() {
                ((AnimationDrawable) mClapLandscape.getDrawable()).start();
            }
        });
        displayClapImage(getResources().getConfiguration().orientation);













        return rootView;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        super.onConfigurationChanged(newConfig);
        displayClapImage(newConfig.orientation);
    }
}
