package com.archermind.callstat.home.settings;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.archermind.callstat.ConfigManager;
import com.archermind.callstat.ILog;
import com.archermind.callstat.R;
import com.archermind.callstat.common.CallStatUtils;
import com.archermind.callstat.common.net.MyHttpPostHelper;

public class NewFeedbackActivity extends Activity implements OnClickListener {

	private RelativeLayout feedback_back_btn;
	private RelativeLayout configrm_btn;
	private EditText sugg_et;
	private ProgressDialog mpd;
	ConfigManager config;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.new_feedback);
		config = new ConfigManager(this);

		initUI();
		initListener();
		// Log.e("i", "NewFeedbackActivity onCreate");
	}

	private void initListener() {
		feedback_back_btn.setOnClickListener(this);
		configrm_btn.setOnClickListener(this);
	}

	private void initUI() {
		feedback_back_btn = (RelativeLayout) findViewById(R.id.feedback_back_rl);
		configrm_btn = (RelativeLayout) findViewById(R.id.feedback_send_rl);
		sugg_et = (EditText) findViewById(R.id.feeddback_sugg_et);
		sugg_et.setFocusable(true);
		sugg_et.setFocusableInTouchMode(true);
		sugg_et.requestFocus();
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {

			public void run() {
				InputMethodManager inputManager = (InputMethodManager) sugg_et
						.getContext().getSystemService(
								Context.INPUT_METHOD_SERVICE);
				inputManager.showSoftInput(sugg_et, 0);
			}

		}, 998);
		String sugg = config.getFeedback();
		if (sugg != null) {
			sugg_et.setText(sugg);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Log.e("i", "NewFeedbackActivity onDestroy");
	}

	@Override
	public void onClick(View v) {
		if (v == feedback_back_btn) {
			finish();
			overridePendingTransition(R.anim.push_down_in, R.anim.push_down_out);
		} else if (v == configrm_btn) {

			String sugg = sugg_et.getText().toString().trim();
			if (sugg == null || "".equals(sugg)) {
				Toast.makeText(this, "您还没有填写意见和建议哦！～", Toast.LENGTH_LONG)
						.show();
				return;
			}
			if (sugg.length() < 5) {
				Toast.makeText(this, "建议内容请大于5个字，以帮助我们理解您的建议！",
						Toast.LENGTH_LONG).show();
				return;
			}

			StringBuilder feedback = new StringBuilder();
			feedback.append("suggestion:" + sugg + "\n" + "\n");
			config.setFeedback(feedback.toString().replace("suggestion:", ""));

			if (CallStatUtils.isNetworkAvailable(this)) {
				new SendFeedbackThread().execute(feedback.toString());
			} else {
				Toast.makeText(this, "网络当前不可用,您的建议已缓存，谢谢支持！", Toast.LENGTH_LONG)
						.show();
				return;
			}

		}
	}

	class SendFeedbackThread extends AsyncTask<Object, Void, Void> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if (mpd == null || !mpd.isShowing()) {
				mpd = ProgressDialog.show(NewFeedbackActivity.this, null,
						"正在投递您的宝贵意见哦...");
				mpd.setCancelable(true);
			}
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			if (mpd != null && mpd.isShowing()) {
				mpd.dismiss();
			}
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			if (mpd != null && mpd.isShowing()) {
				mpd.dismiss();
			}
		}

		@Override
		protected Void doInBackground(Object... params) {
			String url = getString(R.string.feedback_url);
			Map<String, String> map = new HashMap<String, String>();
			String feedback = (String) params[0];
			String imei = config.getImei();
			String number = config.getTopEightNum();
			String version = String.valueOf(config.getVersionCode());
			map.put("phoneNumber", number);
			map.put("imei", imei);
			map.put("feedback", feedback);
			map.put("version", version);

			// Log.i("i", "phoneNumber:" + number);
			// Log.i("i", "imei:" + imei);
			// Log.i("i", "feedback:" + feedback);

			HttpPost request = MyHttpPostHelper.getHttpPost(url);
			UrlEncodedFormEntity entity = MyHttpPostHelper
					.buildUrlEncodedFormEntity(map, HTTP.UTF_8);
			request.setEntity(entity);

			DefaultHttpClient httpClient = new DefaultHttpClient();
			try {
				HttpResponse response = httpClient.execute(request);

				if (response != null) {
					int code = response.getStatusLine().getStatusCode();
					if (code == 200) {
						// Log.i("i", "@@@@@@@@@@@@@@@@@@@" + code);
						config.setFeedback(null);
						String strResult = EntityUtils.toString(response
								.getEntity());
						// Log.i("i", response.getStatusLine().getStatusCode()
						// + "");
						// Log.i("i", "http response test:----------" +
						// strResult);
						sentNotifier.sendEmptyMessage(0);
					} else {
						if (mpd != null && mpd.isShowing()) {
							mpd.dismiss();
						}
						sentNotifier.sendEmptyMessage(-1);
					}
				}
			} catch (Exception e) {
				ILog.LogE(getClass(),
						"SendFeedback failed,Exception:" + e.getMessage());
				sentNotifier.sendEmptyMessage(-2);
				e.printStackTrace();
			}
			return null;
		}

	}

	private Handler sentNotifier = new Handler(new Callback() {

		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				if (CallStatUtils.isMyAppOnDesk(NewFeedbackActivity.this)) {
					Toast.makeText(NewFeedbackActivity.this, "发送成功！谢谢您的支持！",
							Toast.LENGTH_LONG).show();
				}
				sugg_et.setText(null);
				config.setFeedback(null);
				break;
			case -1:
				if (CallStatUtils.isMyAppOnDesk(NewFeedbackActivity.this)) {
					Toast.makeText(NewFeedbackActivity.this,
							"服务器网络异常，请稍候再试，谢谢支持！", Toast.LENGTH_LONG).show();
				}
				break;
			case -2:
				if (CallStatUtils.isMyAppOnDesk(NewFeedbackActivity.this)) {
					Toast.makeText(NewFeedbackActivity.this,
							"本地网络异常，请查看网络是否被禁用！", Toast.LENGTH_LONG).show();
				}
				break;
			default:
				break;
			}
			return false;
		}
	});

	// 监听返回键
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			finish();
			overridePendingTransition(R.anim.push_down_in, R.anim.push_down_out);
			return false;
		}
		return false;
	}
}
