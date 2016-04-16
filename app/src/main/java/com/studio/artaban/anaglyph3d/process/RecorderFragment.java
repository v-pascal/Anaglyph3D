package com.studio.artaban.anaglyph3d.process;

import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.camera.CameraView;

/**
 * Created by pascal on 12/04/16.
 * Recorder fragment to save video
 */
public class RecorderFragment extends Fragment {

    public static final String TAG = "recorder";

    //
    private ImageView mImageCounter;
    private CameraView mCameraView;

    private Animation.AnimationListener mAnimListener = new Animation.AnimationListener() {

        private short mCounter = 0;

        @Override public void onAnimationStart(Animation animation) { }
        @Override public void onAnimationEnd(Animation animation) {

            mImageCounter.setVisibility(View.GONE);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mCameraView.startRecording();
                }
            });
        }
        @Override public void onAnimationRepeat(Animation animation) {
            switch (mCounter++) {
                case 0:
                    mImageCounter.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.counter_3));
                    break;
                case 1:
                    mImageCounter.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.counter_2));
                    break;
                case 2:
                    mImageCounter.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.counter_1));
                    break;
            }
        }
    };

    //////
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_recorder, container, false);

        // Start down count animation
        mImageCounter = (ImageView)rootView.findViewById(R.id.counter_image);
        if (mImageCounter != null) {

            final AlphaAnimation anim = new AlphaAnimation(1.0f, 0f);
            anim.setDuration(1000);
            anim.setRepeatCount(3);
            anim.setAnimationListener(mAnimListener);

            mImageCounter.startAnimation(anim);
        }

        // Create & Position preview camera surface
        mCameraView = new CameraView(getContext(), null);

        Point screenSize = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(screenSize);








        // Portrait

        int width = screenSize.x;
        int height = (int)((float)(width * mCameraView.getPreviewResolution().width) /
                mCameraView.getPreviewResolution().height);
        if (height > screenSize.y) {

            height = screenSize.y;
            width = (int)((float)(screenSize.y * mCameraView.getPreviewResolution().height) /
                    mCameraView.getPreviewResolution().width);
        }








        LayoutParams params = new LayoutParams(width, height);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        ((RelativeLayout) rootView).addView(mCameraView, 0, params);

        mImageCounter.requestLayout();
        return rootView;
    }
}
