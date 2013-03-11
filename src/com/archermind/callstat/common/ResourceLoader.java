package com.archermind.callstat.common;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;

import android.content.Context;
import android.os.Message;

import com.archermind.callstat.common.net.Request;
import com.archermind.callstat.common.net.RequestQueue;

public class ResourceLoader {

	private static final int HTTP_POST_METHOD = 1;
	private static final int HTTP_GET_METHOD = 2;

	/**
	 * Default cache usage pattern
	 */
	public static final int LOAD_DEFAULT = -1;

	/**
	 * Normal cache usage pattern
	 */
	public static final int LOAD_NORMAL = 0;

	/**
	 * Use cache if content is there, even if expired (eg, history nav) If it is
	 * not in the cache, load from network.
	 */
	public static final int LOAD_CACHE_ELSE_NETWORK = 1;

	/**
	 * Don't use the cache, load from network
	 */
	public static final int LOAD_NO_CACHE = 2;

	/**
	 * Don't use the network, load from cache only.
	 */
	public static final int LOAD_CACHE_ONLY = 3;

	private final Context mContext;

	/**
	 * Static instance of a ResourceLoader object.
	 */
	private static ResourceLoader sResourceLoader;

	/**
	 * ResourceLoader request queue (requests are added from the Client Core
	 * Thread).
	 */
	private RequestQueue mRequestQueue;

	// Id generator
	private static int mIDGen = 0;

	/**
	 * @return The singleton instance of the ResourceLoader.
	 */
	public static synchronized ResourceLoader getInstance(Context context) {
		if (sResourceLoader == null) {
			sResourceLoader = new ResourceLoader(
					context.getApplicationContext());
			// as CacheManager can behave based on database transaction, we need
			// to
			// call tick() to trigger endTransaction
			// CacheManager.init(context.getApplicationContext());
			ClientCoreWorker.getHandler().removeMessages(
					ClientCoreWorker.MSG_CACHE_TRANSACTION_TICKER);
			ClientCoreWorker.getHandler().sendEmptyMessage(
					ClientCoreWorker.MSG_CACHE_TRANSACTION_TICKER);
		}
		return sResourceLoader;
	}

	/**
	 * Creates a new ResourceLoader object. XXX: Must be created in the same
	 * thread as UI thread!!!!!
	 */
	private ResourceLoader(Context context) {
		mRequestQueue = new RequestQueue(context);
		mContext = context;
	}

	/**
	 * @param url
	 * @param loader
	 * @return
	 */
	public int startLoadingResource(String url, OnLoadFinishListener loader) {

		return startLoadingResource(url, HTTP_GET_METHOD, null, null, 0,
				LOAD_NORMAL, false, false, loader);
	}

	public int startLoadingResource(String url, OnLoadFinishListener loader,
			long timeout) {

		return startLoadingResource(url, HTTP_GET_METHOD, null, null, 0,
				LOAD_NORMAL, false, false, loader, timeout);
	}

	public int startLoadingResource(String url, OnLoadFinishListener loader,
			int cacheMode) {

		return startLoadingResource(url, HTTP_GET_METHOD, null, null, 0,
				cacheMode, false, false, loader);
	}

	public int startLoadingResource(String url, OnLoadFinishListener loader,
			HttpEntity entity) {
		return startLoadingResource(url, HTTP_POST_METHOD, null, entity, 0,
				LOAD_NO_CACHE, false, true, loader);
	}

	public int startLoadingResource(String url, OnLoadFinishListener loader,
			HttpEntity entity, long timeout) {
		return startLoadingResource(url, HTTP_POST_METHOD, null, entity, 0,
				LOAD_NO_CACHE, false, true, loader, timeout);
	}

	/**
	 * @param url
	 * @param loader
	 * @param cacheMode
	 * @param head
	 *            if head set ture and this request will set first
	 * @return
	 */
	public int startLoadingResource(String url, OnLoadFinishListener loader,
			int cacheMode, boolean head) {

		return startLoadingResource(url, HTTP_GET_METHOD, null, null, 0,
				cacheMode, false, head, loader);
	}

