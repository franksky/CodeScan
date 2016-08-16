package com.frank.codescan.utils;


import java.util.ArrayList;
import java.util.HashMap;

import android.database.Cursor;

import com.frank.codescan.data.Record;

public class RecordScanCache {
    private static ArrayList<Record> sCache = new ArrayList<Record>();

    public synchronized static void loadRecord(Cursor c) {
        sCache.clear();
        sCache = CodeScanUtil.getAllRecord(c);
    }

    public synchronized static ArrayList<Record> getCache() {
        return sCache;
    }

    public synchronized static void addToRecordCache(Record record) {
        sCache.add(record);
    }
}