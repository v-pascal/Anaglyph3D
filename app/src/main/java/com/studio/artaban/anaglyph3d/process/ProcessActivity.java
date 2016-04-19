package com.studio.artaban.anaglyph3d.process;

import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.os.Build;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.transfer.Connectivity;

/**
 * Created by pascal on 12/04/16.
 * Activity to manage video recording and transferring using fragments:
 * _ Position fragment
 * _ Recorder fragment
 * _ Process fragment (transfer)
 * _ Contrast fragment
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
    public void startProcessing(final Camera.Size picSize, final byte[] picRaw) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                // Remove fullscreen mode
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

                // Replace recorder with process fragment
                FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
                ProcessFragment process = new ProcessFragment();

                Bundle picture = new Bundle();
                picture.putInt(ProcessFragment.PICTURE_SIZE_WIDTH, picSize.width);
                picture.putInt(ProcessFragment.PICTURE_SIZE_HEIGHT, picSize.height);
                picture.putByteArray(ProcessFragment.PICTURE_RAW_BUFFER, picRaw);
                process.setArguments(picture);

                fragTransaction.replace(R.id.main_container, process, ProcessFragment.TAG).commit();
                getSupportFragmentManager().executePendingTransactions();
            }
        });
    }

    //
    public void onValidatePosition(View sender) {

        // Send start request to remote device
        Connectivity.getInstance().addRequest(ActivityWrapper.getInstance(),
                ActivityWrapper.REQ_TYPE_START, null);

        // Set fullscreen mode
        int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        if (Build.VERSION.SDK_INT >= 19)
            flags |= View.SYSTEM_UI_FLAG_IMMERSIVE; // Force to keep fullscreen even if touched
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }
    public void onReversePosition(View sender) {

        // Reverse the device orientation in order to position camera at the expected distance
        // -> When cameras cannot be placed at the expected distance due to the position of the camera
        //    on a particular device (often the case for landscape orientation), this option allows the
        //    user to reverse it to place the camera as expected

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

    //////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_process);

        // Set current activity
        ActivityWrapper.set(this);

        /*
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
        */









        FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
        fragTransaction.add(R.id.main_container, new ProcessFragment(), ProcessFragment.TAG).commit();
        getSupportFragmentManager().executePendingTransactions();










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
}
