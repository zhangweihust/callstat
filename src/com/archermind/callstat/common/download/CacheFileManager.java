package com.archermind.callstat.common.download;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import android.content.Context;
import android.content.ContextWrapper;
import android.text.TextUtils;

import com.archermind.callstat.ILog;
import com.archermind.callstat.common.FileSystemUtil;
import com.archermind.callstat.common.StorageUtil;

/**
 * @author lx
 */
public class CacheFileManager extends ContextWrapper {

	private static CacheFileManager sInstance;
	private File mFileCacheDir;
	private File mImageCacheDir;
	private File mOfflineDir;
	private File mOfflineDownload;
	private File mExceptionLogDir;
	private File mImageDir;

	public static final String FILE_CACHE_DIR = "callstat/";
	public static final String FILE_EXCEPTION_LOG = "callstat/exception/";
	public static final String SAVE_IMAGE_DIR = "callstat/images/";
	public static final String OFFLINE_CACHE_DIR = "callstat/offline";
	public static final String OFFLINE_TEMP_DIR = "callstat/download";

	public static CacheFileManager init(Context context) {
		if (null == sInstance) {
			sInstance = new CacheFileManager(context);
		}
		return sInstance;
	}

	public static CacheFileManager getInstance() {
		if (null == sInstance) {
			throw new IllegalStateException("You must init first");
		}
		return sInstance;
	}

	/**
	 * @param base
	 */
	public CacheFileManager(Context base) {
		super(base);
		ensureCacheDir();
	}

