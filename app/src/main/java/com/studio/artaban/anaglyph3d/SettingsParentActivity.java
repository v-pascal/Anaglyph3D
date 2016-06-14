package com.studio.artaban.anaglyph3d;

import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.studio.artaban.anaglyph3d.helpers.Logs;

/**
 * A {@link android.preference.PreferenceActivity} which implements and proxies the necessary calls
 * to be used with AppCompat.
 */
public abstract class SettingsParentActivity extends PreferenceActivity {

    private AppCompatDelegate mDelegate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logs.add(Logs.Type.V, "savedInstanceState: " + ((savedInstanceState != null) ?
                savedInstanceState.toString() : "null"));
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Logs.add(Logs.Type.V, "savedInstanceState: " + ((savedInstanceState != null) ?
                savedInstanceState.toString() : "null"));
        getDelegate().onPostCreate(savedInstanceState);
    }

    public ActionBar getSupportActionBar() {
        Logs.add(Logs.Type.V, null);
        return getDelegate().getSupportActionBar();
    }

    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        Logs.add(Logs.Type.V, "toolbar: " + ((toolbar != null)? toolbar.toString():"null"));
        getDelegate().setSupportActionBar(toolbar);
    }

    @Override
    public MenuInflater getMenuInflater() {
        Logs.add(Logs.Type.V, null);
        return getDelegate().getMenuInflater();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        Logs.add(Logs.Type.V, "layoutResID: " + layoutResID);
        getDelegate().setContentView(layoutResID);
    }

    @Override
    public void setContentView(View view) {
        Logs.add(Logs.Type.V, "view: " + ((view != null)? view.toString():"null"));
        getDelegate().setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        Logs.add(Logs.Type.V, "view: " + ((view != null)? view.toString():"null"));
        getDelegate().setContentView(view, params);
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        Logs.add(Logs.Type.V, "view: " + ((view != null)? view.toString():"null"));
        getDelegate().addContentView(view, params);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Logs.add(Logs.Type.V, null);
        getDelegate().onPostResume();
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        Logs.add(Logs.Type.V, null);
        getDelegate().setTitle(title);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Logs.add(Logs.Type.V, null);
        getDelegate().onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Logs.add(Logs.Type.V, null);
        getDelegate().onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Logs.add(Logs.Type.V, null);
        getDelegate().onDestroy();
    }

    public void invalidateOptionsMenu() {
        Logs.add(Logs.Type.V, null);
        getDelegate().invalidateOptionsMenu();
    }

    private AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }
}
