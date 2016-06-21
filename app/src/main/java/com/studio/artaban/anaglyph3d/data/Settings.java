package com.studio.artaban.anaglyph3d.data;

import android.app.Activity;
import android.hardware.Camera;
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
import com.studio.artaban.anaglyph3d.transfer.IConnectRequest;
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
public class Settings implements IConnectRequest {

    private static Settings ourInstance = new Settings();
    public static Settings getInstance() { return ourInstance; }
    private Settings() { }

    ////// Data keys
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
    private static final String DATA_KEY_MIN_FPS = "minFps";
    private static final String DATA_KEY_MAX_FPS = "maxFps";

    ////// Getters
    public boolean isMaster() { return mMaster; }
    public boolean isMaker() { return mMaker; }
    public String getRemoteDevice() { // Return remote device name

        Logs.add(Logs.Type.V, null);
        if (mRemoteDevice != null)
            return mRemoteDevice.substring(0, mRemoteDevice.indexOf(Constants.BLUETOOTH_DEVICES_SEPARATOR));

        return null;
    }

    public String[] getResolutions() {

        Logs.add(Logs.Type.V, null);
        String[] resolutions = new String[mResolutions.size()];
        for (int i = 0; i < mResolutions.size(); ++i)
            resolutions[i] = (!Settings.getInstance().mOrientation)?
                    mResolutions.get(i).width + Constants.CONFIG_RESOLUTION_SEPARATOR + mResolutions.get(i).height:
                    mResolutions.get(i).height + Constants.CONFIG_RESOLUTION_SEPARATOR + mResolutions.get(i).width;

        return resolutions;
    }
    public String getResolution() {
        return (!mOrientation)?
                mResolution.width + Constants.CONFIG_RESOLUTION_SEPARATOR + mResolution.height:
                mResolution.height + Constants.CONFIG_RESOLUTION_SEPARATOR + mResolution.width;
    }

    public String[] getFpsRanges() {
        Logs.add(Logs.Type.V, null);

        String[] ranges = new String[mFpsRanges.size()];
        for (int i = 0; i < mFpsRanges.size(); ++i)
            ranges[i] = String.valueOf(mFpsRanges.get(i)[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] / 1000);

        return ranges;
    }
    public String getFpsRange() {
        return String.valueOf(mFps[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] / 1000);
    }

    public int getResolutionWidth() { return (mOrientation)? mResolution.height:mResolution.width; }
    public int getResolutionHeight() { return (mOrientation)? mResolution.width:mResolution.height; }

