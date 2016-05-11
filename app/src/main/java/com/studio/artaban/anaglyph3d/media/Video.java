package com.studio.artaban.anaglyph3d.media;

import android.graphics.Bitmap;
import android.os.Bundle;

import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.helpers.Storage;
import com.studio.artaban.anaglyph3d.process.ProcessThread;
import com.studio.artaban.anaglyph3d.process.configure.ContrastActivity;
import com.studio.artaban.anaglyph3d.transfer.BufferRequest;
import com.studio.artaban.anaglyph3d.transfer.ConnectRequest;
import com.studio.artaban.anaglyph3d.transfer.Connectivity;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
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
    public void mergeFrameFiles() {

        mProceedFrame = 0;
        mTotalFrame = 1;

        new Thread(new Runnable() {
            @Override
            public void run() {

                ////// Get local & remote frame count
                File frames = new File(ActivityWrapper.DOCUMENTS_FOLDER);
                File[] files = frames.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {

                        // BUG: Only +[0-9] regex is not matching! Not greedy by default !?! See below...
                        if (filename.matches("^" + Constants.PROCESS_LOCAL_PREFIX + "+[0-9]*[0-9]\\" +
                                Constants.PROCESS_RGBA_EXTENSION + "$"))
                            return true;

                        return false;
                    }
                });
                int localCount = files.length;

                frames = new File(ActivityWrapper.DOCUMENTS_FOLDER);
                files = frames.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {

                        if (filename.matches("^" + Constants.PROCESS_REMOTE_PREFIX + "+[0-9]*[0-9]\\" +
                                Constants.PROCESS_RGBA_EXTENSION + "$"))
                            return true;

                        return false;
                    }
                });
                int remoteCount = files.length;

                // Check if needed to set FPS matching
                if (localCount == remoteCount) {

                    mFrameCount = localCount;
                    mProceedFrame = mTotalFrame; // 1/1
                    return; // No frame to remove
                }

                ////// Remove the too many frames from video with bigger FPS
                final String prefix;
                float deleteLag = 1f; // Used to know which frame file to delete
                if (localCount > remoteCount) {
                    deleteLag -= remoteCount / (float)localCount;
                    prefix = Constants.PROCESS_LOCAL_PREFIX;
                    mFrameCount = remoteCount;
                }
                else {
                    deleteLag -= localCount / (float)remoteCount;
                    prefix = Constants.PROCESS_REMOTE_PREFIX;
                    mFrameCount = localCount;
                }

                frames = new File(ActivityWrapper.DOCUMENTS_FOLDER);
                files = frames.listFiles(new FilenameFilter() {
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

                ////// Rename & remove frame files according files removal above
                float deleteFlag = 0f;
                int removed = 0, frameCount = 0;
                for (File file: files) {

                    ++frameCount;
                    if ((int)deleteFlag > removed) {

                        file.delete(); // Remove file
                        ++removed;
                    }
                    else if (removed != 0) // Rename file in order to keep file index name valid
                        file.renameTo(new File(ActivityWrapper.DOCUMENTS_FOLDER + "/" + prefix +
                                String.format("%04d", frameCount - removed - 1) +
                                Constants.PROCESS_RGBA_EXTENSION));

                    deleteFlag += deleteLag;
                    ++mProceedFrame;
                }
            }
        }).start();
    }

    public static class ConvertData {

        public float contrast; // Configured contrast
        public float brightness; // Configured brightness
        public boolean local; // Flag to define on which frames to apply contrast & brightness

        public short offset; // Configured synchronization
        public boolean localSync; // Flag to define on which frames to apply synchronization

    }
    public void convertFrames(final ConvertData data) {

        mProceedFrame = 0;
        mTotalFrame = 1;

        new Thread(new Runnable() {
            @Override
            public void run() {

                // Define progress bounds
                mTotalFrame = mFrameCount + 1; // + 1 -> for removing files step (last operation)

                ////// Rename frame files to apply synchronization (if needed)
                if (data.offset > 0) {
                    mTotalFrame += mFrameCount - data.offset;

                    String framePath = ActivityWrapper.DOCUMENTS_FOLDER + "/" + ((data.localSync)?
                            Constants.PROCESS_LOCAL_PREFIX:Constants.PROCESS_REMOTE_PREFIX);
                    for (int i = 0; i < mFrameCount; ++i) {

                        File frame = new File(framePath + String.format("%04d", i) +
                                Constants.PROCESS_RGBA_EXTENSION);
                        if (data.offset > i)
                            frame.delete();
                        else
                            frame.renameTo(new File(framePath + String.format("%04d", i - data.offset) +
                                    Constants.PROCESS_RGBA_EXTENSION));

                        ++mProceedFrame;
                    }

                    // Apply conversion to synchronized frames
                    mFrameCount -= data.offset;
                }

                ////// Apply conversion on frame files
                int frameWidth = Settings.getInstance().getResolutionWidth();
                int frameHeight = Settings.getInstance().getResolutionHeight();

                Bitmap localBitmap = Bitmap.createBitmap(frameWidth, frameHeight, Bitmap.Config.ARGB_8888);
                Bitmap remoteBitmap = Bitmap.createBitmap(frameWidth, frameHeight, Bitmap.Config.ARGB_8888);

                boolean applyContrast =
                        (data.brightness > ContrastActivity.DEFAULT_BRIGHTNESS) ||
                        (data.brightness < ContrastActivity.DEFAULT_BRIGHTNESS) ||
                        (data.contrast > ContrastActivity.DEFAULT_CONTRAST) ||
                        (data.contrast < ContrastActivity.DEFAULT_CONTRAST); // ...compare float values

                byte[] buffer = new byte[localBitmap.getByteCount()];
                for (int i = 0; i < mFrameCount; ++i) {

                    ++mProceedFrame;
                    String fileIndex = String.format("%04d", i);

                    // Get local frame buffer
                    File localFile = new File(ActivityWrapper.DOCUMENTS_FOLDER + "/" +
                            Constants.PROCESS_LOCAL_PREFIX + fileIndex + Constants.PROCESS_RGBA_EXTENSION);
                    try {
                        if (new FileInputStream(localFile).read(buffer) != buffer.length)
                            throw new IOException();
                        localBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(buffer));
                    }
                    catch (IOException e) {
                        Logs.add(Logs.Type.F, "Failed to load RGBA file: " + localFile.getAbsolutePath());
                        continue;
                    }

                    // Get remote frame buffer
                    File remoteFile = new File(ActivityWrapper.DOCUMENTS_FOLDER + "/" +
                            Constants.PROCESS_REMOTE_PREFIX + fileIndex + Constants.PROCESS_RGBA_EXTENSION);
                    try {
                        if (new FileInputStream(remoteFile).read(buffer) != buffer.length)
                            throw new IOException();
                        remoteBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(buffer));
                    }
                    catch (IOException e) {
                        Logs.add(Logs.Type.F, "Failed to load RGBA file: " + remoteFile.getAbsolutePath());
                        continue;
                    }

                    // Apply contrast & brightness (if requested)
                    Bitmap bmpContrastLocal, bmpContrastRemote;
                    if (applyContrast) {

                        if (!data.local) {
                            // -> '!data.local' instead of 'data.local' coz a local frame on the remote
                            //    device == local frame on current device
                            bmpContrastLocal = ContrastActivity.applyContrastBrightness(localBitmap,
                                    data.contrast, data.brightness);
                            bmpContrastRemote = remoteBitmap;
                        }
                        else {
                            bmpContrastLocal = localBitmap;
                            bmpContrastRemote = ContrastActivity.applyContrastBrightness(remoteBitmap,
                                    data.contrast, data.brightness);
                        }
                    }
                    else {
                        bmpContrastLocal = localBitmap;
                        bmpContrastRemote = remoteBitmap;
                    }

                    // Merge frame buffer (according camera position)
                    // - Left camera will be blue
                    // - Right camera will be red
                    if (Settings.getInstance().mPosition) { // Here is the condition to define which
                                                            // camera to apply anaglyph color
                        // Swap local & remote bitmap frame
                        Bitmap swap = bmpContrastLocal;
                        bmpContrastLocal = bmpContrastRemote;
                        bmpContrastRemote = swap;
                    }
                    for (int height = 0; height < frameHeight; ++height) {
                        for (int width = 0; width < frameWidth; ++width) {

                            int pixel = (width + (height * frameWidth)) << 2;
                            int localPixel = bmpContrastLocal.getPixel(width, height);
                            int remotePixel = bmpContrastRemote.getPixel(width, height);

                            // Here is code to apply anaglyph color!
                            buffer[pixel + 0] = (byte)((localPixel  & 0x00ff0000) >> 16); // R
                            buffer[pixel + 1] = (byte)((remotePixel & 0x0000ff00) >> 8);  // G
                            buffer[pixel + 2] = (byte)(remotePixel  & 0x000000ff);        // B
                        }
                    }

                    // Save converted frame file
                    File syncFile = (data.localSync)? localFile:remoteFile;

                    try { new FileOutputStream(syncFile).write(buffer); }
                    catch (IOException e) {
                        Logs.add(Logs.Type.F, "Failed to save anaglyph frame file: " +
                                syncFile.getAbsolutePath());
                    }
                }

                ////// Remove remaining frame files
                Storage.removeFiles("^" + ((!data.localSync)?
                        Constants.PROCESS_LOCAL_PREFIX:Constants.PROCESS_REMOTE_PREFIX) +
                        "+[0-9]*[0-9]\\" + Constants.PROCESS_RGBA_EXTENSION + "$");

                ++mProceedFrame;

            }
        }).start();
    }

    //////
    public static boolean extractFramesRGBA(String file, Frame.Orientation orientation, String frames) {

        return ProcessThread.mGStreamer.launch("filesrc location=\"" + file + "\" ! decodebin" +
                        " ! videoflip method=" + orientation.getFlipMethod() + " ! videoconvert" +
                        " ! video/x-raw,format=RGBA ! multifilesink location=\"" + frames + "\"");
    }

    private static final String AUDIO_WAV_FILENAME = "/audio.wav";
    public static boolean extractAudio(String file) {

        return ProcessThread.mGStreamer.launch("filesrc location=\"" + file + "\" ! decodebin" +
                " ! audioconvert ! wavenc ! filesink location=\"" + ActivityWrapper.DOCUMENTS_FOLDER +
                AUDIO_WAV_FILENAME + "\"");
    }

    public static boolean makeAnaglyphVideo(boolean jpegStep, int frameCount, String files) {

        int frameWidth = Settings.getInstance().getResolutionWidth();
        int frameHeight = Settings.getInstance().getResolutionHeight();
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
