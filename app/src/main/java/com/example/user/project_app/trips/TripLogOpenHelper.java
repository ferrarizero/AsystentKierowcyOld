package com.example.user.project_app.trips;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class TripLogOpenHelper extends SQLiteOpenHelper {

    private static final String TAG = TripLogOpenHelper.class.getName();

    public TripLogOpenHelper(Context context) {
        super(context, TripLog.DATABASE_NAME, null, TripLog.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        execSQL(db, TripLog.DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    private void execSQL(SQLiteDatabase db, String[] statements) {
        final String tag = TAG + ".execSQL()";
        for (String sql : statements) {
            Log.d(tag, sql);
            db.execSQL(sql);
        }
    }
}
