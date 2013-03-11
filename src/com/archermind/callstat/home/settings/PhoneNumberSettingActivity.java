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
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.archermind.callstat.ConfigManager;
import com.archermind.callstat.R;
import com.archermind.callstat.common.CallStatUtils;
import com.archermind.callstat.common.IsMobileUtil;
import com.archermind.callstat.common.database.CallStatDatabase;
import com.archermind.callstat.home.bean.PhoneNumberInfo;
import com.archermind.callstat.service.CallStatSMSService;

public class PhoneNumberSettingActivity extends Activity implements
		OnClickListener {
	private ConfigManager config;
	private EditText phonenum_et;
	private String phonenumber;
	private String phonenumber_change;

	// xml文件中对应的省份，城市的数组
	private int[] arrays = { R.array.beijing, R.array.shanghai,
			R.array.tianjin, R.array.chongqing, R.array.anhui, R.array.fujian,
			R.array.gansu, R.array.guangdong, R.array.guangxi, R.array.guizhou,
			R.array.hainan, R.array.hebei, R.array.henan, R.array.heilongjiang,
			R.array.hubei, R.array.hunan, R.array.jilin, R.array.jiangsu,
			R.array.jiangxi, R.array.liaoning, R.array.neimenggu,
			R.array.ningxia, R.array.qinghai, R.array.shandong,
			R.array.shanxi_01, R.array.shanxi_02, R.array.sichuan,
			R.array.xizang, R.array.xinjiang, R.array.yunnan, R.array.zhejiang };

	String[] cities;
	String[] brands;
	String[] provinces;
	String[] two_char_provinces;
	String[] operator;
	int cityarrayId = 0;
	int brandarrayId = 0;
	int cityId = 0;
	int brandId = 0;
	int pro_position = 0;
	int oper_position = 0;
	private RelativeLayout back_rl, modify;
	private String provinceStr = "";
	private String cityStr = "";
	private String opratorStr = "";
	private String brandStr = "";
	boolean isMobileNo;
	private Context context;
	// private Button cancel_btn;
	private TextView bindView;
	private CallStatSMSService.SMSBinder binder;

	private ServiceConnection conn = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub
			binder = (CallStatSMSService.SMSBinder) service;
		}
	};

	/*
	 * private BroadcastReceiver mReceiver = new BroadcastReceiver() {
	 * 
	 * @Override public void onReceive(Context context, Intent intent) { // TODO
	 * Auto-generated method stub
	 * 
	 * Log.i("wanglei", "BroadcastReceiver==" + intent.getAction()); if
	 * (intent.getAction().equals(CallStatSMSService.SEND_BIND_HANDLE)) {
	 * initView(); } else if (intent.getAction().equals(
	 * CallStatSMSService.ACTION_VERIFICATION_RESULT)) {
	 * binder.bindRemoveHandler(); initView(); }
	 * 
	 * } };
	 */
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.phone_number_setting);
		config = new ConfigManager(this);
		context = this;
		/*
		 * IntentFilter filter = new IntentFilter();
		 * filter.addAction(CallStatSMSService.ACTION_VERIFICATION_RESULT);
		 * filter.addAction(CallStatSMSService.SEND_BIND_HANDLE);
		 * registerReceiver(mReceiver, filter);
		 */
		Intent intent = new Intent(this, CallStatSMSService.class);
		if (!CallStatUtils.isServiceRunning(this,
				"com.archermind.callstat.service.CallStatSMSService")) {
			startService(intent);
		}
		bindService(intent, conn, Context.BIND_AUTO_CREATE);
		initUI();
		initListener();
		// initView();
	}

	@Override
	protected void onResume() {
		super.onResume();
		phonenum_et.setSelectAllOnFocus(true);
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		// unregisterReceiver(mReceiver);
		unbindService(conn);
	}

	private void initListener() {
		phonenum_et.addTextChangedListener(watcher);
		back_rl.setOnClickListener(this);
		modify.setOnClickListener(this);
		// cancel_btn.setOnClickListener(this);

	}

	private void initUI() {
		// TODO Auto-generated method stub
		provinces = getResources().getStringArray(R.array.provinces);
		two_char_provinces = getResources().getStringArray(
				R.array.two_chars_provinces);
		operator = getResources().getStringArray(R.array.oprator);
		// cancel_btn = (Button)findViewById(R.id.cancel_btn);
		phonenumber = config.getTopEightNum();
		phonenumber_change = config.getTopEightNum();
		phonenum_et = (EditText) findViewById(R.id.phonenum_et);
		phonenum_et.setText(phonenumber);
		back_rl = (RelativeLayout) findViewById(R.id.back_rl);
		modify = (RelativeLayout) findViewById(R.id.modify);
		// bindView = (TextView) findViewById(R.id.bindText);

	}

	/*
	 * private void initView(){ switch (config.getPhoneBindingStatus()) { case
	 * -1: cancel_btn.setVisibility(View.VISIBLE);
	 * bindView.setVisibility(View.GONE); break; case 0:
	 * cancel_btn.setVisibility(View.GONE);
	 * bindView.setVisibility(View.VISIBLE); bindView.setText("验证中"); break;
	 * case 1: cancel_btn.setVisibility(View.GONE);
	 * bindView.setVisibility(View.VISIBLE); bindView.setText("已绑定"); break;
	 * default: break; } }
	 */
	public void onClick(View v) {
		/*
		 * if (v == cancel_btn) { config.setPhoneBindingStatus(0); initView();
		 * smsVerify(); binder.bindSendHandler();
		 * 
		 * 
		 * } else
		 */
		if (v == modify) {
			savedata();
			Intent mIntent = new Intent(PhoneNumberSettingActivity.this,
					OperatorsSettingActivity.class);
			mIntent.putExtra("phonechange", true);
			mIntent.putExtra("province", provinceStr);
			mIntent.putExtra("city", cityStr);
			mIntent.putExtra("oprator", opratorStr);
			mIntent.putExtra("brand", brandStr);
			// config.setTopEightNum(phonenumber_change);
			// config.setProvince(provinceStr);
			// config.setCity(cityStr);
			// config.setOperator(opratorStr);
			if (phonenum_et.getText().toString().length() == 0) {
				Toast.makeText(getApplication(), "请输入您的手机号", Toast.LENGTH_SHORT)
						.show();
				return;
			} else if (phonenum_et.getText().toString().length() != 11
					|| !isMobileNo) {
				Toast.makeText(getApplication(), "请输入正确的号码", Toast.LENGTH_SHORT)
						.show();
				return;
			} else if (provinceStr.equals("省份")) {
				Toast.makeText(getApplication(), "请选择省份", Toast.LENGTH_SHORT)
						.show();
				startActivityForResult(mIntent, 0);
			} else if (cityStr.equals("城市")) {
				Toast.makeText(getApplication(), "请选择城市", Toast.LENGTH_SHORT)
						.show();
				startActivityForResult(mIntent, 0);
			} else if (opratorStr.equals("运营商")) {
				Toast.makeText(getApplication(), "请选择运营商", Toast.LENGTH_SHORT)
						.show();
				startActivityForResult(mIntent, 0);
			} else if (brandStr.equals("品牌")) {
				Toast.makeText(getApplication(), "请选择品牌", Toast.LENGTH_SHORT)
						.show();
				startActivityForResult(mIntent, 0);
			} else {
				Toast.makeText(getApplication(), "更换成功", Toast.LENGTH_SHORT)
						.show();
				 config.setTopEightNum(phonenumber_change);
				 config.setProvince(provinceStr);
				 config.setCity(cityStr);
				 config.setOperator(opratorStr);
				 config.setPackageBrand(brandStr);
				finish();
				overridePendingTransition(R.anim.push_right_in,
						R.anim.push_right_out);
			}

		} else if (v == back_rl) {
			finish();
			overridePendingTransition(R.anim.push_right_in,
					R.anim.push_right_out);
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (resultCode) {
		case RESULT_OK:
			config.setTopEightNum(phonenumber_change);
			finish();
			overridePendingTransition(R.anim.push_right_in,
					R.anim.push_right_out);
			break;
		case RESULT_CANCELED:
			finish();
			overridePendingTransition(R.anim.push_right_in,
					R.anim.push_right_out);
			break;
		}
	}

	private void savedata() {
		isMobileNo = IsMobileUtil.isMobileNo(phonenum_et.getText().toString());
		phonenumber_change = phonenum_et.getText().toString();
		if (!phonenumber.equals(phonenumber_change)) {
			if (phonenum_et.length() == 11) {
				if (isMobileNo) {
					// config.setTopEightNum(phonenumber_change);
					// config.setProvince(provinceStr);
					// config.setCity(cityStr);
					// config.setOperator(opratorStr);
					if ("中国电信".equals(opratorStr)) {
						// config.setPackageBrand("中国电信");
						brandStr = "中国电信";
					} else {
						// config.setPackageBrand("品牌");
						brandStr = "品牌";
					}

					// Log.e("保存手机号码", "保存成功");
				} else {
					Toast.makeText(getApplication(), "请输入正确的号码",
							Toast.LENGTH_SHORT).show();
				}
			} else {
				Toast.makeText(getApplication(), "请输入正确的号码", Toast.LENGTH_SHORT)
						.show();
			}
		}
	}

	// 监听editext中文字变化的内部类
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
			String ediText = s.toString();
			// Log.i("my", "查询号码数据库");
			if (ediText.length() == 11) {
				// Log.i("my", "查询号码数据库00");
				isMobileNo = IsMobileUtil.isMobileNo(ediText);
				if (isMobileNo) {

					CallStatDatabase db = CallStatDatabase.getInstance(context);
					PhoneNumberInfo info = db.getPhoneNumberInfo5All(ediText);
					if (info != null) {
						provinceStr = info.getProvince();
						cityStr = info.getCity();
						opratorStr = info.getOperator();
						for (int i = 0; i < provinces.length; i++) {
							if (provinceStr.equals(provinces[i])) {
								pro_position = i;
							}
						}
						cities = (String[]) getResources().getStringArray(
								arrays[pro_position]);
						if (opratorStr != null && !"".equals(opratorStr)) {
							for (int i = 0; i < operator.length; i++) {
								if (opratorStr.equals(operator[i])) {
									oper_position = i;
								}
							}

						}
					} else {
						provinceStr = "省份";
						cityStr = "城市";
						opratorStr = "运营商";
						// Log.i("xx", "info == null-------");
					}
				}

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
	/*
	 * private SmsVerifyManager svm; private OnWebLoadListener<SmsVerifyBean>
	 * smsVerifyListener = new OnWebLoadListener<SmsVerifyBean>() {
	 * 
	 * @Override public void OnStart() { Log.e("callstats",
	 * "SmsVerify OnStart"); }
	 * 
	 * @Override public void OnCancel() {
	 * 
	 * }
	 * 
	 * @Override public void OnLoadComplete(int statusCode) { switch
	 * (statusCode) { case OnLoadFinishListener.ERROR_BAD_URL:
	 * Log.e("callstats", "ERROR_BAD_URL"); break; case
	 * OnLoadFinishListener.ERROR_REQUEST_FAILED: Log.e("callstats",
	 * "ERROR_REQUEST_FAILED"); break; case OnLoadFinishListener.ERROR_TIMEOUT:
	 * //请求超时 Log.e("callstats", "ERROR_TIMEOUT"); break; case
	 * OnLoadFinishListener.OK: Log.e("callstats", "OK"); break; default: break;
	 * } }
	 * 
	 * @Override public void OnPaserComplete(SmsVerifyBean svb) { if (svb !=
	 * null) {
	 * 
	 * switch (svb.getSmsID()) { case SmsVerifyBean.SMS_ID_CODE_INVALIDATE:
	 * //无效的smsID
	 * 
	 * return;
	 * 
	 * default: break; } CallStatApplication.token = svb.getSmsID();
	 * Log.i("wanglei", "getSmsGateNum()"+svb.getSmsGateNum());
	 * config.setSmsVerifyNumber(svb.getSmsGateNum()); } } };
	 * 
	 * private void smsVerify() { String url =
	 * getString(R.string.sms_verify_req_url); svm = new SmsVerifyManager(this,
	 * url,HTTP.UTF_8); Log.e("callstats", "smsVerify url:" + url);
	 * Map<String,String> keyValuePairs = new HashMap<String,String>();
	 * keyValuePairs.put("appid", "2"); keyValuePairs.put("Mobile",
	 * config.getTopEightNum()); keyValuePairs.put("Action", "default");
	 * 
	 * HttpEntity entity =
	 * MyHttpPostHelper.buildUrlEncodedFormEntity(keyValuePairs);
	 * svm.setManagerListener(smsVerifyListener); svm.startManager(entity); }
	 */
}
