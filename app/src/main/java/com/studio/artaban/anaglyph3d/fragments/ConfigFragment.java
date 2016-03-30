package com.studio.artaban.anaglyph3d.fragments;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.helpers.Logs;

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
            setPersistent(false); // No 'SharedPreference' coz no reference from XML file
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




                }
                else if (getKey().equals(Settings.DATA_KEY_FPS)) {



                    Settings.getInstance().mFps = mNumberValue;



                }
            }
        }
    };

    //////
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_preference);









        if (!Settings.getInstance().initResolutions())
            Logs.add(Logs.Type.E, "!initResolutions");
        else
            Logs.add(Logs.Type.E, "initResolutions");








        // Initialize settings preferences
        Preference preference = findPreference(Settings.DATA_KEY_POSITION);
        preference.setOnPreferenceChangeListener(this);
        if (!Settings.getInstance().mPosition)
            preference.setDefaultValue(false);

        findPreference(Settings.DATA_KEY_ORIENTATION).setOnPreferenceChangeListener(this);

        final ListPreference preferenceList = (ListPreference)findPreference(Settings.DATA_KEY_RESOLUTION);
        preferenceList.setOnPreferenceChangeListener(this);

        final String[] resolutions = Settings.getInstance().getResolutions();
        preferenceList.setEntries(resolutions);
        preferenceList.setEntryValues(resolutions);
        preferenceList.setSummary(Settings.getInstance().getResolution());

        // Add number picker dialog preferences programmatically (probably caused by API level 21 requirement)
        // -> Unable to declare 'NumberPickerPreference' constructor with 'context' parameter only
        // -> See error message in 'res/xml/settings_preference' XML file
        final PreferenceCategory preferenceCat = (PreferenceCategory)findPreference("settings");

        final NumberPickerPreference durationPreference = new NumberPickerPreference(getActivity(), null);
        durationPreference.setKey(Settings.DATA_KEY_DURATION);
        //durationPreference.setOnPreferenceChangeListener(this);
        // BUG: Not working! 'onPreferenceChange' never called...
        durationPreference.setTitle(R.string.duration);
        durationPreference.setDialogTitle(R.string.duration);
        durationPreference.setDefaultValue(Settings.getInstance().mDuration);
        durationPreference.setSummary(String.valueOf(Settings.getInstance().mDuration));

        durationPreference.mMin = Constants.CONFIG_MIN_DURATION;
        durationPreference.mMax = Constants.CONFIG_MAX_DURATION;

        final NumberPickerPreference fpsPreference = new NumberPickerPreference(getActivity(), null);
        fpsPreference.setKey(Settings.DATA_KEY_FPS);
        //fpsPreference.setOnPreferenceChangeListener(this);
        // BUG: Not working! 'onPreferenceChange' never called...
        fpsPreference.setTitle(R.string.frame_per_second);
        fpsPreference.setDialogTitle(R.string.frame_per_second);
        fpsPreference.setDefaultValue(Settings.getInstance().mFps);
        fpsPreference.setSummary(String.valueOf(Settings.getInstance().mFps));

        fpsPreference.mMin = Constants.CONFIG_MIN_FPS;
        fpsPreference.mMax = Constants.CONFIG_MAX_FPS;

        preferenceCat.addPreference(durationPreference);
        preferenceCat.addPreference(fpsPreference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.getKey().equals(Settings.DATA_KEY_POSITION)) {




            //Settings.getInstance().mPosition = (boolean)newValue;





            return true;
        }
        else if (preference.getKey().equals(Settings.DATA_KEY_ORIENTATION)) {






            return true;
        }
        else if (preference.getKey().equals(Settings.DATA_KEY_RESOLUTION)) {




            preference.setSummary((String)newValue);




            return true;
        }
        //else if (preference.getKey().equals(Settings.DATA_KEY_DURATION)) {
        //else if (preference.getKey().equals(Settings.DATA_KEY_FPS)) {
        // BUG: Never called! Done in 'NumberPickerPreference.onDialogClosed' method

        return false;
    }
}
