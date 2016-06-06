package com.studio.artaban.anaglyph3d.camera;

import android.content.DialogInterface;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.content.Context;
import android.media.MediaRecorder;
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

    private static Camera getCamera() {

        // Get back facing camera ID
        int cameraId = -1;
        int cameraCount = Camera.getNumberOfCameras();
        for (int i = 0; i < cameraCount; ++i) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                break;
            }
        }
        Camera camera = null;
        try {
            if (cameraId > 0)
                camera = Camera.open(cameraId);
            else
                camera = Camera.open(); // Attempt to get a default camera instance
        }
        catch (Exception e) {
            Logs.add(Logs.Type.E, "Camera is not available (in use or does not exist)");
        }
        return camera;
    }
    public static boolean getAvailableSettings(ArrayList<Size> resolutions, ArrayList<int[]> fps) {

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

    //////
    public Size getPreviewResolution() {

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
        // ...only called by the device which is not the maker

        // Stop camera preview (in a few milliseconds)
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopPreview();
            }
        }, Constants.CONN_WAIT_DELAY << 1);
    }
    private boolean startRecorder() {

        try { mMediaRecorder.start(); }
        catch (Exception e) {

            Logs.add(Logs.Type.E, "Failed to start recorder");

            // Force to quit application (after having displayed a message to user)
            ActivityWrapper.stopActivity(ProcessActivity.class,
                    Constants.RESULT_FAILED_RECORDING, null);
            return false;
        }
        return true;
    }
    public void startRecording() {
        if (Settings.getInstance().isMaker()) {

            // Stop camera preview then start recording
            stopPreview();
            startRecorder();
        }
        else { // Start recording (camera preview already stopped)

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startRecorder();
                }
            }, Constants.CONN_WAIT_DELAY);
        }
    }










    public boolean stop() {

        if (mCamera == null)
            return false;

        mCamera.stopPreview();
        try { mCamera.setPreviewDisplay(null); }
        catch (IOException e) {
            Logs.add(Logs.Type.W, "Failed to remove preview display");
        }
        mCamera.setPreviewCallback(null);

        release();
        return true;
    }








    //
    private void stopPreview() {

        // Stop camera preview
        synchronized (mRawPicture) {

            mTakePicture = false;
            mCamera.stopPreview();
            try { mCamera.setPreviewDisplay(null); }
            catch (IOException e) {
                Logs.add(Logs.Type.W, "Failed to remove preview display");
            }
            mCamera.setPreviewCallback(null);
        }
        mCamera.unlock();
    }
    private boolean prepareRecording() {

        // Prepare recording
        if(mMediaRecorder == null)
            mMediaRecorder = new MediaRecorder();

        mMediaRecorder.setPreviewDisplay(getHolder().getSurface());
        mMediaRecorder.setCamera(mCamera);
        mMediaRecorder.setOnInfoListener(this);

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setVideoSize(Settings.getInstance().mResolution.width,
                Settings.getInstance().mResolution.height);

        // Set orientation
        if (Settings.getInstance().mOrientation) { // Portrait

            mMediaRecorder.setOrientationHint(90);
            if (Settings.getInstance().mReverse)
                mMediaRecorder.setOrientationHint(270);
        }
        else if (Settings.getInstance().mReverse) // Landscape & reverse
            mMediaRecorder.setOrientationHint(180);

        mMediaRecorder.setMaxDuration(Settings.getInstance().mDuration * 1000);
        mMediaRecorder.setVideoEncodingBitRate(3000000);
        mMediaRecorder.setVideoFrameRate(
                Settings.getInstance().mFps[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] / 1000);
        mMediaRecorder.setOutputFile(ActivityWrapper.DOCUMENTS_FOLDER + Storage.FILENAME_LOCAL_VIDEO);

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
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {

            // The video recording has finished
            mMediaRecorder.reset();
            mMediaRecorder.release();

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

        if (mCamera == null)
            mCamera = getCamera();

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);

        // ...and needed during a lock/unlock screen operation
    }
    private void release() {

        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    //
    public CameraView(Context context) {
        super(context);
        mTakePicture = true;
        create();
    }
    public CameraView(Context context, AttributeSet attrs) {
        super(context);
        mTakePicture = false;
        create();
    }

    //////
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
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

        if (mHolder.getSurface() == null)
            return;

        // stop preview before making changes
        try { mCamera.stopPreview(); }
        catch (Exception e){
            Logs.add(Logs.Type.W, "Try to stop a non-existent preview: " + e.getMessage());
        }

        // start preview with new settings
        try {

            // set preview size and make any resize, rotate or
            // reformatting changes here
            switch (((WindowManager)getContext().getSystemService(
                    Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation()) {

                case Surface.ROTATION_0:

                    // Natural orientation (Portrait)
                    mCamera.setDisplayOrientation(90);

                    // TODO: Check if natural orientation is portrait
                    break;

                case Surface.ROTATION_90:
                    mCamera.setDisplayOrientation(0); // Landscape (left)
                    break;

                case Surface.ROTATION_180:
                    mCamera.setDisplayOrientation(270); // Portrait (upside down)
                    break;

                case Surface.ROTATION_270:
                    mCamera.setDisplayOrientation(180); // Landscape (right)
                    break;
            }
            mPreviewSize = getPreviewResolution();
            mCamera.getParameters().setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            mCamera.getParameters().setPreviewFormat(ImageFormat.NV21);
            if (mTakePicture) { // Check to prepare recording

                mCamera.getParameters().setPreviewFpsRange(
                        Settings.getInstance().mFps[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
                        Settings.getInstance().mFps[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);

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

                // Prepare media recorder
                prepareRecording();
            }
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        }
        catch (Exception e) {
            Logs.add(Logs.Type.E, "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) { release(); }
}
