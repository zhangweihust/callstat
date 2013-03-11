package com.archermind.callstat.home.settings;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.archermind.callstat.ConfigManager;
import com.archermind.callstat.ILog;
import com.archermind.callstat.R;
import com.archermind.callstat.common.CallStatUtils;
import com.archermind.callstat.firewall.FirewallUtils;
import com.archermind.callstat.home.views.CustomListDialog;
import com.archermind.callstat.service.CallStatSMSService;

public class PackageSettingActivity extends Activity implements
		SeekBar.OnSeekBarChangeListener, OnClickListener, OnTouchListener {

	private ConfigManager config;
	private TextView alert_traffic_tv;
	private EditText call_et;
	private EditText sms_et;
	private EditText traffic_et;
	private RelativeLayout accountday_rl;
	private AlertDialog mDialog = null;

	private String accountday = "";
	private boolean ACOUNT_DAY_DIALOG = false;
	private SeekBar mSeekBar;
	private TextView mProgressText;
	private TextView accoutDayTextView;

	private RelativeLayout network_switch_rl;
	// private RelativeLayout traffic_alert_rl;

	private RelativeLayout traffic_alert_switch_rl;
	private RelativeLayout network_switch;
	private ImageView alert_click_left_iv;
	private ImageView alert_click_right_iv;
	private ImageView network_click_left_iv;
	private ImageView network_click_right_iv;
	float traffic_alert;
	int seekbar_progress;

	int FreeCallTime;
	int FreeMessages;
	float FreeGprs;

	int edit_index = 0;

	int old_call_et;
	int old_sms_et;
	float old_traffic_et;
	float old_traffic_alert;
	boolean old_isBrokenNetwork;
	int old_accountingDay;

	/**
	 * 江苏移动定制
	 */
	private RelativeLayout overlay_package_rl;
	private RelativeLayout overlay_package_switch_rl;
	private ImageView overlay_package_click_left;
	private ImageView overlay_package_click_right;

	private boolean REFLESH_NOTIFICATION = false;

	private RelativeLayout back_rl, modify;

	/**
	 * 与CallStatSMSService进行绑定
	 */
	private CallStatSMSService.SMSBinder binder;
	private ServiceConnection conn = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {

		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			binder = (CallStatSMSService.SMSBinder) service;
		}
	};

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		if (getIntent().getExtras() != null) {
			edit_index = getIntent().getExtras().getInt(
					CallStatUtils.PACKAGE_SET);
			if (edit_index == 3) {
				overridePendingTransition(R.anim.push_left_in,
						R.anim.push_left_out);
			}
		}
		setContentView(R.layout.package_setting);
		config = new ConfigManager(this);
		initUI();
		initListener();

		/* 绑定服务 */
		Intent intent = new Intent(this, CallStatSMSService.class);
		if (!CallStatUtils.isServiceRunning(this,
				"com.archermind.callstat.service.CallStatSMSService")) {
			startService(intent);
		}
		bindService(intent, conn, Context.BIND_AUTO_CREATE);
	}

	private void initListener() {
		mSeekBar.setOnSeekBarChangeListener(this);
		accountday_rl.setOnClickListener(this);
		// traffic_alert_rl.setOnClickListener(this);
		network_switch_rl.setOnClickListener(this);
		call_et.setOnTouchListener(this);
		sms_et.setOnTouchListener(this);
		traffic_et.setOnTouchListener(this);
		back_rl.setOnClickListener(this);
		modify.setOnClickListener(this);
		overlay_package_rl.setOnClickListener(this);
		traffic_et.addTextChangedListener(watcher);
		call_et.addTextChangedListener(watcher1);
		sms_et.addTextChangedListener(watcher2);

	}

	private void initUI() {
		call_et = (EditText) findViewById(R.id.call_et);
		if (config.getFreeCallTime() != 100000) {
			old_call_et = config.getFreeCallTime();
			call_et.setText(old_call_et + "");
		} else {
			call_et.setText("");
		}
		sms_et = (EditText) findViewById(R.id.sms_et);
		if (config.getFreeMessages() != 100000) {
			old_sms_et = config.getFreeMessages();
			sms_et.setText(old_sms_et + "");
		} else {
			sms_et.setText("");
		}

		traffic_et = (EditText) findViewById(R.id.traffic_et);
		if (config.getFreeGprs() != 100000) {
			old_traffic_et = config.getFreeGprs();
			traffic_et.setText((int) old_traffic_et + "");
		} else {
			traffic_et.setText("");

		}

		old_traffic_alert = config.getAlertTrafficNotice();
		old_isBrokenNetwork = config.getIsBrokenNetwork();
		old_accountingDay = config.getAccountingDay();

		mSeekBar = (SeekBar) findViewById(R.id.seek);

		accoutDayTextView = (TextView) findViewById(R.id.accounting_day);

		accoutDayTextView.setText(config.getAccountingDay() + "号");

		traffic_alert = config.getFreeGprs() - config.getAlertTrafficNotice();
		seekbar_progress = (int) Math.round(traffic_alert
				/ config.getFreeGprs() * 100);

		mProgressText = (TextView) findViewById(R.id.progress);
		mProgressText.setText(seekbar_progress + "%");
		mSeekBar.setProgress((seekbar_progress - 60) * 5 / 2);

		accountday_rl = (RelativeLayout) findViewById(R.id.accountday_rl);
		back_rl = (RelativeLayout) findViewById(R.id.back_rl);
		modify = (RelativeLayout) findViewById(R.id.modify);
		// traffic_alert_rl = (RelativeLayout) findViewById(R.id.traffic_alert);
		traffic_alert_switch_rl = (RelativeLayout) findViewById(R.id.traffic_alert_switch);
		network_switch_rl = (RelativeLayout) findViewById(R.id.network_switch_rl);
		network_switch = (RelativeLayout) findViewById(R.id.network_switch);
		alert_click_left_iv = (ImageView) findViewById(R.id.alert_click_left);
		alert_click_right_iv = (ImageView) findViewById(R.id.alert_click_right);
		network_click_left_iv = (ImageView) findViewById(R.id.network_click_left);
		network_click_right_iv = (ImageView) findViewById(R.id.network_click_right);

		switchshow(network_switch, network_click_left_iv,
				network_click_right_iv, config.getIsBrokenNetwork());
		switchshow(traffic_alert_switch_rl, alert_click_left_iv,
				alert_click_right_iv, config.getTrafficAlertSwitch());

		overlay_package_rl = (RelativeLayout) findViewById(R.id.overlay_package_rl);
		overlay_package_switch_rl = (RelativeLayout) findViewById(R.id.overlay_package_switch_rl);
		overlay_package_click_left = (ImageView) findViewById(R.id.overlay_package_click_left);
		overlay_package_click_right = (ImageView) findViewById(R.id.overlay_package_click_right);
		// if(bJS()){
		// overlay_package_rl.setVisibility(View.VISIBLE);
		// switchshow(overlay_package_switch_rl, overlay_package_click_left,
		// overlay_package_click_right, config.getOverlayPackageSwitch());
		// }
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (v == call_et) {
			call_et.requestFocus();
			call_et.selectAll();
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showSoftInput(v, 0);
			return true;
		} else if (v == sms_et) {
			sms_et.requestFocus();
			sms_et.selectAll();
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showSoftInput(v, 0);
			return true;
		} else if (v == traffic_et) {
			traffic_et.requestFocus();
			traffic_et.selectAll();
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showSoftInput(v, 0);
			return true;
		} else {
			return false;
		}

	}

	@Override
	public void onClick(View v) {
		if (v == accountday_rl) {
			showCustomDialog();
		} else if (v == modify) {
			savedata();
			Toast.makeText(getApplication(), "修改成功", Toast.LENGTH_SHORT).show();
			finish();
			overridePendingTransition(R.anim.push_right_in,
					R.anim.push_right_out);
		} else if (v == back_rl) {
			// config.setFreeCallTime(old_call_et);
			// config.setFreeMessages(old_sms_et);
			// config.setFreeGprs(old_traffic_et);
			// config.setAlertTrafficNotice(old_traffic_alert);
			config.setIsBrokenNetwork(old_isBrokenNetwork);
			config.setAccountingDay(old_accountingDay);
			finish();
			overridePendingTransition(R.anim.push_right_in,
					R.anim.push_right_out);
		}
		// else if (v == traffic_alert_rl) {
		// if (config.getTrafficAlertSwitch()) {
		// config.setTrafficAlertSwitch(false);
		// config.setIsBrokenNetwork(true);
		// } else {
		// config.setTrafficAlertSwitch(true);
		// config.setIsBrokenNetwork(false);
		// }
		// switchshow(traffic_alert_switch_rl, alert_click_left_iv,
		// alert_click_right_iv, config.getTrafficAlertSwitch());
		// switchshow(network_switch, network_click_left_iv,
		// network_click_right_iv, config.getIsBrokenNetwork());
		// }
		else if (v == network_switch_rl) {
			if (config.getIsBrokenNetwork()) {
				config.setIsBrokenNetwork(false);
				// config.setTrafficAlertSwitch(true);
			} else {
				config.setIsBrokenNetwork(true);
				config.setTrafficBeyond(false);
				// 判断是否超流量
				if (FirewallUtils.isGprsEnabled(PackageSettingActivity.this)) { // 判断GPRS网络是否连接
					try {
						if (config.getFreeGprs() != 100000) {
							float remain = config.getFreeGprs()
									* 1024
									* 1024
									- (config.getTotalGprsUsed() + config
											.getTotalGprsUsedDifference());
							if (remain < 0) {
								startActivity(new Intent(
										PackageSettingActivity.this,
										TrafficWarningActivity.class));
								config.setTrafficBeyond(true);
							}
						}
					} catch (Exception e) {
						ILog.logException(this.getClass(), e);
					}
				} else {
					ILog.LogI(this.getClass(), "GPRS网络处于关闭状态");
				}
				// config.setTrafficAlertSwitch(false);
			}
			switchshow(network_switch, network_click_left_iv,
					network_click_right_iv, config.getIsBrokenNetwork());
			// switchshow(traffic_alert_switch_rl, alert_click_left_iv,
			// alert_click_right_iv, config.getTrafficAlertSwitch());
		} else if (v == overlay_package_rl) {
			if (config.getOverlayPackageSwitch()) {
				// Log.i("free",
				// "getOverlayPackageSwitch()1111======"
				// + config.getOverlayPackageSwitch());
				config.setOverlayPackageSwitch(false);
				// Log.i("free",
				// "getOverlayPackageSwitch()2222======"
				// + config.getOverlayPackageSwitch());
			} else {
				// Log.i("free",
				// "getOverlayPackageSwitch()3333======"
				// + config.getOverlayPackageSwitch());
				config.setOverlayPackageSwitch(true);
				// Log.i("free",
				// "getOverlayPackageSwitch()4444======"
				// + config.getOverlayPackageSwitch());
			}
			switchshow(overlay_package_switch_rl, overlay_package_click_left,
					overlay_package_click_right,
					config.getOverlayPackageSwitch());
		}
	}

	protected void onResume() {
		super.onResume();
		// 获取对应EditText的焦点
		switch (edit_index) {
		case 1:
			call_et.requestFocus();
			break;
		case 2:
			sms_et.requestFocus();
			break;
		case 3:
			traffic_et.requestFocus();
			break;
		default:
			break;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(conn);
	}

	@Override
	// 在拖动中--即值在改变
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		// progress为当前数值的大小

		seekbar_progress = 60 + progress * 2 / 5;
		mProgressText.setText(seekbar_progress + "%");

	}

	@Override
	// 在拖动中会调用此方法
	public void onStartTrackingTouch(SeekBar seekBar) {

	}

	@Override
	// 停止拖动
	public void onStopTrackingTouch(SeekBar seekBar) {

	}

	private void savedata() {
		if (call_et.getText().toString() != null
				&& !"".equals(call_et.getText().toString())) {
			FreeCallTime = Integer.parseInt(call_et.getText().toString());
		} else {
			FreeCallTime = 100000;
		}
		if (sms_et.getText().toString() != null
				&& !"".equals(sms_et.getText().toString())) {
			FreeMessages = Integer.parseInt(sms_et.getText().toString());
		} else {
			FreeMessages = 100000;
		}
		if (traffic_et.getText().toString() != null
				&& !"".equals(traffic_et.getText().toString())) {
			FreeGprs = Float.parseFloat(traffic_et.getText().toString());
			if (FreeGprs != config.getFreeGprs()) {
				REFLESH_NOTIFICATION = true;
			}
		} else {
			FreeGprs = 100000;
		}

		config.setFreeCallTime(FreeCallTime);
		config.setFreeMessages(FreeMessages);
		config.setFreeGprs(FreeGprs);

		traffic_alert = FreeGprs * (100 - seekbar_progress) / 100f;

		if (traffic_alert != config.getAlertTrafficNotice()) {
			REFLESH_NOTIFICATION = true;
		}

		config.setAlertTrafficNotice(traffic_alert);

		/* 修改包月流量后刷新通知栏 */

		if (REFLESH_NOTIFICATION) {
			config.setMonthTrafficWarn(false);
			config.setTrafficBeyond(false);
			if (config.getStatusKeepNotice()) {
				if (binder != null) {
					binder.open_notification();
				}
			}
			REFLESH_NOTIFICATION = false;

		}

	}

	public void showCustomDialog() {

		CustomListDialog dialog = new CustomListDialog(this);
		// 设置对话框的标题
		dialog.setTitle("选择月结日");
		// 0: 默认第一个单选按钮被选中
		dialog.setSingleChoiceItems(getAcount_days(), day_index(),
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						ACOUNT_DAY_DIALOG = true;
						accountday = getAcount_days()[which];
					}
				});
		// 确定按钮
		dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				// Log.i("free", "ACOUNT_DAY_DIALOG===== "+ACOUNT_DAY_DIALOG);
				if (!ACOUNT_DAY_DIALOG) {
					accountday = getAcount_days()[config.getAccountingDay() - 1];
				}
				// Log.i("free", "accountday===== "+accountday);

				String dayStr = accountday;
				int day = Integer.parseInt(dayStr.substring(0,
						dayStr.length() - 1));
				accoutDayTextView.setText(dayStr);
				config.setAccountingDay(day);
			}
		});

		// 创建一个单选按钮对话框
		dialog.show();
	}

	public String[] getAcount_days() {
		Calendar calendar = Calendar.getInstance();
		int days = calendar.getActualMaximum(Calendar.DATE);
		String[] dates = new String[days];
		for (int i = 0; i < days; i++) {
			dates[i] = (i + 1) + "号";
		}
		return dates;

	}

	public int day_index() {
		Calendar calendar = Calendar.getInstance();
		int days = calendar.getActualMaximum(Calendar.DATE);
		int index = 0;
		if (!"".equals(accoutDayTextView.getText().toString())) {
			String dayStr = accoutDayTextView.getText().toString();
			int len = dayStr.length();
			int day = Integer.parseInt(dayStr.substring(0, len - 1));
			index = day - 1;
		} else {
			index = 0;
		}

		if (index > (days - 1)) {
			index = days - 1;
		}
		return index;

	}

	public void switchshow(RelativeLayout rl, ImageView iv_left,
			ImageView iv_right, Boolean bswitch) {
		if (!bswitch) {
			rl.setBackgroundResource(R.drawable.wqy_setting);
			iv_left.setVisibility(View.VISIBLE);
			iv_right.setVisibility(View.GONE);
		} else {
			rl.setBackgroundResource(R.drawable.yqy_setting);
			iv_left.setVisibility(View.GONE);
			iv_right.setVisibility(View.VISIBLE);
		}

	}

	private TextWatcher watcher = new TextWatcher() {

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {

		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {

		}

		@Override
		public void afterTextChanged(Editable s) {
			String editText = s.toString();
			if (editText.length() == 2 && editText.equals("00")) {
				traffic_et.setText("0");
			}
		}
	};
	private TextWatcher watcher1 = new TextWatcher() {

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {

		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {

		}

		@Override
		public void afterTextChanged(Editable s) {
			String editText = s.toString();
			if (editText.length() == 2 && editText.equals("00")) {
				call_et.setText("0");
			}
		}
	};
	private TextWatcher watcher2 = new TextWatcher() {

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {

		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {

		}

		@Override
		public void afterTextChanged(Editable s) {
			String editText = s.toString();
			if (editText.length() == 2 && editText.equals("00")) {
				sms_et.setText("0");
			}
		}
	};

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
	// // public Boolean bJS() {
	// // if (config.getProvince().equals("江苏")
	// // && config.getOperator().equals("中国移动")) {
	// // return true;
	// // }
	// // return false;
	// //
	//
	// }
}
