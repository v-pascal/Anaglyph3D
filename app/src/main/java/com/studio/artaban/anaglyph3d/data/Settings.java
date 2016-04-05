package com.studio.artaban.anaglyph3d.data;

import android.hardware.Camera.Size;
import android.os.Bundle;

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
    private boolean mMaster; // Master device which has settings priority (false for slave device)
    private String mRemoteDevice; // Remote device info
    public long mPerformance = 1000; // Device performance representation (lowest is best)
    private boolean mMaker = true; // Flag to know which device will have to make the final video (best performance)

    private final ArrayList<Size> mResolutions = new ArrayList<Size>(); // Resolutions list

    public boolean mPosition = true; // Left camera position (false for right position)
    public boolean mOrientation = true; // Portrait orientation (false for landscape orientation)
    public Size mResolution; // Selected resolution
    public int mDuration = 60; // Video duration (in seconds)
    public int mFps = 30; // Frame per second

    // Request types (mask)
    public static final byte REQ_TYPE_INITIALIZE = 0x01;
    public static final byte REQ_TYPE_RESOLUTION = 0x02;
    public static final byte REQ_TYPE_POSITION = 0x04;
    public static final byte REQ_TYPE_ORIENTATION = 0x08;
    public static final byte REQ_TYPE_DURATION = 0x0f;
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

        ArrayList<Size> mergedResolutions = new ArrayList<Size>();
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
    @Override
    public char getRequestId() { return ConnectRequest.REQ_SETTINGS; }

    @Override
    public String getRequest(byte type, Bundle data) {

        if (type == REQ_TYPE_INITIALIZE) {

            mMaster = data.getBoolean(DATA_KEY_POSITION);
            mPosition = data.getBoolean(DATA_KEY_POSITION);
            mRemoteDevice = data.getString(DATA_KEY_REMOTE_DEVICE);

            // Set up default settings
            mOrientation = true;
            mDuration = 60;
            mFps = 30;

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
    public String getReply(byte type, String request) {

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
                    ActivityWrapper.startMainActivity();
                }
            }
            catch (JSONException e) {

                Logs.add(Logs.Type.E, e.getMessage());
                return null;
            }
        }
        else { // Update settings (then reply)

            if ((type & REQ_TYPE_RESOLUTION) == REQ_TYPE_RESOLUTION) {






            }
            if ((type & REQ_TYPE_POSITION) == REQ_TYPE_POSITION) {






            }
            if ((type & REQ_TYPE_ORIENTATION) == REQ_TYPE_ORIENTATION) {





            }
            if ((type & REQ_TYPE_DURATION) == REQ_TYPE_DURATION) {





            }
            if ((type & REQ_TYPE_FPS) == REQ_TYPE_FPS) {





            }
        }
        return reply.toString();
    }

    @Override
    public boolean receiveReply(byte type, String reply) {

        JSONObject receive;
        try { receive = new JSONObject(reply); }
        catch (JSONException e) {

            Logs.add(Logs.Type.E, e.getMessage());
            return false;
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

                    return false; // ...will disconnect
                }

                // Remove resolutions that are not matching with the remote device (from 'mResolutions')
                getMergedResolutions(resolutions);

                mResolution = mResolutions.get(0); // Select resolution (default)

                // Start main activity
                ActivityWrapper.startMainActivity();
            }
            catch (JSONException e) {

                Logs.add(Logs.Type.E, e.getMessage());
                return false;
            }
        }
        else { // Update reply received







        }
        return true;
    }
}
