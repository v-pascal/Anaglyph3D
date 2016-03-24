package com.studio.artaban.anaglyph3d.data;

import android.os.Bundle;
import android.util.Size;

import com.studio.artaban.anaglyph3d.transfert.ConnRequest;

import java.util.ArrayList;

/**
 * Created by pascal on 23/03/16.
 * Video settings
 */
public class Settings implements ConnRequest {

    private static Settings ourInstance = new Settings();
    public static Settings getInstance() { return ourInstance; }
    private Settings() { }

    public boolean getPosition() { return mPosition; }
    public String getRemoteDevice() { return mRemoteDevice; }
    // Accessors

    private boolean mPosition; // Left camera position (false for right position)
    private String mRemoteDevice; // Remote device name
    private final ArrayList<Size> mResolutions = new ArrayList<Size>(); // Resolutions list

    //////
    @Override
    public boolean sendRequest(Bundle data) {

        mPosition = data.getBoolean(KEY_SETTINGS_REMOTE);
        mRemoteDevice = data.getString(KEY_SETTINGS_POSITION);

        // Send settings request






        return true;
    }

    @Override
    public boolean receiveReply(String reply) {





        return false;
    }
}
