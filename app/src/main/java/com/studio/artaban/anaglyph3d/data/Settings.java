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

    // Accessors
    public boolean getMaster() { return mMaster; } // See use...
    public String getRemoteDevice() { return mRemoteDevice; }
    public String[] getResolutions() {

        /*
        String[] resolutions = new String[mResolutions.size()];
        for (int i = 0; i < mResolutions.size(); ++i)
            resolutions[i] = mResolutions.get(i).toString();
        */



        String[] resolutions = new String[5];
        resolutions[0] = "640 x 480";
        resolutions[1] = "1640 x 480";
        resolutions[2] = "2640 x 480";
        resolutions[3] = "640 x 1480";
        resolutions[4] = "1640 x 1480";




        return resolutions;
    }

    // Data
    private boolean mMaster; // Master device (false for slave device)
    private String mRemoteDevice; // Remote device name
    public long mPerformance = 1000; // Device performance representation (lowest is best)

    private final ArrayList<Size> mResolutions = new ArrayList<Size>(); // Resolutions list

    public boolean mPosition = true; // Left camera position (false for right position)
    public boolean mOrientation = true; // Portrait orientation (false for landscape orientation)
    public Size mResolution; // Selected resolution
    public int mDuration = 10000; // Video duration (in milliseconds)
    public int mFps = 30; // Frame per second

    // Request types (mask)
    public static final byte REQ_TYPE_INITIALIZE = 0x01;
    public static final byte REQ_TYPE_RESOLUTION = 0x02;
    public static final byte REQ_TYPE_POSITION = 0x04;
    public static final byte REQ_TYPE_ORIENTATION = 0x08;
    public static final byte REQ_TYPE_DURATION = 0x0f;
    public static final byte REQ_TYPE_FPS = 0x40;

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
