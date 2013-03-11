package com.archermind.callstat.home.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.archermind.callstat.CallStatApplication;
import com.archermind.callstat.ConfigManager;
import com.archermind.callstat.R;
import com.archermind.callstat.common.database.CallStatDatabase;

public class ManuallyModifyActivity extends Activity implements
		OnClickListener, OnTouchListener {

	private CallStatApplication callstatApplication;
	private RelativeLayout mBackLayout, HFYeCode_rl, HFUsedCode_rl,
			GprsUsedCode_rl, GprsYeCode_rl;
	private TextView HFYeCodeTextView, HFUsedCodeTextView,
			GprsUsedCodeTextView, GprsYeCodeTextView;
	private TextView HFYeCodeView, HFUsedCodeView, GprsUsedCodeView,
			GprsYeCodeView;

	private RelativeLayout operator_number_rl;
	private TextView operator_numberTextView;
	private TextView operator_number_nameTextView;
	private Button operator_numberRestore;

	// private ImageView backImageView;

	private ConfigManager config;

	private String HFYeCode = "";
	private String HFUsedCode = "";
	private String GprsUsedCode = "";
	private String GprsYeCode = "";
	private String OperatorNumber = "";
	private Button HFYeCodeRestore, HFUsedCodeRestore, GprsUsedCodeRestore,
			GprsYeCodeRestore;
	private CallStatDatabase callStatDatabase;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		callstatApplication = (CallStatApplication) getApplication();
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.manually_modify);
		callStatDatabase = CallStatDatabase.getInstance(this);
		config = new ConfigManager(this);
		initView();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		getCode();
		HFYeCodeView.setText(HFYeCode);
		HFUsedCodeView.setText(HFUsedCode);
		GprsUsedCodeView.setText(GprsUsedCode);
		GprsYeCodeView.setText(GprsYeCode);
		operator_numberTextView.setText(OperatorNumber);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		String restoreCode = "";
		String code = "";
		switch (v.getId()) {
		case R.id.query_back_rl:// 返回按钮
			finish();
			overridePendingTransition(R.anim.push_right_in,
					R.anim.push_right_out);
			break;
		case R.id.HFUsedCode_rl:// 已用话费查询
			toOtherActivity(ModifyInputDialog.class, 0);
			break;
		case R.id.HFYeCode_rl:// 可用话费查询
			toOtherActivity(ModifyInputDialog.class, 1);
			break;
		case R.id.GprsUsedCode_rl:// 已用流量查询
			toOtherActivity(ModifyInputDialog.class, 2);
			break;
		case R.id.GprsYeCode_rl:// 可用流量查询
			toOtherActivity(ModifyInputDialog.class, 3);
			break;
		case R.id.operator_number_rl:// 已用话费查询
			toOtherActivity(ModifyInputDialog.class, 4);
			break;
		case R.id.HFUsedCode_btn:// 恢复已用话费查询指令
			feesUsedCodeRestore();
			break;
		case R.id.HFYeCode_btn:// 恢复可用话费查询指令
			feesYeCodeRestore();
			break;
		case R.id.GprsUsedCode_btn:// 恢复已用流量查询指令
			trafficUsedCodeRestore();
			break;
		case R.id.GprsYeCode_btn:// 恢复可用流量查询指令
			trafficYeCodeRestore();
			break;
		case R.id.operator_number_btn:// 恢复运营商号码
			operatorNumberRestore();
			break;
		default:
			break;
		}

	}

	private void initView() {
		mBackLayout = (RelativeLayout) findViewById(R.id.query_back_rl);
		HFYeCode_rl = (RelativeLayout) findViewById(R.id.HFYeCode_rl);
		HFUsedCode_rl = (RelativeLayout) findViewById(R.id.HFUsedCode_rl);
		GprsUsedCode_rl = (RelativeLayout) findViewById(R.id.GprsUsedCode_rl);
		GprsYeCode_rl = (RelativeLayout) findViewById(R.id.GprsYeCode_rl);
		operator_number_rl = (RelativeLayout) findViewById(R.id.operator_number_rl);

		HFYeCodeView = (TextView) findViewById(R.id.HFYeCode);
		HFUsedCodeView = (TextView) findViewById(R.id.HFUsedCode);
		GprsYeCodeView = (TextView) findViewById(R.id.GprsYeCode);
		GprsUsedCodeView = (TextView) findViewById(R.id.GprsUsedCode);
		HFYeCodeTextView = (TextView) findViewById(R.id.HFYeCode_name);
		HFYeCodeTextView = (TextView) findViewById(R.id.HFYeCode_name);
		HFUsedCodeTextView = (TextView) findViewById(R.id.HFUsedCode_name);
		GprsUsedCodeTextView = (TextView) findViewById(R.id.GprsUsedCode_name);
		GprsYeCodeTextView = (TextView) findViewById(R.id.GprsYeCode_name);
		HFYeCodeRestore = (Button) findViewById(R.id.HFYeCode_btn);
		HFUsedCodeRestore = (Button) findViewById(R.id.HFUsedCode_btn);
		GprsUsedCodeRestore = (Button) findViewById(R.id.GprsUsedCode_btn);
		GprsYeCodeRestore = (Button) findViewById(R.id.GprsYeCode_btn);
		// backImageView = (ImageView) findViewById(R.id.refresh_img);
		operator_numberTextView = (TextView) findViewById(R.id.operator_number);
		operator_numberRestore = (Button) findViewById(R.id.operator_number_btn);
		operator_number_nameTextView = (TextView) findViewById(R.id.operator_number_name);

		mBackLayout.setOnClickListener(this);
		HFYeCode_rl.setOnClickListener(this);
		HFUsedCode_rl.setOnClickListener(this);
		GprsUsedCode_rl.setOnClickListener(this);
		GprsYeCode_rl.setOnClickListener(this);
		operator_number_rl.setOnClickListener(this);

		HFYeCodeRestore.setOnClickListener(this);
		HFUsedCodeRestore.setOnClickListener(this);
		GprsUsedCodeRestore.setOnClickListener(this);
		GprsYeCodeRestore.setOnClickListener(this);
		operator_numberRestore.setOnClickListener(this);

		mBackLayout.setOnTouchListener(this);
		HFYeCode_rl.setOnTouchListener(this);
		HFUsedCode_rl.setOnTouchListener(this);
		GprsUsedCode_rl.setOnTouchListener(this);
		GprsYeCode_rl.setOnTouchListener(this);
		operator_number_rl.setOnTouchListener(this);

	}

	private void getCode() {
		// if (callstatApplication.initReconciliationInfo()) {
		// if(callstatApplication.getReconciliationBean() != null) {
		// feesCode = callstatApplication.getReconciliationBean()
		// .getMessage(ReconciliationBean.TYPE_CALL_CHARGES)[0];
		// trafficCode = callstatApplication.getReconciliationBean()
		// .getMessage(ReconciliationBean.TYPE_TRAFFIC_QUERY_KEY)[0];
		// }
		// }

		HFYeCode = config.getHFYeCode();
		HFUsedCode = config.getHFUsedCode();
		GprsUsedCode = config.getGprsUsedCode();
		GprsYeCode = config.getGprsYeCode();
		OperatorNumber = config.getOperatorNum();
	}

	private void feesUsedCodeRestore() {
		callStatDatabase.initReconciliationInfo2ConfigXml(config.getProvince(),
				config.getCity(), config.getOperator(),
				config.getPackageBrand(),
				CallStatApplication.feesUsedCodeRestore);
		HFUsedCode = config.getHFUsedCode();
		HFUsedCodeView.setText(HFUsedCode);
	}

	private void feesYeCodeRestore() {
		callStatDatabase
				.initReconciliationInfo2ConfigXml(config.getProvince(),
						config.getCity(), config.getOperator(),
						config.getPackageBrand(),
						CallStatApplication.feesYeCodeRestore);
		HFYeCode = config.getHFYeCode();
		HFYeCodeView.setText(HFYeCode);
	}

	private void trafficUsedCodeRestore() {
		callStatDatabase.initReconciliationInfo2ConfigXml(config.getProvince(),
				config.getCity(), config.getOperator(),
				config.getPackageBrand(),
				CallStatApplication.trafficUsedCodeRestore);
		GprsUsedCode = config.getGprsUsedCode();
		GprsUsedCodeView.setText(GprsUsedCode);
	}

	private void trafficYeCodeRestore() {
		callStatDatabase.initReconciliationInfo2ConfigXml(config.getProvince(),
				config.getCity(), config.getOperator(),
				config.getPackageBrand(),
				CallStatApplication.trafficYeCodeRestore);
		GprsYeCode = config.getGprsYeCode();
		GprsYeCodeView.setText(GprsYeCode);
	}

	private void operatorNumberRestore() {
		callStatDatabase.initReconciliationInfo2ConfigXml(config.getProvince(),
				config.getCity(), config.getOperator(),
				config.getPackageBrand(),
				CallStatApplication.operatorNumberRestore);
		OperatorNumber = config.getOperatorNum();
		operator_numberTextView.setText(OperatorNumber);
	}

	@SuppressWarnings("rawtypes")
	private void toOtherActivity(Class class1, int id) {
		Intent intent = new Intent();
		intent.setClass(ManuallyModifyActivity.this, class1);
		intent.putExtra("id", id);
		intent.putExtra("HFYeCode", HFYeCode);
		intent.putExtra("HFUsedCode", HFUsedCode);
		intent.putExtra("GprsUsedCode", GprsUsedCode);
		intent.putExtra("GprsYeCode", GprsYeCode);
		intent.putExtra("OperatorNumber", OperatorNumber);
		startActivity(intent);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.HFYeCode_rl:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				HFYeCodeView.setTextColor(getResources()
						.getColor(R.color.white));
				HFYeCodeTextView.setTextColor(getResources().getColor(
						R.color.white));
				HFYeCode_rl.setBackgroundResource(R.drawable.set_biglist_blue);
				return false;

			case MotionEvent.ACTION_UP:
				HFYeCodeTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				HFYeCodeView.setTextColor(getResources().getColor(
						R.color.setting_text));
				HFYeCode_rl.setBackgroundResource(R.drawable.set_biglist_bar);
				return false;

			case MotionEvent.ACTION_CANCEL:
				HFYeCodeTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				HFYeCodeView.setTextColor(getResources().getColor(
						R.color.setting_text));
				HFYeCode_rl.setBackgroundResource(R.drawable.set_biglist_bar);
				return false;
			default:
				break;
			}

			break;
		case R.id.HFUsedCode_rl:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				HFUsedCodeView.setTextColor(getResources().getColor(
						R.color.white));
				HFUsedCodeTextView.setTextColor(getResources().getColor(
						R.color.white));
				HFUsedCode_rl
						.setBackgroundResource(R.drawable.set_biglist_blue);
				return false;

			case MotionEvent.ACTION_UP:
				HFUsedCodeTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				HFUsedCodeView.setTextColor(getResources().getColor(
						R.color.setting_text));
				HFUsedCode_rl.setBackgroundResource(R.drawable.set_biglist_bar);
				return false;

			case MotionEvent.ACTION_CANCEL:
				HFUsedCodeTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				HFUsedCodeView.setTextColor(getResources().getColor(
						R.color.setting_text));
				HFUsedCode_rl.setBackgroundResource(R.drawable.set_biglist_bar);
				return false;
			default:
				break;
			}

			break;
		case R.id.GprsUsedCode_rl:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				GprsUsedCodeView.setTextColor(getResources().getColor(
						R.color.white));
				GprsUsedCodeTextView.setTextColor(getResources().getColor(
						R.color.white));
				GprsUsedCode_rl
						.setBackgroundResource(R.drawable.set_biglist_blue);
				return false;

			case MotionEvent.ACTION_UP:
				GprsUsedCodeTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				GprsUsedCodeView.setTextColor(getResources().getColor(
						R.color.setting_text));
				GprsUsedCode_rl
						.setBackgroundResource(R.drawable.set_biglist_bar);
				return false;

			case MotionEvent.ACTION_CANCEL:
				GprsUsedCodeTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				GprsUsedCodeView.setTextColor(getResources().getColor(
						R.color.setting_text));
				GprsUsedCode_rl
						.setBackgroundResource(R.drawable.set_biglist_bar);
				return false;
			default:
				break;
			}

			break;
		case R.id.GprsYeCode_rl:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				GprsYeCodeView.setTextColor(getResources().getColor(
						R.color.white));
				GprsYeCodeTextView.setTextColor(getResources().getColor(
						R.color.white));
				GprsYeCode_rl
						.setBackgroundResource(R.drawable.set_biglist_blue);
				return false;

			case MotionEvent.ACTION_UP:
				GprsYeCodeTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				GprsYeCodeView.setTextColor(getResources().getColor(
						R.color.setting_text));
				GprsYeCode_rl.setBackgroundResource(R.drawable.set_biglist_bar);
				return false;

			case MotionEvent.ACTION_CANCEL:
				GprsYeCodeTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				GprsYeCodeView.setTextColor(getResources().getColor(
						R.color.setting_text));
				GprsYeCode_rl.setBackgroundResource(R.drawable.set_biglist_bar);
				return false;
			default:
				break;
			}

			break;
		case R.id.operator_number_rl:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				operator_numberTextView.setTextColor(getResources().getColor(
						R.color.white));
				operator_number_nameTextView.setTextColor(getResources()
						.getColor(R.color.white));
				operator_number_rl
						.setBackgroundResource(R.drawable.set_biglist_blue);
				return false;

			case MotionEvent.ACTION_UP:
				operator_number_nameTextView.setTextColor(getResources()
						.getColor(R.color.setting_text_black));
				operator_numberTextView.setTextColor(getResources().getColor(
						R.color.setting_text));
				operator_number_rl
						.setBackgroundResource(R.drawable.set_biglist_bar);
				return false;

			case MotionEvent.ACTION_CANCEL:
				operator_number_nameTextView.setTextColor(getResources()
						.getColor(R.color.setting_text_black));
				operator_numberTextView.setTextColor(getResources().getColor(
						R.color.setting_text));
				operator_number_rl
						.setBackgroundResource(R.drawable.set_biglist_bar);
				return false;
			default:
				break;
			}

			break;
		case R.id.query_back_rl:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				// backImageView.setImageResource(R.drawable.back_arrow_x);
				mBackLayout.setBackgroundResource(R.drawable.list_view_bg);
				return false;

			case MotionEvent.ACTION_UP:
				// backImageView.setImageResource(R.drawable.back_arrow);
				mBackLayout.setBackgroundResource(0);
				return false;

			case MotionEvent.ACTION_CANCEL:
				// backImageView.setImageResource(R.drawable.back_arrow);
				mBackLayout.setBackgroundResource(0);
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
