package com.studio.artaban.anaglyph3d.fragments;

import android.content.Context;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.data.Settings;

/**
 * Created by pascal on 27/03/16.
 * Settings configuration fragment
 */
public class ConfigFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    private class NumberPickerPreference extends DialogPreference {

        public int mMin, mMax;
        private NumberPicker mNumberPicker;

        public NumberPickerPreference(Context context, AttributeSet attrs) {
            super(context, attrs);
            setDialogLayoutResource(R.layout.number_dialog);
        }

        @Override
        protected View onCreateDialogView() {
            View dialogView = super.onCreateDialogView();

            mNumberPicker = (NumberPicker)dialogView.findViewById(R.id.numberPicker);
            mNumberPicker.setMinValue(mMin);
            mNumberPicker.setMaxValue(mMax);
            mNumberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

            return dialogView;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_preference);

        // Initialize settings preferences (default)
        Preference preference = findPreference(Settings.DATA_KEY_POSITION);
        preference.setOnPreferenceChangeListener(this);
        if (!Settings.getInstance().mPosition)
            preference.setDefaultValue(false);

        findPreference(Settings.DATA_KEY_ORIENTATION).setOnPreferenceChangeListener(this);

        final ListPreference preferenceList = (ListPreference)findPreference(Settings.DATA_KEY_RESOLUTION);
        preferenceList.setOnPreferenceChangeListener(this);
        preferenceList.setEntries(Settings.getInstance().getResolutions());
        preferenceList.setEntryValues(Settings.getInstance().getResolutions());
        preferenceList.setSummary(Settings.getInstance().getResolutions()[0]);

        final PreferenceCategory preferenceCat = (PreferenceCategory)findPreference("settings");

        final NumberPickerPreference durationPreference = new NumberPickerPreference(getActivity(), null);
        durationPreference.setKey(Settings.DATA_KEY_DURATION);
        durationPreference.setTitle(R.string.duration);
        durationPreference.setDialogTitle(R.string.duration);
        durationPreference.setDefaultValue(60);
        durationPreference.setSummary("60");

        durationPreference.mMin = 10;
        durationPreference.mMax = 180;

        final NumberPickerPreference fpsPreference = new NumberPickerPreference(getActivity(), null);
        fpsPreference.setKey(Settings.DATA_KEY_FPS);
        fpsPreference.setTitle(R.string.frame_per_second);
        fpsPreference.setDialogTitle(R.string.frame_per_second);
        fpsPreference.setDefaultValue(30);
        fpsPreference.setSummary("30");

        fpsPreference.mMin = 20;
        fpsPreference.mMax = 60;

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

            return true;
        }
        else if (preference.getKey().equals(Settings.DATA_KEY_DURATION)) {

            return true;
        }
        else if (preference.getKey().equals(Settings.DATA_KEY_FPS)) {

            return true;
        }
        return false;
    }
}