    ////// Setter
    public boolean initialize() { // Initialize camera settings
        Logs.add(Logs.Type.V, null);

        // Set up default settings
        mOrientation = !mSimulated;
        mDuration = Constants.CONFIG_DEFAULT_DURATION;

        mMaker = true;
        mReverse = false;
        // Needed for simulated 3D initialization

        // Only resolutions & FPS may change (all other settings are in default state, see just above)
        // -> They should contain only resolutions & FPS that are available on both devices (master & slave)
        mResolutions.clear();
        mFpsRanges.clear();

        // Get available camera resolutions
        if (!CameraView.getAvailableSettings(mResolutions, mFpsRanges))
            return false;

        // Select default resolution & fps
        mResolution = mResolutions.get(0);
        mFps = mFpsRanges.get(0);
        checkNoFps();

        return true;
    }
    public boolean setResolution(String resolution, String[] list) {

        Logs.add(Logs.Type.V, "resolution: " + resolution + ", list: " + ((list != null)? list.length:"null"));

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
    public boolean setFps(String fps, String[] list) {

        Logs.add(Logs.Type.V, "fps: " + fps + ", list: " + ((list != null)? list.length:"null"));

        int rangeIndex = Constants.NO_DATA;
        for (int i = 0; i < list.length; ++i) {
            if (list[i].equals(fps)) {

                rangeIndex = i;
                break;
            }
        }
        if (rangeIndex == Constants.NO_DATA)
            return false;

        mFps = mFpsRanges.get(rangeIndex);
        return true;
    }

    private void checkNoFps() { // Check if at least one valid FPS is available (!= 1)

        //if ((mFpsRanges.size() == 1) && (mFps[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] == 1))
        //    mNoFps = true;
        // NB: FPS setting removed
    }

    ////// Data
    private boolean mMaster; // Master device which has priority (false for slave device)
    private String mRemoteDevice; // Remote device info
    public long mPerformance = 1000; // Device performance representation (lowest is best)
    private boolean mMaker = true; // Flag to know which device will have to make the final video (best performance)

    private final ArrayList<Size> mResolutions = new ArrayList<>(); // Available resolutions list
    private final ArrayList<int[]> mFpsRanges = new ArrayList<>(); // Available fps range list (scaled by 1000)

    public boolean mSimulated = false; // Simulated 3D flag
    public boolean mPosition = true; // Left camera position (false for right position)
    public boolean mReverse = false; // Reverse device orientation flag (see 'onReversePosition' method)
    public Size mResolution; // Selected resolution
    public boolean mOrientation = true; // Portrait orientation (false for landscape orientation)
    public short mDuration = Constants.CONFIG_DEFAULT_DURATION; // Video duration (in seconds)
    public int[] mFps; // Frames per second range (scaled by 1000)
    public boolean mNoFps = false; // FPS setting use flag

    // Request types (mask)
    public static final byte REQ_TYPE_INITIALIZE = 0x01;
    public static final byte REQ_TYPE_RESOLUTION = 0x02;
    public static final byte REQ_TYPE_POSITION = 0x04;
    public static final byte REQ_TYPE_ORIENTATION = 0x08;
    public static final byte REQ_TYPE_DURATION = 0x10;
    public static final byte REQ_TYPE_FPS = 0x40;

    //
    private static JSONArray getResolutionsArray(ArrayList<Size> resolutions) {

        Logs.add(Logs.Type.V, "resolutions: " + resolutions);
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

        Logs.add(Logs.Type.V, "resolutions: " + resolutions);
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

    private static JSONArray getFpsRangesArray(ArrayList<int[]> ranges) {

        Logs.add(Logs.Type.V, "ranges: " + ranges);
        JSONArray rangesArray = new JSONArray();
        for (int[] range : ranges) {

            JSONObject limits = new JSONObject();
            try {
                limits.put(DATA_KEY_MIN_FPS, range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX]);
                limits.put(DATA_KEY_MAX_FPS, range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);

                rangesArray.put(limits);
            }
            catch (JSONException e) {

                Logs.add(Logs.Type.E, e.getMessage());
                return null;
            }
        }
        return rangesArray;
    }
    private ArrayList<int[]> getMergedFpsRanges(JSONArray ranges) {

        Logs.add(Logs.Type.V, "ranges: " + ranges);
        ArrayList<int[]> mergedRanges = new ArrayList<>();
        try {
            for (int i = 0; i < ranges.length(); ++i) {

                JSONObject remoteRange = ranges.getJSONObject(i);
                for (int[] localRange : mFpsRanges) {

                    if (remoteRange.getInt(DATA_KEY_MIN_FPS) == localRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX])
                        mergedRanges.add(localRange);
                }
            }
        }
        catch (JSONException e) {
            Logs.add(Logs.Type.E, e.getMessage());
        }
        if (!mergedRanges.isEmpty()) { // Do not update available camera fps ranges if
                                       // once merged there is no matched fps range
            mFpsRanges.clear();
            for (int[] range : mergedRanges)
                mFpsRanges.add(range);
        }
        return mergedRanges;
    }

    //////
    @Override public char getRequestId() { return IConnectRequest.REQ_SETTINGS; }
    @Override public boolean getRequestMerge() { return true; }
    @Override public BufferType getRequestBuffer(byte type) { return BufferType.NONE; }

    @Override public short getMaxWaitReply(byte type) {

        return (type == REQ_TYPE_INITIALIZE)?
                Constants.CONN_MAXWAIT_SETTINGS_INITIALIZE:Constants.CONN_MAXWAIT_DEFAULT;
    }

