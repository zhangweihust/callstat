package com.android.callstat;

import com.android.callstat.common.database.CallStatDatabase;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;

/**
 * @author lxue
 */
public class ConfigManager extends ContextWrapper {

	public static final long DEFAULT_OFFLINE_INTERVAL = 5 * 60 * 1000;
	public static final long DEFAULT_OFFLINE_TIME = 2 * 60 * 1000;

	/**
	 * whether application launch first
	 */
	public static final String KEY_FIRST_LAUNCH = "first_launch";

	public static final String KEY_BOOT_COMPLETE = "boot_complete";
	/**
	 * off-line download interval
	 */
	public static final String KEY_OFFLINE_INTERVAL = "offline_interval";

	public static final String KEY_ACCOUNTING_DATA_VERSION = "accounting_data_version";

	/**
	 * off-line download task will last how long in every time
	 */
	public static final String KEY_OFFLINE_TIME = "offline_time";

	public static final String KEY_DO_NOT_ALERT_AGAIN = "no_alert";

	public static final String KEY_FEEDBACK = "feddback";

	public static final String KEY_SEND_RECONILITION_FAILED_COUNT = "send_reconilition_failed_count";
	/**
	 * get from user
	 */
	// 用户所在省份
	public static final String KEY_PROVINCE = "province";
	// 用户所在城市
	public static final String KEY_CITY = "city";
	// 用户手机前八位号码
	public static final String KEY_TOP_EIGHT_NUM = "top_eight_num";
	// 运营商
	public static final String KEY_OPERATOR = "operator";
	// 套餐品牌
	public static final String KEY_PACKAGE_BRAND = "package_brand";
	// 月结日
	public static final String KEY_ACCOUNTING_DAY = "accounting_day";
	// 套餐赠送通话时间
	public static final String KEY_FREE_CALLTIME = "free_calltime";
	// 套餐赠送短信条数
	public static final String KEY_FREE_MESSAGES = "free_messages";
	// 套餐赠送gprs流量
	public static final String KEY_FREE_GPRS = "free_gprs";
	// 短信单价
	public static final String KEY_MSGPRICE_UNIT = "msgprice_unit";
	// 流量单价
	public static final String KEY_TRAFFICPRICE_UNIT = "trafficprice_unit";
	// 月租
	public static final String KEY_RENT = "rent";

	// 来电显示提示框位置
	public static final String KEY_CALLNOTICE_SETTING = "callnotice_position";

	// 最近一次对账成功的时间
	public static final String KEY_LASTCHECK_TIME = "lastcheck_time";

	//增加一个字段保存上一次对帐方程成功入库的时间
	public static final String KEY_LAST_ADD_EQUATION_TIME = "last_add_equation_time";
	
	// 保存上次对出 余额 的时间
	public static final String KEY_LASTCHECK_HAS_YE_TIME = "lastcheck_has_ye_time";
	// 保存上次对出 流量 的时间
	public static final String KEY_LASTCHECK_HAS_TRAFFIC_TIME = "lastcheck_has_traffic_time";

	public static final String KEY_IS_FIRST_INSTALLED = "is_first_installed";
	/**
	 * user STAT
	 */
	// 程序启动时间
	public static final String KEY_TIME_ON_STARTUP = "time_on_startup";

	// 程序首次安装后 启动时间
	public static final String KEY_FIRST_TIME_ON_STARTUP = "first_time_on_startup";

	// 程序关闭时间
	public static final String KEY_TIME_ON_SHUTDOWN = "time_on_shutdown";
	// 飞行模式开始时间
	public static final String KEY_AIRMODE_OPEN_TIME = "airmode_open_time";
	// 飞行模式关闭时间
	public static final String KEY_AIRMODE_CLOSE_TIME = "airmode_close_time";
	// 周期内总运行时间，每次累加，发送成功后清零
	public static final String KEY_TIME_TOTAL_RUNNING = "time_total_running";

	// 用户统计总时间 新字段
	public static final String KEY_TOTAL_RUNNING_TIME = "total_running_time_new";
	// 用户统计总时间 日使用时长
	public static final String KEY_DAY_TOTAL_RUNNING_TIME = "day_total_running_time_new";
	// 用户统计，每日使用时间记录
	public static final String KEY_DAY_RUNNING_TIME_EVERY_WEEK = "day_total_running_time_every_week";

	// 周期内总运行次数，每次累加,发送成功后清零
	public static final String KEY_TIMES_USED = "times_used";
	// IMEI
	public static final String KEY_IMEI = "imei";
	// CPU最大频率
	public static final String KEY_CPU_MAX_FREQ = "cpu_max_freq";
	// 收手机内存
	public static final String KEY_MEMORY = "memory";
	// CPU型号
	public static final String KEY_CPU_MODEL = "cpu_model";
	// 软件版本
	public static final String KEY_CALLSTAT_VERSION = "version";
	// 手机型号
	public static final String KEY_MOBILE_MODEL = "mobile_model";
	// 屏幕信息
	public static final String KEY_SCREEN_SIZE = "screen_size";
	// 操作系统版本
	public static final String KEY_OS_VERSION = "os_version";

	// 用户硬件信息是否发送成功
	public static final String KEY_IS_DEVICE_INFO_SENT = "is_device_info_sent";

	/**
	 * core configuration for callstat,such as text message code,cost stat and
	 * so on
	 */
	/** 每日最早话费余额 */
	public static final String KEY_EARLIEST_DAILY_AVAILABLE_FEE = "earliest_daily_available_fee";
	/** 每日最早话费余额写入时的日期 */
	public static final String KEY_EARLIEST_DAILY_AVAILABLE_DATE = "earliest_daily_available_date";

	// imsi
	public static final String KEY_IMSI = "imsi";
	// 运营商号码,数据来自数据库
	public static final String KEY_OPERATOR_NUM = "operator_num";
	// 余额查询指令,以及关键字,协议:同一类之间逗号隔开,指令和关键字冒号隔开
	public static final String KEY_HF_YE_CODE = "HF_YE_CODE";
	public static final String KEY_HF_USED_CODE = "HF_USED_CODE";
	// gprs查询指令
	public static final String KEY_GPRS_YE_CODE = "GPRS_YE_CODE";
	public static final String KEY_GPRS_USED_CODE = "GPRS_USED_CODE";

	// 关键字拆分规则
	public static final String KEY_SMS_SPLIT_RULE = "sms_split_rule";

	public static final String KEY_PACKAGE_CODE = "package_code";

	// 上次通话记录的最后一条记录的ID
	public static final String KEY_LAST_CALLLOG_ID = "last_call_log_id";
	// 上次短信记录的最后一条记录的ID
	public static final String KEY_LAST_SMSLOG_ID = "last_sms_log_id";

	// 当前总拨出时间
	public static final String KEY_TOTAL_CALLOUT_TIME = "total_call_outgoing_time";
	// 当前总接听时间
	public static final String KEY_TOTAL_CALLINCOME_TIME = "total_call_incoming_time";
	// 当前总发出短信数
	public static final String KEY_TOTAL_SMS_SENT = "total_sms_sent";
	// 话费余额
	public static final String KEY_FEE_AVAILABLE = "fee_available";
	// 累加出的已用话费
	public static final String KEY_CAL_USED_FEE = "cal_used_fee";
	// 每个月最早的一次对出来的可用余额
	public static final String KEY_EARLIEST_MONTHLY_FEE_AVAILABLE = "earliest_monthly_fee_available";
	// 每个月最早的一次对得出可用余额的日期
	public static final String KEY_EARLIEST_MONTHLY_FEE_AVAILABLE_DATE = "earliest_monthly_fee_available_date";
	/** 通过费率计算，计算出来的话费余额 */
	public static final String KEY_CALCULATE_FEE_AVAILABLE = "calculate_fee_available";

	/** 实时话费 */
	public static final String KEY_FEE_SPENT = "fee_spent";

	/** 本月套餐余量 */
	public static final String KEY_TOTAL_GPRS_MARGIN = "total_gprs_margin";
	// 当日最早流量统计记录,在用户未关闭程序或者未关机情况下就是凌晨
	public static final String KEY_EARLIEST_GPRS_LOG = "earliest_gprs_log";
	// GPRS日期
	public static final String KEY_UPDATA_TRAFFIC_DATE = "updata_traffic_date";

	public static final String KEY_UPDATA_TRAFFIC_MONTH = "updata_traffic_month";
	/** 当日最早wifi统计记录 */
	public static final String KEY_EARLIEST_WIFI_LOG = "earliest_wifi_log";
	/** 当月已使用wifi总流量 */
	public static final String KEY_TOTAL_GPRS_USED_DIFFERENCE = "total_gprs_used_difference";
	/** 昨日GPRS使用流量 */
	public static final String KEY_YESTERDAY_GPRS_USED = "yesterday_gprs_used";
	// 当月已使用总gprs流量
	public static final String KEY_TOTAL_GPRS_USED = "total_gprs_used";
	// 上次对账时候已使用的总gprs流量
	public static final String KEY_PREV_RECONCILITION_GPRS_USED = "prev_reconcilition_gprs_used";
	// gprs总上传流量
	public static final String KEY_GPRS_UPLOAD = "gprs_upload";
	// gprs总下载流量
	public static final String KEY_GPRS_DOWNLOAD = "gprs_download";
	// wifi总下载量
	public static final String KEY_TOTAL_WIFI_USED = "total_wifi_used";
	// wifi总上传流量
	public static final String KEY_WIFI_UPLOAD = "wifi_upload";
	// wifi总下载流量
	public static final String KEY_WIFI_DOWNLOAD = "wifi_download";
	// 缓存模式
	public static final String KEY_CACHE_DIR_MODE = "cache_dir_mode";
	// 缓存存放目录
	public static final String KEY_CACHE_DIR = "cache_dir";
	// 城市去区号
	public static final String KEY_CITY_CODE = "city_code";
	// 升级通知
	public static final String KEY_UPDATE_NOTICE = "update_notice";
	// 升级信息
	public static final String KEY_UPDATE_VERSION_NAME = "version_name";
	// 版本号
	public static final String KEY_UPDATE_VERSION_CODE = "version_code";
	// 下载地址
	public static final String KEY_UPDATE_URL = "download_url";
	// 历史版本
	public static final String KEY_HISTORY_VERSION = "history_version";
	// apk名称
	public static final String KEY_UPDATE_APKNAME = "apk_name";
	// apk dir
	public static final String KEY_UPDATE_APK_FILE_DIR = "apk_file_dir";

