package com.studio.artaban.anaglyph3d;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.fragments.CamFragment;
import com.studio.artaban.anaglyph3d.fragments.ConfigFragment;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.transfer.Connectivity;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private final CamFragment mCamFragment = new CamFragment();
    private final ConfigFragment mConfigFragment = new ConfigFragment();
    ////// Fragments

    private boolean mSettings = false; // Settings fragment displayed
    private int mNavItemSelected = Constants.NO_DATA; // Id of the selected navigation item (or -1 if none)

    public void displayPosition() {

        // Display remote device name into subtitle (with position)
        String remoteDevice;
        if (Settings.getInstance().mPosition)
            remoteDevice = getResources().getString(R.string.camera_right);
        else
            remoteDevice = getResources().getString(R.string.camera_left);
        remoteDevice += " : " + Settings.getInstance().getRemoteDevice();

        final Toolbar appBar = (Toolbar)findViewById(R.id.toolbar);
        appBar.setSubtitle(remoteDevice);
    }

    //////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set current activity
        ActivityWrapper.set(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {

            @Override
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);

                switch (mNavItemSelected) {
                    case R.id.navAlbum: {









                        break;
                    }
                    case R.id.navSettings: {

                        if (mSettings)
                            break;

                        mSettings = true;

                        // Remove camera fragment
                        FragmentTransaction prevFragTransaction = getSupportFragmentManager().beginTransaction();
                        prevFragTransaction.remove(mCamFragment);
                        prevFragTransaction.commit();

                        // Add settings fragment
                        android.app.FragmentTransaction fragTransaction = getFragmentManager().beginTransaction();
                        fragTransaction.add(R.id.mainContainer, mConfigFragment);
                        fragTransaction.commit();

                        // BUG: There is no other way to do this coz using both 'android.app.FragmentTransaction'
                        //      and 'android.support.v4.app.FragmentTransaction' are managed separately. But it is
                        //      needed coz there is no 'PreferenceFragment' into the Android Support Library v4.
                        //
                        // -> Unable to replace camera fragment with settings fragment

                        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                        fab.setVisibility(View.GONE);
                        break;
                    }
                    case R.id.navDisconnect: {

                        DisplayMessage.getInstance().alert(R.string.title_confirm, R.string.confirm_disconnect,
                                null, true, new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Connectivity.getInstance().disconnect();
                                    }
                                });
                        break;
                    }
                    case R.id.navQuit: {

                        DisplayMessage.getInstance().alert(R.string.title_confirm, R.string.confirm_quit,
                                null, true, new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        try {
                                            ActivityWrapper.get().setResult(Constants.RESULT_QUIT_APPLICATION);
                                            ActivityWrapper.get().finish();
                                        }
                                        catch (NullPointerException e) {
                                            Logs.add(Logs.Type.F, "Wrong activity reference");
                                        }
                                    }
                                });
                        break;
                    }
                }
                mNavItemSelected = Constants.NO_DATA;
            }
        };
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setItemTextAppearance(R.style.NavDrawerTextStyle);

        final Toolbar appBar = (Toolbar)findViewById(R.id.toolbar);
        if (Build.VERSION.SDK_INT >= 21) {
            appBar.setBackgroundColor(Color.BLACK);
            getWindow().setNavigationBarColor(Color.BLACK);
            getWindow().setStatusBarColor(Color.BLACK);
        }
        else
            appBar.setBackgroundColor(Color.argb(255,30,30,30)); // Default status bar color (API < 21)

        // Display remote device name into subtitle (with initial position)
        displayPosition();

        // Add camera fragment (after having set initial position)
        FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
        fragTransaction.add(R.id.mainContainer, mCamFragment);
        fragTransaction.commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Set current activity
        ActivityWrapper.set(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        else {

            if (!mSettings)
                moveTaskToBack(true); // Put application into background (paused)

            else { // Settings opened

                mSettings = false;

                // Remove settings fragment
                android.app.FragmentTransaction fragTransaction = getFragmentManager().beginTransaction();
                fragTransaction.remove(mConfigFragment);
                fragTransaction.commit();

                // Add camera fragment
                FragmentTransaction prevFragTransaction = getSupportFragmentManager().beginTransaction();
                prevFragTransaction.add(R.id.mainContainer, mCamFragment);
                prevFragTransaction.commit();

                // BUG: See 'onDrawerClosed' method above at 'R.id.navSettings' case to
                //      understand why it has been implemented int that way.

                final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                fab.setVisibility(View.VISIBLE);
            }
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        mNavItemSelected = item.getItemId();
        // Let's drawer close event do the job (more efficient)

        return true;
    }
}
