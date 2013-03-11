package com.archermind.callstat.home;

import java.text.DecimalFormat;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.archermind.callstat.ConfigManager;
import com.archermind.callstat.ILog;
import com.archermind.callstat.R;

public class SecondInitInfoSettingActivity extends Activity implements
		OnTouchListener, OnClickListener {
	private ConfigManager config;
	private float call_budget;
	private EditText call_budget_et;
	private EditText traffic_et;
	private Button toback;
	private Button finish_btn;
	float FreeGprs;
	ProgressDialog progressDialog;

	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.second_init_info_setting);
		config = new ConfigManager(this);
		initUI();
		initListener();
		// for advanced part

	}

	private void initUI() {
		try {
			call_budget = config.getCallsBudget();
			traffic_et = (EditText) findViewById(R.id.traffic_et);
			call_budget_et = (EditText) findViewById(R.id.calls_et);
			if (call_budget == 100000) {
				call_budget_et.setText("");
			} else {
				call_budget_et.setText(call_budget + "");
			}

			if (config.getFreeGprs() == 100000) {
				traffic_et.setText("");
			} else {
				traffic_et.setText((int) config.getFreeGprs() + "");
			}

			toback = (Button) findViewById(R.id.toback);
			finish_btn = (Button) findViewById(R.id.finish_btn);
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	private void initListener() {
		try {
			call_budget_et.setOnTouchListener(this);
			traffic_et.setOnTouchListener(this);
			toback.setOnClickListener(this);
			finish_btn.setOnClickListener(this);
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (v == call_budget_et) {
			call_budget_et.requestFocus();
			call_budget_et.selectAll();
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showSoftInput(v, 0);
			return true;
		} else if (v == traffic_et) {
			traffic_et.requestFocus();
			traffic_et.selectAll();
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showSoftInput(v, 0);
			return true;
		} else {
			return false;
		}

	}

	@Override
	public void onClick(View v) {
		try {
			if (v == toback) {
				savedata();
				finish();
			} else if (v == finish_btn) {

				if (call_budget_et.getText().toString() == null
						|| "".equals(call_budget_et.getText().toString())) {
					Toast.makeText(getApplication(), "请设置预算话费",
							Toast.LENGTH_SHORT).show();
					return;
				}
				if (traffic_et.getText().toString().equals("")
						|| traffic_et.getText().toString().equals(null)) {
					Toast.makeText(getApplication(), "请设置包月流量",
							Toast.LENGTH_SHORT).show();
					return;
				}

				SavedataThread savedataThread = new SavedataThread();
				savedataThread.execute();
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	private void savedata() {
		try {
			if (!"".equals(traffic_et.getText().toString())) {
				FreeGprs = Float.parseFloat(traffic_et.getText().toString());
			} else {
				FreeGprs = 100000f;
			}
			if (!"".equals(call_budget_et.getText().toString())) {
				call_budget = Float.parseFloat(call_budget_et.getText()
						.toString());
				call_budget = Float.parseFloat(new DecimalFormat("#.00")
						.format(call_budget));
			} else {
				call_budget = 100000;
			}
			config.setCallsBudget(call_budget);
			config.setFreeGprs(FreeGprs);
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	class SavedataThread extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			savedata();
			return null;
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			if (progressDialog != null) {
				progressDialog.dismiss();
			}
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			try {
				Intent intent = new Intent();
				intent.setAction("first");
				intent.setClass(SecondInitInfoSettingActivity.this,
						CallStatMainActivity.class);
				startActivity(intent);
				NewInitInfoSettingActivity.reg.finish();
				finish();
			} catch (Exception e) {
				ILog.logException(this.getClass(), e);
			}
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressDialog = ProgressDialog.show(
					SecondInitInfoSettingActivity.this, null, "正在保存个人信息...",
					true);
			progressDialog.setCancelable(false);
		}

	}

}
