package com.studio.artaban.anaglyph3d.data;

import android.app.Activity;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.support.design.widget.Snackbar;

import com.studio.artaban.anaglyph3d.MainActivity;
import com.studio.artaban.anaglyph3d.SettingsActivity;
import com.studio.artaban.anaglyph3d.camera.CameraView;
import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.transfer.ConnectRequest;
import com.studio.artaban.anaglyph3d.transfer.Connectivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

/**
 * Created by pascal on 23/03/16.
 * Application settings
 */
public class Settings implements ConnectRequest {

    private static Settings ourInstance = new Settings();
    public static Settings getInstance() { return ourInstance; }
    private Settings() { }

    // Data keys
    public static final String DATA_KEY_REMOTE_DEVICE = "remote";
    public static final String DATA_KEY_POSITION = "position";

    private static final String DATA_KEY_PERFORMANCE = "performance";
    // ...to know which device will have to make the video

    private static final String DATA_KEY_RESOLUTIONS = "resolutions";
    private static final String DATA_KEY_WIDTH = "width";
    private static final String DATA_KEY_HEIGHT = "height";
    public static final String DATA_KEY_RESOLUTION = "resolution";
    public static final String DATA_KEY_ORIENTATION = "orientation";
    public static final String DATA_KEY_DURATION = "duration";
    public static final String DATA_KEY_FPS = "fps";

    // Getters
    public boolean isMaster() { return mMaster; }
    public boolean isMaker() { return mMaker; }
    public String getRemoteDevice() { // Return remote device name
        return mRemoteDevice.substring(0, mRemoteDevice.indexOf(Constants.BLUETOOTH_DEVICES_SEPARATOR));
    }
    public String[] getResolutions() {

        String[] resolutions = new String[mResolutions.size()];
        for (int i = 0; i < mResolutions.size(); ++i)
            resolutions[i] = (!Settings.getInstance().mOrientation)?
                    mResolutions.get(i).width + Constants.CONFIG_RESOLUTION_SEPARATOR + mResolutions.get(i).height:
                    mResolutions.get(i).height + Constants.CONFIG_RESOLUTION_SEPARATOR + mResolutions.get(i).width;

        return resolutions;
    }
    public String getResolution() {
        return (!Settings.getInstance().mOrientation)?
                mResolution.width + Constants.CONFIG_RESOLUTION_SEPARATOR + mResolution.height:
                mResolution.height + Constants.CONFIG_RESOLUTION_SEPARATOR + mResolution.width;
    }
    public static void getFrameResolution(Integer width, Integer height) {
        if (getInstance().mOrientation) { // Portrait

            width = new Integer(getInstance().mResolution.height);
            height = new Integer(getInstance().mResolution.width);
        }
        else { // Landscape

            width = new Integer(getInstance().mResolution.width);
            height = new Integer(getInstance().mResolution.height);
        }
    }

    // Setter
    public boolean setResolution(String resolution, String[] list) {

        int resolutionIndex = Constants.NO_DATA;
        for (int i = 0; i < list.length; ++i) {
            if (list[i].equals(resolution)) {

                resolutionIndex = i;
                break;
            }
        }
        if (resolutionIndex == Constants.NO_DATA)
            return false;

        mResolution = mResolutions.get(resolutionIndex);
        return true;
    }

    // Data
    private boolean mMaster; // Master device which has priority (false for slave device)
    private String mRemoteDevice; // Remote device info
    public long mPerformance = 1000; // Device performance representation (lowest is best)
    private boolean mMaker = true; // Flag to know which device will have to make the final video (best performance)

    private final ArrayList<Size> mResolutions = new ArrayList<>(); // Resolutions list

    public boolean mPosition = true; // Left camera position (false for right position)
    public boolean mReverse = false; // Reverse device orientation flag (see 'onReversePosition' method)
    public Size mResolution; // Selected resolution
    public boolean mOrientation = Constants.CONFIG_DEFAULT_ORIENTATION; // Portrait orientation (false for landscape orientation)
    public short mDuration = Constants.CONFIG_DEFAULT_DURATION; // Video duration (in seconds)
    public short mFps = Constants.CONFIG_DEFAULT_FPS; // Frames per second

    // Request types (mask)
    public static final byte REQ_TYPE_INITIALIZE = 0x01;
    public static final byte REQ_TYPE_RESOLUTION = 0x02;
    public static final byte REQ_TYPE_POSITION = 0x04;
    public static final byte REQ_TYPE_ORIENTATION = 0x08;
    public static final byte REQ_TYPE_DURATION = 0x10;
    public static final byte REQ_TYPE_FPS = 0x40;

