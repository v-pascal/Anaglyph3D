package com.studio.artaban.anaglyph3d.helpers;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

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
    public void alert(final int title, final int message1, final String message2, final boolean confirm,
                      final DialogInterface.OnClickListener validListener) {

        // Display alert dialog message
        try {
            ActivityWrapper.get().runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    try {
                        // Build dialog
                        AlertDialog.Builder dialog = new AlertDialog.Builder(ActivityWrapper.get())
                                .setTitle(ActivityWrapper.get().getResources().getString(title))
                                .setMessage(ActivityWrapper.get().getResources().getString(message1) +
                                        ((message2 != null) ? " " + message2 : ""));
                        if (!confirm)
                            dialog.setPositiveButton(ActivityWrapper.get().getResources().getString(R.string.close),
                                    validListener);
                        else
                            dialog.setPositiveButton(ActivityWrapper.get().getResources().getString(android.R.string.ok),
                                            validListener)
                                    .setNegativeButton(ActivityWrapper.get().getResources().getString(android.R.string.cancel),
                                            null)
                                    .setCancelable(false);

                        // Display dialog
                        dialog.create().show();
                    }
                    catch (NullPointerException e) {
                        Logs.add(Logs.Type.F, "Failed to display dialog message");
                    }
                }
            });
        }
        catch (NullPointerException e) {
            Logs.add(Logs.Type.F, "Failed to display dialog message");
        }
    }
    public void toast(final int message, final int duration) {

        // Display toast message
        try {
            ActivityWrapper.get().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    try { Toast.makeText(ActivityWrapper.get(), message, duration).show(); }
                    catch (NullPointerException e) {
                        Logs.add(Logs.Type.F, "Failed to display toast message");
                    }
                }
            });
        }
        catch (NullPointerException e) {
            Logs.add(Logs.Type.F, "Failed to display toast message");
        }
    }
}
