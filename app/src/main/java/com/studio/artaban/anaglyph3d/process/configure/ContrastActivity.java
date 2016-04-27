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
import android.widget.LinearLayout;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.Logs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ContrastActivity extends AppCompatActivity {

    private ImageView mContrastImage;
    private Bitmap mContrastBitmap;

    private int getActionBarHeight() { // Return height of the action bar (in pixel)

        TypedValue typedValue = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true))
            return TypedValue.complexToDimensionPixelSize(typedValue.data,
                    getResources().getDisplayMetrics());

        Logs.add(Logs.Type.W, "'android.R.attr.actionBarSize' attribute not found");
        return 0;
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









        final ViewStub stub = (ViewStub) findViewById(R.id.layout_container);
        stub.setLayoutResource(R.layout.contrast_landscape);
        final View root = stub.inflate();





        mContrastImage = (ImageView)root.findViewById(R.id.image_contrast);
        final ImageView compareImage = (ImageView)root.findViewById(R.id.image_compare);










        File documents = getExternalFilesDir(null);
        if (documents != null)
            ActivityWrapper.DOCUMENTS_FOLDER = documents.getAbsolutePath();
        else
            Logs.add(Logs.Type.F, "Failed to get documents folder");







        ////// Load images
        File localFile = new File(ActivityWrapper.DOCUMENTS_FOLDER,
                Constants.PROCESS_LOCAL_PICTURE_FILENAME);
        File remoteFile = new File(ActivityWrapper.DOCUMENTS_FOLDER,
                Constants.PROCESS_REMOTE_PICTURE_FILENAME);

        File imageFile;
        int imageWidth, imageHeight;
        if (localFile.length() > remoteFile.length()) { // Set contrast & brightness to local picture...

            imageFile = localFile;
            imageWidth = 640;
            imageHeight = 480;
        }
        else { // ...or to remote picture

            imageFile = remoteFile;
            imageWidth = 640;
            imageHeight = 480;
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
        }

        if (imageFile == localFile) {

            imageFile = remoteFile;
            imageWidth = 640;
            imageHeight = 480;
        }
        else {

            imageFile = localFile;
            imageWidth = 640;
            imageHeight = 480;
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
        }




        //getIntent().getExtras().getBundle();





        ///////////////// Landscape


        ////// Position images
        final Point screenSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(screenSize);
        int screenWidth = screenSize.x;
        int screenHeight = screenSize.y - getActionBarHeight();



        // Get control panel height at the screen bottom (in pixel)

        final ImageView icon = (ImageView)root.findViewById(R.id.brightness_icon);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)icon.getLayoutParams();
        screenHeight -= params.height;




        params = (LinearLayout.LayoutParams)mContrastImage.getLayoutParams();
        params.width = screenWidth >> 1;
        params.height = (int)(params.width * 128 / (float)75);
        if (params.height < screenHeight) {

            params.height = screenHeight;
            params.width = (int)(75 * screenHeight / (float)128);

            // Shift both images in order to move them in the middle of the screen (horizontally)
            // -> This is needed coz the 'LinearLayout' allows child images to overstep its bounds
            //    vertically but not horizontally. If not shift the other image will be partially
            //    visible.

            params.setMargins((screenWidth - (params.width << 1)) / 2, 0, 0, 0);
            // ...note the left margin above has a negative value to shift images on the left
        }
        mContrastImage.setLayoutParams(params);

        params = (LinearLayout.LayoutParams)compareImage.getLayoutParams();
        params.width = screenWidth >> 1;
        params.height = (int)(params.width * 480 / (float)640);
        if (params.height < screenHeight) {

            params.height = screenHeight;
            params.width = (int)(640 * screenHeight / (float)480);
        }
        compareImage.setLayoutParams(params);







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
