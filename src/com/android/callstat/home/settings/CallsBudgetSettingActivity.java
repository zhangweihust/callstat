package com.android.callstat.home.settings;

import java.text.DecimalFormat;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.callstat.ConfigManager;
import com.android.callstat.common.CallStatUtils;
import com.archermind.callstat.R;

public class CallsBudgetSettingActivity extends Activity implements
		OnClickListener, SeekBar.OnSeekBarChangeListener, OnTouchListener {
	private ConfigManager config;
	private SeekBar mSeekBar;
	private TextView mProgressText;
	int seekbar_progress;

	private RelativeLayout back_rl, modify;
	private float call_budget;

	private EditText call_budget_et;
	private float calls_alert;

	// private EditText remain_alert_et;

	/**
	 * 与CallStatSMSService进行绑定
	 */
	// private CallStatSMSService.SMSBinder binder;
	// private ServiceConnection conn = new ServiceConnection() {
	//
	// @Override
	// public void onServiceDisconnected(ComponentName name) {
	//
	// }
	//
	// @Override
	// public void onServiceConnected(ComponentName name, IBinder service) {
	// binder = (CallStatSMSService.SMSBinder) service;
	// }
	// };

	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
		setContentView(R.layout.call_budget);
		config = new ConfigManager(this);

		initUI();
		initListener();
	}

	@Override
	protected void onResume() {
		super.onResume();
		/* 绑定服务 */
		// Intent intent = new Intent(this, CallStatSMSService.class);
		// if (!CallStatUtils.isServiceRunning(this,
		// "com.archermind.callstat.service.CallStatSMSService")) {
		// startService(intent);
		// }
		// bindService(intent, conn, Context.BIND_AUTO_CREATE);
	}

	private void initListener() {
		mSeekBar.setOnSeekBarChangeListener(this);
		call_budget_et.setOnTouchListener(this);
		back_rl.setOnClickListener(this);
		modify.setOnClickListener(this);
		// remain_alert_et.setOnTouchListener(this);
		call_budget_et.addTextChangedListener(watcher);
		// remain_alert_et.addTextChangedListener(watcher1);

	}

	private void initUI() {
		mSeekBar = (SeekBar) findViewById(R.id.seek);
		call_budget = config.getCallsBudget();
		mProgressText = (TextView) findViewById(R.id.progress);
		// remain_alert_et = (EditText) findViewById(R.id.remain_alert_et);
		// remain_alert_et.setText(config.getAlertRemainFees()+"");
		calls_alert = (float) call_budget - config.getAlertCallsNotice();
		seekbar_progress = (int) Math.round(calls_alert / (float) call_budget
				* 100);
		mSeekBar.setProgress((seekbar_progress - 60) * 5 / 2);
		mProgressText.setText(seekbar_progress + "%");
		call_budget_et = (EditText) findViewById(R.id.calls_et);
		if (call_budget != 100000) {
			call_budget_et.setText(CallStatUtils.changeFloat(call_budget));
		} else {
			call_budget_et.setText("");
		}
		back_rl = (RelativeLayout) findViewById(R.id.back_rl);
		modify = (RelativeLayout) findViewById(R.id.modify);
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.back_rl:
			break;
		case R.id.modify:
			savedata();
			Toast.makeText(getApplication(), "修改成功", Toast.LENGTH_SHORT).show();
			break;
		}
		finish();
		overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);

	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		if (v == call_budget_et) {
			call_budget_et.requestFocus();
			call_budget_et.selectAll();
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showSoftInput(v, 0);
			return true;
		}
		// else if (v == remain_alert_et) {
		// remain_alert_et.requestFocus();
		// remain_alert_et.selectAll();
		// InputMethodManager imm = (InputMethodManager)
		// getSystemService(Context.INPUT_METHOD_SERVICE);
		// imm.showSoftInput(v, 0);
		// return true;
		// }
		else {
			return false;
		}

	}

	@Override
	protected void onPause() { // TODO Auto-generated method stub
		super.onPause();
		// unbindService(conn);
	}

	@Override
	// 在拖动中--即值在改变
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		// progress为当前数值的大小

		seekbar_progress = 60 + progress * 2 / 5;
		mProgressText.setText(seekbar_progress + "%");
	}

	@Override
	// 在拖动中会调用此方法
	public void onStartTrackingTouch(SeekBar seekBar) {

	}

	@Override
	// 停止拖动
	public void onStopTrackingTouch(SeekBar seekBar) {

	}

	private void savedata() {

		if (call_budget_et.getText().toString() != null
				&& !"".equals(call_budget_et.getText().toString())) {
			call_budget = Float.parseFloat(call_budget_et.getText().toString());
			call_budget = Float.parseFloat(new DecimalFormat("#.00")
					.format(call_budget));
		} else {
			call_budget = 100000;
		}

		// if (!(call_budget_et.getText().toString().equals("") ||
		// call_budget_et
		// .getText().toString().equals(null))) {
		// call_budget = Integer.parseInt(call_budget_et.getText().toString());
		// } else {
		// Toast.makeText(this, "话费预算不能为空！", 3000).show();
		// }
		config.setCallsBudget(call_budget);
		// if(!remain_alert_et.getText().toString().equals("")){
		// config.setAlertRemainFees(Float.parseFloat(remain_alert_et.getText().toString()));
		// }
		// else{
		// config.setAlertRemainFees(0);
		// }
		calls_alert = call_budget * (100 - seekbar_progress) / 100f;
		config.setAlertCallsNotice(calls_alert);

		// if (config.getStatusKeepNotice()) {
		// if (binder != null) {
		// binder.open_notification();
		// }
		// }
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
			if (editText.length() == 1 && editText.equals("0")) {
				call_budget_et.setText("");
			}
		}
	};

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
	// private TextWatcher watcher1 = new TextWatcher() {
	//
	// @Override
	// public void afterTextChanged(Editable s) {
	// // TODO Auto-generated method stub
	//
	// }
	//
	// @Override
	// public void beforeTextChanged(CharSequence s, int start, int count,
	// int after) {
	// // TODO Auto-generated method stub
	//
	// }
	//
	// @Override
	// public void onTextChanged(CharSequence s, int start, int before,
	// int count) {
	// String editText = s.toString();
	//
	// int position = -1;
	// if (editText.contains("."))
	// {
	// position=editText.indexOf(".");
	// }
	// if (editText.length() == 1 && editText.equals(".")) {
	// remain_alert_et.setText("0.");
	// }else if(editText.length() == 2 && editText.startsWith("0")&&
	// editText.charAt(1)!='.'){
	// remain_alert_et.setText("0");
	// }
	// else if (editText.length() == 8 && editText.endsWith(".")) {
	// String str = editText.substring(0,7);
	// remain_alert_et.setText(str);
	// } else if (editText.contains(".") && editText.length() == position+4 ) {
	// String str1 = editText.substring(0,position+3);
	// remain_alert_et.setText(str1);
	// }else if(editText.length()==7&& Float.parseFloat(editText)>999999 ){
	// String str2 = editText.substring(0,6);
	// remain_alert_et.setText(str2);
	// }
	// remain_alert_et.setSelection(remain_alert_et.getText().toString().length());
	//
	// }
	// };

}
