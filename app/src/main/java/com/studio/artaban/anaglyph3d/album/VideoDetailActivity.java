package com.studio.artaban.anaglyph3d.album;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.album.details.DetailPlayerFragment;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;

/**
 * Created by pascal on 16/05/16.
 * Display video detail (only)
 */
public class VideoDetailActivity extends AlbumActivity {

    //////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_detail);

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
        }





        // Check if orientation has changed with a large screen
        final Point screenSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(screenSize);
        if ((getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) &&
                (screenSize.x >= Constants.LARGE_SCREEN_WIDTH)) {

            // Display two panel in same activity
            Intent intent = new Intent();
            intent.putExtra(AlbumActivity.ARG_VIDEO_POSITION,
                    getIntent().getIntExtra(AlbumActivity.ARG_VIDEO_POSITION, 0));

            setResult(666, intent);
            finish();
            return;
        }





        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            Bundle arguments = new Bundle();
            //arguments.putString(DetailPlayerFragment.ARG_ITEM_ID,
            //        getIntent().getStringExtra(DetailPlayerFragment.ARG_ITEM_ID));
            arguments.putInt(AlbumActivity.ARG_VIDEO_POSITION,
                    getIntent().getIntExtra(AlbumActivity.ARG_VIDEO_POSITION, 0));
            DetailPlayerFragment fragment = new DetailPlayerFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.video_detail_container, fragment)
                    .commit();
        }








        setOnDetailListener();







    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            //navigateUpTo(new Intent(this, VideoListActivity.class));

            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
