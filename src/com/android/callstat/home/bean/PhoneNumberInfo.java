package com.android.callstat.home.bean;

public class PhoneNumberInfo {
	private String province;
	private String city;
	private String operator;
	private String number;
	private boolean bIP = false;

	public PhoneNumberInfo(String number, String province, String city,
			String operator) {
		this.number = number;
		this.province = province;
		this.city = city;
		this.operator = operator;
	}

	public void addIP() {
		operator += "IP";
		bIP = true;
	}

	public boolean getBIP() {
		return bIP;
	}

	public String getProvince() {
		return province;
	}

	public String getCity() {
		return city;
	}

	public String getOperator() {
		return operator;
	}

	public String getNumber() {
		return number;
	}

}
