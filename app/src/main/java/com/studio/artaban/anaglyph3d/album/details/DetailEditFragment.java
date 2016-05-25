package com.studio.artaban.anaglyph3d.album.details;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.album.AlbumActivity;
import com.studio.artaban.anaglyph3d.album.VideoListActivity;
import com.studio.artaban.anaglyph3d.data.AlbumTable;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;
import com.studio.artaban.anaglyph3d.tools.GrowthAnimation;

/**
 * Created by pascal on 16/05/16.
 * Video description fragment (with edit action)
 */
public class DetailEditFragment extends Fragment implements View.OnClickListener {

    public static final String TAG = "edit";

    //////
    private AlbumActivity.OnVideoAlbumListener mVideoListener; // Activity video listener
    private int mVideoPosition; // Selected video position

    private EditText mEditTitle;
    private EditText mEditDescription;

    private ImageView mEditImage;
    private ImageView mCancelImage;

    //
    public void saveInfo() {

        VideoListActivity.mVideos.get(mVideoPosition).setTitle(mEditTitle.getText().toString());
        VideoListActivity.mVideos.get(mVideoPosition).setDescription(mEditDescription.getText().toString());
        fillTitle();

        mVideoListener.onSave(mVideoPosition);
    }

    private void fillTitle() {

        ActionBar appBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        if (appBar != null)
            appBar.setTitle(VideoListActivity.mVideos.get(mVideoPosition).toString(getActivity()));
    }
    private void fillInfo() {

        AlbumTable.Video video = VideoListActivity.mVideos.get(mVideoPosition);

        mEditTitle.setText(video.getTitle(getContext(), false, true));
        mEditDescription.setText(video.getDescription(getContext(), true));
    }
    private void setEditMode(boolean editable) { // Update UI components according edit mode

        mVideoListener.setEditFlag(editable);

        mEditTitle.setFocusable(editable);
        mEditTitle.setFocusableInTouchMode(editable);
        mEditDescription.setFocusable(editable);
        mEditDescription.setFocusableInTouchMode(editable);

        if (editable) {
            mEditTitle.requestFocus();
            mEditTitle.setSelection(mEditTitle.getText().length()); // Put cursor at end of line
        }
        else
            mCancelImage.setVisibility(View.GONE);
    }

    //////
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof AlbumActivity.OnVideoAlbumListener)
            mVideoListener = (AlbumActivity.OnVideoAlbumListener)context;
        else
            throw new RuntimeException(context.toString() + " must implement 'OnVideoAlbumListener'");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mVideoPosition = getArguments().getInt(AlbumActivity.DATA_VIDEO_POSITION, 0);

        // Set up activity title
        fillTitle();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.video_detail_edit, container, false);

        mEditTitle = (EditText)rootView.findViewById(R.id.edit_title);
        mEditDescription = (EditText)rootView.findViewById(R.id.edit_description);

        // Add click event listener to the edit & cancel images
        mEditImage = (ImageView)rootView.findViewById(R.id.image_edit);
        mEditImage.setOnClickListener(this);
        mCancelImage = (ImageView)rootView.findViewById(R.id.image_cancel);
        mCancelImage.setOnClickListener(this);

        fillInfo(); // Fill video info

        // Check if adding video process and not already saved or editing
        assert mVideoListener != null;
        if (((mVideoListener.isVideoCreation()) && (!mVideoListener.isVideoSaved())) ||
                (mVideoListener.getEditFlag())) {

            if (getArguments().containsKey(AlbumActivity.DATA_EDITING_TITLE))
                mEditTitle.setText(getArguments().getString(AlbumActivity.DATA_EDITING_TITLE));
            if (getArguments().containsKey(AlbumActivity.DATA_EDITING_DESCRIPTION))
                mEditDescription.setText(getArguments().getString(AlbumActivity.DATA_EDITING_DESCRIPTION));

            // Set edit mode immediately
            mEditImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_save_white_36dp));
            mCancelImage.setVisibility(View.VISIBLE);

            setEditMode(true);
        }
        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mVideoListener.getEditFlag())
            mVideoListener.onStore(mEditTitle.getText().toString(), mEditDescription.getText().toString());
            // Store editing data to keep info when orientation change
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mVideoListener = null;
    }

    //////
    @Override
    public void onClick(View v) {

        assert mVideoListener != null;
        switch (v.getId()) {

            case R.id.image_edit: {
                final boolean editing = mVideoListener.getEditFlag();

                v.clearAnimation();
                v.startAnimation(GrowthAnimation.create(new GrowthAnimation.OnTerminateListener() {
                    @Override
                    public void onAnimationTerminate() {

                        if (!editing)
                            mEditImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_save_white_36dp));
                        else
                            mEditImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_edit_white_36dp));
                        mCancelImage.setVisibility((!editing)? View.VISIBLE:View.GONE);
                        //else // Do not display cancel image for video creation
                    }
                }));

                if (!mVideoListener.getEditFlag())
                    setEditMode(true); // Start editing

                else {

                    ActivityWrapper.hideSoftKeyboard();

                    // Save info
                    setEditMode(false);
                    saveInfo();
                }
                break;
            }
            case R.id.image_cancel: {

                // Display user message according creation or cancel modifications
                int messageId = ((mVideoListener.isVideoCreation()) && (!mVideoListener.isVideoSaved()))?
                        R.string.confirm_cancel_create:R.string.confirm_cancel_modify;

                DisplayMessage.getInstance().alert(R.string.title_warning, messageId, null, true,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                ActivityWrapper.hideSoftKeyboard();

                                if ((mVideoListener.isVideoCreation()) && (!mVideoListener.isVideoSaved()))
                                    mVideoListener.onDelete(); // Cancel video creation

                                else { // Restore info

                                    setEditMode(false);
                                    mEditImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_edit_white_36dp));
                                    fillInfo();
                                }
                            }
                        });
                break;
            }
        }
    }
}
