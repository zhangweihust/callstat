package com.android.callstat.home.views;

import android.content.Context;
import android.widget.Toast;

import com.android.callstat.CallStatApplication;

public class ToastFactory {

	private static Context MyContext = CallStatApplication
			.getCallstatsContext();

	private static Toast toast = null;

	/**
	 * 
	 * @param context
	 *            使用时的上下文
	 * 
	 * @param hint
	 *            在提示框中需要显示的文本
	 * 
	 * @return 返回一个不会重复显示的toast
	 * 
	 * */

	public static Toast getToast(Context context, String hint, int duration) {
		if (/* ToastFactory.context == context */toast != null) {
			// toast.cancel();
			toast.setText(hint);
		} else {
			// ToastFactory.context = context;
			toast = Toast.makeText(context, hint, duration);
		}
		return toast;
	}
}
