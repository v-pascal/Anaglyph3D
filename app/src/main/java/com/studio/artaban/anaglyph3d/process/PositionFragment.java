package com.studio.artaban.anaglyph3d.process;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;

/**
 * Created by pascal on 13/04/16.
 * Position fragment to inform user on devices position before recording
 */
public class PositionFragment extends Fragment {

    public static final String TAG = "position";

    private static final int BACK_DEVICE_WIDTH = 218; // Back_left/right image width (in pixel)
    private static final int BACK_DEVICE_HEIGHT = 284; // Back_left/right image height (in pixel)

    private static Bitmap flip(Bitmap source, boolean vertical) {

        Matrix matrix = new Matrix();
        if (vertical)
            matrix.preScale(1.0f, -1.0f);
        else // Horizontal
            matrix.preScale(-1.0f, 1.0f);

        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    //////
    private ImageView mBackImage;

    //
    public void reverse() {

        // Check if orientation has changed as expected
        // -> Some phone such as the 'Samsung Galaxy Trend Lite' do not apply orientation changes
        int orientation = ((WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE)).
                getDefaultDisplay().getRotation();

        if (Settings.getInstance().mReverse) {
            if (Settings.getInstance().mOrientation) { // Portrait
                if (orientation != Surface.ROTATION_180)
                    orientation = Constants.NO_DATA; // Not in reversed portrait
            }
            else if (orientation != Surface.ROTATION_270)
                orientation = Constants.NO_DATA; // Not in reversed landscape
        }
        else {
            if (Settings.getInstance().mOrientation) { // Portrait
                if (orientation != Surface.ROTATION_0)
                    orientation = Constants.NO_DATA; // Not in portrait
            }
            else if (orientation != Surface.ROTATION_90)
                orientation = Constants.NO_DATA; // Not in landscape
        }
        // TODO: Manage landscape natural orientation devices

        if (orientation == Constants.NO_DATA) {

            // Inform user the reverse option cannot be applied to this device
            DisplayMessage.getInstance().toast(R.string.reverse_disabled, Toast.LENGTH_LONG);
            return;
        }

        // Display the back device image according the new reverse specification
        if (Settings.getInstance().mReverse) {

            if (Settings.getInstance().mOrientation) // Portrait (reversed)
                mBackImage.setImageBitmap(flip(BitmapFactory.decodeResource(getContext().getResources(),
                        (Settings.getInstance().mPosition)?
                                R.drawable.back_portrait_left:R.drawable.back_portrait_right), true));
            else // Landscape (reversed)
                mBackImage.setImageBitmap(flip(BitmapFactory.decodeResource(getContext().getResources(),
                        (Settings.getInstance().mPosition)?
                                R.drawable.back_landscape_left:R.drawable.back_landscape_right), true));
        }
        else {

            if (Settings.getInstance().mOrientation) // Portrait
                mBackImage.setImageDrawable(getContext().getResources().getDrawable(
                        (Settings.getInstance().mPosition)?
                                R.drawable.back_portrait_left:R.drawable.back_portrait_right));
            else // Landscape
                mBackImage.setImageDrawable(getContext().getResources().getDrawable(
                        (Settings.getInstance().mPosition) ?
                                R.drawable.back_landscape_left : R.drawable.back_landscape_right));
        }
    }

    //////
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_position, container, false);
        final Point screenSize = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(screenSize);

        ////// Set up settings info
        final TextView textSetting = (TextView)rootView.findViewById(R.id.text_setting);
        textSetting.setText(getString(R.string.settings,
                Settings.getInstance().getResolution(),
                Settings.getInstance().mDuration,
                Settings.getInstance().mFps[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] / 1000));
        if (Settings.getInstance().mOrientation) // Portrait
            ((LayoutParams)textSetting.getLayoutParams()).addRule(RelativeLayout.CENTER_HORIZONTAL);

