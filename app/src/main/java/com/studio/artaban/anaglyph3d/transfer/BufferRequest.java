package com.studio.artaban.anaglyph3d.transfer;

import android.os.Bundle;

import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.Logs;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

/**
 * Created by pascal on 26/04/16.
 * Abstract class to manage buffer transfer
 */
public abstract class BufferRequest implements IConnectRequest {

    private char mRequestId;
    public BufferRequest(char id) { mRequestId = id; }

    // Data keys
    public static final String DATA_KEY_BUFFER = "buffer";
    private static final String DATA_KEY_BUFFER_SIZE = "size";

    // Request types
    public static final byte REQ_TYPE_DOWNLOAD = 1; // Request to download buffer (upload to remote device)
    public static final byte REQ_TYPE_UPLOAD = 2; // Request to upload buffer (download to current device)

    //////
    @Override public final char getRequestId() { return mRequestId; }
    @Override public final boolean getRequestMerge() { return false; }
    @Override public final BufferType getRequestBuffer(byte type) {
        switch (type) {

            case REQ_TYPE_DOWNLOAD: return BufferType.TO_RECEIVE;
            case REQ_TYPE_UPLOAD: return BufferType.TO_SEND;
            default: {

                Logs.add(Logs.Type.F, "Unexpected request type: " + type);
                return BufferType.NONE;
            }
        }
    }

    @Override public final short getMaxWaitReply(byte type) { return Constants.CONN_MAXWAIT_DEFAULT; }

    @Override
    public final ReceiveResult receiveReply(byte type, String reply) {
        return (send())? ReceiveResult.SUCCESS:ReceiveResult.ERROR;
    }
    @Override
    public final ReceiveResult receiveBuffer(ByteArrayOutputStream buffer) {

        if (buffer.size() == 0)
            return ReceiveResult.NONE; // Nothing has been received

        // Fill buffer received
        try {
            System.arraycopy(buffer.toByteArray(), 0, mBuffer, mTransferSize, buffer.size());
            mTransferSize += buffer.size();
            buffer.reset();
        }
        catch (ArrayIndexOutOfBoundsException e) {

            Logs.add(Logs.Type.F, e.getMessage());
            return ReceiveResult.ERROR;
        }

        //
        if (mTransferSize == mBuffer.length)
            return ReceiveResult.SUCCESS; // Buffer fully received

        if (mTransferSize > mBuffer.length)
            return ReceiveResult.ERROR; // Error: Buffer received bigger than expected

        Logs.add(Logs.Type.D, mRequestId + " - Received: " + mTransferSize + "/" + mBuffer.length);
        return ReceiveResult.PARTIAL; // ...buffer not fully received yet
    }

    //////
    private byte[] mBuffer = new byte[1]; // Must be defined (see 'getBufferSize' method)
    private int mTransferSize = 0;

    private boolean send() { // Send buffer...

        mTransferSize = 0;

        try {
            ActivityWrapper.get().runOnUiThread(new Runnable() { // ...via UI thread
                @Override
                public void run() {

                    int waitEvery = 0;
                    for (int sent = 0; sent < mBuffer.length; sent += Bluetooth.MAX_SEND_BUFFER) {
                        int send = ((sent + Bluetooth.MAX_SEND_BUFFER) < mBuffer.length) ?
                                Bluetooth.MAX_SEND_BUFFER : mBuffer.length - sent;

                        // Send buffer packet
                        if (!Connectivity.getInstance().send(mBuffer, sent, send)) {

                            Logs.add(Logs.Type.E, "Failed to send buffer packet");
                            break;
                        }
                        mTransferSize += send;

                        if (++waitEvery == 25) { // Wait 50 ms every 25 packets sent
                            try { Thread.sleep(50, 0); }
                            catch (InterruptedException e) {
                                Logs.add(Logs.Type.W, e.getMessage());
                            }
                            waitEvery = 0;
                        }
                    }
                }
            });
            return true;
        }
        catch (NullPointerException e) {

            Logs.add(Logs.Type.F, "Wrong activity reference");
            return false;
        }
    }

    //
    public final byte[] getBuffer() { return mBuffer; }

    public final int getBufferSize() { return mBuffer.length; }
    public final int getTransferSize() { return mTransferSize; } // Return the number of byte sent or received

    public final JSONObject getBufferRequest(byte type, Bundle data) {

        switch (type) {
            case REQ_TYPE_DOWNLOAD: break;
            case REQ_TYPE_UPLOAD: return null;
        }

        // Get buffer data
        mBuffer = data.getByteArray(DATA_KEY_BUFFER);
        mTransferSize = 0;

        JSONObject request = new JSONObject();
        try {
            request.put(DATA_KEY_BUFFER_SIZE, mBuffer.length);
            return request;
        }
        catch (JSONException e) {

            Logs.add(Logs.Type.E, e.getMessage());
            return null;
        }
    }
    public final String getBufferReply(byte type, String request) {

        switch (type) {
            case REQ_TYPE_DOWNLOAD: break;
            case REQ_TYPE_UPLOAD: {

                send(); // Send buffer
                return Constants.CONN_REQUEST_ANSWER_TRUE; // ...reply not sent (see 'getRequestBuffer')
            }
        }

        JSONObject buffer;
        try { buffer = new JSONObject(request); }
        catch (JSONException e) {

            Logs.add(Logs.Type.E, e.getMessage());
            return null;
        }

        try {
            mBuffer = new byte[buffer.getInt(DATA_KEY_BUFFER_SIZE)];
            mTransferSize = 0;

            return Constants.CONN_REQUEST_ANSWER_TRUE;
        }
        catch (JSONException e) {
            Logs.add(Logs.Type.E, e.getMessage());
        }
        return null;
    }
}
