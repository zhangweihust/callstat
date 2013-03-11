package com.archermind.callstat.accounting;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;

import com.archermind.callstat.ILog;
import com.archermind.callstat.service.CallStatSMSService;

public class SendMessage {
	private CallStatSMSService context;

	public SendMessage(CallStatSMSService context) {
		super();
		// TODO Auto-generated constructor stub
		this.context = context;
	}

	/**
	 * 发送对账短信
	 * 
	 * @param num
	 *            短信号码组
	 * @param msg
	 *            指令组
	 * @param action
	 *            对账短信类型
	 * @param keyWord
	 *            关键字组
	 * @return 短信发送是否成功额
	 */
	public boolean sendMessage(String num[], String msg[], String action,
			String keyWord[]) {
		ILog.LogI(this.getClass(), "is sendMessage");
		SmsManager smsManager = SmsManager.getDefault();
		/* 创建自定义Action常数的Intent(给PendingIntent参数之用) */
		Intent itSend = new Intent(action);
		Bundle b = new Bundle();
		b.putStringArray("keyWord", keyWord);
		b.putStringArray("phoneNum", num);
		itSend.putExtras(b);
		;
		/* sentIntent参数为传送后接受的广播信息PendingIntent */
		PendingIntent mSendPI = PendingIntent.getBroadcast(
				context.getApplicationContext(),
				(int) System.currentTimeMillis(), itSend,
				PendingIntent.FLAG_UPDATE_CURRENT);
		try {
			for (int i = 0; i < num.length; i++) {
				for (int j = 0; j < msg.length; j++) {
					if (num[i] != null && msg[j] != null && !num[i].equals("")
							&& !msg[j].equals("") && !num[i].equals("-1")
							&& !msg[j].equals("-1")) {
						ILog.LogE(this.getClass(), "num=" + num[i] + ":msg="
								+ msg[j]);
						if (i == 0 && j == 0) {
							smsManager.sendTextMessage(num[i], null, msg[j],
									mSendPI, null);
						} else {
							Intent intent = new Intent(action);
							PendingIntent mSendPITemp = PendingIntent
									.getBroadcast(
											context.getApplicationContext(),
											(int) System.currentTimeMillis(),
											intent,
											PendingIntent.FLAG_UPDATE_CURRENT);
							ILog.LogE(this.getClass(), "num[i]" + num[i]
									+ "  msg[j]" + msg[j]);
							smsManager.sendTextMessage(num[i], null, msg[j],
									mSendPITemp, null);

						}
					}
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			// Log.i("my", e.toString());
			if (e instanceof SecurityException) {
				// new al
				// Toast.makeText(context.get, "请开启发送短信权限！",
				// Toast.LENGTH_LONG).show();
				// Log.i("my", "请开启发送短信权限");
				return false;
				// Intent intent = new Intent(context, CrashReportDialog.class);
				// context.startActivity(intent);
				// // if (CallStatMainActivity.getInstance() != null) {
				// new AlertDialog.Builder(CallStatMainActivity.getInstance())
				// .setTitle(R.string.warm_tip).setMessage("请开启发送短信权限！")
				// .setPositiveButton(R.string.confirm, null).show();
				// }
			}
			ILog.logException(this.getClass(), e);
			return false;
		}
		return true;
	}
}
