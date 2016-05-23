package com.studio.artaban.anaglyph3d.album;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.album.details.DetailEditFragment;
import com.studio.artaban.anaglyph3d.album.details.DetailPlayerFragment;
import com.studio.artaban.anaglyph3d.data.AlbumTable;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.tools.GrowthAnimation;

/**
 * Created by pascal on 16/05/16.
 * Parent album activities (details selection & location detail management)
 */
public abstract class AlbumActivity extends AppCompatActivity implements
        View.OnClickListener, OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks, LocationListener {

    public static final String DATA_VIDEO_POSITION = "position";
    public static final String DATA_VIDEO_DETAIL = "detail";
    public static final String DATA_NEW_VIDEO_ADDED = "added";
    public static final String DATA_NEW_VIDEO_SAVED = "saved";

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

    ////// Location detail
    private static final String TAG_FRAGMENT_LOCATION = "location";
    private AlbumTable.Video mVideo; // Selected video from database

    private GoogleApiClient mGoogleApiClient; // Google API client
    private ImageView mGeolocationImage; // Geolocation image
    private Marker mGeolocationMarker; // Geolocation marker

    protected Location getGeolocation() {

        if (!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();

        assert (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED);
        Location curLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (curLocation == null)
            curLocation = mLastLocation; // Return last known location (if any)

        return curLocation;
    }

    //
    protected boolean mNewVideoAdded = false; // Flag to know if the new video has been added into the DB
    protected boolean mNewVideoSaved = false; // Flag to know if the new video has been saved or deleted (geolocation)

    protected void restoreVideosAlbum(Bundle state) { // Restore album (manage video selection)
        if (state != null) {

            mVideoSelected = state.getInt(DATA_VIDEO_POSITION);
            mNewVideoAdded = state.getBoolean(DATA_NEW_VIDEO_ADDED);
            mNewVideoSaved = state.getBoolean(DATA_NEW_VIDEO_SAVED);

            mDetailTag = state.getString(DATA_VIDEO_DETAIL);
        }
        else if (getIntent().getExtras() != null) {

            if (getIntent().getExtras().containsKey(DATA_VIDEO_POSITION))
                mVideoSelected = getIntent().getIntExtra(DATA_VIDEO_POSITION, 0);
            if (getIntent().getExtras().containsKey(DATA_VIDEO_DETAIL))
                mDetailTag = getIntent().getStringExtra(DATA_VIDEO_DETAIL);

            // Needed with detail activity child
        }
    }
    protected void initializeDetailUI() { // Initialize detail UI

        // Set detail commands behavior
        ImageButton command = (ImageButton) findViewById(R.id.detail_player);
        assert command != null;
        command.setOnClickListener(this);
        command = (ImageButton) findViewById(R.id.detail_edit);
        assert command != null;
        command.setOnClickListener(this);
        command = (ImageButton) findViewById(R.id.detail_share);
        assert command != null;
        command.setOnClickListener(this);
        command = (ImageButton) findViewById(R.id.detail_trash);
        assert command != null;
        command.setOnClickListener(this);

        // Set geolocation image behavior
        mGeolocationImage = (ImageView) findViewById(R.id.locate_user);
        assert mGeolocationImage != null;
        mGeolocationImage.setOnClickListener(this);

        // Prepare geolocation using Google API services
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        // Set detail UI according initial video selection
        updateDetailUI();
    }
    protected void updateDetailUI() { // Update detail UI (according new video selection)

        // Get selected video
        mVideo = VideoListActivity.mVideos.get(mVideoSelected);

        // Set location detail command behavior
        final ImageButton command = (ImageButton) findViewById(R.id.detail_location);
        assert command != null;
        if (mVideo.isLocated()) {

            command.setOnClickListener(this);
            command.setFocusable(true);
            command.setAlpha(1f);
        }
        else { // Disable it

            command.setOnClickListener(null);
            command.setFocusable(false);
            command.setAlpha(0.75f);
        }
    }
    protected void displayVideoDetail() { // Display video detail fragment

        Bundle arguments = new Bundle();
        arguments.putInt(AlbumActivity.DATA_VIDEO_POSITION, mVideoSelected);
        Fragment fragment;

        assert mGeolocationImage != null;
        mGeolocationImage.setVisibility(View.GONE);

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

                // Delete previous geolocation marker
                mGeolocationMarker = null;
                break;
        }
        fragment.setArguments(arguments);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.video_detail_container, fragment)
                .commit();
    }

    //////
    @Override
    protected void onStart() {
        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient != null)
            mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putInt(DATA_VIDEO_POSITION, mVideoSelected);
        outState.putBoolean(DATA_NEW_VIDEO_ADDED, mNewVideoAdded);
        outState.putBoolean(DATA_NEW_VIDEO_SAVED, mNewVideoSaved);

        outState.putString(DATA_VIDEO_DETAIL, mDetailTag);

        super.onSaveInstanceState(outState);
    }

    //
    public abstract void onDelete(); // Delete video entry from album (finish activity if no more video)
    protected abstract void onClose(); // Closing operation

    @Override public void onBackPressed() { onClose(); }
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
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Add a marker at video location and move the camera accordingly
        LatLng video = new LatLng(mVideo.getLatitude(), mVideo.getLongitude());
        mMap.addMarker(new MarkerOptions().position(video).title(getString(R.string.video_location)));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(video, 14));

        // Display geolocation image
        mGeolocationImage.setVisibility(View.VISIBLE);
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
                    LatLng video = new LatLng(mVideo.getLatitude(), mVideo.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(video, 14));
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

                // Display scale animation
                mGeolocationImage.clearAnimation();
                mGeolocationImage.startAnimation(GrowthAnimation.create());

                Location lastLocation = getGeolocation();
                if (lastLocation != null) {

                    LatLng userLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 18));

                    if (mGeolocationMarker != null)
                        mGeolocationMarker.remove();

                    // Add geolocation marker
                    mGeolocationMarker = mMap.addMarker(new MarkerOptions().position(userLatLng)
                            .title(getString(R.string.user_location))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                }
                else
                    DisplayMessage.getInstance().toast(R.string.no_geolocation, Toast.LENGTH_SHORT);

                break;
        }
    }

    //////
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Logs.add(Logs.Type.E, "Failed to connect to Google API Services: " + connectionResult.getErrorMessage());
    }
    @Override public void onConnectionSuspended(int arg0) { }
    @Override public void onConnected(Bundle arg0) {

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        assert (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
    }

    //////
    private Location mLastLocation;

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
    }
}
