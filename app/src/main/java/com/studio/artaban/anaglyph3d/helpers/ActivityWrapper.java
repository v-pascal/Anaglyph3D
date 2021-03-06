package com.studio.artaban.anaglyph3d.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.studio.artaban.anaglyph3d.MainActivity;
import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.process.ProcessActivity;
import com.studio.artaban.anaglyph3d.transfer.IConnectRequest;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;

/**
 * Created by pascal on 01/04/16.
 * Static class containing current activity
 * and that manages activity status & command connectivity request
 */
public class ActivityWrapper implements IConnectRequest {

    private static ActivityWrapper ourInstance = new ActivityWrapper();
    public static ActivityWrapper getInstance() { return ourInstance; }
    private ActivityWrapper() { }

    // Request types
    public static final byte REQ_TYPE_READY = 1; // Application is ready to start recording
    public static final byte REQ_TYPE_START = 2; // Start recording command (start processing)
    public static final byte REQ_TYPE_CANCEL = 3; // Cancel recording command
    public static final byte REQ_TYPE_DOWNCOUNT = 4; // Update down count command

    private boolean mCancelled; // Recording cancel flag (from remote device)

    private String replyRequest(byte type) {

        Logs.add(Logs.Type.V, "type: " + type);
        try {

            if (type == REQ_TYPE_START) {

                mCancelled = false;
                ((ProcessActivity) get()).startRecording();
            }
            else // REQ_TYPE_DOWNCOUNT
                ((ProcessActivity)get()).updateRecording();

            return Constants.CONN_REQUEST_ANSWER_TRUE;
        }
        catch (NullPointerException e) {
            Logs.add(Logs.Type.F, "Wrong activity reference");
        }
        catch (ClassCastException e) {
            Logs.add(Logs.Type.F, "Unexpected activity reference");
        }

        return (mCancelled)? Constants.CONN_REQUEST_ANSWER_TRUE:Constants.CONN_REQUEST_ANSWER_FALSE;
        // NB: Needed when cancel command has been received with a down count pending request (this
        //     avoid to close connection when occurred)
    }

    //
    @Override public char getRequestId() { return IConnectRequest.REQ_ACTIVITY; }
    @Override public boolean getRequestMerge() { return false; }
    @Override public BufferType getRequestBuffer(byte type) { return BufferType.NONE; }

    @Override public short getMaxWaitReply(byte type) { return Constants.CONN_MAXWAIT_DEFAULT; }

