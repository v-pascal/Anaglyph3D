package com.studio.artaban.anaglyph3d.media;

import android.os.Bundle;

import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.process.ProcessThread;
import com.studio.artaban.anaglyph3d.transfer.BufferRequest;
import com.studio.artaban.anaglyph3d.transfer.ConnectRequest;

import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;

/**
 * Created by pascal on 26/04/16.
 * Video class to manage video transfer & extraction
 */
public class Video extends BufferRequest {

    private static Video ourInstance = new Video();
    public static Video getInstance() { return ourInstance; }
    private Video() { super(ConnectRequest.REQ_VIDEO); }

    //////
    @Override
    public String getRequest(byte type, Bundle data) {

        switch (type) {
            case REQ_TYPE_DOWNLOAD: break;
            case REQ_TYPE_UPLOAD: return Constants.CONN_REQUEST_TYPE_ASK;
        }

        JSONObject request = getBufferRequest(type, data);
        if (request != null)
            return request.toString();

        return null;
    }
    @Override
    public String getReply(byte type, String request, PreviousMaster previous) {
        return getBufferReply(type, request);
    }

    //////
    private static final String AUDIO_WAV_FILENAME = "audio.wav";

    public static boolean extractFramesRGBA(String file, Frame.Orientation orientation, String frames) {

        return ProcessThread.mGStreamer.launch("filesrc location=\"" + file + "\" ! decodebin" +
                " ! videoflip method= method=" + orientation.getFlipMethod() + " ! videoconvert" +
                " ! video/x-raw,format=RGBA ! multifilesink location=\"" + frames + "\"");
    }
    public static int mergeFPS() { // Remove the too many frames from video with bigger FPS

        File frames = new File(ActivityWrapper.DOCUMENTS_FOLDER);
        File[] files = frames.listFiles();

        // Count local frame files
        int localFrameCount = 0;
        int startIndex = ActivityWrapper.DOCUMENTS_FOLDER.length() + Constants.PROCESS_LOCAL_PREFIX.length();
        for (File file: files) {
            if (file.getAbsolutePath().matches("^" + ActivityWrapper.DOCUMENTS_FOLDER +
                    Constants.PROCESS_LOCAL_PREFIX + "+[0-9]*[0-9]\\" + Constants.PROCESS_RGBA_EXTENSION + "$")) {
                                              // only +[0-9] is not matching !?! Not greedy by default !?!

                int fileIndex = Integer.parseInt(file.getAbsolutePath().substring(startIndex,
                        file.getAbsolutePath().lastIndexOf('.')));

                // Rename frame file from local%d.rgba to local%04d.rgba (needed to sort correctly)
                file.renameTo(new File(ActivityWrapper.DOCUMENTS_FOLDER +
                        Constants.PROCESS_LOCAL_PREFIX + String.format("%04d", fileIndex) +
                        Constants.PROCESS_RGBA_EXTENSION));
                ++localFrameCount;
            }
        }

        // Count remote frame files
        int remoteFrameCount = 0;
        startIndex = ActivityWrapper.DOCUMENTS_FOLDER.length() + Constants.PROCESS_REMOTE_PREFIX.length();
        for (File file: files) {
            if (file.getAbsolutePath().matches("^" + ActivityWrapper.DOCUMENTS_FOLDER +
                    Constants.PROCESS_REMOTE_PREFIX + "+[0-9]*[0-9]\\" + Constants.PROCESS_RGBA_EXTENSION + "$")) {

                int fileIndex = Integer.parseInt(file.getAbsolutePath().substring(startIndex,
                        file.getAbsolutePath().lastIndexOf('.')));

                // Rename frame file from remote%d.rgba to remote%04d.rgba (needed to sort correctly)
                file.renameTo(new File(ActivityWrapper.DOCUMENTS_FOLDER +
                        Constants.PROCESS_REMOTE_PREFIX + String.format("%04d", fileIndex) +
                        Constants.PROCESS_RGBA_EXTENSION));
                ++remoteFrameCount;
            }
        }

        if (localFrameCount == remoteFrameCount)
            return localFrameCount; // No frame to remove

        //
        String prefix;
        int toRemove;
        if (localFrameCount > remoteFrameCount) {

            toRemove = localFrameCount / ((localFrameCount - remoteFrameCount) + 1);
            prefix = Constants.PROCESS_LOCAL_PREFIX;
        }
        else {

            toRemove = remoteFrameCount / ((remoteFrameCount - localFrameCount) + 1);
            prefix = Constants.PROCESS_REMOTE_PREFIX;
        }
        ++toRemove; // Avoid to remove the last frame file (do not remove one file more)

        frames = new File(ActivityWrapper.DOCUMENTS_FOLDER);
        files = frames.listFiles();
        Arrays.sort(files); // Sort frame files

        int removed = 0, frameCount = 0;
        for (File file: files) {
            if (file.getAbsolutePath().matches("^" + ActivityWrapper.DOCUMENTS_FOLDER + prefix +
                    "+[0-9]*[0-9]\\" + Constants.PROCESS_RGBA_EXTENSION + "$")) {

                ++frameCount;
                if ((frameCount % toRemove) == 0) {

                    file.delete(); // Remove file
                    ++removed;
                }
                else if (removed != 0) // Rename file in order to keep file index name valid
                    file.renameTo(new File(ActivityWrapper.DOCUMENTS_FOLDER + prefix +
                            String.format("%04d", frameCount - removed - 1) + Constants.PROCESS_RGBA_EXTENSION));
            }
        }
        return (frameCount - removed); // Return frame count result
    }
    public static boolean extractAudio(String file) {

        return ProcessThread.mGStreamer.launch("filesrc location=\"" + file + "\" ! decodebin" +
                " ! audioconvert ! wavenc ! filesink location=\"" + ActivityWrapper.DOCUMENTS_FOLDER +
                AUDIO_WAV_FILENAME + "\"");
    }
}