    //
    private static JSONArray getResolutionsArray(ArrayList<Size> resolutions) {

        JSONArray resolutionsArray = new JSONArray();
        for (Size resolution : resolutions) {

            JSONObject size = new JSONObject();
            try {
                size.put(DATA_KEY_WIDTH, resolution.width);
                size.put(DATA_KEY_HEIGHT, resolution.height);

                resolutionsArray.put(size);
            }
            catch (JSONException e) {

                Logs.add(Logs.Type.E, e.getMessage());
                return null;
            }
        }
        return resolutionsArray;
    }
    private ArrayList<Size> getMergedResolutions(JSONArray resolutions) {

        ArrayList<Size> mergedResolutions = new ArrayList<>();
        try {
            for (int i = 0; i < resolutions.length(); ++i) {

                JSONObject remoteResolution = resolutions.getJSONObject(i);
                for (Size localResolution : mResolutions) {

                    if ((remoteResolution.getInt(DATA_KEY_WIDTH) == localResolution.width) &&
                            (remoteResolution.getInt(DATA_KEY_HEIGHT) == localResolution.height))
                        mergedResolutions.add(localResolution);
                }
            }
        }
        catch (JSONException e) {
            Logs.add(Logs.Type.E, e.getMessage());
        }
        if (!mergedResolutions.isEmpty()) { // Do not update available camera resolutions if
                                            // once merged there is no matched resolution
            mResolutions.clear();
            for (Size resolution : mergedResolutions)
                mResolutions.add(resolution);
        }
        return mergedResolutions;
    }

    //////
    @Override public char getRequestId() { return ConnectRequest.REQ_SETTINGS; }
    @Override public boolean getRequestMerge() { return true; }
    @Override public BufferType getRequestBuffer(byte type) { return BufferType.NONE; }

    @Override public short getMaxWaitReply(byte type) {

        return (type == REQ_TYPE_INITIALIZE)?
                Constants.CONN_MAXWAIT_SETTINGS_INITIALIZE:Constants.CONN_MAXWAIT_DEFAULT;
    }

