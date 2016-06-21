package com.studio.artaban.anaglyph3d.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.os.Parcelable;

import com.studio.artaban.anaglyph3d.R;
import com.studio.artaban.anaglyph3d.helpers.Logs;
import com.studio.artaban.anaglyph3d.helpers.Storage;

import java.io.File;
import java.text.DateFormat;
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

    public static class Video extends DataField { /////////////////////////////// Album entry: Video

        private static final short FIELD_COUNT = 9;
        public static String getThumbnailFile(Date date) { // Return thumbnail file path based on a video date

            Logs.add(Logs.Type.V, "date: " + date);
            DateFormat dateFormat = new SimpleDateFormat(Constants.FILENAME_DATE_FORMAT);
            return Storage.DOCUMENTS_FOLDER + Storage.FOLDER_THUMBNAILS + File.separator +
                    dateFormat.format(date) + Constants.EXTENSION_JPEG;
        }
        public static String getVideoFile(Date date) { // Return video file path based on a video date

            Logs.add(Logs.Type.V, "date: " + date);
            DateFormat dateFormat = new SimpleDateFormat(Constants.FILENAME_DATE_FORMAT);
            return Storage.DOCUMENTS_FOLDER + Storage.FOLDER_VIDEOS + File.separator +
                    dateFormat.format(date) + Constants.EXTENSION_WEBM;
        }

        //
        private String title;
        public void setTitle(String title) {

            Logs.add(Logs.Type.V, "title: " + title);
            String titleToSave = title.trim();
            this.title = (!titleToSave.isEmpty())? titleToSave:null;
            mUpdated[COLUMN_INDEX_TITLE] = true;
        }
        private String description;
        public void setDescription(String description) {

            Logs.add(Logs.Type.V, "description: " + description);
            String descToSave = description.trim();
            this.description = (!descToSave.isEmpty())? descToSave:null;
            mUpdated[COLUMN_INDEX_DESCRIPTION] = true;
        }
        private Date date;
        private short duration;
        private boolean location;
        private double latitude;
        private double longitude;
        public void setLocation(double latitude, double longitude) {

            Logs.add(Logs.Type.V, "latitude: " + latitude + ", longitude: " + longitude);
            this.location = true;
            this.latitude = latitude;
            this.longitude = longitude;

            mUpdated[COLUMN_INDEX_LOCATION] = true;
            mUpdated[COLUMN_INDEX_LATITUDE] = true;
            mUpdated[COLUMN_INDEX_LONGITUDE] = true;
        }
        private int thumbnailWidth;
        private int thumbnailHeight;

        //////
        public Video(long id) { super(FIELD_COUNT, id); }
        public Video(long id, String title, String description, Date date, short duration, boolean location,
                     double latitude, double longitude, int thumbnailWidth, int thumbnailHeight) {

            super(FIELD_COUNT, id);
            Logs.add(Logs.Type.V, "title: " + title + ", description: " + description + ", date: " +
                    date + ", duration: " + duration + ", location: " + location + ", latitude: " +
                    latitude + ", longitude: " + longitude + ", thumbnailWidth: " +
                    thumbnailWidth + ", thumbnailHeight: " + thumbnailHeight);

            this.title = title;
            this.description = description;
            this.date = date;
            this.duration = duration;
            this.location = location;
            this.latitude = latitude;
            this.longitude = longitude;
            this.thumbnailWidth = thumbnailWidth;
            this.thumbnailHeight = thumbnailHeight;
        }

        //////
        public Video(Parcel parcel) {

            super(parcel);
            Logs.add(Logs.Type.V, "parcel: " + parcel);

            this.title = parcel.readString();
            this.description = parcel.readString();
            SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATABASE_DATE_FORMAT);
            try { this.date = dateFormat.parse(parcel.readString()); }
            catch (ParseException e) {
                Logs.add(Logs.Type.F, "Wrong date format");
                this.date = new Date();
            }
            this.duration = (short)parcel.readInt();
            boolean[] locationField = new boolean[1];
            parcel.readBooleanArray(locationField);
            this.location = locationField[0];
            this.latitude = parcel.readDouble();
            this.longitude = parcel.readDouble();
            this.thumbnailWidth = parcel.readInt();
            this.thumbnailHeight = parcel.readInt();
        }
        public static final Parcelable.Creator<Video> CREATOR = new Creator<Video>() {

            @Override public Video createFromParcel(Parcel source) { return new Video(source); }
            @Override public Video[] newArray(int size) { return new Video[size]; }
        };
        @Override
        public void writeToParcel(Parcel dest, int flags) {

            super.writeToParcel(dest, flags);
            Logs.add(Logs.Type.V, "dest: " + dest + ", flags: " + flags);

            dest.writeString(this.title);
            dest.writeString(this.description);
            dest.writeString(getDateString(true));
            dest.writeInt(this.duration);
            dest.writeBooleanArray(new boolean[]{this.location});
            dest.writeDouble(this.latitude);
            dest.writeDouble(this.longitude);
            dest.writeInt(this.thumbnailWidth);
            dest.writeInt(this.thumbnailHeight);
        }

        //
        public String getTitle(Context context) { // Activity title

            Logs.add(Logs.Type.V, "context: " + context);
            DateFormat dateFormat = new SimpleDateFormat(context.getString(R.string.title_detail_format));
            String title = (this.title != null)? this.title:context.getString(R.string.undefined);
            return title + " (" + dateFormat.format(this.date) + ")";
        }
        public String getTitle(Context context, boolean duration, boolean edit) { // Video title

            Logs.add(Logs.Type.V, "context: " + context + ", duration: " + duration + ", edit: " + edit);
            if (edit)
                return this.title;

            String title = (this.title != null)? this.title:context.getString(R.string.undefined);
            return title + ((duration)? " (" + this.duration + " sec)":"");
        }
        public Date getDate() { return this.date; }
        public String getDateString(boolean db) {

            Logs.add(Logs.Type.V, "db: " + db);
            SimpleDateFormat dateFormat = new SimpleDateFormat((db)?
                    Constants.DATABASE_DATE_FORMAT:Constants.FILENAME_DATE_FORMAT);
            return dateFormat.format(this.date);
        }
        public String getDate(Context context) { // Video list date: MM/DD/yyyy - hh:mm

            Logs.add(Logs.Type.V, "context: " + context);
            DateFormat dateFormat = new SimpleDateFormat(context.getString(R.string.date_detail_format));
            return dateFormat.format(this.date);
        }
        public short getDuration() { return this.duration; }
        public String getDescription(Context context, boolean edit) {

            Logs.add(Logs.Type.V, "context: " + context + ", edit: " + edit);
            if (edit)
                return this.description;

            return (this.description != null)? this.description:context.getString(R.string.undefined);
        }
        public int getThumbnailWidth() { return this.thumbnailWidth; }
        public int getThumbnailHeight() { return this.thumbnailHeight; }

        public boolean isLocated() { return this.location; }
        public double getLatitude() { return this.latitude; }
        public double getLongitude() { return this.longitude; }

        public String getThumbnailFile() { return getThumbnailFile(this.date); }
        public String getVideoFile() { return getVideoFile(this.date); }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    //
    @Override
    public int insert(SQLiteDatabase db, Object[] data) {

        Logs.add(Logs.Type.V, "db: " + db + ", data: " + ((data != null)? data.length:"null"));

        int insertCount = 0;
        SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATABASE_DATE_FORMAT);
        for (Video video: (Video[])data) {

            ContentValues values = new ContentValues();
            values.put(COLUMN_TITLE, video.title);
            values.put(COLUMN_DESCRIPTION, video.description);
            values.put(COLUMN_DATE, dateFormat.format(video.date));
            values.put(COLUMN_DURATION, video.duration);
            values.put(COLUMN_LOCATION, video.location);
            values.put(COLUMN_LATITUDE, video.latitude);
            values.put(COLUMN_LONGITUDE, video.longitude);
            values.put(COLUMN_THUMBNAIL_WIDTH, video.thumbnailWidth);
            values.put(COLUMN_THUMBNAIL_HEIGHT, video.thumbnailHeight);

            if (db.insert(TABLE_NAME, null, values) != Constants.NO_DATA)
                ++insertCount;
        }
        return insertCount;
    }
    @Override
    public boolean update(SQLiteDatabase db, Object data) {

        Logs.add(Logs.Type.V, "db: " + db + ", data: " + data);

        ContentValues values = new ContentValues();
        Video video = (Video)data;
        if (video.mUpdated[COLUMN_INDEX_TITLE])
            values.put(COLUMN_TITLE, video.title);
        if (video.mUpdated[COLUMN_INDEX_DESCRIPTION])
            values.put(COLUMN_DESCRIPTION, video.description);
        if (video.mUpdated[COLUMN_INDEX_LATITUDE] || video.mUpdated[COLUMN_INDEX_LONGITUDE]) {
            values.put(COLUMN_LOCATION, video.location);
            values.put(COLUMN_LATITUDE, video.latitude);
            values.put(COLUMN_LONGITUDE, video.longitude);
        }

        if (values.size() == 0) {
            Logs.add(Logs.Type.W, "No field to update");
            return false;
        }
        return (db.update(TABLE_NAME, values, DataField.COLUMN_ID + "=?",
                new String[] { String.valueOf(video.id) }) == 1);
    }
    @Override
    public int delete(SQLiteDatabase db, long[] keys) {

        Logs.add(Logs.Type.V, "db: " + db + ", data: " + ((keys != null)? keys.length:"null"));

        int deleteCount = 0;
        if (keys == null) // Remove all entries
            deleteCount = db.delete(TABLE_NAME, "1", null);

        else {
            for (long key: keys)
                deleteCount += db.delete(TABLE_NAME, DataField.COLUMN_ID + "=?",
                        new String[] { String.valueOf(key) });
        }
        return deleteCount;
    }

    @Override
    public int getEntryCount(SQLiteDatabase db) {

        Logs.add(Logs.Type.V, "db: " + db);
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME, null);
        cursor.moveToNext();
        return cursor.getInt(0);
    }
    @Override
    public <T> List<T> getAllEntries(SQLiteDatabase db) {

        Logs.add(Logs.Type.V, "db: " + db);
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, DataField.COLUMN_ID);
        List<Video> videos = new ArrayList<>();
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATABASE_DATE_FORMAT);

            while (cursor.moveToNext()) {
                videos.add(new Video(
                        cursor.getLong(DataField.COLUMN_INDEX_ID), // _id

                        cursor.getString(COLUMN_INDEX_TITLE), // Title
                        cursor.getString(COLUMN_INDEX_DESCRIPTION), // Description
                        dateFormat.parse(cursor.getString(COLUMN_INDEX_DATE)), // Date
                        cursor.getShort(COLUMN_INDEX_DURATION), // Duration
                        cursor.getInt(COLUMN_INDEX_LOCATION) == 1, // Location
                        cursor.getDouble(COLUMN_INDEX_LATITUDE), // Latitude
                        cursor.getDouble(COLUMN_INDEX_LONGITUDE), // Longitude
                        cursor.getShort(COLUMN_INDEX_THUMBNAIL_WIDTH), // Thumbnail width
                        cursor.getShort(COLUMN_INDEX_THUMBNAIL_HEIGHT) // Thumbnail height
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
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_DURATION = "duration";
    public static final String COLUMN_LOCATION = "location";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_THUMBNAIL_WIDTH = "thumbnailWidth";
    public static final String COLUMN_THUMBNAIL_HEIGHT = "thumbnailHeight";

    // Columns index
    private static final short COLUMN_INDEX_TITLE = 1; // DataField.COLUMN_INDEX_ID + 1
    private static final short COLUMN_INDEX_DESCRIPTION = 2;
    private static final short COLUMN_INDEX_DATE = 3;
    private static final short COLUMN_INDEX_DURATION = 4;
    private static final short COLUMN_INDEX_LOCATION = 5;
    private static final short COLUMN_INDEX_LATITUDE = 6;
    private static final short COLUMN_INDEX_LONGITUDE = 7;
    private static final short COLUMN_INDEX_THUMBNAIL_WIDTH = 8;
    private static final short COLUMN_INDEX_THUMBNAIL_HEIGHT = 9;

    private AlbumTable() { }
    public static AlbumTable newInstance() { return new AlbumTable(); }

    @Override
    public void create(SQLiteDatabase db) {

        Logs.add(Logs.Type.V, "db: " + db);
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
                DataField.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

                COLUMN_TITLE + " TEXT," +
                COLUMN_DESCRIPTION + " TEXT," +
                COLUMN_DATE + " TEXT NOT NULL," +
                COLUMN_DURATION + " INTEGER NOT NULL," +
                COLUMN_LOCATION + " INTEGER NOT NULL," +
                COLUMN_LATITUDE + " REAL," +
                COLUMN_LONGITUDE + " REAL," +
                COLUMN_THUMBNAIL_WIDTH + " INTEGER NOT NULL," +
                COLUMN_THUMBNAIL_HEIGHT + " INTEGER NOT NULL" +
                ");");
    }
    @Override
    public void upgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        Logs.add(Logs.Type.V, "db: " + db);
        Logs.add(Logs.Type.W, "Upgrade '" + TABLE_NAME + "' table from " + oldVersion + " to " +
                newVersion + " version: old data will be destroyed!");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        create(db);
    }
}
