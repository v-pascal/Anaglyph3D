package com.studio.artaban.anaglyph3d;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.studio.artaban.anaglyph3d.album.VideoListActivity;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;
import com.studio.artaban.anaglyph3d.transfer.Connectivity;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private int mNavItemSelected = Constants.NO_DATA; // Id of the selected navigation item (or -1 if none)
    private void onSelectNavItem() {

        switch (mNavItemSelected) {
            case R.id.navAlbum: {

                // Display album activity
                Intent intent = new Intent(getApplicationContext(), VideoListActivity.class);
                intent.putExtra(Constants.DATA_CONNECTION_ESTABLISHED, true);
                startActivityForResult(intent, 0);
                break;
            }
            case R.id.navSettings: {

                // Display settings activity
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivityForResult(intent, 0);
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
                                ActivityWrapper.quitApplication();
                            }
                        });
                break;
            }
        }
        mNavItemSelected = Constants.NO_DATA;
    }

    //
    private String mSubTitle;
    public void displayPosition() {

        // Display remote device name into subtitle (with position)
        if (Settings.getInstance().mPosition)
            mSubTitle = getResources().getString(R.string.camera_right);
        else
            mSubTitle = getResources().getString(R.string.camera_left);
        mSubTitle += " : " + Settings.getInstance().getRemoteDevice();

        runOnUiThread(new Runnable() { // Need if called from connectivity thread
            @Override
            public void run() {

                final Toolbar appBar = (Toolbar) findViewById(R.id.toolbar);
                if (appBar != null)
                    appBar.setSubtitle(mSubTitle);

                Fragment mainFragment = getSupportFragmentManager().findFragmentByTag(MainFragment.TAG_FRAGMENT);
                if (mainFragment != null)
                    ((MainFragment) mainFragment).displayPosition();
            }
        });
    }
    public boolean isReady() {

        // Check if activity is ready to start recording
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if ((drawer == null) || (!drawer.isDrawerOpen(GravityCompat.START)))
            return true;

        return false;
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
        if (toolbar != null) {
            if (Build.VERSION.SDK_INT >= 21) {
                toolbar.setBackgroundColor(Color.BLACK);
                getWindow().setNavigationBarColor(Color.BLACK);
                getWindow().setStatusBarColor(Color.BLACK);
            }
            else // Default status bar color (API < 21)
                toolbar.setBackgroundColor(Color.argb(255, 30, 30, 30));
        }

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {









                    // Start recording
                    //Check if remote device is ready








                }
            });
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null) {
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                    R.string.navigation_drawer_open, R.string.navigation_drawer_close) {

                @Override
                public void onDrawerClosed(View view) {
                    super.onDrawerClosed(view);
                    onSelectNavItem();
                }
            };
            drawer.setDrawerListener(toggle);
            toggle.syncState();
        }

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
            navigationView.setItemTextAppearance(R.style.NavDrawerTextStyle);
        }

        // Display remote device name into subtitle (with initial position)
        displayPosition();

        // Add camera fragment (after having set initial position)
        FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
        fragTransaction.add(R.id.main_container, new MainFragment(), MainFragment.TAG_FRAGMENT).commit();
        getSupportFragmentManager().executePendingTransactions();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        displayPosition(); // In case it has changed

        // Set current activity
        ActivityWrapper.set(this);

        if ((requestCode == 0) && (resultCode == Constants.RESULT_LOST_CONNECTION))
            finish(); // Lost connection (back to connect activity)
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if ((drawer != null) && (drawer.isDrawerOpen(GravityCompat.START)))
            drawer.closeDrawer(GravityCompat.START);

        else // Put application into background (paused)
            moveTaskToBack(true);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null)
            drawer.closeDrawer(GravityCompat.START);

        mNavItemSelected = item.getItemId();
        // Let's drawer close event do the job (more efficient)

        return true;
    }
}
