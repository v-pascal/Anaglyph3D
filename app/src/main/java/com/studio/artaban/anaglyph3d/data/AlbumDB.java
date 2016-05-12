package com.studio.artaban.anaglyph3d.data;

import android.database.sqlite.SQLiteDatabase;

import com.studio.artaban.anaglyph3d.helpers.Logs;

import java.util.List;

/**
 * Created by pascal on 12/05/16.
 * Class of the album DB table
 */
public class AlbumDB implements TableDB {

    public static class Video { // Album entry: Video

        private String mID;
        private String mTitle;
        private String mDescription;
        private short mDuration;

        private double mLatitude;
        private double mLongitude;

        private String mPicturePath;






    }

    //
    @Override
    public List<Object> getEntries(SQLiteDatabase db) {






        return null;
    }
    @Override
    public Object getEntry(Object... keys) {







        return null;
    }

    //////
    public static final String TABLE_NAME = "Album";

    public static void onCreate(SQLiteDatabase db) {





        db.execSQL("");





    }
    public static void onUpgrade(SQLiteDatabase db) {

        Logs.add(Logs.Type.W, "Upgrade 'Album' table: old data will be destroyed");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}
