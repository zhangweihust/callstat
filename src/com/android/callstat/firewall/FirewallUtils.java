package com.android.callstat.firewall;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.callstat.CallStatApplication;
import com.android.callstat.firewall.bean.NewTrafficDetail;

/**
 * this class provides common methods for firewall
 * 
 * @author longX
 * */
public class FirewallUtils {
	// TODO
	// this method provides an easy entrance to turn on or off wifi

	public static boolean isWifiEnabled(Context ctx) {
		return ((WifiManager) ctx.getSystemService(Context.WIFI_SERVICE))
				.isWifiEnabled();
	}

	public static boolean shiftWifi(Context ctx) {
		WifiManager wifi = (WifiManager) ctx
				.getSystemService(Context.WIFI_SERVICE);
		if (wifi.isWifiEnabled()) {
			wifi.setWifiEnabled(false);
			return false;
		} else {
			wifi.setWifiEnabled(true);
			return true;
		}
	}

	// this method provides an easy entrance to turn on or off gprs

	public static boolean isGprsEnabled(Context ctx) {
		return (((TelephonyManager) ctx
				.getSystemService(Context.TELEPHONY_SERVICE)).getDataState() == TelephonyManager.DATA_CONNECTED) ? true
				: false;
	}

	public static boolean isGprsConnecting(Context ctx) {
		return (((TelephonyManager) ctx
				.getSystemService(Context.TELEPHONY_SERVICE)).getDataState() == TelephonyManager.DATA_CONNECTING) ? true
				: false;
	}

	public static boolean isGprsSuspended(Context ctx) {
		return (((TelephonyManager) ctx
				.getSystemService(Context.TELEPHONY_SERVICE)).getDataState() == TelephonyManager.DATA_SUSPENDED) ? true
				: false;
	}

	public static boolean shiftGprs(Context ctx) {
		Log.i("callstats", "in  shiftGprs ");
		TelephonyManager tm = (TelephonyManager) ctx
				.getSystemService(Context.TELEPHONY_SERVICE);
		// see also android.telephony.TelephonyManager
		boolean isGprsEnabled = (tm.getDataState() == TelephonyManager.DATA_CONNECTED);

		// Method getITelephonyMethod;
		try {
			/*
			 * getITelephonyMethod =
			 * tm.getClass().getDeclaredMethod("getITelephony");
			 * getITelephonyMethod.setAccessible(true);//私有化函数也能使用 ITelephony
			 * iTelephony = (ITelephony) getITelephonyMethod.invoke(tm);
			 */

			/*
			 * if (DeviceUtils.getOsVersion().contains("2.2")) {
			 * Log.i("callstats", "in  iTelephony "); Log.i("i",
			 * "FirewallUtils"); if(isGprsEnabled) {
			 * //setMobileDataEnabled(ctx,false); Log.i("callstats",
			 * "iTelephony works!"); boolean isSettingWorked =
			 * iTelephony.disableDataConnectivity(); return isSettingWorked ?
			 * (!isGprsEnabled) : isGprsEnabled; } else {
			 * //setMobileDataEnabled(ctx,true); Log.i("callstats",
			 * "iTelephony works!"); boolean isSettingWorked =
			 * iTelephony.enableDataConnectivity(); return isSettingWorked ?
			 * (!isGprsEnabled) : isGprsEnabled; } } else {
			 */
			if (isGprsEnabled) {
				boolean isSettingWorked = setMobileDataEnabled(ctx, false);
				Log.i("callstats", "close action---ConnectivityManager works!");
				return isSettingWorked ? (!isGprsEnabled) : isGprsEnabled;
			} else {
				boolean isSettingWorked = setMobileDataEnabled(ctx, true);
				Log.i("callstats", "open action---ConnectivityManager works!");
				return isSettingWorked ? (!isGprsEnabled) : isGprsEnabled;
			}
			// }
		} catch (SecurityException e) {
			Log.i("callstats", "SecurityException");
			e.printStackTrace();
			return false;
		} catch (IllegalArgumentException e) {
			Log.i("callstats", "IllegalArgumentException");
			e.printStackTrace();
			return false;
		}
	}

