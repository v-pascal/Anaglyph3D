package com.studio.artaban.anaglyph3d.process;

import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout.LayoutParams;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.data.Settings;

/**
 * Created by pascal on 13/04/16.
 * Position fragment to inform user on devices position before recording
 */
public class PositionFragment extends Fragment {

    public static final String TAG = "position";

    private static final int BACK_DEVICE_WIDTH = 218; // Back_left/right image width (in pixel)
    private static final int BACK_DEVICE_HEIGHT = 284; // Back_left/right image height (in pixel)

    //////
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(
                (Settings.getInstance().mPosition)?
                        R.layout.fragment_position_left:R.layout.fragment_position_right,
                container, false);

        // Set up back device image
        final ImageView backDevice = (ImageView)rootView.findViewById(R.id.back_device);
        if (backDevice != null) {

            // The back device image should take place in 50% of the screen height
            Point screenSize = new Point();
            getActivity().getWindowManager().getDefaultDisplay().getSize(screenSize);
            float screenHeight = screenSize.y * 0.5f;

            LayoutParams params = (LayoutParams)backDevice.getLayoutParams();
            params.height = (int)screenHeight;
            params.width = (int)(((float)params.height * BACK_DEVICE_WIDTH) / BACK_DEVICE_HEIGHT);
            backDevice.setLayoutParams(params);

            backDevice.requestLayout();
            rootView.invalidate();
        }
        return rootView;
    }
}
