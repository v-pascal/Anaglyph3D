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
import com.studio.artaban.anaglyph3d.helpers.Storage;
import com.studio.artaban.anaglyph3d.media.Frame;
import com.studio.artaban.anaglyph3d.media.Video;
import com.studio.artaban.anaglyph3d.process.configure.ContrastActivity;
import com.studio.artaban.anaglyph3d.process.configure.SynchroActivity;
import com.studio.artaban.anaglyph3d.transfer.ConnectRequest;
import com.studio.artaban.anaglyph3d.transfer.Connectivity;
import com.studio.artaban.libGST.GstObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by pascal on 23/04/16.
 * Process thread (runnable thread)
 */
public class ProcessThread extends Thread {

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
        FRAMES_CONVERSION (R.string.status_frames_conversion), // Color frame (in blue or red), and apply contrast & brightness

        ////// Make & transfer 3D video step
        MAKE_3D_VIDEO (R.string.status_make_anaglyph), // Make the anaglyph 3D video
        TRANSFER_3D_VIDEO (R.string.status_transfer_anaglyph), // Transfer 3D video (to remote device which is not the maker)
        WAIT_3D_VIDEO (R.string.status_wait_anaglyph); // Wait 3D video received

        //
        private final int stringId;
        Status(int id) { stringId = id; }
        public int getStringId() { return stringId; }
    }
    private Step mStep = Step.CONTRAST;
    private Status mStatus = Status.INITIALIZATION;

    private static boolean mConfigured = false; // Flag to know if user has configured the contrast & brightness

    private static float mContrast = ContrastActivity.DEFAULT_CONTRAST; // Contrast configured by the user
    private static float mBrightness = ContrastActivity.DEFAULT_BRIGHTNESS; // Brightness configured by the user
    private static boolean mLocalFrame = ContrastActivity.DEFAULT_LOCAL_FRAME; // Flag to know on which frames to apply contrast

    private short mSynchroOffset = 0; // Synchronization offset configured by the user
    private boolean mLocalSync = true; // To define from which video to extract the audio (after synchronization)

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
        mSynchroOffset = offset;
        mLocalSync = local;

        mStatus = Status.EXTRACT_AUDIO;
    }
    public static void applyContrastBrightness(float contrast, float brightness, boolean local) {
        mContrast = contrast;
        mBrightness = brightness;
        mLocalFrame = local;

        mConfigured = true;
    }

    //////
    private static ContrastTransfer mTransfer;
    public static ContrastTransfer getInstance() {
        if (mTransfer == null)
            mTransfer = new ContrastTransfer();

        return mTransfer;
    }
    private static class ContrastTransfer implements ConnectRequest {

        @Override public char getRequestId() { return ConnectRequest.REQ_PROCESS; }
        @Override public boolean getRequestMerge() { return false; }
        @Override public BufferType getRequestBuffer(byte type) { return BufferType.NONE; }
        @Override public short getMaxWaitReply(byte type) { return Constants.CONN_MAXWAIT_DEFAULT; }
        @Override
        public String getRequest(byte type, Bundle data) {
            try {

                JSONObject request = new JSONObject();
                request.put(ContrastActivity.DATA_KEY_CONTRAST, mContrast);
                request.put(ContrastActivity.DATA_KEY_BRIGHTNESS, mBrightness);
                request.put(ContrastActivity.DATA_KEY_LOCAL, mLocalFrame);

                return request.toString();
            }
            catch (JSONException e) {
                Logs.add(Logs.Type.E, e.getMessage());
            }
            return null;
        }
        @Override
        public String getReply(byte type, String request, PreviousMaster previous) {
            try {

                JSONObject config = new JSONObject(request);
                applyContrastBrightness((float)config.getDouble(ContrastActivity.DATA_KEY_CONTRAST),
                        (float)config.getDouble(ContrastActivity.DATA_KEY_BRIGHTNESS),
                        config.getBoolean(ContrastActivity.DATA_KEY_LOCAL));

                return Constants.CONN_REQUEST_ANSWER_TRUE;
            }
            catch (JSONException e) {
                Logs.add(Logs.Type.E, e.getMessage());
            }
            return null;
        }

        @Override
        public ReceiveResult receiveReply(byte type, String reply) {
            return (reply.equals(Constants.CONN_REQUEST_ANSWER_TRUE))?
                    ReceiveResult.SUCCESS:ReceiveResult.ERROR;
        }
        @Override public ReceiveResult receiveBuffer(ByteArrayOutputStream buffer) {
            return ReceiveResult.ERROR; // Unexpected call
        }
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

                    case FRAMES_CONVERSION:
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

                    // Heavy process or undefined duration process
                    case WAIT_3D_VIDEO:

                    case WAIT_CONTRAST:
                    case TRANSFER_CONTRAST:

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
        int frameCount = 0;
        boolean local = true; // To define which picture/video to process (local or remote)
                              // ...and more
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
                    mConfigured = false;
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
                                Storage.FILENAME_RAW_PICTURE);

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

                    if (!Frame.convertNV21toRGBA(ActivityWrapper.DOCUMENTS_FOLDER +
                                    Storage.FILENAME_RAW_PICTURE,
                            width, height, ActivityWrapper.DOCUMENTS_FOLDER +
                                    ((local) ?
                                    Storage.FILENAME_LOCAL_PICTURE :
                                    Storage.FILENAME_REMOTE_PICTURE), getOrientation())) {

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

                        // Load contrast activity //////////////////////////////////////////////////
                        Bundle data = new Bundle();
                        data.putInt(Frame.DATA_KEY_WIDTH, mPictureSize.width);
                        data.putInt(Frame.DATA_KEY_HEIGHT, mPictureSize.height);

                        ActivityWrapper.startActivity(ContrastActivity.class, data, 0);

                        //////
                        mStep = Step.VIDEO;
                        mStatus = Status.TRANSFER_VIDEO;

                        // Send local video file to remote device
                        if (!Video.sendFile(ActivityWrapper.DOCUMENTS_FOLDER +
                                Storage.FILENAME_LOCAL_VIDEO)) {

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
                        else
                            publishProgress(Video.getInstance().getTransferSize(),
                                    Video.getInstance().getBufferSize());
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
                case TRANSFER_CONTRAST:
                case WAIT_CONTRAST: { // Wait contrast configuration or transfer for the maker

                    sleep();
                    publishProgress(0, 1);

                    if (mStatus == Status.TRANSFER_CONTRAST) {
                        if (Settings.getInstance().isMaker()) {

                            // Contrast & brightness configuration has been received
                            Video.ConvertData data = new Video.ConvertData();

                            data.contrast = mContrast;
                            data.brightness = mBrightness;
                            data.local = mLocalFrame;
                            data.offset = mSynchroOffset;
                            data.localSync = mLocalSync;
                            data.count = frameCount;

                            Video.getInstance().convertFrames(data);
                            mStatus = Status.FRAMES_CONVERSION;
                        }
                        else {

                            // Send contrast & brightness configuration to remote device
                            Connectivity.getInstance().addRequest(mTransfer, (byte)0, null);

                            //////
                            mStep = Step.MAKE;
                            mStatus = Status.WAIT_3D_VIDEO;
                            publishProgress(0, 1);
                        }
                    }
                    //else {

                    // IF Maker
                    // Wait 'applyContrastBrightness' call from connectivity thread (once received)
                    // ELSE
                    // Wait contrast configuration (wait 'applyContrastBrightness' call from process activity)

                    // ...Nothing else to do (status will be updated 'applyContrastBrightness' via code below)
                    // }
                    if (mConfigured) {
                        mConfigured = false;
                        mStatus = Status.TRANSFER_CONTRAST;
                    }
                    break;
                }
                case SAVE_VIDEO: {

                    sleep();
                    publishProgress(0, 1);

                    // Save transferred video file
                    try {
                        byte[] raw = Video.getInstance().getBuffer();
                        File videoFile = new File(ActivityWrapper.DOCUMENTS_FOLDER,
                                Storage.FILENAME_REMOTE_VIDEO);

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
                                    Storage.FILENAME_LOCAL_VIDEO:
                                    Storage.FILENAME_REMOTE_VIDEO),
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
                        mStep = Step.FRAMES;
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

                                // Load synchronization activity ///////////////////////////////////
                                Bundle data = new Bundle();
                                frameCount = Video.getInstance().getFrameCount();
                                data.putInt(SynchroActivity.DATA_KEY_FRAME_COUNT, frameCount);

                                ActivityWrapper.startActivity(SynchroActivity.class, data, 0);

                                //////
                                mStatus = Status.WAIT_SYNCHRO;
                                publishProgress(0, 1);
                                break;
                            }
                        }
                    }
                    break;
                }
                case WAIT_SYNCHRO: { // Wait synchro configuration (maker only)

                    sleep();
                    break;
                }
                case EXTRACT_AUDIO: {

                    publishProgress(0, 1);

                    if (!Video.extractAudio((!mLocalSync)? // Extract sound from '!mLocalSync' coz
                            Storage.FILENAME_LOCAL_VIDEO: // if shift frames from local video, remote
                            Storage.FILENAME_REMOTE_VIDEO)) { // video sound will be synchronized!

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
                    mStatus = Status.WAIT_CONTRAST;
                    break;
                }
                case FRAMES_CONVERSION: {

                    sleep();
                    publishProgress(Video.getInstance().getProceedFrame(),
                            Video.getInstance().getTotalFrame());

                    //////
                    if (Video.getInstance().getProceedFrame() == Video.getInstance().getTotalFrame()) {

                        local = true; // 'jpegStep' step
                        mStep = Step.MAKE;
                        mStatus = Status.MAKE_3D_VIDEO;
                    }
                    break;
                }
                case MAKE_3D_VIDEO: {

                    publishProgress(0, 1);
                    if (!Video.makeAnaglyphVideo(local, frameCount - mSynchroOffset,
                            ActivityWrapper.DOCUMENTS_FOLDER + ((mLocalSync)?
                            Constants.PROCESS_LOCAL_FRAMES_FILENAME:
                            Constants.PROCESS_REMOTE_FRAMES_FILENAME))) {

                        Logs.add(Logs.Type.E, "Failed to make anaglyph video");
                        mAbort = true;

                        // Inform user
                        DisplayMessage.getInstance().alert(R.string.title_error, R.string.error_make_video,
                                null, false, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ActivityWrapper.stopActivity(ProcessActivity.class,
                                                Constants.NO_DATA);
                                    }
                                });
                        break;
                    }
                    if (local) { // Check 'jpegStep' (first step)
                        local = false; // ...make final video (last step)
                        break;
                    }

                    //////
                    mStatus = Status.TRANSFER_3D_VIDEO;

                    // Send 3D video to remote device
                    if (!Video.sendFile(ActivityWrapper.DOCUMENTS_FOLDER + Storage.FILENAME_3D_VIDEO)) {

                        Logs.add(Logs.Type.E, "Failed to load 3D anaglyph video file");
                        mAbort = true;

                        // Inform user
                        DisplayMessage.getInstance().alert(R.string.title_error, R.string.error_load_anaglyph,
                                null, false, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ActivityWrapper.stopActivity(ProcessActivity.class,
                                                Constants.NO_DATA);
                                    }
                                });
                    }
                    else
                        publishProgress(Video.getInstance().getTransferSize(),
                                Video.getInstance().getBufferSize());
                    break;
                }
                case WAIT_3D_VIDEO: { // Wait 3D video transfer (receiving)

                    sleep();
                    if (Video.getInstance().getTransferSize() != Video.getInstance().getBufferSize())
                        mStatus = Status.TRANSFER_3D_VIDEO;
                        // This must happen once at least (a video cannot be downloaded during one 'sleep')

                    break;
                }
                case TRANSFER_3D_VIDEO: {

                    sleep();
                    publishProgress(Video.getInstance().getTransferSize(),
                            Video.getInstance().getBufferSize());

                    //////
                    if (Video.getInstance().getTransferSize() == Video.getInstance().getBufferSize()) {

















                    }
                    break;
                }
            }
            if (!mAbort)
                mAbort = !Connectivity.getInstance().isConnected();
        }
        Logs.add(Logs.Type.I, "Process thread stopped");
    }
}
