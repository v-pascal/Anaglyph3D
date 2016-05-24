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

    private AlbumActivity.OnVideoAlbumListener mVideoListener;
    private AlbumTable.Video mVideo; // Selected video (DB)

    //////
    private EditText mEditTitle;
    private EditText mEditDescription;

    private ImageView mEditImage;
    private ImageView mCancelImage;

    //
    public void saveInfo() {

        mVideo.setTitle(mEditTitle.getText().toString());
        mVideo.setDescription(mEditDescription.getText().toString());
        fillTitle();

        mVideoListener.onSave(getArguments().getInt(AlbumActivity.DATA_VIDEO_POSITION, 0),
                mEditTitle.getText().toString(),
                mEditDescription.getText().toString());
    }

    private void fillTitle() {

        ActionBar appBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        if (appBar != null)
            appBar.setTitle(mVideo.toString(getActivity()));
    }
    private void fillInfo() {

        mEditTitle.setText(mVideo.getTitle(getContext(), false));
        mEditDescription.setText(mVideo.getDescription(getContext()));
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

        // Get selected video
        mVideo = VideoListActivity.mVideos.get(getArguments().getInt(AlbumActivity.DATA_VIDEO_POSITION, 0));

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

        assert mVideoListener != null;

        // Check if adding video process and not already saved or editing
        if (((mVideoListener.isVideoCreation()) && (!mVideoListener.isVideoSaved())) ||
                (mVideoListener.getEditFlag())) {

            // Set edit mode immediately
            mEditImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_save_white_36dp));
            mCancelImage.setVisibility(View.VISIBLE);

            setEditMode(true);
        }
        else // Fill info
            fillInfo();

        return rootView;
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
                                    ((AlbumActivity)getActivity()).onDelete(); // Cancel video creation

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
