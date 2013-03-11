package com.archermind.callstat.home;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
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
import com.archermind.callstat.common.DeviceUtils;
import com.archermind.callstat.common.database.CallStatDatabase;
import com.archermind.callstat.common.download.CacheFileManager;
import com.archermind.callstat.firewall.TrafficTopActivity;
import com.archermind.callstat.home.adapter.MySettingAdapter;
import com.archermind.callstat.home.settings.ManuallyInputAfterFailedDialog;
import com.archermind.callstat.home.settings.PackageSettingActivity;
import com.archermind.callstat.home.settings.SystemSettingNewActivity;
import com.archermind.callstat.home.views.ArcBar;
import com.archermind.callstat.home.views.ChartView;
import com.archermind.callstat.home.views.ChartView.OnCoordinateChanged;
import com.archermind.callstat.home.views.ToastFactory;
import com.archermind.callstat.service.CallStatSMSService;

public class TrafficManagerActivity extends Activity implements
		OnClickListener, OnCoordinateChanged, OnTouchListener, OnKeyListener {

	private ConfigManager configManager;

	/* init */
	private TextView traffic_remain_textView; // 剩余流量
	private Button traffic_checkBtn; // 校正流量按钮
	private TextView traffic_used_tView; // 流量已用
	private TextView traffic_total_tView; // 总流量
	private TextView traffic_beyond_tView; // 流量超出
	private TextView traffic_total_title;
	private RelativeLayout traffic_details_layout; // 流量排行
	private TextView traffic_center_tView; // 圆盘中间的剩余 或 超出
	private ImageView icon_ImageView;
	RelativeLayout beyond_rl;

	private LinearLayout settingBtn; // 设置按钮

	private TextView traffic_imageBtn;
	private TextView arrow_rightBtn;

	LinearLayout layout;

	private ArcBar trafficProgress;
	private ChartView mChartView;// 日消费曲线图
	private TextView tv_day_of_month_title = null;
	private TextView tv_day_of_month = null;
	private TextView tv_day_of_spend_title = null;
	private TextView tv_day_of_spend = null;
	/* data */
	private float total_gprs;
	private long month_used;
	private long traffic_remain;
	private float traffic_degree;
	//private long traffic_margin;// 对帐获取到的流量剩余

	ProgressBar isAccountPbar;
	TextView trafficRemainStringTextView;

	PopupWindow puw;
	LinearLayout setting_bnt, share_bnt;
	boolean puwIsShown = true;
	ListView settinglv;
	final static int MESSAGE_WHAT_TRAFFIC = 0;

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

	private TrafficReceiver trafficReceiver;
	private IntentFilter trafficFilter;

	/**
	 * BroadcastReceiver 内部类，用于接受消息
	 * 
	 * @author root
	 * 
	 */
	private class TrafficReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			/* 接受对账成功的消息，重新读取配置文件，然后刷新界面 */
			if (ReconciliationUtils.NOTICE_ACTIVITY_ACOUNTING_SUCCESS_ACTION_TRAFFIC
					.equals(intent.getAction())) {
				int index = 0;
				if (intent.getExtras() != null) {
					index = intent.getExtras().getInt("index");
				}
				switch (index) {
				case MessageManager.TYPE_AVAIL_TRAFFIC:
					traffic_data();
					trafficProgress.refleshUI();
					configManager.setFlowCorrectState(true);
					break;
				case MessageManager.TYPE_CONSUME_TRAFFIC:
					ILog.LogI(this.getClass(),
							"TrafficManagerActivity 接收到 “已用流量”对账成功 的广播");
					traffic_data();
					trafficProgress.refleshUI();
					configManager.setFlowCorrectState(true);
					break;
				default:
					break;
				}

			} else if (intent.getAction().equals(
					ReconciliationUtils.NOTICE_START_ACCOUNT)) {// 开始对账，UI开始转圈
				ILog.LogI(this.getClass(),
						"TrafficManagerActivity 接收到 开始全对账 的广播");
				traffic_data();
			} else if (intent.getAction().equals(
					ReconciliationUtils.NOTICE_START_ACCOUNT_TRAFFIC)) {// 开始流量对账，UI开始转圈
				ILog.LogI(this.getClass(),
						"TrafficManagerActivity 接收到 开始流量对账 的广播");
				traffic_data();
			} else if (intent
					.getAction()
					.equals(ReconciliationUtils.PERMANENT_3_MINUTES_ACCOUNTING_FAILED_TIME_OUT_ACTION)) {// 停止转圈
				ILog.LogI(this.getClass(),
						"TrafficManagerActivity 接收到 全对账3分钟超时 的广播");
				traffic_data();
				trafficProgress.refleshUI();
				configManager.setFlowCorrectState(false);
			} else if (intent
					.getAction()
					.equals(ReconciliationUtils.PERMANENT_3_MINUTES_ACCOUNTING_FAILED_TIME_OUT_ACTION_TRAFFIC)) {// 停止转圈
				ILog.LogI(this.getClass(),
						"TrafficManagerActivity 接收到 流量对账3分钟超时 的广播");
				if (CallStatApplication.traffic_anim_is_run) {
					showToast("运营商忙或指令有误，请稍候再试！");
				}
				traffic_data();
				trafficProgress.refleshUI();
				configManager.setFlowCorrectState(false);
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.traffic_manager);
		configManager = new ConfigManager(this);
		mChartView = new ChartView(this);

		initPopupwindow();
		traffic_init(); // 控件初始化
		trafficReceiver_init(); // 广播接收初始化

		Intent intent = new Intent(this, CallStatSMSService.class);
		getParent().bindService(intent, conn, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		traffic_data();
		trafficProgress.refleshUI(); // 环形仪表 动画效果
		setDayConsumeData();

		if (configManager.getFreeGprs() == 100000) {
			traffic_total_tView.setTextColor(getResources().getColorStateList(
					R.drawable.not_setting_string_selector));
		} else {
			traffic_total_tView.setTextColor(getResources().getColor(
					R.color.setting_text_black));
		}

		try {
			// 用户行为采集
			if (mHandler.hasMessages(MESSAGE_WHAT_TRAFFIC)) {
				mHandler.removeMessages(MESSAGE_WHAT_TRAFFIC);
			}
			mHandler.sendEmptyMessageDelayed(MESSAGE_WHAT_TRAFFIC, 5000);
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	/* 接收器初始化 */
	private void trafficReceiver_init() {
		trafficReceiver = new TrafficReceiver();
		trafficFilter = new IntentFilter(
				ReconciliationUtils.NOTICE_ACTIVITY_ACOUNTING_SUCCESS_ACTION_TRAFFIC);
		trafficFilter
				.addAction(ReconciliationUtils.NOTICE_ACTIVITY_ACOUNTING_FAILED_ACTION_TRAFFIC);
		trafficFilter.addAction(CallStatSMSService.PHONE_SHUTDOWN_ACTION);
		trafficFilter.addAction(ReconciliationUtils.NOTICE_START_ACCOUNT);
		trafficFilter
				.addAction(ReconciliationUtils.NOTICE_START_ACCOUNT_TRAFFIC);
		trafficFilter
				.addAction(ReconciliationUtils.PERMANENT_3_MINUTES_ACCOUNTING_FAILED_TIME_OUT_ACTION);
		trafficFilter
				.addAction(ReconciliationUtils.PERMANENT_3_MINUTES_ACCOUNTING_FAILED_TIME_OUT_ACTION_TRAFFIC);
		getParent().registerReceiver(trafficReceiver, trafficFilter);
	}

	public void setDayConsumeData() {
		try {
			java.util.Date utilDate = new java.util.Date();
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
			String NowDate = formatter.format(utilDate);

			float[] data = CallStatDatabase.getInstance(this).getDayGprsSpend(
					NowDate);
			mChartView.setData(data);
			mChartView.setDaysOfMonth(CallStatUtils.getCurrentMonthLastDay());
			mChartView.setConsumeType(1);
			mChartView.setCounterIndex(0);
			tv_day_of_month_title.setText("当前:");
			tv_day_of_month.setText(mChartView.getCurrentDayOfMonth() + "号");
			tv_day_of_spend_title.setText("消费:");
			String traffic_show = CallStatUtils.traffic_unit1((long) mChartView
					.getCurrentDay_consume())[0]
					+ CallStatUtils.traffic_unit1((long) mChartView
							.getCurrentDay_consume())[1];
			tv_day_of_spend.setText(traffic_show);
			mChartView.refreshUi(); // 刷新曲线图
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			getParent().unregisterReceiver(trafficReceiver);
			getParent().unbindService(conn);

			if (mHandler.hasMessages(MESSAGE_WHAT_TRAFFIC)) {
				mHandler.removeMessages(MESSAGE_WHAT_TRAFFIC);
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	/* traffic ui_init */
	private void traffic_init() {
		trafficProgress = (ArcBar) findViewById(R.id.trafficprogressbar);
		traffic_remain_textView = (TextView) findViewById(R.id.trafficmanager_remain);
		traffic_used_tView = (TextView) findViewById(R.id.traffic_budget_used);
		traffic_total_tView = (TextView) findViewById(R.id.traffic_budget_remain);
		traffic_checkBtn = (Button) findViewById(R.id.trafficmanager_checkbtn);
		traffic_details_layout = (RelativeLayout) findViewById(R.id.traffic_details_layout);
		settingBtn = (LinearLayout) findViewById(R.id.traffic_settingBtn_layout);
		tv_day_of_month_title = (TextView) findViewById(R.id.traffic_the_day_of_month_title);
		tv_day_of_month = (TextView) findViewById(R.id.traffic_the_day_of_month);
		tv_day_of_spend_title = (TextView) findViewById(R.id.traffic_the_day_of_spent_title);
		tv_day_of_spend = (TextView) findViewById(R.id.traffic_the_day_of_spent);
		traffic_center_tView = (TextView) findViewById(R.id.trafficmanager_centerString);

		beyond_rl = (RelativeLayout) findViewById(R.id.beyond_rl);
		traffic_beyond_tView = (TextView) findViewById(R.id.traffic_budget_beyond);

		icon_ImageView = (ImageView) findViewById(R.id.traffic_icon_change);

		traffic_total_title = (TextView) findViewById(R.id.traffic_string_change);

		traffic_imageBtn = (TextView) findViewById(R.id.traffic_detail_imageBtn);
		arrow_rightBtn = (TextView) findViewById(R.id.arrow_rightBtn);

		traffic_details_layout.setBackgroundResource(R.drawable.details_border);
		traffic_details_layout.setPadding(0, 0, 0, 0);
		layout = (LinearLayout) findViewById(R.id.chartView_layout);

		isAccountPbar = (ProgressBar) findViewById(R.id.tProgressBar);
		trafficRemainStringTextView = (TextView) findViewById(R.id.trafficmanager_centerString);

		addView();// 曲线图

		traffic_checkBtn.setOnClickListener(this);
		traffic_details_layout.setOnTouchListener(this);
		traffic_details_layout.setOnClickListener(this);
		settingBtn.setOnClickListener(this);
		mChartView.setmOnCoordinateChanged(this);
		traffic_total_tView.setOnClickListener(this);
	}

	public void initPopupwindow() {
		try {
			/*
			 * View view = getLayoutInflater().inflate(
			 * R.layout.new_setting_popup_window, null); setting_bnt =
			 * (LinearLayout) view.findViewById(R.id.setting_bnt); share_bnt =
			 * (LinearLayout) view.findViewById(R.id.share_bnt);
			 * setting_bnt.setOnClickListener(this);
			 * share_bnt.setOnClickListener(this);
			 */
			View view = getLayoutInflater().inflate(
					R.layout.setting_popup_window, null);

			settinglv = (ListView) view.findViewById(R.id.setting_lv);
			MySettingAdapter adapter = new MySettingAdapter(this,
					CallsManagerActivity.getSettingBean());
			// settinglv.setDivider(null);
			settinglv.setAdapter(adapter);
			settinglv.setOnItemClickListener(new SettingBodyClickEvent());
			settinglv.setOnKeyListener(this);
			/*
			 * // 解决点击menu键，popupwindow不消失的问题
			 * view.setFocusableInTouchMode(true); view.setOnKeyListener(this);
			 */
			float density = DeviceUtils.getDeviceDisplayDensity(this);
			puw = new PopupWindow(view, (int) (120 * density),
					(int) (47 * density * 2 + 13 * density), true);
			puw.setBackgroundDrawable(getResources().getDrawable(
					R.drawable.setpopup));
			puw.setOutsideTouchable(true);
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	// popupwindow 中各个子项的点击
	class SettingBodyClickEvent implements OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			switch (arg2) {
			case 0:// goto systemsetting goToSystemSetting();
				puw.dismiss();
				goToSystemSetting();
				break;
			case 1:// share me goToShare();
				puw.dismiss();
				new LoadScreenshotTask().execute();
				break;
			default:
				break;
			}
		}
	}

	private void goToSystemSetting() {
		Intent intent = new Intent();
		intent.setClass(this, SystemSettingNewActivity.class);
		intent.putExtra("anim_type", 1);
		startActivity(intent);
	}

	public void showMenu() {
		try {
			if (puw != null) {
				if (puw.isShowing()) {
					puw.dismiss();
				} else {
					puw.showAsDropDown(settingBtn, -puw.getWidth() / 2
							- settingBtn.getWidth() / 2, 5);
				}
			}
		} catch (Exception e) {
			ILog.logException(CallsManagerActivity.class, e);
		}
	}

	/* traffic data */
	private void traffic_data() {
		try {
			float warnDegress = 36;

			String total_gprsString = "";
			String remain_gprsString = "";
			int a = 0;
			/* 本月使用的流量 */
			month_used = configManager.getTotalGprsUsed()
					+ configManager.getTotalGprsUsedDifference();
			/*
			 * if (month_used == 0) { double trafficSpendTmp = 0; if
			 * (TrafficManagerActivity.this != null) { double[] ret =
			 * CallStatDatabase.getInstance( TrafficManagerActivity.this)
			 * .getThisMonthTotalSpend(); trafficSpendTmp = ret[1]; } month_used
			 * = (long) Math.round(trafficSpendTmp); if (month_used < 0) {
			 * month_used = 0l; } }
			 */

			ILog.LogE(this.getClass(), "configManager.getTotalGprsUsed()="
					+ configManager.getTotalGprsUsed());
			ILog.LogE(this.getClass(),
					"configManager.getTotalGprsUsedDifference()="
							+ configManager.getTotalGprsUsedDifference());
			ILog.LogE(this.getClass(), "month_used=" + month_used);
			/* 套餐总流量 */
			if (configManager.getFreeGprs() != 100000) {
				total_gprs = configManager.getFreeGprs(); // 总的GPRS流量
				if (total_gprs >= changeToMb(month_used)) {
					traffic_center_tView
							.setText(R.string.trafficmanager_remain_string);
				} else {
					traffic_center_tView
							.setText(R.string.trafficmanager_beyond_string);
				}
				/* 本月剩余流量 */
				traffic_remain = (long) (total_gprs * 1024f * 1024f)
						- month_used;
				// 套餐超出
				if (traffic_remain >= 0) {
					beyond_rl.setVisibility(View.GONE);
					traffic_degree = changeToMb(traffic_remain) / total_gprs
							* 360;
					if (changeToMb(Math.abs(traffic_remain)) > configManager
							.getAlertTrafficNotice()) {
						a = 1;
					} else {
						a = 2;
					}
				} else {
					a = 3;
					beyond_rl.setVisibility(View.VISIBLE);
					traffic_degree = -changeToMb(-traffic_remain) / total_gprs
							* 360;
					traffic_beyond_tView.setText(CallStatUtils
							.changeFloat(changeToMb(Math.abs(traffic_remain)))
							+ "MB");
				}

				remain_gprsString = CallStatUtils.changeFloat(changeToMb(Math
						.abs(traffic_remain))) + "";
				if (total_gprs >= 10000) {
					total_gprsString = CallStatUtils
							.changeFloat(changeToGB(total_gprs)) + "G";
				} else {
					total_gprsString = CallStatUtils.changeFloat(total_gprs)
							+ "MB";
				}

				if (total_gprs != 0) {
					warnDegress = configManager.getAlertTrafficNotice()
							/ total_gprs * 360;
				}
			} else {
				a = 3;
				total_gprsString = "未设置";
				beyond_rl.setVisibility(View.GONE);
				remain_gprsString = "套餐未设置";
				traffic_degree = 360;
				traffic_center_tView
						.setText(R.string.trafficmanager_remain_string);
				traffic_remain_textView.setTextColor(getResources().getColor(
						R.color.callsremain_beyond_color));
			}

			// 对帐获取到的流量剩余
			/*
			 * if (configManager.getTotalGprsMargin() != -100000) {
			 * traffic_margin = configManager.getTotalGprsMargin();
			 * remain_gprsString = CallStatUtils.changeFloat(changeToMb(Math
			 * .abs(traffic_margin))) + ""; if (configManager.getFreeGprs() !=
			 * 100000) { month_used =
			 * (long)Math.round(configManager.getFreeGprs())*1024l*1024l -
			 * traffic_margin; } } else { ILog.LogI(this.getClass(),
			 * "流量剩余未对出！"); }
			 */

			// 是否正在对账
			ILog.LogI(this.getClass(), "traffic_anim_is_run ="
					+ CallStatApplication.traffic_anim_is_run);
			boolean flag = ReconciliationUtils
					.getInstance()
					.getHandler()
					.hasMessages(
							ReconciliationUtils.THREE_MINUTES_TOAST_TIME_OUT_TRAFFIC);
			if (ReconciliationUtils.IsCheckingTraffic
					&& ReconciliationUtils
							.getInstance()
							.getHandler()
							.hasMessages(
									ReconciliationUtils.THREE_MINUTES_TOAST_TIME_OUT_TRAFFIC)
					&& CallStatApplication.traffic_anim_is_run) {
				traffic_isAccounting(); // 正在对账
				// traffic_degree = 360;
			} else {
				isAccountPbar.setVisibility(View.GONE);
				trafficRemainStringTextView.setVisibility(View.VISIBLE);
				switch (a) {
				case 1:
					traffic_remain_textView.setTextColor(getResources()
							.getColor(R.color.callsremain_nomal_color));
					break;
				case 2:
					traffic_remain_textView.setTextColor(getResources()
							.getColor(R.color.callsremain_warn_color));
					break;
				case 3:
					traffic_remain_textView.setTextColor(getResources()
							.getColor(R.color.callsremain_beyond_color));
					break;
				default:
					break;
				}

				if (configManager.getFreeGprs() != 100000) {
					traffic_remain_textView.setTextSize(23);
				} else {
					traffic_remain_textView.setTextSize(18);
				}
				traffic_remain_textView.setText(remain_gprsString);
			}

			icon_ImageView.setImageResource(R.drawable.green);
			traffic_total_title.setText("套餐:");
			traffic_total_tView.setText(total_gprsString);

			// 流量已用
			float month_used_temp = changeToMb(month_used);
			if (month_used_temp >= 10000) {
				traffic_used_tView.setText(CallStatUtils
						.changeFloat(changeToGB(month_used_temp)) + "G");
			} else {
				traffic_used_tView.setText(CallStatUtils
						.changeFloat(month_used_temp) + "MB");
			}

			if (traffic_degree < -360) {
				traffic_degree = -360;
			}
			// Log.i("xx", "warnDegress = " + warnDegress);
			trafficProgress.setWarnDegree(warnDegress);
			trafficProgress.setDegree(traffic_degree);
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	/* 正在对账 */
	private void traffic_isAccounting() {
		try {
			isAccountPbar.setVisibility(View.VISIBLE);
			trafficRemainStringTextView.setVisibility(View.GONE);

			traffic_remain_textView.setTextSize(18);
			traffic_remain_textView.setText("正在读取数据");
			traffic_remain_textView.setTextColor(getResources().getColor(
					R.color.is_accounting_string_color));
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		/* 校正流量按钮 */
		case R.id.trafficmanager_checkbtn:
			try {
				if (ReconciliationUtils.IsCheckingTraffic
						&& !ReconciliationUtils.IsCheckingTraffic3Min) {

					if (CallStatApplication.traffic_anim_is_run) {
						ToastFactory.getToast(this, "正在校正", Toast.LENGTH_SHORT)
								.show();
					} else {
						CallStatApplication.traffic_anim_is_run = true;
						traffic_isAccounting();
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
				/*
				 * if (configManager.getFlowCorrectState()) {
				 * autoCorrectTraffic(); } else {
				 * configManager.setFlowCorrectState(true); // 提示手动校正流量
				 * showCorrectDialog(); }
				 */

				if (ReconciliationUtils.IsCheckingTraffic
						&& ReconciliationUtils.IsCheckingTraffic3Min) {
					// 提示手动校正流量
					if (CallStatApplication.traffic_anim_is_run
							&& isAccountPbar.getVisibility() == View.GONE) {
						showCorrectDialog();
					} else if (CallStatApplication.traffic_anim_is_run == false) {
						CallStatApplication.traffic_anim_is_run = true;
						traffic_isAccounting();
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
					} else {
						ToastFactory.getToast(this, "正在校正", Toast.LENGTH_SHORT)
								.show();
					}
				} else {
					CallStatApplication.traffic_anim_is_run = true;
					autoCorrectTraffic();
				}

			} catch (Exception e) {
				ILog.logException(this.getClass(), e);
			}
			break;
		/* 流量排行 */
		case R.id.traffic_details_layout:

			Intent intent = new Intent();
			intent.setClass(this, TrafficTopActivity.class);
			startActivity(intent);

			break;
		/* 设置 */
		case R.id.traffic_settingBtn_layout:
			try {
				showMenu();
			} catch (Exception e) {
				ILog.logException(this.getClass(), e);
			}
			break;
		case R.id.setting_bnt:
			showMenu();
			Intent intent1 = new Intent();
			intent1.setClass(this, SystemSettingNewActivity.class);
			intent1.putExtra("anim_type", 1);
			startActivity(intent1);
			break;
		case R.id.share_bnt:
			showMenu();
			new LoadScreenshotTask().execute();
		case R.id.traffic_budget_remain:
			if (configManager.getFreeGprs() == 100000) {
				Intent intent_total = new Intent(TrafficManagerActivity.this,
						PackageSettingActivity.class);
				intent_total.putExtra(CallStatUtils.PACKAGE_SET, 3);
				startActivity(intent_total);
			}
			break;
		default:
			break;
		}
	}

	private void autoCorrectTraffic() {
		if (binder != null) {
			switch (binder.sendAccounting(ReconciliationUtils.SEND_QUERY)) {
			case 0:
				ToastFactory.getToast(this, "当前处于飞行模式中，无法刷新！",
						Toast.LENGTH_SHORT).show();
				break;
			case 1:
				// 开始刷新
				break;
			}
		}
	}

	public void showCorrectDialog() {
		/*
		 * final Dialog dialog = new Dialog(this, R.style.myDialogTheme);
		 * dialog.setContentView(R.layout.correct_dialog);
		 * 
		 * TextView title = (TextView) dialog.findViewById(R.id.title); TextView
		 * content = (TextView) dialog.findViewById(R.id.tv_body); Button okBtn
		 * = (Button) dialog.findViewById(R.id.ok); Button cancelBtn = (Button)
		 * dialog.findViewById(R.id.cancel);
		 * 
		 * title.setText("校正流量");
		 * content.setText("检测到您上次自动校正流量失败，现在是否需要手动校正流量");
		 * 
		 * okBtn.setOnClickListener(new OnClickListener() {
		 * 
		 * @Override public void onClick(View v) { // TODO Auto-generated method
		 * stub dialog.dismiss(); startActivity(new
		 * Intent(TrafficManagerActivity.this, TrafficCheckActivity.class)); }
		 * });
		 * 
		 * cancelBtn.setOnClickListener(new OnClickListener() {
		 * 
		 * @Override public void onClick(View v) { // TODO Auto-generated method
		 * stub dialog.dismiss(); autoCorrectTraffic(); } });
		 * 
		 * dialog.show();
		 */
		Intent intent = new Intent(TrafficManagerActivity.this,
				ManuallyInputAfterFailedDialog.class);
		intent.putExtra("manual_correct_index", 1);
		startActivity(intent);
	}

	/**
	 * 将B转换成MB
	 */
	private float changeToMb(long tra) {
		float afterChage = 0;
		if (tra > 0) {
			afterChage = (float) Math.round(tra / 1024f / 1024f * 100) / 100;

			if (afterChage <= 0.01) {
				afterChage = 0.01f;
			}
		} else {
			afterChage = 0;
		}

		return afterChage;

	}

	/**
	 * 将MB转换成GB
	 */
	private float changeToGB(float tra) {
		float afterChage = 0;
		if (tra > 0) {
			afterChage = (float) Math.round(tra / 1024f * 100) / 100;

			if (afterChage <= 0.01) {
				afterChage = 0.01f;
			}
		} else {
			afterChage = 0;
		}

		return afterChage;

	}

	@Override
	public void onCoordinateChanged(int x, float y, boolean isUp) {
		try {
			if (isUp) {
				tv_day_of_month_title.setText("当前:");
				tv_day_of_month
						.setText(mChartView.getCurrentDayOfMonth() + "号");
				tv_day_of_spend_title.setText("消费:");
				String traffic_show = CallStatUtils
						.traffic_unit1((long) mChartView
								.getCurrentDay_consume())[0]
						+ CallStatUtils.traffic_unit1((long) mChartView
								.getCurrentDay_consume())[1];
				tv_day_of_spend.setText(traffic_show);

			} else {
				tv_day_of_month_title.setText("日期:");
				tv_day_of_month.setText(String.valueOf(x) + "号");
				tv_day_of_spend_title.setText("消费:");
				String traffic_show = CallStatUtils.traffic_unit1((long) y)[0]
						+ CallStatUtils.traffic_unit1((long) y)[1];
				tv_day_of_spend.setText(traffic_show);
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			traffic_imageBtn.setBackgroundResource(R.drawable.ranking_light);
			arrow_rightBtn.setBackgroundResource(R.drawable.arrow_right_light);
			traffic_details_layout
					.setBackgroundResource(R.drawable.details_border_blue);
			traffic_details_layout.setPadding(0, 0, 0, 0);
			return false;
		case MotionEvent.ACTION_UP:
			traffic_imageBtn.setBackgroundResource(R.drawable.traffic_ranking);
			arrow_rightBtn.setBackgroundResource(R.drawable.arrow_right);
			traffic_details_layout
					.setBackgroundResource(R.drawable.details_border);
			traffic_details_layout.setPadding(0, 0, 0, 0);
			return false;
		case MotionEvent.ACTION_CANCEL:
			traffic_imageBtn.setBackgroundResource(R.drawable.traffic_ranking);
			arrow_rightBtn.setBackgroundResource(R.drawable.arrow_right);
			traffic_details_layout
					.setBackgroundResource(R.drawable.details_border);
			traffic_details_layout.setPadding(0, 0, 0, 0);
			return false;
		default:
			break;
		}
		return false;
	}

	/* 动态添加曲线图 */
	private void addView() {
		LinearLayout.LayoutParams mLayoutParams = new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT,
				CallStatUtils.get_height(TrafficManagerActivity.this));
		mLayoutParams.setMargins(10, 5, 15, 0);
		// layout.removeAllViews();
		layout.addView(mChartView, mLayoutParams);
	}

	private void showToast(String string) {
		try {
			if (CallStatUtils.isMyAppOnDesk(this)) {
				ToastFactory.getToast(this, string, Toast.LENGTH_SHORT).show();
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}

	}

	// 设置PopupWindow的子View相应mune键，使点击MENU键后PopupWindow消失
	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			if (puw.isShowing() && puwIsShown) {
				puw.dismiss();
			} else {
				puwIsShown = true;
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		try {
			showMenu();
			puwIsShown = false;
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
		return false;
	}

	// 分享
	class LoadScreenshotTask extends AsyncTask<Void, Void, File> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(File file) {
			super.onPostExecute(file);
			try {
				Intent shareIntent = new Intent(Intent.ACTION_SEND);
				String textMessage = getResources()
						.getString(R.string.share_me).toString();
				shareIntent.putExtra(Intent.EXTRA_TEXT, textMessage);
				shareIntent.putExtra("sms_body", textMessage);
				if (file != null) {
					shareIntent.putExtra(Intent.EXTRA_STREAM,
							Uri.fromFile(file));
				}
				shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				shareIntent.setType("image/jpeg");
				startActivity(Intent.createChooser(shareIntent, "向好友推荐我哦～"));
			} catch (Exception e) {
				ILog.logException(this.getClass(), e);
			}
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
		}

		@Override
		protected File doInBackground(Void... params) {
			// 保存图片至sd卡
			InputStream is = null;
			File imageDir = CacheFileManager.getInstance().getmImageDir();
			String fileName = "acall_share.png";
			FileOutputStream fos = null;
			File image = null;
			try {
				image = new File(imageDir, fileName);
				File parent = image.getParentFile();
				if (!parent.exists()) {
					parent.mkdirs();
				}
				if (!image.exists()) {
					image.createNewFile();
				} else {
					return image;
				}
				AssetManager am = getAssets();
				fos = new FileOutputStream(image);
				is = am.open("share.png");
				byte[] buff = new byte[1024];
				if (null != fos && is != null) {

					while (is.read(buff) != -1) {
						fos.write(buff);
					}
					fos.flush();
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (is != null)
					try {
						is.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				if (fos != null)
					try {
						fos.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
			return image;
		}
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case MESSAGE_WHAT_TRAFFIC:
				ILog.LogI(this.getClass(), "用户行为采集: 流量界面次数 + 1");
				CallStatDatabase.getInstance(TrafficManagerActivity.this)
						.updateActivityStatistic(TrafficManagerActivity.this.getClass().getSimpleName(),
								configManager.getVersionName());
				break;
			default:
				break;
			}
		}
	};

}
