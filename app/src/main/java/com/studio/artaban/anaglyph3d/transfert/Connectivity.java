package com.studio.artaban.anaglyph3d.transfert;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.studio.artaban.anaglyph3d.ConnActivity;
import com.studio.artaban.anaglyph3d.MainActivity;
import com.studio.artaban.anaglyph3d.helpers.Constants;
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
    private AsyncTask<Void, Void, Void> mStepTask;
    private final Bluetooth mBluetooth = new Bluetooth();
    private boolean mAbort = true;

    private enum Step { UNDEFINED, RESET, DISCOVER, CONNECT, LISTEN }
    private Step mStep = Step.UNDEFINED;

    private class StepTask extends AsyncTask<Void, Void, Void> {

        private final Context mContext;
        public StepTask(Context context) { mContext = context; }
        private void startActivity() {

            Intent intent = new Intent(mContext, MainActivity.class);
            intent.putExtra(ConnActivity.DATA_CONN_DEVICE,
                    mBluetooth.getRemoteDevice().substring(0,
                            mBluetooth.getRemoteDevice().indexOf(Constants.CONN_DEVICES_SEPARATOR)));
            mContext.startActivity(intent);

            mStep = Step.UNDEFINED;
            mAbort = true;
        }

        @Override
        protected Void doInBackground(Void... params) {
            Logs.add(Logs.Type.I, "Connectivity thread started");

            short devIndex = 0, waitListen = 0;
            while(!mAbort) {

                switch (mStep) {
                    case RESET: {

                        mBluetooth.reset();
                        mBluetooth.discover();
                        mStep = Step.DISCOVER;
                        break;
                    }
                    case DISCOVER: {

                        if (!mBluetooth.isDiscovering()) {
                            mStep = Step.CONNECT;
                            devIndex = 0;
                        }
                        break;
                    }
                    case CONNECT: {

                        switch (mBluetooth.getStatus()) {
                            case CONNECTING: break; // Still trying to connect
                            case CONNECTED: {

                                Logs.add(Logs.Type.I, "Connected (MASTER)");
                                startActivity();
                                break;
                            }
                            case READY: {

                                final String device = mBluetooth.getDevice(devIndex++);
                                if (device != null) {

                                    String devAddress =
                                            device.substring(device.indexOf(Constants.CONN_DEVICES_SEPARATOR) + 1);
                                    mBluetooth.connect(true, Constants.CONN_SECURE_UUID, devAddress);
                                }
                                else {

                                    mBluetooth.listen(false, Constants.CONN_SECURE_UUID, Constants.CONN_SECURE_NAME);
                                    mStep = Step.LISTEN;
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
                                    mStep = Step.RESET;
                                break;
                            }
                            case CONNECTED: {

                                Logs.add(Logs.Type.I, "Connected (SLAVE)");
                                startActivity();
                                break;
                            }
                        }
                        break;
                    }
                }
                if (mAbort)
                    break; // Exit immediately

                // Wait
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

        if (mStep != Step.UNDEFINED) {
            Logs.add(Logs.Type.W, "Connectivity already started");
            return true;
        }
        mAbort = false;
        mStep = Step.RESET;
        mStepTask = new StepTask(context);
        mStepTask.execute();
        return true;
    }
    public void reset() { mBluetooth.reset(); }

    //
    public void resume(Context context) {
        mBluetooth.register(context);
    }
    public void pause(Context context) {
        mBluetooth.unregister(context);
    }
    public void destroy() {

        if (!mAbort) {

            mAbort = true;
            try { mStepTask.get(); }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            catch (ExecutionException e) {
                e.printStackTrace();
            }
            mStepTask = null;
            mStep = Step.UNDEFINED;
        }
        mBluetooth.release();
    }
}
