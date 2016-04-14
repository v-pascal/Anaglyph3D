package com.studio.artaban.anaglyph3d.process;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;

import com.studio.artaban.anaglyph3d.R;

/**
 * Created by pascal on 12/04/16.
 * Recorder fragment to save video
 */
public class RecorderFragment extends Fragment {

    public static final String TAG = "recorder";

    //
    private ImageView mImageCounter;
    private Animation.AnimationListener mAnimListener = new Animation.AnimationListener() {

        private short mCounter = 0;

        @Override public void onAnimationStart(Animation animation) {
            mImageCounter.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.counter_3));
        }
        @Override public void onAnimationEnd(Animation animation) {

            mImageCounter.setVisibility(View.GONE);









        }
        @Override public void onAnimationRepeat(Animation animation) {
            switch (mCounter++) {
                case 0:
                    mImageCounter.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.counter_2));
                    break;
                case 1:
                    mImageCounter.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.counter_1));
                    break;
            }
        }
    };

    //////
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_recorder, container, false);
        mImageCounter = (ImageView)rootView.findViewById(R.id.counter_image);
        if (mImageCounter != null) {

            final AlphaAnimation anim = new AlphaAnimation(1.0f, 0f);
            anim.setDuration(1000);
            anim.setRepeatCount(2);
            anim.setAnimationListener(mAnimListener);

            mImageCounter.startAnimation(anim);
        }
        return rootView;
    }
}
