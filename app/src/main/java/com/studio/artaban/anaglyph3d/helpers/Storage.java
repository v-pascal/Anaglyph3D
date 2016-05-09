package com.studio.artaban.anaglyph3d.helpers;

import android.os.StatFs;

import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;

import java.io.File;

/**
 * Created by pascal on 09/05/16.
 * Storage helpers
 */
public final class Storage {

    public static final String FILENAME_RAW_PICTURE = "/raw.nv21"; // Raw NV21 file from preview camera
    public static final String FILENAME_LOCAL_VIDEO = "/local.mp4"; // Local video file
    public static final String FILENAME_REMOTE_VIDEO = "/remote.mp4"; // Remote video file
    public static final String FILENAME_LOCAL_PICTURE = "/local.rgba"; // Local RGBA raw file
    public static final String FILENAME_REMOTE_PICTURE = "/remote.rgba";

    public static final String FILENAME_3D_VIDEO = "/video.webm";

    //////
    public static void removeTempFiles() { // Remove all temporary files from documents folder

        File storage = new File(ActivityWrapper.DOCUMENTS_FOLDER);
        for (File file : storage.listFiles()) {
            if (file.isFile())
                file.delete();
        }
    }
    public static boolean isStorageEnough() { // Return if available storage space is enough for process

        // Get storage space need (according settings)
        long need = Constants.PROCESS_MAX_FPS * // Maximum FPS expected
                Settings.getInstance().mDuration * // Video duration (in second)
                Settings.getInstance().mResolution.height * // Video height (in pixel)
                Settings.getInstance().mResolution.width * // Video width (in pixel)
                4 * // For RGBA info (in bytes)
                2; // For both local and remote files

        need += 5000000; // Add video & picture files size

        // Get available storage space
        StatFs stat = new StatFs(ActivityWrapper.DOCUMENTS_FOLDER);
        long available = (long)stat.getBlockSize() * (long)stat.getAvailableBlocks();

        if (available > need)
            return true;

        Logs.add(Logs.Type.V, "Not enough storage space available (" + need + "/" + available + ")");
        return false;
    }
}
