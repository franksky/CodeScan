package com.frank.codescan;

import com.frank.codescan.Interface.LoginInfo;
import com.frank.codescan.data.RecordListAdapter;
import com.frank.codescan.proxy.ServiceProxy;
import com.frank.codescan.proxy.ServiceProxy.CallBackListener;
import com.frank.codescan.utils.CodescanConst;
import com.frank.codescan.view.MySwitch;
import com.frank.codescan.view.MySwitch.SwitchListener;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

public class MainActivity extends Activity {
    private final static int SCANNIN_GREQUEST_CODE = 1;
    private final static int DIALOG_TYPE_SCAN = 1;
    private final static int DIALOG_TYPE_LOGOUT = 2;
    private final static int DIALOG_TYPE_EXIT = 3;
    private final static int DIALOG_TYPE_VERSION = 4;
    // ��¼״̬�²����ٴε�¼��������ʾ
    private final static int DIALOG_TYPE_RELOGIN = 5;
    private final static int DIALOG_TYPE_LOGIN_SUCCESS = 6;
    private final static int DIALOG_TYPE_LOGIN_FAIL = 7;

    /**
     * ��ʾɨ����
     */
    private static final String TAG = "CodeScan_MainActivity";
    private ListView mListView;
    private RecordListAdapter mAdapter;
    AsyncQueryHandler myHandler;
    private ActionBar mActionBar;
    private LinearLayout mNoRecord;
    private String mDeviceNum;
    private boolean mIsLogin;
    private MySwitch mSwitch;
    private String mUserName;

    private CallBackListener myListener = new CallBackListener() {
        @Override
        public void onLoginResult(String deviceName, String position, boolean success) {
            mDeviceNum = deviceName;
            mActionBar.setTitle(getActionBarTitle(position));
            myHandler.cancelOperation(0);
            if (mSwitch.isLeftChoose()) {
                myHandler.startQuery(0, null, CodescanConst.RECORD_URI, null,
                        "device_num = \'" + mDeviceNum + "\'", null, "date ASC");
            } else {
                myHandler.startQuery(0, null, CodescanConst.RECORD_URI, null,
                        "device_num = \'" + mDeviceNum + "\'" + "AND success = 0", null, "date ASC");
            }
            if (success) {
                mUserName = "";
                if (mDeviceNum.equals("-1")) {
                    mIsLogin = false;
                } else {
                    mIsLogin = true;
                }
            } else {
                mIsLogin = false;
                createMessageDialog(DIALOG_TYPE_LOGIN_FAIL);
            }
        }
    };

