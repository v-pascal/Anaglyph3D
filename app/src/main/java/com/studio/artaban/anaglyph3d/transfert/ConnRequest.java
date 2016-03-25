package com.studio.artaban.anaglyph3d.transfert;

import android.os.Bundle;

/**
 * Created by pascal on 24/03/16.
 * Connection request interface
 */
public interface ConnRequest {

    public static final short REQ_SETTINGS = 1;
    // Request IDs

    String getRequestCmd(short type, Bundle data);
    short getRequestId();

    boolean sendReply(String request);
    boolean receiveReply(String reply);
}
