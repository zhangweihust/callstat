package com.archermind.callstat.common;

import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.CacheManager.CacheResult;

import com.archermind.callstat.DebugFlags;
import com.archermind.callstat.common.ResourceLoader.OnLoadFinishListener;
import com.archermind.callstat.common.net.ConnectionThread;
import com.archermind.callstat.common.net.EventHandler;
import com.archermind.callstat.common.net.Headers;
import com.archermind.callstat.common.net.Request;

class LoadListener extends Handler implements EventHandler {

	// Messages used internally to communicate state between the
	// Network thread and the WebCore thread.
	private static final int MSG_CONTENT_HEADERS = 100;
	private static final int MSG_CONTENT_DATA = 110;
	private static final int MSG_CONTENT_FINISHED = 120;
	private static final int MSG_CONTENT_ERROR = 130;
	private static final int MSG_LOCATION_CHANGED = 140;
	private static final int MSG_LOCATION_CHANGED_REQUEST = 150;
	private static final int MSG_STATUS = 160;
	private static final int MSG_SSL_CERTIFICATE = 170;
	private static final int MSG_SSL_ERROR = 180;

	// Standard HTTP status codes in a more representative format
	private static final int HTTP_OK = 200;
	private static final int HTTP_PARTIAL_CONTENT = 206;
	private static final int HTTP_MOVED_PERMANENTLY = 301;
	private static final int HTTP_FOUND = 302;
	private static final int HTTP_SEE_OTHER = 303;
	private static final int HTTP_NOT_MODIFIED = 304;
	private static final int HTTP_TEMPORARY_REDIRECT = 307;
	private static final int HTTP_AUTH = 401;
	private static final int HTTP_NOT_FOUND = 404;
	private static final int HTTP_PROXY_AUTH = 407;

	private String mUrl;
	private Context mContext;
	private String mMimeType;
	public long mContentLength; // WContent length of the incoming data
	private boolean mCancelled; // The request has been cancelled.
	private boolean mFromCache;
	private CacheResult mCacheResult = null;
	private int mStatusCode;
	private String mStatusText;
	private OnLoadFinishListener mOnLoadFinishListener;
	private Request mRequest;
	ClientCoreWorker.CacheData mCacheData;

	// Flag to indicate that this load is synchronous.
	private boolean mSynchronous;
	private Vector<Message> mMessageQueue;

	private Headers mHeaders;

	// =========================================================================
	// Public functions
	// =========================================================================

	public static LoadListener getLoadListener(Context context, String url,
			OnLoadFinishListener loader, boolean synchronous) {

		return new LoadListener(context, url, loader, synchronous);
	}

	LoadListener(Context context, String url, OnLoadFinishListener loader,
			boolean synchronous) {
		if (DebugFlags.LOAD_LISTENER) {
			Log.v(DebugFlags.LOGTAG, "LoadListener constructor url=" + url);
		}
		mContext = context;
		mOnLoadFinishListener = loader;
		mUrl = url;
		mSynchronous = synchronous;
		if (synchronous) {
			mMessageQueue = new Vector<Message>();
		}
	}

	void attachRequest(Request req) {
		if (DebugFlags.LOAD_LISTENER) {
			Log.v(DebugFlags.LOGTAG, "LoadListener.attachRequestHandle(): "
					+ "requestHandle: " + req);
		}
		mRequest = req;
	}

	void detachRequestHandle() {
		if (DebugFlags.LOAD_LISTENER) {
			Log.v(DebugFlags.LOGTAG, "LoadListener.detachRequestHandle(): "
					+ "requestHandle: " + mRequest);
		}
		mRequest = null;
	}

