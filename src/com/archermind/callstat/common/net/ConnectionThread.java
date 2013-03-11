/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.archermind.callstat.common.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.content.Context;
import android.os.Process;
import android.os.SystemClock;

import com.archermind.callstat.CallStatApplication;
import com.archermind.callstat.DebugFlags;
import com.archermind.callstat.common.CallStatUtils;
import com.archermind.callstat.common.StringUtil;

/**
 * Runs an actual download
 */
public class ConnectionThread extends Thread {

	private Context mContext;
	static final int WAIT_TIMEOUT = 5000;
	static final int WAIT_TICK = 1000;
	private static final int MIN_GZIP_SIZE = 512;

	// Performance probe
	long mCurrentThreadTime;
	long mTotalThreadTime;

	private volatile boolean mRunning = true;
	// private volatile boolean mCancaled = false;
	// private int mCancalExcGroupId;

	public static final int HTTP_POST_METHOD = 1;
	public static final int HTTP_GET_METHOD = 2;
	private SystemFacade mSystemFacade;
	private RequestFeeder mRequestFeeder;

	public ConnectionThread(Context context, int id, SystemFacade systemFacade,
			RequestFeeder requestFeeder) {
		super();
		mContext = context;
		setName("http-thread-" + id);
		mSystemFacade = systemFacade;
		mRequestFeeder = requestFeeder;
	}

	void requestStop() {
		synchronized (mRequestFeeder) {
			mRunning = false;
			mRequestFeeder.notify();
		}
	}

