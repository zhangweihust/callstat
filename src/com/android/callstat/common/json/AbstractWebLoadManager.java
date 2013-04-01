/**     
 * AbstractWebLoadManager.java Create on 2012-4-06   
 *     
 * Copyright (c) 2011-5-20 by am  
 *     
 * @author longX    
 * @version 1.0
 *    
 */
package com.android.callstat.common.json;

import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntity;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.android.callstat.common.ResourceLoader;
import com.android.callstat.common.ResourceLoader.OnLoadFinishListener;

/**
 * @author longX
 * @param <T>
 */
public abstract class AbstractWebLoadManager<T> {

	private String mUrl;
	private OnWebLoadListener<T> mOnWebLoadListener;
	private ResourceLoader mResourceLoader;
	protected Context mContext;
	private String mEncoding;

	public interface OnWebLoadListener<T> {
		public void OnStart();

		public void OnCancel();

		public void OnLoadComplete(int statusCode);

		public void OnPaserComplete(T t);
	}

	/**
	 * @param context
	 * @param url
	 */
	public AbstractWebLoadManager(Context context, String url) {
		mUrl = url;
		mResourceLoader = ResourceLoader.getInstance(context);
		mContext = context;
		mEncoding = null;
	}

	public AbstractWebLoadManager(Context context, String url, String encoding) {
		mUrl = url;
		mResourceLoader = ResourceLoader.getInstance(context);
		mContext = context;
		mEncoding = encoding;
	}

	public void startManager() {
		if (null != mOnWebLoadListener) {
			mOnWebLoadListener.OnStart();
		}
		// mResourceLoader.startLoadingResource(mUrl, mOnLoadFinishListener);
		mResourceLoader.startLoadingResource(mUrl, mOnLoadFinishListener, 0,
				true);
	}

	public void startManager(long timeout) {
		if (null != mOnWebLoadListener) {
			mOnWebLoadListener.OnStart();
		}
		// mResourceLoader.startLoadingResource(mUrl, mOnLoadFinishListener);
		mResourceLoader.startLoadingResource(mUrl, mOnLoadFinishListener,
				timeout);
	}

	public void startManager(int cacheModel) {
		if (null != mOnWebLoadListener) {
			mOnWebLoadListener.OnStart();
		}
		// mResourceLoader.startLoadingResource(mUrl, mOnLoadFinishListener);
		mResourceLoader.startLoadingResource(mUrl, mOnLoadFinishListener,
				cacheModel, true);
	}

	public void startManager(HttpEntity entity) {
		if (null != mOnWebLoadListener) {
			mOnWebLoadListener.OnStart();
		}
		// mResourceLoader.startLoadingResource(mUrl, mOnLoadFinishListener);
		mResourceLoader.startLoadingResource(mUrl, mOnLoadFinishListener,
				entity);
	}

	public void startManager(HttpEntity entity, long timeout) {
		if (null != mOnWebLoadListener) {
			mOnWebLoadListener.OnStart();
		}
		// mResourceLoader.startLoadingResource(mUrl, mOnLoadFinishListener);
		mResourceLoader.startLoadingResource(mUrl, mOnLoadFinishListener,
				entity, timeout);
	}

	public void setManagerListener(OnWebLoadListener<T> onWebLoadListener) {
		mOnWebLoadListener = onWebLoadListener;
	}

	private OnLoadFinishListener mOnLoadFinishListener = new OnLoadFinishListener() {
		@Override
		public void onLoadFinish(byte[] data, int length, String url,
				int requestId, int statusCode) {
			setLoadCompleteListener(data, length, statusCode);
		}
	};

	/**
	 * @param statusCode
	 */
	protected void setLoadCompleteListener(byte[] data, int length,
			int statusCode) {
		if (statusCode == OnLoadFinishListener.OK) {
			stateOKPaser(data);
		}
		if (null != mOnWebLoadListener) {
			mOnWebLoadListener.OnLoadComplete(statusCode);
		}
	}

	/**
	 * @param data
	 */
	private void stateOKPaser(byte[] data) {
		if (mEncoding == null) {
			String json = new String(data);
			startJsonPaser(json);
		} else {
			String json;
			try {
				json = new String(data, mEncoding);
				startJsonPaser(json);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param jsonObject
	 */
	protected void startJsonPaser(String json) {
		new JSONPaserTask().execute(json);
	}

	private class JSONPaserTask extends AsyncTask<String, Void, T> {

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		@Override
		protected T doInBackground(String... params) {
			try {
				return paserJSON(params[0]);
			} catch (Exception e) {
				Log.i("callstats", params[0]);
				mOnWebLoadListener.OnCancel();
				return null;
			}

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(T result) {
			mOnWebLoadListener.OnPaserComplete(result);
			super.onPostExecute(result);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onCancelled()
		 */
		@Override
		protected void onCancelled() {
			mOnWebLoadListener.OnCancel();
			super.onCancelled();
		}

	}

	/**
	 * @param jsonObject
	 * @return
	 */
	protected abstract T paserJSON(String json);

}
