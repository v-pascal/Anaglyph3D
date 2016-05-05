package com.studio.artaban.anaglyph3d.process;

import android.app.Activity;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.os.Bundle;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.media.Frame;
import com.studio.artaban.anaglyph3d.media.Video;
import com.studio.artaban.anaglyph3d.process.configure.ContrastActivity;
import com.studio.artaban.anaglyph3d.process.configure.SynchroActivity;
import com.studio.artaban.anaglyph3d.transfer.Connectivity;
import com.studio.artaban.libGST.GstObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by pascal on 23/04/16.
 * Process thread (runnable thread)
 */
public class ProcessThread extends Thread {

    public static final String DATA_KEY_FRAME_COUNT = "frameCount";
    // Data keys

    public ProcessThread(Camera.Size size, byte[] raw) {
        mPictureSize = size;
        mPictureRaw = raw;
    }

    //////
    private final Camera.Size mPictureSize;
    private final byte[] mPictureRaw;

    private volatile boolean mAbort = false;
    public void release() { // Stop thread

        mAbort = true;

        try { join(); }
        catch (InterruptedException e) {
            Logs.add(Logs.Type.E, e.getMessage());
        }
    }

    public enum Step {

        CONTRAST, // Contrast & brightness
        VIDEO, // Video transfer & extraction
        FRAMES, // Frames conversion
        MAKE // Make & transfer 3D video
    }
    private enum Status {

        ////// Contrast & brightness step
        INITIALIZATION (R.string.status_initialize), // Initialize process (GStreamer)
        TRANSFER_PICTURE (R.string.status_transfer_raw), // Transfer picture (to remote device which is not the maker)
        WAIT_PICTURE (R.string.status_transfer_raw), // Wait until contrast & brightness picture has been received (to device which is not the maker)
        SAVE_PICTURE (R.string.status_save_raw), // Save raw picture into local file
        CONVERT_PICTURE (R.string.status_convert_raw), // Convert local picture from NV21 to ARGB

        ////// Video transfer & extraction step
        TRANSFER_VIDEO (R.string.status_transfer_video), // Transfer video
        WAIT_VIDEO (R.string.status_transfer_video), // Wait until video has been received
        SAVE_VIDEO (R.string.status_save_video), // Save remote video
        EXTRACT_FRAMES_LEFT (R.string.status_extract_left), // Extract ARGB pictures from left camera
        EXTRACT_FRAMES_RIGHT (R.string.status_extract_right), // Extract ARGB pictures from right camera
        RENAME_FRAMES_LEFT (R.string.status_rename_left), // Rename left frame files
        RENAME_FRAMES_RIGHT (R.string.status_rename_right), // Rename right frame files
        MERGE_FPS (R.string.status_merge_fps), // Remove the too many RGB pictures from camera video with bigger FPS
        WAIT_SYNCHRO (Constants.NO_DATA), // Wait synchro configuration
        EXTRACT_AUDIO (R.string.status_extract_audio), // Extract audio from one of the videos
        TRANSFER_CONTRAST (R.string.status_transfer_contrast), // Transfer the contrast & brightness (from device which is not the maker)
        WAIT_CONTRAST (R.string.status_wait_contrast), // Wait until contrast & brightness has been received

        ////// Frames conversion step
        FRAMES_CONVERSION(R.string.status_frames_conversion), // Color frame (in blue or red), and apply contrast & brightness

        ////// Make & transfer 3D video step
        MAKE_3D_VIDEO(0), // Make the anaglyph 3D video
        TRANSFER_3D_VIDEO(0), // Transfer 3D video (to remote device which is not the maker)
        WAIT_3D_VIDEO(0); // Wait 3D video received

        //
        private final int stringId;
        Status(int id) { stringId = id; }
        public int getStringId() { return stringId; }
    }
    private Step mStep = Step.CONTRAST;
    private volatile Status mStatus = Status.INITIALIZATION;

    private boolean mLocalAudio = true; // To define from which video to extract the audio (after synchronization)
    private float mContrast = 1; // Contrast configured by the user
    private float mBrightness = 0; // Brightness configured by the user

    private Frame.Orientation getOrientation() { // Return frame orientation according settings

        if (Settings.getInstance().mOrientation) // Portrait
            return (Settings.getInstance().mReverse)?
                    Frame.Orientation.REVERSE_PORTRAIT: Frame.Orientation.PORTRAIT;
        else // Landscape
            return (Settings.getInstance().mReverse)?
                    Frame.Orientation.REVERSE_LANDSCAPE: Frame.Orientation.LANDSCAPE;
    }

    public static GstObject mGStreamer; // GStreamer object used to manipulate pictures & videos

