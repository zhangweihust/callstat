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
import com.archermind.callstat.R;
import com.archermind.callstat.accounting.ReconciliationUtils;
import com.archermind.callstat.common.CallStatUtils;
import com.archermind.callstat.home.views.ToastFactory;
import com.archermind.callstat.service.CallStatSMSService;

public class TrafficCheckActivity extends Activity implements OnClickListener,
		OnTouchListener {

	private RelativeLayout backBtn; // 返回键
	private RelativeLayout check_automatic_layout; // 自动校正
	private RelativeLayout check_from_operator_layout; // 向运营商查询
	private RelativeLayout hand_input_layout; // 手动输入
	private ConfigManager config;
	private TrafficFeesReceiver trafficFeesReceiver;
	// private TextView check_time_prompt_tv;
	private IntentFilter trafficFeesFilter;
	private TextView clueTextView; // 自动校正开启提示
	private TextView check_timeView; // 上次查询时间
	static boolean TOTLE_SPENT = false;// 判断对账是否获取到总支出
	boolean FEES_REMAIN = false;// 判断对账是否获取到余额
	boolean FIRST_CHECKSPENT_SUCCESS = false; // 第一次成功的对账
	boolean CHECKSPENT = false; // 判断是否点击了对账按钮

	private TextView checkTextView1, checkTextView2, checkTextView3;
	// private ImageView callCheck_back;

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

	private class TrafficFeesReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (ReconciliationUtils.NOTICE_ACTIVITY_ACOUNTING_SUCCESS_ACTION_TRAFFIC
					.equals(intent.getAction())) {
				// Log.i("free", "校正成功traffic");
				// Toast.makeText(context, "校正成功！", 3000).show();
				showCheckTime();
			} else if (ReconciliationUtils.NOTICE_ACTIVITY_ACOUNTING_FAILED_ACTION_TRAFFIC
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
			if (getIntent().getExtras().getInt("traffic_check_anim") == 1) {
				overridePendingTransition(R.anim.push_left_in,
						R.anim.push_left_out);
			}
		}
		setContentView(R.layout.traffic_check);
		config = new ConfigManager(this);
		trafficFeesReceiver = new TrafficFeesReceiver();
		trafficFeesFilter = new IntentFilter();
		trafficFeesFilter
				.addAction(ReconciliationUtils.NOTICE_ACTIVITY_ACOUNTING_SUCCESS_ACTION_TRAFFIC);
		trafficFeesFilter
				.addAction(ReconciliationUtils.NOTICE_ACTIVITY_ACOUNTING_FAILED_ACTION_TRAFFIC);
		registerReceiver(trafficFeesReceiver, trafficFeesFilter);
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
		unregisterReceiver(trafficFeesReceiver);
		unbindService(conn);
	}

	/**
	 * init
	 */
	private void init() {
		backBtn = (RelativeLayout) findViewById(R.id.trafficCheck_rl);
		check_automatic_layout = (RelativeLayout) findViewById(R.id.traffic_check_automatic_layout);
		check_from_operator_layout = (RelativeLayout) findViewById(R.id.check_from_operator_layout);
		hand_input_layout = (RelativeLayout) findViewById(R.id.hand_input_layout);
		clueTextView = (TextView) findViewById(R.id.traffic_check_automatic_warn);
		check_timeView = (TextView) findViewById(R.id.check_time_prompt);

		checkTextView1 = (TextView) findViewById(R.id.traffic_check_automatic_string1);
		checkTextView2 = (TextView) findViewById(R.id.traffic_check_automatic_string2);
		checkTextView3 = (TextView) findViewById(R.id.traffic_check_automatic_string3);
		// callCheck_back = (ImageView) findViewById(R.id.trafficCheck_back);

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
		case R.id.trafficCheck_rl:
			finish();
			overridePendingTransition(R.anim.push_right_in,
					R.anim.push_right_out);
			break;
		/* 自动校正 */
		case R.id.traffic_check_automatic_layout:
			startActivity(new Intent()
					.setClass(this, AutoCorrectActivity.class));
			break;
		/* 向运营商查询 */
		case R.id.check_from_operator_layout:

			// Log.e("my",
			// "configManager.getHasNotReceivedSmsMiui()="
			// + config.getHasNotReceivedSmsMiui());
			if (ReconciliationUtils.IsCheckingTraffic
					&& !ReconciliationUtils.IsCheckingTraffic3Min) {
				if (CallStatApplication.traffic_anim_is_run) {
					ToastFactory.getToast(this, "正在查询中", Toast.LENGTH_SHORT)
							.show();
				} else {
					CallStatApplication.traffic_anim_is_run = true;
					check_timeView.setText("正在查询中");
					ToastFactory.getToast(this, "开始查询", Toast.LENGTH_SHORT)
							.show();
					ReconciliationUtils
							.getInstance()
							.getHandler()
							.removeMessages(
									ReconciliationUtils.THREE_MINUTES_TOAST_TIME_OUT_TRAFFIC);
					ReconciliationUtils
							.getInstance()
							.getHandler()
							.sendEmptyMessageDelayed(
									ReconciliationUtils.THREE_MINUTES_TOAST_TIME_OUT_TRAFFIC,
									ReconciliationUtils.PERMINENT_3_MINUTES); // 3分钟之后发送对账超过3分钟提示
				}
				return;
			}

			if (ReconciliationUtils.IsCheckingTraffic
					&& ReconciliationUtils.IsCheckingTraffic3Min) {
				// 提示手动校正流量
				if (CallStatApplication.traffic_anim_is_run) {
					ToastFactory.getToast(this, "正在查询中", Toast.LENGTH_SHORT)
							.show();
				} else {
					CallStatApplication.traffic_anim_is_run = true;
					check_timeView.setText("正在查询中");
					ToastFactory.getToast(this, "开始查询", Toast.LENGTH_SHORT)
							.show();
					ReconciliationUtils
							.getInstance()
							.getHandler()
							.removeMessages(
									ReconciliationUtils.THREE_MINUTES_TOAST_TIME_OUT_TRAFFIC);
					ReconciliationUtils
							.getInstance()
							.getHandler()
							.sendEmptyMessageDelayed(
									ReconciliationUtils.THREE_MINUTES_TOAST_TIME_OUT_TRAFFIC,
									ReconciliationUtils.PERMINENT_3_MINUTES); // 3分钟之后发送对账超过3分钟提示
				}
			} else {
				if (binder != null) {
					CallStatApplication.traffic_anim_is_run = true;
					switch (binder
							.sendAccounting(ReconciliationUtils.SEND_QUERY)) {
					case 0:
						ToastFactory.getToast(this, "当前处于飞行模式中，不能进行对账！", 2000)
								.show();
						break;
					case 1:
						check_timeView.setText("正在查询中");
						ToastFactory.getToast(this, "开始查询", 2000).show();
						TOTLE_SPENT = false;
						break;
					}

				}
			}
			break;
		/* 手动输入 */
		case R.id.hand_input_layout:
			startActivity(new Intent(this, TrafficInputDialog.class));
			break;
		default:
			break;
		}
	}

	// 获取最近一次成功对账的时间差
	private String getLastCheckTime() {
		long now = System.currentTimeMillis();
		long last = config.getLastCheckHasTrafficTime();
		// Log.i("x", "last ---------------------------= " + last);
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
		if (config.isTrafficAutoCheckOn()) {
			clueTextView.setText("自动校正已开启，每" + config.getAccountFrequency()
					+ "小时校正流量数据。");
		} else {
			clueTextView.setText("自动校正已关闭");
		}

		if (ReconciliationUtils.IsCheckingTraffic
		// &&
		// ReconciliationUtils.getInstance().getHandler().hasMessages(ReconciliationUtils.THREE_MINUTES_TOAST_TIME_OUT_TRAFFIC)
				&& CallStatApplication.traffic_anim_is_run) {
			if (ReconciliationUtils
					.getInstance()
					.getHandler()
					.hasMessages(
							ReconciliationUtils.THREE_MINUTES_TOAST_TIME_OUT_TRAFFIC)) {
				check_timeView.setText("正在查询中");
			} else {
				check_timeView.setText("正在后台处理中");
			}
		} else {
			if (config.getLastCheckHasTrafficTime() != -1) {
				// Log.i("x", "getLastCheckTime() = " + getLastCheckTime());
				long now = System.currentTimeMillis();
				long last = config.getLastCheckHasTrafficTime();
				if ((now - last) / 60000 < 10) {
					check_timeView.setText("上次成功查询：刚刚");
				} else {
					check_timeView
							.setText("上次成功查询：" + getLastCheckTime() + "前");
				}

			} else {
				check_timeView.setText("上次查询失败");
			}
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.traffic_check_automatic_layout:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				checkTextView1.setTextColor(getResources().getColor(
						R.color.white));
				clueTextView.setTextColor(getResources()
						.getColor(R.color.white));
				check_automatic_layout
						.setBackgroundResource(R.drawable.set_biglist_blue);
				return false;

			case MotionEvent.ACTION_UP:
				checkTextView1.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				clueTextView.setTextColor(getResources().getColor(
						R.color.setting_text));
				check_automatic_layout
						.setBackgroundResource(R.drawable.set_biglist_bar);
				return false;

			case MotionEvent.ACTION_CANCEL:
				checkTextView1.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				clueTextView.setTextColor(getResources().getColor(
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

		case R.id.trafficCheck_rl:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				// callCheck_back.setImageResource(R.drawable.back_arrow_x);
				backBtn.setBackgroundResource(R.drawable.list_view_bg);
				return false;

			case MotionEvent.ACTION_UP:
				// callCheck_back.setImageResource(R.drawable.back_arrow);
				backBtn.setBackgroundResource(0);
				return false;

			case MotionEvent.ACTION_CANCEL:
				// callCheck_back.setImageResource(R.drawable.back_arrow);
				backBtn.setBackgroundResource(0);
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
