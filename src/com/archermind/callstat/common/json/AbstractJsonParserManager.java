package com.archermind.callstat.common.json;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.json.JSONException;

import android.content.Context;
import android.os.AsyncTask;

import com.archermind.callstat.common.ResourceLoader;
import com.archermind.callstat.common.ResourceLoader.OnLoadFinishListener;

/**
 * @author longX@archermind
 */
public abstract class AbstractJsonParserManager {

	private String mUrl;
	private OnListJsonParserListener onJsonParserListener;
	private ResourceLoader mResourceLoader;
	private LoadState mLoadState;

	public AbstractJsonParserManager() {
	}

	public AbstractJsonParserManager(Context context, String url) {
		mUrl = url;
		mResourceLoader = ResourceLoader.getInstance(context);
	}

	public void setOnJsonParserListener(
			OnListJsonParserListener onJsonParserListener) {
		this.onJsonParserListener = onJsonParserListener;
	}

	public void startJsonParser() {
		loadJson();
	}

	private void loadJson() {
		mResourceLoader.startLoadingResource(mUrl, new OnLoadFinishListener() {
			@Override
			public void onLoadFinish(byte[] data, int length, String url,
					int requestId, int statusCode) {
				setLoadCompleteListener(statusCode, data);
			}
		}, 2);
	}

	public void startPostJsonParser(HttpEntity entity) {
		mResourceLoader.startLoadingResource(mUrl, new OnLoadFinishListener() {
			@Override
			public void onLoadFinish(byte[] data, int length, String url,
					int requestId, int statusCode) {
				setLoadCompleteListener(statusCode, data);
			}
		}, entity);
	}

	/**
	 * @param statusCode
	 */
	protected void setLoadCompleteListener(int statusCode, byte[] data) {
		switch (statusCode) {
		case OnLoadFinishListener.OK:
			mLoadState = LoadState.OK;
			InputStream inputStream = new ByteArrayInputStream(data);
			startJsonPaserManger(inputStream);
			break;
		case OnLoadFinishListener.ERROR_BAD_URL:
			mLoadState = LoadState.BAD_URL;
			break;
		case OnLoadFinishListener.ERROR_REQUEST_FAILED:
			mLoadState = LoadState.REQUEST_FAILED;
			break;
		case OnLoadFinishListener.ERROR_TIMEOUT:
			mLoadState = LoadState.TIME_OUT;
			break;
		default:
			break;
		}
		if (null != onJsonParserListener) {
			onJsonParserListener.OnJsonLoadComplete(mLoadState);
		}
	}

	public void startJsonPaserManger(InputStream inputStream) {
		new paserJsonTask().execute(inputStream);
	}

	/**
	 * @author lx
	 * 
	 */
	class paserJsonTask extends AsyncTask<InputStream, Void, ArrayList> {

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		@Override
		protected ArrayList<Object> doInBackground(InputStream... params) {
			return paserJson(params[0]);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		protected void onPostExecute(ArrayList result) {

			if (null != result) {
				onJsonParserListener.OnJsonPasersComplete(result);
			}
			super.onPreExecute();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onCancelled()
		 */
		@Override
		protected void onCancelled() {
			super.onCancelled();
		}
	}

	private ArrayList paserJson(InputStream inputStream) {
		try {
			return parser(inputStream);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	protected abstract ArrayList parser(InputStream in) throws JSONException;

	// define interface
	public interface OnListJsonParserListener {
		public void OnJsonLoadComplete(LoadState loadState);

		public void OnJsonPasersComplete(ArrayList result);
	}
}
