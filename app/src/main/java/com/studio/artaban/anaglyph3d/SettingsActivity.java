package com.studio.artaban.anaglyph3d;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;
import android.view.View;
import android.widget.NumberPicker;

import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.transfer.Connectivity;

import org.json.JSONObject;

/**
 * Created by pascal on 21/03/16.
 * Settings activity (preference)
 */
public class SettingsActivity extends SettingsParentActivity
        implements Preference.OnPreferenceChangeListener {

    private class NumberPickerPreference extends DialogPreference {

        public int mMin, mMax;

        private int mNumberValue;
        private NumberPicker mNumberPicker;

        public NumberPickerPreference(Context context, AttributeSet attrs) {
            super(context, attrs);
            setDialogLayoutResource(R.layout.number_dialog);
            setPersistent(false); // Do not store preference (always use default)
        }

        @Override
        public void setDefaultValue(Object defaultValue) {

            mNumberValue = (int)defaultValue;
            persistInt(mNumberValue);
        }

        @Override
        protected View onCreateDialogView() {
            View dialogView = super.onCreateDialogView();

            mNumberPicker = (NumberPicker)dialogView.findViewById(R.id.numberPicker);
            mNumberPicker.setMinValue(mMin);
            mNumberPicker.setMaxValue(mMax);
            mNumberPicker.setValue(mNumberValue);
            mNumberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

            return dialogView;
        }

        @Override
        protected void onDialogClosed(boolean positiveResult) {
            if (positiveResult) {

                mNumberValue = mNumberPicker.getValue();
                persistInt(mNumberValue);
                setSummary(String.valueOf(mNumberValue));

                // Replace 'onPreferenceChange' call here
                if (getKey().equals(Settings.DATA_KEY_DURATION)) {

                    Settings.getInstance().mDuration = mNumberValue;
                    Connectivity.getInstance().addRequest(Settings.getInstance(),
                            Settings.REQ_TYPE_DURATION, null);
                }
                else if (getKey().equals(Settings.DATA_KEY_FPS)) {

                    Settings.getInstance().mFps = mNumberValue;
                    Connectivity.getInstance().addRequest(Settings.getInstance(),
                            Settings.REQ_TYPE_FPS, null);
                }
            }
        }
    };

    // Update settings preferences (used to apply remote device settings update)
    public void update(final JSONObject settings) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (settings.has(Settings.DATA_KEY_POSITION)) {

                    mPositionLock = true;
                    mPositionSwitch.setChecked(Settings.getInstance().mPosition);
                }
                if (settings.has(Settings.DATA_KEY_ORIENTATION)) {

                    mOrientationLock = true;
                    mOrientationSwitch.setChecked(Settings.getInstance().mOrientation);
                    updateResolutions();
                }











            }
        });
    }

    // Fill & Set resolutions preference list and value
    private void updateResolutions() {

        mResolutionLock = true;

        final String[] resolutions = Settings.getInstance().getResolutions();
        mResolutionList.setEntries(resolutions);
        mResolutionList.setEntryValues(resolutions);

        final String resolution = Settings.getInstance().getResolution();
        mResolutionList.setDefaultValue(resolution);
        mResolutionList.setValue(resolution);
        mResolutionList.setSummary(resolution);
    }

    private boolean mPositionLock = true;
    private boolean mOrientationLock = true;
    private boolean mResolutionLock = true;
    // Needed to avoid to send settings request to remote device during assignment (init or update)
    // -> Removing preference change listener to avoid calling 'onPreferenceChange' is not working:
    //
    //    mResolutionList.setOnPreferenceChangeListener(null);
    //    mResolutionList.setValue(resolution); // Call 'onPreferenceChange' anyway!
    //    mResolutionList.setOnPreferenceChangeListener(this);

    // Preferences
    private SwitchPreference mPositionSwitch;
    private SwitchPreference mOrientationSwitch;
    private ListPreference mResolutionList;
    private NumberPickerPreference mDurationPreference;
    private NumberPickerPreference mFpsPreference;

    //////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_preference);

        // Set current activity
        ActivityWrapper.set(this);

        // Update toolbar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            if (Build.VERSION.SDK_INT >= 21) {
                actionBar.setBackgroundDrawable(getResources().getDrawable(android.R.color.black));
                getWindow().setNavigationBarColor(Color.BLACK);
                getWindow().setStatusBarColor(Color.BLACK);
            }
            else // Default status bar color (API < 21)
                actionBar.setBackgroundDrawable(getResources().getDrawable(R.color.api_16_black));

            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Initialize preferences (according settings)
        mResolutionList = (ListPreference)findPreference(Settings.DATA_KEY_RESOLUTION);
        mResolutionList.setOnPreferenceChangeListener(this);
        updateResolutions();

        mPositionSwitch = (SwitchPreference)findPreference(Settings.DATA_KEY_POSITION);
        mPositionSwitch.setChecked(Settings.getInstance().mPosition);
        mPositionSwitch.setOnPreferenceChangeListener(this);

        mOrientationSwitch = (SwitchPreference)findPreference(Settings.DATA_KEY_ORIENTATION);
        mOrientationSwitch.setChecked(Settings.getInstance().mOrientation);
        mOrientationSwitch.setOnPreferenceChangeListener(this);

        // Add number picker dialog preferences programmatically (needed coz API level 21 requirement)
        // -> Unable to declare 'NumberPickerPreference' constructor with 'context' parameter only
        // -> See error message in 'res/xml/settings_preference' XML file
        final PreferenceCategory preferenceCat = (PreferenceCategory)findPreference("settings");

        mDurationPreference = new NumberPickerPreference(this, null);
        mDurationPreference.setKey(Settings.DATA_KEY_DURATION);
        mDurationPreference.setTitle(R.string.duration);
        mDurationPreference.setDialogTitle(R.string.duration);
        mDurationPreference.setDefaultValue(Settings.getInstance().mDuration);
        mDurationPreference.setSummary(String.valueOf(Settings.getInstance().mDuration));
        mDurationPreference.mMin = Constants.CONFIG_MIN_DURATION;
        mDurationPreference.mMax = Constants.CONFIG_MAX_DURATION;
        //mDurationPreference.setOnPreferenceChangeListener(this);
        // BUG: Not working! 'onPreferenceChange' never called...

        mFpsPreference = new NumberPickerPreference(this, null);
        mFpsPreference.setKey(Settings.DATA_KEY_FPS);
        mFpsPreference.setTitle(R.string.frame_per_second);
        mFpsPreference.setDialogTitle(R.string.frame_per_second);
        mFpsPreference.setDefaultValue(Settings.getInstance().mFps);
        mFpsPreference.setSummary(String.valueOf(Settings.getInstance().mFps));
        mFpsPreference.mMin = Constants.CONFIG_MIN_FPS;
        mFpsPreference.mMax = Constants.CONFIG_MAX_FPS;
        //mFpsPreference.setOnPreferenceChangeListener(this);
        // BUG: Not working! 'onPreferenceChange' never called...

        preferenceCat.addPreference(mDurationPreference);
        preferenceCat.addPreference(mFpsPreference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.getKey().equals(Settings.DATA_KEY_POSITION)) {
            if (mPositionLock) {
                mPositionLock = false;
                return true;
            }
            Settings.getInstance().mPosition = (boolean)newValue;
            Connectivity.getInstance().addRequest(Settings.getInstance(),
                    Settings.REQ_TYPE_POSITION, null);
            return true;
        }
        else if (preference.getKey().equals(Settings.DATA_KEY_ORIENTATION)) {
            if (mOrientationLock) {
                mOrientationLock = false;
                return true;
            }
            Settings.getInstance().mOrientation = (boolean)newValue;
            Connectivity.getInstance().addRequest(Settings.getInstance(),
                    Settings.REQ_TYPE_ORIENTATION, null);

            updateResolutions();
            return true;
        }
        else if (preference.getKey().equals(Settings.DATA_KEY_RESOLUTION)) {
            if (mResolutionLock) {
                mResolutionLock = false;
                return true;
            }
            preference.setSummary((String) newValue);
            Settings.getInstance().setResolution((String) newValue,
                    (String[]) ((ListPreference) preference).getEntryValues());
            Connectivity.getInstance().addRequest(Settings.getInstance(),
                    Settings.REQ_TYPE_RESOLUTION, null);
            return true;
        }
        //else if (preference.getKey().equals(Settings.DATA_KEY_DURATION)) {
        //else if (preference.getKey().equals(Settings.DATA_KEY_FPS)) {
        // BUG: Never called! Done in 'NumberPickerPreference.onDialogClosed' method

        return false;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }
}
