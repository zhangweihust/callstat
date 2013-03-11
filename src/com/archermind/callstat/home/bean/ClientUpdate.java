package com.archermind.callstat.home.bean;

import java.io.Serializable;

public class ClientUpdate implements Serializable {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 1L;

	public static final String VERSION_CODE = "versionCode";
	public static final String DOWNLOAD_URL = "url";
	public static final String FORCE_UPDATE = "forceUpdate";
	public static final String DESCRIPTION = "description";
	public static final String APK_NAME = "apkName";
	public static final String APK_SIZE = "apkSize";
	public static final String VERSION_NAME = "versionName";

	private int versionCode;
	private String versionName;
	private String downloadUrl;
	private String apkName;

	public void setApkName(String apkName) {
		this.apkName = apkName;
	}

	// 0:false 1:true
	private int forceUpdate;
	private int apkSize;

	private String[] description;

	public boolean isForceUpdate() {
		return forceUpdate == 1;
	}

	public int getVersionCode() {
		return versionCode;
	}

	public void setVersionCode(int versionCode) {
		this.versionCode = versionCode;
	}

	public String getVersionName() {
		return versionName;
	}

	public void setVersionName(String versionName) {
		this.versionName = versionName;
	}

	public String getDownloadUrl() {
		return downloadUrl;
	}

	public void setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}

	public int getForceUpdate() {
		return forceUpdate;
	}

	public void setForceUpdate(int forceUpdate) {
		this.forceUpdate = forceUpdate;
	}

	public int getApkSize() {
		return apkSize;
	}

	public void setApkSize(int apkSize) {
		this.apkSize = apkSize;
	}

	public String[] getDescription() {
		return description;
	}

	public void setDescription(String[] description) {
		this.description = description;
	}

	public String getApkName() {
		return apkName;
	}

}