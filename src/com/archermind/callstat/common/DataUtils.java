package com.archermind.callstat.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

public final class DataUtils {

	private static final String TAG = DataUtils.class.getSimpleName();
	private static final String NULL = "null";

	public static <T> List<T> parseListFromJson(final JSONArray array,
			Class<T> cl) {
		final List<T> result;
		if (array != null) {
			result = new ArrayList<T>();
			for (int i = 0; i < array.length(); i++) {
				try {
					final T obj = DataUtils.<T> parseObjFromJson(
							array.getJSONObject(i), cl);
					if (obj != null) {
						result.add(obj);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		} else {
			result = null;
		}
		return result;
	}

	/*
	 * Use the default "parse" method to parse a JSONObject.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T parseObjFromJson(final JSONObject jsonObj, Class<T> cl) {
		T result = null;
		try {
			Method parseMethod = cl.getMethod("parse", JSONObject.class);
			result = (T) (parseMethod.invoke(cl, jsonObj));

		} catch (NoSuchMethodException e) {
			assert false : "No parse method defined!";
		} catch (IllegalArgumentException e) {
			assert false : "should not come to here";
		} catch (IllegalAccessException e) {
			assert false : "should not come to here";
		} catch (InvocationTargetException e) {
			assert false : "should not come to here";
		}

		return result;
	}

	/*
	 * parser.method will be used to parse the object.
	 */
	public static <T> List<T> parseListFromJson(final JSONArray array,
			Object parser, String method) {
		final List<T> result;
		if (array != null) {
			result = new ArrayList<T>();
			for (int i = 0; i < array.length(); i++) {
				try {
					final T obj = DataUtils.<T> parseObjFromJson(
							array.getJSONObject(i), parser, method);
					if (obj != null) {
						result.add(obj);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		} else {
			result = null;
		}
		return result;
	}

	/*
	 * parser.method will be used to parse the object.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T parseObjFromJson(final JSONObject json, Object parser,
			String method) {
		T result = null;
		try {
			Method parseMethod = parser.getClass().getMethod(method,
					JSONObject.class);
			result = (T) (parseMethod.invoke(parser, json));

		} catch (NoSuchMethodException e) {
			assert false : "No parse method defined!";
		} catch (IllegalArgumentException e) {
			assert false : "should not come to here";
		} catch (IllegalAccessException e) {
			assert false : "should not come to here";
		} catch (InvocationTargetException e) {
			assert false : "should not come to here";
		}

		return result;
	}

	public static JSONArray stringToJsonArray(String json) {
		if (TextUtils.isEmpty(json) || NULL.equalsIgnoreCase(json)) {
		} else {
			try {
				return new JSONArray(json);
			} catch (final JSONException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static JSONObject stringToJsonObject(String json) {
		JSONObject jsonObject = null;
		if (!TextUtils.isEmpty(json)) {
			try {
				jsonObject = new JSONObject(json);
			} catch (final JSONException e) {
				e.printStackTrace();
				return null;
			}
		}
		return jsonObject;
	}

	public static String getJSONString(JSONArray array, int index) {
		String result = null;
		try {
			JSONObject obj = array.getJSONObject(index);
			result = obj.toString();
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	@Deprecated
	public static <T> List<T> parseListFromCursor(Cursor cursor, Class<T> cl) {
		if (null != cursor) {
			List<T> results = new ArrayList<T>();
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				try {
					Method parseMethod = cl.getMethod("parseCursor",
							Cursor.class);
					T obj = (T) (parseMethod.invoke(cl, cursor));

					results.add(obj);
					cursor.moveToNext();

				} catch (NoSuchMethodException e) {
					assert false : "No parse method defined!";
				} catch (IllegalArgumentException e) {
					assert false : "should not come to here";
				} catch (IllegalAccessException e) {
					assert false : "should not come to here";
				} catch (InvocationTargetException e) {
					assert false : "should not come to here";
				}
			}

			return results;
		} else {
			return null;
		}
	}

	public static <T> List<T> copyList(List<T> source) {
		List<T> result = null;
		if (source != null) {
			result = new ArrayList<T>();
			for (T item : source) {
				result.add(item);
			}
		}

		return result;
	}

	public static String loadTextFromResource(Context context, int resourceId)
			throws UnsupportedEncodingException {

		InputStream is = context.getResources().openRawResource(resourceId);
		BufferedReader br = new BufferedReader(new InputStreamReader(is, "GBK"));
		StringBuilder result = new StringBuilder();
		String readLine = null;

		try {
			while ((readLine = br.readLine()) != null) {
				result.append(readLine);
			}

			is.close();
			br.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return result.toString();
	}
}
