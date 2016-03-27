package com.studio.artaban.anaglyph3d.transfer;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import com.studio.artaban.anaglyph3d.MainActivity;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.helpers.Logs;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by pascal on 20/03/16.
 * Connectivity module
 */
public class Connectivity {

    private static Connectivity ourInstance = new Connectivity();
    public static Connectivity getInstance() { return ourInstance; }
    private Connectivity() { }

    //////
    private AsyncTask<Void, Void, Void> mProcessTask;
    private final Bluetooth mBluetooth = new Bluetooth();
    private final ByteArrayOutputStream mRead = new ByteArrayOutputStream();
    private boolean mAbort = true;

    private enum Status {
        UNDEFINED,

        ////// Not connected
        RESET,    // RAZ
        DISCOVER, // Discover new device(s)
        CONNECT,  // Try to connect to each devices found
        LISTEN,   // Wait devices connection

        ////// Connected
        STAND_BY,  // Send/Receive requests
        WAIT_REPLY // Receive replies
    }
    private Status mStatus = Status.UNDEFINED;

    //
    private class TransferElement {

        public ConnRequest mHandler;
        public byte mType;
        public String mMessage;
    }
    private final List<TransferElement> mRequests = new ArrayList<TransferElement>();

    public void addRequest(ConnRequest handler, byte type, Bundle data) {

        // Get request message from handler
        String message = handler.getRequest(type, data);
        if (message == null)
            return;

        // Add request into request list
        TransferElement request = new TransferElement();
        request.mHandler = handler;
        request.mType = type;
        request.mMessage = message;

        synchronized (mRequests) { mRequests.add(request); }
    }

    ////// Request/Reply element format: BBB-A:CC_*
    // _ BBB -> Digital size of the entire message (in decimal)
    // _ A -> Request ID
    // _ CC -> Request type (in hex)
    // _ * -> Message
    private static final char SEPARATOR_SIZE_REQUEST_ID = '-';
    private static final char SEPARATOR_REQUEST_ID_TYPE = ':';
    private static final char SEPARATOR_TYPE_MESSAGE = '_';

    // Receive request or reply
    private String receive() {

        int size = mBluetooth.read(mRead);
        if (size > 0) {

            String message = mRead.toString();

            // Check element format received
            if ((size > 9) &&
                    ((message.charAt(3) != SEPARATOR_SIZE_REQUEST_ID) ||
                            (message.charAt(5) != SEPARATOR_REQUEST_ID_TYPE) ||
                            (message.charAt(8) != SEPARATOR_TYPE_MESSAGE))) {

                Logs.add(Logs.Type.E, "Wrong request/reply format received");










                return null;
            }
            if (size == Integer.parseInt(message)) {

                mRead.reset();
                return message.substring(4); // Format: A:CC_*
            }
        }
        return null;
    }

    // Send request or reply
    private boolean send(TransferElement element) {

        Integer intSize = element.mMessage.length() + 9;
        ByteBuffer buffer = ByteBuffer.allocate(intSize);

        // Add size of the entire message (BBB)
        String strSize = intSize.toString();
        if (intSize < 100) { // ...always > 9

            buffer.putChar('0');
            buffer.putChar(strSize.charAt(0));
            buffer.putChar(strSize.charAt(1));
        }
        else { // > 100

            buffer.putChar(strSize.charAt(0));
            buffer.putChar(strSize.charAt(1));
            buffer.putChar(strSize.charAt(2));
        }
        buffer.putChar(SEPARATOR_SIZE_REQUEST_ID);

        // Add request ID (A)
        buffer.putChar(element.mHandler.getRequestId());
        buffer.putChar(SEPARATOR_REQUEST_ID_TYPE);

        // Add request type (CC)
        String strType = Integer.toString(element.mType, 16);
        buffer.putChar(strType.charAt(0));
        buffer.putChar(strType.charAt(1));
        buffer.putChar(SEPARATOR_TYPE_MESSAGE);

        // Add message (*)
        buffer.put(element.mMessage.getBytes());

        return mBluetooth.write(buffer.array());
    }

    //
    private class ProcessTask extends AsyncTask<Void, Void, Void> {

        private final Context mContext;
        public ProcessTask(Context context) { mContext = context; }

        // Initialize connection
        private void initialize(boolean position) {

            // Add request to initialize settings
            final Bundle connInfo = new Bundle();
            connInfo.putString(Settings.DATA_KEY_REMOTE_DEVICE,
                    mBluetooth.getRemoteDevice().substring(0,
                            mBluetooth.getRemoteDevice().indexOf(Bluetooth.DEVICES_SEPARATOR)));
            connInfo.putBoolean(Settings.DATA_KEY_POSITION, position);
            Connectivity.getInstance().addRequest(Settings.getInstance(),
                    Settings.REQ_TYPE_INITIALIZE, connInfo);
        }

        // Start main activity
        private void startActivity() {
            Intent intent = new Intent(mContext, MainActivity.class);
            mContext.startActivity(intent);
        }

