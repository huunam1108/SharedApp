package com.example.app1;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MyDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = MyDatabaseHelper.class.getSimpleName();
    private static final String DATABASE_NAME = "setting.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "shared_table";

    public static final String COL_ID = "_id";
    public static final String COL_USER_NAME = "full_name";

    private static final String DATABASE_CREATE =
            "create table " + TABLE_NAME + " ( " + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_USER_NAME + " text NON NULL );";

    /**
     * Instantiates a new database.
     *
     * @param context the context
     */
    public MyDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Database is created");
        db.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}