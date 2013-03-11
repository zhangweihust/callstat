package com.archermind.callstat.home.settings;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.archermind.callstat.CallStatApplication;
import com.archermind.callstat.ConfigManager;
import com.archermind.callstat.ILog;
import com.archermind.callstat.R;
import com.archermind.callstat.accounting.MessageManager;
import com.archermind.callstat.accounting.ReconciliationUtils;
import com.archermind.callstat.common.CallStatUtils;
import com.archermind.callstat.home.views.ToastFactory;
import com.archermind.callstat.service.CallStatSMSService;

public class CallsCheckActivity extends Activity implements OnClickListener,
		OnTouchListener {

	private RelativeLayout backBtn; // 返回键
	private RelativeLayout check_automatic_layout; // 自动校正
	private RelativeLayout check_from_operator_layout; // 向运营商查询
	private RelativeLayout hand_input_layout; // 手动输入
	private TextView check_time_prompt_tv;
	private TextView autoCheckTxt;
	private TextView checkTextView1, checkTextView2, checkTextView3;
	// private ImageView callCheck_back;

	private ConfigManager config;
	private CallFeesReceiver callFeesReceiver;
	private IntentFilter callFeesFilter;
	private TextView clueTextView; // 自动校正开启提示
	private TextView check_timeView; // 上次查询时间

	static boolean TOTLE_SPENT = false;// 判断对账是否获取到总支出
	boolean FEES_REMAIN = false;// 判断对账是否获取到余额
	boolean FIRST_CHECKSPENT_SUCCESS = false; // 第一次成功的对账

	private CallStatSMSService.SMSBinder binder;

	private ServiceConnection conn = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub
			binder = (CallStatSMSService.SMSBinder) service;
		}
	};

	private class CallFeesReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (ReconciliationUtils.NOTICE_ACTIVITY_ACOUNTING_SUCCESS_ACTION_CALLS
					.equals(intent.getAction())) {
				int index = 0;
				if (intent.getExtras() != null) {
					index = intent.getExtras().getInt("index");
				}
				switch (index) {
				case MessageManager.TYPE_CONSUME_FEE: // 对出已用话费信息
					ILog.LogI(this.getClass(),
							"CallManagerActivity 接收到 “已用话费”对账成功 的广播");
					showCheckTime();
					break;
				case MessageManager.TYPE_AVAIL_FEE: // 对出话费余额信息
					ILog.LogI(this.getClass(),
							"CallManagerActivity 接收到 “可用余额”对账成功 的广播");
					showCheckTime();
					break;
				default:
					break;
				}

			} else if (ReconciliationUtils.PERMANENT_3_MINUTES_ACCOUNTING_FAILED_TIME_OUT_ACTION
					.equals(intent.getAction())) {
				showCheckTime();
			} else if (ReconciliationUtils.PERMANENT_3_MINUTES_ACCOUNTING_FAILED_TIME_OUT_ACTION_CALLS
					.equals(intent.getAction())) {
				showCheckTime();
			}

		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		if (getIntent().getExtras() != null) {
			if (getIntent().getExtras().getInt("calls_check_anim") == 1) {
				overridePendingTransition(R.anim.push_left_in,
						R.anim.push_left_out);
			}
		}
		setContentView(R.layout.calls_check);
		config = new ConfigManager(this);
		callFeesReceiver = new CallFeesReceiver();
		callFeesFilter = new IntentFilter();
		callFeesFilter
				.addAction(ReconciliationUtils.NOTICE_ACTIVITY_ACOUNTING_SUCCESS_ACTION_CALLS);
		callFeesFilter
				.addAction(ReconciliationUtils.PERMANENT_3_MINUTES_ACCOUNTING_FAILED_TIME_OUT_ACTION);
		callFeesFilter
				.addAction(ReconciliationUtils.PERMANENT_3_MINUTES_ACCOUNTING_FAILED_TIME_OUT_ACTION_CALLS);
		registerReceiver(callFeesReceiver, callFeesFilter);
		init();

		Intent intent = new Intent(this, CallStatSMSService.class);
		if (!CallStatUtils.isServiceRunning(this,
				"com.archermind.callstat.service.CallStatSMSService")) {
			startService(intent);
		}
		bindService(intent, conn, Context.BIND_AUTO_CREATE);
	}

	protected void onResume() {
		super.onResume();
		showCheckTime();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(callFeesReceiver);
		unbindService(conn);
	}

	/**
	 * init
	 */
	private void init() {
		backBtn = (RelativeLayout) findViewById(R.id.callCheck_rl);
		check_automatic_layout = (RelativeLayout) findViewById(R.id.calls_check_automatic_layout);
		check_from_operator_layout = (RelativeLayout) findViewById(R.id.check_from_operator_layout);
		hand_input_layout = (RelativeLayout) findViewById(R.id.hand_input_layout);
		autoCheckTxt = (TextView) findViewById(R.id.calls_check_automatic_warn);
		// clueTextView = (TextView)
		// findViewById(R.id.calls_check_automatic_warn1);
		check_timeView = (TextView) findViewById(R.id.check_time_prompt);

		checkTextView1 = (TextView) findViewById(R.id.calls_check_automatic_string1);
		checkTextView2 = (TextView) findViewById(R.id.calls_check_automatic_string2);
		checkTextView3 = (TextView) findViewById(R.id.calls_check_automatic_string3);
		// callCheck_back = (ImageView) findViewById(R.id.callCheck_back);

		check_time_prompt_tv = (TextView) findViewById(R.id.check_time_prompt);
		backBtn.setOnClickListener(this);
		check_automatic_layout.setOnClickListener(this);
		check_from_operator_layout.setOnClickListener(this);
		hand_input_layout.setOnClickListener(this);

		backBtn.setOnTouchListener(this);
		check_automatic_layout.setOnTouchListener(this);
		check_from_operator_layout.setOnTouchListener(this);
		hand_input_layout.setOnTouchListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		/* 返回按钮 */
		case R.id.callCheck_rl:
			finish();
			overridePendingTransition(R.anim.push_right_in,
					R.anim.push_right_out);
			break;
		/* 自动校正 */
		case R.id.calls_check_automatic_layout:
			toOtherActivity(AutoMoneyInputDialog.class);
			break;
		/* 向运营商查询 */
		case R.id.check_from_operator_layout:
			// Log.e("my",
			// "configManager.getHasNotReceivedSmsMiui()="
			// + config.getHasNotReceivedSmsMiui());
			if (ReconciliationUtils.IsCheckingAccount
					&& !ReconciliationUtils.IsCheckingAccount3MIn) {
				// Log.i("xx", "正在查询中……");
				if (CallStatApplication.calls_anim_is_run) {
					ToastFactory.getToast(this, "正在查询中", Toast.LENGTH_SHORT)
							.show();
				} else {
					CallStatApplication.calls_anim_is_run = true;
					check_timeView.setText("正在查询中");
					ToastFactory.getToast(this, "开始查询", Toast.LENGTH_SHORT)
							.show();
					ReconciliationUtils
							.getInstance()
							.getHandler()
							.removeMessages(
									ReconciliationUtils.THREE_MINUTES_TOAST_TIME_OUT_CALLS);
					ReconciliationUtils
							.getInstance()
							.getHandler()
							.sendEmptyMessageDelayed(
									ReconciliationUtils.THREE_MINUTES_TOAST_TIME_OUT_CALLS,
									ReconciliationUtils.PERMINENT_3_MINUTES); // 3分钟之后发送对账超过3分钟提示
				}

				return;
			}

			if (ReconciliationUtils.IsCheckingAccount
					&& ReconciliationUtils.IsCheckingAccount3MIn) {
				// 提示手动校正话费
				if (CallStatApplication.calls_anim_is_run) {
					ToastFactory.getToast(this, "正在查询中", Toast.LENGTH_SHORT)
							.show();
				} else {
					CallStatApplication.calls_anim_is_run = true;
					check_timeView.setText("正在查询中");
					ToastFactory.getToast(this, "开始查询", Toast.LENGTH_SHORT)
							.show();
					ReconciliationUtils
							.getInstance()
							.getHandler()
							.removeMessages(
									ReconciliationUtils.THREE_MINUTES_TOAST_TIME_OUT_CALLS);
					ReconciliationUtils
							.getInstance()
							.getHandler()
							.sendEmptyMessageDelayed(
									ReconciliationUtils.THREE_MINUTES_TOAST_TIME_OUT_CALLS,
									ReconciliationUtils.PERMINENT_3_MINUTES); // 3分钟之后发送对账超过3分钟提示
				}

			} else {
				if (binder != null) {
					CallStatApplication.calls_anim_is_run = true;
					switch (binder
							.sendAccounting(ReconciliationUtils.SEND_QUERY)) {
					case 0:
						ToastFactory.getToast(this, "当前处于飞行模式中，不能进行对账！",
								Toast.LENGTH_SHORT).show();
						break;
					case 1:
						check_timeView.setText("正在查询中");
						ToastFactory.getToast(this, "开始查询", Toast.LENGTH_SHORT)
								.show();
						TOTLE_SPENT = false;
						break;
					}
				}
			}

			break;
		/* 手动输入 */
		case R.id.hand_input_layout:
			startActivity(new Intent(this, CallsInputDialogActivity.class));
			break;
		default:
			break;
		}
	}

	@SuppressWarnings("rawtypes")
	private void toOtherActivity(Class class1) {
		Intent intent = new Intent();
		intent.setClass(this, class1);
		startActivity(intent);
	}

	// 获取最近一次成功对账的时间差
	private String getLastCheckTime() {
		long now = System.currentTimeMillis();
		long last = config.getLastCheckHasYeTime();
		String lastCheckTime = null;
		if (last != 0) {
			int time = (int) ((now - last) / 60000);
			int day = time / 1440;
			int hour = time / 60;
			if (day > 0) {
				lastCheckTime = day + "天";
			} else {
				if (hour > 0) {
					lastCheckTime = (time / 60) + "小时";
				} else {
					if (time > 0) {
						lastCheckTime = time + "分钟";
					} else {
						if (now - last >= 0) {
							lastCheckTime = (int) ((now - last) / 1000) + "秒";
						} else {
							lastCheckTime = "0秒";
						}

					}

				}
			}
		} else {
			lastCheckTime = 0 + "分钟";
		}

		return lastCheckTime;

	}

	private void showCheckTime() {
		if (config.isAutoCheck()) {
			autoCheckTxt.setText("自动校正已启用，每" + config.getAccountFrequency()
					+ "小时校正话费数据。");
		} else {
			autoCheckTxt.setText("自动校正已关闭");
		}

		if (ReconciliationUtils.IsCheckingAccount
		// &&
		// ReconciliationUtils.getInstance().getHandler().hasMessages(ReconciliationUtils.THREE_MINUTES_TOAST_TIME_OUT_CALLS)
				&& CallStatApplication.calls_anim_is_run) {
			if (ReconciliationUtils
					.getInstance()
					.getHandler()
					.hasMessages(
							ReconciliationUtils.THREE_MINUTES_TOAST_TIME_OUT_CALLS)) {
				check_time_prompt_tv.setText("正在查询中");
			} else {
				check_time_prompt_tv.setText("正在后台处理中");
			}
		} else {
			if (config.getLastCheckHasYeTime() != -1) {
				// Log.i("x", "getLastCheckTime() = " + getLastCheckTime());
				long now = System.currentTimeMillis();
				long last = config.getLastCheckHasYeTime();
				if ((now - last) / 60000 < 10) {
					check_time_prompt_tv.setText("上次成功查询：刚刚");
				} else {
					check_time_prompt_tv.setText("上次成功查询：" + getLastCheckTime()
							+ "前");
				}
			} else {
				check_time_prompt_tv.setText("上次查询失败");
			}
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.calls_check_automatic_layout:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				checkTextView1.setTextColor(getResources().getColor(
						R.color.white));
				autoCheckTxt.setTextColor(getResources()
						.getColor(R.color.white));
				check_automatic_layout
						.setBackgroundResource(R.drawable.set_biglist_blue);
				return false;

			case MotionEvent.ACTION_UP:
				checkTextView1.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				autoCheckTxt.setTextColor(getResources().getColor(
						R.color.setting_text));
				check_automatic_layout
						.setBackgroundResource(R.drawable.set_biglist_bar);
				return false;

			case MotionEvent.ACTION_CANCEL:
				checkTextView1.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				autoCheckTxt.setTextColor(getResources().getColor(
						R.color.setting_text));
				check_automatic_layout
						.setBackgroundResource(R.drawable.set_biglist_bar);
				return false;
			default:
				break;
			}

			break;
		case R.id.check_from_operator_layout:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				checkTextView2.setTextColor(getResources().getColor(
						R.color.white));
				check_timeView.setTextColor(getResources().getColor(
						R.color.white));
				check_from_operator_layout
						.setBackgroundResource(R.drawable.set_biglist_blue);
				return false;

			case MotionEvent.ACTION_UP:
				checkTextView2.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				check_timeView.setTextColor(getResources().getColor(
						R.color.setting_text));
				check_from_operator_layout
						.setBackgroundResource(R.drawable.set_biglist_bar);
				return false;

			case MotionEvent.ACTION_CANCEL:
				checkTextView2.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				check_timeView.setTextColor(getResources().getColor(
						R.color.setting_text));
				check_from_operator_layout
						.setBackgroundResource(R.drawable.set_biglist_bar);
				return false;
			default:
				break;
			}

			break;
		case R.id.hand_input_layout:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				checkTextView3.setTextColor(getResources().getColor(
						R.color.white));
				hand_input_layout
						.setBackgroundResource(R.drawable.set_biglist_blue);
				return false;

			case MotionEvent.ACTION_UP:
				checkTextView3.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				hand_input_layout
						.setBackgroundResource(R.drawable.set_biglist_bar);
				return false;

			case MotionEvent.ACTION_CANCEL:
				checkTextView3.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				hand_input_layout
						.setBackgroundResource(R.drawable.set_biglist_bar);
				return false;
			default:
				break;
			}

			break;

		case R.id.callCheck_rl:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				// callCheck_back.setImageResource(R.drawable.back_arrow_x);
				// backBtn.setBackgroundResource(R.drawable.list_view_bg);
				return false;

			case MotionEvent.ACTION_UP:
				// callCheck_back.setImageResource(R.drawable.back_arrow);
				// backBtn.setBackgroundResource(0);
				return false;

			case MotionEvent.ACTION_CANCEL:
				// callCheck_back.setImageResource(R.drawable.back_arrow);
				// backBtn.setBackgroundResource(0);
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
