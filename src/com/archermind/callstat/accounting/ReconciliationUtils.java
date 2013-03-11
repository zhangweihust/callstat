package com.archermind.callstat.accounting;

import java.util.HashSet;
import java.util.Set;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.SmsManager;

import com.archermind.callstat.CallStatApplication;
import com.archermind.callstat.ConfigManager;
import com.archermind.callstat.ILog;
import com.archermind.callstat.SmsReceiver;
import com.archermind.callstat.common.CallStatUtils;
import com.archermind.callstat.service.CallStatSMSService;

public class ReconciliationUtils {
	public final static int RECONCILIATION_RUNNING = 1;
	public final static int RECONCILIATION_STOP = -1;
	public final static int FLYING_MODE = 0;

	public static final int PERMINENT_15_MINUTES = 5 * 60000;
	public static final int PERMINENT_3_MINUTES = 2 * 60000;
	public static final int PERMINENT_1_MINUTES = 1 * 60000;

	/** 话费对账15分钟固定拦截超时操作 */
	public static final int INTERCEPT_TIME_OUT_CALLS = 0x3001;

	/** 流量对账15分钟固定拦截超时操作 */
	public static final int INTERCEPT_TIME_OUT_TRAFFIC = 0x3002;

	/** 话费对账超过3分钟提示 */
	public static final int THREE_MINUTES_TOAST_TIME_OUT_CALLS = 0x3003;

	/** 流量对账超过3分钟提示 */
	public static final int THREE_MINUTES_TOAST_TIME_OUT_TRAFFIC = 0x3004;

	/** 话费对账更改指令 */
	public static final int MODIFY_INSTRUCTION_CALLS = 0x3005;

	/** 流量对账更改指令 */
	public static final int MODIFY_INSTRUCTION_TRAFFIC = 0x3006;

	/** 15分钟固定拦截短信超时操作 */
	public static final int INTERCEPT_TIME_OUT = 0x3007;

	/** 1分钟取消拦截短信超时操作 */
	public static final int UNBLOCK_TIME_OUT = 0x3008;

	/** 全对账超过3分钟提示 */
	public static final String PERMANENT_3_MINUTES_ACCOUNTING_FAILED_TIME_OUT_ACTION = "PERMANENT_3_MINUTES_ACCOUNTING_FAILED_TIME_OUT_ACTION";

	/** 话费对账超过3分钟提示 */
	public static final String PERMANENT_3_MINUTES_ACCOUNTING_FAILED_TIME_OUT_ACTION_CALLS = "PERMANENT_3_MINUTES_ACCOUNTING_FAILED_TIME_OUT_ACTION_CALLS";
	/** 流量对账超过3分钟提示 */
	public static final String PERMANENT_3_MINUTES_ACCOUNTING_FAILED_TIME_OUT_ACTION_TRAFFIC = "PERMANENT_3_MINUTES_ACCOUNTING_FAILED_TIME_OUT_ACTION_TRAFFIC";

	/** 通知话费对账失败 */
	public static final String NOTICE_ACTIVITY_ACOUNTING_FAILED_ACTION_CALLS = "NOTICE_ACTIVITY_ACOUNTING_FAILED_ACTION_CALLS";

	/** 通知话费对账成功 */
	public static final String NOTICE_ACTIVITY_ACOUNTING_SUCCESS_ACTION_CALLS = "NOTICE_ACTIVITY_ACOUNTING_SUCCESS_ACTION_CALLS";

	/** 通知流量对账失败 */
	public static final String NOTICE_ACTIVITY_ACOUNTING_FAILED_ACTION_TRAFFIC = "NOTICE_ACTIVITY_ACOUNTING_FAILED_ACTION_TRAFFIC";

	/** 通知流量对账成功 */
	public static final String NOTICE_ACTIVITY_ACOUNTING_SUCCESS_ACTION_TRAFFIC = "NOTICE_ACTIVITY_ACOUNTING_SUCCESS_ACTION_TRAFFIC";

	/** 开始通知话费对账 */
	public static final String NOTICE_START_ACCOUNT_CALLS = "NOTICE_START_ACCOUNT_CALLS";

	/** 开始通知流量对账 */
	public static final String NOTICE_START_ACCOUNT_TRAFFIC = "NOTICE_START_ACCOUNT_TRAFFIC";

	/** 开始通知全对账 */
	public static final String NOTICE_START_ACCOUNT = "NOTICE_START_ACCOUNT";

