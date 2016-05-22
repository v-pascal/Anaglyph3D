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
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.EditText;
import android.widget.ImageView;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.album.AlbumActivity;
import com.studio.artaban.anaglyph3d.album.VideoListActivity;
import com.studio.artaban.anaglyph3d.data.AlbumTable;
import com.studio.artaban.anaglyph3d.helpers.DisplayMessage;

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

    private boolean mReverseAnim; // Flag to know if scale animation of the edit image has been reversed

    //
    public void saveInfo() {

        mVideo.setTitle(mEditTitle.getText().toString());
        mVideo.setDescription(mEditDescription.getText().toString());
        fillTitle();

        mEditListener.onSave(getArguments().getInt(AlbumActivity.DATA_VIDEO_POSITION, 0),
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
        mEditDescription.setText(mVideo.getDescription());
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

        // Fill info
        mEditTitle = (EditText)rootView.findViewById(R.id.edit_title);
        mEditDescription = (EditText)rootView.findViewById(R.id.edit_description);
        fillInfo();

        // Add click event listener to the edit & cancel images
        mEditImage = (ImageView)rootView.findViewById(R.id.image_edit);
        mEditImage.setOnClickListener(this);
        mCancelImage = (ImageView)rootView.findViewById(R.id.image_cancel);
        mCancelImage.setOnClickListener(this);

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

                // Display scale animation
                ScaleAnimation anim = new ScaleAnimation(1f, 1.4f, 1f, 1.4f, // From 1 to 1.4
                        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                anim.setDuration(300);
                anim.setRepeatCount(Animation.INFINITE);
                anim.setRepeatMode(Animation.REVERSE); // From 1.4 to 1
                anim.setAnimationListener(new Animation.AnimationListener() {

                    @Override public void onAnimationStart(Animation animation) { }
                    @Override public void onAnimationEnd(Animation animation) { }
                    @Override
                    public void onAnimationRepeat(Animation animation) {
                        if (mReverseAnim) {

                            animation.cancel(); // Animation terminated
                            if (!editing)
                                mEditImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_save_white_36dp));
                            else
                                mEditImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_edit_white_36dp));
                            if (!mEditListener.isVideoCreation())
                                mCancelImage.setVisibility((!editing)? View.VISIBLE:View.GONE);
                            //else // Do not display cancel image for video creation
                        }
                        else
                            mReverseAnim = true;
                    }
                });
                v.clearAnimation();
                mReverseAnim = false;
                v.startAnimation(anim);

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

                // Restore info
                DisplayMessage.getInstance().alert(R.string.title_warning, R.string.confirm_cancel,
                        null, true, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                setEditMode(false);
                                mEditImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_edit_white_36dp));
                                fillInfo();
                            }
                        });
                break;
            }
        }
    }
}
