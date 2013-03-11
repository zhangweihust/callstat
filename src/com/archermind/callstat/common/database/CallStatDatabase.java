package com.archermind.callstat.common.database;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.provider.CallLog.Calls;
import android.util.Log;
import android.widget.Toast;

import com.archermind.callstat.CallStatApplication;
import com.archermind.callstat.ConfigManager;
import com.archermind.callstat.ILog;
import com.archermind.callstat.accounting.AccountingKeyWordsBean;
import com.archermind.callstat.accounting.ReconciliationInfo;
import com.archermind.callstat.common.CallStatUtils;
import com.archermind.callstat.common.DeviceUtils;
import com.archermind.callstat.common.MTrafficStats;
import com.archermind.callstat.common.PhoneNumberToArea;
import com.archermind.callstat.common.download.CacheFileManager;
import com.archermind.callstat.firewall.FirewallCoreWorker;
import com.archermind.callstat.firewall.bean.NewTrafficDetail;
import com.archermind.callstat.home.bean.PhoneNumberInfo;
import com.archermind.callstat.home.views.ToastFactory;
import com.archermind.callstat.monitor.bean.CallLog;
import com.archermind.callstat.monitor.bean.MonthlyStatDataSource;
import com.archermind.callstat.monitor.bean.SmsLog;

public class CallStatDatabase {
	// log tag
	protected static final String LOGTAG = "callstats";
	public static final String DATABASE_FILE = "CallStatDatabase.db";
	public static final String DATABASE_FILE_ON_SDCARD = "/.callstatDatabase/CallStatDatabase.db";
	public static final String PHONENUMBER_INFO_FILE = "/data/data/com.archermind.callstat/databases/phoneArea.dat";
	// public static final String RECONCILIATION_DATABASE_FILE =
	// "ReconciliationDatabase.db";
	public static final String TRAFFIC_DETAIL_TYPE_WIFI = "type_wifi";
	public static final String TRAFFIC_DETAIL_TYPE_GPRS = "type_gprs";

	public static final String TELPHONE_DATABASE_FILE = "telPhone.db";
	private static final int DATABASE_VERSION = 8;
	private static final int CREATE_TABLE_DEFAULT = 100;

	private static CallStatDatabase mInstance = null;
	private static SQLiteDatabase mDatabase = null;
	private CacheFileManager cacheFileManager;

	// synchronize locks
	private final byte[] mCallLock = new byte[0];
	private final byte[] mSmsLock = new byte[0];
	// private final byte[] mTrafficLock = new byte[0];
	private final byte[] mTrafficDetailLock = new byte[0];
	private final byte[] mtelPhoneLock = new byte[0];
	private final byte[] mReconciliationInfoLock = new byte[0];
	private final byte[] mUserMonthlyStatDataSourceLock = new byte[0];
	private final byte[] mSharedPreLock = new byte[0];
	private final byte[] mActivityStatistic = new byte[0];

	public static final String mTableNames[] = { "t_call_log", "t_sms_log",
			"t_traffic_log", "t_accounting_code", "t_number_from_where",
			"t_traffic_detail", "t_reconciliation_code",
			"t_reconciliation_info", "t_PhoneNumberInfo",
			"t_PhoneNumberCityInfo", "t_each_day_user_consume_info",
			"t_traffic_details_with_accurate_date_uid",
			"t_table_for_user_consume_monthly_statistic", "t_sharedpreference",
			"t_activity_statistic" };

	// Table ids (they are index to mTableNames)
	public static final int TABLE_CALL_LOG = 0;
	public static final int TABLE_SMS_LOG = 1;
	public static final int TABLE_TRAFFIC_LOG = 2;
	public static final int TABLE_ACCOUNTING_CODE = 3;
	public static final int TABLE_NUMBER_FROM_WHERE = 4;
	// 用户统计
	public static final int TABLE_TRAFFIC_DETAIL = 5;

	public static final int TABLE_RECONCILIATION_CODE = 6;

	public static final int TABLE_RECONCILIATION_INFO = 7;

	public static final int TABLE_PHONE_NUMBER_INFO = 8;

	public static final int TABLE_PHONE_NUMBER_CITY_INFO = 9;

	public static final int TABLE_EACH_DAY_USER_CONSUME_INFO = 10;// 用户每天的消费情况表

	public static final int TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID = 11;// 用户消费的流量（包含详细的时间区间和区分Gprs、Wifi）详细统计信息

	public static final int TABLE_USER_CONSUME_MONTHLY_STATISTIC = 12;// 用户月消费行为统计数据来源表

	public static final int TABLE_SHAREDPREFERENCE = 13;// 用户月消费行为统计数据来源表

	public static final int TABLE_ACTIVITY_STATISTIC = 14;// 用户月消费行为统计数据来源表

	// column id strings for "_id" which can be used by any table
	private static final String ID_COL = "_id";

	private static final String[] ID_PROJECTION = new String[] { "_id" };

	private static final String NUMBER_COL_TELPHONE = "number";

	private static final String INFO_COL_TELPHONE = "info";

	public static final String PROVINCE_COl_TEL_PHONE_CITY = "province";

	public static final String CITY_COL_TEL_PHONE_CITY = "city";

	public static final String PROVINCE_ID_COL_TEL_PHONE_CITY = "province_id";

	public static final String CITY_ID_COL_TEL_PHONE_CITY = "city_id";

	// column id strings for "t_call_log" table

	// TYPE_COL_CALL_LOG = "type" defines:
	// 1 for incoming calls
	// 2 for outgoing calls
	// 3 for missed calls,but we don't save missed calls,so just ignore this
	private static final String TYPE_COL_CALL_LOG = "type";

	private static final String TIME_COL_CALL_LOG = "time";

	private static final String DURATION_COL_CALL_LOG = "duration";

	private static final String NUMBER_COL_CALL_LOG = "number";

	private static final String NUMBER_TYPE_CALL_LOG = "number_type";

	// column id strings for "t_sms_log" table

	// TYPE_COL_SMS_LOG = "protocol" defines:
	// 0 for sms
	// 1 for mms
	private static final String TYPE_COL_SMS_LOG = "protocol";

	private static final String TIME_COL_SMS_LOG = "time";

	private static final String NUMBER_COL_SMS_LOG = "number";

	/**
	 * column id strings for "t_activity_statistic" table
	 */
	private static final String ACTIVITY_NAME = "activity_name";

	private static final String ACTIVITY_VERSION = "version_of_statistic";

	private static final String DISPLAY_TIMES = "display_times";

	// column id strings for "t_traffic_detail" table
	public static final String TIME_COL_TRAFFIC_DETAIL = "time";

	public static final String UID_COL_TRAFFIC_DETAIL = "uid";

	public static final String PACKAGE_NAME_COL_TRAFFIC_DETAIL = "package_name";

	public static final String REJECTED_COL_TRAFFIC_DETAIL = "rejected";

	public static final String APP_NAME_COL_TRAFFIC_DETAIL = "app_name";

	public static final String NODE_UPLOAD_COL_TRAFFIC_DETAIL = "node_upload";

	public static final String NODE_DOWLOAD_COL_TRAFFIC_DETAIL = "node_download";

	// public static final String ISUNINSTALLED_COL_TRAFFIC_DETAIL =
	// public static final String TYPE_COL_TRAFFIC_DETAIL = "type";

	public static final String UPLOAD_COL_TRAFFIC_DETAIL = "upload";

	public static final String DOWNLOAD_COL_TRAFFIC_DETAIL = "download";

	// column id strings for "t_accounting_code" table
	public static final String OPERATOR_COL_ACCOUNTING_CODE = "operator";

	public static final String BRAND_COL_ACCOUNTING_CODE = "brand";

	public static final String PROVINCE_COL_ACCOUNTING_CODE = "province";

	public static final String CITY_COL_ACCOUNTING_CODE = "city";

	public static final String TYPE_COL_ACCOUNTING_CODE = "type";

	public static final String CODE_COL_ACCOUNTING_CODE = "code";

	// column id String for "t_number_from_where"
	public static String NUMBER_COL_NUMBER_FROM_WHERE = "number";

	public static String PROVICE_COL_NUMBER_FROM_WHERE = "province";

	public static String CITY_COL_NUMBER_FROM_WHERE = "city";

	public static String OPERATOR_COL_NUMBER_FROM_WHERE = "operator";

	public static String AREA_CODE_COL_NUMBER_FROM_WHERE = "area_code";

	// column id String for "t_missmatched_msg"
	public static String NUMBER_COL_MISSMATCHED_MSG = "number";

	public static String PROVINCE_COL_MISSMATCHED_MSG = "province";

	public static String CITY_COL_MISSMATCHED_MSG = "city";

	public static String OPERATOR_COL_MISSMATCHED_MSG = "operator";

	public static String CODE_COL_MISSMATCHED_MSG = "code";

	public static String MESSAGE_COL_MISSMATCHED_MSG = "message";

	// column id String for "t_reconciliation_code"
	public static final String TYPE_COL_RECONCILIATION_CODE = "type";

	public static final String PROVINCE_COL_RECONCILIATION_CODE = "province";

	public static final String CITY_COL_RECONCILIATION_CODE = "city";

	public static final String MNO_COL_RECONCILIATION_CODE = "mno";

	public static final String BRAND_COL_RECONCILIATION_CODE = "brand";

	public static final String NUMBER_COL_RECONCILIATION_CODE = "number";

	public static final String MESSAGE_COL_RECONCILIATION_CODE = "message";

	public static final String KEY_COL_RECONCILIATION_CODE = "key";

	// column id String for "t_reconciliation_info"

	public static final String TIME_COL_RECONCILIATION_INFO = "time";

	public static final String DIFFERENCE_COL_RECONCILIATION_INFO = "difference";

	public static final String THIS_LOCALITY_DIALING_TIMES_COL_RECONCILIATION_INFO = "this_locality_dialing_times";

	public static final String LONG_DISTANCE_DIALING_TIMES_COL_RECONCILIATION_INFO = "long_distance_dialing_times";

	// public static final String CALLED_TIMES_COL_RECONCILIATION_INFO =
	// "called_times";
	public static final String ROAMING_TIMES_COL_RECONCILIATION_INFO = "roaming_times";

	public static final String UNKONW_DIALING_TIMES_COL_RECONCILIATION_INFO = "unkonw_dialing_times";

	public static final String IP_DIALING_TIMES_COL_RECONCILIATION_INFO = "ip_dialing_times";

	public static final String SHORT_DIALING_TIMES_COL_RECONCILIATION_INFO = "short_dialing_times";

	public static final String SEND_SMS_NUM_COL_RECONCILIATION_INFO = "send_sms_num";

	public static final String TRAFFIC_DATA_COL_RECONCILIATION_INFO = "traffic_data";

	// added by zhangjing to add serveral deltas
	public static final String DELTA1_COL_RECONCILIATION_INFO = "delta1";
	public static final String DELTA2_COL_RECONCILIATION_INFO = "delta2";
	public static final String DELTA3_COL_RECONCILIATION_INFO = "delta3";
	public static final String DELTA4_COL_RECONCILIATION_INFO = "delta4";
	public static final String DELTA5_COL_RECONCILIATION_INFO = "delta5";
	public static final String DELTA6_COL_RECONCILIATION_INFO = "delta6";

	// column id String for "t_each_day_user_consume_info"
	public static final String IMSI_COL_EACH_DAY_USER_CONSUME_INFO = "imsi";
	public static final String EXPENCE_COL_EACH_DAY_USER_CONSUME_INFO = "expence";
	public static final String GPRS_COL_EACH_DAY_USER_CONSUME_INFO = "gprs";

	// column iString for "t_traffic_details_with_accurate_date_uid"
	public static final String DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID = "date";
	public static final String UID_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID = "uid";
	public static final String APPNAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID = "appName";
	public static final String PACKAGENAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID = "packageName";
	public static final String GPRS_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID = "gprs_rejected";
	public static final String WIFI_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID = "wifi_rejected";
	public static final String GPRS_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID = "gprs_upload";
	public static final String GPRS_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID = "gprs_download";
	public static final String WIFI_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID = "wifi_upload";
	public static final String WIFI_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID = "wifi_download";
	public static final String NODE_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID = "node_upload";
	public static final String NODE_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID = "node_download";
	public static final String WIFI_ON_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID = "wifi_on_state";

	// added by zhangjing 为新增加的表格（用户月消费行为统计数据来源表）增加列名
	public static final String TIME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC = "time";
	public static final String PROVINCE_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC = "province";
	public static final String CITY_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC = "city";
	public static final String MNO_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC = "mno";
	public static final String BRAND_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC = "brand";
	public static final String TYPE_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC = "type";
	public static final String NUMBER_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC = "number";
	public static final String NAME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC = "name";
	public static final String DURATION_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC = "duration";
	public static final String USED_GPRS_TO_FIRST_DAY_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC = "used_gprs_to_first_day";
	public static final String USED_GPRS_TO_LAST_EVENT_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC = "used_gprs_to_last_event";
	public static final String USED_FEE_TO_FIRST_DAY_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC = "used_fee_to_first_day";
	public static final String USED_FEE_TO_LAST_EVENT_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC = "used_fee_to_last_event";
	public static final String RATES_FOR_LOCAL_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC = "rates_for_local";
	public static final String RATES_FOR_LONG_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC = "rates_for_long";
	public static final String RATES_FOR_ROAMING_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC = "rates_for_roaming";
	public static final String RATES_FOR_IP_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC = "rates_for_ip_dial";
	public static final String RATES_FOR_SHORT_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC = "rates_for_short";
	public static final String RATES_FOR_TRAFFIC_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC = "rates_for_traffic";
	public static final String WLAN_USED_TO_FIRST_DAY_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC = "wlan_used_to_first_day";
	public static final String WLAN_USED_TO_LAST_EVENT_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC = "wlan_used_to_last_event";
	public static final String RATES_FOR_WLAN_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC = "rates_for_wlan";
	public static final String ALREADY_UPLOAD_FLAG_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC = "upload_flag";

	// columns for t_sharedprefernece
	// KEY_COL_SHAREDPREFERENCE sharedpreference 's key
	// VALUE_COL_SHAREDPREFERENCE value of sharedpreference 's key
	private static final String KEY_COL_SHAREDPREFERENCE = "key";
	private static final String TYPE_COL_SHAREDPREFERENCE = "type";
	private static final String VALUE_COL_SHAREDPREFERENCE = "value";

	private static Context mContext;
	private static boolean mMustRefresh = false;

	private CallStatDatabase() {
		// Singleton only, use getInstance()
	}

	public static synchronized void refreshDB(Context context) {
		Log.d("wc", "refreshDB");
		mMustRefresh = true;
	}