	/** 话费查询 */
	public static final int CALL_CHARGES_ACTION = 0x2001;
	/** 流量查询 */
	public static final int TRAFFIC_QUERY_ACTION = 0x2002;
	/** 套餐查询 */
	public static final int PACKAGE_MARGIN_ACTION = 0x2003;

	/** 进行话费查询操作 */
	public static final int SEND_CALL_CHARGES = 0x1001;
	/** 进行流量查询操作 */
	public static final int SEND_TRAFFIC_QUERY = 0x1002;
	/** 进行全对帐查询操作 */
	public static final int SEND_QUERY = 0x1003;

	/** 正在进行话费查询操作 */
	public static boolean IsCheckingAccount = false;
	/** 进行话费查询操作3分钟 */
	public static boolean IsCheckingAccount3MIn = false;
	/** 正在进行流量查询操作 */
	public static boolean IsCheckingTraffic = false;
	/** 进行流量查询操作3分钟 */
	public static boolean IsCheckingTraffic3Min = false;
	/** 话费查询操作成功 */
	public static boolean IsCheckingAccountSuccess = false;
	/** 流量查询操作成功 */
	public static boolean IsCheckingTrafficSuccess = false;
	/** 用户修改了话费查询指令操作 */
	public static boolean IsModifyAccount = false;
	/** 用户修改了流量查询指令操作 */
	public static boolean IsModifyTraffic = false;
	/** 短信15分钟拦截操作 */
	public static boolean Is15MinInterceptSms = false;
	/** 一分钟拦截机制 */
	public static boolean Is1MinInterceptSms = false;

	/** 用来记录话费查询进行了几轮，最多不能超过3轮 */
	public static int callsReconiliationCount = 0;
	/** 用来记录流量查询进行了几轮，最多不能超过3轮 */
	public static int trafficReconiliationCount = 0;
	/** 用户修改后的查询话费的指令内容 */
	public static String msgAccountYe = null;
	public static String msgAccountUsed = null;
	/** 用户修改后的查询流量的指令内容 */
	public static String msgTrafficYe = null;
	public static String msgTrafficUsed = null;
	public Set<String> modifyTrafficMsg = new HashSet<String>();
	public Set<String> modifyCallMsg = new HashSet<String>();

	private static ReconciliationUtils instance = new ReconciliationUtils();
	private SmsManager smsManager;
	private Handler handler;
	private ConfigManager config;

