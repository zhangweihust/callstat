package com.android.callstat.common.download;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.android.callstat.ConfigManager;
import com.android.callstat.ILog;
import com.android.callstat.common.CallStatUtils;
import com.android.callstat.common.StringUtil;
import com.android.callstat.common.net.MyHttpClient;

/**
 * @author lxue
 */
public class OfflineDownloadTask extends AsyncTask<String, Integer, Integer> {

	private static final int STATUS_SUCCESS = 0;
	private static final int STATUS_CANCELLED = 1;
	private static final int STATUS_NETWORK_ERROR = 2;
	private static final int STATUS_HTTP_DATA_ERROR = 3;
	private static final int STATUS_UNHANDLED_HTTP_CODE = 4;
	private static final int STATUS_FILE_ERROR = 5;
	private static final int STATUS_UNKNOWN_ERROR = 10;

	/** The buffer size used to stream the data */
	public static final int BUFFER_SIZE = 4096;
	/**
	 * The minimum amount of progress that has to be done before the progress
	 * bar gets updated
	 */
	public static final int MIN_PROGRESS_STEP = 4096;
	/**
	 * The minimum amount of time that has to elapse before the progress bar
	 * gets updated, in ms
	 */
	public static final long MIN_PROGRESS_TIME = 200;

	private static final String TAG = OfflineDownloadTask.class.getSimpleName();

	private Context mContext;
	private int mBusId;
	public String mRequestUri;
	private String mExtraPath;
	private String mTempZipfile;
	private long mTotalBytes;
	private long mMinProgressStep;
	private FileOutputStream mOutputStream;
	private OfflineDownloadEvent mOfflineDownloadEvent;
	private CacheFileManager mCacheFileManager;
	private boolean isZip;
	private ConfigManager mConfig;

	/**
	 * @param context
	 * @param busId
	 * @param offlineDownloadEvent
	 */
	public OfflineDownloadTask(Context context, int busId,
			OfflineDownloadEvent offlineDownloadEvent, boolean zip) {
		mConfig = new ConfigManager(context);
		mCacheFileManager = CacheFileManager.getInstance();
		mContext = context;
		mBusId = busId;
		mOfflineDownloadEvent = offlineDownloadEvent;
		mTempZipfile = mCacheFileManager.getOfflineTempDir().getPath()
				+ File.separator + mConfig.getUpdateApkName();
		mConfig.setApkFileDir(mTempZipfile);
		Log.i("i", "mTempZipfile:--------------------" + mTempZipfile);
		mExtraPath = mCacheFileManager.getOfflineDir().getPath()
				+ File.separator + mConfig.getUpdateApkName();
		mMinProgressStep = MIN_PROGRESS_STEP;
		isZip = zip;
	}

	/**
	 * Returns the user agent provided by the initiating app, or use the default
	 * one
	 */
	private String userAgent() {
		String userAgent = "AndroidDownloadManager";
		return userAgent;
	}

	/**
	 * State within executeDownload()
	 */
	private static class InnerState {
		public int mBytesSoFar = 0;
		public String mHeaderETag;
		public boolean mContinuingDownload = false;
		public String mHeaderContentLength;
		public String mHeaderContentDisposition;
		public String mHeaderContentLocation;
		public int mBytesNotified = 0;
		public long mTimeLastNotification = 0;
	}

	private class StopDownload extends Throwable {
		public int mFinalStatus;

		public StopDownload(int finalStatus, String message) {
			super(message);
			mFinalStatus = finalStatus;
		}

