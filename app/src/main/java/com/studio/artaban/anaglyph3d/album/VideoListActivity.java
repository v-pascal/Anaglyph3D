package com.studio.artaban.anaglyph3d.album;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.location.LocationServices;
import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.album.details.DetailPlayerFragment;
import com.studio.artaban.anaglyph3d.data.AlbumTable;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.Database;
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;
import com.studio.artaban.anaglyph3d.helpers.Logs;

import com.google.android.gms.common.api.GoogleApiClient;
import com.studio.artaban.anaglyph3d.helpers.Storage;
import com.studio.artaban.anaglyph3d.media.Frame;

import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * Created by pascal on 16/05/16.
 * Videos list (album)
 */
public class VideoListActivity extends AlbumActivity implements
        GoogleApiClient.OnConnectionFailedListener, AlbumActivity.OnVideoAlbumListener {

    public static List<AlbumTable.Video> mVideos; // Album (videos list)

    //////
    private Database mDB; // Activity database
    private GoogleApiClient mGoogleApiClient; // Google API client
    private boolean mTwoPane; // Flag to know if displaying both list & details panels

    private boolean mSaveFromDetail; // Flag to know if saving video info from detail activity changes
                                     // -> In this case do not display user message (already done)
    //
    @Override
    public void onSave(int videoPosition, String title, String description) { // Save video detail
        AlbumTable.Video video = mVideos.get(videoPosition);
        assert video != null;

        // Check if saving a new video (new video creation request)
        boolean messageDisplayed = false;
        if (isVideoCreation()) {

            assert (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                            PackageManager.PERMISSION_GRANTED);
            Location curlocation = null;
            if (mGoogleApiClient.isConnected())
                curlocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (curlocation != null) {

                Logs.add(Logs.Type.I, "New video geolocation: " + curlocation.getLatitude() + " " +
                        curlocation.getLongitude());
                video.setLocation(curlocation.getLatitude(), curlocation.getLongitude());
            }
            else {
                DisplayMessage.getInstance().toast(R.string.no_location, Toast.LENGTH_LONG);
                messageDisplayed = true;
            }
        }
        video.setTitle(title);
        video.setDescription(description);
        mDB.update(AlbumTable.TABLE_NAME, video);

        if ((!messageDisplayed) && (!mSaveFromDetail))
            DisplayMessage.getInstance().toast(R.string.info_saved, Toast.LENGTH_SHORT);

        //////
        mNewVideoSaved = true;

        fillVideoList(mVideoSelected);
    }
    @Override
    public boolean onDelete() { // Delete video is requested or cancel video creation
        AlbumTable.Video video = mVideos.get(mVideoSelected);
        assert video != null;

        Logs.add(Logs.Type.W, "Deleting video: " + video.toString());
        mDB.delete(AlbumTable.TABLE_NAME, new long[] { video.getId() });

        //////
        if (isVideoCreation())
            mNewVideoSaved = true;

        return fillVideoList(0);
    }

    @Override public boolean isVideoCreation() { return (mNewVideoAdded && !mNewVideoSaved); }
    @Override public void setEditFlag(boolean flag) { mEditFlag = flag; }
    @Override public boolean getEditFlag() { return mEditFlag; }

    //
    @Override
    protected void onClose() {

        saveEditingInfo();
        finish();
    }

    //////
    public class AlbumRecyclerViewAdapter extends RecyclerView.Adapter<AlbumRecyclerViewAdapter.ViewHolder> {

        private final AppCompatActivity mActivity;
        private final List<AlbumTable.Video> mValues;

        public AlbumRecyclerViewAdapter(AppCompatActivity activity, List<AlbumTable.Video> videos) {
            mActivity = activity;
            mValues = videos;
        }

        //////
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_list_content,
                    parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {

            // Set up background and text video item colors
            if ((position % 2) != 0) {

                holder.mTitleView.setBackgroundColor(getResources().getColor(R.color.lighter_gray));
                holder.mTitleView.setTextColor(Color.YELLOW);
                holder.mDateView.setBackgroundColor(getResources().getColor(R.color.lighter_gray));
            }
            AlbumTable.Video video = mVideos.get(position);
            holder.mTitleView.setText(video.getTitle(mActivity, true));
            holder.mDateView.setText(video.getDate(mActivity));

            // Load thumbnail image
            Glide.with(mActivity)
                    .load(new File(mVideos.get(position).getThumbnailFile()))
                    .placeholder(R.drawable.no_thumbnail)
                    .into(holder.mThumbnailView);

            holder.mPosition = position;
            holder.mRootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View sender) { // Change video selection

                    mVideoSelected = holder.mPosition;
                    mDetailTag = DetailPlayerFragment.TAG;
                    if (!saveEditingInfo())
                        selectVideo(true);

                    else // The video list will be refreshed (no need to "select" the video)
                        mLastVideoSelected = mVideoSelected;

                    // NB: 'saveEditingInfo' method is called after 'mVideoSelected' assignment coz this
                    //     member is used to "select" the new video selection (see 'onSave' method)
                    //     -> "select" means manage red border selection (see 'selectVideo' method)

                    if (mTwoPane)
                        displayVideoDetail(); // Display detail of the selected video

                    else {

                        // Display video details activity
                        Intent intent = new Intent(mActivity, VideoDetailActivity.class);
                        intent.putExtra(AlbumActivity.DATA_VIDEO_POSITION, mVideoSelected);

                        mActivity.startActivityForResult(intent, 0);
                    }
                }
            });

            // Check if needed to "select" this video
            if (mVideoSelected == position) {

                holder.mRootView.setPadding(2, 2, 2, 2);
                holder.mRootView.setBackgroundColor(Color.RED);
            }
            // -> Needed when trying to add border to a video item which is not created
            //    yet (when calling 'selectVideo' method before the video item exists)
        }

        @Override public int getItemCount() { return mValues.size(); }

        //
        public class ViewHolder extends RecyclerView.ViewHolder {

            public final View mRootView;

            public final ImageView mThumbnailView; // Thumbnail image
            public final TextView mTitleView; // Title (duration)
            public final TextView mDateView; // Date & time

            public int mPosition;

            public ViewHolder(View view) {
                super(view);
                mRootView = view;

                mThumbnailView = (ImageView) view.findViewById(R.id.thumbnail);
                mTitleView = (TextView) view.findViewById(R.id.title);
                mDateView = (TextView) view.findViewById(R.id.date);
            }
        }
    }

    //
    private int mLastVideoSelected = Constants.NO_DATA; // Previous selected video position
    private RecyclerView mVideosView; // Recycler view containing videos list

    public void selectVideo(boolean user) { // Manage video list entry selection (border color)

        if (!user) // Not a user click event
            mVideosView.scrollToPosition(mVideoSelected);

        // Selection border of the Video item management
        View videoItem = mVideosView.getLayoutManager().findViewByPosition(mVideoSelected);
        if (videoItem != null) {

            videoItem.setPadding(2, 2, 2, 2);
            videoItem.setBackgroundColor(Color.RED);
        }
        if ((mLastVideoSelected != Constants.NO_DATA) && (mLastVideoSelected != mVideoSelected)) {

            // Un-select previous video
            videoItem = mVideosView.getLayoutManager().findViewByPosition(mLastVideoSelected);
            if (videoItem != null) {

                videoItem.setPadding(0, 0, 0, 0);
                videoItem.setBackgroundColor(Color.BLACK);
            }
        }
        mLastVideoSelected = mVideoSelected;
    }
    private boolean fillVideoList(int selectPosition) { // Fill video list recycler view

        mVideos = mDB.getAllEntries(AlbumTable.TABLE_NAME);

        mVideoSelected = selectPosition;
        if (isVideoCreation())
            mVideoSelected = mVideos.size() - 1; // Select last video

        // Check if at least one video is in the album
        if (mVideos.size() < 1) {

            setResult(Constants.RESULT_NO_VIDEO);
            finish();
            return false; // No video to display
        }
        mVideosView.setAdapter(new AlbumRecyclerViewAdapter(this, mVideos));
        return true;
    }

    //////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_list);

        // Set current activity
        ActivityWrapper.set(this);

        ActionBar appBar = getSupportActionBar();
        if (appBar != null) {
            if (Build.VERSION.SDK_INT >= 21) {
                getWindow().setNavigationBarColor(Color.BLACK);
                getWindow().setStatusBarColor(Color.BLACK);
            }
            else // Default status bar color (API < 21)
                appBar.setBackgroundDrawable(getResources().getDrawable(R.color.api_16_black));

            appBar.setDisplayHomeAsUpEnabled(true);
            appBar.setTitle(R.string.nav_album); // Needed when orientation has changed
        }

        // Restore videos album (manage video selection & info), and check connection for default result
        restoreVideosAlbum(savedInstanceState);
        if (!getIntent().getBooleanExtra(Constants.DATA_CONNECTION_ESTABLISHED, false))
            setResult(Constants.RESULT_RESTART_CONNECTION); // Must restart connection (not connected)

        // Prepare location using Google API
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // Open database & get video entries
        mDB = new Database(this);
        mDB.open(true);

        // Check if new entry is requested (and not already added)
        if ((getIntent().getBooleanExtra(Constants.DATA_ADD_VIDEO, false)) && (!mNewVideoAdded)) {

            // Rename and move thumbnail & video files into expected storage folders
            Date date = new Date();
            Storage.saveThumbnail(AlbumTable.Video.getThumbnailFile(date));
            Storage.saveVideo(AlbumTable.Video.getVideoFile(date));

            ////// Add new video into the album
            Bundle data = getIntent().getBundleExtra(Constants.DATA_ACTIVITY); // To get thumbnail resolution
            assert data != null;
            AlbumTable.Video newVideo = new AlbumTable.Video(0, null, null, date, Settings.getInstance().mDuration,
                    false, 0f, 0f, data.getInt(Frame.DATA_KEY_WIDTH), data.getInt(Frame.DATA_KEY_HEIGHT));

            mDB.insert(AlbumTable.TABLE_NAME, new AlbumTable.Video[]{newVideo});
            mNewVideoAdded = true;
        }

        mVideosView = (RecyclerView) findViewById(R.id.video_list);
        mTwoPane = findViewById(R.id.video_detail_container) != null;

        // Check if only videos list will be displayed with...
        if ((!mTwoPane) && ((isVideoCreation()) || // ...a creation request...
                (mVideoSelected != Constants.NO_DATA))) { // ...or if a video is already selected

            fillVideoList(mVideoSelected);
            mLastVideoSelected = mVideoSelected;

            // Display video details activity
            Intent intent = new Intent(this, VideoDetailActivity.class);
            intent.putExtra(AlbumActivity.DATA_VIDEO_POSITION, mVideoSelected);
            intent.putExtra(AlbumActivity.DATA_VIDEO_DETAIL, mDetailTag);

            startActivityForResult(intent, 0);
            return;
        }

        if (!fillVideoList(0))
            return; // No video to display

        // Select video detail (if needed)
        if ((mTwoPane) || (isVideoCreation())) {

            selectVideo(false);
            displayVideoDetail();
        }
        if (mTwoPane) // Check to add click events listener for detail commands
            addDetailClickListener();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        ActivityWrapper.set(this); // Set current activity

        if (requestCode != 0) {
            Logs.add(Logs.Type.F, "Unexpected request code");
            return;
        }
        switch (resultCode) {
            case Constants.RESULT_SAVE_VIDEO: { // Save from detail activity

                //assert !mTwoPane;
                mSaveFromDetail = true;
                onSave(mVideoSelected, data.getExtras().getString(VideoDetailActivity.DATA_VIDEO_TITLE),
                        data.getExtras().getString(VideoDetailActivity.DATA_VIDEO_DESCRIPTION));
                mSaveFromDetail = false;






                //select video







                break;
            }
            case Constants.RESULT_DELETE_VIDEO: { // Delete from detail activity

                //assert !mTwoPane;









                break;
            }
            case Constants.RESULT_SELECT_VIDEO: { // From portrait to landscape with two panels

                //assert mTwoPane;
                mVideoSelected = data.getExtras().getInt(AlbumActivity.DATA_VIDEO_POSITION, 0);
                mDetailTag = data.getExtras().getString(AlbumActivity.DATA_VIDEO_DETAIL,
                        DetailPlayerFragment.TAG);

                selectVideo(false);
                displayVideoDetail();
                break;
            }
        }
    }

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
    protected void onDestroy() {
        super.onDestroy();
        mDB.close();
    }

    //////
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Logs.add(Logs.Type.E, "Failed to connect to Google API Services: " +
                connectionResult.getErrorMessage());
    }
}
