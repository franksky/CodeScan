package com.frank.codescan.Interface;

import android.os.Parcel;
import android.os.Parcelable;

public class LoginInfo implements Parcelable {

    // 对应的设备名称
    public String mDeviceNum;

    // 密码
    public String mPassword;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mDeviceNum);
        dest.writeString(mPassword);
    }
    public LoginInfo(String deviceNum, String passwd) {
        mDeviceNum = deviceNum;
        mPassword = passwd;
    }

    public static final Parcelable.Creator<LoginInfo> CREATOR = new Creator<LoginInfo>() {
        
        @Override
        public LoginInfo[] newArray(int size) {
            return new LoginInfo[size];
        }

        @Override
        public LoginInfo createFromParcel(Parcel source) {
            return new LoginInfo(source.readString(), source.readString());
        }
    };
}