	/**
	 * Start loading a resource.
	 * 
	 * @param url
	 *            The url to load.
	 * @param method
	 *            The http method.
	 * @param headers
	 *            The http headers.
	 * @param postData
	 *            If the method is "POST" postData is sent as the request body.
	 *            Is null when empty.
	 * @param postDataIdentifier
	 *            If the post data contained form this is the form identifier,
	 *            otherwise it is 0.
	 * @param cacheMode
	 *            The cache mode to use when loading this resource. See
	 *            WebSettings.setCacheMode
	 * @param requestId
	 *            The id of the activity that request the resource
	 * @param synchronous
	 *            True if the load is synchronous.
	 * @param loader
	 * @param timeout
	 * @return The request id, or -1 if error.
	 */
	public int startLoadingResource(String url, int method,
			Map<String, String> headers, HttpEntity entity,
			long postDataIdentifier, int cacheMode, boolean synchronous,
			boolean head, OnLoadFinishListener loader, long timeout) {

		// Create a LoadListener
		LoadListener loadListener = LoadListener.getLoadListener(mContext, url,
				loader, synchronous);

		if (URLUtil.isNetworkUrl(url)) {
			/* Create and queue request */
			Request req;
			// set up request
			if (headers == null) {
				headers = new HashMap<String, String>();
			}
			int requestId = mIDGen++;
			req = new Request(method, url, entity, loadListener, headers,
					synchronous, requestId, cacheMode, timeout);
			loadListener.attachRequest(req);
			HashMap<String, Object> map = new HashMap();
			map.put("loadListener", loadListener);
			map.put("cacheMode", cacheMode);
			map.put("request", req);
			map.put("resourceLoader", this);
			map.put("head", head);
			// ClientCoreWorker.getHandler().obtainMessage(
			// ClientCoreWorker.MSG_ADD_HTTPLOADER, map).sendToTarget();

			Message msg = ClientCoreWorker.getHandler().obtainMessage(
					ClientCoreWorker.MSG_ADD_HTTPLOADER);
			msg.obj = map;
			ClientCoreWorker.getHandler().sendMessage(msg);
			return requestId;
		} else if (URLUtil.isFileUrl(url)) {
			return handleLocalFile(url, synchronous, loader);
		}
		/*
		 * else if (loader != null) { loader.onLoadFinish(null, 0, url, -1,
		 * OnLoadFinishListener.ERROR_BAD_URL); }
		 */
		return -1;
	}

	/**
	 * Start loading a resource.
	 * 
	 * @param url
	 *            The url to load.
	 * @param method
	 *            The http method.
	 * @param headers
	 *            The http headers.
	 * @param postData
	 *            If the method is "POST" postData is sent as the request body.
	 *            Is null when empty.
	 * @param postDataIdentifier
	 *            If the post data contained form this is the form identifier,
	 *            otherwise it is 0.
	 * @param cacheMode
	 *            The cache mode to use when loading this resource. See
	 *            WebSettings.setCacheMode
	 * @param requestId
	 *            The id of the activity that request the resource
	 * @param synchronous
	 *            True if the load is synchronous.
	 * @param loader
	 * @return The request id, or -1 if error.
	 */
	public int startLoadingResource(String url, int method,
			Map<String, String> headers, HttpEntity entity,
			long postDataIdentifier, int cacheMode, boolean synchronous,
			boolean head, OnLoadFinishListener loader) {

		// Create a LoadListener
		LoadListener loadListener = LoadListener.getLoadListener(mContext, url,
				loader, synchronous);

		if (URLUtil.isNetworkUrl(url)) {
			/* Create and queue request */
			Request req;
			// set up request
			if (headers == null) {
				headers = new HashMap<String, String>();
			}
			int requestId = mIDGen++;
			req = new Request(method, url, entity, loadListener, headers,
					synchronous, requestId, cacheMode);
			loadListener.attachRequest(req);
			HashMap<String, Object> map = new HashMap();
			map.put("loadListener", loadListener);
			map.put("cacheMode", cacheMode);
			map.put("request", req);
			map.put("resourceLoader", this);
			map.put("head", head);
			// ClientCoreWorker.getHandler().obtainMessage(
			// ClientCoreWorker.MSG_ADD_HTTPLOADER, map).sendToTarget();

			Message msg = ClientCoreWorker.getHandler().obtainMessage(
					ClientCoreWorker.MSG_ADD_HTTPLOADER);
			msg.obj = map;
			ClientCoreWorker.getHandler().sendMessage(msg);
			return requestId;
		} else if (URLUtil.isFileUrl(url)) {
			return handleLocalFile(url, synchronous, loader);
		}
		/*
		 * else if (loader != null) { loader.onLoadFinish(null, 0, url, -1,
		 * OnLoadFinishListener.ERROR_BAD_URL); }
		 */
		return -1;
	}

