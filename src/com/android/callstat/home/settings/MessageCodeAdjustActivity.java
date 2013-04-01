package com.android.callstat.home.settings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.protocol.HTTP;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.callstat.CallStatApplication;
import com.android.callstat.ConfigManager;
import com.android.callstat.ILog;
import com.android.callstat.accounting.AccountingKeyWordsBean;
import com.android.callstat.accounting.ReconciliationUtils;
import com.android.callstat.common.CallStatUtils;
import com.android.callstat.common.ResourceLoader.OnLoadFinishListener;
import com.android.callstat.common.database.CallStatDatabase;
import com.android.callstat.common.json.AbstractWebLoadManager.OnWebLoadListener;
import com.android.callstat.common.net.MyHttpPostHelper;
import com.android.callstat.home.views.ToastFactory;
import com.android.callstat.service.json.AccountingDatabaseUpdater;
import com.archermind.callstat.R;

public class MessageCodeAdjustActivity extends Activity implements
		OnClickListener {

	private EditText number_et;
	private EditText HFYeCode_et;
	private EditText HFUsedCode_et;
	private EditText GprsUsedCode_et;
	private EditText GprsYeCode_et;
	private Button net_update_btn; // 联网更新
	private Button confirm_btn; // 确定

	private ConfigManager config;
	private ProgressDialog mPd;
	private AccountingDatabaseUpdater accountingDatabaseUpdater;
	private CallStatDatabase callStatDatabase;

	private String HFYeCode = "";
	private String HFUsedCode = "";
	private String GprsUsedCode = "";
	private String GprsYeCode = "";
	private String OperatorNumber = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.code_correct_after_failed);
		config = new ConfigManager(this);
		callStatDatabase = CallStatDatabase.getInstance(this);
		init();
	}

	@Override
	protected void onResume() {
		super.onResume();
		editText_data();
	}

	// editText赋值
	private void editText_data() {
		HFYeCode = config.getHFYeCode();
		HFUsedCode = config.getHFUsedCode();
		GprsUsedCode = config.getGprsUsedCode();
		GprsYeCode = config.getGprsYeCode();
		OperatorNumber = config.getOperatorNum();
		number_et.setText(OperatorNumber);
		HFYeCode_et.setText(HFYeCode);
		HFUsedCode_et.setText(HFUsedCode);
		GprsUsedCode_et.setText(GprsUsedCode);
		GprsYeCode_et.setText(GprsYeCode);
	}

	private void init() {
		number_et = (EditText) findViewById(R.id.oprater_number_et);
		HFYeCode_et = (EditText) findViewById(R.id.calls_remain_et);
		HFUsedCode_et = (EditText) findViewById(R.id.calls_used_et);
		GprsUsedCode_et = (EditText) findViewById(R.id.traffic_used_et);
		GprsYeCode_et = (EditText) findViewById(R.id.traffic_remain_et);
		number_et.setOnClickListener(this);
		HFYeCode_et.setOnClickListener(this);
		HFUsedCode_et.setOnClickListener(this);
		GprsUsedCode_et.setOnClickListener(this);
		GprsYeCode_et.setOnClickListener(this);

		net_update_btn = (Button) findViewById(R.id.net_update_btn);
		confirm_btn = (Button) findViewById(R.id.adjust_confirm_btn);
		net_update_btn.setOnClickListener(this);
		confirm_btn.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.net_update_btn: // 联网更新
			checkAccountingCodeUpdate();
			break;
		case R.id.adjust_confirm_btn: // 确定
			confirm();
			break;
		case R.id.oprater_number_et:
			number_et.requestFocus();
			number_et.setCursorVisible(true);
			number_et.selectAll();
			break;
		case R.id.calls_remain_et:
			HFYeCode_et.requestFocus();
			HFYeCode_et.setCursorVisible(true);
			HFYeCode_et.selectAll();
			break;
		case R.id.calls_used_et:
			HFUsedCode_et.requestFocus();
			HFUsedCode_et.setCursorVisible(true);
			HFUsedCode_et.selectAll();
			break;
		case R.id.traffic_used_et:
			GprsUsedCode_et.requestFocus();
			GprsUsedCode_et.setCursorVisible(true);
			GprsUsedCode_et.selectAll();
			break;
		case R.id.traffic_remain_et:
			GprsYeCode_et.requestFocus();
			GprsYeCode_et.setCursorVisible(true);
			GprsYeCode_et.selectAll();
			break;
		default:
			break;
		}
	}

	// 确定按钮判断
	private void confirm() {
		String HFYeCode = HFYeCode_et.getText().toString();
		String HFUsedCode = HFUsedCode_et.getText().toString();
		String GprsUsedCode = GprsUsedCode_et.getText().toString();
		String GprsYeCode = GprsYeCode_et.getText().toString();
		boolean[] IS_CHANGE = new boolean[] { false, false, false, false };
		if (!"".equals(HFYeCode) && !"".equals(HFUsedCode)
				&& !"".equals(GprsUsedCode) && !"".equals(GprsYeCode)) {
			// 检测到修改了查询可用话费指令
			if (!config.getHFYeCode().equals(HFYeCode)) {
				IS_CHANGE[0] = true;
				config.setHFYeCode(HFYeCode);
			}

			if (!config.getHFUsedCode().equals(HFUsedCode)) {
				IS_CHANGE[1] = true;
				config.setHFUsedCode(HFUsedCode);
			}

			if (!config.getGprsYeCode().equals(GprsYeCode)) {
				IS_CHANGE[2] = true;
				config.setGprsYeCode(GprsYeCode);
			}

			if (!config.getGprsUsedCode().equals(GprsUsedCode)) {
				IS_CHANGE[3] = true;
				config.setGprsUsedCode(GprsUsedCode);
			}

			if (IS_CHANGE[0] || IS_CHANGE[1] || IS_CHANGE[2] || IS_CHANGE[3]) {
				toast("指令修改成功");
			}

			// 判断是否修改了指令
			if (IS_CHANGE[0]) {
				if (ReconciliationUtils.IsModifyAccount) {
					toast("后台正在对账，请稍候！");
				}
				ReconciliationUtils
						.getInstance()
						.getHandler()
						.sendEmptyMessage(
								ReconciliationUtils.MODIFY_INSTRUCTION_CALLS);
			}

			if (IS_CHANGE[1]) {
				if (ReconciliationUtils.IsModifyAccount) {
					toast("后台正在对账，请稍候！");
				}
				ReconciliationUtils
						.getInstance()
						.getHandler()
						.sendEmptyMessage(
								ReconciliationUtils.MODIFY_INSTRUCTION_CALLS);
			}

			if (IS_CHANGE[2]) {
				if (ReconciliationUtils.IsModifyTraffic) {
					toast("后台正在对账，请稍候！");
				}
				ReconciliationUtils
						.getInstance()
						.getHandler()
						.sendEmptyMessage(
								ReconciliationUtils.MODIFY_INSTRUCTION_TRAFFIC);
			}

			if (IS_CHANGE[3]) {
				if (ReconciliationUtils.IsModifyTraffic) {
					toast("后台正在对账，请稍候！");
				}
				ReconciliationUtils
						.getInstance()
						.getHandler()
						.sendEmptyMessage(
								ReconciliationUtils.MODIFY_INSTRUCTION_TRAFFIC);
			}
			finish();
		} else {
			toast("查询指令不能为空！");
		}
	}

	// 联网更新
	private void checkAccountingCodeUpdate() {
		if (CallStatUtils.isNetworkAvailable(this)) {
			String url = getString(R.string.update_code_by_city_url);
			ILog.LogI(this.getClass(), url);
			if (accountingDatabaseUpdater == null) {
				accountingDatabaseUpdater = new AccountingDatabaseUpdater(
						getBaseContext(), url);
			}
			Map<String, String> keyValuePairs = new HashMap<String, String>();
			String province = config.getProvince();
			String city = config.getCity();
			String operator = config.getOperator();
			String brand = config.getPackageBrand();
			ILog.LogI(this.getClass(), province + " " + operator + " " + brand);
			keyValuePairs.put("province", province);
			keyValuePairs.put("city", city);
			keyValuePairs.put("operator", operator);
			keyValuePairs.put("brand", brand);
			HttpEntity entity = MyHttpPostHelper.buildUrlEncodedFormEntity(
					keyValuePairs, HTTP.UTF_8);
			accountingDatabaseUpdater
					.setManagerListener(accountingDatabaseUpdateListener);
			accountingDatabaseUpdater.startManager(entity);
			if (mPd == null || !mPd.isShowing()) {
				mPd = ProgressDialog.show(this, null, "正在请求更新，请稍候...");
				mPd.setCancelable(true);
			}

		} else {
			toast("当前网络不可用，请检查是否开启网络功能");
		}
	}

	private OnWebLoadListener<List<AccountingKeyWordsBean>> accountingDatabaseUpdateListener = new OnWebLoadListener<List<AccountingKeyWordsBean>>() {

		@Override
		public void OnStart() {

		}

		@Override
		public void OnCancel() {
			if (mPd != null && mPd.isShowing()) {
				mPd.dismiss();
			}
		}

		@Override
		public void OnLoadComplete(int statusCode) {
			switch (statusCode) {
			case OnLoadFinishListener.ERROR_TIMEOUT:
				/*
				 * new Handler().postDelayed(new Runnable() {
				 * 
				 * @Override public void run() { checkUpdate(); } }, 30000);
				 */
				if (mPd != null && mPd.isShowing()) {
					mPd.dismiss();
				}
				toast("当前网络不可用，请检查是否开启网络功能");
				break;
			case OnLoadFinishListener.ERROR_REQUEST_FAILED:
				break;
			case OnLoadFinishListener.ERROR_REQ_REFUSED:
				if (mPd != null && mPd.isShowing()) {
					mPd.dismiss();
				}
				toast("当前网络不可用，请检查是否被防火墙禁用了网络");
				break;
			default:
				break;
			}
		}

		@Override
		public void OnPaserComplete(List<AccountingKeyWordsBean> list) {
			if (mPd != null && mPd.isShowing()) {
				mPd.setMessage("数据请求完成，正在解析...");
			}
			if (list == null || list.size() == 0) {
				if (mPd != null && mPd.isShowing()) {
					mPd.dismiss();
				}
				editText_data();
				toast("当前已是最新数据");
			} else {
				new UpdateReconciliationCodeThread().execute(list);
				// getCode();
			}
			long now = System.currentTimeMillis();
			config.setCodeUpdateTime(now);
		}
	};

	class UpdateReconciliationCodeThread extends AsyncTask<Object, Void, Void> {

		@Override
		protected void onCancelled() {
			super.onCancelled();
			if (mPd != null && mPd.isShowing()) {
				mPd.dismiss();
			}
			toast("联网更新被取消");
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if (mPd != null && mPd.isShowing()) {
				mPd.setMessage("正在保存数据到本地...");
			}
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			if (mPd != null && mPd.isShowing()) {
				mPd.dismiss();
			}
			toast("数据更新成功");
			// 更新配置文件
			callStatDatabase.initReconciliationInfo2ConfigXml(
					config.getProvince(), config.getCity(),
					config.getOperator(), config.getPackageBrand(),
					CallStatApplication.AllCodeRestore);
			editText_data();
		}

		@Override
		protected Void doInBackground(Object... params) {
			List<AccountingKeyWordsBean> list = (List<AccountingKeyWordsBean>) params[0];
			CallStatDatabase.getInstance(MessageCodeAdjustActivity.this)
					.updateReconciliationCodeList(list);
			return null;
		}

	}

	private void toast(String str) {
		try {
			if (CallStatUtils.isMyAppOnDesk(this)) {
				ToastFactory.getToast(MessageCodeAdjustActivity.this, str,
						Toast.LENGTH_LONG).show();
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

}