		public StopDownload(int finalStatus, String message, Throwable throwable) {
			super(message, throwable);
			mFinalStatus = finalStatus;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	@Override
	protected Integer doInBackground(String... params) {
		MyHttpClient client = null;
		mRequestUri = params[0];
		int finalStatus = STATUS_UNKNOWN_ERROR;
		try {
			Log.v(TAG, "initiating download for " + mRequestUri);
			client = MyHttpClient.newInstance(userAgent(), mContext);
			HttpGet request = new HttpGet(mRequestUri);
			executeDownload(client, request);
			request.abort();
			request = null;
			Log.v(TAG, "download completed for " + mRequestUri);
			publishProgress(100);
			executeCacheFile();
			finalStatus = STATUS_SUCCESS;
		} catch (StopDownload error) {
			Log.w(TAG, "Aborting intsall " + ": " + error.getMessage());
			finalStatus = error.mFinalStatus;
		} catch (Throwable ex) { // sometimes the socket code throws unchecked
			// exceptions
			Log.w(TAG, "Exception intsall " + ": " + ex);
			finalStatus = STATUS_UNKNOWN_ERROR;
			// falls through to the code that reports an error
		} finally {
			if (client != null) {
				client.close();
				client = null;
			}
		}
		cleanupDestination(finalStatus);
		return finalStatus;
	}

	@Override
	protected void onCancelled() {
		// TODO Auto-generated method stub
		super.onCancelled();
		setupDestinationFile();
		if (mOfflineDownloadEvent != null) {
			mOfflineDownloadEvent.cancal(mBusId);
		}
	}

	@Override
	protected void onPostExecute(Integer result) {
		// TODO Auto-generated method stub
		super.onPostExecute(result);
		if (mOfflineDownloadEvent != null) {
			if (result > 0) {
				mOfflineDownloadEvent.error(mBusId, result);
			} else {
				mOfflineDownloadEvent.complete(mBusId);
			}
		}
	}

	@Override
	protected void onPreExecute() {
		// TODO Auto-generated method stub
		super.onPreExecute();
		if (mOfflineDownloadEvent != null) {
			mOfflineDownloadEvent.start(mBusId);
		}
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		super.onProgressUpdate(values);
		if (mOfflineDownloadEvent != null) {
			mOfflineDownloadEvent.progress(mBusId, values[0]);
		}
	}

	/**
	 * 
	 */
	private boolean executeCacheFile() throws StopDownload {
		if (isZip) {
			File installFolder = new File(mExtraPath);
			if (installFolder.exists()) {
				installFolder.delete();
			}
			installFolder.mkdir();
			try {
				int leng = 0;
				byte[] buf = new byte[1024];

				FileInputStream fins = new FileInputStream(mTempZipfile);
				ZipInputStream zis = new ZipInputStream(fins);

				for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis
						.getNextEntry()) {
					checkPausedOrCanceled();
					String enName = entry.getName();
					if (entry.isDirectory()) {
						enName = enName.substring(0, enName.length() - 1);
						File folder = new File(mExtraPath + File.separator
								+ enName);
						folder.mkdirs();
					} else {
						File file = new File(mExtraPath + File.separator
								+ enName);
						file.createNewFile();
						FileOutputStream fos = new FileOutputStream(file);
						while ((leng = zis.read(buf)) != -1) {
							fos.write(buf, 0, leng);
						}
						fos.flush();
						fos.close();
					}
				}
				zis.close();
				fins.close();
				(new File(mTempZipfile)).delete();
				return true;
			} catch (ZipException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.parse("file://" + mTempZipfile),
					"application/vnd.android.package-archive");
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mContext.startActivity(intent);
			return true;
		}
		return false;

	}

	private void executeDownload(MyHttpClient client, HttpGet request)
			throws StopDownload {
		InnerState innerState = new InnerState();
		byte data[] = new byte[BUFFER_SIZE];
		setupDestinationFile();
		// check just before sending the request to avoid using an invalid
		// connection at all
		checkConnectivity();
		HttpResponse response = sendRequest(client, request);
		handleExceptionalStatus(innerState, response);
		Log.v(TAG, "received response for " + mRequestUri);
		processResponseHeaders(innerState, response);
		InputStream entityStream = openResponseEntity(response);
		transferData(innerState, data, entityStream);
	}

	/**
	 * Prepare the destination file to receive data. If the file already exists,
	 * we'll set up appropriately for resumption.
	 */
	private void setupDestinationFile() {
		deleteFile(mTempZipfile);
	}

	void deleteFile(String path) {
		if (path != null) {
			File file = new File(path);
			deleteFile(file);
		}
	}

