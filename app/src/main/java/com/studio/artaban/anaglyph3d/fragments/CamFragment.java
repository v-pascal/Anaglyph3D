package com.studio.artaban.anaglyph3d.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.app.Fragment;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.helpers.CameraView;

/**
 * Created by pascal on 22/03/16.
 * Camera fragment
 */
public class CamFragment extends Fragment {

    private Context mContext;
    public CamFragment(Context context) { mContext = context; }

    private CameraView mPreview;
    private View mCamLayout;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mCamLayout = inflater.inflate(R.layout.fragment_camera, container, false);

        // Set and align glass image according initial position
        if (!Settings.mPosition) {

            final ImageView imgGlass = (ImageView)mCamLayout.findViewById(R.id.imgGlass);
            imgGlass.setImageDrawable(getResources().getDrawable(R.drawable.right_glass));

            RelativeLayout.LayoutParams imgParams = (RelativeLayout.LayoutParams)imgGlass.getLayoutParams();
            imgParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            imgGlass.setLayoutParams(imgParams);
        }
        //else // Default position

        return mCamLayout;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mPreview == null) {

            // Create our camera view and set it as the content of our activity.
            mPreview = new CameraView(mContext);
            FrameLayout preview = (FrameLayout) mCamLayout.findViewById(R.id.cameraView);
            preview.addView(mPreview);
        }
        else
            mPreview.resume();
    }

    @Override
    public void onPause() {

        super.onPause();
        mPreview.pause();
    }
}
