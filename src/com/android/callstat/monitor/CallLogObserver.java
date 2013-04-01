package com.android.callstat.monitor;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.callstat.CallStatApplication;
import com.android.callstat.ConfigManager;
import com.android.callstat.ILog;
import com.android.callstat.common.CallStatUtils;
import com.android.callstat.common.database.CallStatDatabase;
import com.android.callstat.common.download.CacheFileManager;
import com.android.callstat.monitor.bean.MonthlyStatDataSource;

public class CallLogObserver extends ContentObserver {
	private ContentChangedNotifier notifier;
	private ConfigManager config;
	private Context mCtx;
	public static final String CALL_LOG_CHANGED_ACTION = "call log changed";
	public static int numType = -1;
	public static int currentDuration = 0;
	private int mDuration;

	CallStatApplication app;
	private ConfigManager configManager;

	// private receiveSolutionHandler ReceiveSolutionHandler;

	public CallLogObserver(Context ctx, Handler handler) {
		super(handler);
		notifier = (ContentChangedNotifier) handler;
		mCtx = ctx;
		config = new ConfigManager(ctx);
		app = (CallStatApplication) mCtx.getApplicationContext();
		configManager = new ConfigManager(ctx);
		// ReceiveSolutionHandler = new receiveSolutionHandler();
	}

	/*
	 * public receiveSolutionHandler getReceiveSolutionHandler(){ return
	 * ReceiveSolutionHandler; }
	 */

