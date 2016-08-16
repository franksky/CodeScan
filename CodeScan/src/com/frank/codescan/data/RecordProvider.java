package com.frank.codescan.data;

import com.frank.codescan.utils.CodescanConst;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.CancellationSignal;
import android.util.Log;

public class RecordProvider extends ContentProvider {
    private static final UriMatcher sMatch = new UriMatcher(UriMatcher.NO_MATCH);
    private static final String TAG = "CodeScan_RecordProvider";
    private CodeScanDatabaseHelper mHelper;

    private static final class MatchCode {
        public static final int RECORD_NORMAL = 1;
        public static final int RECORD_NUM = 2;
    }

    static {
        sMatch.addURI(CodescanConst.PROVIDER_AUTH, "record",
                MatchCode.RECORD_NORMAL);
        sMatch.addURI(CodescanConst.PROVIDER_AUTH, "record/#",
                MatchCode.RECORD_NUM);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int match = sMatch.match(uri);
        SQLiteDatabase db = mHelper.getWritableDatabase();
        String table;
        switch (match) {
            case MatchCode.RECORD_NORMAL:
                table = CodescanConst.TABLE_RECORD;
                break;
            default:
                Log.w(TAG, "Call insert unknown uri,uri:" + uri.toString());
                return null;
        }
        long rowId = db.insert(table, null, values);
        if (rowId > 0) {
            Uri ret = CodescanConst.RECODE_NOTIFY_URI.buildUpon()
                    .appendPath(String.valueOf(rowId)).build();
            getContext().getContentResolver().notifyChange(ret, null);
            return ret;
        }
        return null;
    }

    @Override
    public boolean onCreate() {
        mHelper = CodeScanDatabaseHelper.getInstance(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        int match = sMatch.match(uri);
        SQLiteDatabase db = mHelper.getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        Uri notifyUri = CodescanConst.RECODE_NOTIFY_URI;
        switch (match) {
            case MatchCode.RECORD_NORMAL:
                qb.setTables(CodescanConst.TABLE_RECORD);
                notifyUri = CodescanConst.RECORD_URI;
                break;
            default:
                Log.w(TAG, "Call query() unknown uri,uri:" + uri.toString());
                return null;
        }
        Cursor ret = qb.query(db, projection, selection, selectionArgs, null,
                null, sortOrder);
        ret.setNotificationUri(getContext().getContentResolver(),
                notifyUri);
        return ret;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        int match = sMatch.match(uri);
        SQLiteDatabase db = mHelper.getWritableDatabase();
        String extWhere = "";
        String table = "";
        switch (match) {
            case MatchCode.RECORD_NORMAL:
                table = CodescanConst.TABLE_RECORD;
                break;
            case MatchCode.RECORD_NUM:
                table = CodescanConst.TABLE_RECORD;
                extWhere = "_id = " + uri.getPathSegments().get(0);
                break;
            default:
                Log.w(TAG, "Call update() unknown uri,uri:" + uri.toString());
                return 0;
        }
        selection = DatabaseUtils.concatenateWhere(selection, extWhere);
        int count = db.update(table, values, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(CodescanConst.RECODE_NOTIFY_URI, null);
        return count;
    }
}
