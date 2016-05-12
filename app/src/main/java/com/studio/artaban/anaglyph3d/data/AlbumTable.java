package com.studio.artaban.anaglyph3d.data;

import android.database.sqlite.SQLiteDatabase;

import com.studio.artaban.anaglyph3d.helpers.Logs;

/**
 * Created by pascal on 12/05/16.
 * Class of the album Database table
 */
public class AlbumTable {

    public static class Video { // Album entry: Video

        public String id;
        public String title;
        public String description;
        public short duration;

        public double latitude;
        public double longitude;

        public String picturePath;
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
