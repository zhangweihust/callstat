package com.android.callstat.monitor.bean;

//与用户月消费行为统计数据来源表相对应的一条记录的数据结构
public class MonthlyStatDataSource {
	// 以下注释所说的事件是指发一条短信或者是打一个电话（必须是有效的，在我们的统计范围之内的）
	public static int SMS = 0;// 定义了几种类型： 短信
	public static int LOCAL = 1; // 本地市话
	public static int LONG = 2; // 长途
	public static int ROAMING = 3; // 漫游
	public static int SHORT = 4; // 短号
	public static int IP = 5; // IP拨号
	public static int UNKNOWN = 6; // 未知归属地号码
	private String id = "";// id与数据库中的该条记录的主键相对应(就用从数据库中读出来的date来表示)
	private String time = ""; // 事件发生的时刻 年月日时分
	private String province = "";// 本地号码所属省份
	private String city = ""; // 本地号码所属城市
	private String mno = ""; // 本地号码的运营商
	private String brand = ""; // 本地号码的品牌
	private int type = 0; // 类型=0，短信；
							// =1，本地市话；=2，本地长途；=3，漫游；=4，短号；=5，IP拨号；=6,未知归属地号码
	private String number = "";// 对方的电话号码
	private String name = "";// 从电话簿中映射出的联系人姓名
	private int duration = 0;// 短信的条数或者是通话的分钟数
	private long usedGprs_toFirstDay = 0;// 截至月初已经使用的Gprs流量（字节数）
	private long usedGprs_toLastEvent = 0;// 截至上次事件已经使用的Gprs流量
	private float usedFee_toFirstDay = 0f;// 截至本月初已经使用的话费
	private float usedFee_toLastEvent = 0f;// 截至上次事件已经使用的话费
	private float rates_for_local = 0f;// 本地通话的估算费率
	private float rates_for_long_distance = 0f;// 长途通话的估算费率
	private float rates_for_roaming = 0f;// 漫游通话的估算费率
	private float rates_for_ip = 0f;// ip拨号的估算费率
	private float rates_for_short = 0f;// 短号拨号的估算费率
	private float rates_for_traffic = 0f;// 超出套餐的流量费率
	private long wlan_used_to_first_day = 0;// 截至本月初已使用的wlan时长
	private long wlan_used_to_last_event = 0;// 截至上次时间已使用的wlan时长
	private float rates_for_sms = 0f;// (由于现在wlan资费智能计算无法完成，所以这个字段存成短信资费)
	private boolean already_upload_flag = false;// 该条记录是否已经上传的标志字符串字段

	public MonthlyStatDataSource(String date, String time, String province,
			String city, String mno, String brand, int type, String number,
			String name, int duration, long usedGprs_toFirstDay,
			long usedGprs_toLastEvent, float usedFee_toFirstDay,
			float usedFee_toLastEvent, float rates_for_local,
			float rates_for_long_distance, float rates_for_roaming,
			float rates_for_ip, float rates_for_short, float rates_for_traffic,
			long wlan_used_to_first_day, long wlan_used_to_last_event,
			float rates_for_wlan, boolean already_upload_flag) {
		this.id = date;
		this.time = time;
		this.province = province;
		this.city = city;
		this.mno = mno;
		this.brand = brand;
		this.type = type;
		this.number = number;
		this.name = name;
		this.duration = duration;
		this.usedGprs_toFirstDay = usedGprs_toFirstDay;
		this.usedGprs_toLastEvent = usedGprs_toLastEvent;
		this.usedFee_toFirstDay = usedFee_toFirstDay;
		this.usedFee_toLastEvent = usedFee_toLastEvent;
		this.rates_for_local = rates_for_local;
		this.rates_for_long_distance = rates_for_long_distance;
		this.rates_for_roaming = rates_for_roaming;
		this.rates_for_ip = rates_for_ip;
		this.rates_for_short = rates_for_short;
		this.rates_for_traffic = rates_for_traffic;
		this.wlan_used_to_first_day = wlan_used_to_first_day;
		this.wlan_used_to_last_event = wlan_used_to_last_event;
		this.rates_for_sms = rates_for_wlan;
		this.already_upload_flag = already_upload_flag;
	}

	public String getId() {
		return id;
	}

	public String getTime() {
		return time;
	}

	public String getProvince() {
		return province;
	}

	public String getCity() {
		return city;
	}

	public String getMno() {
		return mno;
	}

	public String getBrand() {
		return brand;
	}

	public int getType() {
		return type;
	}

	public String getNumber() {
		return number;
	}

	public String getName() {
		return name;
	}

	public int getDuration() {
		return duration;
	}

	public long getUsedGprsTofirstDay() {
		return usedGprs_toFirstDay;
	}

	public long getUsedGprsToLastEvent() {
		return usedGprs_toLastEvent;
	}

	public float getUsedFeeToFirstDay() {
		return usedFee_toFirstDay;
	}

	public float getUsedFeeToLastEvent() {
		return usedFee_toLastEvent;
	}

	public float getRateForLocal() {
		return rates_for_local;
	}

	public float getRatesForLong() {
		return rates_for_long_distance;
	}

	public float getRatesForRoaming() {
		return rates_for_roaming;
	}

	public float getRatesForIP() {
		return rates_for_ip;
	}

	public float getRatesForShort() {
		return rates_for_short;
	}

	public float getRatesForTraffic() {
		return rates_for_traffic;
	}

	public long getWlanUsedToFirstDay() {
		return wlan_used_to_first_day;
	}

	public long getWlanUsedToLastEvent() {
		return wlan_used_to_last_event;
	}

	public float getRatesForSms() {
		return rates_for_sms;
	}

	public boolean getAlreadyUploadFlag() {
		return already_upload_flag;
	}

	public void setAlreadyUploadFlag(boolean already_upload_flag) {
		this.already_upload_flag = already_upload_flag;
	}
}
