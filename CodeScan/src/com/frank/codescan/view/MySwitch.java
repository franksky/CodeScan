package com.frank.codescan.view;

import com.frank.codescan.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;

;

public class MySwitch extends LinearLayout implements OnClickListener {
    private static final String TAG = "CodeScan_MySwitch";
    private SwitchListener mListener;
    private LayoutInflater mLayoutInflater;
    private Button mLeftButton;
    private Button mRightButton;
    private boolean mIsLeftChoose;

    public static interface SwitchListener {
        public void onClick(boolean isLeftClick);
    }

    public MySwitch(Context context) {
        super(context);
        mLayoutInflater = (LayoutInflater) getContext().getSystemService(
                context.LAYOUT_INFLATER_SERVICE);
    }

    public MySwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLayoutInflater = (LayoutInflater) getContext().getSystemService(
                context.LAYOUT_INFLATER_SERVICE);
    }

    public MySwitch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLayoutInflater = (LayoutInflater) getContext().getSystemService(
                context.LAYOUT_INFLATER_SERVICE);
    }

    public void setSwitchListener(SwitchListener listener) {
        mListener = listener;
    }

    @Override
    protected void onFinishInflate() {
        View rootView = mLayoutInflater.inflate(R.layout.my_switch,
                (LinearLayout) findViewById(R.id.switch_layout_root));
        mLeftButton = (Button) rootView.findViewById(R.id.left_button);
        mRightButton = (Button) rootView.findViewById(R.id.right_button);
        mLeftButton.setOnClickListener(this);
        mRightButton.setOnClickListener(this);
        addView(rootView);
        setStyle(true);
        super.onFinishInflate();
    }
    
    public boolean isLeftChoose() {
        return mIsLeftChoose;
    }

    private void setStyle(boolean isLeftPress) {
        if (mListener != null) {
            mListener.onClick(isLeftPress);
        }

        if (isLeftPress) {
            mIsLeftChoose = true;
            mLeftButton.setTextColor(Color.WHITE);
            mLeftButton.setBackground(getResources().getDrawable(R.drawable.mmtitle_bg_alpha));
            mRightButton.setTextColor(Color.BLACK);
            mRightButton.setBackground(getResources().getDrawable(R.drawable.mmtitle_bg_alpha_white));
        } else {
            mIsLeftChoose = false;
            mLeftButton.setTextColor(Color.BLACK);
            mLeftButton.setBackground(getResources().getDrawable(R.drawable.mmtitle_bg_alpha_white));
            mRightButton.setTextColor(Color.WHITE);
            mRightButton.setBackground(getResources().getDrawable(R.drawable.mmtitle_bg_alpha));
        }
    }
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.left_button) {
            setStyle(true);
        } else if (v.getId() == R.id.right_button) {
            setStyle(false);
        }
    }
}
