package com.studio.artaban.anaglyph3d.helpers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.studio.artaban.anaglyph3d.data.AlbumDB;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by pascal on 12/05/16.
 * Database helper
 */
public class DB extends SQLiteOpenHelper {

    private static final String NAME = "anaglyph.db"; // Database name
    public static final int VERSION = 1; // Database version

    public DB(Context context) { super(context, NAME, null, VERSION); }

    //
    public List<Object> getEntries(String table) {

        if (table.equals(AlbumDB.TABLE_NAME))
            return AlbumDB.getEntries(getReadableDatabase());

        Logs.add(Logs.Type.F, "Unexpected DB table name");
        return null;
    }

    //////
    @Override public void onCreate(SQLiteDatabase db) { AlbumDB.onCreate(db); }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        Logs.add(Logs.Type.W, "Upgrading DB from " + oldVersion + " to " + newVersion);
        AlbumDB.onUpgrade(db);
    }
}