    @Override
    public String getRequest(byte type, Bundle data) {

        if (type == REQ_TYPE_INITIALIZE) {

            mMaster = data.getBoolean(DATA_KEY_POSITION);
            mPosition = data.getBoolean(DATA_KEY_POSITION);
            mRemoteDevice = data.getString(DATA_KEY_REMOTE_DEVICE);

            // Set up default settings
            mOrientation = Constants.CONFIG_DEFAULT_ORIENTATION;
            mDuration = Constants.CONFIG_DEFAULT_DURATION;
            mFps = Constants.CONFIG_DEFAULT_FPS;

            // Only resolutions may change (all other settings are in default state, see just above)
            // -> It should contain only resolutions that are available on both devices (master & slave)
            mResolutions.clear();

            // Get available camera resolutions
            if (!CameraView.getAvailableResolutions(mResolutions))
                return null;

            // Select default resolution
            mResolution = mResolutions.get(0);

            if (!mMaster)
                return null; // Only master device send initialize request
        }

        JSONObject request = new JSONObject();
        if (type == REQ_TYPE_INITIALIZE) { // Initialize settings request

            // Add resolutions
            try { request.put(DATA_KEY_RESOLUTIONS, getResolutionsArray(mResolutions)); }
            catch (JSONException e) {

                Logs.add(Logs.Type.E, e.getMessage());
                return null;
            }

            // Add performance
            try { request.put(DATA_KEY_PERFORMANCE, mPerformance); }
            catch (JSONException e) {

                Logs.add(Logs.Type.E, e.getMessage());
                return null;
            }
        }
        else { // Update settings request

            if ((type & REQ_TYPE_RESOLUTION) == REQ_TYPE_RESOLUTION) {

                JSONObject size = new JSONObject();
                try {
                    size.put(DATA_KEY_WIDTH, mResolution.width);
                    size.put(DATA_KEY_HEIGHT, mResolution.height);

                    request.put(DATA_KEY_RESOLUTION, size);
                }
                catch (JSONException e) {

                    Logs.add(Logs.Type.E, e.getMessage());
                    return null;
                }
            }
            if ((type & REQ_TYPE_POSITION) == REQ_TYPE_POSITION) {

                try { request.put(DATA_KEY_POSITION, mPosition); }
                catch (JSONException e) {

                    Logs.add(Logs.Type.E, e.getMessage());
                    return null;
                }
            }
            if ((type & REQ_TYPE_ORIENTATION) == REQ_TYPE_ORIENTATION) {

                try { request.put(DATA_KEY_ORIENTATION, mOrientation); }
                catch (JSONException e) {

                    Logs.add(Logs.Type.E, e.getMessage());
                    return null;
                }
            }
            if ((type & REQ_TYPE_DURATION) == REQ_TYPE_DURATION) {

                try { request.put(DATA_KEY_DURATION, mDuration); }
                catch (JSONException e) {

                    Logs.add(Logs.Type.E, e.getMessage());
                    return null;
                }
            }
            if ((type & REQ_TYPE_FPS) == REQ_TYPE_FPS) {

                try { request.put(DATA_KEY_FPS, mFps); }
                catch (JSONException e) {

                    Logs.add(Logs.Type.E, e.getMessage());
                    return null;
                }
            }
        }
        return request.toString();
    }
    @Override
    public String getReply(byte type, String request, PreviousMaster previous) {

        JSONObject settings;
        try { settings = new JSONObject(request); }
        catch (JSONException e) {

            Logs.add(Logs.Type.E, e.getMessage());
            return null;
        }

        JSONObject reply = new JSONObject();
        if (type == REQ_TYPE_INITIALIZE) { // Initialize settings

            try {
                mMaker = mPerformance < settings.getLong(DATA_KEY_PERFORMANCE);

                // Update resolutions to merge available camera resolutions of the
                // remote device with the current ones.
                final ArrayList<Size> mergedResolutions = getMergedResolutions(
                        settings.getJSONArray(DATA_KEY_RESOLUTIONS));

                // Return maker flag & merged resolutions array (even if empty)
                reply.put(DATA_KEY_PERFORMANCE, mMaker);
                reply.put(DATA_KEY_RESOLUTIONS, getResolutionsArray(mergedResolutions));

                if (!mergedResolutions.isEmpty()) {

                    mResolution = mResolutions.get(0); // Select resolution (default)

                    // Start main activity
                    ActivityWrapper.startActivity(MainActivity.class, null, 0);
                }
            }
            catch (JSONException e) {

                Logs.add(Logs.Type.E, e.getMessage());
                return null;
            }
        }
        else { // Update settings (then reply)

            ////// Request received while waiting reply
            //
            // Get previous master request type (if any)
            byte previousType = (previous != null && previous.mId == ConnectRequest.REQ_SETTINGS)?
                    previous.mType:0;
            // -> Should not update settings from a pending request sent by a slave device if a master
            //    request has already updated them (see 'previousType' checks below)

            ///////////////////////////////////////////

            ArrayList<Integer> messageIds = new ArrayList<>();
            if (((type & REQ_TYPE_RESOLUTION) == REQ_TYPE_RESOLUTION) &&
                    ((previousType & REQ_TYPE_RESOLUTION) != REQ_TYPE_RESOLUTION)) {
                try {
                    JSONObject size = settings.getJSONObject(DATA_KEY_RESOLUTION);
                    String resolution;
                    if (!Settings.getInstance().mOrientation)
                        resolution = size.getInt(DATA_KEY_WIDTH) +
                                Constants.CONFIG_RESOLUTION_SEPARATOR + size.getInt(DATA_KEY_HEIGHT);
                    else
                        resolution = size.getInt(DATA_KEY_HEIGHT) +
                                Constants.CONFIG_RESOLUTION_SEPARATOR + size.getInt(DATA_KEY_WIDTH);
                    setResolution(resolution, getResolutions());
                    reply.put(DATA_KEY_RESOLUTION, true); // Ok
                    messageIds.add(R.string.video_resolution);
                }
                catch (JSONException e) {

                    Logs.add(Logs.Type.E, e.getMessage());
                    return null;
                }
            }
            if (((type & REQ_TYPE_POSITION) == REQ_TYPE_POSITION) &&
                    ((previousType & REQ_TYPE_POSITION) != REQ_TYPE_POSITION)) {
                try {
                    mPosition = !settings.getBoolean(DATA_KEY_POSITION);
                    reply.put(DATA_KEY_POSITION, true); // Ok
                    messageIds.add(R.string.camera_position);
                }
                catch (JSONException e) {

                    Logs.add(Logs.Type.E, e.getMessage());
                    return null;
                }
            }
            if (((type & REQ_TYPE_ORIENTATION) == REQ_TYPE_ORIENTATION) &&
                    ((previousType & REQ_TYPE_ORIENTATION) != REQ_TYPE_ORIENTATION)) {
                try {
                    mOrientation = settings.getBoolean(DATA_KEY_ORIENTATION);
                    reply.put(DATA_KEY_ORIENTATION, true); // Ok
                    messageIds.add(R.string.video_orientation);
                }
                catch (JSONException e) {

                    Logs.add(Logs.Type.E, e.getMessage());
                    return null;
                }
            }
            if (((type & REQ_TYPE_DURATION) == REQ_TYPE_DURATION) &&
                    ((previousType & REQ_TYPE_DURATION) != REQ_TYPE_DURATION)) {
                try {
                    mDuration = (short)settings.getInt(DATA_KEY_DURATION);
                    reply.put(DATA_KEY_DURATION, true); // Ok
                    messageIds.add(R.string.video_duration);
                }
                catch (JSONException e) {

                    Logs.add(Logs.Type.E, e.getMessage());
                    return null;
                }
            }
            if (((type & REQ_TYPE_FPS) == REQ_TYPE_FPS) &&
                    ((previousType & REQ_TYPE_FPS) != REQ_TYPE_FPS)) {
                try {
                    mFps = (short)settings.getInt(DATA_KEY_FPS);
                    reply.put(DATA_KEY_FPS, true); // Ok
                    messageIds.add(R.string.video_fps);
                }
                catch (JSONException e) {

                    Logs.add(Logs.Type.E, e.getMessage());
                    return null;
                }
            }

            // Update activities
            try {
                Activity curActivity = ActivityWrapper.get();
                if (curActivity == null)
                    throw new NullPointerException();

                if (curActivity.getClass().equals(SettingsActivity.class)) // Settings activity
                    ((SettingsActivity)curActivity).update(reply);

                else if (curActivity.getClass().equals(MainActivity.class)) { // Main activity
                    if (reply.has(DATA_KEY_POSITION))
                        ((MainActivity) curActivity).displayPosition();

                    // Display a message on settings changes
                    int[] ids = new int[messageIds.size() + 1];
                    ids[0] = R.string.settings_updated;
                    for (int i = 0; i < messageIds.size(); ++i)
                        ids[i + 1] = messageIds.get(i);

                    DisplayMessage.getInstance().snack(R.id.fab, ids, Snackbar.LENGTH_SHORT);
                }
            }
            catch (NullPointerException e) {
                Logs.add(Logs.Type.F, "Wrong activity reference");
            }
        }
        return reply.toString();
    }

