package com.frank.codescan.data;

import com.frank.codescan.utils.CodescanConst;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class CodeScanDatabaseHelper extends SQLiteOpenHelper{

    private static final String DB_NAME = "CodeScan.db";
    private static CodeScanDatabaseHelper sInstance;
    private static final int DB_VERSION = 1;
    private Context mContext;
    synchronized static CodeScanDatabaseHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new CodeScanDatabaseHelper(context);
        }
        return sInstance;
    }
    public CodeScanDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createRecordTable(db);
    }

    private void createRecordTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " +  CodescanConst.TABLE_RECORD + " ("
                + " _id INTEGER PRIMARY KEY AUTOINCREMENT"
                + " , name TEXT, phone_num TEXT"
                + ", date INTEGER, success INTEGER DEFAULT 0"
                + ", device_num TEXT DEFAULT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //TODO:如果需要，在做
    }
}