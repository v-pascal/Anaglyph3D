package com.studio.artaban.anaglyph3d.media;

import android.os.Bundle;

import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.process.ProcessFragment;
import com.studio.artaban.anaglyph3d.transfer.Bluetooth;
import com.studio.artaban.anaglyph3d.transfer.ConnectRequest;
import com.studio.artaban.anaglyph3d.transfer.Connectivity;

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
    public static final byte REQ_TYPE_DOWNLOAD = 1; // Download picture requested
    public static final byte REQ_TYPE_UPLOAD = 2; // Upload picture requested

    //////
    @Override public char getRequestId() { return ConnectRequest.REQ_FRAME; }
    @Override public boolean getRequestMerge() { return false; }
    @Override public BufferType getRequestBuffer(byte type) {
        switch (type) {

            case REQ_TYPE_DOWNLOAD: return BufferType.TO_RECEIVE;
            case REQ_TYPE_UPLOAD: return BufferType.TO_SEND;
            default: {

                Logs.add(Logs.Type.F, "Unexpected request type: " + type);
                return BufferType.NONE;
            }
        }
    }

    @Override public short getMaxWaitReply(byte type) { return Constants.CONN_MAXWAIT_DEFAULT; }

    @Override
    public String getRequest(byte type, Bundle data) {

        switch (type) {
            case REQ_TYPE_DOWNLOAD: break;
            case REQ_TYPE_UPLOAD: return Constants.CONN_REQUEST_TYPE_ASK;
        }

        // Get picture data
        mBuffer = data.getByteArray(ProcessFragment.PICTURE_RAW_BUFFER);
        mWidth = data.getInt(ProcessFragment.PICTURE_SIZE_WIDTH);
        mHeight = data.getInt(ProcessFragment.PICTURE_SIZE_HEIGHT);

        mBufferSize = mBuffer.length;

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

        switch (type) {
            case REQ_TYPE_DOWNLOAD: break;
            case REQ_TYPE_UPLOAD: {







                // Thread to send buffer
                // -> mPacketCount until mPacketTotal







                return Constants.CONN_REQUEST_ANSWER_TRUE; // Not sent
            }
        }

        JSONObject picture;
        try { picture = new JSONObject(request); }
        catch (JSONException e) {

            Logs.add(Logs.Type.E, e.getMessage());
            return null;
        }

        try {
            mWidth = picture.getInt(DATA_KEY_WIDTH);
            mHeight = picture.getInt(DATA_KEY_HEIGHT);
            mBufferSize = picture.getInt(DATA_KEY_BUFFER_SIZE);

            mBuffer = new byte[mBufferSize];

            mPacketCount = 0;
            mPacketTotal = mBufferSize >> 10;
            // Picture buffer size / 1024 (Bluetooth.MAX_RECEIVE_BUFFER)

            return Constants.CONN_REQUEST_ANSWER_TRUE;
        }
        catch (JSONException e) {
            Logs.add(Logs.Type.E, e.getMessage());
        }
        return null;
    }

    @Override
    public ReceiveResult receiveReply(byte type, String reply) {









        // Thread to send buffer
        // -> mPacketCount until mPacketTotal

        Connectivity.getInstance().sendBuffer(mBuffer);
        mPacketCount = mPacketTotal;









        return ReceiveResult.SUCCESS;
    }
    @Override
    public ReceiveResult receiveBuffer(int size, ByteArrayOutputStream buffer) {

        // Check if partial packet received
        if ((size < Bluetooth.MAX_RECEIVE_BUFFER) && (mPacketCount != mPacketTotal))
            return ReceiveResult.PARTIAL_PACKET; // ...less than the maximum buffer size

        // Fill maximum or remaining buffer received
        System.arraycopy(mBuffer, mPacketCount * Bluetooth.MAX_RECEIVE_BUFFER,
                buffer.toByteArray(), 0, size);
        buffer.reset();

        ++mPacketCount;

        if (mBuffer.length == mBufferSize)
            return ReceiveResult.SUCCESS; // Buffer fully received

        if (mBuffer.length > mBufferSize)
            return ReceiveResult.ERROR; // Error: Buffer received bigger than expected

        return ReceiveResult.PARTIAL; // ...buffer not fully received yet
    }

    //////
    private int mWidth;
    private int mHeight;

    private byte[] mBuffer;
    private int mBufferSize;

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