    //////
    public void applySynchronization(short offset, boolean local) {









        // apply configuration here










        //////
        mLocalAudio = local;
        mStatus = Status.EXTRACT_AUDIO;
    }
    public void applyContrastBrightness(float contrast, float brightness) {

        mContrast = contrast;
        mBrightness = brightness;
    }

    //
    public static class ProgressStatus {

        public String message = "";
        public int progress = 0;
        public int max = 1;
        public Step step = Step.CONTRAST;
        public boolean heavy = false;
    }
    public static final ProgressStatus mProgress = new ProgressStatus();

    private void publishProgress(int progress, int max) {
        try {
            synchronized (mProgress) {

                mProgress.message = ActivityWrapper.get().getResources().getString(mStatus.getStringId());
                mProgress.heavy = false;
                mProgress.max = max;
                mProgress.progress = progress;
                mProgress.step = mStep;

                switch (mStatus) {

                    case RENAME_FRAMES_LEFT:
                    case RENAME_FRAMES_RIGHT:
                    case MERGE_FPS:

                    case WAIT_VIDEO:
                    case TRANSFER_VIDEO:
                    case WAIT_PICTURE:
                    case TRANSFER_PICTURE: {

                        if ((progress != 0) || (max != 1)) {

                            mProgress.message += " (" + progress + "/" + max + ")";
                            break;
                        }
                        //else // Do not display ' (0/1)' while waiting data transfer but...
                        //break; // ...indeterminate below
                    }

                    // Heavy process
                    case CONVERT_PICTURE:
                    case SAVE_VIDEO:
                    case EXTRACT_FRAMES_LEFT:
                    case EXTRACT_FRAMES_RIGHT:
                    case EXTRACT_AUDIO: {

                        mProgress.heavy = true; // Set indeterminate progress bar style
                        break;
                    }
                }
            }

            Activity curActivity = ActivityWrapper.get();

            // Update UI progress status (if possible)
            if (curActivity.getClass().equals(ProcessActivity.class))
                ((ProcessActivity) curActivity).onUpdateProgress();
        }
        catch (NullPointerException e) {
            Logs.add(Logs.Type.F, "Wrong activity reference");
        }
    }

    private void sleep() { // Sleep in process loop

        try { Thread.sleep(Constants.PROCESS_WAIT_TRANSFER, 0); }
        catch (InterruptedException e) {
            Logs.add(Logs.Type.W, "Unable to sleep: " + e.getMessage());
        }
    }

