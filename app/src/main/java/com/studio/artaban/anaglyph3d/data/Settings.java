package com.studio.artaban.anaglyph3d.data;

/**
 * Created by pascal on 23/03/16.
 * Video settings
 */
public class Settings {

    private static Settings ourInstance = new Settings();
    public static Settings getInstance() { return ourInstance; }
    private Settings() { }

    // Accessors
    public boolean getPosition() { return mPosition; }
    public String getRemoteDevice() { return mRemoteDevice; }

    //////
    private boolean mPosition; // Left camera position (false for right position)
    private String mRemoteDevice; // Remote device name

    public boolean initialize(String remote, boolean position) {

        mRemoteDevice = remote;
        mPosition = position;

        // Send initialize settings request



        return true;
    }
}
