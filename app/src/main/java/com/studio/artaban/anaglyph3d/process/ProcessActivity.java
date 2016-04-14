package com.studio.artaban.anaglyph3d.process;

import android.os.Build;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.transfer.Connectivity;

/**
 * Created by pascal on 12/04/16.
 * Activity to manage video recording and transferring using fragments:
 * _ Position fragment
 * _ Recorder fragment
 * _ Status & Contrast fragment
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

    //
    public void onValidatePosition(View sender) {









        /*
        // Send start request to remote device
        Connectivity.getInstance().addRequest(ActivityWrapper.getInstance(),
                ActivityWrapper.REQ_TYPE_START, null);
        */

        // Set fullscreen mode
        int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        if (Build.VERSION.SDK_INT >= 19)
            flags |= View.SYSTEM_UI_FLAG_IMMERSIVE;
        getWindow().getDecorView().setSystemUiVisibility(flags);







        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startRecording();
            }
        }, 1000);






    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        // Send cancel request to remote device (this action will finish the activity)
        Connectivity.getInstance().addRequest(ActivityWrapper.getInstance(),
                ActivityWrapper.REQ_TYPE_CANCEL, null);
    }

    //////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_process);

        // Set current activity
        ActivityWrapper.set(this);

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
}
