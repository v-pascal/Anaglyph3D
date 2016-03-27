package com.studio.artaban.anaglyph3d;

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

import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.fragments.CamFragment;
import com.studio.artaban.anaglyph3d.fragments.ConfigFragment;
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private final CamFragment mCamFragment = new CamFragment(this);
    private final ConfigFragment mConfigFragment = new ConfigFragment(this);
    ////// Fragments

    private void displayPosition() {

        // Display remote device name into subtitle (with position)
        String remoteDevice;
        if (Settings.getInstance().getPosition())
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
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
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

        // Set current activity to be able to display message
        DisplayMessage.getInstance().setActivity(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        else {
            moveTaskToBack(true); // Put application into background (paused)
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.navAlbum) {

        }
        else if (id == R.id.navSettings) {

        }
        else if (id == R.id.navDisconnect) {

        }
        else if (id == R.id.navQuit) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
