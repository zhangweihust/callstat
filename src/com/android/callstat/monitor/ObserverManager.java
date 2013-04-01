package com.android.callstat.monitor;

import android.content.Context;
import android.database.ContentObserver;
import android.provider.CallLog;

import com.android.callstat.ConfigManager;
import com.android.callstat.ILog;
import com.android.callstat.monitor.bean.Sms;

public class ObserverManager {
	private CallLogObserver callObserver;
	private SmsObserver smsObserver;

	private static ObserverManager om;
	private Context mCtx;
	private ContentChangedNotifier notifier;
	private ConfigManager config;

	// notifier states
	public static final int CALL_LOG_CHANGED = 101;
	public static final int SMS_LOG_CHANGED = 102;
	public static final String CALL_LOG_CHANGED_ACTION = "call_log_changed_action";
	public static final String SMS_LOG_CHANGED_ACTION = "sms_log_changed_action";
	public static final String HAS_GUESS_YE_ACTION = "has_guess_ye_action";
	public static final String CALL_LOG_BEAN = "call_log";
	public static final String SMS_LOG_BEAN = "sms_log";

	private ObserverManager(Context ctx) {
		// Log.i("callstats", "initObserverManager");
		mCtx = ctx;
		notifier = new ContentChangedNotifier(ctx);
		callObserver = new CallLogObserver(ctx, notifier);
		config = new ConfigManager(ctx);
		smsObserver = new SmsObserver(ctx, notifier);
	}

	public static ObserverManager getObserverManager(Context ctx) {
		if (om == null) {
			om = new ObserverManager(ctx);
		}
		return om;
	}

	// initialize observers
	public void registerObservers() {
		ILog.LogI(getClass(), "in registerObservers");
		registerObservers(callObserver, smsObserver);
	}

	public void initLogs() {
		callObserver.onChange(false);
		smsObserver.onChange(false);
	}

	// register observers
	public void registerObservers(ContentObserver... observers) {
		for (ContentObserver observer : observers) {
			if (observer instanceof CallLogObserver) {
				mCtx.getContentResolver().registerContentObserver(
						CallLog.Calls.CONTENT_URI, true, observer);
				continue;

			}
			if (observer instanceof SmsObserver) {
				mCtx.getContentResolver().registerContentObserver(
						Sms.G_CONTENT_URI, true, observer);
			}
		}
	}
}
