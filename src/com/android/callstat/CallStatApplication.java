package com.android.callstat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.android.callstat.common.CallStatUtils;
import com.android.callstat.common.DeviceInformation;
import com.android.callstat.common.DeviceUtils;
import com.android.callstat.common.MTrafficStats;
import com.android.callstat.common.DeviceInformation.InfoName;
import com.android.callstat.common.database.CallStatDatabase;
import com.android.callstat.common.download.CacheFileManager;
import com.android.callstat.common.net.MyHttpPostHelper;
import com.android.callstat.firewall.FirewallCoreWorker;
import com.android.callstat.firewall.FirewallUtils;
import com.android.callstat.firewall.bean.NewTrafficDetail;
import com.android.callstat.firewall.bean.Traffic;
import com.android.callstat.firewall.bean.TrafficDetail;
import com.android.callstat.monitor.ObserverManager;
import com.android.callstat.monitor.bean.MonthlyStatDataSource;
import com.archermind.callstat.R;

public class CallStatApplication extends CrashReportingApplication {
	private static Context callstatsContext;
	// The max cache size.
	public static final int MAX_CACHE_SIZE = (5 * 1024 * 1024);
	public static final String TAG = "callstats";

	/** The default user agent used for downloads */
	public static final String DEFAULT_USER_AGENT = "CallStatDownloadManager";

	public static final int HTTP_SOCKET_TIMEOUT = 20 * 1000; // default to 10

	public static int token;

	public static ArrayList<String> Keywordslist = new ArrayList<String>();
	/** 话费查询KEY */
	public final static int CALL_CHARGES_KEY = 0;
	/** 流量查询KEY */
	public final static int TRAFFIC_QUERY_KEY = 1;
	/** 套餐查询KEY */
	public final static int PACKAGE_MARGIN_KEY = 2;
	public static String[] phoneNum;
	public static String[] callMsg;
	public static String[] trafficMsg;
	public static String[] packageMsg;

	// 该变量的更改仅发生在程序主动对账和主动对账完成时更改
	// 主动对账起始设为 true
	// 对账结果返回设为 false
	public boolean isCheckingAccount = false;

	public boolean isFreshStart = true;

	public boolean trafficIsWarn = false;

	public static boolean isOsNew = true;

	private boolean isUpdating = false;

	public static boolean canFirewallWork = true;
	public static boolean canMyFirewallWork = true;

	public Set<Integer> rejectedList = new HashSet<Integer>();

	public Set<Integer> wifiRejectedList = new HashSet<Integer>();

	public static boolean isFirstTimeAppCreate = false;// 应用程序启动执行CallStatApplication.oncrete的时候保存一个标志值，在connecttivity_change的时候做一些事情
	// 对帐指令的恢复
	public static int feesUsedCodeRestore = 1;
	public static int trafficUsedCodeRestore = 2;
	public static int feesYeCodeRestore = 3;
	public static int trafficYeCodeRestore = 4;
	public static int operatorNumberRestore = 5;
	public static int AllCodeRestore = 0;

	// 控制话费界面的对账动画
	public static boolean calls_anim_is_run = false;
	// 控制流量界面的对账动画
	public static boolean traffic_anim_is_run = false;

	// 加元策略实现的区分不同delta的区间端点
	// 00：17～04：17 加delta1
	// 04:17~08:17 加delta2
	// 08：17 ～12：17 加delta3
	// 12:17~16:17 加delta4
	// 16:17~20:17 加delta5
	// 20:17~00：17 加delta6
	// 上述区间会随着开启飞行模式的时刻而改变，开启飞行模式的时刻，或者是我们的程序启动检测到飞行模式开启的时刻开始，约定为加delta1的区间开始的端点.
	public static String[] time_section_append_delta = { "0017", "0417",
			"0817", "1217", "1617", "2017" };

	public boolean isUpdating() {
		return isUpdating;
	}

	public void setUpdating(boolean isUpdating) {
		this.isUpdating = isUpdating;
	}

	public boolean isFreshStart() {
		return isFreshStart;
	}

	public void setFreshStart(boolean isFreshStart) {
		this.isFreshStart = isFreshStart;
	}

	public ArrayList<ApplicationInfo> appInfoList = new ArrayList<ApplicationInfo>();
	// public static ArrayList<String> confilctAppList = new
	// ArrayList<String>();

	public ArrayList<Activity> activities = new ArrayList<Activity>();

	private final byte[] mAppInfoLock = new byte[0];

	// private ReconciliationBean reconciliationBean = new ReconciliationBean();

	public PackageManager packageManager;

	private CacheFileManager cacheFileManager;

	// private CallStatDatabase callStatDatabase;

	private static ConfigManager configManager;

	private InitResources initResources;

	private PendingIntent airModeIntent;

	private ArrayList<NewTrafficDetail> trafficDetailList = new ArrayList<NewTrafficDetail>();

	public ArrayList<NewTrafficDetail> getTrafficDetailList() {
		return trafficDetailList;
	}

	public void setTrafficDetailList(
			ArrayList<NewTrafficDetail> trafficDetailList) {
		this.trafficDetailList = trafficDetailList;
	}

	public PendingIntent getAirModeIntent() {
		return airModeIntent;
	}

	public void setAirModeIntent(PendingIntent airModeIntent) {
		this.airModeIntent = airModeIntent;
	}

	private int usedTimes;
	private float feesRemain;

	public static Context getCallstatsContext() {
		return callstatsContext;
	}

	/** 初始化资源文件 */
	public void initResources() {
		initResources.Unzip("telPhone.zip");
		ArrayList<ApplicationInfo> appinfoTemp = getAppInfo();
		if (appinfoTemp != null) {
			long totalTraffic = 0;
			for (int i = 0; i < appinfoTemp.size(); i++) {
				totalTraffic += MTrafficStats
						.getUidRxBytes(appinfoTemp.get(i).uid)
						+ MTrafficStats.getUidTxBytes(appinfoTemp.get(i).uid);
			}
			if (totalTraffic - MTrafficStats.getMobileTotalBytes() > 0) {
				configManager.setTotalWifiUsed(totalTraffic
						- MTrafficStats.getMobileTotalBytes());
			}
			configManager.setEarliestWifiLog(configManager.getTotalWifiUsed());
			String data = MTrafficStats.getMobileRxBytes() + "," + MTrafficStats.getMobileTxBytes() + "," + MTrafficStats.getMobileTotalBytes() + ","
					+ MTrafficStats.getWifiRxBytes() + ","
					+ MTrafficStats.getWifiTxBytes() + ","
					+ MTrafficStats.getWifiTotalBytes();
			configManager.setTrafficNode(data);
		}

	}

