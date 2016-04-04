package com.studio.artaban.anaglyph3d.data;

/**
 * Created by pascal on 21/03/16.
 * Application constants
 */
public class Constants {

    public static final int NO_DATA = -1;

    ////// Activity (results & data)
    public static final int RESULT_QUIT_APPLICATION = 300;
    public static final int RESULT_LOST_CONNECTION = 301;
    public static final int RESULT_RESTART_CONNECTION = 302;

    public static final String DATA_CONNECTION_ESTABLISHED = "connected";

    ////// Connectivity
    public static final String CONN_SECURE_UUID = "76d0eb60-a913-11e5-bfd6-0003a5d5c51b";
    public static final String CONN_SECURE_NAME = "ANAGLYPH3D";

    ////// Bluetooth
    public static final String BLUETOOTH_DEVICES_SEPARATOR = "\n";

    ////// Settings
    public static final String CONFIG_RESOLUTION_SEPARATOR = " x ";

    public static final int CONFIG_MIN_DURATION = 10;
    public static final int CONFIG_MAX_DURATION = 180;

    public static final int CONFIG_MIN_FPS = 20;
    public static final int CONFIG_MAX_FPS = 60;

}
