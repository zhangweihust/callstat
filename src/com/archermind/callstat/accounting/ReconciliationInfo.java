package com.archermind.callstat.accounting;

public class ReconciliationInfo {
	private long time;
	/** 两次对账之间的差额 */
	private double difference;
	/** 本地通话主叫时间 */
	private int thisLocalityDialingTimes;
	/** 本地长途主叫时间 */
	private int longDistanceDialingTimes;
	/** 漫游时间（漫游暂时不区分拨打还是接听） */
	private int roamingTimes;
	/** 未知主叫时间 */
	private int unkonwDialingTimes;
	/** IP主叫时间 */
	private int ipDialingTimes;

	/** 主叫短号 */
	private int shortDialingTimes;
	// /** 漫游被叫通话时间 */
	// private int roamCalledTimes;
	/** 发送短信条数 */
	private int sendSMSNum;
	/** 超出套餐的数据流量 */
	private long trafficData;

	// added by zhangjing to add delta in caculation.
	private int coeff_delta1;
	private int coeff_delta2;
	private int coeff_delta3;
	private int coeff_delta4;
	private int coeff_delta5;
	private int coeff_delta6;

	public ReconciliationInfo(long time, int thisLocalityDialingTimes,
			int longDistanceDialingTimes, int roamingTimes,
			int unkonwDialingTimes, int ipDialingTimes, int shortDialingTimes,
			int sendSMSNum, long trafficData, double difference,
			int coeff_delta1, int coeff_delta2, int coeff_delta3,
			int coeff_delta4, int coeff_delta5, int coeff_delta6) {
		this.time = time;
		this.thisLocalityDialingTimes = thisLocalityDialingTimes;
		this.longDistanceDialingTimes = longDistanceDialingTimes;
		this.roamingTimes = roamingTimes;
		this.unkonwDialingTimes = unkonwDialingTimes;
		this.ipDialingTimes = ipDialingTimes;
		this.sendSMSNum = sendSMSNum;
		this.trafficData = trafficData;
		this.difference = difference;
		this.shortDialingTimes = shortDialingTimes;

		// added by zhangjing@archermind.com
		this.coeff_delta1 = coeff_delta1;
		this.coeff_delta2 = coeff_delta2;
		this.coeff_delta3 = coeff_delta3;
		this.coeff_delta4 = coeff_delta4;
		this.coeff_delta5 = coeff_delta5;
		this.coeff_delta6 = coeff_delta6;
	}

	// public int getRoamDialingTimes() {
	// return roamDialingTimes;
	// }

	public long getTime() {
		return time;
	}

	public int getIPDialingTimes() {
		return ipDialingTimes;
	}

	public int getThisLocalityDialingTimes() {
		return thisLocalityDialingTimes;
	}

	public int getLongDistanceDialingTimes() {
		return longDistanceDialingTimes;
	}

	// public int getRoamCalledTimes() {
	// return roamCalledTimes;
	// }
	public int getRoamingTimes() {
		return roamingTimes;
	}

	public int getUnkonwDialingTimes() {
		return unkonwDialingTimes;
	}

	public int getSendSmsNum() {
		return sendSMSNum;
	}

	public long getTrafficData() {
		return trafficData;
	}

	public double getDifference() {
		return difference;
	}

	public int getShortDialingTimes() {
		return shortDialingTimes;
	}

	public int getCoeffDelta1() {
		return coeff_delta1;
	}

	public int getCoeffDelta2() {
		return coeff_delta2;
	}

	public int getCoeffDelta3() {
		return coeff_delta3;
	}

	public int getCoeffDelta4() {
		return coeff_delta4;
	}

	public int getCoeffDelta5() {
		return coeff_delta5;
	}

	public int getCoeffDelta6() {
		return coeff_delta6;
	}
}