    @Override
    public String getRequest(byte type, Bundle data) { return Constants.CONN_REQUEST_TYPE_ASK; }
    @Override
    public String getReply(byte type, String request, PreviousMaster previous) {

        Logs.add(Logs.Type.V, "type: " + type + ", request: " + request + ", previous: " + previous);
        switch (type) {

            case REQ_TYPE_READY: {
                try {

                    Activity curActivity = get();
                    if (curActivity.getClass().equals(MainActivity.class)) {
                        if (((MainActivity)curActivity).isReady()) {
                            Logs.add(Logs.Type.I, "Is ready");

                            ////// Start process activity
                            startActivity(ProcessActivity.class, null, 0);

                            return Constants.CONN_REQUEST_ANSWER_TRUE;
                        }

                        // In case where received request after having sent it
                        // -> Process activity not opened yet but main activity no more ready
                        if (((MainActivity)curActivity).isReadySent())
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
            case REQ_TYPE_START:
            case REQ_TYPE_DOWNCOUNT:
                return replyRequest(type);

            case REQ_TYPE_CANCEL: {

                // Cancel recorder (if needed)
                try { ((ProcessActivity)ActivityWrapper.get()).cancelRecorder(); }
                catch (NullPointerException e1) {
                    Logs.add(Logs.Type.F, "Wrong activity reference");
                }
                catch (ClassCastException e1) {
                    Logs.add(Logs.Type.F, "Wrong activity class");
                }

                // Close process activity
                stopActivity(ProcessActivity.class, Constants.NO_DATA, null);
                mCancelled = true;

                break; // Reply not considered (see below)
            }
        }
        return Constants.CONN_REQUEST_ANSWER_FALSE;
    }

    @Override
    public ReceiveResult receiveReply(byte type, String reply) {

        Logs.add(Logs.Type.V, "type: " + type + ", reply: " + reply);
        switch (type) {

            case REQ_TYPE_READY: {

                if (reply.equals(Constants.CONN_REQUEST_ANSWER_TRUE)) {
                    try {
                        // Start process activity (if not already started)
                        if (!get().getClass().equals(ProcessActivity.class))
                            startActivity(ProcessActivity.class, null, 0);
                    }
                    catch (NullPointerException e) {
                        Logs.add(Logs.Type.F, "Wrong activity reference");
                        return ReceiveResult.ERROR;
                    }
                }
                else
                    DisplayMessage.getInstance().toast(R.string.device_not_ready, Toast.LENGTH_LONG);

                return ReceiveResult.SUCCESS;
            }
            case REQ_TYPE_START:
            case REQ_TYPE_DOWNCOUNT:
                return (replyRequest(type).equals(Constants.CONN_REQUEST_ANSWER_TRUE))?
                        ReceiveResult.SUCCESS:ReceiveResult.ERROR;

            case REQ_TYPE_CANCEL:
                return ReceiveResult.SUCCESS; // Nothing to do
        }
        Logs.add(Logs.Type.F, "Unexpected activity reply received");
        return ReceiveResult.ERROR;
    }
    @Override
    public ReceiveResult receiveBuffer(ByteArrayOutputStream buffer) {
        return ReceiveResult.ERROR; // Unexpected call
    }

    //////
    private static WeakReference<Activity> mCurActivity; // Activity reference

    //
    public static void set(Activity activity) { mCurActivity = new WeakReference<Activity>(activity); }
    public static Activity get() throws NullPointerException { return mCurActivity.get(); }

    public static void startActivity(final Class activity, final Bundle data, final int request) {
        // Start activity from current activity using parameters

        Logs.add(Logs.Type.V, "activity: " + activity + ", data: " + data + ", request: " + request);
        try {

            Activity curActivity = get();
            Intent intent = new Intent(curActivity, activity);
            if (data != null)
                intent.putExtra(Constants.DATA_ACTIVITY, data);

            curActivity.startActivityForResult(intent, request);
        }
        catch (NullPointerException e) {
            Logs.add(Logs.Type.F, "Wrong activity reference");
        }
    }
    public static boolean stopActivity(final Class activity, final int result, final Bundle data) {
        // Stop expected activity (if active)

        Logs.add(Logs.Type.V, "activity: " + activity + ", result: " + result + ", data: " + data);
        try {

            Activity curActivity = get();
            if (curActivity.getClass().equals(activity)) {

                if (result != Constants.NO_DATA) {
                    if (data != null) {

                        Intent intent = new Intent();
                        intent.putExtra(Constants.DATA_ACTIVITY, data);
                        curActivity.setResult(result, intent);
                    }
                    else
                        curActivity.setResult(result);
                }
                curActivity.finish();
                return true;
            }
        }
        catch (NullPointerException e) {
            Logs.add(Logs.Type.F, "Wrong activity reference");
        }
        return false;
    }

    //
    public static void hideSoftKeyboard() {
        // Hide soft keyboard if displayed (according activity)

        Logs.add(Logs.Type.V, null);
        try {

            InputMethodManager inputManager = (InputMethodManager)get().
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            View curView = get().getCurrentFocus();
            if (curView != null)
                inputManager.hideSoftInputFromWindow(curView.getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
        }
        catch (NullPointerException e) {
            Logs.add(Logs.Type.W, "Wrong activity reference");
        }
    }
}