    @Override
    public ReceiveResult receiveReply(byte type, String reply) {

        JSONObject receive;
        try { receive = new JSONObject(reply); }
        catch (JSONException e) {

            Logs.add(Logs.Type.E, e.getMessage());
            return ReceiveResult.ERROR;
        }

        //
        if (type == REQ_TYPE_INITIALIZE) { // Initialization reply received

            try {
                mMaker = !receive.getBoolean(DATA_KEY_PERFORMANCE);

                JSONArray resolutions = receive.getJSONArray(DATA_KEY_RESOLUTIONS);
                if (resolutions.length() == 0) {

                    // No available camera resolution is matching between remote and local device
                    Connectivity.getInstance().mNotMatchingDevices.add(mRemoteDevice);

                    DisplayMessage.getInstance().alert(R.string.title_warning,
                            R.string.warning_not_matching,
                            getRemoteDevice(), false, null);

                    return ReceiveResult.ERROR; // ...will disconnect
                }

                // Remove resolutions that are not matching with the remote device (from 'mResolutions')
                getMergedResolutions(resolutions);

                mResolution = mResolutions.get(0); // Select resolution (default)

                // Start main activity
                ActivityWrapper.startActivity(MainActivity.class, null, 0);
            }
            catch (JSONException e) {

                Logs.add(Logs.Type.E, e.getMessage());
                return ReceiveResult.ERROR;
            }
        }
        //else
        // Update reply received (nothing to do)

        return ReceiveResult.SUCCESS;
    }
    @Override
    public ReceiveResult receiveBuffer(ByteArrayOutputStream buffer) {
        return ReceiveResult.ERROR; // Unexpected call
    }
}
