package com.studio.artaban.anaglyph3d.data;

import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.os.Parcelable;

import com.studio.artaban.anaglyph3d.helpers.Logs;

import java.util.List;

/**
 * Created by pascal on 13/05/16.
 * Database table class
 */
public interface IDataTable {

    class DataField implements Parcelable {

        public static final String COLUMN_ID = "_id";
        public static final short COLUMN_INDEX_ID = 0;

        //
        protected boolean[] mUpdated;
        protected long id; // Primary key field

        public final long getId() { return this.id; }
        public final void setId(long id) { this.id = id; }

        //////
        public DataField(short count, long id) {

            Logs.add(Logs.Type.V, "count: " + count + ", id: " + id);
            mUpdated = new boolean[count];
            this.id = id;
        }

        //////
        public DataField(Parcel parcel) {

            Logs.add(Logs.Type.V, "parcel: " + parcel);
            mUpdated = new boolean[parcel.readInt()];
            this.id = parcel.readLong();
        }
        public static final Parcelable.Creator<DataField> CREATOR = new Creator<DataField>() {

            @Override public DataField createFromParcel(Parcel source) { return new DataField(source); }
            @Override public DataField[] newArray(int size) { return new DataField[size]; }
        };
        @Override public int describeContents() { return 0; }
        @Override
        public void writeToParcel(Parcel dest, int flags) {

            Logs.add(Logs.Type.V, "dest: " + dest + ", flags: " + flags);
            dest.writeInt(mUpdated.length);
            dest.writeLong(this.id);
        }
    }

    //////
    int insert(SQLiteDatabase db, Object[] data); // Insert data entries and return added entry count
    boolean update(SQLiteDatabase db, Object data); // Update data entry
    int delete(SQLiteDatabase db, long[] keys); // Delete data entries with keys and return deleted entry count

    int getEntryCount(SQLiteDatabase db); // Return table entry count
    <T> List<T> getAllEntries(SQLiteDatabase db); // Return table entries into object list

    //
    void create(SQLiteDatabase db);
    void upgrade(SQLiteDatabase db, int oldVersion, int newVersion);
}