	// 指令库更新
	public static final String KEY_ACCOUNTING_DATABASE_UPDATE_TIME = "accounting_database_update_time";

	public static final String KEY_PREV_RECONCILITION_SEND_SMS_ = "send_sms_over";

	public static final String KEY_IS_DOWN_TIME = "is_down_time";

	// this part for system settings
	public static final String KEY_HUNG_UP_NOTICE = "hung_up_notice";

	public static final String KEY_AIRMODE_SWITCH = "airmode_switch";

	public static final String KEY_STATUS_KEEP_NOTICE = "status_keep_notice";

	public static final String KEY_FLOAT_WINDOW_INDEX = "float_window_index";
	public static final String KEY_FLOAT_WINDOW_OPEN = "float_window_open";

	/* float window (x,y) */
	public static final String KEY_FLOATWINDOW_POSITION = "float_window_position";

	/** alert notice */

	public static final String KEY_ALERT_CALLS_NOTICE = "key_alert_calls_notice";
	public static final String KEY_ALERT_TRAFFIC_NOTICE = "key_alert_traffic_notice";
	public static final String KEY_ALERT_TODAYTRAFFIC_NOTICE = "key_alert_todaytraffic_notice";
	public static final String KEY_ALERT_SMS_NOTICE = "key_alert_sms_notice";
	public static final String KEY_ALERT_CALL_NOTICE = "key_alert_call_notice";
	public static final String KEY_ALERT_NOTICE_OPEN = "key_alert_notice_open";
	public static final String KEY_ALERT_REMAIN_FEES = "key_alert_remain_fees";

	public static final String KEY_TRAFFIC_ALERT_SWITCH = "key_traffic_alert_switch";

	// 正在进行话费对账
	public static final String KEY_IS_CHECKING_ACCOUNT = "is_checking_account";
	// 正在进行流量的对账
	public static final String KEY_IS_CHECKING_TRAFFIC = "is_checking_traffic";

	public static final String KEY_ACOUNT_FREQUENCY = "account_frequency";
	public static final String KEY_CHECK_CALLFEES = "check_callfees";
	public static final String KEY_CHECK_TRAFFIC = "check_traffic";

	public static final String KEY_MONTH_TRAFFIC_WARN = "month_traffic_warn";
	public static final String KEY_TODAY_TRAFFIC_WARN = "today_traffic_warn";
	// 超流量断网
	public static final String KEY_TRAFFIC_BEYOND = "traffic_beyond";

	/** 手机号码绑定状态 **/
	public static final String KEY_PHONE_BINDING_STATUS = "phone_binding_status";

	/** Reconciliation Code */
	public static final String KEY_CALL_CHARGES = "call_charges";

	public static final String KEY_TRAFFIC_QUERY = "traffic_query";

	public static final String KEY_PACKAGE_MARGIN = "pakcage_margin";

	public static final String KEY_RECONCILIATION_TIME = "reconciliation_time";

	public static final String KEY_TRAFFIC_NODE = "trafficNode";

	public static final String KEY_SCREEN_LOCKED_STATE = "screen_lock_state";

	private final SharedPreferences mPreferences;
	/* 超流量断网 */
	public static final String KEY_IS_BROKEN_NETWORK = "key_is_broken_network";
	/* 超流量报警 */
	public static final String KEY_IS_TRAFFIC_ALERT = "key_is_traffic_alert";
	/* Accounting_log send times */
	public static final String KEY_ACCOUNT_LOG_SEND_TIMES = "account_log_send_times";

	public static final String KEY_IS_SERVICES_START_ON_BOOT_COMPLETE = "is_services_start_on_boot_complete";

	/**
	 * for update
	 */
	public static final String KEY_UPDATE_LATER_TIME = "update_later";

	public static final String KEY_UPDATE_ACCOUNT_CODE = "update_account_code";
	/**
	 * for totalcallfees
	 */

	public static final String KEY_TOTAL_CALLFEES = "total_callfees";

	/**
	 * for firewall
	 */
	public static final String KEY_CAN_MINE_FIREWALL_WORK = "can_mine_firewall_work";
	public static final String KEY_CAN_FIREWALL_WORK = "can_firewall_work";
	public static final String KEY_IS_FIREWALL_ON = "is_firewall_on";

	/**
	 * for calls budget
	 */
	public static final String KEY_CALLS_BUDGET = "calls_budget";

	public static final String KEY_CALLS_BUDGET2 = "calls_budget2";

	// 是否仅桌面显示
	public static final String FLOAT_SHOW_DESK_ONLY = "float_show_desk_only";
	// 联网更新
	public static final String CODE_UPDATE_TIME = "code_update_time";
	// 系统短信号码
	public static final String OUR_SYSTEM_NO = "our_system_number";
	// 渠道号
	public static final String CHANNEL_NOMBER = "channel_nomber";
	// 是否自动对帐
	public static final String IS_AUTO_CHECK = "is_auto_check";

	//
	public static final String KEY_TRAFFIC_AUTO_CHECK_ON = "traffic_auto_check";

	// 今日已用话费
	public static final String KEY_TODAY_ALREADY_USED_FEE = "today_already_used_fee";

	// 第一次启动，在CallManagerActivity中进行对账
	public static final String KEY_FIRST_ACCOUNT = "callmanageractivity_first_account";

	// 江苏移动定制
	public static final String KEY_OVERLAY_PACKAGE_SWITCH = "overlay_package_switch";
	public static final String KEY_HAS_OPENED = "has_opened";

	// 为了避免类似于MIUI 4.0手机在系统短信优先选项开启的情况下，拦截不到短信的情况出现，特此加以下配置变量
	public static final String KEY_HAS_NOT_RECEIVED_SMS_MIUI_COUNT = "has_not_received_sms_miui_count";

	// 加入各个不同通话类型的资费字段（单位: 元/分钟）
	public static final String KEY_RATES_LOCAL = "rates_for_local";
	public static final String KEY_RATES_LONG_DISTANCE = "rates_for_long_distance";
	public static final String KEY_RATES_ROAMING = "rates_for_roaming";
	public static final String KEY_RATES_IP = "rates_for_ip_dialing";
	public static final String KEY_RATES_SHORT = "rates_for_short_number";
	public static final String KEY_RATES_SMS = "rates_for_sms";
	// 另外再增加字段表示超过部分的Gprs资费和超过时长部分的Wlan资费
	public static final String KEY_RATES_TRAFFIC = "rates_for_traffic"; // 单位：元/M
	public static final String KEY_RATES_WLAN = "rates_for_wlan"; // 单位：元/小时
	// 统计出wlan的已用时长
	public static final String KEY_WLAN_USED = "duration_wlan__already_used";

	// 加入字段区分是从receiver中截获短信对帐成功还是从收件箱中收取运营商的短信对帐成功
	public static final String KEY_ACCOUNTING_RECEIVER_OR_INBOX = "accounting_from_receiver_or_inbox";

	public static final String KEY_FEE_SPENT_ACCOUNTING_KEYWORDS = "accounting_fee_spent_key_words";// 已用话费关键字串
	public static final String KEY_FEE_AVAILABLE_ACCOUNTING_KEYWORDS = "accounting_fee_available_key_words";// 剩余话费关键字串
	public static final String KEY_TRAFFIC_USED_KEYWORDS = "accounting_traffic_used_key_words";// 已用流量关键字串
	public static final String KEY_TRAFFIC_AVAILABLE_KEYWORDS = "accounting_traffic_available_key_words";// 剩余流量关键字串

	// 话费校正是否成功的状态
	public static final String KEY_CALLS_CORRECT_STATE = "calls_correct_state";
	// 流量校正是否成功的状态
	public static final String KEY_FLOW_CORRECT_STATE = "flow_correct_state";

	// 增加字段保存话费校正的指令,便于数据还原
	public static final String KEY_CODE_FOR_FEE = "code_for_fee";
	public static final String KEY_CODE_FOR_TRAFFIC = "code_for_traffic";

	// 上次开始全对账的时间
	public static final String KEY_LASTCHECK_TIME_NEW = "new_lastcheck_time";
	// 增加字段保存上一次事件发生时刻（事件是指打一个电话或者是发一条短信，当然这些必须要是我们认为的有效通话和有效短信）的账户余额信息和所用流量信息
	public static final String KEY_LAST_EVENT_FEE_AVAIL = "last_event_fee_avail";
	public static final String KEY_LAST_EVENT_TRAFFIC_USED = "last_event_traffic_used";

	// 增加字段表征用户上次上传消费数据成功的时刻 ， ms数表示
	public static final String KEY_LAST_UPLOAD_USER_CONSUME_INFO_SUCCESS = "last_time_upload_user_consume_info_success";

	// 增加字段表征用户是否已经成功上传上月消费数据
	public static final String KEY_UPLOAD_LAST_MONTH_CONSUME_INFO_SUCCESS = "upload_last_month_consume_info_success";

	// 增加字段表征一天试图上传用户月消费行为数据给服务器的次数(控制客户端在一天之内不能无限次数地给服务器传送数据)
	public static final String KEY_UPLOAD_MONTH_CONSUME_INFO_COUNT = "upload_monthly_consume_info_count";

	private Context mContext;

	public ConfigManager(Context base) {
		super(base);
		mContext = base;
		mPreferences = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
	}

