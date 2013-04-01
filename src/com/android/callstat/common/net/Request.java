package com.android.callstat.common.net;

import java.util.Map;

import org.apache.http.HttpEntity;

/**
 * Represents an HTTP request for a given id.
 */
public class Request {

	private HttpEntity mHttpEntity;
	private int mMethod;
	private String mUri;
	private EventHandler mEventHandler;
	private Map<String, String> mHeaders;
	private boolean mSynchronous;
	private int mRequestId;
	private int mCacheMode;
	private long mTimeout = 15000;

	/**
	 * Instantiates a new Request.
	 * 
	 * @param method
	 *            HTTP_POST_METHOD/HTTP_GET_METHOD
	 * @param url
	 *            The url to load.
	 * @param postData
	 *            If the method is "POST" postData is sent as the request body.
	 *            Is null when empty.
	 * @param userAgent
	 *            The user agent provided by the initiating app
	 * @param ResourceLoadedListener
	 *            request will make loading callbacks on this interface
	 * @param headers
	 *            reqeust headers
	 */
	public Request(int method, String uri, HttpEntity entity,
			EventHandler eventHandler, Map<String, String> headers,
			boolean synchronous, int requestId, int cacheMode, long timeout) {
		mMethod = method;
		mUri = uri;
		mHttpEntity = entity;
		mEventHandler = eventHandler;
		mHeaders = headers;
		mSynchronous = synchronous;
		mRequestId = requestId;
		mCacheMode = cacheMode;
		mTimeout = timeout;
	}

	public Request(int method, String uri, HttpEntity entity,
			EventHandler eventHandler, Map<String, String> headers,
			boolean synchronous, int requestId, int cacheMode) {
		mMethod = method;
		mUri = uri;
		mHttpEntity = entity;
		mEventHandler = eventHandler;
		mHeaders = headers;
		mSynchronous = synchronous;
		mRequestId = requestId;
		mCacheMode = cacheMode;
	}

	public long getmTimeout() {
		return mTimeout;
	}

	public void setmTimeout(long mTimeout) {
		this.mTimeout = mTimeout;
	}

	public void setMethod(int method) {
		mMethod = method;
	}

	public int getMethod() {
		return mMethod;
	}

	public void setUri(String uri) {
		mUri = uri;
	}

	public String getUri() {
		return mUri;
	}

	public void setHttpEntity(HttpEntity entity) {
		mHttpEntity = entity;
	}

	public HttpEntity getHttpEntity() {
		return mHttpEntity;
	}

	public void setEventHandler(EventHandler eventHandler) {
		mEventHandler = eventHandler;
	}

	public EventHandler getEventHandler() {
		return mEventHandler;
	}

	public void setHeaders(Map<String, String> headers) {
		mHeaders = headers;
	}

	public Map<String, String> getHeaders() {
		return mHeaders;
	}

	public void setSynchronous(boolean synchronous) {
		mSynchronous = synchronous;
	}

	public boolean getSynchronous() {
		return mSynchronous;
	}

	public void setRequestId(int requestId) {
		mRequestId = requestId;
	}

	public int getRequestId() {
		return mRequestId;
	}

	public void setCacheMode(int cacheMode) {
		mCacheMode = cacheMode;
	}

	public int getCacheMode() {
		return mCacheMode;
	}

	public boolean isSynchronous() {
		return mSynchronous;
	}

}