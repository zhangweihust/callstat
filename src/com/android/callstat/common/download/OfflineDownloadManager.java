package com.android.callstat.common.download;

import java.util.HashMap;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.AsyncTask;

import com.android.callstat.common.StringUtil;

/**
 * @author long.xue
 */
public class OfflineDownloadManager extends ContextWrapper {

	private static OfflineDownloadManager sInstance;
	private static Context sContext;
	private OfflineDownloadTask offlineDownloadTask;
	private static HashMap<Integer, AsyncTask<?, ?, ?>> sdownloadTask;

	/**
	 * @param base
	 */
	private OfflineDownloadManager(Context base) {
		super(base);
	}

	public static OfflineDownloadManager getInstance(final Context context) {
		if (null == sInstance) {
			sInstance = new OfflineDownloadManager(context);
			sContext = context;
			sdownloadTask = new HashMap<Integer, AsyncTask<?, ?, ?>>();
		}
		return sInstance;
	}

	public void startDownloadTask(int busId, String url,
			OfflineDownloadEvent offlineDownloadEvent, boolean isZip) {
		if (StringUtil.isNullOrWhitespaces(url)) {
			return;
		}
		offlineDownloadTask = new OfflineDownloadTask(sContext, busId,
				offlineDownloadEvent, isZip);
		offlineDownloadTask.execute(url);
		sdownloadTask.put(busId, offlineDownloadTask);
	}

	public void cancelDownloadTask(int busId) {
		OfflineDownloadTask offlineDownloadTask = (OfflineDownloadTask) sdownloadTask
				.get(busId);
		cancelTaskInterrupt(offlineDownloadTask);
	}

	/**
	 * Cancel an {@link AsyncTask}. If it's already running, it'll be
	 * interrupted.
	 */
	private void cancelTaskInterrupt(AsyncTask<?, ?, ?> task) {
		cancelTask(task, true);
	}

	/**
	 * Cancel an {@link AsyncTask}.
	 * 
	 * @param mayInterruptIfRunning
	 *            <tt>true</tt> if the thread executing this task should be
	 *            interrupted; otherwise, in-progress tasks are allowed to
	 *            complete.
	 */
	private void cancelTask(AsyncTask<?, ?, ?> task,
			boolean mayInterruptIfRunning) {
		if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
			task.cancel(mayInterruptIfRunning);
		}
	}
}
