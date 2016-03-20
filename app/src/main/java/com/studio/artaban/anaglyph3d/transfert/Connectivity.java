package com.studio.artaban.anaglyph3d.transfert;

import android.content.Context;
import android.os.AsyncTask;

import com.studio.artaban.anaglyph3d.helpers.Logs;

import java.util.concurrent.ExecutionException;

/**
 * Created by pascal on 20/03/16.
 * Connectivity Helpers
 */
public class Connectivity {
    private static Connectivity ourInstance;

    public static Connectivity getInstance(Context context) {
        if (ourInstance == null)
            ourInstance = new Connectivity(context);
        return ourInstance;
    }

    private AsyncTask<Void, Void, Void> mStepTask;

    private final Bluetooth mBluetooth;
    private boolean mAbort;
    private enum Step { UNDEFINED, RESET, DISCOVER, CONNECT, LISTEN }
    private Step mStep;

    private Connectivity(Context context) {

        mBluetooth = Bluetooth.getInstance(context);
        mStep = Step.UNDEFINED;
        mAbort = false;
    }

    private class StepTask extends AsyncTask<Void, Void, Void> {

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






                                break;
                            }
                            case READY: {

                                final String device = mBluetooth.getDevice(devIndex++);
                                if (device != null)
                                    mBluetooth.connect(false, "", "");

                                else {

                                    mBluetooth.listen(false, "", "");
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









                                break;
                            }
                        }
                        break;
                    }
                }

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
    public boolean start() {

        if (mBluetooth.getStatus() != Bluetooth.Status.READY)
            return false;

        if (mStep != Step.UNDEFINED) {
            Logs.add(Logs.Type.W, "Connectivity already started");
            return true;
        }
        mStep = Step.RESET;
        mStepTask = new StepTask();
        mStepTask.execute();
        return true;
    }
    public void release() {

        mAbort = true;
        try { mStepTask.get(); }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        catch (ExecutionException e) {
            e.printStackTrace();
        }
        mBluetooth.release();
    }
}
