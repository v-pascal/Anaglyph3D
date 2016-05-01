package com.studio.artaban.anaglyph3d.media;

import android.os.Bundle;

import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.process.ProcessThread;
import com.studio.artaban.anaglyph3d.transfer.BufferRequest;
import com.studio.artaban.anaglyph3d.transfer.ConnectRequest;

import org.json.JSONObject;

/**
 * Created by pascal on 26/04/16.
 * Video class to manage video transfer & extraction
 */
public class Video extends BufferRequest {

    private static Video ourInstance = new Video();
    public static Video getInstance() { return ourInstance; }
    private Video() { super(ConnectRequest.REQ_VIDEO); }

    //////
    @Override
    public String getRequest(byte type, Bundle data) {

        switch (type) {
            case REQ_TYPE_DOWNLOAD: break;
            case REQ_TYPE_UPLOAD: return Constants.CONN_REQUEST_TYPE_ASK;
        }

        JSONObject request = getBufferRequest(type, data);
        if (request != null)
            return request.toString();

        return null;
    }
    @Override
    public String getReply(byte type, String request, PreviousMaster previous) {
        return getBufferReply(type, request);
    }

    //////
    private static final String AUDIO_WAV_FILENAME = "audio.wav";

    public static boolean extractFramesRGBA(String file, Frame.Orientation orientation, String frames) {

        return ProcessThread.mGStreamer.launch("filesrc location=\"" + file + "\" ! decodebin" +
                " ! videoflip method= method=" + orientation.getFlipMethod() + " ! videoconvert" +
                " ! video/x-raw,format=RGBA ! multifilesink location=\"" + frames + "\"");
    }
    public static void mergeFPS() {









    }
    public static boolean extractAudio(String file) {

        return ProcessThread.mGStreamer.launch("filesrc location=\"" + file + "\" ! decodebin" +
                " ! audioconvert ! wavenc ! filesink location=\"" + ActivityWrapper.DOCUMENTS_FOLDER +
                AUDIO_WAV_FILENAME + "\"");
    }
}
