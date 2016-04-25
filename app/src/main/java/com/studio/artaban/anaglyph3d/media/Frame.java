package com.studio.artaban.anaglyph3d.media;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.process.ProcessThread;
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
    public static Frame getInstance() { return ourInstance; }
    private Frame() { }

    // Data keys
    public static final String DATA_KEY_WIDTH = "width";
    public static final String DATA_KEY_HEIGHT = "height";
    public static final String DATA_KEY_BUFFER = "buffer";
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
        mBuffer = data.getByteArray(DATA_KEY_BUFFER);
        mWidth = data.getInt(DATA_KEY_WIDTH);
        mHeight = data.getInt(DATA_KEY_HEIGHT);

        mTransferSize = 0;

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

                send(); // Send picture buffer
                return Constants.CONN_REQUEST_ANSWER_TRUE; // ...reply not sent (see 'getRequestBuffer')
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
            mBuffer = new byte[picture.getInt(DATA_KEY_BUFFER_SIZE)];

            mTransferSize = 0;

            return Constants.CONN_REQUEST_ANSWER_TRUE;
        }
        catch (JSONException e) {
            Logs.add(Logs.Type.E, e.getMessage());
        }
        return null;
    }

    @Override
    public ReceiveResult receiveReply(byte type, String reply) {

        send(); // Send picture buffer
        return ReceiveResult.SUCCESS;
    }
    @Override
    public ReceiveResult receiveBuffer(ByteArrayOutputStream buffer) {

        if (buffer.size() == 0)
            return ReceiveResult.NONE; // Nothing has been received

        ReceiveResult result = ReceiveResult.PARTIAL; // Buffer not fully received yet

        // Fill buffer received
        System.arraycopy(buffer.toByteArray(), 0, mBuffer, mTransferSize, buffer.size());
        mTransferSize += buffer.size();
        buffer.reset();

        if (mTransferSize == mBuffer.length)
            return ReceiveResult.SUCCESS; // Buffer fully received

        if (mTransferSize > mBuffer.length)
            return ReceiveResult.ERROR; // Error: Buffer received bigger than expected

        return ReceiveResult.PARTIAL; // ...buffer not fully received yet
    }

    //////
    private int mWidth;
    private int mHeight;
    private byte[] mBuffer = new byte[1]; // Must be defined (see 'getBufferSize' method)

    private volatile int mTransferSize = 0;

    //
    public byte[] getBuffer() { return mBuffer; }

    public int getBufferSize() { return mBuffer.length; }
    public int getTransferSize() { return mTransferSize; } // Return the number of byte sent or received

    private void send() { // Send picture buffer

        mTransferSize = 0;

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {

                for (int sent = 0; sent < mBuffer.length; sent += Bluetooth.MAX_SEND_BUFFER) {
                    int send = ((sent + Bluetooth.MAX_SEND_BUFFER) < mBuffer.length)?
                            Bluetooth.MAX_SEND_BUFFER:mBuffer.length - sent;

                    // Send buffer packet
                    if (!Connectivity.getInstance().send(mBuffer, sent, send)) {

                        Logs.add(Logs.Type.E, "Failed to send buffer packet");
                        break;
                    }
                    mTransferSize += send;
                }
            }
        });
    }

    //////
    public static boolean convertNV21toARGB(String source, int width, int height, String destination) {








        // Portrait ???

        int size = (width * height * 3) >> 1; // NV21 buffer size
        return ProcessThread.mGStreamer.launch("filesrc location=" + source + " blocksize=" + size +
                " ! video/x-raw,format=NV21,width=" + width + ",height=" + height +
                ",framerate=1/1 ! videoconvert ! video/x-raw,format=ARGB ! filesink location=" +
                destination);







    }
}
