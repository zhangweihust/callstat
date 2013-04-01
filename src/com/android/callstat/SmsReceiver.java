package com.android.callstat;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsMessage;
import android.widget.Toast;

import com.android.callstat.accounting.MessageManager;
import com.android.callstat.accounting.ReconciliationUtils;
import com.android.callstat.common.CallStatUtils;
import com.android.callstat.common.PhoneBillCaculateUtils;
import com.android.callstat.home.views.ToastFactory;
import com.android.callstat.service.CallStatSMSService;

public class SmsReceiver extends BroadcastReceiver {
	public static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";
	public static final int ACCOUNTING_FROM_RECEIVER = 0;
	public static final int ACCOUNTING_FROM_INBOX = 1;
	public static final String HF_TYPE = "0";
	public static final String LL_TYPE = "1";
	public static HashMap<Integer, String> accouting_infoMap = null;
	public static ArrayList<String> MessageList = new ArrayList<String>();// 对帐没有成功的短信队列
	String comingNumber;
	static StringBuilder content;
	ConfigManager config;
	private static Context mCtx;

	// private static UploadNewAccoutingCodeThread uploadTask = new
	// UploadNewAccoutingCodeThread();

	@Override
	public void onReceive(Context context, Intent intent) {
		content = new StringBuilder();
		config = new ConfigManager(context);
		mCtx = context;
		if (intent.getAction().equals(SMS_RECEIVED_ACTION)) {
			Object[] pdus = (Object[]) intent.getExtras().get("pdus");
			for (Object p : pdus) {
				byte[] sms = (byte[]) p;
				SmsMessage message = SmsMessage.createFromPdu(sms);
				// 获取短信内容
				content.append(message.getMessageBody());
				// 获取发件人号码
				comingNumber = message.getOriginatingAddress();
			}
			if (comingNumber.startsWith("+86")) {
				comingNumber = comingNumber.substring(3);
			}
			// 将号码放到配置文件,是我们的验证号码则提取关键字
			String[] parts = content.toString().split(":");
			String expr = "^[0-9]+$";
			if (parts.length == 2 && parts[0].contains("话费管家")) {
				StringBuilder sb = new StringBuilder();
				char[] chars = parts[1].toCharArray();
				for (char var : chars) {
					if (String.valueOf(var).matches(expr)) {
						sb.append(var);
					} else {
						break;
					}
				}
				String verifyCode = sb.toString();
				abortBroadcast();
				Intent myIntent = new Intent(context, CallStatSMSService.class);
				myIntent.setAction(CallStatSMSService.ACTION_VERIFICATION_CODE);
				myIntent.putExtra("ACTION_VERIFICATION_CODE", verifyCode);
				context.startService(myIntent);
			}

			ILog.LogD(SmsReceiver.class, "短信内容:" + content + "  发送号码:"
					+ comingNumber);
			if (comingNumber.equals(config.getOperatorNum())) {
				ILog.LogE(getClass(),
						"getOperatorNum:" + config.getOperatorNum());
				// 终止广播
				if (ReconciliationUtils.Is15MinInterceptSms
						|| ReconciliationUtils.Is1MinInterceptSms) {
					abortBroadcast();
				} else {// 超过15分钟拦截，如果收到运营商的短信，不拦截第一条，重新设置1分钟拦截
					ReconciliationUtils.Is1MinInterceptSms = true;
					ReconciliationUtils
							.getInstance()
							.getHandler()
							.removeMessages(
									ReconciliationUtils.UNBLOCK_TIME_OUT);
					ReconciliationUtils
							.getInstance()
							.getHandler()
							.sendEmptyMessageDelayed(
									ReconciliationUtils.UNBLOCK_TIME_OUT,
									ReconciliationUtils.PERMINENT_1_MINUTES); // 1分钟后取消短信拦截
					return;
				}
				ILog.LogD(SmsReceiver.class, "短信内容被拦截，发送号码:" + comingNumber);
				new Thread(new Runnable() {
					@Override
					public void run() {
						accouting_infoMap = MessageManager.accounting_proc(
								content.toString(),
								CallStatApplication.getConfigManager(),
								CallStatApplication.Keywordslist,
								ACCOUNTING_FROM_RECEIVER);
						sendAccountInfo2Ui(accouting_infoMap);
					}
				}).start();
			}
		}
	}

