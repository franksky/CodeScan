package com.frank.codescan.utils;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.frank.codescan.R;
import com.frank.codescan.Interface.LoginInfo;
import com.frank.codescan.config.Config;
import com.frank.codescan.data.Record;
import com.frank.codescan.utils.CodescanConst.RecordColum;

public class CodeScanUtil {
    private static final String TAG = "CodeScan_CodeScanUtil";
    private static final SimpleDateFormat mDateFormat = new SimpleDateFormat(
            "MM-dd");
    private static final SimpleDateFormat mTimeFormat = new SimpleDateFormat(
            "HH:mm");
    // 登录用户信息文件名
    private static final String ACCOUNT_FILE_NAME = "account";
    // 保存登录信息的 sharepreferece 文件的 key值
    private static final String KEY_DEVICE_NUM = "key_device_num";
    // 保存登录信息的 sharepreferece 文件的 key值
    private static final String KEY_POSITION = "key_position";
    private static String mDeviceNum = "-1";

    public static String getDeviceNum() {
        return mDeviceNum;
    }
    public static Record getRecordFromCursor(Cursor c) {
        Record retVal = new Record();
        String name = "";
        long currentTime = 0;
        long id = 0;
        String phoneNumber = "";
        int success = 0;
        try {
            name = c.getString(c.getColumnIndexOrThrow(RecordColum.NAME));
            currentTime = c.getLong(c.getColumnIndexOrThrow(RecordColum.DATE));
            id = c.getLong(c.getColumnIndexOrThrow(RecordColum.ID));
            phoneNumber = c.getString(c
                    .getColumnIndexOrThrow(RecordColum.PHONE_NUM));
            success = c.getInt(c.getColumnIndexOrThrow(RecordColum.SUCCESS));
        } catch (IllegalArgumentException ex) {
            return retVal;
        }
        String date = getDate(currentTime);
        String time = getTime(currentTime);
        retVal.date = date;
        retVal.time = time;
        retVal.name = name;
        retVal.id = id;
        retVal.phoneNumber = phoneNumber;
        retVal.realTime = currentTime;
        retVal.success = success;
        return retVal;
    }

    public static ArrayList<Record> getAllRecord(Cursor c) {
        ArrayList<Record> ret = new ArrayList<Record>();
        if (c == null || c.getCount() == 0) {
            return ret;
        }
        while (c.moveToNext()) {
            Record record = getRecordFromCursor(c);
            ret.add(record);
        }
        return ret;
    }

    public static String getDate(long currentTime) {
        return mDateFormat.format(new Date(currentTime));
    }

    public static String getTime(long currentTime) {
        return mTimeFormat.format(new Date(currentTime));
    }

    // 获取同步消息的JsonObject
    private static JSONObject getSyncDataToHttp(Record r) throws JSONException {
        JSONObject ret = new JSONObject();
        if (!mDeviceNum.equals("-1")) {
            ret.put(CodescanConst.HttpSyncDataColum.REQ_SYSTEM_ID, Config.SYSTEM_ID);
            ret.put(CodescanConst.HttpSyncDataColum.REQ_NAME, r.name);
            ret.put(CodescanConst.HttpSyncDataColum.REQ_PHONE, r.phoneNumber);
            ret.put(CodescanConst.HttpSyncDataColum.REQ_DEVICE_PHONE, mDeviceNum);
            ret.put(CodescanConst.HttpSyncDataColum.REQ_DATE, r.realTime);
        }
        return ret;
    }

    public static JsonObjectRequest RequestSync(Listener<JSONObject> listener, ErrorListener errorListener){
        JSONArray arrayObject = new JSONArray();
        JsonObjectRequest request = null;
        int index = 0;
        int circleSize = RecordScanCache.getCache().size() < CodescanConst.MAX_SYNC_COUNT
                ? RecordScanCache.getCache().size()
                : CodescanConst.MAX_SYNC_COUNT;
        for (index = 0; index < circleSize ; index++) {
            Record r = RecordScanCache.getCache().get(0);
            try {
                JSONObject object = getSyncDataToHttp(r);
                arrayObject.put(index, object);
                RecordScanCache.getCache().remove(0);
            } catch (JSONException ex) {
                Log.w(TAG, "RequestSync error. getSyncDataToHttp() error");
            }
        }
        if (arrayObject.length() == 0) {
            Log.d(TAG, "RequestSync(), cache size is 0");
            return request;
        }
        JSONObject requsetObjectTmp = new JSONObject();
        JSONObject requsetObject = new JSONObject();
        try {
            requsetObjectTmp.put(CodescanConst.HttpSyncDataColum.REQ_CONTENT, arrayObject);
            requsetObject.put(CodescanConst.HttpSyncDataColum.REQ_KEY, requsetObjectTmp);
        } catch (JSONException e) {
            Log.w(TAG, "RequestSync error. requsetObject error");
        }
        if(CodescanConst.DEBUG) {
            Log.d(TAG, "RequestSync():" + requsetObject);
        }
        request = new JsonObjectRequest(CodescanConst.SYNC_SING_ACTION, requsetObject, listener, errorListener);
        return request;
    }

