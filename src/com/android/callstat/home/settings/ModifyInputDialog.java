package com.android.callstat.home.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.callstat.CallStatApplication;
import com.android.callstat.ConfigManager;
import com.android.callstat.common.database.CallStatDatabase;
import com.android.callstat.home.views.ToastFactory;
import com.archermind.callstat.R;

public class ModifyInputDialog extends Activity implements OnClickListener {
	private int id;
	private TextView mTitleView;
	private Button cancel_btn, ok_btn;
	private EditText inputET;

	private CallStatDatabase callStatDatabase;
	private ConfigManager config;

	private String HFYeCode = "";
	private String HFUsedCode = "";
	private String GprsUsedCode = "";
	private String GprsYeCode = "";
	private String OperatorNumber = "";
	private CallStatApplication callstatApplication;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.new_dilog_modify);
		config = new ConfigManager(this);
		callStatDatabase = CallStatDatabase.getInstance(this);
		callstatApplication = (CallStatApplication) getApplication();
		Intent intent = getIntent();
		id = intent.getIntExtra("id", 0);
		HFYeCode = intent.getStringExtra("HFYeCode");
		HFUsedCode = intent.getStringExtra("HFUsedCode");
		GprsYeCode = intent.getStringExtra("GprsYeCode");
		GprsUsedCode = intent.getStringExtra("GprsUsedCode");
		OperatorNumber = intent.getStringExtra("OperatorNumber");
		initView();
	}

	private void initView() {
		mTitleView = (TextView) findViewById(R.id.dialog_title_tv);
		cancel_btn = (Button) findViewById(R.id.cancel_btn);
		ok_btn = (Button) findViewById(R.id.ok_btn);
		inputET = (EditText) findViewById(R.id.input);

		cancel_btn.setOnClickListener(this);
		ok_btn.setOnClickListener(this);
		inputET.setOnClickListener(this);

		if (id == 0) {
			mTitleView.setText("请输入查询已用话费指令");
			inputET.setText(HFUsedCode);
		} else if (id == 1) {
			mTitleView.setText("请输入查询可用话费指令");
			inputET.setText(HFYeCode);
		} else if (id == 2) {
			mTitleView.setText("请输入查询已用流量指令");
			inputET.setText(GprsUsedCode);
		} else if (id == 3) {
			mTitleView.setText("请输入查询可用流量指令");
			inputET.setText(GprsYeCode);
		} else if (id == 4) {
			inputET.setRawInputType(InputType.TYPE_CLASS_NUMBER);
			mTitleView.setText("请输入运营商号码");
			inputET.setText(OperatorNumber);
		}
	}

	private void sure() {
		String newCode = inputET.getText().toString().trim();
		if (!"".equals(newCode) && newCode != null) {
			if (id == 0) {
				if (!HFUsedCode.equals(newCode)) {
					// String HFUsedCode = config.getHFUsedCode();
					// HFUsedCode = HFUsedCode.replace(HFUsedCode.substring(0,
					// HFUsedCode.indexOf(";")),newCode);
					config.setHFUsedCode(newCode);
				}

			} else if (id == 1) {
				if (!HFYeCode.equals(newCode)) {
					// String HFYeCode = config.getHFYeCode();
					// HFYeCode = HFYeCode.replace(HFYeCode.substring(0,
					// HFYeCode.indexOf(";")),newCode);
					config.setHFYeCode(newCode);
				}

			} else if (id == 2) {
				if (!GprsUsedCode.equals(newCode)) {
					// String GprsUsedCode = config.getGprsUsedCode();
					// GprsUsedCode =
					// GprsUsedCode.replace(GprsUsedCode.substring(0,
					// GprsUsedCode.indexOf(";")),newCode);
					config.setGprsUsedCode(newCode);
				}

			} else if (id == 3) {
				if (!GprsYeCode.equals(newCode)) {
					// String GprsYeCode = config.getGprsYeCode();
					// GprsYeCode = GprsYeCode.replace(GprsYeCode.substring(0,
					// GprsYeCode.indexOf(";")),newCode);
					config.setGprsYeCode(newCode);
				}

			} else if (id == 4) {
				if (!OperatorNumber.equals(newCode)) {
					// String GprsYeCode = config.getGprsYeCode();
					// GprsYeCode = GprsYeCode.replace(GprsYeCode.substring(0,
					// GprsYeCode.indexOf(";")),newCode);
					config.setOperatorNum(newCode);
				}

			}
			finish();
		} else {
			if (id == 4) {
				showToast("运营商号码不能为空！");
			} else {
				showToast("查询指令不能为空！");
			}

		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.cancel_btn:
			finish();
			break;
		case R.id.ok_btn:
			sure();
			break;
		case R.id.input:
			// Log.i("www", "input onclick!!!-------");
			inputET.requestFocus();
			inputET.setCursorVisible(true);
			// Selection.selectAll(inputET.getText());
			inputET.selectAll();
			break;
		default:
			break;
		}
	}

	private void showToast(String string) {
		ToastFactory.getToast(getApplicationContext(), string,
				Toast.LENGTH_SHORT).show();
	}

}
