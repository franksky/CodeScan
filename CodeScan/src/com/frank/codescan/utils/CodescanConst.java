package com.frank.codescan.utils;

import com.frank.codescan.config.Config;

import android.net.Uri;

public class CodescanConst {
    public static final String PROVIDER_AUTH = "code_scan";
    // ²åÈëºÍ²éÑ¯±¾µØ¼ÇÂ¼µÄURI
    public static final Uri RECORD_URI = Uri.parse("content://code_scan/record");
    // ¼ÇÂ¼±¾µØ´ò¿¨¼ÇÂ¼µÄ±í
    public static final String TABLE_RECORD = "record";
    // ¸üÐÂÊý¾Ý¿âµÄ»Øµ÷URI
    public static final Uri RECODE_NOTIFY_URI = Uri.parse("content://code_scan");
    // Æô¶¯ServiceµÄAction
    public static final String ACTION_SERVICE = "com.frank.codescan.service.MainService";

    // Í¬²½Ç©µ½Êý¾Ý
    public static final String SYNC_SING_ACTION = Config.KEY_IP_ADDRESS + "/synSignData.action";

    // ��¼
    public static final String GET_SITE_ACTION = Config.KEY_IP_ADDRESS + "/getSiteInfo.action";

    // Ò»´ÎÍ¬²½Êý¾ÝµÄ¸öÊý
    public static final int MAX_SYNC_COUNT = 10;

    public static final boolean DEBUG = true;

    // ±¾µØ¼ÇÂ¼±í×Ö¶Î
    public static class RecordColum {
        public static final String ID = "_id";
        public static final String NAME = "name";
        public static final String DATE = "date";
        public static final String PHONE_NUM = "phone_num";
        public static final String SUCCESS = "success";
        public static final String DEVICE_NUM = "device_num";
    }

    // Í¬²½Êý¾Ý¿â×Ö¶Î
    public static class HttpSyncDataColum {
        public static final String REQ_NAME = "name";
        public static final String REQ_PHONE = "phone";
        public static final String REQ_DEVICE_PHONE = "device_phone";
        public static final String REQ_DATE = "date";
        public static final String REQ_CONTENT = "content";
        public static final String REQ_KEY = "request";
        public static final String REQ_DEVICE_PASS = "device_pass";
        public static final String REQ_SYSTEM_ID = "system_id";

        public static final String RESP_SUCCESS = "success";
        public static final String RESP_ARRAY_FLAG = "arrayflag";
        public static final String RESP_ERROR_CODE = "errorCode";
        public static final String RESP_ERROR_INFO = "errorInfo";
        public static final String RESP_CONTENT = "content";
        public static final String RESP_DATE = "date";
        public static final String RESP_NAME = "name";
        public static final String RESP_KEY = "response";
        public static final String RESP_SUCCESSED = "succeed";
        public static final String RESP_SITE_NAME = "site_name";
        public static final String RESP_DEVICE_PHONE = "device_phone";
    }

    /****************** Service Call Back EVENT begin ********************/
    public static final int SERVICE_CB_EVENT_LOGIN_RESULT = 1;
    /****************** Service Call Back EVENT end ********************/

    /****************** Service Call Back key begin ********************/
    public static final String SERVICE_CB_KEY_LOGIN_POSITION = "cb_login_position";
    public static final String SERVICE_CB_KEY_LOGIN_DEVICE_NUM = "cb_login_device_num";
    public static final String SERVICE_CB_KEY_LOGIN_SUCCESS = "cb_login_success";
    /****************** Service Call Back key end ********************/
}
