package com.archermind.callstat.common;

import java.io.IOException;
import java.io.InputStream;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;

import com.archermind.callstat.ConfigManager;

/**
 * reference: http://www.cnblogs.com/dynasty/archive/2011/04/20/2022409.html
 * this class provides general methods to get device information
 * 
 * @author longX
 * */
public class DeviceUtils {

	private static final String BOGOMIPS_PATTERN = "BogoMIPS[s]*:[s]*(d+.d+)[s]*n";
	private static final String MEMTOTAL_PATTERN = "MemTotal[s]*:[s]*(d+)[s]*kBn";
	private static final String MEMFREE_PATTERN = "MemFree[s]*:[s]*(d+)[s]*kBn";

	/**
	 * @return the version of os
	 */
	public static String fetch_version_info() {
		String result = null;
		CMDExecutor cmdexe = new CMDExecutor();
		try {
			String[] args = { "/system/bin/cat", "/proc/version" };
			result = cmdexe.run(args, "system/bin/");
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		// Log.i("i", "fetch_version_info:" + result);
		return result;
	}

	public static String fetch_tel_status(Context cx) {
		String result = null;
		TelephonyManager tm = (TelephonyManager) cx
				.getSystemService(Context.TELEPHONY_SERVICE);
		/*
		 * String str = " "; str += "DeviceId(IMEI) = " + tm.getDeviceId() +
		 * "\n"; str += "DeviceSoftwareVersion = " +
		 * tm.getDeviceSoftwareVersion() + "\n";
		 */
		// TODO: Do something ...
		String number = tm.getLine1Number();
		String imei = tm.getDeviceId();
		/*
		 * int mcc = cx.getResources().getConfiguration().mcc; int mnc =
		 * cx.getResources().getConfiguration().mnc; str +=
		 * "IMSI MCC (Mobile Country Code): " + String.valueOf(mcc) + "\n"; str
		 * += "IMSI MNC (Mobile Network Code): " + String.valueOf(mnc) + "\n";
		 * result = str;
		 */
		String deviceSoftwareVersion = tm.getDeviceSoftwareVersion();
		// Log.i("i", "number:" + number);
		// Log.i("i", "imei:" + imei);
		// Log.i("i", "deviceSoftwareVersion:" + deviceSoftwareVersion);
		return result;
	}

	public static String fetch_cpu_info() {
		String result = null;
		CMDExecutor cmdexe = new CMDExecutor();
		try {
			String[] args = { "/system/bin/cat",
					"/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq" };
			result = cmdexe.run(args, "/system/bin/");
			// Log.i("i", "fetch_cpu_info=" + result);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return result;
	}

	public static String getMaxCpuFreq() {
		String result = "";
		ProcessBuilder cmd;
		try {
			String[] args = { "/system/bin/cat",
					"/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq" };
			cmd = new ProcessBuilder(args);
			Process process = cmd.start();
			InputStream in = process.getInputStream();
			byte[] re = new byte[24];
			while (in.read(re) != -1) {
				result = result + new String(re);
			}
			in.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			result = "unknown";
		}
		return result.trim();
	}

	/**
	 * get system memory information
	 */
	public static String getMemoryInfo(Context context) {
		StringBuffer memoryInfo = new StringBuffer();

		final ActivityManager activityManager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		ActivityManager.MemoryInfo outInfo = new ActivityManager.MemoryInfo();
		activityManager.getMemoryInfo(outInfo);
		memoryInfo.append("\nTotal Available Memory :")
				.append(outInfo.availMem >> 10).append("k");
		memoryInfo.append("\nTotal Available Memory :")
				.append(outInfo.availMem >> 20).append("k");
		memoryInfo.append("\nIn low memory situation:").append(
				outInfo.lowMemory);

		String result = null;
		CMDExecutor cmdexe = new CMDExecutor();
		try {
			String[] args = { "/system/bin/cat", "/proc/meminfo" };
			result = cmdexe.run(args, "/system/bin/");
			// Log.i("i", "memoryInfo:" + result);
		} catch (IOException ex) {
			// Log.i("fetch_process_info", "ex=" + ex.toString());
		}
		return (memoryInfo.toString() + "\n\n" + result);
	}

	// get hard disk information
	public static String fetch_disk_info() {
		String result = null;
		CMDExecutor cmdexe = new CMDExecutor();
		try {
			String[] args = { "/system/bin/df" };
			result = cmdexe.run(args, "/system/bin/");
			// Log.i("result", "result=" + result);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return result;
	}

	/**
	 * get network device information
	 * */
	public static String fetch_netcfg_info() {
		String result = null;
		CMDExecutor cmdexe = new CMDExecutor();

		try {
			String[] args = { "/system/bin/netcfg" };
			result = cmdexe.run(args, "/system/bin/");
		} catch (IOException ex) {
			// Log.i("fetch_process_info", "ex=" + ex.toString());
		}
		return result;
	}

	// get screen size
	public static String getDisplayMetrics(Context cx) {
		String str = "";
		DisplayMetrics dm = new DisplayMetrics();
		dm = cx.getApplicationContext().getResources().getDisplayMetrics();
		int screenWidth = dm.widthPixels;
		int screenHeight = dm.heightPixels;
		float density = dm.density;
		float xdpi = dm.xdpi;
		float ydpi = dm.ydpi;
		str += "The absolute width: " + String.valueOf(screenWidth)
				+ "pixels\n";
		str += "The absolute heightin: " + String.valueOf(screenHeight)
				+ "pixels\n";
		str += "The logical density of the display. : "
				+ String.valueOf(density) + "\n";
		str += "X dimension : " + String.valueOf(xdpi) + "pixels per inch\n";
		str += "Y dimension : " + String.valueOf(ydpi) + "pixels per inch\n";
		// Log.i("i", "getDisplayMetrics:" + str);
		return str;
	}

	public static float getDeviceDisplayDensity(Context cx) {
		DisplayMetrics dm = new DisplayMetrics();
		dm = cx.getApplicationContext().getResources().getDisplayMetrics();
		return dm.density;
	}

	public static float getDeviceScreenWidth(Context cx) {
		DisplayMetrics dm = new DisplayMetrics();
		dm = cx.getApplicationContext().getResources().getDisplayMetrics();
		return dm.widthPixels;
	}

	public static float getDeviceScreenHeight(Context cx) {
		DisplayMetrics dm = new DisplayMetrics();
		dm = cx.getApplicationContext().getResources().getDisplayMetrics();
		return dm.heightPixels;
	}

	public static String getDeviceModel() {
		return android.os.Build.MODEL;
	}

	public static String getOsVersion() {
		return android.os.Build.VERSION.RELEASE;
	}

	public static String getDeviceScreenSize() {
		return android.os.Build.DISPLAY;
	}

	public static String getOperator(Context ctx) {
		TelephonyManager telManager = (TelephonyManager) ctx
				.getSystemService(Service.TELEPHONY_SERVICE);
		ConfigManager config = new ConfigManager(ctx);
		String operatorCode = telManager.getSimOperator();
		String operator = null;
		if (operatorCode != null) {

			if (operatorCode.equals("46000") || operatorCode.equals("46002")
					|| operatorCode.equals("46007")) {
				operator = "中国移动";
				// 中国移动
				config.setOperator("中国移动");
			} else if (operatorCode.equals("46001")) {
				operator = "中国联通";
				// 中国联通
				config.setOperator("中国联通");
			} else if (operatorCode.equals("46003")) {
				operator = "中国电信";
				// 中国电信
				config.setOperator("中国电信");
			}

		}
		return operator;
	}

	public static String getIMSI(Context ctx) {
		TelephonyManager telManager = (TelephonyManager) ctx
				.getSystemService(Service.TELEPHONY_SERVICE);
		return telManager.getSubscriberId();
	}

	public static String getLocalNumberFromSim(Context ctx) {
		TelephonyManager telManager = (TelephonyManager) ctx
				.getSystemService(Service.TELEPHONY_SERVICE);
		String num = telManager.getLine1Number();
		// Log.e("callstats", "getLocalNumberTopSeven:" + num);
		try {
			if (num != null && num.length() == 11) {
				return num;
			} else {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
	}

	public static boolean isSimcardPlaced(Context ctx) {
		TelephonyManager telManager = (TelephonyManager) ctx
				.getSystemService(Service.TELEPHONY_SERVICE);
		ConfigManager config = new ConfigManager(ctx);
		return config.getIMSI().equals(telManager.getSubscriberId());
	}
}
