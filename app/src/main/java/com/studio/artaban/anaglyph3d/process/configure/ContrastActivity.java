package com.studio.artaban.anaglyph3d.process.configure;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.media.Frame;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ContrastActivity extends AppCompatActivity {

    private ImageView mContrastImage;
    private Bitmap mContrastBitmap;
    // Contrast image info

    private int mCompareWidth;
    private int mCompareHeight;
    // Size of the compare image

    private int getActionBarHeight() { // Return height of the action bar (in pixel)

        TypedValue typedValue = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true))
            return TypedValue.complexToDimensionPixelSize(typedValue.data,
                    getResources().getDisplayMetrics());

        Logs.add(Logs.Type.W, "'android.R.attr.actionBarSize' attribute not found");
        return 0;
    }
    private boolean loadImagesFromFiles(ImageView compareImage) { // Load both images from RGBA files

        File localFile = new File(ActivityWrapper.DOCUMENTS_FOLDER,
                Constants.PROCESS_LOCAL_PICTURE_FILENAME);
        File remoteFile = new File(ActivityWrapper.DOCUMENTS_FOLDER,
                Constants.PROCESS_REMOTE_PICTURE_FILENAME);

        Bundle data = getIntent().getExtras().getBundle(Constants.DATA_ACTIVITY);
        if (data == null) {

            Logs.add(Logs.Type.F, "No data activity found");
            return false;
        }

        //
        File imageFile;
        int imageWidth, imageHeight;
        if (localFile.length() > remoteFile.length()) { // Set contrast & brightness to local picture...

            imageFile = localFile;
            imageWidth = data.getInt(Frame.DATA_KEY_WIDTH);
            imageHeight = data.getInt(Frame.DATA_KEY_HEIGHT);
        }
        else { // ...or to remote picture

            imageFile = remoteFile;
            imageWidth = Frame.getInstance().getWidth();
            imageHeight = Frame.getInstance().getHeight();
        }

        byte[] imageBuffer = new byte[(int)imageFile.length()];
        try {
            if (new FileInputStream(imageFile).read(imageBuffer) == imageBuffer.length) {

                mContrastBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
                mContrastBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(imageBuffer));
                mContrastImage.setImageBitmap(mContrastBitmap);
            }
            else
                throw new IOException();
        }
        catch (IOException e) {

            Logs.add(Logs.Type.E, "Failed to load picture file: " + imageFile.getAbsolutePath());
            return false;
        }

        //
        if (imageFile == localFile) {

            imageFile = remoteFile;
            imageWidth = mCompareWidth = Frame.getInstance().getWidth();
            imageHeight = mCompareHeight = Frame.getInstance().getHeight();
        }
        else {

            imageFile = localFile;
            imageWidth = mCompareWidth = data.getInt(Frame.DATA_KEY_WIDTH);
            imageHeight = mCompareHeight = data.getInt(Frame.DATA_KEY_HEIGHT);
        }

        imageBuffer = new byte[(int)imageFile.length()];
        try {
            if (new FileInputStream(imageFile).read(imageBuffer) == imageBuffer.length) {

                Bitmap compareBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
                compareBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(imageBuffer));
                compareImage.setImageBitmap(compareBitmap);
            }
            else
                throw new IOException();
        }
        catch (IOException e) {

            Logs.add(Logs.Type.E, "Failed to load picture file: " + imageFile.getAbsolutePath());
            return false;
        }
        return true;
    }
    private void positionImages(ImageView compareImage, int screenWidth, int screenHeight) {










        ///////////////// Landscape


        LayoutParams params = (LayoutParams)mContrastImage.getLayoutParams();
        params.width = screenWidth >> 1;
        params.height = (int)(params.width * mContrastBitmap.getHeight() / (float)mContrastBitmap.getWidth());
        if (params.height < screenHeight) {

            params.height = screenHeight;
            params.width = (int)(mContrastBitmap.getWidth() * screenHeight / (float)mContrastBitmap.getHeight());

            // Shift both images in order to move them in the middle of the screen (horizontally)
            // -> This is needed coz the 'LinearLayout' allows child images to overstep its bounds
            //    vertically but not horizontally. If not shift the other image will be partially
            //    visible.

            params.setMargins((screenWidth - (params.width << 1)) / 2, 0, 0, 0);
            // ...note the left margin above has a negative value to shift images on the left
        }
        mContrastImage.setLayoutParams(params);

        params = (LayoutParams)compareImage.getLayoutParams();
        params.width = screenWidth >> 1;
        params.height = (int)(params.width * mCompareHeight / (float)mCompareWidth);
        if (params.height < screenHeight) {

            params.height = screenHeight;
            params.width = (int)(mCompareWidth * screenHeight / (float)mCompareHeight);
        }
        compareImage.setLayoutParams(params);







    }

    //
    public void onValidateContrast(View sender) {









    }

    //////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contrast);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (toolbar != null) {
            if (Build.VERSION.SDK_INT >= 21) {
                toolbar.setBackgroundColor(Color.BLACK);
                getWindow().setNavigationBarColor(Color.BLACK);
                getWindow().setStatusBarColor(Color.BLACK);
            }
            else // Default status bar color (API < 21)
                toolbar.setBackgroundColor(Color.argb(255, 30, 30, 30));
        }

        final ActionBar appBar = getSupportActionBar();
        if (appBar != null)
            appBar.setDisplayHomeAsUpEnabled(true);

        //
        final ViewStub stub = (ViewStub) findViewById(R.id.layout_container);
        stub.setLayoutResource(R.layout.contrast_landscape);

        final View rootView = stub.inflate();
        mContrastImage = (ImageView)rootView.findViewById(R.id.image_contrast);
        final ImageView compareImage = (ImageView)rootView.findViewById(R.id.image_compare);












        File documents = getExternalFilesDir(null);
        if (documents != null)
            ActivityWrapper.DOCUMENTS_FOLDER = documents.getAbsolutePath();
        else
            Logs.add(Logs.Type.F, "Failed to get documents folder");









        ////// Load images
        if (!loadImagesFromFiles(compareImage)) {





            finish();
            return;
        }







        ////// Position images
        final Point screenSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(screenSize);
        int screenHeight = screenSize.y - getActionBarHeight();

        // Get control panel height at the screen bottom (in pixel)
        final ImageView icon = (ImageView)rootView.findViewById(R.id.brightness_icon);
        LayoutParams params = (LayoutParams)icon.getLayoutParams();
        screenHeight -= params.height;

        positionImages(compareImage, screenSize.x, screenHeight);

















    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);




    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {






        super.onSaveInstanceState(outState);
    }
}
