package com.studio.artaban.anaglyph3d.process;

import android.content.Context;
import android.content.DialogInterface;
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
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;
import com.studio.artaban.anaglyph3d.transfer.Connectivity;

/**
 * Created by pascal on 12/04/16.
 * Process fragment to...
 */
public class ProcessFragment extends Fragment {

    public static final String TAG = "process";

    // Data key
    public static final String DATA_KEY_FAILED_RECORDING = "failedRecording";

    // Update progress bar, status text and step displayed
    public void updateProgress() {
        synchronized (ProcessThread.mProgress) {

            switch (ProcessThread.mProgress.step) {
                case MAKE:
                    mMakeChecked.setVisibility(View.VISIBLE);
                case FRAMES:
                    mFramesChecked.setVisibility(View.VISIBLE);
                case VIDEO:
                    mVideoChecked.setVisibility(View.VISIBLE);
                default: // VIDEO
                    break;
            }
            mProgressBar.setMax(ProcessThread.mProgress.max);
            mProgressBar.setProgress(ProcessThread.mProgress.progress);
            mProgressBar.setIndeterminate(ProcessThread.mProgress.heavy);
            mProgressText.setText(ProcessThread.mProgress.message);
        }
    }

    //
    private ImageView mVideoChecked;
    private ImageView mFramesChecked;
    private ImageView mMakeChecked;

    private ProgressBar mProgressBar;
    private TextView mProgressText;

    private ImageView mClapPortrait, mClapLandscape;
    private void displayClapImage(int orientation) { // Display clap image according orientation

        if (orientation == Configuration.ORIENTATION_PORTRAIT) { // Portrait

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
        updateProgress();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Check if recorder has failed to start
        if ((getArguments() != null) && (getArguments().getBoolean(DATA_KEY_FAILED_RECORDING, false))) {

            // Send cancel request to remote device
            Connectivity.getInstance().addRequest(ActivityWrapper.getInstance(),
                    ActivityWrapper.REQ_TYPE_CANCEL, null);

            // Inform user on recorder failure
            DisplayMessage.getInstance().alert(R.string.title_error, R.string.error_start_recording,
                    null, true, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == DialogInterface.BUTTON_POSITIVE)
                                Settings.getInstance().mNoFps = true;

                            // Finish activity
                            getActivity().finish();
                        }
                    });
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        super.onConfigurationChanged(newConfig);
        displayClapImage(newConfig.orientation);
    }
}
