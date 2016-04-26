package com.studio.artaban.anaglyph3d.media;

import android.os.Bundle;

import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.process.ProcessThread;
import com.studio.artaban.anaglyph3d.transfer.BufferRequest;
import com.studio.artaban.anaglyph3d.transfer.ConnectRequest;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by pascal on 21/04/16.
 * Frame class to manage frame conversion & transfer
 */
public class Frame extends BufferRequest {

    private static Frame ourInstance = new Frame();
    public static Frame getInstance() { return ourInstance; }
    private Frame() { super(ConnectRequest.REQ_FRAME); }

    // Data keys
    public static final String DATA_KEY_WIDTH = "width";
    public static final String DATA_KEY_HEIGHT = "height";

    //////
    @Override
    public String getRequest(byte type, Bundle data) {

        switch (type) {
            case REQ_TYPE_DOWNLOAD: break;
            case REQ_TYPE_UPLOAD: return Constants.CONN_REQUEST_TYPE_ASK;
        }

        // Get picture data
        mWidth = data.getInt(DATA_KEY_WIDTH);
        mHeight = data.getInt(DATA_KEY_HEIGHT);

        JSONObject request = getBufferRequest(type, data);
        if (request != null) {
            try {

                request.put(DATA_KEY_WIDTH, mWidth);
                request.put(DATA_KEY_HEIGHT, mHeight);

                return request.toString();
            }
            catch (JSONException e) {

                Logs.add(Logs.Type.E, e.getMessage());
                return null;
            }
        }
        return null;
    }
    @Override
    public String getReply(byte type, String request, PreviousMaster previous) {

        String result = getBufferReply(type, request);
        if (type == REQ_TYPE_UPLOAD)
            return result;

        JSONObject picture;
        try { picture = new JSONObject(request); }
        catch (JSONException e) {

            Logs.add(Logs.Type.E, e.getMessage());
            return null;
        }

        try {
            mWidth = picture.getInt(DATA_KEY_WIDTH);
            mHeight = picture.getInt(DATA_KEY_HEIGHT);

            return result;
        }
        catch (JSONException e) {
            Logs.add(Logs.Type.E, e.getMessage());
        }
        return null;
    }

    //////
    private int mWidth;
    private int mHeight;

    //
    public int getWidth() { return mWidth; }
    public int getHeight() { return mHeight; }

    //////
    public static boolean convertNV21toARGB(String source, int width, int height, String destination) {

        int size = (width * height * 3) >> 1; // NV21 buffer size
        return ProcessThread.mGStreamer.launch("filesrc location=" + source + " blocksize=" + size +
                " ! video/x-raw,format=NV21,width=" + width + ",height=" + height +
                ",framerate=1/1 ! videoconvert ! video/x-raw,format=ARGB ! filesink location=" +
                destination);
    }
}