    // 获取默认的登录信息
    public static Bundle getLoginInfo(Context context) {
        SharedPreferences account = context.getSharedPreferences(ACCOUNT_FILE_NAME, Context.MODE_WORLD_READABLE);
        String deviceNum = account.getString(KEY_DEVICE_NUM, "-1");
        String position = account.getString(KEY_POSITION, context.getResources().getString(R.string.login_no_user));
        Bundle ret = new Bundle();
        ret.putString(CodescanConst.SERVICE_CB_KEY_LOGIN_DEVICE_NUM, deviceNum);
        ret.putString(CodescanConst.SERVICE_CB_KEY_LOGIN_POSITION, position);
        ret.putBoolean(CodescanConst.SERVICE_CB_KEY_LOGIN_SUCCESS, true);
        mDeviceNum = deviceNum;
        return ret;
    }

    // 写入登录信息 返回登录的bundle
    public static Bundle insertLoginInfo(Context context, String deviceNum, String position, boolean success) {
        SharedPreferences account = context.getSharedPreferences(ACCOUNT_FILE_NAME, Context.MODE_WORLD_WRITEABLE);
        Bundle ret = new Bundle();
        SharedPreferences.Editor editor = account.edit();
        editor.putString(KEY_DEVICE_NUM, deviceNum);
        editor.putString(KEY_POSITION, position);
        if (editor.commit()) {
            ret.putString(CodescanConst.SERVICE_CB_KEY_LOGIN_DEVICE_NUM, deviceNum);
            ret.putString(CodescanConst.SERVICE_CB_KEY_LOGIN_POSITION, position);
            ret.putBoolean(CodescanConst.SERVICE_CB_KEY_LOGIN_SUCCESS, success);
            mDeviceNum = deviceNum;
        }
        return ret;
    }

    public static Bundle resetLoginInfo(Context context) {
        SharedPreferences account = context.getSharedPreferences(ACCOUNT_FILE_NAME, Context.MODE_WORLD_WRITEABLE);
        Bundle ret = new Bundle();
        SharedPreferences.Editor editor = account.edit();
        editor.clear();
        editor.commit();
        ret.putString(CodescanConst.SERVICE_CB_KEY_LOGIN_DEVICE_NUM,  "-1");
        ret.putString(CodescanConst.SERVICE_CB_KEY_LOGIN_POSITION, context.getResources().getString(R.string.login_no_user));
        ret.putBoolean(CodescanConst.SERVICE_CB_KEY_LOGIN_SUCCESS, true);
        return ret;
    }

    public static JsonObjectRequest RequestLogin(LoginInfo loginfo, Listener<JSONObject> listener, ErrorListener errorListener) {
        Log.d(TAG, "RequestLogin()");
        JSONObject requsetObject = new JSONObject();
        JSONObject tmpRequsetObject = new JSONObject();
        try {
            tmpRequsetObject.put(CodescanConst.HttpSyncDataColum.REQ_SYSTEM_ID, Config.SYSTEM_ID);
            tmpRequsetObject.put(CodescanConst.HttpSyncDataColum.REQ_DEVICE_PHONE, loginfo.mDeviceNum);
            tmpRequsetObject.put(CodescanConst.HttpSyncDataColum.REQ_DEVICE_PASS, loginfo.mPassword);
            requsetObject.put(CodescanConst.HttpSyncDataColum.REQ_KEY, tmpRequsetObject);
        } catch (JSONException e) {
            Log.d(TAG, "RequestLogin prepare error.");
        }
        if(CodescanConst.DEBUG) {
            Log.d(TAG, "RequestLogin():" + requsetObject);
        }
        JsonObjectRequest request = new JsonObjectRequest(CodescanConst.GET_SITE_ACTION, requsetObject, listener, errorListener);
        return request;
    }

    public static Long getTodayMinTimeLong() {
        Calendar currentDay = Calendar.getInstance();
        currentDay.set(currentDay.get(Calendar.YEAR), currentDay.get(Calendar.MONTH), currentDay.get(Calendar.DAY_OF_MONTH),0,0,0);
        return currentDay.getTimeInMillis()/1000;
    }

    public static Long getTodayMaxTimeLong() {
        return getTodayMinTimeLong() + 3600 * 24;
    }

    public static Long getYesterdayMinTimeLong() {
        return getTodayMinTimeLong() - 3600 * 24;
    }

    public static Long getYesterdayMaxTimeLong() {
        return getTodayMinTimeLong() - 1;
    }

    public static Long getTomorrowMinTimeLong() {
        return getTodayMaxTimeLong() + 1 ;
    }

    public static Long getTomorrowMaxTimeLong() {
        return getTodayMaxTimeLong() + 3600 * 24;
    }
}
