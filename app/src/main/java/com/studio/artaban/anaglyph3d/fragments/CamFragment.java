package com.studio.artaban.anaglyph3d.fragments;

import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.app.Fragment;
import android.widget.FrameLayout;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.helpers.CameraView;
import com.studio.artaban.anaglyph3d.helpers.Logs;

/**
 * Created by pascal on 22/03/16.
 * Camera fragment
 */
public class CamFragment extends Fragment {

    private Context mContext;
    public CamFragment(Context context) { mContext = context; }

    private Camera mCamera;
    private CameraView mPreview;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View camLayout = inflater.inflate(R.layout.fragment_camera, container, false);

        // Create an instance of Camera
        mCamera = CameraView.getCamera();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraView(mContext, mCamera);
        FrameLayout preview = (FrameLayout)camLayout.findViewById(R.id.cameraView);
        preview.addView(mPreview);

        return camLayout;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mCamera == null)
            mCamera = CameraView.getCamera();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }
}
