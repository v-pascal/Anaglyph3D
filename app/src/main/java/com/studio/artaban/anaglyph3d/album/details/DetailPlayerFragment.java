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

    public static final String TAG = "player";
    private AlbumTable.Video mVideo;

    //////
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get selected video
        mVideo = VideoListActivity.mVideos.get(getArguments().getInt(AlbumActivity.DATA_VIDEO_POSITION, 0));

        Activity activity = this.getActivity();
        ActionBar appBar = ((AppCompatActivity)activity).getSupportActionBar();
        if (appBar != null)
            appBar.setTitle(mVideo.toString(getActivity()));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.video_detail_player, container, false);










        // Show the dummy content as text in a TextView.
        if (mVideo != null) {
            ((TextView) rootView.findViewById(R.id.video_detail)).setText(mVideo.toString(getActivity()));
        }










        return rootView;
    }
}
