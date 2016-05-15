package com.studio.artaban.anaglyph3d.album;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.location.LocationServices;
import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.data.AlbumTable;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.dummy.DummyContent;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.Database;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.helpers.Storage;

import com.google.android.gms.common.api.GoogleApiClient;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * An activity representing a list of Videos. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link VideoDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class VideoListActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks {

    public static boolean mAddVideo = false; // Flag to check new video creation request
    private AlbumTable.Video mNewVideo;

    //////
    private Database mDB;
    private GoogleApiClient mGoogleApiClient;



    /////////////////////////////////////////


    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;













    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final List<DummyContent.DummyItem> mValues;

        public SimpleItemRecyclerViewAdapter(List<DummyContent.DummyItem> items) {
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
            holder.mItem = mValues.get(position);
            holder.mIdView.setText(mValues.get(position).id);
            holder.mContentView.setText(mValues.get(position).content);

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mTwoPane) {
                        Bundle arguments = new Bundle();
                        arguments.putString(VideoDetailFragment.ARG_ITEM_ID, holder.mItem.id);
                        VideoDetailFragment fragment = new VideoDetailFragment();
                        fragment.setArguments(arguments);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.video_detail_container, fragment)
                                .commit();
                    } else {
                        Context context = v.getContext();
                        Intent intent = new Intent(context, VideoDetailActivity.class);
                        intent.putExtra(VideoDetailFragment.ARG_ITEM_ID, holder.mItem.id);

                        context.startActivity(intent);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mIdView;
            public final TextView mContentView;
            public DummyContent.DummyItem mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mIdView = (TextView) view.findViewById(R.id.id);
                mContentView = (TextView) view.findViewById(R.id.content);
            }

            @Override
            public String toString() {
                return super.toString() + " '" + mContentView.getText() + "'";
            }
        }
    }











    //////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_list);

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
            else
                toolbar.setBackgroundColor(Color.argb(255,30,30,30)); // Default status bar color (API < 21)

            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Prepare location using Google API
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();

        // Open database
        mDB = new Database(this);
        mDB.open(true);

        // Check if new entry is requested
        if (mAddVideo) {

            // Rename and move thumbnail JPEG file into expected storage folder
            Date date = new Date();
            DateFormat dateFormat = new SimpleDateFormat(Constants.DATABASE_DATE_FORMAT);
            Storage.saveThumbnail(dateFormat.format(date) + Constants.PROCESS_JPEG_EXTENSION);

            // Add new video into the album
            mNewVideo = new AlbumTable.Video(0, null, null, date, Settings.getInstance().mDuration,
                    0f, 0f);

            mAddVideo = false;
        }

        // Check if at least one video is in the album
        List<AlbumTable.Video> videos = mDB.getAllEntries(AlbumTable.TABLE_NAME);
        if (videos == null) {

            setResult(Constants.RESULT_NO_VIDEO);
            finish();
            return;
        }











        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        RecyclerView recyclerView = (RecyclerView)findViewById(R.id.video_list);
        assert recyclerView != null;
        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(DummyContent.ITEMS));

        if (findViewById(R.id.video_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }











        // Check connection
        if (!getIntent().getBooleanExtra(Constants.DATA_CONNECTION_ESTABLISHED, false))
            setResult(Constants.RESULT_RESTART_CONNECTION); // Must restart connection (not connected)
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
    @Override public void onConnectionSuspended(int i) { }
    @Override
    public void onConnected(Bundle bundle) {

        Location curlocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if ((curlocation != null) && (mNewVideo != null)) {

            Logs.add(Logs.Type.I, "New video geolocation: " + curlocation.getLatitude() + " " +
                    curlocation.getLongitude());
            mNewVideo.setLocation(curlocation.getLatitude(), curlocation.getLongitude());
            mDB.update(AlbumTable.TABLE_NAME, mNewVideo);
        }
    }
}
