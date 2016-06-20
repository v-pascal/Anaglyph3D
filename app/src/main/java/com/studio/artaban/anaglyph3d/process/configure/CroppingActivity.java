package com.studio.artaban.anaglyph3d.process.configure;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.helpers.Storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class CroppingActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    public static final String DATA_KEY_ZOOM = "zoom";
    public static final String DATA_KEY_X = "x";
    public static final String DATA_KEY_Y = "y";
    public static final String DATA_KEY_LOCAL = "local";

    private static final String DATA_KEY_TRANS_X = "transX";
    private static final String DATA_KEY_TRANS_Y = "transY";

    private static final String DATA_KEY_CHANGED = "changed";
    // Data keys

    public static final float DEFAULT_ZOOM = 1f;
    public static final int DEFAULT_X = 0;
    public static final int DEFAULT_Y = 0;
    public static final boolean DEFAULT_LOCAL = true;
    // Default values

    private static final float MAX_ZOOM = 3f / 4f;

    //
    private static Bitmap openBitmapFile(boolean local) { // Return bitmap from RGBA file

        Logs.add(Logs.Type.V, "local: " + local);
        File bmpFile = new File(ActivityWrapper.DOCUMENTS_FOLDER + File.separator + ((local)?
                        Constants.PROCESS_LOCAL_PREFIX:Constants.PROCESS_REMOTE_PREFIX) + "0000" +
                        Constants.EXTENSION_RGBA);

        Bitmap bitmap = null;
        byte[] bmpBuffer = new byte[(int)bmpFile.length()];
        try {
            if (new FileInputStream(bmpFile).read(bmpBuffer) != bmpBuffer.length)
                throw new IOException();

            if (Settings.getInstance().mOrientation) // Portrait
                bitmap = Bitmap.createBitmap(Settings.getInstance().mResolution.height,
                        Settings.getInstance().mResolution.width, Bitmap.Config.ARGB_8888);
            else // Landscape
                bitmap = Bitmap.createBitmap(Settings.getInstance().mResolution.width,
                        Settings.getInstance().mResolution.height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(bmpBuffer));
        }
        catch (IOException e) {
            Logs.add(Logs.Type.E, "Failed to load RGBA file: " + bmpFile.getAbsolutePath());
        }
        return bitmap;
    }

    //////
    private float mZoom = DEFAULT_ZOOM; // Zoom configured
    private int mOriginX = DEFAULT_X; // X origin coordinate from which the zoom starts
    private int mOriginY = DEFAULT_Y; // Y origin coordinate from which the zoom starts
    private boolean mLocal = DEFAULT_LOCAL; // Local frame flag on which to apply the zoom

    private boolean mChanged;

    // Manage zoom location
    private int mTransX = 0, mTransY = 0;
    private int mPointerId = Constants.NO_DATA;
    private float mPrevX = 0f, mPrevY = 0f;

    //
    private void onUpdateCropping(SeekBar seekBar) {

        Logs.add(Logs.Type.V, "seekBar: " + seekBar);
        mZoom = ((seekBar.getProgress() * (MAX_ZOOM - 1)) / 100f) + 1f;








        int originX = (int)(mFrameWidth - (mZoom * mFrameWidth)) >> 1;
        int originY = (int)(mFrameHeight - (mZoom * mFrameHeight)) >> 1;

        mOriginX = ((originX + mTransX) < 0)? 0: originX + mTransX;
        mOriginY = ((originY + mTransY) < 0)? 0: originY + mTransY;

        displayZoom();











        if (!mChanged) {

            Logs.add(Logs.Type.I, "Cropping changed");
            mChanged = true;
            mCancelButton.setImageDrawable(getResources().getDrawable(
                    R.drawable.ic_invert_colors_off_white_48dp));
        }
    }
    public void onValidateCropping(View sender) { // Validate cropping settings
        Logs.add(Logs.Type.V, null);

        Intent intent = new Intent();
        intent.putExtra(DATA_KEY_ZOOM, mZoom);
        intent.putExtra(DATA_KEY_X, mOriginX);
        intent.putExtra(DATA_KEY_Y, mOriginY);
        intent.putExtra(DATA_KEY_LOCAL, mLocal);

        setResult(RESULT_OK, intent);
        finish();
    }
    private void onCancel() {
        Logs.add(Logs.Type.V, null);

        // Ask user to skip cropping step
        DisplayMessage.getInstance().alert(R.string.title_warning, R.string.ask_skip_step, null,
                true, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE)
                            finish();
                    }
                });
    }

    //
    private FloatingActionButton mCancelButton;

    private float mFrameWidth; // Frame width
    private float mFrameHeight; // Frame height
    private int mImageWidth; // Frame image width in the screen
    private int mImageHeight; // Frame image height in the screen

    private void resetChange(SeekBar seek) { // Cancel any changes (set to default values)

        Logs.add(Logs.Type.V, "seek: " + seek);
        mCancelButton.setImageDrawable(getResources().getDrawable(
                R.drawable.ic_invert_colors_white_48dp));

        // Reset cropping settings
        mZoom = DEFAULT_ZOOM;
        mOriginX = DEFAULT_X;
        mOriginY = DEFAULT_Y;
        mLocal = DEFAULT_LOCAL;

        mChanged = false;
        seek.setProgress(0);
        displayZoom();
    }
    private void getFrameImageSize(int parentHeight) {
        // Get frame image view size according frame and screen resolution and orientation

        Logs.add(Logs.Type.V, "parentHeight: " + parentHeight);
        final Point screenSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(screenSize);

        parentHeight = screenSize.y - parentHeight;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            // Portrait

            parentHeight >>= 1; // There are two images defined in the screen height
            mImageWidth = screenSize.x;
        }
        else // Landscape
            mImageWidth = screenSize.x >> 1;

        mImageHeight = (int)(mImageWidth * mFrameHeight / mFrameWidth);
        if (mImageHeight > parentHeight) {

            mImageHeight = parentHeight;
            mImageWidth = (int)(mFrameWidth * parentHeight / mFrameHeight);
        }
    }
    private void displayZoom() { // Position images that define zoom configuration

        Logs.add(Logs.Type.V, null);



        /*
        final ImageView leftTop = (ImageView)findViewById((mLocal)? R.id.left_LT:R.id.right_LT);
        final ImageView rightTop = (ImageView)findViewById((mLocal)? R.id.left_RT:R.id.right_RT);
        final ImageView rightBottom = (ImageView)findViewById((mLocal)? R.id.left_RB:R.id.right_RB);
        final ImageView leftBottom = (ImageView)findViewById((mLocal)? R.id.left_LB:R.id.right_LB);
        */
        final ImageView leftTop = (ImageView)findViewById(R.id.left_LT);
        final ImageView rightTop = (ImageView)findViewById(R.id.left_RT);
        final ImageView rightBottom = (ImageView)findViewById(R.id.left_RB);
        final ImageView leftBottom = (ImageView)findViewById(R.id.left_LB);



        assert leftTop != null;
        assert rightTop != null;
        assert rightBottom != null;
        assert leftBottom != null;




        int originX = (int)(mOriginX * mImageWidth / mFrameWidth);
        int originY = (int)(mOriginY * mImageHeight / mFrameHeight);

        leftTop.setTranslationX(originX);
        leftTop.setTranslationY(originY);

        float transX = -(mFrameWidth - (mZoom * mFrameWidth) - originX);
        rightTop.setTranslationX(transX);
        rightTop.setTranslationY(originY);

        float transY = -(mFrameHeight - (mZoom * mFrameHeight) - originY);
        rightBottom.setTranslationX(transX);
        rightBottom.setTranslationY(transY);

        leftBottom.setTranslationX(originX);
        leftBottom.setTranslationY(transY);







    }

    //////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logs.add(Logs.Type.V, "savedInstanceState: " + savedInstanceState);
        setContentView(R.layout.activity_cropping);

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

            mZoom = savedInstanceState.getFloat(DATA_KEY_ZOOM);
            mOriginX = savedInstanceState.getInt(DATA_KEY_X);
            mOriginY = savedInstanceState.getInt(DATA_KEY_Y);
            mLocal = savedInstanceState.getBoolean(DATA_KEY_LOCAL);

            mTransX = savedInstanceState.getInt(DATA_KEY_TRANS_X);
            mTransY = savedInstanceState.getInt(DATA_KEY_TRANS_Y);

            mChanged = savedInstanceState.getBoolean(DATA_KEY_CHANGED);
        }

















        File documents = getExternalFilesDir(null);
        if (documents != null) {

            ActivityWrapper.DOCUMENTS_FOLDER = documents.getAbsolutePath();

            // Create folders
            if (!Storage.createFolder(ActivityWrapper.DOCUMENTS_FOLDER + Storage.FOLDER_DOWNLOAD))
                Logs.add(Logs.Type.E, "Failed to create 'Downloads' folder");
            if (!Storage.createFolder(ActivityWrapper.DOCUMENTS_FOLDER + Storage.FOLDER_THUMBNAILS))
                Logs.add(Logs.Type.E, "Failed to create 'Thumbnails' folder");
            if (!Storage.createFolder(ActivityWrapper.DOCUMENTS_FOLDER + Storage.FOLDER_VIDEOS))
                Logs.add(Logs.Type.E, "Failed to create 'Videos' folder");
        }
        else {

            Logs.add(Logs.Type.F, "Failed to get documents folder");
            DisplayMessage.getInstance().toast(R.string.no_storage, Toast.LENGTH_LONG);
            finish();
            return;
        }
        TypedValue typedValue = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true))
            ActivityWrapper.ACTION_BAR_HEIGHT = TypedValue.complexToDimensionPixelSize(typedValue.data,
                    getResources().getDisplayMetrics());
        else {

            ActivityWrapper.ACTION_BAR_HEIGHT = 0;
            Logs.add(Logs.Type.W, "'android.R.attr.actionBarSize' attribute not found");
        }
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0)
            ActivityWrapper.STATUS_BAR_HEIGHT = getResources().getDimensionPixelSize(resourceId);

        else {

            ActivityWrapper.STATUS_BAR_HEIGHT = 0;
            Logs.add(Logs.Type.W, "'status_bar_height' dimension not found");
        }
        Settings.getInstance().initResolutions();




















        // Get frame resolution
        if (Settings.getInstance().mOrientation) { // Portrait

            mFrameWidth = Settings.getInstance().mResolution.height;
            mFrameHeight = Settings.getInstance().mResolution.width;
        }
        else { // Landscape

            mFrameWidth = Settings.getInstance().mResolution.width;
            mFrameHeight = Settings.getInstance().mResolution.height;
        }

        //
        final SeekBar seekBar = (SeekBar)findViewById(R.id.seek_zoom);
        assert seekBar != null;
        seekBar.setMax(100);
        seekBar.setOnSeekBarChangeListener(this);

        // Configure the floating button that allows user to cancel settings
        mCancelButton = (FloatingActionButton)findViewById(R.id.fab_cancel);
        mCancelButton.setImageDrawable(getResources().getDrawable((!mChanged) ?
                R.drawable.ic_invert_colors_white_48dp : R.drawable.ic_invert_colors_off_white_48dp));
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Logs.add(Logs.Type.V, null);
                resetChange(seekBar);
            }
        });

        // Set validate button position (not exactly in center when device in portrait)
        final ImageView zoomIcon = (ImageView)findViewById(R.id.zoom_icon);
        assert zoomIcon != null;
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)zoomIcon.getLayoutParams();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {

            final FloatingActionButton applyButton = (FloatingActionButton)findViewById(R.id.fab_apply);
            assert applyButton != null;
            applyButton.setTranslationY(-(params.height >> 1));
        }











        // Load frame images
        final ImageView localFrame = (ImageView)findViewById(R.id.frame_left);
        final ImageView remoteFrame = (ImageView)findViewById(R.id.frame_right);

        assert localFrame != null;
        assert remoteFrame != null;



        //localFrame.getTouchables().



        localFrame.setImageBitmap(openBitmapFile(true));
        localFrame.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                Logs.add(Logs.Type.V, "event: " + event);







                /*
                mTransX = -mOriginX;
                mTransY = -mOriginY;
                mOriginY = 0;
                mOriginX = 0;
                displayZoom();
                */




                switch (event.getActionMasked()) {

                    case MotionEvent.ACTION_DOWN: {
                        if (mPointerId == Constants.NO_DATA) {

                            mPointerId = event.getPointerId(0);
                            return true;
                        }
                        break;
                    }
                    case MotionEvent.ACTION_UP:

                        // Always stop when any action up is found
                        mPointerId = Constants.NO_DATA;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (mPointerId != Constants.NO_DATA) {
                            for (int i = 0; i < event.getPointerCount(); ++i) {
                                if (event.getPointerId(i) == mPointerId) {







                                    event.getX(i);
                                    event.getY(i);







                                    break;
                                }
                            }
                        }
                        return true;
                }
                return false;
            }
        });
        remoteFrame.setImageBitmap(openBitmapFile(false));
        remoteFrame.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {












                return false;
            }
        });

        getFrameImageSize(params.height + ActivityWrapper.ACTION_BAR_HEIGHT +
                ActivityWrapper.STATUS_BAR_HEIGHT);
        ((RelativeLayout.LayoutParams)localFrame.getLayoutParams()).height = mImageHeight;
        ((RelativeLayout.LayoutParams)remoteFrame.getLayoutParams()).height = mImageHeight;
        ((RelativeLayout.LayoutParams)localFrame.getLayoutParams()).width = mImageWidth;
        ((RelativeLayout.LayoutParams)remoteFrame.getLayoutParams()).width = mImageWidth;





        displayZoom();






    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putFloat(DATA_KEY_ZOOM, mZoom);
        outState.putInt(DATA_KEY_X, mOriginX);
        outState.putInt(DATA_KEY_Y, mOriginY);
        outState.putBoolean(DATA_KEY_LOCAL, mLocal);

        outState.putInt(DATA_KEY_TRANS_X, mTransX);
        outState.putInt(DATA_KEY_TRANS_Y, mTransY);

        outState.putBoolean(DATA_KEY_CHANGED, mChanged);

        Logs.add(Logs.Type.V, "outState: " + outState);
        super.onSaveInstanceState(outState);
    }

    @Override public void onBackPressed() { onCancel(); }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Logs.add(Logs.Type.V, "item: " + item);
        if (item.getItemId() == android.R.id.home) {

            onCancel();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //////
    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser)
            onUpdateCropping(seekBar);
    }
    @Override public void onStartTrackingTouch(SeekBar seekBar) { }
    @Override public void onStopTrackingTouch(SeekBar seekBar) { }
}
