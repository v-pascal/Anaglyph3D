package com.studio.artaban.anaglyph3d.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

import com.studio.artaban.anaglyph3d.MainActivity;
import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.transfer.Connectivity;

import java.util.Set;

/**
 * Created by pascal on 27/03/16.
 * Settings configuration fragment
 */
public class ConfigFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    private class NumberPickerPreference extends DialogPreference {

        public int mMin, mMax;

        private int mNumberValue;
        private NumberPicker mNumberPicker;

        public NumberPickerPreference(Context context, AttributeSet attrs) {
            super(context, attrs);
            setDialogLayoutResource(R.layout.number_dialog);
            setPersistent(false); // No 'SharedPreference' management coz no reference from XML file
        }                         // -> Done manually (see 'onResume' and 'onPause' methods)

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







                }
                else if (getKey().equals(Settings.DATA_KEY_FPS)) {

                    Settings.getInstance().mFps = mNumberValue;






                }
            }
        }
    };

    // Fill & Set resolution preference list and value
    private void updateResolution(ListPreference resolutionList) {

        final String[] resolutions = Settings.getInstance().getResolutions();
        resolutionList.setEntries(resolutions);
        resolutionList.setEntryValues(resolutions);

        final String resolution = Settings.getInstance().getResolution();
        resolutionList.setDefaultValue(resolution);
        resolutionList.setValue(resolution);
        resolutionList.setSummary(resolution);
    }

    private NumberPickerPreference mDurationPreference;
    private NumberPickerPreference mFpsPreference;

    //////
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_preference);

        // Initialize settings preferences
        SwitchPreference preferencePos = (SwitchPreference)findPreference(Settings.DATA_KEY_POSITION);
        preferencePos.setOnPreferenceChangeListener(this);
        preferencePos.setChecked(Settings.getInstance().mPosition);

        findPreference(Settings.DATA_KEY_ORIENTATION).setOnPreferenceChangeListener(this);

        final ListPreference preferenceList = (ListPreference)findPreference(Settings.DATA_KEY_RESOLUTION);
        preferenceList.setOnPreferenceChangeListener(this);
        updateResolution(preferenceList);

        // Add number picker dialog preferences programmatically (needed coz API level 21 requirement)
        // -> Unable to declare 'NumberPickerPreference' constructor with 'context' parameter only
        // -> See error message in 'res/xml/settings_preference' XML file
        final PreferenceCategory preferenceCat = (PreferenceCategory)findPreference("settings");

        mDurationPreference = new NumberPickerPreference(getActivity(), null);
        mDurationPreference.setKey(Settings.DATA_KEY_DURATION);
        mDurationPreference.setTitle(R.string.duration);
        mDurationPreference.setDialogTitle(R.string.duration);
        mDurationPreference.mMin = Constants.CONFIG_MIN_DURATION;
        mDurationPreference.mMax = Constants.CONFIG_MAX_DURATION;
        //mDurationPreference.setOnPreferenceChangeListener(this);
        // BUG: Not working! 'onPreferenceChange' never called...

        mFpsPreference = new NumberPickerPreference(getActivity(), null);
        mFpsPreference.setKey(Settings.DATA_KEY_FPS);
        mFpsPreference.setTitle(R.string.frame_per_second);
        mFpsPreference.setDialogTitle(R.string.frame_per_second);
        mFpsPreference.mMin = Constants.CONFIG_MIN_FPS;
        mFpsPreference.mMax = Constants.CONFIG_MAX_FPS;
        //mFpsPreference.setOnPreferenceChangeListener(this);
        // BUG: Not working! 'onPreferenceChange' never called...

        preferenceCat.addPreference(mDurationPreference);
        preferenceCat.addPreference(mFpsPreference);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Retrieve stored preference values (managed manually)
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Settings.getInstance().mDuration = settings.getInt(Settings.DATA_KEY_DURATION,
                Settings.getInstance().mDuration);

        Settings.getInstance().mFps = settings.getInt(Settings.DATA_KEY_FPS,
                Settings.getInstance().mFps);

        mDurationPreference.setDefaultValue(Settings.getInstance().mDuration);
        mDurationPreference.setSummary(String.valueOf(Settings.getInstance().mDuration));

        mFpsPreference.setDefaultValue(Settings.getInstance().mFps);
        mFpsPreference.setSummary(String.valueOf(Settings.getInstance().mFps));
    }

    @Override
    public void onPause() {
        super.onPause();

        // Store preference values (managed manually)
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(Settings.DATA_KEY_DURATION, Settings.getInstance().mDuration)
              .putInt(Settings.DATA_KEY_FPS, Settings.getInstance().mFps)
              .apply();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.getKey().equals(Settings.DATA_KEY_POSITION)) {

            Settings.getInstance().mPosition = (boolean)newValue;
            ((MainActivity)getActivity()).displayPosition();







            Connectivity.getInstance().addRequest(Settings.getInstance(), Settings.REQ_TYPE_POSITION, null);








            return true;
        }
        else if (preference.getKey().equals(Settings.DATA_KEY_ORIENTATION)) {

            Settings.getInstance().mOrientation = (boolean)newValue;

            final ListPreference list = (ListPreference)findPreference(Settings.DATA_KEY_RESOLUTION);
            updateResolution((ListPreference)list);







            return true;
        }
        else if (preference.getKey().equals(Settings.DATA_KEY_RESOLUTION)) {

            preference.setSummary((String) newValue);
            Settings.getInstance().setResolution((String) newValue,
                    (String[]) ((ListPreference) preference).getEntryValues());






            return true;
        }
        //else if (preference.getKey().equals(Settings.DATA_KEY_DURATION)) {
        //else if (preference.getKey().equals(Settings.DATA_KEY_FPS)) {
        // BUG: Never called! Done in 'NumberPickerPreference.onDialogClosed' method

        return false;
    }
}
