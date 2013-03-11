package com.archermind.callstat.common;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Process;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.archermind.callstat.BrowserActivity;
import com.archermind.callstat.CallStatApplication;
import com.archermind.callstat.CallStatReceiver;
import com.archermind.callstat.ConfigManager;
import com.archermind.callstat.ILog;
import com.archermind.callstat.R;
import com.archermind.callstat.common.DeviceInformation.InfoName;
import com.archermind.callstat.common.database.CallStatDatabase;
import com.archermind.callstat.common.net.MyHttpPostHelper;
import com.archermind.callstat.firewall.bean.NewTrafficDetail;
import com.archermind.callstat.home.bean.PhoneNumberInfo;
import com.archermind.callstat.monitor.bean.CallLog;
import com.archermind.callstat.monitor.bean.MonthlyStatDataSource;

/**
 * This class provides all the common methods that may be used for the whole
 * project.Static methods that only related to all components of CallStat should
 * be invoked directly by this class name.
 * 
 * @author lx
 */
public class CallStatUtils {

	static ConfigManager config = new ConfigManager(
			CallStatApplication.getCallstatsContext());

	public static ArrayList<Activity> activityList = new ArrayList<Activity>();

	public static final String[] callStatServices = { "com.archermind.callstat.service.CallStatSMSService" };

	public static final String SMS_SERVICE = "com.archermind.callstat.service.CallStatSMSService";

	public static final String PACKAGE_SET = "package_setting";

	public static final String MY_APP_PACKAGE = "com.archermind.callstat";

	public static HashMap<String, String> contact_info_has_map = new HashMap<String, String>();// 此hash表存放联系人号码和联系人姓名的一个映射关系

	private CallStatUtils() {
		// To forbidden instantiate this class.
	}

	/**
	 * Determine whether an application is installed.
	 * 
	 * @param packageName
	 *            the package name of the application.
	 * @return true if the application is installed, false otherwise.
	 */
	public static boolean isApplicationInstalled(final Context context,
			final String packageName) {
		if (TextUtils.isEmpty(packageName)) {
			return false;
		}
		if (null == context) {
			throw new IllegalArgumentException("context may not be null.");
		}
		try {
			context.getPackageManager().getPackageInfo(packageName, 0);
			return true;
		} catch (final NameNotFoundException e) {
			// Application not installed.
		}
		return false;
	}

