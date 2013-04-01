package com.android.callstat.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.format.Time;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.android.callstat.CallStatApplication;
import com.android.callstat.CallStatReceiver;
import com.android.callstat.ConfigManager;
import com.android.callstat.ILog;
import com.android.callstat.SmsReceiver;
import com.android.callstat.accounting.AccountingKeyWordsBean;
import com.android.callstat.accounting.ReconciliationUtils;
import com.android.callstat.common.CallStatUtils;
import com.android.callstat.common.DeviceInformation;
import com.android.callstat.common.DeviceUtils;
import com.android.callstat.common.MTrafficStats;
import com.android.callstat.common.DeviceInformation.InfoName;
import com.android.callstat.common.ResourceLoader.OnLoadFinishListener;
import com.android.callstat.common.database.CallStatDatabase;
import com.android.callstat.common.json.AbstractWebLoadManager.OnWebLoadListener;
import com.android.callstat.common.net.MyHttpPostHelper;
import com.android.callstat.firewall.FirewallCoreWorker;
import com.android.callstat.firewall.FirewallUtils;
import com.android.callstat.firewall.TrafficTopActivity;
import com.android.callstat.home.CallStatMainActivity;
import com.android.callstat.home.CallsDetailsActivity;
import com.android.callstat.home.CallsManagerActivity;
import com.android.callstat.home.TrafficManagerActivity;
import com.android.callstat.home.bean.SmsVerifyBean;
import com.android.callstat.home.settings.TrafficWarningActivity;
import com.android.callstat.monitor.ObserverManager;
import com.android.callstat.service.json.AccountingDatabaseUpdater;
import com.archermind.callstat.R;

//import com.archermind.callstat.itraffic.TrafficMonitoringThread;

public class CallStatSMSService extends Service {
	// **************SMS*******************
	private CallStatReceiver callStatReceiver;
	private SmsReceiver smsReceiver;
	private IntentFilter smsReceivedFiliter;
	private IntentFilter uploadAccoutingCodeFilter;
	private IntentFilter serviceStateChangeFiliter;
	private IntentFilter connectivityChangeFiliter;

	private AccountingDatabaseUpdater accountingDatabaseUpdater;
	/** 网络连接状态发生改变监控 */
	public static final String CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE";
	/** 通知刷新每个进程的流量信息 */
	public static final String REFRESH_TRAFFIC_LOG_SERVICE_ACTION = "refresh_traffic_log_service_action";
	/** WIFI状态发生改变监控 */
	public static final String WIFI_STATE_CHANGED = "android.net.wifi.WIFI_STATE_CHANGED";
	public static final String UPLOAD_ACC_CODE_ACTION = "upload_acc_code_action";

	/** 有应用程序被安装 */
	public static final String PACKAGEADD_ACTION = "PACKAGEADD_ACTION";
	/** 有应用程序卸载 */
	public static final String PACKAGEREMOVED_ACTION = "PACKAGEREMOVED_ACTION";
	public static final String SERVICE_STATE_CHANGE_ACTION = "SERVICE_STATE_CHANAGE_ACTION";
	public static final String SERVICE_STATE_ACTION = "android.intent.action.SERVICE_STATE";
	public static final String CONNECTIVITY_CHANGE_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
	/** 初始化reveiver中的数据 */
	public static final String INIT_RECEIVER_ACTION = "init_receiver_action";
	public static final String PHONE_SHUTDOWN_ACTION = "phone_shutdown_action";
	public static final String PACKAGE_NAME = "com.archermind.callstat";
	/* 用于接收对账成功的消息，刷新状态栏和悬浮窗 */
	public static final String ACCOUNT_SUCCESS = "account_sucess";

	/** 计时器，用于进行后台自动对账 */
	public static final int QUERY_TIME = 3;

	/** 刷新总的流量信心列表操作 */
	public static final int REFRESH_TRAFFIC_LOG = 6;
	// public static final int REMOVE_SMS_INTERCEPT = 7;

	public static final int TOP_ACTIVITY_CAHNGED = 8;
	public static final int NO_TOP_ACTIVITY_CAHNGED = 9;
	/** 刷新并获得当时的实时网速 */
	public static final int REFESH_NET_SPEED = 10;
	public static final int CHECK_FLOATVIEW_SHOW = 11;
	// public static final int REMOVE_REJECT_ACCOUNT_INFO = 12;
	/** 开关飞行模式 **/
	public static final int AIRPLAN_SWITCH = 13;

	private final int BIND_PHONE = 999;

	private boolean bOpenDialog = false;
	private Time time = new Time();
	// private int Key;
	private int gsmSignalStrength = 0;
	// private String Action;
	private SMSBinder binder = new SMSBinder();

	// private TrafficMonitoringThread trafficMonitoringThread;
	// public static final String NETWORK_STATE_CHANGE_ACTION =
	// "NETWORK_STATE_CHANGE_ACTION";
	public static final int WIFI_STATE = 0;
	public static final int GPRS_STATE = 1;
	public static final int CLOSE_STATE = 2;
	private static final String NETWORK_STATE[] = { "wifi", "gprs", null };
	private int network_state;
	private CallStatSMSService context;
	public ConfigManager configManager;
	public CallStatApplication callStatApplication;
	// private CallStatDatabase callstatDatabase;

	/* Notification */
	private NotificationReceiver notificationReceiver;
	private IntentFilter callTimeFilter;
	private IntentFilter smsSendFilter;
	private NotificationManager nm;
	private Notification notification;
	private Notification warn_notification;
	private Notification charge_notification;
	private long gprs_used; // Gprs使用量
	private long today_gprs_used; // 当日Gprs使用量
	private long notification_gprs_remain; // Gprs剩余量
	private float fees_remain;
	public static String WARN_ACTION = "traffic_warnning";
	public static final String ACTION_TODAY_TRAFFIC = "taday_alarm";
	public static final String ACTION_MONTH_TRAFFIC = "month_alarm";

	public static final String ACTION_VERIFICATION_CODE = "verification_code_action";
	public static final String ACTION_VERIFICATION_RESULT = "verification_code_result_action";
	public static final String SEND_BIND_HANDLE = "send_bind_handle";
	public static final String PHONE_BINDIND_STRING = "phone_binding_string";
	public static final String PHONE_UNBINDIND_STRING = "phone_unbinding_string";
	public static final String PHONE_BINDIND_RESULT = "phone_binding_result";
	public static final String PHONE_UNBINDIND_RESULT = "phone_unbinding_result";

	private int calls_budget; // 总预算
	private float calls_budget_used; // 预算已用
	private float calls_budget_remain; // 预算余额

	private int userStatus_send_fause = 0;

	/* FloatView */
	private View floatview;
	private WindowManager.LayoutParams params;
	private WindowManager windowManager;
	private TextView floatstring;
	private TextView floatstyle;
	private TextView left_tView;
	private TextView right_tView;
	private ImageView imageView1;
	private ImageView imageView2;
	TextView blank;
	private LinearLayout before_onclick;
	private Button closebtn;
	private float[] temp = { 0f, 0f };
	private float[] temp2 = { 0f, 0f };
	private float screenWidth;
	private float screenDensity;

	private int statusBarHeight; // 状态栏的高度
	private boolean addview = false; // 悬浮窗口是否已经存在
	private boolean bOpenAir = false;
	private boolean bShowView = false;
	public static boolean bAccountFalied = true;

	private double netSpeed;
	private long prevTotalTraffic;
	private java.text.DecimalFormat df = new java.text.DecimalFormat("#.#");
	private TelephonyManager Tel;
	private MyPhoneStateListener MyListener;
	private boolean CALLS_CHAEGER_NOTICE = false;

	// *****************TrafficMonitoring end;
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	public class SMSBinder extends Binder {
		/**
		 * activity通过bind此Service可以通过调用此方法来进行对账操作
		 */
		public int sendAccounting(int accounting_type) {
			ILog.LogD(SMSBinder.class, "sendAccounting:" + accounting_type);
			Intent intent;
			if (accounting_type == ReconciliationUtils.SEND_CALL_CHARGES) {
				return ReconciliationUtils.getInstance().queryTelephoneBill();
			} else if (accounting_type == ReconciliationUtils.SEND_TRAFFIC_QUERY) {
				return ReconciliationUtils.getInstance().queryGprsTraffic();
			} else if (accounting_type == ReconciliationUtils.SEND_QUERY) {
				/*
				 * intent = new Intent();
				 * intent.setAction(ReconciliationUtils.NOTICE_START_ACCOUNT);
				 * context.sendBroadcast(intent);
				 */
				return ReconciliationUtils.getInstance().queryReconciliation();
			}
			return -1;
		}

		public void checkUpdateBind() {
			checkUpdate();
		}

		public void closeSmsSendErroDialog() {
			bOpenDialog = false;
		}

		public void open_notification() {
			openNotification();
		}

		public void close_notification() {
			closeNotification();
		}

		public void open_floatWindow() {
			floatwindow_data();
		}

		public void close_floatWindow() {
			removeview();
		}

		public void setFloatWindowOpen() {
			if (!myHandler.hasMessages(CHECK_FLOATVIEW_SHOW)) {
				myHandler.sendEmptyMessage(CHECK_FLOATVIEW_SHOW);
			}
		}

		public void setFloatWindowClose() {
			if (myHandler.hasMessages(CHECK_FLOATVIEW_SHOW)) {
				myHandler.removeMessages(CHECK_FLOATVIEW_SHOW);
			}
			if (myHandler.hasMessages(REFESH_NET_SPEED)) {
				myHandler.removeMessages(REFESH_NET_SPEED);
			}
		}

		public void showDeskOnly() {
			boolean isShowDeskOnly = configManager.getFloatShowDeskOnly();
			boolean isFloatWindowOpen = configManager.getFloatWindowOpen();
			// Log.i("wanglei", "isFloatWindowOpen=" + isFloatWindowOpen
			// + ",isShowDeskOnly=" + isShowDeskOnly);
			if (isFloatWindowOpen) {
				// Log.i("wanglei", "isFloatWindowOpen");
				if (isShowDeskOnly) {
					// Log.i("wanglei", "isShowDeskOnly");

					if (!myHandler.hasMessages(CHECK_FLOATVIEW_SHOW)) {
						myHandler.sendEmptyMessage(CHECK_FLOATVIEW_SHOW);
					}
					if (!myHandler.hasMessages(NO_TOP_ACTIVITY_CAHNGED)) {
						myHandler.sendEmptyMessage(NO_TOP_ACTIVITY_CAHNGED);
					}
				} else {

					// Log.i("wanglei", "send  TOP_ACTIVITY_CAHNGED");
					myHandler.sendEmptyMessage(TOP_ACTIVITY_CAHNGED);

					if (configManager.getFloatWindowIndex() == 3) {
						// Log.i("wanglei", "getFloatWindowIndex()==3");
						if (!myHandler.hasMessages(REFESH_NET_SPEED)) {
							myHandler.sendEmptyMessage(REFESH_NET_SPEED);
							// Log.i("wanglei",
							// "myHandler.sendEmptyMessage(REFESH_NET_SPEED);");
						}
					}
				}
			} else {
				if (myHandler.hasMessages(CHECK_FLOATVIEW_SHOW)) {
					myHandler.removeMessages(CHECK_FLOATVIEW_SHOW);
				}
				if (myHandler.hasMessages(REFESH_NET_SPEED)) {
					myHandler.removeMessages(REFESH_NET_SPEED);
				}
				if (myHandler.hasMessages(TOP_ACTIVITY_CAHNGED)) {
					myHandler.removeMessages(TOP_ACTIVITY_CAHNGED);
				}
				if (!myHandler.hasMessages(NO_TOP_ACTIVITY_CAHNGED)) {
					myHandler.sendEmptyMessage(NO_TOP_ACTIVITY_CAHNGED);
				}
			}
		}

		public void bindSendHandler() {
			myHandler.sendEmptyMessageDelayed(BIND_PHONE, 300000);

		}

		public void bindRemoveHandler() {
			if (myHandler.hasMessages(BIND_PHONE)) {
				myHandler.removeMessages(BIND_PHONE);
			}

		}

		public void initNotification() {
			if (configManager.getStatusKeepNotice()) {
				openNotification();
			}
			if (configManager.getFloatWindowOpen()) {
				myHandler.sendEmptyMessage(CHECK_FLOATVIEW_SHOW);
			}
			if (callStatApplication.initTrafficLog()) {
				// Log.i("i", "开始流量统计");
				myHandler.sendEmptyMessage(REFRESH_TRAFFIC_LOG);
			}
		}
	}

