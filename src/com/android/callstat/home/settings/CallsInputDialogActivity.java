package com.android.callstat.home.settings;

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

import com.android.callstat.ConfigManager;
import com.android.callstat.common.CallStatUtils;
import com.android.callstat.common.database.CallStatDatabase;
import com.android.callstat.home.CallsManagerActivity;
import com.android.callstat.service.CallStatSMSService;
import com.archermind.callstat.R;

public class CallsInputDialogActivity extends Activity implements
		OnTouchListener {

	private EditText calls_spent_et;
	private EditText calls_remain_et;
	private ConfigManager config;
	private Button OK_btn;
	private Button cancel_btn;
	float calls_remain;
	float calls_spent;

	private boolean REFLESH_NOTIFICATION = false;
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
		setContentView(R.layout.calls_input_dialog);
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
				if (!"".equals(calls_spent_et.getText().toString())) {

					calls_spent = Float.parseFloat(calls_spent_et.getText()
							.toString());
					REFLESH_NOTIFICATION = true;
					config.setFeeSpent(calls_spent);
				} else {
					if ("".equals(calls_spent_et.getText().toString())) {
						REFLESH_NOTIFICATION = true;
					}

				}

				if (!"".equals(calls_remain_et.getText().toString())
						&& Float.parseFloat(calls_remain_et.getText()
								.toString()) != config
								.getCalculateFeeAvailable()) {
					calls_remain = Float.parseFloat(calls_remain_et.getText()
							.toString());
					config.setCalculateFeeAvailable(calls_remain);
					config.setFeesRemain(calls_remain);
					REFLESH_NOTIFICATION = true;
				} else {
					// config.setCalculateFeeAvailable(100000f);
				}

				/* 刷新通知栏 */
				if (REFLESH_NOTIFICATION) {
					if (config.getStatusKeepNotice()) {
						if (binder != null) {
							binder.open_notification();
						}
					}
					REFLESH_NOTIFICATION = false;
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
		calls_remain_et.addTextChangedListener(watcher);
		calls_spent_et.addTextChangedListener(watcher1);
		calls_remain_et.setOnTouchListener(this);
		calls_spent_et.setOnTouchListener(this);

	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		if (v == calls_remain_et) {
			calls_remain_et.requestFocus();
			calls_remain_et.selectAll();
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showSoftInput(v, 0);
			return true;
		} else if (v == calls_spent_et) {
			calls_spent_et.requestFocus();
			calls_spent_et.selectAll();
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showSoftInput(v, 0);
			return true;
		} else {
			return false;
		}

	}

	private void initUI() {

		calls_remain_et = (EditText) findViewById(R.id.calls_remain_et);
		calls_spent_et = (EditText) findViewById(R.id.calls_spent_et);

		if (config.getCalculateFeeAvailable() != 100000
				&& config.getCalculateFeeAvailable() != 0) {
			calls_remain_et
					.setText(df.format(config.getCalculateFeeAvailable()));
		} else if (config.getCalculateFeeAvailable() == 0) {
			calls_remain_et.setText("0");
		} else {
			calls_remain_et.setText("");
		}
		if (config.getFeeSpent() != 100000f) {
			calls_spent_et.setText(df.format(config.getFeeSpent()));
		} else {
			double feeSpendTmp = 0;
			if (CallsInputDialogActivity.this != null) {
				double[] ret = CallStatDatabase.getInstance(
						CallsInputDialogActivity.this).getThisMonthTotalSpend();
				feeSpendTmp = ret[0];
			}
			if (feeSpendTmp < 0) {
				feeSpendTmp = 0;
			}
			calls_spent_et.setText(df.format(feeSpendTmp));
		}

		OK_btn = (Button) findViewById(R.id.ok_btn);
		cancel_btn = (Button) findViewById(R.id.cancel_btn);

	}

	private TextWatcher watcher = new TextWatcher() {

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			// TODO Auto-generated method stub

		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			// TODO Auto-generated method stub

		}

		@Override
		public void afterTextChanged(Editable s) {
			// TODO Auto-generated method stub
			String editText = s.toString();

			int position = -1;
			if (editText.contains(".")) {
				position = editText.indexOf(".");
			}
			if (editText.length() == 1 && editText.equals(".")) {
				calls_remain_et.setText("0.");
			} else if (editText.length() == 2 && editText.startsWith("0")
					&& editText.charAt(1) != '.') {
				calls_remain_et.setText("0");
			} else if (editText.length() == 8 && editText.endsWith(".")) {
				String str = editText.substring(0, 7);
				calls_remain_et.setText(str);
			} else if (editText.contains(".")
					&& editText.length() == position + 4) {
				String str1 = editText.substring(0, position + 3);
				calls_remain_et.setText(str1);
			} else if (editText.length() == 6
					&& Float.parseFloat(editText) > 99999) {
				String str2 = editText.substring(0, 5);
				calls_remain_et.setText(str2);
			}
			calls_remain_et.setSelection(calls_remain_et.getText().toString()
					.length());

		}
	};
	private TextWatcher watcher1 = new TextWatcher() {

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			// TODO Auto-generated method stub

		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			// TODO Auto-generated method stub

		}

		@Override
		public void afterTextChanged(Editable s) {
			// TODO Auto-generated method stub
			String editText = s.toString();

			int position = -1;
			if (editText.contains(".")) {
				position = editText.indexOf(".");
			}
			if (editText.length() == 1 && editText.equals(".")) {
				calls_spent_et.setText("0.");
			} else if (editText.length() == 2 && editText.startsWith("0")
					&& editText.charAt(1) != '.') {
				calls_spent_et.setText("0");
			} else if (editText.length() == 8 && editText.endsWith(".")) {
				String str = editText.substring(0, 7);
				calls_spent_et.setText(str);
			} else if (editText.contains(".")
					&& editText.length() == position + 4) {
				String str1 = editText.substring(0, position + 3);
				calls_spent_et.setText(str1);
			} else if (editText.length() == 6
					&& Float.parseFloat(editText) > 99999) {
				String str2 = editText.substring(0, 5);
				calls_spent_et.setText(str2);
			}
			calls_spent_et.setSelection(calls_spent_et.getText().toString()
					.length());

		}
	};

}
