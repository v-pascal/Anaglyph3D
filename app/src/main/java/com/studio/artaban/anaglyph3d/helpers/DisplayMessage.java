package com.studio.artaban.anaglyph3d.helpers;

import android.content.DialogInterface;
import android.support.design.widget.Snackbar;
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
        Logs.add(Logs.Type.V, "title: " + title + ", message1: " + message1 + ", message2: " + message2 +
                ", confirm: " + confirm + ", validListener: " + validListener);
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
                                            validListener)
                                    .setCancelable(false);

                        // Display dialog
                        dialog.create().show();
                    }
                    catch (NullPointerException e) {
                        Logs.add(Logs.Type.F, "Failed to display Dialog message");
                    }
                }
            });
        }
        catch (NullPointerException e) {
            Logs.add(Logs.Type.F, "Failed to display Dialog message");
        }
    }
    public void toast(final int message, final int duration) {

        // Display toast message
        Logs.add(Logs.Type.V, "message: " + message + ", duration: " + duration);
        try {

            ActivityWrapper.get().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    try { Toast.makeText(ActivityWrapper.get(), message, duration).show(); }
                    catch (NullPointerException e) {
                        Logs.add(Logs.Type.F, "Failed to display Toast message");
                    }
                }
            });
        }
        catch (NullPointerException e) {
            Logs.add(Logs.Type.F, "Failed to display Toast message");
        }
    }
    public void snack(final int view, final int[] messages, final int duration) {

        // Display SnackBar message
        Logs.add(Logs.Type.V, "view: " + view + ", messages: " + ((messages != null)? messages.length:"null") +
                ", duration: " + duration);
        try {

            ActivityWrapper.get().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    try {
                        String message = "";
                        for (int i = 0; i < messages.length; ++i) {
                            message += ActivityWrapper.get().getResources().getString(messages[i]);
                            if (i == 0)
                                message += " ";
                            else if ((i + 1) < messages.length)
                                message += " & ";
                        }
                        Snackbar.make(ActivityWrapper.get().findViewById(view),
                                message, duration).show();
                    }
                    catch (NullPointerException e) {
                        Logs.add(Logs.Type.F, "Failed to display SnackBar message");
                    }
                }
            });
        }
        catch (NullPointerException e) {
            Logs.add(Logs.Type.F, "Failed to display SnackBar message");
        }
    }
}