	public String getNetworkState() {
		return NETWORK_STATE[network_state];
	}

	private Handler myHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			try {
				switch (msg.what) {

				// 后台计时器
				case QUERY_TIME:
					time.setToNow();
					ILog.LogD(this.getClass(), "handleMessage QUERY_TIME");
					long now = System.currentTimeMillis();
					long last = configManager.getLastCheckTime();

						int time = (int) ((now - last) / 60000);
						int hour = time / 60;
						if (hour >= configManager.getAccountFrequency()) {
							userStatus_send_fause = 0;
							if (binder != null
									&& !configManager.isFirstLaunch()) {
								if (configManager.getHasNotReceivedSmsMiui() <= 3
										&& configManager.isAutoCheck()) {
									ILog.LogD(this.getClass(), "开始后台4小时自动对帐");
									CallStatApplication.calls_anim_is_run = true;
									CallStatApplication.traffic_anim_is_run = true;
									// 重置 开始 全对账的时间
									long now1 = System.currentTimeMillis();
									configManager.setLastCheckTime(now1);
									binder.sendAccounting(ReconciliationUtils.SEND_QUERY);
								}
							}
						}
					myHandler.sendEmptyMessageDelayed(QUERY_TIME, 180000);
					break;

				// 刷新总的流量信息
				case REFRESH_TRAFFIC_LOG:
					// 用户统计，总使用时间
					try {
						int totalTime = configManager.getTotalRunningTime() + 1;
						int daytotalTime = configManager
								.getDayTotalRunningTime() + 1;
						configManager.setTotalRunningTime(totalTime);
						configManager.setDayTotalRunningTime(daytotalTime);
					} catch (Exception e) {
						ILog.logException(this.getClass(), e);
					}

					int deCode = callStatApplication.refreshTrafficLog()[0]
							* 10 + callStatApplication.refreshTrafficLog()[1];
					switch (deCode) {

					// 每日流量超额处理
					case 1:

						break;
					// 总流量超额处理
					case 10:
						if (configManager.getStatusKeepNotice()) {
							openNotification();
						}

						if (FirewallUtils.isGprsEnabled(context)) {
							if (!configManager.getMonthTrafficWarn()) {
								show_trafficwarnNotice("流量报警",
										"本月已用流量超出预警值，建议关闭GPRS网络。");
							}
						}

						break;
					case 20: // 超流量断网

						if (configManager.getIsBrokenNetwork()
								&& !configManager.getTrafficBeyond()) {
							if (FirewallUtils.isGprsEnabled(context)) { // 判断GPRS网络是否连接
								// 弹出对话框
								try {
									Intent intent2 = new Intent(
											CallStatSMSService.this,
											TrafficWarningActivity.class);
									intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
											| Intent.FLAG_ACTIVITY_NO_HISTORY
											| Intent.FLAG_FROM_BACKGROUND
											| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
									startActivity(intent2);
									configManager.setTrafficBeyond(true);
								} catch (Exception e) {
									ILog.logException(this.getClass(), e);
								}
							}
							/*
							 * if (FirewallUtils.isGprsEnabled(context)) {
							 * boolean isGprsStillOpen = true; try {
							 * isGprsStillOpen = FirewallUtils
							 * .shiftGprs(CallStatSMSService.this);
							 * configManager.setTrafficBeyond(true); } catch
							 * (Exception e) { isGprsStillOpen = true;
							 * ILog.logException(this.getClass(), e); } if
							 * (!isGprsStillOpen) {
							 * show_trafficwarnNotice("超流量断网",
							 * "本月已用流量超出套餐，已关闭GPRS网络。"); } else {
							 * show_trafficwarnNotice("超流量断网",
							 * "本月已用流量超出套餐，建议关闭GPRS网络。"); } }
							 */
						}
						break;
					case 11:
						if (configManager.getStatusKeepNotice()) {
							openNotification();
						}
						break;
					case 0:

						if (configManager.getStatusKeepNotice()) {
							openNotification();
						}
						configManager.setMonthTrafficWarn(false);
						configManager.setTrafficBeyond(false);
						break;
					case -11: //added by zhangjing@archermind.com  增加了对于初始配置文件取得默认数据的处理
						break;
					}

					// 用户统计
					sendData();

					// 用户消费行为统计,added by zhangjing@archermind.com
					 sendUserConsumeStatInfo();

					// Log.i("free",
					// "etOverlayPackageSwitch()"
					// + configManager.getOverlayPackageSwitch());
					// Log.i("free", "getHasOpened()" +
					// configManager.getHasOpened());
					// 江苏移动开启3元叠加包
					/*
					 * if (configManager.getOverlayPackageSwitch() &&
					 * !configManager.getHasOpened()) { if
					 * (configManager.getTotalGprsUsed() +
					 * configManager.getTotalGprsUsedDifference() >=
					 * configManager .getFreeGprs()) { new Thread(new Runnable()
					 * {
					 * 
					 * @Override public void run() { SmsManager smsManager =
					 * SmsManager.getDefault();
					 * smsManager.sendTextMessage("10086", null, "ktsjll3",
					 * null, null); Log.i("free", "10086"); Log.i("free",
					 * "ktsjll3"); } }).start(); }
					 * configManager.setHasOpened(true); }
					 */
					ILog.LogI(this.getClass(), "REFRESH_TRAFFIC_LOG");
					// 每分钟检测是否有悬浮窗开启，并驱动其刷新
					if (configManager.getFloatWindowOpen()) {
						if (!myHandler.hasMessages(CHECK_FLOATVIEW_SHOW)) {
							myHandler.sendEmptyMessage(CHECK_FLOATVIEW_SHOW);
						}
					}
					myHandler.sendEmptyMessageDelayed(REFRESH_TRAFFIC_LOG,
							60000);
					break;
				case TOP_ACTIVITY_CAHNGED:
					floatwindow_data();
					reflesh();
					if (!bShowView) {
						bShowView = true;
						prevTotalTraffic = MTrafficStats.getTotalRxBytes()
								+ MTrafficStats.getTotalTxBytes();
						// Log.d("my", "configManager.getFloatWindowIndex()="
						// + configManager.getFloatWindowIndex());
						/*
						 * if (configManager.getFloatWindowIndex() == 3) {
						 * myHandler.sendEmptyMessageDelayed(REFESH_NET_SPEED,
						 * 3000); }
						 */
						// Log.d("my", "开启悬浮窗");
					}
					break;
				case NO_TOP_ACTIVITY_CAHNGED:
					removeview();
					if (bShowView) {
						// Log.d("my", "关闭悬浮窗");
						bShowView = false;
						if (myHandler.hasMessages(REFESH_NET_SPEED)) {
							// Log.i("wanglei",
							// "myHandler.removeMessages(REFESH_NET_SPEED)");
							myHandler.removeMessages(REFESH_NET_SPEED);
						}
					}
					break;
				case REFESH_NET_SPEED:
					long nowTotalTraffic = MTrafficStats.getTotalRxBytes()
							+ MTrafficStats.getTotalTxBytes();
					netSpeed = (double) (nowTotalTraffic - prevTotalTraffic) / 3;
					prevTotalTraffic = nowTotalTraffic;
					floatwindow_data();
					reflesh();
					// Log.i("wanglei", "REFESH_NET_SPEED中");
					// Log.i("my", "netSpeed=========" + netSpeed);
					myHandler.sendEmptyMessageDelayed(REFESH_NET_SPEED, 3000);
					break;
				case CHECK_FLOATVIEW_SHOW:
					boolean result;
					if (configManager.getFloatShowDeskOnly()) {
						result = CallStatUtils.isOnDesk(context,
								R.array.home_keywords);
					} else {
						result = true;
					}
					// Log.i("xxx",
					// "tasks.get(0).baseActivity.getPackageName() = "
					// + tasks.get(0).baseActivity.getPackageName());
					if (result) {
						// Log.i("x", "result======" + result);
						myHandler.sendEmptyMessage(TOP_ACTIVITY_CAHNGED);
					} else {
						myHandler.sendEmptyMessage(NO_TOP_ACTIVITY_CAHNGED);
					}
					myHandler.sendEmptyMessageDelayed(CHECK_FLOATVIEW_SHOW,
							3000);
					break;
				case BIND_PHONE:
					configManager.setPhoneBindingStatus(-1);
					Toast.makeText(getApplicationContext(), "尊享服务绑定超时",
							Toast.LENGTH_SHORT).show();
					// Log.i("wanglei", "绑定超时");
					sendBroadcast(new Intent(SEND_BIND_HANDLE));
					// Log.i("wanglei", "绑定超时hou");
					break;
				default:
					super.handleMessage(msg);
					break;
				}
			} catch (Exception e) {
				ILog.logException(this.getClass(), e);
			}
		}
	};

	/*
	 * class refreshTrafficDetailsThread extends Thread { public void run() {
	 * callStatApplication.refreshTrafficDetail(); } }
	 */

	@Override
	public void onCreate() {
		super.onCreate();
		context = this;
		screenWidth = DeviceUtils.getDeviceScreenWidth(this);
		screenDensity = DeviceUtils.getDeviceDisplayDensity(this);

		/** broadcast register part **/
		/***** 开启短信监控 ****/
		smsReceiver = new SmsReceiver();
		smsReceivedFiliter = new IntentFilter(SmsReceiver.SMS_RECEIVED_ACTION);
		smsReceivedFiliter.setPriority(Integer.MAX_VALUE);
		registerReceiver(smsReceiver, smsReceivedFiliter);

		callStatReceiver = new CallStatReceiver();
		serviceStateChangeFiliter = new IntentFilter(SERVICE_STATE_ACTION);
		registerReceiver(callStatReceiver, serviceStateChangeFiliter);

		connectivityChangeFiliter = new IntentFilter(CONNECTIVITY_CHANGE_ACTION);
		registerReceiver(callStatReceiver, connectivityChangeFiliter);

		uploadAccoutingCodeFilter = new IntentFilter(UPLOAD_ACC_CODE_ACTION);
		registerReceiver(callStatReceiver, uploadAccoutingCodeFilter);

		Intent intent = new Intent(INIT_RECEIVER_ACTION);
		sendBroadcast(intent);

		/** important vars init ***/
		configManager = new ConfigManager(this);
		callStatApplication = (CallStatApplication) getApplication();
		int modeIdx = Settings.System.getInt(getContentResolver(),
				Settings.System.AIRPLANE_MODE_ON, 0);
		bOpenAir = (modeIdx == 1);
		if (configManager.isAirmodeSwitch()) {
			CallStatUtils.initAirAlarm(this, callStatApplication);
		}
		sendToService();

		/** 后台自动对帐 **/
		time.setToNow();
		myHandler.sendEmptyMessageDelayed(QUERY_TIME,
				60000 - time.second * 1000);

		/** 服务开启，判断当前是否开启悬浮窗，并驱动悬浮窗刷新 **/
		if (configManager.getFloatWindowOpen()) {
			if (!myHandler.hasMessages(CHECK_FLOATVIEW_SHOW)) {
				myHandler.sendEmptyMessage(CHECK_FLOATVIEW_SHOW);
			}
		}

		/******** 开启流量监控 ********/

		nm = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE); // 通知栏
		floatwindow_show();// 桌面悬浮窗的实例化
		notificationReceiver = new NotificationReceiver();
		callTimeFilter = new IntentFilter(
				ObserverManager.CALL_LOG_CHANGED_ACTION);
		registerReceiver(notificationReceiver, callTimeFilter);

		smsSendFilter = new IntentFilter(ObserverManager.SMS_LOG_CHANGED_ACTION);
		smsSendFilter.addAction(ObserverManager.HAS_GUESS_YE_ACTION);

		registerReceiver(notificationReceiver, smsSendFilter);

		if (!configManager.isFirstLaunch()) {
			if (configManager.getStatusKeepNotice()) {
				openNotification();
			}
			if (configManager.getFloatWindowOpen()) {
				// myHandler.sendEmptyMessage(CHECK_FLOATVIEW_SHOW);
				binder.showDeskOnly();
			}
			if (callStatApplication.initTrafficLog()) {
				// Log.i("i", "开始流量统计");
				myHandler.sendEmptyMessage(REFRESH_TRAFFIC_LOG);
			}
		}
		MyListener = new MyPhoneStateListener();
		Tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		Tel.listen(MyListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

		new Thread(new Runnable() {

			@Override
			public void run() {
				if (FirewallCoreWorker.hasRootAccess()) {
					resolveConflict();
				}
			}
		}).start();

	}

	private void resolveConflict() {
		try {
			ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
			/*
			 * String[] whiteList = getResources().getStringArray(
			 * R.array.white_apps);
			 */
			List<RunningAppProcessInfo> processes = activityManager
					.getRunningAppProcesses();
			List<ApplicationInfo> appInfoList = callStatApplication.packageManager
					.getInstalledApplications(0);
			// 第一层过滤：将当前正在运行的应用包名和appInfo、pidInfo获得
			for (RunningAppProcessInfo info : processes) {
				for (ApplicationInfo ai : appInfoList) {
					if (info.processName.startsWith(ai.packageName)) {
						// 判断是否位第三方应用
						if (isUserApp(ai)) {
							// 过滤掉不含接受短信权限的
							if (CallStatUtils.hasSMSpermission(
									callStatApplication.packageManager, ai)) {
								// 过滤掉白名单应用
								if (!info.processName.startsWith("android")
										&& !info.processName
												.startsWith("com.android")
										&& !info.processName
												.startsWith("com.miui")
										&& !info.processName
												.equals(PACKAGE_NAME)) {
									ILog.LogE(getClass(), "kill "
											+ info.processName);
									String script = "kill -1 " + info.pid;
									FirewallCoreWorker.runScriptAsRoot(script,
											null, 5000);
								}
							}
						}
					}
				}
			}

		} catch (Exception e) {
			ILog.logException(getClass(), e);
		}
	}

	/**
	 * 是否是系统软件或者是系统软件的更新软件
	 * 
	 * @return
	 */
	public boolean isSystemApp(ApplicationInfo aInfo) {
		return ((aInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
	}

	public boolean isSystemUpdateApp(ApplicationInfo aInfo) {
		return ((aInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0);
	}

	public boolean isUserApp(ApplicationInfo aInfo) {
		return (!isSystemApp(aInfo) && !isSystemUpdateApp(aInfo));
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		new Thread(new Runnable() {

			@Override
			public void run() {
				ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo wifiInfo = connectivityManager
						.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
				boolean isWifiOn = wifiInfo.isConnected() ? true : false;
				if (isWifiOn) {
					callStatApplication
							.refreshNewTrafficDetail(CallStatDatabase.TRAFFIC_DETAIL_TYPE_WIFI);
				} else {
					callStatApplication
							.refreshNewTrafficDetail(CallStatDatabase.TRAFFIC_DETAIL_TYPE_GPRS);
				}
				callStatApplication.refreshTrafficLog();
			}
		}).start();

		Tel.listen(MyListener, PhoneStateListener.LISTEN_NONE);
		if (myHandler.hasMessages(QUERY_TIME)) {
			myHandler.removeMessages(QUERY_TIME);
		}
		if (myHandler.hasMessages(REFRESH_TRAFFIC_LOG)) {
			myHandler.removeMessages(REFRESH_TRAFFIC_LOG);
		}
		if (myHandler.hasMessages(TOP_ACTIVITY_CAHNGED)) {
			myHandler.removeMessages(TOP_ACTIVITY_CAHNGED);
		}
		if (myHandler.hasMessages(NO_TOP_ACTIVITY_CAHNGED)) {
			myHandler.removeMessages(NO_TOP_ACTIVITY_CAHNGED);
		}
		if (myHandler.hasMessages(REFESH_NET_SPEED)) {
			myHandler.removeMessages(REFESH_NET_SPEED);
		}
		if (myHandler.hasMessages(CHECK_FLOATVIEW_SHOW)) {
			myHandler.removeMessages(CHECK_FLOATVIEW_SHOW);
		}

		unregisterReceiver(callStatReceiver);
		unregisterReceiver(smsReceiver);

		closeNotification();
		removeview();
	}

	/* 将Zip文件上传到服务器 */
	private void sendToService() {
		// Log.i("xx", "sendToService--------");
		try {
			long outTime = (long) 7 * 24 * 60 * 60 * 1000;
			final long totalTime = configManager.getTotalRunningTime();
			if (CallStatUtils.isNetworkAvailable(this)) {
				if (totalTime >= outTime) {
					switch (configManager.getAccountLogSendTimes()) {
					case 0:
						break;
					case 1:
						if (CallStatDatabase.getInstance(context)
								.getEquationCount() >= 10) {
							// Log.i("xx", "case 1 ----");
							new Thread(new Runnable() {

								@Override
								public void run() {
									if (uploadString()) {
										configManager.setAccountLogSendTimes(2);
									}
								}
							}).start();

						}

						break;
					case 2:

						if (CallStatDatabase.getInstance(context)
								.getEquationCount() >= 30) {
							// Log.i("xx", "case 2 ----");
							new Thread(new Runnable() {

								@Override
								public void run() {
									if (uploadString()) {
										configManager.setAccountLogSendTimes(3);
									}
								}
							}).start();

						}
						break;
					case 3:
						if (CallStatDatabase.getInstance(context)
								.getEquationCount() >= 60) {
							new Thread(new Runnable() {

								@Override
								public void run() {
									if (uploadString()) {
										configManager.setAccountLogSendTimes(0);
									}
								}
							}).start();
						}
						break;
					default:
						break;
					}
				}
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}

	}

	public boolean uploadString() {

		// Log.i("xx", "uploadString--");
		try {
			String url = getResources().getString(R.string.accounting_log_url);
			Map<String, String> map = new HashMap<String, String>();
			map.put("province", configManager.getProvince());
			map.put("operator", configManager.getOperator());
			map.put("packageBrand", configManager.getPackageBrand());
			map.put("version", configManager.getVersionName());
			map.put("imei", DeviceInformation.getInformation(InfoName.IMEI));
			String content = getContentFromfile();

			map.put("content", content);

			if (url == null) {
				return false;
			}

			HttpPost myRequest = new HttpPost(url);
			UrlEncodedFormEntity myEntity = MyHttpPostHelper
					.buildUrlEncodedFormEntity(map, null);
			myRequest.setEntity(myEntity);
			DefaultHttpClient myDefaultHttpClient = new DefaultHttpClient();
			HttpResponse myResponse = myDefaultHttpClient.execute(myRequest);

			int status = myResponse.getStatusLine().getStatusCode();
			/*
			 * String strResult = EntityUtils.toString(myResponse.getEntity());
			 * Log.i("xx", "strResult:" + strResult);
			 */
			if (status == 200) {
				// 发送成功
				// Log.i("xx", "文字信息发送成功……");
				// getZip(url);
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
			return false;
		}
	}

	public String getContentFromfile() {
		final String FILE_NAME = "/callstat/offline/accounting_log.txt";
		StringBuilder content = new StringBuilder();
		byte[] buff = new byte[1024];
		// 如果手机插入了SD卡，而且应用程序具有访问SD的权限
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			// 获取SD卡对应的存储目录
			String sourceFilePath;
			File sdCardDir = Environment.getExternalStorageDirectory();
			try {
				sourceFilePath = sdCardDir.getCanonicalPath() + FILE_NAME;

				FileInputStream fis = new FileInputStream(sourceFilePath);

				int read = 0;
				while ((read = fis.read(buff)) != -1) {
					content.append(new String(buff, 0, read));
				}

			} catch (Exception e) {
				e.printStackTrace();
				// Log.i("xx", e.getMessage());
			}
		}

		return content.toString();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		try {
			if (intent != null && intent.getAction() != null) {
				if (intent.getAction().equals(PACKAGEADD_ACTION)) {
					callStatApplication.addPackage(intent
							.getStringExtra("package"));
				} else if (intent.getAction().equals(PACKAGEREMOVED_ACTION)) {
					callStatApplication.removedPackage(intent
							.getStringExtra("package"));
				} else if (intent.getAction().equals(
						SERVICE_STATE_CHANGE_ACTION)) {
					Bundle bundle = intent.getExtras();
					if (bundle != null) {
						int state = bundle.getInt("state");
						// Log.d("my", "state = " + state);
						switch (state) {
						case 0x00:
							if (bOpenAir) {
								bOpenAir = false;
							}
							// Log.d("my", "飞行模式关闭");
							break;
						case 0x01:
							// Log.d("my", "Try to connect the net.");
							break;
						case 0x03:
							if (!bOpenAir) {
								bOpenAir = true;
							}
							// Log.d("my", "飞行模式开启");
							break;
						}
					}

				} else if (intent.getAction().equals(PHONE_SHUTDOWN_ACTION)) {
					if (!configManager.IsDownTime()) {
						configManager.setIsDownTime(true);
					}
					Intent intentActivity = new Intent(PHONE_SHUTDOWN_ACTION);
					sendBroadcast(intentActivity);
					bAccountFalied = true;

				} else if (intent.getAction().equals(ACTION_VERIFICATION_CODE)) {
					final String msg = intent
							.getStringExtra("ACTION_VERIFICATION_CODE");

					new AsyncTask<Void, Void, String>() {

						@Override
						protected void onPostExecute(String result) {
							super.onPostExecute(result);
							Intent imyIntent = new Intent(
									ACTION_VERIFICATION_RESULT);
							// Log.i("wanglei", "running------" + result);
							if (result.equals("0")) {
								// configManager.setPhoneBindingStatus(1);
								imyIntent.putExtra("msg", 0);
								context.sendBroadcast(imyIntent);
								// Toast.makeText(getApplicationContext(),
								// "绑定成功",
								// Toast.LENGTH_SHORT).show();
							} else {
								configManager.setPhoneBindingStatus(-1);
								imyIntent.putExtra("msg", 1);
								context.sendBroadcast(imyIntent);
								// Toast.makeText(getApplicationContext(),
								// "绑定失败",
								// Toast.LENGTH_SHORT).show();
							}
						}

						@Override
						protected void onPreExecute() {
							super.onPreExecute();
						}

						@Override
						protected String doInBackground(Void... params) {
							return verifySms(msg);
						}
					}.execute();
				} else if (intent.getAction().equals(PHONE_BINDIND_STRING)) {

					new AsyncTask<Void, Void, String>() {

						@Override
						protected void onPostExecute(String result) {
							super.onPostExecute(result);
							Intent imyIntent = new Intent(PHONE_BINDIND_RESULT);
							// Log.i("wanglei",
							// "PHONE_BINDIND_STRING+running------"
							// + result);
							if (result.equals("0") || result.equals("1")) {
								configManager.setPhoneBindingStatus(1);
								imyIntent.putExtra("msg", 0);
								context.sendBroadcast(imyIntent);
								Toast.makeText(getApplicationContext(),
										"尊享服务启用成功", Toast.LENGTH_SHORT).show();
							} else {
								configManager.setPhoneBindingStatus(-1);
								imyIntent.putExtra("msg", 1);
								context.sendBroadcast(imyIntent);
								if (CallStatUtils
										.isMyAppOnDesk(CallStatSMSService.this)) {
									Toast.makeText(getApplicationContext(),
											"网络故障，请检查网络稍后再试！",
											Toast.LENGTH_SHORT).show();
								}
							}
						}

						@Override
						protected void onPreExecute() {
							super.onPreExecute();
						}

						@Override
						protected String doInBackground(Void... params) {
							return isBinding();
						}
					}.execute();
				} else if (intent.getAction().equals(PHONE_UNBINDIND_STRING)) {

					new AsyncTask<Void, Void, String>() {

						@Override
						protected void onPostExecute(String result) {
							super.onPostExecute(result);
							Intent imyIntent = new Intent(
									PHONE_UNBINDIND_RESULT);
							ILog.LogI(this.getClass(),
									"PHONE_UNBINDIND_STRING+running------"
											+ result);
							if (result.equals("1")) {
								configManager.setPhoneBindingStatus(-1);
								imyIntent.putExtra("msg", 1);
								context.sendBroadcast(imyIntent);
								if (CallStatUtils
										.isMyAppOnDesk(CallStatSMSService.this)) {
									Toast.makeText(getApplicationContext(),
											"尊享服务解除成功", Toast.LENGTH_SHORT)
											.show();
								}
							} else {
								configManager.setPhoneBindingStatus(1);
								imyIntent.putExtra("msg", 0);
								context.sendBroadcast(imyIntent);
								if (CallStatUtils
										.isMyAppOnDesk(CallStatSMSService.this)) {
									Toast.makeText(getApplicationContext(),
											"网络故障，请检查网络稍后再试！",
											Toast.LENGTH_SHORT).show();
								}
							}
						}

						@Override
						protected void onPreExecute() {
							super.onPreExecute();
						}

						@Override
						protected String doInBackground(Void... params) {
							return cancelBind();
						}
					}.execute();
				} else if (intent.getAction().equals(
						REFRESH_TRAFFIC_LOG_SERVICE_ACTION)) {
					if (myHandler.hasMessages(REFRESH_TRAFFIC_LOG)) {
						myHandler.removeMessages(REFRESH_TRAFFIC_LOG);
					}
					myHandler.sendEmptyMessage(REFRESH_TRAFFIC_LOG);
				} else if (intent.getAction().equals(ACCOUNT_SUCCESS)) { // 用于刷新通知栏窗口
																			// 和
					ILog.LogI(this.getClass(), "刷新通知栏窗口---------"); // 悬浮窗
					if (configManager.getStatusKeepNotice()) {
						openNotification();
					}
					/*
					 * if (configManager.getFloatWindowOpen()) {
					 * floatwindow_data(); }
					 */
				}
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	private String cancelBind() {
		String code = "";
		String url = getString(R.string.unbind_phone_url);
		Map<String, String> keyValuePairs = new HashMap<String, String>();
		keyValuePairs.put("phonenumber", configManager.getTopEightNum());

		HttpPost httpRequest = MyHttpPostHelper.getHttpPost(url);
		UrlEncodedFormEntity myEtity = MyHttpPostHelper
				.buildUrlEncodedFormEntity(keyValuePairs, null);

		httpRequest.setEntity(myEtity);
		try {
			HttpResponse httpresp = new DefaultHttpClient()
					.execute(httpRequest);
			code = EntityUtils.toString(httpresp.getEntity());
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return code;
	}

	private String isBinding() {
		String code = "";
		String url = getString(R.string.bind_phone_url);
		Map<String, String> keyValuePairs = new HashMap<String, String>();
		keyValuePairs.put("imei", configManager.getImei());
		keyValuePairs.put("phonenumber", configManager.getTopEightNum());

		HttpPost httpRequest = MyHttpPostHelper.getHttpPost(url);
		UrlEncodedFormEntity myEtity = MyHttpPostHelper
				.buildUrlEncodedFormEntity(keyValuePairs, null);

		httpRequest.setEntity(myEtity);
		try {
			HttpResponse httpresp = new DefaultHttpClient()
					.execute(httpRequest);
			code = EntityUtils.toString(httpresp.getEntity());
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return code;
	}

	private String verifySms(String randomCode) {
		String code = "";
		String url = getString(R.string.sms_verify_url);
		Map<String, String> keyValuePairs = new HashMap<String, String>();
		keyValuePairs.put("appid", "2");
		keyValuePairs.put("code", randomCode);
		keyValuePairs.put("smsID", String.valueOf(CallStatApplication.token));
		keyValuePairs.put("Action", SmsVerifyBean.BINDING_ACTION);

		HttpPost httpRequest = MyHttpPostHelper.getHttpPost(url);
		UrlEncodedFormEntity myEtity = MyHttpPostHelper
				.buildUrlEncodedFormEntity(keyValuePairs, null);

		httpRequest.setEntity(myEtity);
		try {
			HttpResponse httpresp = new DefaultHttpClient()
					.execute(httpRequest);
			code = EntityUtils.toString(httpresp.getEntity());
			// Log.i("i", httpresp.getStatusLine().getStatusCode() + "");
			// Log.i("i", "verifySms:+++++++++" + code);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return code;
	}

	/* 通知栏 */
	public void callfees_notification() {
		try {
			gprs_used = configManager.getTotalGprsUsed()
					+ configManager.getTotalGprsUsedDifference();

			/*
			 * if (gprs_used == 0) { double trafficSpendTmp = 0; if
			 * (CallStatSMSService.this != null) { double[] ret =
			 * CallStatDatabase.getInstance(
			 * CallStatSMSService.this).getThisMonthTotalSpend();
			 * trafficSpendTmp = ret[1]; } gprs_used = (long)
			 * Math.round(trafficSpendTmp); if (gprs_used < 0) { gprs_used = 0;
			 * } }
			 */

			today_gprs_used = configManager.getTotalGprsUsed()
					- configManager.getEarliestGprsLog();
			ILog.LogI(this.getClass(), "today_gprs_used:" + today_gprs_used);
			int progress = 0;
			String remainString = "";
			String today_used_String = "";

			int IS_NOMAL = 1;
			int id = R.id.notification_pbar_nomal;

			int icon = R.drawable.logo_tongzhi;
			// String tickertitle = "话费管家";
			long when = System.currentTimeMillis();
			notification = new Notification(icon, null, when);
			/*
			 * notification = new Notification(); notification.icon = icon;
			 * notification.tickerText = "话费管家";
			 */

			/* 用RemoteViews跨进程显示views */
			RemoteViews views = new RemoteViews(getPackageName(),
					R.layout.new_notification);

			// if (!configManager.IsDownTime()) {
			if (configManager.getCalculateFeeAvailable() != 100000) {
				fees_remain = configManager.getCalculateFeeAvailable();
				// Log.i("i", "openNotification------fees_remain = " +
				// fees_remain);
				views.setTextViewText(R.id.notification_feesremain,
						CallStatUtils.changeFloat(fees_remain) + "元");
				if (fees_remain > configManager.getAlertRemainFees()) {
					views.setTextColor(
							R.id.notification_feesremain,
							getResources().getColor(
									R.color.callsremain_nomal_color));
				} else {
					if (fees_remain > 10) {
						views.setTextColor(
								R.id.notification_feesremain,
								getResources().getColor(
										R.color.callsremain_warn_color));
						CALLS_CHAEGER_NOTICE = false;
						/*
						 * try { if (nm != null) { nm.cancel(3); } } catch
						 * (Exception e) { ILog.logException(this.getClass(),
						 * e); }
						 */
					} else {
						views.setTextColor(
								R.id.notification_feesremain,
								getResources().getColor(
										R.color.callsremain_beyond_color));
						/*
						 * if (!CALLS_CHAEGER_NOTICE) { callsCharegeNotice(); }
						 */
					}
				}
			} else {
				if (ReconciliationUtils.IsCheckingAccount
						&& !ReconciliationUtils.IsCheckingAccount3MIn
						&& CallStatApplication.calls_anim_is_run) {
					views.setTextViewText(R.id.notification_feesremain, "正在对账");
					views.setTextColor(
							R.id.notification_feesremain,
							getResources().getColor(
									R.color.callsremain_beyond_color));
				} else {
					views.setTextViewText(R.id.notification_feesremain, "未对账");
					views.setTextColor(
							R.id.notification_feesremain,
							getResources().getColor(
									R.color.callsremain_beyond_color));
				}
			}
			// } else {
			// views.setTextViewText(R.id.notification_feesremain, "已停机");
			// views.setTextColor(R.id.notification_feesremain, getResources()
			// .getColor(R.color.callsremain_beyond_color));
			// }

			/* 通知栏窗口进度条 */
			if (configManager.getFreeGprs() != 100000) {
				notification_gprs_remain = (long) configManager.getFreeGprs()
						* 1024l * 1024l - gprs_used;
				if (notification_gprs_remain >= 0) {

					remainString = "剩余"
							+ CallStatUtils
									.traffic_unit2(notification_gprs_remain)[0]
							+ CallStatUtils
									.traffic_unit2(notification_gprs_remain)[1];

					if (notification_gprs_remain > configManager
							.getAlertTrafficNotice() * 1024l * 1024l) {
						IS_NOMAL = 1;
					} else {
						IS_NOMAL = 2;
					}

				} else {
					IS_NOMAL = 3;
					remainString = "超出"
							+ CallStatUtils.traffic_unit2(Math
									.abs(notification_gprs_remain))[0]
							+ CallStatUtils.traffic_unit2(Math
									.abs(notification_gprs_remain))[1];
				}

				if (gprs_used != 0) {
					progress = (int) Math.round(gprs_used / 1024f / 1024f
							/ configManager.getFreeGprs() * 100);
					if (progress > 100) {
						progress = 100;
					}
				} else {
					progress = 0;
				}
			} else {
				progress = 0;
				remainString = "已用" + CallStatUtils.traffic_unit2(gprs_used)[0]
						+ CallStatUtils.traffic_unit2(gprs_used)[1];
				// views.setTextViewText(R.id.notification_traffic_string,
				// "流量已用:");
				// views.setTextViewText(
				// R.id.notification_traffic_remain,
				// CallStatUtils.traffic_unit1(gprs_used)[0]
				// + CallStatUtils.traffic_unit1(gprs_used)[1]);
			}

			today_used_String = "今日"
					+ CallStatUtils.traffic_unit2(today_gprs_used)[0]
					+ CallStatUtils.traffic_unit2(today_gprs_used)[1];
			switch (IS_NOMAL) {
			case 1:
				views.setViewVisibility(R.id.notification_pbar_layout1,
						View.VISIBLE);
				views.setViewVisibility(R.id.notification_pbar_layout2,
						View.GONE);
				views.setViewVisibility(R.id.notification_pbar_layout3,
						View.GONE);
				id = R.id.notification_pbar_nomal;
				break;
			case 2:
				views.setViewVisibility(R.id.notification_pbar_layout1,
						View.GONE);
				views.setViewVisibility(R.id.notification_pbar_layout2,
						View.VISIBLE);
				views.setViewVisibility(R.id.notification_pbar_layout3,
						View.GONE);
				id = R.id.notification_pbar_warn;
				if (!configManager.getMonthTrafficWarn()) {
					show_trafficwarnNotice("流量报警", "本月已用流量超出预警值，建议关闭GPRS网络。");
				}
				break;
			case 3:
				views.setViewVisibility(R.id.notification_pbar_layout1,
						View.GONE);
				views.setViewVisibility(R.id.notification_pbar_layout2,
						View.GONE);
				views.setViewVisibility(R.id.notification_pbar_layout3,
						View.VISIBLE);
				id = R.id.notification_pbar_beyond;
				break;
			default:
				break;
			}

			views.setProgressBar(id, 100, progress, false);
			views.setTextViewText(R.id.notification_pbar_text,
					today_used_String + "，" + remainString);

			/* 让声音、振动无限循环，直到用户响应 */
			// notification.flags |= Notification.FLAG_INSISTENT;

			/* 通知被点击后，自动消失 */
			// notification.flags |= Notification.FLAG_AUTO_CANCEL;

			// 点击'Clear'时，不清除该通知(QQ的通知无法清除，就是用的这个)
			notification.flags |= Notification.FLAG_NO_CLEAR;
			notification.flags |= Notification.FLAG_ONGOING_EVENT; // 将通知设置成正在进行
			/* 通知栏的点击事件 */
			Intent intent = new Intent();
			ComponentName componentName = new ComponentName(context,
					CallStatMainActivity.class);
			intent.setComponent(componentName);
			intent.setAction(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_LAUNCHER);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			PendingIntent pIntent = PendingIntent.getActivity(
					getApplicationContext(), 0, intent, 0);
			notification.contentIntent = pIntent;
			notification.contentView = views; // 为通知设置布局
			nm.notify(1, notification);
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}

	}

	@Override
	public void onRebind(Intent intent) {
		// TODO Auto-generated method stub
		super.onRebind(intent);
		// Log.i("my", "onRebind");
	}

	/* 开启通知栏 (可用来刷新) */
	public void openNotification() {
		callfees_notification();
	}

	/* 关闭通知栏 */
	public void closeNotification() {
		try {
			nm.cancel(1);
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	// 刷新通知栏的接收器
	private class NotificationReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			/*
			 * if (ObserverManager.CALL_LOG_CHANGED_ACTION.equals(intent
			 * .getAction())) { Bundle bundle = intent.getExtras(); CallLog call
			 * = (CallLog) bundle
			 * .getSerializable(ObserverManager.CALL_LOG_BEAN); int
			 * new_callouttime = call.getDuration(); int original_callouttime =
			 * configManager.getTotalOutgoingCall(); int original_callintime =
			 * configManager.getTotalIncomingCall(); // Log.i("i",
			 * "new_callouttime:" + new_callouttime + " m"); // Log.i("i",
			 * "original_callouttime:" + original_callouttime // + " m"); //
			 * Log.i("i", "original_callintime:" + original_callintime + //
			 * " m"); if (call.getType() == 1) {
			 * configManager.setTotalIncomingCall(new_callouttime +
			 * original_callintime); if (configManager.getStatusKeepNotice()) {
			 * openNotification(); } } if (call.getType() == 2) {
			 * configManager.setTotalOutgoingCall(new_callouttime +
			 * original_callouttime); if (configManager.getStatusKeepNotice()) {
			 * openNotification(); } } } if
			 * (ObserverManager.SMS_LOG_CHANGED_ACTION.equals(intent
			 * .getAction())) { Bundle bundle = intent.getExtras(); SmsLog sms =
			 * (SmsLog) bundle .getSerializable(ObserverManager.SMS_LOG_BEAN);
			 * if (configManager.getStatusKeepNotice()) { openNotification(); }
			 * // if (sms.getProtocol() == 0) { // original_smssent += 1; //
			 * feesConfigManager.setTotalSmsSent(original_smssent); // } }
			 */

			if (ObserverManager.HAS_GUESS_YE_ACTION.equals(intent.getAction())) {
				if (configManager.getStatusKeepNotice()) {
					openNotification();
				}
			}
		}

	}

	/* float window */

	public void floatwindow_show() {
		// Log.i("x", "floatwindow_show=======================");
		try {
			floatview = LayoutInflater.from(this).inflate(R.layout.floatwindow,
					null);
			floatview.getBackground().setAlpha(0);
			floatstring = (TextView) floatview
					.findViewById(R.id.floatwindow_string);
			floatstyle = (TextView) floatview
					.findViewById(R.id.floatwindow_style);
			closebtn = (Button) floatview.findViewById(R.id.close_btn);

			imageView1 = (ImageView) floatview.findViewById(R.id.image1);
			imageView2 = (ImageView) floatview.findViewById(R.id.image2);
			left_tView = (TextView) floatview.findViewById(R.id.left_textView);
			right_tView = (TextView) floatview
					.findViewById(R.id.right_textView);

			before_onclick = (LinearLayout) floatview
					.findViewById(R.id.before_onclick);
			blank = (TextView) floatview.findViewById(R.id.blank);
			// before_onclick.getBackground().setAlpha(100);
			// before_onclick.setOnClickListener(new OnClickListener() {
			//
			// @Override
			// public void onClick(View v) {
			// // TODO Auto-generated method stub
			// if (closebtn.getVisibility() == View.GONE) {
			// closebtn.setVisibility(View.VISIBLE);
			// } else if (closebtn.getVisibility() == View.VISIBLE) {
			// closebtn.setVisibility(View.GONE);
			// }
			//
			// }
			// });

			/*
			 * closebtn.setOnClickListener(new OnClickListener() {
			 * 
			 * @Override public void onClick(View v) { // TODO Auto-generated
			 * method stub closebtn.setVisibility(View.GONE); if
			 * (myHandler.hasMessages(CHECK_FLOATVIEW_SHOW)) {
			 * myHandler.removeMessages(CHECK_FLOATVIEW_SHOW); } if
			 * (myHandler.hasMessages(REFESH_NET_SPEED)) {
			 * myHandler.removeMessages(REFESH_NET_SPEED); }
			 * closebtn.setVisibility(View.GONE);
			 * blank.setVisibility(View.GONE);
			 * configManager.setFloatWindowOpen(false); sendBroadcast(new
			 * Intent( SystemSettingNewActivity.FLOATWINDOW_BROADCAST_ACTION));
			 * removeview();
			 * 
			 * } });
			 */
			/* content of the float window */
			// int index = configManager.getFloatWindowIndex()

			/* 悬浮窗口配置 */
			windowManager = (WindowManager) this
					.getSystemService(WINDOW_SERVICE);
			/**
			 * LayoutParams.TYPE_SYSTEM_ERROR:保证该悬浮窗口在最上层
			 * LayoutParams.FLAG_NOT_FOCUSABLE:该悬浮窗口不获取焦点，但可以拖动
			 * PixelFormat.TRANSPARENT:悬浮窗口透明
			 */
			params = new WindowManager.LayoutParams();

			// 设置悬浮窗口的大小
			params.width = LayoutParams.WRAP_CONTENT;
			params.height = LayoutParams.WRAP_CONTENT;
			/**
			 * FIRST_SYSTEM_WINDOW的值就是2000。
			 * 2002的值的含义其实就是2000+2。数值2000的含义就是系统级窗口， 2002和2003的区别就是 2003 比
			 * 2002还要更上一层！比如， type为2003的view，能显示在手机下拉状态栏之上！
			 */
			params.type = 2003;
			/**
			 * 这里的flags也很关键 代码实际是wmParams.flags |= FLAG_NOT_FOCUSABLE;
			 * 40的由来是wmParams的默认属性（32）+ FLAG_NOT_FOCUSABLE（8）
			 */
			params.flags = 40;
			params.format = 1;
			// 悬浮窗口的初始位置
			params.gravity = Gravity.LEFT | Gravity.TOP;
			params.x = 0;
			params.y = 0;
			if (configManager.getFloatWindowPosition() != null) {
				String[] position = configManager.getFloatWindowPosition()
						.split(",");
				params.x = Integer.parseInt(position[0]);
				params.y = Integer.parseInt(position[1]);
			} else {

			}

			temp2[0] = params.x;
			temp2[1] = params.y;

			if (temp2[0] <= screenWidth / 2f - before_onclick.getWidth() / 2) {
				imageView1.setVisibility(View.GONE);
				imageView2.setVisibility(View.VISIBLE);
				left_tView.setVisibility(View.VISIBLE);
				right_tView.setVisibility(View.GONE);
				params.x = 0;
			} else {
				imageView1.setVisibility(View.VISIBLE);
				imageView2.setVisibility(View.GONE);
				left_tView.setVisibility(View.GONE);
				right_tView.setVisibility(View.VISIBLE);
				params.x = (int) (screenWidth - before_onclick.getWidth());
			}

			// 悬浮窗口的的拖动
			before_onclick.setOnTouchListener(new OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					int evention = event.getAction();
					switch (evention) {
					case MotionEvent.ACTION_DOWN: // 按下事件，记录按下时手指的XY坐标值
						temp[0] = event.getRawX();
						temp[1] = event.getRawY();

						// Log.i("i", "ACTION_DOWN x:" + params.x);
						// Log.i("i", "ACTION_DOWN y:" + params.y);
						// Log.i("i", "ACTION_DOWN x-event.getRawX():" +
						// temp[0]);
						// Log.i("i", "ACTION_DOWN y-event.getRawy():" +
						// temp[1]);
						// Log.i("i", "ACTION_DOWN");
						break;
					case MotionEvent.ACTION_MOVE:
						// Log.i("i", "ACTION_MOVE");
						// Log.i("i",
						// "ACTION_MOVE x-event.getRawX():" + event.getRawX());
						// Log.i("i",
						// "ACTION_MOVE y-event.getRawy():" + event.getRawY());
						refleshview(
								(int) (temp2[0] + (event.getRawX() - temp[0])),
								(int) (temp2[1] + (event.getRawY() - temp[1])));

						// Log.i("xxx", "params.x == " + params.x);
						// Log.i("xxx", "params.y == " + params.y);
						// Log.i("xxx", "getRawX == " + event.getRawX());
						// Log.i("xxx", "getRawY == " + event.getRawY());
						// Log.i("xxx", "getX == " + event.getX());
						// Log.i("xxx", "getY == " + event.getY());

						break;
					case MotionEvent.ACTION_UP:

						if ((event.getRawX() - event.getX()) <= (screenWidth / 2f - before_onclick
								.getWidth() / 2)) {

							params.x = 0;
							imageView1.setVisibility(View.GONE);
							imageView2.setVisibility(View.VISIBLE);
							left_tView.setVisibility(View.VISIBLE);
							right_tView.setVisibility(View.GONE);

						}

						else if ((event.getRawX() - event.getX()) >= (int) (screenWidth * 1 / 2 - before_onclick
								.getWidth() / 2)) {
							params.x = (int) (screenWidth
									- before_onclick.getWidth() + CallStatUtils
									.pixelToDip(20));
							imageView1.setVisibility(View.VISIBLE);
							imageView2.setVisibility(View.GONE);
							left_tView.setVisibility(View.GONE);
							right_tView.setVisibility(View.VISIBLE);
						}
						temp2[0] = params.x;
						temp2[1] = params.y;

						String position = params.x + "," + params.y;
						configManager.setFloatWindowPosition(position);
						reflesh();
						break;
					default:
						break;
					}

					return true;
				}

			});
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	private void floatwindow_data() {
		try {
			long gprs_used = configManager.getTotalGprsUsed()
					+ configManager.getTotalGprsUsedDifference();
			/*
			 * if (gprs_used == 0) { double trafficSpendTmp = 0; if
			 * (CallStatSMSService.this != null) { double[] ret =
			 * CallStatDatabase.getInstance(
			 * CallStatSMSService.this).getThisMonthTotalSpend();
			 * trafficSpendTmp = ret[1]; } gprs_used = (long)
			 * Math.round(trafficSpendTmp); if (gprs_used < 0) { gprs_used = 0l;
			 * } }
			 */
			if (gprs_used < 0) {
				gprs_used = 0l;
			}

			float total_gprs = configManager.getFreeGprs();
			long gprs_remian = 0;
			int index = configManager.getFloatWindowIndex();
			fees_remain = configManager.getCalculateFeeAvailable();
			switch (index) {
			case 1:
				if (fees_remain != 100000) {
					floatstring.setText(""
							+ CallStatUtils.changeFloat_float(configManager
									.getCalculateFeeAvailable()));
					floatstyle.setText("元");
					if (fees_remain > configManager.getAlertRemainFees()) {
						floatstyle.setTextColor(getResources().getColor(
								R.color.float_view_nomal));
						floatstring.setTextColor(getResources().getColor(
								R.color.float_view_nomal));
					} else {
						if (fees_remain > 10) {
							floatstyle.setTextColor(getResources().getColor(
									R.color.callsremain_warn_color));
							floatstring.setTextColor(getResources().getColor(
									R.color.callsremain_warn_color));
						} else {
							floatstyle.setTextColor(getResources().getColor(
									R.color.callsremain_beyond_color));
							floatstring.setTextColor(getResources().getColor(
									R.color.callsremain_beyond_color));
						}
					}
				} else {
					floatstring.setText("未对账");
					floatstyle.setText("");
					floatstyle.setTextColor(getResources().getColor(
							R.color.float_view_warn));
					floatstring.setTextColor(getResources().getColor(
							R.color.float_view_warn));
				}
				floatstring.setTextSize(11);
				floatstyle.setTextSize(11);
				break;
			case 2:
				if (total_gprs != 100000
						&& gprs_used <= total_gprs * 1024f * 1024f) {
					gprs_remian = (long) total_gprs * 1024l * 1024l - gprs_used;
					floatstring.setText(CallStatUtils
							.traffic_unit_floatview(gprs_remian)[0]);
					floatstyle.setText(CallStatUtils
							.traffic_unit_floatview(gprs_remian)[1]);

					if (gprs_remian > configManager.getAlertTrafficNotice() * 1024 * 1024f) {
						floatstyle.setTextColor(getResources().getColor(
								R.color.float_view_nomal));
						floatstring.setTextColor(getResources().getColor(
								R.color.float_view_nomal));
					} else {
						if (gprs_remian >= 0) {
							floatstyle.setTextColor(getResources().getColor(
									R.color.callsremain_warn_color));
							floatstring.setTextColor(getResources().getColor(
									R.color.callsremain_warn_color));
						} else {
							floatstyle.setTextColor(getResources().getColor(
									R.color.callsremain_beyond_color));
							floatstring.setTextColor(getResources().getColor(
									R.color.callsremain_beyond_color));
						}
					}

				} else {
					if (total_gprs == 100000
							&& configManager.getFloatWindowIndex() == 2) {
						if (fees_remain != 100000) {
							floatstring
									.setText(""
											+ CallStatUtils
													.changeFloat_float(configManager
															.getCalculateFeeAvailable()));
							floatstyle.setText("元");
							if (fees_remain > configManager
									.getAlertRemainFees()) {
								floatstyle.setTextColor(getResources()
										.getColor(R.color.float_view_nomal));
								floatstring.setTextColor(getResources()
										.getColor(R.color.float_view_nomal));
							} else {
								if (fees_remain > 10) {
									floatstyle
											.setTextColor(getResources()
													.getColor(
															R.color.callsremain_warn_color));
									floatstring
											.setTextColor(getResources()
													.getColor(
															R.color.callsremain_warn_color));
								} else {
									floatstyle
											.setTextColor(getResources()
													.getColor(
															R.color.callsremain_beyond_color));
									floatstring
											.setTextColor(getResources()
													.getColor(
															R.color.callsremain_beyond_color));
								}
							}
						} else {
							floatstring.setText("未对账");
							floatstyle.setText("");
							floatstyle.setTextColor(getResources().getColor(
									R.color.float_view_warn));
							floatstring.setTextColor(getResources().getColor(
									R.color.float_view_warn));
						}
						configManager.setFloatWindowIndex(1);
					} else {
						floatstring.setText("0.00");
						floatstyle.setText("M");
						floatstyle.setTextColor(getResources().getColor(
								R.color.float_view_warn));
						floatstring.setTextColor(getResources().getColor(
								R.color.float_view_warn));
					}

				}
				floatstring.setTextSize(11);
				floatstyle.setTextSize(11);
				break;
			case 3:
				floatstring.setText(CallStatUtils.wireSpeed_unit(netSpeed)[0]);
				floatstyle.setText(CallStatUtils.wireSpeed_unit(netSpeed)[1]);

				floatstring
						.setTextColor(getResources().getColor(R.color.white));
				floatstyle.setTextColor(getResources().getColor(R.color.white));

				if (CallStatUtils.wireSpeed_unit(netSpeed)[0].length() > 5) {
					floatstring.setTextSize(10);
					floatstyle.setTextSize(10);
				} else {
					floatstring.setTextSize(11);
					floatstyle.setTextSize(11);
				}
				break;
			default:
				break;
			}

		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	/**
	 * 刷新窗口的位置
	 * 
	 * @param x
	 *            ：拖动后的x坐标
	 * @param y
	 *            ：拖动后的y坐标
	 */
	protected void refleshview(int x, int y) {
		// 不能立即去状态栏的高度
		try {
			if (statusBarHeight == 0) {
				View rootview = floatview.getRootView();
				Rect rect = new Rect();
				// 获取状态栏和标题栏的高度
				rootview.getWindowVisibleDisplayFrame(rect);
				statusBarHeight = rect.top;
			}
			params.x = x;
			params.y = y;
			reflesh();
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	/** 添加或更新悬浮窗口，如果悬浮窗口不存在则添加窗口，如果存在则更新其位置 */
	private void reflesh() {
		try {
			if (windowManager != null) {
				if (addview) {
					windowManager.updateViewLayout(floatview, params);
				} else {
					windowManager.addView(floatview, params);
					addview = true;
				}
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	// 关闭悬浮窗
	private void removeview() {
		try {
			if (windowManager != null) {
				if (addview) {
					windowManager.removeView(floatview);
					addview = false;
				}
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	private void show_trafficwarnNotice(String title, String content) {
		/*
		 * try { configManager.setMonthTrafficWarn(true); Intent intent = new
		 * Intent(CallStatSMSService.this, TrafficWarningActivity.class);
		 * intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
		 * Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_FROM_BACKGROUND |
		 * Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS); startActivity(intent); }
		 * catch (Exception e) { ILog.logException(this.getClass(), e); }
		 */
		RemoteViews warn_views = new RemoteViews(getPackageName(),
				R.layout.warn_notification);
		warn_views.setTextViewText(R.id.warn_notification_content, content);
		warn_views.setViewVisibility(R.id.charge_notification_content,
				View.GONE);
		warn_views.setViewVisibility(R.id.warn_notification_content,
				View.VISIBLE);
		warn_notification = new Notification(R.drawable.logo_tongzhi, title,
				System.currentTimeMillis());
		/* 通知被点击后，自动消失 */
		warn_notification.flags |= Notification.FLAG_AUTO_CANCEL;
		PendingIntent m_PendingIntent = PendingIntent.getActivity(
				CallStatSMSService.this, 0, new Intent(), 0);
		warn_notification.contentView = warn_views;
		warn_notification.contentIntent = m_PendingIntent;
		if (nm != null) {
			nm.notify(2, warn_notification);
			configManager.setMonthTrafficWarn(true);
		}
	}

	private class MyPhoneStateListener extends PhoneStateListener {

		/* 从得到的信号强度,每个tiome供应商有更新 */

		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {

			super.onSignalStrengthsChanged(signalStrength);
			gsmSignalStrength = signalStrength.getGsmSignalStrength();
		}

	}

	private void checkUpdate() {
		String url = getString(R.string.new_accounting_database_update_url);
		// Log.i("callstats", "checkUpdate url :" + url);
		if (accountingDatabaseUpdater == null) {
			accountingDatabaseUpdater = new AccountingDatabaseUpdater(
					getBaseContext(), url);
		}
		Map<String, String> keyValuePairs = new HashMap<String, String>();
		keyValuePairs.put("time",
				configManager.getAccountingDatabaseUpdateTime());
		HttpEntity entity = MyHttpPostHelper
				.buildUrlEncodedFormEntity(keyValuePairs);
		accountingDatabaseUpdater
				.setManagerListener(accountingDatabaseUpdateListener);
		accountingDatabaseUpdater.startManager(entity);
	}

	private OnWebLoadListener<List<AccountingKeyWordsBean>> accountingDatabaseUpdateListener = new OnWebLoadListener<List<AccountingKeyWordsBean>>() {

		@Override
		public void OnStart() {

		}

		@Override
		public void OnCancel() {

		}

		@Override
		public void OnLoadComplete(int statusCode) {
			switch (statusCode) {
			case OnLoadFinishListener.ERROR_TIMEOUT:
				/*
				 * new Handler().postDelayed(new Runnable() {
				 * 
				 * @Override public void run() { checkUpdate(); } }, 30000);
				 */
				break;

			default:
				break;
			}
		}

		@Override
		public void OnPaserComplete(List<AccountingKeyWordsBean> list) {
			if (list != null) {
				for (int i = 0; i < list.size(); i++) {
					AccountingKeyWordsBean bean = list.get(i);
					if (bean != null) {
						CallStatDatabase.getInstance(context)
								.updateReconciliationCode(bean);
					}
				}
			}
		}
	};

	// 用户统计
	private void sendData() {
		long outTime = 7 * 24 * 60 * 60000l; // 一周的时间（ms）
		long naturalTime = System.currentTimeMillis()
				- configManager.getFirstStartUpTime();
		if (naturalTime >= outTime) { // 总使用时间超过1周发送数据
			final int appUsedTimes = configManager.getAppUsedTimes();
			final int totalTime = configManager.getTotalRunningTime();
			final String imei = DeviceInformation.getInformation(InfoName.IMEI);
			final String versionString = configManager.getVersionName();
			// 日使用时间
			String s = "";
			if (!"".equals(configManager.getDayUserStatus())) {
				s = configManager.getDayUserStatus() + ":"
						+ configManager.getDayTotalRunningTime();
			} else {
				long nowTime = System.currentTimeMillis();
				long lastDayTime = nowTime - 24 * 60 * 60 * 1000l;
				s = CallStatUtils.getDateStringFromMilliSec(lastDayTime) + ":"
						+ configManager.getDayTotalRunningTime();
			}
			final String daystausString = s;

			// 用户界面采集
			final int callsDetailsActivity = CallStatDatabase.getInstance(
					context).getActivityStatistic(
					CallsDetailsActivity.class.getSimpleName());
			final int trafficTopActivity = CallStatDatabase
					.getInstance(context).getActivityStatistic(
							TrafficTopActivity.class.getSimpleName());
			final int callsManagerActivity = CallStatDatabase.getInstance(
					context).getActivityStatistic(
					CallsManagerActivity.class.getSimpleName());
			final int trafficManagerActivity = CallStatDatabase.getInstance(
					context).getActivityStatistic(
					TrafficManagerActivity.class.getSimpleName());

			ILog.LogI(this.getClass(), "用户使用统计(版本 " + versionString
					+ ")：总使用次数：" + appUsedTimes + " 总使用时间：" + totalTime + "分钟"
					+ "  每日使用：" + daystausString + "界面：callsDetailsActivity：" + callsDetailsActivity
					+ " trafficTopActivity:" + trafficTopActivity + " callsManagerActivity:" + callsManagerActivity
					+ " trafficManagerActivity:" + trafficManagerActivity );
			if (CallStatUtils.isNetworkAvailable(this)
					&& userStatus_send_fause <= 5) { // 发送失败5次后停止发送，重启应用 和 发送成功
														// 或 达到自动对账的周期后失败次数将重置
				new Thread(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						try {
							String url = getResources().getString(
									R.string.user_process_url);
							Map<String, String> map = new HashMap<String, String>();
							map.put("times", appUsedTimes + "");
							map.put("newTimeTotal", String.valueOf(totalTime));
							map.put("imei", imei);
							map.put("version", versionString);
							map.put("day", daystausString);
							map.put("callsDetailsActivity", String.valueOf(callsDetailsActivity));
							map.put("trafficTopActivity", String.valueOf(trafficTopActivity));
							map.put("callsManagerActivity", String.valueOf(callsManagerActivity));
							map.put("trafficManagerActivity", String.valueOf(trafficManagerActivity));

							if (url == null) {
								return;
							}
							HttpPost myRequest = new HttpPost(url);
							UrlEncodedFormEntity myEntity = MyHttpPostHelper
									.buildUrlEncodedFormEntity(map, null);
							myRequest.setEntity(myEntity);
							DefaultHttpClient myDefaultHttpClient = new DefaultHttpClient();
							myDefaultHttpClient.getParams().setParameter(
									CoreConnectionPNames.SO_TIMEOUT, 30000);
							myDefaultHttpClient.getParams().setParameter(
									CoreConnectionPNames.CONNECTION_TIMEOUT,
									30000);
							HttpResponse myResponse = myDefaultHttpClient
									.execute(myRequest);

							int status = myResponse.getStatusLine()
									.getStatusCode();
							String strResult = EntityUtils.toString(myResponse
									.getEntity());
							// Log.e("callstats", strResult);
							ILog.LogE(this.getClass(), strResult);
							if (status == 200) {
								// 发送成功
								userStatus_send_fause = 0;
								ILog.LogI(this.getClass(), "用户统计发送成功------:"
										+ appUsedTimes + "次；" + totalTime
										+ "分钟。次数和总时间重置为0" + "  每日使用："
										+ daystausString);
								configManager.setTotalRunningTime(0); // 重置的使用的总时间
								configManager.setAppUsedTimes(0); // 重置的次数
								configManager.setDayTotalRunningTime(0);
								configManager.setDayUserStatus(CallStatUtils
										.getDateStringFromMilliSec(System
												.currentTimeMillis()));
								configManager.setFirstStartUpTime(System
										.currentTimeMillis());
								//将用户界面统计的数据库表清空
								CallStatDatabase.getInstance(
										context).dropActivityStatistic();
							} else {
								// strResult);
								userStatus_send_fause += 1; // 发送失败次数增加
							}
						} catch (Exception e) {
							ILog.logException(this.getClass(), e);
						}
					}

				}).start();

			}
		} else {
			// 没有达到7个自然日
		}
	}

	// 用户消费行为统计
	public void sendUserConsumeStatInfo() {
		final int nn = 20;// 判断是否上传上个月用户消费统计信息的一个门限值，如果上个月消费的记录（事件数目）小于20，则不上传上个月的用户消费统计信息
		// 发送上个月的用户消费统计信息（前提是上个月的记录条数大于等于nn）
		if (CallStatUtils.isNetworkAvailable(this)
				&& CallStatDatabase.getInstance(context)
						.getLastMonthConsumeCount() >= nn
				&& !configManager.getLastMonthConsumeInfoUploadFlag()
				&& configManager.getTodayUploadConsumeInfoCount() <= 10) {
			// 上传上个月的月消费行为数据给服务器
			new Thread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					try {
						String url = getResources().getString(
								R.string.userMonthlyConsume_url);
						if (url == null) {
							return;
						}
						String lastMonthLastDayDate = CallStatUtils
								.getLastMonthDayDate();
						String lastMonthFirstDayDate = lastMonthLastDayDate
								.substring(0, lastMonthLastDayDate.length() - 2)
								+ "01";
						String start = lastMonthFirstDayDate + "0000";
						String end = lastMonthLastDayDate + "2359";

						String top10_local_calls = CallStatUtils
								.makeTop10LocalString(start, end);
						String top10_long_calls = CallStatUtils
								.makeTop10LongString(start, end);
						int[] durations = CallStatDatabase.getInstance(context)
								.getDurations(start, end);
						// 结果数组保存的顺序分别为短信条数、本地市话、长途、漫游、短号、IP拨号、未知归属地号码
						String sms = String.valueOf(durations[0]);
						String localTime = String.valueOf(durations[1]);
						String longTime = String.valueOf(durations[2]);
						String roamingTime = String.valueOf(durations[3]);
						String shortTime = String.valueOf(durations[4]);
						String ipTime = String.valueOf(durations[5]);

						float feeUseInInterval = CallStatDatabase.getInstance(
								context).getFeeUsedInInterval(
								lastMonthFirstDayDate, lastMonthLastDayDate);
						long GprsUsedInInterval = CallStatDatabase.getInstance(
								context).getGprsUsedInInterval(
								lastMonthFirstDayDate, lastMonthLastDayDate);
						int days = Integer.parseInt(lastMonthLastDayDate
								.substring(lastMonthLastDayDate.length() - 2));
						// 以下产生Map的过程第一个参数是1表示是发的上个月的整月消费信息
						Map<String, String> map = CallStatUtils
								.generateMonthlyConsumeMap("1", start, end,
										top10_local_calls, top10_long_calls,
										localTime, longTime, roamingTime, sms,
										ipTime, shortTime, GprsUsedInInterval,
										feeUseInInterval, days);
						HttpPost myRequest = new HttpPost(url);
						UrlEncodedFormEntity myEntity = MyHttpPostHelper
								.buildUrlEncodedFormEntity(map, null);
						myRequest.setEntity(myEntity);
						DefaultHttpClient myDefaultHttpClient = new DefaultHttpClient();
						myDefaultHttpClient.getParams().setParameter(
								CoreConnectionPNames.SO_TIMEOUT, 30000);
						myDefaultHttpClient.getParams().setParameter(
								CoreConnectionPNames.CONNECTION_TIMEOUT, 30000);
						HttpResponse myResponse = myDefaultHttpClient
								.execute(myRequest);
						configManager
								.setTodayUploadConsumeInfoCount(configManager
										.getTodayUploadConsumeInfoCount() + 1);
						int status = myResponse.getStatusLine().getStatusCode();
						if (status == 200) {
							String strResult = EntityUtils.toString(
									myResponse.getEntity(), HTTP.UTF_8);
							if (strResult.equalsIgnoreCase("0")) {
								// 发送成功
								configManager
										.setLastTimeUploadConsumeInfoSuccess(System
												.currentTimeMillis());
								configManager
										.setLastMonthConsumeInfoUploadFlag(true);
							}
						}
					} catch (Exception e) {
						ILog.logException(this.getClass(), e);
					}
				}
			}).start();
		}

		long outTime = 7 * 24 * 60 * 60000l; // 一周的时间（ms）
		long naturalTime = System.currentTimeMillis()
				- configManager.getLastTimeUploadConsumeInfoSuccess();
		long days_var = naturalTime / (24 * 60 * 60000l);
		final long delta_days = days_var > 31 ? 31 : days_var;
		ILog.LogE(this.getClass(), "naturalTime = " + naturalTime + ",outTime="
				+ outTime);
		if (CallStatUtils.isNetworkAvailable(this) && naturalTime >= outTime
				&& configManager.getTodayUploadConsumeInfoCount() <= 10) {
			// 上传本月的自上次上传到此次上传时间间隔内的用户消费行为数据给服务器
			new Thread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					try {
						String url = getResources().getString(
								R.string.userMonthlyConsume_url);
						ILog.LogE(this.getClass(), "zhangjing url = " + url);
						if (url == null) {
							return;
						}
						String start = CallStatUtils
								.changeMilliSeconds2YearMonthDayHourMin(configManager
										.getLastTimeUploadConsumeInfoSuccess());
						String end = CallStatUtils
								.changeMilliSeconds2YearMonthDayHourMin(System
										.currentTimeMillis());

						String lastDate = start.substring(0, start.length() - 4);
						String nowDate = end.substring(0, end.length() - 4);

						String top10_local_calls = CallStatUtils
								.makeTop10LocalString(start, end);
						String top10_long_calls = CallStatUtils
								.makeTop10LongString(start, end);
						int[] durations = CallStatDatabase.getInstance(context)
								.getDurations(start, end);
						// 结果数组保存的顺序分别为短信条数、本地市话、长途、漫游、短号、IP拨号、未知归属地号码
						String sms = String.valueOf(durations[0]);
						String localTime = String.valueOf(durations[1]);
						String longTime = String.valueOf(durations[2]);
						String roamingTime = String.valueOf(durations[3]);
						String shortTime = String.valueOf(durations[4]);
						String ipTime = String.valueOf(durations[5]);

						float feeUseInInterval = CallStatDatabase.getInstance(
								context)
								.getFeeUsedInInterval(lastDate, nowDate);
						long GprsUsedInInterval = CallStatDatabase.getInstance(
								context).getGprsUsedInInterval(lastDate,
								nowDate);
						// 以下产生Map的过程第一个参数是0表示是发的不定期的消费信息
						Map<String, String> map = CallStatUtils
								.generateMonthlyConsumeMap("0", start, end,
										top10_local_calls, top10_long_calls,
										localTime, longTime, roamingTime, sms,
										ipTime, shortTime, GprsUsedInInterval,
										feeUseInInterval, (int) delta_days);
						HttpPost myRequest = new HttpPost(url);
						UrlEncodedFormEntity myEntity = MyHttpPostHelper
								.buildUrlEncodedFormEntity(map, null);
						myRequest.setEntity(myEntity);
						DefaultHttpClient myDefaultHttpClient = new DefaultHttpClient();
						myDefaultHttpClient.getParams().setParameter(
								CoreConnectionPNames.SO_TIMEOUT, 30000);
						myDefaultHttpClient.getParams().setParameter(
								CoreConnectionPNames.CONNECTION_TIMEOUT, 30000);
						HttpResponse myResponse = myDefaultHttpClient
								.execute(myRequest);
						ILog.LogE(this.getClass(), "zhangjing http request已经发送");
						configManager
								.setTodayUploadConsumeInfoCount(configManager
										.getTodayUploadConsumeInfoCount() + 1);
						int status = myResponse.getStatusLine().getStatusCode();

						ILog.LogE(
								this.getClass(),
								"zhangjing http request 发送的次数等于"
										+ configManager
												.getTodayUploadConsumeInfoCount());
						ILog.LogE(this.getClass(),
								"zhangjing http requesr status oper="
										+ configManager.getOperator());
						ILog.LogE(this.getClass(),
								"zhangjing http requesr status brand="
										+ configManager.getPackageBrand());
						ILog.LogE(
								this.getClass(),
								"zhangjing http requesr status ratesRoaming="
										+ CallStatDatabase.getInstance(context)
												.getRatesForRoamingFromDb(
														start, end) + "");
						if (status == 200) {
							String strResult = EntityUtils.toString(
									myResponse.getEntity(), HTTP.UTF_8);
							ILog.LogE(this.getClass(),
									"zhangjing http request strResult ="
											+ strResult);
							if (strResult.equalsIgnoreCase("0")) {
								// 发送成功
								ILog.LogE(this.getClass(),
										"zhangjing http request 发送成功");
								configManager
										.setLastTimeUploadConsumeInfoSuccess(System
												.currentTimeMillis());
							}
						}
					} catch (Exception e) {
						ILog.logException(this.getClass(), e);
					}

				}

			}).start();
		}
	}

	private void showChargeNotice() {
		if (configManager.getCalculateFeeAvailable() != 100000) {
			float fees_remain = configManager.getCalculateFeeAvailable();
			if (fees_remain > configManager.getAlertRemainFees()) {
			} else {
				if (fees_remain > 10) { //余额大于10元
					try {
						if (nm != null) {
							nm.cancel(3);
						}
					} catch (Exception e) {
						ILog.logException(this.getClass(), e);
					}

				} else { //余额小于10元
					if (!CALLS_CHAEGER_NOTICE) {
						callsCharegeNotice();
					}
				}
			}
		}
	}

	// 话费余额不足，弹出通知，提示用户充值
	private void callsCharegeNotice() {
		RemoteViews warn_views = new RemoteViews(getPackageName(),
				R.layout.warn_notification);
		warn_views.setTextViewText(R.id.charge_notification_content,
				"话费余额不足，请您注意充值！");
		warn_views.setViewVisibility(R.id.warn_notification_content, View.GONE);
		warn_views.setViewVisibility(R.id.charge_notification_content,
				View.VISIBLE);
		charge_notification = new Notification(R.drawable.logo_tongzhi, null,
				System.currentTimeMillis());
		/* 通知被点击后，自动消失 */
		charge_notification.flags |= Notification.FLAG_AUTO_CANCEL;
		PendingIntent m_PendingIntent = PendingIntent.getActivity(
				CallStatSMSService.this, 0, new Intent(), 0);
		charge_notification.contentView = warn_views;
		charge_notification.contentIntent = m_PendingIntent;
		if (nm != null) {
			nm.notify(3, charge_notification);
			CALLS_CHAEGER_NOTICE = true;
		}
	}

}
