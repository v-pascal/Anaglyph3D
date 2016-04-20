package com.studio.artaban.anaglyph3d.process;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
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
import com.studio.artaban.libGST.GstObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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

        ////// Contrast & brightness step: 3 status
        SAVE_PICTURE (R.string.status_save_raw), // Save raw picture (used to set up the contrast & brightness)

        // Progress for each 1024 bytes packets...
        TRANSFER_PICTURE (R.string.status_transfer_raw), // Transfer picture (to remote device which is not the maker)
        WAIT_PICTURE(0), // Wait until contrast & brightness picture has been received (to device which is not the maker)

        CONVERT_PICTURE (R.string.status_convert_raw), // Convert local picture from NV21 to ARGB or JPEG

        ////// Video transfer & extraction step: 7 status

        // Progress for each 1024 bytes packets...
        TRANSFER_VIDEO_SOURCE(0), // Transfer video
        WAIT_VIDEO(0), // Wait until video has been received

        EXTRACT_FRAMES_LEFT(0), // Extract RGB pictures from left camera
        EXTRACT_FRAMES_RIGHT(0), // Extract RGB pictures from right camera
        EXTRACT_AUDIO(0), // Extract audio from one of the videos
        MERGE_FPS(0), // Remove the too many RGB pictures from camera video with bigger FPS

        TRANSFER_CONTRAST(0), // Transfer the contrast & brightness (from device which is not the maker)
        WAIT_CONTRAST(0), // Wait until contrast & brightness have been received

        ////// Make & transfer 3D video step: 3 status

        // Progress for each pictures extracted from videos...
        APPLY_FRAME_CHANGES(0), // Color frame (in blue or red), and apply contrast & brightness

        MAKE_3D_VIDEO(0), // Make the anaglyph 3D video

        // Progress for each 1024 bytes packets...
        TRANSFER_3D_VIDEO(0), // Transfer 3D video (to remote device which is not the maker)
        WAIT_3D_VIDEO(0), // Wait 3D video received

        FINISHED(Constants.NO_DATA);

        //
        private final int mStringId;
        Status(int id) { mStringId = id; }
        public int getStringId() { return mStringId; }
    }
    private Status mStatus = Status.SAVE_PICTURE;

    private static GstObject mGStreamer;

    private ProcessTask mProcessTask;
    private class ProcessTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            Logs.add(Logs.Type.I, "Start process loop");
            while (mStatus != ProcessFragment.Status.FINISHED) {
                switch (mStatus) {

                    case SAVE_PICTURE: {

                        publishProgress(1);

                        // Initialize GStreamer library
                        if (mGStreamer == null)
                            mGStreamer = new GstObject(getContext());

                        // Save NV21 raw picture file
                        try {
                            byte[] raw = getArguments().getByteArray(PICTURE_RAW_BUFFER);
                            File rawFile = new File(ActivityWrapper.DOCUMENTS_FOLDER,
                                    Constants.PROCESS_RAW_PICTURE_FILENAME);

                            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(rawFile));
                            bos.write(raw);
                            bos.flush();
                            bos.close();
                        }
                        catch (IOException e) {

                            Logs.add(Logs.Type.E, "Failed to save raw picture");
                            mStatus = ProcessFragment.Status.FINISHED;

                            // Inform user
                            DisplayMessage.getInstance().alert(R.string.title_error, R.string.save_error,
                                    null, false, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ActivityWrapper.stopActivity(ProcessActivity.class, Constants.NO_DATA);
                                }
                            });
                            break;
                        }

                        if (!Settings.getInstance().isMaker())
                            mStatus = ProcessFragment.Status.CONVERT_PICTURE;
                        else {

                            mStatus = ProcessFragment.Status.TRANSFER_PICTURE;






                        }
                        break;
                    }
                    case TRANSFER_PICTURE: {





                        break;
                    }
                    case CONVERT_PICTURE: {

                        publishProgress(2);

                        // Convert NV21 to ARGB picture file




                        /*

                        int width = getArguments().getInt(PICTURE_SIZE_WIDTH);
                        int height = getArguments().getInt(PICTURE_SIZE_HEIGHT);


                        File pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

                        GstObject gst = new GstObject(getContext());
                        gst.launch("filesrc location=" + pictures + "/temp.jpg ! jpegdec ! videoconvert ! video/x-raw,format=ARGB !" +
                                " filesink location=" + pictures + "/temp.bin");
                                */

                        //filesrc location=testage.nv21 blocksize=460800 ! video/x-raw,format=NV21,width=640,height=480,framerate=1/1 ! videoconvert ! jpegenc ! filesink location=temp.jpg

                        //filesrc location=myrec1.3gp ! qtdemux ! decodebin ! audioconvert ! wavenc ! filesink location=file1.wav
                        //filesrc location=myrec1.3gp ! qtdemux ! decodebin ! videoconvert ! video/x-raw,format=RGB ! multifilesink location=img_%d.bin




                        mStatus = ProcessFragment.Status.WAIT_CONTRAST;


                        break;
                    }
                }
            }
            Logs.add(Logs.Type.I, "Process loop stopped");
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            mProgressBar.setProgress(values[0]);
            mProgressText.setText(mStatus.getStringId());
        }
    };

    //
    private ProgressBar mProgressBar;
    private TextView mProgressText;

    //////
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Restore system sound (disabled to avoid sound when start and stop recording)
        ((AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE)).
                setStreamMute(AudioManager.STREAM_SYSTEM, false);

        final View rootView = inflater.inflate(R.layout.fragment_process, container, false);
        mProgressBar = (ProgressBar)rootView.findViewById(R.id.status_progress);
        mProgressText = (TextView)rootView.findViewById(R.id.status_text);

        mProgressBar.setMax(3);
        mProgressBar.setProgressDrawable(getResources().getDrawable(R.drawable.process_progess));
        mProgressText.setText(mStatus.getStringId());

        // Display 3D clap image animation
        final ImageView clapImage = (ImageView)rootView.findViewById(R.id.clap_image);
        if (clapImage != null) {
            clapImage.setImageDrawable(getResources().getDrawable(R.drawable.clap_anim));
            clapImage.post(new Runnable() {

                @Override
                public void run() {
                    ((AnimationDrawable) clapImage.getDrawable()).start();
                }
            });
        }

        // Start process thread
        mProcessTask = new ProcessTask();
        mProcessTask.execute();
        return rootView;
    }
}
