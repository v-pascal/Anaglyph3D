package com.studio.artaban.anaglyph3d.process.configure;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.support.design.widget.CoordinatorLayout.LayoutParams;
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
import android.widget.TextView;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.process.ProcessThread;

import java.io.File;

public class SynchroActivity extends AppCompatActivity {

    public static final String DATA_KEY_SYNCHRO_OFFSET = "offset";
    public static final String DATA_KEY_SYNCHRO_LOCAL = "local";

    //////
    private short mOffset = 0; // Frame count to shift from the origin
    private boolean mLocalVideo = true; // Local video from which to apply the synchro (false for remote)

    private ImageView mFrameCompare; // Image to compare to the synchronization frame












    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;



    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_synchro, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "SECTION 1";
                case 1:
                    return "SECTION 2";
                case 2:
                    return "SECTION 3";
            }
            return null;
        }
    }



















    //
    public void onValidateSynchro(View sender) { // Validate synchronization setting

        getIntent().putExtra(DATA_KEY_SYNCHRO_OFFSET, mOffset);
        getIntent().putExtra(DATA_KEY_SYNCHRO_LOCAL, mLocalVideo);

        setResult(Constants.RESULT_PROCESS_SYNCHRO);
        finish();
    }
    public void onChangeFrame(View sender) {






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

        // Restore previous settings (if any)
        if (savedInstanceState != null) {

            mOffset = savedInstanceState.getShort(DATA_KEY_SYNCHRO_OFFSET);
            mLocalVideo = savedInstanceState.getBoolean(DATA_KEY_SYNCHRO_LOCAL);
        }











        Bundle data = new Bundle();
        data.putInt(ProcessThread.DATA_KEY_FRAME_COUNT, 14);
        getIntent().putExtra(Constants.DATA_ACTIVITY, data);
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













        ////// Portrait


        // Position slide info images
        final ImageView leftSlide = (ImageView)findViewById(R.id.left_scroll);
        final ImageView rightSlide = (ImageView)findViewById(R.id.right_scroll);
        leftSlide.setTranslationY((float)ActivityWrapper.ACTION_BAR_HEIGHT + Constants.ACTION_BAR_LAG);
        rightSlide.setTranslationY((float) ActivityWrapper.ACTION_BAR_HEIGHT + Constants.ACTION_BAR_LAG);

        // Position frame image to compare
        mFrameCompare = (ImageView)findViewById(R.id.frame_compare);
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_apply);
        int width, height, margin = ((LayoutParams)fab.getLayoutParams()).rightMargin;

        final Point screenSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(screenSize);
        if (screenSize.x > 480) // Large screen device
            width = (screenSize.x >> 1) - (margin << 1);
            // == 50% of the screen width
        else // Normal screen device
            width = screenSize.x - ActivityWrapper.FAB_SIZE - (margin << 2);
            // == screen width less 'fab' size

        if (Settings.getInstance().mOrientation) // Portrait
            height = (int)(width * Settings.getInstance().mResolution.width /
                    (float)Settings.getInstance().mResolution.height);
        else // Landscape
            height = (int)(width * Settings.getInstance().mResolution.height /
                    (float)Settings.getInstance().mResolution.width);

        int screenHeight = screenSize.y - ActivityWrapper.ACTION_BAR_HEIGHT;
        if ((3 * (height + (margin << 1))) > screenHeight) {

            height = (int)((screenHeight / 3.0f) - (margin << 1));
            if (Settings.getInstance().mOrientation) // Portrait
                width = (int)(height * Settings.getInstance().mResolution.height /
                        (float)Settings.getInstance().mResolution.width);
            else // Landscape
                width = (int)(height * Settings.getInstance().mResolution.width /
                        (float)Settings.getInstance().mResolution.height);
        }








        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);





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

    @Override public void onBackPressed() { onCancel(); }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.home) {

            onCancel();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
