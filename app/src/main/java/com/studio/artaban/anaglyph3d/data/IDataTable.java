package com.studio.artaban.anaglyph3d.data;

import android.database.sqlite.SQLiteDatabase;

import java.util.List;

/**
 * Created by pascal on 13/05/16.
 * Database table class
 */
public interface IDataTable {

    class DataField {

        public static final String COLUMN_ID = "_id";
        public static final short COLUMN_INDEX_ID = 0;

        //
        protected boolean[] mUpdated;
        protected long id; // Primary key field

        public final long getId() { return id; }

        //////
        public DataField(short count, long id) {

            mUpdated = new boolean[count];
            this.id = id;
        }
    }

    //////
    int insert(SQLiteDatabase db, Object[] data); // Insert data entries and return added entry count
    boolean update(SQLiteDatabase db, Object data); // Update data entry
    int delete(SQLiteDatabase db, long[] keys); // Delete data entries with keys and return deleted entry count

    <T> List<T> getAllEntries(SQLiteDatabase db); // Return table entries into object list

}
