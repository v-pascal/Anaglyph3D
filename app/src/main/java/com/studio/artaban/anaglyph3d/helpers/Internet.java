package com.studio.artaban.anaglyph3d.helpers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.studio.artaban.anaglyph3d.data.Constants;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by pascal on 26/05/16.
 * Internet connection helper
 */
public final class Internet {

    public static boolean isOnline(Context context) {
        // Check if Internet connection is available (check INTERNET permission first)

        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return (netInfo != null) && (netInfo.isConnectedOrConnecting());
    }

    //////
    private static final int BUFFER_SIZE = 4096;

    public enum DownloadResult {

        CANCELLED, // The download has been cancelled by the user
        WRONG_URL, // Wrong URL format
        CONNECTION_FAILED, // Failed to connect to URL
        SUCCEEDED // Download succeeded
    }
    public interface OnDownloadListener { // Download listener

        boolean onCheckCancelled();
        void onPublishProgress(int read);
    }

    //
    public static DownloadResult downloadHttpFile(String url, String file, OnDownloadListener listener) {

        InputStream is = null;
        OutputStream os = null;
        HttpURLConnection httpConnection = null;
        try {

            URL urlFile = new URL(url);
            httpConnection = (HttpURLConnection)urlFile.openConnection();
            httpConnection.connect();

            if (httpConnection.getResponseCode() != HttpURLConnection.HTTP_OK)
                throw new IOException();

            // Save reply into expected file
            is = httpConnection.getInputStream();
            os = new FileOutputStream(file);

            byte buffer[] = new byte[BUFFER_SIZE];
            int bufferRead;
            while ((bufferRead = is.read(buffer)) != Constants.NO_DATA) {

                // Check if download has been cancelled
                if ((listener != null) && (listener.onCheckCancelled()))
                    return DownloadResult.CANCELLED;

                // Check if needed to update progress bar
                if (listener != null)
                    listener.onPublishProgress(bufferRead);
                os.write(buffer, 0, bufferRead);
            }
            return DownloadResult.SUCCEEDED;
        }
        catch (MalformedURLException e) {

            Logs.add(Logs.Type.F, "Wrong web service URL: " + e.getMessage());
            return DownloadResult.WRONG_URL;
        }
        catch (IOException e) {

            Logs.add(Logs.Type.E, "Failed to connect to web service: " + e.getMessage());
            return DownloadResult.CONNECTION_FAILED;
        }
        finally {

            if (httpConnection != null)
                httpConnection.disconnect();

            try {
                if (is != null) is.close();
                if (os != null) os.close();
            }
            catch (IOException e) {
                Logs.add(Logs.Type.E, "Failed to close IO streams");
            }
        }
    }
}
