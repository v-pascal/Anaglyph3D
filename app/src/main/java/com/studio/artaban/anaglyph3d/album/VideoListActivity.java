package com.studio.artaban.anaglyph3d.album;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
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
        mDB.delete(AlbumTable.TABLE_NAME, new long[] { video.getId() });








        mNewVideo = null;
        mAddVideo = false;
        //refresh list/detail






    }









    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final List<AlbumTable.Video> mValues;

        public SimpleItemRecyclerViewAdapter(List<AlbumTable.Video> items) {
            mValues = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.video_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mPosition = position;






            holder.mTitleView.setText(String.valueOf(mValues.get(position).getId()));
            holder.mDateView.setText("testage");







            holder.mRootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {




                    // Change video selection
                    if (mTwoPane) {



                        // Fill video details



                        Bundle arguments = new Bundle();
                        //arguments.putString(DetailPlayerFragment.ARG_ITEM_ID, holder.mItem.id);
                        //arguments.putString(DetailPlayerFragment.ARG_ITEM_ID, String.valueOf(holder.mItem.getId()));
                        arguments.putInt(DetailPlayerFragment.ARG_ITEM_ID, holder.mPosition);
                        DetailPlayerFragment fragment = new DetailPlayerFragment();
                        fragment.setArguments(arguments);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.video_detail_container, fragment)
                                .commit();



                    }
                    else {



                        // Display video details



                        Context context = v.getContext();
                        Intent intent = new Intent(context, VideoDetailActivity.class);
                        //intent.putExtra(DetailPlayerFragment.ARG_ITEM_ID, holder.mItem.id);
                        //intent.putExtra(DetailPlayerFragment.ARG_ITEM_ID, String.valueOf(holder.mItem.getId()));
                        intent.putExtra(DetailPlayerFragment.ARG_ITEM_ID, holder.mPosition);

                        context.startActivity(intent);
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

        @Override
        public int getItemCount() {
            return mValues.size();
        }

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

            /*
            @Override
            public String toString() {
                return super.toString() + " '" + mContentView.getText() + "'";
            }
            */
        }
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
            appBar.setTitle(R.string.nav_album); // Needed when orientation change
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

            Date date5 = new Date();
            Date date4 = new Date(date5.getTime() - 360000);
            Date date3 = new Date(date4.getTime() - 360000);
            Date date2 = new Date(date3.getTime() - 360000);
            Date date1 = new Date(date2.getTime() - 360000);
            mDB.insert(AlbumTable.TABLE_NAME, new AlbumTable.Video[] {
                    new AlbumTable.Video(0, "Ma video #1", "Une video #1 pas du tout", date1, (short)10, 0f, 0f, 640, 480),
                    new AlbumTable.Video(0, "Ma video #2", "Une video #2 pas du tout", date2, (short)20, 0f, 0f, 640, 480),
                    new AlbumTable.Video(0, "Ma video #3", "Une video #3 pas du tout", date3, (short)30, 0f, 0f, 640, 480),
                    new AlbumTable.Video(0, "Ma video #4", "Une video #4 pas du tout", date4, (short)40, 0f, 0f, 640, 480),
                    new AlbumTable.Video(0, "Ma video #5", "Une video #5 pas du tout", date5, (short)50, 0f, 0f, 640, 480),
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
        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(mVideos));




        // Select video detail (if needed)
        if (mNewVideo != null) {


        }
        else if (mTwoPane) {


        }









        if (mTwoPane)
            setOnDetailListener();








    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {

            finish(); // Back to previous activity
            return true;
        }
        return super.onOptionsItemSelected(item);
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
