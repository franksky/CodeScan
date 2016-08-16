package com.frank.codescan.data;

import java.util.Calendar;

import com.frank.codescan.R;
import com.frank.codescan.utils.CodeScanUtil;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class RecordListAdapter extends CursorAdapter {

    private LayoutInflater mInflater;
    private Context mContext;
    // ºÅÂëÏÔÊ¾×îºóÎª4Î»¿É¼û¡£
    private static final int LAST_PHONE_NUM_LENGTH = 4;
    public RecordListAdapter(Context context, Cursor c) {
        super(context, c);
        mContext = context;
        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView vName = (TextView) view.findViewById(R.id.name);
        TextView vDate = (TextView) view.findViewById(R.id.date);
        TextView vTime = (TextView) view.findViewById(R.id.time);
        TextView vNum = (TextView) view.findViewById(R.id.phone_num);
        ImageView vStatus = (ImageView) view.findViewById(R.id.status_fail);
        TextView vNameDrawable = (TextView) view
                .findViewById(R.id.name_drawable);
        Record record = CodeScanUtil.getRecordFromCursor(cursor);
        vName.setText(record.name);
        vDate.setText(getDateToday(record));
        vTime.setText(record.time);
        String firstName = "NA";
        // ²ÎÊý¼ì²é
        if (record.name.length() > 0) {
            firstName = record.name.substring(0, 1);
        }
        String num = "";
        if (record.phoneNumber.startsWith("+86") && record.phoneNumber.length() == 14) {
            //´ø¹ú¼ÒÂëÊÖ»úºÅ
            num = record.phoneNumber.substring(0, 6) + "****" + record.phoneNumber.substring(10, 14);
        } else if (record.phoneNumber.length() == 11 && ! record.phoneNumber.startsWith("0")) {
            //ÊÖ»úºÅÂë
            num = record.phoneNumber.substring(0, 3) + "****" + record.phoneNumber.substring(7, 11);
        } else if (record.phoneNumber.length() > 6) {
            //¹Ì¶¨µç»°£¬Ö»ÆÁ±ÎÈýÎ»£¬½áÎ²2¸öºÅÂë±£Áô
            num = record.phoneNumber.substring(0, record.phoneNumber.length() - 5) + "***" + record.phoneNumber.substring(record.phoneNumber.length() - 2, record.phoneNumber.length());
        } else {
            num = record.phoneNumber;
        }
        num = mContext.getString(R.string.num) + num;
        vNum.setText(num);
        vNameDrawable.setText(firstName);
        if (record.isSuccess()) {
            vNameDrawable.setTextColor(mContext.getResources().getColor(
                    R.color.text_name_green));
            vNameDrawable.setBackgroundResource(R.drawable.name_drawable_green);
            vStatus.setVisibility(View.GONE);
        } else {
            vNameDrawable.setTextColor(Color.RED);
            vNameDrawable.setBackgroundResource(R.drawable.name_drawable_red);
            vStatus.setVisibility(View.GONE);
//            vStatus.setVisibility(View.VISIBLE);
        }
    }

    private String getDateToday(Record record) {
        String date = record.date;
//        Calendar cRecord = Calendar.getInstance();
//        cRecord.setTimeInMillis(record.realTime);
//        Calendar currentDay = Calendar.getInstance();
//        if (cRecord.get(Calendar.YEAR) == currentDay.get(Calendar.YEAR)
//                && cRecord.get(Calendar.MONTH) == currentDay.get(Calendar.MONTH)) {
//            if (cRecord.get(Calendar.DAY_OF_MONTH) == currentDay.get(Calendar.DAY_OF_MONTH)) {
//                date = "����";
//            } else if (cRecord.get(Calendar.DAY_OF_MONTH) == currentDay.get(Calendar.DAY_OF_MONTH) - 1) {
//                date = "����";
//            }
//        }
        long recordTime = record.realTime/1000;
        if (CodeScanUtil.getTodayMinTimeLong() < recordTime
            && CodeScanUtil.getTodayMaxTimeLong() > recordTime) {
            date = mContext.getString(R.string.today);
        } else if (CodeScanUtil.getYesterdayMinTimeLong() < recordTime
            && CodeScanUtil.getYesterdayMaxTimeLong() > recordTime) {
            date = mContext.getString(R.string.yesterday);
        }
        return date;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = null;
        view = mInflater.inflate(R.layout.record_list_view, parent, false);
        return view;
    }
}
