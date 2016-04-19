package com.studio.artaban.anaglyph3d.process;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.studio.artaban.anaglyph3d.R;
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

    //////
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Restore system sound (disabled to avoid sound when start and stop recording)
        ((AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE)).
                setStreamMute(AudioManager.STREAM_SYSTEM, false);




        //getArguments()

        /*
        File pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        GstObject gst = new GstObject(getContext());
        gst.launch("filesrc location=" + pictures + "/temp.jpg ! jpegdec ! videoconvert ! video/x-raw,format=RGB !" +
                " filesink location=" + pictures + "/temp..bin");
                */

        //filesrc location=testage.nv21 blocksize=460800 ! video/x-raw,format=NV21,width=640,height=480,framerate=1/1 ! videoconvert ! jpegenc ! filesink location=temp.jpg

        //filesrc location=myrec1.3gp ! qtdemux ! decodebin ! audioconvert ! wavenc ! filesink location=file1.wav
        //filesrc location=myrec1.3gp ! qtdemux ! decodebin ! videoconvert ! video/x-raw,format=RGB ! multifilesink location=img_%d.bin






        return inflater.inflate(R.layout.fragment_process, container, false);
    }
}
