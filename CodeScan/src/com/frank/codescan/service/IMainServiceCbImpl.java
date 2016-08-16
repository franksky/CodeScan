package com.frank.codescan.service;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.frank.codescan.Interface.IMainServiceCb;
import com.frank.codescan.utils.CodescanConst;

public class IMainServiceCbImpl extends IMainServiceCb.Stub{
    private static final String TAG = "CodeScan_IMainServiceCbImpl";
    private Handler mHandler;
    private Context mContext;

    public IMainServiceCbImpl(Context context, Handler handler) {
        mContext = context;
        mHandler = handler;
    }

    @Override
    public void postEvent(int event, Bundle bundle) throws RemoteException {
        Log.d(TAG, "postEvent(), event: " + event);
        Message msg = new Message();
        msg.what = event;
        msg.setData(bundle);
        if (mHandler != null) {
            mHandler.sendMessage(msg);
        }
    }
    
}