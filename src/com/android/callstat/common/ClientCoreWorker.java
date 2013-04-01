package com.android.callstat.common;

import java.util.HashMap;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.android.callstat.common.net.Headers;
import com.android.callstat.common.net.Request;

public final class ClientCoreWorker extends Handler {
	private static final String THREAD_NAME = "ClientCoreWorkerThread";

	private static ClientCoreWorker sWorkerHandler;

	/**
	 * Package level class to be used while creating a cache entry.
	 */
	static class CacheData {
		String mUrl;
		String mMimeType;
		Headers mHeaders;
		byte[] mData;
		int mDataLength;
		int mCacheLevel;
	}

	public static synchronized ClientCoreWorker getHandler() {
		if (sWorkerHandler == null) {
			HandlerThread thread = new HandlerThread(THREAD_NAME,
					android.os.Process.THREAD_PRIORITY_DEFAULT
							+ android.os.Process.THREAD_PRIORITY_LESS_FAVORABLE);
			thread.start();
			sWorkerHandler = new ClientCoreWorker(thread.getLooper());
		}
		return sWorkerHandler;
	}

	private ClientCoreWorker(Looper looper) {
		super(looper);
	}

	// trigger transaction once a minute
	private static final int CACHE_TRANSACTION_TICKER_INTERVAL = 20 * 1000;

	private static boolean mCacheTickersBlocked = true;

	// message ids
	public static final int MSG_ADD_STREAMLOADER = 101;
	public static final int MSG_ADD_HTTPLOADER = 102;
	public static final int MSG_STOP_HTTPLOAD = 103;
	public static final int MSG_CREATE_CACHE = 203;
	public static final int MSG_UPDATE_CACHE_ENCODING = 204;
	public static final int MSG_APPEND_CACHE = 205;
	public static final int MSG_SAVE_CACHE = 206;
	public static final int MSG_REMOVE_CACHE = 207;
	public static final int MSG_TRIM_CACHE = 208;
	public static final int MSG_CLEAR_CACHE = 209;
	public static final int MSG_CACHE_TRANSACTION_TICKER = 210;
	public static final int MSG_PAUSE_CACHE_TRANSACTION = 211;
	public static final int MSG_RESUME_CACHE_TRANSACTION = 212;

	@Override
	public void handleMessage(Message msg) {
		switch (msg.what) {
		case MSG_ADD_STREAMLOADER: {
			break;
		}
		case MSG_ADD_HTTPLOADER: {
			HashMap<String, Object> map = (HashMap<String, Object>) msg.obj;
			LoadListener loadListener = (LoadListener) map.get("loadListener");
			int cacheMode = ((Integer) map.get("cacheMode")).intValue();
			Request req = (Request) map.get("request");
			boolean head = (Boolean) map.get("head");
			ResourceLoader resourceLoader = (ResourceLoader) map
					.get("resourceLoader");
			if (resourceLoader != null && req != null) {
				resourceLoader.handleHTTPLoad(req, cacheMode, loadListener,
						head);
			}
			break;
		}
		case MSG_STOP_HTTPLOAD: {
			int activityId = msg.arg1;
			ResourceLoader resourceLoader = (ResourceLoader) msg.obj;
			if (resourceLoader != null) {
				resourceLoader.handleStopHTTPLoad(activityId);
			}
			break;
		}
		case MSG_CREATE_CACHE: {
			CacheData data = (CacheData) msg.obj;
			/*
			 * CacheManager.createCache(data.mUrl, data.mHeaders,
			 * data.mMimeType, data.mData, data.mDataLength, data.mCacheLevel,
			 * false);
			 */
			break;
		}
		case MSG_REMOVE_CACHE: {
			break;
		}
		case MSG_TRIM_CACHE: {
			break;
		}
		case MSG_CLEAR_CACHE: {
			// CacheManager.clearCache();
			break;
		}
		case MSG_CACHE_TRANSACTION_TICKER: {
			/*
			 * CacheManager.endTransaction(); CacheManager.startTransaction();
			 */
			sendEmptyMessageDelayed(MSG_CACHE_TRANSACTION_TICKER,
					CACHE_TRANSACTION_TICKER_INTERVAL);
			break;
		}
		case MSG_PAUSE_CACHE_TRANSACTION: {
			break;
		}
		case MSG_RESUME_CACHE_TRANSACTION: {
			break;
		}
		}
	}
}