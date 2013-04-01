package com.android.callstat.home.settings;

import java.util.Calendar;
import java.util.TimeZone;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.Time;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.callstat.CallStatApplication;
import com.android.callstat.CallStatReceiver;
import com.android.callstat.ConfigManager;
import com.android.callstat.common.CallStatUtils;
import com.archermind.callstat.R;

public class DisturbActivity extends Activity implements OnClickListener,
		OnTouchListener {

	private RelativeLayout mBackLayout, mDisturbOnOffLayout, mDisturbOnLayout,
			mDisturbOffLayout, mSliderLayout;
	private ImageView mSliderLeftImage, mSliderRightImage/* , backImageView */;
	private TextView offTextView, onTextView, offNameView, onNameView,
			onoffTextView;

	private ConfigManager config;
	private CallStatApplication application;
	private boolean on_off = false;
	private boolean isOnClick = false;

	// private int mHour_close;
	// private int mMinute_close;
	// private int mHour_open;
	// private int mMinute_open;
	// private int time_open;
	// private int time_close;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		application = (CallStatApplication) getApplication();
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.setting_disturb);
		config = new ConfigManager(this);
		initView();

	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		updateImageView();
		initTextView();
		// if (config.isAirmodeSwitch()) {
		// setAlertTime();
		// }

	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		switch (arg0.getId()) {
		case R.id.disturb_back_rl:
			finish();
			overridePendingTransition(R.anim.push_right_in,
					R.anim.push_right_out);
			break;
		case R.id.disturb_on_off_rl:
			isOnClick = true;
			updateImageView();
			isOnClick = false;
			break;
		case R.id.disturb_on_rl:
			if (config.isAirmodeSwitch()) {
				toTimePickerActivity(true);
				// showDateDialog(this, 0,
				// R.layout.mytimepicker_dialog, callBack,
				// config.getAirmodeCloseTime() / 100,
				// config.getAirmodeCloseTime() % 100);

			}

			break;
		case R.id.disturb_off_rl:
			if (config.isAirmodeSwitch()) {
				toTimePickerActivity(false);
				// showDateDialog(this, 1,
				// R.layout.mytimepicker_dialog, callBack,
				// config.getAirmodeOpenTime() / 100,
				// config.getAirmodeOpenTime() % 100);
			}

			break;
		default:
			break;
		}
	}

	private void updateImageView() {
		on_off = config.isAirmodeSwitch();
		// Log.e("callstats", "on_off:" + on_off);
		switchSlider(on_off, isOnClick);
		if (config.isAirmodeSwitch()) {
			offNameView.setTextColor(getResources().getColor(
					R.color.setting_text_black));
			onNameView.setTextColor(getResources().getColor(
					R.color.setting_text_black));
			offTextView.setTextColor(getResources().getColor(
					R.color.setting_text));
			onTextView.setTextColor(getResources().getColor(
					R.color.setting_text));
		} else {
			offNameView.setTextColor(getResources().getColor(
					R.color.setting_text_unclick));
			onNameView.setTextColor(getResources().getColor(
					R.color.setting_text_unclick));
			offTextView.setTextColor(getResources().getColor(
					R.color.setting_text_unclick));
			onTextView.setTextColor(getResources().getColor(
					R.color.setting_text_unclick));
		}
	}

	private void switchSlider(boolean on_off, boolean isOnClick) {
		if (on_off) {
			if (isOnClick) {// 关闭防打扰
				mSliderLeftImage.setVisibility(View.VISIBLE);
				mSliderRightImage.setVisibility(View.GONE);
				mSliderLayout.setBackgroundResource(R.drawable.wqy_setting);
				config.setAirmodeSwitch(false);
				closeAirMode();
			} else {
				mSliderLeftImage.setVisibility(View.GONE);
				mSliderRightImage.setVisibility(View.VISIBLE);
				mSliderLayout.setBackgroundResource(R.drawable.yqy_setting);
			}
		} else {
			if (isOnClick) {// 开启防打扰
				mSliderLeftImage.setVisibility(View.GONE);
				mSliderRightImage.setVisibility(View.VISIBLE);
				mSliderLayout.setBackgroundResource(R.drawable.yqy_setting);
				config.setAirmodeSwitch(true);
				setAlertTime();
			} else {
				mSliderLeftImage.setVisibility(View.VISIBLE);
				mSliderRightImage.setVisibility(View.GONE);
				mSliderLayout.setBackgroundResource(R.drawable.wqy_setting);
			}
		}
	}

	private void initView() {
		mBackLayout = (RelativeLayout) findViewById(R.id.disturb_back_rl);
		mDisturbOnOffLayout = (RelativeLayout) findViewById(R.id.disturb_on_off_rl);
		mDisturbOnLayout = (RelativeLayout) findViewById(R.id.disturb_on_rl);
		mDisturbOffLayout = (RelativeLayout) findViewById(R.id.disturb_off_rl);
		mSliderLayout = (RelativeLayout) findViewById(R.id.slider_rl);

		mSliderLeftImage = (ImageView) findViewById(R.id.slider_img_left);
		mSliderRightImage = (ImageView) findViewById(R.id.slider_img_right);

		offTextView = (TextView) findViewById(R.id.disturb_off_tv);
		onTextView = (TextView) findViewById(R.id.disturb_on_tv);
		offNameView = (TextView) findViewById(R.id.disturb_off_name);
		onNameView = (TextView) findViewById(R.id.disturb_on_name);

		// backImageView = (ImageView) findViewById(R.id.refresh_img);
		onoffTextView = (TextView) findViewById(R.id.disturb_on_off_name);

		mBackLayout.setOnClickListener(this);
		mDisturbOnOffLayout.setOnClickListener(this);
		mDisturbOnLayout.setOnClickListener(this);
		mDisturbOffLayout.setOnClickListener(this);

		mBackLayout.setOnTouchListener(this);
		mDisturbOnOffLayout.setOnTouchListener(this);
		mDisturbOnLayout.setOnTouchListener(this);
		mDisturbOffLayout.setOnTouchListener(this);

	}

	private void initTextView() {
		offTextView.setText(gettime(config.getAirmodeOpenTime()));
		onTextView.setText(gettime(config.getAirmodeCloseTime()));
	}

	private void closeAirMode() {
		Time time = new Time();
		time.setToNow();
		Calendar cNow = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
		int now_hour = cNow.get(Calendar.HOUR_OF_DAY);
		int now_minute = cNow.get(Calendar.MINUTE);
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		PendingIntent pi = application.getAirModeIntent();
		if (pi != null) {
			am.cancel(pi);
		}
		// 开启时间>关闭
		if (config.getAirmodeCloseTime() < config.getAirmodeOpenTime()) {
			// 现在时间<开启时间 同时 现在时间》关闭时间 ==现在关闭飞行模式
			if (now_hour * 100 + now_minute < config.getAirmodeOpenTime()
					&& now_hour * 100 + now_minute >= config
							.getAirmodeCloseTime()) {
				// Log.i("DisturbActivity", "now="+(now_hour * 100 +
				// now_minute));
				// Log.i("DisturbActivity",
				// "AirmodeOpenTime="+config.getAirmodeOpenTime());
				// Log.i("DisturbActivity",
				// "AirmodeCloseTime="+config.getAirmodeCloseTime());
				return;

			} else {// 现在为开启飞行模式。可是防打扰关了，应该关闭飞行模式
				// Log.i("DisturbActivity", "飞行模式关闭");
				// Log.i("DisturbActivity", "now="+(now_hour * 100 +
				// now_minute));
				// Log.i("DisturbActivity",
				// "AirmodeOpenTime="+config.getAirmodeOpenTime());
				// Log.i("DisturbActivity",
				// "AirmodeCloseTime="+config.getAirmodeCloseTime());
				CallStatUtils.setAirplaneModeOn(this, false);
			}
		} else {// 开启《关闭
			if (now_hour * 100 + now_minute < config.getAirmodeOpenTime()
					|| now_hour * 100 + now_minute >= config
							.getAirmodeCloseTime()) {// 现在《开启 或 现在》关闭 ==现在关闭飞行模式
				// Log.i("DisturbActivity", "now="+(now_hour * 100 +
				// now_minute));
				// Log.i("DisturbActivity",
				// "AirmodeOpenTime="+config.getAirmodeOpenTime());
				// Log.i("DisturbActivity",
				// "AirmodeCloseTime="+config.getAirmodeCloseTime());
				return;
			} else {// 现在为开启飞行模式。可是防打扰关了，应该关闭飞行模式
				// Log.i("DisturbActivity", "now="+(now_hour * 100 +
				// now_minute));
				// Log.i("DisturbActivity",
				// "AirmodeOpenTime="+config.getAirmodeOpenTime());
				// Log.i("DisturbActivity",
				// "AirmodeCloseTime="+config.getAirmodeCloseTime());
				// Log.i("DisturbActivity", "飞行模式开启");
				CallStatUtils.setAirplaneModeOn(this, false);
			}
		}
	}

	public void setAlertTime() {

		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);

		PendingIntent pi = application.getAirModeIntent();
		if (pi == null) {
			Intent intent = new Intent(this, CallStatReceiver.class);
			intent.setAction(CallStatReceiver.AIR_ALARM);

			application.setAirModeIntent(PendingIntent.getBroadcast(this, 0,
					intent, PendingIntent.FLAG_UPDATE_CURRENT));
		}

		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
		Calendar cNow = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
		int now_hour = cNow.get(Calendar.HOUR_OF_DAY);
		int now_minute = cNow.get(Calendar.MINUTE);
		// 关闭<开启
		if (config.getAirmodeCloseTime() < config.getAirmodeOpenTime()) {
			if (now_hour * 100 + now_minute < config.getAirmodeCloseTime()
					|| now_hour * 100 + now_minute >= config
							.getAirmodeOpenTime()) {// 现在<关闭 或 现在>开启 ==现在是开启飞行模式
				CallStatUtils.setAirplaneModeOn(this, true);
				if (now_hour * 100 + now_minute >= config.getAirmodeOpenTime()) {
					c.add(Calendar.DAY_OF_MONTH, 1);
				}

				c.set(Calendar.HOUR_OF_DAY, config.getAirmodeCloseTime() / 100);
				c.set(Calendar.MINUTE, config.getAirmodeCloseTime() % 100);
				// Log.i("myHandler", "asdfksdagfkad====1");
				// Log.e("myHandler", "time:" + config.getAirmodeCloseTime());
			} else {// 现在为关闭飞行模式
				CallStatUtils.setAirplaneModeOn(this, false);// 王磊加
				c.set(Calendar.HOUR_OF_DAY, config.getAirmodeOpenTime() / 100);
				c.set(Calendar.MINUTE, config.getAirmodeOpenTime() % 100);
				// Log.e("myHandler", "time:" + config.getAirmodeOpenTime());
			}
		} else {// 关闭》开启
			if (now_hour * 100 + now_minute < config.getAirmodeOpenTime()
					|| now_hour * 100 + now_minute >= config
							.getAirmodeCloseTime()) {// 现在<开启 或 现在》关闭
														// ==现在是关闭飞行模式
				CallStatUtils.setAirplaneModeOn(this, false);// 王磊加
				if (now_hour * 100 + now_minute >= config.getAirmodeCloseTime()) {
					c.add(Calendar.DAY_OF_MONTH, 1);
				}
				c.set(Calendar.HOUR_OF_DAY, config.getAirmodeOpenTime() / 100);
				c.set(Calendar.MINUTE, config.getAirmodeOpenTime() % 100);
				// Log.e("myHandler", "time:" + config.getAirmodeOpenTime());
			} else {
				CallStatUtils.setAirplaneModeOn(this, true);
				c.set(Calendar.HOUR_OF_DAY, config.getAirmodeCloseTime() / 100);
				c.set(Calendar.MINUTE, config.getAirmodeCloseTime() % 100);
				// Log.i("myHandler", "asdfksdagfkad====4");
				// Log.e("myHandler", "time:" + config.getAirmodeCloseTime());
			}
		}
		// Log.e("myHandler", "time:" + c.getTime().toLocaleString());
		am.set(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(),
				application.getAirModeIntent());
	}

	/** 设置防打扰中飞行模式,theme=0关闭飞行模式，则为恢复无线设备；为1开启飞行模式，关闭无线设备 */
	/*
	 * private Dialog showDateDialog(Context context, final int theme, int
	 * content, OnTimeSetListener callBack, int hour, int minute) {
	 * 
	 * LayoutInflater inflater = (LayoutInflater) context
	 * .getSystemService(Context.LAYOUT_INFLATER_SERVICE); View viewContent =
	 * inflater.inflate(content, null);
	 * 
	 * final MyTimePicker mDatePicker = (MyTimePicker) viewContent
	 * .findViewById(R.id.myTimePicker); mDatePicker.init(hour, minute,
	 * callBack); switch (theme) { case 0: AlertDialog dialog = new
	 * AlertDialog.Builder(this) .setView(viewContent) .setPositiveButton("设置",
	 * new DialogInterface.OnClickListener() {
	 * 
	 * @Override public void onClick(DialogInterface dialog, int which) {
	 * 
	 * mHour_close = mDatePicker.getHour(); mMinute_close =
	 * mDatePicker.getMinute(); time_close = 100 * mHour_close + mMinute_close;
	 * if (time_close != config .getAirmodeOpenTime()) {
	 * onTextView.setText(gettime(time_close));
	 * config.setAirmodeCloseTime(time_close); setAlertTime(); } else {
	 * Toast.makeText( DisturbActivity.this, "开关飞行模式时间不能相同",
	 * Toast.LENGTH_SHORT).show();
	 * 
	 * } }
	 * 
	 * }).setNegativeButton("取消", null).show(); return dialog; case 1:
	 * AlertDialog dialog1 = new AlertDialog.Builder(this) .setView(viewContent)
	 * .setPositiveButton("设置", new DialogInterface.OnClickListener() {
	 * 
	 * @Override public void onClick(DialogInterface dialog, int which) {
	 * 
	 * mHour_open = mDatePicker.getHour(); mMinute_open =
	 * mDatePicker.getMinute(); time_open = 100 * mHour_open + mMinute_open; if
	 * (time_open != config .getAirmodeCloseTime()) {
	 * offTextView.setText(gettime(time_open));
	 * config.setAirmodeOpenTime(time_open); setAlertTime(); } else {
	 * Toast.makeText( DisturbActivity.this, "开关飞行模式时间不能相同",
	 * Toast.LENGTH_SHORT).show();
	 * 
	 * } }
	 * 
	 * }).setNegativeButton("取消", null).show(); return dialog1; default: return
	 * null; }
	 * 
	 * }
	 */
	public String gettime(int time) {
		String sTime;
		if (time % 100 >= 10) {
			if (time / 100 >= 10) {
				sTime = time / 100 + ":" + time % 100;
			} else {
				sTime = "0" + time / 100 + ":" + time % 100;
			}

		} else {
			if (time / 100 >= 10) {
				sTime = time / 100 + ":" + "0" + time % 100;
			} else {
				sTime = "0" + time / 100 + ":" + "0" + time % 100;
			}

		}
		return sTime;
	}

	private void toTimePickerActivity(boolean isOn) {
		Intent intent = new Intent(this, TimePickerActivity.class);
		intent.putExtra("isOn", isOn);
		startActivity(intent);
	}

	/*
	 * private OnTimeSetListener callBack = new OnTimeSetListener() {
	 * 
	 * @Override public void onTimeSet(MyTimePicker view, int hour, int minute)
	 * { // Toast.makeText(SystemSettingActivity.this, "time set:" + hour + //
	 * " : " +minute , Toast.LENGTH_SHORT).show(); } };
	 */

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.disturb_on_off_rl:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				onoffTextView.setTextColor(getResources().getColor(
						R.color.white));
				mDisturbOnOffLayout
						.setBackgroundResource(R.drawable.set_biglist_blue);
				return false;

			case MotionEvent.ACTION_UP:
				onoffTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				mDisturbOnOffLayout
						.setBackgroundResource(R.drawable.set_biglist_bar);
				return false;

			case MotionEvent.ACTION_CANCEL:
				onoffTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				mDisturbOnOffLayout
						.setBackgroundResource(R.drawable.set_biglist_bar);
				return false;
			default:
				break;
			}

			break;
		case R.id.disturb_off_rl:
			if (config.isAirmodeSwitch()) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					offNameView.setTextColor(getResources().getColor(
							R.color.white));
					offTextView.setTextColor(getResources().getColor(
							R.color.white));
					mDisturbOffLayout
							.setBackgroundResource(R.drawable.set_biglist_blue);
					return false;

				case MotionEvent.ACTION_UP:
					if (config.isAirmodeSwitch()) {
						offNameView.setTextColor(getResources().getColor(
								R.color.setting_text_black));
						offTextView.setTextColor(getResources().getColor(
								R.color.setting_text));
						mDisturbOffLayout
								.setBackgroundResource(R.drawable.set_biglist_bar);
					} else {
						offNameView.setTextColor(getResources().getColor(
								R.color.setting_text_unclick));
						offTextView.setTextColor(getResources().getColor(
								R.color.setting_text_unclick));
						mDisturbOffLayout
								.setBackgroundResource(R.drawable.set_biglist_bar);
					}

					return false;

				case MotionEvent.ACTION_CANCEL:
					if (config.isAirmodeSwitch()) {
						offNameView.setTextColor(getResources().getColor(
								R.color.setting_text_black));
						offTextView.setTextColor(getResources().getColor(
								R.color.setting_text));
						mDisturbOffLayout
								.setBackgroundResource(R.drawable.set_biglist_bar);
					} else {
						offNameView.setTextColor(getResources().getColor(
								R.color.setting_text_unclick));
						offTextView.setTextColor(getResources().getColor(
								R.color.setting_text_unclick));
						mDisturbOffLayout
								.setBackgroundResource(R.drawable.set_biglist_bar);
					}
					return false;
				default:
					break;
				}
			}
			break;

		case R.id.disturb_on_rl:
			if (config.isAirmodeSwitch()) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					onNameView.setTextColor(getResources().getColor(
							R.color.white));
					onTextView.setTextColor(getResources().getColor(
							R.color.white));
					mDisturbOnLayout
							.setBackgroundResource(R.drawable.set_biglist_blue);
					return false;

				case MotionEvent.ACTION_UP:
					if (config.isAirmodeSwitch()) {
						onNameView.setTextColor(getResources().getColor(
								R.color.setting_text_black));
						onTextView.setTextColor(getResources().getColor(
								R.color.setting_text));
						mDisturbOnLayout
								.setBackgroundResource(R.drawable.set_biglist_bar);
					} else {
						onNameView.setTextColor(getResources().getColor(
								R.color.setting_text_unclick));
						onTextView.setTextColor(getResources().getColor(
								R.color.setting_text_unclick));
						mDisturbOnLayout
								.setBackgroundResource(R.drawable.set_biglist_bar);
					}

					return false;

				case MotionEvent.ACTION_CANCEL:
					if (config.isAirmodeSwitch()) {
						onNameView.setTextColor(getResources().getColor(
								R.color.setting_text_black));
						onTextView.setTextColor(getResources().getColor(
								R.color.setting_text));
						mDisturbOnLayout
								.setBackgroundResource(R.drawable.set_biglist_bar);
					} else {
						onNameView.setTextColor(getResources().getColor(
								R.color.setting_text_unclick));
						onTextView.setTextColor(getResources().getColor(
								R.color.setting_text_unclick));
						mDisturbOnLayout
								.setBackgroundResource(R.drawable.set_biglist_bar);
					}
					return false;
				default:
					break;
				}
			}
			break;
		case R.id.disturb_back_rl:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				// backImageView.setImageResource(R.drawable.back_arrow_x);
				mBackLayout.setBackgroundResource(R.drawable.list_view_bg);
				return false;

			case MotionEvent.ACTION_UP:
				// backImageView.setImageResource(R.drawable.back_arrow);
				mBackLayout.setBackgroundResource(0);
				return false;

			case MotionEvent.ACTION_CANCEL:
				// backImageView.setImageResource(R.drawable.back_arrow);
				mBackLayout.setBackgroundResource(0);
				return false;
			default:
				break;
			}

			break;
		default:
			break;
		}
		return false;
	}

	// 监听返回键
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			finish();
			overridePendingTransition(R.anim.push_right_in,
					R.anim.push_right_out);
			return false;
		}
		return false;
	}
}
