package com.studio.artaban.anaglyph3d.transfer;

import android.os.Bundle;

/**
 * Created by pascal on 24/03/16.
 * Connection request interface
 */
public interface ConnectRequest {

    ////// Request IDs
    char REQ_NONE = ' '; // Unknown request
    char REQ_SETTINGS = 'S'; // Request to update settings (class 'Settings')
    char REQ_ACTIVITY = 'A'; // Request to know the remote activity status (class 'ActivityWrapper')

    ////// Request received while waiting reply
    class PreviousMaster {

        public char mId = REQ_NONE;
        public byte mType = 0;
        //public String mMessage = null;
    };
    // Useful for master device when it replies to a pending request sent by the slave device
    // -> See 'previous' parameter of the 'getReply' method

    ///////////////////////////////////////////

    char getRequestId(); // Return request Id
    short getMaxWaitReply(byte type); // Return maximum delay to receive reply before disconnect
    boolean getRequestMerge(); // Return if request can be merged (if request types are mask values)

    String getRequest(byte type, Bundle data); // Return request message
    String getReply(byte type, String request, PreviousMaster previous); // Return reply of the request

    boolean receiveReply(byte type, String reply); // Receive the reply of the request sent
}
