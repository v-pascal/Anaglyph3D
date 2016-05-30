package com.studio.artaban.anaglyph3d.media;

import android.os.Bundle;

import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.process.ProcessThread;
import com.studio.artaban.anaglyph3d.transfer.BufferRequest;
import com.studio.artaban.anaglyph3d.transfer.IConnectRequest;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by pascal on 21/04/16.
 * Frame class to manage frame transfer & conversion
 */
public class Frame extends BufferRequest {

    private static Frame ourInstance = new Frame();
    public static Frame getInstance() { return ourInstance; }
    private Frame() { super(IConnectRequest.REQ_FRAME); }

    // Data keys
    public static final String DATA_KEY_WIDTH = "width";
    public static final String DATA_KEY_HEIGHT = "height";
    public static final String DATA_KEY_REVERSE = "reverse";

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
        mReverse = data.getBoolean(DATA_KEY_REVERSE);

        JSONObject request = getBufferRequest(type, data);
        if (request != null) {
            try {

                request.put(DATA_KEY_WIDTH, mWidth);
                request.put(DATA_KEY_HEIGHT, mHeight);
                request.put(DATA_KEY_REVERSE, mReverse);

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
            mReverse = picture.getBoolean(DATA_KEY_REVERSE);

            // Reply current reverse setting
            return ((Settings.getInstance().mReverse)?
                    Constants.CONN_REQUEST_ANSWER_TRUE:Constants.CONN_REQUEST_ANSWER_FALSE);
        }
        catch (JSONException e) {
            Logs.add(Logs.Type.E, e.getMessage());
        }
        return null;
    }

    @Override
    public ReceiveResult receiveReply(byte type, String reply) {

        mReverse = reply.equals(Constants.CONN_REQUEST_ANSWER_TRUE); // Receive remote reverse setting
        return (send())?
                ReceiveResult.SUCCESS:ReceiveResult.ERROR;
    }

    //////
    private int mWidth; // Frame width
    private int mHeight; // Frame height
    private boolean mReverse; // Reverse picture

    public int getWidth() { return mWidth; }
    public int getHeight() { return mHeight; }
    public boolean getReverse() { return mReverse; }

    //////
    public enum Orientation {

        LANDSCAPE ((short)0),
        PORTRAIT ((short)1),
        REVERSE_LANDSCAPE ((short)2),
        REVERSE_PORTRAIT ((short)3);

        //
        private final short flipMethod; // GStreamer flip method (to apply orientation)
        Orientation(short method) { flipMethod = method; }
        public short getFlipMethod() { return flipMethod; }
    };
    public static boolean convertNV21toRGBA(String source, int width, int height,
                                            String destination, Orientation orientation) {

        int size = (width * height * 3) >> 1; // NV21 buffer size
        return ProcessThread.mGStreamer.launch("filesrc location=\"" + source + "\" blocksize=" + size +
                " ! video/x-raw,format=NV21,width=" + width + ",height=" + height + ",framerate=1/1" +
                " ! videoflip method=" + orientation.getFlipMethod() + " ! videoconvert" +
                " ! video/x-raw,format=RGBA ! filesink location=\"" + destination + "\"");
    }

    public static boolean convertRGBAtoJPEG(String source, int width, int height, String destination) {

        int size = (width * height) << 2; // RGBA buffer size
        return ProcessThread.mGStreamer.launch("filesrc location=\"" + source + "\" blocksize=" + size +
                " ! video/x-raw,format=RGBA,width=" + width + ",height=" + height + ",framerate=1/1" +
                " ! videoconvert ! jpegenc ! filesink location=\"" + destination + "\"");
    }
}
