package com.studio.artaban.anaglyph3d.helpers;

import android.hardware.Camera;
import android.content.Context;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * Created by pascal on 22/03/16.
 * Camera helper
 */
public class CameraView extends SurfaceView implements SurfaceHolder.Callback {

    public static Camera getCamera() {
        Camera cam = null;
        try { cam = Camera.open(); } // attempt to get a Camera instance
        catch (Exception e) {
            Logs.add(Logs.Type.E, "Camera is not available (in use or does not exist)");
        }
        return cam;
    }

    private SurfaceHolder mHolder;
    private Camera mCamera;

    public CameraView(Context context, Camera camera) {
        super(context);
        mCamera = camera;

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
