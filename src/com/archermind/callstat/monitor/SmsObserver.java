package com.archermind.callstat.monitor;

import java.util.HashMap;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.archermind.callstat.CallStatApplication;
import com.archermind.callstat.ConfigManager;
import com.archermind.callstat.ILog;
import com.archermind.callstat.SmsReceiver;
import com.archermind.callstat.accounting.MessageManager;
import com.archermind.callstat.accounting.ReconciliationUtils;
import com.archermind.callstat.common.CallStatUtils;
import com.archermind.callstat.common.database.CallStatDatabase;
import com.archermind.callstat.monitor.bean.MonthlyStatDataSource;
import com.archermind.callstat.monitor.bean.Sms;
import com.archermind.callstat.monitor.bean.SmsLog;

public class SmsObserver extends ContentObserver {
	private ContentChangedNotifier notifier;
	private Context mCtx;
	private ConfigManager config;
	private Handler myHandler;
	private static long sentTime = 1L;
	private static final int CHANGED_LATER = 0x001;

	CallStatApplication app;
	private ConfigManager configManager;

	public SmsObserver(Context ctx, Handler handler) {
		super(handler);
		notifier = (ContentChangedNotifier) handler;
		mCtx = ctx;
		config = new ConfigManager(ctx);
		app = (CallStatApplication) mCtx.getApplicationContext();
		configManager = new ConfigManager(ctx);
		myHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				if (msg.what == CHANGED_LATER)
					smsOnChangeProc();
			}

		};
	}

	@Override
	public void onChange(boolean selfChange) {
		super.onChange(selfChange);
		// get sms log cursor
		Log.i("callstats", "selfChange sms收到了有解的消息");
		if (!selfChange) {
			Log.i("callstats", "selfChange sms收到了有解的消息  isFirstLaunch()");
			smsOnChangeProc();

		}
	}

	private void smsOnChangeProc() {
		Cursor c = null;
		try {
			int sms_id = configManager.getLastSmslogId();
			Log.i("callstats", "selfChange sms_id:" + sms_id);
			if (config.isFirstLaunch()) {
				c = mCtx.getContentResolver().query(
						Sms.G_CONTENT_URI,
						null,
						Sms._ID + "> ? AND " + Sms.DATE + " >= ? ",
						new String[] {
								String.valueOf(sms_id),
								String.valueOf(CallStatUtils
										.getFirstOfMonthInMillis()) },
						Sms._ID + " ASC");
			} else {
				c = mCtx.getContentResolver().query(Sms.G_CONTENT_URI, null,
						Sms._ID + "> ? ",
						new String[] { String.valueOf(sms_id) },
						Sms._ID + " ASC");
			}
			if (app == null) {
				app = (CallStatApplication) mCtx.getApplicationContext();
			}
			int _id = 0;
			if (c != null && c.getCount() > 0) {
				Log.i("callstats", "selfChange c.getCount():" + c.getCount());
				for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
					// first of all we have to confirm that if current
					// message was sent
					int type = c.getInt(c.getColumnIndex(Sms.TYPE));
					Log.i("callstats", "type = :" + type);
					String number = c.getString(c.getColumnIndex(Sms.ADDRESS));
					String msg_body = c.getString(c.getColumnIndex(Sms.BODY));

					if (type != Sms.MESSAGE_TYPE_SENT) {
						Log.i("callstats", "type != Sms.MESSAGE_TYPE_SENT:");
						if (!config.isFirstLaunch()) {
							if (type == Sms.MESSAGE_TYPE_INBOX) {
								String num = config.getOperatorNum();
								if (num.contains(number)) {
									ILog.LogE(this.getClass(), "number="
											+ number + "  msg_body=" + msg_body);
									if (ReconciliationUtils.IsCheckingAccount
											|| ReconciliationUtils.IsCheckingTraffic) {
										ILog.LogE(this.getClass(), "进入收件箱匹配短信");
										HashMap<Integer, String> has = MessageManager
												.accounting_proc(
														msg_body,
														config,
														CallStatApplication.Keywordslist,
														SmsReceiver.ACCOUNTING_FROM_INBOX);
										if (has.size() != 0) {
											ILog.LogE(this.getClass(),
													"收件箱匹配成功向service发消息");
											SmsReceiver.sendAccountInfo2Ui(has);
										}
									}

								}
							}
						}
						continue;
					}

					if (CallStatUtils.isFreeCall(mCtx, number)) {
						Log.i("callstats",
								"CallStatUtils.isFreeCall(mCtx, number)");
						continue;
					}

					long date = c.getLong(c.getColumnIndex(Sms.DATE));
					if (sentTime == date) {
						Log.i("callstats", "sentTime == date");
						continue;
					}
					sentTime = date;
					config.setTotalSmsSent(config.getTotalSmsSent() + 1);
					int protocol = c.getInt(c.getColumnIndex(Sms.PROTOCOL));
					Log.e("i", "PROTOCOL:" + protocol);
					SmsLog sms = new SmsLog(number, date, protocol);
					String num_use = number;
					num_use = num_use.replace("+86", "").replace(" ", "");

					MonthlyStatDataSource monthly_stat_rec = CallStatUtils
							.makeMonthlyStatDataSourceRec(String.valueOf(date),
									MonthlyStatDataSource.SMS, config, num_use,
									1);

					CallStatDatabase database = CallStatDatabase
							.getInstance(mCtx);
					if (database != null) {
						database.createTable(CallStatDatabase.TABLE_SMS_LOG);
						database.addSmsLog(sms);

						database.createTable(CallStatDatabase.TABLE_USER_CONSUME_MONTHLY_STATISTIC);// create
																									// table
																									// if
																									// not
																									// exists
						database.addMonthlyStatDataSource(monthly_stat_rec);
					}
					/*
					 * Message msg = new Message(); msg.obj = sms; msg.what =
					 * ObserverManager.SMS_LOG_CHANGED;
					 * notifier.sendMessage(msg);
					 */
				}
				if (c.moveToPrevious()) {
					if (!config.isFirstLaunch()) {
						int type = c.getInt(c.getColumnIndex(Sms.TYPE));
						if (type != Sms.MESSAGE_TYPE_SENT) {
							if (type == Sms.MESSAGE_TYPE_INBOX) {
								_id = c.getInt(c.getColumnIndex(Sms._ID));
								if (_id != 0)
									configManager.setLastSmslogId(_id);
								Log.i("callstats",
										"selfChange setLastSmslogId:" + _id);
							}
							return;
						}
					}
					_id = c.getInt(c.getColumnIndex(Sms._ID));
				}
				if (_id != 0)
					configManager.setLastSmslogId(_id);
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		} finally {
			if (c != null)
				c.close();
		}
		// 将此时话费余额记录下来，保存为上次事件发生时刻的话费余额
		if (configManager.getFeesRemian() != 100000) {
			configManager.setLastEventFeeAvail(configManager.getFeesRemian());
		}
		// 将此时已用的Gprs流量记录下来，保存为上次事件发生时刻的已用Gprs流量
		if (configManager.getTotalGprsUsedDifference() != 0) {
			configManager.setLastEventTrafficUsed(configManager
					.getTotalGprsUsed()
					+ configManager.getTotalGprsUsedDifference());
		}
	}

}
