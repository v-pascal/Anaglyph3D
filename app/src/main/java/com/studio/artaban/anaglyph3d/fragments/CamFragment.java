package com.studio.artaban.anaglyph3d.fragments;

import android.content.Context;
import android.os.Build;
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

    private ImageView mImgGlass;
    public void displayPosition() { // According settings position

        final RelativeLayout cameraLayout = (RelativeLayout)mCamLayout.findViewById(R.id.cameraLayout);
        if (mImgGlass != null)
            cameraLayout.removeView(mImgGlass);

        // Create or recreate glass image view cause unable to change parent layout alignment dynamically
        mImgGlass = new ImageView(mContext);
        mImgGlass.setAdjustViewBounds(true);
        RelativeLayout.LayoutParams imgParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);

        if (Settings.getInstance().getPosition()) {

            mImgGlass.setImageDrawable(getResources().getDrawable(R.drawable.left_glass));
            if (Build.VERSION.SDK_INT >= 17)
                imgParams.addRule(RelativeLayout.ALIGN_PARENT_END);
            imgParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        }
        else {

            mImgGlass.setImageDrawable(getResources().getDrawable(R.drawable.right_glass));
            if (Build.VERSION.SDK_INT >= 17)
                imgParams.addRule(RelativeLayout.ALIGN_PARENT_START);
            imgParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        }
        imgParams.addRule(RelativeLayout.CENTER_VERTICAL);
        mImgGlass.setLayoutParams(imgParams);

        // Add glass image view to the layout
        cameraLayout.addView(mImgGlass, imgParams);
    }

    //////
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mCamLayout = inflater.inflate(R.layout.fragment_camera, container, false);

        // Display glass image according position (initial)
        displayPosition();

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
