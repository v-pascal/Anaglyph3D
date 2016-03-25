package com.studio.artaban.anaglyph3d.helpers;

import android.hardware.Camera;
import android.content.Context;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.IOException;

/**
 * Created by pascal on 22/03/16.
 * Camera helper
 */
public class CameraView extends SurfaceView implements SurfaceHolder.Callback {

    public static Camera getCamera() {
        Camera camera = null;
        try { camera = Camera.open(); } // Attempt to get a default camera instance
        catch (Exception e) {
            Logs.add(Logs.Type.E, "Camera is not available (in use or does not exist)");
        }
        return camera;
    }

    private SurfaceHolder mHolder;
    private Camera mCamera;

    public void resume() {
        if (mCamera == null)
            mCamera = getCamera();
    }
    public void pause() {
        mCamera.release();
        mCamera = null;
    }

    //////
    public CameraView(Context context) {
        super(context);
        mCamera = getCamera();

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
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

        // start preview with new settings
        try {
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
