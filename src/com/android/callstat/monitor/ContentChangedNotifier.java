package com.android.callstat.monitor;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import com.android.callstat.common.database.CallStatDatabase;
import com.android.callstat.monitor.bean.CallLog;
import com.android.callstat.monitor.bean.SmsLog;

public class ContentChangedNotifier extends Handler {
	private Context mCtx;

	public ContentChangedNotifier(Context ctx) {
		mCtx = ctx;
	}

	@Override
	public void handleMessage(Message msg) {
		super.handleMessage(msg);
		int what = msg.what;
		CallStatDatabase database = CallStatDatabase.getInstance(mCtx);
		switch (what) {
		case ObserverManager.CALL_LOG_CHANGED:
			Intent call_intent = new Intent(
					ObserverManager.CALL_LOG_CHANGED_ACTION);
			CallLog call = (CallLog) msg.obj;

			if (call.getType() == 1) {

			}
			if (database != null) {
				database.createTable(CallStatDatabase.TABLE_CALL_LOG);
				database.addCallLog(call);
			}
			/*
			 * new AddCallLogThread().execute(call); Bundle bundle = new
			 * Bundle(); bundle.putSerializable(ObserverManager.CALL_LOG_BEAN,
			 * call); call_intent.putExtras(bundle);
			 * call_intent.setAction(ObserverManager.CALL_LOG_CHANGED_ACTION);
			 * mCtx.sendBroadcast(call_intent);
			 */
			break;
		case ObserverManager.SMS_LOG_CHANGED:
			SmsLog sms = (SmsLog) msg.obj;
			if (database != null) {
				database.createTable(CallStatDatabase.TABLE_SMS_LOG);
				database.addSmsLog(sms);
			}
			break;
		default:
			break;
		}
	}

	class AddCallLogThread extends AsyncTask<Object, Object, Void> {

		@Override
		protected Void doInBackground(Object... params) {
			try {
				CallStatDatabase database = CallStatDatabase.getInstance(mCtx);
				if (database != null) {
					CallLog call = (CallLog) params[0];
					database.createTable(CallStatDatabase.TABLE_CALL_LOG);
					database.addCallLog(call);
				}
			} catch (Exception e) {
				// Log.i("i", getClass().getName() + ": " + e.getMessage() +
				// "\n"
				// + e.getCause().getMessage());
			}
			return null;
		}

	}

	class AddSmsLogThread extends AsyncTask<Object, Object, Void> {

		@Override
		protected Void doInBackground(Object... params) {
			try {
				// Log.i("callstats", "AddSmsLogThread");
				CallStatDatabase database = CallStatDatabase.getInstance(mCtx);
				if (database != null) {
					// Log.i("callstats", "AddSmsLogThread  database != null");
					SmsLog sms = (SmsLog) params[0];
					// Log.i("callstats", sms.getNumber());
					database.createTable(CallStatDatabase.TABLE_SMS_LOG);
					database.addSmsLog(sms);
				}
			} catch (Exception e) {
				// Log.i("i", getClass().getName() + ": " + e.getMessage() +
				// "\n"
				// + e.getCause().getMessage());
			}
			return null;
		}

	}

}
