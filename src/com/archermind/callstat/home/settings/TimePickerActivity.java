package com.archermind.callstat.home.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.archermind.callstat.ConfigManager;
import com.archermind.callstat.R;
import com.archermind.callstat.common.views.MyTimePicker;
import com.archermind.callstat.common.views.MyTimePicker.OnTimeSetListener;

public class TimePickerActivity extends Activity implements OnClickListener {

	private Button cancel_btn, ok_btn;
	private boolean isON;// 为true，表示恢复无线电，即关闭飞行
	private MyTimePicker mDatePicker;
	private int mHour, mMinute, mTime;

	private ConfigManager config;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mytimepicker_dialog);
		config = new ConfigManager(this);
		Intent intent = getIntent();
		isON = intent.getBooleanExtra("isOn", false);
		initView();
	}

	@Override
	public void onClick(View arg0) {
		switch (arg0.getId()) {
		case R.id.time_cancel_btn:
			finish();
			break;
		case R.id.time_ok_btn:
			ok();

			break;
		default:
			break;
		}
	}

	private void initView() {
		mDatePicker = (MyTimePicker) findViewById(R.id.myTimePicker);
		if (isON) {
			mDatePicker.init(config.getAirmodeCloseTime() / 100,
					config.getAirmodeCloseTime() % 100, callBack);
		} else {
			mDatePicker.init(config.getAirmodeOpenTime() / 100,
					config.getAirmodeOpenTime() % 100, callBack);
		}

		cancel_btn = (Button) mDatePicker.findViewById(R.id.time_cancel_btn);
		ok_btn = (Button) mDatePicker.findViewById(R.id.time_ok_btn);

		cancel_btn.setOnClickListener(this);
		ok_btn.setOnClickListener(this);
	}

	private void ok() {
		mHour = mDatePicker.getHour();
		mMinute = mDatePicker.getMinute();
		mTime = 100 * mHour + mMinute;
		if (isON) {
			if (mTime != config.getAirmodeOpenTime()) {
				config.setAirmodeCloseTime(mTime);
				finish();
			} else {
				Toast.makeText(TimePickerActivity.this, "开启和关闭飞行模式的时间不能相同",
						Toast.LENGTH_SHORT).show();
			}
		} else {
			if (mTime != config.getAirmodeCloseTime()) {
				config.setAirmodeOpenTime(mTime);
				finish();
			} else {
				Toast.makeText(TimePickerActivity.this, "开启和关闭飞行模式的时间不能相同",
						Toast.LENGTH_SHORT).show();
			}
		}
	}

	private OnTimeSetListener callBack = new OnTimeSetListener() {

		@Override
		public void onTimeSet(MyTimePicker view, int hour, int minute) {
			// Toast.makeText(SystemSettingActivity.this, "time set:" + hour +
			// " : " +minute , Toast.LENGTH_SHORT).show();
		}
	};

}
