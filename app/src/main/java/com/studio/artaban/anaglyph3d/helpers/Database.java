package com.studio.artaban.anaglyph3d.helpers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.studio.artaban.anaglyph3d.data.AlbumTable;
import com.studio.artaban.anaglyph3d.data.Constants;
import com.studio.artaban.anaglyph3d.data.IDataTable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pascal on 12/05/16.
 * Database helper
 */
public class Database extends SQLiteOpenHelper {

    private static final String NAME = "anaglyph.db"; // Database name
    public static final int VERSION = 1; // Database version

    protected SQLiteDatabase mDatabase;

    protected static Map<String, IDataTable> mTableMap = new HashMap<>();
    static {
        mTableMap.put(AlbumTable.TABLE_NAME, AlbumTable.newInstance());
    }

    //////
    private boolean isReady() {
        if ((mDatabase == null) || (!mDatabase.isOpen())) {
            Logs.add(Logs.Type.W, "Database not opened");
            return false;
        }
        return true;
    }
    private boolean isWritable() {
        if (!isReady())
            return false;

        if (mDatabase.isReadOnly()) {
            Logs.add(Logs.Type.W, "Read only database opened");
            return false;
        }
        return true;
    }
    private boolean isTableExist(String name) {
        if (mTableMap.containsKey(name))
            return true;

        Logs.add(Logs.Type.W, "Data table '" + name + "' not defined");
        return false;
    }

    //
    public IDataTable getTable(String name) { return mTableMap.get(name); }
    public int insert(String table, Object[] data) {
        return (isWritable() && isTableExist(table))?
                mTableMap.get(table).insert(mDatabase, data):Constants.NO_DATA;
    }
    public boolean update(String table, Object data) {
        return (isWritable() && isTableExist(table) && mTableMap.get(table).update(mDatabase, data));
    }
    public int delete(String table, long[] keys) {
        return (isWritable() && isTableExist(table))?
                mTableMap.get(table).delete(mDatabase, keys):Constants.NO_DATA;
    }

    public <T> List<T> getAllEntries(String table) {
        if (!isReady() || !isTableExist(table))
            return null;

        return mTableMap.get(table).getAllEntries(mDatabase);
    }

    //////
    public Database(Context context) { super(context, NAME, null, VERSION); }
    public void open(boolean write) {
        mDatabase = (write)? getWritableDatabase():getReadableDatabase();
    }

    //////
    @Override public void onCreate(SQLiteDatabase db) {
        for (IDataTable table: mTableMap.values())
            table.create(db);
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        Logs.add(Logs.Type.W, "Upgrading DB from " + oldVersion + " to " + newVersion);
        for (IDataTable table: mTableMap.values())
            table.upgrade(db, oldVersion, newVersion);
    }
}