	static boolean deleteFile(File file) {
		if ((file != null) && !file.delete()) {
			Log.w(TAG, "cannot delete cert: " + file);
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Check if current connectivity is valid for this request.
	 */
	private void checkConnectivity() throws StopDownload {
		boolean networkUsable = isNetworkAvailable();
		if (!networkUsable) {
			throw new StopDownload(STATUS_NETWORK_ERROR,
					"Network is not available");
		}
	}

	public boolean isNetworkAvailable() {
		boolean isAvailable = false;
		final ConnectivityManager cm = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (null != cm) {
			final NetworkInfo[] netinfo = cm.getAllNetworkInfo();
			if (null != netinfo) {
				for (int i = 0; i < netinfo.length; i++) {
					if (netinfo[i].isConnected()) {
						isAvailable = true;
					}
				}
			}
		}
		return isAvailable;
	}

	/**
	 * Send the request to the server, handling any I/O exceptions.
	 */
	private HttpResponse sendRequest(MyHttpClient client, HttpGet request)
			throws StopDownload {
		try {
			if (CallStatUtils.isOMS()) {
				return client.execute(request);
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
							return httpclient.execute(request);
						} else {
							return client.execute(request);
						}
					} else {
						return client.execute(request);
					}
				} else {
					return client.execute(request);
				}
			}
		} catch (IllegalArgumentException ex) {
			throw new StopDownload(STATUS_HTTP_DATA_ERROR,
					"while trying to execute request: " + ex.toString(), ex);
		} catch (IOException ex) {
			throw new StopDownload(STATUS_UNKNOWN_ERROR,
					"while trying to execute request: " + ex.toString(), ex);
		}
	}

