package com.studio.artaban.anaglyph3d.helpers;

import android.util.Log;

import com.studio.artaban.anaglyph3d.BuildConfig;

/**
 * Created by pascal on 20/03/16.
 * Log helper
 */
public final class Logs {

    public enum Type { V, I, D, W, E, F } // F == Wtf (What a Terrible Failure)
    private static final String ANAGLYPH_3D_TAG = "Anaglyph3D";

    public static void add(Type type, String message) {

        if ((!BuildConfig.DEBUG) && (type == Type.V || type == Type.I || type == Type.D))
            return; // Do not add low level log in DEBUG mode

        final String caller = "[" + (new Exception().getStackTrace()[1].getClassName()) + "]" +
                              "{" + (new Exception().getStackTrace()[1].getMethodName()) + "} ";
        final String msg = (message != null)? message:"";
        switch (type) {
            case V: Log.v(ANAGLYPH_3D_TAG, caller + msg); break;   // VERBOSE
            case I: Log.i(ANAGLYPH_3D_TAG, caller + msg); break;   // INFO
            case D: Log.d(ANAGLYPH_3D_TAG, caller + msg); break;   // DEBUG
            case W: Log.w(ANAGLYPH_3D_TAG, caller + msg); break;   // WARNING
            case E: Log.e(ANAGLYPH_3D_TAG, caller + msg); break;   // ERROR
            case F: Log.wtf(ANAGLYPH_3D_TAG, caller + msg); break; // FATAL
        }
    }
}
