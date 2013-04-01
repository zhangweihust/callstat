package com.android.callstat.home.json;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;

import com.android.callstat.common.DataUtils;
import com.android.callstat.common.StringUtil;
import com.android.callstat.common.json.AbstractWebLoadManager;
import com.android.callstat.home.bean.ClientUpdate;

public class UpdateManager extends AbstractWebLoadManager<ClientUpdate> {

	/**
	 * @param context
	 * @param url
	 */
	public UpdateManager(Context context, String url) {
		this(context, url, null);
	}

	/**
	 * @param context
	 * @param url
	 * @param encoding
	 */
	public UpdateManager(Context context, String url, String encoding) {
		super(context, url, encoding);
	}

	@Override
	protected ClientUpdate paserJSON(String json) {
		// Log.i("i", "json:----------" + json);
		JSONObject jsonObject = DataUtils.stringToJsonObject(json);
		return paserClientUpdate(jsonObject);
	}

	/**
	 * @param jsonObject
	 * @return
	 */
	private ClientUpdate paserClientUpdate(JSONObject jsonObject) {
		if (jsonObject == null) {
			return null;
		}
		ClientUpdate clientUpdate = new ClientUpdate();

		clientUpdate.setDownloadUrl(StringUtil.optString(jsonObject,
				ClientUpdate.DOWNLOAD_URL));
		clientUpdate.setVersionCode(jsonObject
				.optInt(ClientUpdate.VERSION_CODE));
		clientUpdate.setForceUpdate(jsonObject
				.optInt(ClientUpdate.FORCE_UPDATE));
		clientUpdate.setVersionName(jsonObject
				.optString(ClientUpdate.VERSION_NAME));
		clientUpdate.setApkName(jsonObject.optString(ClientUpdate.APK_NAME));
		clientUpdate.setApkSize(jsonObject.optInt(ClientUpdate.APK_SIZE));

		JSONArray description = jsonObject
				.optJSONArray(ClientUpdate.DESCRIPTION);
		String[] descriptions = paserDescriptions(description);
		clientUpdate.setDescription(descriptions);

		return clientUpdate;
	}

	/**
	 * @param jsonArray
	 * @return
	 */
	private String[] paserDescriptions(JSONArray jsonArray) {
		if (null == jsonArray) {
			return null;
		}
		int len = jsonArray.length();
		String[] descriptions = new String[len];
		for (int i = 0; i < len; i++) {
			String description = jsonArray.optString(i);
			descriptions[i] = description;
		}
		return descriptions;
	}
}