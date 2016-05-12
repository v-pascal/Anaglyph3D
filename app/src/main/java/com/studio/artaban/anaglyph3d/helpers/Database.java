package com.studio.artaban.anaglyph3d.helpers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.studio.artaban.anaglyph3d.data.AlbumTable;

/**
 * Created by pascal on 12/05/16.
 * Database helper
 */
public class Database extends SQLiteOpenHelper {

    private static final String NAME = "anaglyph.db"; // Database name
    public static final int VERSION = 1; // Database version

    private SQLiteDatabase mDatabase;

    //
    public Database(Context context) { super(context, NAME, null, VERSION); }
    public void open(boolean write) {
        mDatabase = (write)? getWritableDatabase():getReadableDatabase();
    }

    //////
    @Override public void onCreate(SQLiteDatabase db) { AlbumTable.onCreate(db); }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        Logs.add(Logs.Type.W, "Upgrading DB from " + oldVersion + " to " + newVersion);
        AlbumTable.onUpgrade(db);
    }
}
