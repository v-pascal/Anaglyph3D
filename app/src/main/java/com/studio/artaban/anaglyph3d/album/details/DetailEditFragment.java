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

    private AlbumActivity.OnVideoAlbumListener mEditListener;
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

        mEditListener.onSave(getArguments().getInt(AlbumActivity.DATA_VIDEO_POSITION, 0),
                mEditTitle.getText().toString(),
                mEditDescription.getText().toString(),
                null);
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

        mEditListener.setEditFlag(editable);

        mEditTitle.setFocusable(editable);
        mEditTitle.setFocusableInTouchMode(editable);
        mEditDescription.setFocusable(editable);
        mEditDescription.setFocusableInTouchMode(editable);

        if (editable)
            mEditTitle.requestFocus();
        else
            mCancelImage.setVisibility(View.GONE);
    }

    //////
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof AlbumActivity.OnVideoAlbumListener)
            mEditListener = (AlbumActivity.OnVideoAlbumListener)context;
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

        assert mEditListener != null;

        // Check if adding video process (and not already saved)
        if ((mEditListener.isVideoCreation()) && (!mEditListener.isVideoSaved())) {

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
        mEditListener = null;
    }

    //////
    @Override
    public void onClick(View v) {

        assert mEditListener != null;
        switch (v.getId()) {

            case R.id.image_edit: {
                final boolean editing = mEditListener.getEditFlag();

                v.clearAnimation();
                v.startAnimation(GrowthAnimation.create(new GrowthAnimation.OnTerminateListener() {
                    @Override
                    public void onAnimationTerminate() {

                        if (!editing)
                            mEditImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_save_white_36dp));
                        else
                            mEditImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_edit_white_36dp));
                        if (!mEditListener.isVideoCreation())
                            mCancelImage.setVisibility((!editing)? View.VISIBLE:View.GONE);
                        //else // Do not display cancel image for video creation
                    }
                }));

                if (!mEditListener.getEditFlag())
                    setEditMode(true); // Start editing

                else {

                    // Save info
                    setEditMode(false);
                    saveInfo();
                }
                break;
            }
            case R.id.image_cancel: {

                // Display user message according creation or cancel modifications
                int messageId = (mEditListener.isVideoCreation())?
                        R.string.confirm_cancel_create:R.string.confirm_cancel_modify;

                DisplayMessage.getInstance().alert(R.string.title_warning, messageId, null, true,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                // Hide soft keyboard (if displayed)
                                ActivityWrapper.hideSoftKeyboard();

                                if (mEditListener.isVideoCreation())
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
