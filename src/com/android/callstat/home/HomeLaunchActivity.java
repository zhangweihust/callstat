package com.android.callstat.home;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;

import com.android.callstat.CallStatApplication;
import com.android.callstat.ConfigManager;
import com.android.callstat.ILog;
import com.android.callstat.common.CallStatUtils;
import com.android.callstat.common.DeviceInformation;
import com.android.callstat.common.DeviceUtils;
import com.android.callstat.common.DeviceInformation.InfoName;
import com.android.callstat.common.database.CallStatDatabase;
import com.android.callstat.common.net.MyHttpPostHelper;
import com.android.callstat.monitor.bean.Sms;
import com.archermind.callstat.R;

/**
 * 程序运行的第一个画面
 * 
 * @author longX
 */
public class HomeLaunchActivity extends Activity {

	public static final String TAG = "callstats";
	private boolean isDeviceDataReady = false;
	private boolean isGotoHome = false;
	private boolean mIsFreshStart = true;

	private static final int TRY_TO_GO_TO_HOME = 101;
	private static final int GO_TO_HOME_NOW = 102;

	private Map<String, String> map;

	// views
	/*
	 * private ImageView loadImageView; private AnimationDrawable
	 * animationDrawable;
	 */

	public static final String UPDATE = "update";

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:

			return true;

