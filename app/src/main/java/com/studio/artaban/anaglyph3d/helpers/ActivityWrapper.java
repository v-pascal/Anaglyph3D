package com.studio.artaban.anaglyph3d.helpers;

import android.app.Activity;

import java.lang.ref.WeakReference;

/**
 * Created by pascal on 01/04/16.
 * Static class containing current activity
 */
public class ActivityWrapper {

    private static WeakReference<Activity> mCurActivity; // Activity reference

    //////
    public static void set(Activity activity) { mCurActivity = new WeakReference<Activity>(activity); }
    public static Activity get() throws NullPointerException { return mCurActivity.get(); }
}
