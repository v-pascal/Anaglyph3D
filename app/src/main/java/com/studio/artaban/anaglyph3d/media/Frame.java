package com.studio.artaban.anaglyph3d.media;

import android.graphics.Bitmap;
import android.os.Bundle;

import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.Settings;
import com.studio.artaban.anaglyph3d.helpers.ActivityWrapper;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.helpers.Storage;
import com.studio.artaban.anaglyph3d.process.ProcessThread;
import com.studio.artaban.anaglyph3d.process.configure.ShiftActivity;
import com.studio.artaban.anaglyph3d.transfer.IConnectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by pascal on 21/04/16.
 * Frame class to manage frame transfer & conversion
 */
public class Frame extends MediaProcess {

    private static Frame ourInstance = new Frame();
    public static Frame getInstance() { return ourInstance; }
    private Frame() { super(IConnectRequest.REQ_FRAME); }

    // Data keys
    public static final String DATA_KEY_WIDTH = "width";
    public static final String DATA_KEY_HEIGHT = "height";
    public static final String DATA_KEY_REVERSE = "reverse";

    //////
    @Override
    public String getRequest(byte type, Bundle data) {

        Logs.add(Logs.Type.V, "type: " + type + ", data: " + data);
        switch (type) {
            case REQ_TYPE_DOWNLOAD: break;
            case REQ_TYPE_UPLOAD: return Constants.CONN_REQUEST_TYPE_ASK;
        }

        // Get picture data
        mWidth = data.getInt(DATA_KEY_WIDTH);
        mHeight = data.getInt(DATA_KEY_HEIGHT);
        mReverse = data.getBoolean(DATA_KEY_REVERSE);

        JSONObject request = getBufferRequest(type, data);
        if (request != null) {
            try {

                Logs.add(Logs.Type.I, "Assign data");
                request.put(DATA_KEY_WIDTH, mWidth);
                request.put(DATA_KEY_HEIGHT, mHeight);
                request.put(DATA_KEY_REVERSE, mReverse);

                return request.toString();
            }
            catch (JSONException e) {

                Logs.add(Logs.Type.E, e.getMessage());
                return null;
            }
        }
        return null;
    }
    @Override
    public String getReply(byte type, String request, PreviousMaster previous) {

        Logs.add(Logs.Type.V, "type: " + type + ", request: " + request + ", previous: " + previous);
        String result = getBufferReply(type, request);
        if (type == REQ_TYPE_UPLOAD)
            return result;

        Logs.add(Logs.Type.I, null);
        JSONObject picture;
        try { picture = new JSONObject(request); }
        catch (JSONException e) {

            Logs.add(Logs.Type.E, e.getMessage());
            return null;
        }

        try {
            mWidth = picture.getInt(DATA_KEY_WIDTH);
            mHeight = picture.getInt(DATA_KEY_HEIGHT);
            mReverse = picture.getBoolean(DATA_KEY_REVERSE);

            // Reply current reverse setting
            return ((Settings.getInstance().mReverse)?
                    Constants.CONN_REQUEST_ANSWER_TRUE:Constants.CONN_REQUEST_ANSWER_FALSE);
        }
        catch (JSONException e) {
            Logs.add(Logs.Type.E, e.getMessage());
        }
        return null;
    }

    @Override
    public ReceiveResult receiveReply(byte type, String reply) {

        Logs.add(Logs.Type.V, "type: " + type + ", reply: " + reply);
        mReverse = reply.equals(Constants.CONN_REQUEST_ANSWER_TRUE); // Receive remote reverse setting

        send();
        return ReceiveResult.SUCCESS;
    }

    //////
    private int mWidth; // Frame width
    private int mHeight; // Frame height
    private boolean mReverse; // Reverse picture

    public int getWidth() { return mWidth; }
    public int getHeight() { return mHeight; }
    public boolean getReverse() { return mReverse; }

