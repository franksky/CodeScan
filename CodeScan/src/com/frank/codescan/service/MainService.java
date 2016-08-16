package com.frank.codescan.service;

import com.frank.codescan.utils.CodeScanWorker;
import com.frank.codescan.utils.CodescanConst;
import com.frank.codescan.utils.RecordScanCache;

import android.app.Service;
import android.content.AsyncQueryHandler;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

public class MainService extends Service {
    private static final String TAG = "CodeScan_MainService";
    private IMainServiceImpl mService;
    public static final int MSG_SYNC_DATA_LATER        = 1;
    public static final int MSG_RELOAD_SYNC_LOCAL_DATA = 2;
    public static final int MSG_LOGIN_RESULT           = 3;

    AsyncQueryHandler myHandler;
    private Handler mSendHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (CodescanConst.DEBUG) {
                Log.d(TAG, "handleMessage msg.what:" + msg.what);
            }
            switch (msg.what) {
                case MSG_SYNC_DATA_LATER:
                    mSendHandler.removeMessages(MSG_SYNC_DATA_LATER);
                    if (CodeScanWorker.getInstance() != null) {
                        CodeScanWorker.getInstance().send();
                    }
                    break;
                case MSG_RELOAD_SYNC_LOCAL_DATA:
                    mSendHandler.removeMessages(MSG_RELOAD_SYNC_LOCAL_DATA);
                    myHandler.cancelOperation(0);
                    myHandler.startQuery(0, null, CodescanConst.RECORD_URI, null,
                            CodescanConst.RecordColum.SUCCESS + " = 0", null, null);
                    break;
                case MSG_LOGIN_RESULT:
                    // 这里需要把登录信息通过bundle带回去。
                    CbPostEvent(CodescanConst.SERVICE_CB_EVENT_LOGIN_RESULT, msg.getData());
                    break;
                default:
                    break;
            }
        }
    };

    // 回调 postEvent 给AP端
    private void CbPostEvent(int event, Bundle bundle) {
        int i = mService.getCallBackList().beginBroadcast();
        while (i > 0) {
            i--;
            try {
                mService.getCallBackList().getBroadcastItem(i).postEvent(event, bundle);
            } catch (RemoteException e) {
                // The RemoteCallbackList will take care of removing
                // the dead object for us.
            }
        }
        mService.getCallBackList().finishBroadcast();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mService = new IMainServiceImpl(getApplicationContext(), mSendHandler);
        CodeScanWorker.init(getApplicationContext(), mSendHandler);
        myHandler = new AsyncQueryHandler(getContentResolver()) {
            @Override
            protected void onQueryComplete(int token, Object cookie,
                    Cursor cursor) {
                Log.d(TAG, "onQueryComplete()");
                RecordScanCache.loadRecord(cursor);
                if (RecordScanCache.getCache().size() == 0) {
                    return;
                }
                CodeScanWorker.getInstance().send();
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // 查询本地 success为0的记录，表示没有同步到网侧
        myHandler.cancelOperation(0);
        myHandler.startQuery(0, null, CodescanConst.RECORD_URI, null,
                CodescanConst.RecordColum.SUCCESS + " = 0", null, null);
        return mService.asBinder();
    }
}
