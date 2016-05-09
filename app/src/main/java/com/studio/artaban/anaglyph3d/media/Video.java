package com.studio.artaban.anaglyph3d.media;

import android.os.Bundle;

import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.Storage;
import com.studio.artaban.anaglyph3d.process.ProcessThread;
import com.studio.artaban.anaglyph3d.transfer.BufferRequest;
import com.studio.artaban.anaglyph3d.transfer.ConnectRequest;
import com.studio.artaban.anaglyph3d.transfer.Connectivity;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
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
    private int mTotalFrame = 1;
    private int mProceedFrame = 0;

    private int mFrameCount = 0;

    public int getTotalFrame() { return mTotalFrame; }
    public int getProceedFrame() { return mProceedFrame; }

    public int getFrameCount() { return mFrameCount; }

    //
    private int mLocalCount = 0;
    private int mRemoteCount = 0;

    public void renameFrameFiles(final boolean local) {

        mProceedFrame = 0;
        mTotalFrame = 1;

        new Thread(new Runnable() {
            @Override
            public void run() {

                ////// Count frame files
                File frames = new File(ActivityWrapper.DOCUMENTS_FOLDER);
                File[] files = frames.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {

                        // BUG: Only +[0-9] regex is not matching! Not greedy by default !?! See below...
                        if (filename.matches("^" + ((local)?
                                Constants.PROCESS_LOCAL_PREFIX:
                                Constants.PROCESS_REMOTE_PREFIX) + "+[0-9]*[0-9]\\" +
                                Constants.PROCESS_RGBA_EXTENSION + "$"))
                            return true;

                        return false;
                    }
                });
                mTotalFrame = files.length;

                String prefix;
                if (local) {
                    mLocalCount = mTotalFrame;
                    prefix = Constants.PROCESS_LOCAL_PREFIX;
                }
                else {
                    mRemoteCount = mTotalFrame;
                    prefix = Constants.PROCESS_REMOTE_PREFIX;
                }
                int startIndex = ActivityWrapper.DOCUMENTS_FOLDER.length() + 1 + prefix.length();
                                                                        // + 1 -> "/"
                ////// Rename frame files
                for (File file: files) {
                    int fileIndex = Integer.parseInt(file.getAbsolutePath().substring(startIndex,
                            file.getAbsolutePath().lastIndexOf('.')));

                    // Rename frame file from local%d.rgba to local%04d.rgba (needed to sort files correctly)
                    file.renameTo(new File(ActivityWrapper.DOCUMENTS_FOLDER + "/" + prefix +
                            String.format("%04d", fileIndex) + Constants.PROCESS_RGBA_EXTENSION));

                    ++mProceedFrame;
                }
            }
        }).start();
    }
    public void mergeFrameFiles() {

        mProceedFrame = 0;
        mTotalFrame = 1;

        new Thread(new Runnable() {
            @Override
            public void run() {

                if (mLocalCount == mRemoteCount) {

                    mFrameCount = mLocalCount;
                    mProceedFrame = mTotalFrame; // 1/1
                    return; // No frame to remove
                }

                ////// Remove the too many frames from video with bigger FPS
                final String prefix;
                int toRemove;
                if (mLocalCount > mRemoteCount) {
                    toRemove = mLocalCount / ((mLocalCount - mRemoteCount) + 1);
                    prefix = Constants.PROCESS_LOCAL_PREFIX;
                }
                else {
                    toRemove = mRemoteCount / ((mRemoteCount - mLocalCount) + 1);
                    prefix = Constants.PROCESS_REMOTE_PREFIX;
                }
                ++toRemove; // Avoid to remove the last frame file (do not remove one file more)

                File frames = new File(ActivityWrapper.DOCUMENTS_FOLDER);
                File[] files = frames.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {

                        // BUG: Only +[0-9] regex is not matching! Not greedy by default !?! See below...
                        if (filename.matches("^" + prefix + "+[0-9]*[0-9]\\" +
                                Constants.PROCESS_RGBA_EXTENSION + "$"))
                            return true;

                        return false;
                    }
                });
                mTotalFrame = files.length;
                Arrays.sort(files); // Sort frame files

                int removed = 0, frameCount = 0;
                for (File file: files) {

                    ++frameCount;
                    if ((frameCount % toRemove) == 0) {

                        file.delete(); // Remove file
                        ++removed;
                    }
                    else if (removed != 0) // Rename file in order to keep file index name valid
                        file.renameTo(new File(ActivityWrapper.DOCUMENTS_FOLDER + "/" + prefix +
                                String.format("%04d", frameCount - removed - 1) +
                                Constants.PROCESS_RGBA_EXTENSION));

                    ++mProceedFrame;
                }
                mFrameCount = (frameCount - removed);
            }
        }).start();
    }
    public void convertFrames(final float contrast, final float brightness,
                              final short offset, final boolean local) {

        mProceedFrame = 0;
        mTotalFrame = 1;

        new Thread(new Runnable() {
            @Override
            public void run() {










                // Apply conversion on local files
                // Remove !local files











            }
        }).start();
    }

    //////
    private static final String AUDIO_WAV_FILENAME = "/audio.wav";

    public static boolean extractFramesRGBA(String file, Frame.Orientation orientation, String frames) {

        return ProcessThread.mGStreamer.launch("filesrc location=\"" + file + "\" ! decodebin" +
                        " ! videoflip method=" + orientation.getFlipMethod() + " ! videoconvert" +
                        " ! video/x-raw,format=RGBA ! multifilesink location=\"" + frames + "\"");
    }
    public static boolean extractAudio(String file) {

        return ProcessThread.mGStreamer.launch("filesrc location=\"" + file + "\" ! decodebin" +
                " ! audioconvert ! wavenc ! filesink location=\"" + ActivityWrapper.DOCUMENTS_FOLDER +
                AUDIO_WAV_FILENAME + "\"");
    }
    public static boolean makeAnaglyphVideo(boolean jpegStep, int frameCount, String files) {

        int frameWidth, frameHeight;
        if (Settings.getInstance().mOrientation) { // Portrait

            frameWidth = Settings.getInstance().mResolution.height;
            frameHeight = Settings.getInstance().mResolution.width;
        }
        else { // Landscape

            frameWidth = Settings.getInstance().mResolution.width;
            frameHeight = Settings.getInstance().mResolution.height;
        }
        if (jpegStep)
            return ProcessThread.mGStreamer.launch("multifilesrc location=\"" + files + "\" index=0" +
                    " caps=\"video/x-raw,format=RGBA,width=" + frameWidth + ",height=" + frameHeight +
                    ",framerate=1/1\" ! decodebin ! videoconvert ! jpegenc ! multifilesink" +
                    " location=\"" + ActivityWrapper.DOCUMENTS_FOLDER + "/img%d.jpg\"");
        else
            return ProcessThread.mGStreamer.launch("webmmux name=mux ! filesink" +
                    " location=\"" + ActivityWrapper.DOCUMENTS_FOLDER + Storage.FILENAME_3D_VIDEO +
                    "\" multifilesrc location=\"" + ActivityWrapper.DOCUMENTS_FOLDER + "/img%d.jpg\" index=0" +
                    " caps=\"image/jpeg,width=" + frameWidth + ",height=" + frameHeight +
                    ",framerate=" + frameCount + "/" + Settings.getInstance().mDuration +
                    "\" ! jpegdec ! videoconvert ! vp8enc ! queue ! mux.video_0 filesrc" +
                    " location=\"" + ActivityWrapper.DOCUMENTS_FOLDER + AUDIO_WAV_FILENAME +
                    "\" ! decodebin ! audioconvert ! vorbisenc ! queue ! mux.audio_0");
    }
    public static boolean sendFile(String file) {
        try {

            // Load video file buffer
            File videoFile = new File(file);
            byte[] videoBuffer = new byte[(int)videoFile.length()];
            if (new FileInputStream(videoFile).read(videoBuffer) != videoBuffer.length)
                return false;

            Bundle data = new Bundle();
            data.putByteArray(DATA_KEY_BUFFER, videoBuffer);

            // Send download video request (upload to remote device)
            Connectivity.getInstance().addRequest(getInstance(), REQ_TYPE_DOWNLOAD, data);
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }
}
