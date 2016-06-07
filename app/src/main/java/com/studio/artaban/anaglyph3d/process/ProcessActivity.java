package com.studio.artaban.anaglyph3d.process;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.helpers.Storage;
import com.studio.artaban.anaglyph3d.process.configure.CorrectionActivity;
import com.studio.artaban.anaglyph3d.process.configure.ShiftActivity;
import com.studio.artaban.anaglyph3d.process.configure.SynchroActivity;
import com.studio.artaban.anaglyph3d.transfer.Connectivity;
import com.studio.artaban.libGST.GstObject;

/**
 * Created by pascal on 12/04/16.
 * Activity to manage video recording and transferring using fragments:
 * _ Position fragment: to inform user on devices position & settings
 * _ Recorder fragment: to start recording
 * _ Process fragment: to make the video (status fragment)
 */
public class ProcessActivity extends AppCompatActivity {

    private static final String WAKE_LOCK_NAME = "Anaglyph-3D";
    private WakeLock mWakeLock; // To avoid device in pause during video recording

    private static final int DOWNCOUNT_DELAY = 1000; // Delay between down count (simulated 3D only)

    //
    public void startRecording() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                // Replace position with recorder fragment (if not already the case)
                if (getSupportFragmentManager().findFragmentByTag(RecorderFragment.TAG) != null)
                    return;

                FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
                fragTransaction.replace(R.id.main_container, new RecorderFragment(),
                        RecorderFragment.TAG).commit();
                getSupportFragmentManager().executePendingTransactions();

