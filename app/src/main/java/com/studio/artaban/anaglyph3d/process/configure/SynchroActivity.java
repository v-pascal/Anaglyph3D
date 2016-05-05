package com.studio.artaban.anaglyph3d.process.configure;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.process.ProcessThread;

import java.io.File;

public class SynchroActivity extends AppCompatActivity {

    public static final String DATA_KEY_SYNCHRO_OFFSET = "offset";
    public static final String DATA_KEY_SYNCHRO_LOCAL = "local";

    //////
    private short mOffset; // Frame count to shift from the origin
    private boolean mLocalVideo; // Local video from which to apply the synchro (false for remote)





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





    //
    public void onValidateSynchro(View sender) { // Validate synchronization setting

        getIntent().putExtra(DATA_KEY_SYNCHRO_OFFSET, mOffset);
        getIntent().putExtra(DATA_KEY_SYNCHRO_LOCAL, mLocalVideo);

        setResult(Constants.RESULT_PROCESS_SYNCHRO);
        finish();
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















        ////// Portrait

        final ImageView leftSlide = (ImageView)findViewById(R.id.left_scroll);
        final ImageView rightSlide = (ImageView)findViewById(R.id.right_scroll);
        leftSlide.setTranslationY((float)ActivityWrapper.ACTION_BAR_HEIGHT + Constants.ACTION_BAR_LAG);
        rightSlide.setTranslationY((float)ActivityWrapper.ACTION_BAR_HEIGHT + Constants.ACTION_BAR_LAG);



        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_apply);







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
            @Override public void onPageSelected(int position) { }
            @Override public void onPageScrollStateChanged(int state) { }
        });
















    }

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
}
