package com.archermind.callstat.home.settings;

import java.text.DecimalFormat;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.archermind.callstat.ConfigManager;
import com.archermind.callstat.R;
import com.archermind.callstat.common.CallStatUtils;
import com.archermind.callstat.service.CallStatSMSService;

public class TrafficInputDialog extends Activity implements OnTouchListener {

	private Boolean Bnull = false;

	private RelativeLayout edit_rl;
	private EditText traffic_et;
	private ConfigManager config;
	private Button OK_btn;
	private Button cancel_btn;

	// String traffic_mb;
	float traffic_used;
	private boolean REFLESH_NATIFICATION = false;
	DecimalFormat df = new DecimalFormat("#.##");
	/**
	 * 与CallStatSMSService进行绑定
	 */
	private CallStatSMSService.SMSBinder binder;
	private ServiceConnection conn = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {

		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			binder = (CallStatSMSService.SMSBinder) service;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.traffic_input_dialog);
		config = new ConfigManager(this);
		// for advanced part
		initUI();
		initListener();

		/* 绑定服务 */
		Intent intent = new Intent(this, CallStatSMSService.class);
		if (!CallStatUtils.isServiceRunning(this,
				"com.archermind.callstat.service.CallStatSMSService")) {
			startService(intent);
		}
		bindService(intent, conn, Context.BIND_AUTO_CREATE);
	}

	protected void onResume() {
		super.onResume();
		initUI();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(conn);
	}

	private void initListener() {
		OK_btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String traffic_str;
				if (!"".equals(traffic_et.getText().toString())) {

					long gprs = (long) (Float.parseFloat(traffic_et.getText()
							.toString()) * 1024 * 1024);
					long difference = gprs - config.getTotalGprsUsed();
					// Log.i("my", "difference=" + difference);

					if (difference != config.getTotalGprsUsedDifference()) {
						REFLESH_NATIFICATION = true;
					}

					config.setTotalGprsUsedDifference(difference);
				}

				if (REFLESH_NATIFICATION) {
					config.setMonthTrafficWarn(false);
					config.setTrafficBeyond(false);
					if (config.getStatusKeepNotice()) {
						if (binder != null) {
							binder.open_notification();
						}
					}
					REFLESH_NATIFICATION = false;
				}

				finish();
			}
		});
		cancel_btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});

		traffic_et.addTextChangedListener(watcher);
		traffic_et.setOnTouchListener(this);

	}

	private void initUI() {

		traffic_used = (float) Math.round((config.getTotalGprsUsed() + config
				.getTotalGprsUsedDifference()) / 1024f / 1024f * 100) / 100;
		// // traffic_mb = CallStatUtils.traffic_unit(config.getTotalGprsUsed()
		// + config.getTotalGprsUsedDifference())[1];
		edit_rl = (RelativeLayout) findViewById(R.id.edit_rl);
		traffic_et = (EditText) findViewById(R.id.traffic_et);

		traffic_et.setText(df.format(traffic_used));

		OK_btn = (Button) findViewById(R.id.ok_btn);
		cancel_btn = (Button) findViewById(R.id.cancel_btn);

	}

	private TextWatcher watcher = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable s) {
			// TODO Auto-generated method stub

		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			String editText = s.toString();

			int position = -1;
			if (editText.contains(".")) {
				position = editText.indexOf(".");
			}
			if (editText.length() == 1 && editText.equals(".")) {
				traffic_et.setText("0.");
			} else if (editText.length() == 2 && editText.startsWith("0")
					&& editText.charAt(1) != '.') {
				traffic_et.setText("0");
			} else if (editText.length() == 8 && editText.endsWith(".")) {
				String str = editText.substring(0, 7);
				traffic_et.setText(str);
			} else if (editText.contains(".")
					&& editText.length() == position + 4) {
				String str1 = editText.substring(0, position + 3);
				traffic_et.setText(str1);
			} else if (editText.length() == 6
					&& Float.parseFloat(editText) > 99999) {
				String str2 = editText.substring(0, 5);
				traffic_et.setText(str2);
			}
			traffic_et.setSelection(traffic_et.getText().toString().length());

		}
	};

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (v == traffic_et) {
			traffic_et.requestFocus();
			traffic_et.selectAll();
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showSoftInput(v, 0);
			return true;
		}
		return false;
	}
}
