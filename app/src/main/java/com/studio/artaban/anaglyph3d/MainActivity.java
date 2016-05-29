package com.studio.artaban.anaglyph3d;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.studio.artaban.anaglyph3d.album.VideoListActivity;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.helpers.Storage;
import com.studio.artaban.anaglyph3d.transfer.Connectivity;

/**
 * Created by pascal on 19/03/16.
 * Main activity (from where to start recording)
 */
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {

    private int mNavItemSelected = Constants.NO_DATA; // Id of the selected navigation item (or -1 if none)
    private void onSelectNavItem() {

        switch (mNavItemSelected) {
            case R.id.navAlbum: {

                // Display album activity
                Intent intent = new Intent(this, VideoListActivity.class);
                intent.putExtra(Constants.DATA_CONNECTION_ESTABLISHED, true);

                startActivityForResult(intent, 0);
                break;
            }
            case R.id.navSettings: {

                // Display settings activity
                Intent intent = new Intent(this, SettingsActivity.class);
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
                                ActivityWrapper.stopActivity(MainActivity.class,
                                        Constants.RESULT_QUIT_APPLICATION, null);
                            }
                        });
                break;
            }
        }
        mNavItemSelected = Constants.NO_DATA;
    }
    private boolean checkMemorySpace() { // Return if storage memory space is enough to process (maker)

        if (Settings.getInstance().isMaker()) {
            long storageNeed = Storage.isStorageEnough();

            if (storageNeed != 0) {
                // Inform user
                DisplayMessage.getInstance().alert(R.string.title_warning, R.string.error_storage_available,
                        String.format("%.2f Go", storageNeed / (float)1000000000), false, null);

                return false;
            }
        }
        return true;
    }

    //
    private String mSubTitle;

    private static final int GLASS_ANIM_DURATION = 1000; // In millisecond
    private boolean mGlassDisplayed;

    private void positionGlass(ImageView glass) { // Position glass image according setting

        RelativeLayout.LayoutParams imgParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        if (Settings.getInstance().mPosition) {

            glass.setImageDrawable(getResources().getDrawable(R.drawable.left_glass));
            if (Build.VERSION.SDK_INT >= 17)
                imgParams.addRule(RelativeLayout.ALIGN_PARENT_END);
            imgParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        }
        else {

            glass.setImageDrawable(getResources().getDrawable(R.drawable.right_glass));
            if (Build.VERSION.SDK_INT >= 17)
                imgParams.addRule(RelativeLayout.ALIGN_PARENT_START);
            imgParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        }
        imgParams.addRule(RelativeLayout.CENTER_VERTICAL);
        glass.setLayoutParams(imgParams);
    }
    private void displayGlass(ImageView glass) { // Display animation glass

        positionGlass(glass);

        // Anim glass
        TranslateAnimation anim = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, (Settings.getInstance().mPosition)? 1f:-1f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f);
        anim.setDuration(GLASS_ANIM_DURATION);
        glass.startAnimation(anim);
    }

    public void displayPosition(final boolean animation) {

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

                final ImageView imgGlass = (ImageView)findViewById(R.id.glass_image);
                if (imgGlass != null) {

                    if (!animation)
                        positionGlass(imgGlass);

                    else { // Display glass with a translate animation

                        imgGlass.clearAnimation();
                        if (!mGlassDisplayed) {

                            mGlassDisplayed = true;
                            displayGlass(imgGlass);
                        }
                        else {

                            TranslateAnimation anim = new TranslateAnimation(
                                    Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF,
                                    (Settings.getInstance().mPosition)? -1f:1f,
                                    Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f);
                            anim.setDuration(GLASS_ANIM_DURATION);
                            anim.setAnimationListener(new Animation.AnimationListener() {

                                @Override public void onAnimationStart(Animation animation) { }
                                @Override public void onAnimationRepeat(Animation animation) { }
                                @Override
                                public void onAnimationEnd(Animation animation) {
                                    displayGlass(imgGlass);
                                }
                            });
                            imgGlass.startAnimation(anim);
                        }
                    }
                }
            }
        });
    }

    //
    private boolean mInPause = true; // Activity in background (pause status)
    private boolean mReadySent = false; // Flag to know if activity was ready when requested

    public boolean isReadySent() { return mReadySent; } // See comments when used...
    public boolean isReady() {

        if (mReadySent)
            return false; // Avoid to reopen process activity

        // Check if activity is ready to start recording:
        // -> Activity not in pause
        // -> Menu not displayed
        // -> Storage memory space enough (for maker only)
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if ((drawer != null) && (!drawer.isDrawerOpen(GravityCompat.START)) &&
                (!mInPause) && (checkMemorySpace())) {

            mReadySent = true;
            return true; // ...will open process activity
        }
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
        assert toolbar != null;
        if (Build.VERSION.SDK_INT >= 21) {
            toolbar.setBackgroundColor(Color.BLACK);
            getWindow().setNavigationBarColor(Color.BLACK);
            getWindow().setStatusBarColor(Color.BLACK);
        }
        else // Default status bar color (API < 21)
            toolbar.setBackgroundColor(Color.argb(255, 30, 30, 30));

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(this);

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
        displayPosition(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        ActivityWrapper.set(this); // Set current activity

        displayPosition(false); // In case it has changed

        if (requestCode != 0) {
            Logs.add(Logs.Type.F, "Unexpected request code");
            return;
        }
        switch (resultCode) {
            case Constants.RESULT_LOST_CONNECTION: {

                finish(); // Lost connection (back to connect activity)
                break;
            }
            case Constants.RESULT_QUIT_APPLICATION: {

                setResult(Constants.RESULT_QUIT_APPLICATION);
                finish();
                break;
            }
            case Constants.RESULT_DISPLAY_ALBUM: {

                // Display album activity to add the new video into the album
                Intent intent = new Intent(this, VideoListActivity.class);
                intent.putExtra(Constants.DATA_ACTIVITY, data.getBundleExtra(Constants.DATA_ACTIVITY));
                intent.putExtra(Constants.DATA_CONNECTION_ESTABLISHED, true);
                intent.putExtra(Constants.DATA_ADD_VIDEO, true);

                startActivityForResult(intent, 0);
                break;
            }
            case Constants.RESULT_NO_VIDEO: {

                DisplayMessage.getInstance().toast(R.string.no_video, Toast.LENGTH_LONG);
                break;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mInPause = mReadySent = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mInPause = true;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if ((drawer != null) && (drawer.isDrawerOpen(GravityCompat.START)))
            drawer.closeDrawer(GravityCompat.START);

        else // Put application into background (paused)
            moveTaskToBack(true);
    }

    //////
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null)
            drawer.closeDrawer(GravityCompat.START);

        mNavItemSelected = item.getItemId();
        // Let's drawer close event do the job (more efficient)

        return true;
    }

    //////
    @Override
    public void onClick(View view) { // Start recording
        if (checkMemorySpace()) {

            // Add connectivity request to check if remote device is ready...
            Connectivity.getInstance().addRequest(ActivityWrapper.getInstance(),
                    ActivityWrapper.REQ_TYPE_READY, null);

            // ...let's start the process activity if so
        }
    }
}