	/*
	 * This message handler is to facilitate communication between the network
	 * thread and the browser thread.
	 */
	@Override
	public void handleMessage(Message msg) {
		switch (msg.what) {
		case MSG_CONTENT_HEADERS:
			/*
			 * This message is sent when the LoadListener has headers available.
			 * The headers are sent onto WebCore to see what we should do with
			 * them.
			 */
			handleHeaders((Headers) msg.obj);
			break;
		case MSG_CONTENT_FINISHED:
			/*
			 * This message is sent when the LoadListener knows that the load is
			 * finished. This message is not sent in the case of an error.
			 */
			HashMap map = (HashMap) msg.obj;
			byte[] data = (byte[]) map.get("data");
			int length = ((Integer) map.get("length")).intValue();
			handleEndData(data, length);
			break;
		case MSG_CONTENT_ERROR:
			/*
			 * This message is sent when a load error has occured. The
			 * LoadListener will clean itself up.
			 */
			handleError(msg.arg1, (String) msg.obj);
			break;
		case MSG_STATUS:
			/*
			 * This message is sent from the network thread when the http stack
			 * has received the status response from the server.
			 */
			HashMap status = (HashMap) msg.obj;
			handleStatus(((Integer) status.get("major")).intValue(),
					((Integer) status.get("minor")).intValue(),
					((Integer) status.get("code")).intValue(),
					(String) status.get("reason"));
			break;
		}
	}

	Context getContext() {
		return mContext;
	}

	/* package */boolean isSynchronous() {
		return mSynchronous;
	}

	/**
	 * @return True iff the load has been cancelled
	 */
	public boolean cancelled() {
		return mCancelled;
	}

	/**
	 * Parse the headers sent from the server.
	 * 
	 * @param headers
	 *            gives up the HeaderGroup IMPORTANT: as this is called from
	 *            network thread, can't call native directly
	 */
	public void headers(Headers headers) {
		if (DebugFlags.LOAD_LISTENER)
			Log.v(DebugFlags.LOGTAG, "LoadListener.headers");
		// call db (setCookie) in the non-WebCore thread
		if (mCancelled)
			return;
		sendMessageInternal(obtainMessage(MSG_CONTENT_HEADERS, headers));
	}

	// Does the header parsing work on the WebCore thread.
	private void handleHeaders(Headers headers) {

		mHeaders = headers;
		long contentLength = headers.getContentLength();

		if (contentLength != Headers.NO_CONTENT_LENGTH) {
			mContentLength = contentLength;
		} else {
			mContentLength = 0;
		}

		String contentType = headers.getContentType();
		if (contentType != null) {
			parseContentTypeHeader(contentType);

			// If we have one of "generic" MIME types, try to deduce
			// the right MIME type from the file extension (if any):

		} else {
			/*
			 * Often when servers respond with 304 Not Modified or a Redirect,
			 * then they don't specify a MIMEType. When this occurs, the
			 * function below is called. In the case of 304 Not Modified, the
			 * cached headers are used rather than the headers that are returned
			 * from the server.
			 */
			guessMimeType();
		}
		if (mStatusCode == HTTP_OK && mRequest != null
				&& mRequest.getMethod() != ConnectionThread.HTTP_POST_METHOD) {
			mCacheData = new ClientCoreWorker.CacheData();
			mCacheData.mUrl = mUrl;
			mCacheData.mMimeType = mMimeType;
			mCacheData.mHeaders = headers;
		}
	}

	/**
	 * Report the status of the response. TODO: Comments about each parameter.
	 * IMPORTANT: as this is called from network thread, can't call native
	 * directly
	 */
	public void status(int majorVersion, int minorVersion, int code, /*
																	 * Status-Code
																	 * value
																	 */
	String reasonPhrase) {
		if (DebugFlags.LOAD_LISTENER) {
			Log.v(DebugFlags.LOGTAG, "LoadListener: from: " + mUrl + " major: "
					+ majorVersion + " minor: " + minorVersion + " code: "
					+ code + " reason: " + reasonPhrase);
		}
		HashMap status = new HashMap();
		status.put("major", majorVersion);
		status.put("minor", minorVersion);
		status.put("code", code);
		status.put("reason", reasonPhrase);
		sendMessageInternal(obtainMessage(MSG_STATUS, status));
	}

	// Handle the status callback on the WebCore thread.
	private void handleStatus(int major, int minor, int code, String reason) {
		if (mCancelled)
			return;

		mStatusCode = code;
		mStatusText = reason;
	}