	public static void sendAccountInfo2Ui(
			HashMap<Integer, String> accouting_infoMap) {
		if (accouting_infoMap.size() != 0) {
			ILog.LogD(SmsReceiver.class, "Receiver匹配成功向service发消息");
		} else {
			MessageList.add(content.toString());
		}
		boolean flag_call = false;
		boolean flag_traffic = false;
		for (int i = 0; i < CallStatApplication.Keywordslist.size(); i++) {
			if (accouting_infoMap.get(i) != null) {
				if (i == MessageManager.TYPE_AVAIL_FEE
						|| i == MessageManager.TYPE_CONSUME_FEE) {
					
					CallStatApplication.getConfigManager()
					.setLastCheckHasYeTime(System.currentTimeMillis());
					
					if (ReconciliationUtils.IsModifyAccount) {// 对帐成功修改本地指令库
						ILog.LogD(SmsReceiver.class, "话费对帐成功修改本地指令库");
						Intent intent = new Intent();
						intent.setAction(CallStatSMSService.UPLOAD_ACC_CODE_ACTION);
						intent.putExtra("type", HF_TYPE);
						mCtx.sendBroadcast(intent);
					}
					if (CallStatApplication.calls_anim_is_run) {
						flag_call = true;
					}
					if (i == MessageManager.TYPE_AVAIL_FEE
							&& flag_call
							&& CallStatUtils.isMyAppOnDesk(CallStatApplication
									.getCallstatsContext())) {
						ReconciliationUtils.getInstance().getHandler()
								.post(new Runnable() {
									@Override
									public void run() {
										ToastFactory.getToast(
												CallStatApplication
														.getCallstatsContext(),
												"话费校正成功", Toast.LENGTH_SHORT)
												.show();
									}

								});
					}

					ReconciliationUtils.getInstance().resetVariable(
							ReconciliationUtils.SEND_CALL_CHARGES);// 重置变量
				} else {
					if (ReconciliationUtils.IsModifyTraffic) {// 对帐成功修改本地指令库
						ILog.LogD(SmsReceiver.class, "流量对帐成功修改本地指令库");
						Intent intent = new Intent();
						intent.setAction(CallStatSMSService.UPLOAD_ACC_CODE_ACTION);
						intent.putExtra("type", LL_TYPE);
						mCtx.sendBroadcast(intent);
					}
					if (CallStatApplication.traffic_anim_is_run) {
						flag_traffic = true;
					}
					if (i == MessageManager.TYPE_CONSUME_TRAFFIC
							&& flag_traffic
							&& CallStatUtils.isMyAppOnDesk(CallStatApplication
									.getCallstatsContext())) {
						ReconciliationUtils.getInstance().getHandler()
								.post(new Runnable() {
									@Override
									public void run() {
										ToastFactory.getToast(
												CallStatApplication
														.getCallstatsContext(),
												"流量校正成功", Toast.LENGTH_SHORT)
												.show();
									}

								});
					}

					ReconciliationUtils.getInstance().resetVariable(
							ReconciliationUtils.SEND_TRAFFIC_QUERY);// 重置变量
				}
				Intent intent = null;
				switch (i) {
				case MessageManager.TYPE_AVAIL_FEE:
					ILog.LogD(SmsReceiver.class,
							"剩余话费 ---" + accouting_infoMap.get(i));
					float feeavail = Float.parseFloat(accouting_infoMap.get(i));
					intent = new Intent();
					intent.putExtra("index", MessageManager.TYPE_AVAIL_FEE);
					intent.setAction(ReconciliationUtils.NOTICE_ACTIVITY_ACOUNTING_SUCCESS_ACTION_CALLS);
					CallStatApplication.getInstance().sendBroadcast(intent);
					PhoneBillCaculateUtils.addEquationToDatabase(
							CallStatApplication.getConfigManager(),
							Float.parseFloat(accouting_infoMap.get(i)));
					
					//modified by zhangjing@archermind.com
					if (feeavail >= 0) {
						ConfigManager config = CallStatApplication.getConfigManager();
						config.setCalculateFeeAvailable(feeavail);
						config.setFeesRemain(feeavail);
						config.setPrevReconcilitionGprsUsed(config.getTotalGprsUsed());
						config.setPrevReconcilitionSendSms(config.getTotalSmsSent());
					}
					break;
				case MessageManager.TYPE_CONSUME_FEE:
					ILog.LogD(SmsReceiver.class, "已经使用话费 ---"
							+ accouting_infoMap.get(i));
					float feespent = Float.parseFloat(accouting_infoMap.get(i));
					if (feespent >= 0) {
						CallStatApplication.getConfigManager().setFeeSpent(
								feespent);
					}
					intent = new Intent();
					intent.putExtra("index", MessageManager.TYPE_CONSUME_FEE);
					intent.setAction(ReconciliationUtils.NOTICE_ACTIVITY_ACOUNTING_SUCCESS_ACTION_CALLS);
					CallStatApplication.getInstance().sendBroadcast(intent);
					break;
				case MessageManager.TYPE_AVAIL_TRAFFIC:
					ILog.LogD(SmsReceiver.class,
							"剩余流量 ---" + accouting_infoMap.get(i));
					long leftGprs = change2Long(accouting_infoMap.get(i));
					if (leftGprs >= 0) {
						CallStatApplication.getConfigManager()
								.setTotalGprsMargin(leftGprs);
					}
					intent = new Intent();
					intent.putExtra("index", MessageManager.TYPE_AVAIL_TRAFFIC);
					intent.setAction(ReconciliationUtils.NOTICE_ACTIVITY_ACOUNTING_SUCCESS_ACTION_TRAFFIC);
					CallStatApplication.getInstance().sendBroadcast(intent);
					break;
				case MessageManager.TYPE_CONSUME_TRAFFIC:
					ILog.LogD(SmsReceiver.class, "已经使用流量 ---"
							+ accouting_infoMap.get(i));
					long usedGprs = change2Long(accouting_infoMap.get(i));
					if (usedGprs >= 0) {
						long difference = (long) usedGprs
								- CallStatApplication.getConfigManager()
										.getTotalGprsUsed();
						CallStatApplication.getConfigManager()
								.setTotalGprsUsedDifference(difference);
					}
					CallStatApplication.getConfigManager()
							.setLastCheckHasTrafficTime(
									System.currentTimeMillis());
					intent = new Intent();
					intent.putExtra("index",
							MessageManager.TYPE_CONSUME_TRAFFIC);
					intent.setAction(ReconciliationUtils.NOTICE_ACTIVITY_ACOUNTING_SUCCESS_ACTION_TRAFFIC);
					CallStatApplication.getInstance().sendBroadcast(intent);
					break;
				}
			} else {
				ILog.LogD(SmsReceiver.class, "短信收到但没有匹配成功--" + i);
			}
		}
		flag_call = false;
		flag_traffic = false;
	}

	private static long change2Long(String data) {
		data = data.replace(" ", "");
		double pure_data = Double.parseDouble(data.substring(0,
				data.length() - 1));
		if (data.contains("K") || data.contains("k")) {
			pure_data = pure_data * 1024;
		} else if (data.contains("M") || data.contains("m")) {
			pure_data = pure_data * 1024 * 1024;
		} else if (data.contains("G") || data.contains("g")) {
			pure_data = pure_data * 1024 * 1024 * 1024;
		} else if (data.contains("T") || data.contains("t")) {
			pure_data = pure_data * 1024 * 1024 * 1024 * 1024;
		}
		return (long) pure_data;
	}
}
