package com.archermind.callstat.home;

import java.util.Calendar;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.archermind.callstat.ConfigManager;
import com.archermind.callstat.ILog;
import com.archermind.callstat.R;
import com.archermind.callstat.accounting.AccountingKeyWordsBean;
import com.archermind.callstat.common.CallStatUtils;
import com.archermind.callstat.common.DeviceUtils;
import com.archermind.callstat.common.IsMobileUtil;
import com.archermind.callstat.common.ResourceLoader.OnLoadFinishListener;
import com.archermind.callstat.common.database.CallStatDatabase;
import com.archermind.callstat.common.json.AbstractWebLoadManager.OnWebLoadListener;
import com.archermind.callstat.home.bean.PhoneNumberInfo;
import com.archermind.callstat.home.views.CustomListDialog;
import com.archermind.callstat.home.views.ToastFactory;
import com.archermind.callstat.service.json.AccountingDatabaseUpdater;

public class NewInitInfoSettingActivity extends Activity implements
		OnClickListener {

	public static NewInitInfoSettingActivity reg;

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
	private String provinceStr = "";
	private String cityStr = "";
	private String opratorStr = "";
	private String brandStr = "";
	boolean isMobileNo;
	private Context context;

	private int[] brandarrays = { R.array.mobile, R.array.unicom,
			R.array.telecom };
	// 存放省份的数组
	private String[] operators;

	// 定义一个数组存放arrays中对应的城市数组
	final int SELECT_PROVINCES = 0x111;
	final int SELECT_OPERATOR = 0x112;
	Dialog mDialog = null;
	boolean provinceClickOrNot = false;
	boolean cityClickOrNot = false;
	boolean opertorClickOrNot = false;
	boolean brandClickOrNot = false;
	LinearLayout provinceLayout = null;
	LinearLayout cityLayout = null;
	LinearLayout opertorLayout = null;
	LinearLayout brandLayout = null;
	LinearLayout position_ll;
	LinearLayout position_title_ll;
	RelativeLayout opertor_ll;
	LinearLayout opertor_title_ll;
	LinearLayout brand_title_ll;
	RelativeLayout brand_ll;
	TextView phoneTextView = null;
	TextView provinceTextView = null;
	TextView cityTextView = null;
	TextView opertorTextView = null;
	TextView brandTextView = null;
	TextView phoneBehind = null;
	TextView warnstring;
	private Button tonext;

	RelativeLayout mainLayout = null;
	LinearLayout scrollLayout = null;
	private AccountingDatabaseUpdater accountingDatabaseUpdater;
	boolean[] selected = new boolean[] { false, false };

	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.common);
		config = new ConfigManager(this);
		context = this;
		reg = this;
		initUI();
		initListener();

		// for advanced part
		Calendar calendar = Calendar.getInstance();
		int days = calendar.getActualMaximum(Calendar.DATE);
		if (days < config.getAccountingDay()) {
			config.setAccountingDay(days);
		}
		if (CallStatUtils.isNetworkAvailable(NewInitInfoSettingActivity.this)) {
			checkAccountingCodeUpdate();
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		phonenum_et.setSelectAllOnFocus(true);

		reflesh_click();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	private void initListener() {
		try {
			// phonenum_et.setOnTouchListener(this);
			phonenum_et.addTextChangedListener(watcher);
			tonext.setOnClickListener(this);

			provinceLayout.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// Log.e("callstats", "provinceLayout.setOnClickListener");
					// 回复原来的背景
					try {
						showCustomDialog(SELECT_PROVINCES);
					} catch (Exception e) {
						ILog.logException(this.getClass(), e);
					}

				}
			});
			// 城市点击事件监听
			cityLayout.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					try {
						showCustomDialog(config.getProvince(), cities, true);
					} catch (Exception e) {
						ILog.logException(this.getClass(), e);
					}
				}
			});
			// 运营商的点击事件监听
			opertorLayout.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					try {
						showCustomDialog(SELECT_OPERATOR);
					} catch (Exception e) {
						ILog.logException(this.getClass(), e);
					}
				}
			});
			// 品牌点击事件监听
			brandLayout.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					try {
						showCustomDialog(config.getOperator(), brands, false);
					} catch (Exception e) {
						ILog.logException(this.getClass(), e);
					}
				}
			});
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	public void onClick(View v) {
		if (v == tonext) {
			try {
				isMobileNo = IsMobileUtil.isMobileNo(phonenum_et.getText()
						.toString());
				if (phonenum_et.getText().toString().length() == 0) {
					ToastFactory
							.getToast(this, "请输入您的手机号码", Toast.LENGTH_SHORT)
							.show();
				} else if (phonenum_et.getText().toString().length() != 11
						|| !isMobileNo) {
					ToastFactory.getToast(this, "请输入正确的号码", Toast.LENGTH_SHORT)
							.show();
				} else if (provinceTextView.getText().toString().equals("省份")) {
					ToastFactory.getToast(this, "请选择省份", Toast.LENGTH_SHORT)
							.show();
				} else if (cityTextView.getText().toString().equals("城市")) {
					ToastFactory.getToast(this, "请选择城市", Toast.LENGTH_SHORT)
							.show();
				} else if (opertorTextView.getText().toString().equals("运营商")) {
					ToastFactory.getToast(this, "请选择运营商", Toast.LENGTH_SHORT)
							.show();
				} else if (brandTextView.getText().toString().equals("品牌")) {
					ToastFactory.getToast(this, "请选择品牌", Toast.LENGTH_SHORT)
							.show();
				}

				if (toNext_can_click()) {
					startActivity(new Intent(NewInitInfoSettingActivity.this,
							SecondInitInfoSettingActivity.class));
				}
			} catch (Exception e) {
				ILog.logException(this.getClass(), e);
			}
		}
	}

	private void initUI() {
		// TODO Auto-generated method stub
		provinces = getResources().getStringArray(R.array.provinces);
		two_char_provinces = getResources().getStringArray(
				R.array.two_chars_provinces);
		operator = getResources().getStringArray(R.array.oprator);
		phonenumber = config.getTopEightNum();
		phonenumber_change = config.getTopEightNum();
		phonenum_et = (EditText) findViewById(R.id.phonenum_et);
		phonenum_et.setText(phonenumber);
		position_ll = (LinearLayout) findViewById(R.id.position_ll);
		position_title_ll = (LinearLayout) findViewById(R.id.position_title_ll);
		opertor_ll = (RelativeLayout) findViewById(R.id.opertor_ll);
		opertor_title_ll = (LinearLayout) findViewById(R.id.opertor_title_ll);
		brand_ll = (RelativeLayout) findViewById(R.id.brand_ll);
		brand_title_ll = (LinearLayout) findViewById(R.id.brand_title_ll);

		tonext = (Button) findViewById(R.id.tonext);

		// 初始化6个TextView控件
		provinceTextView = (TextView) findViewById(R.id.province);
		provinceTextView.setText(config.getProvince());
		cityTextView = (TextView) findViewById(R.id.city);
		cityTextView.setText(config.getCity());
		opertorTextView = (TextView) findViewById(R.id.opertor);
		opertorTextView.setText(config.getOperator());
		brandTextView = (TextView) findViewById(R.id.brand);
		brandTextView.setText(config.getPackageBrand());

		provinceLayout = (LinearLayout) findViewById(R.id.provincelayout);
		cityLayout = (LinearLayout) findViewById(R.id.citylayout);
		opertorLayout = (LinearLayout) findViewById(R.id.opertorlayout);
		brandLayout = (LinearLayout) findViewById(R.id.brandlayout);

		// 初始化LinearLayout
		scrollLayout = (LinearLayout) findViewById(R.id.scroll_layout);
		// 初始化数组

		provinces = getResources().getStringArray(R.array.provinces);
		operators = getResources().getStringArray(R.array.oprator);
		writeInfoFromSimToConfig();
		oper_position = getOpertorsId(config.getOperator());
		pro_position = getProvincesId(config.getProvince());
		cities = (String[]) getResources().getStringArray(arrays[pro_position]);
		brands = (String[]) getResources().getStringArray(
				brandarrays[oper_position]);
		// 判断从初始化信息获取的信息是否为空，如不为空初始化存放信息的字符串并显示初始化信息内容
		// 初始化Editext控件
		try {
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
			phonenum_et.setText(config.getTopEightNum());
			provinceTextView.setText(config.getProvince());
			cityTextView.setText(config.getCity());
			opertorTextView.setText(config.getOperator());
			String ediText = phonenum_et.getText().toString();
			CallStatDatabase db = CallStatDatabase.getInstance(context);
			PhoneNumberInfo info = db.getPhoneNumberInfo5All(ediText);
			if (ediText.length() != 11) {
				position_ll.setVisibility(View.GONE);
				position_title_ll.setVisibility(View.GONE);
				opertor_ll.setVisibility(View.GONE);
				opertor_title_ll.setVisibility(View.GONE);
				brand_ll.setVisibility(View.GONE);
				brand_title_ll.setVisibility(View.GONE);
			} else {
				if (info != null) {
					viewVisible(false);
				} else {
					viewVisible(true);
				}
			}

		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	private void savedata() {
		try {
			config.setTopEightNum(phonenumber_change);
			config.setProvince(provinceStr);
			provinceTextView.setText(provinceStr);
			config.setCity(cityStr);
			cityTextView.setText(cityStr);
			config.setOperator(opratorStr);
			opertorTextView.setText(opratorStr);
			config.setPackageBrand(brandStr);
			brandTextView.setText(brandStr);
			// Log.e("保存手机号码", "保存成功");
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	private void viewVisible(Boolean bool) {
		try {
			if (bool == true) {
				position_ll.setVisibility(View.VISIBLE);
				position_title_ll.setVisibility(View.VISIBLE);
				opertor_ll.setVisibility(View.VISIBLE);
				opertor_title_ll.setVisibility(View.VISIBLE);
				brand_ll.setVisibility(View.VISIBLE);
				brand_title_ll.setVisibility(View.VISIBLE);
			} else if (bool == false) {
				position_ll.setVisibility(View.GONE);
				position_title_ll.setVisibility(View.GONE);
				opertor_ll.setVisibility(View.GONE);
				opertor_title_ll.setVisibility(View.GONE);
				brand_ll.setVisibility(View.VISIBLE);
				brand_title_ll.setVisibility(View.VISIBLE);
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
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
			try {
				String ediText = s.toString();
				phonenumber = config.getTopEightNum();
				// Log.i("my", "查询号码数据库");
				if (ediText.length() == 11) {
					// Log.i("my", "查询号码数据库00");
					isMobileNo = IsMobileUtil.isMobileNo(ediText);
					if (isMobileNo) {

						CallStatDatabase db = CallStatDatabase
								.getInstance(context);
						PhoneNumberInfo info = db
								.getPhoneNumberInfo5All(ediText);
						phonenumber_change = phonenum_et.getText().toString();
						if (info != null) {
							if (!phonenumber.equals(phonenumber_change)) {
								viewVisible(false);
								provinceStr = info.getProvince();
								cityStr = info.getCity();
								opratorStr = info.getOperator();
								for (int i = 0; i < provinces.length; i++) {
									if (provinceStr.equals(provinces[i])) {
										pro_position = i;
									}
								}
								cities = (String[]) getResources()
										.getStringArray(arrays[pro_position]);
								if (opratorStr != null
										&& !"".equals(opratorStr)) {
									for (int i = 0; i < operator.length; i++) {
										if (opratorStr.equals(operator[i])) {
											oper_position = i;
										}
									}

								}
								brands = (String[]) getResources()
										.getStringArray(
												brandarrays[oper_position]);
								brandClickOrNot = false;

								if (brands.length == 1) {
									brandStr = brands[0];
								} else {
									brandStr = "品牌";
									Toast.makeText(getApplication(), "请选择品牌",
											Toast.LENGTH_SHORT).show();
								}
								brandTextView.setText(brandStr);
								config.setPackageBrand(brandStr);
							}

						} else {
							viewVisible(true);
							Toast.makeText(getApplication(), "请选择省份",
									Toast.LENGTH_SHORT).show();
							provinceStr = "省份";
							cityStr = "城市";
							opratorStr = "运营商";
							brandStr = "品牌";

						}
						savedata();

						/*
						 * Log.i("free", config.getProvince() + config.getCity()
						 * + config.getOperator());
						 */

					} else {
						position_ll.setVisibility(View.GONE);
						position_title_ll.setVisibility(View.GONE);
						opertor_ll.setVisibility(View.GONE);
						opertor_title_ll.setVisibility(View.GONE);
						brand_ll.setVisibility(View.GONE);
						brand_title_ll.setVisibility(View.GONE);
						Toast.makeText(getApplication(), "请输入正确的号码",
								Toast.LENGTH_SHORT).show();
					}
					reflesh_click();
				}
			} catch (Exception e) {
				ILog.logException(this.getClass(), e);
			}

		}
	};

	private void showCustomDialog(String string, final String[] array,
			final boolean city) {
		try {
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
							public void onClick(DialogInterface dialog,
									int which) {
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

							public void onClick(DialogInterface dialog,
									int which) {
								brandStr = array[which];
								brandClickOrNot = true;
							}
						});
			}
			// 确定按钮
			dialog.setPositiveButton("确定",
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							if (city) {
								if (cityClickOrNot) {
									cityTextView.setText(cityStr);
									config.setCity(cityStr);
								} else {
									cityStr = getResources()
											.getStringArray(
													arrays[getProvincesId(provinceTextView
															.getText()
															.toString())])[cityId];
									cityTextView.setText(cityStr);
									config.setCity(cityStr);
								}
							} else {
								if (brandClickOrNot) {
									brandTextView.setText(brandStr);
									config.setPackageBrand(brandStr);
								} else {
									brandStr = getResources()
											.getStringArray(
													brandarrays[getOpertorsId(opertorTextView
															.getText()
															.toString())])[brandId];
									brandTextView.setText(brandStr);
									config.setPackageBrand(brandStr);
								}
							}
							reflesh_click();
						}
					});
			dialog.show();

		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	protected void showCustomDialog(int id) {
		try {
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

							public void onClick(DialogInterface dialog,
									int which) {

								provinceStr = getResources().getStringArray(
										R.array.provinces)[which];

								cityarrayId = which;

								provinceClickOrNot = true;
							}
						});
				// 确定按钮
				dialog.setPositiveButton("确定",
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,
									int which) {

								if (provinceClickOrNot) {
									provinceStr = getResources()
											.getStringArray(R.array.provinces)[cityarrayId];
									provinceTextView.setText(provinceStr);
									cities = (String[]) getResources()
											.getStringArray(arrays[cityarrayId]);
									cityStr = getResources().getStringArray(
											arrays[cityarrayId])[0];
									cityTextView.setText(cityStr);
									config.setProvince(provinceStr);
									config.setCity(cityStr);
									// provinceClickOrNot = false;
									// Log.i("my", "provinceClickOrNot==="
									// + provinceClickOrNot);
								} else {

									pro_position = getProvincesId(provinceTextView
											.getText().toString());

									provinceStr = getResources()
											.getStringArray(R.array.provinces)[pro_position];
									cities = (String[]) getResources()
											.getStringArray(
													arrays[pro_position]);
									cityStr = getResources().getStringArray(
											arrays[pro_position])[0];
									provinceTextView.setText(provinceStr);
									cityTextView.setText(cityStr);
									config.setProvince(provinceStr);
									config.setCity(cityStr);
									// Log.i("my", "provinceClickOrNot==="
									// + provinceClickOrNot);
								}
								reflesh_click();
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

							public void onClick(DialogInterface dialog,
									int which) {
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

							public void onClick(DialogInterface dialog,
									int which) {
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
									config.setOperator(opratorStr);
									config.setPackageBrand(brandStr);
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
									config.setOperator(opratorStr);
									config.setPackageBrand(brandStr);
								}
								reflesh_click();
							}
						});
				// 创建一个单选按钮对话框
				dialog.show();
				break;
			default:
				break;
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	private int getProvincesId(String string) {
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
		try {
			for (int i = 0; i < operators.length; i++) {
				if (string.equals(operators[i])) {
					oper_position = i;
				}
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
		return oper_position;
	}

	private void writeInfoFromSimToConfig() {
		try {
			String phoneNum_read_from_sim = DeviceUtils
					.getLocalNumberFromSim(this);
			CallStatDatabase db = CallStatDatabase.getInstance(context);
			if (config != null && phoneNum_read_from_sim != null) {
				PhoneNumberInfo info = db
						.getPhoneNumberInfo5All(phoneNum_read_from_sim);
				config.setTopEightNum(phoneNum_read_from_sim);
				config.setProvince(info.getProvince());
				config.setCity(info.getCity());
				config.setOperator(info.getOperator());
				// config.setPackageBrand("神州行");
				if (info.getOperator() != null) {
					int id = getOpertorsId(info.getOperator());
					String[] brString = (String[]) getResources()
							.getStringArray(brandarrays[id]);
					// Log.i("xx", "info.getOperator() =" + info.getOperator());
					// Log.i("xx", "id =" + id);
					// Log.i("xx", "brString =" + brString);
					if (brString.length == 1) {
						config.setPackageBrand(brString[0]);
					} else {
						config.setPackageBrand("品牌");
					}
				}
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	private void checkAccountingCodeUpdate() {
		try {
			String url = getString(R.string.accounting_database_update_url);
			if (accountingDatabaseUpdater == null) {
				accountingDatabaseUpdater = new AccountingDatabaseUpdater(this,
						url);
			}
			accountingDatabaseUpdater
					.setManagerListener(accountingDatabaseUpdateListener);
			accountingDatabaseUpdater.startManager();
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	private OnWebLoadListener<List<AccountingKeyWordsBean>> accountingDatabaseUpdateListener = new OnWebLoadListener<List<AccountingKeyWordsBean>>() {

		@Override
		public void OnStart() {

		}

		@Override
		public void OnCancel() {

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
				break;

			default:
				break;
			}
		}

		@Override
		public void OnPaserComplete(List<AccountingKeyWordsBean> list) {
			if (list != null) {
				for (int i = 0; i < list.size(); i++) {
					final AccountingKeyWordsBean bean = list.get(i);
					if (bean != null) {
						/*
						 * Log.i("callstats", bean.getProvince() + "  " +
						 * bean.getCode());
						 */
						new Thread(new Runnable() {

							@Override
							public void run() {
								CallStatDatabase.getInstance(
										NewInitInfoSettingActivity.this)
										.updateReconciliationCode(bean);
							}
						}).start();
					}
				}
			}
		}
	};

	// 判断“下一步”是否可以点击
	private boolean toNext_can_click() {
		isMobileNo = IsMobileUtil.isMobileNo(phonenum_et.getText().toString());
		if (phonenum_et.getText().toString().length() == 0) {
			return false;
		} else if (phonenum_et.getText().toString().length() != 11
				|| !isMobileNo) {
			return false;
		} else if (provinceTextView.getText().toString().equals("省份")) {
			return false;
		} else if (cityTextView.getText().toString().equals("城市")) {
			return false;
		} else if (opertorTextView.getText().toString().equals("运营商")) {
			return false;
		} else if (brandTextView.getText().toString().equals("品牌")) {
			return false;
		} else {
			return true;
		}
	}

	private void reflesh_click() {
		try {
			if (toNext_can_click()) {
				tonext.setBackgroundResource(R.drawable.btn_topup_selector);
				tonext.setTextColor(getResources().getColorStateList(
						R.drawable.init_settingbtn_selector));
			} else {
				tonext.setBackgroundResource(R.drawable.popup_but_nor);
				tonext.setTextColor(getResources().getColor(R.color.gray));
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}
}
