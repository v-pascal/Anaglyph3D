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
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.Date;
import java.util.List;

/**
 * Created by pascal on 16/05/16.
 * Videos list (album)
 */
public class VideoListActivity extends AlbumActivity implements GoogleApiClient.OnConnectionFailedListener {

    public static boolean mAddVideo = false; // Flag to check new video creation request
    private static AlbumTable.Video mNewVideo = null; // New video (only when creation is requested)

    //////
    private Database mDB;
    private GoogleApiClient mGoogleApiClient;
    public static List<AlbumTable.Video> mVideos;

    private boolean mTwoPane; // Flag to know if displaying both panel: list & details

    //
    public void onSave(AlbumTable.Video video, String title, String description) { // Save video detail

        // Check if saving new video
        if (mNewVideo != null) {
            video = mNewVideo;

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
            else
                DisplayMessage.getInstance().toast(R.string.no_location, Toast.LENGTH_LONG);
        }
        video.setTitle(title);
        video.setDescription(description);
        mDB.update(AlbumTable.TABLE_NAME, video);








        mNewVideo = null;
        mAddVideo = false;
        //refresh list/detail







    }
    public void onDelete(AlbumTable.Video video) { // Delete video is requested or cancel video creation

        if (mNewVideo != null)
            video = mNewVideo; // Cancel video creation

        Logs.add(Logs.Type.W, "Deleting video: " + video.toString());
        mDB.delete(AlbumTable.TABLE_NAME, new long[]{video.getId()});








        mNewVideo = null;
        mAddVideo = false;
        //refresh list/detail






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
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mPosition = position;






            holder.mTitleView.setText(String.valueOf(mValues.get(position).getId()));
            holder.mDateView.setText("testage");







            holder.mRootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { // Change video selection

                    if (mTwoPane)
                        selectVideo(true, holder.mPosition); // Fill video details

                    else {

                        // Display video details activity
                        Intent intent = new Intent(mActivity, VideoDetailActivity.class);
                        intent.putExtra(AlbumActivity.ARG_VIDEO_POSITION, holder.mPosition);

                        mActivity.startActivityForResult(intent, 0);
                    }
                }
            });


            //Picasso pic;


            /*
            if (parseList.get(position).get("logo") != null) {
                    ParseFile image = (ParseFile) parseList.get(position).get("logo");
                    String url = image.getUrl();
                    Glide.with(context)
                            .load(url)
                            .placeholder(R.drawable.piwo_48)
                            .transform(new CircleTransform(context))
                            .into(holder.imageView);
                } else {
                    // make sure Glide doesn't load anything into this view until told otherwise
                    Glide.clear(holder.imageView);
                    // remove the placeholder (optional); read comments below
                    holder.imageView.setImageDrawable(null);
                }
                */


        }

        @Override public int getItemCount() { return mValues.size(); }

        //
        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mRootView;
            public final ImageView mThumbnailView;
            public final TextView mTitleView; // Title (duration)
            public final TextView mDateView; // Date & time

            public int mPosition;

            public ViewHolder(View view) {
                super(view);
                mRootView = view;




                mThumbnailView = null;
                mTitleView = (TextView) view.findViewById(R.id.id);
                mDateView = (TextView) view.findViewById(R.id.content);




            }
        }
    }

    //
    public void selectVideo(boolean user, int position) {




        if (user) {

            //recyclerView.scrollToPosition(mVideos.size() - 1);
            //recyclerView.scrollToPosition(position);

        }





        Bundle arguments = new Bundle();
        arguments.putInt(AlbumActivity.ARG_VIDEO_POSITION, position);

        DetailPlayerFragment fragment = new DetailPlayerFragment();
        fragment.setArguments(arguments);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.video_detail_container, fragment)
                .commit();






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

        // Prepare location using Google API
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // Open database & get video entries
        mDB = new Database(this);
        mDB.open(true);

        mVideos = mDB.getAllEntries(AlbumTable.TABLE_NAME);










        if (mVideos.size() < 1) {
            Logs.add(Logs.Type.E, "Insert videos");

            Date date20 = new Date();
            Date date19 = new Date(date20.getTime() - 360000);
            Date date18 = new Date(date19.getTime() - 360000);
            Date date17 = new Date(date18.getTime() - 360000);
            Date date16 = new Date(date17.getTime() - 360000);
            Date date15 = new Date(date16.getTime() - 360000);
            Date date14 = new Date(date15.getTime() - 360000);
            Date date13 = new Date(date14.getTime() - 360000);
            Date date12 = new Date(date13.getTime() - 360000);
            Date date11 = new Date(date12.getTime() - 360000);
            Date date10 = new Date(date11.getTime() - 360000);
            Date date9 = new Date(date10.getTime() - 360000);
            Date date8 = new Date(date9.getTime() - 360000);
            Date date7 = new Date(date8.getTime() - 360000);
            Date date6 = new Date(date7.getTime() - 360000);
            Date date5 = new Date(date6.getTime() - 360000);
            Date date4 = new Date(date5.getTime() - 360000);
            Date date3 = new Date(date4.getTime() - 360000);
            Date date2 = new Date(date3.getTime() - 360000);
            Date date1 = new Date(date2.getTime() - 360000);
            mDB.insert(AlbumTable.TABLE_NAME, new AlbumTable.Video[] {
                    new AlbumTable.Video(0, "Ma video #1", "Une video #1 pas mal du tout", date1, (short)10, 0f, 0f, 640, 480),
                    new AlbumTable.Video(0, "Ma video #2", "Une video #2 pas mal du tout", date2, (short)20, 0f, 0f, 640, 480),
                    new AlbumTable.Video(0, "Ma video #3", "Une video #3 pas mal du tout", date3, (short)30, 0f, 0f, 640, 480),
                    new AlbumTable.Video(0, "Ma video #4", "Une video #4 pas mal du tout", date4, (short)40, 0f, 0f, 640, 480),
                    new AlbumTable.Video(0, "Ma video #5", "Une video #5 pas mal du tout", date5, (short)50, 0f, 0f, 640, 480),
                    new AlbumTable.Video(0, "Ma video #6", "Une video #6 pas mal du tout", date6, (short)50, 0f, 0f, 640, 480),
                    new AlbumTable.Video(0, "Ma video #7", "Une video #7 pas mal du tout", date7, (short)50, 0f, 0f, 640, 480),
                    new AlbumTable.Video(0, "Ma video #8", "Une video #8 pas mal du tout", date8, (short)50, 0f, 0f, 640, 480),
                    new AlbumTable.Video(0, "Ma video #9", "Une video #9 pas mal du tout", date9, (short)50, 0f, 0f, 640, 480),
                    new AlbumTable.Video(0, "Ma video #10", "Une video #10 pas mal du tout", date10, (short)50, 0f, 0f, 640, 480),
                    new AlbumTable.Video(0, "Ma video #11", "Une video #11 pas mal du tout", date11, (short)50, 0f, 0f, 640, 480),
                    new AlbumTable.Video(0, "Ma video #12", "Une video #12 pas mal du tout", date12, (short)50, 0f, 0f, 640, 480),
                    new AlbumTable.Video(0, "Ma video #13", "Une video #13 pas mal du tout", date13, (short)50, 0f, 0f, 640, 480),
                    new AlbumTable.Video(0, "Ma video #14", "Une video #14 pas mal du tout", date14, (short)50, 0f, 0f, 640, 480),
                    new AlbumTable.Video(0, "Ma video #15", "Une video #15 pas mal du tout", date15, (short)50, 0f, 0f, 640, 480),
                    new AlbumTable.Video(0, "Ma video #16", "Une video #16 pas mal du tout", date16, (short)50, 0f, 0f, 640, 480),
                    new AlbumTable.Video(0, "Ma video #17", "Une video #17 pas mal du tout", date17, (short)50, 0f, 0f, 640, 480),
                    new AlbumTable.Video(0, "Ma video #18", "Une video #18 pas mal du tout", date18, (short)50, 0f, 0f, 640, 480),
                    new AlbumTable.Video(0, "Ma video #19", "Une video #19 pas mal du tout", date19, (short)50, 0f, 0f, 640, 480),
                    new AlbumTable.Video(0, "Ma video #20", "Une video #20 pas mal du tout", date20, (short)50, 0f, 0f, 640, 480),
            });
        }











        // Check if new entry is requested
        if ((mAddVideo) && (mNewVideo == null)) {

            // Rename and move thumbnail JPEG file into expected storage folder
            Date date = new Date();
            Storage.saveThumbnail(AlbumTable.Video.getThumbnailFile(date));

            ////// Add new video into the album
            Bundle data = getIntent().getBundleExtra(Constants.DATA_ACTIVITY); // To get thumbnail resolution
            assert data != null;
            mNewVideo = new AlbumTable.Video(0, null, null, date, Settings.getInstance().mDuration,
                    0f, 0f, data.getInt(Frame.DATA_KEY_WIDTH), data.getInt(Frame.DATA_KEY_HEIGHT));

            mDB.insert(AlbumTable.TABLE_NAME, new AlbumTable.Video[] { mNewVideo });

            // Assign new id created for this new video (which is the last entry added coz order by date)
            mVideos = mDB.getAllEntries(AlbumTable.TABLE_NAME);
            mNewVideo.setId(mVideos.get(mVideos.size() - 1).getId());
        }

        // Check if at least one video is in the album
        if (mVideos.size() < 1) {

            setResult(Constants.RESULT_NO_VIDEO);
            finish();
            return;
        }

        // Check connection
        if (!getIntent().getBooleanExtra(Constants.DATA_CONNECTION_ESTABLISHED, false))
            setResult(Constants.RESULT_RESTART_CONNECTION); // Must restart connection (not connected)











        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.video_list);
        assert recyclerView != null;
        mTwoPane = findViewById(R.id.video_detail_container) != null;
        recyclerView.setAdapter(new AlbumRecyclerViewAdapter(this, mVideos));




        // Select video detail (if needed)
        if (mNewVideo != null) {

            // Select last video



        }
        else if (mTwoPane) {

            // Select first video



        }
        if (mTwoPane)
            setOnDetailListener();















    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        ActivityWrapper.set(this); // Set current activity

        if (requestCode != 0) {
            Logs.add(Logs.Type.F, "Unexpected request code");
            return;
        }
        if (resultCode == Constants.RESULT_SELECT_VIDEO) {






            // data.getExtras().getInt(AlbumActivity.ARG_VIDEO_POSITION)







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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {

            finish(); // Back to previous activity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //////
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Logs.add(Logs.Type.E, "Failed to connect to Google API Services: " +
                connectionResult.getErrorMessage());
    }
}
