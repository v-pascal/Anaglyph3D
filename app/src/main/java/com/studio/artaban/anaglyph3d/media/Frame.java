package com.studio.artaban.anaglyph3d.media;

import android.os.Bundle;

import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.process.ProcessFragment;
import com.studio.artaban.anaglyph3d.transfer.ConnectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

/**
 * Created by pascal on 21/04/16.
 * Frame class to manage frame conversion & transfer
 */
public class Frame implements ConnectRequest {

    private static Frame ourInstance = new Frame();
    public static Frame getInstance() {
        return ourInstance;
    }
    private Frame() { }

    // Data keys
    private static final String DATA_KEY_WIDTH = "width";
    private static final String DATA_KEY_HEIGHT = "height";
    private static final String DATA_KEY_BUFFER_SIZE = "size";

    // Request types
    public static final byte REQ_TYPE_TRANSFER = 1; // Transfer picture

    //////
    @Override public char getRequestId() { return ConnectRequest.REQ_FRAME; }
    @Override public boolean getRequestMerge() { return false; }
    @Override public boolean getRequestBuffer(byte type) { return true; }

    @Override public short getMaxWaitReply(byte type) { return Constants.CONN_MAXWAIT_DEFAULT; }

    @Override
    public String getRequest(byte type, Bundle data) {
        if (type != REQ_TYPE_TRANSFER)
            return null;

        // Get picture buffer
        mBuffer = data.getByteArray(ProcessFragment.PICTURE_RAW_BUFFER);

        JSONObject request = new JSONObject();
        try {
            mWidth = data.getInt(ProcessFragment.PICTURE_SIZE_WIDTH);
            mHeight = data.getInt(ProcessFragment.PICTURE_SIZE_HEIGHT);

            request.put(DATA_KEY_WIDTH, mWidth);
            request.put(DATA_KEY_HEIGHT, mHeight);
            request.put(DATA_KEY_BUFFER_SIZE, mBuffer.length);

            return request.toString();
        }
        catch (JSONException e) {

            Logs.add(Logs.Type.E, e.getMessage());
            return null;
        }
    }
    @Override
    public String getReply(byte type, String request, PreviousMaster previous) {





        return null;
    }

    @Override
    public ReceiveResult receiveReply(byte type, String reply) {







        return ReceiveResult.WRONG;
    }
    @Override
    public ReceiveResult receiveBuffer(int size, ByteArrayOutputStream buffer) {






        return ReceiveResult.WRONG;
    }

    //////
    private int mWidth;
    private int mHeight;

    private byte[] mBuffer;

    //
    public int getPacketCount() {

        return 0;
    }
}