	public void setValue(String type, String key, String value) {
		if (type == null || key == null || value == null) {
			return;
		}
		final Editor editor = mPreferences.edit();
		if (String.class.getSimpleName().equals(type)) {
			editor.putString(key, value);
		} else if (Integer.class.getSimpleName().equals(type)) {
			editor.putInt(key, Integer.valueOf(value));
		} else if (Float.class.getSimpleName().equals(type)) {
			editor.putFloat(key, Float.valueOf(value));
		} else if (Double.class.getSimpleName().equals(type)) {
			editor.putFloat(key, (float) Float.valueOf(value));
		} else if (Long.class.getSimpleName().equals(type)) {
			editor.putLong(key, Long.valueOf(value));
		} else if (Boolean.class.getSimpleName().equals(type)) {
			editor.putBoolean(key, Boolean.valueOf(value));
		}
		editor.commit();
	}

	public void setScreenState(boolean islock) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_SCREEN_LOCKED_STATE, islock);
		editor.commit();
	}

	public boolean getScreenState() {
		return mPreferences.getBoolean(KEY_SCREEN_LOCKED_STATE, false);
	}

	public void setIMSI(String imsi) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_IMSI, imsi);
		editor.commit();
	}

	public String getIMSI() {
		return mPreferences.getString(KEY_IMSI, "null");
	}

	public void setIsDownTime(boolean isDownTime) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_IS_DOWN_TIME, isDownTime);
		editor.commit();
	}

	public boolean IsDownTime() {
		return mPreferences.getBoolean(KEY_IS_DOWN_TIME, false);
	}

	public void setIsBrokenNetwork(boolean IsBrokenNetwork) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_IS_BROKEN_NETWORK, IsBrokenNetwork);
		editor.commit();
	}

	public boolean getIsBrokenNetwork() {
		return mPreferences.getBoolean(KEY_IS_BROKEN_NETWORK, true);
	}

	public void setIsTrafficAlert(boolean IsTrafficAlert) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_IS_TRAFFIC_ALERT, IsTrafficAlert);
		editor.commit();
	}

	public boolean getIsTrafficAlert() {
		return mPreferences.getBoolean(KEY_IS_TRAFFIC_ALERT, false);
	}

	public void setCanMineFirewallWork(boolean canMineFirewallWork) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_CAN_MINE_FIREWALL_WORK, canMineFirewallWork);
		editor.commit();
	}

	public boolean canMineFirewallWork() {
		return mPreferences.getBoolean(KEY_IS_DOWN_TIME, true);
	}

	public void setCanFirewallWork(boolean canFirewallWork) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_CAN_FIREWALL_WORK, canFirewallWork);
		editor.commit();
	}

	public boolean canFirewallWork() {
		return mPreferences.getBoolean(KEY_CAN_FIREWALL_WORK, true);
	}

	public void setUpdateAccountCode(boolean updateAccountCode) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_UPDATE_ACCOUNT_CODE, updateAccountCode);
		editor.commit();
	}

	public boolean isUpdateAccountCode() {
		return mPreferences.getBoolean(KEY_UPDATE_ACCOUNT_CODE, false);
	}

	public void setIsCheckingAccount(boolean isCheckingAccount) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_IS_CHECKING_ACCOUNT, isCheckingAccount);
		editor.commit();
	}

	public boolean getIsCheckingAccount() {
		return mPreferences.getBoolean(KEY_IS_CHECKING_ACCOUNT, false);
	}

	public void setIsCheckingTraffic(boolean isCheckingAccount) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_IS_CHECKING_TRAFFIC, isCheckingAccount);
		editor.commit();
	}

	public boolean getIsCheckingTraffic() {
		return mPreferences.getBoolean(KEY_IS_CHECKING_TRAFFIC, false);
	}

	public String getReconcilitionTime() {
		return mPreferences.getString(KEY_RECONCILIATION_TIME,
				"01:00,05:00,09:00,13:00,17:00,21:00");
	}

	public void setPrevReconcilitionSendSms(int send_sms) {
		final Editor editor = mPreferences.edit();
		editor.putInt(KEY_PREV_RECONCILITION_SEND_SMS_, send_sms);
		editor.commit();
	}

	public int getPrevReconcilitionSendSms() {
		return mPreferences.getInt(KEY_PREV_RECONCILITION_SEND_SMS_, 0);
	}

	public void setCalculateFeeAvailable(float calculateFeeAvailable) {
		final Editor editor = mPreferences.edit();
		editor.putFloat(KEY_CALCULATE_FEE_AVAILABLE, calculateFeeAvailable);
		editor.commit();
	}

	public float getCalculateFeeAvailable() {
		return mPreferences.getFloat(KEY_CALCULATE_FEE_AVAILABLE, 100000f);
	}

	public void setSendReconilitionFailedCount(int count) {
		final Editor editor = mPreferences.edit();
		editor.putInt(KEY_SEND_RECONILITION_FAILED_COUNT, count);
		editor.commit();
	}

	public int getSendReconilitionFailedCount() {
		return mPreferences.getInt(KEY_SEND_RECONILITION_FAILED_COUNT, 5);
	}

	public void setPrevReconcilitionGprsUsed(long prevReconcilitionGprsUsed) {
		final Editor editor = mPreferences.edit();
		editor.putLong(KEY_PREV_RECONCILITION_GPRS_USED,
				prevReconcilitionGprsUsed);
		editor.commit();
	}

	public long getPrevReconcilitionGprsUsed() {
		return mPreferences.getLong(KEY_PREV_RECONCILITION_GPRS_USED, -1);
	}

	public void setEarliestGprsLog(long earliestGprsLog) {
		final Editor editor = mPreferences.edit();
		editor.putLong(KEY_EARLIEST_GPRS_LOG, earliestGprsLog);
		editor.commit();
	}

	public void setEarliestWifiLog(long earliestWifiLog) {
		final Editor editor = mPreferences.edit();
		editor.putLong(KEY_EARLIEST_WIFI_LOG, earliestWifiLog);
		editor.commit();
	}

	public long getEarliestWifiLog() {
		return mPreferences.getLong(KEY_EARLIEST_WIFI_LOG, 0);
	}

	public void setTotalWifiUsed(long totalWifiUsed) {
		final Editor editor = mPreferences.edit();
		editor.putLong(KEY_TOTAL_WIFI_USED, totalWifiUsed);
		editor.commit();
	}

	public long getTotalWifiUsed() {
		return mPreferences.getLong(KEY_TOTAL_WIFI_USED, 0);
	}

	public void setUpdataTrafficDate(String date) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_UPDATA_TRAFFIC_DATE, date);
		editor.commit();
	}

	public String getUpdataTrafficDate() {
		return mPreferences.getString(KEY_UPDATA_TRAFFIC_DATE, null);
	}

	public void setCallNoticeSetting(String name) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_CALLNOTICE_SETTING, name);
		editor.commit();
	}

	public String getFeedback() {
		return mPreferences.getString(KEY_FEEDBACK, null);
	}

	public void setFeedback(String feedback) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_FEEDBACK, feedback);
		editor.commit();
	}

	public long getUpdateLater() {
		return mPreferences.getLong(KEY_UPDATE_LATER_TIME, 0);
	}

	public void setUpdateLater(long updateLater) {
		final Editor editor = mPreferences.edit();
		editor.putLong(KEY_UPDATE_LATER_TIME, updateLater);
		editor.commit();
	}

	public String getCallNoticeSetting() {
		return mPreferences.getString(KEY_CALLNOTICE_SETTING, null);
	}

	public void setLastCheckTime(long checkTime) {
		final Editor editor = mPreferences.edit();
		editor.putLong(KEY_LASTCHECK_TIME, checkTime);
		editor.commit();
	}

	public long getLastCheckTime() {
		return mPreferences.getLong(KEY_LASTCHECK_TIME, 0);
	}

	//增加api函数设置保存上次添加方程的时刻（毫秒数）
	public void setLastAddEquationTime(long add_equation_time) {
		final Editor editor = mPreferences.edit();
		editor.putLong(KEY_LAST_ADD_EQUATION_TIME, add_equation_time);
		editor.commit();
	}
	//增加api函数获取上次添加方程的时刻（毫秒数）
	public long getLastAddEquationTime() {
		return mPreferences.getLong(KEY_LAST_ADD_EQUATION_TIME, -1);
	}
	
	public void setLastBeginCheckTime(long checkTime) {
		final Editor editor = mPreferences.edit();
		editor.putLong(KEY_LASTCHECK_TIME_NEW, checkTime);
		editor.commit();
	}

	public long getLastBeginCheckTime() {
		return mPreferences.getLong(KEY_LASTCHECK_TIME_NEW, 0);
	}

	public void setUpdateApkName(String name) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_UPDATE_APKNAME, name);
		editor.commit();
	}

	public String getUpdateApkName() {
		return mPreferences.getString(KEY_UPDATE_APKNAME, null);
	}

	public long getEarliestGprsLog() {
		return mPreferences.getLong(KEY_EARLIEST_GPRS_LOG, 0);
	}

	public void setTotalGprsMargin(long totalGprsMargin) {
		final Editor editor = mPreferences.edit();
		editor.putLong(KEY_TOTAL_GPRS_MARGIN, totalGprsMargin);
		editor.commit();
	}

	public long getTotalGprsMargin() {
		return mPreferences.getLong(KEY_TOTAL_GPRS_MARGIN, -100000);
	}

	public void setTotalGprsUsedDifference(long totalGprsUsedDifference) {
		final Editor editor = mPreferences.edit();
		editor.putLong(KEY_TOTAL_GPRS_USED_DIFFERENCE, totalGprsUsedDifference);
		editor.commit();
	}

	public long getTotalGprsUsedDifference() {
		return mPreferences.getLong(KEY_TOTAL_GPRS_USED_DIFFERENCE, 0);
	}

	public void setYesterdayGprsUsed(long yesterdayGprsUsed) {
		final Editor editor = mPreferences.edit();
		editor.putLong(KEY_YESTERDAY_GPRS_USED, yesterdayGprsUsed);
		editor.commit();
	}

	public long getYesterdayGprsUsed() {
		return mPreferences.getLong(KEY_YESTERDAY_GPRS_USED, 0);
	}

	public void setTotalGprsUsed(long totalGprsUsed) {
		final Editor editor = mPreferences.edit();
		editor.putLong(KEY_TOTAL_GPRS_USED, totalGprsUsed);
		editor.commit();
	}

	public long getTotalGprsUsed() {
		return mPreferences.getLong(KEY_TOTAL_GPRS_USED, 0);
	}

	public void setGprsUpload(long gprsUpload) {
		final Editor editor = mPreferences.edit();
		editor.putLong(KEY_GPRS_UPLOAD, gprsUpload);
		editor.commit();
	}

	public long getGprsUpload() {
		return mPreferences.getLong(KEY_GPRS_UPLOAD, 0);
	}

	public void setGprsDownload(long gprsDownload) {
		final Editor editor = mPreferences.edit();
		editor.putLong(KEY_GPRS_DOWNLOAD, gprsDownload);
		editor.commit();
	}

	public long getGprsDownload() {
		return mPreferences.getLong(KEY_GPRS_DOWNLOAD, 0);
	}

	public void setWifiUpload(long wifiUpload) {
		final Editor editor = mPreferences.edit();
		editor.putLong(KEY_WIFI_UPLOAD, wifiUpload);
		editor.commit();
	}

	public long getWifiUpload() {
		return mPreferences.getLong(KEY_WIFI_UPLOAD, 0);
	}

	public void setWifiDownload(long wifiDownload) {
		final Editor editor = mPreferences.edit();
		editor.putLong(KEY_WIFI_DOWNLOAD, wifiDownload);
		editor.commit();
	}

	public long getWifiDownload() {
		return mPreferences.getLong(KEY_WIFI_DOWNLOAD, 0);
	}

	public void setOperatorNum(String operatorNum) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_OPERATOR_NUM, operatorNum);
		editor.commit();
	}

	public String getOperatorNum() {
		return mPreferences.getString(KEY_OPERATOR_NUM, null);
	}

	public String getHFUsedCode() {
		return mPreferences.getString(KEY_HF_USED_CODE, null);
	}

	public void setHFUsedCode(String yeCode) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_HF_USED_CODE, yeCode);
		editor.commit();
	}

	public String getHFYeCode() {
		return mPreferences.getString(KEY_HF_YE_CODE, null);
	}

	public void setHFYeCode(String yeCode) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_HF_YE_CODE, yeCode);
		editor.commit();
	}

	public void setGprsUsedCode(String gprsCode) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_GPRS_USED_CODE, gprsCode);
		editor.commit();
	}

	public String getGprsUsedCode() {
		return mPreferences.getString(KEY_GPRS_USED_CODE, null);
	}

	public void setGprsYeCode(String gprsCode) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_GPRS_YE_CODE, gprsCode);
		editor.commit();
	}

	public String getGprsYeCode() {
		return mPreferences.getString(KEY_GPRS_YE_CODE, null);
	}

	public void setPackageCode(String packageCode) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_PACKAGE_CODE, packageCode);
		editor.commit();
	}

	public String getPackageCode() {
		return mPreferences.getString(KEY_PACKAGE_CODE, null);
	}

	// public void setCallCharges(String callCharges) {
	// final Editor editor = mPreferences.edit();
	// editor.putString(KEY_CALL_CHARGES, callCharges);
	// editor.commit();
	// }
	//
	// public String getCallCharges() {
	// return mPreferences.getString(KEY_CALL_CHARGES, null);
	// }
	//
	// public void setTrafficQuery(String trafficQuery) {
	// final Editor editor = mPreferences.edit();
	// editor.putString(KEY_TRAFFIC_QUERY, trafficQuery);
	// editor.commit();
	// }
	//
	// public String getTrafficQuery() {
	// return mPreferences.getString(KEY_TRAFFIC_QUERY, null);
	// }
	//
	// public void setPackageMargin(String packageMargin) {
	// final Editor editor = mPreferences.edit();
	// editor.putString(KEY_PACKAGE_MARGIN, packageMargin);
	// editor.commit();
	// }
	//
	// public String getPackageMargin() {
	// return mPreferences.getString(KEY_PACKAGE_MARGIN, null);
	// }
	//
	// public void setReconcilitionTime(String reconcilitionTime) {
	// final Editor editor = mPreferences.edit();
	// editor.putString(KEY_RECONCILIATION_TIME, reconcilitionTime);
	// editor.commit();
	// }
	//
	// public String getReconcilitionTime() {
	// return mPreferences.getString(KEY_RECONCILIATION_TIME, null);
	// }
	//
	public void setTrafficNode(String trafficNode) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_TRAFFIC_NODE, trafficNode);
		editor.commit();
	}

	public String getTrafficNode() {
		return mPreferences.getString(KEY_TRAFFIC_NODE, "0,0,0,0,0,0");
	}

	public boolean isFirstLaunch() {
		return mPreferences.getBoolean(KEY_FIRST_LAUNCH, true);
	}

	public void setDoNotAlertAgain(final boolean noAlert) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_DO_NOT_ALERT_AGAIN, noAlert);
		editor.commit();
	}

	public boolean isAlertAgain() {
		return mPreferences.getBoolean(KEY_DO_NOT_ALERT_AGAIN, false);
	}

	/**
	 * we can set first launch to false in WicityApplication.onTerminate();
	 * 
	 * @param isFirstLaunch
	 */
	public void setFirstLaunch(final boolean isFirstLaunch) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_FIRST_LAUNCH, isFirstLaunch);
		editor.commit();
	}

	/**
	 * return off-line interval for download
	 * 
	 * @return
	 */
	public long getOfflineInterval() {
		return mPreferences.getLong(KEY_OFFLINE_INTERVAL,
				DEFAULT_OFFLINE_INTERVAL);
	}

	/**
	 * set off-line interval for download
	 * 
	 */
	public void setOfflineInterval(final long interval) {
		final Editor editor = mPreferences.edit();
		editor.putLong(KEY_OFFLINE_INTERVAL, interval);
		editor.commit();
	}

	/**
	 * get off-line time of download task will be running
	 * 
	 * @return
	 */
	public long getOfflineTime() {
		return mPreferences.getLong(KEY_OFFLINE_TIME, DEFAULT_OFFLINE_TIME);
	}

	/**
	 * set off-line time for download task will be running
	 * 
	 * @param offlineTime
	 */
	public void setOfflineTime(final long offlineTime) {
		final Editor editor = mPreferences.edit();
		editor.putLong(KEY_OFFLINE_TIME, offlineTime);
		editor.commit();
	}

	public void setCacheDirMode(final int mode) {
		final Editor editor = mPreferences.edit();
		editor.putInt(KEY_CACHE_DIR_MODE, mode);
		editor.commit();
	}

	/**
	 * define cache mode 0 no define 1 external storage 2 internal storage
	 * 
	 * @return
	 */
	public int getCacheDirMode() {
		return mPreferences.getInt(KEY_CACHE_DIR_MODE, 0);
	}

	public void setCurrentCacheDir(final String dir) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_CACHE_DIR, dir);
		editor.commit();
	}

	public int getAccountingDataVersion() {
		return mPreferences.getInt(KEY_ACCOUNTING_DATA_VERSION, 0);
	}

	public void setAccountingDataVersion(final int version) {
		final Editor editor = mPreferences.edit();
		editor.putInt(KEY_ACCOUNTING_DATA_VERSION, version);
		editor.commit();
	}

	public int getTotalSmsSent() {
		return mPreferences.getInt(KEY_TOTAL_SMS_SENT, 0);
	}

	public void setTotalSmsSent(int sent) {
		final Editor editor = mPreferences.edit();
		editor.putInt(KEY_TOTAL_SMS_SENT, sent);
		editor.commit();
	}

	public String getCurrentCacheDir() {
		return mPreferences.getString(KEY_CACHE_DIR, null);
	}

	public void setCity(String city) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_CITY, city);
		editor.commit();
	}

	public void setFloatWindowIndex(int fw) {
		final Editor editor = mPreferences.edit();
		editor.putInt(KEY_FLOAT_WINDOW_INDEX, fw);
		editor.commit();
	}

	public int getFloatWindowIndex() {
		return mPreferences.getInt(KEY_FLOAT_WINDOW_INDEX, 1);
	}

	public void setFloatWindowOpen(boolean open) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_FLOAT_WINDOW_OPEN, open);
		editor.commit();
	}

	public boolean getFloatWindowOpen() {
		return mPreferences.getBoolean(KEY_FLOAT_WINDOW_OPEN, false);
	}

	public String getCity() {
		return mPreferences.getString(KEY_CITY, "城市");
	}

	public void setProvince(String province) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_PROVINCE, province);
		editor.commit();
	}

	public String getProvince() {
		return mPreferences.getString(KEY_PROVINCE, "省份");
	}

	public void setCityCode(String cityCode) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_CITY_CODE, cityCode);
		editor.commit();
	}

	public void setTopEightNum(String number) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_TOP_EIGHT_NUM, number);
		editor.commit();
	}

	public void setOperator(String operator) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_OPERATOR, operator);
		editor.commit();
	}

	public void setPackageBrand(String brand) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_PACKAGE_BRAND, brand);
		editor.commit();
	}

	public String getCityCode() {
		String citycode = mPreferences.getString(KEY_CITY_CODE, "027-0");
		return citycode.split("-")[0];
	}

	public String getOperator() {
		String operator = mPreferences.getString(KEY_OPERATOR, "运营商");
		return operator;
	}

	public String getPackageBrand() {
		String PackageBrand = mPreferences.getString(KEY_PACKAGE_BRAND, "品牌");
		return PackageBrand;
	}

	public String getTopEightNum() {
		String TopEightNum = mPreferences.getString(KEY_TOP_EIGHT_NUM, "");
		return TopEightNum;
	}

	/**
	 * 每个模块需要用到新的字段,照着自己添加
	 * */

	public void setFeesRemain(float feesremian) {
		final Editor editor = mPreferences.edit();
		editor.putFloat(KEY_FEE_AVAILABLE, feesremian);
		editor.commit();
	}

	public float getFeesRemian() {
		float feesremain = mPreferences.getFloat(KEY_FEE_AVAILABLE, 100000);
		return feesremain;
	}

	public void setCalUsedFee(float cal_used_fee) {
		final Editor editor = mPreferences.edit();
		editor.putFloat(KEY_CAL_USED_FEE, cal_used_fee);
		editor.commit();
	}

	public float getCalUsedFee() {
		float cal_used_fee = mPreferences.getFloat(KEY_CAL_USED_FEE, 0);
		return cal_used_fee;
	}

	public void setEarliestMonthlyFeeAvailable(float earliestMonthlyFeeAvail) {
		final Editor editor = mPreferences.edit();
		editor.putFloat(KEY_EARLIEST_MONTHLY_FEE_AVAILABLE,
				earliestMonthlyFeeAvail);
		editor.commit();
	}

	public float getEarliestMonthlyFeeAvailable() {
		return mPreferences.getFloat(KEY_CAL_USED_FEE, -100000);
	}

	public void setEarliestMonthlyFeeAvailableDate(
			String earliestMonthlyFeeAvailDate) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_EARLIEST_MONTHLY_FEE_AVAILABLE_DATE,
				earliestMonthlyFeeAvailDate);
		editor.commit();
	}

	public String getEarliestMonthlyFeeAvailableDate() {
		return mPreferences.getString(KEY_EARLIEST_MONTHLY_FEE_AVAILABLE_DATE,
				"-100000");
	}

	public void setLastCheckHasYeTime(long times) {
		final Editor editor = mPreferences.edit();
		editor.putLong(KEY_LASTCHECK_HAS_YE_TIME, times);
		editor.commit();
	}

	public long getLastCheckHasYeTime() {
		return mPreferences.getLong(KEY_LASTCHECK_HAS_YE_TIME, -1);
	}

	public void setLastCheckHasTrafficTime(long times) {
		final Editor editor = mPreferences.edit();
		editor.putLong(KEY_LASTCHECK_HAS_TRAFFIC_TIME, times);
		editor.commit();
	}

	public long getLastCheckHasTrafficTime() {
		return mPreferences.getLong(KEY_LASTCHECK_HAS_TRAFFIC_TIME, -1);
	}

	public void setFeeSpent(float feespent) {
		final Editor editor = mPreferences.edit();
		editor.putFloat(KEY_FEE_SPENT, feespent);
		editor.commit();
	}

	// modified by zhangjing
	public float getFeeSpent() {
		return mPreferences.getFloat(KEY_FEE_SPENT, 100000f);
	}

	public void setAccountingDay(int AccountingDay) {
		final Editor editor = mPreferences.edit();
		editor.putInt(KEY_ACCOUNTING_DAY, AccountingDay);
		editor.commit();
	}

	public void setLastCalllogId(int _id) {
		final Editor editor = mPreferences.edit();
		editor.putInt(KEY_LAST_CALLLOG_ID, _id);
		editor.commit();
	}

	public void setLastSmslogId(int _id) {
		final Editor editor = mPreferences.edit();
		editor.putInt(KEY_LAST_SMSLOG_ID, _id);
		editor.commit();
	}

	public int getLastCalllogId() {
		return mPreferences.getInt(KEY_LAST_CALLLOG_ID, 0);
	}

	public int getLastSmslogId() {
		return mPreferences.getInt(KEY_LAST_SMSLOG_ID, 0);
	}

	public int getTotalOutgoingCall() {
		return mPreferences.getInt(KEY_TOTAL_CALLOUT_TIME, 0);
	}

	public void setTotalOutgoingCall(int outgingCall) {
		final Editor editor = mPreferences.edit();
		editor.putInt(KEY_TOTAL_CALLOUT_TIME, outgingCall);
		editor.commit();
	}

	public int getTotalIncomingCall() {
		return mPreferences.getInt(KEY_TOTAL_CALLINCOME_TIME, 0);
	}

	public void setTotalIncomingCall(int incomingCall) {
		final Editor editor = mPreferences.edit();
		editor.putInt(KEY_TOTAL_CALLINCOME_TIME, incomingCall);
		editor.commit();
	}

	public int getAccountingDay() {
		return mPreferences.getInt(KEY_ACCOUNTING_DAY, 1);
	}

	public void setFreeCallTime(int freeCallTime) {
		final Editor editor = mPreferences.edit();
		editor.putInt(KEY_FREE_CALLTIME, freeCallTime);
		editor.commit();
	}

	public int getFreeCallTime() {
		return mPreferences.getInt(KEY_FREE_CALLTIME, 100000);
	}

	public void setFreeMessages(int freeessages) {
		final Editor editor = mPreferences.edit();
		editor.putInt(KEY_FREE_MESSAGES, freeessages);
		editor.commit();
	}

	public int getFreeMessages() {
		return mPreferences.getInt(KEY_FREE_MESSAGES, 100000);
	}

	public void setFreeGprs(float freeGprs) {
		final Editor editor = mPreferences.edit();
		editor.putFloat(KEY_FREE_GPRS, freeGprs);
		editor.commit();
	}

	// public void setRent(int rent) {
	// final Editor editor = mPreferences.edit();
	// editor.putInt(KEY_RENT, rent);
	// editor.commit();
	// }
	//
	// public int getRent() {
	// int rent = mPreferences.getInt(KEY_RENT, 100000);
	// return rent;
	// }

	public float getFreeGprs() {
		return mPreferences.getFloat(KEY_FREE_GPRS, 100000);
	}

	public void setMsgUnitPrice(float msgUnitPrice) {
		final Editor editor = mPreferences.edit();
		editor.putFloat(KEY_MSGPRICE_UNIT, msgUnitPrice);
		editor.commit();
	}

	public float getMsgUnitPrice() {
		return mPreferences.getFloat(KEY_MSGPRICE_UNIT, 0.1f);
	}

	// public void setTrafficUnitPrice(Float trafficUnitPrice) {
	// final Editor editor = mPreferences.edit();
	// editor.putFloat(KEY_TRAFFICPRICE_UNIT, trafficUnitPrice);
	// editor.commit();
	// }
	//
	// public Float getTrafficUnitPrice() {
	// Float trafficUnitPrice =
	// mPreferences.getFloat(KEY_TRAFFICPRICE_UNIT,1.0f);
	// return trafficUnitPrice;
	// }

	// TODO
	public boolean isUpdateNotice() {
		return mPreferences.getBoolean(KEY_UPDATE_NOTICE, true);
	}

	public void setUpdateNotice(boolean isUpdateNotice) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_UPDATE_NOTICE, isUpdateNotice);
		editor.commit();
	}

	public void setUpdateUrl(String updateUrl) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_UPDATE_URL, updateUrl);
		editor.commit();
	}

	public String getUpdateUrl() {
		return mPreferences.getString(KEY_UPDATE_URL, null);
	}

	public void setVersionName(String versionName) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_UPDATE_VERSION_NAME, versionName);
		editor.commit();
	}

	public String getVersionName() {
		return mPreferences.getString(KEY_UPDATE_VERSION_NAME,
				getCurrentVerString());
	}

	public int getVersionCode() {
		return mPreferences.getInt(KEY_UPDATE_VERSION_CODE, getVersion());
	}

	public void setVersionCode(int versionCode) {
		final Editor editor = mPreferences.edit();
		editor.putInt(KEY_UPDATE_VERSION_CODE, versionCode);
		editor.commit();
	}

	public void setHostoryVersion(int ver) {
		final Editor editor = mPreferences.edit();
		editor.putInt(KEY_HISTORY_VERSION, ver);
		editor.commit();
	}

	public int getHistoryVersion() {
		return mPreferences.getInt(KEY_HISTORY_VERSION, 0);
	}

	public String getImei() {
		return mPreferences.getString(KEY_IMEI, null);
	}

	public void setImei(String imei) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_IMEI, imei);
		editor.commit();
	}

	public String getCpuMaxFreq() {
		return mPreferences.getString(KEY_CPU_MAX_FREQ, null);
	}

	public void setCpuMaxFreq(String cpuMaxFreq) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_CPU_MAX_FREQ, cpuMaxFreq);
		editor.commit();
	}

	public String getMemory() {
		return mPreferences.getString(KEY_MEMORY, null);
	}

	public void setMemory(String memory) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_MEMORY, memory);
		editor.commit();
	}

	public String getCpuModel() {
		return mPreferences.getString(KEY_CPU_MODEL, null);
	}

	public void setCpuModel(String cpuModel) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_CPU_MODEL, cpuModel);
		editor.commit();
	}

	public String getCallstatVersion() {
		return mPreferences.getString(KEY_CALLSTAT_VERSION, null);
	}

	public void setCallstatVersion(String callstatVersion) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_CALLSTAT_VERSION, callstatVersion);
		editor.commit();
	}

	public String getMobileModel() {
		return mPreferences.getString(KEY_MOBILE_MODEL, null);
	}

	public void setMobileModel(String mobileModel) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_MOBILE_MODEL, mobileModel);
		editor.commit();
	}

	public String getScreenSize() {
		return mPreferences.getString(KEY_SCREEN_SIZE, "480*800");
	}

	public void setScreenSize(String screenSize) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_SCREEN_SIZE, screenSize);
		editor.commit();
	}

	public String getOsVersion() {
		return mPreferences.getString(KEY_OS_VERSION, null);
	}

	public void setOsVersion(String osVersion) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_OS_VERSION, osVersion);
		editor.commit();
	}

	public int getVersion() {
		try {
			final PackageManager pm = getPackageManager();
			final PackageInfo packageInfo = pm.getPackageInfo(getPackageName(),
					0);
			return packageInfo.versionCode;
		} catch (final NameNotFoundException e) {
			// ...
		}
		return 0;
	}

	private String getCurrentVerString() {
		try {
			final PackageManager pm = getPackageManager();
			final PackageInfo packageInfo = pm.getPackageInfo(getPackageName(),
					0);
			return packageInfo.versionName;
		} catch (final NameNotFoundException e) {
			// ...
		}
		return null;
	}

	public boolean isHungupNotice() {
		return mPreferences.getBoolean(KEY_HUNG_UP_NOTICE, true);
	}

	public void setHungupNotice(boolean isHungupNotice) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_HUNG_UP_NOTICE, isHungupNotice);
		editor.commit();
	}

	public boolean isAirmodeSwitch() {
		return mPreferences.getBoolean(KEY_AIRMODE_SWITCH, false);
	}

	public void setAirmodeSwitch(boolean isAirmodeSwitch) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_AIRMODE_SWITCH, isAirmodeSwitch);
		editor.commit();
		CallStatDatabase.getInstance(mContext)
				.setSharedPreferenceValueToDatabase(KEY_AIRMODE_SWITCH,
						Boolean.class.getSimpleName(),
						String.valueOf(isAirmodeSwitch));
	}

	public void setAirmodeOpenTime(int time) {
		final Editor editor = mPreferences.edit();
		editor.putInt(KEY_AIRMODE_OPEN_TIME, time);
		editor.commit();
		CallStatDatabase.getInstance(mContext)
				.setSharedPreferenceValueToDatabase(KEY_AIRMODE_OPEN_TIME,
						Integer.class.getSimpleName(), String.valueOf(time));
	}

	public int getAirmodeOpenTime() {
		return mPreferences.getInt(KEY_AIRMODE_OPEN_TIME, 2300);
	}

	public void setAirmodeCloseTime(int time) {
		final Editor editor = mPreferences.edit();
		editor.putInt(KEY_AIRMODE_CLOSE_TIME, time);
		editor.commit();
		CallStatDatabase.getInstance(mContext)
				.setSharedPreferenceValueToDatabase(KEY_AIRMODE_CLOSE_TIME,
						Integer.class.getSimpleName(), String.valueOf(time));
	}

	public int getAirmodeCloseTime() {
		return mPreferences.getInt(KEY_AIRMODE_CLOSE_TIME, 700);
	}

	public boolean getStatusKeepNotice() {
		return mPreferences.getBoolean(KEY_STATUS_KEEP_NOTICE, true);
	}

	public void setStatusKeepNotice(boolean isStatusKeepNotice) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_STATUS_KEEP_NOTICE, isStatusKeepNotice);
		editor.commit();
	}

	public float getAlertCallsNotice() {

		float defValue = (float) Math.round(getCallsBudget()) / 10f;

		return mPreferences.getFloat(KEY_ALERT_CALLS_NOTICE, defValue);
	}

	public void setAlertCallsNotice(float AlertCalls) {
		final Editor editor = mPreferences.edit();
		editor.putFloat(KEY_ALERT_CALLS_NOTICE, AlertCalls);
		editor.commit();
	}

	public float getAlertRemainFees() {

		return mPreferences.getFloat(KEY_ALERT_REMAIN_FEES, 20);
	}

	public void setAlertRemainFees(float AlertCalls) {
		final Editor editor = mPreferences.edit();
		editor.putFloat(KEY_ALERT_REMAIN_FEES, AlertCalls);
		editor.commit();
	}

	public float getAlertTrafficNotice() {
		float count = 10;
		if (getFreeGprs() != 100000) {
			count = getFreeGprs() / 10f;
		}
		return mPreferences.getFloat(KEY_ALERT_TRAFFIC_NOTICE, count);
	}

	public void setAlertTrafficNotice(float AlerTraffic) {
		final Editor editor = mPreferences.edit();
		editor.putFloat(KEY_ALERT_TRAFFIC_NOTICE, AlerTraffic);
		editor.commit();
	}

	public int getAlertTodayTrafficNotice() {
		return mPreferences.getInt(KEY_ALERT_TODAYTRAFFIC_NOTICE, 5);
	}

	public void setAlertTodayTrafficNotice(int AlerTraffic) {
		final Editor editor = mPreferences.edit();
		editor.putInt(KEY_ALERT_TODAYTRAFFIC_NOTICE, AlerTraffic);
		editor.commit();
	}

	public int getAlertSmsNotice() {
		int count = 10;
		if (getFreeMessages() != 100000) {
			count = getFreeMessages() / 10;
		}
		return mPreferences.getInt(KEY_ALERT_SMS_NOTICE, count);
	}

	public void setAlertSmsNotice(int AlerSms) {
		final Editor editor = mPreferences.edit();
		editor.putInt(KEY_ALERT_SMS_NOTICE, AlerSms);
		editor.commit();
	}

	public int getAlertCallNotice() {
		int count = 10;
		if (getFreeCallTime() != 100000) {
			count = getFreeCallTime() / 10;
		}
		return mPreferences.getInt(KEY_ALERT_CALL_NOTICE, count);
	}

	public void setAlertCallNotice(int AlerCall) {
		final Editor editor = mPreferences.edit();
		editor.putInt(KEY_ALERT_CALL_NOTICE, AlerCall);
		editor.commit();
	}

	public boolean getCallNoticeOpen() {
		return mPreferences.getBoolean(KEY_ALERT_NOTICE_OPEN, true);
	}

	public void setCallNoticeOpen(boolean AlerCall) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_ALERT_NOTICE_OPEN, AlerCall);
		editor.commit();
	}

	public boolean getTrafficAlertSwitch() {
		return mPreferences.getBoolean(KEY_TRAFFIC_ALERT_SWITCH, true);
	}

	public void setTrafficAlertSwitch(boolean Alert) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_TRAFFIC_ALERT_SWITCH, Alert);
		editor.commit();
	}

	public void setUpdataTrafficMonth(String nowMonth) {
		// TODO Auto-generated method stub
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_UPDATA_TRAFFIC_MONTH, nowMonth);
		editor.commit();
	}

	public String getUpdataTrafficMonth() {
		// TODO Auto-generated method stub
		return mPreferences.getString(KEY_UPDATA_TRAFFIC_MONTH, null);
	}

	public void setFloatWindowPosition(String position) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_FLOATWINDOW_POSITION, position);
		editor.commit();
	}

	public String getFloatWindowPosition() {
		return mPreferences.getString(KEY_FLOATWINDOW_POSITION, null);
	}

	public void setAppUsedTimes(int times) {
		final Editor editor = mPreferences.edit();
		editor.putInt(KEY_TIMES_USED, times);
		editor.commit();
	}

	public int getAppUsedTimes() {
		return mPreferences.getInt(KEY_TIMES_USED, 0);

	}

	public void setStartUpTime(long time) {
		final Editor editor = mPreferences.edit();
		editor.putLong(KEY_TIME_ON_STARTUP, time);
		editor.commit();
	}

	public long getStartUpTime() {
		return mPreferences.getLong(KEY_TIME_ON_STARTUP, 0);

	}

	public void setFirstStartUpTime(long time) {
		final Editor editor = mPreferences.edit();
		editor.putLong(KEY_FIRST_TIME_ON_STARTUP, time);
		editor.commit();
	}

	public long getFirstStartUpTime() {
		return mPreferences.getLong(KEY_FIRST_TIME_ON_STARTUP, 0);

	}

	public void setTotalRunningTime(int time) {
		final Editor editor = mPreferences.edit();
		editor.putInt(KEY_TOTAL_RUNNING_TIME, time);
		editor.commit();
	}

	public int getTotalRunningTime() {
		return mPreferences.getInt(KEY_TOTAL_RUNNING_TIME, 0);

	}

	public void setDayTotalRunningTime(int time) {
		final Editor editor = mPreferences.edit();
		editor.putInt(KEY_DAY_TOTAL_RUNNING_TIME, time);
		editor.commit();
	}

	public int getDayTotalRunningTime() {
		return mPreferences.getInt(KEY_DAY_TOTAL_RUNNING_TIME, 0);

	}

	public void setAccountFrequency(int frequency) {
		final Editor editor = mPreferences.edit();
		editor.putInt(KEY_ACOUNT_FREQUENCY, frequency);
		editor.commit();
	}

	public int getAccountFrequency() {
		return mPreferences.getInt(KEY_ACOUNT_FREQUENCY, 4);
	}

	public void setIsCheckCallFees(boolean ischeckfees) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_CHECK_CALLFEES, ischeckfees);
		editor.commit();
	}

	public boolean getIsCheckCallFees() {
		return mPreferences.getBoolean(KEY_CHECK_CALLFEES, true);

	}

	public void setIsCheckTraffic(boolean ischecktraffic) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_CHECK_TRAFFIC, ischecktraffic);
		editor.commit();
	}

	public boolean getIsCheckTraffic() {
		return mPreferences.getBoolean(KEY_CHECK_TRAFFIC, true);

	}

	public void setMonthTrafficWarn(boolean ischecktraffic) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_MONTH_TRAFFIC_WARN, ischecktraffic);
		editor.commit();
	}

	public boolean getMonthTrafficWarn() {
		return mPreferences.getBoolean(KEY_MONTH_TRAFFIC_WARN, false);

	}

	public void setTrafficBeyond(boolean ischecktraffic) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_TRAFFIC_BEYOND, ischecktraffic);
		editor.commit();
	}

	public boolean getTrafficBeyond() {
		return mPreferences.getBoolean(KEY_TRAFFIC_BEYOND, false);

	}

	public void setTodayTrafficWarn(boolean ischecktraffic) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_TODAY_TRAFFIC_WARN, ischecktraffic);
		editor.commit();
	}

	public boolean getTodayTrafficWarn() {
		return mPreferences.getBoolean(KEY_TODAY_TRAFFIC_WARN, false);

	}

	public int getPhoneBindingStatus() {
		return mPreferences.getInt(KEY_PHONE_BINDING_STATUS, -1);
	}

	public void setPhoneBindingStatus(int status) {
		final Editor editor = mPreferences.edit();
		editor.putInt(KEY_PHONE_BINDING_STATUS, status);
		editor.commit();
		CallStatDatabase.getInstance(mContext)
				.setSharedPreferenceValueToDatabase(KEY_PHONE_BINDING_STATUS,
						Integer.class.getSimpleName(), String.valueOf(status));
	}

	public int getAccountLogSendTimes() {
		return mPreferences.getInt(KEY_ACCOUNT_LOG_SEND_TIMES, 1);
	}

	public void setAccountLogSendTimes(int times) {
		final Editor editor = mPreferences.edit();
		editor.putInt(KEY_ACCOUNT_LOG_SEND_TIMES, times);
		editor.commit();
	}

	public float getTotalCallFees() {
		return mPreferences.getFloat(KEY_TOTAL_CALLFEES, -100000);
	}

	public void setTotalCallFees(float totalcallfees) {
		final Editor editor = mPreferences.edit();
		editor.putFloat(KEY_TOTAL_CALLFEES, totalcallfees);
		editor.commit();
	}

	public void setFirewallOn(boolean isFirewallOn) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_IS_FIREWALL_ON, isFirewallOn);
		editor.commit();
		CallStatDatabase.getInstance(mContext)
				.setSharedPreferenceValueToDatabase(KEY_IS_FIREWALL_ON,
						Boolean.class.getSimpleName(),
						String.valueOf(isFirewallOn));
	}

	public boolean isFirewallOn() {
		return mPreferences.getBoolean(KEY_IS_FIREWALL_ON, false);

	}

	public float getCallsBudget() {
		if (mPreferences.contains(KEY_CALLS_BUDGET2)) {
			return mPreferences.getFloat(KEY_CALLS_BUDGET2, 100000);
		} else {
			return mPreferences.getInt(KEY_CALLS_BUDGET, 100000);
		}
	}

	// public void setCallsBudget(int budget) {
	// final Editor editor = mPreferences.edit();
	// editor.putInt(KEY_CALLS_BUDGET, budget);
	// editor.commit();
	// }

	public void setCallsBudget(float budget) {
		final Editor editor = mPreferences.edit();
		editor.putFloat(KEY_CALLS_BUDGET2, budget);
		editor.commit();
	}

	public void setFloatShowDeskOnly(boolean isFloatShowDeskOnly) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(FLOAT_SHOW_DESK_ONLY, isFloatShowDeskOnly);
		editor.commit();
	}

	public boolean getFloatShowDeskOnly() {
		return mPreferences.getBoolean(FLOAT_SHOW_DESK_ONLY, true);
	}

	public void setTrafficAutoCheckOn(boolean isTrafficAutoCheckOn) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_TRAFFIC_AUTO_CHECK_ON, isTrafficAutoCheckOn);
		editor.commit();
	}

	public boolean isTrafficAutoCheckOn() {
		return mPreferences.getBoolean(KEY_TRAFFIC_AUTO_CHECK_ON, true);
	}

	public long getCodeUpdateTime() {
		// TODO Auto-generated method stub
		return mPreferences.getLong(CODE_UPDATE_TIME, 0);
	}

	public void setCodeUpdateTime(long now) {
		// TODO Auto-generated method stub
		final Editor editor = mPreferences.edit();
		editor.putLong(CODE_UPDATE_TIME, now);
		editor.commit();
	}

	public void setTodayAlreayConsumedFee(float value) {
		final Editor editor = mPreferences.edit();
		editor.putFloat(KEY_TODAY_ALREADY_USED_FEE, value);
		editor.commit();
	}

	public float getTodayAlreayConsumedFee() {
		return mPreferences.getFloat(KEY_TODAY_ALREADY_USED_FEE, 0);
	}

	public void setSmsVerifyNumber(String ourNO) {
		final Editor editor = mPreferences.edit();
		editor.putString(OUR_SYSTEM_NO, ourNO);
		editor.commit();
	}

	public String getSmsVerifyNumber() {
		return mPreferences.getString(OUR_SYSTEM_NO, "10657516014601");
	}

	// added by zhangjing
	public void setEarliestDailyAvailFee(float avail_fee) {
		final Editor editor = mPreferences.edit();
		editor.putFloat(KEY_EARLIEST_DAILY_AVAILABLE_FEE, avail_fee);
		editor.commit();
	}

	public float getEarliestDailyAvailFee() {
		return mPreferences.getFloat(KEY_EARLIEST_DAILY_AVAILABLE_FEE, 100000f);
	}

	public void setEarliestDailyAvailFeeDate(String date) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_EARLIEST_DAILY_AVAILABLE_DATE, date);
		editor.commit();
	}

	public String getEarliestDailyAvailFeeDate() {
		return mPreferences.getString(KEY_EARLIEST_DAILY_AVAILABLE_DATE, "");
	}

	public void setAccountingDatabaseUpdateTime(String time) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_ACCOUNTING_DATABASE_UPDATE_TIME, time);
		editor.commit();
	}

	public String getAccountingDatabaseUpdateTime() {
		return mPreferences.getString(KEY_ACCOUNTING_DATABASE_UPDATE_TIME, "0");
	}

	// 江苏移动 3元流量叠加包
	public void setOverlayPackageSwitch(boolean overlayPackageSwitch) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_OVERLAY_PACKAGE_SWITCH, overlayPackageSwitch);
		editor.commit();
	}

	public boolean getOverlayPackageSwitch() {
		return mPreferences.getBoolean(KEY_OVERLAY_PACKAGE_SWITCH, false);
	}

	public void setHasOpened(boolean hasOpened) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_HAS_OPENED, hasOpened);
		editor.commit();
	}

	public boolean getHasOpened() {
		return mPreferences.getBoolean(KEY_HAS_OPENED, false);
	}

	public void setHasNotReceivedSmsMiui(int value) {
		final Editor editor = mPreferences.edit();
		editor.putInt(KEY_HAS_NOT_RECEIVED_SMS_MIUI_COUNT, value);
		editor.commit();
	}

	public int getHasNotReceivedSmsMiui() {
		return mPreferences.getInt(KEY_HAS_NOT_RECEIVED_SMS_MIUI_COUNT, 0);
	}

	public void setServicesStartOnBootComplete(boolean isStart) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_IS_SERVICES_START_ON_BOOT_COMPLETE, isStart);
		editor.commit();
	}

	public boolean isServicesStartOnBootComplete() {
		return mPreferences.getBoolean(KEY_IS_SERVICES_START_ON_BOOT_COMPLETE,
				true);
	}

	public void setChannelNomber(String nomber) {
		final Editor editor = mPreferences.edit();
		editor.putString(CHANNEL_NOMBER, nomber);
		editor.commit();
	}

	public String getChannelNomber() {
		return mPreferences.getString(CHANNEL_NOMBER, "0");
	}

	public void setAutoCheck(boolean is) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(IS_AUTO_CHECK, is);
		editor.commit();
	}

	public boolean isAutoCheck() {
		return mPreferences.getBoolean(IS_AUTO_CHECK, true);
	}

	public void setFirstInstalled(boolean is) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_IS_FIRST_INSTALLED, is);
		editor.commit();
	}

	public boolean isFirstInstalled() {
		return mPreferences.getBoolean(KEY_IS_FIRST_INSTALLED, true);
	}

	public void setFirstAccount(boolean is) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_FIRST_ACCOUNT, is);
		editor.commit();
	}

	public boolean getFirstAccount() {
		return mPreferences.getBoolean(KEY_FIRST_ACCOUNT, true);
	}

	// 设置和读取市话资费，默认值为-1
	public void setLocalRates(float rates_for_local) {
		final Editor editor = mPreferences.edit();
		editor.putFloat(KEY_RATES_LOCAL, rates_for_local);
		editor.commit();
	}

	public float getLocalRates() {
		return mPreferences.getFloat(KEY_RATES_LOCAL, -1);
	}

	// 设置和读取长途资费，默认值为-1
	public void setLongRates(float rates_for_long_distance) {
		final Editor editor = mPreferences.edit();
		editor.putFloat(KEY_RATES_LONG_DISTANCE, rates_for_long_distance);
		editor.commit();
	}

	public float getLongRates() {
		return mPreferences.getFloat(KEY_RATES_LONG_DISTANCE, -1);
	}

	// 设置和读取漫游资费，默认值为-1
	public void setRoamingRates(float rates_for_roaming) {
		final Editor editor = mPreferences.edit();
		editor.putFloat(KEY_RATES_ROAMING, rates_for_roaming);
		editor.commit();
	}

	public float getRoamingRates() {
		return mPreferences.getFloat(KEY_RATES_ROAMING, -1);
	}

	// 设置和读取Ip拨号资费，默认值为-1
	public void setRatesIP(float rates_for_ip_dialing) {
		final Editor editor = mPreferences.edit();
		editor.putFloat(KEY_RATES_IP, rates_for_ip_dialing);
		editor.commit();
	}

	public float getRatesIP() {
		return mPreferences.getFloat(KEY_RATES_IP, -1);
	}

	// 计算出来之后设置和读取短号资费，默认值为-1.
	public void setRatesShort(float rates_for_short) {
		final Editor editor = mPreferences.edit();
		editor.putFloat(KEY_RATES_SHORT, rates_for_short);
		editor.commit();
	}

	public float getRatesShort() {
		return mPreferences.getFloat(KEY_RATES_SHORT, -1);
	}

	// 计算出来之后设置短信资费，默认值为-1
	public void setRatesSms(float rates_for_sms) {
		final Editor editor = mPreferences.edit();
		editor.putFloat(KEY_RATES_SMS, rates_for_sms);
		editor.commit();
	}

	public float getRatesSms() {
		return mPreferences.getFloat(KEY_RATES_SMS, -1);
	}

	// 设置和读取超出部分的流量资费，默认值为-1.
	public void setRatesTraffic(float rates_for_traffic) {
		final Editor editor = mPreferences.edit();
		editor.putFloat(KEY_RATES_TRAFFIC, rates_for_traffic);
		editor.commit();
	}

	public float getRatesTraffic() {
		return mPreferences.getFloat(KEY_RATES_TRAFFIC, -1);
	}

	// 设置和读取超出部分的wlan资费
	public void setRatesWlan(float rates_for_wlan) {
		final Editor editor = mPreferences.edit();
		editor.putFloat(KEY_RATES_WLAN, rates_for_wlan);
		editor.commit();
	}

	public float getRatesWlan() {
		return mPreferences.getFloat(KEY_RATES_WLAN, -1);
	}

	// 设置已用的wlan时长
	public void setAlreadyUsedWlan(int duration) {
		final Editor editor = mPreferences.edit();
		editor.putFloat(KEY_WLAN_USED, duration);
		editor.commit();
	}

	// 读取已用的wlan时长
	public int getAlreadyUsedWlan() {
		return mPreferences.getInt(KEY_WLAN_USED, 0);
	}

	public void setSmsSplitRule(String rule) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_SMS_SPLIT_RULE, rule);
		editor.commit();
	}

	public String getSmsSplitRule() {
		return mPreferences.getString(KEY_SMS_SPLIT_RULE, "，；。#；。#。");
	}

	public void setApkFileDir(String apk_dir) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_UPDATE_APK_FILE_DIR, apk_dir);
		editor.commit();
	}

	public String getApkFileDir() {
		return mPreferences.getString(KEY_UPDATE_APK_FILE_DIR, "");
	}

	public void setDeviceInfoSent(boolean is) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_IS_DEVICE_INFO_SENT, is);
		editor.commit();
	}

	public boolean isDeviceInfoSent() {
		return mPreferences.getBoolean(KEY_IS_DEVICE_INFO_SENT, false);
	}

	// added by zhangjing@archermind
	// 增加字段来区分是从receiver中接收运营商短信对帐成功还是从收件箱里面获取运营商的短信对帐成功
	// 字段名称KEY_ACCOUNTING_RECEIVER_OR_INBOX;
	// 默认值为-1
	// 如果从receiver中对帐成功，则值为0；若从收件箱里面对帐成功，则值为1；
	public void setInboxOrReceiver(int accounting_receiver_or_inbox) {
		final Editor editor = mPreferences.edit();
		editor.putInt(KEY_ACCOUNTING_RECEIVER_OR_INBOX,
				accounting_receiver_or_inbox);
		editor.commit();
	}

	public int getInboxOrReceiver() {
		return mPreferences.getInt(KEY_ACCOUNTING_RECEIVER_OR_INBOX, -1);
	}

	// 增加字段，来保存关键字串 //消费总金额
	public void setFeeUsedKeywords(String fee_spent_keywords) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_FEE_SPENT_ACCOUNTING_KEYWORDS, fee_spent_keywords);
		editor.commit();
	}

	public String getFeeUsedKeywords() {
		return mPreferences
				.getString(
						KEY_FEE_SPENT_ACCOUNTING_KEYWORDS,
						"消费的总金额,手机话费总额,未出账话费,未出账费用,即时话费,实时话费,累计话费,话费合计,产生话费,实时费用,实际消费,实收话费,累计消费,消费了,已消费,已使用(话费),!回复,!拨打");
	}

	// 增加字段，来保存关键字串 //剩余总金额
	public void setFeeAvailKeywords(String fee_avail_keywords) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_FEE_AVAILABLE_ACCOUNTING_KEYWORDS,
				fee_avail_keywords);
		editor.commit();
	}

	public String getFeeAvailKeywords() {
		return mPreferences
				.getString(
						KEY_FEE_AVAILABLE_ACCOUNTING_KEYWORDS,
						"当前余额普通用户的可用余额,当月可用预存款总额,话费实时余额,话费账户余额,账户共用余额,预存款余额,账户总余额,帐户总余额,当前的余额,普通话费余额,可用余额,帐户余额,当前余额,最新余额,话费余额,账户余额,余额,目前剩余(话费),!回复,!拨打");
	}

	// 增加字段，来保存关键字串 //已用流量
	public void setTrafficUsedKeywords(String traffic_used_keywords) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_TRAFFIC_USED_KEYWORDS, traffic_used_keywords);
		editor.commit();
	}

	public String getTrafficUsedKeywords() {
		return mPreferences
				.getString(
						KEY_TRAFFIC_USED_KEYWORDS,
						"本月使用的上网数据流量,使用的国内上网数据流量,移动数据流量已使用,已使用移动数据流量,使用的免费移动数据流量,套餐内已优惠的流量,市内上网流量,国内上网流量,国内流量总和,已使用数据流量,已使用流量总和,使用总流量,GPRS使用流量,使用流量总和,流量已使用,已使用流量,已使用量,流量已用,使用流量,流量总和,总和为(流量#上网),已使用(流量#上网#约),已免费(流量#上网),!回复,!拨打");
	}

	// 增加字段，来保存关键字串 //剩余流量
	public void setTrafficAvailKeywords(String traffic_avail_keywords) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_TRAFFIC_AVAILABLE_KEYWORDS, traffic_avail_keywords);
		editor.commit();
	}

	public String getTrafficAvailKeywords() {
		return mPreferences
				.getString(
						KEY_TRAFFIC_AVAILABLE_KEYWORDS,
						"剩余的免费移动数据流量,GPRS剩余,剩余GPRS,套餐内尚余,剩余流量,流量剩余,套餐外流量,剩余量,还剩余(流量#上网),剩余(流量#上网#约),还剩(流量#上网),!回复,!拨打");
	}

	public void setBootComplete(boolean is) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_BOOT_COMPLETE, is);
		editor.commit();
	}

	public boolean isBootComplete() {
		return mPreferences.getBoolean(KEY_BOOT_COMPLETE, false);
	}

	public void setCallsCorrectState(boolean callsCorrectState) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_CALLS_CORRECT_STATE, callsCorrectState);
		editor.commit();
	}

	public boolean getCallsCorrectState() {
		return mPreferences.getBoolean(KEY_CALLS_CORRECT_STATE, true);
	}

	public void setFlowCorrectState(boolean flowCorrectState) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_FLOW_CORRECT_STATE, flowCorrectState);
		editor.commit();
	}

	public boolean getFlowCorrectState() {
		return mPreferences.getBoolean(KEY_FLOW_CORRECT_STATE, true);
	}

	// added by zhangjing 增加了话费和流量指令的关键字以便于指令还原
	public void setCodeForFee(String code_for_fee) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_CODE_FOR_FEE, code_for_fee);
		editor.commit();
	}

	public String getCodeForFee() {
		return mPreferences.getString(KEY_CODE_FOR_FEE, "YE");
	}

	public void setCodeForTraffic(String code_for_traffic) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_CODE_FOR_TRAFFIC, code_for_traffic);
		editor.commit();
	}

	public String getCodeForTraffic() {
		return mPreferences.getString(KEY_CODE_FOR_TRAFFIC, "CXSJLL");
	}

	// 增加字段保存上次事件的余额信息
	public void setLastEventFeeAvail(float last_event_fee_avail) {
		final Editor editor = mPreferences.edit();
		editor.putFloat(KEY_LAST_EVENT_FEE_AVAIL, last_event_fee_avail);
		editor.commit();
	}

	public float getLastEventFeeAvail() {
		return mPreferences.getFloat(KEY_LAST_EVENT_FEE_AVAIL, -100000);
	}

	// 增加字段保存上次事件的已用流量信息
	public void setLastEventTrafficUsed(long last_event_traffic_used) {
		final Editor editor = mPreferences.edit();
		editor.putLong(KEY_LAST_EVENT_TRAFFIC_USED, last_event_traffic_used);
		editor.commit();
	}

	public long getLastEventTrafficUsed() {
		return mPreferences.getLong(KEY_LAST_EVENT_TRAFFIC_USED, -100000);
	}

	public void setDayUserStatus(String days) {
		final Editor editor = mPreferences.edit();
		editor.putString(KEY_DAY_RUNNING_TIME_EVERY_WEEK, days);
		editor.commit();
	}

	public String getDayUserStatus() {
		return mPreferences.getString(KEY_DAY_RUNNING_TIME_EVERY_WEEK, "");
	}

	// 设置上次成功上传用户消费行为数据时刻的api函数
	public void setLastTimeUploadConsumeInfoSuccess(long ms) {
		final Editor editor = mPreferences.edit();
		editor.putLong(KEY_LAST_UPLOAD_USER_CONSUME_INFO_SUCCESS, ms);
		editor.commit();
	}

	// 获取上次成功上传用户消费行为数据时刻的api函数
	public long getLastTimeUploadConsumeInfoSuccess() {
		return mPreferences.getLong(KEY_LAST_UPLOAD_USER_CONSUME_INFO_SUCCESS,
				0);
	}

	// 设置上个月用户消费行为数据是否已经上传的标志变量
	public void setLastMonthConsumeInfoUploadFlag(boolean upload_flag) {
		final Editor editor = mPreferences.edit();
		editor.putBoolean(KEY_UPLOAD_LAST_MONTH_CONSUME_INFO_SUCCESS,
				upload_flag);
		editor.commit();
	}

	// 获取上个月用户消费行为数据是否已经上传的标志变量的api函数
	public boolean getLastMonthConsumeInfoUploadFlag() {
		return mPreferences.getBoolean(
				KEY_UPLOAD_LAST_MONTH_CONSUME_INFO_SUCCESS, false);
	}

	// 设置今日已经试图上传用户月消费行为数据的次数 added by zhangjing@archermind.com
	public void setTodayUploadConsumeInfoCount(int count) {
		final Editor editor = mPreferences.edit();
		editor.putInt(KEY_UPLOAD_MONTH_CONSUME_INFO_COUNT, count);
		editor.commit();
	}

	// 获取今日已经试图上传用户月消费行为数据的次数
	public int getTodayUploadConsumeInfoCount() {
		return mPreferences.getInt(KEY_UPLOAD_MONTH_CONSUME_INFO_COUNT, 0);
	}
}