		default:
			break;
		}
		return super.onKeyDown(keyCode, event);
	}

	private long acc_day_millis = 0;
	private ConfigManager config;
	private CallStatApplication myapp;
	private StateWatcher watcher;

	// this is a fake url
	long now;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		myapp = (CallStatApplication) getApplication();
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.home_launch);
		// loadImageView.getAnimation();
		config = new ConfigManager(this);
		acc_day_millis = CallStatUtils.getAccDayInMillis(config);
		mIsFreshStart = myapp.isFreshStart();
		watcher = new StateWatcher();
		if (config.isFirstLaunch()) {
			Log.i("i", "isfirstLaunch:" + config.isFirstLaunch());
			new InitLocalDataThread().execute();
			watcher.sendEmptyMessageDelayed(TRY_TO_GO_TO_HOME, 2000);
			// 新的机制已经不需要初始化通话记录和短信记录条数了
			// new InitCallLogThread().execute();

			// 初始化 用户统计的初始时间
			long startuptime = System.currentTimeMillis();
			config.setDayUserStatus(CallStatUtils
					.getDateStringFromMilliSec(startuptime));
			config.setFirstStartUpTime(startuptime);
			config.setLastTimeUploadConsumeInfoSuccess(startuptime);// added by
																	// zhangjing@archermind.com

		} else {
			if (mIsFreshStart) {
				// 信息重发机制
				if (!config.isDeviceInfoSent()) {
					new InitLocalDataThread().execute();
					watcher.sendEmptyMessageDelayed(TRY_TO_GO_TO_HOME, 2000);
				} else {
					isDeviceDataReady = true;
					watcher.sendEmptyMessageDelayed(GO_TO_HOME_NOW, 2000);
				}
				// new InitCallLogThread().execute();
			} else {
				watcher.sendEmptyMessageDelayed(GO_TO_HOME_NOW, 2000);
			}
		}
		CallStatUtils.startServices(CallStatApplication.getCallstatsContext(),
				CallStatUtils.callStatServices);
		// 启动程序检测绑定状态，要是为0，说明上次绑定没有成功，将其置为-1.
		if (config.getPhoneBindingStatus() == 0) {
			config.setPhoneBindingStatus(-1);
		}
	}

	private void goToHome() {
		isGotoHome = true;
		Intent intent = new Intent();
		if (config.isFirstLaunch()) {
			intent.setClass(HomeLaunchActivity.this, HomeGuideActivity.class);
		} else {
			intent.setClass(HomeLaunchActivity.this, CallStatMainActivity.class);
		}
		Log.i("i", "in gotohome");
		startActivity(intent);
		myapp.setFreshStart(false);
		HomeLaunchActivity.this.finish();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (watcher.hasMessages(GO_TO_HOME_NOW)) {
			watcher.removeMessages(GO_TO_HOME_NOW);
			// Log.i("i", "onStop watcher.removeMessages(GO_TO_HOME_NOW);");
		}
		if (watcher.hasMessages(TRY_TO_GO_TO_HOME)) {
			watcher.removeMessages(TRY_TO_GO_TO_HOME);
			// Log.i("i", "onStop watcher.removeMessages(TRY_TO_GO_TO_HOME);");
		}
		finish();
	}

	public void prepareToGoHome(boolean isFreshStart) {
		// TODO if() service or activity?
		// if data ready? list.size()>0
		// else { }
		if (isFreshStart) {
			if (/* isDeviceDataReady && */!isGotoHome) {
				goToHome();
			}
		} else {
			goToHome();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.ViewTreeObserver.OnPreDrawListener#onPreDraw()
	 */
	/*
	 * @Override public boolean onPreDraw() { if (mFirst) {
	 * animationDrawable.start(); mFirst = false; } return true; }
	 */

	@Override
	protected void onDestroy() {
		super.onDestroy();
		myapp.setFreshStart(false);
	}

	// 定义MyHandler
	public class StateWatcher extends Handler {
		// 处理消息
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			if (msg.what == TRY_TO_GO_TO_HOME) {
				prepareToGoHome(true);
			} else if (msg.what == GO_TO_HOME_NOW) {
				prepareToGoHome(false);
			}
		}
	}

	public class InitLocalDataThread extends AsyncTask<Object, Object, Object> {

		@Override
		protected Object doInBackground(Object... params) {

			myapp.initResources();
			// when copy data is done,you are able to update database
			Log.i("i", "HomeLaunchActivity ------ isCopyready");

			config.setIMSI(DeviceUtils.getIMSI(HomeLaunchActivity.this));
			// Log.e("callstats", config.getIMSI());

			// 从数据库恢复数据到配置文件
			// 首先判断是否有sd卡上的db文件
			try {
				File sdCardDir = Environment.getExternalStorageDirectory();
				String dbPathOnSdcard = sdCardDir.getCanonicalPath()
						+ CallStatDatabase.DATABASE_FILE_ON_SDCARD;
				if (CallStatUtils.isDatabaseFileExist(dbPathOnSdcard)) {
					ILog.LogI(getClass(), "sd card and the sddb exists");
					CallStatDatabase.getInstance(HomeLaunchActivity.this)
							.recoverSharedPrefernce();
					ILog.LogI(getClass(), "recoverSharedPrefernce successful");
				}
			} catch (Exception e) {
				ILog.logException(getClass(), e);
			}

			map = new HashMap<String, String>();

			/*
			 * String telstats =
			 * DeviceUtils.fetch_tel_status(HomeLaunchActivity.this); String cpu
			 * = DeviceUtils.fetch_cpu_info(); String memory =
			 * DeviceUtils.getMemoryInfo(HomeLaunchActivity.this); String screen
			 * = DeviceUtils.getDisplayMetrics(HomeLaunchActivity.this); String
			 * easyScrren = DeviceUtils.getDeviceScreenSize(); Log.i("i",
			 * "easyScreen:" + easyScrren); String osversion =
			 * DeviceUtils.getOsVersion(); Log.i("i", "osversion:" + osversion);
			 */

			// Message msg = new Message();
			for (InfoName name : InfoName.values()) {
				switch (name) {
				case IMEI:
					String imei = DeviceInformation.getInformation(name);
					// Log.i("i", "imei:" + imei);
					config.setImei(imei);
					map.put("imei", imei);
					continue;

				case CPU_MAX_FREQUENCY:
					String cpuMax = DeviceUtils.getMaxCpuFreq();
					// Log.i("i", "cpuMax:" + cpuMax);
					config.setCpuMaxFreq(cpuMax);
					map.put("cpuClk", cpuMax);
					continue;

				case CPU_MODEL:
					String cpuModel = DeviceInformation.getInformation(name);
					// Log.i("i", "cpuModel:" + cpuModel);
					config.setCpuModel(cpuModel);
					map.put("cpuModel", cpuModel);
					continue;

				case MEMORY_TOTAL:
					String memoryTotal = DeviceInformation.getInformation(name);
					Log.i("i", "memoryTotal:" + memoryTotal);
					config.setMemory(memoryTotal);
					map.put("memoryTotal", memoryTotal);
					continue;

				case PHONE_CALLSTATS_VERSION:
					String version = DeviceInformation.getInformation(name);
					// Log.i("i", "version:" + version);
					config.setCallstatVersion(version);
					map.put("callstatsVersion", version);
					continue;

				case PHONE_MODEL:
					String phoneModel = DeviceInformation.getInformation(name);
					// Log.i("i", "phoneModel:" + phoneModel);
					config.setMobileModel(phoneModel);
					map.put("phoneModel", phoneModel);
					continue;

				case SCREEN_RESOLUTION:
					String screen = DeviceInformation.getInformation(name);
					// Log.i("i", "screen:" + screen);
					config.setScreenSize(screen);
					map.put("screenResolution", screen);
					continue;

				case SYSTEM_VERSION:
					String osVersion = DeviceInformation.getInformation(name);
					// Log.i("i", "osVersion:" + osVersion);
					config.setOsVersion(osVersion);
					map.put("osVersion", osVersion);
					continue;
				default:
					break;
				}
			}
			Log.i("i", "HomeLaunchActivity ------ isDeviceDataReady");

			if (!config.isDeviceInfoSent()) {
				new AsyncTask<Object, Void, Void>() {

					@Override
					protected Void doInBackground(Object... params) {
						if (CallStatUtils
								.isNetworkAvailable(HomeLaunchActivity.this)) {
							Log.i(TAG, "isNetworkAvailable" + true);
							String url = getResources().getString(
									R.string.user_stats_url);
							Log.i(TAG, "urser info url:" + url);
							HttpPost request = new HttpPost(url);
							UrlEncodedFormEntity entity = MyHttpPostHelper
									.buildUrlEncodedFormEntity(
											(Map<String, String>) params[0],
											null);
							request.setEntity(entity);
							DefaultHttpClient client = new DefaultHttpClient();

							try {
								HttpResponse response = client.execute(request);
								int code = response.getStatusLine()
										.getStatusCode();
								String strResult = EntityUtils
										.toString(response.getEntity());
								Log.i(TAG, "code:" + code);
								Log.i(TAG, strResult);
								if (code == 200) {
									config.setDeviceInfoSent(true);
									Log.i(TAG, "userinfo sent successfully!");
								}
							} catch (ClientProtocolException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						return null;
					}
				}.execute(map);
			}
			isDeviceDataReady = true;
			return null;
		}

	}

	class InitCallLogThread extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			Cursor c = null;
			int total_call_outgoing = config.getTotalOutgoingCall();
			int total_call_incoming = config.getTotalIncomingCall();

			int current_call_outgoing = 0;
			int current_call_incoming = 0;

			String[] projection = new String[] { Calls.DATE, Calls.NUMBER,
					Calls.TYPE, Calls.DURATION };
			String where = Calls.DATE + " > ? ";
			String[] selectionArgs = new String[] { String
					.valueOf(acc_day_millis) };

			try {
				c = getContentResolver().query(CallLog.Calls.CONTENT_URI,
						projection, where, selectionArgs,
						Calls.DEFAULT_SORT_ORDER);
				if (c != null) {
					if (c.moveToFirst()) {
						do {
							/*
							 * public static final int INCOMING_TYPE = 1; public
							 * static final int OUTGOING_TYPE = 2; public static
							 * final int MISSED_TYPE = 3;
							 */
							int type = c.getInt(c.getColumnIndex(Calls.TYPE));
							String number = c.getString(c
									.getColumnIndex(Calls.NUMBER));
							if (type == CallLog.Calls.MISSED_TYPE
									|| "10086".equals(number)
									|| "10010".equals(number)
									|| "10001".equals(number)) {
								continue;
							}
							if (type == CallLog.Calls.OUTGOING_TYPE) {
								int duration = c.getInt(c
										.getColumnIndex(Calls.DURATION));
								if (duration % 60 == 0) {
									current_call_outgoing += duration / 60;
								} else {
									current_call_outgoing += duration / 60 + 1;
								}
								continue;
							}
							if (type == CallLog.Calls.INCOMING_TYPE) {
								int duration = c.getInt(c
										.getColumnIndex(Calls.DURATION));
								if (duration % 60 == 0) {
									current_call_incoming += duration / 60;
								} else {
									current_call_incoming += duration / 60 + 1;
								}
								continue;
							}

						} while (c.moveToNext());

						/*
						 * Log.i("i",
						 * "HomeLaunchActivity -- current month current_call_outgoing:"
						 * + current_call_outgoing + " secs"); Log.i("i",
						 * "HomeLaunchActivity -- current month total_call_outgoing:"
						 * + total_call_outgoing + " secs");
						 * 
						 * Log.i("i",
						 * "HomeLaunchActivity -- current month current_call_incoming:"
						 * + current_call_incoming + " secs"); Log.i("i",
						 * "HomeLaunchActivity -- current month total_call_incoming:"
						 * + total_call_incoming + " secs");
						 */

						if (current_call_outgoing > total_call_outgoing) {
							config.setTotalOutgoingCall(current_call_outgoing);
							/*
							 * Log.i("i",
							 * "HomeLaunchActivity -- current month total_call_incoming:"
							 * + config.getTotalOutgoingCall() + " mins");
							 */
						}
						if (current_call_incoming > total_call_incoming) {
							config.setTotalIncomingCall(current_call_incoming);
							/*
							 * Log.i("i",
							 * "HomeLaunchActivity -- current month total_call_incoming:"
							 * + config.getTotalIncomingCall() + " mins");
							 */
						}
					}
				}
			} catch (Exception e) {
				// Log.i("i", "init call log count excption:" + e.getMessage());
			} finally {
				if (c != null)
					c.close();
			}
			Log.i("i", "HomeLaunchActivity ------ isCallsDataReady");

			Cursor c_sms = null;
			int total_sms_sent = config.getTotalSmsSent();

			int current_sms_sent = 0;
			// type: 2 for sent
			String[] sms_projection = new String[] { "date", "type", "address" };
			String sms_where = " DATE " + " > ? AND ADDRESS NOT LIKE ? ";
			// String numFilter = config.getOperatorNum();
			String[] sms_selectionArgs = new String[] {
					String.valueOf(acc_day_millis), "10086%" };
			String orderBy = "date desc";
			try {
				Log.i("i", "HomeLaunchActivity ------ try");
				c_sms = getContentResolver().query(Sms.CONTENT_URI,
						sms_projection, sms_where, sms_selectionArgs, orderBy);
				if (c_sms != null) {
					if (c_sms.moveToFirst()) {
						current_sms_sent = c_sms.getCount();
					}
				}
				/*
				 * Log.i("i",
				 * "HomeLaunchActivity -- current month current_sms_sent:" +
				 * current_sms_sent + " 条"); Log.i("i",
				 * "HomeLaunchActivity -- current month total_sms_sent:" +
				 * total_sms_sent + " 条");
				 */
				if (current_sms_sent > total_sms_sent) {
					config.setTotalSmsSent(current_sms_sent);
				}
			} catch (Exception e) {
				Log.i("i", "init call sms count excption:" + e.getMessage());
			} finally {
				if (c_sms != null)
					c_sms.close();
			}
			Log.i("i", "HomeLaunchActivity ------ isSmsDataReady");
			// isCallsDataReady = true;
			watcher.sendEmptyMessageDelayed(TRY_TO_GO_TO_HOME, 1500);
			return null;
		}

	}
}