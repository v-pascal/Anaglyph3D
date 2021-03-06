package com.studio.artaban.anaglyph3d.data;

import java.io.File;

/**
 * Created by pascal on 21/03/16.
 * Application constants
 */
public final class Constants {

    public static final int NO_DATA = -1; // No value (integer)
    public static final String DATABASE_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.000"; // SQLite date format
    public static final String FILENAME_DATE_FORMAT = "yyyy-MM-dd_HH-mm-ss.000"; // Date format used for filename (FAT32 compatible)


    ////// Dimension
    public static int DIMEN_ACTION_BAR_HEIGHT; // Action bar height (in pixel)
    public static int DIMEN_STATUS_BAR_HEIGHT; // Status bar height (in pixel)
    public static int DIMEN_FAB_SIZE; // Floating Action Button size (in pixel)


    ////// Activity
    public static final int RESULT_QUIT_APPLICATION = 300; // Quit application requested result
    public static final int RESULT_LOST_CONNECTION = 301; // Connection lost result
    public static final int RESULT_DISPLAY_ALBUM = 302; // Display album with current 3D video result
    public static final int RESULT_NO_VIDEO = 303; // No video result
    public static final int RESULT_SELECT_VIDEO = 304; // Select video result
    public static final int RESULT_SAVE_VIDEO = 305; // Save video result
    public static final int RESULT_DELETE_VIDEO = 306; // Delete video result

    // Data keys
    public static final String DATA_ACTIVITY = "data"; // Bundle activity data key
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
    public static final int CONFIG_PERFORMANCE_LOOP = 4096; // Loop count for performance calculation

    public static final short CONFIG_DEFAULT_DURATION = 20; // Default duration (in seconds)
    public static final int CONFIG_MIN_DURATION = 10; // Minimum duration (in seconds)
    public static final int CONFIG_MAX_DURATION = 180; // Maximum duration (in seconds)
    public static final short CONFIG_DEFAULT_FPS = 20; // Default FPS used to know memory space available

    ////// Extensions
    public static final String EXTENSION_RGBA = ".rgba";
    public static final String EXTENSION_JPEG = ".jpg";
    public static final String EXTENSION_WEBM = ".webm";


    ////// Process
    public static final int PROCESS_REQUEST_CORRECTION = 1; // Contrast & brightness activity request code
    public static final int PROCESS_REQUEST_SYNCHRO = 2; // Synchronization activity request code
    public static final int PROCESS_REQUEST_SHIFT = 3; // Shift activity request code
    public static final int PROCESS_REQUEST_CROPPING = 4; // Cropping activity request code

    public static final short PROCESS_WAIT_TRANSFER = 700; // Progress loop sleep (in milliseconds)

    public static final String PROCESS_LOCAL_PREFIX = "local";
    public static final String PROCESS_REMOTE_PREFIX = "remote";
    public static final String PROCESS_LOCAL_FRAMES = File.separator + PROCESS_LOCAL_PREFIX + "%04d" + EXTENSION_RGBA;
    public static final String PROCESS_REMOTE_FRAMES = File.separator + PROCESS_REMOTE_PREFIX + "%04d" + EXTENSION_RGBA;


    ////// Download videos
    public static final String DOWNLOAD_URL = "http://studio-artaban.com/Anaglyph3D/videos.php";

}
