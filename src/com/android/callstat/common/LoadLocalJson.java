package com.android.callstat.common;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;

public class LoadLocalJson {

	/**
	 * @param context
	 * @param jsonResourceId
	 *            the txt's id
	 * @return return the JsonArray
	 */
	public static JSONArray loadJSONArray(Context context, int jsonResourceId) {
		String jsonStr = "";
		JSONArray jsonArray = null;
		try {
			jsonStr = DataUtils.loadTextFromResource(context, jsonResourceId);
			jsonArray = new JSONArray(jsonStr);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return jsonArray;
	}

	/**
	 * @param context
	 * @param jsonResourceId
	 *            the txt's id
	 * @return the JsonObject
	 */
	public static JSONObject loadJSONObject(Context context, int jsonResourceId) {
		String jsonStr = "";
		JSONObject jsonObject = null;
		try {
			jsonStr = DataUtils.loadTextFromResource(context, jsonResourceId);
			jsonObject = new JSONObject(jsonStr);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return jsonObject;
	}
}
