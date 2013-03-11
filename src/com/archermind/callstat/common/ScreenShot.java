package com.archermind.callstat.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.View;

public class ScreenShot {

	// 获取指定Activity的截屏，保存到png文件
	public static Bitmap takeScreenShot(Activity activity) {
		// View是你需要截图的View
		View view = activity.getWindow().getDecorView();
		view.setDrawingCacheEnabled(true);
		view.buildDrawingCache();
		Bitmap b1 = view.getDrawingCache();

		// 获取状态栏高度
		Rect frame = new Rect();
		activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
		int statusBarHeight = frame.top;
		// Log.i("callstats", "takeScreenShot statusBarHeight:" +
		// statusBarHeight);

		/*
		 * // 获取屏幕长和高 int width =
		 * activity.getWindowManager().getDefaultDisplay().getWidth(); int
		 * height = activity.getWindowManager().getDefaultDisplay()
		 * .getHeight();
		 */
		// 去掉标题栏
		Bitmap b = Bitmap.createBitmap(b1, 0, 0, b1.getWidth(), b1.getHeight());
		view.destroyDrawingCache();
		return b;
	}

	// 保存到sdcard
	public static File savePic(Bitmap b, File dir, String strFileName) {
		FileOutputStream fos = null;
		File image = null;
		try {
			image = new File(dir, strFileName);
			// Log.i("callstats", "savePic:" + image.getAbsolutePath());
			File parent = image.getParentFile();
			if (!parent.exists()) {
				parent.mkdirs();
			}
			if (!image.exists()) {
				image.createNewFile();
			}
			fos = new FileOutputStream(image);
			if (null != fos) {
				b.compress(Bitmap.CompressFormat.PNG, 100, fos);
				fos.flush();
				fos.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return image;
	}

	// 程序入口
	public static void shoot(Activity a) {
		// ScreenShot.savePic(ScreenShot.takeScreenShot(a), "sdcard/xx.png");
	}

}
