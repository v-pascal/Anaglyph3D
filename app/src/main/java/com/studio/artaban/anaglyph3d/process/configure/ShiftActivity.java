package com.studio.artaban.anaglyph3d.process.configure;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.helpers.Storage;
import com.studio.artaban.anaglyph3d.media.Frame;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ShiftActivity extends AppCompatActivity {

    // Data keys
    public static final String DATA_KEY_SHIFT = "shift";
    public static final String DATA_KEY_GUSHING = "gushing";
    private static final String DATA_KEY_CHANGED = "changed";

    // Default configuration
    public static final float DEFAULT_SHIFT = 0f;
    public static final float DEFAULT_GUSHING = 0f;

    public static Bitmap applyCorrection(Bitmap curBitmap, boolean left, float shift, float gushing) {

        float red = 1f, green = 1f, blue = 1f;
        if (left)
            green = blue = 0f;
        else
            red = 0f;

        ColorMatrix matrix = new ColorMatrix(new float[] {

                red, 0, 0, 0, 0,
                0, green, 0, 0, 0,
                0, 0, blue, 0, 0,
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

    //
    private ImageView mLeftImage, mRightImage;
    private Bitmap mBitmap;

    private float mShift = DEFAULT_SHIFT; // Shift configured
    private float mGushing = DEFAULT_GUSHING; // Gushing configuration
    private boolean mChanged = false; // User configuration flag

    private boolean loadFrame() { // Load frame file to define 3D simulation

        File imageFile = new File(ActivityWrapper.DOCUMENTS_FOLDER + Storage.FILENAME_LOCAL_PICTURE);
        int width = getIntent().getIntExtra(Frame.DATA_KEY_WIDTH, 0);
        int height = getIntent().getIntExtra(Frame.DATA_KEY_HEIGHT, 0);

        byte[] imageBuffer = new byte[(int)imageFile.length()];
        try {
            if (new FileInputStream(imageFile).read(imageBuffer) != imageBuffer.length)
                throw new IOException();

            mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(imageBuffer));

            //
            mLeftImage.setImageBitmap(mBitmap);
            mRightImage.setImageBitmap(mBitmap);
        }
        catch (IOException e) {

            Logs.add(Logs.Type.F, "Failed to load picture file: " + imageFile.getAbsolutePath());
            return false;
        }
        return true;
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
















        getIntent().putExtra(Frame.DATA_KEY_WIDTH, 640);
        getIntent().putExtra(Frame.DATA_KEY_HEIGHT, 480);
        File documents = getExternalFilesDir(null);
        if (documents != null)
            ActivityWrapper.DOCUMENTS_FOLDER = documents.getAbsolutePath();
        else
            Logs.add(Logs.Type.F, "Failed to get documents folder");


















        // Load & Display frame image
        mLeftImage = (ImageView)findViewById(R.id.image_left);
        mRightImage = (ImageView)findViewById(R.id.image_right);

        if (!loadFrame()) {

            // Inform user
            DisplayMessage.getInstance().toast(R.string.error_shift_failed, Toast.LENGTH_LONG);

            finish();
            return;
        }

        // Apply shift & gushing settings
        mLeftImage.setImageBitmap(applyCorrection(mBitmap, true, mShift, mGushing));
        mRightImage.setImageBitmap(applyCorrection(mBitmap, false, mShift, mGushing));
    }

    //
    private void onUpdateSettings(SeekBar seekBar) {
        /*
        switch (mCorrectionType) {

            case CORRECTION_ID_CONTRAST:
                if (seekBar.getProgress() < 50)
                    mContrast = seekBar.getProgress() / (float)50;
                else
                    mContrast = (9 * (seekBar.getProgress() - 50) / (float)50) + 1;

                // With: Progress in [0;50] -> Contrast [0;1]
                //       Progress in [50;100] -> Contrast [1;10]
                break;

            case CORRECTION_ID_BRIGHTNESS:
                mBrightness = seekBar.getProgress() - 255;
                break;

            case CORRECTION_ID_RED_BALANCE:
                mRedBalance = (seekBar.getProgress() + 5) / 10f;
                break;

            case CORRECTION_ID_GREEN_BALANCE:
                mGreenBalance = (seekBar.getProgress() + 5) / 10f;
                break;

            case CORRECTION_ID_BLUE_BALANCE:
                mBlueBalance = (seekBar.getProgress() + 5) / 10f;
                break;
        }
        mCorrectionImage.setImageBitmap(applyCorrection(mCorrectionBitmap, mContrast, mBrightness,
                mRedBalance, mGreenBalance, mBlueBalance));
        if (!mChanged) {

            mChanged = true;
            mCancelButton.setImageDrawable(getResources().getDrawable(
                    R.drawable.ic_invert_colors_off_white_48dp));
        }
        */
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putFloat(DATA_KEY_SHIFT, mShift);
        outState.putFloat(DATA_KEY_GUSHING, mGushing);
        outState.putBoolean(DATA_KEY_CHANGED, mChanged);
        super.onSaveInstanceState(outState);
    }
}