	/** 月结设置 */
	public void monthlyInit() {
		configManager.setFeeSpent(0.0f);
		configManager.setTotalGprsUsedDifference(0);
		configManager.setTotalGprsUsed(0);
		configManager.setGprsDownload(0);
		configManager.setGprsUpload(0);
		configManager.setCalUsedFee(0);
		configManager.setTotalWifiUsed(0);
		configManager.setWifiDownload(0);
		configManager.setWifiUpload(0);
		configManager.setEarliestGprsLog(0);
		configManager.setEarliestWifiLog(0);
		configManager.setTotalOutgoingCall(0);
		configManager.setTotalSmsSent(0);
		configManager.setTotalIncomingCall(0);
		configManager.setHasOpened(false);
		configManager.setPrevReconcilitionGprsUsed(0);//modified by zhangjing@archermind.com
		configManager.setPrevReconcilitionSendSms(0); //modified by zhangjing@archermind.com

		// add by zhangjing.
		java.util.Date utilDate = new java.util.Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
		String NowDate = formatter.format(utilDate);
		configManager.setEarliestMonthlyFeeAvailableDate(NowDate);
		configManager.setEarliestMonthlyFeeAvailable(configManager
				.getCalculateFeeAvailable());

		configManager.setLastMonthConsumeInfoUploadFlag(false);
	}