        ////// Set up back device image
        mBackImage = (ImageView)rootView.findViewById(R.id.back_device);
        if (mBackImage != null) {

            // Display the appropriate back device image at the appropriate position
            LayoutParams params = (LayoutParams)mBackImage.getLayoutParams();
            if (Settings.getInstance().mPosition) {

                mBackImage.setImageDrawable(getContext().getResources().getDrawable(
                        (Settings.getInstance().mOrientation)?
                                R.drawable.back_portrait_left:R.drawable.back_landscape_left));
                if (Build.VERSION.SDK_INT >= 17)
                    params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            }
            else {

                mBackImage.setImageDrawable(getContext().getResources().getDrawable(
                        (Settings.getInstance().mOrientation)?
                                R.drawable.back_portrait_right:R.drawable.back_landscape_right));
                if (Build.VERSION.SDK_INT >= 17)
                    params.addRule(RelativeLayout.ALIGN_PARENT_START);
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            }

            // The back device image should take place in 50% of the screen height...
            params.height = (int)(screenSize.y * 0.5f);
            if (Settings.getInstance().mOrientation) // Portrait
                params.width = (int)(((float)params.height * BACK_DEVICE_WIDTH) / BACK_DEVICE_HEIGHT);
            else // Landscape
                params.width = (int)(((float)params.height * BACK_DEVICE_HEIGHT) / BACK_DEVICE_WIDTH);
            if (params.width > (screenSize.x >> 1)) { // ...in a maximum of 50% of the screen width

                params.width = screenSize.x >> 1;
                params.height = (int)((float)(screenSize.y * params.width) / screenSize.x);
            }
            mBackImage.setLayoutParams(params);
            mBackImage.requestLayout();
        }

        ////// Set up reverse image
        final ImageView backReverse = (ImageView)rootView.findViewById(R.id.back_reverse);
        int reverseMargin = 0;
        if (backReverse != null) {

            // Display the reverse image at the appropriate position...
            LayoutParams params = (LayoutParams)backReverse.getLayoutParams();

            // ...with a size of 25% of the screen width/height
            if (Settings.getInstance().mOrientation) { // Portrait
                params.height = (int) (screenSize.x * 0.25f);
                reverseMargin = params.height >> 1;
            }
            else { // Landscape

                if (Build.VERSION.SDK_INT >= 17)
                    params.removeRule(RelativeLayout.CENTER_VERTICAL);
                params.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.back_device);

                params.height = (int) (screenSize.y * 0.25f);
                reverseMargin = (screenSize.x >> 2) - (params.height >> 1);
            }
            params.width = params.height;

            if (Settings.getInstance().mPosition) { // Left camera

                if (Build.VERSION.SDK_INT >= 17)
                    params.addRule(RelativeLayout.ALIGN_PARENT_START);
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                params.setMargins(reverseMargin, 0, 0, 0);
            }
            else { // Right camera

                if (Build.VERSION.SDK_INT >= 17)
                    params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                params.setMargins(0, 0, reverseMargin, 0);
            }
            backReverse.setLayoutParams(params);
            backReverse.requestLayout();
        }

        ////// Set up device or 3D type info
        final TextView textDistance = (TextView)rootView.findViewById(R.id.text_distance);
        if (!Settings.getInstance().mOrientation) { // Landscape

            if (Build.VERSION.SDK_INT >= 17)
                ((LayoutParams)textDistance.getLayoutParams()).removeRule(RelativeLayout.ABOVE);
            ((LayoutParams)textDistance.getLayoutParams()).addRule(RelativeLayout.ABOVE, R.id.back_reverse);

            if (Settings.getInstance().mPosition) { // Left camera

                ((LayoutParams)textDistance.getLayoutParams()).setMargins(reverseMargin, 0, 0, 0);
                ((LayoutParams)textSetting.getLayoutParams()).setMargins(reverseMargin, 0, 0, 0);
            }
            else { // Right camera

                if (Build.VERSION.SDK_INT >= 17) {
                    ((LayoutParams) textDistance.getLayoutParams()).addRule(RelativeLayout.ALIGN_START, R.id.back_reverse);
                    ((LayoutParams)textSetting.getLayoutParams()).addRule(RelativeLayout.ALIGN_START, R.id.back_reverse);
                }
                ((LayoutParams) textDistance.getLayoutParams()).addRule(RelativeLayout.ALIGN_LEFT, R.id.back_reverse);
                ((LayoutParams)textSetting.getLayoutParams()).addRule(RelativeLayout.ALIGN_LEFT, R.id.back_reverse);
            }
        }
        else // Portrait
            ((LayoutParams)textDistance.getLayoutParams()).addRule(RelativeLayout.CENTER_HORIZONTAL);

        if (Settings.getInstance().mSimulated) { // Simulated 3D

            mBackImage.setImageDrawable(getResources().getDrawable(R.drawable.left_device));
            backReverse.setImageDrawable(getResources().getDrawable(R.drawable.simulated_device));
            textDistance.setText(getString(R.string.simulated_3d));
        }
        rootView.invalidate();
        return rootView;
    }
}
