package com.studio.artaban.anaglyph3d.camera;

import android.content.DialogInterface;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;
import com.studio.artaban.anaglyph3d.helpers.Logs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by pascal on 22/03/16.
 * Camera helper
 */
public class CameraView extends SurfaceView implements SurfaceHolder.Callback {

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
    public static boolean getAvailableResolutions(ArrayList<Size> resolutions) {

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
        List<Size> camResolutions = camera.getParameters().getSupportedPreviewSizes();
        for (final Size camResolution: camResolutions)
            resolutions.add(camResolution);

        camera.release();
        return true;
    }

    //////
    private SurfaceHolder mHolder;
    private Camera mCamera;

    public void resume() {

        if (mCamera == null)
            mCamera = getCamera();

        setCallback();
        // Needed during a lock/unlock screen operation
    }
    public void pause() {

        mCamera.release();
        mCamera = null;
    }

    //
    public CameraView(Context context, AttributeSet attrs) {
        super(context);
        mCamera = getCamera();

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        setCallback();
    }

    private void setCallback() {

        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    //////
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
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
            switch (((WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation()) {
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
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        }
        catch (Exception e) {
            Logs.add(Logs.Type.E, "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) { }
}
