package com.studio.artaban.anaglyph3d.transfer;

import android.os.Bundle;

/**
 * Created by pascal on 24/03/16.
 * Connection request interface
 */
public interface ConnectRequest {

    char REQ_NONE = ' ';
    char REQ_SETTINGS = 'S';
    ////// Request IDs

    ////// Request received while waiting reply
    class PreviousMaster {

        public char mId = REQ_NONE;
        public byte mType = 0;
        //public String mMessage = null;
    };
    // Useful for master device when it replies to a pending request sent by the slave device
    // -> See 'previous' parameter of the 'getReply' method

    ///////////////////////////////////////////

    char getRequestId();
    short getMaxWaitReply(byte type);

    String getRequest(byte type, Bundle data);
    String getReply(byte type, String request, PreviousMaster previous);

    boolean receiveReply(byte type, String reply);
}
