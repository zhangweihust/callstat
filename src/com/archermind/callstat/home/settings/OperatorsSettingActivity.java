package com.archermind.callstat.home.settings;

import java.util.Calendar;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.archermind.callstat.CallStatApplication;
import com.archermind.callstat.ConfigManager;
import com.archermind.callstat.R;
import com.archermind.callstat.common.database.CallStatDatabase;
import com.archermind.callstat.home.views.CustomListDialog;

public class OperatorsSettingActivity extends Activity {
	private RelativeLayout back;

	private CallStatApplication application;

	private ConfigManager config;
	public static String MONTHLY_CLEAR = "from_monthly_clear_setting";
	// title

	public static final String INFOSETTING_DETAIL = "infosetting_detail";
	// 定义相关的字符串存放信息
	private String provinceStr = "";
	private String old_provinceStr = "";
	private String cityStr = "";
	private String old_cityStr = "";
	private String opratorStr = "";
	private String old_opratorStr = "";
	private String brandStr = "";
	private String old_brandStr = "";
	// xml文件中对应的省份，城市的数组
	private int[] arrays = { R.array.beijing, R.array.shanghai,
			R.array.tianjin, R.array.chongqing, R.array.anhui, R.array.fujian,
			R.array.gansu, R.array.guangdong, R.array.guangxi, R.array.guizhou,
			R.array.hainan, R.array.hebei, R.array.henan, R.array.heilongjiang,
			R.array.hubei, R.array.hunan, R.array.jilin, R.array.jiangsu,
			R.array.jiangxi, R.array.liaoning, R.array.neimenggu,
			R.array.ningxia, R.array.qinghai, R.array.shandong,
			R.array.shanxi_01, R.array.shanxi_02, R.array.sichuan,
			R.array.xizang, R.array.xinjiang, R.array.yunnan, R.array.zhejiang, };
	// xml文件中对应的品牌和套餐的数组
	private int[] brandarrays = { R.array.mobile, R.array.unicom,
			R.array.telecom };
	// 存放省份的数组
	private String[] provinces;
	private String[] operators;
	String[] two_char_provinces;

	int pro_position = 0;
	int oper_position = 0;

	// 定义一个数组存放arrays中对应的城市数组
	String[] cities;
	String[] brands;
	final int SELECT_PROVINCES = 0x111;
	final int SELECT_OPERATOR = 0x112;
	int cityarrayId = 0;
	int brandarrayId = 0;
	int cityId = 0;
	int brandId = 0;
	Dialog mDialog = null;
	boolean isMobileNo;
	boolean provinceClickOrNot = false;
	boolean cityClickOrNot = false;
	boolean opertorClickOrNot = false;
	boolean brandClickOrNot = false;
	boolean isnumberchange = false;
	LinearLayout provinceLayout = null;
	LinearLayout cityLayout = null;
	LinearLayout opertorLayout = null;
	LinearLayout brandLayout = null;

	TextView provinceTextView = null;
	TextView cityTextView = null;
	TextView opertorTextView = null;
	TextView brandTextView = null;
	TextView phoneBehind = null;
	TextView warnstring;

	LinearLayout scrollLayout = null;

	RelativeLayout modify = null;

