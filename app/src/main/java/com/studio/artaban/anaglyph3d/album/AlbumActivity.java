package com.studio.artaban.anaglyph3d.album;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
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

import java.io.File;

/**
 * Created by pascal on 16/05/16.
 * Parent album activities (details selection & location detail management)
 */
public abstract class AlbumActivity extends AppCompatActivity implements
        View.OnClickListener, OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks, LocationListener {

    public static final String DATA_VIDEO_POSITION = "position";
    public static final String DATA_VIDEO_DETAIL = "detail";
    public static final String DATA_VIDEO_EDITING = "editing";
    public static final String DATA_NEW_VIDEO_ADDED = "newAdded";
    public static final String DATA_NEW_VIDEO_SAVED = "newSaved";

    public static final String DATA_EDITING_TITLE = "title";
    public static final String DATA_EDITING_DESCRIPTION = "description";

    public static final String DATA_VIDEOS_DOWNLOADED = "downloaded";

    //////
    public interface OnVideoAlbumListener { //////////////////////// Videos album listener interface

        void onSave(int videoPosition); // Save video detail
        void onStore(String title, String description); // Store video detail (orientation change)
        void onDelete(); // Delete video entry from album (finish activity if no more video)

        boolean isVideoCreation(); // Return if new video is selected
        boolean isVideoSaved(); // Return if new video details have been saved

        void setEditing(boolean flag);
        boolean getEditing();
        // Edit video detail member flag
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    //
    protected int mVideoSelected = Constants.NO_DATA; // Selected video position (or video to select)
    protected String mDetailTag = DetailPlayerFragment.TAG; // Fragment detail displayed (tag)

    protected boolean mEditing = false; // Flag to know if editing video info (title & description)
    public void setEditing(boolean flag) { mEditing = flag; }
    public boolean getEditing() { return mEditing; }

    protected boolean saveEditingInfo() { // Save video detail if editing when user changes detail displayed

        if (!mEditing)
            return false; // No editing info to save

        ////// Save video detail changes B4:
        // _ Loading new detail fragment
        // _ Finishing list activity
        assert getSupportFragmentManager().findFragmentById(R.id.video_detail_container) != null;
        if (getSupportFragmentManager().findFragmentById(R.id.video_detail_container) instanceof DetailEditFragment)
            ((DetailEditFragment) getSupportFragmentManager().findFragmentById(R.id.video_detail_container)).saveInfo();
        else
            throw new RuntimeException("Unexpected fragment edit mode");

        mEditing = false;
        return true; // Video info saved
    }

    protected String mEditTitle;
    protected String mEditDescription;
    // Editing video data to store when orientation change without having saved yet

    protected void onStore(String title, String description) { // Store video detail (orientation change)

        mEditTitle = title;
        mEditDescription = description;
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

        else if (mLastLocation == null)
            mLastLocation = curLocation; // Why not!

        return curLocation;
    }

    //
    protected boolean mNewVideoAdded = false; // Flag to know if the new video has been added into the DB
    protected boolean mNewVideoSaved = false; // Flag to know if the new video has been saved or deleted (geolocation)
    protected boolean isVideoCreation() {
        return (mNewVideoAdded && !mNewVideoSaved);
    }
    protected boolean mDownloadAdded = false;

    protected void restoreVideosAlbum(Bundle state) { // Restore album (manage video selection)
        if (state != null) {

            mVideoSelected = state.getInt(DATA_VIDEO_POSITION);
            mDetailTag = state.getString(DATA_VIDEO_DETAIL);
            mNewVideoAdded = state.getBoolean(DATA_NEW_VIDEO_ADDED);
            mNewVideoSaved = state.getBoolean(DATA_NEW_VIDEO_SAVED);

            mEditing = state.getBoolean(DATA_VIDEO_EDITING);
            mEditTitle = state.getString(DATA_EDITING_TITLE);
            mEditDescription = state.getString(DATA_EDITING_DESCRIPTION);

            mDownloadAdded = state.getBoolean(DATA_VIDEOS_DOWNLOADED);
        }
        else if (getIntent().getExtras() != null) {

            if (getIntent().getExtras().containsKey(DATA_VIDEO_POSITION))
                mVideoSelected = getIntent().getIntExtra(DATA_VIDEO_POSITION, 0);
            if (getIntent().getExtras().containsKey(DATA_VIDEO_DETAIL))
                mDetailTag = getIntent().getStringExtra(DATA_VIDEO_DETAIL);
            if (getIntent().getExtras().containsKey(DATA_NEW_VIDEO_ADDED))
                mNewVideoAdded = getIntent().getBooleanExtra(DATA_NEW_VIDEO_ADDED, false);
            if (getIntent().getExtras().containsKey(DATA_NEW_VIDEO_SAVED))
                mNewVideoSaved = getIntent().getBooleanExtra(DATA_NEW_VIDEO_SAVED, false);

            if (getIntent().getExtras().containsKey(DATA_VIDEO_EDITING))
                mEditing = getIntent().getBooleanExtra(DATA_VIDEO_EDITING, false);
            if (getIntent().getExtras().containsKey(DATA_EDITING_TITLE))
                mEditTitle = getIntent().getStringExtra(DATA_EDITING_TITLE);
            if (getIntent().getExtras().containsKey(DATA_EDITING_DESCRIPTION))
                mEditDescription = getIntent().getStringExtra(DATA_EDITING_DESCRIPTION);

            if (getIntent().getExtras().containsKey(DATA_VIDEOS_DOWNLOADED))
                mDownloadAdded = getIntent().getBooleanExtra(DATA_VIDEOS_DOWNLOADED, false);

            // Needed with detail activity child
        }

        // Prepare geolocation using Google API services
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
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
        mGeolocationImage.clearAnimation();
        mGeolocationImage.setVisibility(View.GONE);

        // Display expected video detail
        switch (mDetailTag) {

            case DetailPlayerFragment.TAG: // Player
                fragment = new DetailPlayerFragment();
                break;

            case DetailEditFragment.TAG: // Edit
                fragment = new DetailEditFragment();
                if (mEditing) {

                    arguments.putString(DATA_EDITING_TITLE, mEditTitle);
                    arguments.putString(DATA_EDITING_DESCRIPTION, mEditDescription);
                }
                break;

            default: // Location

                fragment = new SupportMapFragment();
                ((SupportMapFragment) fragment).getMapAsync(this);

                // Set activity title accordingly
                ActionBar appBar = getSupportActionBar();
                if (appBar != null)
                    appBar.setTitle(mVideo.getTitle(this));

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
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putInt(DATA_VIDEO_POSITION, mVideoSelected);
        outState.putString(DATA_VIDEO_DETAIL, mDetailTag);
        outState.putBoolean(DATA_NEW_VIDEO_ADDED, mNewVideoAdded);
        outState.putBoolean(DATA_NEW_VIDEO_SAVED, mNewVideoSaved);

        outState.putBoolean(DATA_VIDEO_EDITING, mEditing);
        outState.putString(DATA_EDITING_TITLE, mEditTitle);
        outState.putString(DATA_EDITING_DESCRIPTION, mEditDescription);

        outState.putBoolean(DATA_VIDEOS_DOWNLOADED, mDownloadAdded);

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

                Intent intent = new Intent(Intent.ACTION_SEND);
                Uri data = Uri.fromFile(new File(mVideo.getVideoFile()));

                intent.setDataAndType(data, "video/*");
                intent.putExtra(Intent.EXTRA_STREAM, data);
                intent.putExtra(ShareLocalActivity.EXTRA_SUBFOLDER, "Anaglyph-3D");
                intent.putExtra(ShareLocalActivity.EXTRA_OVERWRITE, true);

                // Start activity that will display where to share the video
                startActivity(Intent.createChooser(intent, getString(R.string.chose_share)));

                // NB: The 'ShareLocalActivity' to share the video into local folder will be
                //     listed too (using intent filter)
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
    private static Location mLastLocation; // Last known location

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
    }
}
