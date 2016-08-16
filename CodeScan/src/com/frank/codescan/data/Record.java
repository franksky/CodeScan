package com.frank.codescan.data;
public class Record {
    public String name;
    public String date;
    public String time;
    public String phoneNumber;
    public long id;
    public long realTime;
    public int success;
    public Record() {
        name = "";
        date = "";
        time = "";
        phoneNumber = "";
        id = 0;
        realTime = 0;
        success = 0;
    }
    public boolean isSuccess() {
        return success == 1;
    }
}