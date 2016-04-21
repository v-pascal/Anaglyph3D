package com.studio.artaban.anaglyph3d.transfer;

import android.os.Bundle;

import java.io.ByteArrayOutputStream;

/**
 * Created by pascal on 24/03/16.
 * Connection request interface
 */
public interface ConnectRequest {

    ////// Request IDs
    char REQ_NONE = ' '; // Unknown request
    char REQ_SETTINGS = 'S'; // Request to update settings (class 'Settings')
    char REQ_ACTIVITY = 'A'; // Request to start process (class 'ActivityWrapper')
    char REQ_FRAME = 'F'; // Request to transfer pictures (class 'Frame')

    ////// Request received while waiting reply
    class PreviousMaster {

        public char mId = REQ_NONE;
        public byte mType = 0;
        //public String mMessage = null;
    };
    // Useful for master device when it replies to a pending request sent by the slave device
    // -> See 'previous' parameter of the 'getReply' method

    ///////////////////////////////////////////

    enum BufferType {

        NONE, // No buffer will be received or sent

        TO_RECEIVE, // A buffer will be sent by the remote device (after having sent a reply to the request)
        TO_SEND, // A buffer will be sent by the current device (as a reply to a request)
    };

    char getRequestId(); // Return request Id
    boolean getRequestMerge(); // Return if request can be merged (if request types are mask values)
    BufferType getRequestBuffer(byte type); // Return if a buffer will be received or sent

    short getMaxWaitReply(byte type); // Return maximum delay to receive reply before disconnect

    String getRequest(byte type, Bundle data); // Return request message
    String getReply(byte type, String request, PreviousMaster previous); // Return reply of the request

    enum ReceiveResult {

        ////// Request & Buffer reply results
        ERROR, // Wrong receive reply result
        SUCCESS, // Receive reply successful or finish receiving buffer results
        // TODO: Add result here to inform connectivity loop to not receive request during sending buffer

        ////// Buffer reply result
        PARTIAL_PACKET, // Receive partial packet reply result
        PARTIAL // Receive partial buffer reply result
    };

    ReceiveResult receiveReply(byte type, String reply); // Receive the reply of the request sent
    ReceiveResult receiveBuffer(int size, ByteArrayOutputStream buffer); // Receive buffer sent
}
