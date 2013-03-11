package com.archermind.callstat.common;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.view.KeyEvent;

public class ProgressDialogUtil {
	private static ProgressDialog progressDialog = null;

	// private static Context mContext=new
	public static void showProgress(Context context) {
		try {
			progressDialog = ProgressDialog.show(context, null, "数据加载中，请稍候...",
					false, true);
		} catch (Exception e) {
		}
		progressDialog.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(DialogInterface dialog, int keyCode,
					KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					dismissProgress();
				}
				return false;
			}
		});
	}

	public static void dismissProgress() {
		if (progressDialog != null) {
			progressDialog.dismiss();
			progressDialog = null;
		}
	}
}
