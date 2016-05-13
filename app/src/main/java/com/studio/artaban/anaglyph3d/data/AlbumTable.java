package com.studio.artaban.anaglyph3d.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.studio.artaban.anaglyph3d.helpers.Logs;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by pascal on 12/05/16.
 * Class of the album Database table
 */
public class AlbumTable implements IDataTable {

    public static class Video extends DataField { // Album entry: Video

        private static final short FIELD_COUNT = 6;

        //
        private String title;
        public void setTitle(String title) {

            this.title = title;
            mUpdated[COLUMN_INDEX_TITLE] = true;
        }
        private String description;
        public void setDescription(String description) {

            this.description = description;
            mUpdated[COLUMN_INDEX_DESCRIPTION] = true;
        }
        private Date date;
        private short duration;
        private double latitude;
        private double longitude;

        //////
        public Video(long id) { super(FIELD_COUNT, id); }
        public Video(long id, String title, String description, Date date, short duration,
                     double latitude, double longitude) {

            super(FIELD_COUNT, id);

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
        SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATABASE_DATE_FORMAT);
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
        if (video.mUpdated[COLUMN_INDEX_TITLE])
            values.put(COLUMN_TITLE, video.title);
        if (video.mUpdated[COLUMN_INDEX_DESCRIPTION])
            values.put(COLUMN_DESCRIPTION, video.description);

        return (db.update(TABLE_NAME, values, DataField.COLUMN_ID + "=?",
                new String[] { String.valueOf(video.id) }) == 1);
    }
    @Override
    public int delete(SQLiteDatabase db, long[] keys) {















        return 0;
    }

    @Override
    public <T> List<T> getAllEntries(SQLiteDatabase db) {

        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, COLUMN_DATE, null);
        List<Video> videos = new ArrayList<>();
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATABASE_DATE_FORMAT.substring(0,
                    Constants.DATABASE_DATE_FORMAT.length() - 4));

            while (cursor.moveToNext()) {
                String dateField = cursor.getString(COLUMN_INDEX_DATE);
                videos.add(new Video(
                        cursor.getLong(DataField.COLUMN_INDEX_ID), // _id

                        cursor.getString(COLUMN_INDEX_TITLE), // Title
                        cursor.getString(COLUMN_INDEX_DESCRIPTION), // Description
                        dateFormat.parse(dateField.substring(0, dateField.length() - 4)), // Date
                        cursor.getShort(COLUMN_INDEX_DURATION), // Duration
                        cursor.getDouble(COLUMN_INDEX_LATITUDE), // Latitude
                        cursor.getDouble(COLUMN_INDEX_LONGITUDE) // Longitude
                ));
            }
        }
        catch (ParseException e) {
            Logs.add(Logs.Type.E, e.getMessage());
        }
        finally {
            cursor.close();
        }
        return (List<T>)videos;
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

    // Columns index
    private static final short COLUMN_INDEX_TITLE = 1; // DataField.COLUMN_INDEX_ID + 1
    private static final short COLUMN_INDEX_DESCRIPTION = 2;
    private static final short COLUMN_INDEX_DATE = 3;
    private static final short COLUMN_INDEX_DURATION = 4;
    private static final short COLUMN_INDEX_LATITUDE = 5;
    private static final short COLUMN_INDEX_LONGITUDE = 6;

    private AlbumTable() { }
    public static AlbumTable newInstance() { return new AlbumTable(); }

    public static void create(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
                DataField.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

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
