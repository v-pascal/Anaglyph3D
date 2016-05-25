package com.studio.artaban.anaglyph3d.album.details;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
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
import android.widget.TextView;

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
        thumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Start activity that will display the video
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(new File(mVideo.getVideoFile())), "video/*");

                startActivity(Intent.createChooser(intent, getContext().getString(R.string.chose_player)));
            }
        });

        // Scale play image according panels displayed
        final ImageView play = (ImageView)rootView.findViewById(R.id.image_play);
        final Point screenSize = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(screenSize);

        float scale;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            scale = screenSize.y / 552f;
        else // Portrait (50% of the screen width)
            scale = screenSize.x / (float)(Constants.DRAWABLE_PLAY_SIZE << 1);

        play.setScaleX(scale);
        play.setScaleY(scale);

        // Fill video info
        TextView info = (TextView)rootView.findViewById(R.id.title);
        info.setText(mVideo.getTitle(getContext(), false, false));
        info = (TextView)rootView.findViewById(R.id.date);
        info.setText(mVideo.getDate(getContext()));
        info = (TextView)rootView.findViewById(R.id.duration);
        info.setText(mVideo.getDuration() + " sec");

        return rootView;
    }
}
