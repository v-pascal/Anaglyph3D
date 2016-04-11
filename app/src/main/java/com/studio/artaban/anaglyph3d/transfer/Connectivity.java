package com.studio.artaban.anaglyph3d.transfer;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

import com.studio.artaban.anaglyph3d.ConnectActivity;
import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;
import com.studio.artaban.anaglyph3d.helpers.Logs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
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

    public volatile boolean mListenDevice = false; // false: Try to connect (master), true: Listen (slave)
    public final ArrayList<String> mNotMatchingDevices = new ArrayList<>();
    // Array containing device info that camera resolutions do not match with current ones

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
    private final List<TransferElement> mRequests = new ArrayList<>();

    //
    public boolean addRequest(ConnectRequest handler, byte type, Bundle data) {

        if (!isConnected())
            return false;

        // Get request message from handler
        String message = handler.getRequest(type, data);
        if (message == null)
            return false;

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

                requestType |= mRequests.get(i).mType; // Merge type (mask)
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
    private boolean processRequest(String request) {

        ////// Request received while waiting reply
        ConnectRequest.PreviousMaster previousRequest = null;
        if ((Settings.getInstance().isMaster()) && (mPendingRequest != null)) {

            previousRequest = new ConnectRequest.PreviousMaster();
            synchronized (mRequests) {
                previousRequest.mId = mRequests.get(0).mHandler.getRequestId();
                previousRequest.mType = mRequests.get(0).mType;
                //previousRequest.mMessage = mRequests.get(0).mMessage;
                mRequests.remove(0);
            }
        }
        mPendingRequest = null; // No more pending request
        ///////////////////////////////////////////

        TransferElement reply = new TransferElement();
        switch (request.charAt(0)) {

            case ConnectRequest.REQ_SETTINGS: reply.mHandler = Settings.getInstance(); break;
            default: {

                Logs.add(Logs.Type.E, "Unexpected request received");
                mDisconnectError = true;
                return false;
            }
        }
        reply.mType = (byte)Integer.parseInt(request.substring(2, 4), 16);
        reply.mMessage = reply.mHandler.getReply(reply.mType, request.substring(5), previousRequest);
        if (reply.mMessage == null) {

            Logs.add(Logs.Type.E, "Failed to reply to request");
            mDisconnectError = true;
            return false;
        }

        // Send reply
        if (!send(false, reply)) {

            Logs.add(Logs.Type.E, "Failed to send reply");
            mDisconnectError = true;
            return false;
        }
        return true;
    }

    public void disconnect() { mDisconnectRequest = true; }
    private boolean isConnected() {

        return (!mDisconnectRequest) && (!mDisconnectError) &&
                (mBluetooth.getStatus() == Bluetooth.Status.CONNECTED);
    }

    ////// Request/Reply element format: BBB-X#A:CC_*
    // _ BBB -> Digital size of the entire message (in decimal)
    // _ X -> 'Q': Request element, 'R': Reply element
    // _ A -> Request ID
    // _ CC -> Request type (in hex)
    // _ * -> Message
    private static final char SEPARATOR_SIZE_ELEMENT = '-';
    private static final char SEPARATOR_ELEMENT_REQUEST_ID = '#';
    private static final char SEPARATOR_REQUEST_ID_TYPE = ':';
    private static final char SEPARATOR_TYPE_MESSAGE = '_';

    private static final char ELEMENT_FLAG_REQUEST = 'Q';
    private static final char ELEMENT_FLAG_REPLY = 'R';

    private String mPendingRequest = null; // Pending request received during a reply wait

    // Receive request or reply
    private String receive(boolean reply) {

        ////// Request received while waiting reply
        if ((!reply) && (mPendingRequest != null))
            return mPendingRequest;
            // Needed when a request has been received during a wait reply

        ///////////////////////////////////////////

        int size = mBluetooth.read(mRead);
        if ((size == 0) && (mRead.size() > 0))
            size = mRead.size(); // Needed when received request & reply in same time

        if (size > 0) {

            String message;
            try { message = mRead.toString("UTF-8"); }
            catch (UnsupportedEncodingException e) {

                Logs.add(Logs.Type.E, "Failed to get request UTF-8 encoded string");
                return null;
            }

            // Check element format received
            if ((size > 11) &&
                    ((message.charAt(3) != SEPARATOR_SIZE_ELEMENT) ||
                            (message.charAt(5) != SEPARATOR_ELEMENT_REQUEST_ID) ||
                            (message.charAt(7) != SEPARATOR_REQUEST_ID_TYPE) ||
                            (message.charAt(10) != SEPARATOR_TYPE_MESSAGE))) {

                Logs.add(Logs.Type.E, "Wrong request/reply format received");
                mDisconnectError = true;
                return null;
            }
            int sizeMessage = Integer.parseInt(message.substring(0, 3));
            if (size >= sizeMessage) {

                // Purge message received
                if (size == sizeMessage)
                    mRead.reset(); // Full message received

                else { // Full & next message received

                    message = message.substring(0, sizeMessage);
                    byte[] store = Arrays.copyOfRange(mRead.toByteArray(), sizeMessage, size);
                    mRead.reset();

                    try { mRead.write(store); }
                    catch (IOException e) {

                        Logs.add(Logs.Type.E, "Failed to purge message received");
                        mDisconnectError = true;
                        return null;
                    }
                }

                // Return result according what is expected
                switch (message.charAt(4)) {
                    case ELEMENT_FLAG_REPLY: {
                        if (reply)
                            return message.substring(6); // Format: A:CC_*

                        // Waiting request but received reply
                        Logs.add(Logs.Type.F, "Received unexpected reply");

                        mDisconnectError = true;
                        break;
                    }
                    case ELEMENT_FLAG_REQUEST: {
                        if (!reply)
                            return message.substring(6);

                        mPendingRequest = message.substring(6);
                        break;
                    }
                }
            }
        }
        return null;
    }

    // Send request or reply
    private boolean send(boolean request, TransferElement element) {

        Integer byteCount = element.mMessage.length() + 11;
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
        buffer.put((byte) SEPARATOR_SIZE_ELEMENT);

        // Add request/reply element flag
        buffer.put((byte) ((request)? ELEMENT_FLAG_REQUEST:ELEMENT_FLAG_REPLY));
        buffer.put((byte) SEPARATOR_ELEMENT_REQUEST_ID);

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

            synchronized (mRequests) { mRequests.clear(); }
            mRead.reset();

            // Add request to initialize settings
            final Bundle connInfo = new Bundle();
            connInfo.putString(Settings.DATA_KEY_REMOTE_DEVICE, mBluetooth.getRemoteDevice());
            connInfo.putBoolean(Settings.DATA_KEY_POSITION, position);
            Connectivity.getInstance().addRequest(Settings.getInstance(),
                    Settings.REQ_TYPE_INITIALIZE, connInfo);
        }

        // Close connection
        private void close() {

            // Close current activity (back to connect activity)
            try {
                Activity curActivity = ActivityWrapper.get();
                if (curActivity == null)
                    throw new NullPointerException();

                if (!curActivity.getClass().equals(ConnectActivity.class)) { // Not connect activity
                    curActivity.setResult(Constants.RESULT_LOST_CONNECTION);
                    curActivity.finish();
                }
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
        }

        private short mMaxWait = 0; // Maximum delay to receive reply before disconnect (in loop count)

        ////// Process (connected status):
        // _ Send requests (from request list)
        // _ Receive data: requests & replies (from remote device)
        private void process() {

            // Check if need to disconnect or if still connected
            if (!isConnected())
                close();

            switch (mStatus) {
                case STAND_BY: {

                    // Check if received request
                    String request = receive(false);
                    if (request != null) {
                        if (!processRequest(request))
                            close();
                    }
                    else if (mDisconnectError) // Check if error during message receive
                        close();

                    else synchronized (mRequests) { // Check if existing request to send

                        if (!mergeRequests())
                            break; // No request to send

                        // Send request
                        if (!send(true, mRequests.get(0))) {

                            Logs.add(Logs.Type.E, "Failed to send request");
                            mDisconnectError = true;
                            close();
                            break;
                        }
                        mMaxWait = mRequests.get(0).mHandler.getMaxWaitReply(mRequests.get(0).mType);
                        mStatus = Connectivity.Status.WAIT_REPLY;
                    }
                    break;
                }
                case WAIT_REPLY: {

                    // Check if received reply
                    String reply = receive(true);
                    if (reply != null) {

                        synchronized (mRequests) {

                            int noMatchCount = mNotMatchingDevices.size();
                            if ((mRequests.isEmpty()) || (!mRequests.get(0).mHandler.receiveReply(
                                    (byte) Integer.parseInt(reply.substring(2, 4), 16),
                                    reply.substring(5)))) {

                                Logs.add(Logs.Type.E, "Unexpected reply received (or device not matching)");
                                mDisconnectError = (noMatchCount == mNotMatchingDevices.size());
                                close();
                                break;
                            }

                            ////// Request received while waiting reply
                            if (mPendingRequest == null) // Always null for slave
                                mRequests.remove(0);
                            //else // Let's the 'STAND_BY' case process the pending request according
                                   // the request we received the reply just above (for master only)

                            ///////////////////////////////////////////

                            mStatus = Connectivity.Status.STAND_BY;
                        }
                    }
                    ////// Request received while waiting reply
                    else if (mPendingRequest != null) {
                        if (!Settings.getInstance().isMaster()) {
                            if (!processRequest(mPendingRequest))
                                close();
                        }
                        //else // The master will wait its reply
                    }
                    ///////////////////////////////////////////
                    if (mMaxWait-- == 0) {

                        Logs.add(Logs.Type.E, "The time limit to receive reply has expired");
                        mDisconnectError = true;
                        close();
                    }
                    break;
                }
            }
        }

        //
        @Override
        protected Void doInBackground(Void... params) {
            Logs.add(Logs.Type.I, "Connectivity thread started");

            short devIndex = 0;
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
                        if (mListenDevice) {

                            // Listen
                            mBluetooth.listen(true, Constants.CONN_SECURE_UUID,
                                    Constants.CONN_SECURE_NAME);
                            mStatus = Connectivity.Status.LISTEN;
                        }
                        else {

                            // Discover (then try to connect)
                            mBluetooth.discover();
                            mStatus = Connectivity.Status.DISCOVER;
                        }
                        break;
                    }
                    case DISCOVER: {

                        if (!mBluetooth.isDiscovering()) {
                            mStatus = Connectivity.Status.CONNECT;
                            devIndex = 0;
                        }
                        break;
                    }
                    case CONNECT: { // Try to connect to discovered devices

                        switch (mBluetooth.getStatus()) {
                            case CONNECTING: break; // Still trying to connect
                            case CONNECTED: {
                                Logs.add(Logs.Type.I, "Connected (MASTER)");
                                initialize(true);

                                mStatus = Connectivity.Status.STAND_BY;
                                break;
                            }
                            case READY: {

                                // Check if user has changed the device to search
                                if (mListenDevice) {
                                    mStatus = Connectivity.Status.RESET;
                                    break;
                                }

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
                                else // No more device to try to connect
                                    mStatus = Connectivity.Status.RESET;

                                break;
                            }
                        }
                        break;
                    }
                    case LISTEN: {

                        switch (mBluetooth.getStatus()) {
                            case LISTENING: { // Still listening

                                // Check if user has changed the device to search
                                if (!mListenDevice)
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
                try { Thread.sleep(Constants.CONN_WAIT_DELAY, 0); }
                catch (InterruptedException e) {
                    Logs.add(Logs.Type.W, "Unable to sleep: " + e.getMessage());
                }
            }
            Logs.add(Logs.Type.I, "Connectivity thread ended");
            return null;
        }
    }

    //////
    public boolean start(Context context) {

        if ((mBluetooth.getStatus() == Bluetooth.Status.DISABLED) && (!mBluetooth.initialize(context)))
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
