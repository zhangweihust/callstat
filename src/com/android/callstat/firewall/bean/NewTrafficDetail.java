package com.android.callstat.firewall.bean;

public class NewTrafficDetail {
	// added by zhangjing
	private String date;// 记录流量消耗详细信息的日期 格式20120822
	private int uid;
	private String appName;
	private String packageName;
	private boolean isGprsRejected = false;
	private boolean isWifiRejected = false;
	private long gprs_upload;
	private long gprs_download;
	private long wifi_upload;
	private long wifi_download;
	private long node_upload;
	private long node_download;
	private double gprs;
	private boolean wifiOn;

	public NewTrafficDetail(String date, int uid, String appName,
			String packageName, boolean isGprsRejected, boolean isWifiRejected,
			long gprs_upload, long gprs_download, long wifi_upload,
			long wifi_download, long node_upload, long node_download,
			boolean wifiOn) {
		this.date = date;
		this.uid = uid;
		this.appName = appName;
		this.packageName = packageName;
		this.isGprsRejected = isGprsRejected;
		this.isWifiRejected = isWifiRejected;
		this.gprs_upload = gprs_upload;
		this.gprs_download = gprs_download;
		this.gprs = gprs_upload + gprs_download;
		this.wifi_upload = wifi_upload;
		this.wifi_download = wifi_download;
		this.node_upload = node_upload;
		this.node_download = node_download;
		this.wifiOn = wifiOn;
	}

	public NewTrafficDetail(long gprs_upload, long gprs_download,
			long wifi_upload, long wifi_download) {
		this.gprs_upload = gprs_upload;
		this.gprs_download = gprs_download;
		this.gprs = gprs_upload + gprs_download;
		this.wifi_upload = wifi_upload;
		this.wifi_download = wifi_download;
	}

	public void setWifiOn(boolean wifiOn) {
		this.wifiOn = wifiOn;
	}

	public boolean getWifiOn() {
		return this.wifiOn;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public int getUid() {
		return uid;
	}

	public void setUid(int uid) {
		this.uid = uid;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public boolean getIsGprsRejected() {
		return isGprsRejected;
	}

	public void setIsGprsRejected(boolean isGprsRejected) {
		this.isGprsRejected = isGprsRejected;
	}

	public boolean getIsWifiRejected() {
		return isWifiRejected;
	}

	public void setIsWifiRejected(boolean isWifiRejected) {
		this.isWifiRejected = isWifiRejected;
	}

	public long getGprsUpload() {
		return gprs_upload;
	}

	public void setGprsUpload(long gprs_upload) {
		this.gprs_upload = gprs_upload;
	}

	public long getGprsDownload() {
		return gprs_download;
	}

	public void setGprs(double gprs) {
		this.gprs = gprs;
	}

	public double getGprs() {
		return this.gprs;
	}

	public void setGprsDownload(long gprs_download) {
		this.gprs_download = gprs_download;
	}

	public long getWifiUpload() {
		return wifi_upload;
	}

	public void setWifiUpload(long wifi_upload) {
		this.wifi_upload = wifi_upload;
	}

	public long getWifiDownload() {
		return wifi_download;
	}

	public void setWifiDownload(long wifi_download) {
		this.wifi_download = wifi_download;
	}

	public long getNodeUpload() {
		return node_upload;
	}

	public void setNodeUpload(long node_upload) {
		this.node_upload = node_upload;
	}

	public long getNodeDownload() {
		return node_download;
	}

	public void setNodeDownload(long node_download) {
		this.node_download = node_download;
	}

}
