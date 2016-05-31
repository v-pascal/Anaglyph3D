package com.studio.artaban.anaglyph3d.data;

import java.io.File;

/**
 * Created by pascal on 21/03/16.
 * Application constants
 */
public final class Constants {

    public static final int NO_DATA = -1; // No value (integer)
    public static final short FAB_SIZE_DPI = 56; // Floating Actionbar Button size (in dpi)
    public static final short DRAWABLE_PLAY_SIZE = 256; // Drawable 'play' image size (in pixel)
    public static final String DATABASE_DATE_FORMAT = "yyyy-MM-dd hh:mm:ss.000"; // SQLite date format


    ////// Activity
    public static final int RESULT_QUIT_APPLICATION = 300; // Quit application requested result
    public static final int RESULT_LOST_CONNECTION = 301; // Connection lost result
    public static final int RESULT_RESTART_CONNECTION = 302; // Active connect activity result
    public static final int RESULT_DISPLAY_ALBUM = 303; // Display album with current 3D video result
    public static final int RESULT_NO_VIDEO = 304; // No video result
    public static final int RESULT_SELECT_VIDEO = 305; // Select video result
    public static final int RESULT_SAVE_VIDEO = 306; // Save video result
    public static final int RESULT_DELETE_VIDEO = 307; // Delete video result

    // Data keys
    public static final String DATA_ACTIVITY = "data"; // Bundle activity data key
    public static final String DATA_CONNECTION_ESTABLISHED = "connected"; // Connection flag key
    public static final String DATA_ADD_VIDEO = "newVideo"; // Create new video flag key


    ////// Connectivity
    public static final String CONN_SECURE_UUID = "76d0eb60-a913-11e5-bfd6-0003a5d5c51b"; // Bluetooth secure UUID
    public static final String CONN_SECURE_NAME = "ANAGLYPH-3D"; // Bluetooth connection name

    public static final short CONN_WAIT_DELAY = 10; // Connection loop sleep (in milliseconds)
    public static final short CONN_WAIT_BUFFER = 100; // Connection loop sleep when receiving buffer (in milliseconds)

    // Maximum delay to receive reply before disconnect (in loop count)
    public static final short CONN_MAXWAIT_DEFAULT = 200;
    public static final short CONN_MAXWAIT_SETTINGS_INITIALIZE = 1000;

    // Common request
    public static final String CONN_REQUEST_TYPE_ASK = "?";
    public static final String CONN_REQUEST_ANSWER_TRUE = "T";
    public static final String CONN_REQUEST_ANSWER_FALSE = "F";


    ////// Bluetooth
    public static final String BLUETOOTH_DEVICES_SEPARATOR = "\n"; // Separator between device name and address


    ////// Settings
    public static final String CONFIG_RESOLUTION_SEPARATOR = " x "; // Separator between with and height
    public static final String CONFIG_FPS_SEPARATOR = " < "; // Separator between min & max fps range
    public static final int CONFIG_PERFORMANCE_LOOP = 4096; // Loop count for performance calculation

    public static final boolean CONFIG_DEFAULT_ORIENTATION = true; // Default orientation (portrait)
    public static final short CONFIG_DEFAULT_DURATION = 30; // Default duration (in seconds)
    public static final int CONFIG_MIN_DURATION = 10; // Minimum duration (in seconds)
    public static final int CONFIG_MAX_DURATION = 180; // Maximum duration (in seconds)


    ////// Extensions
    public static final String EXTENSION_RGBA = ".rgba";
    public static final String EXTENSION_JPEG = ".jpg";
    public static final String EXTENSION_WEBM = ".webm";


    ////// Process
    public static final int PROCESS_REQUEST_CORRECTION = 1; // Contrast & brightness activity request code
    public static final int PROCESS_REQUEST_SYNCHRO = 2; // Synchronization activity request code

    public static final short PROCESS_WAIT_TRANSFER = 700; // Progress loop sleep (in milliseconds)
    public static final short PROCESS_MAX_FPS = 16; // Maximum FPS expected (used to define storage space need)

    public static final String PROCESS_LOCAL_PREFIX = "local";
    public static final String PROCESS_REMOTE_PREFIX = "remote";
    public static final String PROCESS_LOCAL_FRAMES = File.separator + PROCESS_LOCAL_PREFIX + "%04d" + EXTENSION_RGBA;
    public static final String PROCESS_REMOTE_FRAMES = File.separator + PROCESS_REMOTE_PREFIX + "%04d" + EXTENSION_RGBA;

}
