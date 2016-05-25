package com.studio.artaban.anaglyph3d.album;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.helpers.Storage;

import java.io.File;
import java.io.IOException;

/**
 * Created by pascal on 25/05/16.
 * Activity to copy:
 * _ video/* mime type file into 'Movies' local folder
 * _ image/* mime type file into 'Pictures' local folder
 */
public class ShareLocalActivity extends Activity {

    public static final String EXTRA_SUBFOLDER = "com.studio.artaban.intent.extra.SUBFOLDER";
    public static final String EXTRA_OVERWRITE = "com.studio.artaban.intent.extra.OVERWRITE";

    //
    private static final String LOG_TAG = "My files";

    private static final String MIME_TYPE_IMAGE = "image/";
    private static final String MIME_TYPE_VIDEO = "video/";

    private enum FolderId { NONE, MOVIES, PICTURES }

    //////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check data
        Uri dataUri = (Uri)getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
        File dataFile = (dataUri != null)? new File(dataUri.getPath()):null;

        FolderId folder = FolderId.NONE;
        if (getIntent().getType().startsWith(MIME_TYPE_IMAGE)) folder = FolderId.PICTURES;
        if (getIntent().getType().startsWith(MIME_TYPE_VIDEO)) folder = FolderId.MOVIES;

        if (!Intent.ACTION_SEND.equals(getIntent().getAction()) || (folder == FolderId.NONE) ||
                (dataFile == null) || (!dataFile.exists()) || (!dataFile.isFile())) {

            Log.e(LOG_TAG, "Wrong intent data!\nExpected: 'video/*' or 'image/*' MIME" +
                    " type with a valid URI data file");
            finish();
            return;
        }

        String folderPath = (folder == FolderId.MOVIES)?
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath():
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();

        // Add sub folder (if expected)
        if (getIntent().hasExtra(EXTRA_SUBFOLDER)) {

            folderPath += (getIntent().getStringExtra(EXTRA_SUBFOLDER) != null)?
                    File.separator + getIntent().getStringExtra(EXTRA_SUBFOLDER):null;

            File directory = new File(folderPath);
            if (!directory.exists())
                directory.mkdir();
        }
        File localFile = new File(folderPath + dataUri.getPath().substring(
                dataUri.getPath().lastIndexOf(File.separator)));

        try {
            // Check if local file already exists
            if (localFile.exists()) {
                if (getIntent().getBooleanExtra(EXTRA_OVERWRITE, false)) // Overwrite requested
                    localFile.delete();

                else {
                    Toast.makeText(this, R.string.already_exist, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }
            Storage.copyFile(dataFile, localFile);

            int success = (folder == FolderId.MOVIES)? R.string.movie_copied:R.string.picture_copied;
            String message = getString(success,
                    (getIntent().hasExtra(EXTRA_SUBFOLDER))? File.separator +
                            getIntent().getStringExtra(EXTRA_SUBFOLDER):null);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
        catch (IOException e) {

            Log.e(LOG_TAG, "Failed to copy file '" + dataFile.getAbsolutePath() + "' to '" +
                    localFile.getAbsolutePath() + "'");

            int error = (folder == FolderId.MOVIES)? R.string.movie_not_copied:R.string.picture_not_copied;
            String message = getString(error,
                    (getIntent().hasExtra(EXTRA_SUBFOLDER))? File.separator +
                            getIntent().getStringExtra(EXTRA_SUBFOLDER):null);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
