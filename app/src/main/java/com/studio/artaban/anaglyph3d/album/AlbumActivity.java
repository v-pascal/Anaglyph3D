package com.studio.artaban.anaglyph3d.album;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.album.details.DetailEditFragment;
import com.studio.artaban.anaglyph3d.album.details.DetailPlayerFragment;
import com.studio.artaban.anaglyph3d.data.AlbumTable;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;

/**
 * Created by pascal on 16/05/16.
 * Parent album activities (details selection & location detail management)
 */
public abstract class AlbumActivity extends AppCompatActivity implements
        View.OnClickListener, OnMapReadyCallback {

    private static final String TAG_FRAGMENT_LOCATION = "location";

    //
    public static final String DATA_VIDEO_POSITION = "position";
    public static final String DATA_NEW_VIDEO_ADDED = "added";
    public static final String DATA_NEW_VIDEO_SAVED = "saved";

    public static final String DATA_VIDEO_DETAIL = "detail";

    //////
    public interface OnVideoAlbumListener { // Videos album listener interface

        void onSave(int videoPosition, String title, String description); // Save video detail

        boolean isVideoCreation(); // Return if new video is selected

        void setEditFlag(boolean flag);

        boolean getEditFlag();
        // Edit video detail member flag
    }

    //
    protected int mVideoSelected = Constants.NO_DATA; // Selected video position (or video to select)
    protected String mDetailTag = DetailPlayerFragment.TAG; // Fragment detail displayed (tag)

    protected boolean mEditFlag = false; // Flag to know if editing video info (title & description)

    protected boolean saveEditingInfo() { // Save video detail if editing when user changes detail displayed

        if (!mEditFlag)
            return false; // No editing info to save

        ////// Save video detail changes B4:
        // _ Loading new detail fragment
        // _ Finishing list activity
        assert getSupportFragmentManager().findFragmentById(R.id.video_detail_container) != null;
        if (getSupportFragmentManager().findFragmentById(R.id.video_detail_container) instanceof DetailEditFragment)
            ((DetailEditFragment) getSupportFragmentManager().findFragmentById(R.id.video_detail_container)).saveInfo();
        else
            throw new RuntimeException("Unexpected fragment edit mode");

        mEditFlag = false;
        return true; // Video info saved
    }

    private AlbumTable.Video mVideo; // Selected video from database (needed for location detail)
    private ImageView mGeolocation; // Geolocation image (needed for location detail)

    protected boolean mNewVideoAdded = false; // Flag to know if the new video has been added into the DB
    protected boolean mNewVideoSaved = false; // Flag to know if the new video has been saved or deleted (geolocation)

    //
    protected void restoreVideosAlbum(Bundle state) { // Restore album (manage video selection)
        if (state != null) {

            mVideoSelected = state.getInt(DATA_VIDEO_POSITION);
            mNewVideoAdded = state.getBoolean(DATA_NEW_VIDEO_ADDED);
            mNewVideoSaved = state.getBoolean(DATA_NEW_VIDEO_SAVED);

            mDetailTag = state.getString(DATA_VIDEO_DETAIL);
        } else if (getIntent().getExtras() != null) {

            if (getIntent().getExtras().containsKey(DATA_VIDEO_POSITION))
                mVideoSelected = getIntent().getIntExtra(DATA_VIDEO_POSITION, 0);
            if (getIntent().getExtras().containsKey(DATA_VIDEO_DETAIL))
                mDetailTag = getIntent().getStringExtra(DATA_VIDEO_DETAIL);

            // Needed with detail activity child
        }
    }

    protected void initializeDetailUI() { // Initialize detail UI (according selected video)

        // Get selected video
        mVideo = VideoListActivity.mVideos.get(mVideoSelected);

        // Set detail commands behavior
        ImageButton command = (ImageButton) findViewById(R.id.detail_player);
        assert command != null;
        command.setOnClickListener(this);
        command = (ImageButton) findViewById(R.id.detail_edit);
        assert command != null;
        command.setOnClickListener(this);
        command = (ImageButton) findViewById(R.id.detail_location);
        assert command != null;
        if (mVideo.isLocated())
            command.setOnClickListener(this);

        else { // Disable it
            command.setFocusable(false);
            command.setAlpha(0.6f);
        }
        command = (ImageButton) findViewById(R.id.detail_share);
        assert command != null;
        command.setOnClickListener(this);
        command = (ImageButton) findViewById(R.id.detail_trash);
        assert command != null;
        command.setOnClickListener(this);

        // Set geolocation image behavior (if needed)
        if (mVideo.isLocated()) {

            mGeolocation = (ImageView) findViewById(R.id.locate_user);
            assert mGeolocation != null;
            mGeolocation.setOnClickListener(this);
        }
    }

    protected void displayVideoDetail() { // Display video detail fragment

        Bundle arguments = new Bundle();
        arguments.putInt(AlbumActivity.DATA_VIDEO_POSITION, mVideoSelected);
        Fragment fragment;

        // Display expected video detail
        switch (mDetailTag) {

            case DetailPlayerFragment.TAG: // Player
                fragment = new DetailPlayerFragment();
                break;

            case DetailEditFragment.TAG: // Edit
                fragment = new DetailEditFragment();
                break;

            default: // Location

                fragment = new SupportMapFragment();
                ((SupportMapFragment) fragment).getMapAsync(this);

                // Set activity title accordingly
                ActionBar appBar = getSupportActionBar();
                if (appBar != null)
                    appBar.setTitle(mVideo.toString(this));

                // Display geolocation image
                assert mGeolocation != null;
                mGeolocation.setVisibility(View.VISIBLE);

                break;
        }
        fragment.setArguments(arguments);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.video_detail_container, fragment)
                .commit();
    }

    //////
    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putInt(DATA_VIDEO_POSITION, mVideoSelected);
        outState.putBoolean(DATA_NEW_VIDEO_ADDED, mNewVideoAdded);
        outState.putBoolean(DATA_NEW_VIDEO_SAVED, mNewVideoSaved);

        outState.putString(DATA_VIDEO_DETAIL, mDetailTag);

        super.onSaveInstanceState(outState);
    }

    //
    protected abstract void onDelete(); // Delete video entry from album (finish activity if no more video)

    protected abstract void onClose(); // Closing operation

    @Override
    public void onBackPressed() {
        onClose();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {

            onClose();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //////
    private GoogleMap mMap;

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;






        /*
        assert mVideo.isLocated();
        mVideo.getLatitude();
        mVideo.getLongitude();
        */




        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34f, 151f);
        mMap.addMarker(new MarkerOptions().position(sydney).title(getString(R.string.video_location)));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));




    }

    //////
    @Override
    public void onClick(View v) { // Manage change detail command events
        switch (v.getId()) {

            ////// Details
            case R.id.detail_player:
                saveEditingInfo();
                mDetailTag = DetailPlayerFragment.TAG;
                displayVideoDetail();
                break;

            case R.id.detail_edit:
                mDetailTag = DetailEditFragment.TAG;
                displayVideoDetail();
                break;

            case R.id.detail_location:
                saveEditingInfo();
                if (!mDetailTag.equals(TAG_FRAGMENT_LOCATION)) {

                    mDetailTag = TAG_FRAGMENT_LOCATION;
                    displayVideoDetail();
                }
                else {

                    // Location fragment already displayed so move camera to video location






                }
                break;

            case R.id.detail_share:















                break;

            case R.id.detail_trash:
                DisplayMessage.getInstance().alert(R.string.title_warning, R.string.confirm_delete,
                        null, true, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                onDelete(); // Delete selected video
                            }
                        });
                break;

            ////// Geolocation
            case R.id.locate_user:









                //assert mMap != null;
                assert (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED ||
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                                PackageManager.PERMISSION_GRANTED);
                //mMap.setMyLocationEnabled(true);








                break;
        }
    }
}
