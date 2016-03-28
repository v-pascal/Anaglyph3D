package com.studio.artaban.anaglyph3d.fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.studio.artaban.anaglyph3d.R;

/**
 * Created by pascal on 27/03/16.
 * Settings configuration fragment
 */
public class ConfigFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_preference);
    }
}
