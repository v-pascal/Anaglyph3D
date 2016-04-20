package com.studio.artaban.anaglyph3d.process;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.libGST.GstObject;

import java.io.File;

/**
 * Created by pascal on 12/04/16.
 * Process fragment to...
 */
public class ProcessFragment extends Fragment {

    public static final String TAG = "process";

    public static final String PICTURE_SIZE_WIDTH = "width";
    public static final String PICTURE_SIZE_HEIGHT = "height";
    public static final String PICTURE_RAW_BUFFER = "raw";

    //
    private enum Status {

        SAVE_RAW_PICTURE (R.string.status_save_raw), // Save local picture (used to set up the contrast & brightness)
        CONVERT_RAW_PICTURE (R.string.status_convert_raw), // Convert local picture from NV21 to ARGB
        TRANSFER_PICTURE (R.string.status_transfer_raw), // Transfer picture (to remote device which is not the maker)

        // Progress for each 1024 bytes packets...
        TRANSFER_VIDEO_SOURCE(0), // Transfer video

        EXTRACT_FRAMES_LEFT(0), // Extract RGB pictures from left camera
        EXTRACT_FRAMES_RIGHT(0), // Extract RGB pictures from right camera
        EXTRACT_AUDIO(0), // Extract audio from one of the videos
        MERGE_FPS(0), // Remove the too many RGB pictures from camera video with bigger FPS
        WAIT_CONTRAST(0), // Wait until contrast & brightness have been set
        TRANSFER_CONTRAST(0), // Transfer the contrast & brightness

        // Progress for each pictures extracted from videos...
        APPLY_FRAME_CHANGES(0), // Color frame (in blue or red), and apply contrast & brightness

        MAKE_3D_VIDEO(0), // Make the anaglyph 3D video

        // Progress for each 1024 bytes packets...
        TRANSFER_3D_VIDEO(0); // Transfer 3D video (to remote device which is not the maker)

        //
        private final int mStringId;
        Status(int id) { mStringId = id; }
        public int getStringId() { return mStringId; }
    }
    private Status mStatus = Status.SAVE_RAW_PICTURE;

    private ProcessTask mProcessTask;
    private class ProcessTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            switch (mStatus) {

                case SAVE_RAW_PICTURE: {

                    /*
                    int width = getArguments().getInt(PICTURE_SIZE_WIDTH);
                    int height = getArguments().getInt(PICTURE_SIZE_HEIGHT);
                    byte[] raw = getArguments().getByteArray(PICTURE_RAW_BUFFER);
                    */





                    break;
                }
                case CONVERT_RAW_PICTURE: {



                    /*
                    File pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

                    GstObject gst = new GstObject(getContext());
                    gst.launch("filesrc location=" + pictures + "/temp.jpg ! jpegdec ! videoconvert ! video/x-raw,format=ARGB !" +
                            " filesink location=" + pictures + "/temp.bin");





                    ######---------------------------

                    - Contrast & brigthness
                    - Videos transfer & extraction
                      Frames conversion
                      Transfer 3D video

                    Status: Bla blabla bla lba...





                            */

                    //filesrc location=testage.nv21 blocksize=460800 ! video/x-raw,format=NV21,width=640,height=480,framerate=1/1 ! videoconvert ! jpegenc ! filesink location=temp.jpg

                    //filesrc location=myrec1.3gp ! qtdemux ! decodebin ! audioconvert ! wavenc ! filesink location=file1.wav
                    //filesrc location=myrec1.3gp ! qtdemux ! decodebin ! videoconvert ! video/x-raw,format=RGB ! multifilesink location=img_%d.bin




                    break;
                }
            }
            return null;
        }
    };

    //
    private ImageView mClapImage;
    private GridLayout mStepLayout;

    //////
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mProcessTask = new ProcessTask();
        mProcessTask.execute();

        // Restore system sound (disabled to avoid sound when start and stop recording)
        ((AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE)).
                setStreamMute(AudioManager.STREAM_SYSTEM, false);

        final View rootView = inflater.inflate(R.layout.fragment_process, container, false);
        mStepLayout = (GridLayout)rootView.findViewById(R.id.layout_step);

        // Display 3D clap animation
        mClapImage = (ImageView)rootView.findViewById(R.id.clap_image);
        if (mClapImage != null) {

            mClapImage.setImageDrawable(getResources().getDrawable(R.drawable.clap_anim));
            final AnimationDrawable animClap = (AnimationDrawable)mClapImage.getDrawable();
            mClapImage.post(new Runnable() {

                @Override
                public void run() {
                    animClap.start();
                }
            });
        }
        return rootView;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        super.onConfigurationChanged(newConfig);
        switch (((WindowManager)getContext().getSystemService(
                Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation()) {

            case Surface.ROTATION_0: // Portrait
                break;

            case Surface.ROTATION_90: // Landscape




                Logs.add(Logs.Type.I, "Landscape");


                LayoutParams params = (LayoutParams)mClapImage.getLayoutParams();
                if (Build.VERSION.SDK_INT >= 17)
                    params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                mClapImage.setLayoutParams(params);
                mClapImage.requestLayout();



                params = (LayoutParams)mStepLayout.getLayoutParams();
                if (Build.VERSION.SDK_INT >= 17)
                    params.removeRule(RelativeLayout.BELOW);
                params.addRule(RelativeLayout.LEFT_OF, R.id.clap_image);
                mStepLayout.setLayoutParams(params);
                mStepLayout.requestLayout();



                break;

            case Surface.ROTATION_180: // Reversed portrait
                break;
            case Surface.ROTATION_270: // Reversed landscape
                break;
        }
    }
}
