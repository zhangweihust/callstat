package com.archermind.callstat.accounting;

public class AccountingKeyWordsBean {
	private String province;
	private int type;
	private String operator;
	private String brand;
	private String queryNum;
	private String code;
	private String keywords;

	public AccountingKeyWordsBean(String province, int type, String operator,
			String brand, String queryNum, String code, String keywords) {
		this.province = province;
		this.type = type;
		this.operator = operator;
		this.brand = brand;
		this.queryNum = queryNum;
		this.code = code;
		this.keywords = keywords;
	}

	public String getProvince() {
		return province;
	}

	public void setProvince(String province) {
		this.province = province;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	public String getQueryNum() {
		return queryNum;
	}

	public void setQueryNum(String queryNum) {
		this.queryNum = queryNum;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getKeywords() {
		return keywords;
	}

	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}
}
