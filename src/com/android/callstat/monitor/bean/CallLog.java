package com.android.callstat.monitor.bean;

import java.io.Serializable;

/**
 * this class is used as a general java bean
 * */
public class CallLog implements Serializable {
	/**
	 * 
	 */
	/** 免费电话 */
	public static final int CALL_FREE = 0;
	/** 本地电话 */
	public static final int CALL_LOCAL = 1;
	/** 长途电话 */
	public static final int CALL_LONG_DISTANCE = 2;
	/** IP拨号 */
	public static final int CALL_IP = 3;
	/** 短号电话 */
	public static final int CALL_SHORT = 4;
	/** 未知号码 */
	public static final int CALL_UNKONW = -1;
	/** 漫游状态 */
	public static final int CALL_ROAMING = 5;

	private static final long serialVersionUID = 1L;
	private String number;
	private Integer type;
	private Integer duration;
	private String date;
	private int numberType;

	public CallLog(String number, String date, Integer type, Integer duration,
			int numberType) {
		this.number = number;
		this.type = type;
		this.duration = duration;
		this.date = date;
		this.numberType = numberType;
	}

	public int getNumberType() {
		return numberType;
	}

	public void setNumberType(int numberType) {
		this.numberType = numberType;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public Integer getDuration() {
		return duration;
	}

	public void setDuration(Integer duration) {
		this.duration = duration;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

}
