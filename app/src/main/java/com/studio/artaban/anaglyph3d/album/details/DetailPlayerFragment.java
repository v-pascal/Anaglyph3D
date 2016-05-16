package com.studio.artaban.anaglyph3d.album.details;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.album.AlbumActivity;
import com.studio.artaban.anaglyph3d.album.VideoListActivity;
import com.studio.artaban.anaglyph3d.data.AlbumTable;

/**
 * Created by pascal on 16/05/16.
 * Video player fragment
 */
public class DetailPlayerFragment extends Fragment {





    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    //public static final String ARG_ITEM_ID = "item_id";

    /**
     * The dummy content this fragment is presenting.
     */
    private AlbumTable.Video mItem;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public DetailPlayerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(AlbumActivity.ARG_VIDEO_POSITION)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.

            //mItem = Dummy Content.ITEM_MAP.get(getArguments().getString(ARG_ITEM_ID));
            mItem = VideoListActivity.mVideos.get(getArguments().getInt(AlbumActivity.ARG_VIDEO_POSITION, 0));






            Activity activity = this.getActivity();
            ActionBar appBar = ((AppCompatActivity)activity).getSupportActionBar();
            if (appBar != null)
                appBar.setTitle(mItem.toString(getActivity()));

            /*
            CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.toolbar_layout);
            if (appBarLayout != null) {
                appBarLayout.setTitle(mItem.content);
            }
            */






        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.video_detail, container, false);

        // Show the dummy content as text in a TextView.
        if (mItem != null) {
            ((TextView) rootView.findViewById(R.id.video_detail)).setText(mItem.toString(getActivity()));
        }

        return rootView;
    }
}