    @Override
    public String getRequest(byte type, Bundle data) {

        Logs.add(Logs.Type.V, "type: " + type + ", data: " + data);
        if (type == REQ_TYPE_INITIALIZE) {

            mMaster = data.getBoolean(DATA_KEY_POSITION);
            mPosition = data.getBoolean(DATA_KEY_POSITION);
            mRemoteDevice = data.getString(DATA_KEY_REMOTE_DEVICE);

            if (!initialize()) // Initialize camera settings
                return null;

            if (!mMaster)
                return null; // Only master device send initialize request
        }

        JSONObject request = new JSONObject();
        if (type == REQ_TYPE_INITIALIZE) { // Initialize settings request

            // Add resolutions & fps ranges
            try {
                request.put(DATA_KEY_RESOLUTIONS, getResolutionsArray(mResolutions));
                request.put(DATA_KEY_FPS, getFpsRangesArray(mFpsRanges));
            }
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

                JSONObject limits = new JSONObject();
                try {
                    limits.put(DATA_KEY_MIN_FPS, mFps[Camera.Parameters.PREVIEW_FPS_MIN_INDEX]);
                    limits.put(DATA_KEY_MAX_FPS, mFps[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);

                    request.put(DATA_KEY_FPS, limits);
                }
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

        Logs.add(Logs.Type.V, "type: " + type + ", request: " + request + ", previous: " + previous);
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

                // Update resolutions & fps ranges to merge available camera resolutions & fps of the
                // remote device with the current ones.
                final ArrayList<Size> mergedResolutions = getMergedResolutions(
                        settings.getJSONArray(DATA_KEY_RESOLUTIONS));
                //final ArrayList<int[]> mergedFpsRanges = getMergedFpsRanges(
                //        settings.getJSONArray(DATA_KEY_FPS));
                // NB: FPS setting removed

                // Return maker flag & merged resolutions & fps ranges array (even if empties)
                reply.put(DATA_KEY_PERFORMANCE, mMaker);
                reply.put(DATA_KEY_RESOLUTIONS, getResolutionsArray(mergedResolutions));
                //reply.put(DATA_KEY_FPS, getFpsRangesArray(mergedFpsRanges));
                // NB: FPS setting removed

                //if ((!mergedResolutions.isEmpty()) && (!mergedFpsRanges.isEmpty())) { FPS setting removed
                if (!mergedResolutions.isEmpty()) {

                    // Select resolution & fps (default)
                    mResolution = mResolutions.get(0);
                    mFps = mFpsRanges.get(0);
                    checkNoFps();

                    ////// Start main activity
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
            byte previousType = (previous != null && previous.mId == IConnectRequest.REQ_SETTINGS)?
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

                    Logs.add(Logs.Type.I, "REQ_TYPE_RESOLUTION");
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

                    Logs.add(Logs.Type.I, "REQ_TYPE_POSITION");
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

                    Logs.add(Logs.Type.I, "REQ_TYPE_ORIENTATION");
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

                    Logs.add(Logs.Type.I, "REQ_TYPE_DURATION");
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
                    JSONObject limits = settings.getJSONObject(DATA_KEY_FPS);
                    String fps = String.valueOf(limits.getInt(DATA_KEY_MIN_FPS));
                    setFps(fps, getFpsRanges());
                    reply.put(DATA_KEY_FPS, true); // Ok

                    Logs.add(Logs.Type.I, "REQ_TYPE_FPS");
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
                        ((MainActivity) curActivity).displayPosition(true);

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

        Logs.add(Logs.Type.V, "type: " + type + ", reply: " + reply);
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
                //JSONArray fpsRanges = receive.getJSONArray(DATA_KEY_FPS);
                //if ((resolutions.length() == 0) || (fpsRanges.length() == 0)) { FPS setting removed
                if (resolutions.length() == 0) {

                    // No available camera resolution or fps is matching between remote and local device
                    Connectivity.getInstance().mNotMatchingDevices.add(mRemoteDevice);

                    DisplayMessage.getInstance().alert(R.string.title_warning,
                            R.string.warning_not_matching,
                            getRemoteDevice(), false, null);

                    return ReceiveResult.ERROR; // ...will disconnect
                }

                // Remove resolutions & fps ranges that are not matching with the remote device
                getMergedResolutions(resolutions);
                //getMergedFpsRanges(fpsRanges);
                // NB: FPS setting removed

                // Select resolution & fps (default)
                mResolution = mResolutions.get(0);
                mFps = mFpsRanges.get(0);
                checkNoFps();

                ////// Start main activity
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
