package com.archermind.callstat.home.bean;

public class SmsVerifyBean {

	public static final String SMS_ID = "smsID";
	public static final String SMS_GATE_NUM = "tel";
	public static final String BINDING_ACTION = "binding";

	public static final int SMS_ID_CODE_INVALIDATE = 0;
	public static final int SMS_ID_CODE_REPEAT = -1;

	private int smsID;
	private String smsGateNum;

	public SmsVerifyBean(int smsId, String smsGateNum) {
		this.smsID = smsId;
		this.smsGateNum = smsGateNum;
	}

	public int getSmsID() {
		return smsID;
	}

	public void setSmsID(int smsID) {
		this.smsID = smsID;
	}

	public String getSmsGateNum() {
		return smsGateNum;
	}

	public void setSmsGateNum(String smsGateNum) {
		this.smsGateNum = smsGateNum;
	}
}
