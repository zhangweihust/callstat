package com.archermind.callstat.common;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;

import com.archermind.callstat.CallStatApplication;

public class DeviceInformation {
	public static String getInformation(InfoName infoName) {
		String value = "";
		String str1 = null;
		String str2 = null;
		String[] arrayOfString = null;
		FileReader fr = null;
		BufferedReader localBufferedReader = null;
		DisplayMetrics dm;
		switch (infoName) {
		case IMEI:
			TelephonyManager tm = (TelephonyManager) CallStatApplication
					.getCallstatsContext().getSystemService(
							Context.TELEPHONY_SERVICE);
			value = tm.getDeviceId();
			break;
		case SYSTEM_VERSION:
			value = android.os.Build.DISPLAY;
			break;
		case PHONE_CALLSTATS_VERSION:
			try {
				PackageInfo packageInfo = CallStatApplication
						.getCallstatsContext()
						.getPackageManager()
						.getPackageInfo("com.archermind.callstat",
								PackageManager.GET_CONFIGURATIONS);
				value = packageInfo.versionName;
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
			break;
		case CPU_MODEL:
			str1 = "/proc/cpuinfo";
			try {
				fr = new FileReader(str1);
				localBufferedReader = new BufferedReader(fr, 8192);
				str2 = localBufferedReader.readLine();
				if (str2 == null) {
					return "未知";
				}
				arrayOfString = str2.split("\\s+");
				for (int i = 2; i < arrayOfString.length; i++) {
					value += arrayOfString[i] + " ";
				}
			} catch (IOException e) {
			} finally {
				try {
					if (localBufferedReader != null) {
						localBufferedReader.close();
					}
					if (fr != null) {
						fr.close();
					}
				} catch (Exception e) {

				}
			}
			break;
		case CPU_MAX_FREQUENCY:
			str1 = "/proc/cpuinfo";
			try {
				fr = new FileReader(str1);
				localBufferedReader = new BufferedReader(fr);
				str2 = localBufferedReader.readLine();
				if (str2 == null) {
					return "未知";
				}
				arrayOfString = str2.split("\\s+");
				value = arrayOfString[2];
			} catch (IOException e) {
			} finally {
				try {
					if (localBufferedReader != null) {
						localBufferedReader.close();
					}
					if (fr != null) {
						fr.close();
					}
				} catch (Exception e) {

				}
			}
			break;
		case MEMORY_TOTAL:
			str1 = "/proc/meminfo";
			try {
				fr = new FileReader(str1);
				localBufferedReader = new BufferedReader(fr);
				str2 = localBufferedReader.readLine();
				if (str2 == null) {
					return "未知";
				}
				arrayOfString = str2.split(":");
				value = arrayOfString[1].trim();

			} catch (IOException e) {
			} finally {
				try {
					if (localBufferedReader != null) {
						localBufferedReader.close();
					}
					if (fr != null) {
						fr.close();
					}
				} catch (Exception e) {

				}
			}
			break;
		case SCREEN_RESOLUTION:
			dm = CallStatApplication.getCallstatsContext().getResources()
					.getDisplayMetrics();
			value = String.valueOf(dm.widthPixels) + "*"
					+ String.valueOf(dm.heightPixels);
			break;
		case PHONE_MODEL:
			value = android.os.Build.MODEL;
			break;
		}
		return value;
	}

	public enum InfoName {
		IMEI("imei"), SYSTEM_VERSION("osVersion"), PHONE_CALLSTATS_VERSION(
				"softVersion"), CPU_MODEL("cpuModel"), CPU_MAX_FREQUENCY(
				"cpuClk"), MEMORY_TOTAL("memSize"), SCREEN_RESOLUTION(
				"windowSize"), PHONE_MODEL("machModel");
		private String name;

		private InfoName(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
