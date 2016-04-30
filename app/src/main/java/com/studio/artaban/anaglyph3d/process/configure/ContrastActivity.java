package com.studio.artaban.anaglyph3d.process.configure;

import android.content.DialogInterface;
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
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.Toast;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.media.Frame;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ContrastActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener,
        DialogInterface.OnClickListener {

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
            if (Settings.getInstance().mOrientation) { // Portrait
                imageWidth = data.getInt(Frame.DATA_KEY_HEIGHT);
                imageHeight = data.getInt(Frame.DATA_KEY_WIDTH);
            }
            else { // Landscape
                imageWidth = data.getInt(Frame.DATA_KEY_WIDTH);
                imageHeight = data.getInt(Frame.DATA_KEY_HEIGHT);
            }
        }
        else { // ...or to remote picture

            imageFile = remoteFile;
            if (Settings.getInstance().mOrientation) { // Portrait
                imageWidth = Frame.getInstance().getHeight();
                imageHeight = Frame.getInstance().getWidth();
            }
            else { // Landscape
                imageWidth = Frame.getInstance().getWidth();
                imageHeight = Frame.getInstance().getHeight();
            }
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
            if (Settings.getInstance().mOrientation) { // Portrait
                imageWidth = mCompareWidth = Frame.getInstance().getHeight();
                imageHeight = mCompareHeight = Frame.getInstance().getWidth();
            }
            else { // Landscape
                imageWidth = mCompareWidth = Frame.getInstance().getWidth();
                imageHeight = mCompareHeight = Frame.getInstance().getHeight();
            }
        }
        else {

            imageFile = localFile;
            if (Settings.getInstance().mOrientation) { // Portrait
                imageWidth = mCompareWidth = data.getInt(Frame.DATA_KEY_HEIGHT);
                imageHeight = mCompareHeight = data.getInt(Frame.DATA_KEY_WIDTH);
            }
            else { // Landscape
                imageWidth = mCompareWidth = data.getInt(Frame.DATA_KEY_WIDTH);
                imageHeight = mCompareHeight = data.getInt(Frame.DATA_KEY_HEIGHT);
            }
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
        // Position contrast & compare images according the orientation

        LayoutParams params = (LayoutParams)mContrastImage.getLayoutParams();
        if (getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {

            params.height = screenHeight >> 1;
            params.width = (int)(params.height * mContrastBitmap.getWidth() / (float)mContrastBitmap.getHeight());
            if (params.width < screenWidth) {

                params.width = screenWidth;
                params.height = (int)(params.width * mContrastBitmap.getHeight() / (float)mContrastBitmap.getWidth());

                // Shift both images in order to move them in the middle of the screen (vertically)
                // -> This is needed coz a 'LinearLayout' oriented vertically allows child images to
                //    overstep its bounds horizontally but not vertically. If not shift the other image
                //    will be partially visible.

                params.setMargins(0, (screenHeight >> 1) - params.height, 0, 0);
                // ...note the top margin above has a negative value to shift images to the top
            }
        }
        else { // Landscape

            params.width = screenWidth >> 1;
            params.height = (int)(params.width * mContrastBitmap.getHeight() / (float)mContrastBitmap.getWidth());
            if (params.height < screenHeight) {

                params.height = screenHeight;
                params.width = (int)(mContrastBitmap.getWidth() * screenHeight / (float)mContrastBitmap.getHeight());

                // Shift both images in order to move them in the middle of the screen (horizontally)
                // -> This is needed coz a 'LinearLayout' oriented horizontally allows child images to
                //    overstep its bounds vertically but not horizontally. If not shift the other image
                //    will be partially visible.

                params.setMargins((screenWidth - (params.width << 1)) / 2, 0, 0, 0);
                // ...note the left margin above has a negative value to shift images to the left
            }
        }
        mContrastImage.setLayoutParams(params);

        params = (LayoutParams)compareImage.getLayoutParams();
        if (getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {

            params.height = screenHeight >> 1;
            params.width = (int)(params.height * mCompareWidth / (float)mCompareHeight);
            if (params.width < screenWidth) {

                params.width = screenWidth;
                params.height = (int)(params.width * mCompareHeight / (float)mCompareWidth);
            }
        }
        else { // Landscape

            params.width = screenWidth >> 1;
            params.height = (int)(params.width * mCompareHeight / (float)mCompareWidth);
            if (params.height < screenHeight) {

                params.height = screenHeight;
                params.width = (int)(mCompareWidth * screenHeight / (float)mCompareHeight);
            }
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
        // With: Contrast in [0;10] and 1 as default
        //       Brightness in [-255;255] and 0 as default
        // So: Contrast 0...1...10 => Progress 0...50...100
        //     Brightness -255...0...255 => Progress 0...255...510

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
                if (seekBar.getProgress() < 50)
                    mContrast = seekBar.getProgress() / (float)50;
                else
                    mContrast = (9 * (seekBar.getProgress() - 50) / (float)50) + 1;

                // With: Progress in [0;50] -> Contrast [0;1]
                //       Progress in [50;100] -> Contrast [1;10]
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
    private void onCancel() {

        // Ask user to skip contrast & brightness step
        DisplayMessage.getInstance().alert(R.string.title_warning, R.string.ask_skip_contrast,
                null, true, this);
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
            stubView.setLayoutResource(R.layout.contrast_portrait);
        else
            stubView.setLayoutResource(R.layout.contrast_landscape);

        final View rootView = stubView.inflate();
        mContrastImage = (ImageView)rootView.findViewById(R.id.image_contrast);
        final ImageView compareImage = (ImageView)rootView.findViewById(R.id.image_compare);

        // Set contrast seek bar position
        final SeekBar contrastSeek = (SeekBar)rootView.findViewById(R.id.seek_contrast);
        contrastSeek.setMax(100);
        if (mContrast < 1) // Contrast [0;1] -> Progress [0;50]
            contrastSeek.setProgress(50 * (int)mContrast);
        else // Contrast [1;10] -> Progress [50;100]
            contrastSeek.setProgress((int)(50 * (mContrast - 1) / (float)9) + 50);
        contrastSeek.setOnSeekBarChangeListener(this);

        // Set brightness seek bar position
        final SeekBar brightnessSeek = (SeekBar)rootView.findViewById(R.id.seek_brightness);
        brightnessSeek.setMax(510); // == 2 * 255
        brightnessSeek.setProgress((int)mBrightness + 255);
        brightnessSeek.setOnSeekBarChangeListener(this);

        // Configure the floating button that allows user to cancel settings (and that informs user on
        // which image the contrast & brightness configuration is applied)
        mCancelButton = (FloatingActionButton)findViewById(R.id.fab_cancel);
        mCancelButton.setImageDrawable(getResources().getDrawable((!mChanged) ?
                R.drawable.ic_invert_colors_white_48dp : R.drawable.ic_invert_colors_off_white_48dp));
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mCancelButton.setImageDrawable(getResources().getDrawable(
                        R.drawable.ic_invert_colors_white_48dp));

                // Reset contrast & brightness settings
                mContrast = 1;
                mBrightness = 0;
                mChanged = false;

                contrastSeek.setProgress(50);
                brightnessSeek.setProgress(255);

                applyContrastBrightness();
            }
        });
        if (getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {

            CoordinatorLayout.LayoutParams fabParams =
                    (CoordinatorLayout.LayoutParams)mCancelButton.getLayoutParams();

            fabParams.setMargins(0, getActionBarHeight() + 8, 0, 0);
            fabParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            mCancelButton.setLayoutParams(fabParams);

            // Set validate button position (not exactly in center)
            final FloatingActionButton applyButton = (FloatingActionButton)findViewById(R.id.fab_apply);
            ((CoordinatorLayout.LayoutParams)applyButton.getLayoutParams()).setMargins(0, 0, 0, 32);

        }
        else
            ((CoordinatorLayout.LayoutParams)mCancelButton.getLayoutParams()).gravity =
                    Gravity.START|Gravity.CENTER_VERTICAL;

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
        if (getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            screenHeight -= params.height << 1;
        else
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

    @Override public void onBackPressed() { onCancel(); }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.home) {

            onCancel();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //////
    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE)
            finish();
    }

    //////
    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser)
            onUpdateContrastBrightness(seekBar);
    }
    @Override public void onStartTrackingTouch(SeekBar seekBar) { }
    @Override public void onStopTrackingTouch(SeekBar seekBar) { }

}
