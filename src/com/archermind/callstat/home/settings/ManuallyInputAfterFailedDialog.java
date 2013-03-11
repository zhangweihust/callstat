package com.archermind.callstat.home.settings;

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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.archermind.callstat.ConfigManager;
import com.archermind.callstat.R;
import com.archermind.callstat.service.CallStatSMSService;

public class ManuallyInputAfterFailedDialog extends Activity implements
		OnClickListener, OnTouchListener {

	private EditText calls_remain_eText;
	private RelativeLayout code_rl;
	private Button cancelBtn;
	private Button confirmBtn;
	private TextView titleTextView;
	private TextView contentTextView;
	private TextView unitTextView;
	private ImageView manual_code_arrow;
	private TextView manual_code_string;
	private int index = 0;
	ConfigManager config;
	private java.text.DecimalFormat df = new java.text.DecimalFormat("#.##");

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
		setContentView(R.layout.manual_correct_after_failed);
		config = new ConfigManager(this);
		init();
		if (getIntent().getExtras() != null) {
			index = getIntent().getExtras().getInt("manual_correct_index");
		}

		data(index);

		/* 绑定服务 */
		Intent intent = new Intent(this, CallStatSMSService.class);
		bindService(intent, conn, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(conn);
	}

	private void init() {
		calls_remain_eText = (EditText) findViewById(R.id.manual_calls_remain_et);
		code_rl = (RelativeLayout) findViewById(R.id.manual_code_rl);
		cancelBtn = (Button) findViewById(R.id.manual_cancel_btn);
		confirmBtn = (Button) findViewById(R.id.manual_confirm_btn);
		titleTextView = (TextView) findViewById(R.id.manual_title);
		contentTextView = (TextView) findViewById(R.id.manual_calls_string);
		unitTextView = (TextView) findViewById(R.id.manual_unit);
		manual_code_arrow = (ImageView) findViewById(R.id.manual_code_arrow);
		manual_code_string = (TextView) findViewById(R.id.manual_code_string);

		code_rl.setOnClickListener(this);
		cancelBtn.setOnClickListener(this);
		confirmBtn.setOnClickListener(this);
		code_rl.setOnTouchListener(this);

		calls_remain_eText.addTextChangedListener(watcher);
		calls_remain_eText.setOnClickListener(this);
	}

	private void data(int index) {
		switch (index) {
		case 0:
			// titleTextView.setText("手动校正余额");
			contentTextView.setText("话费余额");
			unitTextView.setText("元");

			// EditText中显示余额
			if (config.getCalculateFeeAvailable() != 100000f) {
				calls_remain_eText.setText(df.format(config
						.getCalculateFeeAvailable()));
			}

			break;
		case 1:
			// titleTextView.setText("手动校正流量");
			contentTextView.setText("流量已用");
			unitTextView.setText("MB");

			// EditText中显示已用流量
			/* 本月使用的流量 */
			long month_used = config.getTotalGprsUsed()
					+ config.getTotalGprsUsedDifference();
			String month_used_string = df.format(month_used / 1024f / 1024f);
			calls_remain_eText.setText(month_used_string);
			break;
		default:
			break;
		}

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.manual_code_rl: // 短信指令调整
			startActivity(new Intent(ManuallyInputAfterFailedDialog.this,
					MessageCodeAdjustActivity.class));
			break;
		case R.id.manual_cancel_btn: // 取消按钮
			finish();
			break;
		case R.id.manual_confirm_btn: // 确定按钮
			// if (index == 0 &&
			// !"".equals(calls_remain_eText.getText().toString())) {
			// float callsRemain = Float.parseFloat(calls_remain_eText
			// .getText().toString());
			// if (callsRemain != config.getCalculateFeeAvailable()) {
			// config.setCalculateFeeAvailable(callsRemain);
			// config.setFeesRemain(callsRemain);
			// if (config.getStatusKeepNotice()) {
			// if (binder != null) {
			// binder.open_notification();
			// }
			// }
			// }
			// } else if (index == 1
			// && !"".equals(calls_remain_eText.getText().toString())) {
			// long gprs = (long) (Float.parseFloat(calls_remain_eText
			// .getText().toString()) * 1024 * 1024);
			// long difference = gprs - config.getTotalGprsUsed();
			// if (difference != config.getTotalGprsUsedDifference()) {
			// config.setTotalGprsUsedDifference(difference);
			// config.setMonthTrafficWarn(false);
			// config.setTrafficBeyond(false);
			// if (config.getStatusKeepNotice()) {
			// if (binder != null) {
			// binder.open_notification();
			// }
			// }
			// }
			// }
			finish();
			break;
		case R.id.manual_calls_remain_et:
			calls_remain_eText.requestFocus();
			calls_remain_eText.setCursorVisible(true);
			// Selection.selectAll(inputET.getText());
			calls_remain_eText.selectAll();
			break;
		default:
			break;
		}

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
				calls_remain_eText.setText("0.");
			} else if (editText.length() == 2 && editText.startsWith("0")
					&& editText.charAt(1) != '.') {
				calls_remain_eText.setText("0");
			} else if (editText.length() == 8 && editText.endsWith(".")) {
				String str = editText.substring(0, 7);
				calls_remain_eText.setText(str);
			} else if (editText.contains(".")
					&& editText.length() == position + 4) {
				String str1 = editText.substring(0, position + 3);
				calls_remain_eText.setText(str1);
			} else if (editText.length() == 6
					&& Float.parseFloat(editText) > 99999) {
				String str2 = editText.substring(0, 5);
				calls_remain_eText.setText(str2);
			}
			calls_remain_eText.setSelection(calls_remain_eText.getText()
					.toString().length());

		}
	};

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (v.getId()) {
		case R.id.manual_code_rl:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				manual_code_string.setTextColor(getResources().getColor(
						R.color.white));
				code_rl.setBackgroundResource(R.drawable.set_biglist_blue);
				manual_code_arrow
						.setImageResource(R.drawable.arrow_right_light);
				return false;

			case MotionEvent.ACTION_UP:
				manual_code_string.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				code_rl.setBackgroundResource(R.drawable.set_biglist_bar);
				manual_code_arrow.setImageResource(R.drawable.arrow_right);
				return false;
			case MotionEvent.ACTION_CANCEL:
				manual_code_string.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				code_rl.setBackgroundResource(R.drawable.set_biglist_bar);
				manual_code_arrow.setImageResource(R.drawable.arrow_right);
				return false;
			default:
				break;
			}

			break;
		default:
			break;
		}
		return false;
	}

}
