package com.studio.artaban.anaglyph3d.transfer;

import android.os.Bundle;

/**
 * Created by pascal on 24/03/16.
 * Connection request interface
 */
public interface ConnRequest {

    public static final char REQ_SETTINGS = 'S';
    // Request IDs

    char getRequestId();
    String getRequest(byte type, Bundle data);
    String getReply(byte type, String request);

    boolean receiveReply(byte type, String reply);
}
