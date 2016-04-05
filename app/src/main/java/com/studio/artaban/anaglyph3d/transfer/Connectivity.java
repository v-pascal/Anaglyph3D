package com.studio.artaban.anaglyph3d.transfer;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.MainActivity;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;
import com.studio.artaban.anaglyph3d.helpers.Logs;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
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
    public final ArrayList<String> mNotMatchingDevices = new ArrayList<String>();
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

    private boolean mDisconnectRequest = false;
    private boolean mDisconnectError = false;

    private class TransferElement {

        public ConnectRequest mHandler;
        public byte mType;
        public String mMessage;
    }
    private final List<TransferElement> mRequests = new ArrayList<TransferElement>();

    //
    public boolean addRequest(ConnectRequest handler, byte type, Bundle data) {

        if (!isConnected())
            return false;

        // Get request message from handler
        String message = handler.getRequest(type, data);
        if (message == null)
            return false;





        Logs.add(Logs.Type.I, "addRequest: OK - " + type);






        // Add request into request list
        TransferElement request = new TransferElement();
        request.mHandler = handler;
        request.mType = type;
        request.mMessage = message;

        synchronized (mRequests) { mRequests.add(request); }

        return true;
    }
    private boolean mergeRequests() {

        if (mRequests.isEmpty())
            return false;

        // Merge requests with same request Id that follows
        char requestId = mRequests.get(0).mHandler.getRequestId();
        byte requestType = mRequests.get(0).mType;

        int removeCount = 0;
        for (int i = 1; i < mRequests.size(); ++i) {

            if (requestId == mRequests.get(i).mHandler.getRequestId()) {

                requestType |= mRequests.get(i).mType;
                mRequests.get(i).mType = requestType;
                ++removeCount;
            }
            else
                break;
        }

        // Remove merged request(s)
        while (removeCount-- != 0)
            mRequests.remove(0);

        return true; // Send request
    }

    public void disconnect() { mDisconnectRequest = true; }
    private boolean isConnected() {

        return (!mDisconnectRequest) && (!mDisconnectError) &&
                (mBluetooth.getStatus() == Bluetooth.Status.CONNECTED);
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

            String message;
            try { message = mRead.toString("UTF-8"); }
            catch (UnsupportedEncodingException e) {

                Logs.add(Logs.Type.E, "Failed to get request UTF-8 encoded string");
                return null;
            }

            // Check element format received
            if ((size > 9) &&
                    ((message.charAt(3) != SEPARATOR_SIZE_REQUEST_ID) ||
                            (message.charAt(5) != SEPARATOR_REQUEST_ID_TYPE) ||
                            (message.charAt(8) != SEPARATOR_TYPE_MESSAGE))) {

                Logs.add(Logs.Type.E, "Wrong request/reply format received");
                mDisconnectError = true;
                return null;
            }
            if (size == Integer.parseInt(message.substring(0, 3))) {

                // Full message received
                mRead.reset();
                return message.substring(4); // Format: A:CC_*
            }
        }
        return null;
    }

    // Send request or reply
    private boolean send(TransferElement element) {

        Integer byteCount = element.mMessage.length() + 9;
        ByteBuffer buffer = ByteBuffer.allocate(byteCount);

        // Add size of the entire message (BBB)
        String strSize = byteCount.toString();
        if (byteCount < 100) { // ...always > 9

            buffer.put((byte) '0');
            buffer.put((byte) strSize.charAt(0));
            buffer.put((byte) strSize.charAt(1));
        }
        else { // > 100

            buffer.put((byte) strSize.charAt(0));
            buffer.put((byte) strSize.charAt(1));
            buffer.put((byte) strSize.charAt(2));
        }
        buffer.put((byte) SEPARATOR_SIZE_REQUEST_ID);

        // Add request ID (A)
        buffer.put((byte) element.mHandler.getRequestId());
        buffer.put((byte) SEPARATOR_REQUEST_ID_TYPE);

        // Add request type (CC)
        String strType = Integer.toString(element.mType, 16);
        if (strType.length() > 1) {

            buffer.put((byte) strType.charAt(0));
            buffer.put((byte) strType.charAt(1));
        }
        else {

            buffer.put((byte) '0');
            buffer.put((byte) strType.charAt(0));
        }
        buffer.put((byte) SEPARATOR_TYPE_MESSAGE);

        // Add message (*)
        buffer.put(element.mMessage.getBytes()); // Default charset UTF-8

        return mBluetooth.write(buffer.array(), byteCount);
    }

    //
    private class ProcessTask extends AsyncTask<Void, Void, Void> {

        // Initialize connection
        private void initialize(boolean position) {

            // Add request to initialize settings
            final Bundle connInfo = new Bundle();
            connInfo.putString(Settings.DATA_KEY_REMOTE_DEVICE, mBluetooth.getRemoteDevice());
            connInfo.putBoolean(Settings.DATA_KEY_POSITION, position);
            Connectivity.getInstance().addRequest(Settings.getInstance(),
                    Settings.REQ_TYPE_INITIALIZE, connInfo);
        }

        // Close connection
        private void close() {

            // Remove or close main activity from the activities stack (using activity result)
            try {
                Activity curActivity = ActivityWrapper.get();
                if (curActivity == null)
                    throw new NullPointerException();

                if (curActivity.getClass().equals(MainActivity.class))
                    curActivity.finish();
                else // ...other activity (back to connect activity, if not already the case)
                    curActivity.setResult(Constants.RESULT_LOST_CONNECTION);
            }
            catch (NullPointerException e) {
                Logs.add(Logs.Type.F, "Wrong activity reference");
            }

            // Display a Toast message to inform user that the connection has been lost (if not requested)
            if (!mDisconnectRequest) {

                if (!mDisconnectError)
                    DisplayMessage.getInstance().toast(R.string.connection_lost, Toast.LENGTH_LONG);
                else
                    DisplayMessage.getInstance().toast(R.string.connection_error,
                            Toast.LENGTH_LONG);
            }
            mStatus = Connectivity.Status.RESET;
            mDisconnectRequest = mDisconnectError = false;
            synchronized (mRequests) { mRequests.clear(); }
        }

        ////// Process (connected status):
        // _ Send requests (from request list)
        // _ Receive data: requests & replies (from remote device)
        private void process() {

            // Check if need to disconnect or if still connected
            if (!isConnected()) {
                close();
                return;
            }

            //
            if (mStatus == Connectivity.Status.STAND_BY) {

                // Check if received request
                String request = receive();
                if (request != null) {

                    TransferElement reply = new TransferElement();
                    switch (request.charAt(0)) {

                        case ConnectRequest.REQ_SETTINGS: reply.mHandler = Settings.getInstance(); break;
                        default: {

                            Logs.add(Logs.Type.E, "Unexpected request received");
                            mDisconnectError = true;
                            close();
                            return;
                        }
                    }
                    reply.mType = (byte)Integer.parseInt(request.substring(2, 4), 16);
                    reply.mMessage = reply.mHandler.getReply(reply.mType, request.substring(5));
                    if (reply.mMessage == null) {

                        Logs.add(Logs.Type.E, "Failed to reply to request");
                        mDisconnectError = true;
                        close();
                        return;
                    }

                    // Send reply
                    if (!send(reply)) {

                        Logs.add(Logs.Type.E, "Failed to send reply");
                        mDisconnectError = true;
                        close();
                    }
                }
                else if (mDisconnectError) // Check if error during message receive
                    close();

                else synchronized (mRequests) { // Check if existing request to send







                    if (mRequests.isEmpty())
                        return;

                    //if (!mergeRequests())
                    //    return; // No request to send









                    // Send request
                    if (!send(mRequests.get(0))) {

                        Logs.add(Logs.Type.E, "Failed to send request");
                        mDisconnectError = true;
                        close();
                        return;
                    }
                    mStatus = Connectivity.Status.WAIT_REPLY;
                }
            }
            else { // Wait reply

                // Check if received reply
                String reply = receive();
                if (reply != null) {

                    synchronized (mRequests) {



                        /*
                        if (mRequests.isEmpty()) {

                            Logs.add(Logs.Type.E, "Reply received without request sent");
                            mDisconnectError = true;
                            close();
                            return;
                        }
                        */



                        if (!mRequests.get(0).mHandler.receiveReply(
                                (byte) Integer.parseInt(reply.substring(2, 4), 16), reply.substring(5))) {

                            Logs.add(Logs.Type.E, "Unexpected reply received (or device not matching)");
                            mDisconnectError = true;
                            close();
                        }
                        else
                            mRequests.remove(0);
                    }
                }
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            Logs.add(Logs.Type.I, "Connectivity thread started");

            short devIndex = 0, waitListen = 0;
            while(!mAbort) {

                switch (mStatus) {
                    case UNDEFINED: {

                        mStatus = Connectivity.Status.RESET;

                        // Get device performance (representation)
                        Thread performThread = new Thread(new Runnable() {
                            @Override
                            public void run() {

                                double[] randoms = new double[Constants.CONFIG_PERFORMANCE_LOOP];
                                for (int i = 0; i < Constants.CONFIG_PERFORMANCE_LOOP; ++i)
                                    randoms[i] = Math.random();

                                long performance = System.currentTimeMillis();
                                double calculate = 0;
                                for (int i = 0; i < Constants.CONFIG_PERFORMANCE_LOOP; ++i)
                                    calculate += Math.cbrt(Math.cos(Math.sin(randoms[i])) * 1000000);

                                Settings.getInstance().mPerformance =
                                        System.currentTimeMillis() - performance;
                            }
                        });
                        performThread.setPriority(Thread.MAX_PRIORITY);
                        performThread.start();
                        break;
                    }
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

                                    // Do not try to connect to device which available camera resolutions
                                    // do not match with local ones
                                    if (mNotMatchingDevices.contains(device))
                                        break;

                                    String devAddress =
                                            device.substring(device.indexOf(
                                                    Constants.BLUETOOTH_DEVICES_SEPARATOR) + 1);
                                    mBluetooth.connect(true, Constants.CONN_SECURE_UUID, devAddress);
                                }
                                else {

                                    mBluetooth.listen(true, Constants.CONN_SECURE_UUID,
                                            Constants.CONN_SECURE_NAME);
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
                    Logs.add(Logs.Type.W, "Unable to sleep: " + e.getMessage());
                }
            }
            Logs.add(Logs.Type.I, "Connectivity thread ended");
            return null;
        }
    }

    //////
    public boolean start() {

        if ((mBluetooth.getStatus() == Bluetooth.Status.DISABLED) && (!mBluetooth.initialize()))
            return false;

        if (mStatus != Status.UNDEFINED) {
            Logs.add(Logs.Type.W, "Connectivity already started");
            return true;
        }
        mAbort = false;
        mStatus = Status.UNDEFINED;
        mProcessTask = new ProcessTask();
        mProcessTask.execute();
        return true;
    }
    public void stop() {

        if ((mAbort) || (mBluetooth.getStatus() == Bluetooth.Status.DISABLED))
            return;

        mAbort = true;
        try { mProcessTask.get(); }
        catch (InterruptedException e) {
            Logs.add(Logs.Type.E, "Failed to interrupt connectivity task: " + e.getMessage());
        }
        catch (ExecutionException e) {
            Logs.add(Logs.Type.E, "Failed to stop connectivity task: " + e.getMessage());
        }
        mProcessTask = null;
        mStatus = Status.UNDEFINED;
        mBluetooth.reset();
    }

    //
    public void resume(Context context) {

        mBluetooth.enable(); // Enable bluetooth (in case it has been disabled during the pause)
        mBluetooth.register(context);
    }
    public void pause(Context context) { mBluetooth.unregister(context); }
    public void destroy() {

        stop();
        mBluetooth.release();
    }
}
