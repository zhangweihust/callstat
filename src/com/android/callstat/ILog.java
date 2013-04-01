package com.android.callstat;

import java.util.Date;

import android.util.Log;

import com.android.callstat.common.download.CacheFileManager;

public class ILog {
	public final static String TAG = "callstats";

	public static void LogD(Class classz, String str) {
		Log.d(TAG, classz.getCanonicalName() + "--->" + str);
	}

	public static void LogI(Class classz, String str) {
		Log.i(TAG, classz.getCanonicalName() + "--->" + str);
	}

	public static void LogE(Class classz, String str) {
		Log.e(TAG, classz.getCanonicalName() + "--->" + str);
	}

	public static void LogV(Class classz, String str) {
		Log.v(TAG, classz.getCanonicalName() + "--->" + str);
	}

	public static void logException(Class c, Throwable e) {
		try {
			StringBuilder exceptionInfo = new StringBuilder();
			if (e == null) {
				exceptionInfo.append(new Date().toGMTString() + "\n"
						+ "Exception:"
						+ "e is null,probably null pointer exception" + "\n");
			} else {
				e.printStackTrace();
				exceptionInfo.append(new Date().toGMTString() + "\n");
				exceptionInfo.append(e.getClass().getCanonicalName() + ":"
						+ e.getMessage() + "\n");
				StackTraceElement[] stes = e.getStackTrace();
				for (StackTraceElement ste : stes) {
					exceptionInfo.append("at " + ste.getClassName() + "$"
							+ ste.getMethodName() + "$" + ste.getFileName()
							+ ":" + ste.getLineNumber() + "\n");
				}
			}

			LogE(c, exceptionInfo.toString());
			CacheFileManager.getInstance().log(exceptionInfo.toString());
		} catch (Exception ex) {
			LogE(c, ex.getMessage());
		}
	}
}