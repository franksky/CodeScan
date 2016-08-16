package com.frank.codescan.Interface;
import com.frank.codescan.Interface.LoginInfo;
import com.frank.codescan.Interface.IMainServiceCb;

interface IMainService {
    void processScan(String result);
    void syncLocalData();
    void login(in LoginInfo loginfo);
    void registerCb(IMainServiceCb cb);
    void unRegisterCb(IMainServiceCb cb);
    void getLogin();
    void logout();
}