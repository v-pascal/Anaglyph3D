package com.studio.artaban.anaglyph3d.tools;

import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;

import com.studio.artaban.anaglyph3d.helpers.Logs;

/**
 * Created by pascal on 23/05/16.
 * Scale animation (goings and comings)
 * -> Usually used to inform user a click has been taken into account
 */
public class GrowthAnimation extends ScaleAnimation {

    public static final float DEFAULT_SCALE_ORIGIN = 1f;
    public static final float DEFAULT_SCALE_LARGE = 1.4f;

    public static final int DEFAULT_DURATION = 300;

    //
    public interface OnTerminateListener {
        void onAnimationTerminate();
    }

    //////
    private OnTerminateListener mTerminateListener;
    private boolean mReverseAnim;

    private GrowthAnimation(float scaleOrigin, float scaleLarge, OnTerminateListener listener) {

        super(scaleOrigin, scaleLarge, scaleOrigin, scaleLarge,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);

        Logs.add(Logs.Type.V, "scaleOrigin: " + scaleOrigin + ", scaleLarge: " +
                scaleLarge + ", listener: " + listener);

        mTerminateListener = listener;
        mReverseAnim = false;

        setDuration(DEFAULT_DURATION);
        setRepeatCount(Animation.INFINITE);
        setRepeatMode(Animation.REVERSE);
        setAnimationListener(new Animation.AnimationListener() {

            @Override public void onAnimationStart(Animation animation) { }
            @Override public void onAnimationEnd(Animation animation) { }
            @Override
            public void onAnimationRepeat(Animation animation) {
                if (mReverseAnim) {

                    animation.cancel(); // Animation terminated
                    if (mTerminateListener != null)
                        mTerminateListener.onAnimationTerminate();
                }
                else
                    mReverseAnim = true;
            }
        });
    }

    ////// Constructors factory
    public static GrowthAnimation create() {
        Logs.add(Logs.Type.V, null);
        return new GrowthAnimation(DEFAULT_SCALE_ORIGIN, DEFAULT_SCALE_LARGE, null);
    }
    public static GrowthAnimation create(OnTerminateListener listener) {
        Logs.add(Logs.Type.V, "listener: " + listener);
        return new GrowthAnimation(DEFAULT_SCALE_ORIGIN, DEFAULT_SCALE_LARGE, listener);
    }
    public static GrowthAnimation create(float scaleOrigin, float scaleLarge) {
        Logs.add(Logs.Type.V, "scaleOrigin: " + scaleOrigin + ", scaleLarge: " + scaleLarge);
        return new GrowthAnimation(scaleOrigin, scaleLarge, null);
    }
    public static GrowthAnimation create(float scaleOrigin, float scaleLarge, OnTerminateListener listener) {
        return new GrowthAnimation(scaleOrigin, scaleLarge, listener);
    }
}
