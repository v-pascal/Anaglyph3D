package com.studio.artaban.anaglyph3d.process;

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
import com.studio.artaban.anaglyph3d.transfer.Connectivity;
import com.studio.artaban.libGST.GstObject;

import java.io.BufferedOutputStream;
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

    public enum Step { CONTRAST, VIDEO, FRAMES, MAKE }
    private enum Status {

        ////// Contrast & brightness step: 4 status
        INITIALIZATION (R.string.status_initialize), // Initialize process (GStreamer)

        // Progress for each 1024 bytes packets...
        TRANSFER_PICTURE (R.string.status_transfer_raw), // Transfer picture (to remote device which is not the maker)
        WAIT_PICTURE (R.string.status_transfer_raw), // Wait until contrast & brightness picture has been received (to device which is not the maker)

        SAVE_PICTURE (R.string.status_save_raw), // Save raw picture into local file
        CONVERT_PICTURE (R.string.status_convert_raw), // Convert local picture from NV21 to ARGB

        ////// Video transfer & extraction step: 7 status

        // Progress for each 1024 bytes packets...
        TRANSFER_VIDEO (R.string.status_transfer_video), // Transfer video
        WAIT_VIDEO (R.string.status_transfer_video), // Wait until video has been received

        EXTRACT_FRAMES_LEFT(0), // Extract ARGB pictures from left camera
        EXTRACT_FRAMES_RIGHT(0), // Extract ARGB pictures from right camera
        EXTRACT_AUDIO(0), // Extract audio from one of the videos
        MERGE_FPS(0), // Remove the too many RGB pictures from camera video with bigger FPS

        TRANSFER_CONTRAST (R.string.status_transfer_contrast), // Transfer the contrast & brightness (from device which is not the maker)
        WAIT_CONTRAST (R.string.status_wait_contrast), // Wait until contrast & brightness have been received

        ////// Make & transfer 3D video step: 3 status

        // Progress for each pictures extracted from videos...
        APPLY_FRAME_CHANGES(0), // Color frame (in blue or red), and apply contrast & brightness

        MAKE_3D_VIDEO(0), // Make the anaglyph 3D video

        // Progress for each 1024 bytes packets...
        TRANSFER_3D_VIDEO(0), // Transfer 3D video (to remote device which is not the maker)
        WAIT_3D_VIDEO(0); // Wait 3D video received

        //
        private final int mStringId;
        Status(int id) { mStringId = id; }
        public int getStringId() { return mStringId; }
    }
    private Step mStep = Step.CONTRAST;
    private Status mStatus = Status.INITIALIZATION;

    public static GstObject mGStreamer;

    //
    public class ProgressStatus {

        public String message;
        public int progress;
        public int max;
        public Step step;
        public boolean heavy;
    }

    private final ProgressStatus mProgress = new ProgressStatus();
    private void publishProgress(int progress, int max) {
        try {
            String status = ActivityWrapper.get().getResources().getString(mStatus.getStringId());
            boolean heavyProcess = false;

            switch (mStatus) {

                case WAIT_VIDEO:
                case TRANSFER_VIDEO:
                case WAIT_PICTURE:
                case TRANSFER_PICTURE: {

                    if ((progress != 0) || (max != 1)) {

                        status += " (" + progress + "/" + max + ")";
                        break;
                    }
                    //else // Do not display ' (0/1)' while waiting data transfer but indeterminate
                }

                // Heavy process
                case CONVERT_PICTURE:
                case EXTRACT_FRAMES_LEFT:
                case EXTRACT_FRAMES_RIGHT:
                case EXTRACT_AUDIO: {

                    heavyProcess = true; // Set indeterminate progress bar style
                    break;
                }
            }














            ((ProcessActivity) ActivityWrapper.get()).onUpdateProgress(status, progress, max,
                    mStep, heavyProcess);
        }
        catch (NullPointerException e) {
            Logs.add(Logs.Type.F, "Wrong activity reference");
        }
        catch (ClassCastException e) {
            Logs.add(Logs.Type.F, "Unexpected activity reference");
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
        boolean localPicture = true; // To define which picture to process (local or remote)

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
                    localPicture = true;
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

                            localPicture = false;
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

                    publishProgress(2 + ((localPicture)? 0:2), 4);

                    // Save NV21 local/remote raw picture file
                    try {
                        byte[] raw = (localPicture)? mPictureRaw:Frame.getInstance().getBuffer();
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
                        DisplayMessage.getInstance().alert(R.string.title_error, R.string.save_error,
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

                    publishProgress(3 + ((localPicture)? 0:2), 4);

                    // Convert NV21 to ARGB picture file
                    int width, height;
                    if (Settings.getInstance().mOrientation) { // Portrait

                        width = (localPicture)? mPictureSize.height:Frame.getInstance().getHeight();
                        height = (localPicture)? mPictureSize.width:Frame.getInstance().getWidth();
                    }
                    else { // Landscape

                        width = (localPicture)? mPictureSize.width:Frame.getInstance().getWidth();
                        height = (localPicture)? mPictureSize.height:Frame.getInstance().getHeight();
                    }
                    Frame.convertNV21toARGB(ActivityWrapper.DOCUMENTS_FOLDER + Constants.PROCESS_RAW_PICTURE_FILENAME,
                            width, height, ActivityWrapper.DOCUMENTS_FOLDER +
                                    ((localPicture)?
                                    Constants.PROCESS_LOCAL_PICTURE_FILENAME:
                                    Constants.PROCESS_REMOTE_PICTURE_FILENAME));

                    if (localPicture)
                        mStatus = Status.WAIT_PICTURE;

                    else {








                        //load Contrast fragment












                        mStatus = Status.TRANSFER_VIDEO;

                        // Load local video buffer







                        Bundle data = new Bundle();
                        data.putByteArray(Video.DATA_KEY_BUFFER, videoBuffer);

                        // Send download video request (upload to remote device)
                        Connectivity.getInstance().addRequest(Video.getInstance(),
                                Video.REQ_TYPE_DOWNLOAD, data);

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
                    if (Frame.getInstance().getTransferSize() == Frame.getInstance().getBufferSize()) {
                        if (!Settings.getInstance().isMaker()) {










                        }
                        else { // Maker











                        }
                    }
                    break;
                }
            }
            mAbort = !Connectivity.getInstance().isConnected();
        }
        Logs.add(Logs.Type.I, "Process thread stopped");
    }
}
