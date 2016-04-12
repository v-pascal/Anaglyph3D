package com.studio.artaban.anaglyph3d.data;

/**
 * Created by pascal on 21/03/16.
 * Application constants
 */
public class Constants {

    public static final int NO_DATA = -1; // No value (integer)

    ////// Activity (results & data)
    public static final int RESULT_QUIT_APPLICATION = 300; // Quit application requested
    public static final int RESULT_LOST_CONNECTION = 301; // Connection lost result
    public static final int RESULT_RESTART_CONNECTION = 302; // Active connect activity

    public static final String DATA_CONNECTION_ESTABLISHED = "connected"; // Connection flag key

    ////// Connectivity
    public static final String CONN_SECURE_UUID = "76d0eb60-a913-11e5-bfd6-0003a5d5c51b"; // Bluetooth secure UUID
    public static final String CONN_SECURE_NAME = "ANAGLYPH-3D"; // Bluetooth connection name

    public static final int CONN_WAIT_DELAY = 10; // Connection loop sleep (in milliseconds)

    // Maximum delay to receive reply before disconnect (in loop count)
    public static final short CONN_MAXWAIT_DEFAULT = 200;
    public static final short CONN_MAXWAIT_SETTINGS_INITIALIZE = 1000;

    ////// Bluetooth
    public static final String BLUETOOTH_DEVICES_SEPARATOR = "\n"; // Separator between device name and address

    ////// Settings
    public static final String CONFIG_RESOLUTION_SEPARATOR = " x "; // Separator between with and height
    public static final int CONFIG_PERFORMANCE_LOOP = 100000; // Loop count for performance calculation

    public static final short CONFIG_DEFAULT_DURATION = 60; // Default duration (in seconds)
    public static final int CONFIG_MIN_DURATION = 10; // Minimum duration (in seconds)
    public static final int CONFIG_MAX_DURATION = 180; // Maximum duration (in seconds)

    public static final short CONFIG_DEFAULT_FPS = 8; // Default frames per second
    public static final int CONFIG_MIN_FPS = 5; // Minimum frames per second
    public static final int CONFIG_MAX_FPS = 20; // Maximum frames per second
}
