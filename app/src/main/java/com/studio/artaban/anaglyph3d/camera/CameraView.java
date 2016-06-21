package com.studio.artaban.anaglyph3d.camera;

import android.content.DialogInterface;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.content.Context;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.helpers.Storage;
import com.studio.artaban.anaglyph3d.process.ProcessActivity;
import com.studio.artaban.anaglyph3d.transfer.Connectivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by pascal on 22/03/16.
 * Camera helper
 */
public class CameraView extends SurfaceView
        implements SurfaceHolder.Callback, MediaRecorder.OnInfoListener {

    private static int getCameraId(boolean backFacing) {
        Logs.add(Logs.Type.V, "backFacing: " + backFacing);

        int cameraId = Constants.NO_DATA;
        int cameraCount = Camera.getNumberOfCameras();
        int facing = (backFacing)?
                Camera.CameraInfo.CAMERA_FACING_BACK:Camera.CameraInfo.CAMERA_FACING_FRONT;
        for (int i = 0; i < cameraCount; ++i) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == facing) {
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }
    private static Camera getCamera() {
        Logs.add(Logs.Type.V, null);

        // Get back facing camera ID
        int cameraId = getCameraId(true);
        if (cameraId < 0)
            cameraId = getCameraId(false);
        Camera camera = null;
        try {
            if (cameraId < 0)
                camera = Camera.open(); // Attempt to get a default camera instance
            else
                camera = Camera.open(cameraId);
        }
        catch (Exception e) {
            Logs.add(Logs.Type.E, "Camera is not available (in use or does not exist)");
        }
        return camera;
    }
    public static boolean getAvailableSettings(ArrayList<Size> resolutions, ArrayList<int[]> fps) {
        Logs.add(Logs.Type.V, null);

        Camera camera = getCamera();
        if (camera == null) {

            Logs.add(Logs.Type.E, "Failed to get available camera resolutions");
            DisplayMessage.getInstance().alert(R.string.title_error, R.string.camera_disabled,
                    null, false, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try { ActivityWrapper.get().finish(); }
                            catch (NullPointerException e) {
                                Logs.add(Logs.Type.F, "Wrong activity reference");
                            }
                        }
                    });

            return false;
        }

        ////// Resolutions
        List<Size> camResolutions = camera.getParameters().getSupportedVideoSizes();
        if (camResolutions == null)
            camResolutions = camera.getParameters().getSupportedPreviewSizes();
        for (final Size camResolution: camResolutions)
            resolutions.add(camResolution);

        ////// Frames per second (range)
        List<int[]> camFPS = camera.getParameters().getSupportedPreviewFpsRange();
        for (int[] cam: camFPS) {

            // Check if minimum FPS already exists and if so replace it by the new one (only if
            // the maximum FPS is less than previous one)

            boolean toReplace = true; // To add (as well)
            for (int i = 0; i < fps.size(); ++i) {

                if (fps.get(i)[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] == cam[Camera.Parameters.PREVIEW_FPS_MIN_INDEX]) {

                    if (fps.get(i)[Camera.Parameters.PREVIEW_FPS_MAX_INDEX] > cam[Camera.Parameters.PREVIEW_FPS_MAX_INDEX])
                        fps.remove(i); // Replace it
                    else
                        toReplace = false; // Keep previous one
                    break;
                }
                //else // Add it (if last entry)
            }
            if (toReplace)
                fps.add(cam);
        }
        Collections.sort(fps, new Comparator<int[]>() {
            @Override
            public int compare(int[] lhs, int[] rhs) {
                return (lhs[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] < rhs[Camera.Parameters.PREVIEW_FPS_MIN_INDEX])?
                        1:2; // Never == 0 coz always different
            }
        });

        camera.release();
        return true;
    }
    private static CamcorderProfile getCameraProfile() {
        // Find the best quality profile according resolution

        Logs.add(Logs.Type.V, null);

        int cameraId = getCameraId(true);
        if (cameraId < 0)
            cameraId = getCameraId(false);

        int quality;
        if (Build.VERSION.SDK_INT >= 21) {

            if (Settings.getInstance().mResolution.height >= 2160)
                quality = CamcorderProfile.QUALITY_HIGH_SPEED_2160P;
            else if (Settings.getInstance().mResolution.height >= 1080)
                quality = CamcorderProfile.QUALITY_HIGH_SPEED_1080P;
            else if (Settings.getInstance().mResolution.height >= 720)
                quality = CamcorderProfile.QUALITY_HIGH_SPEED_720P;
            else if (Settings.getInstance().mResolution.height >= 480)
                quality = CamcorderProfile.QUALITY_HIGH_SPEED_480P;
            else
                quality = CamcorderProfile.QUALITY_HIGH_SPEED_LOW;
        }
        else {

            if (Settings.getInstance().mResolution.height >= 1080)
                quality = CamcorderProfile.QUALITY_1080P;
            else if (Settings.getInstance().mResolution.height >= 720)
                quality = CamcorderProfile.QUALITY_720P;
            else if (Settings.getInstance().mResolution.height >= 480)
                quality = CamcorderProfile.QUALITY_480P;
            else
                quality = CamcorderProfile.QUALITY_LOW;
        }
        if (CamcorderProfile.hasProfile(cameraId, quality))
            return CamcorderProfile.get(cameraId, quality);

        return null;
    }

    //////
    public Size getPreviewResolution() {
        Logs.add(Logs.Type.V, null);

        // Set preview resolution according the video setting
        if (mCamera.getParameters().getSupportedVideoSizes() != null) {

            // _ Same ratio with the video setting resolution
            // _ Width x Height < Width x Height of the video setting
            float ratio = ((float)Settings.getInstance().mResolution.width) /
                    Settings.getInstance().mResolution.height;
            int product = Settings.getInstance().mResolution.width *
                    Settings.getInstance().mResolution.height;

            List<Size> previewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            Size defaultSize = null;
            for (Size previewSize : previewSizes) {
                if (ratio == (((float)previewSize.width) / previewSize.height)) {

                    int previewProduct = previewSize.width * previewSize.height;
                    if (product > previewProduct)
                        return previewSize; // Ok

                    if (product == previewProduct)
                        defaultSize = previewSize;
                }
            }
            if (defaultSize != null)
                return  defaultSize;
                // Return at least default size with same ratio and product

            Logs.add(Logs.Type.W, "Failed to get appropriate preview size");
            // ...if not set keep video size as preview size (below)
        }

        // Same preview & setting resolution
        return Settings.getInstance().mResolution;
    }

    public void postRecording() {

        Logs.add(Logs.Type.V, null);
        // ...only called by the device which is not the maker

        // Stop camera preview (in a few milliseconds)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                stopPreview();
            }
        }, Constants.CONN_WAIT_DELAY << 1);
    }
    private boolean startRecorder() {
        Logs.add(Logs.Type.V, null);
        try {

            // Prepare media recorder
            if (!prepareRecording())
                throw new Exception();

            mMediaRecorder.start();
        }
        catch (Exception e) {

            Logs.add(Logs.Type.E, "Failed to start recorder");

            // Cancel recorder
            try { ((ProcessActivity)ActivityWrapper.get()).cancelRecorder(); }
            catch (NullPointerException e1) {
                Logs.add(Logs.Type.F, "Wrong activity reference");
            }
            catch (ClassCastException e1) {
                Logs.add(Logs.Type.F, "Wrong activity class");
            }

            // Send cancel request to remote device
            Connectivity.getInstance().addRequest(ActivityWrapper.getInstance(),
                    ActivityWrapper.REQ_TYPE_CANCEL, null);

            // Inform user on recorder failure
            DisplayMessage.getInstance().alert(R.string.title_error, R.string.error_start_recording,
                    null, true, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == DialogInterface.BUTTON_POSITIVE)
                                Settings.getInstance().mNoFps = true;

                            // Finish activity
                            ActivityWrapper.stopActivity(ProcessActivity.class, Constants.NO_DATA, null);
                        }
                    });

            return false;
        }
        return true;
    }
    public void startRecording() {
        Logs.add(Logs.Type.V, null);

        if (Settings.getInstance().isMaker()) {

            // Stop camera preview then start recording
            stopPreview();
            startRecorder();
        }
        else { // Start recording (camera preview already stopped)

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startRecorder();
                }
            }, Constants.CONN_WAIT_DELAY);
        }
    }

    //
    private void stopPreview() {
        Logs.add(Logs.Type.V, null);

        // Stop camera preview
        synchronized (mRawPicture) {

            mTakePicture = false;
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
        }
        mCamera.unlock();
    }
    private boolean prepareRecording() {
        Logs.add(Logs.Type.V, null);

        // Prepare recording
        if(mMediaRecorder == null)
            mMediaRecorder = new MediaRecorder();

        mMediaRecorder.setPreviewDisplay(getHolder().getSurface());
        mMediaRecorder.setCamera(mCamera);
        mMediaRecorder.setOnInfoListener(this);

        // Set media sources
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

        // Set video & audio encoder
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setVideoSize(Settings.getInstance().mResolution.width,
                Settings.getInstance().mResolution.height);

        // Attempt to apply best video quality such as FPS & Video Bit Rate according resolution
        if (!Settings.getInstance().mNoFps) {

            CamcorderProfile camProfile = getCameraProfile();
            if (camProfile != null) {

                Logs.add(Logs.Type.I, "VBR: " + camProfile.videoBitRate + " FPS: " + camProfile.videoFrameRate);
                mMediaRecorder.setVideoFrameRate(camProfile.videoFrameRate);
                mMediaRecorder.setVideoEncodingBitRate(camProfile.videoBitRate);
            }
            else
                mMediaRecorder.setVideoEncodingBitRate(3000000);
        }
        //else // Use default FPS & Video Bit Rate if user has confirmed a video recording
        //        without video quality setting applied (avoid start recorder failure)

        // Set orientation
        if (Settings.getInstance().mOrientation) { // Portrait

            mMediaRecorder.setOrientationHint(90);
            if (Settings.getInstance().mReverse)
                mMediaRecorder.setOrientationHint(270);
        }
        else if (Settings.getInstance().mReverse) // Landscape & reverse
            mMediaRecorder.setOrientationHint(180);

        // Set duration & output file name
        mMediaRecorder.setMaxDuration(Settings.getInstance().mDuration * 1000);
        mMediaRecorder.setOutputFile(Storage.DOCUMENTS_FOLDER + Storage.FILENAME_LOCAL_VIDEO);

        try { mMediaRecorder.prepare(); }
        catch (IOException e) {

            Logs.add(Logs.Type.E, "Failed to prepare recorder: " + e.getMessage());
            return false;
        }
        return true;
    }

    //////
    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        Logs.add(Logs.Type.V, "what: " + what);

        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {

            // The video recording has finished
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;

            try { ((ProcessActivity)ActivityWrapper.get()).startProcessing(mPreviewSize, mRawPicture); }
            catch (Exception e) {
                Logs.add(Logs.Type.F, "Unexpected activity reference");
            }
        }
    }

    //////
    private SurfaceHolder mHolder;
    private Camera mCamera;

    private MediaRecorder mMediaRecorder;

    private Size mPreviewSize;
    private boolean mTakePicture;
    private byte[] mRawPicture;

    private void create() {

        Logs.add(Logs.Type.V, null);
        if (mCamera == null)
            mCamera = getCamera();

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);

        // ...and needed during a lock/unlock screen operation
    }
    public void release() {
        Logs.add(Logs.Type.V, null);

        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    //
    public CameraView(Context context) {
        super(context);
        Logs.add(Logs.Type.V, null);
        mTakePicture = true;
        create();
    }
    public CameraView(Context context, AttributeSet attrs) {
        super(context);
        Logs.add(Logs.Type.V, null);
        mTakePicture = false;
        create();
    }

    //////
    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        Logs.add(Logs.Type.V, null);
        create();

        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        }
        catch (NullPointerException e) {
            Logs.add(Logs.Type.W, "Camera not ready: " + e.getMessage());
        }
        catch (IOException e) {
            Logs.add(Logs.Type.E, "Error setting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        Logs.add(Logs.Type.V, null);
        if (mHolder.getSurface() == null)
            return;

        // stop preview before making changes
        try { mCamera.stopPreview(); }
        catch (Exception e){
            Logs.add(Logs.Type.W, "Try to stop a non-existent preview: " + e.getMessage());
        }

        // Apply camera preview settings and start it
        try {

            // set preview size and make any resize, rotate or
            // reformatting changes here
            switch (((WindowManager)getContext().getSystemService(
                    Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation()) {

                case Surface.ROTATION_0:

                    // Natural orientation (Portrait)
                    Logs.add(Logs.Type.I, "ROTATION_0");
                    mCamera.setDisplayOrientation(90);

                    // TODO: Check if natural orientation is portrait
                    break;

                case Surface.ROTATION_90:
                    Logs.add(Logs.Type.I, "ROTATION_90");
                    mCamera.setDisplayOrientation(0); // Landscape (left)
                    break;

                case Surface.ROTATION_180:
                    Logs.add(Logs.Type.I, "ROTATION_180");
                    mCamera.setDisplayOrientation(270); // Portrait (upside down)
                    break;

                case Surface.ROTATION_270:
                    Logs.add(Logs.Type.I, "ROTATION_270");
                    mCamera.setDisplayOrientation(180); // Landscape (right)
                    break;
            }
            mPreviewSize = getPreviewResolution();
            mCamera.getParameters().setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            mCamera.getParameters().setPreviewFormat(ImageFormat.NV21);

            mCamera.getParameters().setRecordingHint(true);
            mCamera.getParameters().setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
            mCamera.getParameters().setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            mCamera.getParameters().setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            Logs.add(Logs.Type.I, "Parameters set");

            if (mTakePicture) { // Check to prepare recording

                mPreviewSize = mCamera.getParameters().getPreviewSize();
                // NB: Needed in case where it failed to assign specific preview size

                mRawPicture = new byte[(mPreviewSize.width * mPreviewSize.height * 3) >> 1]; // NV21 buffer size
                mCamera.setPreviewCallback(new Camera.PreviewCallback() {

                    @Override
                    public void onPreviewFrame(byte[] data, Camera camera) {
                        synchronized (mRawPicture) {
                            if (mTakePicture)
                                System.arraycopy(data, 0, mRawPicture, 0, data.length);
                        }
                    }
                });
            }
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        }
        catch (NullPointerException e) {
            Logs.add(Logs.Type.W, "Camera not ready: " + e.getMessage());
        }
        catch (IOException e) {
            Logs.add(Logs.Type.E, "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        Logs.add(Logs.Type.V, null);
        release();
    }
}
