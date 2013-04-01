package com.android.callstat.common.download;

/**
 * @author long.xue
 */
public interface OfflineDownloadEvent {

	public void start(int busId);

	public void progress(int busId, int progress);

	public void error(int busId, int status);

	public void cancal(int busId);

	public void complete(int busId);

}