	public static int shiftGprs(Context ctx, int from) {

		Log.i("callstats", "in  shiftGprs ");
		TelephonyManager tm = (TelephonyManager) ctx
				.getSystemService(Context.TELEPHONY_SERVICE);
		// see also android.telephony.TelephonyManager
		boolean isGprsEnabled = (tm.getDataState() == TelephonyManager.DATA_CONNECTED);
		int code = -1;
		// Method getITelephonyMethod;
		try {
			/*
			 * getITelephonyMethod =
			 * tm.getClass().getDeclaredMethod("getITelephony");
			 * getITelephonyMethod.setAccessible(true);//私有化函数也能使用 ITelephony
			 * iTelephony = (ITelephony) getITelephonyMethod.invoke(tm);
			 * 
			 * if (DeviceUtils.getOsVersion().contains("2.2")) {
			 * Log.i("callstats", "in  iTelephony "); if(isGprsEnabled) {
			 * //setMobileDataEnabled(ctx,false); Log.i("callstats",
			 * "iTelephony works!"); boolean isSettingWorked =
			 * iTelephony.disableDataConnectivity(); if (isSettingWorked) { code
			 * = 0; } else { code = 1; } } else {
			 * //setMobileDataEnabled(ctx,true); Log.i("callstats",
			 * "iTelephony works!"); boolean isSettingWorked =
			 * iTelephony.enableDataConnectivity(); if (isSettingWorked) { code
			 * = 1; } else { code = 0; } } } else {
			 */
			if (isGprsEnabled) {
				int flag = setMobileDataEnabled(ctx, false, from);
				if (flag == 1) {
					code = 0;
				}
				if (flag == -1) {
					code = -1;
				}
			} else {
				int flag = setMobileDataEnabled(ctx, true, from);
				if (flag == 1) {
					code = 1;
				}
				if (flag == -1) {
					code = -1;
				}
			}
			// }
		} catch (SecurityException e) {
			Log.i("callstats", "SecurityException");
			e.printStackTrace();
			code = -1;
		} catch (IllegalArgumentException e) {
			Log.i("callstats", "IllegalArgumentException");
			e.printStackTrace();
			code = -1;
		}
		return code;
	}

	private static boolean setMobileDataEnabled(Context context, boolean enabled) {
		boolean isSettingWorked = false;
		final ConnectivityManager conman = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		Class<?> conmanClass;
		try {
			conmanClass = Class.forName(conman.getClass().getName());
			// mService is modified by private
			final Field iConnectivityManagerField = conmanClass
					.getDeclaredField("mService");
			iConnectivityManagerField.setAccessible(true);

			final Object iConnectivityManager = iConnectivityManagerField
					.get(conman);
			final Class<?> iConnectivityManagerClass = Class
					.forName(iConnectivityManager.getClass().getName());
			final Method setMobileDataEnabledMethod = iConnectivityManagerClass
					.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
			setMobileDataEnabledMethod.setAccessible(true);

			setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
			isSettingWorked = true;
		} catch (ClassNotFoundException e) {
			Log.i("callstats", "ClassNotFoundException");
			isSettingWorked = false;
			e.printStackTrace();
		} catch (SecurityException e) {
			Log.i("callstats", "SecurityException");
			isSettingWorked = false;
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			Log.i("callstats", "NoSuchFieldException");
			isSettingWorked = false;
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			Log.i("callstats", "IllegalArgumentException");
			isSettingWorked = false;
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			Log.i("callstats", "IllegalAccessException");
			isSettingWorked = false;
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			Log.i("callstats", "InvocationTargetException");
			isSettingWorked = false;
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			Log.i("callstats", "NoSuchMethodException");
			isSettingWorked = false;
			e.printStackTrace();
		}
		return isSettingWorked;
	}

	private static int setMobileDataEnabled(Context context, boolean enabled,
			int from) {
		int code = -1;
		final ConnectivityManager conman = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		Class<?> conmanClass;
		try {
			conmanClass = Class.forName(conman.getClass().getName());
			// mService is modified by private
			final Field iConnectivityManagerField = conmanClass
					.getDeclaredField("mService");
			iConnectivityManagerField.setAccessible(true);

			final Object iConnectivityManager = iConnectivityManagerField
					.get(conman);
			final Class<?> iConnectivityManagerClass = Class
					.forName(iConnectivityManager.getClass().getName());
			final Method setMobileDataEnabledMethod = iConnectivityManagerClass
					.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
			setMobileDataEnabledMethod.setAccessible(true);

			setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
			code = 1;
		} catch (ClassNotFoundException e) {
			Log.i("callstats", "ClassNotFoundException");
			code = -1;
			e.printStackTrace();
		} catch (SecurityException e) {
			Log.i("callstats", "SecurityException");
			code = -1;
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			Log.i("callstats", "NoSuchFieldException");
			code = -1;
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			Log.i("callstats", "IllegalArgumentException");
			code = -1;
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			Log.i("callstats", "IllegalAccessException");
			code = -1;
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			Log.i("callstats", "InvocationTargetException");
			code = -1;
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			Log.i("callstats", "NoSuchMethodException");
			code = -1;
			e.printStackTrace();
		}
		return code;
	}

