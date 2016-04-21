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
    @Override public boolean getRequestBuffer(byte type) { return (type == REQ_TYPE_TRANSFER); }

    @Override public short getMaxWaitReply(byte type) { return Constants.CONN_MAXWAIT_DEFAULT; }

    @Override
    public String getRequest(byte type, Bundle data) {
        if (type != REQ_TYPE_TRANSFER)
            return null;

        // Get picture data
        mBuffer = data.getByteArray(ProcessFragment.PICTURE_RAW_BUFFER);
        mWidth = data.getInt(ProcessFragment.PICTURE_SIZE_WIDTH);
        mHeight = data.getInt(ProcessFragment.PICTURE_SIZE_HEIGHT);

        mPacketCount = 0;
        mPacketTotal = mBuffer.length >> 10;
        // Picture buffer size / 1024 (Bluetooth.MAX_RECEIVE_BUFFER)

        JSONObject request = new JSONObject();
        try {
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




        // Thread to send buffer
        // -> mPacketCount until mPacketTotal




        return ReceiveResult.WRONG;
    }
    @Override
    public ReceiveResult receiveBuffer(int size, ByteArrayOutputStream buffer) {






        //mPacketCount

        //System.arraycopy(mBuffer, mPacketCount * 1024, buffer.toByteArray(), 0, size);






        return ReceiveResult.WRONG;
    }

    //////
    private int mWidth;
    private int mHeight;

    private byte[] mBuffer;

    private int mPacketCount;
    private int mPacketTotal;

    //
    public byte[] getBuffer() { return mBuffer; }

    public int getPacketTotal() { return mPacketTotal; } // Return the total number of packet to send or receive
    public int getPacketCount() { return mPacketCount; } // Return the number of packet sent or received

    //////
    public static boolean convertNV21toARGB(String source, int width, int height, String destination) {

        int size = (width * height * 3) >> 1; // NV21 buffer size
        return ProcessFragment.mGStreamer.launch("filesrc location=" + source + " blocksize=" + size +
                " ! video/x-raw,format=NV21,width=" + width + ",height=" + height +
                ",framerate=1/1 ! videoconvert ! video/x-raw,format=ARGB ! filesink location=" +
                destination);
    }
}