    BroadcastReceiver mConnectChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "receive internet config change");
            reFreshIntentStatus();
        }
    };

    private void reFreshIntentStatus() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);  
        NetworkInfo mobileInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);  
        NetworkInfo wifiInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);  
        if (mobileInfo.isConnected() || wifiInfo.isConnected()) {
            Log.d(TAG, "reFreshIntentStatus() is connect");
            try {
                ServiceProxy.getInstance(this).syncLocalData();
            } catch(RemoteException ex) {
                Log.w(TAG, "Call syncLocalData() RemoteException");
            }
        }
    }
    // �˵���������
    private static final int MENU_SCAN = Menu.FIRST + 1;
    private static final int MENU_SETTING = Menu.FIRST + 2;
    private static final int MENU_LOGIN = Menu.FIRST + 3;
    private static final int MENU_VERSION = Menu.FIRST + 4;
    private static final int MENU_LOGOUT = Menu.FIRST + 5;
    private static final int MENU_EXIT = Menu.FIRST + 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mActionBar = getActionBar();
        // ��������������actionBar����ʽ
        mActionBar.setBackgroundDrawable(getResources().getDrawable(
                R.drawable.mmtitle_bg_alpha));
        mActionBar.setTitle("");
        mIsLogin = false;
        mUserName = "";
        // �󶨷�������������Դ��������Service
        ServiceProxy.getInstance(getApplicationContext()).bindService();
        ServiceProxy.getInstance(getApplicationContext()).registerCallBack(
                myListener);

        // �����ť��ת����ά��ɨ����棬�����õ���startActivityForResult��ת
        // ɨ������֮������ý���
        mListView = (ListView) findViewById(R.id.result_list);
        mAdapter = new RecordListAdapter(this, null);
        mNoRecord = (LinearLayout) findViewById(R.id.no_record);
        mSwitch = (MySwitch) findViewById(R.id.my_switch);
        mSwitch.setSwitchListener(new SwitchListener() {
            @Override
            public void onClick(boolean isLeftClick) {
                // ����ȫ������
                if (isLeftClick) {
                    myHandler.cancelOperation(0);
                    myHandler.startQuery(0, null, CodescanConst.RECORD_URI, null,
                            "device_num = \'" + mDeviceNum + "\'", null, "date ASC");
                } else {
                    myHandler.cancelOperation(0);
                    myHandler.startQuery(0, null, CodescanConst.RECORD_URI, null,
                            "device_num = \'" + mDeviceNum + "\'" + "AND success = 0", null, "date ASC");
                }
            }
        });

        // ������ʼֵ��
        mDeviceNum = "-1";

        myHandler = new AsyncQueryHandler(getContentResolver()) {
            @Override
            protected void onQueryComplete(int token, Object cookie,
                    Cursor cursor) {
                mAdapter.changeCursor(cursor);
                if (cursor.getCount() > 0) {
                    mNoRecord.setVisibility(View.GONE);
                } else {
                    mNoRecord.setVisibility(View.VISIBLE);
                }
            }
        };

    }

    @Override
    protected void onStart() {
        super.onStart();
        mListView.setAdapter(mAdapter);
        myHandler.cancelOperation(0);
        myHandler.startQuery(0, null, CodescanConst.RECORD_URI, null,
                "device_num = \'" + mDeviceNum + "\'", null, "date ASC");
        // ��ѯ�������ݿ��Ѿ�������ͬ���Ķ���������ѯ��ɺ󣬻��Զ�����ͬ��
        reFreshIntentStatus();
        registerReceiver(mConnectChange, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mConnectChange);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // ɨ��
        menu.add(Menu.NONE, MENU_SCAN, 1, R.string.menu_scan_title)
                .setIcon(R.drawable.search_icon)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        // ����
        SubMenu settingMenu = menu.addSubMenu(Menu.NONE, MENU_SETTING, 2,
                R.string.menu_setting_title).setIcon(R.drawable.setting_icon);
        settingMenu.add(Menu.NONE, MENU_LOGIN, 1,
                R.string.menu_setting_sub_login);
        settingMenu.add(Menu.NONE, MENU_VERSION, 2,
                R.string.menu_setting_sub_version);
        settingMenu.add(Menu.NONE, MENU_LOGOUT, 3,
                R.string.menu_setting_sub_logout);
        settingMenu.add(Menu.NONE, MENU_EXIT, 4,
                R.string.menu_setting_sub_exit);
        settingMenu.getItem().setShowAsActionFlags(
                MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    // ������������action_bar title������
    private String getActionBarTitle(String title) {
        StringBuilder builder = new StringBuilder();
        builder.append("(").append(title).append(")");
        return builder.toString();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SCAN: {
                if (mIsLogin) {
                    Intent intent = new Intent();
                    intent.setClass(MainActivity.this,
                            CaptureActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivityForResult(intent, SCANNIN_GREQUEST_CODE);
                } else {
                    createMessageDialog(DIALOG_TYPE_SCAN);
                }
                break;
            }
            case MENU_EXIT: {
                createMessageDialog(DIALOG_TYPE_EXIT);
                break;
            }
            case MENU_LOGIN: {
                if (!mIsLogin) {
                    createLoginDialog();
                } else {
                    createMessageDialog(DIALOG_TYPE_RELOGIN);
                }
                break;
            }
            case MENU_LOGOUT: {
                createMessageDialog(DIALOG_TYPE_LOGOUT);
                break;
            }
            case MENU_VERSION: {
                createMessageDialog(DIALOG_TYPE_VERSION);
                break;
            }
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void createLoginDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setIcon(R.drawable.ic_launcher);
        dialogBuilder.setTitle(R.string.login_title);
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.login_view,
                (ViewGroup) findViewById(R.id.layout_root));
        final EditText userName = (EditText) view.findViewById(R.id.user_name);
        final EditText userPasswd = (EditText) view.findViewById(R.id.user_passwd);
        dialogBuilder.setView(view);
        userName.setText(mUserName);
        dialogBuilder.setPositiveButton(R.string.dialog_button_ok,
                new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String deviceNum = userName.getText().toString();
                        mUserName = deviceNum;
                        String passwd = userPasswd.getText().toString();
                        LoginInfo login = new LoginInfo(deviceNum, passwd);
                        try {
                            ServiceProxy.getInstance(getApplicationContext()).login(login);
                        } catch (RemoteException e) {
                            Log.e(TAG, "Call service login() remote exception.");
                        }
                    }
                });
        dialogBuilder.setNegativeButton(R.string.dialog_button_cancel, null);
        AlertDialog d = dialogBuilder.create();
        d.show();
    }

    private void createMessageDialog(final int dialogType) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setIcon(R.drawable.ic_launcher);
        switch (dialogType) {
            case DIALOG_TYPE_LOGOUT:
                dialogBuilder.setTitle(R.string.dialog_msg_logout_title);
                dialogBuilder.setMessage(R.string.dialog_msg_logout_confirm);
                break;
            case DIALOG_TYPE_EXIT:
                dialogBuilder.setTitle(R.string.dialog_msg_exit_title);
                dialogBuilder.setMessage(R.string.dialog_msg_exit_confirm);
                break;
            case DIALOG_TYPE_VERSION:
                dialogBuilder.setTitle(R.string.dialog_msg_version_title);
                dialogBuilder.setMessage(R.string.dialog_msg_version_confirm);
                break;
            case DIALOG_TYPE_SCAN:
                dialogBuilder.setTitle(R.string.dialog_msg_scan_title);
                dialogBuilder.setMessage(R.string.dialog_msg_scan_confirm);
                break;
            case DIALOG_TYPE_RELOGIN:
                dialogBuilder.setTitle(R.string.login_title);
                dialogBuilder.setMessage(R.string.dialog_msg_relogin_confirm);
                break;
            case DIALOG_TYPE_LOGIN_SUCCESS:
                dialogBuilder.setTitle(R.string.login_title);
                dialogBuilder.setMessage(R.string.dialog_msg_login_success_confirm);
                break;
            case DIALOG_TYPE_LOGIN_FAIL:
                dialogBuilder.setTitle(R.string.login_title);
                dialogBuilder.setMessage(R.string.dialog_msg_login_fail_confirm);
                break;
            default:
                break;
        }
        dialogBuilder.setPositiveButton(R.string.dialog_button_ok,
                new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (dialogType) {
                            case DIALOG_TYPE_LOGOUT:
                                try {
                                    ServiceProxy.getInstance(
                                            getApplicationContext()).logout();
                                } catch (RemoteException e) {
                                    Log.e(TAG,
                                            "call service logout() RemoteException");
                                }
                                break;
                            case DIALOG_TYPE_EXIT:
                                finish();
                                break;
                            case DIALOG_TYPE_SCAN:
                                createLoginDialog();
                                break;
                            case DIALOG_TYPE_RELOGIN:
                                try {
                                    ServiceProxy.getInstance(getApplicationContext()).logout();
                                    mDeviceNum = "-1";
                                } catch (RemoteException e) {
                                    Log.e(TAG, "call service logout() RemoteException");
                                }
                                createLoginDialog();
                                break;
                            case DIALOG_TYPE_LOGIN_FAIL:
                                createLoginDialog();
                                break;
                            default:
                                break;
                        }
                    }
                });
        boolean showNegativeButton = true;
        if (dialogType == DIALOG_TYPE_VERSION || dialogType ==  DIALOG_TYPE_LOGIN_SUCCESS) {
            showNegativeButton = false;
        }
        if (showNegativeButton) {
            dialogBuilder.setNegativeButton(R.string.dialog_button_cancel,
                    new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
        }
        AlertDialog d = dialogBuilder.create();
        d.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case SCANNIN_GREQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    String result = bundle.getString("result");
                    // ��ʾɨ�赽������
                    try {
                        ServiceProxy.getInstance(this).processScan(result);
                        myHandler.cancelOperation(0);
                        myHandler.startQuery(0, null, CodescanConst.RECORD_URI,
                                null, "device_num = \'" + mDeviceNum + "\'",
                                null, "date ASC");
                    } catch (RemoteException e) {
                        Log.e(TAG, "Call Service RemoteException");
                    }
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        ServiceProxy.getInstance(getApplicationContext()).unbindService();
        ServiceProxy.getInstance(getApplicationContext()).unRegisterCallBack(
                myListener);
        ServiceProxy.getInstance(getApplicationContext()).stopService();
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        ServiceProxy.getInstance(getApplicationContext()).bindService();
    }
}
