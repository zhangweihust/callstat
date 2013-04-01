package com.android.callstat.common.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.android.callstat.DebugFlags;

class SystemFacade {

	private Context mContext;

	public SystemFacade(Context context) {
		mContext = context;
	}

	public long currentTimeMillis() {
		return System.currentTimeMillis();
	}

	public Integer getActiveNetworkType() {
		ConnectivityManager connectivity = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity == null) {
			Log.w(DebugFlags.LOGTAG, "couldn't get connectivity manager");
			return null;
		}

		NetworkInfo activeInfo = connectivity.getActiveNetworkInfo();
		if (activeInfo == null) {
			if (DebugFlags.SYSTEM_FACADE) {
				Log.v(DebugFlags.LOGTAG, "network is not available");
			}
			return null;
		}
		return activeInfo.getType();
	}

	public boolean isNetworkRoaming() {
		ConnectivityManager connectivity = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity == null) {
			Log.w(DebugFlags.LOGTAG, "couldn't get connectivity manager");
			return false;
		}

		NetworkInfo info = connectivity.getActiveNetworkInfo();
		boolean isMobile = (info != null && info.getType() == ConnectivityManager.TYPE_MOBILE);
		boolean isRoaming = isMobile && info.isRoaming();
		if (DebugFlags.SYSTEM_FACADE && isRoaming) {
			Log.v(DebugFlags.LOGTAG, "network is roaming");
		}
		return isRoaming;
	}

	public Long getMaxBytesOverMobile() {
		return null;
	}

	public Long getRecommendedMaxBytesOverMobile() {
		return null;
	}
}