	public void stopAllLoadingResource() {
		// Message msg = ClientCoreWorker.getHandler().obtainMessage(
		// ClientCoreWorker.MSG_STOP_HTTPLOAD, -1, 0, this);
		mRequestQueue.removeAllRequest();
	}

	public void stopLoadingResource(int requestId) {
		// Message msg = ClientCoreWorker.getHandler().obtainMessage(
		// ClientCoreWorker.MSG_STOP_HTTPLOAD, requestId, 0, this);
		mRequestQueue.removeRequest(requestId);
	}

	int handleLocalFile(String url, boolean synchronous,
			OnLoadFinishListener loader) {
		if (URLUtil.isAssetUrl(url)) {
			if (synchronous) {
			} else {
			}
			return mIDGen++;
		} else if (URLUtil.isResourceUrl(url)) {
			if (synchronous) {
			} else {
			}
			return mIDGen++;
		} else if (URLUtil.isFileUrl(url)) {
			if (synchronous) {
			} else {
			}
			return mIDGen++;
		}
		return -1;
	}

	boolean handleHTTPLoad(Request request, int cacheMode,
			LoadListener listener, boolean head) {
		// response was handled by Cache, don't issue HTTP request
		populateStaticHeaders(request.getHeaders());
		if (handleCache(cacheMode, request, listener)) {
			return true;
		}
		if (request.isSynchronous()) {
			// TODO:ͬ������
		} else {
			mRequestQueue.queueRequest(request, head);
		}
		return true;
	}

	void handleStopHTTPLoad(int requestId) {
		mRequestQueue.removeRequest(requestId);
	}

	/*
	 * This function is used by the handleHTTPLoad to setup the cache headers
	 * correctly. Returns true if the response was handled from the cache
	 */
	private boolean handleCache(int cacheMode, Request request,
			LoadListener listener) {
		switch (cacheMode) {
		case LOAD_NO_CACHE:
			break;
		case LOAD_CACHE_ONLY:
			break;
		case LOAD_CACHE_ELSE_NETWORK:
			break;
		default:
		case LOAD_NORMAL:
			// /return listener.checkCache(request.getHeaders());
			// return
			// ((LoadListener)(request.mEventHandler)).checkCache(request.mHeaders);
			break;
		}
		return false;
	}

	/**
	 * Add the static headers that don't change with each request.
	 */
	private void populateStaticHeaders(Map<String, String> headers) {

	}

	public interface OnLoadFinishListener {

		public static final int OK = 0;

		public static final int ERROR_BAD_URL = -1;

		public static final int ERROR_REQUEST_FAILED = -2;

		public static final int ERROR_TIMEOUT = -3;

		public static final int ERROR_REQ_REFUSED = -4;

		public void onLoadFinish(byte[] data, int length, String url,
				int requestId, int statusCode);
	}
}