package com.studio.artaban.anaglyph3d.helpers;

import android.hardware.Camera;
import android.os.StatFs;

import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;

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
    public static final String FILENAME_DOWNLOAD_JSON = File.separator + "videos.json"; // JSON webservice file

    public static final String FILENAME_3D_VIDEO = File.separator + "video.webm"; // Final anaglyph 3D video file
    public static final String FILENAME_THUMBNAIL_PICTURE = File.separator + "thumbnail.jpg"; // Thumbnail JPEG picture file

    public static final String FOLDER_THUMBNAILS = File.separator + "Thumbnails";
    public static final String FOLDER_VIDEOS = File.separator + "Videos";
    public static final String FOLDER_DOWNLOAD = File.separator + "Downloads";

    //
    public static boolean moveFile(String src, String dst) {

        Logs.add(Logs.Type.V, "src: " + src + ", dst: " + dst);
        File sourceFile = new File(src);
        File destinationFile = new File(dst);

        // Check if file exists
        if (!sourceFile.exists()) {

            Logs.add(Logs.Type.W, "Source file '" + src + "' does not exist");
            return false;
        }

        // Check if the source is a file
        if (!sourceFile.isFile()) {

            Logs.add(Logs.Type.W, "The source '" + src + "' is not a file");
            return false;
        }

        if (!sourceFile.renameTo(destinationFile)) {
            try { // Try to copy it then delete

                copyFile(sourceFile, destinationFile);
                sourceFile.delete();
            }
            catch (IOException e) {

                Logs.add(Logs.Type.E, "Failed to move file from '" + src + "' to '" + dst + "'");
                return false;
            }
        }
        return true;
    }
    public static void copyFile(File src, File dst) throws IOException {
        Logs.add(Logs.Type.V, "src: " + src + ", dst: " + dst);

        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try { inChannel.transferTo(0, inChannel.size(), outChannel); }
        finally {

            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }
    public static String readFile(File file) throws IOException {
        Logs.add(Logs.Type.V, "file: " + file);

        FileInputStream fin = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
        StringBuilder content = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null)
            content.append(line).append("\n");

        reader.close();
        fin.close();
        return content.toString();
    }
    public static void removeFiles(final String regex) {
        Logs.add(Logs.Type.V, "regex: " + regex);

        File files = new File(ActivityWrapper.DOCUMENTS_FOLDER);
        File[] filesToDelete = files.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.matches(regex);
            }
        });
        if (filesToDelete == null)
            return;

        for (File file : filesToDelete) {
            if (file.isFile())
                file.delete();
        }
    }
    public static boolean createFolder(String folder) {
        Logs.add(Logs.Type.V, "folder: " + folder);

        File directory = new File(folder);
        if ((!directory.exists()) && (!directory.mkdir())) {

            Logs.add(Logs.Type.E, "Failed to create folder: " + folder);
            return false;
        }
        return true;
    }

    //////
    public static void removeTempFiles(boolean downloads) {
        // Remove all temporary files from downloads or documents folder
        Logs.add(Logs.Type.V, "downloads: " + downloads);

        File storage = new File(ActivityWrapper.DOCUMENTS_FOLDER + ((downloads)? FOLDER_DOWNLOAD:""));
        if (storage.listFiles() == null)
            return;

        for (File file : storage.listFiles()) {
            if (file.isFile())
                file.delete();
        }
    }

    public static int getFrameFileCount(final boolean local) {
        // Return the number of frame files contained in documents folder (local or remote frame files)
        Logs.add(Logs.Type.V, "local: " + local);

        File frames = new File(ActivityWrapper.DOCUMENTS_FOLDER);
        File[] files = frames.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {

                // BUG: Only +[0-9] regex is not matching! Not greedy by default !?! See below...
                if (filename.matches("^" + ((local)?
                        Constants.PROCESS_LOCAL_PREFIX:Constants.PROCESS_REMOTE_PREFIX) +
                        "+[0-9]*[0-9]\\" + Constants.EXTENSION_RGBA + "$"))
                    return true;

                return false;
            }
        });
        return (files != null)? files.length:0;
    }

    public static long isStorageEnough() { // Return if available storage space is enough to process
        Logs.add(Logs.Type.V, null);

        // Get storage space need (according settings)
        long need = Settings.getInstance().mFps[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] * // FPS
                Settings.getInstance().mDuration * // Video duration (in second)
                Settings.getInstance().mResolution.height * // Video height (in pixel)
                Settings.getInstance().mResolution.width * // Video width (in pixel)
                4 * // For RGBA info (in bytes)
                2; // For both local and remote files

        if (Settings.getInstance().mSimulated)
            need >>= 1; // Only local files

        need += 4000000; // Add video & picture files size

        // Get available storage space
        StatFs stat = new StatFs(ActivityWrapper.DOCUMENTS_FOLDER);
        long available = (long)stat.getBlockSize() * (long)stat.getAvailableBlocks();

        if (available > need)
            return 0; // Ok: enough memory space to process

        Logs.add(Logs.Type.W, "Not enough storage space available (" + need + "/" + available + ")");
        return need; // Bad: not enough memory space to process (return memory space need)
    }
}
