package com.studio.artaban.anaglyph3d.data;

import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.helpers.CameraView;
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.transfert.ConnRequest;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pascal on 23/03/16.
 * Video settings
 */
public class Settings implements ConnRequest {

    private static Settings ourInstance = new Settings();
    public static Settings getInstance() { return ourInstance; }
    private Settings() { }

    // Data keys
    public static final String DATA_KEY_REMOTE_DEVICE = "remoteDevice";
    public static final String DATA_KEY_POSITION = "position";

    // Accessors
    public boolean getPosition() { return mPosition; }
    public String getRemoteDevice() { return mRemoteDevice; }

    // Data
    private String mRemoteDevice; // Remote device name

    private boolean mPosition; // Left camera position (false for right position)
    private final ArrayList<Size> mResolutions = new ArrayList<Size>(); // Resolutions list
    private boolean mOrientation; // Portrait orientation (false for landscape orientation)
    private int mDuration; // Video duration (in milliseconds)
    private int mFps; // Frame per second

    // Request types
    public static final short REQ_TYPE_INITIALIZE = 1;
    public static final short REQ_TYPE_RESOLUTIONS = 2;
    public static final short REQ_TYPE_POSITION = 3;
    public static final short REQ_TYPE_ORIENTATION = 4;
    public static final short REQ_TYPE_DURATION = 5;
    public static final short REQ_TYPE_FPS = 6;

    //////
    @Override
    public String getRequestCmd(short type, Bundle data) {
        if (type == REQ_TYPE_INITIALIZE) {

            mPosition = data.getBoolean(DATA_KEY_POSITION);
            mRemoteDevice = data.getString(DATA_KEY_REMOTE_DEVICE);

            // Only left camera send initialize request
            if (!mPosition)
                return null;

            type = REQ_TYPE_RESOLUTIONS; // Only resolutions may change
        }                                // -> All other settings are in default state
        switch (type) {
            case REQ_TYPE_RESOLUTIONS: {

                // Get available camera resolutions
                final Camera camera = CameraView.getCamera();
                if (camera == null) {

                    Logs.add(Logs.Type.E, "Failed to get available camera resolutions");
                    DisplayMessage.getInstance().alert(R.string.error_title, R.string.camera_disabled, true);
                    return null;
                }
                mResolutions.clear();

                List<Size> camResolutions = camera.getParameters().getSupportedPreviewSizes();
                for (final Size camResolution: camResolutions)
                    mResolutions.add(camResolution);

                break;
            }
            default: {
                Logs.add(Logs.Type.F, "Unexpected settings request type");
                return null;
            }
        }

        JSONObject request = new JSONObject();










        return request.toString();
    }

    @Override
    public boolean sendReply(String request) {





        return false;
    }

    @Override
    public boolean receiveReply(String reply) {






        return false;
    }

    @Override
    public short getRequestId() { return ConnRequest.REQ_SETTINGS; }
}
