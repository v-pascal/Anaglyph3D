package com.studio.artaban.anaglyph3d.process.configure;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
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

public class ShiftActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    // Data keys
    public static final String DATA_KEY_SHIFT = "shift";
    public static final String DATA_KEY_GUSHING = "gushing";
    private static final String DATA_KEY_CHANGED = "changed";

    // Default configuration
    public static final float DEFAULT_SHIFT = 0f;
    public static final float DEFAULT_GUSHING = 0f;

    //
    private static final float MIN_SHIFT = 1 / 90f; // Minimum shift between left & right images (image width proportional)
    private static final float MAX_SHIFT_RATIO = 1 / 20f; // Maximum shift between left & right images (image width proportional)

    private static final short MIN_GUSHING = 5; // Minimum gushing to seek (in seek bar progression)
    private static final float MAX_GUSHING_RATIO = 1.1f; // Maximum gushing ratio (image scale)

    public static Bitmap applySimulation(Bitmap curBitmap, float shift, float gushing) {
        // NB: Need large heap memory coz three bitmaps are opened simultaneously to make simulated 3D frame

        // With:
        // _ shift in [-MAX_SHIFT_RATIO;MAX_SHIFT_RATIO] with default 0 (minimum shift to apply)
        // _ gushing in [-MAX_GUSHING_RATIO;-1] and [1;MAX_GUSHING_RATIO] with default 0 (nothing to apply)

        int offsetX = 0, offsetY = 0;

        Bitmap gushingBitmap = null;
        boolean noGushing = (gushing < 1f) && (gushing > -1f);
        boolean gushingBlue = gushing < 0.5f; // Negative setting

        if (!noGushing) {

            if (gushingBlue) // To apply on blue frame
                gushing *= -1f;

            gushingBitmap = Bitmap.createScaledBitmap(curBitmap,
                    (int) (curBitmap.getWidth() * gushing),
                    (int) (curBitmap.getHeight() * gushing), false);

            offsetX = (int)((curBitmap.getWidth() * gushing) - curBitmap.getWidth()) >> 1;
            offsetY = (int)((curBitmap.getHeight() * gushing) - curBitmap.getHeight()) >> 1;
        }

        //
        int minShift = (int)(MIN_SHIFT * curBitmap.getWidth());
        int pixelShift = (int)(shift * curBitmap.getWidth());
        if (((pixelShift < 0) && (pixelShift > -minShift)) || ((pixelShift >= 0) && (pixelShift < minShift)))
            pixelShift = minShift;
            // Needed to avoid to display left & right images in perfect overlay (without 3D)

        boolean shiftBlue = false;
        if (pixelShift < 0) { // To apply on blue frame

            pixelShift = -pixelShift;
            offsetX += pixelShift;
            shiftBlue = true;
        }
        int width = curBitmap.getWidth() - pixelShift;
        int height = curBitmap.getHeight();

        Bitmap redBitmap;
        Bitmap bitmap3D; // Blue bitmap

        if (!noGushing) {
            if (!gushingBlue) {

                redBitmap = Bitmap.createBitmap(gushingBitmap, (!shiftBlue)?
                        (offsetX + pixelShift):(offsetX - pixelShift), offsetY, width, height);
                bitmap3D = Bitmap.createBitmap(curBitmap, (shiftBlue)? pixelShift:0, 0, width, height);
            }
            else {
                redBitmap = Bitmap.createBitmap(curBitmap, (!shiftBlue)? pixelShift:0, 0, width, height);
                bitmap3D = Bitmap.createBitmap(gushingBitmap, (shiftBlue)?
                        (offsetX - pixelShift):offsetX, offsetY, width, height);
            }
        }
        else {
            redBitmap = Bitmap.createBitmap(curBitmap, (!shiftBlue)? pixelShift:0, 0, width, height);
            bitmap3D = Bitmap.createBitmap(curBitmap, (shiftBlue)? pixelShift:0, 0, width, height);
        }

        //
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x)
                bitmap3D.setPixel(x, y, (redBitmap.getPixel(x, y) & 0xffff0000) |
                        (bitmap3D.getPixel(x, y) & 0xff00ffff));
        }
        return bitmap3D;
    }

    //////
    private ImageView mImage;
    private FloatingActionButton mCancelButton;
    private Bitmap mBitmap;

    private boolean loadFrame() { // Load frame file to define 3D simulation

        File imageFile = new File(ActivityWrapper.DOCUMENTS_FOLDER + Storage.FILENAME_LOCAL_PICTURE);
        Bundle data = getIntent().getBundleExtra(Constants.DATA_ACTIVITY);
        int width = data.getInt(Frame.DATA_KEY_WIDTH);
        int height = data.getInt(Frame.DATA_KEY_HEIGHT);

        byte[] imageBuffer = new byte[(int)imageFile.length()];
        try {
            if (new FileInputStream(imageFile).read(imageBuffer) != imageBuffer.length)
                throw new IOException();

            if (!Settings.getInstance().mOrientation) // Landscape
                mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            else // Portrait
                mBitmap = Bitmap.createBitmap(height, width, Bitmap.Config.ARGB_8888);
            mBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(imageBuffer));
        }
        catch (IOException e) {

            Logs.add(Logs.Type.F, "Failed to load picture file: " + imageFile.getAbsolutePath());
            return false;
        }
        return true;
    }

    //
    private float mShift = DEFAULT_SHIFT; // Shift configured
    private float mGushing = DEFAULT_GUSHING; // Gushing configuration
    private boolean mChanged = false; // User configuration flag

    private void onUpdateSettings(SeekBar seekBar) {
        switch (seekBar.getId()) {

            case R.id.seek_shift:
                mShift = ((seekBar.getProgress() - 50) * MAX_SHIFT_RATIO) / 50f;
                break;

            case R.id.seek_gushing:
                if (((seekBar.getProgress() >= 50) && ((seekBar.getProgress() - 50) < MIN_GUSHING)) ||
                        ((seekBar.getProgress() < 50) && ((50 - seekBar.getProgress()) < MIN_GUSHING))) {

                    mGushing = 0f;
                    break;
                }
                // NB: Needed to set default gushing value (no gushing to apply)

                if (seekBar.getProgress() > 50)
                    mGushing = ((seekBar.getProgress() - 50f) * (MAX_GUSHING_RATIO - 1f) + 50f) / 50f;
                else
                    mGushing = ((seekBar.getProgress() * (MAX_GUSHING_RATIO - 1f)) -
                            (49f * MAX_GUSHING_RATIO)) / 49f;
                break;
        }
        mImage.setImageBitmap(applySimulation(mBitmap, mShift, mGushing));

        if (!mChanged) {

            mChanged = true;
            mCancelButton.setImageDrawable(getResources().getDrawable(
                    R.drawable.ic_invert_colors_off_white_48dp));
        }
    }
    public void onValidateContrast(View sender) { // Validate contrast & brightness settings

        Intent intent = new Intent();
        intent.putExtra(DATA_KEY_SHIFT, mShift);
        intent.putExtra(DATA_KEY_GUSHING, mGushing);

        setResult(RESULT_OK, intent);
        finish();
    }
    private void onCancel() {

        // Ask user to confirm shift & gushing step skip
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
        setContentView(R.layout.activity_shift);

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

            mShift = savedInstanceState.getFloat(DATA_KEY_SHIFT);
            mGushing = savedInstanceState.getFloat(DATA_KEY_GUSHING);
            mChanged = savedInstanceState.getBoolean(DATA_KEY_CHANGED);
        }

        // Set up seek bars
        final SeekBar shift = (SeekBar)findViewById(R.id.seek_shift);
        assert shift != null;
        shift.setOnSeekBarChangeListener(this);
        shift.setMax(100);
        shift.setProgress(50);
        final SeekBar gushing = (SeekBar)findViewById(R.id.seek_gushing);
        assert gushing != null;
        gushing.setOnSeekBarChangeListener(this);
        gushing.setMax(100);
        gushing.setProgress(50);

        // Configure the floating button that allows user to cancel settings
        mCancelButton = (FloatingActionButton)findViewById(R.id.fab_cancel);
        mCancelButton.setImageDrawable(getResources().getDrawable((!mChanged) ?
                R.drawable.ic_invert_colors_white_48dp : R.drawable.ic_invert_colors_off_white_48dp));
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mCancelButton.setImageDrawable(getResources().getDrawable(
                        R.drawable.ic_invert_colors_white_48dp));

                // Reset shift & gushing settings
                mShift = DEFAULT_SHIFT;
                mGushing = DEFAULT_GUSHING;
                mChanged = false;

                shift.setProgress(50);
                gushing.setProgress(50);

                mImage.setImageBitmap(applySimulation(mBitmap, DEFAULT_SHIFT, DEFAULT_GUSHING));
            }
        });

        // Load & Display frame image
        mImage = (ImageView)findViewById(R.id.image_final);

        if (!loadFrame()) {

            // Inform user
            DisplayMessage.getInstance().toast(R.string.error_shift_failed, Toast.LENGTH_LONG);

            finish();
            return;
        }

        // Apply shift & gushing settings
        mImage.setImageBitmap(applySimulation(mBitmap, mShift, mGushing));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putFloat(DATA_KEY_SHIFT, mShift);
        outState.putFloat(DATA_KEY_GUSHING, mGushing);
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
    @Override public void onStartTrackingTouch(SeekBar seekBar) { }
    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { }
    @Override public void onStopTrackingTouch(SeekBar seekBar) { onUpdateSettings(seekBar); }
}
