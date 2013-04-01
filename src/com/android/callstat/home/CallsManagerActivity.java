package com.android.callstat.home;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
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
import android.os.Looper;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
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

import com.android.callstat.CallStatApplication;
import com.android.callstat.ConfigManager;
import com.android.callstat.ILog;
import com.android.callstat.accounting.MessageManager;
import com.android.callstat.accounting.ReconciliationUtils;
import com.android.callstat.common.CallStatUtils;
import com.android.callstat.common.DeviceUtils;
import com.android.callstat.common.database.CallStatDatabase;
import com.android.callstat.common.download.CacheFileManager;
import com.android.callstat.home.adapter.MySettingAdapter;
import com.android.callstat.home.bean.SettingBean;
import com.android.callstat.home.settings.CallsBudgetSettingActivity;
import com.android.callstat.home.settings.ManuallyInputAfterFailedDialog;
import com.android.callstat.home.settings.SystemSettingNewActivity;
import com.android.callstat.home.views.ArcBar;
import com.android.callstat.home.views.ChartView;
import com.android.callstat.home.views.ToastFactory;
import com.android.callstat.home.views.ChartView.OnCoordinateChanged;
import com.android.callstat.monitor.ObserverManager;
import com.android.callstat.service.CallStatSMSService;
import com.archermind.callstat.R;

