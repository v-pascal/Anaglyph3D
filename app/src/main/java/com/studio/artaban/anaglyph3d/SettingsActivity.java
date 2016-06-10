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
            setSummary(String.valueOf(mNumberValue));
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
                assert getKey().equals(Settings.DATA_KEY_DURATION);

                Settings.getInstance().mDuration = (short)mNumberValue;
                Connectivity.getInstance().addRequest(Settings.getInstance(),
                        Settings.REQ_TYPE_DURATION, null);
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
                    if (!settings.has(Settings.DATA_KEY_RESOLUTION))
                        updateResolutions();
                    //else // Done below
                }
                if (settings.has(Settings.DATA_KEY_RESOLUTION))
                    updateResolutions();

                if (settings.has(Settings.DATA_KEY_DURATION))
                    mDurationPreference.setDefaultValue((int)Settings.getInstance().mDuration);

                if (settings.has(Settings.DATA_KEY_FPS))
                    updateFpsRanges();

                //
                mPositionLock = false;
                mOrientationLock = false;
            }
        });
    }

    // Fill & Set resolutions preference list and value
    private void updateResolutions() {

        final String[] resolutions = Settings.getInstance().getResolutions();
        mResolutionList.setEntries(resolutions);
        mResolutionList.setEntryValues(resolutions);

        final String resolution = Settings.getInstance().getResolution();
        mResolutionList.setDefaultValue(resolution);
        mResolutionList.setValue(resolution);
        mResolutionList.setSummary(resolution);
    }

    // Fill & Set fps ranges preference list and value
    private void updateFpsRanges() {

        final String[] fpsRanges = Settings.getInstance().getFpsRanges();
        mFpsList.setEntries(fpsRanges);
        mFpsList.setEntryValues(fpsRanges);

        final String fpsRange = Settings.getInstance().getFpsRange();
        mFpsList.setDefaultValue(fpsRange);
        mFpsList.setValue(fpsRange);
        mFpsList.setSummary(fpsRange);
    }

    private boolean mPositionLock = true;
    private boolean mOrientationLock = true;
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
    private ListPreference mFpsList;

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

        mFpsList = (ListPreference)findPreference(Settings.DATA_KEY_FPS);
        mFpsList.setOnPreferenceChangeListener(this);
        updateFpsRanges();
        if ((Settings.getInstance().mSimulated) || (Settings.getInstance().mNoFps))
            mFpsList.setEnabled(false);
            // Disable FPS if simulated 3D is requested or if no valid FPS is available

        mPositionSwitch = (SwitchPreference)findPreference(Settings.DATA_KEY_POSITION);
        mPositionSwitch.setChecked(Settings.getInstance().mPosition);
        if (Settings.getInstance().mPosition) // Left camera
            mPositionSwitch.setSummary(getResources().getString(R.string.left));
        else // Right camera
            mPositionSwitch.setSummary(getResources().getString(R.string.right));
        mPositionSwitch.setOnPreferenceChangeListener(this);
        mPositionSwitch.setEnabled(!Settings.getInstance().mSimulated);

        mOrientationSwitch = (SwitchPreference)findPreference(Settings.DATA_KEY_ORIENTATION);
        mOrientationSwitch.setChecked(Settings.getInstance().mOrientation);
        if (Settings.getInstance().mOrientation) // Portrait
            mOrientationSwitch.setSummary(getResources().getString(R.string.portrait));
        else // Landscape
            mOrientationSwitch.setSummary(getResources().getString(R.string.landscape));
        mOrientationSwitch.setOnPreferenceChangeListener(this);

        // Add number picker dialog preferences programmatically (needed coz API level 21 requirement)
        // -> Unable to declare 'NumberPickerPreference' constructor with 'context' parameter only
        // -> See error message in 'res/xml/settings_preference' XML file
        final PreferenceCategory preferenceCat = (PreferenceCategory)findPreference("settings");

        mDurationPreference = new NumberPickerPreference(this, null);
        mDurationPreference.setKey(Settings.DATA_KEY_DURATION);
        mDurationPreference.setTitle(R.string.duration);
        mDurationPreference.setDialogTitle(R.string.duration);
        mDurationPreference.setDefaultValue((int)Settings.getInstance().mDuration);
        mDurationPreference.mMin = Constants.CONFIG_MIN_DURATION;
        mDurationPreference.mMax = Constants.CONFIG_MAX_DURATION;
        //mDurationPreference.setOnPreferenceChangeListener(this);
        // BUG: Not working! 'onPreferenceChange' never called...

        preferenceCat.addPreference(mDurationPreference);

        //
        mPositionLock = false;
        mOrientationLock = false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.getKey().equals(Settings.DATA_KEY_POSITION)) {
            if (mPositionLock) {
                mPositionLock = false;
                return true;
            }

            // Avoid to send request to remote device if the setting has not changed
            if (Settings.getInstance().mPosition == (boolean)newValue)
                return true; // BUG: Why calling 'onPreferenceChange' if not changed?

            Settings.getInstance().mPosition = (boolean)newValue;
            if (Settings.getInstance().mPosition) // Left camera
                mPositionSwitch.setSummary(getResources().getString(R.string.left));
            else // Right camera
                mPositionSwitch.setSummary(getResources().getString(R.string.right));
            Connectivity.getInstance().addRequest(Settings.getInstance(),
                    Settings.REQ_TYPE_POSITION, null);
            return true;
        }
        else if (preference.getKey().equals(Settings.DATA_KEY_ORIENTATION)) {
            if (mOrientationLock) {
                mOrientationLock = false;
                return true;
            }

            // Avoid to send request to remote device if the setting has not changed
            if (Settings.getInstance().mOrientation == (boolean)newValue)
                return true; // BUG: Why calling 'onPreferenceChange' if not changed?

            Settings.getInstance().mOrientation = (boolean)newValue;
            if (Settings.getInstance().mOrientation) // Portrait
                preference.setSummary(getResources().getString(R.string.portrait));
            else // Landscape
                preference.setSummary(getResources().getString(R.string.landscape));
            Connectivity.getInstance().addRequest(Settings.getInstance(),
                    Settings.REQ_TYPE_ORIENTATION, null);

            updateResolutions();
            return true;
        }
        else if (preference.getKey().equals(Settings.DATA_KEY_RESOLUTION)) {

            preference.setSummary((String) newValue);
            Settings.getInstance().setResolution((String) newValue,
                    (String[]) ((ListPreference) preference).getEntryValues());
            Connectivity.getInstance().addRequest(Settings.getInstance(),
                    Settings.REQ_TYPE_RESOLUTION, null);
            return true;
        }
        else if (preference.getKey().equals(Settings.DATA_KEY_FPS)) {

            preference.setSummary((String) newValue);
            Settings.getInstance().setFps((String) newValue,
                    (String[]) ((ListPreference) preference).getEntryValues());
            Connectivity.getInstance().addRequest(Settings.getInstance(),
                    Settings.REQ_TYPE_FPS, null);
            return true;
        }
        //else if (preference.getKey().equals(Settings.DATA_KEY_DURATION)) {
        // BUG: Never called! Done in 'NumberPickerPreference.onDialogClosed' method

        return false;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item))
                NavUtils.navigateUpFromSameTask(this);

            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }
}
