package com.studio.artaban.anaglyph3d.process.configure;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.Toast;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.media.Frame;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ContrastActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    public static final String DATA_KEY_CONTRAST = "contrast";
    public static final String DATA_KEY_BRIGHTNESS = "brightness";
    private static final String DATA_KEY_CHANGED = "changed";

    //////
    private ImageView mContrastImage;
    private Bitmap mContrastBitmap;
    // Contrast & brightness image info

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







        //if (getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)




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

    private float mContrast = 1;
    private float mBrightness = 0;
    private boolean mChanged = false;

    private FloatingActionButton mCancelButton;

    private void applyContrastBrightness() { // Apply contrast & brightness settings

        ColorMatrix matrix = new ColorMatrix(new float[] {

                mContrast, 0, 0, 0, mBrightness,
                0, mContrast, 0, 0, mBrightness,
                0, 0, mContrast, 0, mBrightness,
                0, 0, 0, 1, 0
        });
        Bitmap bitmap = Bitmap.createBitmap(mContrastBitmap.getWidth(), mContrastBitmap.getHeight(),
                mContrastBitmap.getConfig());

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(matrix));
        canvas.drawBitmap(mContrastBitmap, 0, 0, paint);

        mContrastImage.setImageBitmap(bitmap);
    }

    //
    private void onUpdateContrastBrightness(SeekBar seekBar) {
        switch (seekBar.getId()) {

            case R.id.seek_contrast:
                mContrast = seekBar.getProgress() / 10;
                break;

            case R.id.seek_brightness:
                mBrightness = seekBar.getProgress() - 255;
                break;
        }
        applyContrastBrightness();
        if (!mChanged) {

            mChanged = true;
            mCancelButton.setImageDrawable(getResources().getDrawable(
                    R.drawable.ic_invert_colors_off_white_48dp));
        }
    }
    public void onValidateContrast(View sender) { // Validate contrast & brightness settings

        getIntent().putExtra(DATA_KEY_CONTRAST, mContrast);
        getIntent().putExtra(DATA_KEY_BRIGHTNESS, mBrightness);

        setResult(Constants.RESULT_PROCESS_CONTRAST);
        finish();
    }

    //////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contrast);

        // Set current activity
        ActivityWrapper.set(this);

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

        // Set default activity result
        setResult(Constants.RESULT_PROCESS_CANCELLED);

        // Restore previous settings (if any)
        if (savedInstanceState != null) {

            mContrast = savedInstanceState.getFloat(DATA_KEY_CONTRAST);
            mBrightness = savedInstanceState.getFloat(DATA_KEY_BRIGHTNESS);
            mChanged = savedInstanceState.getBoolean(DATA_KEY_CHANGED);
        }

        // Set layout to display according orientation
        final ViewStub stubView = (ViewStub) findViewById(R.id.layout_container);
        if (getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            stubView.setLayoutResource(R.layout.contrast_landscape);
        else
            stubView.setLayoutResource(R.layout.contrast_landscape);

        final View rootView = stubView.inflate();
        mContrastImage = (ImageView)rootView.findViewById(R.id.image_contrast);
        final ImageView compareImage = (ImageView)rootView.findViewById(R.id.image_compare);

        // Set contrast seek bar position
        final SeekBar contrastSeek = (SeekBar)rootView.findViewById(R.id.seek_contrast);
        contrastSeek.setMax(100);





        contrastSeek.setProgress(10 * (int)mContrast);
        /*
        if (mContrast < 1)
            contrastSeek.setProgress(50 * (int)mContrast);
        else if (mContrast > 1)
            contrastSeek.setProgress();
        else
            contrastSeek.setProgress(50);
            */





        contrastSeek.setOnSeekBarChangeListener(this);

        // Set brightness seek bar position
        final SeekBar brightnessSeek = (SeekBar)rootView.findViewById(R.id.seek_brightness);
        brightnessSeek.setMax(512);
        brightnessSeek.setProgress((int)mBrightness + 255);
        brightnessSeek.setOnSeekBarChangeListener(this);

        // Configure floating button that allows user to cancel settings (and that informs user on
        // which image the contrast & brightness configuration is applied)
        mCancelButton = (FloatingActionButton)findViewById(R.id.fab_apply);
        mCancelButton.setImageDrawable(getResources().getDrawable((!mChanged)?
                R.drawable.ic_invert_colors_white_48dp:R.drawable.ic_invert_colors_off_white_48dp));
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mCancelButton.setImageDrawable(getResources().getDrawable(
                        R.drawable.ic_invert_colors_white_48dp));

                // Reset contrast & brightness settings
                mContrast = 1;
                mBrightness = 0;
                mChanged = false;

                contrastSeek.setProgress(10 * (int)mContrast);
                brightnessSeek.setProgress((int)mBrightness + 255);

                applyContrastBrightness();
            }
        });












        File documents = getExternalFilesDir(null);
        if (documents != null)
            ActivityWrapper.DOCUMENTS_FOLDER = documents.getAbsolutePath();
        else
            Logs.add(Logs.Type.F, "Failed to get documents folder");
        Frame.getInstance().init();
        Bundle data = new Bundle();
        data.putInt(Frame.DATA_KEY_WIDTH, 640);
        data.putInt(Frame.DATA_KEY_HEIGHT, 480);
        getIntent().putExtra(Constants.DATA_ACTIVITY, data);












        ////// Load images
        if (!loadImagesFromFiles(compareImage)) {

            // Inform user
            DisplayMessage.getInstance().toast(R.string.error_contrast_failed, Toast.LENGTH_LONG);

            finish();
            return;
        }

        // Apply contrast & brightness settings
        applyContrastBrightness();

        ////// Position images
        final Point screenSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(screenSize);
        int screenHeight = screenSize.y - getActionBarHeight();

        // Get control panel height at the screen bottom according orientation (in pixel)



        final ImageView icon = (ImageView)rootView.findViewById(R.id.brightness_icon);
        LayoutParams params = (LayoutParams)icon.getLayoutParams();
        screenHeight -= params.height;





        positionImages(compareImage, screenSize.x, screenHeight);

















    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putFloat(DATA_KEY_CONTRAST, mContrast);
        outState.putFloat(DATA_KEY_BRIGHTNESS, mBrightness);
        outState.putBoolean(DATA_KEY_CHANGED, mChanged);

        super.onSaveInstanceState(outState);
    }

    //////
    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { }
    @Override public void onStartTrackingTouch(SeekBar seekBar) { onUpdateContrastBrightness(seekBar); }
    @Override public void onStopTrackingTouch(SeekBar seekBar) { onUpdateContrastBrightness(seekBar); }

}
