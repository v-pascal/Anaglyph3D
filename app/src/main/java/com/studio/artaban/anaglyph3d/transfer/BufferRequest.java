package com.studio.artaban.anaglyph3d.transfer;

import android.os.Bundle;

import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.helpers.Logs;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * Created by pascal on 26/04/16.
 * Abstract class to manage buffer transfer
 */
public abstract class BufferRequest implements ConnectRequest {

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

        send(); // Send buffer
        return ReceiveResult.SUCCESS;
    }
    @Override
    public final ReceiveResult receiveBuffer(ByteArrayOutputStream buffer) {

        if (buffer.size() == 0)
            return ReceiveResult.NONE; // Nothing has been received









        if (((mBuffer.length - mTransferSize) >= (Bluetooth.MAX_SEND_BUFFER - 4)) &&
                (buffer.size() < Bluetooth.MAX_SEND_BUFFER))
            return ReceiveResult.NONE;




        if ((mBuffer.length - mTransferSize) <= (Bluetooth.MAX_SEND_BUFFER - 4)) {

            ByteBuffer bufferOffset = ByteBuffer.wrap(buffer.toByteArray(), 0, 4);
            System.arraycopy(buffer.toByteArray(), 4, mBuffer, bufferOffset.getInt(), buffer.size() - 4);
            mTransferSize += buffer.size() - 4;
        }
        else if ((buffer.size() % Bluetooth.MAX_SEND_BUFFER) == 0) {

            for (int offset = 0; offset < buffer.size(); offset += Bluetooth.MAX_SEND_BUFFER) {

                ByteBuffer bufferOffset = ByteBuffer.wrap(buffer.toByteArray(), offset, 4);
                System.arraycopy(buffer.toByteArray(), offset + 4, mBuffer, bufferOffset.getInt(),
                        Bluetooth.MAX_SEND_BUFFER - 4);
                mTransferSize += Bluetooth.MAX_SEND_BUFFER - 4;
            }
        }
        else
            return ReceiveResult.NONE;

        buffer.reset();










        // Fill buffer received
        //System.arraycopy(buffer.toByteArray(), 0, mBuffer, mTransferSize, buffer.size());
        //mTransferSize += buffer.size();
        //buffer.reset();








        if (mTransferSize == mBuffer.length)
            return ReceiveResult.SUCCESS; // Buffer fully received

        if (mTransferSize > mBuffer.length)
            return ReceiveResult.ERROR; // Error: Buffer received bigger than expected

        Logs.add(Logs.Type.D, mRequestId + " - Received: " + mTransferSize + "/" + mBuffer.length);
        return ReceiveResult.PARTIAL; // ...buffer not fully received yet






    }

    //////
    private byte[] mBuffer = new byte[1]; // Must be defined (see 'getBufferSize' method)
    private volatile int mTransferSize = 0;

    private void send() { // Send buffer

        mTransferSize = 0;

        new Thread(new Runnable() {
            @Override
            public void run() {

                int waitEvery = 0;
                for (int sent = 0; sent < mBuffer.length; sent += Bluetooth.MAX_SEND_BUFFER - 4) {
                /*
                for (int sent = 0; sent < mBuffer.length; sent += Bluetooth.MAX_SEND_BUFFER) {
                    int send = ((sent + Bluetooth.MAX_SEND_BUFFER) < mBuffer.length)?
                            Bluetooth.MAX_SEND_BUFFER:mBuffer.length - sent;

                    // Send buffer packet
                    if (!Connectivity.getInstance().send(mBuffer, sent, send)) {

                        Logs.add(Logs.Type.E, "Failed to send buffer packet");
                        break;
                    }
                    mTransferSize += send;
                    */





                    int send = ((sent + Bluetooth.MAX_SEND_BUFFER - 4) < mBuffer.length)?
                            Bluetooth.MAX_SEND_BUFFER - 4:mBuffer.length - sent;

                    // Send offset
                    ByteBuffer bufferOffset = ByteBuffer.allocate(4);
                    bufferOffset.putInt(sent);
                    if (!Connectivity.getInstance().send(bufferOffset.array(), 0, 4)) {

                        Logs.add(Logs.Type.E, "Failed to send buffer offset");
                        break;
                    }

                    // Send buffer packet
                    if (!Connectivity.getInstance().send(mBuffer, sent, send)) {

                        Logs.add(Logs.Type.E, "Failed to send buffer packet");
                        break;
                    }
                    mTransferSize += send;









                    if (++waitEvery == 50) { // Wait 100 ms every 50 packets sent
                        try { Thread.sleep(100, 0); }
                        catch (InterruptedException e) {
                            Logs.add(Logs.Type.W, e.getMessage());
                        }
                        waitEvery = 0;
                    }
                }
            }
        }).start();
    }
    public boolean send(int missing) { // Send only bytes not received by the remote device
                                       // -> After the maximum time limit to receive buffer has expired

        return Connectivity.getInstance().send(mBuffer, mBuffer.length - missing, missing);

        // BUG: Sometime the remote device will not receive the entire buffer after having sent it from
        //      method above. A new request has been added to avoid this issue (see 'Connectivity' class)
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
