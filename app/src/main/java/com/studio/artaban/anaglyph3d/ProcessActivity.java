package com.studio.artaban.anaglyph3d;

import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;

/**
 * Created by pascal on 12/04/16.
 * Activity to manage video recording and transferring using fragments:
 * _ Recorder fragment
 * _ Status fragment
 * _ Contrast fragment
 */
public class ProcessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_process);

        // Set current activity
        ActivityWrapper.set(this);

        // Add camera fragment (after having set initial position)
        FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
        fragTransaction.add(R.id.main_container, new RecorderFragment(),
                RecorderFragment.TAG_FRAGMENT).commit();
        getSupportFragmentManager().executePendingTransactions();
    }
}
