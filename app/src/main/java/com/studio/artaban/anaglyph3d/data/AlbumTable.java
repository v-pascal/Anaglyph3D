package com.studio.artaban.anaglyph3d.data;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.studio.artaban.anaglyph3d.helpers.Logs;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by pascal on 12/05/16.
 * Class of the album Database table
 */
public class AlbumTable implements IDataTable {

    public static class Video { // Album entry: Video

        private long id;

        private String title;
        private boolean _title = false;
        public void setTitle(String title) {
            this.title = title;
            _title = true;
        }
        private String description;
        private boolean _description = false;
        public void setDescription(String description) {
            this.description = description;
            _description = true;
        }
        private Date date;
        private short duration;
        private double latitude;
        private double longitude;

        //
        public Video(long id) { this.id = id; }
        public Video(long id, String title, String description, Date date, short duration,
                     double latitude, double longitude) {

            this.id = id;

            this.title = title;
            this.description = description;
            this.date = date;
            this.duration = duration;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    //
    @Override
    public int insert(SQLiteDatabase db, Object[] data) {

        int insertCount = 0;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.000");
        for (Video video: (Video[])data) {

            ContentValues values = new ContentValues();
            values.put(COLUMN_TITLE, video.title);
            values.put(COLUMN_DESCRIPTION, video.description);
            values.put(COLUMN_DATE, dateFormat.format(video.date));
            values.put(COLUMN_DURATION, video.duration);
            values.put(COLUMN_LATITUDE, video.latitude);
            values.put(COLUMN_LONGITUDE, video.longitude);

            if (db.insert(TABLE_NAME, null, values) != Constants.NO_DATA)
                ++insertCount;
        }
        return insertCount;
    }
    @Override
    public boolean update(SQLiteDatabase db, Object data) {

        ContentValues values = new ContentValues();
        Video video = (Video)data;
        if (video._title)
            values.put(COLUMN_TITLE, video.title);
        if (video._description)
            values.put(COLUMN_DESCRIPTION, video.description);

        return (db.update(TABLE_NAME, values, IDataTable.COLUMN_ID + "=?",
                new String[] { String.valueOf(video.id) }) == 1);
    }
    @Override
    public int delete(SQLiteDatabase db, int[] keys) {











        return 0;
    }

    @Override
    public <T> List<T> getAllEntries(SQLiteDatabase db) {











        return null;
    }

    //////
    public static final String TABLE_NAME = "Album";

    // Columns
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_DURATION = "duration";
    private static final String COLUMN_LATITUDE = "latitude";
    private static final String COLUMN_LONGITUDE = "longitude";

    private AlbumTable() { }
    public static AlbumTable newInstance() { return new AlbumTable(); }

    public static void create(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
                IDataTable.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_TITLE + " TEXT," +
                COLUMN_DESCRIPTION + " TEXT," +
                COLUMN_DATE + " TEXT NOT NULL," +
                COLUMN_DURATION + " INTEGER NOT NULL," +
                COLUMN_LATITUDE + " REAL," +
                COLUMN_LONGITUDE + " REAL" +
                ");");
    }
    public static void upgrade(SQLiteDatabase db) {

        Logs.add(Logs.Type.W, "Upgrade 'Album' table: old data will be destroyed");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        create(db);
    }
}
