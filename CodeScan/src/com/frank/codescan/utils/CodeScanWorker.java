package com.frank.codescan.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.frank.codescan.R;
import com.frank.codescan.Interface.LoginInfo;
import com.frank.codescan.service.MainService;

public class CodeScanWorker {
    private boolean isWorking = false;
    private RequestQueue mRequestQueue;
    private Context mContext;
    private static final String TAG = "CodeScan_CodeScanWorker";
    // 消息同步对应的listener begin
    private Listener<JSONObject> mReListener;
    private ErrorListener mReqErrorListener;
    // 消息同步对应的listener end
    // 消息同步对应的listener begin
    private Listener<JSONObject> mLoginListener;
    private ErrorListener mLoginErrorListener;
    // 消息同步对应的listener end
    private static final Object mIsWorkingLock = new Object();
    private static CodeScanWorker mInstance = null;
    private Handler mHandler;

    public static CodeScanWorker getInstance() {
        return mInstance;
    }

    public static void init(Context context, Handler handler) {
        mInstance = new CodeScanWorker(context, handler);
    }

    private CodeScanWorker(Context context, Handler handler) {
        mContext = context;
        mHandler = handler;
        mRequestQueue = Volley.newRequestQueue(context);
        mReListener = new Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if (CodescanConst.DEBUG) {
                    Log.d(TAG, "sync data onResponse():" + response);
                }
                Log.d(TAG, "sync data onResponse() success");
                // 0: failed
                // 1: success
                int success = 0;
                String name = "";
                long time = 0;
                String deviceNum = "-1";
                JSONArray contentArray = new JSONArray();
                JSONObject objectTmp = new JSONObject();
                ContentValues c = new ContentValues();
                try {
                    response = response
                            .getJSONObject(CodescanConst.HttpSyncDataColum.RESP_KEY);
                    contentArray = response
                            .getJSONArray(CodescanConst.HttpSyncDataColum.RESP_CONTENT);
                    for (int i = 0; i < contentArray.length(); i++) {
                        objectTmp = contentArray.getJSONObject(i);
                        success = objectTmp
                                .getInt(CodescanConst.HttpSyncDataColum.RESP_SUCCESS);
                        name = objectTmp
                                .getString(CodescanConst.HttpSyncDataColum.RESP_NAME);
                        time = objectTmp
                                .getLong(CodescanConst.HttpSyncDataColum.RESP_DATE);
                        deviceNum = objectTmp
                                .getString(CodescanConst.HttpSyncDataColum.RESP_DEVICE_PHONE);
                        Log.d(TAG, "success:" + success + ",name:" + name
                                + ",time:" + time);
                        c.clear();
                        c.put(CodescanConst.RecordColum.SUCCESS, success);
                        c.put(CodescanConst.RecordColum.DEVICE_NUM, deviceNum);
                        mContext.getContentResolver().update(
                                CodescanConst.RECORD_URI, c,
                                "name = ? and date = ?",
                                new String[] { name, String.valueOf(time) });
                    }
                } catch (JSONException ex) {
                    Log.e(TAG, "parse JSONObject in onResponse() error");
                }
                synchronized (mIsWorkingLock) {
                    isWorking = false;
                    if (CodescanConst.DEBUG) {
                        Log.d(TAG, "----3 isWorking = " + isWorking);
                    }
                }
                send();
            }
        };
        mReqErrorListener = new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.w(TAG, "sync error " + error.getMessage());
                synchronized (mIsWorkingLock) {
                    isWorking = false;
                    if (CodescanConst.DEBUG) {
                        Log.d(TAG, "----4 isWorking = " + isWorking);
                    }
                }
            }
        };
        mLoginListener = new Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                if (CodescanConst.DEBUG) {
                    Log.d(TAG, "login onResponse():" + response);
                }
                Log.d(TAG, "login onResponse() success.");
                boolean success = false;
                String position = mContext.getResources().getString(R.string.login_no_user);
                String deviceNum = "-1";
                try {
                    response = response
                            .getJSONObject(CodescanConst.HttpSyncDataColum.RESP_KEY);
                    success = response.getInt(CodescanConst.HttpSyncDataColum.RESP_SUCCESSED) == 1;
                    if (success) {
                        response = response.getJSONObject(CodescanConst.HttpSyncDataColum.RESP_CONTENT);
                        position = response.getString(CodescanConst.HttpSyncDataColum.RESP_SITE_NAME);
                        deviceNum = response.getString(CodescanConst.HttpSyncDataColum.RESP_DEVICE_PHONE);
                    }
                    Bundle bundle = CodeScanUtil.insertLoginInfo(mContext, deviceNum, position, success);
                    Message message = new Message();
                    message.what = MainService.MSG_LOGIN_RESULT;
                    message.setData(bundle);
                    mHandler.sendMessageDelayed(message, 100);
                } catch (JSONException ex) {
                    Log.e(TAG, "parse JSONObject in onResponse() error");
                }
            }
        };

        mLoginErrorListener = new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.w(TAG, "login error:" + error.getMessage());
            }
        };
    }

    public void send() {
        // TODO: 增加网络判断，如果联网，就可以发送，否则直接返回
        synchronized (mIsWorkingLock) {
            if (CodescanConst.DEBUG) {
                Log.d(TAG, "----1 isWorking = " + isWorking);
            }
            if (isWorking) {
                mHandler.sendEmptyMessageDelayed(
                        MainService.MSG_SYNC_DATA_LATER, 500);
                return;
            }
        }

        if (RecordScanCache.getCache().size() == 0) {
            mHandler.sendEmptyMessageDelayed(
                    MainService.MSG_RELOAD_SYNC_LOCAL_DATA, 500);
            return;
        }

        JsonObjectRequest request = CodeScanUtil.RequestSync(mReListener,
                mReqErrorListener);
        if (request != null) {
            synchronized (mIsWorkingLock) {
                Log.d(TAG, "send().Start to sync data.");
                isWorking = true;
                if (CodescanConst.DEBUG) {
                    Log.d(TAG, "----2 isWorking = " + isWorking);
                }
            }
            mRequestQueue.add(request);
        }
    }

    public void login(LoginInfo login) {
        Log.d(TAG, "call login()");
        JsonObjectRequest request = CodeScanUtil.RequestLogin(login, mLoginListener,
                mLoginErrorListener);
        mRequestQueue.add(request);
    }
}
