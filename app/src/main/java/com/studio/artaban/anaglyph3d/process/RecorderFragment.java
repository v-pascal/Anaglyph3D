package com.studio.artaban.anaglyph3d.process;

import android.content.Context;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.TextView;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.camera.CameraView;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.transfer.Connectivity;

/**
 * Created by pascal on 12/04/16.
 * Recorder fragment to save video
 */
public class RecorderFragment extends Fragment {

    public static final String TAG = "recorder";
    public static final int MAX_COUNTER = 4;

    //////
    private CameraView mCameraView;

    private boolean mCancelled;
    public void cancel() {

        mCancelled = true;
        mCameraView.release();
    }

    //
    private ImageView mImageCounter;
    private int mCounter = MAX_COUNTER;

    private void downCount() { // Display down count update

        Logs.add(Logs.Type.V, null);
        if (mCancelled)
            return;

        if (mCounter == 0) { // Finished to display down count...
            Logs.add(Logs.Type.I, "Down count: 0");

            // Start recording
            mImageCounter.setVisibility(View.GONE);
            mCameraView.startRecording();

            // Display recording info
            mRecordingHandler.post(mRecordingRunnable);
            return;
        }

        // Display next down count
        mImageCounter.clearAnimation();
        switch (mCounter--) {
            case 4:
                Logs.add(Logs.Type.I, "Down count: 4");
                mImageCounter.setImageDrawable(getActivity().getResources().
                        getDrawable(R.drawable.counter_4));
                break;
            case 3:
                Logs.add(Logs.Type.I, "Down count: 3");
                mImageCounter.setImageDrawable(getActivity().getResources().
                        getDrawable(R.drawable.counter_3));
                break;
            case 2:
                Logs.add(Logs.Type.I, "Down count: 2");
                mImageCounter.setImageDrawable(getActivity().getResources().
                        getDrawable(R.drawable.counter_2));
                break;
            case 1:
                Logs.add(Logs.Type.I, "Down count: 1");
                mImageCounter.setImageDrawable(getActivity().getResources().
                        getDrawable(R.drawable.counter_1));
                break;
        }
        final AlphaAnimation anim = new AlphaAnimation(1.0f, 0f);
        anim.setDuration(1000);
        anim.setFillAfter(true);
        anim.setAnimationListener(new Animation.AnimationListener() {

            @Override public void onAnimationStart(Animation animation) { }
            @Override public void onAnimationRepeat(Animation animation) { }
            @Override
            public void onAnimationEnd(Animation animation) {

                Logs.add(Logs.Type.V, null);
                if (mCancelled)
                    return;

                if (Settings.getInstance().isMaker())
                    return; // ...only for device which is not the maker

                // Send down count request to the maker
                Connectivity.getInstance().addRequest(ActivityWrapper.getInstance(),
                        ActivityWrapper.REQ_TYPE_DOWNCOUNT, null);

                Logs.add(Logs.Type.I, "mCounter: " + mCounter);
                if (mCounter == 0)
                    mCameraView.postRecording();
            }
        });

        playBip();
        mImageCounter.startAnimation(anim);
    }
    public void updateDownCount() { // Called by both devices until counter equal zero

        Logs.add(Logs.Type.V, null);
        if (!Settings.getInstance().mSimulated) // Real 3D
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    downCount();
                }
            }, Constants.CONN_WAIT_DELAY << 1);

        else // Simulated 3D (should be done on UI thread immediately)
            downCount();
    }

    private void playBip() { // Play a bip sound during the down count

        Logs.add(Logs.Type.V, null);
        if (!Settings.getInstance().isMaker())
            return; // Only the maker will play sound (better performance)

        final MediaPlayer mediaPlayer = MediaPlayer.create(getContext(), R.raw.bip_sound);
        mediaPlayer.start();
    }

    //
    private RelativeLayout mRecordingLayout;
    private Handler mRecordingHandler = new Handler();
    private Runnable mRecordingRunnable = new Runnable() {

        private int mProgress = 0;
        private String getProgressText(int progress) {

            Logs.add(Logs.Type.V, "progress: " + progress);
            int seconds = progress % 60;
            int minutes = progress / 60;

            String progressText = "";
            if (minutes < 10)
                progressText = "0";
            progressText += String.valueOf(minutes);
            progressText += ":";
            if (seconds < 10)
                progressText += "0";
            progressText += String.valueOf(seconds);

            return progressText;
        }

        @Override
        public void run() {

            Logs.add(Logs.Type.V, null);
            final SeekBar progress = (SeekBar)mRecordingLayout.findViewById(R.id.record_progress);
            progress.setProgress(mProgress);

            final TextView done = (TextView)mRecordingLayout.findViewById(R.id.record_done);
            done.setText(getProgressText(mProgress));
            final TextView todo = (TextView)mRecordingLayout.findViewById(R.id.record_todo);
            todo.setText(getProgressText(Settings.getInstance().mDuration - mProgress));

            mRecordingLayout.setVisibility(View.VISIBLE);

            if ((mProgress++ != Settings.getInstance().mDuration) && (!mCancelled))
                mRecordingHandler.postDelayed(this, 1000);
        }
    };

    //////
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Logs.add(Logs.Type.V, null);

        final View rootView = inflater.inflate(R.layout.fragment_recorder, container, false);
        mRecordingLayout = (RelativeLayout)rootView.findViewById(R.id.recording_layout);
        mImageCounter = (ImageView)rootView.findViewById(R.id.counter_image);

        // Avoid the user to change recorder progression (which is displayed in a 'SeekBar')
        final SeekBar progress = (SeekBar)mRecordingLayout.findViewById(R.id.record_progress);
        progress.setMax(Settings.getInstance().mDuration);
        progress.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        // Create & Position preview camera surface
        mCameraView = new CameraView(getContext());
        mCameraView.release();

        Point screenSize = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(screenSize);
        int width = screenSize.x;
        int height;
        if (Settings.getInstance().mOrientation) // Portrait
            height = (int)((float)(screenSize.x * mCameraView.getPreviewResolution().width) /
                    mCameraView.getPreviewResolution().height);
        else // Landscape
            height = (int)((float)(screenSize.x * mCameraView.getPreviewResolution().height) /
                    mCameraView.getPreviewResolution().width);
        if (height > screenSize.y) {

            height = screenSize.y;
            if (Settings.getInstance().mOrientation) // Portrait
                width = (int)((float)(screenSize.y * mCameraView.getPreviewResolution().height) /
                        mCameraView.getPreviewResolution().width);
            else // Landscape
                width = (int)((float)(screenSize.y * mCameraView.getPreviewResolution().width) /
                        mCameraView.getPreviewResolution().height);
        }
        LayoutParams params = new LayoutParams(width, height);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        ((RelativeLayout) rootView).addView(mCameraView, 0, params);

        // Send start down count request (if not the maker)
        if (!Settings.getInstance().isMaker())
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    Logs.add(Logs.Type.V, null);
                    Connectivity.getInstance().addRequest(ActivityWrapper.getInstance(),
                            ActivityWrapper.REQ_TYPE_DOWNCOUNT, null);
                }
            }, 1000);

        // Disable sound that is played when recording
        ((AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE)).
                setStreamMute(AudioManager.STREAM_SYSTEM, true);

        return rootView;
    }
}
