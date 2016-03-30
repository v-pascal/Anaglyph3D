package com.studio.artaban.anaglyph3d.data;

import android.hardware.Camera.Size;
import android.os.Bundle;

import com.studio.artaban.anaglyph3d.helpers.CameraView;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.transfer.ConnRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by pascal on 23/03/16.
 * Video settings
 */
public class Settings implements ConnRequest {

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
    public String getRemoteDevice() { return mRemoteDevice; }
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
    private boolean mMaster; // Master device (false for slave device)
    private String mRemoteDevice; // Remote device name
    public long mPerformance = 1000; // Device performance representation (lowest is best)

    private final ArrayList<Size> mResolutions = new ArrayList<Size>(); // Resolutions list

    public boolean mPosition = true; // Left camera position (false for right position)
    public boolean mOrientation = true; // Portrait orientation (false for landscape orientation)
    public Size mResolution; // Selected resolution
    public int mDuration = 60000; // Video duration (in milliseconds)
    public int mFps = 30; // Frame per second

    // Request types (mask)
    public static final byte REQ_TYPE_INITIALIZE = 0x01;
    public static final byte REQ_TYPE_RESOLUTION = 0x02;
    public static final byte REQ_TYPE_POSITION = 0x04;
    public static final byte REQ_TYPE_ORIENTATION = 0x08;
    public static final byte REQ_TYPE_DURATION = 0x0f;
    public static final byte REQ_TYPE_FPS = 0x40;












    public boolean initResolutions() {

        if (!CameraView.getAvailableResolutions(mResolutions))
            return false;

        mResolution = mResolutions.get(0);
        return true;
    }










    //////
    @Override
    public char getRequestId() { return ConnRequest.REQ_SETTINGS; }

    @Override
    public String getRequest(byte type, Bundle data) {

        if (type == REQ_TYPE_INITIALIZE) {

            mMaster = data.getBoolean(DATA_KEY_POSITION);
            mPosition = data.getBoolean(DATA_KEY_POSITION);
            mRemoteDevice = data.getString(DATA_KEY_REMOTE_DEVICE);

            // Only resolutions may change (all other settings are in default state)
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
        if (type == REQ_TYPE_INITIALIZE) { // Initialize settings

            JSONArray resolutions = new JSONArray();
            for (Size resolution : mResolutions) {

                JSONObject size = new JSONObject();
                try {
                    size.put(DATA_KEY_WIDTH, resolution.width);
                    size.put(DATA_KEY_HEIGHT, resolution.height);

                    resolutions.put(size);
                }
                catch (JSONException e) {

                    Logs.add(Logs.Type.E, e.getMessage());
                    return null;
                }
            }
            try { request.put(DATA_KEY_RESOLUTIONS, resolutions); }
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
        else { // Update settings

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

        try { JSONObject settings = new JSONObject(request); }
        catch (JSONException e) {

            Logs.add(Logs.Type.E, e.getMessage());
            return null;
        }

        String reply;
        if (type == REQ_TYPE_INITIALIZE) { // Initialize settings

        }
        else { // Update settings

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
        return null;
    }

    @Override
    public boolean receiveReply(byte type, String reply) {






        return false;
    }
}
