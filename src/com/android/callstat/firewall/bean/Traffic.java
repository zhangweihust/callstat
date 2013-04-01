package com.android.callstat.firewall.bean;

public class Traffic {
	// date by milliseconds
	private String time;

	private String yearMonth;
	// 2G/3G or wifi
	private String type;

	// upload and download,computed by kb
	private long upload;

	private long download;

	public Traffic(String time, String year_month, String type, long upload,
			long download) {
		this.time = time;
		this.yearMonth = year_month;
		this.type = type;
		this.upload = upload;
		this.download = download;
	}

	public String getTime() {
		return time;
	}

	public void setDate(String date) {
		this.time = date;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public long getUpload() {
		return upload;
	}

	public void setUpload(long upload) {
		this.upload = upload;
	}

	public long getDownload() {
		return download;
	}

	public void setDownload(long download) {
		this.download = download;
	}

	public void setYearMonth(String yearMonth) {
		this.yearMonth = yearMonth;
	}

	public String getYearMonth() {
		return yearMonth;
	}
}