	public static synchronized CallStatDatabase getInstance(Context context) {
		mContext = context;
		String path = "/data/data/com.archermind.callstat/databases/"
				+ DATABASE_FILE;
		if (mInstance == null || mMustRefresh) {
			mMustRefresh = false;
			if (mDatabase != null) {
				mDatabase.close();
				mDatabase = null;
			}

			Log.d("wc", "dbgetInstance");
			mInstance = new CallStatDatabase();
			try {
				if (CallStatUtils.isDatabaseFileExist(path)) {
					Log.d("wc", "DDDB存在");
					// DDDB存在
					ILog.LogI(CallStatDatabase.class,
							"isDatabaseFileExist:true");
					int count = 0;
					ConfigManager config = new ConfigManager(mContext);
					while (!Environment.getExternalStorageState().equals(
							Environment.MEDIA_MOUNTED)
							&& config.isBootComplete()) {
						count++;
						ILog.LogI(CallStatDatabase.class, "count:" + count);
						if (count >= 20) {
							config.setBootComplete(false);
							break;
						}
						Thread.sleep(1000);
					}
					if (Environment.getExternalStorageState().equals(
							Environment.MEDIA_MOUNTED)) {
						// SD卡存在
						Log.d("wc", "SD卡存在");
						ILog.LogI(CallStatDatabase.class,
								"Environment.MEDIA_MOUNTED");
						// 是否有root权限
						if (FirewallCoreWorker.hasRootAccess()) {
							ILog.LogI(CallStatDatabase.class,
									"hasRootAccess:true");
							// 改变data下database文件权限
							FirewallCoreWorker.modifyFilePermission(path);
							// 将data下的数据库文件copy到sd卡下
							File sdCardDir = Environment
									.getExternalStorageDirectory();
							String dbPathOnSdcard = sdCardDir
									.getCanonicalPath()
									+ DATABASE_FILE_ON_SDCARD;
							ILog.LogI(CallStatDatabase.class, "dbPathOnSdcard:"
									+ dbPathOnSdcard);
							File dbDir = new File(dbPathOnSdcard)
									.getParentFile();
							if (!dbDir.exists()) {
								dbDir.mkdirs();
							}

							if (!CallStatUtils
									.isDatabaseFileExist(dbPathOnSdcard)) {
								Log.d("wc",
										"SDDB不存在，copyDDDB=>SDDB,删除DDDB，返回SDDB");
								// SDDB不存在，copyDDDB=>SDDB,删除DDDB，返回SDDB
								if (CallStatUtils
										.CopyFile(path, dbPathOnSdcard)) {
									ILog.LogI(CallStatDatabase.class,
											"CopyFile:successfull. and prepare to open database on:"
													+ DATABASE_FILE_ON_SDCARD);
									if (mDatabase == null) {
										mDatabase = SQLiteDatabase
												.openOrCreateDatabase(
														dbPathOnSdcard, null);
									} else {
										if (!mDatabase.isOpen()) {
											mDatabase = SQLiteDatabase
													.openOrCreateDatabase(
															dbPathOnSdcard,
															null);
										}
									}
								} else {
									ILog.LogI(CallStatDatabase.class,
											"prepare to open database on:"
													+ DATABASE_FILE);
									if (mDatabase == null) {
										mDatabase = context
												.openOrCreateDatabase(
														DATABASE_FILE, 0, null);
									} else {
										if (!mDatabase.isOpen()) {
											mDatabase = context
													.openOrCreateDatabase(
															DATABASE_FILE, 0,
															null);
										}
									}
								}
							} else {
								// SDDB存在，合并sddb和dddb,删除DDDB，返回sddb
								Log.d("wc", "SDDB存在，合并sddb和dddb,删除DDDB，返回sddb");
								CallStatUtils.UnionFile(path, dbPathOnSdcard);
								ILog.LogI(CallStatDatabase.class,
										"UnionFile:successfull. and prepare to open database on:"
												+ DATABASE_FILE_ON_SDCARD);
								if (mDatabase == null) {
									mDatabase = SQLiteDatabase
											.openOrCreateDatabase(
													dbPathOnSdcard, null);
								} else {
									if (!mDatabase.isOpen()) {
										mDatabase = SQLiteDatabase
												.openOrCreateDatabase(
														dbPathOnSdcard, null);
									}
								}
							}

						} else {
							// 如果没有root，则直接在data目录下创建打开数据库
							ILog.LogI(CallStatDatabase.class,
									"isDatabaseFileExist:true");
							ILog.LogI(CallStatDatabase.class,
									"prepare to open database on:"
											+ DATABASE_FILE);
							if (mDatabase == null) {
								mDatabase = context.openOrCreateDatabase(
										DATABASE_FILE, 0, null);
							} else {
								if (!mDatabase.isOpen()) {
									mDatabase = context.openOrCreateDatabase(
											DATABASE_FILE, 0, null);
								}
							}
						}
					} else {
						// SD卡不存在，则直接在data目录下创建打开数据库
						Log.d("wc", "SD卡不存在，则直接在data目录下创建打开数据库");
						ILog.LogI(CallStatDatabase.class,
								"prepare to open database on:" + DATABASE_FILE);
						if (mDatabase == null) {
							mDatabase = context.openOrCreateDatabase(
									DATABASE_FILE, 0, null);
						} else {
							if (!mDatabase.isOpen()) {
								mDatabase = context.openOrCreateDatabase(
										DATABASE_FILE, 0, null);
							}
						}
					}
				} else {
					// DDDB不存在
					Log.d("wc", "DDDB不存在");
					ILog.LogI(
							CallStatDatabase.class,
							"isDatabaseFileExist:false and time:"
									+ System.currentTimeMillis());
					int count = 0;
					ConfigManager config = new ConfigManager(mContext);
					while (!Environment.getExternalStorageState().equals(
							Environment.MEDIA_MOUNTED)
							&& config.isBootComplete()) {
						count++;
						ILog.LogI(CallStatDatabase.class,
								"time:" + System.currentTimeMillis() + "count:"
										+ count);
						if (count >= 20) {
							config.setBootComplete(false);
							break;
						}
						Thread.sleep(1000);
					}
					if (Environment.getExternalStorageState().equals(
							Environment.MEDIA_MOUNTED)) {
						ILog.LogI(CallStatDatabase.class,
								"Environment.MEDIA_MOUNTED");
						// sd存在卡，则在sd卡上创建，并打开数据库
						Log.d("wc", "sd存在卡，则在sd卡上创建，并打开数据库");
						File sdCardDir = Environment
								.getExternalStorageDirectory();
						String dbPathOnSdcard = sdCardDir.getCanonicalPath()
								+ DATABASE_FILE_ON_SDCARD;
						ILog.LogI(CallStatDatabase.class, "dbPathOnSdcard:"
								+ dbPathOnSdcard);
						File dbDir = new File(dbPathOnSdcard).getParentFile();
						if (!dbDir.exists()) {
							dbDir.mkdirs();
						}
						ILog.LogI(CallStatDatabase.class,
								"prepare to open database on:"
										+ DATABASE_FILE_ON_SDCARD);
						if (mDatabase == null) {
							mDatabase = SQLiteDatabase.openOrCreateDatabase(
									dbPathOnSdcard, null);
						} else {
							if (!mDatabase.isOpen()) {
								mDatabase = SQLiteDatabase
										.openOrCreateDatabase(dbPathOnSdcard,
												null);
							}
						}
					} else {
						Log.d("wc", "SD卡不存在，则直接在data目录下创建打开数据库");
						// SD卡不存在，则直接在data目录下创建打开数据库
						ILog.LogI(CallStatDatabase.class,
								"no old version db file and no sdcard");
						ILog.LogI(CallStatDatabase.class,
								"prepare to open database on:" + DATABASE_FILE);
						if (mDatabase == null) {
							mDatabase = context.openOrCreateDatabase(
									DATABASE_FILE, 0, null);
						} else {
							if (!mDatabase.isOpen()) {
								mDatabase = context.openOrCreateDatabase(
										DATABASE_FILE, 0, null);
							}
						}
					}
				}

			} catch (SQLiteException e) {
				// try again by deleting the old db and create a new one
				ILog.LogI(CallStatDatabase.class,
						"prepare to open database on:" + DATABASE_FILE);
				if (mDatabase == null) {
					mDatabase = context.openOrCreateDatabase(DATABASE_FILE, 0,
							null);
				} else {
					if (!mDatabase.isOpen()) {
						mDatabase = context.openOrCreateDatabase(DATABASE_FILE,
								0, null);
					}
				}
				ILog.logException(CallStatDatabase.class, e);
			} catch (IOException e) {
				ILog.logException(CallStatDatabase.class, e);
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// mDatabase should not be null,
			// the only case is RequestAPI test has problem to create db
			// Log.i(LOGTAG, " version: " + mDatabase.getVersion()
			// + " DATABASE_VERSION: " + DATABASE_VERSION);
			if (mDatabase != null/* && mDatabase.getVersion() != DATABASE_VERSION */) {
				mDatabase.beginTransaction();
				try {
					upgradeDatabase();
					mDatabase.setTransactionSuccessful();
				} catch (Exception e) {
					ILog.logException(CallStatDatabase.class, e);
				} finally {
					if (mDatabase.inTransaction()) {
						mDatabase.endTransaction();
					}
				}
			}

			if (mDatabase != null) {
				// use per table Mutex lock, turn off database lock, this
				// improves performance as database's ReentrantLock is expansive
				mDatabase.setLockingEnabled(false);
			}
		}
		return mInstance;
	}

	private static void upgradeDatabase() {
		/*
		 * int oldVersion = mDatabase.getVersion(); if (oldVersion == 2 ||
		 * oldVersion == 3) {
		 * createNewColTable(TABLE_EACH_DAY_USER_CONSUME_INFO); } //
		 * database版本为6时，后加入对帐信息表新字段，先删除旧表 else if (oldVersion == 6) {
		 * createNewColTable(TABLE_RECONCILIATION_INFO); } else {
		 */
		createTable();
		// }
		mDatabase.setVersion(DATABASE_VERSION); // original should be
	}

	/**
	 * 表结构发生变换，需要先删除记录
	 */
	private static void createNewColTable(int value) {
		switch (value) {
		case TABLE_EACH_DAY_USER_CONSUME_INFO:
			mDatabase.execSQL("CREATE TABLE IF NOT EXISTS "
					+ mTableNames[TABLE_EACH_DAY_USER_CONSUME_INFO] + " ("
					+ ID_COL + " DATE PRIMARY KEY, "
					+ IMSI_COL_EACH_DAY_USER_CONSUME_INFO + " TEXT, "
					+ EXPENCE_COL_EACH_DAY_USER_CONSUME_INFO + " INTEGER, "
					+ GPRS_COL_EACH_DAY_USER_CONSUME_INFO + " INTEGER );");
			java.util.Date utilDate = new java.util.Date();
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
			String NowDate = formatter.format(utilDate);
			restoreHistoryDayConsumeData(NowDate);
			Log.e("my", "restoreHistoryDayConsumeData successful");
			break;
		case TABLE_RECONCILIATION_INFO:

			mDatabase.execSQL("DROP TABLE " + " IF EXISTS "
					+ mTableNames[TABLE_RECONCILIATION_INFO]);

			mDatabase.execSQL("CREATE TABLE IF NOT EXISTS "
					+ mTableNames[TABLE_RECONCILIATION_INFO] + " (" + ID_COL
					+ " INTEGER PRIMARY KEY, " + TIME_COL_RECONCILIATION_INFO
					+ " INTEGER, " + DIFFERENCE_COL_RECONCILIATION_INFO
					+ " TEXT, "
					+ THIS_LOCALITY_DIALING_TIMES_COL_RECONCILIATION_INFO
					+ " INTEGER, "
					+ LONG_DISTANCE_DIALING_TIMES_COL_RECONCILIATION_INFO
					+ " INTEGER, " + ROAMING_TIMES_COL_RECONCILIATION_INFO
					+ " INTEGER, "
					+ UNKONW_DIALING_TIMES_COL_RECONCILIATION_INFO
					+ " INTEGER, " + IP_DIALING_TIMES_COL_RECONCILIATION_INFO
					+ " INTEGER, "
					+ SHORT_DIALING_TIMES_COL_RECONCILIATION_INFO
					+ " INTEGER, " + SEND_SMS_NUM_COL_RECONCILIATION_INFO
					+ " INTEGER, " + TRAFFIC_DATA_COL_RECONCILIATION_INFO
					+ " TEXT, " + DELTA1_COL_RECONCILIATION_INFO + " INTEGER, "
					+ DELTA2_COL_RECONCILIATION_INFO + " INTEGER, "
					+ DELTA3_COL_RECONCILIATION_INFO + " INTEGER, "
					+ DELTA4_COL_RECONCILIATION_INFO + " INTEGER, "
					+ DELTA5_COL_RECONCILIATION_INFO + " INTEGER, "
					+ DELTA6_COL_RECONCILIATION_INFO + " INTEGER );");
			break;
		default:
			break;
		}
	}

	/**
	 * this is for create
	 * */

	public void createTable(int tableId) {
		switch (tableId) {
		case CREATE_TABLE_DEFAULT:

			// TABLE_CALL_LOG
			mDatabase.execSQL("CREATE TABLE IF NOT EXISTS "
					+ mTableNames[TABLE_CALL_LOG] + " (" + ID_COL
					+ " INTEGER PRIMARY KEY, " + TYPE_COL_CALL_LOG
					+ " INTEGER, " + TIME_COL_CALL_LOG + " INTEGER, "
					+ DURATION_COL_CALL_LOG + " INTEGER, "
					+ NUMBER_TYPE_CALL_LOG + " INTEGER, " + NUMBER_COL_CALL_LOG
					+ " TEXT " + ");");

			// TABLE_SMS_LOG
			mDatabase.execSQL("CREATE TABLE IF NOT EXISTS "
					+ mTableNames[TABLE_SMS_LOG] + " (" + ID_COL
					+ " INTEGER PRIMARY KEY, " + TYPE_COL_SMS_LOG
					+ " INTEGER, " + TIME_COL_SMS_LOG + " INTEGER, "
					+ NUMBER_COL_SMS_LOG + " TEXT" + " );");

			// TABLE_TRAFFIC_DETAIL
			mDatabase.execSQL("CREATE TABLE IF NOT EXISTS "
					+ mTableNames[TABLE_TRAFFIC_DETAIL] + " ("
					+ TIME_COL_TRAFFIC_DETAIL + " TEXT, "
					+ APP_NAME_COL_TRAFFIC_DETAIL + " TEXT, "
					+ UID_COL_TRAFFIC_DETAIL + " INTEGER PRIMARY KEY, "
					+ PACKAGE_NAME_COL_TRAFFIC_DETAIL + " TEXT, "
					+ REJECTED_COL_TRAFFIC_DETAIL + " NUMERIC, "
					+ UPLOAD_COL_TRAFFIC_DETAIL + " INTEGER , "
					+ DOWNLOAD_COL_TRAFFIC_DETAIL + " INTEGER , "
					+ NODE_UPLOAD_COL_TRAFFIC_DETAIL + " INTEGER, "
					+ NODE_DOWLOAD_COL_TRAFFIC_DETAIL + " INTEGER " + " );");

			// TABLE_ACCOUNTING_CODE
			mDatabase.execSQL("CREATE TABLE IF NOT EXISTS "
					+ mTableNames[TABLE_ACCOUNTING_CODE] + " (" + ID_COL
					+ " INTEGER PRIMARY KEY, " + OPERATOR_COL_ACCOUNTING_CODE
					+ " TEXT, " + BRAND_COL_ACCOUNTING_CODE + " TEXT, "
					+ PROVINCE_COL_ACCOUNTING_CODE + " TEXT, "
					+ CITY_COL_ACCOUNTING_CODE + " TEXT, "
					+ TYPE_COL_ACCOUNTING_CODE + " TEXT, "
					+ CODE_COL_ACCOUNTING_CODE + " TEXT " + " );");

			// TABLE_RECONCILIATION_INFO
			mDatabase.execSQL("CREATE TABLE IF NOT EXISTS "
					+ mTableNames[TABLE_RECONCILIATION_INFO] + " (" + ID_COL
					+ " INTEGER PRIMARY KEY, " + TIME_COL_RECONCILIATION_INFO
					+ " INTEGER, " + DIFFERENCE_COL_RECONCILIATION_INFO
					+ " TEXT, "
					+ THIS_LOCALITY_DIALING_TIMES_COL_RECONCILIATION_INFO
					+ " INTEGER, "
					+ LONG_DISTANCE_DIALING_TIMES_COL_RECONCILIATION_INFO
					+ " INTEGER, " + ROAMING_TIMES_COL_RECONCILIATION_INFO
					+ " INTEGER, "
					+ UNKONW_DIALING_TIMES_COL_RECONCILIATION_INFO
					+ " INTEGER, " + IP_DIALING_TIMES_COL_RECONCILIATION_INFO
					+ " INTEGER, "
					+ SHORT_DIALING_TIMES_COL_RECONCILIATION_INFO
					+ " INTEGER, " + SEND_SMS_NUM_COL_RECONCILIATION_INFO
					+ " INTEGER, " + TRAFFIC_DATA_COL_RECONCILIATION_INFO
					+ " TEXT, " + DELTA1_COL_RECONCILIATION_INFO + " INTEGER, "
					+ DELTA2_COL_RECONCILIATION_INFO + " INTEGER, "
					+ DELTA3_COL_RECONCILIATION_INFO + " INTEGER, "
					+ DELTA4_COL_RECONCILIATION_INFO + " INTEGER, "
					+ DELTA5_COL_RECONCILIATION_INFO + " INTEGER, "
					+ DELTA6_COL_RECONCILIATION_INFO + " INTEGER );");

			mDatabase.execSQL("CREATE TABLE  IF NOT EXISTS "
					+ mTableNames[TABLE_EACH_DAY_USER_CONSUME_INFO] + " ("
					+ ID_COL + " DATE PRIMARY KEY, "
					+ IMSI_COL_EACH_DAY_USER_CONSUME_INFO + " TEXT, "
					+ EXPENCE_COL_EACH_DAY_USER_CONSUME_INFO + " INTEGER, "
					+ GPRS_COL_EACH_DAY_USER_CONSUME_INFO + " INTEGER );");
			break;
		case TABLE_CALL_LOG:
			// TABLE_CALL_LOG
			mDatabase.execSQL("CREATE TABLE IF NOT EXISTS "
					+ mTableNames[TABLE_CALL_LOG] + " (" + ID_COL
					+ " INTEGER PRIMARY KEY, " + TYPE_COL_CALL_LOG
					+ " INTEGER, " + TIME_COL_CALL_LOG + " INTEGER, "
					+ DURATION_COL_CALL_LOG + " INTEGER, "
					+ NUMBER_TYPE_CALL_LOG + " INTEGER, " + NUMBER_COL_CALL_LOG
					+ " TEXT " + ");");
			break;
		case TABLE_SMS_LOG:
			// TABLE_SMS_LOG
			mDatabase.execSQL("CREATE TABLE IF NOT EXISTS "
					+ mTableNames[TABLE_SMS_LOG] + " (" + ID_COL
					+ " INTEGER PRIMARY KEY, " + TYPE_COL_SMS_LOG
					+ " INTEGER, " + TIME_COL_SMS_LOG + " INTEGER, "
					+ NUMBER_COL_SMS_LOG + " TEXT" + " );");
			break;
		case TABLE_ACTIVITY_STATISTIC:
			mDatabase.execSQL("CREATE TABLE IF NOT EXISTS  "
					+ mTableNames[TABLE_ACTIVITY_STATISTIC] + " ("
					+ ACTIVITY_NAME + " TEXT, " + ACTIVITY_VERSION + " TEXT, "
					+ DISPLAY_TIMES + " INTEGER, " + " PRIMARY KEY " + "("
					+ ACTIVITY_NAME + "," + ACTIVITY_VERSION + ")" + " );");
			break;
		case TABLE_ACCOUNTING_CODE:
			mDatabase.execSQL("CREATE TABLE IF NOT EXISTS "
					+ mTableNames[TABLE_ACCOUNTING_CODE] + " (" + ID_COL
					+ " INTEGER PRIMARY KEY, " + OPERATOR_COL_ACCOUNTING_CODE
					+ " TEXT, " + BRAND_COL_ACCOUNTING_CODE + " TEXT, "
					+ PROVINCE_COL_ACCOUNTING_CODE + " TEXT, "
					+ CITY_COL_ACCOUNTING_CODE + " TEXT, "
					+ TYPE_COL_ACCOUNTING_CODE + " TEXT, "
					+ CODE_COL_ACCOUNTING_CODE + " TEXT " + " );");
			break;
		case TABLE_RECONCILIATION_INFO:
			mDatabase.execSQL("CREATE TABLE IF NOT EXISTS "
					+ mTableNames[TABLE_RECONCILIATION_INFO] + " (" + ID_COL
					+ " INTEGER PRIMARY KEY, " + TIME_COL_RECONCILIATION_INFO
					+ " INTEGER, " + DIFFERENCE_COL_RECONCILIATION_INFO
					+ " TEXT, "
					+ THIS_LOCALITY_DIALING_TIMES_COL_RECONCILIATION_INFO
					+ " INTEGER, "
					+ LONG_DISTANCE_DIALING_TIMES_COL_RECONCILIATION_INFO
					+ " INTEGER, " + ROAMING_TIMES_COL_RECONCILIATION_INFO
					+ " INTEGER, "
					+ UNKONW_DIALING_TIMES_COL_RECONCILIATION_INFO
					+ " INTEGER, " + IP_DIALING_TIMES_COL_RECONCILIATION_INFO
					+ " INTEGER, "
					+ SHORT_DIALING_TIMES_COL_RECONCILIATION_INFO
					+ " INTEGER, " + SEND_SMS_NUM_COL_RECONCILIATION_INFO
					+ " INTEGER, " + TRAFFIC_DATA_COL_RECONCILIATION_INFO
					+ " TEXT, " + DELTA1_COL_RECONCILIATION_INFO + " INTEGER, "
					+ DELTA2_COL_RECONCILIATION_INFO + " INTEGER, "
					+ DELTA3_COL_RECONCILIATION_INFO + " INTEGER, "
					+ DELTA4_COL_RECONCILIATION_INFO + " INTEGER, "
					+ DELTA5_COL_RECONCILIATION_INFO + " INTEGER, "
					+ DELTA6_COL_RECONCILIATION_INFO + " INTEGER );");
			break;
		case TABLE_EACH_DAY_USER_CONSUME_INFO:
			mDatabase.execSQL("CREATE TABLE IF NOT EXISTS  "
					+ mTableNames[TABLE_EACH_DAY_USER_CONSUME_INFO] + " ("
					+ ID_COL + " DATE PRIMARY KEY, "
					+ IMSI_COL_EACH_DAY_USER_CONSUME_INFO + " TEXT, "
					+ EXPENCE_COL_EACH_DAY_USER_CONSUME_INFO + " INTEGER, "
					+ GPRS_COL_EACH_DAY_USER_CONSUME_INFO + " INTEGER );");
			break;
		case TABLE_TRAFFIC_DETAIL:
			// TABLE_TRAFFIC_DETAIL
			mDatabase.execSQL("CREATE TABLE IF NOT EXISTS "
					+ mTableNames[TABLE_TRAFFIC_DETAIL] + " ("
					+ TIME_COL_TRAFFIC_DETAIL + " TEXT, "
					+ APP_NAME_COL_TRAFFIC_DETAIL + " TEXT, "
					+ UID_COL_TRAFFIC_DETAIL + " INTEGER PRIMARY KEY, "
					+ PACKAGE_NAME_COL_TRAFFIC_DETAIL + " TEXT, "
					+ REJECTED_COL_TRAFFIC_DETAIL + " NUMERIC, "
					+ UPLOAD_COL_TRAFFIC_DETAIL + " INTEGER , "
					+ DOWNLOAD_COL_TRAFFIC_DETAIL + " INTEGER , "
					+ NODE_UPLOAD_COL_TRAFFIC_DETAIL + " INTEGER, "
					+ NODE_DOWLOAD_COL_TRAFFIC_DETAIL + " INTEGER" + " );");
			break;

		// added by zhangjing
		case TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID:
			mDatabase.execSQL("CREATE TABLE IF NOT EXISTS "
					+ mTableNames[TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID]
					+ " (" + DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
					+ " DATE, "
					+ UID_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
					+ " INTEGER, "
					+ APPNAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
					+ " TEXT, "
					+ PACKAGENAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
					+ " TEXT, "
					+ GPRS_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
					+ " NUMERIC, "
					+ WIFI_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
					+ " NUMERIC, "
					+ GPRS_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
					+ " INTEGER, "
					+ GPRS_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
					+ " INTEGER, "
					+ WIFI_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
					+ " INTEGER, "
					+ WIFI_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
					+ " INTEGER, "
					+ NODE_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
					+ " INTEGER, "
					+ NODE_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
					+ " INTEGER, "
					+ WIFI_ON_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
					+ " NUMERIC, " + " PRIMARY KEY " + "("
					+ DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID + ","
					+ UID_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID + ")"
					+ " );");
			break;
		// added by zhangjing 加了一张表（为了用户月消费行为的统计）
		case TABLE_USER_CONSUME_MONTHLY_STATISTIC:
			mDatabase
					.execSQL("CREATE TABLE IF NOT EXISTS "
							+ mTableNames[TABLE_USER_CONSUME_MONTHLY_STATISTIC]
							+ " ("
							+ ID_COL
							+ " TEXT PRIMARY KEY, "
							+ TIME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
							+ " TEXT, "
							+ PROVINCE_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
							+ " TEXT, "
							+ CITY_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
							+ " TEXT, "
							+ MNO_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
							+ " TEXT, "
							+ BRAND_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
							+ " TEXT, "
							+ TYPE_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
							+ " INTEGER, "
							+ NUMBER_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
							+ " TEXT, "
							+ NAME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
							+ " TEXT, "
							+ DURATION_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
							+ " INTEGER, "
							+ USED_GPRS_TO_FIRST_DAY_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
							+ " INTEGER, "
							+ USED_GPRS_TO_LAST_EVENT_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
							+ " INTEGER, "
							+ USED_FEE_TO_FIRST_DAY_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
							+ " INTEGER, "
							+ USED_FEE_TO_LAST_EVENT_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
							+ " INTEGER, "
							+ RATES_FOR_LOCAL_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
							+ " INTEGER, "
							+ RATES_FOR_LONG_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
							+ " INTEGER, "
							+ RATES_FOR_ROAMING_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
							+ " INTEGER, "
							+ RATES_FOR_IP_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
							+ " INTEGER, "
							+ RATES_FOR_SHORT_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
							+ " INTEGER, "
							+ RATES_FOR_TRAFFIC_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
							+ " INTEGER, "
							+ WLAN_USED_TO_FIRST_DAY_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
							+ " INTEGER, "
							+ WLAN_USED_TO_LAST_EVENT_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
							+ " INTEGER, "
							+ RATES_FOR_WLAN_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
							+ " INTEGER, "
							+ ALREADY_UPLOAD_FLAG_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
							+ " NUMERIC" + " );");
			break;

		case TABLE_SHAREDPREFERENCE:
			mDatabase.execSQL("CREATE TABLE IF NOT EXISTS "
					+ mTableNames[TABLE_SHAREDPREFERENCE] + " ("
					+ KEY_COL_SHAREDPREFERENCE + " TEXT PRIMARY KEY, "
					+ " TEXT, " + TYPE_COL_SHAREDPREFERENCE + " TEXT, "
					+ VALUE_COL_SHAREDPREFERENCE + " TEXT);");
			break;
		default:
			break;
		}
	}

	/**
	 * this is for update
	 */
	private static void createTable() {

		// TABLE_CALL_LOG
		mDatabase.execSQL("CREATE TABLE IF NOT EXISTS  "
				+ mTableNames[TABLE_CALL_LOG] + " (" + ID_COL
				+ " INTEGER PRIMARY KEY, " + TYPE_COL_CALL_LOG + " INTEGER, "
				+ TIME_COL_CALL_LOG + " INTEGER, " + DURATION_COL_CALL_LOG
				+ " INTEGER, " + NUMBER_TYPE_CALL_LOG + " INTEGER, "
				+ NUMBER_COL_CALL_LOG + " TEXT " + ");");

		// TABLE_SMS_LOG
		mDatabase.execSQL("CREATE TABLE IF NOT EXISTS  "
				+ mTableNames[TABLE_SMS_LOG] + " (" + ID_COL
				+ " INTEGER PRIMARY KEY, " + TYPE_COL_SMS_LOG + " INTEGER, "
				+ TIME_COL_SMS_LOG + " INTEGER, " + NUMBER_COL_SMS_LOG
				+ " TEXT " + " );");

		// TABLE_ACTIVITY_STATISTIC
		mDatabase.execSQL(" CREATE TABLE IF NOT EXISTS  "
				+ mTableNames[TABLE_ACTIVITY_STATISTIC] + " (" + ACTIVITY_NAME
				+ " TEXT, " + ACTIVITY_VERSION + " TEXT, " + DISPLAY_TIMES
				+ " INTEGER," + " PRIMARY KEY " + "(" + ACTIVITY_NAME + ","
				+ ACTIVITY_VERSION + ")" + " ); ");

		mDatabase.execSQL("CREATE TABLE IF NOT EXISTS "
				+ mTableNames[TABLE_ACCOUNTING_CODE] + " (" + ID_COL
				+ " INTEGER PRIMARY KEY, " + OPERATOR_COL_ACCOUNTING_CODE
				+ " TEXT, " + BRAND_COL_ACCOUNTING_CODE + " TEXT, "
				+ PROVINCE_COL_ACCOUNTING_CODE + " TEXT, "
				+ CITY_COL_ACCOUNTING_CODE + " TEXT, "
				+ TYPE_COL_ACCOUNTING_CODE + " TEXT, "
				+ CODE_COL_ACCOUNTING_CODE + " TEXT" + " );");

		// TABLE_RECONCILIATION_INFO
		// modyfied by zhangjing@archermind.com
		mDatabase.execSQL("CREATE TABLE IF NOT EXISTS "
				+ mTableNames[TABLE_RECONCILIATION_INFO] + " (" + ID_COL
				+ " INTEGER PRIMARY KEY, " + TIME_COL_RECONCILIATION_INFO
				+ " INTEGER, " + DIFFERENCE_COL_RECONCILIATION_INFO + " TEXT, "
				+ THIS_LOCALITY_DIALING_TIMES_COL_RECONCILIATION_INFO
				+ " INTEGER, "
				+ LONG_DISTANCE_DIALING_TIMES_COL_RECONCILIATION_INFO
				+ " INTEGER, " + ROAMING_TIMES_COL_RECONCILIATION_INFO
				+ " INTEGER, " + UNKONW_DIALING_TIMES_COL_RECONCILIATION_INFO
				+ " INTEGER, " + IP_DIALING_TIMES_COL_RECONCILIATION_INFO
				+ " INTEGER, " + SHORT_DIALING_TIMES_COL_RECONCILIATION_INFO
				+ " INTEGER, " + SEND_SMS_NUM_COL_RECONCILIATION_INFO
				+ " INTEGER, " + TRAFFIC_DATA_COL_RECONCILIATION_INFO
				+ " TEXT, " + DELTA1_COL_RECONCILIATION_INFO + " INTEGER, "
				+ DELTA2_COL_RECONCILIATION_INFO + " INTEGER, "
				+ DELTA3_COL_RECONCILIATION_INFO + " INTEGER, "
				+ DELTA4_COL_RECONCILIATION_INFO + " INTEGER, "
				+ DELTA5_COL_RECONCILIATION_INFO + " INTEGER, "
				+ DELTA6_COL_RECONCILIATION_INFO + " INTEGER );");

		mDatabase.execSQL("CREATE TABLE IF NOT EXISTS "
				+ mTableNames[TABLE_EACH_DAY_USER_CONSUME_INFO] + " (" + ID_COL
				+ " DATE PRIMARY KEY, " + IMSI_COL_EACH_DAY_USER_CONSUME_INFO
				+ " TEXT, " + EXPENCE_COL_EACH_DAY_USER_CONSUME_INFO
				+ " INTEGER, " + GPRS_COL_EACH_DAY_USER_CONSUME_INFO
				+ " INTEGER );");

		// added by zhangjing
		mDatabase.execSQL("CREATE TABLE IF NOT EXISTS "
				+ mTableNames[TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID]
				+ " (" + DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
				+ " DATE, " + UID_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
				+ " INTEGER, "
				+ APPNAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
				+ " TEXT, "
				+ PACKAGENAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
				+ " TEXT, "
				+ GPRS_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
				+ " NUMERIC, "
				+ WIFI_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
				+ " NUMERIC, "
				+ GPRS_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
				+ " INTEGER, "
				+ GPRS_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
				+ " INTEGER, "
				+ WIFI_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
				+ " INTEGER, "
				+ WIFI_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
				+ " INTEGER, "
				+ NODE_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
				+ " INTEGER, "
				+ NODE_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
				+ " INTEGER, "
				+ WIFI_ON_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
				+ " NUMERIC, " + " PRIMARY KEY " + "("
				+ DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID + ","
				+ UID_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID + ")" + " );");

		mDatabase
				.execSQL("CREATE TABLE IF NOT EXISTS "
						+ mTableNames[TABLE_USER_CONSUME_MONTHLY_STATISTIC]
						+ " ("
						+ ID_COL
						+ " TEXT PRIMARY KEY, "
						+ TIME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
						+ " TEXT, "
						+ PROVINCE_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
						+ " TEXT, "
						+ CITY_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
						+ " TEXT, "
						+ MNO_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
						+ " TEXT, "
						+ BRAND_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
						+ " TEXT, "
						+ TYPE_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
						+ " INTEGER, "
						+ NUMBER_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
						+ " TEXT, "
						+ NAME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
						+ " TEXT, "
						+ DURATION_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
						+ " INTEGER, "
						+ USED_GPRS_TO_FIRST_DAY_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
						+ " INTEGER, "
						+ USED_GPRS_TO_LAST_EVENT_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
						+ " INTEGER, "
						+ USED_FEE_TO_FIRST_DAY_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
						+ " INTEGER, "
						+ USED_FEE_TO_LAST_EVENT_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
						+ " INTEGER, "
						+ RATES_FOR_LOCAL_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
						+ " INTEGER, "
						+ RATES_FOR_LONG_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
						+ " INTEGER, "
						+ RATES_FOR_ROAMING_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
						+ " INTEGER, "
						+ RATES_FOR_IP_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
						+ " INTEGER, "
						+ RATES_FOR_SHORT_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
						+ " INTEGER, "
						+ RATES_FOR_TRAFFIC_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
						+ " INTEGER, "
						+ WLAN_USED_TO_FIRST_DAY_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
						+ " INTEGER, "
						+ WLAN_USED_TO_LAST_EVENT_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
						+ " INTEGER, "
						+ RATES_FOR_WLAN_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
						+ " INTEGER, "
						+ ALREADY_UPLOAD_FLAG_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
						+ " NUMERIC" + " );");

		mDatabase.execSQL("CREATE TABLE IF NOT EXISTS "
				+ mTableNames[TABLE_SHAREDPREFERENCE] + " ("
				+ KEY_COL_SHAREDPREFERENCE + " TEXT PRIMARY KEY, "
				+ TYPE_COL_SHAREDPREFERENCE + " TEXT, "
				+ VALUE_COL_SHAREDPREFERENCE + " TEXT);");
	}

	private boolean hasEntries(int tableId) {
		if (mDatabase == null) {
			return false;
		}

		Cursor cursor = null;
		boolean ret = false;
		try {
			cursor = mDatabase.query(mTableNames[tableId], ID_PROJECTION, null,
					null, null, null, null);
			ret = cursor.moveToFirst() == true;
		} catch (IllegalStateException e) {
			Log.e(LOGTAG, "hasEntries", e);
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return ret;
	}

	/**
	 * 根据传递过来的电话号码获取归属地信息
	 * 
	 * @param allNumber
	 *            传过来的电话号码
	 * @return 返回的查询到的归属地信息对象
	 */
	public PhoneNumberInfo getPhoneNumberInfo5All(String allNumber) {
		if ("".equals(allNumber) || allNumber == null) {
			return null;
		}
		// Log.i("my", "allNumber=" + allNumber);
		StringBuffer sb = new StringBuffer(allNumber);
		if (sb.charAt(0) == '+') {
			sb.deleteCharAt(0);
		}

		// 86
		if (sb.charAt(0) == '8' && sb.charAt(1) == '6') {
			sb.delete(0, 2);
		}
		if (sb.toString().startsWith("17951")
				|| sb.toString().startsWith("12593")
				|| sb.toString().startsWith("17911")
				|| sb.toString().startsWith("10193")
				|| sb.toString().startsWith("17909")
				|| sb.toString().startsWith("10901")) {
			sb = sb.delete(0, 5);
			if (sb.charAt(0) == '0') {
				sb.deleteCharAt(0);
				if (sb.length() > 7) {
					sb.delete(7, sb.length());

				}
				PhoneNumberInfo dial1 = getPhoneNumberInfo(sb.toString());
				if (dial1 != null) {
					dial1.addIP();
					// Log.i("my", "dial.getOperator()=" + dial1.getOperator());
					return dial1;
				}
				// 首先按照4位区号查询，若查询为空，再按3位区号查询
				if (sb.length() >= 3) {
					sb.delete(3, sb.length());
				}

				PhoneNumberInfo dial = getPhoneNumberInfo(sb.toString());

				if (dial != null) {
					dial.addIP();
					// Log.i("my", "dial.getOperator()=" + dial.getOperator());
					return dial;
				}

				if (sb.length() >= 2) {
					sb.delete(2, sb.length());
				}
				dial = getPhoneNumberInfo(sb.toString());

				if (dial != null) {
					dial.addIP();
					// Log.i("my", "dial.getOperator()=" + dial.getOperator());
					return dial;
				}
			} else if (sb.charAt(0) == '1') {
				if (sb.length() > 7) {
					sb.delete(7, sb.length());

				}
				PhoneNumberInfo dial = getPhoneNumberInfo(sb.toString());
				if (dial != null) {
					dial.addIP();
					// Log.i("my", "dial.getOperator()=" + dial.getOperator());
					return dial;
				}
			}
		}
		if (sb.charAt(0) == '0') {
			sb.deleteCharAt(0);
			if (sb.length() > 7) {
				sb.delete(7, sb.length());

			}
			PhoneNumberInfo dial1 = getPhoneNumberInfo(sb.toString());
			if (dial1 != null) {
				return dial1;
			}
			// 首先按照4位区号查询，若查询为空，再按3位区号查询
			if (sb.length() >= 3) {
				sb.delete(3, sb.length());
			}

			PhoneNumberInfo dial = getPhoneNumberInfo(sb.toString());

			if (dial != null) {
				// Log.i("my", "dial.getOperator()=" + dial.getOperator());
				return dial;
			}

			if (sb.length() >= 2) {
				sb.delete(2, sb.length());
			}
		}
		// 以1开头，是手机号或者服务行业号码
		else if (sb.charAt(0) == '1') {
			if (sb.length() > 7) {
				sb.delete(7, sb.length());

			}
		}
		// Log.i("my", "phoneNumber=" + sb.toString());
		return getPhoneNumberInfo(sb.toString());

	}

	private PhoneNumberInfo getPhoneNumberInfo(String number) {
		Log.i("my", "number---" + number);
		Cursor cursor = null;
		Cursor cursor2 = null;
		SQLiteDatabase phoneNubemrInfoDatabase = null;
		PhoneNumberInfo phoneNumberInfo = null;
		String telInfo = null;
		synchronized (mtelPhoneLock) {
			try {
				phoneNubemrInfoDatabase = mContext.openOrCreateDatabase(
						TELPHONE_DATABASE_FILE, 0, null);
				if (number.length() != 7) {
					String where = NUMBER_COL_TELPHONE + " = ?";
					cursor = phoneNubemrInfoDatabase.query(
							mTableNames[TABLE_PHONE_NUMBER_INFO],
							new String[] { INFO_COL_TELPHONE }, where,
							new String[] { number }, null, null, null);
					if (cursor.moveToFirst()) {
						telInfo = String.valueOf(cursor.getInt(cursor
								.getColumnIndex(INFO_COL_TELPHONE)));
					}
					Log.i("my", "telInfo444444=" + telInfo);
				} else {
					telInfo = String.valueOf(PhoneNumberToArea.getAreaCode(
							PHONENUMBER_INFO_FILE, number));
					Log.i("my", "telInfo7777=" + telInfo);
				}

				if (telInfo.length() == 5) {
					String operator = null;
					switch (Integer.parseInt(telInfo.substring(0, 1))) {
					case 1:
						operator = "中国移动";
						break;
					case 2:
						operator = "中国联通";
						break;
					case 3:
						operator = "中国电信";
						break;
					case 4:
						operator = "固话";
						break;
					}
					cursor2 = phoneNubemrInfoDatabase
							.query(mTableNames[TABLE_PHONE_NUMBER_CITY_INFO],
									new String[] { PROVINCE_COl_TEL_PHONE_CITY,
											CITY_COL_TEL_PHONE_CITY },
									PROVINCE_ID_COL_TEL_PHONE_CITY + " = ? "
											+ " AND "
											+ CITY_ID_COL_TEL_PHONE_CITY
											+ " = ?",
									new String[] { telInfo.substring(1, 3),
											telInfo.substring(3, 5) }, null,
									null, null);
					if (cursor2.moveToFirst()) {

						phoneNumberInfo = new PhoneNumberInfo(
								number,
								cursor2.getString(cursor2
										.getColumnIndex(PROVINCE_COl_TEL_PHONE_CITY)),
								cursor2.getString(cursor2
										.getColumnIndex(CITY_COL_TEL_PHONE_CITY)),
								operator);
					}
				}
			} catch (Exception e) {
				// TODO: handle exception
				Log.i("my", "Exception:" + e.getMessage());
			} finally {
				if (cursor != null) {
					cursor.close();
					// Log.i("my", "关闭数据库游标");
				}
				if (cursor2 != null) {
					cursor2.close();
				}
				if (phoneNubemrInfoDatabase != null) {
					phoneNubemrInfoDatabase.close();
					// Log.i("my", "关闭数据库");
				}
			}
		}
		return phoneNumberInfo;
	}

	// RecociliationInfo functions
	/**
	 * 添加对账信息方程组
	 */
	public void addReconciliationInfo(ReconciliationInfo reconciliationInfo) {
		if (reconciliationInfo == null) {
			return;
		}
		Cursor cursor = null;
		synchronized (mReconciliationInfoLock) {
			try {
				createTable(TABLE_RECONCILIATION_INFO);
				ContentValues newValues = new ContentValues();
				newValues.put(TIME_COL_RECONCILIATION_INFO,
						reconciliationInfo.getTime());
				newValues.put(DIFFERENCE_COL_RECONCILIATION_INFO,
						String.valueOf(reconciliationInfo.getDifference()));
				newValues.put(
						THIS_LOCALITY_DIALING_TIMES_COL_RECONCILIATION_INFO,
						reconciliationInfo.getThisLocalityDialingTimes());
				newValues.put(
						LONG_DISTANCE_DIALING_TIMES_COL_RECONCILIATION_INFO,
						reconciliationInfo.getLongDistanceDialingTimes());
				newValues.put(ROAMING_TIMES_COL_RECONCILIATION_INFO,
						reconciliationInfo.getRoamingTimes());
				newValues.put(UNKONW_DIALING_TIMES_COL_RECONCILIATION_INFO,
						reconciliationInfo.getUnkonwDialingTimes());
				newValues.put(IP_DIALING_TIMES_COL_RECONCILIATION_INFO,
						reconciliationInfo.getIPDialingTimes());
				newValues.put(SHORT_DIALING_TIMES_COL_RECONCILIATION_INFO,
						reconciliationInfo.getShortDialingTimes());
				newValues.put(SEND_SMS_NUM_COL_RECONCILIATION_INFO,
						reconciliationInfo.getSendSmsNum());
				newValues.put(TRAFFIC_DATA_COL_RECONCILIATION_INFO,
						String.valueOf(reconciliationInfo.getTrafficData()));
				newValues.put(DELTA1_COL_RECONCILIATION_INFO,
						reconciliationInfo.getCoeffDelta1());
				newValues.put(DELTA2_COL_RECONCILIATION_INFO,
						reconciliationInfo.getCoeffDelta2());
				newValues.put(DELTA3_COL_RECONCILIATION_INFO,
						reconciliationInfo.getCoeffDelta3());
				newValues.put(DELTA4_COL_RECONCILIATION_INFO,
						reconciliationInfo.getCoeffDelta4());
				newValues.put(DELTA5_COL_RECONCILIATION_INFO,
						reconciliationInfo.getCoeffDelta5());
				newValues.put(DELTA6_COL_RECONCILIATION_INFO,
						reconciliationInfo.getCoeffDelta6());
				mDatabase.insert(mTableNames[TABLE_RECONCILIATION_INFO], null,
						newValues);

				String orderBy = TIME_COL_RECONCILIATION_INFO + " desc ";
				cursor = mDatabase.query(
						mTableNames[TABLE_RECONCILIATION_INFO], null, null,
						null, null, null, orderBy);
				if (cursor.getCount() > 400) {
					cursor.moveToLast();
					mDatabase
							.delete(mTableNames[TABLE_RECONCILIATION_INFO],
									TIME_COL_RECONCILIATION_INFO + " = ? ",
									new String[] { cursor.getString(cursor
											.getColumnIndex(TIME_COL_RECONCILIATION_INFO)) });
				}
			} catch (Exception e) {
				// TODO: handle exception
				ILog.logException(this.getClass(), e);
			} finally {
				if (cursor != null) {
					cursor.close();
				}
			}
		}
		new refreshEachDayAllConsumeThread(reconciliationInfo).start();
	}

	public class refreshEachDayAllConsumeThread extends Thread {
		ReconciliationInfo recon;

		refreshEachDayAllConsumeThread(ReconciliationInfo reconInfo) {
			recon = reconInfo;
		}

		public void run() {
			CallStatDatabase.getInstance(mContext).refreshEachDayAllConsume(
					recon);
		}
	}

	public void refreshEachDayAllConsume(ReconciliationInfo reconciliationInfo) {
		try {

			if (mDatabase == null) {
				return;
			}

			java.util.Date utilDate = new java.util.Date();
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
			String NowDate = formatter.format(utilDate);

			String sql = " select " + GPRS_COL_EACH_DAY_USER_CONSUME_INFO
					+ " from  " + mTableNames[TABLE_EACH_DAY_USER_CONSUME_INFO]
					+ " where " + ID_COL + " = ?";

			Cursor c = null;

			c = mDatabase.rawQuery(sql, new String[] { NowDate });

			long today_gprs_used = 0l;

			if (c != null && c.moveToFirst()) {
				today_gprs_used = c.getLong(0);
			}

			ConfigManager conManager = new ConfigManager(mContext);
			ContentValues newValues = new ContentValues();
			newValues.put(ID_COL, NowDate);
			newValues.put(IMSI_COL_EACH_DAY_USER_CONSUME_INFO,
					DeviceUtils.getIMSI(mContext));
			float fee_Accumulation = 0f;
			String orderBy = ID_COL + " desc ";
			Cursor cursor = mDatabase.query(
					mTableNames[TABLE_EACH_DAY_USER_CONSUME_INFO], null, ID_COL
							+ "=" + NowDate, null, null, null, orderBy);
			if (cursor != null && cursor.getCount() != 0) {
				if (cursor.moveToFirst()) {
					fee_Accumulation = cursor
							.getFloat(cursor
									.getColumnIndex(EXPENCE_COL_EACH_DAY_USER_CONSUME_INFO));
					conManager.setTodayAlreayConsumedFee(fee_Accumulation);
				}
			}
			cursor.close();
			if (conManager.getEarliestDailyAvailFee() != 100000f
					&& conManager.getCalculateFeeAvailable() != 100000f) {
				float fee_each_day_use = conManager.getEarliestDailyAvailFee()
						- conManager.getCalculateFeeAvailable();
				/*
				 * if(fee_Accumulation>fee_each_day_use){ fee_each_day_use +=
				 * fee_Accumulation; }
				 */
				newValues.put(EXPENCE_COL_EACH_DAY_USER_CONSUME_INFO,
						fee_each_day_use/*
										 * reconciliationInfo . getDifference
										 * ()+ fee_Accumulation
										 */);
			} else {
				newValues.put(EXPENCE_COL_EACH_DAY_USER_CONSUME_INFO,
						fee_Accumulation);
			}

			long traffic_each_day_use = conManager.getTotalGprsUsed()
					- conManager.getEarliestGprsLog();

			/*
			 * if (today_gprs_used >= traffic_each_day_use) {
			 * traffic_each_day_use += today_gprs_used; }
			 */

			newValues.put(GPRS_COL_EACH_DAY_USER_CONSUME_INFO,
					traffic_each_day_use);
			long retVal = mDatabase.replace(
					mTableNames[TABLE_EACH_DAY_USER_CONSUME_INFO],
					IMSI_COL_EACH_DAY_USER_CONSUME_INFO, newValues);
			if (retVal == -1) {
				// Log.e("my","some error happened during the data write to each day form in all consume");
			} else {
				// Log.e("my","successfully inserted the data to each day form in all consume, the line number is "+retVal);
			}
			float fees_remain = conManager.getFeesRemian();
			if (fees_remain != 100000) {
				if (!conManager.getEarliestMonthlyFeeAvailableDate()
						.substring(0, 6)
						.equalsIgnoreCase(NowDate.substring(0, 6))) {
					conManager.setEarliestMonthlyFeeAvailableDate(NowDate);
					conManager.setEarliestMonthlyFeeAvailable(fees_remain);
				}
			}

		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	public void refreshEachDayTrafficConsume() {
		Cursor c = null;
		try {
			if (mDatabase == null) {
				return;
			}
			java.util.Date utilDate = new java.util.Date();
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
			String NowDate = formatter.format(utilDate);

			String sql = " select " + GPRS_COL_EACH_DAY_USER_CONSUME_INFO
					+ " from  " + mTableNames[TABLE_EACH_DAY_USER_CONSUME_INFO]
					+ " where " + ID_COL + " = ?";

			c = mDatabase.rawQuery(sql, new String[] { NowDate });

			long today_gprs_used = 0l;

			if (c != null && c.moveToFirst()) {
				today_gprs_used = c.getLong(0);
			}
			c.close();
			ConfigManager conManager = new ConfigManager(mContext);
			ContentValues newValues = new ContentValues();
			newValues.put(ID_COL, NowDate);
			newValues.put(IMSI_COL_EACH_DAY_USER_CONSUME_INFO,
					DeviceUtils.getIMSI(mContext));
			float todayAlreadyConsumedFee = 0f;
			String orderBy = ID_COL + " desc ";
			Cursor cursor = mDatabase.query(
					mTableNames[TABLE_EACH_DAY_USER_CONSUME_INFO], null, ID_COL
							+ "=" + NowDate, null, null, null, orderBy);
			if (cursor != null && cursor.getCount() != 0) {
				if (cursor.moveToFirst()) {
					todayAlreadyConsumedFee = cursor
							.getFloat(cursor
									.getColumnIndex(EXPENCE_COL_EACH_DAY_USER_CONSUME_INFO));
				}
			}
			cursor.close();
			newValues.put(EXPENCE_COL_EACH_DAY_USER_CONSUME_INFO,
					todayAlreadyConsumedFee);
			long traffic_each_day_use = conManager.getTotalGprsUsed()
					- conManager.getEarliestGprsLog();
			/*
			 * if (today_gprs_used >= traffic_each_day_use) {
			 * traffic_each_day_use += today_gprs_used; }
			 */

			// Log.e("my","refreshEachDayTrafficConsume in conManager.getTotalGprsUsed()="+
			// conManager.getTotalGprsUsed()+",conManager.getEarliestGprsLog()="+conManager.getEarliestGprsLog());

			newValues.put(GPRS_COL_EACH_DAY_USER_CONSUME_INFO,
			/* traffic_each_day_use */traffic_each_day_use);
			long retVal = mDatabase.replace(
					mTableNames[TABLE_EACH_DAY_USER_CONSUME_INFO],
					IMSI_COL_EACH_DAY_USER_CONSUME_INFO, newValues);
			if (retVal == -1) {
				// Log.e("my","some error happened during the data write to each day form in traffic consume");
			} else {
				// Log.e("my","successfully inserted the data to each day form in traffic consume, the line number is "+retVal);
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	//
	public void refreshEachDayFeeConsume() {
		Cursor c = null;
		try {
			if (mDatabase == null) {
				return;
			}

			java.util.Date utilDate = new java.util.Date();
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
			String NowDate = formatter.format(utilDate);

			String sql = " select * from "
					+ mTableNames[TABLE_EACH_DAY_USER_CONSUME_INFO] + " where "
					+ ID_COL + " = ?";

			c = mDatabase.rawQuery(sql, new String[] { NowDate });

			long today_gprs_used = 0l;
			float today_fee_used = 0f;
			if (c != null && c.moveToFirst()) {
				today_gprs_used = c.getLong(c
						.getColumnIndex(GPRS_COL_EACH_DAY_USER_CONSUME_INFO));
				today_fee_used = c
						.getFloat(c
								.getColumnIndex(EXPENCE_COL_EACH_DAY_USER_CONSUME_INFO));
			}

			ConfigManager conManager = new ConfigManager(mContext);
			ContentValues newValues = new ContentValues();
			newValues.put(ID_COL, NowDate);
			newValues.put(IMSI_COL_EACH_DAY_USER_CONSUME_INFO,
					DeviceUtils.getIMSI(mContext));
			if (conManager.getEarliestDailyAvailFee() != 100000f
					&& conManager.getCalculateFeeAvailable() != 100000f) {
				/*
				 * if(today_fee_used >conManager.getEarliestDailyAvailFee() -
				 * conManager.getCalculateFeeAvailable()){ today_fee_used +=
				 * conManager.getEarliestDailyAvailFee() -
				 * conManager.getCalculateFeeAvailable(); }
				 */
				newValues.put(
						EXPENCE_COL_EACH_DAY_USER_CONSUME_INFO,
						conManager.getEarliestDailyAvailFee()
								- conManager.getCalculateFeeAvailable());
			} else {
				newValues.put(EXPENCE_COL_EACH_DAY_USER_CONSUME_INFO,
						today_fee_used);
			}

			long traffic_each_day_use = conManager.getTotalGprsUsed()
					- conManager.getEarliestGprsLog();
			/*
			 * if (today_gprs_used >= traffic_each_day_use) {
			 * traffic_each_day_use += today_gprs_used; }
			 */

			newValues.put(GPRS_COL_EACH_DAY_USER_CONSUME_INFO,
			/* today_gprs_used */traffic_each_day_use);
			long retVal = mDatabase.replace(
					mTableNames[TABLE_EACH_DAY_USER_CONSUME_INFO],
					IMSI_COL_EACH_DAY_USER_CONSUME_INFO, newValues);
			if (retVal == -1) {
				// Log.e("my","some error happened during the data write to each day form in fee consume");
			} else {
				// Log.e("my","successfully inserted the data to each day form in fee consume, the line number is "+retVal);
			}
			float fees_remain = conManager.getFeesRemian();
			if (fees_remain != 100000) {
				if (!conManager.getEarliestMonthlyFeeAvailableDate()
						.substring(0, 6)
						.equalsIgnoreCase(NowDate.substring(0, 6))) {
					conManager.setEarliestMonthlyFeeAvailableDate(NowDate);
					conManager.setEarliestMonthlyFeeAvailable(fees_remain);
				}
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public long getYesterdayGprsUsed() {
		Cursor c = null;
		long yesterdayGprsUsed = 0;
		if (mDatabase == null) {
			// TODO reset
			return yesterdayGprsUsed;
		}
		try {
			String sql = "select " + GPRS_COL_EACH_DAY_USER_CONSUME_INFO
					+ " from " + mTableNames[TABLE_EACH_DAY_USER_CONSUME_INFO]
					+ " where " + ID_COL + " = ? ";
			c = mDatabase.rawQuery(sql,
					new String[] { CallStatUtils.getYesterday() });
			if (c != null && c.moveToFirst()) {
				yesterdayGprsUsed = c.getLong(c
						.getColumnIndex(GPRS_COL_EACH_DAY_USER_CONSUME_INFO));
			}
		} catch (Exception e) {
			ILog.logException(getClass(), e);
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return yesterdayGprsUsed;
	}

	public static void restoreHistoryDayConsumeData(String NowDate) {
		try {
			ILog.LogI(CallStatDatabase.class, "in restoreHistoryDayConsumeData");
			String Year_Month = NowDate.substring(0, NowDate.length() - 2);// 将日期中的年、月字段取出来
			String Day_of_Month = NowDate.substring(6);// 将日期中的“日”项取出来
			String Begin_Year_Month_Day = Year_Month + "01";
			int data_len = Integer.parseInt(Day_of_Month);
			float[] hist_fee = new float[data_len];
			float[] hist_traffic = new float[data_len];
			for (int i = 0; i < hist_fee.length; i++) {
				int day = data_len - (hist_fee.length - i - 1);
				DecimalFormat df = new DecimalFormat("00");
				String str_date_start = Year_Month + df.format((long) day)
						+ "0000";
				String str_date_end = Year_Month + df.format((long) day + 1)
						+ "0000";// 为了和数据库中的保存的日期格式一致 年月日时分

				String orderBy = ID_COL + " asc ";
				Cursor cursor = mDatabase.query(
						mTableNames[TABLE_RECONCILIATION_INFO],
						null,
						TIME_COL_RECONCILIATION_INFO + " between "
								+ Long.parseLong(str_date_start) + " and "
								+ Long.parseLong(str_date_end), null, null,
						null, orderBy);

				if (cursor != null && cursor.getCount() == 0) {
					ILog.LogI(CallStatDatabase.class, "cursor.getCount() == 0");
					cursor.close();
				} else if (cursor != null && cursor.getCount() != 0) {
					ILog.LogI(CallStatDatabase.class, "cursor.getCount():"
							+ cursor.getCount());
					// Log.e("my","zhangjing date is "+Year_Month+df.format((long)day)+" cursor.getCount()="+cursor.getCount());
					if (cursor.moveToFirst()) {
						do {
							hist_fee[i] += cursor
									.getFloat(cursor
											.getColumnIndex(DIFFERENCE_COL_RECONCILIATION_INFO));
							hist_traffic[i] += Integer
									.parseInt(cursor.getString(cursor
											.getColumnIndex(TRAFFIC_DATA_COL_RECONCILIATION_INFO)));
						} while (cursor.moveToNext());
					}
					cursor.close();
					ContentValues newValues = new ContentValues();
					newValues.put(ID_COL, Year_Month + df.format((long) day));
					newValues.put(IMSI_COL_EACH_DAY_USER_CONSUME_INFO,
							DeviceUtils.getIMSI(mContext));
					newValues.put(EXPENCE_COL_EACH_DAY_USER_CONSUME_INFO,
							hist_fee[i]);
					newValues.put(GPRS_COL_EACH_DAY_USER_CONSUME_INFO,
							hist_traffic[i]);
					ILog.LogI(CallStatDatabase.class, "hist_traffic[i]:"
							+ hist_traffic[i]);
					mDatabase.replace(
							mTableNames[TABLE_EACH_DAY_USER_CONSUME_INFO],
							IMSI_COL_EACH_DAY_USER_CONSUME_INFO, newValues);
				}

			}
		} catch (Exception e) {
			ILog.logException(CallStatDatabase.class, e);
		}

	}

	// 得到自本月一号到今天的消费数组
	public float[] getDayFeeSpendArray(String NowDate) {
		String Year_Month = NowDate.substring(0, NowDate.length() - 2);// 将日期中的年、月字段取出来
		String Day_of_Month = NowDate.substring(6);// 将日期中的“日”项取出来
		String Begin_Year_Month_Day = Year_Month + "01";
		int data_len = Integer.parseInt(Day_of_Month);
		float[] retArray = new float[data_len];
		try {
			String orderBy = ID_COL + " asc ";
			Cursor cursor = mDatabase.query(
					mTableNames[TABLE_EACH_DAY_USER_CONSUME_INFO], null, ID_COL
							+ " between " + Begin_Year_Month_Day + " and "
							+ NowDate, null, null, null, orderBy);
			if (cursor != null && cursor.getCount() == 0) {
				cursor.close();
			} else if (cursor != null && cursor.getCount() != 0) {
				if (cursor.moveToFirst()) {
					do {
						String date = cursor.getString(cursor
								.getColumnIndex(ID_COL));
						int day = Integer.parseInt(date.substring(6));
						retArray[day - 1] = cursor
								.getFloat(cursor
										.getColumnIndex(EXPENCE_COL_EACH_DAY_USER_CONSUME_INFO));
					} while (cursor.moveToNext());
				}
				cursor.close();
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}

		return retArray;
	}

	public float[] getDayGprsSpend(String NowDate) {
		String Year_Month = NowDate.substring(0, NowDate.length() - 2);
		String Day_of_Month = NowDate.substring(6);// 将日期中的“日”项取出来
		String Begin_Year_Month_Day = Year_Month + "01";
		int data_len = Integer.parseInt(Day_of_Month);
		float[] retArray = new float[data_len];
		try {
			String orderBy = ID_COL + " asc ";
			Cursor cursor = mDatabase.query(
					mTableNames[TABLE_EACH_DAY_USER_CONSUME_INFO], null, ID_COL
							+ " between " + Begin_Year_Month_Day + " and "
							+ NowDate, null, null, null, orderBy);
			if (cursor != null && cursor.getCount() == 0) {
				cursor.close();
			} else if (cursor != null && cursor.getCount() != 0) {
				if (cursor.moveToFirst()) {
					do {
						String date = cursor.getString(cursor
								.getColumnIndex(ID_COL));
						int day = Integer.parseInt(date.substring(6));
						retArray[day - 1] = cursor
								.getFloat(cursor
										.getColumnIndex(GPRS_COL_EACH_DAY_USER_CONSUME_INFO));
					} while (cursor.moveToNext());
					cursor.close();
				}

			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}

		return retArray;
	}

	/**
	 * 获取对账信息方程组列表
	 * 
	 * @return 返回此列表
	 */
	public ArrayList<ReconciliationInfo> getReconciliationInfoList() {
		Cursor cursor = null;
		ArrayList<ReconciliationInfo> reconciliationInfoList = null;
		cacheFileManager = new CacheFileManager(mContext);
		ConfigManager configManager = new ConfigManager(mContext);

		synchronized (mReconciliationInfoLock) {
			try {
				createTable(TABLE_RECONCILIATION_INFO);
				String orderBy = TIME_COL_RECONCILIATION_INFO + " desc ";
				cursor = mDatabase.query(
						mTableNames[TABLE_RECONCILIATION_INFO], null, null,
						null, null, null, orderBy);
				if (cursor.moveToFirst()) {
					reconciliationInfoList = new ArrayList<ReconciliationInfo>();
					while (true) {
						ReconciliationInfo reconciliationInfo = new ReconciliationInfo(
								cursor.getLong(cursor
										.getColumnIndex(TIME_COL_RECONCILIATION_INFO)),
								cursor.getInt(cursor
										.getColumnIndex(THIS_LOCALITY_DIALING_TIMES_COL_RECONCILIATION_INFO)),
								cursor.getInt(cursor
										.getColumnIndex(LONG_DISTANCE_DIALING_TIMES_COL_RECONCILIATION_INFO)),
								cursor.getInt(cursor
										.getColumnIndex(ROAMING_TIMES_COL_RECONCILIATION_INFO)),
								cursor.getInt(cursor
										.getColumnIndex(UNKONW_DIALING_TIMES_COL_RECONCILIATION_INFO)),
								cursor.getInt(cursor
										.getColumnIndex(IP_DIALING_TIMES_COL_RECONCILIATION_INFO)),
								cursor.getInt(cursor
										.getColumnIndex(SHORT_DIALING_TIMES_COL_RECONCILIATION_INFO)),
								cursor.getInt(cursor
										.getColumnIndex(SEND_SMS_NUM_COL_RECONCILIATION_INFO)),
								Long.parseLong(cursor.getString(cursor
										.getColumnIndex(TRAFFIC_DATA_COL_RECONCILIATION_INFO))),
								Double.parseDouble(cursor.getString(cursor
										.getColumnIndex(DIFFERENCE_COL_RECONCILIATION_INFO))),
								cursor.getInt(cursor
										.getColumnIndex(DELTA1_COL_RECONCILIATION_INFO)),
								cursor.getInt(cursor
										.getColumnIndex(DELTA2_COL_RECONCILIATION_INFO)),
								cursor.getInt(cursor
										.getColumnIndex(DELTA3_COL_RECONCILIATION_INFO)),
								cursor.getInt(cursor
										.getColumnIndex(DELTA4_COL_RECONCILIATION_INFO)),
								cursor.getInt(cursor
										.getColumnIndex(DELTA5_COL_RECONCILIATION_INFO)),
								cursor.getInt(cursor
										.getColumnIndex(DELTA6_COL_RECONCILIATION_INFO)));

						cacheFileManager.logAccounting("对账时间:"
								+ reconciliationInfo.getTime()
								+ "本地主叫:"
								+ reconciliationInfo
										.getThisLocalityDialingTimes()
								+ "本地长途:"
								+ reconciliationInfo
										.getLongDistanceDialingTimes() + "漫游:"
								+ reconciliationInfo.getRoamingTimes() + "未知:"
								+ reconciliationInfo.getUnkonwDialingTimes()
								+ "IP拨号:"
								+ reconciliationInfo.getIPDialingTimes()
								+ "短号拨号:"
								+ reconciliationInfo.getShortDialingTimes()
								+ "短信条数:" + reconciliationInfo.getSendSmsNum()
								+ "流量信息:" + reconciliationInfo.getTrafficData()
								+ "费用:" + reconciliationInfo.getDifference()
								+ "\n");
						reconciliationInfoList.add(reconciliationInfo);
						if (!cursor.moveToNext()) {
							break;
						}
					}
				}
			} catch (Exception e) {
				// TODO: handle exception
				ILog.logException(this.getClass(), e);
			} finally {
				if (cursor != null) {
					cursor.close();
				}
			}

		}
		return reconciliationInfoList;
	}

	public int getEquationCount() {

		String sql = " SELECT COUNT(*) FROM t_reconciliation_info ";
		int count = 0;
		synchronized (mReconciliationInfoLock) {
			Cursor cursor = null;
			try {
				cursor = mDatabase.rawQuery(sql, null);
				if (cursor.moveToFirst()) {
					count = cursor.getInt(0);
				}
			} catch (Exception e) {
				ILog.logException(this.getClass(), e);
			} finally {
				if (cursor != null)
					cursor.close();
			}
		}
		return count;

	}

	//
	// /////////////////// Reconciliation functions///////////////////////
	//
	/**
	 * 更新对账号码指令关键字数据库
	 */
	public int updateReconciliationCode(AccountingKeyWordsBean accountingKeyWrod) {
		int code = -1;
		SQLiteDatabase reconciliationDatabase = null;
		synchronized (mtelPhoneLock) {
			try {
				// Log.e("callstats", "in updateReconciliationCode:");
				reconciliationDatabase = mContext.openOrCreateDatabase(
						TELPHONE_DATABASE_FILE, 0, null);
				String where = PROVINCE_COL_RECONCILIATION_CODE + " = ?"
						+ " AND " + MNO_COL_RECONCILIATION_CODE + " = ?"
						+ " AND " + BRAND_COL_RECONCILIATION_CODE + " = ?"
						+ " AND " + TYPE_COL_RECONCILIATION_CODE + " = ?";
				// Log.e("callstats", "where clause:" + "\n " + where);
				ContentValues newValues = new ContentValues();
				newValues.put(KEY_COL_RECONCILIATION_CODE,
						accountingKeyWrod.getKeywords());
				newValues.put(MESSAGE_COL_RECONCILIATION_CODE,
						accountingKeyWrod.getCode());
				code = reconciliationDatabase.update(
						mTableNames[TABLE_RECONCILIATION_CODE],
						newValues,
						where,
						new String[] { accountingKeyWrod.getProvince(),
								accountingKeyWrod.getOperator(),
								accountingKeyWrod.getBrand(),
								String.valueOf(accountingKeyWrod.getType()) });
			} catch (Exception e) {
			} finally {
				if (reconciliationDatabase != null) {
					reconciliationDatabase.close();
				}
			}
		}

		return code;
	}

	public void updateReconciliationCodeList(List<AccountingKeyWordsBean> list) {
		long time = System.currentTimeMillis();
		ConfigManager config = new ConfigManager(mContext);
		SQLiteDatabase reconciliationDatabase = null;
		try {
			reconciliationDatabase = mContext.openOrCreateDatabase(
					TELPHONE_DATABASE_FILE, 0, null);
			String where = PROVINCE_COL_RECONCILIATION_CODE + " = ?" + " AND "
					+ MNO_COL_RECONCILIATION_CODE + " = ?" + " AND "
					+ BRAND_COL_RECONCILIATION_CODE + " = ?" + " AND "
					+ TYPE_COL_RECONCILIATION_CODE + " = ?";
			synchronized (mReconciliationInfoLock) {
				reconciliationDatabase.beginTransaction();
				for (AccountingKeyWordsBean accountingKeyWrod : list) {
					ContentValues newValues = new ContentValues();
					newValues.put(KEY_COL_RECONCILIATION_CODE,
							accountingKeyWrod.getKeywords());
					newValues.put(MESSAGE_COL_RECONCILIATION_CODE,
							accountingKeyWrod.getCode());
					reconciliationDatabase
							.update(mTableNames[TABLE_RECONCILIATION_CODE],
									newValues,
									where,
									new String[] {
											accountingKeyWrod.getProvince(),
											accountingKeyWrod.getOperator(),
											accountingKeyWrod.getBrand(),
											String.valueOf(accountingKeyWrod
													.getType()) });
				}
				reconciliationDatabase.setTransactionSuccessful();
			}
		} catch (Exception e) {
			ILog.logException(CallStatDatabase.class, e);
		} finally {
			if (reconciliationDatabase.inTransaction()) {
				reconciliationDatabase.endTransaction();
			}
			if (reconciliationDatabase != null)
				reconciliationDatabase.close();
		}
		ILog.LogI(
				CallStatDatabase.class,
				"accouting code list updated,time costs:"
						+ (System.currentTimeMillis() - time));
	}

	/**
	 * 在用户矫正对账指令中所进行的矫正指令操作
	 * 
	 * @param province
	 *            省份
	 * @param mno
	 *            运营商
	 * @param brand
	 *            品牌
	 * @param type
	 *            对账的类型
	 * @param key
	 *            所要修改的内容，号码，还是指令
	 * @param data
	 *            修改的数据
	 */
	public void modifyReconciliationCode(String province, String mno,
			String brand, String type, String key, String data) {
		SQLiteDatabase reconciliationDatabase = null;
		synchronized (mtelPhoneLock) {
			try {
				reconciliationDatabase = mContext.openOrCreateDatabase(
						TELPHONE_DATABASE_FILE, 0, null);
				String where = PROVINCE_COL_RECONCILIATION_CODE + " = ?"
						+ " AND " + MNO_COL_RECONCILIATION_CODE + " = ?"
						+ " AND " + BRAND_COL_RECONCILIATION_CODE + " = ?"
						+ " AND " + TYPE_COL_RECONCILIATION_CODE + " = ?";
				ContentValues newValues = new ContentValues();
				newValues.put(key, data);
				reconciliationDatabase.update(
						mTableNames[TABLE_RECONCILIATION_CODE], newValues,
						where, new String[] { province, mno, brand, type });
			} catch (Exception e) {
				// TODO: handle exception
			} finally {
				if (reconciliationDatabase != null) {
					reconciliationDatabase.close();
				}
			}
		}
	}

	/**
	 * 根据省份运营商信息获取对账查询号码指令
	 * 
	 * @param province
	 *            省份
	 * @param city
	 *            城市
	 * @param mno
	 *            运营商
	 * @param brand
	 *            品牌
	 * @return 返回此对象
	 */
	public void initReconciliationInfo2ConfigXml(String province, String city,
			String mno, String brand, int restore) {
		Cursor cursor = null;
		SQLiteDatabase db = null;
		try {
			db = CallStatApplication.getInstance().openOrCreateDatabase(
					TELPHONE_DATABASE_FILE, Context.MODE_PRIVATE, null);
			String where = PROVINCE_COL_RECONCILIATION_CODE + " = ? AND "
					// + CITY_COL_RECONCILIATION_CODE + " = ? AND "
					+ MNO_COL_RECONCILIATION_CODE + " = ? AND "
					+ BRAND_COL_RECONCILIATION_CODE + " = ? ";
			cursor = db.query("t_reconciliation_code", new String[] {
					TYPE_COL_RECONCILIATION_CODE,
					NUMBER_COL_RECONCILIATION_CODE,
					MESSAGE_COL_RECONCILIATION_CODE,
					KEY_COL_RECONCILIATION_CODE }, where, new String[] {
					province, mno, brand }, null, null,
					TYPE_COL_RECONCILIATION_CODE + " ASC");
			if (cursor != null && cursor.getCount() > 0) {
				ConfigManager config = new ConfigManager(mContext);
				String numberDb = null, keyDb, messageDb;
				int type;
				for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor
						.moveToNext()) {
					type = Integer.parseInt(cursor.getString(cursor
							.getColumnIndex(TYPE_COL_RECONCILIATION_CODE)));
					messageDb = cursor.getString(cursor
							.getColumnIndex(MESSAGE_COL_RECONCILIATION_CODE));
					keyDb = cursor.getString(cursor
							.getColumnIndex(KEY_COL_RECONCILIATION_CODE));
					numberDb = cursor.getString(cursor
							.getColumnIndex(NUMBER_COL_RECONCILIATION_CODE));
					if (type == 0) {// 属于话费相关
						if (messageDb != null) {
							if (messageDb.contains(":")) {
								if (restore == CallStatApplication.feesUsedCodeRestore
										|| restore == CallStatApplication.AllCodeRestore) {
									config.setHFUsedCode(messageDb.split(":")[0]);// 已用话费
								}
								if (restore == CallStatApplication.feesYeCodeRestore
										|| restore == CallStatApplication.AllCodeRestore) {
									config.setHFYeCode(messageDb.split(":")[1]);// 已用话费
								}
							} else {
								if (restore == CallStatApplication.feesUsedCodeRestore
										|| restore == CallStatApplication.AllCodeRestore) {
									config.setHFUsedCode(messageDb);// 已用话费
								}
								if (restore == CallStatApplication.feesYeCodeRestore
										|| restore == CallStatApplication.AllCodeRestore) {
									config.setHFYeCode(messageDb);// 已用话费
								}
							}
						}
						if (keyDb != null) {
							if (keyDb.contains(":")) {
								config.setFeeUsedKeywords(keyDb.split(":")[0]);// 已用话费关键字
								config.setFeeAvailKeywords(keyDb.split(":")[1]);// 可用话费关键字
							}
						}
					} else if (type == 1) {// 流量相关
						if (messageDb != null) {
							if (messageDb.contains(":")) {
								if (restore == CallStatApplication.trafficUsedCodeRestore
										|| restore == CallStatApplication.AllCodeRestore) {
									config.setGprsUsedCode(messageDb.split(":")[0]);// 已用流量
								}
								if (restore == CallStatApplication.trafficYeCodeRestore
										|| restore == CallStatApplication.AllCodeRestore) {
									config.setGprsYeCode(messageDb.split(":")[1]);// 已用流量
								}
							} else {
								if (restore == CallStatApplication.trafficUsedCodeRestore
										|| restore == CallStatApplication.AllCodeRestore) {
									config.setGprsUsedCode(messageDb);// 已用流量
								}
								if (restore == CallStatApplication.trafficYeCodeRestore
										|| restore == CallStatApplication.AllCodeRestore) {
									config.setGprsYeCode(messageDb);// 已用流量
								}
							}
						}
						if (keyDb != null) {
							if (keyDb.contains(":")) {
								config.setTrafficUsedKeywords(keyDb.split(":")[0]);// 已用流量关键字
								config.setTrafficAvailKeywords(keyDb.split(":")[1]);// 可用流量关键字
							}
						}
					} else if (type == 2) {// 套餐相关
						if (messageDb != null) {
							if (messageDb.contains(":")) {
								config.setPackageCode(messageDb.split(":")[0]);// 套餐
							} else {
								config.setPackageCode(messageDb);// 套餐
							}
						}

					}
				}
				// if(restore == CallStatApplication.operatorNumberRestore ||
				// restore == CallStatApplication.AllCodeRestore){
				config.setOperatorNum(numberDb);
				// }
			} else {
				ILog.LogI(this.getClass(), "从数据库获取对账指令为空:------------"
						+ province + ":" + city + ":" + mno + ":" + brand);
				if (CallStatUtils.isMyAppOnDesk(CallStatApplication
						.getInstance())) {
					ToastFactory.getToast(CallStatApplication.getInstance(),
							"获取短信指令失败，请您核对运营商信息是否正确！", Toast.LENGTH_LONG)
							.show();
				}
			}
		} catch (Exception e) {
			ILog.LogE(getClass(), "初始化短信对帐本地配置文件失败:" + e.toString());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
			if (db != null) {
				db.close();
			}
		}
	}

	//
	// /////////////////// Call log functions///////////////////////
	//
	public void addCallLog(CallLog callLog) {
		if (callLog == null || mDatabase == null) {
			return;
		}
		synchronized (mCallLock) {
			final ContentValues c = new ContentValues();
			c.put(TYPE_COL_CALL_LOG, callLog.getType());
			c.put(TIME_COL_CALL_LOG, callLog.getDate());
			c.put(DURATION_COL_CALL_LOG, callLog.getDuration());
			c.put(NUMBER_COL_CALL_LOG, callLog.getNumber());
			c.put(NUMBER_TYPE_CALL_LOG, callLog.getNumberType());
			if (!queryCalllogExist(Long.parseLong(callLog.getDate()),
					callLog.getNumber())) {
				mDatabase.insert(mTableNames[TABLE_CALL_LOG],
						NUMBER_COL_CALL_LOG, c);
			}
		}
	}

	public void addCallLogFromList(ArrayList<CallLog> list) {
		if (list == null || mDatabase == null || list.isEmpty()) {
			return;
		}
		try {
			synchronized (mCallLock) {
				mDatabase.beginTransaction();
				for (CallLog callLog : list) {
					final ContentValues c = new ContentValues();
					c.put(TYPE_COL_CALL_LOG, callLog.getType());
					c.put(TIME_COL_CALL_LOG, callLog.getDate());
					c.put(DURATION_COL_CALL_LOG, callLog.getDuration());
					c.put(NUMBER_COL_CALL_LOG, callLog.getNumber());
					c.put(NUMBER_TYPE_CALL_LOG, callLog.getNumberType());
					if (!queryCalllogExist(Long.parseLong(callLog.getDate()),
							callLog.getNumber())) {
						mDatabase.insert(mTableNames[TABLE_CALL_LOG],
								NUMBER_COL_CALL_LOG, c);
					}
				}
				mDatabase.setTransactionSuccessful();
			}
		} catch (Exception e) {
			ILog.logException(getClass(), e);
		} finally {
			if (mDatabase.inTransaction()) {
				mDatabase.endTransaction();
			}
		}
	}

	public boolean removeMoreCallLogsByNumber(List<CallLog> callLogs) {
		synchronized (mCallLock) {
			if (callLogs == null || mDatabase == null) {
				return false;
			}

			String sql = "(";

			for (int i = 0; i < callLogs.size(); i++) {
				if (i != 0) {
					sql += " OR ";
				}
				sql = sql + NUMBER_COL_CALL_LOG + " = \'"
						+ callLogs.get(i).getNumber() + "\'";
			}
			sql += ")";
			mDatabase.delete(mTableNames[TABLE_CALL_LOG], sql, null);
			return true;
		}
	}

	// delete one record by the string passed in,this string
	// will be considered as the condition of the filter
	// when the sql be executed
	public void removeCallLog(String columnName, String str) {
		if (str == null || str.equals("")) {
			return;
		}
		final String where = "(" + columnName + " == ?)";
		synchronized (mCallLock) {
			mDatabase.delete(mTableNames[TABLE_CALL_LOG], where,
					new String[] { str });
		}
	}

	public List<CallLog> getAllCallLogs() {
		if (mDatabase == null) {
			return null;
		}

		synchronized (mCallLock) {
			List<CallLog> ret = null;
			Cursor cursor = null;
			try {
				cursor = mDatabase.rawQuery("SELECT * FROM "
						+ mTableNames[TABLE_CALL_LOG], null);
				if (cursor.moveToFirst()) {
					ret = new ArrayList<CallLog>();
					do {
						String number = cursor.getString(cursor
								.getColumnIndex(NUMBER_COL_CALL_LOG));

						String date = cursor.getString(cursor
								.getColumnIndex(TIME_COL_CALL_LOG));

						Integer type = cursor.getInt(cursor
								.getColumnIndex(TYPE_COL_CALL_LOG));

						Integer duration = cursor.getInt(cursor
								.getColumnIndex(DURATION_COL_CALL_LOG));
						int numberType = cursor.getInt(cursor
								.getColumnIndex(NUMBER_TYPE_CALL_LOG));
						CallLog callLog = new CallLog(number, date, type,
								duration, numberType);

						ret.add(callLog);
					} while (cursor.moveToNext());
				}
			} catch (IllegalStateException e) {
				// Log.e(LOGTAG, "getUsernamePassword", e);
			} finally {
				if (cursor != null)
					cursor.close();
			}
			return ret;
		}
	}

	public boolean hasCallLogs() {
		synchronized (mCallLock) {
			return hasEntries(TABLE_CALL_LOG);
		}
	}

	// check if there is one or more than one records in the database,judge by
	// number
	public boolean hasCallLog(String number) {
		synchronized (mCallLock) {

			Cursor cursor = null;
			boolean ret = false;
			try {
				final String where = "(" + NUMBER_COL_CALL_LOG + " like ?)";
				cursor = mDatabase.query(mTableNames[TABLE_CALL_LOG],
						ID_PROJECTION, where, new String[] { number }, null,
						null, null);
				ret = cursor.moveToFirst() == true;
			} catch (IllegalStateException e) {
				// Log.e("i", e.getMessage());
			} finally {
				if (cursor != null)
					cursor.close();
			}
			return ret;
		}
	}

	public void clearCallLogs() {
		if (mDatabase == null) {
			return;
		}

		synchronized (mCallLock) {
			mDatabase.delete(mTableNames[TABLE_CALL_LOG], null, null);
		}
	}

	public List<CallLog> getAllOutgoingCalls() {
		if (mDatabase == null) {
			return null;
		}

		synchronized (mCallLock) {
			List<CallLog> ret = null;
			Cursor cursor = null;
			try {
				/*
				 * String sql = "SELECT "+ DURATION_COL_CALL_LOG + " FROM " +
				 * mTableNames[TABLE_CALL_LOG] + " WHERE "+ TYPE_COL_CALL_LOG +
				 * " = 2 ";
				 */
				String sql = "SELECT " + " * " + " FROM "
						+ mTableNames[TABLE_CALL_LOG] + " WHERE "
						+ TYPE_COL_CALL_LOG + " = 2 ";
				cursor = mDatabase.rawQuery(sql, null);
				if (cursor.moveToFirst()) {
					ret = new ArrayList<CallLog>();
					do {
						String number = cursor.getString(cursor
								.getColumnIndex(NUMBER_COL_CALL_LOG));

						String date = cursor.getString(cursor
								.getColumnIndex(TIME_COL_CALL_LOG));

						Integer type = cursor.getInt(cursor
								.getColumnIndex(TYPE_COL_CALL_LOG));

						Integer duration = cursor.getInt(cursor
								.getColumnIndex(DURATION_COL_CALL_LOG));
						int numberType = cursor.getInt(cursor
								.getColumnIndex(NUMBER_TYPE_CALL_LOG));

						CallLog callLog = new CallLog(number, date, type,
								duration, numberType);

						ret.add(callLog);
					} while (cursor.moveToNext());
				}
			} catch (IllegalStateException e) {
				// Log.e(LOGTAG, "getUsernamePassword", e);
			} finally {
				if (cursor != null)
					cursor.close();
			}
			return ret;
		}
	}

	// get total time of all of the outgoing calls,returned by minutes
	public int getTotalCallsTime(int type) {
		int totalCall = -1;
		if (mDatabase == null) {
			// if -1 was returned ,means there is no such database
			return totalCall;
		}

		synchronized (mCallLock) {
			Cursor cursor = null;
			try {
				String sql = "SELECT " + DURATION_COL_CALL_LOG + " FROM "
						+ mTableNames[TABLE_CALL_LOG] + " WHERE "
						+ TYPE_COL_CALL_LOG + " = ?";
				/*
				 * String sql = "SELECT "+ " * " + " FROM " +
				 * mTableNames[TABLE_CALL_LOG] + " WHERE "+ TYPE_COL_CALL_LOG +
				 * " = 2 ";
				 */
				cursor = mDatabase.rawQuery(sql,
						new String[] { String.valueOf(type) });
				if (cursor.moveToFirst()) {
					do {
						Integer duration = cursor.getInt(cursor
								.getColumnIndex(DURATION_COL_CALL_LOG));
						totalCall += duration;

					} while (cursor.moveToNext());
				}
			} catch (IllegalStateException e) {
				// Log.e(LOGTAG, "getUsernamePassword", e);
			} finally {
				if (cursor != null)
					cursor.close();
			}
			return totalCall;
		}
	}

	/**
	 * 查询某一段时间内的所有通话记录
	 * 
	 * @param lastTime
	 *            起始时间
	 * @param now
	 *            终止时间
	 * @return 返回此通话记录
	 */
	public int[] getLatelyLocalCallTime(long lastTime, long now) {
		int[] callTime = { 0, 0, 0, 0, 0, 0 };
		if (mDatabase == null) {
			// if -1 was returned ,means there is no such database
			return callTime;
		}
		synchronized (mCallLock) {
			Cursor cursor = null;
			try {
				String where = TIME_COL_CALL_LOG + " BETWEEN ? AND ?";
				/*
				 * String sql = "SELECT "+ " * " + " FROM " +
				 * mTableNames[TABLE_CALL_LOG] + " WHERE "+ TYPE_COL_CALL_LOG +
				 * " = 2 ";
				 */
				cursor = mDatabase.query(
						mTableNames[TABLE_CALL_LOG],
						new String[] { TYPE_COL_CALL_LOG, NUMBER_TYPE_CALL_LOG,
								DURATION_COL_CALL_LOG },
						where,
						new String[] { String.valueOf(lastTime),
								String.valueOf(now) }, null, null, null);

				if (cursor.moveToFirst()) {
					do {
						Integer duration = cursor.getInt(cursor
								.getColumnIndex(DURATION_COL_CALL_LOG));
						int type = cursor.getInt(cursor
								.getColumnIndex(TYPE_COL_CALL_LOG));
						switch (type) {
						case Calls.INCOMING_TYPE:
							// callTime[2] += duration;
							int numberType1 = cursor.getInt(cursor
									.getColumnIndex(NUMBER_TYPE_CALL_LOG));
							switch (numberType1) {
							case CallLog.CALL_ROAMING:
								callTime[2] += duration;
								break;
							}
							break;
						case Calls.OUTGOING_TYPE:
							int numberType = cursor.getInt(cursor
									.getColumnIndex(NUMBER_TYPE_CALL_LOG));
							switch (numberType) {
							case CallLog.CALL_LOCAL:
								callTime[0] += duration;
								break;
							case CallLog.CALL_LONG_DISTANCE:
								callTime[1] += duration;
								break;
							case CallLog.CALL_UNKONW:
								callTime[3] += duration;
								break;
							case CallLog.CALL_IP:
								callTime[4] += duration;
								break;
							case CallLog.CALL_SHORT:
								callTime[5] += duration;
								break;
							case CallLog.CALL_ROAMING:
								callTime[2] += duration;
								break;
							}
							break;
						}
					} while (cursor.moveToNext());
				}
			} catch (IllegalStateException e) {
				// Log.e(LOGTAG, "getUsernamePassword", e);
			} finally {
				if (cursor != null)
					cursor.close();
			}
			return callTime;
		}
	}

	public boolean queryCalllogExist(long date, String number) {
		Cursor cursor = mDatabase
				.query(mTableNames[TABLE_CALL_LOG], null, TIME_COL_CALL_LOG
						+ " = ? AND " + NUMBER_COL_CALL_LOG + "= ?",
						new String[] { String.valueOf(date), number }, null,
						null, null);
		if (cursor != null && cursor.getCount() > 0) {
			cursor.close();
			return true;
		} else {
			cursor.close();
			return false;
		}
	}

	// //////////////////////// Call log end ///////////////////////

	// -----------------Traffic detail fuctions------------------//
	public void setUninstalledOfTrafficDetail(int uid) {
		synchronized (mTrafficDetailLock) {
			createTable(TABLE_TRAFFIC_DETAIL);
			Cursor cursor = mDatabase.query(mTableNames[TABLE_TRAFFIC_DETAIL],
					null, UID_COL_TRAFFIC_DETAIL + " = ?",
					new String[] { String.valueOf(uid) }, null, null, null);
			try {
				if (cursor.moveToFirst()) {
					mDatabase.delete(mTableNames[TABLE_TRAFFIC_DETAIL],
							UID_COL_TRAFFIC_DETAIL + " = ?",
							new String[] { String.valueOf(uid) });
				}
			} catch (Exception e) {
				// TODO: handle exception
			} finally {
				if (cursor != null) {
					cursor.close();
				}
			}
		}
	}

	public void updateYesterDay() {
		if (mDatabase == null) {
			return;
		}
		List<NewTrafficDetail> list = new ArrayList<NewTrafficDetail>();
		createTable(TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID);
		Cursor cursor = mDatabase.query(
				mTableNames[TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID],
				null, DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID + " = "
						+ Long.parseLong(CallStatUtils.getYesterday()), null,
				null, null, null);
		if (cursor.moveToFirst()) {
			do {
				int uid = cursor
						.getInt(cursor
								.getColumnIndex(UID_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
				String appName = cursor
						.getString(cursor
								.getColumnIndex(APPNAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));

				String packageName = cursor
						.getString(cursor
								.getColumnIndex(PACKAGENAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));

				boolean isGprsrejected = ((cursor
						.getInt(cursor
								.getColumnIndex(GPRS_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID))) == 1) ? true
						: false;
				boolean isWifirejected = ((cursor
						.getInt(cursor
								.getColumnIndex(WIFI_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID))) == 1) ? true
						: false;

				long gprs_upload = cursor
						.getLong(cursor
								.getColumnIndex(GPRS_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
				long gprs_download = cursor
						.getLong(cursor
								.getColumnIndex(GPRS_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));

				long wifi_upload = cursor
						.getLong(cursor
								.getColumnIndex(WIFI_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
				long wifi_download = cursor
						.getLong(cursor
								.getColumnIndex(WIFI_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));

				long node_upload = cursor
						.getLong(cursor
								.getColumnIndex(NODE_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));

				long node_download = cursor
						.getLong(cursor
								.getColumnIndex(NODE_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
				boolean lastWifiOn = cursor
						.getInt(cursor
								.getColumnIndex(WIFI_ON_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID)) == 1 ? true
						: false;

				ConnectivityManager connectivityManager = (ConnectivityManager) mContext
						.getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo wifiInfo = connectivityManager
						.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
				boolean nowWifiOn = wifiInfo.isConnected() ? true : false;

				if (nowWifiOn == true && lastWifiOn == true) {
					NewTrafficDetail td = new NewTrafficDetail(
							CallStatUtils.getYesterday(), uid, appName,
							packageName, isGprsrejected, isWifirejected,
							gprs_upload, gprs_download, wifi_upload
									+ MTrafficStats.getUidTxBytes(uid)
									- node_upload, wifi_download
									+ MTrafficStats.getUidRxBytes(uid)
									- node_download,
							// 直接用当前node更新到数据库
							MTrafficStats.getUidTxBytes(uid),
							MTrafficStats.getUidRxBytes(uid), nowWifiOn);
					list.add(td);
					continue;
				} else if (nowWifiOn == true && lastWifiOn == false) {
					NewTrafficDetail td = new NewTrafficDetail(
							CallStatUtils.getYesterday(), uid, appName,
							packageName, isGprsrejected, isWifirejected,
							gprs_upload + MTrafficStats.getUidTxBytes(uid)
									- node_upload, gprs_download
									+ MTrafficStats.getUidRxBytes(uid)
									- node_download, wifi_upload,
							wifi_download,
							// 直接用当前node更新到数据库
							MTrafficStats.getUidTxBytes(uid),
							MTrafficStats.getUidRxBytes(uid), nowWifiOn);
					list.add(td);
					continue;
				} else if (nowWifiOn == false && lastWifiOn == true) {
					NewTrafficDetail td = new NewTrafficDetail(
							CallStatUtils.getYesterday(), uid, appName,
							packageName, isGprsrejected, isWifirejected,
							gprs_upload, gprs_download, wifi_upload
									+ MTrafficStats.getUidTxBytes(uid)
									- node_upload, wifi_download
									+ MTrafficStats.getUidRxBytes(uid)
									- node_download,
							// 直接用当前node更新到数据库
							MTrafficStats.getUidTxBytes(uid),
							MTrafficStats.getUidRxBytes(uid), nowWifiOn);
					list.add(td);
					continue;
				} else if (nowWifiOn == false && lastWifiOn == false) {
					NewTrafficDetail td = new NewTrafficDetail(
							CallStatUtils.getYesterday(), uid, appName,
							packageName, isGprsrejected, isWifirejected,
							gprs_upload + MTrafficStats.getUidTxBytes(uid)
									- node_upload, gprs_download
									+ MTrafficStats.getUidRxBytes(uid)
									- node_download, wifi_upload,
							wifi_download,
							// 直接用当前node更新到数据库
							MTrafficStats.getUidTxBytes(uid),
							MTrafficStats.getUidRxBytes(uid), nowWifiOn);
					list.add(td);
					continue;
				}

			} while (cursor.moveToNext());
			updateNewTrafficDetail(list);
		}
	}

	public void updateYesterDay(String type) {
		if (mDatabase == null) {
			return;
		}
		List<NewTrafficDetail> list = new ArrayList<NewTrafficDetail>();
		createTable(TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID);
		Cursor cursor = mDatabase.query(
				mTableNames[TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID],
				null, DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID + " = "
						+ Long.parseLong(CallStatUtils.getYesterday()), null,
				null, null, null);

		try {
			if (cursor.moveToFirst()) {
				do {
					int uid = cursor
							.getInt(cursor
									.getColumnIndex(UID_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
					String appName = cursor
							.getString(cursor
									.getColumnIndex(APPNAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));

					String packageName = cursor
							.getString(cursor
									.getColumnIndex(PACKAGENAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));

					boolean isGprsrejected = ((cursor
							.getInt(cursor
									.getColumnIndex(GPRS_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID))) == 1) ? true
							: false;
					boolean isWifirejected = ((cursor
							.getInt(cursor
									.getColumnIndex(WIFI_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID))) == 1) ? true
							: false;

					long gprs_upload = cursor
							.getLong(cursor
									.getColumnIndex(GPRS_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
					long gprs_download = cursor
							.getLong(cursor
									.getColumnIndex(GPRS_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));

					long wifi_upload = cursor
							.getLong(cursor
									.getColumnIndex(WIFI_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
					long wifi_download = cursor
							.getLong(cursor
									.getColumnIndex(WIFI_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));

					long node_upload = cursor
							.getLong(cursor
									.getColumnIndex(NODE_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));

					long node_download = cursor
							.getLong(cursor
									.getColumnIndex(NODE_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
					boolean lastWifiOn = cursor
							.getInt(cursor
									.getColumnIndex(WIFI_ON_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID)) == 1 ? true
							: false;
					long current_node_upload = MTrafficStats.getUidTxBytes(uid);
					long current_node_download = MTrafficStats.getUidRxBytes(uid);
					NewTrafficDetail td;

					ConnectivityManager connectivityManager = (ConnectivityManager) mContext
							.getSystemService(Context.CONNECTIVITY_SERVICE);
					NetworkInfo wifiInfo = connectivityManager
							.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
					boolean nowWifiOn = wifiInfo.isConnected() ? true : false;

					if (type.equals(TRAFFIC_DETAIL_TYPE_WIFI)) {
						td = new NewTrafficDetail(
								CallStatUtils.getYesterday(),
								uid,
								appName,
								packageName,
								isGprsrejected,
								isWifirejected,
								gprs_upload,
								gprs_download,
								wifi_upload
										+ (current_node_upload - node_upload > 0 ? current_node_upload
												- node_upload
												: 0),
								wifi_download
										+ (current_node_download
												- node_download > 0 ? current_node_download
												- node_download
												: 0),
								// 直接用当前node更新到数据库
								current_node_upload, current_node_download,
								nowWifiOn);
						list.add(td);
					} else if (type.equals(TRAFFIC_DETAIL_TYPE_GPRS)) {
						td = new NewTrafficDetail(
								CallStatUtils.getYesterday(),
								uid,
								appName,
								packageName,
								isGprsrejected,
								isWifirejected,
								gprs_upload
										+ (current_node_upload - node_upload > 0 ? current_node_upload
												- node_upload
												: 0),
								gprs_download
										+ (current_node_download
												- node_download > 0 ? current_node_download
												- node_download
												: 0), wifi_upload,
								wifi_download,
								// 直接用当前node更新到数据库
								current_node_upload, current_node_download,
								nowWifiOn);
						list.add(td);
					}
				} while (cursor.moveToNext());
			}
		} catch (Exception e) {
			// TODO: handle exception
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		updateNewTrafficDetail(list);
	}

	public void updateYesterdayAndInitTodayTrafficDetailList(String type)
	{

		if (mDatabase == null) {
			return;
		}
		List<NewTrafficDetail> list = new ArrayList<NewTrafficDetail>();
		createTable(TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID);
		Cursor cursor = mDatabase.query(
				mTableNames[TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID],
				null, DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID + " = "
						+ Long.parseLong(CallStatUtils.getYesterday()), null,
				null, null, null);

		try {
			if (cursor.moveToFirst()) {
				do {
					int uid = cursor
							.getInt(cursor
									.getColumnIndex(UID_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
					String appName = cursor
							.getString(cursor
									.getColumnIndex(APPNAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));

					String packageName = cursor
							.getString(cursor
									.getColumnIndex(PACKAGENAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));

					boolean isGprsrejected = ((cursor
							.getInt(cursor
									.getColumnIndex(GPRS_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID))) == 1) ? true
							: false;
					boolean isWifirejected = ((cursor
							.getInt(cursor
									.getColumnIndex(WIFI_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID))) == 1) ? true
							: false;

					long gprs_upload = cursor
							.getLong(cursor
									.getColumnIndex(GPRS_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
					long gprs_download = cursor
							.getLong(cursor
									.getColumnIndex(GPRS_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));

					long wifi_upload = cursor
							.getLong(cursor
									.getColumnIndex(WIFI_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
					long wifi_download = cursor
							.getLong(cursor
									.getColumnIndex(WIFI_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));

					long node_upload = cursor
							.getLong(cursor
									.getColumnIndex(NODE_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));

					long node_download = cursor
							.getLong(cursor
									.getColumnIndex(NODE_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
					boolean lastWifiOn = cursor
							.getInt(cursor
									.getColumnIndex(WIFI_ON_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID)) == 1 ? true
							: false;
					long current_node_upload = MTrafficStats.getUidTxBytes(uid);
					long current_node_download = MTrafficStats.getUidRxBytes(uid);
					NewTrafficDetail y_td;
					NewTrafficDetail t_td;

					ConnectivityManager connectivityManager = (ConnectivityManager) mContext
							.getSystemService(Context.CONNECTIVITY_SERVICE);
					NetworkInfo wifiInfo = connectivityManager
							.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
					boolean nowWifiOn = wifiInfo.isConnected() ? true : false;

					if (type.equals(TRAFFIC_DETAIL_TYPE_WIFI)) {
						y_td = new NewTrafficDetail(
								CallStatUtils.getYesterday(),
								uid,
								appName,
								packageName,
								isGprsrejected,
								isWifirejected,
								gprs_upload,
								gprs_download,
								wifi_upload
										+ (current_node_upload - node_upload > 0 ? current_node_upload
												- node_upload
												: 0),
								wifi_download
										+ (current_node_download
												- node_download > 0 ? current_node_download
												- node_download
												: 0),
								// 直接用当前node更新到数据库
								current_node_upload, current_node_download,
								nowWifiOn);
						t_td = new NewTrafficDetail(
								CallStatUtils.getNowDate(),
								uid,
								appName,
								packageName,
								isGprsrejected,
								isWifirejected,
								0,
								0,
								0,
								0,
								// 直接用当前node更新到数据库
								current_node_upload, current_node_download,
								nowWifiOn);
						list.add(y_td);
						list.add(t_td);
					} else if (type.equals(TRAFFIC_DETAIL_TYPE_GPRS)) {
						y_td = new NewTrafficDetail(
								CallStatUtils.getYesterday(),
								uid,
								appName,
								packageName,
								isGprsrejected,
								isWifirejected,
								gprs_upload
										+ (current_node_upload - node_upload > 0 ? current_node_upload
												- node_upload
												: 0),
								gprs_download
										+ (current_node_download
												- node_download > 0 ? current_node_download
												- node_download
												: 0), wifi_upload,
								wifi_download,
								// 直接用当前node更新到数据库
								current_node_upload, current_node_download,
								nowWifiOn);
						t_td = new NewTrafficDetail(
								CallStatUtils.getNowDate(),
								uid,
								appName,
								packageName,
								isGprsrejected,
								isWifirejected,
								0,
								0, 
								0,
								0,
								// 直接用当前node更新到数据库
								current_node_upload, current_node_download,
								nowWifiOn);
						list.add(y_td);
						list.add(t_td);
					}
				} while (cursor.moveToNext());
			}
		} catch (Exception e) {
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		updateNewTrafficDetail(list);
	}
	
	/*
	 * public void makeDataInDbSequence(){ if (mDatabase == null) { return; }
	 * List<NewTrafficDetail> list = new ArrayList<NewTrafficDetail>(); String
	 * orderBy = DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID + " desc ";
	 * Cursor cursor = mDatabase.query(
	 * mTableNames[TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID], null, null,
	 * null, null, null, orderBy); if(cursor.getCount()==0){ return; } String
	 * latest_date = "00000000"; if(cursor.moveToFirst()){ latest_date =
	 * cursor.getString
	 * (cursor.getColumnIndex(DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
	 * } cursor.close();
	 * 
	 * if(latest_date.equalsIgnoreCase("00000000")){ return; } cursor =
	 * mDatabase.query(
	 * mTableNames[TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID], null,
	 * DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID +" = "+latest_date, null,
	 * null, null, null); String Year_Month = latest_date.substring(0,
	 * latest_date.length() - 2);//将日期中的年、月字段取出来 String Now_Year_Month =
	 * CallStatUtils.getNowDate().substring(0,
	 * CallStatUtils.getNowDate().length() - 2); NewTrafficDetail td;
	 * if(!Year_Month.equalsIgnoreCase(Now_Year_Month)){
	 * if(cursor.moveToFirst()){ do{ int uid = cursor.getInt(cursor
	 * .getColumnIndex(UID_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID)); String
	 * appName = cursor.getString(cursor
	 * .getColumnIndex(APPNAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
	 * 
	 * String packageName = cursor .getString(cursor
	 * .getColumnIndex(PACKAGENAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
	 * 
	 * boolean isGprsrejected = ((cursor.getInt(cursor
	 * .getColumnIndex(GPRS_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
	 * ))) == 1) ? true : false; boolean isWifirejected = ((cursor.getInt(cursor
	 * .
	 * getColumnIndex(WIFI_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID))
	 * ) == 1) ? true : false; WifiManager wifiManager = (WifiManager)
	 * mContext.getSystemService(Service.WIFI_SERVICE); boolean nowWifiOn =
	 * (wifiManager.getWifiState()==WifiManager.WIFI_STATE_ENABLED)? true:false;
	 * for(int i =
	 * Integer.parseInt(Now_Year_Month+"01");i<Integer.parseInt(CallStatUtils
	 * .getNowDate());i++){ td = new NewTrafficDetail(String.valueOf(i),uid,
	 * appName, packageName,isGprsrejected,isWifirejected, 0,0,0 ,0, //
	 * 直接用当前node更新到数据库 0,0,nowWifiOn); list.add(td); }
	 * 
	 * }while(cursor.moveToNext()); }
	 * 
	 * }else{ int i = Integer.parseInt(latest_date); int j =
	 * Integer.parseInt(CallStatUtils.getNowDate());
	 * 
	 * if(cursor.moveToFirst()){ do{ int uid = cursor.getInt(cursor
	 * .getColumnIndex(UID_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID)); String
	 * appName = cursor.getString(cursor
	 * .getColumnIndex(APPNAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
	 * 
	 * String packageName = cursor .getString(cursor
	 * .getColumnIndex(PACKAGENAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
	 * 
	 * boolean isGprsrejected = ((cursor.getInt(cursor
	 * .getColumnIndex(GPRS_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
	 * ))) == 1) ? true : false; boolean isWifirejected = ((cursor.getInt(cursor
	 * .
	 * getColumnIndex(WIFI_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID))
	 * ) == 1) ? true : false; WifiManager wifiManager = (WifiManager)
	 * mContext.getSystemService(Service.WIFI_SERVICE); boolean nowWifiOn =
	 * (wifiManager.getWifiState()==WifiManager.WIFI_STATE_ENABLED)? true:false;
	 * for(int k=i+1;k<j;k++){ td = new NewTrafficDetail(String.valueOf(k),uid,
	 * appName, packageName,isGprsrejected,isWifirejected, 0,0,0 ,0, //
	 * 直接用当前node更新到数据库 0,0,nowWifiOn); list.add(td); }
	 * 
	 * }while(cursor.moveToNext()); } }
	 * 
	 * updateNewTrafficDetail(list); }
	 */

	// 刷新这一天的NodeData字段的数据
	public void refreshEachDayNodeData() {
		if (mDatabase == null) {
			return;
		}
		Cursor cursor = null;
		List<NewTrafficDetail> list = new ArrayList<NewTrafficDetail>();
		try {
			createTable(TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID);
			cursor = mDatabase.query(
					mTableNames[TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID],
					null, DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
							+ " = " + CallStatUtils.getNowDate(), null, null,
					null, null);
			NewTrafficDetail td;

			if (cursor.moveToFirst()) {
				do {
					int uid = cursor
							.getInt(cursor
									.getColumnIndex(UID_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
					String appName = cursor
							.getString(cursor
									.getColumnIndex(APPNAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));

					String packageName = cursor
							.getString(cursor
									.getColumnIndex(PACKAGENAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));

					boolean isGprsrejected = ((cursor
							.getInt(cursor
									.getColumnIndex(GPRS_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID))) == 1) ? true
							: false;
					boolean isWifirejected = ((cursor
							.getInt(cursor
									.getColumnIndex(WIFI_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID))) == 1) ? true
							: false;
					long gprs_upload = cursor
							.getLong(cursor
									.getColumnIndex(GPRS_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
					long gprs_download = cursor
							.getLong(cursor
									.getColumnIndex(GPRS_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));

					long wifi_upload = cursor
							.getLong(cursor
									.getColumnIndex(WIFI_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
					long wifi_download = cursor
							.getLong(cursor
									.getColumnIndex(WIFI_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));

					ConnectivityManager connectivityManager = (ConnectivityManager) mContext
							.getSystemService(Context.CONNECTIVITY_SERVICE);
					NetworkInfo wifiInfo = connectivityManager
							.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
					boolean nowWifiOn = wifiInfo.isConnected() ? true : false;

					long current_node_upload = MTrafficStats.getUidTxBytes(uid);
					long current_node_download = MTrafficStats
							.getUidRxBytes(uid);

					td = new NewTrafficDetail(CallStatUtils.getNowDate(), uid,
							appName, packageName, isGprsrejected,
							isWifirejected, gprs_upload, gprs_download,
							wifi_upload, wifi_download,
							// 直接用当前node更新到数据库
							current_node_upload, current_node_download,
							nowWifiOn);
					list.add(td);

				} while (cursor.moveToNext());
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		} finally {
			if (cursor != null)
				cursor.close();
		}
		updateNewTrafficDetail(list);
	}

	public List<NewTrafficDetail> getLatestNewTrafficDetail(String type) {
		long times = System.currentTimeMillis();
		List<NewTrafficDetail> list = new ArrayList<NewTrafficDetail>();
		if (mDatabase == null) {
			return null;
		}
		ILog.LogE(this.getClass(), "in getLatestNewTrafficDetail");
		Cursor cursor = null;
		try {
			createTable(TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID);
			cursor = mDatabase.query(
					mTableNames[TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID],
					null,
					DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID + " = "
							+ Long.parseLong(CallStatUtils.getNowDate()), null,
					null, null, null);
			ILog.LogE(this.getClass(), "cursor:" + cursor.getCount());
			if (cursor.moveToFirst()) {
				do {

					int uid = cursor
							.getInt(cursor
									.getColumnIndex(UID_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
					String appName = cursor
							.getString(cursor
									.getColumnIndex(APPNAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));

					String packageName = cursor
							.getString(cursor
									.getColumnIndex(PACKAGENAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));

					boolean isGprsrejected = ((cursor
							.getInt(cursor
									.getColumnIndex(GPRS_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID))) == 1) ? true
							: false;
					boolean isWifirejected = ((cursor
							.getInt(cursor
									.getColumnIndex(WIFI_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID))) == 1) ? true
							: false;
					long gprs_upload = cursor
							.getLong(cursor
									.getColumnIndex(GPRS_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
					long gprs_download = cursor
							.getLong(cursor
									.getColumnIndex(GPRS_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));

					long wifi_upload = cursor
							.getLong(cursor
									.getColumnIndex(WIFI_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
					long wifi_download = cursor
							.getLong(cursor
									.getColumnIndex(WIFI_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));

					long node_upload = cursor
							.getLong(cursor
									.getColumnIndex(NODE_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));

					long node_download = cursor
							.getLong(cursor
									.getColumnIndex(NODE_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
					long current_node_upload = 0;
					long current_node_download = 0;

					ConnectivityManager connectivityManager = (ConnectivityManager) mContext
							.getSystemService(Context.CONNECTIVITY_SERVICE);
					NetworkInfo wifiInfo = connectivityManager
							.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
					boolean nowWifiOn = wifiInfo.isConnected() ? true : false;

					current_node_upload = MTrafficStats.getUidTxBytes(uid);
					current_node_download = MTrafficStats.getUidRxBytes(uid);
					NewTrafficDetail td;
					// 有重启现象发生
					/*
					 * if(current_node_upload+current_node_download <
					 * node_upload+node_download){ td = new
					 * NewTrafficDetail(CallStatUtils.getNowDate(),uid, appName,
					 * packageName,isGprsrejected,isWifirejected,
					 * gprs_upload,gprs_download,wifi_upload ,wifi_download, //
					 * 直接用当前node更新到数据库
					 * current_node_upload,current_node_download,nowWifiOn);
					 * Log.e("my","if reboot in"); list.add(td); continue; }
					 */
					if (type.equals(TRAFFIC_DETAIL_TYPE_WIFI)) {
						td = new NewTrafficDetail(
								CallStatUtils.getNowDate(),
								uid,
								appName,
								packageName,
								isGprsrejected,
								isWifirejected,
								gprs_upload,
								gprs_download,
								wifi_upload
										+ (current_node_upload - node_upload > 0 ? current_node_upload
												- node_upload
												: 0),
								wifi_download
										+ (current_node_download
												- node_download > 0 ? current_node_download
												- node_download
												: 0),
								// 直接用当前node更新到数据库
								current_node_upload, current_node_download,
								nowWifiOn);
						list.add(td);
					} else if (type.equals(TRAFFIC_DETAIL_TYPE_GPRS)) {
						td = new NewTrafficDetail(
								CallStatUtils.getNowDate(),
								uid,
								appName,
								packageName,
								isGprsrejected,
								isWifirejected,
								gprs_upload
										+ (current_node_upload - node_upload > 0 ? current_node_upload
												- node_upload
												: 0),
								gprs_download
										+ (current_node_download
												- node_download > 0 ? current_node_download
												- node_download
												: 0), wifi_upload,
								wifi_download,
								// 直接用当前node更新到数据库
								current_node_upload, current_node_download,
								nowWifiOn);
						list.add(td);
					}

				} while (cursor.moveToNext());
			} else {
				ILog.LogE(this.getClass(), "database has no data");
			}
		} catch (Exception e) {
			// Log.e(LOGTAG, "getUsernamePassword", e);
			ILog.logException(this.getClass(), e);
		} finally {
			if (cursor != null)
				cursor.close();
		}
		ILog.LogI(
				CallStatDatabase.class,
				"refreshNewTrafficDetail()Times============"
						+ (System.currentTimeMillis() - times));
		return list;
	}

	public List<NewTrafficDetail> getLatestNewTrafficDetail() {
		long times = System.currentTimeMillis();
		List<NewTrafficDetail> list = new ArrayList<NewTrafficDetail>();
		if (mDatabase == null) {
			return null;
		}

		synchronized (mTrafficDetailLock) {
			Cursor cursor = null;
			try {
				// Log.i(LOGTAG, "in query database");
				/*
				 * cursor = mDatabase.rawQuery("SELECT * FROM " +
				 * mTableNames[TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID],
				 * null);
				 */
				// ArrayList<ApplicationInfo> appInfoList = getAppInfo();

				/*
				 * String orderBy =
				 * DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID + " desc ";
				 * cursor = mDatabase.query(
				 * mTableNames[TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID],
				 * null, null, null, null, null, orderBy); String latest_date =
				 * "00000000"; if(cursor.moveToFirst()){ latest_date =
				 * cursor.getString(cursor.getColumnIndex(
				 * DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID)); }
				 * cursor.close();
				 */
				createTable(TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID);
				cursor = mDatabase
						.query(mTableNames[TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID],
								null,
								DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
										+ " = "
										+ Long.parseLong(CallStatUtils
												.getNowDate()), null, null,
								null, null);
				// ArrayList<Integer> uidList = new ArrayList<Integer>();
				if (cursor.moveToFirst()) {
					do {

						String date = cursor
								.getString(cursor
										.getColumnIndex(DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));

						int uid = cursor
								.getInt(cursor
										.getColumnIndex(UID_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
						String appName = cursor
								.getString(cursor
										.getColumnIndex(APPNAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));

						String packageName = cursor
								.getString(cursor
										.getColumnIndex(PACKAGENAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));

						boolean isGprsrejected = ((cursor
								.getInt(cursor
										.getColumnIndex(GPRS_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID))) == 1) ? true
								: false;
						boolean isWifirejected = ((cursor
								.getInt(cursor
										.getColumnIndex(WIFI_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID))) == 1) ? true
								: false;
						long gprs_upload = cursor
								.getLong(cursor
										.getColumnIndex(GPRS_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
						long gprs_download = cursor
								.getLong(cursor
										.getColumnIndex(GPRS_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));

						long wifi_upload = cursor
								.getLong(cursor
										.getColumnIndex(WIFI_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
						long wifi_download = cursor
								.getLong(cursor
										.getColumnIndex(WIFI_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));

						long node_upload = cursor
								.getLong(cursor
										.getColumnIndex(NODE_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));

						long node_download = cursor
								.getLong(cursor
										.getColumnIndex(NODE_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
						boolean lastWifiOn = cursor
								.getInt(cursor
										.getColumnIndex(WIFI_ON_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID)) == 1 ? true
								: false;

						long current_node_upload = 0;
						long current_node_download = 0;

						ConnectivityManager connectivityManager = (ConnectivityManager) mContext
								.getSystemService(Context.CONNECTIVITY_SERVICE);
						NetworkInfo wifiInfo = connectivityManager
								.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
						boolean nowWifiOn = wifiInfo.isConnected() ? true
								: false;

						current_node_upload = MTrafficStats.getUidTxBytes(uid);
						current_node_download = MTrafficStats
								.getUidRxBytes(uid);
						// Log.e("my","zhangjing current_node_upload="+current_node_upload);
						NewTrafficDetail td;
						// 有重启现象发生
						if (current_node_upload + current_node_download < node_upload
								+ node_download) {
							td = new NewTrafficDetail(
									CallStatUtils.getNowDate(), uid, appName,
									packageName, isGprsrejected,
									isWifirejected, gprs_upload, gprs_download,
									wifi_upload, wifi_download,
									// 直接用当前node更新到数据库
									current_node_upload, current_node_download,
									nowWifiOn);
							Log.e("my", "if reboot in");
							list.add(td);
							continue;
						}
						if (mContext != null) {
							if (nowWifiOn == true && lastWifiOn == true) {
								td = new NewTrafficDetail(
										CallStatUtils.getNowDate(),
										uid,
										appName,
										packageName,
										isGprsrejected,
										isWifirejected,
										gprs_upload,
										gprs_download,
										wifi_upload
												+ (current_node_upload
														- node_upload > 0 ? current_node_upload
														- node_upload
														: 0),
										wifi_download
												+ (current_node_download
														- node_download > 0 ? current_node_download
														- node_download
														: 0),
										// 直接用当前node更新到数据库
										current_node_upload,
										current_node_download, nowWifiOn);
								// Log.e("my","if zhangjing1 in");
								list.add(td);
								continue;
							} else if (nowWifiOn == true && lastWifiOn == false) {
								td = new NewTrafficDetail(
										CallStatUtils.getNowDate(),
										uid,
										appName,
										packageName,
										isGprsrejected,
										isWifirejected,
										gprs_upload
												+ (current_node_upload
														- node_upload > 0 ? current_node_upload
														- node_upload
														: 0),
										gprs_download
												+ (current_node_download
														- node_download > 0 ? current_node_download
														- node_download
														: 0), wifi_upload,
										wifi_download,
										// 直接用当前node更新到数据库
										current_node_upload,
										current_node_download, nowWifiOn);
								// Log.e("my","if zhangjing2 in");
								list.add(td);
								continue;
							} else if (nowWifiOn == false && lastWifiOn == true) {
								td = new NewTrafficDetail(
										CallStatUtils.getNowDate(),
										uid,
										appName,
										packageName,
										isGprsrejected,
										isWifirejected,
										gprs_upload,
										gprs_download,
										wifi_upload
												+ (current_node_upload
														- node_upload > 0 ? current_node_upload
														- node_upload
														: 0),
										wifi_download
												+ (current_node_download
														- node_download > 0 ? current_node_download
														- node_download
														: 0),
										// 直接用当前node更新到数据库
										current_node_upload,
										current_node_download, nowWifiOn);
								// Log.e("my","if zhangjing3 in");
								list.add(td);
								continue;
							} else if (nowWifiOn == false
									&& lastWifiOn == false) {
								td = new NewTrafficDetail(
										CallStatUtils.getNowDate(),
										uid,
										appName,
										packageName,
										isGprsrejected,
										isWifirejected,
										gprs_upload
												+ (current_node_upload
														- node_upload > 0 ? current_node_upload
														- node_upload
														: 0),
										gprs_download
												+ (current_node_download
														- node_download > 0 ? current_node_download
														- node_download
														: 0), wifi_upload,
										wifi_download,
										// 直接用当前node更新到数据库
										current_node_upload,
										current_node_download, nowWifiOn);
								// Log.e("my","if zhangjing4 in");
								list.add(td);
								continue;
							}
						}

					} while (cursor.moveToNext());
				} else {
					// Log.i(LOGTAG, "database has no data");
				}
			} catch (Exception e) {
				ILog.logException(this.getClass(), e);
			} finally {
				if (cursor != null)
					cursor.close();
			}
		}
		// Log.i("callstats",
		// "refreshNewTrafficDetail()Times============"
		// + (System.currentTimeMillis() - times));
		return list;

	}

	public long updateNewTrafficDetail(NewTrafficDetail td) {
		if (mDatabase == null) {
			return -1L;
		}
		synchronized (mTrafficDetailLock) {
			try {
				final ContentValues c = new ContentValues();

				int gprs_rejected = 0;
				int wifi_rejected = 0;
				if (td.getIsGprsRejected())
					gprs_rejected = 1;
				if (td.getIsWifiRejected())
					wifi_rejected = 1;

				c.put(DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID,
						td.getDate());
				c.put(UID_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID,
						td.getUid());
				c.put(APPNAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID,
						td.getAppName());
				c.put(PACKAGENAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID,
						td.getPackageName());
				c.put(GPRS_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID,
						gprs_rejected);
				c.put(WIFI_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID,
						wifi_rejected);
				c.put(GPRS_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID,
						td.getGprsUpload());
				c.put(GPRS_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID,
						td.getGprsDownload());
				c.put(WIFI_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID,
						td.getWifiUpload());
				c.put(WIFI_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID,
						td.getWifiDownload());
				c.put(NODE_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID,
						td.getNodeUpload());
				c.put(NODE_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID,
						td.getNodeDownload());
				c.put(WIFI_ON_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID,
						td.getWifiOn());

				createTable(TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID);
				long rowId = mDatabase
						.replace(
								mTableNames[TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID],
								"nullColumnHack", c);
				// Log.i("callstats", "updateTrafficDetail rowId:" + rowId);
				return rowId;
			} catch (Exception e) {
				ILog.logException(this.getClass(), e);
			}

		}
		return -1L;
	}

	public void updateNewTrafficDetail(List<NewTrafficDetail> trafficDetails) {
		long times = System.currentTimeMillis();
		ILog.LogE(this.getClass(), "updateNewTrafficDetail begins");
		if (mDatabase == null) {
			return;
		}
		if (trafficDetails == null || trafficDetails.size() == 0) {
			return;
		}

		synchronized (mTrafficDetailLock) {
			try {
				mDatabase.beginTransaction();

				for (NewTrafficDetail td : trafficDetails) {
					final ContentValues c = new ContentValues();

					int gprs_rejected = 0;
					int wifi_rejected = 0;
					if (td.getIsGprsRejected())
						gprs_rejected = 1;
					if (td.getIsWifiRejected())
						wifi_rejected = 1;

					c.put(DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID,
							td.getDate());
					c.put(UID_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID,
							td.getUid());
					c.put(APPNAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID,
							td.getAppName());
					c.put(PACKAGENAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID,
							td.getPackageName());
					c.put(GPRS_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID,
							gprs_rejected);
					c.put(WIFI_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID,
							wifi_rejected);
					c.put(GPRS_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID,
							td.getGprsUpload());
					c.put(GPRS_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID,
							td.getGprsDownload());
					c.put(WIFI_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID,
							td.getWifiUpload());
					c.put(WIFI_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID,
							td.getWifiDownload());
					c.put(NODE_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID,
							td.getNodeUpload());
					c.put(NODE_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID,
							td.getNodeDownload());
					c.put(WIFI_ON_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID,
							td.getWifiOn());
					createTable(TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID);
					mDatabase
							.replace(
									mTableNames[TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID],
									DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID,
									c);
				}
				mDatabase.setTransactionSuccessful();
			} catch (Exception e) {
				ILog.logException(this.getClass(), e);
			} finally {
				if (mDatabase.inTransaction()) {
					mDatabase.endTransaction();
				}
			}
		}

		ILog.LogE(this.getClass(), "updateNewTrafficDetail cost time:"
				+ (System.currentTimeMillis() - times));
	}

	/*
	 * public long updateTrafficDetail(TrafficDetail td){ if (mDatabase == null)
	 * { return -1L; } synchronized (mTrafficDetailLock) { final ContentValues c
	 * = new ContentValues();
	 * 
	 * int rejected = 0; if (td.isRejected()) rejected = 1;
	 * c.put(APP_NAME_COL_TRAFFIC_DETAIL, td.getAppName());
	 * c.put(UID_COL_TRAFFIC_DETAIL, td.getUid());
	 * c.put(TIME_COL_TRAFFIC_DETAIL, td.getTime());
	 * c.put(PACKAGE_NAME_COL_TRAFFIC_DETAIL, td.getPackageName());
	 * c.put(REJECTED_COL_TRAFFIC_DETAIL, rejected);
	 * c.put(UPLOAD_COL_TRAFFIC_DETAIL, td.getUpload());
	 * c.put(DOWNLOAD_COL_TRAFFIC_DETAIL, td.getDownload());
	 * c.put(NODE_UPLOAD_COL_TRAFFIC_DETAIL, td.getNode_upload());
	 * c.put(NODE_DOWLOAD_COL_TRAFFIC_DETAIL, td.getNode_download()); long rowId
	 * = mDatabase.replace(mTableNames[TABLE_TRAFFIC_DETAIL], "nullColumnHack",
	 * c);; Log.i("callstats", "updateTrafficDetail rowId:" + rowId); return
	 * rowId; } }
	 */
	public int deleteNewTrafficDetail(int uid) {
		// Log.i("callstats", " in deleteTrafficDetail");
		if (mDatabase == null) {
			return -1;
		}

		createTable(TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID);

		synchronized (mTrafficDetailLock) {
			String where = UID_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
					+ " = ? ";
			int rowId = mDatabase.delete(
					mTableNames[TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID],
					where, new String[] { String.valueOf(uid) });
			// Log.i("callstats", "deleteTrafficDetail rowId:" + rowId);
			return rowId;
		}

	}

	public int deleteTrafficDetail(int uid) {
		// Log.i("callstats", " in deleteTrafficDetail");
		if (mDatabase == null) {
			return -1;
		}
		synchronized (mTrafficDetailLock) {
			String where = UID_COL_TRAFFIC_DETAIL + " = ? ";
			int rowId = mDatabase.delete(mTableNames[TABLE_TRAFFIC_DETAIL],
					where, new String[] { String.valueOf(uid) });
			// Log.i("callstats", "deleteTrafficDetail rowId:" + rowId);
			return rowId;
		}

	}

	/*
	 * public void updateTrafficDetail(List<TrafficDetail> trafficDetails) { if
	 * (mDatabase == null) { return; }
	 * 
	 * synchronized (mTrafficDetailLock) { Log.i(LOGTAG,
	 * "in updateTrafficDetail database");
	 * 
	 * long time = System.currentTimeMillis();
	 * 
	 * for (TrafficDetail td : trafficDetails) { String appName =
	 * td.getAppName();
	 * 
	 * int uid = td.getUid();
	 * 
	 * String packageName = td.getPackageName();
	 * 
	 * int rejected = 0; if (td.isRejected()) rejected = 1;
	 * 
	 * long upload = td.getUpload(); long download = td.getDownload(); long
	 * node_upload = td.getNode_upload(); long node_download =
	 * td.getNode_download();
	 * 
	 * ContentValues cv = new ContentValues(); cv.put(TIME_COL_TRAFFIC_DETAIL,
	 * time); cv.put(APP_NAME_COL_TRAFFIC_DETAIL, appName);
	 * cv.put(UID_COL_TRAFFIC_DETAIL, uid);
	 * cv.put(PACKAGE_NAME_COL_TRAFFIC_DETAIL, packageName);
	 * cv.put(REJECTED_COL_TRAFFIC_DETAIL, rejected);
	 * cv.put(UPLOAD_COL_TRAFFIC_DETAIL, upload);
	 * cv.put(DOWNLOAD_COL_TRAFFIC_DETAIL, download);
	 * cv.put(NODE_UPLOAD_COL_TRAFFIC_DETAIL, node_upload);
	 * cv.put(NODE_DOWLOAD_COL_TRAFFIC_DETAIL, node_download);
	 * 
	 * 
	 * // mDatabase.execSQL(sql);
	 * mDatabase.replace(mTableNames[TABLE_TRAFFIC_DETAIL], "nullColumnHack",
	 * cv);
	 * 
	 * } } }
	 */

	// -----------------Traffic detail end------------------//
	// -----------------Traffic log fuctions------------------//
	// public void updateAddTrafficLog(Traffic traffic) {
	// if (traffic == null) {
	// return;
	// }
	// Cursor cursor = null;
	// synchronized (mTrafficLock) {
	// String where = TYPE_COL_TRAFFIC_LOG + " = ? " + " AND "
	// + YEAR_MONTH_COL_TRAFFIC_LOG + " = ?";
	// createTable(TABLE_TRAFFIC_LOG);
	// cursor = mDatabase.query(mTableNames[TABLE_TRAFFIC_LOG], null,
	// where,
	// new String[] { traffic.getType(), traffic.getYearMonth() },
	// null, null, null);
	// Log.i("my", "updateAddTrafficLog=" + traffic.getTime() + ","
	// + traffic.getType() + "," + traffic.getUpload() + ","
	// + traffic.getDownload() + ","
	// + (traffic.getUpload() + traffic.getDownload()));
	// try {
	// ContentValues newValues = new ContentValues();
	// newValues.put(TIME_COL_TRAFFIC_LOG, traffic.getTime());
	// newValues.put(YEAR_MONTH_COL_TRAFFIC_LOG,
	// traffic.getYearMonth());
	// newValues.put(TYPE_COL_TRAFFIC_LOG, traffic.getType());
	// newValues.put(UPLOAD_COL_TRAFFIC_LOG,
	// String.valueOf(traffic.getUpload()));
	// newValues.put(DOWNLOAD_COL_TRAFFIC_LOG,
	// String.valueOf(traffic.getDownload()));
	// newValues.put(
	// TOTAL_COL_TRAFFIC_LOG,
	// String.valueOf(traffic.getUpload()
	// + traffic.getDownload()));
	// if (!cursor.moveToFirst()) {
	// mDatabase.insert(mTableNames[TABLE_TRAFFIC_LOG], null,
	// newValues);
	// } else {
	// mDatabase.update(
	// mTableNames[TABLE_TRAFFIC_LOG],
	// newValues,
	// where,
	// new String[] { traffic.getType(),
	// traffic.getYearMonth() });
	// }
	// } catch (Exception e) {
	// // TODO: handle exception
	// } finally {
	// if (cursor != null) {
	// cursor.close();
	// }
	// }
	// }
	// }

	// public Traffic[] getTrafficLogs(String YearMonth) {
	// synchronized (mTrafficLock) {
	// Cursor cursor = null;
	// try {
	// cursor = mDatabase.query(mTableNames[TABLE_TRAFFIC_LOG], null,
	// YEAR_MONTH_COL_TRAFFIC_LOG + " = ?",
	// new String[] { YearMonth }, null, null, null);
	// if (cursor.moveToFirst()) {
	// Traffic[] trafficLog = new Traffic[2];
	// while (true) {
	// if (cursor.getString(
	// cursor.getColumnIndex(TYPE_COL_TRAFFIC_LOG))
	// .equals("gprs")) {
	// trafficLog[CallStatApplication.TRAFFIC_LOG_GPRS] = new Traffic(
	// cursor.getString(cursor
	// .getColumnIndex(TIME_COL_TRAFFIC_LOG)),
	// cursor.getString(cursor
	// .getColumnIndex(YEAR_MONTH_COL_TRAFFIC_LOG)),
	// cursor.getString(cursor
	// .getColumnIndex(TYPE_COL_TRAFFIC_LOG)),
	// Long.parseLong(cursor.getString(cursor
	// .getColumnIndex(UPLOAD_COL_TRAFFIC_LOG))),
	// Long.parseLong(cursor.getString(cursor
	// .getColumnIndex(DOWNLOAD_COL_TRAFFIC_LOG))));
	// } else {
	// trafficLog[CallStatApplication.TRAFFIC_LOG_WIFI] = new Traffic(
	// cursor.getString(cursor
	// .getColumnIndex(TIME_COL_TRAFFIC_LOG)),
	// cursor.getString(cursor
	// .getColumnIndex(YEAR_MONTH_COL_TRAFFIC_LOG)),
	// cursor.getString(cursor
	// .getColumnIndex(TYPE_COL_TRAFFIC_LOG)),
	// Long.parseLong(cursor.getString(cursor
	// .getColumnIndex(UPLOAD_COL_TRAFFIC_LOG))),
	// Long.parseLong(cursor.getString(cursor
	// .getColumnIndex(DOWNLOAD_COL_TRAFFIC_LOG))));
	// }
	// if (!cursor.moveToNext()) {
	// return trafficLog;
	// }
	// }
	// } else {
	// return null;
	// }
	// } catch (Exception e) {
	// // TODO: handle exception
	// } finally {
	// if (cursor != null) {
	// cursor.close();
	// }
	// }
	//
	// }
	// return null;
	// }

	// -----------------Traffic log end------------------//
	//
	// /////////////////// Sms log functions///////////////////////
	//
	// 增加方法实现增加用户月消费行为的原始数据表
	public void addMonthlyStatDataSource(MonthlyStatDataSource datasource) {
		if (datasource == null || mDatabase == null) {
			return;
		}
		synchronized (mUserMonthlyStatDataSourceLock) {
			final ContentValues c = new ContentValues();
			c.put(ID_COL, datasource.getId());
			c.put(TIME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC,
					datasource.getTime());
			c.put(PROVINCE_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC,
					datasource.getProvince());
			c.put(CITY_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC,
					datasource.getCity());
			c.put(MNO_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC,
					datasource.getMno());
			c.put(BRAND_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC,
					datasource.getBrand());
			c.put(TYPE_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC,
					datasource.getType());
			c.put(NUMBER_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC,
					datasource.getNumber());
			c.put(NAME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC,
					datasource.getName());
			c.put(DURATION_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC,
					datasource.getDuration());
			c.put(USED_GPRS_TO_FIRST_DAY_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC,
					datasource.getUsedGprsTofirstDay());
			c.put(USED_GPRS_TO_LAST_EVENT_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC,
					datasource.getUsedGprsToLastEvent());
			c.put(USED_FEE_TO_FIRST_DAY_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC,
					datasource.getUsedFeeToFirstDay());
			c.put(USED_FEE_TO_LAST_EVENT_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC,
					datasource.getUsedFeeToLastEvent());
			c.put(RATES_FOR_LOCAL_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC,
					datasource.getRateForLocal());
			c.put(RATES_FOR_LONG_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC,
					datasource.getRatesForLong());
			c.put(RATES_FOR_ROAMING_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC,
					datasource.getRatesForRoaming());
			c.put(RATES_FOR_IP_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC,
					datasource.getRatesForIP());
			c.put(RATES_FOR_SHORT_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC,
					datasource.getRatesForShort());
			c.put(RATES_FOR_TRAFFIC_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC,
					datasource.getRatesForTraffic());
			c.put(WLAN_USED_TO_FIRST_DAY_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC,
					datasource.getWlanUsedToFirstDay());
			c.put(WLAN_USED_TO_LAST_EVENT_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC,
					datasource.getWlanUsedToLastEvent());
			c.put(RATES_FOR_WLAN_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC,
					datasource.getRatesForSms());
			c.put(ALREADY_UPLOAD_FLAG_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC,
					datasource.getAlreadyUploadFlag());
			if (!queryRecordExistInMonthlyStat(datasource.getId(),
					datasource.getNumber())) {
				long rowid = mDatabase.insert(
						mTableNames[TABLE_USER_CONSUME_MONTHLY_STATISTIC],
						null, c);
				ILog.LogI(this.getClass(), "主键为" + rowid
						+ "的行被成功的插入了用户月消费统计数据库表中");
			}
		}
	}

	// added by zhangjing@archermind.com
	public boolean queryRecordExistInMonthlyStat(String date, String number) {
		Cursor cursor = mDatabase.query(
				mTableNames[TABLE_USER_CONSUME_MONTHLY_STATISTIC], null, ID_COL
						+ " = ? AND "
						+ NUMBER_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
						+ " = ?", new String[] { date, number }, null, null,
				null);
		if (cursor != null && cursor.getCount() > 0) {
			cursor.close();
			return true;
		} else {
			cursor.close();
			return false;
		}
	}

	public void addSmsLog(SmsLog sms) {
		if (sms == null || mDatabase == null) {
			return;
		}
		synchronized (mSmsLock) {
			final ContentValues c = new ContentValues();
			c.put(TYPE_COL_SMS_LOG, sms.getProtocol());
			c.put(TIME_COL_SMS_LOG, sms.getDate());
			c.put(NUMBER_COL_SMS_LOG, sms.getNumber());
			if (!querySmsExist(sms.getDate(), sms.getNumber()))
				mDatabase.insert(mTableNames[TABLE_SMS_LOG],
						NUMBER_COL_SMS_LOG, c);
		}
	}

	public void addSmsLogFromList(ArrayList<SmsLog> list) {
		if (list == null || mDatabase == null || list.isEmpty()) {
			return;
		}
		try {
			synchronized (mSmsLock) {
				mDatabase.beginTransaction();
				for (SmsLog sms : list) {
					final ContentValues c = new ContentValues();
					c.put(TYPE_COL_SMS_LOG, sms.getProtocol());
					c.put(TIME_COL_SMS_LOG, sms.getDate());
					c.put(NUMBER_COL_SMS_LOG, sms.getNumber());
					if (!querySmsExist(sms.getDate(), sms.getNumber()))
						mDatabase.insert(mTableNames[TABLE_SMS_LOG],
								NUMBER_COL_SMS_LOG, c);
				}
				mDatabase.setTransactionSuccessful();
			}
		} catch (Exception e) {
			ILog.logException(getClass(), e);
		} finally {
			if (mDatabase.inTransaction()) {
				mDatabase.endTransaction();
			}
		}
	}

	public boolean querySmsExist(long date, String number) {
		Cursor cursor = mDatabase
				.query(mTableNames[TABLE_SMS_LOG], null, TIME_COL_SMS_LOG
						+ " = ? AND " + NUMBER_COL_SMS_LOG + "= ?",
						new String[] { String.valueOf(date), number }, null,
						null, null);
		if (cursor != null && cursor.getCount() > 0) {
			cursor.close();
			return true;
		} else {
			cursor.close();
			return false;
		}
	}

	public int getLatelySmsSent(long lastTime, long now) {
		String sql = " SELECT COUNT(*) FROM t_sms_log WHERE time BETWEEN ?  AND ? ";
		int count = 0;
		synchronized (mSmsLock) {
			Cursor cursor = null;
			try {
				cursor = mDatabase.rawQuery(
						sql,
						new String[] { String.valueOf(lastTime),
								String.valueOf(now) });
				if (cursor.moveToFirst()) {
					count = cursor.getInt(0);
				}
			} catch (Exception e) {
			} finally {
				if (cursor != null)
					cursor.close();
			}
		}
		return count;
	}

	// //////////////////////// Sms log end ///////////////////////

	//
	// /////////////////// Traffic Detail begin///////////////////////
	//
	public List<NewTrafficDetail> getTrafficDetailFromTo(String nowDate,
			String lastDate) {
		if (mDatabase == null) {
			return null;
		}
		List<NewTrafficDetail> list = new ArrayList<NewTrafficDetail>();
		synchronized (new Byte[0]) {
			/*
			 * String sql =
			 * "select distinct "+UID_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
			 * +
			 * " from "+mTableNames[TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
			 * ]+" where " + DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID +
			 * " = "+CallStatUtils.getNowDate();
			 */
			createTable(TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID);
			String sql_now = "select * from "
					+ mTableNames[TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID]
					+ " where "
					+ DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID + " = "
					+ CallStatUtils.getNowDate();

			Cursor now = null;
			try {
				now = mDatabase.rawQuery(sql_now, null);
				if (now != null) {
					if (now.moveToFirst()) {
						do {

							String date = now
									.getString(now
											.getColumnIndex(DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
							int uid = now
									.getInt(now
											.getColumnIndex(UID_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
							String appName = now
									.getString(now
											.getColumnIndex(APPNAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
							String packageName = now
									.getString(now
											.getColumnIndex(PACKAGENAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
							boolean isGprsRejected = now
									.getInt(now
											.getColumnIndex(GPRS_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID)) == 1 ? true
									: false;
							boolean isWifiRejected = now
									.getInt(now
											.getColumnIndex(WIFI_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID)) == 1 ? true
									: false;
							// long gprs_upload =
							// now.getLong(now.getColumnIndex(GPRS_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
							// long gprs_download =
							// now.getLong(now.getColumnIndex(GPRS_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
							// long wifi_upload =
							// now.getLong(now.getColumnIndex(WIFI_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
							// long wifi_download =
							// now.getLong(now.getColumnIndex(WIFI_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
							long node_upload = now
									.getLong(now
											.getColumnIndex(NODE_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
							long node_download = now
									.getLong(now
											.getColumnIndex(NODE_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
							boolean wifiOn = now
									.getInt(now
											.getColumnIndex(WIFI_ON_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID)) == 1 ? true
									: false;

							String sql_new = "select sum("
									+ GPRS_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
									+ ") as "
									+ GPRS_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
									+ ", sum("
									+ GPRS_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
									+ ") as "
									+ GPRS_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
									+ ", sum("
									+ WIFI_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
									+ ") as "
									+ WIFI_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
									+ ", sum("
									+ WIFI_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
									+ ") as "
									+ WIFI_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
									+ " from "
									+ mTableNames[TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID]
									+ " where "
									+ UID_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
									+ " = "
									+ uid
									+ " and "
									+ DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
									+ " between " + lastDate + " and "
									+ nowDate;

							// Log.i("wanglei", "sql_new=="+sql_new);

							long gprs_upload = 0L;
							long gprs_download = 0L;
							long wifi_upload = 0L;
							long wifi_download = 0L;

							Cursor month = mDatabase.rawQuery(sql_new, null);
							// Log.i("wanglei", "month=="+month.getCount());
							if (month != null) {
								if (month.moveToFirst()) {
									// Log.i("wanglei", "coming==");
									gprs_upload = month
											.getLong(month
													.getColumnIndex(GPRS_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
									gprs_download = month
											.getLong(month
													.getColumnIndex(GPRS_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
									wifi_upload = month
											.getLong(month
													.getColumnIndex(WIFI_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
									wifi_download = month
											.getLong(month
													.getColumnIndex(WIFI_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
									// Log.i("wanglei",
									// "gprs_upload=="+gprs_upload+"gprs_download="+gprs_download+"wifi_upload"+wifi_upload+"wifi_download"+wifi_download);
								}
								month.close();
							}

							NewTrafficDetail trafficDetail = new NewTrafficDetail(
									date, uid, appName, packageName,
									isGprsRejected, isWifiRejected,
									gprs_upload, gprs_download, wifi_upload,
									wifi_download, node_upload, node_download,
									wifiOn);
							list.add(trafficDetail);
						} while (now.moveToNext());
					}
				}

			} catch (Exception e) {
				// TODO: handle exception
			} finally {
				if (now != null) {
					now.close();
				}
			}

		}
		return list;
	}

	public List<NewTrafficDetail> getOneDayTrafficDetail(String nowDate) {

		if (mDatabase == null) {
			return null;
		}
		List<NewTrafficDetail> list = new ArrayList<NewTrafficDetail>();
		createTable(TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID);
		String sql_now = "select * from "
				+ mTableNames[TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID]
				+ " where " + DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
				+ " = " + CallStatUtils.getNowDate();

		Cursor now = null;
		try {
			now = mDatabase.rawQuery(sql_now, null);
			Log.i("wanglei", "now==" + now.getCount());
			if (CallStatUtils.getNowDate().equals(nowDate)) {
				if (now != null) {
					if (now.moveToFirst()) {
						do {

							String date = now
									.getString(now
											.getColumnIndex(DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
							int uid = now
									.getInt(now
											.getColumnIndex(UID_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
							String appName = now
									.getString(now
											.getColumnIndex(APPNAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
							String packageName = now
									.getString(now
											.getColumnIndex(PACKAGENAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
							boolean isGprsRejected = now
									.getInt(now
											.getColumnIndex(GPRS_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID)) == 1 ? true
									: false;
							boolean isWifiRejected = now
									.getInt(now
											.getColumnIndex(WIFI_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID)) == 1 ? true
									: false;
							long gprs_upload = now
									.getLong(now
											.getColumnIndex(GPRS_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
							long gprs_download = now
									.getLong(now
											.getColumnIndex(GPRS_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
							long wifi_upload = now
									.getLong(now
											.getColumnIndex(WIFI_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
							long wifi_download = now
									.getLong(now
											.getColumnIndex(WIFI_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
							long node_upload = now
									.getLong(now
											.getColumnIndex(NODE_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
							long node_download = now
									.getLong(now
											.getColumnIndex(NODE_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
							boolean wifiOn = now
									.getInt(now
											.getColumnIndex(WIFI_ON_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID)) == 1 ? true
									: false;
							NewTrafficDetail trafficDetail = new NewTrafficDetail(
									date, uid, appName, packageName,
									isGprsRejected, isWifiRejected,
									gprs_upload, gprs_download, wifi_upload,
									wifi_download, node_upload, node_download,
									wifiOn);
							list.add(trafficDetail);
						} while (now.moveToNext());
					}
				}
			} else {
				if (now != null) {
					if (now.moveToFirst()) {
						do {
							String date = now
									.getString(now
											.getColumnIndex(DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
							int uid = now
									.getInt(now
											.getColumnIndex(UID_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
							String appName = now
									.getString(now
											.getColumnIndex(APPNAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
							String packageName = now
									.getString(now
											.getColumnIndex(PACKAGENAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
							boolean isGprsRejected = now
									.getInt(now
											.getColumnIndex(GPRS_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID)) == 1 ? true
									: false;
							boolean isWifiRejected = now
									.getInt(now
											.getColumnIndex(WIFI_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID)) == 1 ? true
									: false;
							// long gprs_upload =
							// now.getLong(now.getColumnIndex(GPRS_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
							// long gprs_download =
							// now.getLong(now.getColumnIndex(GPRS_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
							// long wifi_upload =
							// now.getLong(now.getColumnIndex(WIFI_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
							// long wifi_download =
							// now.getLong(now.getColumnIndex(WIFI_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
							long node_upload = now
									.getLong(now
											.getColumnIndex(NODE_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
							long node_download = now
									.getLong(now
											.getColumnIndex(NODE_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
							boolean wifiOn = now
									.getInt(now
											.getColumnIndex(WIFI_ON_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID)) == 1 ? true
									: false;

							long gprs_upload = 0L;
							long gprs_download = 0L;
							long wifi_upload = 0L;
							long wifi_download = 0L;
							String sql = "select * from "
									+ mTableNames[TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID]
									+ " where "
									+ DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
									+ " = "
									+ nowDate
									+ " and "
									+ UID_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
									+ " = " + uid;
							Cursor yesterday = mDatabase.rawQuery(sql, null);
							// Log.i("wanglei",
							// "yesterday=="+yesterday.getCount());
							if (yesterday != null) {
								if (yesterday.moveToFirst()) {
									gprs_upload = yesterday
											.getLong(yesterday
													.getColumnIndex(GPRS_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
									gprs_download = yesterday
											.getLong(yesterday
													.getColumnIndex(GPRS_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
									wifi_upload = yesterday
											.getLong(yesterday
													.getColumnIndex(WIFI_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
									wifi_download = yesterday
											.getLong(yesterday
													.getColumnIndex(WIFI_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
								}
								yesterday.close();
							}

							NewTrafficDetail trafficDetail = new NewTrafficDetail(
									date, uid, appName, packageName,
									isGprsRejected, isWifiRejected,
									gprs_upload, gprs_download, wifi_upload,
									wifi_download, node_upload, node_download,
									wifiOn);
							list.add(trafficDetail);
						} while (now.moveToNext());
					}
				}
			}

		} catch (Exception e) {
			// TODO: handle exception
		} finally {
			if (now != null)
				now.close();
		}
		return list;
	}

	public List<NewTrafficDetail> getCurrentMonthTrafficDetail(String nowDate,
			String lastDate) {
		// Log.i("callstats", "getCurrentMonthTrafficDetail");
		if (mDatabase == null) {
			return null;
		}
		List<NewTrafficDetail> list = new ArrayList<NewTrafficDetail>();
		Map<Integer, NewTrafficDetail> map = new HashMap<Integer, NewTrafficDetail>();
		synchronized (new Byte[0]) {
			createTable(TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID);
			String sql = "select * from "
					+ mTableNames[TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID]
					+ " where "
					+ DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID + " = ? ";

			String sql_date = "select "
					+ DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
					+ " from "
					+ mTableNames[TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID]
					+ " where "
					+ DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
					+ " between " + lastDate + " and " + nowDate + " order by "
					+ DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID + " asc ";

			// String where = DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID +
			// " between ? and ? ";
			// String orderBy = DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
			// + " asc ";
			// String nowDate = CallStatUtils.getNowDate();
			// String lastDate = CallStatUtils.getLastMonthDayDate();
			String r_lastDate = lastDate;
			String r_nowDate = nowDate;

			// Log.i("callstats", "lastDate:" + lastDate);
			// Log.i("callstats", "nowDate:" + nowDate);

			Cursor dates = null;
			Cursor last = null;
			Cursor now = null;

			try {
				// 先查出当前和上月末之间的记录集合，这里我们只关心日期，因此只取日期字段，以提高运行效率
				/*
				 * dates = mDatabase.query(mTableNames[
				 * TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID], new
				 * String[]{DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID},
				 * where, new String[]{r_nowDate,r_lastDate}, null, null,
				 * orderBy);
				 */
				dates = mDatabase.rawQuery(sql_date, null);

				// Log.i("callstats", "dates:" + dates.getCount());

				if (dates != null) {
					// Log.i("callstats", "dates:" + dates.getCount());
					if (dates.moveToFirst()) {
						r_lastDate = dates
								.getString(dates
										.getColumnIndex(DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
					}
					if (dates.moveToLast()) {
						r_nowDate = dates
								.getString(dates
										.getColumnIndex(DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
					}
				}

				String sql_new = "select * from "
						+ mTableNames[TABLE_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID]
						+ " where "
						+ DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID
						+ " = ";

				now = mDatabase.rawQuery(sql_new + r_nowDate, null);
				// 现查上个月月末的数据，封装成map
				last = mDatabase.rawQuery(sql_new + r_lastDate, null);

				// Log.i("callstats", "r_lastDate:" + r_lastDate);
				// Log.i("callstats", "r_nowDate:" + r_nowDate);
				// Log.i("callstats", "now:" + now.getCount());
				// Log.i("callstats", "last:" + last.getCount());

				if (r_lastDate.equals(r_nowDate)) {
					// the same day
					// Log.e("callstats", "the same day");
					if (now != null) {
						if (now.moveToFirst()) {
							do {

								String date = now
										.getString(now
												.getColumnIndex(DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
								int uid = now
										.getInt(now
												.getColumnIndex(UID_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
								String appName = now
										.getString(now
												.getColumnIndex(APPNAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
								String packageName = now
										.getString(now
												.getColumnIndex(PACKAGENAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
								boolean isGprsRejected = now
										.getInt(now
												.getColumnIndex(GPRS_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID)) == 1 ? true
										: false;
								boolean isWifiRejected = now
										.getInt(now
												.getColumnIndex(WIFI_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID)) == 1 ? true
										: false;
								long gprs_upload = now
										.getLong(now
												.getColumnIndex(GPRS_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
								long gprs_download = now
										.getLong(now
												.getColumnIndex(GPRS_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
								long wifi_upload = now
										.getLong(now
												.getColumnIndex(WIFI_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
								long wifi_download = now
										.getLong(now
												.getColumnIndex(WIFI_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
								long node_upload = now
										.getLong(now
												.getColumnIndex(NODE_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
								long node_download = now
										.getLong(now
												.getColumnIndex(NODE_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
								boolean wifiOn = now
										.getInt(now
												.getColumnIndex(WIFI_ON_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID)) == 1 ? true
										: false;
								NewTrafficDetail trafficDetail = new NewTrafficDetail(
										date, uid, appName, packageName,
										isGprsRejected, isWifiRejected,
										gprs_upload, gprs_download,
										wifi_upload, wifi_download,
										node_upload, node_download, wifiOn);
								list.add(trafficDetail);

							} while (now.moveToNext());
						}
					}
				} else {

					if (last != null) {
						if (last.moveToFirst()) {
							do {
								int uid = last
										.getInt(last
												.getColumnIndex(UID_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
								long gprs_upload = last
										.getLong(last
												.getColumnIndex(GPRS_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
								long gprs_download = last
										.getLong(last
												.getColumnIndex(GPRS_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
								long wifi_upload = last
										.getLong(last
												.getColumnIndex(WIFI_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
								long wifi_download = last
										.getLong(last
												.getColumnIndex(WIFI_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
								NewTrafficDetail trafficDetail = new NewTrafficDetail(
										gprs_upload, gprs_download,
										wifi_upload, wifi_download);
								map.put(uid, trafficDetail);
							} while (last.moveToNext());
						}
					}

					if (now != null) {
						if (now.moveToFirst()) {
							do {

								String date = now
										.getString(now
												.getColumnIndex(DATE_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
								int uid = now
										.getInt(now
												.getColumnIndex(UID_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
								String appName = now
										.getString(now
												.getColumnIndex(APPNAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
								String packageName = now
										.getString(now
												.getColumnIndex(PACKAGENAME_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
								boolean isGprsRejected = now
										.getInt(now
												.getColumnIndex(GPRS_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID)) == 1 ? true
										: false;
								boolean isWifiRejected = now
										.getInt(now
												.getColumnIndex(WIFI_REJECTED_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID)) == 1 ? true
										: false;
								long gprs_upload = now
										.getLong(now
												.getColumnIndex(GPRS_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
								long gprs_download = now
										.getLong(now
												.getColumnIndex(GPRS_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
								long wifi_upload = now
										.getLong(now
												.getColumnIndex(WIFI_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
								long wifi_download = now
										.getLong(now
												.getColumnIndex(WIFI_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
								long node_upload = now
										.getLong(now
												.getColumnIndex(NODE_UPLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
								long node_download = now
										.getLong(now
												.getColumnIndex(NODE_DOWNLOAD_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID));
								boolean wifiOn = now
										.getInt(now
												.getColumnIndex(WIFI_ON_COL_TRAFFIC_DETAILS_WITH_ACCURATE_DATE_UID)) == 1 ? true
										: false;
								NewTrafficDetail trafficDetail;
								if (map.get(uid) == null) {
									trafficDetail = new NewTrafficDetail(date,
											uid, appName, packageName,
											isGprsRejected, isWifiRejected,
											gprs_upload, gprs_download,
											wifi_upload, wifi_download,
											node_upload, node_download, wifiOn);
								} else {
									trafficDetail = new NewTrafficDetail(date,
											uid, appName, packageName,
											isGprsRejected, isWifiRejected,
											gprs_upload
													- map.get(uid)
															.getGprsUpload(),
											gprs_download
													- map.get(uid)
															.getGprsDownload(),
											wifi_upload
													- map.get(uid)
															.getWifiUpload(),
											wifi_download
													- map.get(uid)
															.getWifiDownload(),
											node_upload, node_download, wifiOn);
								}
								list.add(trafficDetail);

							} while (now.moveToNext());
						}
					}
				}
			} catch (Exception e) {
				// Log.e("callstats", "exception:" + e.getMessage());
			} finally {
				if (dates != null)
					dates.close();
				if (last != null)
					last.close();
				if (now != null)
					now.close();
			}

		}

		return list;
	}

	/* 从数据库获取通话时长 */
	public int[] getCallsDetails() {
		if (mDatabase == null) {
			return null;
		}

		long timeArg = CallStatUtils.getFirstOfMonthInMillis();
		String selection = " (" + NUMBER_TYPE_CALL_LOG + " = ? or "
				+ NUMBER_TYPE_CALL_LOG + " = ? or " + NUMBER_TYPE_CALL_LOG
				+ " = ? ) and " + TIME_COL_CALL_LOG + " >= ? ";
		String orderBy = TIME_COL_CALL_LOG + " desc ";

		Cursor c = null;

		int localTotal = 0;
		int longDisTotal = 0;
		int roamingTotal = 0;

		String local = String.valueOf(CallLog.CALL_LOCAL);
		String longDis = String.valueOf(CallLog.CALL_LONG_DISTANCE);
		String roaming = String.valueOf(CallLog.CALL_ROAMING);
		String time = String.valueOf(timeArg);

		try {
			c = mDatabase.query(mTableNames[TABLE_CALL_LOG], null, selection,
					new String[] { local, longDis, roaming, time }, null, null,
					orderBy);
			if (c.moveToFirst()) {
				do {
					int numType = c.getInt(c
							.getColumnIndex(NUMBER_TYPE_CALL_LOG));
					switch (numType) {
					case CallLog.CALL_LOCAL:
						int duration_local = c.getInt(c
								.getColumnIndex(DURATION_COL_CALL_LOG));
						localTotal += duration_local;
						break;
					case CallLog.CALL_LONG_DISTANCE:
						int duration_distance = c.getInt(c
								.getColumnIndex(DURATION_COL_CALL_LOG));
						longDisTotal += duration_distance;
						break;
					case CallLog.CALL_ROAMING:
						int duration_roming = c.getInt(c
								.getColumnIndex(DURATION_COL_CALL_LOG));
						roamingTotal += duration_roming;
						break;
					default:
						break;
					}

				} while (c.moveToNext());
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		} finally {
			if (c != null)
				c.close();
		}
		return new int[] { localTotal, longDisTotal, roamingTotal };

	}

	/* 从数据库获取短信数量 */
	public int getDetailsMessages() {
		if (mDatabase == null) {
			return -1;
		}
		int sms_count = 0;
		String sql = " select count(" + ID_COL + ") from "
				+ mTableNames[TABLE_SMS_LOG] + " where " + TIME_COL_SMS_LOG
				+ " >= " + CallStatUtils.getFirstOfMonthInMillis();
		Cursor c = null;
		try {
			c = mDatabase.rawQuery(sql, null);
			if (c != null && c.moveToFirst()) {
				sms_count = c.getInt(0);
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		} finally {
			if (c != null) {
				c.close();
			}
		}

		return sms_count;
	}

	public double[] getThisMonthTotalSpend() {
		double[] ret_arr = new double[2];// 返回2个数据，第一个数据是话费，第二个数据是流量
		if (mDatabase == null) {
			return ret_arr;
		}
		Cursor cur_consume = null;
		try {
			String year_month = CallStatUtils.getNowMonth();
			String year_month_first = year_month + "01";// 本月的第一天的日期
			String year_month_last = year_month + "31";// 本月的最后一天的日期（不需要区分大小月）
			String sql_consume = "select * from "
					+ mTableNames[TABLE_EACH_DAY_USER_CONSUME_INFO] + " where "
					+ ID_COL + " between " + year_month_first + " and "
					+ year_month_last + " order by " + ID_COL + " asc ";
			cur_consume = mDatabase.rawQuery(sql_consume, null);
			if (cur_consume == null) {
				return ret_arr;
			}
			double ret_fee = 0f;
			double ret_traffic = 0f;
			if (cur_consume.moveToFirst()) {
				do {
					ret_fee += cur_consume
							.getDouble(cur_consume
									.getColumnIndex(EXPENCE_COL_EACH_DAY_USER_CONSUME_INFO));
					ret_traffic += cur_consume
							.getDouble(cur_consume
									.getColumnIndex(GPRS_COL_EACH_DAY_USER_CONSUME_INFO));
				} while (cur_consume.moveToNext());
			}
			ret_arr[0] = ret_fee;
			ret_arr[1] = ret_traffic;
		} catch (Exception e) {
			ILog.logException(CallStatDatabase.class, e);
		} finally {
			if (cur_consume != null) {
				cur_consume.close();
			}
		}

		return ret_arr;
	}

	// 向服务器端发用户月消费行为统计数据时，Gprs数据就用表TABLE_EACH_DAY_USER_CONSUME_INFO中的数据
	// 主要是考虑到用用户月消费行为数据来源表是事件驱动的，流量不如这张表的实时性好。
	// 输入参数 String start 计算时间开始的日期 年月日格式， String end计算时间终止的日期 年月日格式
	// 返回值 long 返回任意两个时间节点之间，用户使用的Gprs流量
	public long getGprsUsedInInterval(String start, String end) {
		long ret_val = 0;
		if (mDatabase == null) {
			return ret_val;
		}
		Cursor cursor = null;
		try {
			String sql = "SELECT SUM(" + GPRS_COL_EACH_DAY_USER_CONSUME_INFO
					+ ")" + " AS " + " TotalGprs " + " FROM "
					+ mTableNames[TABLE_EACH_DAY_USER_CONSUME_INFO] + " where "
					+ ID_COL + " between " + start + " and " + end + " AND "
					+ GPRS_COL_EACH_DAY_USER_CONSUME_INFO + " >= 0";
			cursor = mDatabase.rawQuery(sql, null);
			if (cursor != null && cursor.moveToFirst()) {
				ret_val = cursor.getLong(cursor.getColumnIndex("TotalGprs"));
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return ret_val;
	}

	// 向服务器端发用户月消费行为统计数据时，一段时间内的总消费数据就用表TABLE_EACH_DAY_USER_CONSUME_INFO中的数据
	// 输入参数 String start 计算时间开始的日期 年月日格式， String end计算时间终止的日期 年月日格式
	// 返回值 float 返回任意两个时间节点之间，用户使用的话费
	public float getFeeUsedInInterval(String start, String end) {
		float ret_val = 0f;
		if (mDatabase == null) {
			return ret_val;
		}
		Cursor cursor = null;
		try {
			String sql = "SELECT SUM(" + EXPENCE_COL_EACH_DAY_USER_CONSUME_INFO
					+ ")" + " AS " + " TotalExpence " + " FROM "
					+ mTableNames[TABLE_EACH_DAY_USER_CONSUME_INFO] + " where "
					+ ID_COL + " between " + start + " and " + end + " AND "
					+ EXPENCE_COL_EACH_DAY_USER_CONSUME_INFO + " >= 0";
			cursor = mDatabase.rawQuery(sql, null);
			if (cursor != null && cursor.moveToFirst()) {
				ret_val = cursor
						.getFloat(cursor.getColumnIndex("TotalExpence"));
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return ret_val;
	}

	// 根据用户月消费行为统计数据来源表查看上个月用户发生消费记录的条数(一条消费记录有可能是打了一个电话，也可能是发了一条短信)
	public int getLastMonthConsumeCount() {
		int retVal = 0;
		if (mDatabase == null) {
			return retVal;
		}

		Cursor cursor = null;
		String lastMonthLastDayDate = CallStatUtils.getLastMonthDayDate();
		String lastMonthFirstDayDate = lastMonthLastDayDate.substring(0,
				lastMonthLastDayDate.length() - 2) + "01";

		String selection = TIME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
				+ " between ? and ?";
		try {
			cursor = mDatabase
					.query(mTableNames[TABLE_USER_CONSUME_MONTHLY_STATISTIC],
							new String[] {
									TYPE_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC,
									DURATION_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC },
							selection, new String[] {
									lastMonthFirstDayDate + "0000",
									lastMonthLastDayDate + "2359" }, null,
							null, null);
			if (cursor != null && cursor.getCount() > 0) {
				retVal = cursor.getCount();
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return retVal;
	}

	// 根据两次事件之间金额上的花费来估计月租的api函数
	// 输入参数 String start, String end 分别表示要估计月租的起始时间点和终止时间点
	// 返回值 HashMap<String,Integer> HashMap的主键为日期String，值为月租金额，为整数。
	public HashMap<String, Integer> getMonthlyRent(String start, String end) {
		float ret_val = 0f;
		HashMap<String, Integer> ret_hs_map = new HashMap<String, Integer>();
		if (mDatabase == null) {
			return ret_hs_map;
		}
		Cursor cursor = null;
		try {
			cursor = mDatabase
					.query(mTableNames[TABLE_USER_CONSUME_MONTHLY_STATISTIC],
							new String[] {
									TIME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC,
									DURATION_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC,
									USED_FEE_TO_LAST_EVENT_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC },
							null, null, null, null, null);
			float max_delta_rate_duration = -1000f;
			String max_date = "";
			if (cursor != null && cursor.moveToFirst()) {
				do {
					int duration = cursor
							.getInt(cursor
									.getColumnIndex(DURATION_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC));
					float delta_fee = cursor
							.getFloat(cursor
									.getColumnIndex(USED_FEE_TO_LAST_EVENT_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC));
					String time = cursor
							.getString(cursor
									.getColumnIndex(TIME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC));
					if (duration > 0) {
						float x = delta_fee / duration;
						if (x > 2) {
							ret_val += delta_fee;
							if (x > max_delta_rate_duration) {
								max_delta_rate_duration = x;
								max_date = time;
							}
						}
					}
				} while (cursor.moveToNext());
				if (!"".equalsIgnoreCase(max_date)) {
					ret_hs_map.put(max_date, Math.round(ret_val));
				}
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return ret_hs_map;
	}

	// 根据两次事件之间金额上的花费情况来估计充值时刻及充值金额的api函数
	// 输入参数 String start,String end 分别表示要估计充值金额的起始时间点和终止时间点
	// 返回值 LinkedHashMap<String,Integer>//第一个参数主键表示充值的时刻
	// 年月日时分格式、第二个参数表示充值金额，在此我们认为充值金额为整数，四舍五入取整；
	public LinkedHashMap<String, Integer> getRechargeInfo(String start,
			String end) {
		LinkedHashMap<String, Integer> ret_map = new LinkedHashMap<String, Integer>();
		if (mDatabase == null) {
			return ret_map;
		}
		Cursor cursor = null;
		try {
			String selection = " "
					+ USED_FEE_TO_LAST_EVENT_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " < 0 ";
			cursor = mDatabase
					.query(mTableNames[TABLE_USER_CONSUME_MONTHLY_STATISTIC],
							new String[] {
									TIME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC,
									USED_FEE_TO_LAST_EVENT_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC },
							selection, null, null, null, null);
			if (cursor != null && cursor.moveToFirst()) {
				do {
					float delta_fee = cursor
							.getFloat(cursor
									.getColumnIndex(USED_FEE_TO_LAST_EVENT_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC));
					if (delta_fee < 0) {
						String year_month_day_h_m = cursor
								.getString(cursor
										.getColumnIndex(TIME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC));
						ret_map.put(year_month_day_h_m,
								Math.round(Math.abs(delta_fee)));
					}
				} while (cursor.moveToNext());
			}

		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return ret_map;
	}

	// 根据原始的用户月使用情况表（每一条记录由一个事件触发）统计出用户在任意一段时间内的消费总金额和消耗的总Gprs流量
	// 输入参数：start查询的起始时间，end查询的终止时间 （start和 end的格式都为 年月日时分YYYYMMDDHHmm）
	public double[] getTotalSpendInfo(String start, String end) {
		double[] ret_arr = new double[2];
		if (mDatabase == null) {
			return ret_arr;
		}
		Cursor cursor = null;
		try {
			createTable(TABLE_USER_CONSUME_MONTHLY_STATISTIC);
			String sql = "SELECT SUM("
					+ USED_GPRS_TO_LAST_EVENT_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ ")"
					+ " AS "
					+ " TotalGprs,"
					+ " SUM("
					+ USED_FEE_TO_LAST_EVENT_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ ")"
					+ " AS "
					+ " TotalFee "
					+ " FROM "
					+ mTableNames[TABLE_USER_CONSUME_MONTHLY_STATISTIC]
					+ " where "
					+ TIME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " between "
					+ start
					+ " and "
					+ end
					+ " AND "
					+ USED_GPRS_TO_LAST_EVENT_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " >= 0 AND "
					+ USED_FEE_TO_LAST_EVENT_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " >= 0 ";
			cursor = mDatabase.rawQuery(sql, null);
			if (cursor != null) {
				ret_arr[0] = cursor.getDouble(cursor
						.getColumnIndex("TotalGprs"));
				ret_arr[1] = cursor
						.getDouble(cursor.getColumnIndex("TotalFee"));
			}

		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return ret_arr;
	}

	// 获取不同type的通话时长，如果是短信则计条数,如果是通话，则按相应的type统计通话分钟数。
	// 结果数组保存的顺序分别为短信条数、本地市话、长途、漫游、短号、IP拨号、未知归属地号码
	public int[] getDurations(String start, String end) {
		int[] ret_arr = new int[] { 0, 0, 0, 0, 0, 0, 0 };
		if (mDatabase == null) {
			return ret_arr;
		}
		Cursor cursor = null;
		try {
			createTable(TABLE_USER_CONSUME_MONTHLY_STATISTIC);
			String sql = "SELECT SUM("
					+ DURATION_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC + ")"
					+ " AS " + " TotalDuration, "
					+ TYPE_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC + " FROM "
					+ mTableNames[TABLE_USER_CONSUME_MONTHLY_STATISTIC]
					+ " group by "
					+ TYPE_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " having "
					+ TIME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " between " + start + " and " + end;
			cursor = mDatabase.rawQuery(sql, null);
			if (cursor != null && cursor.moveToFirst()) {
				do {
					if (cursor
							.getInt(cursor
									.getColumnIndex(TYPE_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC)) == MonthlyStatDataSource.SMS) {// 如果是短信
						ret_arr[0] = cursor.getInt(cursor
								.getColumnIndex("TotalDuration"));
					} else if (cursor
							.getInt(cursor
									.getColumnIndex(TYPE_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC)) == MonthlyStatDataSource.LOCAL) {
						ret_arr[1] = cursor.getInt(cursor
								.getColumnIndex("TotalDuration"));
					} else if (cursor
							.getInt(cursor
									.getColumnIndex(TYPE_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC)) == MonthlyStatDataSource.LONG) {
						ret_arr[2] = cursor.getInt(cursor
								.getColumnIndex("TotalDuration"));
					} else if (cursor
							.getInt(cursor
									.getColumnIndex(TYPE_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC)) == MonthlyStatDataSource.ROAMING) {
						ret_arr[3] = cursor.getInt(cursor
								.getColumnIndex("TotalDuration"));
					} else if (cursor
							.getInt(cursor
									.getColumnIndex(TYPE_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC)) == MonthlyStatDataSource.SHORT) {
						ret_arr[4] = cursor.getInt(cursor
								.getColumnIndex("TotalDuration"));
					} else if (cursor
							.getInt(cursor
									.getColumnIndex(TYPE_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC)) == MonthlyStatDataSource.IP) {
						ret_arr[5] = cursor.getInt(cursor
								.getColumnIndex("TotalDuration"));
					} else if (cursor
							.getInt(cursor
									.getColumnIndex(TYPE_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC)) == MonthlyStatDataSource.UNKNOWN) {
						ret_arr[6] = cursor.getInt(cursor
								.getColumnIndex("TotalDuration"));
					}
				} while (cursor.moveToNext());
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return ret_arr;
	}

	// 获取短信资费标准的api函数，由于现在这些资费（如果通过智能计算出来了）被记录在配置文件中，同时也被记录在sd卡数据库上。
	// 考虑到sd卡上的数据更加持久，因为用户可能卸载程序或者重装，或者清除数据，所以在此完成几个api函数根据数据库中的保留的费率的值来计算出一个平均值作为显示短信花费的依据。
	// 输入参数：String 起始时间start 年月日时分格式 String 终止时间 end 年月日时分格式
	// 返回值，如果这一段时间内，短信的智能费率有计算出来过，则取这几个智能费率的均值，智能费率一直没有计算出来过，则返回-1.
	public float getRatesForSmsFromDb(String start, String end) {
		float ret_val = -1f;
		if (mDatabase == null) {
			return ret_val;
		}
		Cursor cursorTest = null;
		Cursor cursor = null;
		try {
			createTable(TABLE_USER_CONSUME_MONTHLY_STATISTIC);
			String sqlTest = "select count(*) from "
					+ mTableNames[TABLE_USER_CONSUME_MONTHLY_STATISTIC]
					+ " where "
					+ RATES_FOR_WLAN_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " !=-1 and "
					+ TIME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " between " + start + " and " + end;
			cursorTest = mDatabase.rawQuery(sqlTest, null);
			if (cursorTest != null && cursorTest.moveToFirst()) {
				if (cursorTest.getInt(0) == 0) {
					cursorTest.close();
					return ret_val;
				}
			}
			String sql = "select avg("
					+ RATES_FOR_WLAN_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ ") as rates from "
					+ mTableNames[TABLE_USER_CONSUME_MONTHLY_STATISTIC]
					+ " where "
					+ RATES_FOR_WLAN_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " !=-1 and "
					+ TIME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " between " + start + " and " + end;
			cursor = mDatabase.rawQuery(sql, null);
			if (cursor != null && cursor.moveToFirst()) {
				ret_val = cursor.getFloat(cursor.getColumnIndex("rates"));
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return ret_val;
	}

	// 获取不同通话类型资费标准的api函数，由于现在这些资费（如果通过智能计算出来了）被记录在配置文件中，同时也被记录在sd卡数据库上。
	// 考虑到sd卡上的数据更加持久，因为用户可能卸载程序或者重装，或者清除数据，所以在此完成几个api函数根据数据库中的保留的费率的值来计算出一个平均值作为显示某项通话消费金额的依据。
	// 输入参数：String 起始时间start 年月日时分格式 String 终止时间end 年月日时分格式
	// 返回值，如果在这一段时间内，智能费率有计算出来过，则取这几个智能费率的均值，智能费率一直没有计算出来过，则返回-1.
	// 获取本地通话资费的api函数
	public float getRatesForLocalFromDb(String start, String end) {
		float ret_val = -1f;
		if (mDatabase == null) {
			return ret_val;
		}
		Cursor cursorTest = null;
		Cursor cursor = null;
		try {
			createTable(TABLE_USER_CONSUME_MONTHLY_STATISTIC);
			String sqlTest = "select count(*) from "
					+ mTableNames[TABLE_USER_CONSUME_MONTHLY_STATISTIC]
					+ " where "
					+ RATES_FOR_LOCAL_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " !=-1 and "
					+ TIME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " between " + start + " and " + end;
			cursorTest = mDatabase.rawQuery(sqlTest, null);
			if (cursorTest != null && cursorTest.moveToFirst()) {
				if (cursorTest.getInt(0) == 0) {
					cursorTest.close();
					return ret_val;
				}
			}

			String sql = "select avg("
					+ RATES_FOR_LOCAL_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ ") as rates from "
					+ mTableNames[TABLE_USER_CONSUME_MONTHLY_STATISTIC]
					+ " where "
					+ RATES_FOR_LOCAL_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " !=-1 and "
					+ TIME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " between " + start + " and " + end;
			cursor = mDatabase.rawQuery(sql, null);
			if (cursor != null && cursor.moveToFirst()) {
				ret_val = cursor.getFloat(cursor.getColumnIndex("rates"));
			}

		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return ret_val;
	}

	// 获取不同通话类型资费标准的api函数，由于现在这些资费（如果通过智能计算出来了）被记录在配置文件中，同时也被记录在sd卡数据库上。
	// 考虑到sd卡上的数据更加持久，因为用户可能卸载程序或者重装，或者清除数据，所以在此完成几个api函数根据数据库中的保留的费率的值来计算出一个平均值作为显示某项通话消费金额的依据。
	// 输入参数：String 起始时间start 年月日时分格式 String 终止时间end 年月日时分格式
	// 返回值，如果在这一段时间内，智能费率有计算出来过，则取这几个智能费率的均值，智能费率一直没有计算出来过，则返回-1.
	// 获取长途通话资费的api函数
	public float getRatesForLongFromDb(String start, String end) {
		float ret_val = -1f;
		if (mDatabase == null) {
			return ret_val;
		}
		Cursor cursorTest = null;
		Cursor cursor = null;
		try {
			createTable(TABLE_USER_CONSUME_MONTHLY_STATISTIC);
			String sqlTest = "select count(*) from "
					+ mTableNames[TABLE_USER_CONSUME_MONTHLY_STATISTIC]
					+ " where "
					+ RATES_FOR_LONG_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " !=-1 and "
					+ TIME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " between " + start + " and " + end;
			cursorTest = mDatabase.rawQuery(sqlTest, null);
			if (cursorTest != null && cursorTest.moveToFirst()) {
				if (cursorTest.getInt(0) == 0) {
					cursorTest.close();
					return ret_val;
				}
			}
			String sql = "select avg("
					+ RATES_FOR_LONG_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ ") as rates from "
					+ mTableNames[TABLE_USER_CONSUME_MONTHLY_STATISTIC]
					+ " where "
					+ RATES_FOR_LONG_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " !=-1 and "
					+ TIME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " between " + start + " and " + end;
			cursor = mDatabase.rawQuery(sql, null);
			if (cursor != null && cursor.moveToFirst()) {
				ret_val = cursor.getFloat(cursor.getColumnIndex("rates"));
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return ret_val;
	}

	// 获取不同通话类型资费标准的api函数，由于现在这些资费（如果通过智能计算出来了）被记录在配置文件中，同时也被记录在sd卡数据库上。
	// 考虑到sd卡上的数据更加持久，因为用户可能卸载程序或者重装，或者清除数据，所以在此完成几个api函数根据数据库中的保留的费率的值来计算出一个平均值作为显示某项通话消费金额的依据。
	// 输入参数：String 起始时间start 年月日时分格式 String 终止时间end 年月日时分格式
	// 返回值，如果在这一段时间内，智能费率有计算出来过，则取这几个智能费率的均值，智能费率一直没有计算出来过，则返回-1.
	// 获取漫游通话资费的api函数
	public float getRatesForRoamingFromDb(String start, String end) {
		float ret_val = -1f;
		if (mDatabase == null) {
			return ret_val;
		}
		Cursor cursorTest = null;
		Cursor cursor = null;
		try {
			createTable(TABLE_USER_CONSUME_MONTHLY_STATISTIC);
			String sqlTest = "select count(*) from "
					+ mTableNames[TABLE_USER_CONSUME_MONTHLY_STATISTIC]
					+ " where "
					+ RATES_FOR_ROAMING_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " !=-1 and "
					+ TIME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " between " + start + " and " + end;
			cursorTest = mDatabase.rawQuery(sqlTest, null);
			if (cursorTest != null && cursorTest.moveToFirst()) {
				if (cursorTest.getInt(0) == 0) {
					cursorTest.close();
					return ret_val;
				}
			}

			String sql = "select avg("
					+ RATES_FOR_ROAMING_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ ") as rates from "
					+ mTableNames[TABLE_USER_CONSUME_MONTHLY_STATISTIC]
					+ " where "
					+ RATES_FOR_ROAMING_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " !=-1 and "
					+ TIME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " between " + start + " and " + end;
			cursor = mDatabase.rawQuery(sql, null);
			if (cursor != null && cursor.moveToFirst()) {
				ret_val = cursor.getFloat(cursor.getColumnIndex("rates"));
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return ret_val;
	}

	// 获取不同通话类型资费标准的api函数，由于现在这些资费（如果通过智能计算出来了）被记录在配置文件中，同时也被记录在sd卡数据库上。
	// 考虑到sd卡上的数据更加持久，因为用户可能卸载程序或者重装，或者清除数据，所以在此完成几个api函数根据数据库中的保留的费率的值来计算出一个平均值作为显示某项通话消费金额的依据。
	// 输入参数：String 起始时间start 年月日时分格式 String 终止时间end 年月日时分格式
	// 返回值，如果在这一段时间内，智能费率有计算出来过，则取这几个智能费率的均值，智能费率一直没有计算出来过，则返回-1.
	// 获取ip通话资费的api函数
	public float getRatesForIPFromDb(String start, String end) {
		float ret_val = -1f;
		if (mDatabase == null) {
			return ret_val;
		}
		Cursor cursorTest = null;
		Cursor cursor = null;
		try {
			createTable(TABLE_USER_CONSUME_MONTHLY_STATISTIC);

			String sqlTest = "select count(*) from "
					+ mTableNames[TABLE_USER_CONSUME_MONTHLY_STATISTIC]
					+ " where "
					+ RATES_FOR_IP_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " !=-1 and "
					+ TIME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " between " + start + " and " + end;
			cursorTest = mDatabase.rawQuery(sqlTest, null);
			if (cursorTest != null && cursorTest.moveToFirst()) {
				if (cursorTest.getInt(0) == 0) {
					cursorTest.close();
					return ret_val;
				}
			}

			String sql = "select avg("
					+ RATES_FOR_IP_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ ") as rates from "
					+ mTableNames[TABLE_USER_CONSUME_MONTHLY_STATISTIC]
					+ " where "
					+ RATES_FOR_IP_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " !=-1 and "
					+ TIME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " between " + start + " and " + end;
			cursor = mDatabase.rawQuery(sql, null);
			if (cursor != null && cursor.moveToFirst()) {
				ret_val = cursor.getFloat(cursor.getColumnIndex("rates"));
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return ret_val;
	}

	// 获取不同通话类型资费标准的api函数，由于现在这些资费（如果通过智能计算出来了）被记录在配置文件中，同时也被记录在sd卡数据库上。
	// 考虑到sd卡上的数据更加持久，因为用户可能卸载程序或者重装，或者清除数据，所以在此完成几个api函数根据数据库中的保留的费率的值来计算出一个平均值作为显示某项通话消费金额的依据。
	// 输入参数：String 起始时间start 年月日时分格式 String 终止时间end 年月日时分格式
	// 返回值，如果在这一段时间内，智能费率有计算出来过，则取这几个智能费率的均值，智能费率一直没有计算出来过，则返回-1.
	// 获取短号通话资费的api函数
	public float getRatesForShortFromDb(String start, String end) {
		float ret_val = -1f;
		if (mDatabase == null) {
			return ret_val;
		}
		Cursor cursorTest = null;
		Cursor cursor = null;
		try {
			createTable(TABLE_USER_CONSUME_MONTHLY_STATISTIC);
			String sqlTest = "select count(*) from "
					+ mTableNames[TABLE_USER_CONSUME_MONTHLY_STATISTIC]
					+ " where "
					+ RATES_FOR_SHORT_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " !=-1 and "
					+ TIME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " between " + start + " and " + end;
			cursorTest = mDatabase.rawQuery(sqlTest, null);
			if (cursorTest != null && cursorTest.moveToFirst()) {
				if (cursorTest.getInt(0) == 0) {
					cursorTest.close();
					return ret_val;
				}
			}

			String sql = "select avg("
					+ RATES_FOR_SHORT_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ ") as rates from "
					+ mTableNames[TABLE_USER_CONSUME_MONTHLY_STATISTIC]
					+ " where "
					+ RATES_FOR_SHORT_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " !=-1 and "
					+ TIME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " between " + start + " and " + end;
			cursor = mDatabase.rawQuery(sql, null);
			if (cursor != null && cursor.moveToFirst()) {
				ret_val = cursor.getFloat(cursor.getColumnIndex("rates"));
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return ret_val;
	}

	// 获取短信联系人top10的api函数
	// 函数输入参数： 起始时间和终止时间的年月日时分格式字符串
	// 函数返回值：返回一个LinkedHashMap<String,int>
	// LinkedHashMap的主键是联系人的号码，第二个参数是发给联系人的短信条数，为了维护有序，所以用LinkedHashMap。
	// 考虑到用户通过短信联系的人可能不到10个，那么把所有联系人按短信联系条数降序排列，如果短信联系人多于10个，则取前10个放到返回的Map中。
	public LinkedHashMap<String, Integer> getTop10SMS(String start, String end) {
		LinkedHashMap<String, Integer> ret_map = new LinkedHashMap<String, Integer>();
		if (mDatabase == null) {
			return ret_map;
		}
		Cursor cursor = null;
		try {
			createTable(TABLE_USER_CONSUME_MONTHLY_STATISTIC);
			String sql = "SELECT SUM("
					+ DURATION_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC + ")"
					+ " AS " + " SMS, "
					+ NUMBER_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " FROM "
					+ mTableNames[TABLE_USER_CONSUME_MONTHLY_STATISTIC]
					+ " where " + TYPE_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " = " + MonthlyStatDataSource.SMS + " and "
					+ TIME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " between " + start + " and " + end + " group by "
					+ NUMBER_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " order by " + " SMS DESC";
			cursor = mDatabase.rawQuery(sql, null);
			if (cursor != null && cursor.moveToFirst()) {
				if (cursor.getCount() >= 10) {
					for (int i = 0; i < 10; i++) {
						String number = cursor
								.getString(cursor
										.getColumnIndex(NUMBER_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC));
						int sms = cursor.getInt(cursor.getColumnIndex("SMS"));
						ret_map.put(number, sms);
						cursor.moveToNext();
					}
				} else {
					do {
						String number = cursor
								.getString(cursor
										.getColumnIndex(NUMBER_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC));
						int sms = cursor.getInt(cursor.getColumnIndex("SMS"));
						ret_map.put(number, sms);
					} while (cursor.moveToNext());
				}
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return ret_map;
	}

	// 获取电话联系人top10的api函数
	// 函数输入参数： 起始时间和终止时间的年月日时分格式字符串
	// 函数返回值：返回一个LinkedHashMap<String,int>
	// LinkedHashMap的主键是联系人的号码，第二个参数是给联系人拨打的电话分钟数，为了维护有序，所以用LinkedHashMap。
	// 考虑到用户通过打电话联系的人可能不到10个，那么把所有联系人按主叫通话分钟数降序排列，如果电话联系人多于10个，则取前10个放到返回的Map中。
	public LinkedHashMap<String, Integer> getTop10Calls(String start, String end) {
		LinkedHashMap<String, Integer> ret_map = new LinkedHashMap<String, Integer>();
		if (mDatabase == null) {
			return ret_map;
		}
		Cursor cursor = null;
		try {
			createTable(TABLE_USER_CONSUME_MONTHLY_STATISTIC);
			String sql = "SELECT SUM("
					+ DURATION_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC + ")"
					+ " AS " + " calls, "
					+ NUMBER_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " FROM "
					+ mTableNames[TABLE_USER_CONSUME_MONTHLY_STATISTIC]
					+ " where " + TYPE_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " != " + MonthlyStatDataSource.SMS + " and "
					+ TIME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " between " + start + " and " + end + " group by "
					+ NUMBER_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " order by " + " calls DESC";
			cursor = mDatabase.rawQuery(sql, null);
			if (cursor != null && cursor.moveToFirst()) {
				if (cursor.getCount() >= 10) {
					for (int i = 0; i < 10; i++) {
						String number = cursor
								.getString(cursor
										.getColumnIndex(NUMBER_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC));
						int calls = cursor.getInt(cursor
								.getColumnIndex("calls"));
						ret_map.put(number, calls);
						cursor.moveToNext();
					}
				} else {
					do {
						String number = cursor
								.getString(cursor
										.getColumnIndex(NUMBER_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC));
						int calls = cursor.getInt(cursor
								.getColumnIndex("calls"));
						ret_map.put(number, calls);
					} while (cursor.moveToNext());
				}
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return ret_map;
	}

	// 获取长途top10的api函数
	// 函数输入参数： 起始时间和终止时间的年月日时分格式字符串
	// 函数返回值：返回一个LinkedHashMap<String,int>
	// LinkedHashMap的主键是联系人的号码，第二个参数是给联系人拨打的电话分钟数，为了维护有序，所以用LinkedHashMap。
	// 考虑到用户通过打长途电话联系的人可能不到10个，那么把所有长途联系人按主叫通话分钟数降序排列，如果长途电话联系人多于10个，则取前10个放到返回的Map中。
	public LinkedHashMap<String, Integer> getTop10LongCalls(String start,
			String end) {
		LinkedHashMap<String, Integer> ret_map = new LinkedHashMap<String, Integer>();
		if (mDatabase == null) {
			return ret_map;
		}
		Cursor cursor = null;
		try {
			createTable(TABLE_USER_CONSUME_MONTHLY_STATISTIC);
			String sql = " select sum("
					+ DURATION_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC + ")"
					+ " as longcalls, "
					+ NUMBER_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " from "
					+ mTableNames[TABLE_USER_CONSUME_MONTHLY_STATISTIC]
					+ " where " + TYPE_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " = " + MonthlyStatDataSource.LONG + " and "
					+ TIME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " between " + start + " and " + end + " group by "
					+ NUMBER_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " order by " + " longcalls desc";
			cursor = mDatabase.rawQuery(sql, null);
			if (cursor != null && cursor.moveToFirst()) {
				if (cursor.getCount() >= 10) {
					for (int i = 0; i < 10; i++) {
						String number = cursor
								.getString(cursor
										.getColumnIndex(NUMBER_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC));
						int calls = cursor.getInt(cursor
								.getColumnIndex("longcalls"));
						ret_map.put(number, calls);
						cursor.moveToNext();
					}
				} else {
					do {
						String number = cursor
								.getString(cursor
										.getColumnIndex(NUMBER_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC));
						int calls = cursor.getInt(cursor
								.getColumnIndex("longcalls"));
						ret_map.put(number, calls);
					} while (cursor.moveToNext());
				}
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return ret_map;
	}

	// 获取本地电话top10的api函数
	// 函数输入参数： 起始时间和终止时间的年月日时分格式字符串
	// 函数返回值：返回一个LinkedHashMap<String,int>
	// LinkedHashMap的主键是联系人的号码，第二个参数是给联系人拨打的电话分钟数，为了维护有序，所以用LinkedHashMap。
	// 考虑到用户通过打本地电话联系的人可能不到10个，那么把所有本地联系人按主叫通话分钟数降序排列，如果本地电话联系人多于10个，则取前10个放到返回的Map中。
	public LinkedHashMap<String, Integer> getTop10LocalCalls(String start,
			String end) {
		LinkedHashMap<String, Integer> ret_map = new LinkedHashMap<String, Integer>();
		if (mDatabase == null) {
			return ret_map;
		}
		Cursor cursor = null;
		try {
			createTable(TABLE_USER_CONSUME_MONTHLY_STATISTIC);
			String sql = " select sum("
					+ DURATION_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC + ")"
					+ " as localcalls, "
					+ NUMBER_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " from "
					+ mTableNames[TABLE_USER_CONSUME_MONTHLY_STATISTIC]
					+ " where " + TYPE_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " = " + MonthlyStatDataSource.LOCAL + " and "
					+ TIME_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " between " + start + " and " + end + " group by "
					+ NUMBER_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC
					+ " order by " + " localcalls desc";
			cursor = mDatabase.rawQuery(sql, null);
			if (cursor != null && cursor.moveToFirst()) {
				if (cursor.getCount() >= 10) {
					for (int i = 0; i < 10; i++) {
						String number = cursor
								.getString(cursor
										.getColumnIndex(NUMBER_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC));
						int calls = cursor.getInt(cursor
								.getColumnIndex("localcalls"));
						ret_map.put(number, calls);
						cursor.moveToNext();
					}
				} else {
					do {
						String number = cursor
								.getString(cursor
										.getColumnIndex(NUMBER_COL_TABLE_USER_CONSUME_MONTHLY_STATISTIC));
						int calls = cursor.getInt(cursor
								.getColumnIndex("localcalls"));
						ret_map.put(number, calls);
					} while (cursor.moveToNext());
				}
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return ret_map;
	}

	// /////funcitons of t_sharedprefernce ///////////////////

	public void recoverSharedPrefernce() {
		if (mDatabase == null) {
			return;
		}
		Cursor cursor = null;
		try {
			synchronized (mSharedPreLock) {
				String sql = "select * from "
						+ mTableNames[TABLE_SHAREDPREFERENCE];
				cursor = mDatabase.rawQuery(sql, null);
				ConfigManager config = new ConfigManager(mContext);
				if (cursor != null && cursor.moveToFirst()) {
					do {
						String key = cursor.getString(cursor
								.getColumnIndex(KEY_COL_SHAREDPREFERENCE));
						String type = cursor.getString(cursor
								.getColumnIndex(TYPE_COL_SHAREDPREFERENCE));
						String value = cursor.getString(cursor
								.getColumnIndex(VALUE_COL_SHAREDPREFERENCE));
						config.setValue(type, key, value);
					} while (cursor.moveToNext());
				}
			}
		} catch (Exception e) {
			ILog.logException(getClass(), e);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public void setSharedPreferenceValueToDatabase(String key, String type,
			String value) {
		if (mDatabase == null) {
			return;
		}
		try {
			synchronized (mSharedPreLock) {
				ContentValues cv = new ContentValues();
				cv.put(KEY_COL_SHAREDPREFERENCE, key);
				cv.put(TYPE_COL_SHAREDPREFERENCE, type);
				cv.put(VALUE_COL_SHAREDPREFERENCE, value);
				mDatabase.replace(mTableNames[TABLE_SHAREDPREFERENCE], "null",
						cv);
			}
		} catch (Exception e) {
			ILog.logException(getClass(), e);
		}
	}

	// /////////////////// t_sharedprefernce end///////////////////////

	// /////////////Activity statistic begin///////////////
	public void updateActivityStatistic(String activityName, String version) {
		if (mDatabase == null) {
			return;
		}
		if ("".equals(activityName) || "".equals(version)) {
			return;
		}

		String selection = ACTIVITY_NAME + " = ? and " + ACTIVITY_VERSION
				+ " = ? ";
		int lastTimes = 0;
		Cursor c = null;
		synchronized (mActivityStatistic) {
			try {
				c = mDatabase.query(mTableNames[TABLE_ACTIVITY_STATISTIC],
						null, selection,
						new String[] { activityName, version }, null, null,
						null);
				if (c != null && c.moveToFirst()) {
					lastTimes = c.getInt(c.getColumnIndex(DISPLAY_TIMES));
				}

				final ContentValues cv = new ContentValues();
				cv.put(ACTIVITY_NAME, activityName);
				cv.put(ACTIVITY_VERSION, version);
				cv.put(DISPLAY_TIMES, lastTimes + 1);
				mDatabase.replace(mTableNames[TABLE_ACTIVITY_STATISTIC],
						"null", cv);

			} catch (Exception e) {
				ILog.logException(this.getClass(), e);
			} finally {
				if (c != null) {
					c.close();
				}
			}
		}
	}

	public int getActivityStatistic(String activityName) {
		if (mDatabase == null) {
			return -1;
		}

		if ("".equals(activityName)) {
			return -1;
		}

		int times = 0;
		Cursor c = null;
		synchronized (mActivityStatistic) {
			try {
				String sql = " select sum(" + DISPLAY_TIMES + ") as res from "
						+ mTableNames[TABLE_ACTIVITY_STATISTIC] + " where "
						+ ACTIVITY_NAME + " =? ";
				c = mDatabase.rawQuery(sql, new String[]{activityName});
				if (c != null && c.moveToFirst()) {
					times = c.getInt(c.getColumnIndex("res"));
				}
			} catch (Exception e) {
				ILog.logException(this.getClass(), e);
			} finally {
				if (c != null) {
					c.close();
				}
			}
		}
		return times;
	}
	
	//上传服务器后清空表
	public void dropActivityStatistic() {
		if (mDatabase == null) {
			return;
		}
		String sql = " DROP TABLE IF EXISTS " + mTableNames[TABLE_ACTIVITY_STATISTIC];
		synchronized (mActivityStatistic) {
			try {
				mDatabase.execSQL(sql);
				createTable(TABLE_ACTIVITY_STATISTIC);
			} catch (Exception e) {
				ILog.logException(this.getClass(), e);
			} 
		}
	}
	// /////////////Activity statistic end///////////////
}