package com.studio.artaban.anaglyph3d.process.configure;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
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
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;
import com.studio.artaban.anaglyph3d.helpers.Logs;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class SynchroActivity extends AppCompatActivity {

    public static final String DATA_KEY_FRAME_COUNT = "frameCount";
    public static final String DATA_KEY_SYNCHRO_OFFSET = "offset";
    public static final String DATA_KEY_SYNCHRO_LOCAL = "local";
    // Data keys

    private static final int MAX_OFFSET = 50; // Offset limit == frame count - MAX_OFFSET

    //////
    private short mOffset = 0; // Frame count to shift from the origin (synchro result)
    private boolean mLocalVideo = true; // Local video from which to apply the synchro (false for remote)
    private int mFrameCount = 0;

    private ViewPager mViewPager;

    //
    public static class FrameHolderFragment extends Fragment {

        public static final String ARG_FRAME_POSITION = "framePosition";

        public static FrameHolderFragment newInstance(int position) {

            FrameHolderFragment fragment = new FrameHolderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_FRAME_POSITION, position);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_synchro, container, false);





            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_FRAME_POSITION)));






            return rootView;
        }
    }
    private class FramesPagerAdapter extends FragmentPagerAdapter {

        private WeakReference<FrameHolderFragment>[] mFragmentList = new WeakReference[mFrameCount];

        public FramesPagerAdapter(FragmentManager fm) { super(fm); }
        public void reset() {






        }

        @Override public Fragment getItem(int position) {

            FrameHolderFragment item = null;
            if (mFragmentList[position] != null)
                item = (FrameHolderFragment)mFragmentList[position].get();

            if (item == null) {

                item = FrameHolderFragment.newInstance(position + 1);
                mFragmentList[position] = new WeakReference<FrameHolderFragment>(item);
            }
            return item;
        }
        @Override public int getCount() { return mFrameCount; }
    }






/*
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
*/





    //
    public void onValidateSynchro(View sender) { // Validate synchronization setting

        getIntent().putExtra(DATA_KEY_SYNCHRO_OFFSET, mOffset);
        getIntent().putExtra(DATA_KEY_SYNCHRO_LOCAL, mLocalVideo);

        setResult(Constants.RESULT_PROCESS_SYNCHRO);
        finish();
    }
    public void onChangeFrame(View sender) {

        mLocalVideo = !mLocalVideo;
        mOffset = 0;

        // Update UI
        ((FramesPagerAdapter)mViewPager.getAdapter()).reset();
        mViewPager.setCurrentItem(mOffset, false);
    }
    private void onCancel() {

        // Ask user to skip synchronization step
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
        setContentView(R.layout.activity_synchro);

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

            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onCancel();
                }
            });
            // See why this listener in bug description below...
        }

        ActionBar appBar = getSupportActionBar();
        if (appBar != null)
            appBar.setDisplayHomeAsUpEnabled(true);
            // BUG: This method should call 'onOptionsItemSelected' when back icon is pressed but do
            //      not it !?! Defining 'setNavigationOnClickListener' on toolbar in code just above
            //      will fix this.















        Bundle datas = new Bundle();
        datas.putInt(DATA_KEY_FRAME_COUNT, 1000);
        getIntent().putExtra(Constants.DATA_ACTIVITY, datas);
        File documents = getExternalFilesDir(null);
        if (documents != null)
            ActivityWrapper.DOCUMENTS_FOLDER = documents.getAbsolutePath();
        else
            Logs.add(Logs.Type.F, "Failed to get documents folder");
        TypedValue typedValue = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true))
            ActivityWrapper.ACTION_BAR_HEIGHT = TypedValue.complexToDimensionPixelSize(typedValue.data,
                    getResources().getDisplayMetrics());
        else {
            ActivityWrapper.ACTION_BAR_HEIGHT = 0;
            Logs.add(Logs.Type.W, "'android.R.attr.actionBarSize' attribute not found");
        }
        ActivityWrapper.FAB_SIZE = Math.round(56 * (getResources().getDisplayMetrics().xdpi/DisplayMetrics.DENSITY_DEFAULT));
        Settings.getInstance().initResolutions();
















        // Restore previous settings (if any)
        if (savedInstanceState != null) {

            mOffset = savedInstanceState.getShort(DATA_KEY_SYNCHRO_OFFSET);
            mLocalVideo = savedInstanceState.getBoolean(DATA_KEY_SYNCHRO_LOCAL);
        }
        Bundle data = getIntent().getExtras().getBundle(Constants.DATA_ACTIVITY);
        if (data != null)
            mFrameCount = data.getInt(DATA_KEY_FRAME_COUNT) - MAX_OFFSET;







        ////// Portrait


        // Position slide info images
        final ImageView leftSlide = (ImageView)findViewById(R.id.left_scroll);
        final ImageView rightSlide = (ImageView)findViewById(R.id.right_scroll);
        leftSlide.setTranslationY((float)ActivityWrapper.ACTION_BAR_HEIGHT + Constants.ACTION_BAR_LAG);
        rightSlide.setTranslationY((float) ActivityWrapper.ACTION_BAR_HEIGHT + Constants.ACTION_BAR_LAG);

        // Position frame image to compare
        final ImageView frameCompare = (ImageView)findViewById(R.id.frame_compare);
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_apply);
        LayoutParams params = (LayoutParams)frameCompare.getLayoutParams();
        int margin = ((CoordinatorLayout.LayoutParams)fab.getLayoutParams()).rightMargin;

        final Point screenSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(screenSize);
        if (screenSize.x > 480) // Large screen device
            params.width = (screenSize.x >> 1) - (margin << 1);
            // == 50% of the screen width
        else // Normal screen device
            params.width = screenSize.x - ActivityWrapper.FAB_SIZE - (margin << 2);
            // == screen width less 'fab' size

        if (Settings.getInstance().mOrientation) // Portrait
            params.height = (int)(params.width * Settings.getInstance().mResolution.width /
                    (float)Settings.getInstance().mResolution.height);
        else // Landscape
            params.height = (int)(params.width * Settings.getInstance().mResolution.height /
                    (float)Settings.getInstance().mResolution.width);

        int screenHeight = screenSize.y - ActivityWrapper.ACTION_BAR_HEIGHT;
        if ((3 * (params.height + (margin << 1))) > screenHeight) {

            params.height = (int)((screenHeight / 3.0f) - (margin << 1));
            if (Settings.getInstance().mOrientation) // Portrait
                params.width = (int)(params.height * Settings.getInstance().mResolution.height /
                        (float)Settings.getInstance().mResolution.width);
            else // Landscape
                params.width = (int)(params.height * Settings.getInstance().mResolution.width /
                        (float)Settings.getInstance().mResolution.height);
        }
        frameCompare.setLayoutParams(params);









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
            @Override public void onPageSelected(int position) { mOffset = (short)position; }
            @Override public void onPageScrollStateChanged(int state) { }
        });










    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putShort(DATA_KEY_SYNCHRO_OFFSET, mOffset);
        outState.putBoolean(DATA_KEY_SYNCHRO_LOCAL, mLocalVideo);

        super.onSaveInstanceState(outState);
    }

    @Override public void onBackPressed() {
        onCancel();
    }
}
