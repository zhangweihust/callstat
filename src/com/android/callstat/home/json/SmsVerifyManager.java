package com.android.callstat.home.json;

import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.android.callstat.common.DataUtils;
import com.android.callstat.common.json.AbstractWebLoadManager;
import com.android.callstat.home.bean.SmsVerifyBean;

public class SmsVerifyManager extends AbstractWebLoadManager<SmsVerifyBean> {

	public SmsVerifyManager(Context context, String url) {
		super(context, url);
	}

	public SmsVerifyManager(Context context, String url, String encoding) {
		super(context, url, encoding);
	}

	@Override
	protected SmsVerifyBean paserJSON(String json) {
		JSONObject jsonObject = DataUtils.stringToJsonObject(json);
		if (jsonObject == null) {
			return null;
		}

		int smsId = jsonObject.optInt(SmsVerifyBean.SMS_ID);
		Log.i("wanglei", "SmsVerifyBean.SMS_ID====" + smsId);
		String tel = jsonObject.optString(SmsVerifyBean.SMS_GATE_NUM);

		SmsVerifyBean svb = new SmsVerifyBean(smsId, tel);
		;
		return svb;
	}

}
