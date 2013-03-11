package com.archermind.callstat.service.json;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;

import com.archermind.callstat.ConfigManager;
import com.archermind.callstat.ILog;
import com.archermind.callstat.accounting.AccountingKeyWordsBean;
import com.archermind.callstat.common.DataUtils;
import com.archermind.callstat.common.download.CacheFileManager;
import com.archermind.callstat.common.json.AbstractWebLoadManager;

public class AccountingDatabaseUpdater extends
		AbstractWebLoadManager<List<AccountingKeyWordsBean>> {

	public AccountingDatabaseUpdater(Context context, String url) {
		super(context, url);
	}

	@Override
	protected List<AccountingKeyWordsBean> paserJSON(String json) {
		ILog.LogI(this.getClass(), "String json:----------" + json);
		// CacheFileManager.getInstance().log(json);
		// pre process
		String[] strArr = json.split("bill=");
		String jsonPart = strArr[0];
		String[] keywords = strArr[1].split("flowrate=");
		JSONArray jsonArray = DataUtils.stringToJsonArray(jsonPart);
		return paseReconciliationBeanList(jsonArray, keywords);
	}

	private List<AccountingKeyWordsBean> paseReconciliationBeanList(
			JSONArray jsonArray, String[] keywords) {
		if (jsonArray == null) {
			return null;
		}
		List<AccountingKeyWordsBean> list = new ArrayList<AccountingKeyWordsBean>();

		ConfigManager config = new ConfigManager(mContext);
		String ye_keywords = keywords[0];
		String ll_keywords = keywords[1];

		int len = jsonArray.length();
		for (int i = 0; i < len; i++) {
			JSONObject json = jsonArray.optJSONObject(i);
			String province = json.optString("province");
			int type = json.optInt("type");
			String operator = json.optString("operator");
			String brand = json.optString("brand");

			String queryNum = json.optString("queryNum");
			String code = json.optString("code");
			String keyword = "";
			switch (type) {
			case 0:
				keyword = ye_keywords;
				break;
			case 1:
				keyword = ll_keywords;
				break;
			default:
				break;
			}

			if (i == len - 1) {
				config.setAccountingDatabaseUpdateTime(json.optString("time"));
			}

			AccountingKeyWordsBean akb = new AccountingKeyWordsBean(province,
					type, operator, brand, queryNum, code, keyword);

			list.add(akb);
		}
		return list;
	}

}
