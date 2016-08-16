package com.frank.codescan.service;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import com.frank.codescan.Interface.IMainService;
import com.frank.codescan.Interface.IMainServiceCb;
import com.frank.codescan.Interface.LoginInfo;
import com.frank.codescan.data.Record;
import com.frank.codescan.utils.CodeScanUtil;
import com.frank.codescan.utils.CodeScanWorker;
import com.frank.codescan.utils.CodescanConst;
import com.frank.codescan.utils.RecordScanCache;

public class IMainServiceImpl extends IMainService.Stub {

    private static final String TAG = "CodeScan_IMainServiceImpl";
    private Context mContext;
    private Handler mHandler;
    private RemoteCallbackList<IMainServiceCb> mCallBackList =
            new RemoteCallbackList<IMainServiceCb>();

    public RemoteCallbackList<IMainServiceCb> getCallBackList() {
        return mCallBackList;
    }

    public IMainServiceImpl(Context context, Handler handler) {
        mContext = context;
        mHandler = handler;
    }

    @Override
    public void processScan(String result) throws RemoteException {
        Log.d(TAG, "processScan()");
        ContentValues record = prepareData(result);
        if (record.size() > 0) {
            Uri ret = mContext.getContentResolver().insert(CodescanConst.RECORD_URI, record);
            long rowId = 0;
            try {
                rowId = Long.valueOf(ret.getLastPathSegment());
            } catch (NumberFormatException ex) {
                Log.w(TAG, "get rowId error.");
                return;
            }
            Record r = new Record();
            long currentTime = record.getAsLong(CodescanConst.RecordColum.DATE);
            r.id = rowId;
            r.date = CodeScanUtil.getDate(currentTime);
            r.time = CodeScanUtil.getTime(currentTime);
            r.name = record.getAsString(CodescanConst.RecordColum.NAME);
            r.phoneNumber = record.getAsString(CodescanConst.RecordColum.PHONE_NUM);
            r.realTime = currentTime;
            RecordScanCache.addToRecordCache(r);
            if (CodeScanWorker.getInstance() != null) {
                CodeScanWorker.getInstance().send();
            }
        }
    }

    @Override
    public void syncLocalData() {
        Log.d(TAG, "syncLocalData()");
        if (CodeScanWorker.getInstance() != null) {
            CodeScanWorker.getInstance().send();
        }
    }

    @Override
    public void login(LoginInfo loginfo) {
        Log.d(TAG, "login()");
        if (CodeScanWorker.getInstance() != null) {
            CodeScanWorker.getInstance().login(loginfo);
        }
    }

    // QD is name_num. eg. Áõö­_13476817029
    private ContentValues prepareData(String result) {
        ContentValues values = new ContentValues();
        String[] records = null;
        try {
            records = result.split("_");
        } catch(NullPointerException e) {
            Log.w(TAG, "QD invalid");
        }
        long currentTime = System.currentTimeMillis();
        if (records != null && records.length == 2) {
            values.put(CodescanConst.RecordColum.NAME, records[0]);
            values.put(CodescanConst.RecordColum.PHONE_NUM, records[1]);
            values.put(CodescanConst.RecordColum.DATE, currentTime);
            values.put(CodescanConst.RecordColum.DEVICE_NUM, CodeScanUtil.getDeviceNum());
        }
        return values;
    }

    @Override
    public void registerCb(IMainServiceCb cb) throws RemoteException {
        if (mCallBackList != null) {
            mCallBackList.register(cb);
        }
    }

    @Override
    public void unRegisterCb(IMainServiceCb cb) throws RemoteException {
        if (mCallBackList != null) {
            mCallBackList.unregister(cb);
        }
    }

    @Override
    public void getLogin() throws RemoteException {
        Log.d(TAG, "getLogin()");
        Bundle bundle = CodeScanUtil.getLoginInfo(mContext);
        Message msg = new Message();
        msg.what = MainService.MSG_LOGIN_RESULT;
        msg.setData(bundle);
        mHandler.sendMessageDelayed(msg, 200);
    }

    @Override
    public void logout() throws RemoteException {
        Log.d(TAG, "logout()");
        Bundle bundle = CodeScanUtil.resetLoginInfo(mContext);
        Message msg = new Message();
        msg.what = MainService.MSG_LOGIN_RESULT;
        msg.setData(bundle);
        mHandler.sendMessageDelayed(msg, 200);
    }
}