    //
    public void convertFrames(final float shift, final float gushing) {
        // Convert video frames by adding simulated 3D configured by the user
        Logs.add(Logs.Type.V, "shift: " + shift + ", gushing: " + gushing);

        mProceedFrame = 0;
        mTotalFrame = 1;

        new Thread(new Runnable() {
            @Override
            public void run() {

                // Define progress bounds
                mFrameCount = Storage.getFrameFileCount(true);
                mTotalFrame = mFrameCount;

                //
                Bitmap frameBitmap = Bitmap.createBitmap(
                        Settings.getInstance().getResolutionWidth(),
                        Settings.getInstance().getResolutionHeight(), Bitmap.Config.ARGB_8888);

                byte[] buffer = new byte[frameBitmap.getByteCount()];

                // Apply simulated 3D configured by the user on each frame files
                for (int i = 0; i < mFrameCount; ++i) {

                    ++mProceedFrame;
                    String fileIndex = String.format("%04d", i);

                    // Get local frame buffer
                    File localFile = new File(ActivityWrapper.DOCUMENTS_FOLDER + File.separator +
                            Constants.PROCESS_LOCAL_PREFIX + fileIndex + Constants.EXTENSION_RGBA);
                    try {
                        if (new FileInputStream(localFile).read(buffer) != buffer.length)
                            throw new IOException();
                        frameBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(buffer));
                    }
                    catch (IOException e) {
                        Logs.add(Logs.Type.F, "Failed to load RGBA file: " + localFile.getAbsolutePath());
                        continue;
                    }

                    // Convert frame into simulated 3D frame & store it into buffer
                    Bitmap bitmap3D = ShiftActivity.applySimulation(frameBitmap, shift, gushing);
                    ByteBuffer buffer3D = ByteBuffer.wrap(buffer);
                    bitmap3D.copyPixelsToBuffer(buffer3D);

                    // Store new frame resolution
                    if (i == 0) {
                        mWidth = bitmap3D.getWidth();
                        mHeight = bitmap3D.getHeight();
                    }

                    try { // Save converted frame file
                        new FileOutputStream(localFile).write(buffer3D.array(), 0, bitmap3D.getByteCount());
                    }
                    catch (IOException e) {
                        Logs.add(Logs.Type.F, "Failed to save anaglyph frame file: " +
                                localFile.getAbsolutePath());
                    }
                }

            }
        }).start();
    }

    //////
    public enum Orientation {

        LANDSCAPE ((short)0),
        PORTRAIT ((short)1),
        REVERSE_LANDSCAPE ((short)2),
        REVERSE_PORTRAIT ((short)3);

        //
        private final short flipMethod; // GStreamer flip method (to apply orientation)
        Orientation(short method) { flipMethod = method; }
        public short getFlipMethod() { return flipMethod; }
    };
    public static boolean convertNV21toRGBA(String source, int width, int height,
                                            String destination, Orientation orientation) {

        Logs.add(Logs.Type.V, "source: " + source + ", width: " + width + ", height: " +
                height + ", destination: " + destination + ", orientation: " + orientation);

        int size = (width * height * 3) >> 1; // NV21 buffer size
        return ProcessThread.mGStreamer.launch("filesrc location=\"" + source + "\" blocksize=" + size +
                " ! video/x-raw,format=NV21,width=" + width + ",height=" + height + ",framerate=1/1" +
                " ! videoflip method=" + orientation.getFlipMethod() + " ! videoconvert" +
                " ! video/x-raw,format=RGBA ! filesink location=\"" + destination + "\"");
    }

    public static boolean convertRGBAtoJPEG(String source, int width, int height, String destination) {

        Logs.add(Logs.Type.V, "source: " + source + ", width: " + width + ", height: " +
                height + ", destination: " + destination);

        int size = (width * height) << 2; // RGBA buffer size
        return ProcessThread.mGStreamer.launch("filesrc location=\"" + source + "\" blocksize=" + size +
                " ! video/x-raw,format=RGBA,width=" + width + ",height=" + height + ",framerate=1/1" +
                " ! videoconvert ! jpegenc ! filesink location=\"" + destination + "\"");
    }
}