	/**
	 * Implementation of error handler for EventHandler. Subclasses should call
	 * this method to have error fields set.
	 * 
	 * @param id
	 *            The error id described by EventHandler.
	 * @param description
	 *            A string description of the error. IMPORTANT: as this is
	 *            called from network thread, can't call native directly
	 */
	public void error(int id, String description) {
		if (DebugFlags.LOAD_LISTENER) {
			Log.v(DebugFlags.LOGTAG, "LoadListener.error url:" + mUrl + " id:"
					+ id + " description:" + description);
		}
		sendMessageInternal(obtainMessage(MSG_CONTENT_ERROR, id, 0, description));
	}

	// Handle the error on the WebCore thread.
	private void handleError(int id, String description) {
		int status = -1;

		switch (id) {
		case ERROR_BAD_URL:
			status = OnLoadFinishListener.ERROR_BAD_URL;
			break;
		case ERROR_TIMEOUT:
			status = OnLoadFinishListener.ERROR_TIMEOUT;
			break;
		case ERROR:
			status = OnLoadFinishListener.ERROR_REQ_REFUSED;
			break;
		default:
			status = OnLoadFinishListener.ERROR_REQUEST_FAILED;
			break;
		}

		mOnLoadFinishListener.onLoadFinish(null, 0, mUrl,
				mRequest.getRequestId(), status);
	}

	/**
	 * Event handler's endData call. Send a message to the handler notifying
	 * them that the data has finished. IMPORTANT: as this is called from
	 * network thread, can't call native directly
	 */
	public void endData(byte[] data, int length) {
		if (DebugFlags.LOAD_LISTENER) {
			Log.v(DebugFlags.LOGTAG, "LoadListener.endData(): url: " + mUrl);
		}
		HashMap map = new HashMap();
		map.put("data", data);
		map.put("length", new Integer(length));
		sendMessageInternal(obtainMessage(MSG_CONTENT_FINISHED, map));
		if (mStatusCode == HTTP_OK
				&& mRequest.getCacheMode() != ResourceLoader.LOAD_NO_CACHE) {
			mCacheData.mData = data;
			mCacheData.mDataLength = length;
			ClientCoreWorker
					.getHandler()
					.obtainMessage(ClientCoreWorker.MSG_CREATE_CACHE,
							mCacheData).sendToTarget();
		}
	}

	// Handle the end of data.
	private void handleEndData(byte[] data, int length) {
		if (mCancelled || mOnLoadFinishListener == null)
			return;
		int status;
		switch (mStatusCode) {
		case HTTP_OK:
			status = OnLoadFinishListener.OK;
			Log.v(DebugFlags.LOGTAG, "&&&&&&&&:fromnetwork url: " + mUrl
					+ " length: " + length);
			break;
		case HTTP_NOT_MODIFIED:
			/*
			 * data = mCacheResult.getContentBlog(); length =
			 * mCacheResult.getContentLength();
			 */
			status = OnLoadFinishListener.OK;
			break;
		default:
			if (mFromCache) {
				Log.v(DebugFlags.LOGTAG, "&&&&&&&&:fromcache   url: " + mUrl
						+ " length: " + length);
				status = OnLoadFinishListener.OK;
				break;
			}
			data = null;
			length = 0;
			status = OnLoadFinishListener.ERROR_REQUEST_FAILED;
			break;
		}
		mOnLoadFinishListener.onLoadFinish(data, length, mUrl,
				mRequest.getRequestId(), status);
	}

	/**
	 * Either send a message to ourselves or queue the message if this is a
	 * synchronous load.
	 */
	private void sendMessageInternal(Message msg) {
		if (mSynchronous) {
			mMessageQueue.add(msg);
		} else {
			sendMessage(msg);
		}
	}