	/**
	 * Loop until app shutdown.
	 */
	public void run() {
		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

		// these are used to get performance data. When it is not in the timing,
		// mCurrentThreadTime is 0. When it starts timing, mCurrentThreadTime is
		// first set to -1, it will be set to the current thread time when the
		// next request starts.
		mCurrentThreadTime = 0;
		mTotalThreadTime = 0;

		MyHttpClient client = null;
		client = createHttpClient(mContext);

		while (mRunning) {
			if (mCurrentThreadTime == -1) {
				mCurrentThreadTime = SystemClock.currentThreadTimeMillis();
			}

			Request request;

			// mCancaled = false;
			/* Get a request to process */
			request = mRequestFeeder.getRequest();

			/* wait for work */
			if (request == null) {
				synchronized (mRequestFeeder) {
					if (DebugFlags.CONNECTION_THREAD)
						HttpLog.v("ConnectionThread: Waiting for work");
					try {
						mRequestFeeder.wait();
					} catch (InterruptedException e) {
					}
					if (mCurrentThreadTime != 0) {
						mCurrentThreadTime = SystemClock
								.currentThreadTimeMillis();
					}
				}
			} else {
				if (DebugFlags.CONNECTION_THREAD) {
					// HttpLog.v("ConnectionThread: new request " +
					// request.mHost + " " + request );
				}
				try {
					httpConnection(mContext, false, client, null, 0, request);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		if (client != null) {
			client.close();
		}
	}

	/**
	 * A helper method to send or retrieve data through HTTP protocol.
	 * 
	 * @param token
	 *            The token to identify the sending progress.
	 * @param url
	 *            The URL used in a GET request. Null when the method is
	 *            HTTP_POST_METHOD.
	 * @param pdu
	 *            The data to be POST. Null when the method is HTTP_GET_METHOD.
	 * @param method
	 *            HTTP_POST_METHOD or HTTP_GET_METHOD.
	 * @return A byte array which contains the response data. If an HTTP error
	 *         code is returned, an IOException will be thrown.
	 * @throws IOException
	 *             if any error occurred on network interface or an HTTP error
	 *             code(&gt;=400) returned from the server.
	 */
	private byte[] httpConnection(Context context, boolean isProxySet,
			MyHttpClient client, String proxyHost, int proxyPort,
			Request request) throws IOException {
		if (request.getUri() == null) {
			throw new IllegalArgumentException("URL must not be null.");
		}
		int timeout = (int) request.getmTimeout();
		if (timeout != 0 && timeout < 5000) {
			timeout = 5000;
		}
		client.getParams().setParameter(
				CoreConnectionPNames.CONNECTION_TIMEOUT, timeout);
		client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT,
				timeout);
		try {
			URI hostUrl = new URI(request.getUri());
			HttpHost target = new HttpHost(hostUrl.getHost(),
					hostUrl.getPort(), HttpHost.DEFAULT_SCHEME_NAME);
			HttpLog.d("URL:" + request.getUri() + " Host:" + hostUrl.getHost()
					+ " Port:" + hostUrl.getPort());

			HttpRequest req = null;
			switch (request.getMethod()) {
			case HTTP_POST_METHOD:
				HttpPost post = new HttpPost(request.getUri());
				HttpEntity content = request.getHttpEntity();
				if (content != null) {
					post.setEntity(content);
					if (content instanceof StringEntity) {
						final StringEntity stringEntity = (StringEntity) content;
						post.setHeader(stringEntity.getContentEncoding());
						post.setHeader(stringEntity.getContentType());
					}
				}
				req = post;
				break;
			case HTTP_GET_METHOD:
				req = new HttpGet(request.getUri());
				break;
			default:
				HttpLog.e("Unknown HTTP method: " + request.getMethod()
						+ ". Must be one of POST[" + HTTP_POST_METHOD
						+ "] or GET[" + HTTP_GET_METHOD + "].");
				return null;
			}
			HttpResponse response = null;
			if (CallStatUtils.isOMS()) {
				setRequest(response, isProxySet, req, client, proxyHost,
						proxyPort, request);
				response = client.execute(target, req);
			} else {
				if (CallStatUtils.isMOBILE(mContext)) {
					String apn = CallStatUtils.getAPN(mContext);
					if (!StringUtil.isNullOrWhitespaces(apn)) {
						if (apn.equals("CMWAP")) {
							HttpClient httpclient = new DefaultHttpClient();
							HttpHost proxy = new HttpHost("10.0.0.172", 80,
									"http");
							httpclient.getParams().setParameter(
									ConnRoutePNames.DEFAULT_PROXY, proxy);
							response = httpclient.execute(target, req);
						} else {
							setRequest(response, isProxySet, req, client,
									proxyHost, proxyPort, request);
							response = client.execute(target, req);
						}
					} else {
						setRequest(response, isProxySet, req, client,
								proxyHost, proxyPort, request);
						response = client.execute(target, req);
					}
				} else {
					setRequest(response, isProxySet, req, client, proxyHost,
							proxyPort, request);
					response = client.execute(target, req);
				}

			}
			StatusLine status = response.getStatusLine();
			request.getEventHandler().status(
					status.getProtocolVersion().getMajor(),
					status.getProtocolVersion().getMinor(),
					status.getStatusCode(), status.getReasonPhrase());
			switch (status.getStatusCode()) {
			case 200:
				break;
			case 304:
				request.getEventHandler().endData(null, 0);
				return null;
			default:
				request.getEventHandler().endData(null, 0);
				throw new IOException("HTTP error: " + status.getReasonPhrase()
						+ " CODE:" + status.getStatusCode());
			}
			Headers headers = new Headers();
			readResponseHeaders(headers, response);
			request.getEventHandler().headers(headers);

			HttpEntity entity = response.getEntity();
			byte[] body = null;
			if (entity != null) {
				try {
					int contentLength = (int) entity.getContentLength();
					if (contentLength > 0) {
						body = new byte[contentLength];
						// DataInputStream dis = new
						// DataInputStream(entity.getContent());
						InputStream in = entity.getContent();
						int offset = 0;
						int length = contentLength;
						try {
							while (length > 0) {
								int result = in.read(body, offset, length);
								HttpLog.v("################result:" + result);
								offset += result;
								length -= result;
								if (length <= 0) {
									request.getEventHandler().endData(body,
											contentLength);
								}
							}
						} finally {
							try {
								in.close();
								// request.mLoadListener.loaded(body,
								// contentLength);
								// if(length == 0)
								// CallbackProxy.getHandler().onFinishResourceLoading(body,
								// contentLength, request.mLoadListener);
							} catch (IOException e) {
								HttpLog.e("Error closing input stream: "
										+ e.getMessage());
							}
						}
					} else {
						ByteArrayBuilder dataBuilder = new ByteArrayBuilder();
						body = new byte[8192];
						InputStream in = entity.getContent();
						int result = 0;
						int count = 0;
						int lowWater = body.length / 2;
						try {
							while (result != -1) {
								result = in.read(body, count, body.length
										- count);
								if (result != -1) {
									HttpLog.v("################result:"
											+ result);
									count += result;
								}
								if (result == -1 || count >= lowWater) {
									dataBuilder.append(body, 0, count);
									count = 0;
								}
								if (result == -1) {
									if (dataBuilder.getByteSize() > 0) {
										byte[] cert = new byte[dataBuilder
												.getByteSize()];
										int offset = 0;
										while (true) {
											ByteArrayBuilder.Chunk c = dataBuilder
													.getFirstChunk();
											if (c == null)
												break;
											if (c.mLength != 0) {
												System.arraycopy(c.mArray, 0,
														cert, offset, c.mLength);
												offset += c.mLength;
											}
											c.release();
										}
										request.getEventHandler().endData(cert,
												cert.length);
									}
								}
							}
						} finally {
							try {
								in.close();
							} catch (IOException e) {
								HttpLog.e("Error closing input stream: "
										+ e.getMessage());
							}
						}

					}
				} finally {
					if (entity != null) {
						entity.consumeContent();
					}
				}
			}
			return body;
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			request.getEventHandler().error(EventHandler.ERROR_BAD_URL,
					e.getMessage());
			e.printStackTrace();
		} catch (IllegalStateException e) {
			request.getEventHandler().error(EventHandler.ERROR, e.getMessage());
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			request.getEventHandler().error(EventHandler.ERROR_BAD_URL,
					e.getMessage());
			e.printStackTrace();
		} catch (SocketException e) {
			request.getEventHandler().error(EventHandler.ERROR, e.getMessage());
			e.printStackTrace();
		} catch (ConnectTimeoutException e) {
			request.getEventHandler().error(EventHandler.ERROR_TIMEOUT,
					e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			request.getEventHandler().error(EventHandler.ERROR, e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	void setRequest(HttpResponse response, boolean isProxySet, HttpRequest req,
			MyHttpClient client, String proxyHost, int proxyPort,
			Request request) {
		// Set route parameters for the request.
		HttpParams params = client.getParams();
		if (isProxySet) {
			ConnRouteParams.setDefaultProxy(params, new HttpHost(proxyHost,
					proxyPort));
		}
		req.setParams(params);
		// Set necessary HTTP headers
		addHeaders(req, request.getHeaders());
	}

	/**
	 * Read headers from the HTTP response and store them into Headers.
	 */
	private void readResponseHeaders(Headers headers, HttpResponse response) {
		Header[] http_headers = response.getAllHeaders();
		for (int i = 0; i < http_headers.length; i++) {
			headers.setHeader(http_headers[i]);
		}
	}

	/**
	 * Add header represented by given pair to request. Header will be formatted
	 * in request as "name: value\r\n".
	 * 
	 * @param name
	 *            of header
	 * @param value
	 *            of header
	 */
	void addHeader(HttpRequest req, String name, String value) {
		if (name == null) {
			String damage = "Null http header name";
			HttpLog.e(damage);
			throw new NullPointerException(damage);
		}
		if (value == null || value.length() == 0) {
			String damage = "Null or empty value for header \"" + name + "\"";
			HttpLog.e(damage);
			throw new RuntimeException(damage);
		}
		req.addHeader(name, value);
	}

	/**
	 * Add all headers in given map to this request. This is a helper method: it
	 * calls addHeader for each pair in the map.
	 */
	void addHeaders(HttpRequest req, Map<String, String> headers) {
		if (headers == null) {
			return;
		}

		Entry<String, String> entry;
		Iterator<Entry<String, String>> i = headers.entrySet().iterator();
		while (i.hasNext()) {
			entry = i.next();
			addHeader(req, entry.getKey(), entry.getValue());
		}
	}

	private static MyHttpClient createHttpClient(Context context) {
		String userAgent = CallStatApplication.DEFAULT_USER_AGENT;

		MyHttpClient client = MyHttpClient.newInstance(userAgent, context);
		HttpParams params = client.getParams();
		HttpProtocolParams.setContentCharset(params, "UTF-8");
		// HttpClientParams.setRedirecting(params, true);
		// set the socket timeout
		int soTimeout = CallStatApplication.HTTP_SOCKET_TIMEOUT;

		if (DebugFlags.CONNECTION_THREAD) {
			HttpLog.v("[HttpUtils] createHttpClient w/ socket timeout "
					+ soTimeout + " ms, " + ", UA=" + userAgent);
		}

		HttpConnectionParams.setSoTimeout(params, soTimeout);
		return client;
	}

}
