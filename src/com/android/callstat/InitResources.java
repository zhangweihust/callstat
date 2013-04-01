package com.android.callstat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.Context;
import android.content.res.AssetManager;

public class InitResources {
	private Context _context;

	public InitResources(Context context) {
		_context = context;
	}

	/**
	 * 解压ZIP压缩文件
	 * 
	 * @param dirname
	 *            文件名
	 */
	public void Unzip(String dirname) {
		int BUFFER = 4096; // 这里缓冲区我们使用4KB，
		String strEntry; // 保存每个zip的条目名称
		String targetDir = "/data/data/com.archermind.callstat/databases/";
		AssetManager assetManager = _context.getAssets();
		try {
			BufferedOutputStream dest = null; // 缓冲输出流
			InputStream fis = assetManager.open(dirname);
			ZipInputStream zis = new ZipInputStream(
					new BufferedInputStream(fis));
			ZipEntry entry; // 每个zip条目的实例

			while ((entry = zis.getNextEntry()) != null) {

				try {
					// Log.i("my", "=" + entry);
					int count;
					byte data[] = new byte[BUFFER];
					strEntry = entry.getName();

					File entryFile = new File(targetDir + strEntry);
					File entryDir = new File(entryFile.getParent());
					if (!entryDir.exists()) {
						entryDir.mkdirs();
					}

					FileOutputStream fos = new FileOutputStream(entryFile);
					dest = new BufferedOutputStream(fos, BUFFER);
					while ((count = zis.read(data, 0, BUFFER)) != -1) {
						dest.write(data, 0, count);
					}
					dest.flush();
					dest.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			zis.close();
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	/*
	 * private void copyFile(InputStream in, OutputStream out) { byte[] buffer =
	 * new byte[1024]; int read; try { while ((read = in.read(buffer)) > 0) {
	 * out.write(buffer, 0, read); } } catch (IOException e) { // TODO
	 * Auto-generated catch block e.printStackTrace(); } }
	 */

}