	/**
	 * Ensure the cache directory.
	 */
	public void ensureCacheDir() {
		try {
			final File storageDir = StorageUtil.getExternalStorageDirectory();
			if (storageDir != null) {
				final File fileCacheDir = new File(storageDir, FILE_CACHE_DIR);
				if (!fileCacheDir.exists()) {
					fileCacheDir.mkdirs();
				}
				mFileCacheDir = fileCacheDir;

				if (storageDir != null) {
					final File fileImageDir = new File(storageDir,
							SAVE_IMAGE_DIR);
					if (!fileCacheDir.exists()) {
						fileCacheDir.mkdirs();
					}
					mImageDir = fileImageDir;
				}

				final File fileTempDir = new File(storageDir, OFFLINE_TEMP_DIR);
				if (!fileTempDir.exists()) {
					fileTempDir.mkdirs();
				}
				mOfflineDownload = fileTempDir;

				final File fileExceptionLogDir = new File(storageDir,
						FILE_EXCEPTION_LOG);
				if (!fileExceptionLogDir.exists()) {
					fileExceptionLogDir.mkdirs();
				}
				mExceptionLogDir = fileExceptionLogDir;

				final File offlineDir = new File(storageDir, OFFLINE_CACHE_DIR);
				if (!offlineDir.exists()) {
					offlineDir.mkdirs();
				}
				mOfflineDir = offlineDir;
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}

	}

	private void setNomedia() {
		File nomedia = new File(mImageCacheDir, ".nomedia");
		if (!nomedia.exists()) {
			try {
				nomedia.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public File getImageCacheFile(String url) {
		if (!TextUtils.isEmpty(url)) {
			url = url.substring(url.lastIndexOf("/") + 1);
			return new File(mImageCacheDir, url);
		}
		return null;
	}

	/**
	 * Retrieve the cache file for the give url.
	 * 
	 * @param url
	 *            the url of the image.
	 * @return the cache {@link File}, or null if url is invalid.
	 */
	public boolean isImageCached(final String url) {
		final File cacheFile = getImageCacheFile(url);
		if (null != cacheFile && cacheFile.exists()) {
			return true;
		}
		return false;
	}

	/**
	 * Retrieve the cache file for the give url.
	 * 
	 * @param url
	 *            the url of the news.
	 * @return the cache {@link File}, or null if url is invalid.
	 */
	public File getCacheFile(final String url) {
		if (!TextUtils.isEmpty(url)) {
			return new File(mFileCacheDir, String.format("%08x.cache",
					url.hashCode()));
		}
		return null;
	}

	/**
	 * Determine whether the image file is valid.
	 * 
	 * @param file
	 *            the image file.
	 * @return true if the file exits and can be decoded, false otherwise.
	 */
	public boolean isCacheFileValid(final File file) {
		if (file != null && file.exists()) {
			return isCacheFileValidInternal(file);
		}
		return false;
	}

	private boolean isCacheFileValidInternal(final File cacheFile) {
		if (cacheFile.isFile()) {
			return true;
		} else {
			cacheFile.delete();
		}
		return false;
	}

	public String loadCacheFromUrl(String url) {
		String json = null;
		final File cacheFile = getCacheFile(url);
		if (null != cacheFile) {
			json = loadCache(cacheFile);
		}
		return json;
	}

	/**
	 * @return the mOfflineDir
	 */
	public File getOfflineDir() {
		return mOfflineDir;
	}

	/**
	 * @param mOfflineDir
	 *            the mOfflineDir to set
	 */
	public void setOfflineDir(File mOfflineDir) {
		this.mOfflineDir = mOfflineDir;
	}

	/**
	 * @return the mOfflineTempDir
	 */
	public File getOfflineTempDir() {
		return mOfflineDownload;
	}

	/**
	 * @param mOfflineTempDir
	 *            the mOfflineTempDir to set
	 */
	public void setOfflineTempDir(File mOfflineTempDir) {
		this.mOfflineDownload = mOfflineTempDir;
	}

	public File getmImageDir() {
		return mImageDir;
	}

	/**
	 * @param file
	 * @return
	 */
	private String loadCache(File file) {
		try {
			FileInputStream inputStream = new FileInputStream(file);
			return inputStream2String(inputStream);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	String inputStream2String(InputStream is) {
		BufferedReader in = new BufferedReader(new InputStreamReader(is));
		StringBuffer buffer = new StringBuffer();
		String line = "";
		try {
			while ((line = in.readLine()) != null) {
				buffer.append(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return buffer.toString();
	}

	public void logSMS(String data) {
		FileOutputStream fos = null;
		OutputStreamWriter osw = null;
		try {
			String fileName = "SMS_log.txt";
			File f = new File(mOfflineDir, fileName);
			File parent = f.getParentFile();

			if (!parent.exists()) {
				// Log.i("i", "parent not exists");
				parent.mkdirs();
				// Log.i("i", "parent dir:" + parent.getPath());
			}
			if (!f.exists()) {
				f.createNewFile();
			}
			fos = new FileOutputStream(f, true);
			// fos = openFileOutput(fileName, MODE_APPEND);
			osw = new OutputStreamWriter(fos);
			osw.write(data + "\r\n");
			osw.flush();
		} catch (Exception ex) {
			// Log.i("i", "exeption while log file :" + ex.getMessage());
			ex.printStackTrace();
		} finally {

			if (osw != null) {
				try {
					osw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	public void logAccoutingTime(String data) {
		FileOutputStream fos = null;
		OutputStreamWriter osw = null;
		try {
			String fileName = "accounting_time_log.txt";
			File f = new File(mOfflineDir, fileName);
			File parent = f.getParentFile();

			if (!parent.exists()) {
				// Log.i("i", "parent not exists");
				parent.mkdirs();
				// Log.i("i", "parent dir:" + parent.getPath());
			}
			if (!f.exists()) {
				f.createNewFile();
			}
			fos = new FileOutputStream(f, true);
			// fos = openFileOutput(fileName, MODE_APPEND);
			osw = new OutputStreamWriter(fos);
			osw.write(data + "\r\n");
			osw.flush();
		} catch (Exception ex) {
			// Log.i("i", "exeption while log file :" + ex.getMessage());
			ex.printStackTrace();
		} finally {

			if (osw != null) {
				try {
					osw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	public void logAccounting(String data) {

		// Log.i("i", "my");
		FileOutputStream fos = null;
		OutputStreamWriter osw = null;
		try {
			String fileName = "accounting_log.txt";
			File f = new File(mOfflineDir, fileName);
			File parent = f.getParentFile();

			if (!parent.exists()) {
				// Log.i("i", "parent not exists");
				parent.mkdirs();
				// Log.i("i", "parent dir:" + parent.getPath());
			}
			if (!f.exists()) {
				f.createNewFile();
			}
			fos = new FileOutputStream(f, true);
			// fos = openFileOutput(fileName, MODE_APPEND);
			osw = new OutputStreamWriter(fos);
			osw.write(data + "\r\n");
			osw.flush();
		} catch (Exception ex) {
			// Log.i("i", "exeption while log file :" + ex.getMessage());
			ex.printStackTrace();
		} finally {

			if (osw != null) {
				try {
					osw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	public void logMy(String data) {
		// Log.i("i", "my");
		FileOutputStream fos = null;
		OutputStreamWriter osw = null;
		try {
			String fileName = "log.txt";
			File f = new File(mExceptionLogDir, fileName);
			File parent = f.getParentFile();

			if (!parent.exists()) {
				// Log.i("i", "parent not exists");
				parent.mkdirs();
				// Log.i("i", "parent dir:" + parent.getPath());
			}
			if (!f.exists()) {
				f.createNewFile();
			}
			fos = new FileOutputStream(f, true);
			// fos = openFileOutput(fileName, MODE_APPEND);
			osw = new OutputStreamWriter(fos);
			osw.write(data + "\r\n");
			osw.flush();
		} catch (Exception ex) {
			// Log.i("i", "exeption while log file :" + ex.getMessage());
			ex.printStackTrace();
		} finally {

			if (osw != null) {
				try {
					osw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	public void log(String data) {
		// Log.i("i", "in debug log");
		FileOutputStream fos = null;
		OutputStreamWriter osw = null;
		try {
			String fileName = "Exception log.txt";
			File f = new File(mExceptionLogDir, fileName);
			File parent = f.getParentFile();

			if (!parent.exists()) {
				// Log.i("i", "parent not exists");
				parent.mkdirs();
				// Log.i("i", "parent dir:" + parent.getPath());
			}
			if (!f.exists()) {
				f.createNewFile();
			}
			fos = new FileOutputStream(f, true);
			// fos = openFileOutput(fileName, MODE_APPEND);
			osw = new OutputStreamWriter(fos);
			osw.write(data + "\r\n");
			osw.flush();
		} catch (Exception ex) {
			// Log.i("i", "exeption while log file :" + ex.getMessage());
			ex.printStackTrace();
		} finally {

			if (osw != null) {
				try {
					osw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	/**
	 * Clean the cache directory.
	 */
	public void cleanCache() {
		FileSystemUtil.cleanDir(mFileCacheDir);
		FileSystemUtil.cleanDir(mImageCacheDir);
	}
}
