package com.studio.artaban.anaglyph3d.media;

import android.os.Bundle;

import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.helpers.Logs;
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
        try { return request.toString(); }
        catch (NullPointerException e) {

            Logs.add(Logs.Type.E, e.getMessage());
            return null;
        }
    }
    @Override
    public String getReply(byte type, String request, PreviousMaster previous) {
        return getBufferReply(type, request);
    }

    //////








}
