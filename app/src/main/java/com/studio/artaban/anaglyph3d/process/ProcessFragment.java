package com.studio.artaban.anaglyph3d.process;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.media.Frame;
import com.studio.artaban.anaglyph3d.transfer.Connectivity;
import com.studio.artaban.libGST.GstObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

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

        ////// Contrast & brightness step: 4 status
        INITIALIZATION (R.string.status_initialize), // Initialize process (GStreamer)

        // Progress for each 1024 bytes packets...
        TRANSFER_PICTURE (R.string.status_transfer_raw), // Transfer picture (to remote device which is not the maker)
        WAIT_PICTURE (R.string.status_transfer_raw), // Wait until contrast & brightness picture has been received (to device which is not the maker)

        SAVE_PICTURE (R.string.status_save_raw), // Save raw picture into local file
        CONVERT_PICTURE (R.string.status_convert_raw), // Convert local picture from NV21 to ARGB or JPEG

        ////// Video transfer & extraction step: 7 status

        // Progress for each 1024 bytes packets...
        TRANSFER_VIDEO (R.string.status_transfer_video), // Transfer video
        WAIT_VIDEO (R.string.status_transfer_video), // Wait until video has been received

        EXTRACT_FRAMES_LEFT(0), // Extract RGB pictures from left camera
        EXTRACT_FRAMES_RIGHT(0), // Extract RGB pictures from right camera
        EXTRACT_AUDIO(0), // Extract audio from one of the videos
        MERGE_FPS(0), // Remove the too many RGB pictures from camera video with bigger FPS

        TRANSFER_CONTRAST (R.string.status_transfer_contrast), // Transfer the contrast & brightness (from device which is not the maker)
        WAIT_CONTRAST (R.string.status_wait_contrast), // Wait until contrast & brightness have been received

        ////// Make & transfer 3D video step: 3 status

        // Progress for each pictures extracted from videos...
        APPLY_FRAME_CHANGES(0), // Color frame (in blue or red), and apply contrast & brightness

        MAKE_3D_VIDEO(0), // Make the anaglyph 3D video

        // Progress for each 1024 bytes packets...
        TRANSFER_3D_VIDEO(0), // Transfer 3D video (to remote device which is not the maker)
        WAIT_3D_VIDEO(0); // Wait 3D video received

        //
        private final int mStringId;
        Status(int id) { mStringId = id; }
        public int getStringId() { return mStringId; }
    }
    private Status mStatus = Status.INITIALIZATION;
    private volatile boolean mAbort = false;

    public static GstObject mGStreamer;

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private ProcessTask mProcessTask;
    private class ProcessTask extends AsyncTask<Void, Integer, Void> {

        /*
        private boolean mLocalPicture; // To define which picture to process
        private void sleep() { // Sleep in process task

            try { Thread.sleep(Constants.PROCESS_WAIT_TRANSFER, 0); }
            catch (InterruptedException e) {
                Logs.add(Logs.Type.W, "Unable to sleep: " + e.getMessage());
            }
        }
        private Runnable mInitRunnable = new Runnable() {
            @Override
            public void run() {

                // Initialize GStreamer library
                if (mGStreamer == null)
                    mGStreamer = new GstObject(getContext());




                Logs.add(Logs.Type.E, "3");






                // Notify initialization finished
                synchronized (this) { notify(); }
            }
        };
        */

        @Override
        protected Void doInBackground(Void... params) {

            Logs.add(Logs.Type.E, "Start process loop");






            /*
            while (!mAbort) {
                switch (mStatus) {

                    case INITIALIZATION: {





                        Logs.add(Logs.Type.E, "1");




                        publishProgress(1);

                        // Initialize GStreamer library (on UI thread)
                        synchronized (mInitRunnable) {

                            getActivity().runOnUiThread(mInitRunnable);




                            Logs.add(Logs.Type.E, "2");




                            // Wait initialization finish on UI thread
                            try { mInitRunnable.wait(); }
                            catch (InterruptedException e) {
                                Logs.add(Logs.Type.E, e.getMessage());
                            }
                        }





                        Logs.add(Logs.Type.E, "4");






                        //////
                        mLocalPicture = true;
                        if (!Settings.getInstance().isMaker())
                            mStatus = ProcessFragment.Status.SAVE_PICTURE;

                        else {
                            mStatus = ProcessFragment.Status.TRANSFER_PICTURE;

                            // Send picture transfer request
                            Connectivity.getInstance().addRequest(Frame.getInstance(),
                                    Frame.REQ_TYPE_DOWNLOAD, getArguments());

                            publishProgress(Frame.getInstance().getPacketCount());
                        }
                        break;
                    }
                    case WAIT_PICTURE:
                    case TRANSFER_PICTURE: {

                        sleep();
                        publishProgress(Frame.getInstance().getPacketCount());

                        //////
                        if (Frame.getInstance().getPacketCount() == Frame.getInstance().getPacketTotal()) {
                            if (!Settings.getInstance().isMaker()) {

                                mLocalPicture = false;
                                mStatus = ProcessFragment.Status.SAVE_PICTURE;
                            }
                            else {









                                mStatus = ProcessFragment.Status.WAIT_VIDEO;







                            }
                        }
                        break;
                    }

                    // Called twice: for both local and remote pictures
                    case SAVE_PICTURE: {

                        publishProgress(2 + ((mLocalPicture)? 0:2));

                        // Save NV21 local/remote raw picture file
                        try {
                            byte[] raw = (mLocalPicture)?
                                    getArguments().getByteArray(PICTURE_RAW_BUFFER):
                                    Frame.getInstance().getBuffer();

                            File rawFile = new File(ActivityWrapper.DOCUMENTS_FOLDER,
                                    Constants.PROCESS_RAW_PICTURE_FILENAME);

                            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(rawFile));
                            bos.write(raw);
                            bos.flush();
                            bos.close();

                            mStatus = ProcessFragment.Status.CONVERT_PICTURE;
                        }
                        catch (IOException e) {

                            Logs.add(Logs.Type.E, "Failed to save raw picture");
                            mAbort = true;

                            // Inform user
                            DisplayMessage.getInstance().alert(R.string.title_error, R.string.save_error,
                                    null, false, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            ActivityWrapper.stopActivity(ProcessActivity.class,
                                                    Constants.NO_DATA);
                                        }
                                    });
                        }
                        break;
                    }
                    case CONVERT_PICTURE: {

                        publishProgress(3 + ((mLocalPicture)? 0:2));

                        // Convert NV21 to ARGB picture file
                        int width = getArguments().getInt(PICTURE_SIZE_WIDTH);
                        int height = getArguments().getInt(PICTURE_SIZE_HEIGHT);

                        Frame.convertNV21toARGB(ActivityWrapper.DOCUMENTS_FOLDER + Constants.PROCESS_RAW_PICTURE_FILENAME,
                                width, height, ActivityWrapper.DOCUMENTS_FOLDER + ((mLocalPicture)?
                                        Constants.PROCESS_LOCAL_PICTURE_FILENAME:
                                        Constants.PROCESS_REMOTE_PICTURE_FILENAME));

                        if (mLocalPicture)
                            mStatus = ProcessFragment.Status.WAIT_PICTURE;

                        else {








                            //load Contrast fragment

                            mStatus = ProcessFragment.Status.WAIT_CONTRAST;









                        }
                        break;
                    }

                    case WAIT_VIDEO: {





                        sleep();





                        break;
                    }
                    case WAIT_CONTRAST: {




                        sleep();





                        break;
                    }
                }
            }
            */
            Logs.add(Logs.Type.E, "Process loop stopped");
            return null;
        }

        /*
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            switch (mStatus) {

                case WAIT_VIDEO:
                case TRANSFER_VIDEO:
                case WAIT_PICTURE:
                case TRANSFER_PICTURE: {

                    mProgressBar.setMax(Frame.getInstance().getPacketTotal());
                    mProgressBar.setProgress(values[0]);

                    String status = getResources().getString(mStatus.getStringId());
                    status += " (" + values[0] + "/" + Frame.getInstance().getPacketTotal() + ")";
                    mProgressText.setText(status);
                    break;
                }
                default: {

                    if (mStatus == ProcessFragment.Status.SAVE_PICTURE)
                        mProgressBar.setMax(5);

                    //
                    mProgressBar.setProgress(values[0]);
                    mProgressText.setText(mStatus.getStringId());
                    break;
                }
            }
        }
        */
    };

    //
    private ProgressBar mProgressBar;
    private TextView mProgressText;

    private ImageView mClapPortrait, mClapLandscape;
    private void displayClapImage(int orientation) { // Display clap image according orientation

        if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) { // Portrait

            mClapLandscape.setVisibility(View.GONE);
            mClapPortrait.setVisibility(View.VISIBLE);
        }
        else { // Landscape

            mClapPortrait.setVisibility(View.GONE);
            mClapLandscape.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);



        /*
        Logs.add(Logs.Type.E, "1");


        // Start process thread
        mProcessTask = new ProcessTask();
        mProcessTask.execute();


        Logs.add(Logs.Type.E, "2");
        */

    }


    private class myTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            Logs.add(Logs.Type.E, "testage");

            return null;
        }
    }


    public void start() {

        Logs.add(Logs.Type.E, "1");


        // Start process thread
        //mProcessTask = new ProcessTask();
        //mProcessTask.execute();

        new myTask().execute();


        Logs.add(Logs.Type.E, "2");
    }


    //////
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Restore system sound (disabled to avoid sound when start and stop recording)
        ((AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE)).
                setStreamMute(AudioManager.STREAM_SYSTEM, false);

        final View rootView = inflater.inflate(R.layout.fragment_process, container, false);
        mProgressBar = (ProgressBar)rootView.findViewById(R.id.status_progress);
        mProgressText = (TextView)rootView.findViewById(R.id.status_text);

        mProgressBar.setMax(5);
        mProgressBar.setProgressDrawable(getResources().getDrawable(R.drawable.process_progess));
        mProgressText.setText(mStatus.getStringId());

        // Set unspecified orientation (default device orientation)
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        // Display 3D clap image animation
        mClapPortrait = (ImageView)rootView.findViewById(R.id.clap_image_top);
        mClapPortrait.post(new Runnable() {

            @Override
            public void run() {
                ((AnimationDrawable) mClapPortrait.getDrawable()).start();
            }
        });
        mClapLandscape = (ImageView)rootView.findViewById(R.id.clap_image_right);
        mClapLandscape.post(new Runnable() {

            @Override
            public void run() {
                ((AnimationDrawable) mClapLandscape.getDrawable()).start();
            }
        });
        displayClapImage(getResources().getConfiguration().orientation);

        // Start process thread
        //mProcessTask = new ProcessTask();
        //mProcessTask.execute();



        //start();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                Logs.add(Logs.Type.E, "test 1");

                mProcessTask = new ProcessTask();
                mProcessTask.execute();
            }
        }, 2000);



        return rootView;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        displayClapImage(newConfig.orientation);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mAbort = true;
        try { mProcessTask.get(); }
        catch (InterruptedException e) {
            Logs.add(Logs.Type.E, "Failed to interrupt process task: " + e.getMessage());
        }
        catch (ExecutionException e) {
            Logs.add(Logs.Type.E, "Failed to stop process task: " + e.getMessage());
        }
        mProcessTask = null;
    }
}