                // Wake lock during video recording
                mWakeLock = ((PowerManager)getSystemService(Context.POWER_SERVICE))
                        .newWakeLock(PowerManager.FULL_WAKE_LOCK, WAKE_LOCK_NAME);
                mWakeLock.acquire();
            }
        });
    }
    public void updateRecording() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() { // Update down count

                RecorderFragment recorder = (RecorderFragment) getSupportFragmentManager().
                        findFragmentByTag(RecorderFragment.TAG);
                recorder.updateDownCount();
            }
        });
    }
    private ProcessThread mProcessThread;
    public void startProcessing(Camera.Size picSize, byte[] picRaw) {

        mWakeLock.release(); // Stop wake lock requested
        mWakeLock = null;

        // Set unspecified orientation (default device orientation)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        // Start process thread
        mProcessThread = new ProcessThread(picSize, picRaw);
        mProcessThread.start();

        // Replace recorder with process fragment
        FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
        fragTransaction.replace(R.id.main_container, new ProcessFragment(), ProcessFragment.TAG).commit();
        getSupportFragmentManager().executePendingTransactions();
    }

    //
    public void onValidatePosition(View sender) {
        // User has confirmed to start video recorder

        if (!Settings.getInstance().mSimulated) // Real 3D
            Connectivity.getInstance().addRequest(ActivityWrapper.getInstance(),
                    ActivityWrapper.REQ_TYPE_START, null);
            // Send start request to remote device

        else { // Simulated 3D

            final RecorderFragment recorder = new RecorderFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.main_container, recorder, RecorderFragment.TAG)
                    .commit();
            getSupportFragmentManager().executePendingTransactions();

            // Wake lock during video recording
            mWakeLock = ((PowerManager)getSystemService(Context.POWER_SERVICE))
                    .newWakeLock(PowerManager.FULL_WAKE_LOCK, WAKE_LOCK_NAME);
            mWakeLock.acquire();

            // Display down count B4 recording
            new Thread(new Runnable() {

                private void sleep() { // Delay

                    try { Thread.sleep(DOWNCOUNT_DELAY, 0); }
                    catch (InterruptedException e) {
                        Logs.add(Logs.Type.E, e.getMessage());
                    }
                }

                @Override
                public void run() {

                    sleep();
                    Runnable runDownCount = new Runnable() {
                        @Override
                        public void run() {

                            recorder.updateDownCount();
                            synchronized (this) { notify(); }
                        }
                    };

                    // Decrease down count
                    for (short i = 0; i < (RecorderFragment.MAX_COUNTER + 1); ++i) {
                        // + 1 above to loop one more time in order to start recording

                        synchronized (runDownCount) {

                            runOnUiThread(runDownCount);

                            // Wait down count update finished on UI thread
                            try { runDownCount.wait(); }
                            catch (InterruptedException e) {
                                Logs.add(Logs.Type.E, e.getMessage());
                            }
                        }
                        sleep(); // Delay
                    }
                }
            }).start();
        }
    }
    public void onReversePosition(View sender) {

        // Reverse the device orientation in order to position camera at the expected distance
        // -> When cameras cannot be placed at the expected distance due to the location of the camera
        //    on a particular device (often the case for landscape orientation), this option allows the
        //    user to reverse it in order to place the camera as the expected distance
        if (Settings.getInstance().mSimulated)
            return; // Option only available for real 3D

        Settings.getInstance().mReverse = !Settings.getInstance().mReverse;

        // Change orientation
        if (Settings.getInstance().mOrientation)
            setRequestedOrientation((!Settings.getInstance().mReverse)?
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
        else
            setRequestedOrientation((!Settings.getInstance().mReverse)?
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);

        ((PositionFragment)getSupportFragmentManager().findFragmentByTag(PositionFragment.TAG)).reverse();
    }

    public void onUpdateProgress() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                ProcessFragment processFragment = (ProcessFragment) getSupportFragmentManager().
                        findFragmentByTag(ProcessFragment.TAG);

                if (processFragment != null)
                    processFragment.updateProgress();
                else
                    Logs.add(Logs.Type.F, "Unexpected fragment found");
            }
        });
    }
    public void onInitialize() { // Initialize GStreamer library on UI thread

        Runnable initRunnable = new Runnable() {
            @Override
            public void run() {

                // Initialize GStreamer library
                if (ProcessThread.mGStreamer == null)
                    ProcessThread.mGStreamer = new GstObject(ProcessActivity.this);

                // Notify initialization finished
                synchronized (this) { notify(); }
            }
        };
        synchronized (initRunnable) {

            runOnUiThread(initRunnable);

            // Wait initialization finish on UI thread
            try { initRunnable.wait(); }
            catch (InterruptedException e) {
                Logs.add(Logs.Type.E, e.getMessage());
            }
        }
    }

    //////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_process);

        // Set current activity
        ActivityWrapper.set(this);

        // Set orientation
        Settings.getInstance().mReverse = false;
        if (Settings.getInstance().mOrientation)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // Add position fragment
        FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
        fragTransaction.add(R.id.main_container, new PositionFragment(), PositionFragment.TAG).commit();
        getSupportFragmentManager().executePendingTransactions();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        ActivityWrapper.set(this); // Set current activity

        switch (requestCode) {
            case Constants.PROCESS_REQUEST_CORRECTION: {

                if (resultCode == RESULT_OK)
                    ProcessThread.applyCorrection(
                            data.getFloatExtra(CorrectionActivity.DATA_KEY_CONTRAST,
                                    CorrectionActivity.DEFAULT_CONTRAST),
                            data.getFloatExtra(CorrectionActivity.DATA_KEY_BRIGHTNESS,
                                    CorrectionActivity.DEFAULT_BRIGHTNESS),
                            data.getFloatExtra(CorrectionActivity.DATA_KEY_RED_BALANCE,
                                    CorrectionActivity.DEFAULT_BALANCE),
                            data.getFloatExtra(CorrectionActivity.DATA_KEY_GREEN_BALANCE,
                                    CorrectionActivity.DEFAULT_BALANCE),
                            data.getFloatExtra(CorrectionActivity.DATA_KEY_BLUE_BALANCE,
                                    CorrectionActivity.DEFAULT_BALANCE),
                            data.getBooleanExtra(CorrectionActivity.DATA_KEY_LOCAL,
                                    CorrectionActivity.DEFAULT_LOCAL_FRAME));

                else if (resultCode != Constants.RESULT_LOST_CONNECTION)
                    ProcessThread.applyCorrection(CorrectionActivity.DEFAULT_CONTRAST,
                            CorrectionActivity.DEFAULT_BRIGHTNESS,
                            CorrectionActivity.DEFAULT_BALANCE,
                            CorrectionActivity.DEFAULT_BALANCE,
                            CorrectionActivity.DEFAULT_BALANCE,
                            CorrectionActivity.DEFAULT_LOCAL_FRAME);
                else {

                    setResult(Constants.RESULT_LOST_CONNECTION);
                    finish();
                }
                break;
            }
            case Constants.PROCESS_REQUEST_SYNCHRO: {

                if (resultCode == RESULT_OK)
                    mProcessThread.applySynchronization(
                            data.getShortExtra(SynchroActivity.DATA_KEY_SYNCHRO_OFFSET,
                                    SynchroActivity.DEFAULT_OFFSET),
                            data.getBooleanExtra(SynchroActivity.DATA_KEY_SYNCHRO_LOCAL,
                                    SynchroActivity.DEFAULT_LOCAL));

                else if (resultCode != Constants.RESULT_LOST_CONNECTION)
                    mProcessThread.applySynchronization(SynchroActivity.DEFAULT_OFFSET,
                            SynchroActivity.DEFAULT_LOCAL);
                else {

                    setResult(Constants.RESULT_LOST_CONNECTION);
                    finish();
                }
                break;
            }
            case Constants.PROCESS_REQUEST_SHIFT: {

                if (resultCode == RESULT_OK)
                    mProcessThread.applySimulation(
                            data.getFloatExtra(ShiftActivity.DATA_KEY_SHIFT,
                                    ShiftActivity.DEFAULT_SHIFT),
                            data.getFloatExtra(ShiftActivity.DATA_KEY_GUSHING,
                                    ShiftActivity.DEFAULT_GUSHING));

                //else if (resultCode != Constants.RESULT_LOST_CONNECTION) // Could not happen
                else
                    mProcessThread.applySimulation(ShiftActivity.DEFAULT_SHIFT,
                            ShiftActivity.DEFAULT_GUSHING);
                break;
            }
            default: {

                Logs.add(Logs.Type.F, "Unexpected request code");
                break;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if ((!isFinishing()) &&
                (getSupportFragmentManager().findFragmentByTag(PositionFragment.TAG) != null)) {

            finish(); // Finish activity when paused

            // Send cancel request to remote device
            Connectivity.getInstance().addRequest(ActivityWrapper.getInstance(),
                    ActivityWrapper.REQ_TYPE_CANCEL, null);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        // Send cancel request to remote device (this action will finish the activity)
        Connectivity.getInstance().addRequest(ActivityWrapper.getInstance(),
                ActivityWrapper.REQ_TYPE_CANCEL, null);
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        if ((mProcessThread != null) && (mProcessThread.isAlive())) {

            mProcessThread.release();
            mProcessThread = null;
        }
        if (mWakeLock != null)
            mWakeLock.release();

        // Remove all temporary files from storage
        Storage.removeTempFiles(false);
    }
}