	public static Set<Integer> getBothRejectedList(List<NewTrafficDetail> all) {
		Set<Integer> rejectedList = new TreeSet<Integer>();
		AllRejectedList.clear();
		WifiRejectedList.clear();
		for (NewTrafficDetail detail : all) {
			if (detail.getIsGprsRejected()) {
				rejectedList.add(detail.getUid());
			}
			if (detail.getIsWifiRejected()) {
				WifiRejectedList.add(detail.getUid());
			}
			AllRejectedList.add(detail.getUid());
		}
		return rejectedList;
	}

	public static Set<Integer> getRejectedList(List<NewTrafficDetail> all) {
		Set<Integer> rejectedList = new TreeSet<Integer>();
		AllRejectedList.clear();
		WifiRejectedList.clear();
		for (NewTrafficDetail detail : all) {
			if (detail.getIsGprsRejected()) {
				rejectedList.add(detail.getUid());
			}
			if (detail.getIsWifiRejected()) {
				WifiRejectedList.add(detail.getUid());
			}
			AllRejectedList.add(detail.getUid());
		}
		return rejectedList;
	}

	static Set<Integer> AllRejectedList = new TreeSet<Integer>();
	static Set<Integer> WifiRejectedList = new TreeSet<Integer>();

	// 必须让getRejectedList（）方法先执行，否则后面2个方法取值不正确
	public static Set<Integer> getWifiRejectedList() {

		return WifiRejectedList;
	}

	public static Set<Integer> getAllRejectedList() {
		return AllRejectedList;
	}

	public static Set<Integer> getRejectedList(List<NewTrafficDetail> src,
			List<String> rejectedDataDirs) {
		Set<Integer> rejectedUids = new TreeSet<Integer>();

		for (NewTrafficDetail detail : src) {
			int uid = detail.getUid();
			String dir = detail.getPackageName();
			detail.setIsGprsRejected(false);
			for (String dataDir : rejectedDataDirs) {
				if (dir != null && dir.equals(dataDir)) {
					rejectedUids.add(uid);
					detail.setIsGprsRejected(true);
					continue;
				}
			}
		}
		return rejectedUids;
	}

	public static void applyRejectedList(List<NewTrafficDetail> all) {
		try {
			FirewallCoreWorker.initIptables(
					CallStatApplication.canMyFirewallWork, true);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (!CallStatApplication.canFirewallWork) {
			return;
		}

		for (NewTrafficDetail detail : all) {
			if (detail.getIsGprsRejected()) {
				FirewallCoreWorker.applyRule(detail.getUid(), "A",
						CallStatApplication.canMyFirewallWork);
			}
		}
	}

	/*
	 * public static Set<Integer> getRejectedList(List<TrafficDetail> src) {
	 * Set<Integer> rejectedUids = new TreeSet<Integer>(); for (TrafficDetail
	 * detail : src) { if (detail.isRejected()) {
	 * rejectedUids.add(detail.getUid()); } }
	 * 
	 * return rejectedUids; }
	 */

	/**
	 * @param Map
	 *            <String,String> key:dataDir value:app_user
	 * */
	public static List<String> getRejectedPackageNames(
			Map<String, String> appUserAndDir, List<String> rejectedApps) {
		List<String> rejectedDataDirs = new ArrayList<String>();
		Set<String> keys = appUserAndDir.keySet();
		for (String key : keys) {
			for (String value : rejectedApps) {
				String tmp = appUserAndDir.get(key);
				if (tmp != null && tmp.equals(value)) {
					rejectedDataDirs.add(key);
				}
			}
		}

		return rejectedDataDirs;
	}

	public static void initIptables() {

	}

}
