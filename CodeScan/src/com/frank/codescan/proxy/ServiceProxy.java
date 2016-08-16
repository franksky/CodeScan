package com.frank.codescan.proxy;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.frank.codescan.R;
import com.frank.codescan.Interface.IMainService;
import com.frank.codescan.Interface.LoginInfo;
import com.frank.codescan.service.IMainServiceCbImpl;
import com.frank.codescan.utils.CodescanConst;

import android.os.Message;

public class ServiceProxy {
    private static ServiceProxy mInstace;
    private IMainService mService;
    private boolean isBinded = false;
    private boolean isBinding = false;
    private Context mContext;
    private MyConnection mMyConnection;
    private static final String TAG = "CodeScan_ServiceProxy";
    private ArrayList<CallBackListener> mListener = new ArrayList<CallBackListener>();
    private IMainServiceCbImpl mServiceCb;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            Log.d(TAG, "service call back handleMessage():" + msg.what);
            switch(msg.what) {
                case CodescanConst.SERVICE_CB_EVENT_LOGIN_RESULT:
                    Bundle loginResult = msg.getData();
                    String deviceNum = null;
                    String position = null;
                    boolean success = false;
                    if (loginResult != null) {
                        deviceNum = loginResult.getString(CodescanConst.SERVICE_CB_KEY_LOGIN_DEVICE_NUM, "-1");
                        position = loginResult.getString(CodescanConst.SERVICE_CB_KEY_LOGIN_POSITION, "");
                        success = loginResult.getBoolean(CodescanConst.SERVICE_CB_KEY_LOGIN_SUCCESS, false);
                    }
                    for(CallBackListener listener : mListener) {
                        if (listener != null && !TextUtils.isEmpty(deviceNum)
                                && !TextUtils.isEmpty(position)) {
                            listener.onLoginResult(deviceNum, position, success);
                        }
                    }
                    break;
                default:
                    break;
            }
        };
    };

    public interface CallBackListener {
        public void onLoginResult(String deviceName, String position, boolean success);
    }

    public void registerCallBack(CallBackListener listener) {
        mListener.add(listener);
    }

    public void unRegisterCallBack(CallBackListener listener) {
        mListener.remove(listener);
    }

    private ServiceProxy(Context context) {
        mContext = context;
        mMyConnection = new MyConnection();
        mServiceCb = new IMainServiceCbImpl(mContext, mHandler);
    }

    public static ServiceProxy getInstance(Context context) {
        if (mInstace == null) {
            mInstace = new ServiceProxy(context);
        }
        return mInstace;
    }

    public void bindService() {
        boolean noBind = isBinding || isBinded;
        if (!noBind) {
            Intent service = new Intent(CodescanConst.ACTION_SERVICE);
            mContext.bindService(service, mMyConnection,
                    Context.BIND_AUTO_CREATE);
            isBinding = true;
        }
    }

    public void unbindService() {
        if (isBinded) {
            mContext.unbindService(mMyConnection);
        }
        isBinding = false;
        isBinded = false;
    }

    public void stopService() {
        if (isBinded) {
            mContext.unbindService(mMyConnection);
        }
        isBinding = false;
        isBinded = false;
        mContext.stopService(new Intent(CodescanConst.ACTION_SERVICE));
    }

    private class MyConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            isBinded = true;
            isBinding = false;
            mService = IMainService.Stub.asInterface(service);

            if (mService != null) {
                try {
                    mService.registerCb(mServiceCb);
                    mService.syncLocalData();
                    mService.getLogin();
                } catch (RemoteException e) {
                    Log.e(TAG, "register Service CallBack error");
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (mService != null) {
                try {
                    mService.unRegisterCb(mServiceCb);
                } catch (RemoteException e) {
                    Log.e(TAG, "unRegister Service CallBack error");
                }
            }
            isBinded = false;
            mService = null;
            bindService();
        }
    }

    public boolean processScan(String result) throws RemoteException {
        if (mService == null) {
            return false;
        }
        mService.processScan(result);
        return true;
    }

    public boolean syncLocalData() throws RemoteException {
        if (mService == null) {
            return false;
        }
        mService.syncLocalData();
        return true;
    }

    public boolean login(LoginInfo info) throws RemoteException {
        Log.d(TAG, "login()");
        if (mService == null) {
            return false;
        }
        mService.login(info);
        return true;
    }

    public boolean getLogin() throws RemoteException {
        Log.d(TAG, "mService = " + mService);
        if (mService == null) {
            return false;
        }
        mService.getLogin();
        return true;
    }

    public boolean logout() throws RemoteException {
        Log.d(TAG, "mService = " + mService);
        if (mService == null) {
            return false;
        }
        mService.logout();
        return true;
    }
}
