package com.archermind.callstat.monitor;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import com.archermind.callstat.ConfigManager;
import com.archermind.callstat.common.database.CallStatDatabase;

public class ReceiveSolutionHandler extends Handler {
	private int mDuration = 0;
	private Context mCtx;

	private Toast mToast;

	// 继承Handler类时，必须重写handleMessage方法
	public ReceiveSolutionHandler() {
	}

	public ReceiveSolutionHandler(Looper lo, Context ctx, int duration) {
		super(lo);
		this.mCtx = ctx;
		this.mDuration = duration;
	}

	private void showToast(final Toast toast) {
		toast.show();
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				toast.show();
			}
		}, 2500);
	}

	@Override
	public void handleMessage(Message msg) {
		super.handleMessage(msg);
		Bundle b = msg.getData();// Obtains a Bundle of arbitrary data
									// associated with this event
		String haveSolutionFlag = b.getString("haveSolutionFlag");
		// Log.i("callstats","进入handleMessage");

		if (haveSolutionFlag.equalsIgnoreCase("null")) {
			// Log.i("callstats","收到了方程组为空的消息");
			// 如果收到了方程组为空的消息，则做...
			if (new ConfigManager(mCtx).isHungupNotice()) {
				mToast = Toast.makeText(mCtx, "AM提示：本次通话时长："
						+ getTimeShown(mDuration), Toast.LENGTH_LONG);
				// showToast(mToast);
			}
			return;
		}

		if (haveSolutionFlag.equalsIgnoreCase("true")) {
			// 如果收到了有解的消息，则做...
			// Log.i("callstats", "收到了有解的消息");
			ConfigManager config = new ConfigManager(mCtx);
			if (config.isHungupNotice()) {
				mToast = Toast.makeText(mCtx, "AM提示：本次通话时长："
						+ getTimeShown(mDuration) + "\n" + "预计话费余额为："
						+ PhonebillCaculateThread.AccountBalance + "元",
						Toast.LENGTH_LONG);
				// showToast(mToast);
				new refreshDayConsumeThread(
						(float) PhonebillCaculateThread.AccountConsume).start();
				Intent intent = new Intent();
				intent.setAction(ObserverManager.HAS_GUESS_YE_ACTION);
				mCtx.sendBroadcast(intent);
			}
		} else {
			// 如果收到了无解的消息，则做...
			// Log.i("callstats", "收到了无解的消息");
			if (new ConfigManager(mCtx).isHungupNotice()) {
				mToast = Toast.makeText(mCtx, "AM提示：本次通话时长："
						+ getTimeShown(mDuration), Toast.LENGTH_LONG);
				// showToast(mToast);
			}
		}
	}

	private String getTimeShown(int duration) {
		String time = "";
		if (duration < 60) {
			time = duration + "秒";
		} else if (duration >= 60 && duration < 3600) {
			int min = duration / 60;
			int sec = duration % 60;
			time = min + "分" + sec + "秒";
		} else {
			int hour = duration / 3600;
			int leftWithoutHour = duration - (hour * 3600);
			int min = leftWithoutHour / 60;
			int sec = leftWithoutHour % 60;
			time = hour + "小时" + min + "分" + sec + "秒";
		}
		return time;
	}

	class refreshDayConsumeThread extends Thread {
		private float value;

		refreshDayConsumeThread(float value) {
			this.value = value;
		}

		public void run() {
			CallStatDatabase.getInstance(mCtx).refreshEachDayFeeConsume();
		}
	};
}