	/**
	 * Check the HTTP response status and handle anything unusual (e.g. not
	 * 200/206).
	 */
	private void handleExceptionalStatus(InnerState innerState,
			HttpResponse response) throws StopDownload {
		int expectedStatus = 200;
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != expectedStatus) {
			throw new StopDownload(STATUS_UNHANDLED_HTTP_CODE, "http error "
					+ statusCode);
		}
	}

	/**
	 * Read HTTP response headers and take appropriate action, including setting
	 * up the destination file and updating the database.
	 */
	private void processResponseHeaders(InnerState innerState,
			HttpResponse response) throws StopDownload {

		readResponseHeaders(innerState, response);
		Log.v(TAG, "writing " + mRequestUri + " to " + mTempZipfile);
		// check connectivity again now that we know the total size
		checkConnectivity();
	}

	/**
	 * Read headers from the HTTP response and store them into local state.
	 */
	private void readResponseHeaders(InnerState innerState,
			HttpResponse response) throws StopDownload {
		Header header = response.getFirstHeader("Content-Disposition");
		if (header != null) {
			innerState.mHeaderContentDisposition = header.getValue();
		}
		header = response.getFirstHeader("Content-Location");
		if (header != null) {
			innerState.mHeaderContentLocation = header.getValue();
		}
		header = response.getFirstHeader("ETag");
		if (header != null) {
			innerState.mHeaderETag = header.getValue();
		}
		String headerTransferEncoding = null;
		header = response.getFirstHeader("Transfer-Encoding");
		if (header != null) {
			headerTransferEncoding = header.getValue();
		}
		if (headerTransferEncoding == null) {
			header = response.getFirstHeader("Content-Length");
			if (header != null) {
				innerState.mHeaderContentLength = header.getValue();
				mTotalBytes = Long.parseLong(innerState.mHeaderContentLength);
				mMinProgressStep = mTotalBytes / 20;
			}
		} else {
			// Ignore content-length with transfer-encoding - 2616 4.4 3
			Log.v(TAG, "ignoring content-length because of xfer-encoding");
		}
		Log.v(TAG, "Content-Disposition: "
				+ innerState.mHeaderContentDisposition);
		Log.v(TAG, "Content-Length: " + innerState.mHeaderContentLength);
		Log.v(TAG, "Content-Location: " + innerState.mHeaderContentLocation);
		Log.v(TAG, "ETag: " + innerState.mHeaderETag);
		Log.v(TAG, "Transfer-Encoding: " + headerTransferEncoding);

		boolean noSizeInfo = innerState.mHeaderContentLength == null
				&& (headerTransferEncoding == null || !headerTransferEncoding
						.equalsIgnoreCase("chunked"));
		if (noSizeInfo) {
			throw new StopDownload(STATUS_HTTP_DATA_ERROR,
					"can't know size of download, giving up");
		}
	}

	/**
	 * Open a stream for the HTTP response entity, handling I/O errors.
	 * 
	 * @return an InputStream to read the response entity
	 */
	private InputStream openResponseEntity(HttpResponse response)
			throws StopDownload {
		try {
			return response.getEntity().getContent();
		} catch (IOException ex) {
			logNetworkState();
			throw new StopDownload(STATUS_UNKNOWN_ERROR,
					"while getting entity: " + ex.toString(), ex);
		}
	}

	private void logNetworkState() {
		// Log.i(TAG, "Net " + (isNetworkAvailable() ? "Up" : "Down"));
	}

	/**
	 * Transfer as much data as possible from the HTTP response to the
	 * destination file.
	 * 
	 * @param data
	 *            buffer to use to read data
	 * @param entityStream
	 *            stream for reading the HTTP response entity
	 */
	private void transferData(InnerState innerState, byte[] data,
			InputStream entityStream) throws StopDownload {
		try {
			for (;;) {
				int bytesRead = readFromResponse(innerState, data, entityStream);
				if (bytesRead == -1) { // success, end of stream already reached
					handleEndOfStream(innerState);
					return;
				}

				writeDataToDestination(data, bytesRead);
				innerState.mBytesSoFar += bytesRead;
				reportProgress(innerState);

				Log.v(TAG, "downloaded " + innerState.mBytesSoFar + " for "
						+ mRequestUri);

				checkPausedOrCanceled();
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	/**
	 * Read some data from the HTTP response stream, handling I/O errors.
	 * 
	 * @param data
	 *            buffer to use to read data
	 * @param entityStream
	 *            stream for reading the HTTP response entity
	 * @return the number of bytes actually read or -1 if the end of the stream
	 *         has been reached
	 */
	private int readFromResponse(InnerState innerState, byte[] data,
			InputStream entityStream) throws StopDownload {
		try {
			return entityStream.read(data);
		} catch (IOException ex) {
			logNetworkState();
			throw new StopDownload(STATUS_UNKNOWN_ERROR,
					"while reading response: " + ex.toString(), ex);
		}
	}

	/**
	 * Called when we've reached the end of the HTTP response stream, to update
	 * the database and check for consistency.
	 */
	private void handleEndOfStream(InnerState innerState) throws StopDownload {
		boolean lengthMismatched = (innerState.mHeaderContentLength != null)
				&& (innerState.mBytesSoFar != Integer
						.parseInt(innerState.mHeaderContentLength));
		if (lengthMismatched) {
			throw new StopDownload(STATUS_UNKNOWN_ERROR,
					"mismatched content length");
		}
	}

	/**
	 * Write a data buffer to the destination file.
	 * 
	 * @param data
	 *            buffer containing the data to write
	 * @param bytesRead
	 *            how many bytes to write from the buffer
	 */
	private void writeDataToDestination(byte[] data, int bytesRead)
			throws StopDownload {
		for (;;) {
			try {
				if (mOutputStream == null) {
					mOutputStream = new FileOutputStream(mTempZipfile, true);
				}
				mOutputStream.write(data, 0, bytesRead);
				return;
			} catch (IOException ex) {
				throw new StopDownload(STATUS_FILE_ERROR,
						"while writing destination file: " + ex.toString(), ex);
			}
		}
	}

	/**
	 * Report download progress through the database if necessary.
	 */
	private void reportProgress(InnerState innerState) {
		long now = System.currentTimeMillis();
		if (innerState.mBytesSoFar - innerState.mBytesNotified > mMinProgressStep
				&& now - innerState.mTimeLastNotification > MIN_PROGRESS_TIME) {
			publishProgress((int) ((innerState.mBytesSoFar / (float) mTotalBytes) * 100));
			innerState.mBytesNotified = innerState.mBytesSoFar;
			innerState.mTimeLastNotification = now;
		}
	}

	/**
	 * Check if the download has been paused or canceled, stopping the request
	 * appropriately if it has been.
	 */
	private void checkPausedOrCanceled() throws StopDownload {
		if (isCancelled()) {
			throw new StopDownload(STATUS_CANCELLED,
					"install Canceled by owner");
		}
	}

	/**
	 * Called just before the thread finishes, regardless of status, to take any
	 * necessary action on the downloaded file.
	 */
	private void cleanupDestination(int finalStatus) {
		closeDestination();
		// if (mTempZipfile != null) {
		// new File(mTempZipfile).delete();
		// mTempZipfile = null;
		// }
	}

	/**
	 * Close the destination output stream.
	 */
	private void closeDestination() {
		try {
			// close the file
			if (mOutputStream != null) {
				mOutputStream.close();
				mOutputStream = null;
			}
		} catch (IOException ex) {
			Log.v(TAG, "exception when closing the file after download : " + ex);
			// nothing can really be done if the file can't be closed
		}
	}

}
