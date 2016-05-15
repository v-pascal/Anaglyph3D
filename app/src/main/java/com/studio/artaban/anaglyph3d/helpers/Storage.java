package com.studio.artaban.anaglyph3d.helpers;

import android.os.StatFs;

import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by pascal on 09/05/16.
 * Storage helper
 */
public final class Storage {

    public static final String FILENAME_RAW_PICTURE = File.separator + "raw.nv21"; // Raw NV21 file from preview camera
    public static final String FILENAME_LOCAL_VIDEO = File.separator + "local.mp4"; // Local video file
    public static final String FILENAME_REMOTE_VIDEO = File.separator + "remote.mp4"; // Remote video file
    public static final String FILENAME_LOCAL_PICTURE = File.separator + "local.rgba"; // Local RGBA raw file
    public static final String FILENAME_REMOTE_PICTURE = File.separator + "remote.rgba"; // Remote RGBA raw file

    public static final String FILENAME_3D_VIDEO = File.separator + "video.webm"; // Final anaglyph 3D video file
    public static final String FILENAME_THUMBNAIL_PICTURE = File.separator + "thumbnail.jpg"; // Thumbnail JPEG picture file

    private static final String FOLDER_THUMBNAIL = File.separator + "Thumbnails";

    //////
    public static void removeTempFiles() { // Remove all temporary files from documents folder

        File storage = new File(ActivityWrapper.DOCUMENTS_FOLDER);
        for (File file : storage.listFiles()) {
            if (file.isFile())
                file.delete();
        }
    }
    public static void removeFiles(final String regex) {

        File files = new File(ActivityWrapper.DOCUMENTS_FOLDER);
        File[] filesToDelete = files.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.matches(regex);
            }
        });
        for (File file : filesToDelete) {
            if (file.isFile())
                file.delete();
        }
    }

    public static long isStorageEnough() { // Return if available storage space is enough to process

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
            return 0; // Ok: enough memory space to process

        Logs.add(Logs.Type.W, "Not enough storage space available (" + need + "/" + available + ")");
        return need; // Bad: not enough memory space to process (return memory space need)
    }

    public static boolean saveThumbnail(String file) { // Rename and move thumbnail JPEG into appropriate folder

        File thumbnailFolder = new File(ActivityWrapper.DOCUMENTS_FOLDER + FOLDER_THUMBNAIL);
        if ((!thumbnailFolder.exists()) && (!thumbnailFolder.mkdir())) {

            Logs.add(Logs.Type.E, "Failed to create thumbnails folder");
            return false;
        }
        File thumbnailFile = new File(ActivityWrapper.DOCUMENTS_FOLDER + FILENAME_THUMBNAIL_PICTURE);
        if (!thumbnailFile.renameTo(new File(ActivityWrapper.DOCUMENTS_FOLDER + FOLDER_THUMBNAIL +
                File.separator + file))) {

            Logs.add(Logs.Type.E, "Failed to rename thumbnail picture file");
            return false;
        }
        return true;
    }
}