	public static void appExit(Context context) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(context);
		final String appName = context.getString(R.string.app_name);
		builder.setTitle(appName);
		builder.setMessage(context.getString(R.string.exit_app_ensure, appName));
		builder.setPositiveButton(R.string.callstat_ok,
				mOnExitDialogClickListener);
		builder.setNegativeButton(R.string.callstat_cancel,
				mOnExitDialogClickListener);
		builder.show();
	}

	public static byte[] drawableToByteArr(Drawable drawable) {
		Bitmap bmp = (((BitmapDrawable) drawable).getBitmap());
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		bmp.compress(CompressFormat.PNG, 100, os);
		return os.toByteArray();
	}

	public static Drawable byteArrToDrawable(byte[] byteArr) {
		Bitmap bitmap = BitmapFactory.decodeByteArray(byteArr, 0,
				byteArr.length, null);
		BitmapDrawable bitmapDrawable = new BitmapDrawable(bitmap);
		return bitmapDrawable;
	}

	private final static DialogInterface.OnClickListener mOnExitDialogClickListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(final DialogInterface dialog, final int which) {
			switch (which) {
			case DialogInterface.BUTTON_POSITIVE:
				Process.killProcess(Process.myPid());
				break;
			case DialogInterface.BUTTON_NEGATIVE:
				dialog.dismiss();
				break;
			default:
				break;
			}
		}
	};

	public final static float DENSITY = Resources.getSystem()
			.getDisplayMetrics().density;
	public final static float DENSITY_PPI = Resources.getSystem()
			.getDisplayMetrics().densityDpi;

	public static int dipToPixel(int dip) {
		return (int) (dip * DENSITY + 0.5f);
	}

	public static float pixelToDip(float px) {
		return (px - 0.5f) / DENSITY;
	}

	public static void startDownload(Context context, String downloadUrl) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(downloadUrl));
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

	// public static boolean writeSharedPreferences(Context context, String
	// name,
	// int mode, String key, String msg) {
	// SharedPreferences.Editor edit = context
	// .getSharedPreferences(name, mode).edit();
	// edit.putString(key, msg);
	// return edit.commit();
	// }

	// public static String readSharedPreferences(Context context, String name,
	// int mode, String key) {
	// return context.getSharedPreferences(name, mode).getString(key, null);
	// }
	public static boolean isScreenLocked(Context c) {
		KeyguardManager mKeyguardManager = (KeyguardManager) c
				.getSystemService(Context.KEYGUARD_SERVICE);
		return !mKeyguardManager.inKeyguardRestrictedInputMode();
	}

	public static void openSystemBrowser(Context context, String url) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(url));
		intent.setClassName("com.android.browser",
				"com.android.browser.BrowserActivity");
		context.startActivity(intent);
	}

	public static void openURL(Context context, String url,String title) {
		/*if (StringUtil.isNullOrWhitespaces(title)) {
			title = context.getString(R.string.app_name);
		}*/
		Intent intent = new Intent(context,BrowserActivity.class);
		intent.putExtra("WAPURL", url);
		//intent.putExtra("WAPTITLE", title);
		context.startActivity(intent);
	}
	
	public static boolean isApplicationUpdatable(final Context context,
			final String packageName, final int versionCode) {
		if (TextUtils.isEmpty(packageName)) {
			return false;
		}
		if (null == context) {
			throw new IllegalArgumentException("context may not be null.");
		}

		try {
			final PackageInfo packageInfo = context.getPackageManager()
					.getPackageInfo(packageName, 0);
			if (packageInfo.versionCode < versionCode) {
				return true;
			}
		} catch (final NameNotFoundException e) {
			// Application not installed.
		}
		return false;
	}

	public static boolean isOMS() {
		if (getPropertyStream() != null) {
			return true;
		} else {
			return false;
		}
	}

	private static FileInputStream getPropertyStream() {
		try {
			File property = new java.io.File("/opl/etc/properties.xml");
			if (property.exists()) {
				return new FileInputStream(new java.io.File(
						"/opl/etc/properties.xml"));
			} else {
				property = new java.io.File("/opl/etc/product_properties.xml");
				if (property.exists()) {
					return new FileInputStream(new java.io.File(
							"/opl/etc/product_properties.xml"));
				} else {
					return null;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param context
	 * @return apn
	 */
	public static String getAPN(Context context) {
		Cursor cr = null;
		try {
			Uri uri = Uri.parse("content://telephony/carriers/preferapn");
			cr = context.getContentResolver()
					.query(uri, null, null, null, null);
			while (cr != null && cr.moveToNext()) {
				// String id = cr.getString(cr.getColumnIndex("_id"));
				// String apn = cr.getString(cr.getColumnIndex("apn"));
				String proxy = cr.getString(cr.getColumnIndex("proxy"));
				if (proxy.equals("10.0.0.172")) {
					return "CMWAP";
				} else {
					return "CMNET";
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		} finally {
			if (null != cr) {
				cr.close();
			}
		}
		return null;
	}

	// public static void sortByTraffic(ArrayList<TrafficDetail> list) {
	// Collections.sort(list, new Comparator<TrafficDetail>() {
	//
	// @Override
	// public int compare(TrafficDetail object1, TrafficDetail object2) {
	// // TODO Auto-generated method stub
	// float total1 = object1.getUpload() + object1.getDownload();
	// float total2 = object2.getUpload() + object2.getDownload();
	// return (int) (total2 - total1);
	// }
	//
	// });
	// }

	public static void sortByNewTraffic(ArrayList<NewTrafficDetail> list) {
		try {

			Collections.sort(list, new Comparator<NewTrafficDetail>() {

				@Override
				public int compare(NewTrafficDetail object1,
						NewTrafficDetail object2) {
					float total1 = object1.getGprsDownload()
							+ object1.getGprsUpload();
					float total2 = object2.getGprsDownload()
							+ object2.getGprsUpload();
					return (int) (total2 - total1);
				}
			});
		} catch (Exception e) {
			ILog.logException(CallStatUtils.class, e);
		}
	}

	public static Boolean isMOBILE(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cm.getActiveNetworkInfo();
		try {
			String typeName = info.getTypeName();
			if (typeName.toUpperCase().equals("MOBILE")) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}

	public static long getAccDayInMillis(ConfigManager config) {
		TimeZone tz = TimeZone.getTimeZone("GMT+8");
		Calendar cld = Calendar.getInstance(tz);

		int acc_day = config.getAccountingDay();
		int year = cld.get(Calendar.YEAR);
		int month = cld.get(Calendar.MONTH);
		int day = cld.get(Calendar.DAY_OF_MONTH);

		if (acc_day <= day) {
			cld.set(Calendar.YEAR, year);
			cld.set(Calendar.MONTH, month);
			cld.set(Calendar.DAY_OF_MONTH, acc_day);
			cld.set(Calendar.HOUR_OF_DAY, 0);
			cld.set(Calendar.MINUTE, 0);
			cld.set(Calendar.SECOND, 0);
		} else {
			int lastMonth = ((month - 1) > -1) ? month - 1 : 11;
			cld.set(Calendar.MONTH, lastMonth);
			cld.set(Calendar.DAY_OF_MONTH, acc_day);
			cld.set(Calendar.HOUR_OF_DAY, 0);
			cld.set(Calendar.MINUTE, 0);
			cld.set(Calendar.SECOND, 0);
		}

		return cld.getTimeInMillis();
	}

	public static long getFirstOfMonthInMillis() {
		TimeZone tz = TimeZone.getTimeZone("GMT+8");
		Calendar cld = Calendar.getInstance(tz);
		cld.set(Calendar.DAY_OF_MONTH, 1);
		cld.set(Calendar.HOUR_OF_DAY, 0);
		cld.set(Calendar.MINUTE, 0);
		cld.set(Calendar.SECOND, 0);
		return cld.getTimeInMillis();
	}

	public static long getNowToTomorrowMillis() {
		TimeZone tz = TimeZone.getTimeZone("GMT+8");
		Calendar cld = Calendar.getInstance(tz);
		int year = cld.get(Calendar.YEAR);
		int month = cld.get(Calendar.MONTH);
		int day = cld.get(Calendar.DAY_OF_MONTH);
		cld.set(year, month, day + 1, 0, 0, 0);
		return cld.getTimeInMillis();
	}

	public static boolean isIntentAvailable(Context context, Intent intent) {
		final PackageManager packageManager = context.getPackageManager();
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
				PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

	public static boolean isDatabaseFileExist(String path) {
		File file = new File(path);
		return file.exists();
	}

	public static boolean isNetworkAvailable(Context mContext) {
		boolean isAvailable = false;
		final ConnectivityManager cm = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (null != cm) {
			final NetworkInfo[] netinfo = cm.getAllNetworkInfo();
			if (null != netinfo) {
				for (int i = 0; i < netinfo.length; i++) {
					if (netinfo[i].isConnected()) {
						isAvailable = true;
					}
				}
			}
		}
		return isAvailable;
	}

	/**
	 * 此服务是否有在运行
	 * 
	 * @param mContext
	 *            传进context对象
	 * @param className
	 *            服务名称
	 * @return true运行,false没在运行
	 */
	public static boolean isServiceRunning(Context mContext, String className) {
		boolean isRunning = false;
		ActivityManager activityManager = (ActivityManager) mContext
				.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningServiceInfo> serviceList = activityManager
				.getRunningServices(30);
		if (!(serviceList.size() > 0)) {
			return false;
		}
		for (int i = 0; i < serviceList.size(); i++) {
			if (serviceList.get(i).service.getClassName().equals(className) == true) {
				isRunning = true;
				break;
			}
		}
		return isRunning;
	}

	public static String[][] getString2Arr(String str) {
		if (!str.contains(":")) {
			return null;
		}
		String[] temp = str.split(":");
		if (temp.length == 0) {
			return null;
		}
		String[][] strArr = new String[temp.length][];
		for (int i = 0; i < temp.length; i++) {
			if (!temp[i].contains(";")) {
				return null;
			}
			strArr[i] = temp[i].split(";");
		}
		return strArr;
	}

	/**
	 * 判定此应用程序是否含有此权限
	 * 
	 * @param context
	 *            传进context对象
	 * @param appInfo
	 *            此应用的appInfo对象
	 * @param permission
	 *            权限名称
	 * @return true有，false没有
	 */
	public static boolean hasPermission5App(Context context,
			ApplicationInfo appInfo, String permission) {
		PackageManager packageManager = context.getPackageManager();
		int reslut = packageManager.checkPermission(permission,
				appInfo.packageName);
		if (reslut == PackageManager.PERMISSION_GRANTED) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean hasSMSpermission(PackageManager packageManager,
			ApplicationInfo appInfo) {
		return (packageManager.checkPermission(
				"android.permission.RECEIVE_SMS", appInfo.packageName) == PackageManager.PERMISSION_GRANTED) ? true
				: false;
	}

	public static void startServiceByName(Context context, String serviceName) {
		if (!isServiceRunning(context, serviceName)) {
			Class<?> service;
			// Log.i("my", "ServiceName=" + serviceName);
			try {
				// if ("com.archermind.callstat.service.CallStatSMSService"
				// .equals(serviceName)) {
				// networkListener(context);
				// } else {
				service = Class.forName(serviceName);
				Intent intent = new Intent(context, service);
				context.startService(intent);
				// }

			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		} else {
			// Log.i("i",
			// "com.archermind.callstat.service.CallStatSMSService is already running....");
		}
	}

	public static void startServices(Context mContext, String[] serviceNames) {
		if (serviceNames.length <= 0) {
			return;
		}
		for (String serviceName : serviceNames) {
			ActivityManager activityManager = (ActivityManager) mContext
					.getSystemService(Context.ACTIVITY_SERVICE);
			List<ActivityManager.RunningServiceInfo> serviceList = activityManager
					.getRunningServices(300);
			if (!(serviceList.size() > 0)) {
				Class<?> service;
				// Log.i("my", "ServiceName=" + serviceName);
				try {
					// if ("com.archermind.callstat.service.CallStatSMSService"
					// .equals(serviceName)) {
					// networkListener(mContext);
					// } else {
					service = Class.forName(serviceName);
					Intent intent = new Intent(mContext, service);
					mContext.startService(intent);
					// }

				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			} else {
				boolean isRunning = false;
				for (int i = 0; i < serviceList.size(); i++) {
					if (serviceList.get(i).service.getClassName().equals(
							serviceName) == true) {
						isRunning = true;
						break;
					}
				}
				if (!isRunning) {
					Class<?> service;
					try {
						// if
						// ("com.archermind.callstat.service.CallStatSMSService"
						// .equals(serviceName)) {
						// networkListener(mContext);
						// } else {
						service = Class.forName(serviceName);
						Intent intent = new Intent(mContext, service);
						mContext.startService(intent);
						// }
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public static void stopServices(Context mContext, String[] serviceNames) {
		if (serviceNames.length <= 0) {
			return;
		}
		for (String serviceName : serviceNames) {
			ActivityManager activityManager = (ActivityManager) mContext
					.getSystemService(Context.ACTIVITY_SERVICE);
			List<ActivityManager.RunningServiceInfo> serviceList = activityManager
					.getRunningServices(300);
			if (serviceList.size() > 0) {
				for (int i = 0; i < serviceList.size(); i++) {
					if (serviceList.get(i).service.getClassName().equals(
							serviceName) == true) {
						Class<?> service;
						try {
							service = Class.forName(serviceName);
							Intent intent = new Intent(mContext, service);
							mContext.stopService(intent);
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}
						break;
					}
				}
			}
		}
	}

	/**
	 * 获取当前日期
	 * 
	 * @return 格式20120101
	 */
	public static String getNowDate() {
		final Calendar c = Calendar.getInstance();
		int mYear = c.get(Calendar.YEAR); // 获取当前年份
		int mMonth = c.get(Calendar.MONTH) + 1;// 获取当前月份
		int mDay = c.get(Calendar.DAY_OF_MONTH);// 获取当前月份的日期号码
		return String.valueOf(mYear)
				+ String.valueOf((mMonth / 10 > 0 ? mMonth : ("0" + mMonth)))
				+ String.valueOf((mDay / 10 > 0 ? mDay : ("0" + mDay)));
	}

	/**
	 * 获取前天日期
	 * 
	 * @return 格式20120101
	 */
	public static String getBeforeYesterday() {
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
		c.add(Calendar.DATE, -2);
		int mYear = c.get(Calendar.YEAR);
		int mMonth = c.get(Calendar.MONTH) + 1;
		int mDay = c.get(Calendar.DAY_OF_MONTH);
		return String.valueOf(mYear)
				+ String.valueOf((mMonth / 10 > 0 ? mMonth : ("0" + mMonth)))
				+ String.valueOf((mDay / 10 > 0 ? mDay : ("0" + mDay)));
	}

	/**
	 * 获取昨天日期
	 * 
	 * @return 格式20120101
	 */
	public static String getYesterday() {
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
		c.add(Calendar.DATE, -1);
		int mYear = c.get(Calendar.YEAR);
		int mMonth = c.get(Calendar.MONTH) + 1;
		int mDay = c.get(Calendar.DAY_OF_MONTH);
		return String.valueOf(mYear)
				+ String.valueOf((mMonth / 10 > 0 ? mMonth : ("0" + mMonth)))
				+ String.valueOf((mDay / 10 > 0 ? mDay : ("0" + mDay)));
	}

	/**
	 * 获取本月的第一天时间
	 * 
	 * @return 格式20120101
	 */
	public static String getFirstMonthDayDate() {
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
		int day = c.get(Calendar.DATE);
		c.add(Calendar.DATE, -(day - 1));
		int mYear = c.get(Calendar.YEAR);
		int mMonth = c.get(Calendar.MONTH) + 1;
		int mDay = c.get(Calendar.DAY_OF_MONTH);

		return String.valueOf(mYear)
				+ String.valueOf((mMonth / 10 > 0 ? mMonth : ("0" + mMonth)))
				+ String.valueOf((mDay / 10 > 0 ? mDay : ("0" + mDay)));
	}

	/**
	 * 获取上一个月的最后一天时间
	 * 
	 * @return 格式20120101
	 */
	public static String getLastMonthDayDate() {
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
		int day = c.get(Calendar.DATE);
		c.add(Calendar.DATE, -day);
		int mYear = c.get(Calendar.YEAR);
		int mMonth = c.get(Calendar.MONTH) + 1;
		int mDay = c.get(Calendar.DAY_OF_MONTH);

		return String.valueOf(mYear)
				+ String.valueOf((mMonth / 10 > 0 ? mMonth : ("0" + mMonth)))
				+ String.valueOf((mDay / 10 > 0 ? mDay : ("0" + mDay)));
	}

	/**
	 * 获取当前时间
	 * 
	 * @return 格式201201010101
	 */
	public static String getNowTime() {
		final Calendar c = Calendar.getInstance();
		int mYear = c.get(Calendar.YEAR); // 获取当前年份
		int mMonth = c.get(Calendar.MONTH) + 1;// 获取当前月份
		int mDay = c.get(Calendar.DAY_OF_MONTH);// 获取当前月份的日期号码
		int mHour = c.get(Calendar.HOUR_OF_DAY);
		int mMinute = c.get(Calendar.MINUTE);
		return String.valueOf(mYear)
				+ String.valueOf((mMonth / 10 > 0 ? mMonth : ("0" + mMonth)))
				+ String.valueOf((mDay / 10 > 0 ? mDay : ("0" + mDay)))
				+ String.valueOf((mHour / 10 > 0 ? mHour : ("0" + mHour)))
				+ String.valueOf((mMinute / 10 > 0 ? mMinute : ("0" + mMinute)));
	}

	public static String getNowMonth() {
		String nowDate = getNowDate();
		return nowDate.substring(0, nowDate.length() - 2);
	}

	public static int getNowDateDate() {
		String nowDate = getNowDate();
		return Integer.parseInt(nowDate.substring(nowDate.length() - 2));
	}

	// public static void networkListener(Context context) {
	// Intent intent = new Intent(context, CallStatSMSService.class);
	// context.startService(intent);
	// // ConnectivityManager connectivityManager = (ConnectivityManager)
	// // context
	// // .getSystemService(Context.CONNECTIVITY_SERVICE);
	// // NetworkInfo activeNetInfo =
	// // connectivityManager.getActiveNetworkInfo();
	// // if (activeNetInfo != null) {
	// // Log.i("Active Network Type", activeNetInfo.toString());
	// // switch (activeNetInfo.getType()) {
	// // case ConnectivityManager.TYPE_WIFI:
	// // if (activeNetInfo.isConnected()) {
	// // Intent intentNetwork = new Intent(context,
	// // CallStatSMSService.class);
	// // intentNetwork
	// // .setAction(CallStatSMSService.NETWORK_STATE_CHANGE_ACTION);
	// // intentNetwork.putExtra("NetWrokState",
	// // CallStatSMSService.WIFI_STATE);
	// // context.startService(intentNetwork);
	// // }
	// // break;
	// // case ConnectivityManager.TYPE_MOBILE:
	// // if (activeNetInfo.isConnected()) {
	// // Intent intentNetwork = new Intent(context,
	// // CallStatSMSService.class);
	// // intentNetwork
	// // .setAction(CallStatSMSService.NETWORK_STATE_CHANGE_ACTION);
	// // intentNetwork.putExtra("NetWrokState",
	// // CallStatSMSService.GPRS_STATE);
	// // context.startService(intentNetwork);
	// // }
	// // break;
	// // }
	// // } else {
	// // Intent intentNetwork = new Intent(context, CallStatSMSService.class);
	// // intentNetwork
	// // .setAction(CallStatSMSService.NETWORK_STATE_CHANGE_ACTION);
	// // intentNetwork.putExtra("NetWrokState",
	// // CallStatSMSService.CLOSE_STATE);
	// // context.startService(intentNetwork);
	// // }
	// }

	public static Object[] getProvinceAndCityFromTelNum(String[] provinces,
			String area) {
		try {
			if (area != null) {
				String prov = "";
				String[] cities = null;
				String prov_temp = area.substring(0, 2);
				String city_temp = "";
				if (prov_temp != null && prov_temp.length() == 2) {
					int len = provinces.length;
					boolean isTwoChars = false;
					for (int i = 0; i < len; i++) {
						if (prov_temp.equals(provinces[i])) {
							prov = prov_temp;
							isTwoChars = true;
							city_temp = area.substring(2);
							if (city_temp.contains("、")) {
								cities = city_temp.split("、");
							} else {
								cities = new String[] { city_temp };
							}
							return new Object[] { prov, cities };
						}
					}

					if (!isTwoChars) {
						prov_temp = area.substring(0, 3);
						if (prov_temp != null && prov_temp.length() == 3) {
							String[] three_chars_provinces = new String[] {
									"黑龙江", "内蒙古" };
							int _len = 2;
							for (int i = 0; i < _len; i++) {
								if (prov_temp.equals(three_chars_provinces[i])) {
									prov = prov_temp;
									isTwoChars = true;
									city_temp = area.substring(3);
									if (city_temp.contains("、")) {
										cities = city_temp.split("、");
									} else {
										cities = new String[] { city_temp };
									}
									return new Object[] { prov, cities };
								}
							}
						} else {
							return null;
						}
					}
				} else {
					return null;
				}
			}
		} catch (Exception e) {
			ILog.logException(CallStatUtils.class, e);
		}
		return null;
	}

	public static boolean isRoaming(Context context) {
		return ((TelephonyManager) context
				.getSystemService(Service.TELEPHONY_SERVICE))
				.isNetworkRoaming();
	}

	public static int getPhoneNumberType(CallStatDatabase db,
			ConfigManager config, String number) {
		// Random random = new Random();
		// return (random.nextInt() >>> 1 % 5 - 1);
		if (number.indexOf("-") != -1) {
			StringBuffer strBuffer = new StringBuffer();
			strBuffer.append(number.substring(0, number.indexOf("-")));
			strBuffer.append(number.substring(number.indexOf("-") + 1));
		}
		int len = number.length();
		// phone or mobile
		ILog.LogI(CallStatUtils.class, "outgoing type and number:" + number);
		if (len >= 11) {
			return getNumTypeIfIsMobile(db, config, number);
		}

		if (len == 10) {
			if (number.startsWith("400")) {
				return CallLog.CALL_LOCAL;
			} else {
				return CallLog.CALL_UNKONW;
			}
		}
		// local phone
		if (len == 7 || len == 8) {
			return CallLog.CALL_LOCAL;
		}
		if (len == 5
				&& (number.startsWith("95") || number.startsWith("11") || number
						.startsWith("12"))) {
			return CallLog.CALL_LOCAL;
		}
		if (len <= 6 && len >= 3) {
			return CallLog.CALL_SHORT;
		}

		return CallLog.CALL_UNKONW;
	}

	/**
	 * 获取自上次对账到现在所消费的2G/3G流量
	 * 
	 * @param configManager
	 *            configManager对象
	 * @return 流量值
	 */
	public static long getPrevReconcilitionToNowGprsUsed(
			ConfigManager configManager) {
		if (configManager.getFreeGprs() == 100000) {
			return configManager.getTotalGprsUsed()
					- configManager.getPrevReconcilitionGprsUsed();
		}
		if ((configManager.getTotalGprsUsed() + configManager
				.getTotalGprsUsedDifference()) <= (configManager.getFreeGprs() * 1024 * 1024)) {
			return 0;
		} else {
			return configManager.getTotalGprsUsed()
					- configManager.getPrevReconcilitionGprsUsed();
		}
	}

	/**
	 * 获取自上次对账到现在所消费短信
	 * 
	 * @param configManager
	 *            configManager对象
	 * @return 短信条数
	 */
	public static int getPrevReconcilitaionToNowSmsUsed(
			ConfigManager configManger) {
		if (configManger.getFreeMessages() == 100000) {
			return configManger.getTotalSmsSent()
					- configManger.getPrevReconcilitionSendSms();
		}
		if (configManger.getTotalSmsSent() <= configManger.getFreeMessages()) {
			return 0;
		} else {
			return configManger.getTotalSmsSent()
					- configManger.getPrevReconcilitionSendSms();
		}
	}

	public static boolean isFreeCall(Context ctx, String num) {

		String[] freeNums = ctx.getResources().getStringArray(
				R.array.free_numbers);

		boolean isFree = false;

		for (String free_num : freeNums) {
			if (free_num.equals(num)) {
				isFree = true;
				break;
			}

		}

		return isFree;
	}

	// mobile or phone
	private static int getNumTypeIfIsMobile(CallStatDatabase db,
			ConfigManager config, String number) {
		PhoneNumberInfo info = db.getPhoneNumberInfo5All(number);

		if (info == null) {
			ILog.LogI(CallStatUtils.class, "info == null");
			return CallLog.CALL_LONG_DISTANCE;
		}
		if (info.getBIP()) {
			return CallLog.CALL_IP;
		}
		String localcity = config.getCity();
		String numcity = info.getCity();
		if (numcity != null && localcity != null) {
			if (numcity.equals(localcity)) {
				ILog.LogI(CallStatUtils.class, "numcity:" + numcity
						+ " localcity:" + localcity);
				return CallLog.CALL_LOCAL;
			} else {
				ILog.LogI(CallStatUtils.class, "numcity:" + numcity
						+ " localcity:" + localcity);
				return CallLog.CALL_LONG_DISTANCE;
			}
		} else {
			return CallLog.CALL_UNKONW;
		}
	}

	/* 流量单位转换 */
	public static String[] traffic_unit(long traffic) {
		String[] afterchange = new String[2];
		try {
			if (traffic > 0) {
				if (traffic < 1024) {
					afterchange[0] = traffic + "";
					afterchange[1] = "B";
				} else {
					if (traffic < (1024 * 1024)) {
						afterchange[0] = (float) Math
								.round(traffic / 1024f * 100) / 100 + "";
						afterchange[1] = "K";
					} else {
						if (traffic < (1024l * 1024l * 1024l)) {
							afterchange[0] = (float) Math
									.round(traffic / 1024f / 1024f * 100)
									/ 100
									+ "";
							afterchange[1] = "M";
						} else {
							if (traffic < (1024l * 1024l * 1024l * 1024l)) {
								afterchange[0] = (float) Math.round(traffic
										/ 1024f / 1024f / 1024f * 100)
										/ 100 + "";
								afterchange[1] = "G";
							} else {
								afterchange[0] = (float) Math.round(traffic
										/ 1024f / 1024f / 1024f / 1024f * 100)
										/ 100 + "";
								afterchange[1] = "T";
							}
						}
					}
				}
			} else {
				afterchange[0] = "0";
				afterchange[1] = "B";
			}
		} catch (Exception e) {
			ILog.logException(CallStatUtils.class, e);
		}
		return afterchange;
	}

	/* 流量单位转换 */
	public static String[] traffic_unit_floatview(long traffic) {
		String[] afterchange = new String[2];
		try {
			if (traffic > 0) {
				if (traffic < 1024) {
					afterchange[0] = "0.00";
					afterchange[1] = "M";
				} else {
					if (traffic < (1024 * 1024)) {
						afterchange[0] = round(traffic / 1024f / 1024f, 2) + "";
						String[] string = afterchange[0].split("\\.");
						if (string[1].length() == 1) {
							afterchange[0] = afterchange[0] + "0";
						}
						afterchange[1] = "M";
					} else {
						if (traffic < (1024l * 1024l * 1024l)) {
							afterchange[0] = round(traffic / 1024f / 1024f, 2)
									+ "";
							String[] string = afterchange[0].split("\\.");
							if (string[1].length() == 1) {
								afterchange[0] = afterchange[0] + "0";
							}
							afterchange[1] = "M";
						} else {
							if (traffic < (1024l * 1024l * 1024l * 1024l)) {
								afterchange[0] = round(
										traffic / 1024f / 1024f / 1024f, 2)
										+ "";
								String[] string = afterchange[0].split("\\.");
								if (string[1].length() == 1) {
									afterchange[0] = afterchange[0] + "0";
								}
								afterchange[1] = "G";
							} else {
								afterchange[0] = (float) Math.round(traffic
										/ 1024f / 1024f / 1024f / 1024f * 100)
										/ 100 + "";
								afterchange[1] = "T";
							}
						}
					}
				}
			} else {
				afterchange[0] = "0";
				afterchange[1] = "B";
			}
		} catch (Exception e) {
			ILog.logException(CallStatUtils.class, e);
		}
		return afterchange;
	}

	public static String[] traffic_unit1(long traffic) {
		String[] afterchange = new String[2];
		try {
			if (traffic > 0) {
				if (traffic < 1024) {
					afterchange[0] = traffic + "";
					afterchange[1] = "B";
				} else {
					if (traffic < (1024 * 1024)) {
						afterchange[0] = (float) Math
								.round(traffic / 1024f * 10) / 10 + "";
						afterchange[1] = "K";
					} else {
						if (traffic < (1024l * 1024l * 1024l)) {
							afterchange[0] = (float) Math
									.round(traffic / 1024f / 1024f * 100)
									/ 100
									+ "";
							afterchange[1] = "M";
						} else {
							if (traffic < (1024l * 1024l * 1024l * 1024l)) {
								afterchange[0] = (float) Math.round(traffic
										/ 1024f / 1024f / 1024f * 100)
										/ 100 + "";
								afterchange[1] = "G";
							} else {
								afterchange[0] = (float) Math.round(traffic
										/ 1024f / 1024f / 1024f / 1024f * 100)
										/ 100 + "";
								afterchange[1] = "T";
							}
						}
					}
				}
			} else {
				afterchange[0] = "0";
				afterchange[1] = "B";
			}
		} catch (Exception e) {
			ILog.logException(CallStatUtils.class, e);
		}
		return afterchange;
	}

	public static String[] traffic_unit2(long traffic) {
		String[] afterchange = new String[2];
		try {
			if (traffic > 0) {
				if (traffic < 1024) {
					afterchange[0] = "0";
					afterchange[1] = "K";
				} else {
					if (traffic < (1024 * 1024)) {
						afterchange[0] = (int) Math.round(traffic / 1024f) + "";
						afterchange[1] = "K";
					} else {
						if (traffic < (1024l * 1024l * 1024l)) {
							afterchange[0] = changeFloat(traffic / 1024f / 1024f);
							afterchange[1] = "M";
						} else {
							if (traffic < (1024l * 1024l * 1024l * 1024l)) {
								afterchange[0] = changeFloat(traffic / 1024f / 1024f / 1024f);
								afterchange[1] = "G";
							} else {
								afterchange[0] = changeFloat(traffic / 1024f
										/ 1024f / 1024f / 1024f);
								afterchange[1] = "T";
							}
						}
					}
				}
			} else {
				afterchange[0] = "0";
				afterchange[1] = "K";
			}

		} catch (Exception e) {
			ILog.logException(CallStatUtils.class, e);
		}
		return afterchange;
	}

	public static String removeString(String s) {
		StringTokenizer st = new StringTokenizer(s, "中国", false);
		String t = "";
		while (st.hasMoreElements()) {
			t += st.nextElement();
		}
		return t;
	}

	public static float getSpingDirection(float[] coordinates) {
		float x1 = coordinates[0];
		float y1 = coordinates[1];

		float x2 = coordinates[2];
		float y2 = coordinates[3];

		float x3 = coordinates[4];
		float y3 = coordinates[5];

		return (x2 - x1) * (y3 - y2) - (y2 - y1) * (x3 - x2);
	}

	public static boolean isFlyingMode(Context mCtx) {
		return (Settings.System.getInt(mCtx.getContentResolver(),
				Settings.System.AIRPLANE_MODE_ON, 0) == 1) ? true : false;
	}

	/**
	 * 切换手机飞行模式开关
	 * 
	 * @param mContext
	 *            context对象
	 * @param enabling
	 *            true打开飞行模式，false关闭飞行模式
	 */
	public static void setAirplaneModeOn(Context mContext, boolean enabling) {
		// Change the system setting
		Settings.System.putInt(mContext.getContentResolver(),
				Settings.System.AIRPLANE_MODE_ON, enabling ? 1 : 0);
		// Post the intent
		Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
		intent.putExtra("state", enabling);
		mContext.sendBroadcast(intent);
	}

	public static void initAirAlarm(Context context, CallStatApplication app) {
		try {
			AlarmManager am = (AlarmManager) context
					.getSystemService(Service.ALARM_SERVICE);
			PendingIntent pi = app.getAirModeIntent();
			if (pi != null) {
				am.cancel(pi);
			}
			Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
			Calendar cNow = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
			int now_hour = cNow.get(Calendar.HOUR_OF_DAY);
			int now_minute = cNow.get(Calendar.MINUTE);
			// no matter what happens,we have to cancel the old pending intent
			// and
			// set a new one
			// and so does it in the system setting activity,we need to cancel
			// the
			// old pending intent there
			Intent intent = new Intent(context, CallStatReceiver.class);
			intent.setAction(CallStatReceiver.AIR_ALARM);

			app.setAirModeIntent(PendingIntent.getBroadcast(context, 0, intent,
					PendingIntent.FLAG_UPDATE_CURRENT));
			ConfigManager config = new ConfigManager(context);
			// set time
			// this is exactly where we process delay time

			if (config.getAirmodeCloseTime() < config.getAirmodeOpenTime()) {
				if (now_hour * 100 + now_minute < config.getAirmodeCloseTime()
						|| now_hour * 100 + now_minute > config
								.getAirmodeOpenTime()) {
					CallStatUtils.setAirplaneModeOn(context, true);
					// Log.e("my", "打开飞行模式");
					if (now_hour * 100 + now_minute > config
							.getAirmodeOpenTime()) {
						c.add(Calendar.DAY_OF_MONTH, 1);
					}
					c.set(Calendar.HOUR_OF_DAY,
							config.getAirmodeCloseTime() / 100);
					c.set(Calendar.MINUTE, config.getAirmodeCloseTime() % 100);
					// Log.e("myHandler", "time:" +
					// config.getAirmodeCloseTime());
				} else {
					CallStatUtils.setAirplaneModeOn(context, false);
					c.set(Calendar.HOUR_OF_DAY,
							config.getAirmodeOpenTime() / 100);
					c.set(Calendar.MINUTE, config.getAirmodeOpenTime() % 100);
					// Log.e("myHandler", "time:" +
					// config.getAirmodeOpenTime());
				}
			} else {
				if (now_hour * 100 + now_minute < config.getAirmodeOpenTime()
						|| now_hour * 100 + now_minute > config
								.getAirmodeCloseTime()) {
					if (now_hour * 100 + now_minute > config
							.getAirmodeCloseTime()) {
						c.add(Calendar.DAY_OF_MONTH, 1);
					}
					CallStatUtils.setAirplaneModeOn(context, false);
					c.set(Calendar.HOUR_OF_DAY,
							config.getAirmodeOpenTime() / 100);
					c.set(Calendar.MINUTE, config.getAirmodeOpenTime() % 100);
					// Log.e("myHandler", "time:" +
					// config.getAirmodeOpenTime());
				} else {
					CallStatUtils.setAirplaneModeOn(context, true);
					c.set(Calendar.HOUR_OF_DAY,
							config.getAirmodeCloseTime() / 100);
					c.set(Calendar.MINUTE, config.getAirmodeCloseTime() % 100);
					// Log.e("myHandler", "time:" +
					// config.getAirmodeCloseTime());
				}
			}

			// Log.e("myHandler", "time:" + c.getTime().toGMTString());
			am.set(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(),
					app.getAirModeIntent());
		} catch (Exception e) {
			ILog.logException(CallStatUtils.class, e);
		}

	}

	/* 浮点形数据的小数点后都为0时，去掉小数点及小数点后面的0 */
	public static String changeFloat(float f) {
		java.text.DecimalFormat df = new java.text.DecimalFormat("#.##");
		return df.format(f);
	}

	/* 悬浮窗严格的小数点要求 */
	public static String changeFloat_float(float f) {
		String s = round(f, 2) + "";
		String[] string = s.split("\\.");
		if (string[1].length() == 1) {
			s = s + "0";
		}

		if (s.length() > 7) {
			java.text.DecimalFormat df = new java.text.DecimalFormat("#.#");
			s = df.format(f);
		}

		return s;

	}

	// added by zhangjing
	/**
	 * 取得当月天数
	 * */
	public static int getCurrentMonthLastDay() {
		Calendar a = Calendar.getInstance();
		a.set(Calendar.DATE, 1);// 把日期设置为当月第一天
		a.roll(Calendar.DATE, -1);// 日期回滚一天，也就是最后一天
		int maxDate = a.get(Calendar.DATE);
		return maxDate;
	}

	public static int get_height(Context context) {

		int height = (int) Math
				.round(getSize_inch(context) / DENSITY_PPI * 240) + 163;
		if (height < 163) {
			height = 163;
		}
		return (int) Math.round(height * DENSITY);

	}

	public static float getSize_inch(Context context) {
		// float width = DeviceUtils.getDeviceScreenWidth(context);
		float height = DeviceUtils.getDeviceScreenHeight(context);
		float size_inch = (float) (height / DENSITY) - 533;
		return size_inch;

	}

	public static boolean isOnDesk(Context context, int id) {
		ActivityManager activityManager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningTaskInfo> tasks = activityManager.getRunningTasks(1);
		String src = tasks.get(0).baseActivity.getClassName();

		String[] targets = context.getResources().getStringArray(id);
		for (String target : targets) {
			if (src.contains(target)) {
				return true;
			}
		}
		return false;
	}

	/* 处理网速 单位为： 0.00K/s 或 0.00M/s */
	public static String[] wireSpeed_unit(double speed) {
		String[] strings = new String[2];
		try {
			if (speed >= 0) {
				if (speed / 1024f / 1024f >= 1) {
					strings[0] = round(speed / 1024f / 1024f, 2) + "";
					String[] oldString = strings[0].split("\\.");
					if (oldString[1].length() == 1) {
						strings[0] = strings[0] + "0";
					}
					strings[1] = "M/s";
				} else if (speed / 1024f >= 1) {
					strings[0] = round(speed / 1024f, 2) + "";
					String[] oldString = strings[0].split("\\.");

					if (oldString[0].length() == 4) {
						strings[0] = round(speed / 1024f / 1024f, 2) + "";
						strings[1] = "M/s";
					} else {
						strings[1] = "K/s";
					}
					if (oldString[1].length() == 1) {
						strings[0] = strings[0] + "0";
					}

				} else {
					strings[0] = round(speed / 1024f, 2) + "";
					String[] oldString = strings[0].split("\\.");
					if (oldString[1].length() == 1) {
						strings[0] = strings[0] + "0";
					}
					strings[1] = "K/s";
				}
			} else {
				strings[0] = "0.00";
				strings[1] = "K/s";
			}

		} catch (Exception e) {
			ILog.logException(CallStatUtils.class, e);
		}
		return strings;

	}

	/**
	 * 提供精确的小数位四舍五入处理。
	 * 
	 * @param v
	 *            需要四舍五入的数字
	 * @param scale
	 *            小数点后保留几位
	 * @return 四舍五入后的结果
	 */
	public static double round(double v, int scale) {
		if (scale < 0) {
			throw new IllegalArgumentException(
					"The scale must be a positive integer or zero");
		}
		BigDecimal b = new BigDecimal(Double.toString(v));
		BigDecimal one = new BigDecimal("1");
		return b.divide(one, scale, BigDecimal.ROUND_HALF_UP).doubleValue();
	}

	// 将毫秒数转换成年月日时分的格式 格式201201010101
	public static String changeMilliSeconds2YearMonthDayHourMin(long mseconds) {
		Date date = new Date(mseconds);
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmm");
		return df.format(date);
	}

	public static String getTimeStringFromMilliSec(long ms) {
		Date date = new Date(ms);
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return df.format(date);
	}

	public static String getDateStringFromMilliSec(long ms) {
		Date date = new Date(ms);
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		return df.format(date);
	}

	/**
	 * 提供改变对帐delta区间的端点。
	 * */
	public static void changeTheSectionOfDelta() {
		try {
			String YearMonthDay_HourMinute = CallStatUtils.getNowTime();
			String HourMinute = YearMonthDay_HourMinute.substring(8,
					YearMonthDay_HourMinute.length());

			int startHour = Integer.parseInt(HourMinute.substring(0, 2));
			int minute = Integer.parseInt(HourMinute.substring(2,
					HourMinute.length()));
			ILog.LogE(CallStatUtils.class, "startHour is " + startHour
					+ "	Minute is " + minute);
			DecimalFormat df = new DecimalFormat("00");
			String delta1 = df.format(startHour % 24) + df.format(minute);
			String delta2 = df.format((startHour + 4) % 24) + df.format(minute);
			String delta3 = df.format((startHour + 8) % 24) + df.format(minute);
			String delta4 = df.format((startHour + 12) % 24)
					+ df.format(minute);
			String delta5 = df.format((startHour + 16) % 24)
					+ df.format(minute);
			String delta6 = df.format((startHour + 20) % 24)
					+ df.format(minute);

			CallStatApplication.time_section_append_delta[0] = delta1;
			CallStatApplication.time_section_append_delta[1] = delta2;
			CallStatApplication.time_section_append_delta[2] = delta3;
			CallStatApplication.time_section_append_delta[3] = delta4;
			CallStatApplication.time_section_append_delta[4] = delta5;
			CallStatApplication.time_section_append_delta[5] = delta6;
		} catch (Exception e) {
			ILog.logException(CallStatUtils.class, e);
		}

	}

	/**
	 * 判断给定的对帐时刻（时分）处于哪个区间，即需要加delta几。
	 * 
	 * @param String
	 *            对帐成功的时刻（时分格式，小时和分钟均采取两位数表示 如1800，表示18：00）
	 * @return 需要加delta几的index index = 0，1，2，3，4，5
	 */
	public static int which_delta_begin_to_add(String hour_minute) {
		try {
			String delta1, delta2, delta3, delta4, delta5, delta6;
			delta1 = CallStatApplication.time_section_append_delta[0];
			delta2 = CallStatApplication.time_section_append_delta[1];
			delta3 = CallStatApplication.time_section_append_delta[2];
			delta4 = CallStatApplication.time_section_append_delta[3];
			delta5 = CallStatApplication.time_section_append_delta[4];
			delta6 = CallStatApplication.time_section_append_delta[5];

			int int_delta1, int_delta2, int_delta3, int_delta4, int_delta5, int_delta6;
			int_delta1 = Integer.parseInt(delta1);
			int_delta2 = Integer.parseInt(delta2);
			int_delta3 = Integer.parseInt(delta3);
			int_delta4 = Integer.parseInt(delta4);
			int_delta5 = Integer.parseInt(delta5);
			int_delta6 = Integer.parseInt(delta6);

			int int_hour_minute = Integer.parseInt(hour_minute);
			if (int_delta1 < int_delta2) {
				if (int_hour_minute >= int_delta1
						&& int_hour_minute <= int_delta2) {
					return 0;
				}
			} else {
				if ((int_hour_minute >= int_delta1 && int_hour_minute <= 2359)
						|| (int_hour_minute >= 0 && int_hour_minute <= int_delta2)) {
					return 0;
				}
			}

			if (int_delta2 < int_delta3) {
				if (int_hour_minute >= int_delta2
						&& int_hour_minute <= int_delta3) {
					return 1;
				}
			} else {
				if ((int_hour_minute >= int_delta2 && int_hour_minute <= 2359)
						|| (int_hour_minute >= 0 && int_hour_minute <= int_delta3)) {
					return 1;
				}
			}

			if (int_delta3 < int_delta4) {
				if (int_hour_minute >= int_delta3
						&& int_hour_minute <= int_delta4) {
					return 2;
				}
			} else {
				if ((int_hour_minute >= int_delta3 && int_hour_minute <= 2359)
						|| (int_hour_minute >= 0 && int_hour_minute <= int_delta4)) {
					return 2;
				}
			}

			if (int_delta4 < int_delta5) {
				if (int_hour_minute >= int_delta4
						&& int_hour_minute <= int_delta5) {
					return 3;
				}
			} else {
				if ((int_hour_minute >= int_delta4 && int_hour_minute <= 2359)
						|| (int_hour_minute >= 0 && int_hour_minute <= int_delta5)) {
					return 3;
				}
			}

			if (int_delta5 < int_delta6) {
				if (int_hour_minute >= int_delta5
						&& int_hour_minute <= int_delta6) {
					return 4;
				}
			} else {
				if ((int_hour_minute >= int_delta5 && int_hour_minute <= 2359)
						|| (int_hour_minute >= 0 && int_hour_minute <= int_delta6)) {
					return 4;
				}
			}

			if (int_delta6 < int_delta1) {
				if (int_hour_minute >= int_delta6
						&& int_hour_minute <= int_delta1) {
					return 5;
				}
			} else {
				if ((int_hour_minute >= int_delta6 && int_hour_minute <= 2359)
						|| (int_hour_minute >= 0 && int_hour_minute <= int_delta1)) {
					return 5;
				}
			}
		} catch (Exception e) {
			ILog.logException(CallStatUtils.class, e);
			return 0;
		}

		return 0;
	}

	// 获取此次对帐和最近一次成功对账的时间差
	public static String getLastCheckTime() {
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

	public static boolean isAllZeros(int[] int_array) {
		for (int i = 0; i < int_array.length; i++) {
			if (int_array[i] != 0) {
				return false;
			}
		}
		return true;
	}

	// 文件拷贝
	// 要复制的目录下的所有非子目录(文件夹)文件拷贝
	public static boolean CopyFile(String fromFile, String toFile) {
		boolean isCopyDone = false;
		File src = new File(fromFile);
		try {
			InputStream fosfrom = new FileInputStream(fromFile);
			OutputStream fosto = new FileOutputStream(toFile);
			byte bt[] = new byte[1024];
			int c;
			while ((c = fosfrom.read(bt)) != -1) {
				fosto.write(bt, 0, c);
			}
			fosfrom.close();
			fosto.close();
			isCopyDone = true;

		} catch (Exception ex) {
			isCopyDone = false;
		} finally {
			if (isCopyDone) {
				Log.d("wc", "copy over");
				src.delete();
				Log.d("wc", "old sqlite db has been deleted");
				ILog.LogI(CallStatUtils.class,
						"old sqlite db has been deleted.");
			}
		}

		return isCopyDone;
	}

	// db合并
	public static void UnionFile(String fromFile, String toFile) {
		boolean isDone = false;
		SQLiteDatabase DDDB = null;
		SQLiteDatabase SDDB = null;
		try {
			DDDB = SQLiteDatabase.openOrCreateDatabase(fromFile, null);
			SDDB = SQLiteDatabase.openOrCreateDatabase(toFile, null);
			Cursor tablesCursor = DDDB
					.rawQuery(
							"select name from sqlite_master where type='table' order by name",
							null);
			while (tablesCursor.moveToNext()) {
				Log.d("wc", tablesCursor.getString(0));
				Cursor c = DDDB.query(tablesCursor.getString(0), null, null,
						null, null, null, null);
				String columnNames[] = c.getColumnNames();
				Log.d("wc", "count" + c.getCount());
				while (c.moveToNext()) {
					ContentValues values = new ContentValues();
					for (String columnName : columnNames) {
						values.put(columnName,
								c.getString(c.getColumnIndex(columnName)));
					}
					if (tablesCursor.getString(0).equals("t_accounting_code")
							|| tablesCursor.getString(0).equals("t_call_log")
							|| tablesCursor.getString(0).equals(
									"t_reconciliation_info")
							|| tablesCursor.getString(0).equals("t_sms_log")) {
						Log.d("wc", "remove");
						values.remove("_id");
					}
					try {
						Log.d("wc", "insert:" + values);
						SDDB.insert(tablesCursor.getString(0), null, values);
						Log.d("wc", "insert OK");
					} catch (Exception e) {
						Log.d("wc", "Union over");
						ILog.LogI(
								CallStatUtils.class,
								"insert exception:tableName="
										+ tablesCursor.getString(0));
					}
				}
				c.close();
			}
			if (tablesCursor != null)
				tablesCursor.close();
			isDone = true;

		} catch (Exception ex) {
			ex.printStackTrace();
			isDone = false;
		} finally {
			if (null != DDDB)
				DDDB.close();
			if (null != SDDB)
				SDDB.close();
			if (isDone) {
				File src = new File(fromFile);
				src.delete();
				ILog.LogI(CallStatUtils.class,
						"old sqlite db has been deleted.");
			}
		}
	}

	// 将System.currentTimeMillis 转换成当前日期
	public static String[] getDate(long lastTimeMillis, long nowTimemillis) {
		String[] dateStrings = new String[] { "", "" };
		Date lastDate = new Date(lastTimeMillis);
		Date nowDate = new Date(nowTimemillis);
		Calendar aCalendar = Calendar.getInstance();
		Calendar bCalendar = Calendar.getInstance();
		aCalendar.setTime(lastDate);
		bCalendar.setTime(nowDate);
		int days = 0;
		while (aCalendar.before(bCalendar)) {
			days++;
			aCalendar.add(Calendar.DAY_OF_YEAR, 1);
		}
		dateStrings[0] = String.valueOf(days - 1);
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		dateStrings[1] = String.valueOf(sdf.format(lastDate));
		return dateStrings;
	}

	public static void updateSmSMatchRule(Context mContext) {
		if (isNetworkAvailable(mContext)) {
			String url = mContext.getString(R.string.sms_match_rule_url);
			ILog.LogI(CallStatUtils.class, "updateSmSMatchRule url:" + url);

			HttpGet request = new HttpGet(url);
			DefaultHttpClient client = new DefaultHttpClient();
			try {
				HttpResponse response = client.execute(request);
				int code = response.getStatusLine().getStatusCode();
				String strResult = EntityUtils.toString(response.getEntity(),
						HTTP.UTF_8);
				if (code == 200) {
					ILog.LogI(CallStatUtils.class, "request successfully,code:"
							+ code);
					String[] parts = strResult.split("\n");
					StringBuilder sb = new StringBuilder();
					for (int i = 0; i < parts.length; i++) {
						sb.append(parts[i] + "#");
					}

					String rule = sb.toString();
					if (rule.endsWith("#")) {
						rule = rule.substring(0, rule.length() - 1);
					}
					ILog.LogI(CallStatUtils.class, "rule:" + rule);
					new ConfigManager(mContext).setSmsSplitRule(rule);
				}
			} catch (ClientProtocolException e) {
				// ILog.logException(CallStatUtils.class, e);
				e.printStackTrace();
			} catch (IOException e) {
				// ILog.logException(CallStatUtils.class, e);
				e.printStackTrace();
			}
		}
	}

	public static boolean isMIUI4Plus() {
		String model = android.os.Build.MODEL;
		String release = android.os.Build.VERSION.RELEASE;
		String incremental = android.os.Build.VERSION.INCREMENTAL;
		return model.startsWith("MI-") && release.startsWith("4")
				&& incremental.startsWith("ICS");
	}

	public static int updateCode(Context mContext, String province,
			String operator, String brand, String queryNum, int type,
			String code) {
		int result = 1;
		if (isNetworkAvailable(mContext)) {
			String url = mContext.getString(R.string.update_code_url);
			ILog.LogI(CallStatUtils.class, "update_code_url:" + url);

			HttpPost request = new HttpPost(url);
			DefaultHttpClient client = new DefaultHttpClient();
			try {
				ConfigManager config = new ConfigManager(mContext);
				Map<String, String> map = new HashMap<String, String>();
				map.put("province", province);
				map.put("operator", operator);
				map.put("brand", brand);
				map.put("queryNum", queryNum);
				map.put("type", String.valueOf(type));
				map.put("code", code);
				UrlEncodedFormEntity entity = MyHttpPostHelper
						.buildUrlEncodedFormEntity(map, null);
				request.setEntity(entity);
				HttpResponse response = client.execute(request);
				int res_pcode = response.getStatusLine().getStatusCode();
				String strResult = EntityUtils.toString(response.getEntity(),
						HTTP.UTF_8);
				if (res_pcode == 200) {
					ILog.LogI(CallStatUtils.class, "request successfully,code:"
							+ code);
					result = Integer.parseInt(strResult);
				}
			} catch (ClientProtocolException e) {
				ILog.logException(CallStatUtils.class, e);
				e.printStackTrace();
			} catch (IOException e) {
				ILog.logException(CallStatUtils.class, e);
				e.printStackTrace();
			}
		}
		return result;
	}

	// 判断程序是不是处于前台运行
	public static boolean isMyAppOnDesk(Context context) {
		ActivityManager activityManager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningTaskInfo> tasks = activityManager.getRunningTasks(1);
		return tasks.get(0).topActivity.getPackageName().equals(MY_APP_PACKAGE);
	}

	/*
	 * //根据对方电话号码找联系人的方法 //由于考虑到有些手机上存联系人会存 +86，所以下面采用了模糊匹配的办法。 public static
	 * String getNameFromPhone(String number) { String name = "未知号码联系人";
	 * String[] projection = { ContactsContract.PhoneLookup.DISPLAY_NAME,
	 * ContactsContract.CommonDataKinds.Phone.NUMBER };
	 * 
	 * Cursor cursor =
	 * CallStatApplication.getCallstatsContext().getContentResolver().query(
	 * ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, // Which
	 * columns to return. ContactsContract.CommonDataKinds.Phone.NUMBER +
	 * " LIKE ?",//WHERE clause. new String[]{"%"+ number}, // WHERE clause
	 * value substitution null); // Sort order.
	 * 
	 * if (cursor == null) { ILog.LogD(CallStatUtils.class, "getPeople null");
	 * return name; } ILog.LogD(CallStatUtils.class,
	 * "getPeople cursor.getCount() = " + cursor.getCount()); for (int i = 0; i
	 * < cursor.getCount(); i++) { cursor.moveToPosition(i);
	 * 
	 * int nameFieldColumnIndex = cursor
	 * .getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME); name =
	 * cursor.getString(nameFieldColumnIndex); ILog.LogI(CallStatUtils.class, ""
	 * + name + " .... " + nameFieldColumnIndex); } cursor.close(); return name;
	 * }
	 */

	// 这个函数做联系人号码和联系人姓名的一个映射，联系人号码做了去除前缀和空格的处理。
	public static HashMap<String, String> InitContactMap() {
		String number = "12345678954";
		String name = "未知号码联系人";
		String[] projection = { ContactsContract.PhoneLookup.DISPLAY_NAME,
				ContactsContract.CommonDataKinds.Phone.NUMBER };
		Cursor cursor = null;
		try {
			cursor = CallStatApplication
					.getCallstatsContext()
					.getContentResolver()
					.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
							projection, // Which columns to return.
							null,// WHERE clause.
							null, // WHERE clause value substitution
							null);
			if (cursor != null && cursor.moveToFirst()) {
				do {
					int numberFiledColumnIndex = cursor
							.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
					int nameFieldColumnIndex = cursor
							.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
					number = cursor.getString(numberFiledColumnIndex);
					name = cursor.getString(nameFieldColumnIndex);
					if (number.contains(" ")) {
						number = number.replace(" ", "");
					}
					if (number.startsWith("+86")) {
						number = number.substring(3);
					}
					contact_info_has_map.put(number, name);
				} while (cursor.moveToNext());
			}
		} catch (Exception e) {
			ILog.logException(CallStatUtils.class, e);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return contact_info_has_map;
	}

	// 根据对方电话号码找联系人的方法(根据初始化好的映射关系表来查找联系人姓名)
	public static String getNameFromPhone(String number) {
		if (contact_info_has_map == null || contact_info_has_map.isEmpty()) {
			return "未知号码联系人";
		}
		number = number.replace("+86", "").replace(" ", "");
		String ret = contact_info_has_map.get(number);
		if (ret != null) {
			return ret;
		} else {
			return "未知号码联系人";
		}
	}

	// 根据联系人姓名查找联系人的电话号码（也是根据初始化好的映射关系表来查找）
	public static String getPhoneFromName(String name) {
		if (contact_info_has_map == null || contact_info_has_map.isEmpty()) {
			return null;
		}
		Set<String> contact_number = contact_info_has_map.keySet();// 联系人号码组成的集合
		for (String number : contact_number) {
			if (contact_info_has_map.get(number) != null
					&& contact_info_has_map.get(number).equalsIgnoreCase(name)) {
				return number;
			}
		}
		return null;
	}

	// config表示配置文件的句柄，number代表对方的电话号码，duration代表短信的条数（如果此次事件是短信）或者是所打电话的分钟数（如果此次事件是打电话）
	public static MonthlyStatDataSource makeMonthlyStatDataSourceRec(
			String date, int type, ConfigManager config, String number,
			int duration) {
		float used_fee_to_first_day = 0f;
		long delta_gprs = 0;
		float delta_fee = 0f;
		int delta_wlan = 0;
		if (config.getFeeSpent() != 100000) {
			used_fee_to_first_day = config.getFeeSpent();
		}
		if (config.getLastEventTrafficUsed() != -100000
				&& config.getTotalGprsUsedDifference() != 0) {// 说明上次事件发生时流量使用量已记录下来
			delta_gprs = (long) (config.getTotalGprsUsed()
					+ config.getTotalGprsUsedDifference() - config
					.getLastEventTrafficUsed());
		}
		if (config.getLastEventFeeAvail() != -100000
				&& config.getFeesRemian() != 100000) {// 说明上次事件发生时剩余话费已经记录下来
			delta_fee = config.getLastEventFeeAvail() - config.getFeesRemian();
		}

		MonthlyStatDataSource monthly_stat_rec = new MonthlyStatDataSource(
				date,
				CallStatUtils.changeMilliSeconds2YearMonthDayHourMin(Long
						.parseLong(date)),
				config.getProvince(),
				config.getCity(),
				config.getOperator(),
				config.getPackageBrand(),
				type,
				number,
				CallStatUtils.getNameFromPhone(number),
				duration,
				config.getTotalGprsUsed() + config.getTotalGprsUsedDifference(),
				delta_gprs, used_fee_to_first_day, delta_fee, config
						.getLocalRates(), config.getLongRates(), config
						.getRoamingRates(), config.getRatesIP(), config
						.getRatesShort(), config.getRatesTraffic(), config
						.getAlreadyUsedWlan(), delta_wlan,
				config.getRatesSms(), false);
		return monthly_stat_rec;
	}

	// 输入参数：start 起始统计时间（年月日时分格式），end 终止统计时间（年月日时分格式）
	// 函数返回值：返回反映用户月消费行为的短信前十名字符串
	public static String makeTop10smsString(String start, String end) {
		String top10sms = "";
		StringBuffer strBufTop10sms = new StringBuffer();
		LinkedHashMap<String, Integer> mapTop10Sms = CallStatDatabase
				.getInstance(CallStatApplication.getCallstatsContext())
				.getTop10SMS(start, end);
		Set<String> setsms = mapTop10Sms.keySet();
		for (String s : setsms) {
			strBufTop10sms.append(s);
			strBufTop10sms.append(":");
			strBufTop10sms.append(CallStatUtils.getNameFromPhone(s));
			strBufTop10sms.append(":");
			strBufTop10sms.append(mapTop10Sms.get(s).toString());
			strBufTop10sms.append(",");
		}
		top10sms = strBufTop10sms.toString();
		if (top10sms.endsWith(",")) {
			top10sms = top10sms.substring(0, top10sms.length() - 1);
		}
		return top10sms;
	}

	// 输入参数：start 起始统计时间（年月日时分格式），end 终止统计时间（年月日时分格式）
	// 函数返回值：返回反映用户月消费行为的通话前十名字符串
	public static String makeTop10phoneString(String start, String end) {
		String top10phone = "";
		StringBuffer strBufTop10calls = new StringBuffer();
		LinkedHashMap<String, Integer> mapTop10calls = CallStatDatabase
				.getInstance(CallStatApplication.getCallstatsContext())
				.getTop10Calls(start, end);
		Set<String> setcalls = mapTop10calls.keySet();
		for (String s : setcalls) {
			strBufTop10calls.append(s);
			strBufTop10calls.append(":");
			strBufTop10calls.append(CallStatUtils.getNameFromPhone(s));
			strBufTop10calls.append(":");
			strBufTop10calls.append(mapTop10calls.get(s).toString());
			strBufTop10calls.append(",");
		}
		top10phone = strBufTop10calls.toString();
		if (top10phone.endsWith(",")) {
			top10phone = top10phone.substring(0, top10phone.length() - 1);
		}
		return top10phone;
	}

	// 输入参数：start 起始统计时间（年月日时分格式），end 终止统计时间（年月日时分格式）
	// 函数返回值：返回反映用户月消费行为的长途通话前十名字符串
	public static String makeTop10LongString(String start, String end) {
		String top10_long = "";
		StringBuffer strBufTop10calls = new StringBuffer();
		LinkedHashMap<String, Integer> mapTop10_long = CallStatDatabase
				.getInstance(CallStatApplication.getCallstatsContext())
				.getTop10LongCalls(start, end);
		Set<String> setcalls = mapTop10_long.keySet();
		for (String s : setcalls) {
			PhoneNumberInfo phone_info = CallStatDatabase.getInstance(
					CallStatApplication.getCallstatsContext())
					.getPhoneNumberInfo5All(s);
			strBufTop10calls.append(s);
			strBufTop10calls.append(":");
			strBufTop10calls.append(CallStatUtils.getNameFromPhone(s));
			strBufTop10calls.append(":");
			strBufTop10calls.append(mapTop10_long.get(s).toString());
			strBufTop10calls.append(":");
			strBufTop10calls.append(phone_info.getProvince());
			strBufTop10calls.append(":");
			strBufTop10calls.append(phone_info.getCity());
			strBufTop10calls.append(",");
		}
		top10_long = strBufTop10calls.toString();
		if (top10_long.endsWith(",")) {
			top10_long = top10_long.substring(0, top10_long.length() - 1);
		}
		return top10_long;
	}

	// 输入参数：start 起始统计时间（年月日时分格式），end 终止统计时间（年月日时分格式）
	// 函数返回值：返回反映用户月消费行为的本地通话前十名字符串
	public static String makeTop10LocalString(String start, String end) {
		String top10_local = "";
		StringBuffer strBufTop10calls = new StringBuffer();
		LinkedHashMap<String, Integer> mapTop10_local = CallStatDatabase
				.getInstance(CallStatApplication.getCallstatsContext())
				.getTop10LocalCalls(start, end);
		Set<String> setcalls = mapTop10_local.keySet();
		for (String s : setcalls) {
			strBufTop10calls.append(s);
			strBufTop10calls.append(":");
			strBufTop10calls.append(CallStatUtils.getNameFromPhone(s));
			strBufTop10calls.append(":");
			strBufTop10calls.append(mapTop10_local.get(s).toString());
			strBufTop10calls.append(",");
		}
		top10_local = strBufTop10calls.toString();
		if (top10_local.endsWith(",")) {
			top10_local = top10_local.substring(0, top10_local.length() - 1);
		}
		return top10_local;
	}

	// 输入参数：String is_month,String start,String end,String top10sms;String
	// top10phone;String localTime;String
	// longTime;String roamingTime;String sms;
	// String ipTime;String shortTime;long GprsUsedInInterval;float
	// feeUseInInterval; int days;
	// 函数返回值：返回给服务器发送用户月消费行为的map键值对
	public static Map<String, String> generateMonthlyConsumeMap(
			String is_month, String start, String end, String top10local,
			String top10long, String localTime, String longTime,
			String roamingTime, String sms, String ipTime, String shortTime,
			long GprsUsedInInterval, float feeUseInInterval, int days) {
		Map<String, String> map = new HashMap<String, String>();
		ConfigManager configManager = new ConfigManager(
				CallStatApplication.getCallstatsContext());
		CallStatDatabase callstatdatabase = CallStatDatabase
				.getInstance(CallStatApplication.getCallstatsContext());
		map.put("time", CallStatUtils.getNowDate());
		map.put("province", configManager.getProvince());
		map.put("city", configManager.getCity());
		map.put("oper", configManager.getOperator());
		map.put("brand", configManager.getPackageBrand());
		map.put("top10local", top10local);
		map.put("top10long", top10long);
		map.put("alllocaltime", localTime);
		map.put("alllongtime", longTime);
		map.put("allroamingtime", Integer.parseInt(roamingTime) / 2 + "");
		map.put("allsroamingtime", Integer.parseInt(roamingTime) / 2 + "");
		map.put("allsms", sms);
		map.put("allIP", ipTime);
		map.put("allshort", shortTime);
		map.put("allusedGprs", GprsUsedInInterval + "");
		map.put("allwlan_used", "0");
		map.put("allfee", feeUseInInterval + "");
		map.put("rates_for_local",
				callstatdatabase.getRatesForLocalFromDb(start, end) + "");
		map.put("rates_for_long_distance",
				callstatdatabase.getRatesForLongFromDb(start, end) + "");
		map.put("rates_for_roaming",
				callstatdatabase.getRatesForRoamingFromDb(start, end) + "");
		map.put("rates_for_sroaming",
				callstatdatabase.getRatesForRoamingFromDb(start, end) + "");
		map.put("rates_for_IP",
				callstatdatabase.getRatesForIPFromDb(start, end) + "");
		map.put("rates_for_short",
				callstatdatabase.getRatesForShortFromDb(start, end) + "");
		map.put("rates_for_traffic", "1");// 先暂时写成1元/M
		map.put("rates_for_wlan", "0");
		map.put("rates_for_sms",
				callstatdatabase.getRatesForSmsFromDb(start, end) + "");
		map.put("version", configManager.getVersionName());
		map.put("days", days + "");
		map.put("is_month", is_month);
		map.put("imei", DeviceInformation.getInformation(InfoName.IMEI));
		map.put("uuid", "");
		map.put("phonenum", configManager.getTopEightNum());
		return map;
	}

}
