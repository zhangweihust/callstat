package com.archermind.callstat;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.app.AlarmManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import com.archermind.callstat.common.CallStatUtils;
import com.archermind.callstat.common.database.CallStatDatabase;
import com.archermind.callstat.common.net.MyHttpPostHelper;
import com.archermind.callstat.firewall.bean.TrafficDetail;
import com.archermind.callstat.service.CallStatSMSService;

public class CallStatReceiver extends BroadcastReceiver {
	public static final String AIR_ALARM = "air_alarm";

	public static boolean traffic_need_report = true;

	public static boolean wifi_state_connected = true;

	public static HashMap<Integer, String> accouting_infoMap = null;

	private CallStatApplication application;

	private ConfigManager configManager;

	private Context mContext;

	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			final Context mCtx = context;
			mContext = context;
			configManager = new ConfigManager(context);
			application = (CallStatApplication) context.getApplicationContext();

			// 发送查询话费短信之后收到广播消息
			if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
				ILog.LogI(CallStatReceiver.class, "收到开机广播");
				configManager.setBootComplete(true);
				if (!configManager.isFirstLaunch()
						&& configManager.isServicesStartOnBootComplete()) {
					CallStatUtils.startServices(context,
							CallStatUtils.callStatServices);

					new Thread(new Runnable() {

						@Override
						public void run() {
							CallStatApplication app = (CallStatApplication) mCtx
									.getApplicationContext();
							app.initTrafficDetail();
						}
					}).start();
					// ConfigManager configManager = new ConfigManager(context);
					// configManager.setTrafficNode("0,0,0,0,0,0");
				}
			} else if (intent.getAction().equals(Intent.ACTION_SHUTDOWN)) {
				if (CallStatUtils.isServiceRunning(context,
						"com.archermind.callstat.service.CallStatSMSService")) {
					Intent intentService = new Intent(context,
							CallStatSMSService.class);
					context.stopService(intentService);
				}
				// Log.i("my", "接收到关机广播");
			} else if (intent.getAction().equals(Intent.ACTION_TIME_CHANGED)) {
				if (configManager.isAirmodeSwitch()) {
					// Log.e("my", "打开飞行模式" );
					CallStatUtils.initAirAlarm(mCtx, application);
				}
				// Log.i("xxx", "接收系统时间改变广播");
			} else if (intent.getAction().equals(
					"android.intent.action.PACKAGE_ADDED")) {
				if (!configManager.isFirstLaunch()) {
					if (CallStatUtils
							.isServiceRunning(context,
									"com.archermind.callstat.service.CallStatSMSService")) {
						Intent intentService = new Intent(context,
								CallStatSMSService.class);
						intentService
								.setAction(CallStatSMSService.PACKAGEADD_ACTION);
						intentService.putExtra("package", intent
								.getDataString().substring(8));
						context.startService(intentService);
					}

				}

				// Log.i("my", "有软件被安装=" + intent.getDataString());

			} else if (intent.getAction().equals(
					"android.intent.action.PACKAGE_REMOVED")) {
				if (!configManager.isFirstLaunch()) {
					if (CallStatUtils
							.isServiceRunning(context,
									"com.archermind.callstat.service.CallStatSMSService")) {
						Intent intentService = new Intent(context,
								CallStatSMSService.class);
						intentService
								.setAction(CallStatSMSService.PACKAGEREMOVED_ACTION);
						intentService.putExtra("package", intent
								.getDataString().substring(8));
						context.startService(intentService);
					}

				}

				// Log.i("my", "有软件被卸载=" + intent.getDataString());
			} else if (intent.getAction().equals(
					"android.intent.action.SERVICE_STATE")) {
				if (!configManager.isFirstLaunch()) {
					if (CallStatUtils
							.isServiceRunning(context,
									"com.archermind.callstat.service.CallStatSMSService")) {
						Intent intentService = new Intent(context,
								CallStatSMSService.class);
						intentService
								.setAction(CallStatSMSService.SERVICE_STATE_CHANGE_ACTION);
						intentService.putExtras(intent.getExtras());
						context.startService(intentService);
					}
				}
			} else if (intent.getAction().equals(AIR_ALARM)) {
				AlarmManager am = (AlarmManager) context
						.getSystemService(Service.ALARM_SERVICE);
				// Log.e("callstats", "intent.getAction().equals(AIR_ALARM)");
				Calendar c = Calendar
						.getInstance(TimeZone.getTimeZone("GMT+8"));
				Calendar cNow = Calendar.getInstance(TimeZone
						.getTimeZone("GMT+8"));
				cNow.setTimeInMillis(System.currentTimeMillis());
				c.setTimeInMillis(System.currentTimeMillis());
				int mHour = cNow.get(Calendar.HOUR_OF_DAY);
				int mMinute = cNow.get(Calendar.MINUTE);
				if (mHour * 100 + mMinute == configManager
						.getAirmodeCloseTime()) {
					if (configManager.getAirmodeCloseTime() > configManager
							.getAirmodeOpenTime()) {
						// Log.i("callstats", "mHour*100+mMinute :"
						// + (mHour * 100 + mMinute)
						// + "configManager.getAirmodeCloseTime():"
						// + configManager.getAirmodeCloseTime());
						CallStatUtils.setAirplaneModeOn(context, false);
						c.add(Calendar.DAY_OF_MONTH, 1);
						c.set(Calendar.HOUR_OF_DAY,
								configManager.getAirmodeOpenTime() / 100);
						c.set(Calendar.MINUTE,
								configManager.getAirmodeOpenTime() % 100);
					} else {
						// Log.i("callstats", "mHour*100+mMinute :"
						// + (mHour * 100 + mMinute)
						// + "configManager.getAirmodeCloseTime():"
						// + configManager.getAirmodeCloseTime());
						CallStatUtils.setAirplaneModeOn(context, false);
						c.set(Calendar.HOUR_OF_DAY,
								configManager.getAirmodeOpenTime() / 100);
						c.set(Calendar.MINUTE,
								configManager.getAirmodeOpenTime() % 100);
					}

				} else {
					if (configManager.getAirmodeCloseTime() > configManager
							.getAirmodeOpenTime()) {
						// Log.i("callstats", "mHour*100+mMinute :"
						// + (mHour * 100 + mMinute)
						// + "configManager.getAirmodeCloseTime():"
						// + configManager.getAirmodeCloseTime());
						CallStatUtils.setAirplaneModeOn(context, true);
						c.set(Calendar.HOUR_OF_DAY,
								configManager.getAirmodeCloseTime() / 100);
						c.set(Calendar.MINUTE,
								configManager.getAirmodeCloseTime() % 100);
					} else {
						// Log.i("callstats", "mHour*100+mMinute :"
						// + (mHour * 100 + mMinute)
						// + "configManager.getAirmodeCloseTime():"
						// + configManager.getAirmodeCloseTime());
						CallStatUtils.setAirplaneModeOn(context, true);
						c.add(Calendar.DAY_OF_MONTH, 1);
						c.set(Calendar.HOUR_OF_DAY,
								configManager.getAirmodeCloseTime() / 100);
						c.set(Calendar.MINUTE,
								configManager.getAirmodeCloseTime() % 100);
					}

				}

				// get new alarm managerer am
				// Log.e("callstats", "in reciever time:"
				// + c.getTime().toLocaleString());
				am.set(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(),
						application.getAirModeIntent());

				// new intent to see what we are going to do next

			} else if (intent.getAction().equals(
					CallStatSMSService.CONNECTIVITY_CHANGE)) {
				ILog.LogE(this.getClass(), "CONNECTIVITY_CHANGE");
				if (CallStatApplication.isFirstTimeAppCreate) {
					CallStatApplication.isFirstTimeAppCreate = false;
					return;
				}
				ConnectivityManager connectivityManager = (ConnectivityManager) context
						.getSystemService(Context.CONNECTIVITY_SERVICE);

				NetworkInfo gprsInfo = connectivityManager
						.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

				NetworkInfo wifiInfo = connectivityManager
						.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

				if (gprsInfo.isConnected() && !wifiInfo.isConnected()) {
					if (wifi_state_connected) {
						ILog.LogE(this.getClass(),
								"CONNECTIVITY_CHANGE gprsInfo.isConnected()");
						application
								.refreshNewTrafficDetail(CallStatDatabase.TRAFFIC_DETAIL_TYPE_WIFI);
					}
					wifi_state_connected = false;
				} else if (!gprsInfo.isConnected() && wifiInfo.isConnected()) {
					if (!wifi_state_connected) {
						ILog.LogE(this.getClass(),
								"CONNECTIVITY_CHANGE wifiInfo.isConnected()");
						application
								.refreshNewTrafficDetail(CallStatDatabase.TRAFFIC_DETAIL_TYPE_GPRS);
					}
					wifi_state_connected = true;
				} else if (gprsInfo.isConnected() && wifiInfo.isConnected()) {
					if (wifi_state_connected) {
						application
								.refreshNewTrafficDetail(CallStatDatabase.TRAFFIC_DETAIL_TYPE_WIFI);
					} else {
						application
								.refreshNewTrafficDetail(CallStatDatabase.TRAFFIC_DETAIL_TYPE_GPRS);
					}
					wifi_state_connected = true;
				} else if (!gprsInfo.isConnected() && !wifiInfo.isConnected()) {
					if (wifi_state_connected) {
						application
								.refreshNewTrafficDetail(CallStatDatabase.TRAFFIC_DETAIL_TYPE_WIFI);
					} else {
						application
								.refreshNewTrafficDetail(CallStatDatabase.TRAFFIC_DETAIL_TYPE_GPRS);
					}
					wifi_state_connected = false;
				}
			} else if (intent.getAction().equals(
					"android.intent.action.MEDIA_MOUNTED")
					|| intent.getAction().equals(
							"android.intent.action.MEDIA_UNMOUNTED")) {
				CallStatDatabase.refreshDB(context);
			} else if (intent.getAction().equals(
					CallStatSMSService.UPLOAD_ACC_CODE_ACTION)) {
				String type = intent.getStringExtra("type");
				if (CallStatUtils.isNetworkAvailable(mContext)) {
					new UploadNewAccoutingCodeThread().execute(type);
				}
			}
		} catch (Exception e) { // end try
			ILog.logException(this.getClass(), e);
		}
	}

	class UploadNewAccoutingCodeThread extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... params) {
			String url = mContext.getString(R.string.upload_code_by_city_url);
			Map<String, String> map = new HashMap<String, String>();
			String province = configManager.getProvince();
			String city = configManager.getCity();
			String type = params[0];
			String operator = configManager.getOperator();
			String brand = configManager.getPackageBrand();
			String queryNum = configManager.getOperatorNum();
			String code = "";
			if (SmsReceiver.HF_TYPE.equals(type)) {
				String hfUsedCode = configManager.getHFUsedCode();
				String hfYeCode = configManager.getHFYeCode();
				if (hfYeCode.equals(hfUsedCode)) {
					code = hfYeCode;
				} else {
					code = hfUsedCode + ":" + hfYeCode;
				}
			} else if (SmsReceiver.LL_TYPE.equals(type)) {
				String gprsUsedCode = configManager.getGprsUsedCode();
				String gprsYeCode = configManager.getGprsYeCode();
				if (gprsYeCode.equals(gprsUsedCode)) {
					code = gprsUsedCode;
				} else {
					code = gprsUsedCode + ":" + gprsYeCode;
				}
			}
			map.put("province", province);
			map.put("city", city);
			map.put("type", type);
			map.put("operator", operator);
			map.put("brand", brand);
			map.put("queryNum", queryNum);
			map.put("code", code);

			HttpPost request = MyHttpPostHelper.getHttpPost(url);
			UrlEncodedFormEntity entity = MyHttpPostHelper
					.buildUrlEncodedFormEntity(map, HTTP.UTF_8);
			request.setEntity(entity);

			DefaultHttpClient httpClient = new DefaultHttpClient();
			try {
				HttpResponse response = httpClient.execute(request);

				if (response != null) {
					int resultCode = response.getStatusLine().getStatusCode();
					if (resultCode == 200) {
						String strResult = EntityUtils.toString(response
								.getEntity());
					} else {
						// TODO
					}
				}
			} catch (Exception e) {
				ILog.LogE(
						getClass(),
						"UploadNewAccoutingCodeThread failed,Exception:"
								+ e.getMessage());
				e.printStackTrace();
			}
			return null;
		}

	}

	public void replaceUidTrafficDetail(int uid, TrafficDetail td,
			Map<Integer, TrafficDetail> map) {
		if (map.get(uid) == null) {
			map.put(uid, td);
		} else {
			map.remove(uid);
			map.put(uid, td);
		}
	}

}