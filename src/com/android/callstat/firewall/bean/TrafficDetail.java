package com.android.callstat.firewall.bean;

public class TrafficDetail {
	// time by milliseconds
	private String date;
	// uid of the specific application
	private int uid;
	// the name of the specific application
	private String appName;
	// type of the used network mode:2g/3g or wifi
	// private String type;
	// upload and download
	private long gprs_upload;
	private long gprs_download;

	private long wifi_upload;
	private long wifi_download;

	// private long date;

	private String packageName;

	// private Drawable icon;
	private boolean isGprsRejected = false;
	private boolean isWifiRejected = false;

	private long node_upload;

	private long node_download;

	// private boolean isUninstalled = false;

	// public boolean isUninstalled() {
	// return isUninstalled;
	// }
	//
	// public void setUninstalled(boolean isUninstalled) {
	// this.isUninstalled = isUninstalled;
	// }

	public boolean getWifiRejected() {
		return isWifiRejected;
	}

	public void setWifiRejected(boolean isWifiRejected) {
		this.isWifiRejected = isWifiRejected;
	}

	public TrafficDetail(String date, int uid, String appName,
			String packageName, long gprs_upload, long gprs_download,
			long wifi_upload, long wifi_download, long node_upload,
			long node_download) {
		// this.icon = icon;
		this.date = date;
		this.uid = uid;
		this.appName = appName;
		this.packageName = packageName;
		// this.type = type;
		this.gprs_upload = gprs_upload;
		this.gprs_download = gprs_download;

		this.wifi_upload = wifi_upload;
		this.wifi_download = wifi_download;

		this.node_upload = node_upload;
		this.node_download = node_download;

		// this.isUninstalled = isUninstalled;
		// this.date = date;
	}

	/*
	 * public TrafficDetail(int uid, String appName, String packageName, boolean
	 * rejected,long upload, long download ,long node_upload,long node_download)
	 * { this.uid = uid; this.appName = appName; this.packageName = packageName;
	 * this.isRejected = rejected; this.upload = upload; this.download =
	 * download; this.node_upload = node_upload; this.node_download =
	 * node_download;
	 * 
	 * }
	 */

	public long getNode_upload() {
		return node_upload;
	}

	public void setNode_upload(long node_upload) {
		this.node_upload = node_upload;
	}

	public long getNode_download() {
		return node_download;
	}

	public void setNode_download(long node_download) {
		this.node_download = node_download;
	}

	public String getDate() {
		return date;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getPackageName() {
		return packageName;
	}

	// public long getDate() {
	// return date;
	// }

	// public void setDate(long date) {
	// this.date = date;
	// }

	public void setDate(String date) {
		this.date = date;
	}

	public int getUid() {
		return uid;
	}

	public void setUid(Integer uid) {
		this.uid = uid;
	}

	public String getAppName() {
		return appName;
	}

	// public Drawable getIcon() {
	// return icon;
	// }
	//
	// public void setIcon(Drawable icon) {
	// this.icon = icon;
	// }

	public boolean getGprsRejected() {
		return isGprsRejected;
	}

	public void setGprsRejected(boolean isRejected) {
		this.isGprsRejected = isRejected;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	// public String getType() {
	// return type;
	// }

	// public void setType(String type) {
	// this.type = type;
	// }

	public long getGprsUpload() {
		return gprs_upload;
	}

	public void setGprsUpload(long upload) {
		this.gprs_upload = upload;
	}

	public long getWifiUpload() {
		return wifi_upload;
	}

	public void setWifiUpload(long upload) {
		this.wifi_upload = upload;
	}

	public long getGprsDownload() {
		return gprs_download;
	}

	public void setGprsDownload(long download) {
		this.gprs_download = download;
	}

	public long getWifiDownload() {
		return wifi_download;
	}

	public void setWifiDownload(long download) {
		this.wifi_download = download;
	}

	public long getNodeDownload() {
		return node_download;
	}

	public void setNodeDownload(long download) {
		this.node_download = download;
	}

}
