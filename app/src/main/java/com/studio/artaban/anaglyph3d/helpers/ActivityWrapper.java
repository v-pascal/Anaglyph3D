package com.studio.artaban.anaglyph3d.helpers;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.studio.artaban.anaglyph3d.MainActivity;
import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.process.ProcessActivity;
import com.studio.artaban.anaglyph3d.transfer.ConnectRequest;

import java.lang.ref.WeakReference;

/**
 * Created by pascal on 01/04/16.
 * Static class containing current activity
 * and that manages activity status & command connectivity request
 */
public class ActivityWrapper implements ConnectRequest {

    private static ActivityWrapper ourInstance = new ActivityWrapper();
    public static ActivityWrapper getInstance() { return ourInstance; }
    private ActivityWrapper() { }

    // Request types
    public static final byte REQ_TYPE_READY = 1; // Application is ready to start recording
    public static final byte REQ_TYPE_START = 2; // Start recording command
    public static final byte REQ_TYPE_CANCEL = 3; // Cancel recording

    private String replyStartRequest() {
        try {

            ((ProcessActivity)get()).startRecording();
            return Constants.CONN_REQUEST_ANSWER_TRUE;
        }
        catch (NullPointerException e) {
            Logs.add(Logs.Type.F, "Wrong activity reference");
        }
        catch (ClassCastException e) {
            Logs.add(Logs.Type.F, "Unexpected activity reference");
        }
        return Constants.CONN_REQUEST_ANSWER_FALSE;
    }

    //
    @Override public char getRequestId() { return ConnectRequest.REQ_ACTIVITY; }
    @Override public short getMaxWaitReply(byte type) { return Constants.CONN_MAXWAIT_DEFAULT; }
    @Override public String getRequest(byte type, Bundle data) { return Constants.CONN_REQUEST_TYPE_ASK; }
    @Override
    public String getReply(byte type, String request, PreviousMaster previous) {

        switch (type) {
            case REQ_TYPE_READY: {
                try {

                    Activity curActivity = get();
                    if ((curActivity.getClass().equals(MainActivity.class)) &&
                            ((MainActivity)curActivity).isReady()) {

                        // Start process activity
                        startActivity(ProcessActivity.class, 0);

                        return Constants.CONN_REQUEST_ANSWER_TRUE;
                    }
                    else if (curActivity.getClass().equals(ProcessActivity.class))
                        return Constants.CONN_REQUEST_ANSWER_TRUE; // Already answered
                }
                catch (NullPointerException e) {
                    Logs.add(Logs.Type.F, "Wrong activity reference");
                }
                break;
            }
            case REQ_TYPE_START: return replyStartRequest();
            case REQ_TYPE_CANCEL: {

                stopActivity(ProcessActivity.class);
                break; // Reply not considered (see below)
            }
        }
        return Constants.CONN_REQUEST_ANSWER_FALSE;
    }
    @Override
    public boolean receiveReply(byte type, String reply) {

        switch (type) {
            case REQ_TYPE_READY: {

                if (reply.equals(Constants.CONN_REQUEST_ANSWER_TRUE)) {
                    try {
                        // Start process activity (if not already started)
                        if (!get().getClass().equals(ProcessActivity.class))
                            startActivity(ProcessActivity.class, 0);
                    }
                    catch (NullPointerException e) {
                        Logs.add(Logs.Type.F, "Wrong activity reference");
                        return false;
                    }
                }
                else
                    DisplayMessage.getInstance().toast(R.string.device_not_ready, Toast.LENGTH_LONG);
                return true;
            }
            case REQ_TYPE_START:
                return (replyStartRequest().equals(Constants.CONN_REQUEST_ANSWER_TRUE));

            case REQ_TYPE_CANCEL:
                return true; // Nothing to do
        }
        Logs.add(Logs.Type.F, "Unexpected activity reply received");
        return false;
    }


    //////
    private static WeakReference<Activity> mCurActivity; // Activity reference

    private static boolean mQuitApp = false; // Flag to quit application (requested by the user)
    // -> Needed to quit application in connect activity (see 'onResume' method), probably because
    //    Main activity has been launch through a weak reference ('setResult' not working)

    //////
    public static void set(Activity activity) { mCurActivity = new WeakReference<Activity>(activity); }
    public static Activity get() throws NullPointerException { return mCurActivity.get(); }

    public static void startActivity(final Class activity, final int request) {
        try {

            get().runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    try {
                        Intent intent = new Intent(get(), activity);
                        get().startActivityForResult(intent, request);
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
    public static void stopActivity(final Class activity) {
        try {

            get().runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    try {
                        Activity curActivity = get();
                        if (curActivity.getClass().equals(activity))
                            curActivity.finish();
                    }
                    catch (NullPointerException e) {
                        Logs.add(Logs.Type.F, "Wrong/Unexpected activity reference");
                    }
                }
            });
        }
        catch (NullPointerException e) {
            Logs.add(Logs.Type.F, "Wrong activity reference");
        }
    }

    public static boolean isQuitAppRequested() {

        if (mQuitApp) {
            mQuitApp = false; // Needed coz static variable
            return true;
        }
        return false;
    }
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
