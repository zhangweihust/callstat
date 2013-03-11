package com.archermind.callstat;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;

import com.archermind.callstat.common.download.CacheFileManager;
import com.archermind.callstat.common.net.MyHttpPostHelper;

/**
 * This is the dialog Activity used by ACRA to get authorization from the user
 * to send reports. Requires android:theme="@android:style/Theme.Dialog" and
 * android:launchMode="singleInstance" in your AndroidManifest to work properly.
 */
public class CrashReportDialog extends Activity {

	/**
	 * Default left title icon.
	 */
	private String mReportCause = null;
	private String mReportUrl = null;
	private Bundle mCrashResources;
	private ConfigManager config;

	private Button yes;
	private Button no;
	private Button restart;
	private LinearLayout mainLayout;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Log.i("i", "crash report dialog was created");
		try {
			mReportCause = getIntent().getStringExtra(
					ErrorReporter.EXTRA_REPORT_CAUSE_STRING);
			mReportUrl = getIntent().getStringExtra(
					ErrorReporter.EXTRA_REPORT_URL);
			if (TextUtils.isEmpty(mReportCause)
					|| TextUtils.isEmpty(mReportUrl)) {
				finish();
			}
			config = new ConfigManager(this);
			requestWindowFeature(Window.FEATURE_LEFT_ICON);
			final CrashReportingApplication application = (CrashReportingApplication) getApplication();
			mCrashResources = application.getCrashResources();
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			setContentView(R.layout.crashreportdialog);

			yes = (Button) findViewById(R.id.rep);
			no = (Button) findViewById(R.id.exit);
			mainLayout = (LinearLayout) LayoutInflater.from(this).inflate(
					R.layout.crashreportdialog, null);
			mainLayout.getBackground().setAlpha(0);

			/*
			 * final LinearLayout root = new LinearLayout(this);
			 * root.setOrientation(LinearLayout.VERTICAL); root.setPadding(10,
			 * 10, 10, 10); root.setLayoutParams(new
			 * LayoutParams(LayoutParams.FILL_PARENT,
			 * LayoutParams.WRAP_CONTENT)); final ScrollView scroll = new
			 * ScrollView(this); root.addView(scroll, new
			 * LinearLayout.LayoutParams( LayoutParams.FILL_PARENT,
			 * LayoutParams.FILL_PARENT, 1.0f));
			 * 
			 * final TextView text = new TextView(this);
			 * 
			 * text.setText(mCrashResources
			 * .getString(CrashReportingApplication.RES_DIALOG_TEXT));
			 * scroll.addView(text, LayoutParams.FILL_PARENT,
			 * LayoutParams.FILL_PARENT);
			 * 
			 * final LinearLayout buttons = new LinearLayout(this);
			 * buttons.setLayoutParams(new LinearLayout.LayoutParams(
			 * LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			 * buttons.setPadding(buttons.getPaddingLeft(), 10,
			 * buttons.getPaddingRight(), buttons.getPaddingBottom());
			 * 
			 * final Button yes = new Button(this);
			 */
			// yes.setText(mCrashResources
			// .getString(CrashReportingApplication.RES_BUTTON_REPORT));
			yes.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(final View v) {
					// Start email to send report
					sendReport();
					exit();
				}

			});
			/*
			 * buttons.addView(yes, new LinearLayout.LayoutParams(
			 * LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1.0f));
			 * final Button no = new Button(this);
			 */
			// no.setText(mCrashResources
			// .getString(CrashReportingApplication.RES_BUTTON_CANCEL));
			no.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(final View v) {
					// final File file = new File(mReportCause);
					// if (file.exists()) {
					// file.delete();
					// }
					sendReport();
					exit();
				}

			});
			/*
			 * buttons.addView(no, new LinearLayout.LayoutParams(
			 * LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1.0f));
			 */

			final String restartButtonText = mCrashResources
					.getString(CrashReportingApplication.RES_BUTTON_RESTART);
			if (null != restartButtonText && restartButtonText.length() > 0) {
				restart.setVisibility(View.VISIBLE);
				restart.setText(restartButtonText);
				restart.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(final View v) {
						application.onRestart();
						exit();
					}

				});
				/*
				 * buttons.addView(restart, new LinearLayout.LayoutParams(
				 * LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1.0f));
				 */
			}

			/*
			 * root.addView(buttons, new LinearLayout.LayoutParams(
			 * LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			 */

			/*
			 * setTitle(mCrashResources
			 * .getString(CrashReportingApplication.RES_DIALOG_TITLE));
			 * 
			 * final int resLeftIcon = mCrashResources
			 * .getInt(CrashReportingApplication.RES_DIALOG_ICON); if
			 * (resLeftIcon != 0) {
			 * getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
			 * resLeftIcon); } else {
			 * getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
			 * CRASH_DIALOG_LEFT_ICON); }
			 */
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}

	}

	private void sendReport() {
		new LogSendTask().execute();
	}

	class LogSendTask extends AsyncTask<Void, Void, Void> {

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		@Override
		protected Void doInBackground(Void... params) {
			try {
				Map<String, String> data = new HashMap<String, String>();
				String imei = config.getImei();
				String display = config.getScreenSize();
				String version = config.getVersionName();
				String model = config.getMobileModel();

				data.put("imei", imei);
				data.put("display", display);
				data.put("version", version);
				data.put("model", model);
				data.put("cause", mReportCause);
				// Log.i("i", "model-----------" + model);
				// Log.i("i", "imei-----------" + imei);
				HttpPost httpRequest = MyHttpPostHelper.getHttpPost(mReportUrl);
				UrlEncodedFormEntity myEtity = MyHttpPostHelper
						.buildUrlEncodedFormEntity(data, null);

				httpRequest.setEntity(myEtity);

				HttpResponse httpresp = new DefaultHttpClient()
						.execute(httpRequest);
				String strResult = EntityUtils.toString(httpresp.getEntity());
				// Log.i("i", httpresp.getStatusLine().getStatusCode()+"");
				// Log.i("i", "http response test:+++++++++" + strResult);
				ILog.LogI(this.getClass(), strResult);
			} catch (Exception e) {
				ILog.logException(this.getClass(), e);
			}

			CacheFileManager.getInstance().log(mReportCause);
			return null;
		}

	}

	/*
	 * private HttpEntity buildHttpEntity() { JSONObject method = new
	 * JSONObject(); try { method.put("test", "this is a test"); Log.d(TAG,
	 * "json=" + method.toString());
	 * 
	 * return new StringEntity("test", "UTF-8"); } catch (Exception e) {
	 * e.printStackTrace(); } return null; }
	 */

	private void exit() {
		finish();
	}

}
