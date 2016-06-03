package com.studio.artaban.anaglyph3d.process.configure;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import com.studio.artaban.anaglyph3d.R;

public class ShiftActivity extends AppCompatActivity {

    public static final String DATA_KEY_SHIFT = "shift";
    public static final String DATA_KEY_GUSHING = "gushing";
    private static final String DATA_KEY_CHANGED = "changed";

    //
    private ImageView mLeftImage, mRightImage;
    private Bitmap mLeftBitmap, mRightBitmap;

    private float mShift; // Shift configured
    private float mGushing; // Gushing configuration
    private boolean mChanged; // User configuration flag

    //////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shift);

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
        mLeftImage = (ImageView)findViewById(R.id.image_left);
        mRightImage = (ImageView)findViewById(R.id.image_right);






        /*
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
        */






    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putFloat(DATA_KEY_SHIFT, mShift);
        outState.putFloat(DATA_KEY_GUSHING, mGushing);
        outState.putBoolean(DATA_KEY_CHANGED, mChanged);
        super.onSaveInstanceState(outState);
    }
}
