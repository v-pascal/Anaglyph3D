package com.studio.artaban.anaglyph3d.album.details;

import android.app.Activity;
import android.content.Intent;
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
import com.studio.artaban.anaglyph3d.helpers.Logs;

import java.io.File;

/**
 * Created by pascal on 16/05/16.
 * Video player fragment
 */
public class DetailPlayerFragment extends Fragment {

    public static final String TAG = "player";

    //
    private AlbumTable.Video mVideo;

    //////
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logs.add(Logs.Type.V, "savedInstanceState: " + savedInstanceState);

        // Get selected video
        mVideo = VideoListActivity.mVideos.get(getArguments().getInt(AlbumActivity.DATA_VIDEO_POSITION, 0));

        Activity activity = this.getActivity();
        ActionBar appBar = ((AppCompatActivity)activity).getSupportActionBar();
        if (appBar != null)
            appBar.setTitle(mVideo.getTitle(getContext()));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Logs.add(Logs.Type.V, null);
        View rootView = inflater.inflate(R.layout.video_detail_player, container, false);

        // Load thumbnail image from storage
        final ImageView thumbnail = (ImageView)rootView.findViewById(R.id.image_thumbnail);
        Uri thumbnailFile = Uri.fromFile(new File(mVideo.getThumbnailFile()));
        thumbnail.setImageURI(thumbnailFile);

        // Add click listener on the play video image
        final ImageView play = (ImageView)rootView.findViewById(R.id.image_play);
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logs.add(Logs.Type.V, null);

                // Start activity that will display the video
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(new File(mVideo.getVideoFile())), "video/*");

                startActivity(Intent.createChooser(intent, getContext().getString(R.string.chose_player)));
            }
        });

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
