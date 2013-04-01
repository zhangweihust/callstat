package com.android.callstat.home.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnKeyListener;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.android.callstat.CallStatApplication;
import com.android.callstat.ConfigManager;
import com.android.callstat.common.CallStatUtils;
import com.android.callstat.common.DeviceInformation;
import com.android.callstat.common.StringUtil;
import com.android.callstat.common.DeviceInformation.InfoName;
import com.android.callstat.common.ResourceLoader.OnLoadFinishListener;
import com.android.callstat.common.download.OfflineDownloadEvent;
import com.android.callstat.common.download.OfflineDownloadManager;
import com.android.callstat.common.json.AbstractWebLoadManager.OnWebLoadListener;
import com.android.callstat.home.HomeGuideActivity;
import com.android.callstat.home.HomeGuidePrivacyActivity;
import com.android.callstat.home.bean.ClientUpdate;
import com.android.callstat.home.json.UpdateManager;
import com.archermind.callstat.R;

public class NewAboutUsActivity extends Activity implements OnClickListener,
		OnTouchListener {

	private RelativeLayout back;
	private TextView version, channelTextView;
	private RelativeLayout checkUpdate;
	private Button sina;
	private Button tencent;
	private RelativeLayout toOurNet;
	private RelativeLayout version_rl;

	private ConfigManager config;
	private CallStatApplication application;
	private UpdateManager updateManager;
	public static ClientUpdate mClientUpdate;
	public static final String CHANGE_UPDATE_NOTICE_ACTION = "change_update_notice_action";
	private ProgressDialog mProgressDialog;
	private static final int MAX_PROGRESS = 100;
	private NotificationManager notificationManager;
	private RemoteViews contentView;
	private Notification notification;
	private int _ID = 0x111;
	private int mProgressStatus = 0;

	private ProgressDialog progressDialog;

	private OnWebLoadListener<ClientUpdate> updateListener = new OnWebLoadListener<ClientUpdate>() {

		@Override
		public void OnStart() {
			// Log.i("i", "updateListener OnStart");
		}

		@Override
		public void OnCancel() {

		}

		@Override
		public void OnLoadComplete(int statusCode) {
			switch (statusCode) {
			case OnLoadFinishListener.ERROR_TIMEOUT:
				if (progressDialog != null && progressDialog.isShowing()) {
					progressDialog.dismiss();
				}
				showDialogupdate("网络异常，更新失败，请稍候再试！");
				break;

			case OnLoadFinishListener.OK:

				break;
			case OnLoadFinishListener.ERROR_REQ_REFUSED:
				if (progressDialog != null && progressDialog.isShowing()) {
					progressDialog.dismiss();
				}
				showDialogupdate("网络异常，更新失败，请检查网络是否被防火墙禁用！");
				break;
			default:
				break;
			}
		}

		@Override
		public void OnPaserComplete(ClientUpdate clientUpdate) {
			if (null != clientUpdate) {
				if (CallStatUtils.isApplicationUpdatable(
						NewAboutUsActivity.this, getPackageName(),
						clientUpdate.getVersionCode())) {
					storeUpdateInfo(clientUpdate);
					setupUpdate(clientUpdate);
				} else {
					showDialogupdate("当前已是最新版本。");
				}
				if (progressDialog != null) {
					progressDialog.dismiss();
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.new_about_us);
		config = new ConfigManager(this);
		application = (CallStatApplication) getApplication();

		initNotification();

		initUI();
	}

	private void initUI() {
		back = (RelativeLayout) findViewById(R.id.aboutus_back_rl);
		version = (TextView) findViewById(R.id.version_tv);
		checkUpdate = (RelativeLayout) findViewById(R.id.check_update);
		sina = (Button) findViewById(R.id.sina_weibo);
		tencent = (Button) findViewById(R.id.tencent_weibo);
		toOurNet = (RelativeLayout) findViewById(R.id.go_to_us);
		// serviceRule = (RelativeLayout) findViewById(R.id.serviceRule);
		channelTextView = (TextView) findViewById(R.id.qudao_tv);

		version.setText(DeviceInformation
				.getInformation(InfoName.PHONE_CALLSTATS_VERSION));
		channelTextView.setText(config.getChannelNomber());

		back.setOnClickListener(this);
		checkUpdate.setOnClickListener(this);
		sina.setOnClickListener(this);
		tencent.setOnClickListener(this);
		toOurNet.setOnClickListener(this);
		// serviceRule.setOnClickListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	/**
	 * @param clientUpdate
	 */
	private void storeUpdateInfo(ClientUpdate clientUpdate) {
		config.setVersionCode(clientUpdate.getVersionCode());
		config.setVersionName(clientUpdate.getVersionName());
		config.setUpdateUrl(clientUpdate.getDownloadUrl());
		config.setUpdateApkName(clientUpdate.getApkName());
	}

	/**
	 * @param ClientUpdate
	 *            t
	 */
	protected void setupUpdate(ClientUpdate t) {
		// Log.i("i", "setupUpdate----------");
		if (null != t) {
			// Log.i("i", "setupUpdate----------ClientUpdate != null");
			if (CallStatUtils.isApplicationUpdatable(this, getPackageName(),
					t.getVersionCode())) {
				if (t.isForceUpdate()) {
					forceUpdate(t);
				} else {
					normalUpdate(t);
				}
			}
		} else {
			// Log.i("i", "CallStatMainActivity ----- ClientUpdate == null");
		}
	}

	/**
	 * @param t
	 */
	private void forceUpdate(final ClientUpdate t) {

		final Dialog dialog = new Dialog(this, R.style.myDialogTheme);
		dialog.setContentView(R.layout.update_dialog);
		Button updateNow = (Button) dialog.findViewById(R.id.update_now);
		Button updateLater = (Button) dialog.findViewById(R.id.update_later);
		Button updateOk = (Button) dialog.findViewById(R.id.callstat_ok);

		updateLater.setVisibility(View.GONE);
		updateNow.setVisibility(View.GONE);
		updateOk.setVisibility(View.VISIBLE);

		updateOk.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startDownload(NewAboutUsActivity.this, t.getDownloadUrl());
				dialog.dismiss();
			}
		});

		TextView version_tv = (TextView) dialog.findViewById(R.id.version_tv);
		TextView apksize_tv = (TextView) dialog.findViewById(R.id.apksize_tv);
		version_tv.setText(t.getVersionName());
		String[] parts = CallStatUtils.traffic_unit(t.getApkSize());
		apksize_tv.setText(parts[0] + parts[1]);
		TextView description_tv = (TextView) dialog
				.findViewById(R.id.home_update_description);
		TextView update_title = (TextView) dialog
				.findViewById(R.id.home_update_title);
		update_title.setText(getString(R.string.found_new_version_force));
		StringBuilder sb = new StringBuilder();
		String[] descriptions = t.getDescription();
		int len = descriptions.length;
		for (int i = 0; i < len; i++) {
			sb.append(descriptions[i] + "\n");
		}
		description_tv.setText(sb.toString());

		dialog.show();
	}

	/**
	 * @param t
	 */
	private void normalUpdate(final ClientUpdate t) {
		final Dialog dialog = new Dialog(this, R.style.myDialogTheme);
		dialog.setContentView(R.layout.update_dialog);
		Button updateNow = (Button) dialog.findViewById(R.id.update_now);
		Button updateLater = (Button) dialog.findViewById(R.id.update_later);

		updateNow.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				startDownload(NewAboutUsActivity.this, t.getDownloadUrl());
				dialog.dismiss();
			}
		});

		updateLater.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				config.setUpdateNotice(false);
				config.setUpdateLater(System.currentTimeMillis());
				dialog.dismiss();
			}
		});

		TextView version_tv = (TextView) dialog.findViewById(R.id.version_tv);
		TextView apksize_tv = (TextView) dialog.findViewById(R.id.apksize_tv);

		TextView description_tv = (TextView) dialog
				.findViewById(R.id.home_update_description);
		TextView update_title = (TextView) dialog
				.findViewById(R.id.home_update_title);
		update_title.setText(getString(R.string.found_new_version));

		version_tv.setText(t.getVersionName());
		String[] parts = CallStatUtils.traffic_unit(t.getApkSize());
		apksize_tv.setText(parts[0] + parts[1]);

		StringBuilder sb = new StringBuilder();
		String[] descriptions = t.getDescription();
		int len = descriptions.length;
		for (int i = 0; i < len; i++) {
			sb.append(descriptions[i] + "\n");
		}
		description_tv.setText(sb.toString());

		dialog.show();
	}

	@Override
	public void onClick(View v) {
		/*
		 * if (v == back) { finish(); }
		 */
		switch (v.getId()) {
		case R.id.aboutus_back_rl:
			finish();
			overridePendingTransition(R.anim.push_right_in,
					R.anim.push_right_out);
			break;
		case R.id.check_update:
			checkUpdate();
			break;
		case R.id.sina_weibo:
			goToSinaWeibo();
			break;
		case R.id.tencent_weibo:
			goToTencentWeibo();
			break;
		case R.id.go_to_us:
			goToOurNet();
			break;
		// case R.id.serviceRule:
		// Intent intent = new Intent(this, HomeGuidePrivacyActivity.class);
		// intent.putExtra("activity_state", "about");
		// startActivity(intent);
		// break;
		default:
			break;
		}
	}

	public void startDownload(Context context, String url) {
		// Log.i("i", "startDownloadTask startDownload");
		initDownload(context, url);
	}

	private void cancelDownload() {
		OfflineDownloadManager.getInstance(this).cancelDownloadTask(0);
		notificationManager.cancel(_ID);
	}

	private void initNotification() {
		String tickerText = getString(R.string.app_name);
		int icon = android.R.drawable.stat_sys_download;
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notification = new Notification(icon, tickerText,
				System.currentTimeMillis());
		Intent notificationIntent = new Intent(this, ProgressDialog.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		contentView = new RemoteViews(getPackageName(),
				R.layout.home_notification);

		contentView.setTextViewText(R.id.notificationTitle, tickerText);
		contentView.setTextViewText(R.id.notificationPercent, mProgressStatus
				+ "%");
		contentView.setProgressBar(R.id.notificationProgress, MAX_PROGRESS,
				mProgressStatus, false);
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.contentView = contentView;
		notification.contentIntent = contentIntent;
	}

	private void update() {
		if (!application.isUpdating()) {
			// Log.i("i", "updateInfo");
			if (CallStatUtils.isNetworkAvailable(this)) {
				int versionCode = config.getVersionCode();
				String versionName = config.getVersionName();
				final String downloadUrl = config.getUpdateUrl();

				if (!StringUtil.isNullOrWhitespaces(downloadUrl)) {
					if (CallStatUtils.isApplicationUpdatable(
							NewAboutUsActivity.this, getPackageName(),
							versionCode)) {
						new AlertDialog.Builder(NewAboutUsActivity.this)
								.setTitle("发现新版本")
								.setMessage(
										getString(R.string.found_new_version)
												+ "\n版本号：" + versionName)
								.setPositiveButton(R.string.callstat_ok,
										new DialogInterface.OnClickListener() {
											@Override
											public void onClick(
													DialogInterface dialog,
													int which) {
												// Log.i("i",
												// "setPositiveButton");
												startDownload(
														NewAboutUsActivity.this,
														downloadUrl);
											}
										})
								.setNegativeButton(R.string.callstat_cancel,
										null).create().show();
					} else {
						checkUpdate();
					}
				} else {
					checkUpdate();
				}
			} else {
				showDialogupdate("当前网络不可用，请检查网络状态！");
			}
		} else {
			new AlertDialog.Builder(NewAboutUsActivity.this)
					.setTitle("更新提示")
					.setMessage("系统正在更新，是否取消更新？")
					.setPositiveButton("继续更新", null)
					.setNegativeButton("取消更新",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									cancelDownload();
								}
							}).create().show();
		}
	}

	private void checkUpdate() {
		if (CallStatUtils.isNetworkAvailable(this)) {
			String url = getString(R.string.request_update_url);
			progressDialog = ProgressDialog.show(NewAboutUsActivity.this, null,
					"正在检查更新...", true);
			progressDialog.setCancelable(true);
			// Log.i("i", "checkUpdate:" + url);
			updateManager = new UpdateManager(NewAboutUsActivity.this, url);
			updateManager.setManagerListener(updateListener);
			updateManager.startManager();
		} else {
			showDialogupdate("当前网络不可用，请检查网络状态！");
		}
	}

	private void showDialogupdate(String msg) {
		final Dialog dialog = new Dialog(this, R.style.myDialogTheme);
		dialog.setContentView(R.layout.check_update_dialog);
		TextView content = (TextView) dialog.findViewById(R.id.update_content);
		Button btn = (Button) dialog.findViewById(R.id.btn_ok);

		content.setText(msg);

		btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				dialog.dismiss();
			}
		});
		dialog.show();
		// new AlertDialog.Builder(NewAboutUsActivity.this).setTitle("升级提示")
		// .setMessage(msg).setPositiveButton(R.string.callstat_ok, null)
		// .create().show();
	}

	/**
	 * @param context
	 * @param url
	 * 
	 */
	private void initDownload(final Context context, String url) {
		// Log.i("i", "startDownloadTask initDownload");
		application.setUpdating(true);
		mProgressDialog = new ProgressDialog(NewAboutUsActivity.this);
		mProgressDialog.setTitle("版本更新");
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setMax(MAX_PROGRESS);
		mProgressDialog.setButton(DialogInterface.BUTTON_POSITIVE,
				getString(R.string.download_hide),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
				getString(R.string.download_cancel),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						cancelDownload();
						dialog.dismiss();
					}
				});

		mProgressDialog.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(DialogInterface dialog, int keyCode,
					KeyEvent event) {

				if (keyCode == KeyEvent.KEYCODE_BACK) {
					if (mProgressDialog != null && mProgressDialog.isShowing()
							&& event.getAction() != KeyEvent.ACTION_UP) {
						// Log.i("callstats",
						// "onKeyDown keyCode == KeyEvent.KEYCODE_BACK");
						Toast.makeText(NewAboutUsActivity.this,
								"系统正在下载更新，请选择后台更新或者取消更新再试哦～",
								Toast.LENGTH_SHORT).show();
						return true;
					}
					return false;
				}
				return false;
			}
		});

		mProgressDialog.show();
		mProgressDialog
				.setOnCancelListener(new DialogInterface.OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
						cancelDownload();
					}
				});

		OfflineDownloadManager.getInstance(context).startDownloadTask(0, url,
				new OfflineDownloadEvent() {

					@Override
					public void start(int busId) {
						/* application.setUpdating(true); */
						// Log.i("i", "startDownloadTask start");
					}

					@Override
					public void progress(int busId, int progress) {
						if (progress >= MAX_PROGRESS) {
							mProgressDialog.dismiss();
						} else {
							mProgressDialog.setProgress(progress);
						}
						contentView.setTextViewText(R.id.notificationPercent,
								progress + "%");
						contentView.setProgressBar(R.id.notificationProgress,
								100, progress, false);
						notificationManager.notify(_ID, notification);
					}

					@Override
					public void error(int busId, int status) {
						// Log.i("i", "startDownloadTask error");
						application.setUpdating(false);
						// close current window
						if (mProgressDialog != null) {
							mProgressDialog.dismiss();
						}
						cancelDownload();
						/*
						 * startActivity(new Intent().setClass(
						 * CallStatApplication.getCallstatsContext(),
						 * UpdateExceptionActivity.class));
						 */
					}

					@Override
					public void complete(int busId) {
						// Log.i("i", "startDownloadTask complete");
						notificationManager.cancel(_ID);
						application.setUpdating(false);
					}

					@Override
					public void cancal(int busId) {
						// Log.i("i", "startDownloadTask cancal");
						application.setUpdating(false);
					}
				}, false);
		// notificationManager.notify(_ID, notification);
	}

	private void goToTencentWeibo() {
		String url = getString(R.string.tencent_weibo);
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(url));
		startActivity(intent);
	}

	private void goToSinaWeibo() {
		String url = getString(R.string.sina_weibo);
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(url));
		startActivity(intent);
	}

	private void goToOurNet() {
		String url = getString(R.string.our_net);
		// Log.e("callstats", "url:" + url);
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(url));
		startActivity(intent);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		return false;
	}

	// 监听返回键
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			finish();
			overridePendingTransition(R.anim.push_right_in,
					R.anim.push_right_out);
			return false;
		}
		return false;
	}
}
