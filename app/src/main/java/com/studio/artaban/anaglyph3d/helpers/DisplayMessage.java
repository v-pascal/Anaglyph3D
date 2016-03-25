package com.studio.artaban.anaglyph3d.helpers;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import com.studio.artaban.anaglyph3d.R;

/**
 * Created by pascal on 25/03/16.
 * Display alert, toast or snack message (thread safe)
 */
public class DisplayMessage {

    private static DisplayMessage ourInstance = new DisplayMessage();
    public static DisplayMessage getInstance() { return ourInstance; }
    private DisplayMessage() { }

    //
    private AppCompatActivity mCurActivity;
    public void setActivity(AppCompatActivity activity) { mCurActivity = activity; }

    //////
    public void alert(final int title, final int message, final boolean quit) {

        // Display alert dialog message
        mCurActivity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                new AlertDialog.Builder(mCurActivity)
                        .setTitle(mCurActivity.getResources().getString(title))
                        .setMessage(mCurActivity.getResources().getString(message))
                        .setCancelable(true)
                        .setPositiveButton(mCurActivity.getResources().getString(R.string.close),
                                new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (quit)
                                    mCurActivity.finish();
                            }
                        })
                        .create().show();
            }
        });
    }
}