	boolean[] selected = new boolean[] { false, false };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		application = (CallStatApplication) getApplication();
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.operators_setting);
		config = new ConfigManager(this);
		// for advanced part
		Calendar calendar = Calendar.getInstance();
		int days = calendar.getActualMaximum(Calendar.DATE);
		if (days < config.getAccountingDay()) {
			config.setAccountingDay(days);
		}

		// for personal part
		initPersonal();// 初始化控件
		initPersonalListener();// 监听事件
	}

	private void initPersonalListener() {

		// 省份的点击监听
		provinceLayout.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Log.e("callstats", "provinceLayout.setOnClickListener");
				// 回复原来的背景

				showCustomDialog(SELECT_PROVINCES);

			}
		});
		// 城市点击事件监听
		cityLayout.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				showCustomDialog(config.getProvince(), cities, true);
			}
		});
		// 运营商的点击事件监听
		opertorLayout.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				showCustomDialog(SELECT_OPERATOR);

			}
		});
		modify.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				modify();
			}
		});

		back.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED, null);
				finish();
				overridePendingTransition(R.anim.push_right_in,
						R.anim.push_right_out);
			}

		});
		// 品牌点击事件监听
		brandLayout.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				showCustomDialog(opratorStr, brands, false);

			}
		});

	}

	private void modify() {
		if (provinceTextView.getText().toString().equals("省份")) {
			Toast.makeText(getApplication(), "请选择省份", Toast.LENGTH_SHORT)
					.show();
		} else if (cityTextView.getText().toString().equals("城市")) {
			Toast.makeText(getApplication(), "请选择城市", Toast.LENGTH_SHORT)
					.show();
		} else if (opertorTextView.getText().toString().equals("运营商")) {
			Toast.makeText(getApplication(), "请选择运营商", Toast.LENGTH_SHORT)
					.show();
		} else if (brandTextView.getText().toString().equals("品牌")) {
			Toast.makeText(getApplication(), "请选择品牌", Toast.LENGTH_SHORT)
					.show();

		} else {
			config.setProvince(provinceStr);
			config.setCity(cityStr);
			config.setOperator(opratorStr);
			config.setPackageBrand(brandStr);
			if (isnumberchange) {
				Toast.makeText(getApplication(), "您修改了手机号码，请重新验证",
						Toast.LENGTH_SHORT).show();
				config.setPhoneBindingStatus(-1);
				setResult(RESULT_OK, null);
			} else {
				Toast.makeText(getApplication(), "修改成功", Toast.LENGTH_SHORT)
						.show();
			}
			CallStatDatabase.getInstance(OperatorsSettingActivity.this)
					.initReconciliationInfo2ConfigXml(config.getProvince(),
							config.getCity(), config.getOperator(),
							config.getPackageBrand(),
							CallStatApplication.AllCodeRestore);
			config.setPackageBrand(brandTextView.getText().toString());
			finish();
			overridePendingTransition(R.anim.push_right_in,
					R.anim.push_right_out);
		}
	}

	private void initPersonal() {
		back = (RelativeLayout) findViewById(R.id.back_rl);
		provinces = getResources().getStringArray(R.array.provinces);
		two_char_provinces = getResources().getStringArray(
				R.array.two_chars_provinces);
		operators = getResources().getStringArray(R.array.oprator);

		// warnstring = (TextView) findViewById(R.id.main_warnstring);

		// 初始化Editext控件
		// 初始化6个TextView控件
		provinceTextView = (TextView) findViewById(R.id.province);
		cityTextView = (TextView) findViewById(R.id.city);
		opertorTextView = (TextView) findViewById(R.id.opertor);
		brandTextView = (TextView) findViewById(R.id.brand);

		// 初始化5个RelativeLayout控件
		provinceLayout = (LinearLayout) findViewById(R.id.provincelayout);
		cityLayout = (LinearLayout) findViewById(R.id.citylayout);
		opertorLayout = (LinearLayout) findViewById(R.id.opertorlayout);
		brandLayout = (LinearLayout) findViewById(R.id.brandlayout);
		modify = (RelativeLayout) findViewById(R.id.modify);

		// 初始化LinearLayout
		scrollLayout = (LinearLayout) findViewById(R.id.scroll_layout);
		// 初始化数组
		provinces = getResources().getStringArray(R.array.provinces);
		operators = getResources().getStringArray(R.array.oprator);
		oper_position = getOpertorsId(config.getOperator());
		pro_position = getProvincesId(config.getProvince());
		
		Intent intent = getIntent();
		isnumberchange = intent.getBooleanExtra("phonechange", false);
		provinceStr = intent.getStringExtra("province");
		cityStr = intent.getStringExtra("city");
		opratorStr = intent.getStringExtra("oprator");
		brandStr = intent.getStringExtra("brand");
		// 判断从初始化信息获取的信息是否为空，如不为空初始化存放信息的字符串并显示初始化信息内容
		if (isnumberchange) {
			if (provinceStr != null && !("").equals(provinceStr)) {
				provinceTextView.setText(provinceStr);
				pro_position = getProvincesId(provinceStr);
			}
			if (cityStr != null && !("").equals(cityStr)) {
				cityTextView.setText(cityStr);
			}
			if (opratorStr != null && !("").equals(opratorStr)) {
				opertorTextView.setText(opratorStr);
				oper_position = getOpertorsId(opratorStr);
			}
			if (brandStr != null && !("").equals(brandStr)) {
				brandTextView.setText(brandStr);
			}
		} else {
			if (config.getProvince() != null
					&& !("").equals(config.getProvince())) {
				provinceStr = config.getProvince();
				provinceTextView.setText(provinceStr);
			}
			if (config.getCity() != null && !("").equals(config.getCity())) {
				cityStr = config.getCity();
				cityTextView.setText(cityStr);
			}
			if (config.getOperator() != null
					&& !("").equals(config.getOperator())) {
				opratorStr = config.getOperator();
				opertorTextView.setText(opratorStr);
			}
			if (config.getPackageBrand() != null
					&& !("").equals(config.getPackageBrand())) {
				brandStr = config.getPackageBrand();
				brandTextView.setText(brandStr);
			}
		}
		cities = (String[]) getResources().getStringArray(arrays[pro_position]);
		brands = (String[]) getResources().getStringArray(
				brandarrays[oper_position]);
	}

	private void showCustomDialog(String string, final String[] array,
			final boolean city) {
		CustomListDialog dialog = new CustomListDialog(this);
		// 设置对话框的标题

		// 0: 默认第一个单选按钮被选中
		if (city) {
			dialog.setTitle(provinceTextView.getText().toString());
			String[] pro_city = array;
			for (int i = 0; i < pro_city.length; i++) {
				if (cityStr.equals(pro_city[i])) {
					cityId = i;
				}
			}
			dialog.setSingleChoiceItems(array, cityId,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							cityStr = array[which];
							cityClickOrNot = true;
						}
					});
		} else {
			dialog.setTitle(opertorTextView.getText().toString());
			String[] op_brand = array;
			for (int i = 0; i < op_brand.length; i++) {
				if (brandStr.equals(op_brand[i])) {
					brandId = i;
				}
			}
			dialog.setSingleChoiceItems(array, brandId,
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							brandStr = array[which];
							brandClickOrNot = true;
						}
					});
		}
		// 确定按钮
		dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				if (city) {
					if (cityClickOrNot) {
						cityTextView.setText(cityStr);
						// config.setCity(cityStr);
					} else {
						cityStr = getResources().getStringArray(
								arrays[getProvincesId(provinceTextView
										.getText().toString())])[cityId];
						cityTextView.setText(cityStr);
						// config.setCity(cityStr);
					}
				} else {
					if (brandClickOrNot) {
						brandTextView.setText(brandStr);
						// config.setPackageBrand(brandStr);
					} else {
						brandStr = getResources().getStringArray(
								brandarrays[getOpertorsId(opertorTextView
										.getText().toString())])[brandId];
						brandTextView.setText(brandStr);
						// config.setPackageBrand(brandStr);
					}
				}
			}
		});

		dialog.show();
	}

	private void showCustomDialog(int id) {
		CustomListDialog dialog = new CustomListDialog(this);
		switch (id) {
		case SELECT_PROVINCES:
			// 设置对话框的标题
			dialog.setTitle("选择省份");

			// Log.i("xx", provinceTextView.getText().toString());
			// Log.i("xx",
			// "getProvincesId(provinceTextView.getText().toString()="
			// + getProvincesId(provinceTextView.getText().toString()));
			// 0: 默认第一个单选按钮被选中
			dialog.setSingleChoiceItems(R.array.provinces,
					getProvincesId(provinceTextView.getText().toString()),
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {

							provinceStr = getResources().getStringArray(
									R.array.provinces)[which];

							cityarrayId = which;

							provinceClickOrNot = true;
						}
					});
			// 确定按钮
			dialog.setPositiveButton("确定",
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {

							if (provinceClickOrNot) {
								provinceStr = getResources().getStringArray(
										R.array.provinces)[cityarrayId];
								provinceTextView.setText(provinceStr);
								cities = (String[]) getResources()
										.getStringArray(arrays[cityarrayId]);
								cityStr = getResources().getStringArray(
										arrays[cityarrayId])[0];
								cityTextView.setText(cityStr);
								// config.setProvince(provinceStr);
								// config.setCity(cityStr);
								// provinceClickOrNot = false;
								// Log.i("my", "provinceClickOrNot==="
								// + provinceClickOrNot);
							} else {

								pro_position = getProvincesId(provinceTextView
										.getText().toString());

								provinceStr = getResources().getStringArray(
										R.array.provinces)[pro_position];
								cities = (String[]) getResources()
										.getStringArray(arrays[pro_position]);
								cityStr = getResources().getStringArray(
										arrays[pro_position])[0];
								provinceTextView.setText(provinceStr);
								cityTextView.setText(cityStr);
								// config.setProvince(provinceStr);
								// config.setCity(cityStr);
								// Log.i("my", "provinceClickOrNot==="
								// + provinceClickOrNot);
							}
						}
					});
			// 创建一个单选按钮对话框
			dialog.show();
			break;

		case SELECT_OPERATOR:
			// 设置对话框的标题
			dialog.setTitle("选择运营商");
			// 0: 默认第一个单选按钮被选中
			dialog.setSingleChoiceItems(R.array.oprator,
					getOpertorsId(opertorTextView.getText().toString()),
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							opratorStr = getResources().getStringArray(
									R.array.oprator)[which];
							// brandStr = getResources().getStringArray(
							// brandarrays[which])[0];
							brandarrayId = which;
							opertorClickOrNot = true;
						}
					});
			// 确定按钮brandTextView opertorTextView
			dialog.setPositiveButton("确定",
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							if (opertorClickOrNot) {
								// Log.i("my", "onclick===" + brandarrayId);
								opratorStr = getResources().getStringArray(
										R.array.oprator)[brandarrayId];
								brands = (String[]) getResources()
										.getStringArray(
												brandarrays[brandarrayId]);
								brandStr = getResources().getStringArray(
										brandarrays[brandarrayId])[0];
								opertorTextView.setText(opratorStr);
								brandTextView.setText(brandStr);
								// config.setOperator(opratorStr);
								// config.setPackageBrand(brandStr);
							} else {
								opratorStr = getResources().getStringArray(
										R.array.oprator)[oper_position];
								brands = (String[]) getResources()
										.getStringArray(
												brandarrays[oper_position]);
								brandStr = getResources().getStringArray(
										brandarrays[oper_position])[brandId];
								opertorTextView.setText(opratorStr);
								brandTextView.setText(brandStr);
								// config.setOperator(opratorStr);
								// config.setPackageBrand(brandStr);
							}

						}
					});
			// 创建一个单选按钮对话框
			dialog.show();
			break;
		default:
			break;
		}
	}

	private int getProvincesId(String string) {
		// Log.i("my", "String==========" + string);
		int pro_position = 0;
		for (int i = 0; i < provinces.length; i++) {
			if (string.equals(provinces[i])) {
				pro_position = i;
			}
		}
		return pro_position;
	}

	private int getOpertorsId(String string) {
		// Log.i("my", "String==========" + string);
		int oper_position = 0;
		for (int i = 0; i < operators.length; i++) {
			if (string.equals(operators[i])) {
				oper_position = i;
			}
		}
		return oper_position;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
			setResult(RESULT_CANCELED, null);
			finish();
			return true;
		} else {
			// Log.e("callstats",
			// "onKeyDown event.getKeyCode():" + event.getKeyCode());
			return super.onKeyDown(keyCode, event);
		}
	}
}