public class CallsManagerActivity extends Activity implements OnClickListener,
		OnCoordinateChanged, OnTouchListener, OnKeyListener {

	private ConfigManager configManager;
	ListView settinglv;
	ProgressDialog progressDialog;
	/* init */
	private TextView calls_remain_textView; // 账户余额
	private Button calls_checkBtn; // 校正话费按钮
	private TextView callsbudget_used_tView; // 预算已用
	private TextView callsbudget_remain_tView; // 总预算
	private TextView callsbudget_beyond_tView; // 预算超出
	private RelativeLayout calls_details_layout; // 话费详单
	private ImageView icon_change_ImageView;
	private TextView string_change_TextView;
	static LinearLayout settingBtn_layout; // 设置按钮

	private ArcBar myProgressBar; // 环形进度条
	private ChartView mChartView;// 日消费曲线图

	private TextView tv_day_of_month_title = null;
	private TextView tv_day_of_month = null;
	private TextView tv_day_of_spend_title = null;
	private TextView tv_day_of_spend = null;

	private TextView details_imageBtn;
	private TextView arrow_rightBtn;
	public static Thread initLogThread = null;
	final static int MESSAGE_WHAT = 0;
	LinearLayout layout;

	ProgressBar isAccountPbar;
	TextView callRemainStringTextView;

	RelativeLayout beyond_rl;

	/* data */
	private float calls_remain; // 账户余额
	private float calls_budget; // 总预算
	private float calls_budget_used; // 预算已用
	private float calls_budget_remain; // 预算余额
	private float degree = 360;

	private CallFeesReceiver callFeesReceiver;
	private IntentFilter callFeesFilter;

	PopupWindow puw;
	boolean puwIsShown = true;
	LinearLayout setting_bnt, share_bnt;
	String action;
	public static boolean isInitLogsDone = false;

	private CallStatSMSService.SMSBinder binder;
	private ServiceConnection conn = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			binder = (CallStatSMSService.SMSBinder) service;
		}
	};

	/**
	 * BroadcastReceiver 内部类，用于接受消息
	 * 
	 * @author root
	 * 
	 */
	private class CallFeesReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent
					.getAction()
					.equals(ReconciliationUtils.NOTICE_ACTIVITY_ACOUNTING_SUCCESS_ACTION_CALLS)) {
				int index = 0;
				if (intent.getExtras() != null) {
					index = intent.getExtras().getInt("index");
				}
				switch (index) {
				case MessageManager.TYPE_CONSUME_FEE: // 对出已用话费信息
					ILog.LogI(this.getClass(),
							"CallManagerActivity 接收到 “已用话费”对账成功 的广播");
					callsManager_data();
					myProgressBar.refleshUI();
					break;
				case MessageManager.TYPE_AVAIL_FEE: // 对出话费余额信息
					ILog.LogI(this.getClass(),
							"CallManagerActivity 接收到 “可用余额”对账成功 的广播");
					callsManager_data();
					myProgressBar.refleshUI();
					break;
				default:
					break;
				}

			} else if (intent.getAction().equals(
					ReconciliationUtils.NOTICE_START_ACCOUNT)) { // 开始对账，通知UI显示正在获取数据
				ILog.LogI(this.getClass(), "CallManagerActivity 接收到 开始全对账 的广播");
				callsManager_data();
			} else if (intent.getAction().equals(
					ReconciliationUtils.NOTICE_START_ACCOUNT_CALLS)) { // 开始话费对账，通知UI显示正在获取数据
				ILog.LogI(this.getClass(), "CallManagerActivity 接收到 开始话费对账 的广播");
				callsManager_data();
			} else if (intent
					.getAction()
					.equals(ReconciliationUtils.PERMANENT_3_MINUTES_ACCOUNTING_FAILED_TIME_OUT_ACTION)) { // 3分钟，通知UI停止转圈
				ILog.LogI(this.getClass(), "CallManagerActivity 接收到全对账3分钟超时的广播");
				callsManager_data();
				myProgressBar.refleshUI();
				configManager.setCallsCorrectState(false);
			} else if (intent
					.getAction()
					.equals(ReconciliationUtils.PERMANENT_3_MINUTES_ACCOUNTING_FAILED_TIME_OUT_ACTION_CALLS)) { // 3分钟，通知UI停止转圈
				ILog.LogI(this.getClass(),
						"CallManagerActivity 接收到话费对账3分钟超时的广播");
				if (CallStatApplication.calls_anim_is_run) {
					showToast("运营商忙或指令有误，请稍候再试！");
				}
				callsManager_data();
				myProgressBar.refleshUI();
				configManager.setCallsCorrectState(false);
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.calls_manager);
		mChartView = new ChartView(this);
		configManager = new ConfigManager(this);
		action = getIntent().getAction();
		receiver_init();
		initPopupwindow();
		callsManager_init();
		if (configManager.getHFUsedCode() == null) {
			CallStatDatabase.getInstance(this)
					.initReconciliationInfo2ConfigXml(
							configManager.getProvince(),
							configManager.getCity(),
							configManager.getOperator(),
							configManager.getPackageBrand(),
							CallStatApplication.AllCodeRestore);
		}
		if ("first".equals(action)) {
			Toast.makeText(this, "正在向运营商查询您的话费信息，这个过程大约需要1-2分钟，请稍候。",
					Toast.LENGTH_LONG).show();
			CallStatDatabase.getInstance(this)
					.initReconciliationInfo2ConfigXml(
							configManager.getProvince(),
							configManager.getCity(),
							configManager.getOperator(),
							configManager.getPackageBrand(),
							CallStatApplication.AllCodeRestore);
			CallStatApplication.Keywordslist.clear();
			CallStatApplication.Keywordslist.add(configManager
					.getFeeUsedKeywords());
			CallStatApplication.Keywordslist.add(configManager
					.getFeeAvailKeywords());
			CallStatApplication.Keywordslist.add(configManager
					.getTrafficUsedKeywords());
			CallStatApplication.Keywordslist.add(configManager
					.getTrafficAvailKeywords());
			initLogThread = new Thread(new Runnable() {

				@Override
				public void run() {
					ILog.LogI(getClass(), "Looper.prepare();");
					Looper.prepare();
					long time = System.currentTimeMillis();
					ILog.LogI(getClass(), "time :" + time);
					configManager.setFirstLaunch(true);
					ObserverManager.getObserverManager(
							CallsManagerActivity.this).initLogs();
					isInitLogsDone = true;
					configManager.setFirstLaunch(false);
					ILog.LogI(getClass(),
							"time costs:" + (System.currentTimeMillis() - time));
				}
			});
			initLogThread.start();
		} else {
			CallStatApplication.Keywordslist.clear();
			CallStatApplication.Keywordslist.add(configManager
					.getFeeUsedKeywords());
			CallStatApplication.Keywordslist.add(configManager
					.getFeeAvailKeywords());
			CallStatApplication.Keywordslist.add(configManager
					.getTrafficUsedKeywords());
			CallStatApplication.Keywordslist.add(configManager
					.getTrafficAvailKeywords());
		}

		Intent intent = new Intent(this, CallStatSMSService.class);
		getParent().bindService(intent, conn, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onResume() {
		super.onResume();
		/* 刷新数据 */
		callsManager_data();
		myProgressBar.refleshUI();
		// 从数据库中取得日消费数据并显示
		setDayConsumeData();

		if (configManager.getCallsBudget() == 100000) {
			callsbudget_remain_tView.setTextColor(getResources()
					.getColorStateList(R.drawable.not_setting_string_selector));
		} else {
			callsbudget_remain_tView.setTextColor(getResources().getColor(
					R.color.setting_text_black));
		}

		// 用户行为采集
		try {
			if (mHandler.hasMessages(MESSAGE_WHAT)) {
				mHandler.removeMessages(MESSAGE_WHAT);
			}
			mHandler.sendEmptyMessageDelayed(MESSAGE_WHAT, 5000);
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	public void setDayConsumeData() {
		try {
			java.util.Date utilDate = new java.util.Date();
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
			String NowDate = formatter.format(utilDate);
			CallStatDatabase.getInstance(this).refreshEachDayFeeConsume();
			float[] data = CallStatDatabase.getInstance(this)
					.getDayFeeSpendArray(NowDate);// 先从数据库中把历史数据（包括至今日的数据读出来）

			mChartView.setData(data);
			mChartView.setDaysOfMonth(CallStatUtils.getCurrentMonthLastDay());
			mChartView.setConsumeType(0);
			mChartView.setCounterIndex(0);
			tv_day_of_month_title.setText("当前:");
			tv_day_of_month.setText(mChartView.getCurrentDayOfMonth() + "号");
			tv_day_of_spend_title.setText("消费:");
			tv_day_of_spend.setText(CallStatUtils.changeFloat(mChartView
					.getCurrentDay_consume()) + "元");
			mChartView.refreshUi(); // 刷新曲线图
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			getParent().unregisterReceiver(callFeesReceiver);
			getParent().unbindService(conn);
			if (mHandler.hasMessages(MESSAGE_WHAT)) {
				mHandler.removeMessages(MESSAGE_WHAT);
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	/* 控件初始化 */
	private void callsManager_init() {
		myProgressBar = (ArcBar) findViewById(R.id.myprogressbar);
		calls_remain_textView = (TextView) findViewById(R.id.callsmanager_remain);
		calls_checkBtn = (Button) findViewById(R.id.callsmanager_checkbtn);
		callsbudget_used_tView = (TextView) findViewById(R.id.calls_budget_used);
		callsbudget_remain_tView = (TextView) findViewById(R.id.calls_budget_remain);
		callsbudget_beyond_tView = (TextView) findViewById(R.id.calls_budget_beyond);
		beyond_rl = (RelativeLayout) findViewById(R.id.beyond_rl);
		calls_details_layout = (RelativeLayout) findViewById(R.id.calls_details_layout);
		calls_details_layout.setBackgroundResource(R.drawable.details_border);
		calls_details_layout.setPadding(0, 0, 0, 0);
		icon_change_ImageView = (ImageView) findViewById(R.id.calls_icon_change);
		string_change_TextView = (TextView) findViewById(R.id.calls_string_change);
		layout = (LinearLayout) findViewById(R.id.chartView_layout);

		settingBtn_layout = (LinearLayout) findViewById(R.id.calls_settingBtn_layout);
		details_imageBtn = (TextView) findViewById(R.id.details_imageBtn);
		arrow_rightBtn = (TextView) findViewById(R.id.arrow_rightBtn);

		isAccountPbar = (ProgressBar) findViewById(R.id.mProgressBar);
		callRemainStringTextView = (TextView) findViewById(R.id.callsmanager_remain_title);

		tv_day_of_month_title = (TextView) findViewById(R.id.the_day_of_month_title);
		tv_day_of_month = (TextView) findViewById(R.id.the_day_of_month);
		tv_day_of_spend_title = (TextView) findViewById(R.id.the_day_of_spent_title);
		tv_day_of_spend = (TextView) findViewById(R.id.the_day_of_spent);
		calls_checkBtn.setOnClickListener(this);
		calls_details_layout.setOnTouchListener(this);
		calls_details_layout.setOnClickListener(this);
		settingBtn_layout.setOnClickListener(this);
		callsbudget_remain_tView.setOnClickListener(this);

		try {
			addView(); // 曲线图
			mChartView.setmOnCoordinateChanged(this);
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	/* 接收广播初始化 */
	private void receiver_init() {
		callFeesReceiver = new CallFeesReceiver();
		callFeesFilter = new IntentFilter(
				ReconciliationUtils.NOTICE_ACTIVITY_ACOUNTING_SUCCESS_ACTION_CALLS);
		callFeesFilter
				.addAction(ReconciliationUtils.NOTICE_ACTIVITY_ACOUNTING_FAILED_ACTION_CALLS);
		callFeesFilter.addAction(CallStatSMSService.PHONE_SHUTDOWN_ACTION);
		callFeesFilter.addAction(ReconciliationUtils.NOTICE_START_ACCOUNT);
		callFeesFilter
				.addAction(ReconciliationUtils.NOTICE_START_ACCOUNT_CALLS);
		callFeesFilter
				.addAction(ReconciliationUtils.PERMANENT_3_MINUTES_ACCOUNTING_FAILED_TIME_OUT_ACTION);
		callFeesFilter
				.addAction(ReconciliationUtils.PERMANENT_3_MINUTES_ACCOUNTING_FAILED_TIME_OUT_ACTION_CALLS);
		getParent().registerReceiver(callFeesReceiver, callFeesFilter);
	}

	/* 获取数据并显示 */
	private void callsManager_data() {
		try {
			// 判断是否正在对账
			ILog.LogI(this.getClass(), "calls_anim_is_run ="
					+ CallStatApplication.calls_anim_is_run);
			if (ReconciliationUtils.IsCheckingAccount
					&& ReconciliationUtils
							.getInstance()
							.getHandler()
							.hasMessages(
									ReconciliationUtils.THREE_MINUTES_TOAST_TIME_OUT_CALLS)
					&& CallStatApplication.calls_anim_is_run) {
				calls_is_accounting(); // 正在对账中
			} else {
				isAccountPbar.setVisibility(View.GONE);
				callRemainStringTextView.setVisibility(View.VISIBLE);
				callsRemain_data();// 获取账户余额
			}

			calls_budgut_data(); // 预算，已用，预算剩余及环形进度条的数据
			calls_warn(); // 报警、超出的控制
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	/* 显示正在对账 */
	private void calls_is_accounting() {
		try {
			calls_remain_textView.setTextSize(18);
			calls_remain_textView.setText("正在对账");
			calls_remain_textView.setTextColor(getResources().getColor(
					R.color.is_accounting_string_color));
			isAccountPbar.setVisibility(View.VISIBLE);
			callRemainStringTextView.setVisibility(View.GONE);
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	/* 账户余额显示 */
	private void callsRemain_data() {
		try {
			calls_remain_textView.setTextSize(23);
			if (configManager.getCalculateFeeAvailable() != 100000) { // 对出余额
				calls_remain = configManager.getCalculateFeeAvailable();
				ILog.LogI(this.getClass(), "获取到余额信息：余额 = " + calls_remain);
				calls_remain_textView.setText(CallStatUtils
						.changeFloat(calls_remain) + "");
				if (calls_remain > configManager.getAlertRemainFees()) { // 未报警
					calls_remain_textView.setTextColor(getResources().getColor(
							R.color.callsremain_nomal_color));
				} else { // 报警
					calls_remain_textView.setTextColor(getResources().getColor(
							R.color.callsremain_warn_color));
				}

			} else { // 未对出余额
				calls_remain_textView.setText("未对账");
				calls_remain_textView.setTextColor(getResources().getColor(
						R.color.callsremain_warn_color));
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	/* 总预算 预算已用 预算剩余 */
	private void calls_budgut_data() {
		try {
			/* 总预算 */
			calls_budget = configManager.getCallsBudget();
			String usedString = "";
			String remainString = "";
			String beyondString = "";
			// 预算已用
			double feeSpendTmp = 0;
			if (configManager.getFeeSpent() == 100000f) {
				if (CallsManagerActivity.this != null) {
					double[] ret = CallStatDatabase.getInstance(
							CallsManagerActivity.this).getThisMonthTotalSpend();
					feeSpendTmp = ret[0];
				}
				calls_budget_used = (float) feeSpendTmp;
				if (calls_budget_used < 0) {
					calls_budget_used = 0f;
				}
			} else {
				calls_budget_used = configManager.getFeeSpent();
			}

			usedString = CallStatUtils.changeFloat(calls_budget_used) + "元";

			if (calls_budget != 100000) {
				remainString = CallStatUtils.changeFloat(calls_budget) + "元";
				// 预算剩余/超出
				if (calls_budget != 0) {
					calls_budget_remain = calls_budget - calls_budget_used;
					degree = calls_budget_remain / (float) calls_budget * 360f;
					if (degree < -360) {
						degree = -360;
					}
				} else {
					degree = -360;
					calls_budget_remain = calls_budget;
				}

				// 预算剩余
				if (calls_budget < calls_budget_used) {
					beyondString = CallStatUtils.changeFloat(calls_budget_used
							- calls_budget)
							+ "元";
				}
			} else {
				remainString = "未设置";
				degree = 360;
			}

			callsbudget_beyond_tView.setText(beyondString);
			callsbudget_used_tView.setText(usedString);
			callsbudget_remain_tView.setText(remainString);
			myProgressBar.setDegree(degree);
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	/* 报警值、超出,小图标变化 */
	private void calls_warn() {
		try {
			/* 获取报警值 */
			float alert_calls = configManager.getAlertCallsNotice();
			if ((calls_budget - calls_budget_used) >= 0) { // 预算未超出
				beyond_rl.setVisibility(View.GONE);
				if ((calls_budget - calls_budget_used) > alert_calls) {// 预算未报警
					icon_change_ImageView.setImageResource(R.drawable.green);
				} else { // 预算报警
					icon_change_ImageView.setImageResource(R.drawable.green);
				}
				string_change_TextView
						.setText(R.string.callsmanager_budget_string);
			} else { // 预算超出
				beyond_rl.setVisibility(View.VISIBLE);
			}

			/* 设置圆环进度条的报警值 */
			float warnDegress = 36;
			if (calls_budget != 100000 && calls_budget != 0) {
				warnDegress = configManager.getAlertCallsNotice()
						/ (float) calls_budget * 360;
			}
			myProgressBar.setWarnDegree(warnDegress);
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		switch (id) {
		/* 校正按钮点击事件 */
		case R.id.callsmanager_checkbtn:
			try {
				if (ReconciliationUtils.IsCheckingAccount
						&& !ReconciliationUtils.IsCheckingAccount3MIn) {
					if (CallStatApplication.calls_anim_is_run) {
						ToastFactory.getToast(this, "正在校正", Toast.LENGTH_SHORT)
								.show();
					} else {
						CallStatApplication.calls_anim_is_run = true;
						calls_is_accounting();
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

				/*
				 * if (configManager.getCallsCorrectState()) {
				 * autoCorrectCalls(); } else {
				 * configManager.setCallsCorrectState(true); // 提示手动校正话费
				 * showCorrectDialog(); }
				 */

				if (ReconciliationUtils.IsCheckingAccount
						&& ReconciliationUtils.IsCheckingAccount3MIn) {
					// 提示手动校正话费
					if (CallStatApplication.calls_anim_is_run
							&& isAccountPbar.getVisibility() == View.GONE) {
						showCorrectDialog();
					} else if (CallStatApplication.calls_anim_is_run == false) {
						CallStatApplication.calls_anim_is_run = true;
						calls_is_accounting();
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
					} else {
						ToastFactory.getToast(this, "正在校正", Toast.LENGTH_SHORT)
								.show();
					}

				} else {
					CallStatApplication.calls_anim_is_run = true;
					autoCorrectCalls();
				}

			} catch (Exception e) {
				ILog.logException(this.getClass(), e);
			}

			break;
		/* 话费详单点击 */
		case R.id.calls_details_layout:
			Intent intent = new Intent(CallsManagerActivity.this,
					CallsDetailsActivity.class);
			startActivity(intent);
			break;
		/* 设置按钮点击 */
		case R.id.calls_settingBtn_layout:
			showMenu();
			break;
		/*
		 * case R.id.setting_bnt: showMenu(); goToSystemSetting(); break; case
		 * R.id.share_bnt: showMenu(); new LoadScreenshotTask().execute();
		 * break;
		 */
		case R.id.calls_budget_remain:
			if (configManager.getCallsBudget() == 100000) { // 如果未设置预算则跳到预算设置界面
				startActivity(new Intent(CallsManagerActivity.this,
						CallsBudgetSettingActivity.class));
			}
			break;
		default:
			break;
		}
	}

	private void autoCorrectCalls() {
		if (binder != null) {
			switch (binder.sendAccounting(ReconciliationUtils.SEND_QUERY)) {
			case 0:
				if (CallStatUtils.isMyAppOnDesk(this)) {
					ToastFactory.getToast(this, "当前处于飞行模式中，无法刷新！",
							Toast.LENGTH_SHORT).show();
				}
				break;
			case 1:
				// 正在刷新
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
		 * title.setText("校正话费");
		 * content.setText("检测到您上次自动校正话费失败，现在是否需要手动校正话费");
		 * 
		 * okBtn.setOnClickListener(new OnClickListener() {
		 * 
		 * @Override public void onClick(View v) { dialog.dismiss();
		 * startActivity(new Intent(CallsManagerActivity.this,
		 * CallsCheckActivity.class)); } });
		 * 
		 * cancelBtn.setOnClickListener(new OnClickListener() {
		 * 
		 * @Override public void onClick(View v) { dialog.dismiss();
		 * autoCorrectCalls(); } });
		 * 
		 * dialog.show();
		 */
		Intent intent = new Intent(CallsManagerActivity.this,
				ManuallyInputAfterFailedDialog.class);
		intent.putExtra("manual_correct_index", 0);
		startActivity(intent);
	}

	@Override
	public void onCoordinateChanged(int x, float y, boolean isUp) {
		try {
			if (isUp) {
				tv_day_of_month_title.setText("当前:");
				tv_day_of_month
						.setText(mChartView.getCurrentDayOfMonth() + "号");
				tv_day_of_spend_title.setText("消费:");
				tv_day_of_spend.setText(CallStatUtils.changeFloat(mChartView
						.getCurrentDay_consume()) + "元");

			} else {
				tv_day_of_month_title.setText("日期:");
				tv_day_of_month.setText(String.valueOf(x) + "号");
				tv_day_of_spend_title.setText("消费:");
				tv_day_of_spend.setText(CallStatUtils.changeFloat(y) + "元");
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	public void initPopupwindow() {
		try {
			View view = getLayoutInflater().inflate(
					R.layout.setting_popup_window, null);

			settinglv = (ListView) view.findViewById(R.id.setting_lv);
			MySettingAdapter adapter = new MySettingAdapter(this,
					getSettingBean());
			// settinglv.setDivider(null);
			settinglv.setAdapter(adapter);
			settinglv.setOnItemClickListener(new SettingBodyClickEvent());
			settinglv.setOnKeyListener(this);
			/*
			 * setting_bnt = (LinearLayout) view.findViewById(R.id.setting_bnt);
			 * share_bnt = (LinearLayout) view.findViewById(R.id.share_bnt);
			 * setting_bnt.setOnClickListener(this);
			 * share_bnt.setOnClickListener(this);
			 */

			// 解决点击menu键，popupwindow不消失的问题

			/*
			 * view.setFocusableInTouchMode(true); view.setOnKeyListener(this);
			 */

			float density = DeviceUtils.getDeviceDisplayDensity(this);
			puw = new PopupWindow(view, (int) (120 * density),
					(int) (47 * density * 2 + 13 * density), true);

			puw.setBackgroundDrawable(getResources().getDrawable(
					R.drawable.setpopup));
			puw.getContentView().setPadding(0, 0, 0, 0);
			// 点击空白区域popupwindow消失
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

	public static List<SettingBean> getSettingBean() {
		List<SettingBean> list = new ArrayList<SettingBean>();

		SettingBean setting = new SettingBean("设置");
		SettingBean share = new SettingBean("分享");

		list.add(setting);
		list.add(share);

		return list;
	}

	private void goToSystemSetting() {
		Intent intent = new Intent();
		intent.setClass(this, SystemSettingNewActivity.class);
		intent.putExtra("anim_type", 1);
		startActivity(intent);
	}

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

	public void showMenu() {
		try {
			if (puw != null) {
				if (puw.isShowing()) {
					puw.dismiss();
				} else {
					puw.showAsDropDown(settingBtn_layout, -puw.getWidth() / 2
							- settingBtn_layout.getWidth() / 2, 5);
					puwIsShown = true;
				}
			}
		} catch (Exception e) {
			ILog.logException(CallsManagerActivity.class, e);
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			details_imageBtn.setBackgroundResource(R.drawable.details_light);
			arrow_rightBtn.setBackgroundResource(R.drawable.arrow_right_light);
			calls_details_layout
					.setBackgroundResource(R.drawable.details_border_blue);
			calls_details_layout.setPadding(0, 0, 0, 0);
			return false;
		case MotionEvent.ACTION_UP:
			details_imageBtn.setBackgroundResource(R.drawable.details);
			arrow_rightBtn.setBackgroundResource(R.drawable.arrow_right);
			calls_details_layout
					.setBackgroundResource(R.drawable.details_border);
			calls_details_layout.setPadding(0, 0, 0, 0);
			return false;
		case MotionEvent.ACTION_CANCEL:
			details_imageBtn.setBackgroundResource(R.drawable.details);
			arrow_rightBtn.setBackgroundResource(R.drawable.arrow_right);
			calls_details_layout
					.setBackgroundResource(R.drawable.details_border);
			calls_details_layout.setPadding(0, 0, 0, 0);
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
				CallStatUtils.get_height(CallsManagerActivity.this));
		mLayoutParams.setMargins(10, 5, 15, 0);
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

	// 设置PopupWindow的子View相应menu键，使点击MENU键后PopupWindow消失
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

	@Override
	protected void onStop() {
		super.onStop();
	}

	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case MESSAGE_WHAT:
				ILog.LogI(this.getClass(), "用户行为采集: 话费界面次数 + 1 = "
						+ CallsManagerActivity.this.getClass().getSimpleName());
				CallStatDatabase.getInstance(CallsManagerActivity.this)
						.updateActivityStatistic(
								CallsManagerActivity.this.getClass()
										.getSimpleName(),
								configManager.getVersionName());
				break;
			default:
				break;
			}
		}
	};
}
