package com.studio.artaban.anaglyph3d.helpers;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import com.studio.artaban.anaglyph3d.R;

/**
 * Created by pascal on 25/03/16.
 * Display alert, toast or snack message (thread safe)
 */
public class DisplayMessage {

    private static DisplayMessage ourInstance = new DisplayMessage();
    public static DisplayMessage getInstance() { return ourInstance; }
    private DisplayMessage() { }

    //////
    public void alert(final int title, final int message, final boolean quit) {

        // Display alert dialog message
        try {
            ActivityWrapper.get().runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    Activity curActivity;
                    try {
                        curActivity = ActivityWrapper.get();
                        if (curActivity == null)
                            throw new NullPointerException();
                    }
                    catch (NullPointerException e) {

                        Logs.add(Logs.Type.F, "Failed to display dialog message");
                        return;
                    }
                    new AlertDialog.Builder(ActivityWrapper.get())
                            .setTitle(curActivity.getResources().getString(title))
                            .setMessage(curActivity.getResources().getString(message))
                            .setCancelable(true)
                            .setPositiveButton(curActivity.getResources().getString(R.string.close),
                                    new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (quit)
                                                ActivityWrapper.get().finish();
                                        }
                                    })
                            .create().show();
                }
            });
        }
        catch (NullPointerException e) {
            Logs.add(Logs.Type.F, "Failed to display dialog message");
        }
    }
}
