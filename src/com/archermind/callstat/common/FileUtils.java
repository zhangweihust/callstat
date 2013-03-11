package com.archermind.callstat.common;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;

import android.content.Context;
import android.os.Environment;

/**
 * Tools for managing files. Not for public consumption.
 * 
 * @hide
 */
public class FileUtils {

	private static final String LOG_TAG = "FileUtils";

	public static final int IO_BUFFER_SIZE = 4 * 1024;
	public static final String APK_LOWCASE_FILE_TYPE = ".apk";
	public static final String APK_UPCASE_FILE_TYPE = ".APK";

	private static final String HOME_DIR = "callstatapk";
	private static final String CACHE_DIRECTORY = HOME_DIR + "/cache";
	private static final String DOWNLOAD_DIRECTORY = HOME_DIR + "/apk";

	/**
	 * @return
	 */
	public static File getDownloadDirectory() {
		File file = null;
		if (Environment.MEDIA_MOUNTED.equals(StorageUtil
				.getExternalStorageState())) {
			file = new File(StorageUtil.getExternalStorageDirectory(),
					DOWNLOAD_DIRECTORY);
			if (!file.exists()) {
				file.mkdirs();
			}
		}
		return file;
	}

	public static String readInput(File file) {
		StringBuffer buffer = new StringBuffer();
		try {
			FileInputStream fis = new FileInputStream(file);
			InputStreamReader isr = new InputStreamReader(fis, "utf-8");
			Reader in = new BufferedReader(isr);
			int i;
			while ((i = in.read()) > -1) {
				buffer.append((char) i);
			}
			in.close();
			return buffer.toString();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	// copy a file from srcFile to destFile, return true if succeed, return
	// false if fail
	public static boolean copyFile(File srcFile, File destFile) {
		boolean result = false;
		try {
			InputStream in = new FileInputStream(srcFile);
			try {
				result = copyToFile(in, destFile);
			} finally {
				in.close();
			}
		} catch (IOException e) {
			result = false;
		}
		return result;
	}

	/**
	 * Copy data from a source stream to destFile. Return true if succeed,
	 * return false if failed.
	 */
	public static boolean copyToFile(InputStream inputStream, File destFile) {
		try {
			if (destFile.exists()) {
				destFile.delete();
			}
			OutputStream out = new FileOutputStream(destFile);
			try {
				byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = inputStream.read(buffer)) >= 0) {
					out.write(buffer, 0, bytesRead);
				}
			} finally {
				out.close();
			}
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Copy the content of the input stream into the output stream, using a
	 * temporary byte array buffer whose size is defined by
	 * {@link #IO_BUFFER_SIZE}.
	 * 
	 * @param in
	 *            The input stream to copy from.
	 * @param out
	 *            The output stream to copy to.
	 * 
	 * @throws java.io.IOException
	 *             If any error occurs during the copy.
	 */
	public static void copy(InputStream in, OutputStream out)
			throws IOException {
		byte[] b = new byte[IO_BUFFER_SIZE];
		int read;
		while ((read = in.read(b)) != -1) {
			out.write(b, 0, read);
		}
	}

	public static void closeStream(Closeable stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException e) {
				// Log.e(LOG_TAG, "Could not close stream", e);
			}
		}
	}

	/**
	 * @param file
	 * @return
	 */
	public static boolean isApkFile(File file) {
		if (file == null || file.isDirectory()) {
			return false;
		}
		String fileName = file.getName();
		if (fileName.contains(".")
				&& fileName.substring(fileName.lastIndexOf('.')).toLowerCase()
						.equals(APK_LOWCASE_FILE_TYPE)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @param filePath
	 * @return
	 */
	public static boolean isApkFile(String filePath) {
		if (filePath == null || new File(filePath).isDirectory()) {
			return false;
		}

		if (filePath.contains(".")
				&& filePath.substring(filePath.lastIndexOf('.')).toLowerCase()
						.equals(APK_LOWCASE_FILE_TYPE)) {
			return true;
		} else {
			return false;
		}
	}

	public static String getApkNameFromFileName(String fileName) {
		// FIXME
		return fileName.replace(APK_LOWCASE_FILE_TYPE, "").replace(
				APK_UPCASE_FILE_TYPE, "");
	}

	public static File getCacheDirectory(Context context) {
		File file = null;
		if (Environment.MEDIA_MOUNTED.equals(StorageUtil
				.getExternalStorageState())) {
			file = new File(StorageUtil.getExternalStorageDirectory(),
					CACHE_DIRECTORY);
			if (!file.exists()) {
				file.mkdirs();
				try {
					new File(file, ".nomedia").createNewFile();
				} catch (IOException e) {
					// Log.e(LOG_TAG, "Could not create .nomedia file in " +
					// file.getPath());
				}
			}
		} else {
			// Use phone cache when sdcard is not available
			file = context.getCacheDir();
		}
		return file;
	}
}
