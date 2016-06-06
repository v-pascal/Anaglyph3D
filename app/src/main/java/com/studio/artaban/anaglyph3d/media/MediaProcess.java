package com.studio.artaban.anaglyph3d.media;

import com.studio.artaban.anaglyph3d.transfer.BufferRequest;

/**
 * Created by pascal on 06/06/16.
 * Frame & Video media process class
 */
public abstract class MediaProcess extends BufferRequest {

    public MediaProcess(char requestId) { super(requestId); }

    //
    protected int mTotalFrame = 1;
    protected int mProceedFrame = 0;

    protected int mFrameCount = 0;

    //////
    public final int getTotalFrame() { return mTotalFrame; }
    public final int getProceedFrame() { return mProceedFrame; }

    public final int getFrameCount() { return mFrameCount; }
}
