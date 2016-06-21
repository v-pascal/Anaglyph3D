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
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

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
    public static final String DATA_KEY_ORIGIN_X = "x";
    public static final String DATA_KEY_ORIGIN_Y = "y";
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

    //////
    public static Bitmap applyCropping(Bitmap curBitmap, float zoom, int originX, int originY) {
        // Apply cropping configuration on current bitmap
        // NB: Can need a large heap memory

        Logs.add(Logs.Type.V, "curBitmap: " + curBitmap + ", zoom: " + zoom + ", originX: " +
                originX + ", originY: " + originY);
        Bitmap cropBitmap = Bitmap.createBitmap(curBitmap, originX, originY,
                (int) (curBitmap.getWidth() * zoom), (int) (curBitmap.getHeight() * zoom));

        int width = curBitmap.getWidth();
        int height = curBitmap.getHeight();
        curBitmap.recycle();
        // Try to avoid out of memory error

        return Bitmap.createScaledBitmap(cropBitmap, width, height, false);
    }
    private static Bitmap openBitmapFile(boolean local) { // Return bitmap from RGBA file

        Logs.add(Logs.Type.V, "local: " + local);
        File bmpFile = new File(Storage.DOCUMENTS_FOLDER + File.separator + ((local)?
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

        updateOrigin(0, 0);
        updateZoom(false);

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
        intent.putExtra(DATA_KEY_ORIGIN_X, mOriginX);
        intent.putExtra(DATA_KEY_ORIGIN_Y, mOriginY);
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
        mCancelButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_invert_colors_white_48dp));

        // Reset cropping settings
        mZoom = DEFAULT_ZOOM;
        mOriginX = DEFAULT_X;
        mOriginY = DEFAULT_Y;
        mLocal = DEFAULT_LOCAL;

        mChanged = false;

        //
        mTransX = mTransY = 0;
        mPointerId = Constants.NO_DATA;
        mPrevX = mPrevY = 0f;

        seek.setProgress(0);
        updateZoom(true);
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
        Logs.add(Logs.Type.I, "mImageWidth: " + mImageWidth + ", mImageHeight: " + mImageHeight);
    }
    private void updateZoom(boolean display) {
        // Position images that show the zoom according current configuration

        //Logs.add(Logs.Type.V, null);
        final ImageView leftTop = (ImageView)findViewById((mLocal)? R.id.left_LT:R.id.right_LT);
        final ImageView rightTop = (ImageView)findViewById((mLocal)? R.id.left_RT:R.id.right_RT);
        final ImageView rightBottom = (ImageView)findViewById((mLocal)? R.id.left_RB:R.id.right_RB);
        final ImageView leftBottom = (ImageView)findViewById((mLocal)? R.id.left_LB:R.id.right_LB);

        assert leftTop != null;
        assert rightTop != null;
        assert rightBottom != null;
        assert leftBottom != null;

        int originX = (int)(mOriginX * mImageWidth / mFrameWidth);
        int originY = (int)(mOriginY * mImageHeight / mFrameHeight);

        leftTop.setTranslationX(originX);
        leftTop.setTranslationY(originY);

        float transX = -(mImageWidth - (mZoom * mImageWidth) - originX);
        rightTop.setTranslationX(transX);
        rightTop.setTranslationY(originY);

        float transY = -(mImageHeight - (mZoom * mImageHeight) - originY);
        rightBottom.setTranslationX(transX);
        rightBottom.setTranslationY(transY);

        leftBottom.setTranslationX(originX);
        leftBottom.setTranslationY(transY);

        //
        if (display)
            displayZoom();
    }
    private void displayZoom() {
        // Show or hide images according the frame on which to apply the zoom

        Logs.add(Logs.Type.V, null);
        if (mLocal) {

            ImageView corner = (ImageView)findViewById(R.id.right_LT);
            assert corner != null;
            corner.setVisibility(View.GONE);
            corner = (ImageView)findViewById(R.id.right_RT);
            assert corner != null;
            corner.setVisibility(View.GONE);
            corner = (ImageView)findViewById(R.id.right_RB);
            assert corner != null;
            corner.setVisibility(View.GONE);
            corner = (ImageView)findViewById(R.id.right_LB);
            assert corner != null;
            corner.setVisibility(View.GONE);

            //
            corner = (ImageView)findViewById(R.id.left_LT);
            assert corner != null;
            corner.setVisibility(View.VISIBLE);
            corner = (ImageView)findViewById(R.id.left_RT);
            assert corner != null;
            corner.setVisibility(View.VISIBLE);
            corner = (ImageView)findViewById(R.id.left_RB);
            assert corner != null;
            corner.setVisibility(View.VISIBLE);
            corner = (ImageView)findViewById(R.id.left_LB);
            assert corner != null;
            corner.setVisibility(View.VISIBLE);
        }
        else {

            ImageView corner = (ImageView)findViewById(R.id.left_LT);
            assert corner != null;
            corner.setVisibility(View.GONE);
            corner = (ImageView)findViewById(R.id.left_RT);
            assert corner != null;
            corner.setVisibility(View.GONE);
            corner = (ImageView)findViewById(R.id.left_RB);
            assert corner != null;
            corner.setVisibility(View.GONE);
            corner = (ImageView)findViewById(R.id.left_LB);
            assert corner != null;
            corner.setVisibility(View.GONE);

            //
            corner = (ImageView)findViewById(R.id.right_LT);
            assert corner != null;
            corner.setVisibility(View.VISIBLE);
            corner = (ImageView)findViewById(R.id.right_RT);
            assert corner != null;
            corner.setVisibility(View.VISIBLE);
            corner = (ImageView)findViewById(R.id.right_RB);
            assert corner != null;
            corner.setVisibility(View.VISIBLE);
            corner = (ImageView)findViewById(R.id.right_LB);
            assert corner != null;
            corner.setVisibility(View.VISIBLE);
        }
    }
    private boolean moveOrigin(boolean local, MotionEvent event) {

        //Logs.add(Logs.Type.V, "local: " + local + ", event: " + event);
        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN: {
                if (mPointerId == Constants.NO_DATA) {
                    if (mLocal != local) {

                        Logs.add(Logs.Type.I, "Change frame");
                        mLocal = !mLocal;
                        updateZoom(true);
                    }
                    else {

                        Logs.add(Logs.Type.I, "Start changing origin");
                        mPointerId = event.getPointerId(0);
                        mPrevX = event.getX(0);
                        mPrevY = event.getY(0);
                    }
                    return true;
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                if (mPointerId == Constants.NO_DATA)
                    break;

                for (int i = 0; i < event.getPointerCount(); ++i) {
                    if (event.getPointerId(i) == mPointerId) {

                        Logs.add(Logs.Type.I, "Finish changing origin");
                        mPointerId = Constants.NO_DATA;
                        mPrevX = mPrevY = 0f;
                        break;
                    }
                }
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (mPointerId == Constants.NO_DATA)
                    break;

                for (int i = 0; i < event.getPointerCount(); ++i) {
                    if (event.getPointerId(i) == mPointerId) {

                        updateOrigin((int)(event.getX(i) - mPrevX), (int)(event.getY(i) - mPrevY));
                        updateZoom(false);
                        // TODO: Add landscape natural orientation devices management

                        mPrevX = event.getX(i);
                        mPrevY = event.getY(i);
                        break;
                    }
                }
                return true;
            }
        }
        return false;
    }
    private void updateOrigin(int deltaX, int deltaY) {
        // Update origin values according new configuration

        //Logs.add(Logs.Type.V, null);
        int originX = (int)(mFrameWidth - (mZoom * mFrameWidth));
        int originY = (int)(mFrameHeight - (mZoom * mFrameHeight));

        int newOrigin = (originX >> 1) + mTransX + deltaX;
        if (newOrigin >= 0) {
            if (newOrigin <= originX) {

                mTransX += deltaX;
                mOriginX = newOrigin;
            }
            else
                mOriginX = originX;
        }
        else
            mOriginX = 0;

        newOrigin = (originY >> 1) + mTransY + deltaY;
        if (newOrigin >= 0) {
            if (newOrigin <= originY) {

                mTransY += deltaY;
                mOriginY = newOrigin;
            }
            else
                mOriginY = originY;
        }
        else
            mOriginY = 0;
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
            mOriginX = savedInstanceState.getInt(DATA_KEY_ORIGIN_X);
            mOriginY = savedInstanceState.getInt(DATA_KEY_ORIGIN_Y);
            mLocal = savedInstanceState.getBoolean(DATA_KEY_LOCAL);

            mTransX = savedInstanceState.getInt(DATA_KEY_TRANS_X);
            mTransY = savedInstanceState.getInt(DATA_KEY_TRANS_Y);

            mChanged = savedInstanceState.getBoolean(DATA_KEY_CHANGED);
        }

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

        localFrame.setImageBitmap(openBitmapFile(true));
        localFrame.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                Logs.add(Logs.Type.V, "event: " + event);
                return moveOrigin(true, event);
            }
        });
        remoteFrame.setImageBitmap(openBitmapFile(false));
        remoteFrame.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                Logs.add(Logs.Type.V, "event: " + event);
                return moveOrigin(false, event);
            }
        });

        getFrameImageSize(params.height + Constants.DIMEN_ACTION_BAR_HEIGHT +
                Constants.DIMEN_STATUS_BAR_HEIGHT);
        ((RelativeLayout.LayoutParams)localFrame.getLayoutParams()).height = mImageHeight;
        ((RelativeLayout.LayoutParams)remoteFrame.getLayoutParams()).height = mImageHeight;
        ((RelativeLayout.LayoutParams)localFrame.getLayoutParams()).width = mImageWidth;
        ((RelativeLayout.LayoutParams)remoteFrame.getLayoutParams()).width = mImageWidth;

        updateZoom(true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putFloat(DATA_KEY_ZOOM, mZoom);
        outState.putInt(DATA_KEY_ORIGIN_X, mOriginX);
        outState.putInt(DATA_KEY_ORIGIN_Y, mOriginY);
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
