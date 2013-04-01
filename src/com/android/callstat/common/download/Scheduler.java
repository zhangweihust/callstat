package com.android.callstat.common.download;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;

import com.android.callstat.ConfigManager;

public final class Scheduler extends ContextWrapper {

	private static Scheduler sInstance;

	/**
	 * Retrieve the instance.
	 * 
	 * @return the value of instance
	 */
	public static synchronized final Scheduler init(final Context context) {
		if (null == sInstance) {
			sInstance = new Scheduler(context);
		}
		return sInstance;
	}

	/**
	 * Retrieve the instance.
	 * 
	 * @return the value of instance
	 */
	public static synchronized final Scheduler getInstance() {
		if (null == sInstance) {
			throw new IllegalStateException("Not initialized.");
		}
		return sInstance;
	}

	/**
	 * Initiate a new instance of {@link Scheduler}.
	 * 
	 * @param base
	 */
	private Scheduler(final Context base) {
		super(base);
	}

	/**
	 * the main toggle for schedule task
	 */
	public void scheduleCollectTask() {
		final ConfigManager configManager = new ConfigManager(this);
		final AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		if (configManager.isFirstLaunch()) {
			setCollectTask(alarmManager, configManager.getOfflineInterval(),
					false);
		}
	}

	private void setCollectTask(final AlarmManager alarmManager,
			final long intervalTime, final boolean cancelForward) {
		final long triggerTime = System.currentTimeMillis();
		final Intent intent = new Intent(this, OfflineDownloadService.class);
		final PendingIntent pendingIntent = PendingIntent.getService(this, 0,
				intent, 0);
		if (cancelForward) {
			alarmManager.cancel(pendingIntent);
		}
		alarmManager.setRepeating(AlarmManager.RTC, triggerTime, intervalTime,
				pendingIntent);
	}

	/**
	 * force to schedule a new task
	 */
	public void forceScheduleCollectTasks() {
		final ConfigManager configManager = new ConfigManager(this);
		final AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		setCollectTask(alarmManager, configManager.getOfflineInterval(), true);
	}

}
