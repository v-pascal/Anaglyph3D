package com.studio.artaban.anaglyph3d.transfert;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import com.studio.artaban.anaglyph3d.MainActivity;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.helpers.Logs;

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
        CONNECTED
    }
    private Status mStatus = Status.UNDEFINED;

    private class ProcessTask extends AsyncTask<Void, Void, Void> {

        private final Context mContext;
        public ProcessTask(Context context) { mContext = context; }

        // Start main activity
        private void startActivity() {
            Intent intent = new Intent(mContext, MainActivity.class);
            mContext.startActivity(intent);
        }

        // Send settings request
        private void sendSettingsRequest(boolean position) {

            final Bundle connInfo = new Bundle();
            connInfo.putString(ConnRequest.KEY_SETTINGS_REMOTE,
                    mBluetooth.getRemoteDevice().substring(0,
                            mBluetooth.getRemoteDevice().indexOf(Bluetooth.DEVICES_SEPARATOR)));
            connInfo.putBoolean(ConnRequest.KEY_SETTINGS_POSITION, true);
            Settings.getInstance().sendRequest(connInfo);
        }

        // Receive data: requests & replies (connected status)
        private void receiveData() {




            //receiveRequest
            // sendReply

            //receiveReply




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
                                sendSettingsRequest(true);

                                mStatus = Connectivity.Status.CONNECTED;
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

                                if (++waitListen == 50) // 50 * 100 == 5 seconds
                                    mStatus = Connectivity.Status.RESET;
                                break;
                            }
                            case CONNECTED: {
                                Logs.add(Logs.Type.I, "Connected (SLAVE)");
                                sendSettingsRequest(false);

                                mStatus = Connectivity.Status.CONNECTED;
                                break;
                            }
                        }
                        break;
                    }
                    case CONNECTED: {

                        receiveData();
                        break;
                    }
                }
                if (mAbort)
                    break; // Exit immediately

                // Sleep
                try { Thread.sleep(100, 0); }
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