	private ReconciliationUtils() {
		smsManager = SmsManager.getDefault();
		config = new ConfigManager(CallStatApplication.getInstance());
		handler = new Handler() {
			Bundle bundle;

			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				switch (msg.what) {
				case THREE_MINUTES_TOAST_TIME_OUT_CALLS:
					ILog.LogD(getClass(), "3分钟过去了话费对帐没有成功");
					IsCheckingAccount3MIn = true;
					Intent intent_calls = new Intent();
					intent_calls
							.setAction(PERMANENT_3_MINUTES_ACCOUNTING_FAILED_TIME_OUT_ACTION_CALLS);
					CallStatApplication.getCallstatsContext().sendBroadcast(
							intent_calls);
					break;
				case THREE_MINUTES_TOAST_TIME_OUT_TRAFFIC:
					ILog.LogD(getClass(), "3分钟过去了流量对帐没有成功");
					IsCheckingTraffic3Min = true;
					Intent intent_traffic = new Intent();
					intent_traffic
							.setAction(PERMANENT_3_MINUTES_ACCOUNTING_FAILED_TIME_OUT_ACTION_TRAFFIC);
					CallStatApplication.getCallstatsContext().sendBroadcast(
							intent_traffic);
					break;
				case INTERCEPT_TIME_OUT_CALLS:
					if (callsReconiliationCount < 1) {
						IsCheckingAccount = false;
						IsCheckingAccount3MIn = false;
						if (IsModifyAccount) {// 修改指令后用新指令来发送对帐信息
							callsReconiliationCount++;
							ILog.LogD(getClass(), "修改了指令，15分钟过去了话费对帐没有成功:"
									+ callsReconiliationCount);
							queryTelephoneBill();
						} else {
							callsReconiliationCount++;
							ILog.LogD(getClass(), "15分钟过去了话费对帐没有成功:"
									+ callsReconiliationCount);
							queryTelephoneBill();
						}
					} else {// 三次对帐尝试都失败了
						resetVariable(SEND_CALL_CHARGES);
						ILog.LogD(getClass(), "3次15分钟过去了话费对帐没有成功:"
								+ callsReconiliationCount);
						CallStatApplication.calls_anim_is_run = false;
						if (SmsReceiver.MessageList.size() != 0) {
							CallStatApplication.getInstance()
									.reconciliationFailed(
											SmsReceiver.MessageList);
							SmsReceiver.MessageList.clear();
						}
					}
					break;
				case INTERCEPT_TIME_OUT_TRAFFIC:
					if (trafficReconiliationCount < 1) {
						IsCheckingTraffic = false;
						IsCheckingTraffic3Min = false;
						if (IsModifyTraffic) {// 修改指令后用新指令来发送对帐信息
							trafficReconiliationCount++;
							ILog.LogD(getClass(), "修改了指令，15分钟过去了流量对帐没有成功:"
									+ trafficReconiliationCount);
							queryGprsTraffic();
						} else {
							trafficReconiliationCount++;
							ILog.LogD(getClass(), "15分钟过去了流量对帐没有成功:"
									+ trafficReconiliationCount);
							queryGprsTraffic();
						}
					} else {// 三次对帐尝试都失败了
						resetVariable(SEND_TRAFFIC_QUERY);
						ILog.LogD(getClass(), "3次15分钟过去了流量对帐没有成功:"
								+ trafficReconiliationCount);
						CallStatApplication.traffic_anim_is_run = false;
						if (SmsReceiver.MessageList.size() != 0) {
							CallStatApplication.getInstance()
									.reconciliationFailed(
											SmsReceiver.MessageList);
							SmsReceiver.MessageList.clear();
						}
					}
					break;
				case MODIFY_INSTRUCTION_CALLS:
					ILog.LogD(getClass(), "MODIFY_INSTRUCTION_CALLS");
					IsModifyAccount = true;
					callsReconiliationCount = -1;
					handler.removeMessages(INTERCEPT_TIME_OUT_CALLS);
					handler.sendEmptyMessage(INTERCEPT_TIME_OUT_CALLS);
					break;
				case MODIFY_INSTRUCTION_TRAFFIC:
					ILog.LogD(getClass(), "MODIFY_INSTRUCTION_TRAFFIC");
					IsModifyTraffic = true;
					trafficReconiliationCount = -1;
					handler.removeMessages(INTERCEPT_TIME_OUT_TRAFFIC);
					handler.sendEmptyMessage(INTERCEPT_TIME_OUT_TRAFFIC);
					break;
				case INTERCEPT_TIME_OUT:
					Is15MinInterceptSms = false;
					break;
				case UNBLOCK_TIME_OUT:
					Is1MinInterceptSms = false;
					break;
				}
			}
		};
	}

	public static ReconciliationUtils getInstance() {
		return instance;
	}

	public int queryReconciliation() {// 全对帐
		if (!CallStatUtils.isFlyingMode(CallStatApplication
				.getCallstatsContext())) {// 判断是否是飞行模式
			ILog.LogE(this.getClass(), "开始全对账操作——————");
			Intent intent = new Intent(
					CallStatApplication.getCallstatsContext(),
					CallStatSMSService.class);
			intent.setAction(CallStatSMSService.ACCOUNT_SUCCESS);
			CallStatApplication.getCallstatsContext().startService(intent);

			queryTelephoneBill();
			queryGprsTraffic();
			return RECONCILIATION_RUNNING;
		} else {
			return FLYING_MODE;
		}

	}

	// public int queryModifyTelephoneBill() {// 用户主动更改指令以后调用修改的指令发送话费查询
	// if (!CallStatUtils.isFlyingMode(CallStatApplication
	// .getCallstatsContext())) {// 判断是否是飞行模式
	// if (!IsCheckingAccount) {
	// Is15MinInterceptSms = true;
	// IsCheckingAccount = true;
	//
	// // 发送广播通知UI，开启动画
	// Intent intent = new Intent();
	// intent.setAction(NOTICE_START_ACCOUNT_CALLS);
	// CallStatApplication.getCallstatsContext().sendBroadcast(intent);
	//
	// IsCheckingAccountSuccess = false;
	// for (String msg : modifyCallMsg) {
	// ILog.LogD(this.getClass(), "发送短信msg=" + msg);
	// smsManager.sendTextMessage(config.getOperatorNum(), null,
	// msg, null, null);
	// }
	// handler.removeMessages(THREE_MINUTES_TOAST_TIME_OUT_CALLS);
	// handler.removeMessages(INTERCEPT_TIME_OUT_CALLS);
	// handler.sendEmptyMessageDelayed(
	// THREE_MINUTES_TOAST_TIME_OUT_CALLS, PERMINENT_3_MINUTES); //
	// 3分钟之后发送对账超过3分钟提示
	// handler.sendEmptyMessageDelayed(INTERCEPT_TIME_OUT_CALLS,
	// PERMINENT_15_MINUTES); // 15分钟之后发送对账超过15分钟提示
	// handler.removeMessages(INTERCEPT_TIME_OUT);
	// handler.sendEmptyMessageDelayed(INTERCEPT_TIME_OUT,
	// PERMINENT_15_MINUTES); // 15分钟后取消短信拦截
	// return RECONCILIATION_RUNNING;
	// }
	// return RECONCILIATION_STOP;
	// } else {
	// return FLYING_MODE;
	// }
	// }

	public int queryTelephoneBill() {
		if (!CallStatUtils.isFlyingMode(CallStatApplication
				.getCallstatsContext())) {// 判断是否是飞行模式
			if (!IsCheckingAccount) {
				Is15MinInterceptSms = true;
				IsCheckingAccount = true;
				IsCheckingAccount3MIn = false;
				// 发送广播通知UI，开启动画
				Intent intent = new Intent();
				intent.setAction(NOTICE_START_ACCOUNT_CALLS);
				CallStatApplication.getCallstatsContext().sendBroadcast(intent);

				IsCheckingAccountSuccess = false;
				sendMessage(CALL_CHARGES_ACTION);
				handler.removeMessages(THREE_MINUTES_TOAST_TIME_OUT_CALLS);
				handler.removeMessages(INTERCEPT_TIME_OUT_CALLS);
				handler.sendEmptyMessageDelayed(
						THREE_MINUTES_TOAST_TIME_OUT_CALLS, PERMINENT_3_MINUTES); // 3分钟之后发送对账超过3分钟提示
				handler.sendEmptyMessageDelayed(INTERCEPT_TIME_OUT_CALLS,
						PERMINENT_15_MINUTES); // 15分钟之后发送对账超过15分钟提示
				handler.removeMessages(INTERCEPT_TIME_OUT);
				handler.sendEmptyMessageDelayed(INTERCEPT_TIME_OUT,
						PERMINENT_15_MINUTES); // 15分钟后取消短信拦截
				return RECONCILIATION_RUNNING;
			}
			return RECONCILIATION_STOP;
		} else {
			return FLYING_MODE;
		}
	}

	// public int queryModifyGprsTraffic() {// 用户主动更改指令以后调用修改的指令发送流量查询
	// if (!CallStatUtils.isFlyingMode(CallStatApplication
	// .getCallstatsContext())) {// 判断是否是飞行模式
	// if (!IsCheckingTraffic) {
	// Is15MinInterceptSms = true;
	// IsCheckingTraffic = true;
	//
	// // 发送广播通知UI，开启动画
	// Intent intent = new Intent();
	// intent.setAction(NOTICE_START_ACCOUNT_TRAFFIC);
	// CallStatApplication.getCallstatsContext().sendBroadcast(intent);
	//
	// IsCheckingTrafficSuccess = false;
	//
	// for (String msg : modifyTrafficMsg) {
	// ILog.LogD(this.getClass(), "发送短信msg=" + msg);
	// smsManager.sendTextMessage(config.getOperatorNum(), null,
	// msg, null, null);
	// }
	// handler.removeMessages(THREE_MINUTES_TOAST_TIME_OUT_TRAFFIC);
	// handler.removeMessages(INTERCEPT_TIME_OUT_TRAFFIC);
	// handler.sendEmptyMessageDelayed(
	// THREE_MINUTES_TOAST_TIME_OUT_TRAFFIC,
	// PERMINENT_3_MINUTES); // 3分钟之后发送对账超过3分钟提示
	// handler.sendEmptyMessageDelayed(INTERCEPT_TIME_OUT_TRAFFIC,
	// PERMINENT_15_MINUTES); // 15分钟之后发送对账超过15分钟提示
	// handler.removeMessages(INTERCEPT_TIME_OUT);
	// handler.sendEmptyMessageDelayed(INTERCEPT_TIME_OUT,
	// PERMINENT_15_MINUTES); // 15分钟后取消短信拦截
	// return RECONCILIATION_RUNNING;
	// }
	// return RECONCILIATION_STOP;
	// } else {
	// return FLYING_MODE;
	// }
	// }

	public int queryGprsTraffic() {
		if (!CallStatUtils.isFlyingMode(CallStatApplication
				.getCallstatsContext())) {// 判断是否是飞行模式
			if (!IsCheckingTraffic) {
				Is15MinInterceptSms = true;
				IsCheckingTraffic = true;
				IsCheckingTraffic3Min = false;
				// 发送广播通知UI，开启动画
				Intent intent = new Intent();
				intent.setAction(NOTICE_START_ACCOUNT_TRAFFIC);
				CallStatApplication.getCallstatsContext().sendBroadcast(intent);

				IsCheckingTrafficSuccess = false;
				sendMessage(TRAFFIC_QUERY_ACTION);
				sendMessage(PACKAGE_MARGIN_ACTION);
				handler.removeMessages(THREE_MINUTES_TOAST_TIME_OUT_TRAFFIC);
				handler.removeMessages(INTERCEPT_TIME_OUT_TRAFFIC);
				handler.sendEmptyMessageDelayed(
						THREE_MINUTES_TOAST_TIME_OUT_TRAFFIC,
						PERMINENT_3_MINUTES); // 3分钟之后发送对账超过3分钟提示
				handler.sendEmptyMessageDelayed(INTERCEPT_TIME_OUT_TRAFFIC,
						PERMINENT_15_MINUTES); // 15分钟之后发送对账超过15分钟提示
				handler.removeMessages(INTERCEPT_TIME_OUT);
				handler.sendEmptyMessageDelayed(INTERCEPT_TIME_OUT,
						PERMINENT_15_MINUTES); // 15分钟后取消短信拦截
				return RECONCILIATION_RUNNING;
			}
			return RECONCILIATION_STOP;
		} else {
			return FLYING_MODE;
		}
	}

	public void resetVariable(int type) {
		if (type == SEND_CALL_CHARGES) {
			IsModifyAccount = false;
			callsReconiliationCount = 0;
			IsCheckingAccount = false;
			IsCheckingAccount3MIn = false;
			IsCheckingAccountSuccess = false;
			CallStatApplication.calls_anim_is_run = false;

			// 刷新通知栏
			Intent serviceintent = new Intent(
					CallStatApplication.getCallstatsContext(),
					CallStatSMSService.class);
			serviceintent.setAction(CallStatSMSService.ACCOUNT_SUCCESS);
			CallStatApplication.getCallstatsContext().startService(
					serviceintent);

			handler.removeMessages(THREE_MINUTES_TOAST_TIME_OUT_CALLS);
			handler.removeMessages(INTERCEPT_TIME_OUT_CALLS);
			ILog.LogD(this.getClass(), "话费查询变量重置");
		} else if (type == SEND_TRAFFIC_QUERY) {
			IsModifyTraffic = false;
			trafficReconiliationCount = 0;
			IsCheckingTraffic = false;
			IsCheckingTraffic3Min = false;
			IsCheckingTrafficSuccess = false;
			CallStatApplication.traffic_anim_is_run = false;

			// 刷新通知栏
			Intent serviceintent2 = new Intent(
					CallStatApplication.getCallstatsContext(),
					CallStatSMSService.class);
			serviceintent2.setAction(CallStatSMSService.ACCOUNT_SUCCESS);
			CallStatApplication.getCallstatsContext().startService(
					serviceintent2);

			handler.removeMessages(THREE_MINUTES_TOAST_TIME_OUT_TRAFFIC);
			handler.removeMessages(INTERCEPT_TIME_OUT_TRAFFIC);
			ILog.LogD(this.getClass(), "流量查询变量重置");
		}
	}

	public boolean sendMessage(int action) {
		String num = config.getOperatorNum();
		Set<String> msg = new HashSet<String>();
		switch (action) {
		case CALL_CHARGES_ACTION:
			msg.add(config.getHFUsedCode());
			msg.add(config.getHFYeCode());
			ILog.LogD(this.getClass(), "action CALL_CHARGES_ACTION");
			break;
		case TRAFFIC_QUERY_ACTION:
			msg.add(config.getGprsUsedCode());
			msg.add(config.getGprsYeCode());
			ILog.LogD(this.getClass(), "action TRAFFIC_QUERY_ACTION");
			break;
		case PACKAGE_MARGIN_ACTION:
			msg.add(config.getPackageCode());
			ILog.LogD(this.getClass(), "action PACKAGE_MARGIN_ACTION");
			break;
		}
		ILog.LogD(this.getClass(), "num = " + num
				+ "  ------------------------msg = " + msg);
		if (num != null && msg != null) {
			try {
				for (String s : msg) {
					if (s != null && !num.equals("") && !s.equals("")
							&& !num.equals("-1") && !s.equals("-1")) {
						ILog.LogD(this.getClass(), "发送短信num=" + num + ":msg="
								+ s);
						smsManager.sendTextMessage(num, null, s, null, null);
					}
				}
			} catch (Exception e) {
				return false;
			}
		} else {
			return false;
		}
		return true;
	}

	public Handler getHandler() {
		return handler;
	}

}
