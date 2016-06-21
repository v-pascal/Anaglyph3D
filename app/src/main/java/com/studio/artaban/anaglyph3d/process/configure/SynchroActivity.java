package com.studio.artaban.anaglyph3d.process.configure;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageView;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.helpers.Storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class SynchroActivity extends AppCompatActivity {

    public static final String DATA_KEY_FRAME_COUNT = "frameCount";
    public static final String DATA_KEY_SYNCHRO_OFFSET = "offset";
    public static final String DATA_KEY_SYNCHRO_LOCAL = "local";
    // Data keys

    public static final short DEFAULT_OFFSET = 0;
    public static final boolean DEFAULT_LOCAL = true;
    // Default values

    private static final int FRAME_REMAINING = 16;
    // Offset limit == frame count - FRAME_REMAINING

    //////
    private short mOffset = DEFAULT_OFFSET; // Frame count to shift from the origin (synchro result)
    private boolean mLocalVideo = DEFAULT_LOCAL; // Local video from which to apply the synchro (false for remote)

    private int mFrameCount = 0;
    private ViewPager mViewPager;
    private ImageView mCompareImage;

    private static Bitmap openBitmapFile(int position, boolean local) { // Return bitmap from RGBA file

        Logs.add(Logs.Type.V, "position: " + position + ", local: " + local);
        File bmpFile = new File(Storage.DOCUMENTS_FOLDER + File.separator + ((local)?
                        Constants.PROCESS_LOCAL_PREFIX:Constants.PROCESS_REMOTE_PREFIX) +
                String.format("%04d", position) + Constants.EXTENSION_RGBA);

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

    //
    public static class PlaceholderFragment extends Fragment {

        public static PlaceholderFragment newInstance(int position, boolean local) {

            Bundle args = new Bundle();
            args.putInt(DATA_KEY_SYNCHRO_OFFSET, position);
            args.putBoolean(DATA_KEY_SYNCHRO_LOCAL, local);

            PlaceholderFragment fragment = new PlaceholderFragment();
            fragment.setArguments(args);
            return fragment;
        }

        //
        private ImageView mFrameImage;

        //////
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            Logs.add(Logs.Type.V, null);
            View rootView = inflater.inflate(R.layout.fragment_synchro, container, false);
            TextView framePosition = (TextView) rootView.findViewById(R.id.frame_position);
            framePosition.setText("#" + getArguments().getInt(DATA_KEY_SYNCHRO_OFFSET));
            mFrameImage = (ImageView)rootView.findViewById(R.id.frame_image);
            return rootView;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            Logs.add(Logs.Type.V, null);

            Bitmap bitmap = openBitmapFile(getArguments().getInt(DATA_KEY_SYNCHRO_OFFSET),
                    getArguments().getBoolean(DATA_KEY_SYNCHRO_LOCAL));
            if (bitmap != null)
                mFrameImage.setImageBitmap(bitmap);
        }
    }
    private class FramesPagerAdapter extends FragmentStatePagerAdapter {

        public FramesPagerAdapter(FragmentManager fm) { super(fm); }

        @Override public Fragment getItem(int position) {
            return PlaceholderFragment.newInstance(position, mLocalVideo);
        }
        @Override public int getCount() { return mFrameCount; }
    }

    //
    public void onValidateSynchro(View sender) { // Validate synchronization setting
        Logs.add(Logs.Type.V, null);

        Intent intent = new Intent();
        intent.putExtra(DATA_KEY_SYNCHRO_OFFSET, mOffset);
        intent.putExtra(DATA_KEY_SYNCHRO_LOCAL, mLocalVideo);

        setResult(RESULT_OK, intent);
        finish();
    }
    public void onChangeFrame(View sender) {
        Logs.add(Logs.Type.V, null);

        mLocalVideo = !mLocalVideo;
        mOffset = 0;

        // Update UI
        Bitmap bmpCompare = openBitmapFile(0, !mLocalVideo);
        if (bmpCompare != null)
            mCompareImage.setImageBitmap(bmpCompare);

        mViewPager.setAdapter(new FramesPagerAdapter(getSupportFragmentManager()));
    }

    //////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logs.add(Logs.Type.V, "savedInstanceState: " + savedInstanceState);
        setContentView(R.layout.activity_synchro);

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

            mOffset = savedInstanceState.getShort(DATA_KEY_SYNCHRO_OFFSET);
            mLocalVideo = savedInstanceState.getBoolean(DATA_KEY_SYNCHRO_LOCAL);
        }
        Bundle data = getIntent().getExtras().getBundle(Constants.DATA_ACTIVITY);
        if (data != null)
            mFrameCount = data.getInt(DATA_KEY_FRAME_COUNT) - FRAME_REMAINING;
        else
            Logs.add(Logs.Type.F, "No frame count defined");

        // Position slide info images
        final ImageView leftSlide = (ImageView)findViewById(R.id.left_scroll);
        final ImageView rightSlide = (ImageView)findViewById(R.id.right_scroll);
        leftSlide.setTranslationY(getResources().getDimension(R.dimen.synchro_slide_lag));
        rightSlide.setTranslationY(getResources().getDimension(R.dimen.synchro_slide_lag));

        // Position frame image to compare
        mCompareImage = (ImageView)findViewById(R.id.frame_compare);
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_apply);
        assert fab != null;
        LayoutParams params = (LayoutParams)mCompareImage.getLayoutParams();
        int margin = ((CoordinatorLayout.LayoutParams)fab.getLayoutParams()).rightMargin;

        final Point screenSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(screenSize);

        int frameWidth = Settings.getInstance().getResolutionWidth();
        int frameHeight = Settings.getInstance().getResolutionHeight();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            Logs.add(Logs.Type.I, "Portrait");

            ////// Portrait
            if (getResources().getBoolean(R.bool.w480dp)) // Large screen device
                params.width = (screenSize.x >> 1) - (margin << 1);
                // == 50% of the screen width
            else // Normal screen device
                params.width = screenSize.x - Constants.DIMEN_FAB_SIZE - (margin << 2);
                // == screen width less 'fab' size

            params.height = (int)(params.width * frameHeight / (float)frameWidth);
            int screenHeight = screenSize.y - Constants.DIMEN_ACTION_BAR_HEIGHT;
            if ((3 * (params.height + (margin << 1))) > screenHeight) { // Maximum 1/3 of the screen height

                params.height = (int)((screenHeight / 3.0f) - (margin << 1));
                params.width = (int)(params.height * frameWidth / (float)frameHeight);
            }
        }
        else { ////// Landscape
            Logs.add(Logs.Type.I, "Landscape");

            if (Settings.getInstance().mOrientation) // Portrait
                params.height = (screenSize.y - Constants.DIMEN_ACTION_BAR_HEIGHT) >> 1;
                // 50% of screen height
            else // Landscape
                params.height = (int)((screenSize.y - Constants.DIMEN_ACTION_BAR_HEIGHT) / 3f);
                // 1/3 of the screen height

            params.width = (int)(params.height * frameWidth / (float)frameHeight);
            if (params.width > (screenSize.x >> 1)) { // Maximum 50% of screen width

                params.width = screenSize.x >> 1;
                params.height = (int)(params.width * frameHeight / (float)frameWidth);
            }
        }
        Bitmap bmpCompare = openBitmapFile(0, !mLocalVideo);
        if (bmpCompare != null)
            mCompareImage.setImageBitmap(bmpCompare);
        mCompareImage.setLayoutParams(params);

        // Initialize pager view
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(new FramesPagerAdapter(getSupportFragmentManager()));
        if (mOffset != 0) {

            mViewPager.setCurrentItem(mOffset);
            rightSlide.setAlpha(1f);
        }
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (position == 0)
                    rightSlide.setAlpha(positionOffset);
            }

            @Override public void onPageSelected(int position) { mOffset = (short) position; }
            @Override public void onPageScrollStateChanged(int state) { }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putShort(DATA_KEY_SYNCHRO_OFFSET, mOffset);
        outState.putBoolean(DATA_KEY_SYNCHRO_LOCAL, mLocalVideo);

        Logs.add(Logs.Type.V, "outState: " + outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Logs.add(Logs.Type.V, "item: " + item);
        if (item.getItemId() == android.R.id.home) {

            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
