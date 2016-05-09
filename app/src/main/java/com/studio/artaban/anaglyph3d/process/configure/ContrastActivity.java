package com.studio.artaban.anaglyph3d.process.configure;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
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
import com.studio.artaban.anaglyph3d.helpers.Storage;
import com.studio.artaban.anaglyph3d.media.Frame;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ContrastActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    public static final String DATA_KEY_CONTRAST = "contrast";
    public static final String DATA_KEY_BRIGHTNESS = "brightness";
    public static final String DATA_KEY_LOCAL = "local";

    private static final String DATA_KEY_CHANGED = "changed";
    // Data keys

    public static final float DEFAULT_CONTRAST = 1f;
    public static final float DEFAULT_BRIGHTNESS = 0f;
    public static final boolean DEFAULT_LOCAL_FRAME = true;
    // Default values

    public static Bitmap applyContrastBrightness(Bitmap curBitmap, float contrast, float brightness) {

        ColorMatrix matrix = new ColorMatrix(new float[] {

                contrast, 0, 0, 0, brightness,
                0, contrast, 0, 0, brightness,
                0, 0, contrast, 0, brightness,
                0, 0, 0, 1, 0
        });
        // With: Contrast in [0;10] and 1 as default
        //       Brightness in [-255;255] and 0 as default
        // So: Contrast 0...1...10 => Progress 0...50...100
        //     Brightness -255...0...255 => Progress 0...255...510

        Bitmap bitmap = Bitmap.createBitmap(curBitmap.getWidth(), curBitmap.getHeight(),
                curBitmap.getConfig());

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(matrix));
        canvas.drawBitmap(curBitmap, 0, 0, paint);

        return bitmap;
    }

    //////
    private ImageView mContrastImage;
    private Bitmap mContrastBitmap;
    // Contrast & brightness image info

    private int mCompareWidth;
    private int mCompareHeight;
    // Size of the compare image

    private boolean loadImagesFromFiles(ImageView compareImage) { // Load both images from RGBA files

        File localFile = new File(ActivityWrapper.DOCUMENTS_FOLDER, Storage.FILENAME_LOCAL_PICTURE);
        File remoteFile = new File(ActivityWrapper.DOCUMENTS_FOLDER, Storage.FILENAME_REMOTE_PICTURE);

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
            mLocalFrame = true;
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
            mLocalFrame = false;
        }

        byte[] imageBuffer = new byte[(int)imageFile.length()];
        try {
            if (new FileInputStream(imageFile).read(imageBuffer) != imageBuffer.length)
                throw new IOException();

            mContrastBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
            mContrastBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(imageBuffer));
            mContrastImage.setImageBitmap(mContrastBitmap);
        }
        catch (IOException e) {

            Logs.add(Logs.Type.F, "Failed to load picture file: " + imageFile.getAbsolutePath());
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
            if (new FileInputStream(imageFile).read(imageBuffer) != imageBuffer.length)
                throw new IOException();

            Bitmap compareBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
            compareBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(imageBuffer));
            compareImage.setImageBitmap(compareBitmap);
        }
        catch (IOException e) {

            Logs.add(Logs.Type.F, "Failed to load picture file: " + imageFile.getAbsolutePath());
            return false;
        }
        return true;
    }
    private void setResult() {

        Intent intent = new Intent();
        intent.putExtra(DATA_KEY_CONTRAST, mContrast);
        intent.putExtra(DATA_KEY_BRIGHTNESS, mBrightness);
        intent.putExtra(DATA_KEY_LOCAL, mLocalFrame);

        setResult(Constants.RESULT_PROCESS_CONTRAST, intent);
    }

    //////
    private float mContrast = DEFAULT_CONTRAST;
    private float mBrightness = DEFAULT_BRIGHTNESS;
    private boolean mLocalFrame = DEFAULT_LOCAL_FRAME;

    private boolean mChanged = false;
    private FloatingActionButton mCancelButton;

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
        mContrastImage.setImageBitmap(applyContrastBrightness(mContrastBitmap, mContrast, mBrightness));
        if (!mChanged) {

            mChanged = true;
            mCancelButton.setImageDrawable(getResources().getDrawable(
                    R.drawable.ic_invert_colors_off_white_48dp));
        }
    }
    public void onValidateContrast(View sender) { // Validate contrast & brightness settings

        setResult();
        finish();
    }
    private void onCancel() {

        // Ask user to skip contrast & brightness step
        DisplayMessage.getInstance().alert(R.string.title_warning, R.string.ask_skip_step, null,
                true, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE)
                            finish();
                    }
                });
    }

    //////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contrast);

        // Set current activity
        ActivityWrapper.set(this);

        ActionBar appBar = getSupportActionBar();
        if (appBar != null) {

            if (Build.VERSION.SDK_INT >= 21) {
                getWindow().setNavigationBarColor(Color.BLACK);
                getWindow().setStatusBarColor(Color.BLACK);
            }
            else // Default status bar color (API < 21)
                appBar.setBackgroundDrawable(getResources().getDrawable(R.color.api_16_black));

            appBar.setDisplayHomeAsUpEnabled(true);
        }

        // Set default result
        setResult();

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
        if (mContrast < DEFAULT_CONTRAST) // Contrast [0;1] -> Progress [0;50]
            contrastSeek.setProgress(50 * (int)mContrast);
        else // Contrast [1;10] -> Progress [50;100]
            contrastSeek.setProgress((int)(50 * (mContrast - DEFAULT_CONTRAST) / (float)9) + 50);
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
                mContrast = DEFAULT_CONTRAST;
                mBrightness = DEFAULT_BRIGHTNESS;
                mChanged = false;

                contrastSeek.setProgress(50);
                brightnessSeek.setProgress(255);

                mContrastImage.setImageBitmap(applyContrastBrightness(mContrastBitmap,
                        DEFAULT_CONTRAST, DEFAULT_BRIGHTNESS));
            }
        });
        if (getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            ((CoordinatorLayout.LayoutParams)mCancelButton.getLayoutParams()).gravity =
                    Gravity.TOP | Gravity.CENTER_HORIZONTAL;
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
        mContrastImage.setImageBitmap(applyContrastBrightness(mContrastBitmap, mContrast, mBrightness));

        // Set validate button position (not exactly in center when device in portrait)
        if (getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {

            final ImageView icon = (ImageView)rootView.findViewById(R.id.brightness_icon);
            LayoutParams params = (LayoutParams)icon.getLayoutParams();
            final FloatingActionButton applyButton = (FloatingActionButton)findViewById(R.id.fab_apply);
            applyButton.setTranslationY(-params.height);
        }
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
        if (item.getItemId() == android.R.id.home) {

            onCancel();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //////
    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser)
            onUpdateContrastBrightness(seekBar);
    }
    @Override public void onStartTrackingTouch(SeekBar seekBar) { }
    @Override public void onStopTrackingTouch(SeekBar seekBar) { }
}