	/**
	 * 对账失败后短信信息上传服务器
	 * 
	 * @param message
	 *            短信消息列表
	 */
	public void reconciliationFailed(final ArrayList<String> message) {
		if (message == null || message.size() == 0) {
			return;
		}
		int count = configManager.getSendReconilitionFailedCount();
		if (count == 0) {
			return;
		} else {
			count--;
			configManager.setSendReconilitionFailedCount(count);
		}
		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					Map<String, String> map = new HashMap<String, String>();
					map.put("version",

					configManager.getVersionName());
					String privince = new String(configManager.getProvince()
							.getBytes(), "UTF-8");
					map.put("province", privince);
					String city = new String(
							configManager.getCity().getBytes(), "UTF-8");
					map.put("city", city);
					String operator = new String(configManager.getOperator()
							.getBytes(), "UTF-8");
					map.put("operator", operator);
					String packageBrand = new String(configManager
							.getPackageBrand().getBytes(), "UTF-8");
					map.put("packageBrand", packageBrand);
					// map.put("date", CallStatUtils.getNowTime());
					if (message != null) {
						StringBuffer strBuffer = new StringBuffer();
						for (int i = 0; i < message.size(); i++) {
							if (i != message.size() - 1) {
								strBuffer.append(message.get(i) + "\n");
							} else {
								strBuffer.append(message.get(i)
										+ CallStatUtils.getNowTime() + "\n");
							}
						}
						String msg = new String(
								strBuffer.toString().getBytes(), "UTF-8");
						// Log.i("my", "msg=======" + msg);
						map.put("message", msg);

					}
					String url = getResources().getString(
							R.string.reconciliation_failed_url);
					// Log.i("my", "url------------------" + url);
					if (url == null) {
						return;
					}
					HttpPost myRequest = new HttpPost(url);
					UrlEncodedFormEntity myEntity = MyHttpPostHelper
							.buildUrlEncodedFormEntity(map, null);
					myRequest.setEntity(myEntity);
					DefaultHttpClient myDefaultHttpClient = new DefaultHttpClient();
					HttpResponse myResponse = null;
					myResponse = myDefaultHttpClient.execute(myRequest);
					if (myResponse != null) {
						int status = myResponse.getStatusLine().getStatusCode();
						String strResult = EntityUtils.toString(myResponse
								.getEntity());
						/*
						 * Log.i("my",
						 * myResponse.getStatusLine().getStatusCode() + "");
						 * Log.i("my", "status=" + status + "||对账信息发送失败:" +
						 * strResult);
						 */
					}
				} catch (Exception e) {
					ILog.logException(this.getClass(), e);
					return;
				}

			}
		}).start();

		// }
	}

	/**
	 * 获取对账信息时进行保存
	 * 
	 * @param bundle
	 *            一个bundle对象里面保存的对账信息
	 * @return 此次获取对账信息的类型
	 */
	// public int writeReconciliationInfo2SharedPreferences(Bundle bundle) {
	// try {
	// if (bundle == null) {
	// // Log.i("my", "-1111111111111111111111111");
	// return -1;
	// }
	// ReconciliationBean rb = getReconciliationBean();
	// if (rb == null) {
	// return -1;
	// }
	//
	// // test for zhangjing@archermind.com
	//
	// double feeSpent = bundle.getDouble(
	// configManager.getFeeSpentKeywords(), -1);
	// if (Math.abs(feeSpent + 1) > 1e-6) {
	// if (Math.abs(feeSpent) > 1e-6) {
	// configManager.setFeeSpent((float) feeSpent);
	// // Log.i("my", "1111111111111111111111111");
	// return 1;
	// }
	// }
	//
	// // test for zhangjing@archermind.com
	// feesRemain = (float) bundle.getDouble(
	// configManager.getFeeAvailKeywords(), -1);
	//
	// if (feesRemain != -1) {
	// cacheFileManager.logAccounting("configManager.getFeesRemian()="
	// + configManager.getFeesRemian()
	// + ":configManager.getLastCheckHasYeTime()="
	// + configManager.getLastCheckHasYeTime()
	// + ":configManager.getFeesRemian() - feesRemain="
	// + (configManager.getFeesRemian() - feesRemain)
	// + ":configManager.getPrevReconcilitionGprsUsed()="
	// + configManager.getPrevReconcilitionGprsUsed() + ":"
	// + CallStatUtils.getNowTime() + "\n");
	// if (configManager.getFeesRemian() != 100000
	// && configManager.getLastCheckHasYeTime() != -1
	// && configManager.getFeesRemian() - feesRemain > 0
	// && configManager.getPrevReconcilitionGprsUsed() != -1) {
	// int calledTimes[] = CallStatDatabase.getInstance(
	// callstatsContext).getLatelyLocalCallTime(
	// configManager.getLastCheckHasYeTime(),
	// System.currentTimeMillis());
	// int deltas[] = { 0, 0, 0, 0, 0, 0 };
	// String lastCheck_time = CallStatUtils
	// .changeMilliSeconds2YearMonthDayHourMin(configManager
	// .getLastCheckHasYeTime());
	// String hour_minute = lastCheck_time.substring(8,
	// lastCheck_time.length());
	// int delta_begin_index = CallStatUtils
	// .which_delta_begin_to_add(hour_minute);
	//
	// long s = (System.currentTimeMillis() - configManager
	// .getLastCheckHasYeTime()) / 1000;
	// long steps = s / (4 * 3600);// 求出时间差跨了几个4小时，若1个就没有跨，则deltas数组每个元素均为0；
	//
	// // 求所加各个delta对应的各个系数的计算方法
	// for (int ii = 1; ii <= steps; ii++) {
	// if (ii % 6 == 1) {
	// deltas[delta_begin_index % 6]++;
	// } else if (ii % 6 == 2) {
	// deltas[(delta_begin_index + 1) % 6]++;
	// } else if (ii % 6 == 3) {
	// deltas[(delta_begin_index + 2) % 6]++;
	// } else if (ii % 6 == 4) {
	// deltas[(delta_begin_index + 3) % 6]++;
	// } else if (ii % 6 == 5) {
	// deltas[(delta_begin_index + 4) % 6]++;
	// } else if (ii % 6 == 0) {
	// deltas[(delta_begin_index + 5) % 6]++;
	// }
	// }
	//
	// int smsNum = CallStatUtils
	// .getPrevReconcilitaionToNowSmsUsed(configManager);
	// long trafficData = CallStatUtils
	// .getPrevReconcilitionToNowGprsUsed(configManager);
	// double difference = configManager.getFeesRemian()
	// - feesRemain;
	// cacheFileManager.logAccounting("CALL_LOCAL:"
	// + calledTimes[0] + ",CALL_LONG_DISTANCE:"
	// + calledTimes[1] + ",INCOMING_TYPE:"
	// + calledTimes[2] + ",CALL_UNKONW:" + calledTimes[3]
	// + ",CALL_IP:" + calledTimes[4] + ",CALL_SHORT:"
	// + calledTimes[5] + ",smsNum:" + smsNum
	// + ",trafficData:" + trafficData + ",difference:"
	// + difference + ":" + CallStatUtils.getNowTime()
	// + "\n");
	//
	// if ((smsNum != 0 || trafficData != 0 || calledTimes[0] != 0
	// || calledTimes[1] != 0 || calledTimes[2] != 0
	// || calledTimes[3] != 0 || calledTimes[4] != 0
	// || calledTimes[5] != 0 || !CallStatUtils
	// .isAllZeros(deltas)) // deltas系数不全为0，也往数据库中存放。
	// && CallStatUtils.getNowDateDate() != configManager
	// .getAccountingDay()) {
	//
	// CallStatDatabase
	// .getInstance(callstatsContext)
	// .createTable(
	// CallStatDatabase.TABLE_RECONCILIATION_INFO);
	// CallStatDatabase
	// .getInstance(callstatsContext)
	// .addReconciliationInfo(
	// new ReconciliationInfo(Long
	// .parseLong(CallStatUtils
	// .getNowTime()),
	// calledTimes[0], calledTimes[1],
	// calledTimes[2], calledTimes[3],
	// calledTimes[4], calledTimes[5],
	// smsNum, trafficData,
	// difference, deltas[0],
	// deltas[1], deltas[2],
	// deltas[3], deltas[4], deltas[5]));
	// }
	// }
	// // 以下添加语句表示明显的充值行为，added by zhangjing
	// if (configManager.getFeesRemian() != 100000
	// && configManager.getFeesRemian() - feesRemain < 0) {
	// java.util.Date utilDate = new java.util.Date();
	// SimpleDateFormat formatter = new SimpleDateFormat(
	// "yyyyMMdd");
	// String NowDate = formatter.format(utilDate);
	// configManager.setEarliestMonthlyFeeAvailableDate(NowDate);
	// configManager.setEarliestMonthlyFeeAvailable(feesRemain
	// - configManager.getFeesRemian()
	// + configManager.getEarliestMonthlyFeeAvailable());
	// }
	//
	// if (configManager.getCalculateFeeAvailable() != 100000) {
	// configManager.setPrevCalculateFeeAvailable(configManager
	// .getCalculateFeeAvailable());
	// cacheFileManager.logAccounting("本次计算预估误差范围为:"
	// + Math.abs(feesRemain
	// / configManager.getCalculateFeeAvailable()
	// - 1) + "本次对账值为：" + feesRemain + "上次预估值为："
	// + configManager.getCalculateFeeAvailable());
	// configManager.setFeeCalculateDeviation(Math.abs(feesRemain
	// - configManager.getCalculateFeeAvailable()));
	// }
	// configManager.setFeesRemain(feesRemain);
	// configManager.setLastCheckHasYeTime(System.currentTimeMillis());
	// configManager.setPrevReconcilitionGprsUsed(configManager
	// .getTotalGprsUsed());
	// configManager.setPrevReconcilitionSendSms(configManager
	// .getTotalSmsSent());
	// configManager.setCalculateFeeAvailable(feesRemain);
	// // Log.i("my", "22222222222222222222222");
	// return 2;
	// }
	//
	// // test for zhangjing@archermind.com
	// double totalGprsMargin = bundle.getDouble(
	// configManager.getTrafficAvailKeywords(), -1);
	// if (totalGprsMargin != -1 && Math.abs(totalGprsMargin) > 1e-6) {
	// configManager.setTotalGprsMargin((long) totalGprsMargin);
	// // Log.i("my", "33333333333333333333333333");
	// return 3;
	// }
	// // test for zhangjing@archermind.com
	// double totalGprsUsed = bundle.getDouble(
	// configManager.getTrafficSpentKeywords(), -1);
	// /*// 如果对出来的项是总共使用的Gprs流量，并且对出来的数据小于UI上显示的已经使用的数据，说明是换了套餐的，做一个特殊处理。
	// if (Math.abs(totalGprsUsed + 1) > 1e-6
	// && totalGprsUsed < configManager.getTotalGprsUsed()
	// + configManager.getTotalGprsUsedDifference()) {
	// return 4;
	// }*/
	// if (Math.abs(totalGprsUsed + 1) > 1e-6) {
	// if (Math.abs(totalGprsUsed) > 1e-6) {
	// long difference = (long) totalGprsUsed
	// - configManager.getTotalGprsUsed();
	// configManager.setTotalGprsUsedDifference(difference);
	// configManager.setLastCheckHasTrafficTime(System
	// .currentTimeMillis());// 对出流量后保存成功的时间（lxj）
	// // Log.i("my", "4444444444444444444444");
	// return 4;
	// }
	// }
	//
	// } catch (Exception e) {
	// ILog.logException(this.getClass(), e);
	// return -1;
	// }
	//
	// return 0;
	// }

	/**
	 * 获取已安装的还有网络权限的应用程序列表
	 * 
	 * @return 此程序列表
	 */
	public ArrayList<ApplicationInfo> getAppInfo() {
		try {
			ArrayList<ApplicationInfo> appInfoList = (ArrayList<ApplicationInfo>) packageManager
					.getInstalledApplications(0);
			abc: for (int i = 0; i < appInfoList.size(); i++) {
				ApplicationInfo appInfo = appInfoList.get(i);
				for (int j = i + 1; j < appInfoList.size(); j++) {
					if (appInfo.uid == appInfoList.get(j).uid) {
						appInfoList.remove(appInfo);
						i--;
						continue abc;
					}
				}
				if (!CallStatUtils.hasPermission5App(callstatsContext, appInfo,
						"android.permission.INTERNET")) {
					appInfoList.remove(appInfo);
					i--;
				}
				if (i == appInfoList.size() - 1) {
					return appInfoList;
				}
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
		return appInfoList;
	}

	public boolean initTrafficDetail() {

		CallStatDatabase.getInstance(callstatsContext).createTable(
				CallStatDatabase.TABLE_TRAFFIC_DETAIL);
		List<NewTrafficDetail> list = CallStatDatabase.getInstance(
				callstatsContext).getLatestNewTrafficDetail();

		FirewallUtils.applyRejectedList(list);

		return true;
	}

	/**
	 * 初始化流量统计信息
	 * 
	 * @return
	 */
	public boolean initTrafficLog() {
		// Log.i("my", "开始初始化数据流量统计信息");
		synchronized (mAppInfoLock) {
			if (appInfoList.size() == 0) {
				appInfoList = getAppInfo();
			}
		}
		refreshTrafficLog();
		return true;
	}

	/**
	 * 刷新配置文件中的总的流量信息统计表
	 */
	private int[] refreshSharedPreferences(Traffic trafficGprs,
			Traffic trafficWifi) {
		// Log.e("my", "refreshSharedPreferences is called");
		ILog.LogE(
				this.getClass(),
				"configManager.getTotalGprsUsed()="
						+ configManager.getTotalGprsUsed()
						+ "	configManager.getEarliestGprsLog()="
						+ configManager.getEarliestGprsLog());
		int[] warnCode = { 0, 0 };
		try {
			if (trafficGprs != null && trafficWifi != null) {
				configManager
						.setTotalGprsUsed(configManager.getTotalGprsUsed()
								+ (trafficGprs.getDownload() + trafficGprs
										.getUpload()));
				configManager.setGprsDownload(configManager.getGprsDownload()
						+ trafficGprs.getDownload());
				configManager.setGprsUpload(configManager.getGprsUpload()
						+ trafficGprs.getUpload());
				configManager
						.setTotalWifiUsed(configManager.getTotalWifiUsed()
								+ (trafficWifi.getDownload() + trafficWifi
										.getUpload()));
				configManager.setWifiDownload(configManager.getWifiDownload()
						+ trafficWifi.getDownload());
				configManager.setWifiUpload(configManager.getWifiUpload()
						+ trafficWifi.getUpload());
				if (configManager.getUpdataTrafficMonth() != null) {
					if (!configManager.getUpdataTrafficMonth().equals(
							CallStatUtils.getNowMonth())
							&& CallStatUtils.getNowDateDate() >= configManager
									.getAccountingDay()) {
						monthlyInit();
						configManager.setUpdataTrafficMonth(CallStatUtils
								.getNowMonth());
					}
				} else {
					if (CallStatUtils.getNowDateDate() >= configManager
							.getAccountingDay()) {
						configManager.setUpdataTrafficMonth(CallStatUtils
								.getNowMonth());
					}
				}
				// 判断当前日期，更新每日流量节点
				if (configManager.getUpdataTrafficDate() != null) {
					if (!configManager.getUpdataTrafficDate().equals(
							CallStatUtils.getNowDate())) {
						configManager.setTodayUploadConsumeInfoCount(0);// 在每日最早时刻，将这个变量复位为0；
						configManager.setYesterdayGprsUsed(configManager
								.getTotalGprsUsed()
								- configManager.getEarliestGprsLog());
						configManager.setEarliestGprsLog(configManager
								.getTotalGprsUsed());
						configManager.setEarliestWifiLog(configManager
								.getTotalWifiUsed());
						configManager.setUpdataTrafficDate(CallStatUtils
								.getNowDate());
						configManager.setUpdateAccountCode(false);

						// 跨天后保存 昨日 使用时间，并重置DayTotalRunningTime
						try {
							int dayTime = configManager
									.getDayTotalRunningTime();
							String dayString = configManager.getDayUserStatus();
							String daystatus = "";
							long nowTime = System.currentTimeMillis();
							long lastDayTime = nowTime - 24 * 60 * 60 * 1000l;
							if (!"".equals(dayString)) {
								daystatus = dayString
										+ ":"
										+ dayTime
										+ ","
										+ CallStatUtils
												.getDateStringFromMilliSec(nowTime);
							} else {
								daystatus = CallStatUtils
										.getDateStringFromMilliSec(lastDayTime)
										+ ":"
										+ dayTime
										+ ","
										+ CallStatUtils
												.getDateStringFromMilliSec(nowTime);
							}
							configManager.setDayUserStatus(daystatus);
							configManager.setDayTotalRunningTime(0);
						} catch (Exception e) {
							ILog.logException(this.getClass(), e);
						}

						// 跨天了 jing1.zhang@archermind.com
						ConnectivityManager connectivityManager = (ConnectivityManager) callstatsContext
								.getSystemService(Context.CONNECTIVITY_SERVICE);
						NetworkInfo wifiInfo = connectivityManager
								.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
						final String type = wifiInfo.isConnected() ? CallStatDatabase.TRAFFIC_DETAIL_TYPE_WIFI
								: CallStatDatabase.TRAFFIC_DETAIL_TYPE_GPRS;
						final Context context_used = callstatsContext;
						new Thread(new Runnable() {
							@Override
							public void run() {
								CallStatDatabase.getInstance(context_used)
										.updateYesterdayAndInitTodayTrafficDetailList(type);
							}
						}).start();

					}
				} else {
					configManager.setYesterdayGprsUsed(0);
					configManager.setEarliestGprsLog(configManager
							.getTotalGprsUsed());
					configManager.setUpdataTrafficDate(CallStatUtils
							.getNowDate());
				}
				if (configManager.getFreeGprs() != 100000) {
					float remain = configManager.getFreeGprs()
							* 1024
							* 1024
							- (configManager.getTotalGprsUsed() + configManager
									.getTotalGprsUsedDifference());
					if (remain <= configManager.getAlertTrafficNotice() * 1024 * 1024) {
						if (remain >= 0) {
							warnCode[0] = 1;
						} else {
							warnCode[0] = 2;
						}
					}
				}

				if ((configManager.getTotalGprsUsed() - configManager
						.getEarliestGprsLog()) >= (configManager
						.getAlertTodayTrafficNotice() * 1024 * 1024)
						&& (configManager.getTotalGprsUsed() - configManager
								.getEarliestGprsLog()) != 0) {
					warnCode[1] = 1;
				}
			}
			// }).start();
			// }
			if (configManager.getEarliestDailyAvailFee() != 100000) {
				if (!configManager.getEarliestDailyAvailFeeDate()
						.equalsIgnoreCase(CallStatUtils.getNowDate())) {
					configManager.setEarliestDailyAvailFeeDate(CallStatUtils
							.getNowDate());
					configManager.setEarliestDailyAvailFee(configManager
							.getCalculateFeeAvailable());
				}
			} else {
				if (configManager.getCalculateFeeAvailable() != 100000) {
					configManager.setEarliestDailyAvailFee(configManager
							.getCalculateFeeAvailable());
					java.util.Date utilDate = new java.util.Date();
					SimpleDateFormat formatter = new SimpleDateFormat(
							"yyyyMMdd");
					String NowDate = formatter.format(utilDate);
					configManager.setEarliestDailyAvailFeeDate(NowDate);
				}
			}
			// refresh each day consume traffic
			/*
			 * Log.e("my","configManager.getCalculateFeeAvailable()="+configManager
			 * . getCalculateFeeAvailable()+
			 * ",configManager.getEarliestDailyAvailFee()="
			 * +configManager.getEarliestDailyAvailFee());
			 */
			new refreshEachDayTrafficConsumeThread().start();
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
		return warnCode;
	}

	public class refreshEachDayTrafficConsumeThread extends Thread {
		public void run() {
			try {
				CallStatDatabase.getInstance(getCallstatsContext())
						.refreshEachDayTrafficConsume();
			} catch (Exception e) {
				ILog.logException(this.getClass(), e);
			}

		}
	}

	/**
	 * 有软件安装时进行的操作
	 * 
	 * @param packageName
	 *            软件包名
	 */
	public void addPackage(String packageName) {
		try {
			synchronized (mAppInfoLock) {
				ApplicationInfo appInfo = packageManager.getApplicationInfo(
						packageName, 0);
				if (CallStatUtils.hasPermission5App(callstatsContext, appInfo,
						"android.permission.INTERNET")) {
					appInfoList.add(appInfo);
					String date = CallStatUtils.getNowDate();
					String appName = packageManager
							.getApplicationLabel(appInfo).toString();
					int uid = appInfo.uid;

					long current_node_upload = MTrafficStats.getUidTxBytes(uid);

					long current_node_download = MTrafficStats
							.getUidRxBytes(uid);

					ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
					NetworkInfo wifiInfo = connectivityManager
							.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
					boolean nowWifiOn = wifiInfo.isConnected() ? true : false;

					final NewTrafficDetail td = new NewTrafficDetail(date, uid,
							appName, packageName, false, false, 0, 0, 0, 0,
							current_node_upload, current_node_download,
							nowWifiOn);

					new Thread(new Runnable() {

						@Override
						public void run() {
							CallStatDatabase.getInstance(callstatsContext)
									.updateNewTrafficDetail(td);
						}
					}).start();
				}
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
		// Log.i("callstats", "有软件被安装");
	}

	/**
	 * 有应用被卸载时所进行的操作
	 * 
	 * @param packageName
	 *            被卸载应用的包名
	 */
	public void removedPackage(String packageName) {
		try {
			synchronized (mAppInfoLock) {
				if (packageManager != null) {
					for (int i = 0; i < appInfoList.size(); i++) {
						if (appInfoList.get(i).packageName.equals(packageName)) {
							ApplicationInfo appInfo = appInfoList.get(i);
							final int uid = appInfo.uid;
							// Log.i("callstats",
							// "android.permission.INTERNET");
							appInfoList.remove(appInfo);
							new Thread(new Runnable() {

								@Override
								public void run() {
									CallStatDatabase.getInstance(
											callstatsContext)
											.deleteNewTrafficDetail(uid);
								}
							}).start();
							// Log.i("callstats", "sofware uninstalled...");
							break;
						}
					}
				}
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	/**
	 * 刷新所有程序数据流量统计信息
	 */
	public int[] refreshTrafficLog() {
		// TODO Auto-generated method stub
		try {
			String data = MTrafficStats.getMobileRxBytes() + ","
					+ MTrafficStats.getMobileTxBytes() + ","
					+ MTrafficStats.getMobileTotalBytes() + ","
					+ MTrafficStats.getWifiRxBytes() + ","
					+ MTrafficStats.getWifiTxBytes() + ","
					+ MTrafficStats.getWifiTotalBytes();
			CacheFileManager.getInstance().logAccounting(CallStatUtils.getNowTime()+"		总GPRS接收数据："+MTrafficStats.getMobileRxBytes()+",总GPRS发送数据:"+MTrafficStats.getMobileTxBytes()+",总GPRS数据:"+MTrafficStats.getMobileTotalBytes()+
					"	总wifi接收数据："+MTrafficStats.getWifiRxBytes()+",总wifi发送数据:"+MTrafficStats.getWifiTxBytes()+",总wifi数据:"+MTrafficStats.getWifiTotalBytes());
			String onstr = configManager.getTrafficNode();// 读取on.txt记录到onstr里
			// Log.i("my", "onstr=" + onstr);
			// Log.i("my", "data=" + data);
			String ondata[] = onstr.split(",");// 将onstr各项分离 放到ondata里
			// 计算增量
			String[] delta = new String[6];
			String nowdata[] = data.split(",");
			delta[0] = String.valueOf(Long.parseLong(nowdata[0])
					- Long.parseLong(ondata[0]));
			delta[1] = String.valueOf(Long.parseLong(nowdata[1])
					- Long.parseLong(ondata[1]));
			delta[2] = String.valueOf(Long.parseLong(nowdata[2])
					- Long.parseLong(ondata[2]));
			delta[3] = String.valueOf(Long.parseLong(nowdata[3])
					- Long.parseLong(ondata[3]));
			delta[4] = String.valueOf(Long.parseLong(nowdata[4])
					- Long.parseLong(ondata[4]));
			delta[5] = String.valueOf(Long.parseLong(nowdata[5])
					- Long.parseLong(ondata[5]));
			StringBuffer deltadata = new StringBuffer();
			for (int i = 0; i < delta.length; i++) {
				if (i != delta.length - 1) {
					deltadata.append(delta[i] + ",");
				} else {
					deltadata.append(delta[i]);
				}
			}
			configManager.setTrafficNode(data);
			if(onstr.equals("0,0,0,0,0,0")){ // modified by zhangjing@archermind.com
				return new int[]{-1,-1};
			}
			System.gc();
			if ((Long.parseLong(delta[3]) < 0 || Long.parseLong(delta[4]) < 0)
					&& (Long.parseLong(delta[0]) < 0 || Long
							.parseLong(delta[1]) < 0)) {
				return refreshSharedPreferences(
						new Traffic(String.valueOf(System.currentTimeMillis()),
								CallStatUtils.getNowMonth(), "gprs",
								Long.parseLong(nowdata[1]),
								Long.parseLong(nowdata[0])),
						new Traffic(String.valueOf(System.currentTimeMillis()),
								CallStatUtils.getNowMonth(), "wifi", Long
										.parseLong(nowdata[4]), Long
										.parseLong(nowdata[3])));
			} else if (Long.parseLong(delta[3]) < 0
					|| Long.parseLong(delta[4]) < 0) {
				return refreshSharedPreferences(
						new Traffic(String.valueOf(System.currentTimeMillis()),
								CallStatUtils.getNowMonth(), "gprs",
								Long.parseLong(delta[1]),
								Long.parseLong(delta[0])),
						new Traffic(String.valueOf(System.currentTimeMillis()),
								CallStatUtils.getNowMonth(), "wifi", Long
										.parseLong(nowdata[4]), Long
										.parseLong(nowdata[3])));
			} else if (Long.parseLong(delta[0]) < 0
					|| Long.parseLong(delta[1]) < 0) {
				return refreshSharedPreferences(
						new Traffic(String.valueOf(System.currentTimeMillis()),
								CallStatUtils.getNowMonth(), "gprs",
								Long.parseLong(nowdata[1]),
								Long.parseLong(nowdata[0])),
						new Traffic(String.valueOf(System.currentTimeMillis()),
								CallStatUtils.getNowMonth(), "wifi", Long
										.parseLong(delta[4]), Long
										.parseLong(delta[3])));
			} else {
				return refreshSharedPreferences(
						new Traffic(String.valueOf(System.currentTimeMillis()),
								CallStatUtils.getNowMonth(), "gprs",
								Long.parseLong(delta[1]),
								Long.parseLong(delta[0])),
						new Traffic(String.valueOf(System.currentTimeMillis()),
								CallStatUtils.getNowMonth(), "wifi", Long
										.parseLong(delta[4]), Long
										.parseLong(delta[3])));
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
			return null;
		}
	}

	/**
	 * 刷新每个应用程序的流量统计信息(更详细的统计信息,包含日期信息（可以准确的分析哪个区间用了多少流量）)
	 */
	public ArrayList<NewTrafficDetail> refreshNewTrafficDetail() {

		final ArrayList<NewTrafficDetail> trafficDetailList = (ArrayList<NewTrafficDetail>) CallStatDatabase
				.getInstance(callstatsContext).getLatestNewTrafficDetail();
		try {
			long times = System.currentTimeMillis();
			int len = appInfoList.size();
			int size = trafficDetailList.size();
			/*
			 * Log.i(TAG,
			 * "CallStatDatabase.getInstance(callstatsContext).getLatestTrafficDetail().size():"
			 * + size + "\n" + "appInfoList.size():" + len);
			 * Log.e("my","The ListGetFrom refreshTrafficDetail:");
			 */

			if (size > 0 && len > 0 && size != len) {
				Log.i(TAG, "trafficDetailList.size():" + size
						+ " appInfoList.size():" + len);
				synchronizeNewTrafficDetail(trafficDetailList, appInfoList);
			} /*
			 * else if (size > 0 && len > 0 && size == len) {
			 * CallStatUtils.sortByNewTraffic(trafficDetailList);
			 * Log.e("my","refreshTrafficDetail is in" + "size == len"); }
			 */else {
				if (len == 0) {
					appInfoList = getAppInfo();
					len = appInfoList.size();
				}
				if (size == 0) {
					synchronized (mAppInfoLock) {
						ApplicationInfo appInfo;

						for (int i = 0; i < len; i++) {
							String date = CallStatUtils.getNowDate();
							appInfo = appInfoList.get(i);
							String appName = (String) packageManager
									.getApplicationLabel(appInfo);

							int uid = appInfo.uid;

							String packageName = appInfo.packageName;

							long current_node_upload = 0;
							long current_node_download = 0;

							ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
							NetworkInfo wifiInfo = connectivityManager
									.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
							boolean isWifiOn = wifiInfo.isConnected() ? true
									: false;

							current_node_upload = MTrafficStats
									.getUidTxBytes(uid);
							current_node_download = MTrafficStats
									.getUidRxBytes(uid);

							// Log.e("my","the first time initial database current_node_upload="+current_node_upload+",current_node_download"+current_node_download);
							NewTrafficDetail td = new NewTrafficDetail(date,
									uid, appName, packageName, false, false, 0,
									0, 0, 0, current_node_upload,
									current_node_download, isWifiOn);

							trafficDetailList.add(td);
						}

					}
				}

			}
			new AsyncTask<Void, Void, Void>() {

				@Override
				protected Void doInBackground(Void... params) {
					CallStatDatabase.getInstance(callstatsContext)
							.updateNewTrafficDetail(trafficDetailList);
					return null;
				}
			}.execute();
			CallStatUtils.sortByNewTraffic(trafficDetailList);
			Log.i("callstats",
					"total ()Times============"
							+ (System.currentTimeMillis() - times));
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
		return trafficDetailList;
	}

	public void initNewTrafficDetailList() {

		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifiInfo = connectivityManager
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		String type = wifiInfo.isConnected() ? CallStatDatabase.TRAFFIC_DETAIL_TYPE_WIFI
				: CallStatDatabase.TRAFFIC_DETAIL_TYPE_GPRS;

		// 完成数据库和当前应用程序的同步工作
		trafficDetailList = (ArrayList<NewTrafficDetail>) CallStatDatabase
				.getInstance(this).getLatestNewTrafficDetail(type);
		if (appInfoList.size() == 0) {
			appInfoList = getAppInfo();
		}
		if (trafficDetailList.size() == 0) {
			trafficDetailList = initNewTrafficDetailList(appInfoList);
			CallStatDatabase.getInstance(this).updateNewTrafficDetail(
					trafficDetailList);
		} else {
			synchronizeNewTrafficDetail(trafficDetailList, appInfoList);
			// 如果有今日记录
			// 今日所有的数据均保留，只更新今日的node数据
			CallStatDatabase.getInstance(this).refreshEachDayNodeData();
		}
	}

	public ArrayList<NewTrafficDetail> refreshNewTrafficDetail(String type) {

		ArrayList<NewTrafficDetail> trafficDetailList = (ArrayList<NewTrafficDetail>) CallStatDatabase
				.getInstance(callstatsContext).getLatestNewTrafficDetail(type);
		try {
			long times = System.currentTimeMillis();
			int len = appInfoList.size();
			int size = trafficDetailList.size();
			ILog.LogE(this.getClass(), "size=" + size);
			if (len == 0) {
				appInfoList = getAppInfo();
				len = appInfoList.size();
			}
			if (size == 0) {
				ILog.LogE(this.getClass(),
						"today refreshNewTrafficDetail(String type) in");
				trafficDetailList = initNewTrafficDetailList(appInfoList);
			}
			new AsyncTask<Object, Void, Object>() {
				@Override
				protected Object doInBackground(Object... params) {
					ArrayList<NewTrafficDetail> list = (ArrayList<NewTrafficDetail>) params[0];
					CallStatDatabase.getInstance(callstatsContext)
							.updateNewTrafficDetail(list);
					return null;
				}
			}.execute(trafficDetailList);
			CallStatUtils.sortByNewTraffic(trafficDetailList);
			Log.i("callstats", "refreshNewTrafficDetail Times============"
					+ (System.currentTimeMillis() - times));
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}

		return trafficDetailList;

	}

	public ArrayList<NewTrafficDetail> initNewTrafficDetailList(
			ArrayList<ApplicationInfo> appInfoList) {
		ApplicationInfo appInfo;
		ArrayList<NewTrafficDetail> trafficDetailList = new ArrayList<NewTrafficDetail>();
		int len = appInfoList.size();
		try {
			for (int i = 0; i < len; i++) {
				String date = CallStatUtils.getNowDate();
				appInfo = appInfoList.get(i);
				String appName = (String) packageManager
						.getApplicationLabel(appInfo);

				int uid = appInfo.uid;

				String packageName = appInfo.packageName;

				long current_node_upload = 0;
				long current_node_download = 0;

				ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo wifiInfo = connectivityManager
						.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
				boolean isWifiOn = wifiInfo.isConnected() ? true : false;

				current_node_upload = MTrafficStats.getUidTxBytes(uid);
				current_node_download = MTrafficStats.getUidRxBytes(uid);

				// Log.e("my","the first time initial database current_node_upload="+current_node_upload+",current_node_download"+current_node_download);
				NewTrafficDetail td = new NewTrafficDetail(date, uid, appName,
						packageName, false, false, 0, 0, 0, 0,
						current_node_upload, current_node_download, isWifiOn);

				trafficDetailList.add(td);
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}

		return trafficDetailList;
	}

	public void synchronizeNewTrafficDetail(
			ArrayList<NewTrafficDetail> newTrafficDetailList,
			ArrayList<ApplicationInfo> appInfoList) {
		try {
			long now = System.currentTimeMillis();
			Map<Integer, NewTrafficDetail> tdMap = getNewTrafficDetailMapFromNewTrafficDetailList(newTrafficDetailList);
			Set<Integer> uids_db = tdMap.keySet();
			Map<Integer, ApplicationInfo> hashMap = getApplicationInfoMapFromAppInfoList(appInfoList);
			Set<Integer> uids_appinfo = hashMap.keySet();

			for (ApplicationInfo info : appInfoList) {
				int uid = info.uid;
				if (uids_db.contains(uid)) {
					uids_db.remove(uid);
					uids_appinfo.remove(uid);
				}
			}

			// 数据库冗余数据清理
			if (uids_db.size() > 0) {
				// Log.e("callstats", "synchronizeTrafficDetail:数据库冗余数据清理");
				for (Integer uid : uids_db) {
					CallStatDatabase.getInstance(callstatsContext)
							.deleteNewTrafficDetail(uid);
					newTrafficDetailList.remove(tdMap.get(uid));
				}
			}

			// 数据库缺失信息添加
			if (uids_appinfo.size() > 0) {
				String date = CallStatUtils.getNowDate();
				// Log.e("callstats", "synchronizeTrafficDetail:数据库缺失信息添加");
				for (Integer uid : uids_appinfo) {
					ApplicationInfo info = hashMap.get(uid);
					String appName = (String) packageManager
							.getApplicationLabel(info);
					String packageName = info.packageName;
					/*
					 * TrafficDetail td = new TrafficDetail(uid, appName,
					 * packageName, false, current_node_upload, // 历史数据+当前节点
					 * current_node_download, // 历史数据+当前节点 // 直接用当前node更新到数据库
					 * current_node_upload, current_node_download);
					 */
					ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
					NetworkInfo wifiInfo = connectivityManager
							.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
					boolean nowWifiOn = wifiInfo.isConnected() ? true : false;

					NewTrafficDetail td = new NewTrafficDetail(date, uid,
							appName, packageName, false, false, 0, 0, 0, 0,
							MTrafficStats.getUidRxBytes(uid),
							MTrafficStats.getUidTxBytes(uid), nowWifiOn);
					CallStatDatabase.getInstance(callstatsContext)
							.updateNewTrafficDetail(td);
					newTrafficDetailList.add(td);
				}
			}
			ILog.LogI(
					this.getClass(),
					"synchronizeTrafficDetail finished,time cost:"
							+ (System.currentTimeMillis() - now));
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	private Map<Integer, NewTrafficDetail> getNewTrafficDetailMapFromNewTrafficDetailList(
			ArrayList<NewTrafficDetail> trafficDetailList) {
		Map<Integer, NewTrafficDetail> hashMap = new HashMap<Integer, NewTrafficDetail>();
		for (NewTrafficDetail td : trafficDetailList) {
			hashMap.put(td.getUid(), td);
		}
		return hashMap;
	}

	private Map<Integer, TrafficDetail> getTrafficDetailMapFromTrafficDetailList(
			ArrayList<TrafficDetail> trafficDetailList) {
		Map<Integer, TrafficDetail> hashMap = new HashMap<Integer, TrafficDetail>();
		for (TrafficDetail td : trafficDetailList) {
			hashMap.put(td.getUid(), td);
		}
		return hashMap;
	}

	private Map<Integer, ApplicationInfo> getApplicationInfoMapFromAppInfoList(
			ArrayList<ApplicationInfo> appInfoList) {
		Map<Integer, ApplicationInfo> hashMap = new HashMap<Integer, ApplicationInfo>();
		for (ApplicationInfo info : appInfoList) {
			hashMap.put(info.uid, info);
		}
		return hashMap;
	}

	private Set<Integer> getUidsFromTrafficDetailList(
			ArrayList<TrafficDetail> trafficDetailList) {
		Set<Integer> uids = new TreeSet<Integer>();

		for (TrafficDetail detail : trafficDetailList) {
			uids.add(detail.getUid());
		}
		return uids;
	}

	private static CallStatApplication instance;

	public CallStatApplication() {
		CallStatApplication.instance = this;
	}

	public static CallStatApplication getInstance() {
		return instance;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		ILog.LogI(CallStatApplication.class,
				Environment.getExternalStorageState());
		ILog.LogI(this.getClass(), "onCreate:" + System.currentTimeMillis());
		// 保证文件管理类先初始化
		cacheFileManager = CacheFileManager.init(this);
		callstatsContext = getApplicationContext();
		configManager = new ConfigManager(callstatsContext);
		canFirewallWork = configManager.canFirewallWork();
		canMyFirewallWork = configManager.canMineFirewallWork();
		ObserverManager.getObserverManager(this).registerObservers();
		// 将apk资源解压到data下
		initResources = new InitResources(callstatsContext);
		isOsNew = Float.parseFloat(DeviceUtils.getOsVersion().substring(0, 3)) >= 2.2;
		packageManager = callstatsContext.getPackageManager();
		appInfoList = getAppInfo();
		processData();
		// refreshNewTrafficDetail();
		ILog.LogI(this.getClass(),
				"onCreate: end:" + System.currentTimeMillis());
		isFirstTimeAppCreate = true;// added by zhangjing@archermind.com
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				initNewTrafficDetailList();
				CallStatUtils.updateSmSMatchRule(callstatsContext);
				return null;
			}
		}.execute();
		CallStatUtils.InitContactMap();// 初始化联系人号码和姓名的对应关系列表 added by
										// zhangjing@archermind.com
	}

	public void processData() {
		usedTimes = configManager.getAppUsedTimes() + 1;
		configManager.setAppUsedTimes(usedTimes);
		long startuptime = System.currentTimeMillis();
		configManager.setStartUpTime(startuptime);

		// sendData();
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		configManager.setUpdateNotice(true);
		Log.v(DebugFlags.LOGTAG, "#########onTerminate");
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.archermind.callstat.CrashReportingApplication#getCrashResources()
	 */
	@Override
	public Bundle getCrashResources() {
		final Bundle result = new Bundle();
		try {
			result.putString(RES_EMAIL_SUBJECT,
					getResources().getText(R.string.crash_email_subject)
							.toString());
			result.putString(RES_EMAIL_TEXT,
					getResources().getText(R.string.crash_email_text)
							.toString());
			result.putString(RES_DIALOG_TITLE,
					getResources().getText(R.string.crash_dialog_title)
							.toString());
			result.putString(RES_DIALOG_TEXT,
					getResources().getText(R.string.crash_dialog_text)
							.toString());
			result.putString(RES_BUTTON_REPORT,
					getResources().getText(R.string.crash_report).toString());
			result.putString(RES_BUTTON_CANCEL,
					getResources().getText(R.string.crash_exit).toString());
			// Hide restart button
			result.putString(RES_BUTTON_RESTART, "");
			result.putInt(RES_DIALOG_ICON, R.drawable.ic_launcher);
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
		return result;
	}

	// 计算用户统计的总运行时间
	private int getTotalTime(long ms) {
		int totalTime = (ms % 60000 == 0) ? (int) (ms / 60000)
				: (int) (ms / 60000 + 1);

		return totalTime;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.archermind.callstat.CrashReportingApplication#getReportUrl()
	 */
	@Override
	public String getReportUrl() {
		// TODO
		return getResources().getText(R.string.report_url).toString();
	}

	public static ConfigManager getConfigManager() {
		return configManager;
	}

	public static String[] getPhoneNum() {
		return phoneNum;
	}

	public static String[] getCallMsg() {
		return callMsg;
	}

	public static String[] getTrafficMsg() {
		return trafficMsg;
	}

	public static String[] getPackageMsg() {
		return packageMsg;
	}

	// 将用户月消费行为统计原始数据发往服务器 added by zhangjing@archermind.com
	// 具体发往服务器的周期和方式尚需进一步讨论确认
	public int sendMonthlyStatSrcToServer(MonthlyStatDataSource monthly_stat_rec) {
		final String imei = DeviceInformation.getInformation(InfoName.IMEI);
		final String versionString = configManager.getVersionName();
		try {
			String url = getResources().getString(R.string.user_process_url); // url的取值还需要进一步确认
																				// zhangjing@archermind.com
			Map<String, String> map = new HashMap<String, String>();
			map.put("imei", imei);
			map.put("time", monthly_stat_rec.getTime());
			map.put("province", monthly_stat_rec.getProvince());
			map.put("city", monthly_stat_rec.getCity());
			map.put("mno", monthly_stat_rec.getMno());
			map.put("brand", monthly_stat_rec.getBrand());
			map.put("type", monthly_stat_rec.getType() + "");
			map.put("number", monthly_stat_rec.getNumber());
			map.put("name", monthly_stat_rec.getName());
			map.put("duration", monthly_stat_rec.getDuration() + "");
			map.put("usedGprs_toLastEvent",
					monthly_stat_rec.getUsedGprsToLastEvent() + "");
			map.put("usedFee_toLastEvent",
					monthly_stat_rec.getUsedFeeToLastEvent() + "");
			map.put("rates_for_local", monthly_stat_rec.getRateForLocal() + "");
			map.put("rates_for_long_distance",
					monthly_stat_rec.getRatesForLong() + "");
			map.put("rates_for_roaming", monthly_stat_rec.getRatesForRoaming()
					+ "");
			map.put("rates_for_ip", monthly_stat_rec.getRatesForIP() + "");
			map.put("rates_for_short", monthly_stat_rec.getRatesForShort() + "");
			map.put("rates_for_traffic", monthly_stat_rec.getRatesForTraffic()
					+ "");
			map.put("wlan_used_to_last_event",
					monthly_stat_rec.getWlanUsedToLastEvent() + "");
			map.put("rates_for_wlan", monthly_stat_rec.getRatesForSms() + "");
			map.put("version", versionString);

			if (url == null) {
				return -1;
			}
			HttpPost myRequest = new HttpPost(url);
			UrlEncodedFormEntity myEntity = MyHttpPostHelper
					.buildUrlEncodedFormEntity(map, null);
			myRequest.setEntity(myEntity);
			DefaultHttpClient myDefaultHttpClient = new DefaultHttpClient();
			HttpResponse myResponse = myDefaultHttpClient.execute(myRequest);

			int status = myResponse.getStatusLine().getStatusCode();
			return status;
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
			return -1;
		}
	}
}
