package com.android.callstat.home;

import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;

import com.android.callstat.CallStatApplication;
import com.android.callstat.ConfigManager;
import com.android.callstat.ILog;
import com.android.callstat.accounting.AccountingKeyWordsBean;
import com.android.callstat.accounting.ReconciliationUtils;
import com.android.callstat.common.CallStatUtils;
import com.android.callstat.common.ResourceLoader.OnLoadFinishListener;
import com.android.callstat.common.database.CallStatDatabase;
import com.android.callstat.common.download.OfflineDownloadEvent;
import com.android.callstat.common.download.OfflineDownloadManager;
import com.android.callstat.common.json.AbstractWebLoadManager.OnWebLoadListener;
import com.android.callstat.home.bean.ClientUpdate;
import com.android.callstat.home.json.UpdateManager;
import com.android.callstat.home.views.CustomTabHost;
import com.android.callstat.service.CallStatSMSService;
import com.android.callstat.service.json.AccountingDatabaseUpdater;
import com.archermind.callstat.R;

public class CallStatMainActivity extends TabActivity implements
		OnTabChangeListener, OnGestureListener {

	private GestureDetector gestureDetector;
	private static final int FLEEP_DISTANCE = 120;

	private int currentTabID = 0;

	private CustomTabHost tabHost;
	private CallStatApplication application;
	private int[] icons_off = { R.drawable.new_button_callbutler_normal,
			R.drawable.new_button_datamanager_normal /* , R.drawable.setting_off */};
	private String[] strings = { "话费查询", "流量监控"/* , "信息设置" */};
	private Intent[] intents = new Intent[2];
	private TabSpec[] tabSpecs = new TabSpec[2];
	public LinearLayout views;
	private ImageView icon; // 切换卡的图标
	private TextView tabspectxt;// 切换卡的标签

	boolean isCheckTabPressed = false;

	public static String a = "0";
	public static String b = "1";
	// public static String c = "2";

	private ClientUpdate clientUpdate;

	private int lastTabId;
	private static HashMap<String, Integer> unclickedicon = new HashMap<String, Integer>();
	private static HashMap<String, Integer> clickedicon = new HashMap<String, Integer>();
	public static boolean correctFailFlag = false;

	/**
	 * from here is the part for menu
	 * */
	// private String[] strMenu = { "数据更新", "分享我吧", "关于我们", "意见反馈", "使用帮助",
	// "退出程序" };
	private int[] iconMenu = { R.drawable.pop_up_ico_refresh,
			R.drawable.pop_up_ico_share, R.drawable.pop_up_ico_about,
			R.drawable.pop_up_ico_feedback, R.drawable.pop_up_ico_help,
			R.drawable.pop_up_ico_exit };

	/**
	 * from here is the part for update
	 * */
	private String mUrl;
	private UpdateManager updateManager;
	private AccountingDatabaseUpdater accountingDatabaseUpdater;
	private ProgressDialog progressDialog;
	public static final String CHANGE_UPDATE_NOTICE_ACTION = "change_update_notice_action";
	private ProgressDialog mProgressDialog;
	private static final int MAX_PROGRESS = 100;
	private NotificationManager notificationManager;
	private RemoteViews contentView;
	private Notification notification;
	private int _ID = 0x111;
	private int mProgressStatus = 0;

	private ConfigManager config;
	String action;
	static {
		unclickedicon.put(a, R.drawable.new_button_callbutler_normal);
		unclickedicon.put(b, R.drawable.new_button_datamanager_normal);
		// unclickedicon.put(c, R.drawable.setting_off);

		clickedicon.put(a, R.drawable.new_button_callbutler_select);
		clickedicon.put(b, R.drawable.new_button_datamanager_select);
		// clickedicon.put(c, R.drawable.setting_on);

	}

	private CallStatSMSService.SMSBinder binder;
	private ServiceConnection conn = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		overridePendingTransition(R.anim.push_down_in, R.anim.push_down_out);
		try {
			conn = new ServiceConnection() {

				@Override
				public void onServiceDisconnected(ComponentName name) {

				}

				@Override
				public void onServiceConnected(ComponentName name,
						IBinder service) {
					binder = (CallStatSMSService.SMSBinder) service;
					if (binder != null && config.getFirstAccount()) {
						CallStatApplication.calls_anim_is_run = true;
						CallStatApplication.traffic_anim_is_run = true;

						// 重置 开始 全对账的时间
						long now = System.currentTimeMillis();
						config.setLastCheckTime(now);

						binder.sendAccounting(ReconciliationUtils.SEND_QUERY);
						config.setFirstAccount(false);
						binder.initNotification();
					}
				}
			};
			application = (CallStatApplication) getApplication();
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			setContentView(R.layout.callstatmain);
			// 获取tabhost
			tabHost = (CustomTabHost) findViewById(android.R.id.tabhost);
			// tabHost = (TabHost) findViewById(R.id.main_tabhost);
			// tabHost.setup(this.getLocalActivityManager());
			tabHost.setCount_True(false);
			tabHost.setOnTabChangedListener(this);
			config = new ConfigManager(this);
			action = getIntent().getAction();
			init();
			initNotification();

			application.activities.add(this);

			mUrl = getString(R.string.request_update_url);
			// setupUpdate(application.getmClientUpdate());
			if (isUpdateTimeup()) {
				if (CallStatUtils.isNetworkAvailable(this)) {
					config.setUpdateNotice(true);
					checkUpdate();
				}
			}

			lastTabId = tabHost.getCurrentTab();

			gestureDetector = new GestureDetector(this);
			/*
			 * new View.OnTouchListener() { public boolean onTouch(View v,
			 * MotionEvent event) { if (gestureDetector.onTouchEvent(event)) {
			 * return true; } return false; } };
			 */

			/* 绑定服务 */
			Intent intent = new Intent(this, CallStatSMSService.class);
			bindService(intent, conn, Context.BIND_AUTO_CREATE);

			if (correctFailFlag) {
				if (isMIUI4Plus()) {
					showCustomDialog(R.string.correct_account_faile_miui);
				} else {
					showCustomDialog(R.string.correct_account_faile);
				}
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	private boolean isMIUI4Plus() {
		String model = android.os.Build.MODEL;
		String release = android.os.Build.VERSION.RELEASE;
		String incremental = android.os.Build.VERSION.INCREMENTAL;
		return model.startsWith("MI-") && release.startsWith("4")
				&& incremental.startsWith("ICS");
	}

	private void showCustomDialog(int resId) {
		final Dialog dialog = new Dialog(this, R.style.myDialogTheme);
		dialog.setContentView(R.layout.correct_account_fail_dialog);

		TextView contentText = (TextView) dialog.findViewById(R.id.content);
		contentText.setText(resId);
		Button continue_use = (Button) dialog.findViewById(R.id.continue_use);
		Button correct_succ = (Button) dialog.findViewById(R.id.correct_succ);

		continue_use.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				dialog.dismiss();
			}
		});

		correct_succ.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				correctFailFlag = false;
				dialog.dismiss();
			}
		});

		dialog.show();

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		application.activities.remove(this);
		unbindService(conn);
	}

	@Override
	protected void onPause() {
		super.onPause();
		overridePendingTransition(0, R.anim.push_up_out);
		// Log.i("i", "onPause");
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	// init方法
	public void init() {

		tabSpecs[0] = tabHost.newTabSpec("0");
		tabSpecs[1] = tabHost.newTabSpec("1");
		// tabSpecs[2] = tabHost.newTabSpec("2");

		// 切换卡的跳转
		Intent intent0 = new Intent(CallStatMainActivity.this,
				CallsManagerActivity.class);

		if ("first".equals(action)) {
			intent0.setAction("first");
		}

		intents[0] = intent0;
		Intent intent1 = new Intent(CallStatMainActivity.this,
				TrafficManagerActivity.class);
		intents[1] = intent1;

		initTabs(tabSpecs, icons_off, strings, intents);
	}

	public void initTabs(TabSpec[] tabSpecs, int[] icons, String[] strings,
			Intent[] intents) {
		try {
			int len = icons.length;
			for (int i = 0; i < len; i++) {
				// 设置切换卡的布局
				views = (LinearLayout) LayoutInflater.from(this).inflate(
						R.layout.tabspec, null);
				// tabspectxt = (TextView)
				// views.findViewById(R.id.tabspec_text);
				icon = (ImageView) views.findViewById(R.id.tabspec_icon);
				icon.setImageResource(icons[i]);
				// tabspectxt.setText(strings[i]);
				tabSpecs[i].setIndicator(views).setContent(intents[i]);
				tabHost.addTab(tabSpecs[i]);
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	// 设置切换页的点击效果(图片的改变)
	@Override
	public void onTabChanged(String tabId) {
		try {
			int current = Integer.valueOf(tabId);

			int last = lastTabId;
			ImageView iv = (ImageView) tabHost.getTabWidget().getChildAt(last)
					.findViewById(R.id.tabspec_icon);
			iv.setImageDrawable(getResources().getDrawable(
					unclickedicon.get(String.valueOf(lastTabId))));

			iv = (ImageView) tabHost.getTabWidget().getChildAt(current)
					.findViewById(R.id.tabspec_icon);
			iv.setImageDrawable(getResources().getDrawable(
					clickedicon.get(tabId)));
			lastTabId = Integer.parseInt(tabId);
			if (current != 0) {
				tabHost.setCount_True(true);
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	/**
	 * from here is the part for menu
	 */
	/*
	 * private void initHomeMenu() { // Log.i("i", "initHomeMenu"); bodyAdapter
	 * = new HomeOptionMenu.MenuBodyAdapter(this, strMenu, iconMenu, 13,
	 * 0xFFFFFFFF); optionMenu = new HomeOptionMenu(this, new BodyClickEvent(),
	 * 0x55123456, R.anim.popup_animation);
	 * 
	 * optionMenu.update(); optionMenu.SetBodyAdapter(bodyAdapter); }
	 */

	/*
	 * class BodyClickEvent implements OnItemClickListener {
	 * 
	 * @Override public void onItemClick(AdapterView<?> arg0, View arg1, int
	 * arg2, long arg3) { optionMenu.dismiss(); switch (arg2) { case 5:// exit
	 * appExit(CallStatMainActivity.this); break; case 0:// goto system setting
	 * checkAccountingCodeUpdate(); break; case 1:// share me goToShare();
	 * break; case 2:// about us goToAboutUs(); break; case 3:// any
	 * suggestions? goToFeedBack(); break; case 4:// looking for help?
	 * goToHelp(); break; default: break; } } }
	 */

	/**
	 * from here is the part for update
	 */

	private void checkUpdate() {
		try {
			updateManager = new UpdateManager(CallStatMainActivity.this, mUrl);
			updateManager.setManagerListener(updateListener);
			updateManager.startManager();
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	private OnWebLoadListener<ClientUpdate> updateListener = new OnWebLoadListener<ClientUpdate>() {

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

			/*
			 * case OnLoadFinishListener.ERROR_REQUEST_FAILED:
			 * 
			 * new Handler().postDelayed(new Runnable() {
			 * 
			 * @Override public void run() { checkUpdate(); } }, 30000);
			 * 
			 * if (progressDialog != null) { progressDialog.dismiss(); } new
			 * AlertDialog.Builder(AboutUsActivity.this) .setTitle("更新提示")
			 * .setMessage("网络异常，更新失败，请稍候再试！") .setPositiveButton("确定", null)
			 * .create().show(); break;
			 */

			default:
				break;
			}
		}

		@Override
		public void OnPaserComplete(ClientUpdate clientUpdate) {
			if (null != clientUpdate) {
				// Log.i("i", "update is not null" +
				// clientUpdate.getVersionName());
				setupUpdate(clientUpdate);
				CallStatMainActivity.this.clientUpdate = clientUpdate;
			}
		}
	};

	/**
	 * @param clientUpdate
	 */
	private void storeUpdateInfo(ClientUpdate clientUpdate) {
		config.setVersionCode(clientUpdate.getVersionCode());
		config.setVersionName(clientUpdate.getVersionName());
		config.setUpdateUrl(clientUpdate.getDownloadUrl());
		config.setUpdateApkName(clientUpdate.getApkName());
	}

	/**
	 * @param ClientUpdate
	 *            t
	 */
	protected void setupUpdate(ClientUpdate t) {
		// Log.i("i", "setupUpdate----------");
		if (null != t) {
			// Log.i("i", "setupUpdate----------ClientUpdate != null");
			if (CallStatUtils.isApplicationUpdatable(CallStatMainActivity.this,
					getPackageName(), t.getVersionCode())
					&& !application.isUpdating()) {
				if (t.isForceUpdate()) {
					forceUpdate(t);
				} else {
					normalUpdate(t);
				}
			}
		} else {
			// Log.i("i", "CallStatMainActivity ----- ClientUpdate == null");
		}
	}

	/**
	 * @param t
	 */
	private void forceUpdate(final ClientUpdate t) {

		final Dialog dialog = new Dialog(this, R.style.myDialogTheme);
		dialog.setContentView(R.layout.update_dialog);
		Button updateNow = (Button) dialog.findViewById(R.id.update_now);
		Button updateLater = (Button) dialog.findViewById(R.id.update_later);
		Button updateOk = (Button) dialog.findViewById(R.id.callstat_ok);

		updateLater.setVisibility(View.GONE);
		updateNow.setVisibility(View.GONE);
		updateOk.setVisibility(View.VISIBLE);

		updateOk.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startDownload(CallStatMainActivity.this, t.getDownloadUrl());
				dialog.dismiss();
			}
		});

		TextView description_tv = (TextView) dialog
				.findViewById(R.id.home_update_description);
		TextView update_title = (TextView) dialog
				.findViewById(R.id.home_update_title);
		update_title.setText(getString(R.string.found_new_version_force));
		StringBuilder sb = new StringBuilder();
		String[] descriptions = t.getDescription();
		int len = descriptions.length;
		for (int i = 0; i < len; i++) {
			sb.append(descriptions[i] + "\n");
		}
		description_tv.setText(sb.toString());

		dialog.show();
	}

	/**
	 * @param t
	 */
	private void normalUpdate(final ClientUpdate t) {

		boolean isUpdateNow = config.isUpdateNotice();
		// Log.i("i", "is update now:----" + isUpdateNow);
		if (isUpdateNow) {
			final Dialog dialog = new Dialog(this, R.style.myDialogTheme);
			dialog.setContentView(R.layout.update_dialog);
			Button updateNow = (Button) dialog.findViewById(R.id.update_now);
			Button updateLater = (Button) dialog
					.findViewById(R.id.update_later);

			updateNow.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					startDownload(CallStatMainActivity.this, t.getDownloadUrl());
					config.setUpdateLater(System.currentTimeMillis());
					dialog.dismiss();
				}
			});

			updateLater.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					config.setUpdateNotice(false);
					config.setUpdateLater(System.currentTimeMillis());

					/*
					 * AlarmManager am = (AlarmManager)
					 * getSystemService(ALARM_SERVICE); Intent intent = new
					 * Intent( CallStatApplication .getCallstatsContext(),
					 * CallStatReceiver.class); intent.setAction(
					 * CHANGE_UPDATE_NOTICE_ACTION); PendingIntent pendingItent
					 * = PendingIntent.getBroadcast( CallStatApplication
					 * .getCallstatsContext(), 0, intent,
					 * Intent.FLAG_ACTIVITY_NEW_TASK); long interval = 24 * 60 *
					 * 60 * 1000; Calendar c = Calendar.getInstance(); long
					 * triggerTime = CallStatUtils .getNowToTomorrowMillis();
					 * am.setRepeating( AlarmManager.ELAPSED_REALTIME_WAKEUP,
					 * triggerTime, interval, pendingItent);
					 */

					dialog.dismiss();
				}
			});

			TextView version_tv = (TextView) dialog
					.findViewById(R.id.version_tv);
			TextView apksize_tv = (TextView) dialog
					.findViewById(R.id.apksize_tv);
			TextView description_tv = (TextView) dialog
					.findViewById(R.id.home_update_description);
			TextView update_title = (TextView) dialog
					.findViewById(R.id.home_update_title);
			update_title.setText(getString(R.string.found_new_version));

			version_tv.setText(t.getVersionName());
			String[] parts = CallStatUtils.traffic_unit(t.getApkSize());
			apksize_tv.setText(parts[0] + parts[1]);

			StringBuilder sb = new StringBuilder();
			String[] descriptions = t.getDescription();
			int len = descriptions.length;
			for (int i = 0; i < len; i++) {
				sb.append(descriptions[i] + "\n");
			}
			description_tv.setText(sb.toString());

			dialog.show();
		}
	}

	/**
	 * @param context
	 * @param url
	 * 
	 */
	private void initDownload(final Context context, String url) {
		application.setUpdating(true);
		mProgressDialog = new ProgressDialog(CallStatMainActivity.this);
		mProgressDialog.setTitle("版本更新");
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setMax(MAX_PROGRESS);
		mProgressDialog.setButton(DialogInterface.BUTTON_POSITIVE,
				getString(R.string.download_hide),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
				getString(R.string.download_cancel),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						cancelDownload();
						dialog.dismiss();
					}
				});

		mProgressDialog.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(DialogInterface dialog, int keyCode,
					KeyEvent event) {

				if (keyCode == KeyEvent.KEYCODE_BACK) {
					if (mProgressDialog != null && mProgressDialog.isShowing()
							&& event.getAction() != KeyEvent.ACTION_UP) {
						// Log.i("callstats",
						// "onKeyDown keyCode == KeyEvent.KEYCODE_BACK");
						Toast.makeText(CallStatMainActivity.this,
								"系统正在下载更新，请选择后台更新或者取消更新再试哦～",
								Toast.LENGTH_SHORT).show();
						return true;
					}
					return false;
				}
				return false;
			}
		});

		mProgressDialog.show();

		mProgressDialog
				.setOnCancelListener(new DialogInterface.OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
						cancelDownload();
					}
				});

		OfflineDownloadManager.getInstance(context).startDownloadTask(0, url,
				new OfflineDownloadEvent() {

					@Override
					public void start(int busId) {

					}

					@Override
					public void progress(int busId, int progress) {
						if (progress >= MAX_PROGRESS) {
							mProgressDialog.dismiss();
						} else {
							mProgressDialog.setProgress(progress);
						}
						contentView.setTextViewText(R.id.notificationPercent,
								progress + "%");
						contentView.setProgressBar(R.id.notificationProgress,
								100, progress, false);
						notificationManager.notify(_ID, notification);
					}

					@Override
					public void error(int busId, int status) {
						application.setUpdating(false);
						if (mProgressDialog != null) {
							mProgressDialog.dismiss();
						}
						cancelDownload();
						// startActivity(new
						// Intent().setClass(CallStatApplication.getCallstatsContext(),
						// UpdateExceptionActivity.class));
					}

					@Override
					public void complete(int busId) {
						notificationManager.cancel(_ID);
						application.setUpdating(false);
						if (clientUpdate != null) {
							storeUpdateInfo(clientUpdate);
						}
					}

					@Override
					public void cancal(int busId) {
						application.setUpdating(false);
					}
				}, false);
		// notificationManager.notify(_ID, notification);
	}

	public void startDownload(Context context, String url) {
		initDownload(context, url);
	}

	private void cancelDownload() {
		OfflineDownloadManager.getInstance(CallStatMainActivity.this)
				.cancelDownloadTask(0);
		notificationManager.cancel(_ID);
	}

	private void initNotification() {
		String tickerText = getString(R.string.app_name);
		int icon = android.R.drawable.stat_sys_download;
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notification = new Notification(icon, tickerText,
				System.currentTimeMillis());
		Intent notificationIntent = new Intent(this, this.getClass());
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		contentView = new RemoteViews(getPackageName(),
				R.layout.home_notification);

		contentView.setTextViewText(R.id.notificationTitle, tickerText);
		contentView.setTextViewText(R.id.notificationPercent, mProgressStatus
				+ "%");
		contentView.setProgressBar(R.id.notificationProgress, MAX_PROGRESS,
				mProgressStatus, false);
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.contentView = contentView;
		notification.contentIntent = contentIntent;
	}

	private boolean isUpdateTimeup() {
		return (System.currentTimeMillis() - config.getUpdateLater()) > (24 * 60 * 60 * 1000);
	}

	private void showDialogupdate(String msg) {
		new AlertDialog.Builder(CallStatMainActivity.this).setTitle("更新提示")
				.setMessage(msg).setPositiveButton(R.string.callstat_ok, null)
				.create().show();
	}

	private void checkAccountingCodeUpdate() {
		if (CallStatUtils.isNetworkAvailable(this)) {
			progressDialog = ProgressDialog.show(CallStatMainActivity.this,
					null, "正在检查更新...", true);
			progressDialog.setCancelable(true);
			String url = getString(R.string.accounting_database_update_url);
			// Log.i("callstats", "checkUpdate url :" + url);
			if (accountingDatabaseUpdater == null) {
				accountingDatabaseUpdater = new AccountingDatabaseUpdater(
						getBaseContext(), url);
			}
			accountingDatabaseUpdater
					.setManagerListener(accountingDatabaseUpdateListener);
			accountingDatabaseUpdater.startManager();
		} else {
			showDialogupdate("当前网络不可用，请检查网络状态！");
		}
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
				if (progressDialog != null) {
					progressDialog.dismiss();
				}
				break;
			case OnLoadFinishListener.ERROR_REQUEST_FAILED:
				if (progressDialog != null) {
					progressDialog.dismiss();
				}
				break;
			default:
				break;
			}
		}

		@Override
		public void OnPaserComplete(List<AccountingKeyWordsBean> list) {
			if (list.size() == 0) {
				showDialogupdate("当前已是最新数据。");
			} else {
				for (int i = 0; i < list.size(); i++) {
					AccountingKeyWordsBean bean = list.get(i);
					if (bean != null) {
						CallStatDatabase.getInstance(CallStatMainActivity.this)
								.updateReconciliationCode(bean);
					}
				}
				showDialogupdate("数据更新成功。");
			}
			if (progressDialog != null) {
				progressDialog.dismiss();
			}
		}
	};

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		if (e1.getX() - e2.getX() <= (-FLEEP_DISTANCE)) { // 向右滑动
			if (currentTabID == 0) {

			} else {
				currentTabID = currentTabID - 1;
			}
		} else if (e1.getX() - e2.getX() >= FLEEP_DISTANCE) { // 向左滑动
			if (currentTabID == tabHost.getChildCount()) {

			} else {
				currentTabID = currentTabID + 1;
			}
		}
		tabHost.setCurrentTab(currentTabID);
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {

	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {

	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		if (gestureDetector.onTouchEvent(event)) {
			event.setAction(MotionEvent.ACTION_CANCEL);
		}
		return super.dispatchTouchEvent(event);
	}

	// TabActivity中监听返回键
	/*
	 * @Override public boolean dispatchKeyEvent(KeyEvent event) { if
	 * (event.getKeyCode() == KeyEvent.KEYCODE_BACK) { Log.i("xx",
	 * "KEYCODE_BACK"); finish(); overridePendingTransition(0,
	 * R.anim.push_up_out); return false; } return
	 * super.dispatchKeyEvent(event); }
	 */
}
