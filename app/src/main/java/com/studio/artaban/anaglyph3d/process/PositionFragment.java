package com.studio.artaban.anaglyph3d.process;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.data.Settings;

import org.w3c.dom.Text;

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







        // Check if orientation has changed as expected (if not make changes as expected)
        // -> Some phone as the 'Samsung Galaxy Trend Lite' do not apply orientation changes
        if (Settings.getInstance().mReverse) {

        }
        else {
            if (Settings.getInstance().mOrientation) { // Portrait


                /*
                if (mDistanceText.getRotation() > 0f) {


                    mDistanceText.setRoation(0f);
                    //Change alignment...


                }
                */

            }
            else { // Landscape



            }
        }













    }

    //////
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_position, container, false);
        final Point screenSize = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(screenSize);

        // Set up back device image
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

        // Set up reverse image
        final ImageView backReverse = (ImageView)rootView.findViewById(R.id.back_reverse);
        if (backReverse != null) {

            // Display the reverse image at the appropriate position...
            LayoutParams params = (LayoutParams)backReverse.getLayoutParams();

            // ...with a size of 25% of the screen width/height
            int midPos;
            if (Settings.getInstance().mOrientation) { // Portrait
                params.height = (int) (screenSize.x * 0.25f);
                midPos = params.height >> 1;
            }
            else { // Landscape
                params.height = (int) (screenSize.y * 0.25f);
                midPos = (screenSize.x >> 2) - (params.height >> 1);
            }
            params.width = params.height;

            if (Settings.getInstance().mPosition) {

                if (Build.VERSION.SDK_INT >= 17)
                    params.addRule(RelativeLayout.ALIGN_PARENT_START);
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                params.setMargins(midPos, 0, 0, 0);
            }
            else {

                if (Build.VERSION.SDK_INT >= 17)
                    params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                params.setMargins(0, 0, midPos, 0);
            }
            backReverse.setLayoutParams(params);
            backReverse.requestLayout();
        }
        rootView.invalidate();
        return rootView;
    }
}
