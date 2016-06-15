package com.studio.artaban.anaglyph3d.process.configure;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
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
import android.view.Menu;
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

public class CorrectionActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    public static final String DATA_KEY_CONTRAST = "contrast";
    public static final String DATA_KEY_BRIGHTNESS = "brightness";
    public static final String DATA_KEY_RED_BALANCE = "redBalance";
    public static final String DATA_KEY_GREEN_BALANCE = "greenBalance";
    public static final String DATA_KEY_BLUE_BALANCE = "blueBalance";
    public static final String DATA_KEY_LOCAL = "local";

    private static final String DATA_KEY_CHANGED = "changed";
    private static final String DATA_KEY_CORRECTION = "correction";
    // Data keys

    public static final float DEFAULT_CONTRAST = 1f;
    public static final float DEFAULT_BRIGHTNESS = 0f;
    public static final float DEFAULT_BALANCE = 1f;
    public static final boolean DEFAULT_LOCAL_FRAME = true;
    // Default values

    public static Bitmap applyCorrection(Bitmap curBitmap, float contrast, float brightness,
                                         float red, float green, float blue) {

        // Apply contrast, brightness and colors balance correction to current bitmap
        Logs.add(Logs.Type.V, "curBitmap: " + curBitmap + ", contrast: " + contrast + ", brightness: " +
                brightness + ", red: " + red + ", green: " + green + ", blue: " + blue);

        ColorMatrix matrix = new ColorMatrix(new float[] {

                red * contrast, 0, 0, 0, brightness,
                0, green * contrast, 0, 0, brightness,
                0, 0, blue * contrast, 0, brightness,
                0, 0, 0, 1, 0
        });
        // With: Contrast in [0;10] and 1 as default
        //       Brightness in [-255;255] and 0 as default
        //       red balance in [0.5;1.5] and 1 as default
        //       green balance in [0.5;1.5] and 1 as default
        //       blue balance in [0.5;1.5] and 1 as default
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
    private ImageView mCorrectionImage;
    private Bitmap mCorrectionBitmap;
    // Contrast, brightness & color balance displayed image

    private boolean loadImagesFromFiles(ImageView compareImage) { // Load both images from RGBA files

        Logs.add(Logs.Type.V, "compareImage: " + compareImage);
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

            mCorrectionBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
            mCorrectionBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(imageBuffer));
            mCorrectionImage.setImageBitmap(mCorrectionBitmap);
        }
        catch (IOException e) {

            Logs.add(Logs.Type.F, "Failed to load picture file: " + imageFile.getAbsolutePath());
            return false;
        }

        //
        if (imageFile == localFile) {

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
        else {

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

    //////
    private float mContrast = DEFAULT_CONTRAST;
    private float mBrightness = DEFAULT_BRIGHTNESS;
    private float mRedBalance = DEFAULT_BALANCE;
    private float mGreenBalance = DEFAULT_BALANCE;
    private float mBlueBalance = DEFAULT_BALANCE;
    private boolean mLocalFrame = DEFAULT_LOCAL_FRAME;
    // Contrast, brightness & color balance data

    private boolean mChanged = false;
    private FloatingActionButton mCancelButton;

    private static final short CORRECTION_ID_CONTRAST = 1;
    private static final short CORRECTION_ID_BRIGHTNESS = 2;
    private static final short CORRECTION_ID_RED_BALANCE = 3;
    private static final short CORRECTION_ID_GREEN_BALANCE = 4;
    private static final short CORRECTION_ID_BLUE_BALANCE = 5;

    private short mCorrectionType = CORRECTION_ID_CONTRAST; // Correction in progress
    private SeekBar mSeekBar; // Seek bar to set correction
    private ImageView mCorrectionIcon; // Icon of the correction type

    private boolean displayCorrection(short type) {

        Logs.add(Logs.Type.V, "type: " + type);
        switch (type) {

            case CORRECTION_ID_CONTRAST:
                Logs.add(Logs.Type.I, "CORRECTION_ID_CONTRAST");

                mSeekBar.setMax(100);
                if (mContrast < DEFAULT_CONTRAST) // Contrast [0;1] -> Progress [0;50]
                    mSeekBar.setProgress((int)(50f * mContrast));
                else // Contrast [1;10] -> Progress [50;100]
                    mSeekBar.setProgress((int)(50 * (mContrast - DEFAULT_CONTRAST) / (float)9) + 50);

                mCorrectionIcon.setImageDrawable(getResources().getDrawable(R.drawable.contrast));
                mCorrectionType = type;
                return true;

            case CORRECTION_ID_BRIGHTNESS:
                Logs.add(Logs.Type.I, "CORRECTION_ID_BRIGHTNESS");

                mSeekBar.setMax(510); // == 2 * 255
                mSeekBar.setProgress((int) mBrightness + 255);

                mCorrectionIcon.setImageDrawable(getResources().getDrawable(R.drawable.brightness));
                mCorrectionType = type;
                return true;

            case CORRECTION_ID_RED_BALANCE:
                Logs.add(Logs.Type.I, "CORRECTION_ID_RED_BALANCE");

                mSeekBar.setMax(10);
                mSeekBar.setProgress((int) (mRedBalance * 10f) - 5);

                mCorrectionIcon.setImageDrawable(getResources().getDrawable(R.drawable.red_balance));
                mCorrectionType = type;
                return true;

            case CORRECTION_ID_GREEN_BALANCE:
                Logs.add(Logs.Type.I, "CORRECTION_ID_GREEN_BALANCE");

                mSeekBar.setMax(10);
                mSeekBar.setProgress((int) (mGreenBalance * 10f) - 5);

                mCorrectionIcon.setImageDrawable(getResources().getDrawable(R.drawable.green_balance));
                mCorrectionType = type;
                return true;

            case CORRECTION_ID_BLUE_BALANCE:
                Logs.add(Logs.Type.I, "CORRECTION_ID_BLUE_BALANCE");

                mSeekBar.setMax(10);
                mSeekBar.setProgress((int) (mBlueBalance * 10f) - 5);

                mCorrectionIcon.setImageDrawable(getResources().getDrawable(R.drawable.blue_balance));
                mCorrectionType = type;
                return true;
        }
        return false;
    }

    //
    private void onUpdateCorrection(SeekBar seekBar) {

        Logs.add(Logs.Type.V, "seekBar: " + seekBar);
        switch (mCorrectionType) {

            case CORRECTION_ID_CONTRAST:
                Logs.add(Logs.Type.I, "CORRECTION_ID_CONTRAST");
                if (seekBar.getProgress() < 50)
                    mContrast = seekBar.getProgress() / (float)50;
                else
                    mContrast = (9 * (seekBar.getProgress() - 50) / (float)50) + 1;

                // With: Progress in [0;50] -> Contrast [0;1]
                //       Progress in [50;100] -> Contrast [1;10]
                break;

            case CORRECTION_ID_BRIGHTNESS:
                Logs.add(Logs.Type.I, "CORRECTION_ID_BRIGHTNESS");
                mBrightness = seekBar.getProgress() - 255;
                break;

            case CORRECTION_ID_RED_BALANCE:
                Logs.add(Logs.Type.I, "CORRECTION_ID_RED_BALANCE");
                mRedBalance = (seekBar.getProgress() + 5) / 10f;
                break;

            case CORRECTION_ID_GREEN_BALANCE:
                Logs.add(Logs.Type.I, "CORRECTION_ID_GREEN_BALANCE");
                mGreenBalance = (seekBar.getProgress() + 5) / 10f;
                break;

            case CORRECTION_ID_BLUE_BALANCE:
                Logs.add(Logs.Type.I, "CORRECTION_ID_BLUE_BALANCE");
                mBlueBalance = (seekBar.getProgress() + 5) / 10f;
                break;
        }
        mCorrectionImage.setImageBitmap(applyCorrection(mCorrectionBitmap, mContrast, mBrightness,
                mRedBalance, mGreenBalance, mBlueBalance));
        if (!mChanged) {

            Logs.add(Logs.Type.I, "Correction changed");
            mChanged = true;
            mCancelButton.setImageDrawable(getResources().getDrawable(
                    R.drawable.ic_invert_colors_off_white_48dp));
        }
    }
    public void onValidateContrast(View sender) { // Validate contrast & brightness settings
        Logs.add(Logs.Type.V, null);

        Intent intent = new Intent();
        intent.putExtra(DATA_KEY_CONTRAST, mContrast);
        intent.putExtra(DATA_KEY_BRIGHTNESS, mBrightness);
        intent.putExtra(DATA_KEY_RED_BALANCE, mRedBalance);
        intent.putExtra(DATA_KEY_GREEN_BALANCE, mGreenBalance);
        intent.putExtra(DATA_KEY_BLUE_BALANCE, mBlueBalance);
        intent.putExtra(DATA_KEY_LOCAL, mLocalFrame);

        setResult(RESULT_OK, intent);
        finish();
    }
    private void onCancel() {
        Logs.add(Logs.Type.V, null);

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
        Logs.add(Logs.Type.V, "savedInstanceState: " + savedInstanceState);
        setContentView(R.layout.activity_correction);

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

        // Restore previous settings (if any)
        if (savedInstanceState != null) {

            mContrast = savedInstanceState.getFloat(DATA_KEY_CONTRAST);
            mBrightness = savedInstanceState.getFloat(DATA_KEY_BRIGHTNESS);
            mRedBalance = savedInstanceState.getFloat(DATA_KEY_RED_BALANCE);
            mGreenBalance = savedInstanceState.getFloat(DATA_KEY_GREEN_BALANCE);
            mBlueBalance = savedInstanceState.getFloat(DATA_KEY_BLUE_BALANCE);

            mChanged = savedInstanceState.getBoolean(DATA_KEY_CHANGED);
            mCorrectionType = savedInstanceState.getShort(DATA_KEY_CORRECTION);
        }

        // Set layout to display according orientation
        final ViewStub stubView = (ViewStub) findViewById(R.id.layout_container);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            Logs.add(Logs.Type.I, "Portrait");
            stubView.setLayoutResource(R.layout.correction_portrait);
        }
        else {
            Logs.add(Logs.Type.I, "Landscape");
            stubView.setLayoutResource(R.layout.correction_landscape);
        }

        final View rootView = stubView.inflate();
        mCorrectionImage = (ImageView)rootView.findViewById(R.id.image_contrast);
        mCorrectionIcon = (ImageView)rootView.findViewById(R.id.correction_icon);
        final ImageView compareImage = (ImageView)rootView.findViewById(R.id.image_compare);

        // Set seek bar position according correction type
        mSeekBar = (SeekBar)rootView.findViewById(R.id.seek_correction);
        mSeekBar.setOnSeekBarChangeListener(this);
        displayCorrection(mCorrectionType);

        // Configure the floating button that allows user to cancel settings (and that informs user on
        // which image the contrast & brightness configuration is applied)
        mCancelButton = (FloatingActionButton)findViewById(R.id.fab_cancel);
        mCancelButton.setImageDrawable(getResources().getDrawable((!mChanged) ?
                R.drawable.ic_invert_colors_white_48dp : R.drawable.ic_invert_colors_off_white_48dp));
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Logs.add(Logs.Type.V, null);
                mCancelButton.setImageDrawable(getResources().getDrawable(
                        R.drawable.ic_invert_colors_white_48dp));

                // Reset contrast & brightness settings
                mContrast = DEFAULT_CONTRAST;
                mBrightness = DEFAULT_BRIGHTNESS;
                mRedBalance = DEFAULT_BALANCE;
                mGreenBalance = DEFAULT_BALANCE;
                mBlueBalance = DEFAULT_BALANCE;
                mChanged = false;

                switch (mCorrectionType) {

                    case CORRECTION_ID_CONTRAST:
                        mSeekBar.setProgress(50);
                        break;

                    case CORRECTION_ID_BRIGHTNESS:
                        mSeekBar.setProgress(255);
                        break;

                    case CORRECTION_ID_RED_BALANCE:
                    case CORRECTION_ID_GREEN_BALANCE:
                    case CORRECTION_ID_BLUE_BALANCE:
                        mSeekBar.setProgress(5);
                        break;
                }
                mCorrectionImage.setImageBitmap(applyCorrection(mCorrectionBitmap, DEFAULT_CONTRAST,
                        DEFAULT_BRIGHTNESS, DEFAULT_BALANCE, DEFAULT_BALANCE, DEFAULT_BALANCE));
            }
        });
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
            ((CoordinatorLayout.LayoutParams)mCancelButton.getLayoutParams()).gravity =
                    Gravity.TOP|Gravity.CENTER_HORIZONTAL;
        else
            ((CoordinatorLayout.LayoutParams)mCancelButton.getLayoutParams()).gravity =
                    Gravity.START|Gravity.CENTER_VERTICAL;

        // Set validate button position (not exactly in center when device in portrait)
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {

            LayoutParams params = (LayoutParams)mCorrectionIcon.getLayoutParams();
            final FloatingActionButton applyButton = (FloatingActionButton)findViewById(R.id.fab_apply);
            applyButton.setTranslationY(-(params.height >> 1));
        }

        ////// Load images
        if (!loadImagesFromFiles(compareImage)) {

            // Inform user
            DisplayMessage.getInstance().toast(R.string.error_correction_failed, Toast.LENGTH_LONG);

            finish();
            return;
        }

        // Apply contrast & brightness settings
        mCorrectionImage.setImageBitmap(applyCorrection(mCorrectionBitmap, mContrast, mBrightness,
                mRedBalance, mGreenBalance, mBlueBalance));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        Logs.add(Logs.Type.V, "menu: " + menu);
        getMenuInflater().inflate(R.menu.activity_correction, menu);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putFloat(DATA_KEY_CONTRAST, mContrast);
        outState.putFloat(DATA_KEY_BRIGHTNESS, mBrightness);
        outState.putFloat(DATA_KEY_RED_BALANCE, mRedBalance);
        outState.putFloat(DATA_KEY_GREEN_BALANCE, mGreenBalance);
        outState.putFloat(DATA_KEY_BLUE_BALANCE, mBlueBalance);

        outState.putBoolean(DATA_KEY_CHANGED, mChanged);
        outState.putShort(DATA_KEY_CORRECTION, mCorrectionType);

        Logs.add(Logs.Type.V, "outState: " + outState);
        super.onSaveInstanceState(outState);
    }

    @Override public void onBackPressed() { onCancel(); }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Logs.add(Logs.Type.V, "item: " + item);

        short correction = 0;
        switch (item.getItemId()) {

            case android.R.id.home:
                onCancel();
                return true;

            // Change correction type
            case R.id.menu_contrast:
                correction = CORRECTION_ID_CONTRAST;
                break;

            case R.id.menu_brightness:
                correction = CORRECTION_ID_BRIGHTNESS;
                break;

            case R.id.menu_red_balance:
                correction = CORRECTION_ID_RED_BALANCE;
                break;

            case R.id.menu_green_balance:
                correction = CORRECTION_ID_GREEN_BALANCE;
                break;

            case R.id.menu_blue_balance:
                correction = CORRECTION_ID_BLUE_BALANCE;
                break;
        }
        if (displayCorrection(correction))
            return true;

        return super.onOptionsItemSelected(item);
    }

    //////
    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser)
            onUpdateCorrection(seekBar);
    }
    @Override public void onStartTrackingTouch(SeekBar seekBar) { }
    @Override public void onStopTrackingTouch(SeekBar seekBar) { }
}