        ////// Process (connected status):
        // _ Send requests (from request list)
        // _ Receive data: requests & replies (from remote device)
        private void process() {

            if (mStatus == Connectivity.Status.STAND_BY) {

                // Check if received request
                String request = receive();
                if (request != null) {

                    TransferElement reply = new TransferElement();
                    switch (Integer.parseInt(request)) {

                        case ConnRequest.REQ_SETTINGS:  reply.mHandler = Settings.getInstance(); break;
                        default: {

                            Logs.add(Logs.Type.E, "Unexpected request received");









                            return;
                        }
                    }
                    reply.mType = (byte)Integer.parseInt(request.substring(2), 16);
                    reply.mMessage = reply.mHandler.getReply(reply.mType, request.substring(5));
                    if (reply.mMessage == null) {

                        Logs.add(Logs.Type.E, "Failed to reply to request");









                        return;
                    }

                    // Send reply
                    if (!send(reply)) {

                        Logs.add(Logs.Type.E, "Failed to send reply");







                        return;
                    }
                }

                // Check if any request to send
                synchronized (mRequests) {
                    if (mRequests.isEmpty())
                        return;

                    // Send request
                    if (!send(mRequests.get(0))) {

                        Logs.add(Logs.Type.E, "Failed to send request");







                        return;
                    }
                    mStatus = Connectivity.Status.WAIT_REPLY;
                }
            }
            else { // Wait reply

                // Check if received reply
                String reply = receive();
                if (reply != null) {

                    if (!mRequests.get(0).mHandler.receiveReply(
                            (byte)Integer.parseInt(reply.substring(2), 16), reply.substring(5))) {

                        Logs.add(Logs.Type.E, "Unexpected reply received");








                    }
                    mRequests.remove(0);
                }
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            Logs.add(Logs.Type.I, "Connectivity thread started");

            short devIndex = 0, waitListen = 0;
            while(!mAbort) {

                switch (mStatus) {
                    case RESET: {

                        mBluetooth.reset();
                        mBluetooth.discover();
                        mStatus = Connectivity.Status.DISCOVER;
                        break;
                    }
                    case DISCOVER: {

                        if (!mBluetooth.isDiscovering()) {
                            mStatus = Connectivity.Status.CONNECT;
                            devIndex = 0;
                        }
                        break;
                    }
                    case CONNECT: {

                        switch (mBluetooth.getStatus()) {
                            case CONNECTING: break; // Still trying to connect
                            case CONNECTED: {
                                Logs.add(Logs.Type.I, "Connected (MASTER)");
                                initialize(true);

                                mStatus = Connectivity.Status.STAND_BY;
                                break;
                            }
                            case READY: {

                                final String device = mBluetooth.getDevice(devIndex++);
                                if (device != null) {

                                    String devAddress =
                                            device.substring(device.indexOf(Bluetooth.DEVICES_SEPARATOR) + 1);
                                    mBluetooth.connect(true, Constants.CONN_SECURE_UUID, devAddress);
                                }
                                else {

                                    mBluetooth.listen(false, Constants.CONN_SECURE_UUID, Constants.CONN_SECURE_NAME);
                                    mStatus = Connectivity.Status.LISTEN;
                                    waitListen = 0;
                                }
                                break;
                            }
                        }
                        break;
                    }
                    case LISTEN: {

                        switch (mBluetooth.getStatus()) {
                            case LISTENING: { // Still listening

                                if (++waitListen == 25) // 25 * 200 == 5 seconds
                                    mStatus = Connectivity.Status.RESET;
                                break;
                            }
                            case CONNECTED: {
                                Logs.add(Logs.Type.I, "Connected (SLAVE)");
                                initialize(false);

                                mStatus = Connectivity.Status.STAND_BY;
                                break;
                            }
                        }
                        break;
                    }
                    default: { // Connected

                        process();
                        break;
                    }
                }
                if (mAbort)
                    break; // Exit immediately

                // Sleep
                try { Thread.sleep(200, 0); }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Logs.add(Logs.Type.I, "Connectivity thread ended");
            return null;
        }
    }

    //////
    public boolean start(Context context) {

        if ((mBluetooth.getStatus() == Bluetooth.Status.DISABLED) && (!mBluetooth.initialize()))
            return false;

        if (mStatus != Status.UNDEFINED) {
            Logs.add(Logs.Type.W, "Connectivity already started");
            return true;
        }
        mAbort = false;
        mStatus = Status.RESET;
        mProcessTask = new ProcessTask(context);
        mProcessTask.execute();
        return true;
    }
    public void reset() { mBluetooth.reset(); }

    //
    public void resume(Context context) { mBluetooth.register(context); }
    public void pause(Context context) { mBluetooth.unregister(context); }
    public void destroy() {

        if (!mAbort) {

            mAbort = true;
            try { mProcessTask.get(); }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            catch (ExecutionException e) {
                e.printStackTrace();
            }
            mProcessTask = null;
            mStatus = Status.UNDEFINED;
        }
        mBluetooth.release();
    }
}