	@Override
	public void onChange(boolean selfChange) {
		super.onChange(selfChange);
		Log.i("callstats", "selfChange call收到了有解的消息");
		// get call log cursor
		/*
		 * public static final int INCOMING_TYPE = 1; public static final int
		 * OUTGOING_TYPE = 2; public static final int MISSED_TYPE = 3;
		 */
		Cursor c = null;
		if (!selfChange) {
			try {
				int call_id = configManager.getLastCalllogId();
				if (config.isFirstLaunch()) {
					c = mCtx.getContentResolver().query(
							CallLog.Calls.CONTENT_URI,
							new String[] { Calls._ID, Calls.DATE, Calls.NUMBER,
									Calls.TYPE, Calls.DURATION },
							Calls._ID + "> ? AND " + Calls.DATE + " >= ?",
							new String[] {
									String.valueOf(call_id),
									String.valueOf(CallStatUtils
											.getFirstOfMonthInMillis()) },
							Calls._ID + " ASC");
					// ILog.LogI(this.getClass(), "c.getCount()" +
					// c.getCount());
				} else {
					c = mCtx.getContentResolver().query(
							CallLog.Calls.CONTENT_URI,
							new String[] { Calls._ID, Calls.DATE, Calls.NUMBER,
									Calls.TYPE, Calls.DURATION },
							Calls._ID + "> ? ",
							new String[] { String.valueOf(call_id) },
							Calls._ID + " ASC");
				}
				if (app == null) {
					app = (CallStatApplication) mCtx.getApplicationContext();
				}
				int _id = 0;
				if (c != null && c.getCount() > 0) {
					for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
						int type = c.getInt(c.getColumnIndex(Calls.TYPE));
						// TODO
						String number = c.getString(c
								.getColumnIndex(Calls.NUMBER));
						if (type == Calls.MISSED_TYPE) {
							continue;
						}
						if (CallStatUtils.isFreeCall(mCtx, number)) {
							continue;
						}
						Integer duration = c.getInt(c
								.getColumnIndex(Calls.DURATION));
						if (duration <= 0) {
							continue;
						}
						mDuration = duration;
						if (duration % 60 == 0) {
							duration = duration / 60;
						} else {
							duration = duration / 60 + 1;
						}
						int numberType = -1;
						if (type == Calls.OUTGOING_TYPE) {
							CallStatDatabase db = CallStatDatabase
									.getInstance(mCtx);
							numberType = CallStatUtils.getPhoneNumberType(db,
									config, number);
						} else if (type == Calls.INCOMING_TYPE) {
							numberType = com.android.callstat.monitor.bean.CallLog.CALL_FREE;
						}
						numType = numberType;
						currentDuration = duration;

						if (!config.isFirstLaunch()) {
							CacheFileManager.getInstance().logAccounting(
									"发生有效通话行为：\n" + "本次通话类型：" + numType
											+ "本次通话时长：" + duration);
						}
						if (!config.isFirstLaunch()) {
							new PhonebillCaculateThread(mCtx,
									new ReceiveSolutionHandler(
											mCtx.getMainLooper(), mCtx,
											mDuration)).start();
						}
						String date = c.getString(c.getColumnIndex(Calls.DATE));
						if (c.isLast()) {
							TelephonyManager tm = (TelephonyManager) mCtx
									.getSystemService(Context.TELEPHONY_SERVICE);
							if (tm.isNetworkRoaming()) {
								numberType = com.android.callstat.monitor.bean.CallLog.CALL_ROAMING;
							}
						}
						com.android.callstat.monitor.bean.CallLog call = new com.android.callstat.monitor.bean.CallLog(
								number, date, type, duration, numberType);

						int monthly_stat_data_src_type = MonthlyStatDataSource.UNKNOWN;

						if (numberType == com.android.callstat.monitor.bean.CallLog.CALL_LOCAL) {
							monthly_stat_data_src_type = MonthlyStatDataSource.LOCAL;
						} else if (numberType == com.android.callstat.monitor.bean.CallLog.CALL_LONG_DISTANCE) {
							monthly_stat_data_src_type = MonthlyStatDataSource.LONG;
						} else if (numberType == com.android.callstat.monitor.bean.CallLog.CALL_ROAMING) {
							monthly_stat_data_src_type = MonthlyStatDataSource.ROAMING;
						} else if (numberType == com.android.callstat.monitor.bean.CallLog.CALL_SHORT) {
							monthly_stat_data_src_type = MonthlyStatDataSource.SHORT;
						} else if (numberType == com.android.callstat.monitor.bean.CallLog.CALL_IP) {
							monthly_stat_data_src_type = MonthlyStatDataSource.IP;
						} else if (numberType == com.android.callstat.monitor.bean.CallLog.CALL_UNKONW) {
							monthly_stat_data_src_type = MonthlyStatDataSource.UNKNOWN;
						}
						// zhangjing@archermind.com
						// 此处type传入的参数需要和我自己定义的type类型对应一下，然后传入
						String num_use = number;
						num_use = num_use.replace("+86", "").replace(" ", "");
						MonthlyStatDataSource monthly_stat_rec = CallStatUtils
								.makeMonthlyStatDataSourceRec(date,
										monthly_stat_data_src_type, config,
										num_use, duration);
						// app.sendMonthlyStatSrcToServer(monthly_stat_rec);
						CallStatDatabase db = CallStatDatabase
								.getInstance(mCtx);
						if (db != null) {
							db.createTable(CallStatDatabase.TABLE_CALL_LOG);
							db.addCallLog(call);

							// added by zhangjing@archermind.com
							db.createTable(CallStatDatabase.TABLE_USER_CONSUME_MONTHLY_STATISTIC);// create
																									// table
																									// if
																									// not
																									// exists
							if (numberType != com.android.callstat.monitor.bean.CallLog.CALL_FREE) {
								db.addMonthlyStatDataSource(monthly_stat_rec);
							}
						}
						/*
						 * Message msg = new Message(); msg.obj = call; msg.what
						 * = ObserverManager.CALL_LOG_CHANGED;
						 * notifier.sendMessage(msg);
						 */
					}
					if (c.moveToPrevious()) {
						_id = c.getInt(c.getColumnIndex(Calls._ID));
					}
					if (_id != 0)
						configManager.setLastCalllogId(_id);
				}
			} catch (Exception e) {
				ILog.logException(this.getClass(), e);
			} finally {
				if (c != null)
					c.close();
			}
			// 将此时话费余额记录下来，保存为上次事件发生时刻的话费余额
			if (configManager.getFeesRemian() != 100000) {
				configManager.setLastEventFeeAvail(configManager
						.getFeesRemian());
			}
			// 将此时已用的Gprs流量记录下来，保存为上次事件发生时刻的已用Gprs流量
			if (configManager.getTotalGprsUsedDifference() != 0) {
				configManager.setLastEventTrafficUsed(configManager
						.getTotalGprsUsed()
						+ configManager.getTotalGprsUsedDifference());
			}
		}
	}

	/*
	 * private String getFeeShown(double AccountBalance) { DecimalFormat df =
	 * new DecimalFormat("#.##"); return df.format(AccountBalance); }
	 */

	// 在对UI进行更新时，执行时所在的线程为主UI线程
	/*
	 * class receiveSolutionHandler extends
	 * Handler{//继承Handler类时，必须重写handleMessage方法 public
	 * receiveSolutionHandler(){} public receiveSolutionHandler(Looper lo){
	 * super(lo); }
	 * 
	 * @Override public void handleMessage(Message msg) {
	 * super.handleMessage(msg); Bundle b = msg.getData();//Obtains a Bundle of
	 * arbitrary data associated with this event String haveSolutionFlag =
	 * b.getString("haveSolutionFlag");
	 * if(haveSolutionFlag.equalsIgnoreCase("null")){ //如果收到了方程组为空的消息，则做...
	 * Toast.makeText(mCtx, "AM提示：本次通话时长：" + getTimeShown(mDuration),
	 * Toast.LENGTH_LONG).show(); return; }
	 * 
	 * if(haveSolutionFlag.equalsIgnoreCase("true")){ //如果收到了有解的消息，则做...
	 * Log.i("callstats", "收到了有解的消息"); Toast.makeText(mCtx, "AM提示：本次通话时长：" +
	 * getTimeShown(mDuration) + "\n" + "预计话费余额为：" +
	 * PhonebillCaculateThread.AccountBalance + "元", Toast.LENGTH_LONG).show();
	 * }else{ //如果收到了无解的消息，则做... Log.i("callstats", "收到了无解的消息");
	 * Toast.makeText(mCtx, "AM提示：本次通话时长：" + getTimeShown(mDuration),
	 * Toast.LENGTH_LONG).show(); }
	 * 
	 * } }
	 */
}