	/**
	 * Check the cache for the current URL, and load it if it is valid.
	 * 
	 * @param headers
	 *            for the request
	 * @return true if cached response is used.
	 */
	/*
	 * boolean checkCache(Map<String, String> headers) { if
	 * (DebugFlags.LOAD_LISTENER) { Log.v(DebugFlags.LOGTAG,
	 * "LoadListener.checkCache"); }
	 * 
	 * // Get the cache file name for the current URL //mCacheResult =
	 * CacheManager.getCache(mUrl, headers); // reset the flag mFromCache =
	 * false; if (mCacheResult != null) { // If I got a cachedUrl and the
	 * revalidation header was not // added, then the cached content valid, we
	 * should use it. if
	 * (!headers.containsKey(CacheManager.HEADER_KEY_IFNONEMATCH) && !headers
	 * .containsKey(CacheManager.HEADER_KEY_IFMODIFIEDSINCE)) { if
	 * (DebugFlags.LOAD_LISTENER) { Log.v(DebugFlags.LOGTAG,
	 * "LoadListener: HTTP URL in cache " + "and usable: " + mUrl); } mFromCache
	 * = true; endData(mCacheResult.getContentBlog(),
	 * mCacheResult.getContentLength()); return true; } } return false; }
	 */

	/**
	 * Parses the content-type header. The first part only allows '-' if it
	 * follows x or X.
	 */
	private static final Pattern CONTENT_TYPE_PATTERN = Pattern
			.compile("^((?:[xX]-)?[a-zA-Z\\*]+/[\\w\\+\\*-]+[\\.[\\w\\+-]+]*)$");

	void parseContentTypeHeader(String contentType) {
		if (DebugFlags.LOAD_LISTENER) {
			Log.v(DebugFlags.LOGTAG, "LoadListener.parseContentTypeHeader: "
					+ "contentType: " + contentType);
		}

		if (contentType != null) {
			int i = contentType.indexOf(';');
			if (i >= 0) {
				mMimeType = contentType.substring(0, i);
			} else {
				mMimeType = contentType;
			}
			// Trim leading and trailing whitespace
			mMimeType = mMimeType.trim();

			try {
				Matcher m = CONTENT_TYPE_PATTERN.matcher(mMimeType);
				if (m.find()) {
					mMimeType = m.group(1);
				} else {
					guessMimeType();
				}
			} catch (IllegalStateException ex) {
				guessMimeType();
			}
		}
		mMimeType = mMimeType.toLowerCase();
	}

	/**
	 * Guesses MIME type if one was not specified. Defaults to 'text/html'. In
	 * addition, tries to guess the MIME type based on the extension.
	 * 
	 */
	private void guessMimeType() {
		// Note: This is ok because this is used only for the main content
		// of frames. If no content-type was specified, it is fine to
		// default to text/html.
		mMimeType = "text/html";
		String newMimeType = guessMimeTypeFromExtension(mUrl);
		if (newMimeType != null) {
			mMimeType = newMimeType;
		}
	}

	/**
	 * guess MIME type based on the file extension.
	 */
	private String guessMimeTypeFromExtension(String url) {
		// PENDING: need to normalize url
		if (DebugFlags.LOAD_LISTENER) {
			Log.v(DebugFlags.LOGTAG, "guessMimeTypeFromExtension: url = " + url);
		}
		String extension = getFileExtensionFromUrl(url);
		if (extension.equals("png") || extension.equals("jpg")
				|| extension.equals("jpeg") || extension.equals("gif")
				|| extension.equals("ico") || extension.equals("bmp")) {
			return "image/" + extension;
		}
		return null;
	}

	/**
	 * Returns the file extension or an empty string iff there is no extension.
	 * This method is a convenience method for obtaining the extension of a url
	 * and has undefined results for other Strings.
	 * 
	 * @param url
	 * @return The file extension of the given url.
	 */
	public static String getFileExtensionFromUrl(String url) {
		if (url != null && url.length() > 0) {
			int query = url.lastIndexOf('?');
			if (query > 0) {
				url = url.substring(0, query);
			}
			int filenamePos = url.lastIndexOf('/');
			String filename = 0 <= filenamePos ? url.substring(filenamePos + 1)
					: url;

			// if the filename contains special characters, we don't
			// consider it valid for our matching purposes:
			if (filename.length() > 0
					&& Pattern
							.matches("[a-zA-Z_0-9\\.\\-\\(\\)\\%]+", filename)) {
				int dotPos = filename.lastIndexOf('.');
				if (0 <= dotPos) {
					return filename.substring(dotPos + 1);
				}
			}
		}

		return "";
	}
}
