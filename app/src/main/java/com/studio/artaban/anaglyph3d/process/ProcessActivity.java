package com.studio.artaban.anaglyph3d.process;

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
import com.studio.artaban.anaglyph3d.transfer.Connectivity;

/**
 * Created by pascal on 12/04/16.
 * Activity to manage video recording and transferring using fragments:
 * _ Position fragment
 * _ Recorder fragment
 * _ Process fragment (status)
 * _ Contrast fragment
 * _ Synchronize fragment
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

        // Remove fullscreen mode
        //getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

        // Replace recorder with process fragment
        FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
        fragTransaction.replace(R.id.main_container, new ProcessFragment(), ProcessFragment.TAG).commit();
        getSupportFragmentManager().executePendingTransactions();

        // Start process thread
        mProcessThread = new ProcessThread(picSize, picRaw);
        mProcessThread.start();
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
    public void onUpdateProgress(final ProcessThread.Status status, final int progress) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                ProcessFragment processFragment = (ProcessFragment)getSupportFragmentManager().
                        findFragmentByTag(ProcessFragment.TAG);

                if (processFragment != null)
                    processFragment.updateProgress(status, progress);
                //else // Contrast or Synchronize fragment opened (nothing to update)
            }
        });
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
