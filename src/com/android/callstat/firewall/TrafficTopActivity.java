package com.android.callstat.firewall;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.callstat.CallStatApplication;
import com.android.callstat.ConfigManager;
import com.android.callstat.ILog;
import com.android.callstat.common.CallStatUtils;
import com.android.callstat.common.database.CallStatDatabase;
import com.android.callstat.firewall.adapter.TrafficAdapter;
import com.android.callstat.firewall.bean.NewTrafficDetail;
import com.android.callstat.home.CallsManagerActivity;
import com.android.callstat.home.views.CustomListDialog;
import com.android.callstat.home.views.ToastFactory;
import com.archermind.callstat.R;

public class TrafficTopActivity extends Activity implements OnClickListener/*
																			 * ,
																			 * OnTouchListener
																			 */{
	public static final String TAG = "callstats";
	private Set<Integer> mRejectedList = new TreeSet<Integer>();// 所有程序的UID集合
	private ConfigManager config;
	private CallStatApplication myApp;
	private TrafficAdapter adapter;
	private int installedApps = 0;
	// private int rejected = 0;
	// private int wifiReject = 0;
	// private Set<Integer> mWifiRejectedList = new TreeSet<Integer>();

	private RelativeLayout back_rl;
	private RelativeLayout text_rl;
	// private TextView target_apps_tv;
	// private TextView rejected_apps_tv;
	private RelativeLayout firewall;
	private RelativeLayout firewall_slider_bed;
	private ImageView firewall_slider1;
	private ImageView firewall_slider2;
	private ListView trafficRank;
	private LinearLayout freq_tv;
	private ArrayList<NewTrafficDetail> newTrafficDetails = new ArrayList<NewTrafficDetail>();
	List<String[]> records;
	ProgressDialog progressDialog;
	long count1;
	View convertView;
	TrafficAdapter.ViewHolder holder;
	int selected;
	final static int MESSAGE_WHAT_TRAFFICTOP = 0;

	public static Button g2_bnt, wifi_bnt;

	private int closeOrOpenAll;// 1为2g/3g，2为wifi

	public static int howlong = 0;// 0为今天，1为昨天，2为本月

	private String[] iswhichday;
	private TextView whichDayTextView;
	private TextView denyGprs, denyWifi;

	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			denyGprs.setText(myApp.rejectedList.size() + "");
			denyWifi.setText(myApp.wifiRejectedList.size() + "");
		};
	};

	@Override
	protected void onResume() {
		super.onResume();
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}

		// 用户行为采集
		try {
			if (mHandler.hasMessages(MESSAGE_WHAT_TRAFFICTOP)) {
				mHandler.removeMessages(MESSAGE_WHAT_TRAFFICTOP);
			}
			mHandler.sendEmptyMessageDelayed(MESSAGE_WHAT_TRAFFICTOP, 5000);
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		howlong = 0;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// trafficDetails = null;
		if (mHandler.hasMessages(MESSAGE_WHAT_TRAFFICTOP)) {
			mHandler.removeMessages(MESSAGE_WHAT_TRAFFICTOP);
		}
		System.gc();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		count1 = System.currentTimeMillis();
		iswhichday = getResources().getStringArray(R.array.whichday);
		super.onCreate(savedInstanceState);
		overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
		myApp = (CallStatApplication) getApplication();
		config = new ConfigManager(this);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.trafficstatistics_main_new);

		initUI();

		// 获取CallStatApplication对象
		firewall.setOnClickListener(this);
		back_rl.setOnClickListener(this);
		// trafficRank.setOnItemClickListener(this);
		// trafficRank.setOnTouchListener(this);
		// trafficRank.setOnItemSelectedListener(this);

		InitTrafficDetailThread detailThread = new InitTrafficDetailThread();
		detailThread.execute();
	}

	private void initUI() {
		back_rl = (RelativeLayout) findViewById(R.id.back_rl);
		text_rl = (RelativeLayout) findViewById(R.id.text_rl);
		// target_apps_tv = (TextView) findViewById(R.id.tv_target_apps);
		// rejected_apps_tv = (TextView) findViewById(R.id.tv_rejected_apps);

		trafficRank = (ListView) findViewById(R.id.trafficrank_list);
		trafficRank.setDivider(null);
		firewall = (RelativeLayout) findViewById(R.id.RLayout2);
		firewall_slider_bed = (RelativeLayout) findViewById(R.id.firewall_slider_rl);
		firewall_slider1 = (ImageView) findViewById(R.id.firewall_slider_img1);
		firewall_slider2 = (ImageView) findViewById(R.id.firewall_slider_img2);
		switchFirewallSlider(config.isFirewallOn());

		freq_tv = (LinearLayout) findViewById(R.id.freq_tv);
		whichDayTextView = (TextView) findViewById(R.id.which_day);
		denyGprs = (TextView) findViewById(R.id.deny_gprs);
		denyWifi = (TextView) findViewById(R.id.deny_wifi);

		long today_gprs_used = config.getTotalGprsUsed()
				- config.getEarliestGprsLog();
		ILog.LogI(this.getClass(), "today_gprs_used:" + today_gprs_used);
		String[] traffic2 = CallStatUtils.traffic_unit(today_gprs_used);

		whichDayTextView.setText(getResources()
				.getStringArray(R.array.whichday)[howlong]
				+ "("
				+ traffic2[0]
				+ traffic2[1] + ")");

		// Log.i("wanglei", "howlong==="+howlong);

		g2_bnt = (Button) findViewById(R.id.g2);
		wifi_bnt = (Button) findViewById(R.id.wifi);
		g2_bnt.setOnClickListener(this);
		wifi_bnt.setOnClickListener(this);
		freq_tv.setOnClickListener(this);
	}

	/*
	 * private void switchFirewall() { boolean flag = config.isFirewallOn();
	 * TrafficAdapter adapter = (TrafficAdapter) trafficRank.getAdapter(); if
	 * (flag) { config.setFirewallOn(false);
	 * FirewallCoreWorker.purgeIptables(CallStatApplication.canMyFirewallWork);
	 * switchFirewallSlider(false); adapter.showFirewallBtn(false); } else {
	 * config.setFirewallOn(true); try { FirewallCoreWorker.initIptables(
	 * CallStatApplication.canMyFirewallWork, false);
	 * FirewallCoreWorker.applyRulesWithForbiddenList(rejectedList,"",
	 * CallStatApplication.canMyFirewallWork); } catch (IOException e) {
	 * e.printStackTrace(); } switchFirewallSlider(true);
	 * adapter.showFirewallBtn(true); } }
	 */

	private void switchFirewallSlider(boolean isFirewallOn) {
		// Log.e("callstats", "on_off:" + isFirewallOn);
		if (isFirewallOn) {
			if (firewall_slider1.getVisibility() == View.VISIBLE) {
				firewall_slider1.setVisibility(View.GONE);
				firewall_slider2.setVisibility(View.VISIBLE);
			}
			firewall_slider_bed.setBackgroundResource(R.drawable.yqy);
		} else {
			if (firewall_slider2.getVisibility() == View.VISIBLE) {
				firewall_slider2.setVisibility(View.GONE);
				firewall_slider1.setVisibility(View.VISIBLE);
			}
			firewall_slider_bed
					.setBackgroundResource(R.drawable.firewall_all_off);
		}

	}

	class InitTrafficDetailThread extends
			AsyncTask<Void, Void, ArrayList<NewTrafficDetail>> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressDialog = ProgressDialog.show(TrafficTopActivity.this, null,
					"正在努力加载数据.....", true);
			progressDialog.setCancelable(true);
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			if (progressDialog != null) {
				progressDialog.dismiss();
			}
		}

		@Override
		protected ArrayList<NewTrafficDetail> doInBackground(Void... params) {
			if (FirewallCoreWorker.hasRootAccess()) {
				String ipRuls = FirewallCoreWorker
						.showIptablesRules(CallStatApplication.canMyFirewallWork);
				if (CallStatApplication.canMyFirewallWork) {
					if (ipRuls != null && ipRuls.contains("Segmentation fault")) {
						CallStatApplication.canMyFirewallWork = false;
						config.setCanMineFirewallWork(false);
						ipRuls = FirewallCoreWorker
								.showIptablesRules(CallStatApplication.canMyFirewallWork);
					}
				}

				try {
					FirewallCoreWorker.initIptables(
							CallStatApplication.canMyFirewallWork, false);
				} catch (IOException e) {
					e.printStackTrace();
				}

			}

			if (newTrafficDetails == null || newTrafficDetails.isEmpty()) {
				// Log.i("callstats", "直接获取：trafficDetails is null");
				ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo wifiInfo = connectivityManager
						.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
				boolean isWifiOn = wifiInfo.isConnected() ? true : false;
				if (isWifiOn) {
					newTrafficDetails = myApp
							.refreshNewTrafficDetail(CallStatDatabase.TRAFFIC_DETAIL_TYPE_WIFI);
				} else {
					newTrafficDetails = myApp
							.refreshNewTrafficDetail(CallStatDatabase.TRAFFIC_DETAIL_TYPE_GPRS);
				}
			}
			if (newTrafficDetails != null && !newTrafficDetails.isEmpty()) {
				installedApps = newTrafficDetails.size();

				/*
				 * List<String> rejectedPackageNames = FirewallUtils
				 * .getRejectedPackageNames(appUserAndDir, rejectedApps);
				 */

				// zhangjing not sure
				myApp.rejectedList = FirewallUtils
						.getRejectedList(newTrafficDetails);

				myApp.wifiRejectedList = FirewallUtils.getWifiRejectedList();

				mRejectedList = FirewallUtils.getAllRejectedList();

			}
			return newTrafficDetails;
		}

		@Override
		protected void onPostExecute(ArrayList<NewTrafficDetail> result) {
			super.onPostExecute(result);

			if (newTrafficDetails != null && newTrafficDetails.size() > 0) {

				correctGprs(config.getTotalGprsUsed()
						- config.getEarliestGprsLog());

				CallStatUtils.sortByNewTraffic(newTrafficDetails);
				adapter = new TrafficAdapter(TrafficTopActivity.this,
						newTrafficDetails, handler);
				trafficRank.setAdapter(adapter);
			}

			if (!config.isFirewallOn()) {
				FirewallCoreWorker
						.purgeIptables(CallStatApplication.canMyFirewallWork);
			}
			if (progressDialog != null) {
				progressDialog.dismiss();
			}
			refreshUI();
		}
	}

	/*
	 * private void updateTrafficDetail(TrafficDetail td) { new
	 * UpdateTrafficDetailThread().execute(td); }
	 * 
	 * class UpdateTrafficDetailThread extends AsyncTask<Object, Void, Long> {
	 * 
	 * @Override protected Long doInBackground(Object... params) {
	 * CallStatDatabase db =
	 * CallStatDatabase.getInstance(TrafficTopActivity.this); TrafficDetail td =
	 * (TrafficDetail) params[0]; return db.updateTrafficDetail(td); }
	 * 
	 * @Override protected void onPostExecute(Long result) {
	 * super.onPostExecute(result); } }
	 */

	/*
	 * public void applyRule(final AdapterView<?> parent, final View view, final
	 * int position, final long id) { TrafficAdapter adapter = (TrafficAdapter)
	 * parent.getAdapter(); TrafficDetail detail = (TrafficDetail)
	 * adapter.getItem(position); // first,we have to know current
	 * state:accepted or rejected? boolean isRejected = detail.isRejected(); //
	 * if rejected,we have to set on-and-off image being on // meantime
	 * adapter.rejected--; // otherwise we do the opposite thing
	 * Log.e("callstats", "position:" + position); if (isRejected) {
	 * 
	 * 
	 * FirewallCoreWorker.applyRulesWithForbiddenList(rejectedList,
	 * FirewallCoreWorker .GPRS_FILTER,CallStatApplication.canMyFirewallWork);
	 * 
	 * FirewallCoreWorker.applyRule(detail.getUid(), "D",
	 * CallStatApplication.canMyFirewallWork);
	 * config.setCanFirewallWork(CallStatApplication.canFirewallWork); if
	 * (!CallStatApplication.canFirewallWork) {
	 * Toast.makeText(TrafficTopActivity.this, "您的手机暂不支持防火墙功能",
	 * Toast.LENGTH_LONG).show(); return; }
	 * //rejectedList.remove(Integer.valueOf(detail.getUid())); //rejected =
	 * rejectedList.size();
	 * 
	 * TrafficAdapter.ViewHolder holder = ((ViewHolder) view.getTag());
	 * detail.setRejected(false); updateTrafficDetail(detail);
	 * switchSliderForSingleItem(holder.slider_bed,
	 * holder.slider1,holder.slider2, true);//!rejected refreshUI(); } else {
	 * 
	 * 
	 * FirewallCoreWorker.applyRulesWithForbiddenList(rejectedList,
	 * FirewallCoreWorker .GPRS_FILTER,CallStatApplication.canMyFirewallWork);
	 * 
	 * FirewallCoreWorker.applyRule(detail.getUid(), "A",
	 * CallStatApplication.canMyFirewallWork);
	 * 
	 * if (!CallStatApplication.canFirewallWork) {
	 * Toast.makeText(TrafficTopActivity.this, "您的手机暂不支持防火墙功能",
	 * Toast.LENGTH_LONG).show(); return; }
	 * //rejectedList.add(Integer.valueOf(detail.getUid())); //rejected =
	 * rejectedList.size(); TrafficAdapter.ViewHolder holder = ((ViewHolder)
	 * view.getTag()); detail.setRejected(true); updateTrafficDetail(detail);
	 * switchSliderForSingleItem(holder.slider_bed,
	 * holder.slider1,holder.slider2,false);//!rejected refreshUI(); } }
	 */

	public void refreshUI() {
		denyGprs.setText(myApp.rejectedList.size() + "");
		denyWifi.setText(myApp.wifiRejectedList.size() + "");
		if (config.isFirewallOn()) {
			// text_rl.setVisibility(View.VISIBLE);
			// target_apps_tv.setText("可联网程序数:" + installedApps);
			// rejected_apps_tv.setText("已禁止:" + rejected);
			int rejected = myApp.rejectedList.size();
			if (rejected == 0) {
				g2_bnt.setBackgroundResource(R.drawable.button_9);
			} else if (rejected == installedApps) {
				g2_bnt.setBackgroundResource(R.drawable.button_16);
			} else {
				g2_bnt.setBackgroundResource(R.drawable.button_10);
			}
			int wifiRejected = myApp.wifiRejectedList.size();
			if (wifiRejected == 0) {
				wifi_bnt.setBackgroundResource(R.drawable.button_15);
			} else if (wifiRejected == installedApps) {
				wifi_bnt.setBackgroundResource(R.drawable.button_11);
			} else {
				wifi_bnt.setBackgroundResource(R.drawable.button_12);
			}
			g2_bnt.setEnabled(true);
			wifi_bnt.setEnabled(true);
		} else {
			// text_rl.setVisibility(View.GONE);
			g2_bnt.setBackgroundResource(R.drawable.button_10);
			wifi_bnt.setBackgroundResource(R.drawable.button_12);
			g2_bnt.setEnabled(false);
			wifi_bnt.setEnabled(false);
		}
	}

	/*
	 * private void switchSliderForSingleItem(RelativeLayout
	 * slider_bed,ImageView slider1,ImageView slider2,boolean on_off) { if
	 * (on_off) { if (slider1.getVisibility() ==View.VISIBLE) {
	 * slider1.setVisibility(View.GONE); slider2.setVisibility(View.VISIBLE); }
	 * slider_bed.setBackgroundResource(R.drawable.firewall_item_slider_bed_on);
	 * } else { if (slider2.getVisibility() ==View.VISIBLE) {
	 * slider2.setVisibility(View.GONE); slider1.setVisibility(View.VISIBLE); }
	 * slider_bed
	 * .setBackgroundResource(R.drawable.firewall_item_slider_bed_off); }
	 * 
	 * }
	 */
	/*
	 * @Override public void onItemClick(AdapterView<?> parent, View view, int
	 * position, long id) { TrafficAdapter adapter = (TrafficAdapter)
	 * parent.getAdapter(); adapter.setSelectedItem(position);
	 * adapter.notifyDataSetInvalidated(); if (config.isFirewallOn()) { if
	 * (CallStatApplication.canFirewallWork) { if
	 * (!FirewallCoreWorker.hasRootAccess()) {
	 * Toast.makeText(TrafficTopActivity.this,
	 * "亲！防火墙需要root权限。请先确保您的手机对应用程序开放了root权限哦！", Toast.LENGTH_LONG).show();
	 * 
	 * } else { if (true) { applyRule(parent, view, position, id); } } } else {
	 * Toast.makeText(TrafficTopActivity.this, "您的手机暂不支持防火墙功能",
	 * Toast.LENGTH_LONG).show(); } } }
	 */
	@Override
	public void onClick(View v) {
		if (v == firewall) {
			new ShiftFirewallThread().execute();
		} else if (v == back_rl) {
			howlong = 0;
			finish();
			overridePendingTransition(R.anim.push_right_in,
					R.anim.push_right_out);
		} else if (v == g2_bnt) {
			if (CallStatApplication.canFirewallWork) {
				if (!FirewallCoreWorker.hasRootAccess()) {
					showToast("亲！防火墙需要root权限。请先确保您的手机对应用程序开放了root权限哦！");
				} else {
					closeOrOpenAll = 1;
					new UpdateAllFirewall().execute();
				}
			} else {
				showToast("您的手机暂不支持防火墙功能");
			}

		} else if (v == wifi_bnt) {
			if (CallStatApplication.canFirewallWork) {
				if (!FirewallCoreWorker.hasRootAccess()) {
					showToast("亲！防火墙需要root权限。请先确保您的手机对应用程序开放了root权限哦！");
				} else {
					closeOrOpenAll = 2;
					new UpdateAllFirewall().execute();
				}
			} else {
				showToast("您的手机暂不支持防火墙功能");
			}
		} else if (v == freq_tv) {
			showCustomDailog();
		}

	}

	class UpdateAllFirewall extends AsyncTask<Void, Void, String> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if (closeOrOpenAll == 1) {
				progressDialog = ProgressDialog.show(TrafficTopActivity.this,
						null, "正在处理2G/3G防火墙.....", true);
			} else {
				progressDialog = ProgressDialog.show(TrafficTopActivity.this,
						null, "正在处理WIFI防火墙.....", true);
			}

			progressDialog.setCancelable(true);
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			if (closeOrOpenAll == 1) {
				if (result.equals("1")) {
					g2_bnt.setBackgroundResource(R.drawable.button_16);
				} else if (result.equals("2")) {
					g2_bnt.setBackgroundResource(R.drawable.button_9);
				} else {
					g2_bnt.setBackgroundResource(R.drawable.button_10);
				}
			} else {
				if (result.equals("1")) {
					wifi_bnt.setBackgroundResource(R.drawable.button_11);
				} else if (result.equals("2")) {
					wifi_bnt.setBackgroundResource(R.drawable.button_15);
				} else {
					wifi_bnt.setBackgroundResource(R.drawable.button_12);
				}
			}

			adapter.notifyDataSetChanged();
			refreshUI();
			if (progressDialog != null) {
				progressDialog.dismiss();
			}
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			refreshUI();
			if (progressDialog != null) {
				progressDialog.dismiss();
			}
		}

		@Override
		protected String doInBackground(Void... arg0) {

			String code = "";

			if (closeOrOpenAll == 1) {
				int rejected = myApp.rejectedList.size();
				if (rejected == 0) {
					for (NewTrafficDetail trafficDetail : newTrafficDetails) {
						trafficDetail.setIsGprsRejected(true);
						myApp.rejectedList.add(Integer.valueOf(trafficDetail
								.getUid()));

					}
					FirewallCoreWorker.applyRulesWithForbiddenList(
							mRejectedList, FirewallCoreWorker.RULE_REJECT,
							FirewallCoreWorker.GPRS_FILTER,
							CallStatApplication.canMyFirewallWork);
					code = "1";
				} else if (rejected == installedApps) {
					for (NewTrafficDetail trafficDetail : newTrafficDetails) {
						trafficDetail.setIsGprsRejected(false);
						myApp.rejectedList.remove(Integer.valueOf(trafficDetail
								.getUid()));
					}
					FirewallCoreWorker.applyRulesWithForbiddenList(
							mRejectedList, FirewallCoreWorker.RULE_ALLOW,
							FirewallCoreWorker.GPRS_FILTER,
							CallStatApplication.canMyFirewallWork);
					code = "2";
				} else {
					for (NewTrafficDetail trafficDetail : newTrafficDetails) {
						if (trafficDetail.getIsGprsRejected()) {
							trafficDetail.setIsGprsRejected(false);
							myApp.rejectedList.remove(Integer
									.valueOf(trafficDetail.getUid()));
						}
					}
					FirewallCoreWorker.applyRulesWithForbiddenList(
							mRejectedList, FirewallCoreWorker.RULE_ALLOW,
							FirewallCoreWorker.GPRS_FILTER,
							CallStatApplication.canMyFirewallWork);
					code = "3";
				}
			} else {
				int rejected = myApp.wifiRejectedList.size();
				if (rejected == 0) {
					for (NewTrafficDetail trafficDetail : newTrafficDetails) {
						trafficDetail.setIsWifiRejected(true);
						myApp.wifiRejectedList.add(Integer
								.valueOf(trafficDetail.getUid()));

					}
					FirewallCoreWorker.applyRulesWithForbiddenList(
							mRejectedList, FirewallCoreWorker.RULE_REJECT,
							FirewallCoreWorker.WIFI_FILTER,
							CallStatApplication.canMyFirewallWork);
					code = "1";
				} else if (rejected == installedApps) {
					for (NewTrafficDetail trafficDetail : newTrafficDetails) {
						trafficDetail.setIsWifiRejected(false);
						myApp.wifiRejectedList.remove(Integer
								.valueOf(trafficDetail.getUid()));
					}
					FirewallCoreWorker.applyRulesWithForbiddenList(
							mRejectedList, FirewallCoreWorker.RULE_ALLOW,
							FirewallCoreWorker.WIFI_FILTER,
							CallStatApplication.canMyFirewallWork);
					code = "2";
				} else {
					for (NewTrafficDetail trafficDetail : newTrafficDetails) {
						if (trafficDetail.getIsWifiRejected()) {
							trafficDetail.setIsWifiRejected(false);
							myApp.wifiRejectedList.remove(Integer
									.valueOf(trafficDetail.getUid()));
						}
					}
					FirewallCoreWorker.applyRulesWithForbiddenList(
							mRejectedList, FirewallCoreWorker.RULE_ALLOW,
							FirewallCoreWorker.WIFI_FILTER,
							CallStatApplication.canMyFirewallWork);
					code = "3";
				}
			}
			CallStatDatabase.getInstance(TrafficTopActivity.this)
					.updateNewTrafficDetail(newTrafficDetails);
			return code;
		}

	}

	class ShiftFirewallThread extends AsyncTask<Void, Void, Void> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressDialog = ProgressDialog.show(TrafficTopActivity.this, null,
					"正在努力加载数据.....", true);
			progressDialog.setCancelable(true);
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			adapter = (TrafficAdapter) trafficRank.getAdapter();
			switchFirewallSlider(config.isFirewallOn());
			if (adapter == null) {
				return;
			}
			adapter.showFirewallBtn(config.isFirewallOn());
			refreshUI();
			if (progressDialog != null) {
				progressDialog.dismiss();
			}
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			refreshUI();
			if (progressDialog != null) {
				progressDialog.dismiss();
			}
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);
		}

		@Override
		protected Void doInBackground(Void... params) {
			// Log.e("callstats", "ShiftFirewallThread");
			boolean flag = config.isFirewallOn();
			if (flag) {
				config.setFirewallOn(false);
				FirewallCoreWorker
						.purgeIptables(CallStatApplication.canMyFirewallWork);
			} else {
				config.setFirewallOn(true);
				try {
					FirewallCoreWorker.initIptables(
							CallStatApplication.canMyFirewallWork, false);
					FirewallCoreWorker.applyRulesWithForbiddenList(
							myApp.rejectedList, FirewallCoreWorker.RULE_REJECT,
							FirewallCoreWorker.GPRS_FILTER,
							CallStatApplication.canMyFirewallWork);
					FirewallCoreWorker.applyRulesWithForbiddenList(
							myApp.wifiRejectedList,
							FirewallCoreWorker.RULE_REJECT,
							FirewallCoreWorker.WIFI_FILTER,
							CallStatApplication.canMyFirewallWork);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return null;
		}
	}

	private void showCustomDailog() {
		// TODO Auto-generated method stub
		CustomListDialog dialog = new CustomListDialog(this);
		// 设置对话框的标题
		dialog.setTitle("2G/3G流量显示");
		// Log.i("xx", provinceTextView.getText().toString());
		// Log.i("xx",
		// "getProvincesId(provinceTextView.getText().toString()="
		// + getProvincesId(provinceTextView.getText().toString()));
		// 0: 默认第一个单选按钮被选中
		dialog.setSingleChoiceItems(R.array.whichday, howlong,
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						howlong = which;
					}
				});
		dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				/*
				 * String temp = getResources().getStringArray(
				 * R.array.whichday)[howlong]; whichDayTextView.setText(temp);
				 * adapter.notifyDataSetChanged();
				 */
				new AsyncTask<Void, Void, String>() {

					@Override
					protected String doInBackground(Void... params) {
						String temp = getResources().getStringArray(
								R.array.whichday)[howlong];
						CallStatDatabase database = CallStatDatabase
								.getInstance(TrafficTopActivity.this);
						switch (howlong) {
						case 0:
							newTrafficDetails = (ArrayList<NewTrafficDetail>) database
									.getOneDayTrafficDetail(CallStatUtils
											.getNowDate());
							break;
						case 1:
							newTrafficDetails = (ArrayList<NewTrafficDetail>) database
									.getOneDayTrafficDetail(CallStatUtils
											.getYesterday());
							break;
						case 2:
							newTrafficDetails = (ArrayList<NewTrafficDetail>) database
									.getTrafficDetailFromTo(CallStatUtils
											.getNowDate(), CallStatUtils
											.getFirstMonthDayDate());
							break;
						default:
							break;
						}

						return temp;
					}

					@Override
					protected void onPostExecute(String result) {
						// TODO Auto-generated method stub
						super.onPostExecute(result);
						switch (howlong) {
						case 0:
							String[] traffic0 = CallStatUtils
									.traffic_unit((config.getTotalGprsUsed() - config
											.getEarliestGprsLog()));
							whichDayTextView.setText(result + "(" + traffic0[0]
									+ traffic0[1] + ")");

							if (newTrafficDetails != null
									&& newTrafficDetails.size() > 0) {

								correctGprs(config.getTotalGprsUsed()
										- config.getEarliestGprsLog());
							}
							break;
						case 1:
							// 先获得昨日的gprs流量使用量，为了保持数据统一性，一致从日消费表中获取
							long yesterdayGprsUsed = CallStatDatabase
									.getInstance(TrafficTopActivity.this)
									.getYesterdayGprsUsed();
							String[] traffic1 = CallStatUtils
									.traffic_unit(yesterdayGprsUsed);
							whichDayTextView.setText(result + "(" + traffic1[0]
									+ traffic1[1] + ")");
							if (newTrafficDetails != null
									&& newTrafficDetails.size() > 0) {

								correctGprs(config.getYesterdayGprsUsed());
							}
							break;
						case 2:
							String[] traffic2 = CallStatUtils.traffic_unit(config
									.getTotalGprsUsed()
									+ config.getTotalGprsUsedDifference());
							whichDayTextView.setText(result + "(" + traffic2[0]
									+ traffic2[1] + ")");
							if (newTrafficDetails != null
									&& newTrafficDetails.size() > 0) {

								correctGprs(config.getTotalGprsUsed());
							}
							break;
						default:
							break;
						}

						CallStatUtils.sortByNewTraffic(newTrafficDetails);
						adapter = new TrafficAdapter(TrafficTopActivity.this,
								newTrafficDetails, handler);
						trafficRank.setAdapter(adapter);
						// adapter.notifyDataSetChanged();
						if (progressDialog != null) {
							progressDialog.dismiss();
						}
					}

					@Override
					protected void onPreExecute() {
						super.onPreExecute();
						progressDialog = ProgressDialog.show(
								TrafficTopActivity.this, null, "正在努力加载数据.....",
								true);
						progressDialog.setCancelable(true);
					}

				}.execute();

			}
		});
		dialog.show();
	}

	private void correctGprs(long standGprs) {
		long totalTrafficGprs = 0;

		for (NewTrafficDetail trafficDetail : newTrafficDetails) {
			totalTrafficGprs += trafficDetail.getGprs();
		}

		if (totalTrafficGprs <= 0) {
			return;
		}

		double difference = standGprs * 1.0 / totalTrafficGprs;

		for (NewTrafficDetail trafficDetail : newTrafficDetails) {
			trafficDetail.setGprs(trafficDetail.getGprs() * difference);
		}
	}

	/*
	 * private int getWhichDay(String string) { int position = 0; for (int i =
	 * 0; i < iswhichday.length; i++) { if (string.equals(iswhichday[i])) {
	 * position = i; } } return position; }
	 */

	// @Override
	// public void finish() {
	// // TODO Auto-generated method stub
	// Log.e("callstats",
	// "TrafficTopActivity -----finish:" + this.isTaskRoot());
	// this.moveTaskToBack(true);
	// }

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

	// toast
	private void showToast(String string) {
		try {
			if (CallStatUtils.isMyAppOnDesk(TrafficTopActivity.this)) {
				ToastFactory.getToast(TrafficTopActivity.this, string,
						Toast.LENGTH_LONG).show();
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case MESSAGE_WHAT_TRAFFICTOP:
				ILog.LogI(this.getClass(), "用户行为采集: 流量排行界面次数 + 1");
				CallStatDatabase.getInstance(TrafficTopActivity.this)
						.updateActivityStatistic(TrafficTopActivity.this.getClass().getSimpleName(),
								config.getVersionName());
				break;
			default:
				break;
			}
		}
	};

}
