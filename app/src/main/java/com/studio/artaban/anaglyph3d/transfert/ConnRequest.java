package com.studio.artaban.anaglyph3d.transfert;

import android.os.Bundle;

/**
 * Created by pascal on 24/03/16.
 * Connection request interface
 */
public interface ConnRequest {

    public static final String KEY_SETTINGS_REMOTE = "remoteDevice";
    public static final String KEY_SETTINGS_POSITION = "position";
    // Data keys

    boolean sendRequest(Bundle data);
    boolean receiveReply(String reply);
}