    //////
    @Override
    public void run() {

        Logs.add(Logs.Type.V, "Process thread started");
        boolean local = true; // To define which picture/video to process (local or remote)

        while (!mAbort) {
            switch (mStatus) {

                case INITIALIZATION: {

                    publishProgress(1, (Settings.getInstance().isMaker())? 2:4);

                    // Initialize GStreamer library (on UI thread)
                    try { ((ProcessActivity)ActivityWrapper.get()).onInitialize(); }
                    catch (NullPointerException e) {

                        Logs.add(Logs.Type.F, "Wrong activity reference");
                        mAbort = true;
                        break;
                    }
                    catch (ClassCastException e) {

                        Logs.add(Logs.Type.F, "Unexpected activity reference");
                        mAbort = true;
                        break;
                    }

                    //////
                    local = true;
                    if (!Settings.getInstance().isMaker())
                        mStatus = Status.SAVE_PICTURE;

                    else { // Maker

                        mStatus = Status.TRANSFER_PICTURE;

                        Bundle data = new Bundle();
                        data.putInt(Frame.DATA_KEY_WIDTH, mPictureSize.width);
                        data.putInt(Frame.DATA_KEY_HEIGHT, mPictureSize.height);
                        data.putByteArray(Frame.DATA_KEY_BUFFER, mPictureRaw);

                        // Send download picture request (upload to remote device)
                        Connectivity.getInstance().addRequest(Frame.getInstance(),
                                Frame.REQ_TYPE_DOWNLOAD, data);

                        publishProgress(Frame.getInstance().getTransferSize(),
                                Frame.getInstance().getBufferSize());
                    }
                    break;
                }
                case WAIT_PICTURE:
                case TRANSFER_PICTURE: {

                    sleep();
                    publishProgress(Frame.getInstance().getTransferSize(),
                            Frame.getInstance().getBufferSize());

                    //////
                    if (Frame.getInstance().getTransferSize() == Frame.getInstance().getBufferSize()) {
                        if (!Settings.getInstance().isMaker()) {

                            local = false;
                            mStatus = Status.SAVE_PICTURE;
                        }
                        else { // Maker

                            mStep = Step.VIDEO;
                            mStatus = Status.WAIT_VIDEO;
                        }
                    }
                    break;
                }

                ////// Called twice: for both local and remote pictures
                case SAVE_PICTURE: {

                    publishProgress(2 + ((local)? 0:2), 4);

                    // Save NV21 local/remote raw picture file
                    try {
                        byte[] raw = (local)? mPictureRaw:Frame.getInstance().getBuffer();
                        File rawFile = new File(ActivityWrapper.DOCUMENTS_FOLDER,
                                Constants.PROCESS_RAW_PICTURE_FILENAME);

                        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(rawFile));
                        bos.write(raw);
                        bos.flush();
                        bos.close();

                        mStatus = Status.CONVERT_PICTURE;
                    }
                    catch (IOException e) {

                        Logs.add(Logs.Type.E, "Failed to save raw picture");
                        mAbort = true;

                        // Inform user
                        DisplayMessage.getInstance().alert(R.string.title_error, R.string.error_save_picture,
                                null, false, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ActivityWrapper.stopActivity(ProcessActivity.class,
                                                Constants.NO_DATA);
                                    }
                                });
                    }
                    break;
                }
                case CONVERT_PICTURE: {

                    publishProgress(3 + ((local)? 0:2), 4);

                    // Convert NV21 to RGBA picture file

                    int width = (local)? mPictureSize.width:Frame.getInstance().getWidth();
                    int height = (local)? mPictureSize.height:Frame.getInstance().getHeight();
                    // -> Raw picture always in landscape orientation

                    if (!Frame.convertNV21toRGBA(ActivityWrapper.DOCUMENTS_FOLDER + Constants.PROCESS_RAW_PICTURE_FILENAME,
                            width, height, ActivityWrapper.DOCUMENTS_FOLDER +
                                    ((local) ?
                                            Constants.PROCESS_LOCAL_PICTURE_FILENAME :
                                            Constants.PROCESS_REMOTE_PICTURE_FILENAME), getOrientation())) {

                        Logs.add(Logs.Type.E, "Failed to convert contrast picture");
                        mAbort = true;

                        // Inform user
                        DisplayMessage.getInstance().alert(R.string.title_error, R.string.error_convert_contrast,
                                null, false, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ActivityWrapper.stopActivity(ProcessActivity.class,
                                                Constants.NO_DATA);
                                    }
                                });
                        break;
                    }
                    if (local)
                        mStatus = Status.WAIT_PICTURE;

                    else {

                        // Load contrast activity
                        Bundle data = new Bundle();
                        data.putInt(Frame.DATA_KEY_WIDTH, mPictureSize.width);
                        data.putInt(Frame.DATA_KEY_HEIGHT, mPictureSize.height);

                        ActivityWrapper.startActivity(ContrastActivity.class, data, 0);

                        //////
                        mStatus = Status.TRANSFER_VIDEO;

                        try {
                            // Load local video buffer
                            File videoFile = new File(ActivityWrapper.DOCUMENTS_FOLDER,
                                    Constants.PROCESS_LOCAL_VIDEO_FILENAME);
                            byte[] videoBuffer = new byte[(int)videoFile.length()];
                            if (new FileInputStream(videoFile).read(videoBuffer) != videoBuffer.length)
                                throw new IOException();

                            data = new Bundle();
                            data.putByteArray(Video.DATA_KEY_BUFFER, videoBuffer);

                            // Send download video request (upload to remote device)
                            Connectivity.getInstance().addRequest(Video.getInstance(),
                                    Video.REQ_TYPE_DOWNLOAD, data);

                            publishProgress(Video.getInstance().getTransferSize(),
                                    Video.getInstance().getBufferSize());
                        }
                        catch (IOException e) {

                            Logs.add(Logs.Type.E, "Failed to load video file");
                            mAbort = true;

                            // Inform user
                            DisplayMessage.getInstance().alert(R.string.title_error, R.string.error_load_video,
                                    null, false, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            ActivityWrapper.stopActivity(ProcessActivity.class,
                                                    Constants.NO_DATA);
                                        }
                                    });
                        }
                    }
                    break;
                }
                //////

                case TRANSFER_VIDEO:
                case WAIT_VIDEO: {

                    sleep();
                    publishProgress(Video.getInstance().getTransferSize(),
                            Video.getInstance().getBufferSize());

                    //////
                    if (Video.getInstance().getTransferSize() == Video.getInstance().getBufferSize())
                        mStatus = (Settings.getInstance().isMaker())? Status.SAVE_VIDEO:Status.WAIT_CONTRAST;
                    break;
                }
                case WAIT_CONTRAST: // Wait contrast transfer or configuration
                case TRANSFER_CONTRAST: {








                    sleep();
                    publishProgress(0, 1);








                    break;
                }
                case SAVE_VIDEO: {

                    sleep();
                    publishProgress(0, 1);

                    // Save transferred video file
                    try {
                        byte[] raw = Video.getInstance().getBuffer();
                        File videoFile = new File(ActivityWrapper.DOCUMENTS_FOLDER,
                                Constants.PROCESS_REMOTE_VIDEO_FILENAME);

                        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(videoFile));
                        bos.write(raw);
                        bos.flush();
                        bos.close();

                        local = true;
                        mStatus = Status.EXTRACT_FRAMES_LEFT;
                    }
                    catch (IOException e) {

                        Logs.add(Logs.Type.E, "Failed to save remote video");
                        mAbort = true;

                        // Inform user
                        DisplayMessage.getInstance().alert(R.string.title_error, R.string.error_save_video,
                                null, false, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ActivityWrapper.stopActivity(ProcessActivity.class,
                                                Constants.NO_DATA);
                                    }
                                });
                    }
                    break;
                }
                case EXTRACT_FRAMES_LEFT:
                case EXTRACT_FRAMES_RIGHT: {

                    sleep();
                    publishProgress(0, 1);

                    if (!Video.extractFramesRGBA(ActivityWrapper.DOCUMENTS_FOLDER + ((local)?
                                    Constants.PROCESS_LOCAL_VIDEO_FILENAME:
                                    Constants.PROCESS_REMOTE_VIDEO_FILENAME),
                            getOrientation(), ActivityWrapper.DOCUMENTS_FOLDER + ((local)?
                                    Constants.PROCESS_LOCAL_FRAMES_FILENAME:
                                    Constants.PROCESS_REMOTE_FRAMES_FILENAME))) {

                        Logs.add(Logs.Type.E, "Failed to extract video frames");
                        mAbort = true;

                        // Inform user
                        DisplayMessage.getInstance().alert(R.string.title_error, R.string.error_extract_frames,
                                null, false, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ActivityWrapper.stopActivity(ProcessActivity.class,
                                                Constants.NO_DATA);
                                    }
                                });
                        break;
                    }

                    //////
                    if (local) {

                        local = false; // Extract right video frames
                        mStatus = Status.EXTRACT_FRAMES_RIGHT;
                    }
                    else {

                        Video.getInstance().renameFrameFiles(true);
                        mStatus = Status.RENAME_FRAMES_LEFT;
                    }
                    break;
                }
                case RENAME_FRAMES_LEFT:
                case RENAME_FRAMES_RIGHT:
                case MERGE_FPS: {

                    sleep();
                    publishProgress(Video.getInstance().getProceedFrame(),
                            Video.getInstance().getTotalFrame());

                    //////
                    if (Video.getInstance().getProceedFrame() == Video.getInstance().getTotalFrame()) {
                        switch (mStatus) {

                            case RENAME_FRAMES_LEFT: {

                                Video.getInstance().renameFrameFiles(false);
                                mStatus = Status.RENAME_FRAMES_RIGHT;
                                break;
                            }
                            case RENAME_FRAMES_RIGHT: {

                                Video.getInstance().mergeFrameFiles();
                                mStatus = Status.MERGE_FPS;
                                break;
                            }
                            case MERGE_FPS: {

                                // Load synchronization activity
                                Bundle data = new Bundle();
                                data.putInt(DATA_KEY_FRAME_COUNT, Video.getInstance().getFrameCount());

                                ActivityWrapper.startActivity(SynchroActivity.class, data, 0);

                                //////
                                mStatus = Status.WAIT_SYNCHRO;
                                break;
                            }
                        }
                    }
                    break;
                }
                case WAIT_SYNCHRO: {

                    sleep();
                    break;
                }
                case EXTRACT_AUDIO: {

                    sleep();
                    publishProgress(0, 1);

                    if (!Video.extractAudio((mLocalAudio)?
                            Constants.PROCESS_LOCAL_VIDEO_FILENAME:
                            Constants.PROCESS_REMOTE_VIDEO_FILENAME)) {

                        Logs.add(Logs.Type.E, "Failed to extract video audio");
                        mAbort = true;

                        // Inform user
                        DisplayMessage.getInstance().alert(R.string.title_error, R.string.error_extract_audio,
                                null, false, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ActivityWrapper.stopActivity(ProcessActivity.class,
                                                Constants.NO_DATA);
                                    }
                                });
                        break;
                    }

                    //////
                    mStatus = Status.FRAMES_CONVERSION;
                    break;
                }
                case FRAMES_CONVERSION: {









                    sleep();
                    publishProgress(0, 1);










                    break;
                }
            }
            if (!mAbort)
                mAbort = !Connectivity.getInstance().isConnected();
        }
        Logs.add(Logs.Type.I, "Process thread stopped");
    }
}
