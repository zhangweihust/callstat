package com.archermind.callstat.monitor.bean;

import java.io.Serializable;

/**
 * this class is used as a general java bean
 * */
public class SmsLog implements Serializable {
	private static final long serialVersionUID = 1L;
	private Integer protocol;
	private String number;
	private long date;

	public SmsLog(String number, long date, Integer protocol) {
		this.number = number;
		this.date = date;
		this.protocol = protocol;
	}

	public Integer getProtocol() {
		return protocol;
	}

	public void setProtocol(Integer protocol) {
		this.protocol = protocol;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public long getDate() {
		return date;
	}

	public void setDate(long date) {
		this.date = date;
	}
}
