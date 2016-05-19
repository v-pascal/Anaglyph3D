package com.studio.artaban.anaglyph3d.album.details;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.album.AlbumActivity;
import com.studio.artaban.anaglyph3d.album.VideoListActivity;
import com.studio.artaban.anaglyph3d.data.AlbumTable;
import com.studio.artaban.anaglyph3d.data.Constants;

import java.io.File;

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

        // Load thumbnail image from storage
        final ImageView thumbnail = (ImageView)rootView.findViewById(R.id.image_thumbnail);
        Uri thumbnailFile = Uri.fromFile(new File(mVideo.getThumbnailFile()));
        thumbnail.setImageURI(thumbnailFile);

        // Scale play image according panels displayed
        final ImageView play = (ImageView)rootView.findViewById(R.id.image_play);
        final Point screenSize = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(screenSize);

        float scale;
        if (getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            scale = screenSize.y / 926f;
        else // Portrait (50% of the screen width)
            scale = screenSize.x / (float)(Constants.DRAWABLE_PLAY_SIZE << 1);

        play.setScaleX(scale);
        play.setScaleY(scale);













        return rootView;
    }
}
