package com.studio.artaban.anaglyph3d.process;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.media.Frame;
import com.studio.artaban.anaglyph3d.media.Video;
import com.studio.artaban.anaglyph3d.transfer.Connectivity;
import com.studio.artaban.libGST.GstObject;

import java.io.File;

/**
 * Created by pascal on 12/04/16.
 * Activity to manage video recording and transferring using fragments:
 * _ Position fragment: to inform user on devices position
 * _ Recorder fragment: to start recording
 * _ Process fragment: to make the video (status fragment)
 */
public class ProcessActivity extends AppCompatActivity {

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
            }
        });
    }
    public void updateRecording() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() { // Update down count

                RecorderFragment recorder = (RecorderFragment)getSupportFragmentManager().
                        findFragmentByTag(RecorderFragment.TAG);
                recorder.updateDownCount();
            }
        });
    }
    private ProcessThread mProcessThread;
    public void startProcessing(Camera.Size picSize, byte[] picRaw) {

        // Set unspecified orientation (default device orientation)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        // Remove fullscreen mode
        //getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

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

        // Send start request to remote device
        Connectivity.getInstance().addRequest(ActivityWrapper.getInstance(),
                ActivityWrapper.REQ_TYPE_START, null);

        // Set fullscreen mode
        //int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        //if (Build.VERSION.SDK_INT >= 19)
        //    flags |= View.SYSTEM_UI_FLAG_IMMERSIVE; // Force to keep fullscreen even if touched
        //getWindow().getDecorView().setSystemUiVisibility(flags);

        // BUG: Make a crash on some device when the screen is touched (e.g 'Samsung Galaxy Trend Lite')
    }
    public void onReversePosition(View sender) {

        // Reverse the device orientation in order to position camera at the expected distance
        // -> When cameras cannot be placed at the expected distance due to the location of the camera
        //    on a particular device (often the case for landscape orientation), this option allows the
        //    user to reverse it in order to place the camera as the expected distance

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

                ProcessFragment processFragment = (ProcessFragment)getSupportFragmentManager().
                        findFragmentByTag(ProcessFragment.TAG);

                if (processFragment != null)
                    processFragment.updateProgress();
                else
                    Logs.add(Logs.Type.F, "Unexpected fragment found");
            }
        });
    }
    public void onInitialize() { // Initialize GStreamer library on UI thread

        final Context context = this;
        Runnable initRunnable = new Runnable() {
            @Override
            public void run() {

                // Initialize GStreamer library
                if (ProcessThread.mGStreamer == null)
                    ProcessThread.mGStreamer = new GstObject(context);

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

        // Set default activity result
        setResult(Constants.RESULT_PROCESS_CANCELLED);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if ((getSupportFragmentManager().findFragmentByTag(PositionFragment.TAG) != null) &&
                (!isFinishing())) {

            finish(); // Finish activity when paused

            // Send cancel request to remote device
            Connectivity.getInstance().addRequest(ActivityWrapper.getInstance(),
                    ActivityWrapper.REQ_TYPE_CANCEL, null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);







        //Check contrast or synchronization activity results:
        // RESULT_PROCESS_CANCELLED

        // RESULT_PROCESS_CONTRAST
        // RESULT_PROCESS_SYNCHRO

        //getIntent().putExtra(DATA_KEY_CONTRAST, mContrast);
        //getIntent().putExtra(DATA_KEY_BRIGHTNESS, mBrightness);

        //mProcessThread.applyContrastBrightness();
        //mProcessThread.applySynchronization();






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

        if ((mProcessThread != null) && (mProcessThread.isAlive()))
            mProcessThread.release();
    }
}
