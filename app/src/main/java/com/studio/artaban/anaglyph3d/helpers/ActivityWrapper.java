package com.studio.artaban.anaglyph3d.helpers;

import android.app.Activity;
import android.content.Intent;

import com.studio.artaban.anaglyph3d.MainActivity;
import com.studio.artaban.anaglyph3d.data.Constants;

import java.lang.ref.WeakReference;

/**
 * Created by pascal on 01/04/16.
 * Static class containing current activity
 */
public class ActivityWrapper {

    private static WeakReference<Activity> mCurActivity; // Activity reference

    private static boolean mQuitApp = false; // Flag to quit application (requested by the user)
    // -> Needed to quit application in Connect activity (see 'onResume' method), probably because
    //    Main activity has been launch through a weak reference ('setResult' not working)

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
    public static boolean isQuitAppRequested() { return mQuitApp; }
    public static void quitApplication() {

        mQuitApp = true;
        try {
            //mCurActivity.get().getIntent().setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            //mCurActivity.get().setResult(Constants.RESULT_QUIT_APPLICATION);
            // BUG: Not working! See comments in 'mQuitApp' declaration

            mCurActivity.get().finish();
        }
        catch (NullPointerException e) {
            Logs.add(Logs.Type.F, "Wrong activity reference");
        }
    }
}
