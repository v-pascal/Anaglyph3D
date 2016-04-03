package com.studio.artaban.anaglyph3d.helpers;

import android.app.Activity;
import android.content.Intent;

import com.studio.artaban.anaglyph3d.MainActivity;

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

    public static void startMainActivity() {

        try {
            get().runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    try {
                        Intent intent = new Intent(get(), MainActivity.class);
                        get().startActivityForResult(intent, 0);
                    }
                    catch (NullPointerException e) {
                        Logs.add(Logs.Type.F, "Wrong activity reference");
                    }
                }
            });
        }
        catch (NullPointerException e) {
            Logs.add(Logs.Type.F, "Wrong activity reference");
        }
    }
}
