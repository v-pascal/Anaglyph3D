package com.studio.artaban.anaglyph3d.transfert;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.studio.artaban.anaglyph3d.MainActivity;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.helpers.Logs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by pascal on 20/03/16.
 * Connectivity Helpers
 */
public class Connectivity {

    private static Connectivity ourInstance = new Connectivity();
    public static Connectivity getInstance() { return ourInstance; }
    private Connectivity() { }

    //////
    private AsyncTask<Void, Void, Void> mProcessTask;
    private final Bluetooth mBluetooth = new Bluetooth();
    private boolean mAbort = true;

    private enum Status {
        UNDEFINED,

        // Not connected
        RESET,
        DISCOVER,
        CONNECT,
        LISTEN,

        // Connected
        STAND_BY,
        WAIT_REPLY
    }
    private Status mStatus = Status.UNDEFINED;

    private class Request {
        public ConnRequest mCaller;
        public String mCommand;
    }
    private final List<Request> mRequests = new ArrayList<Request>();
    public void addRequest(ConnRequest caller, short type, Bundle data) {

        // Get request command from caller
        String command = caller.getRequestCmd(type, data);
        if (command == null)
            return;

        // Add request into request list
        Request request = new Request();
        request.mCaller = caller;
        request.mCommand = command;
        synchronized (mRequests) { mRequests.add(request); }
    }

    private class ProcessTask extends AsyncTask<Void, Void, Void> {

        private final Context mContext;
        public ProcessTask(Context context) { mContext = context; }

        // Start main activity
        private void startActivity() {
            Intent intent = new Intent(mContext, MainActivity.class);
            mContext.startActivity(intent);
        }

        // Add settings request
        private void addSettingsRequest(boolean position) {

            final Bundle connInfo = new Bundle();
            connInfo.putString(Settings.DATA_KEY_REMOTE_DEVICE,
                    mBluetooth.getRemoteDevice().substring(0,
                            mBluetooth.getRemoteDevice().indexOf(Bluetooth.DEVICES_SEPARATOR)));
            connInfo.putBoolean(Settings.DATA_KEY_POSITION, position);
            Connectivity.getInstance().addRequest(Settings.getInstance(),
                    Settings.REQ_TYPE_INITIALIZE, connInfo);
        }

        // Process (connected status):
        // _ Send requests (from request list)
        // _ Receive data: requests & replies
        private void process() {

            if (mStatus == Connectivity.Status.STAND_BY) {



                // # Receive data
                // Request
                // -> sendReply (according request ID)




                // # Send data
                // synchronized (mRequests) {
                //     mStatus = WAIT_REPLY;
                // }



            }
            else { // Wait reply






                // # Receive data
                // Reply
                // -> receiveReply (according request ID)





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
                                addSettingsRequest(true);

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
                                addSettingsRequest(false);

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
