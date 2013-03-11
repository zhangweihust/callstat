package com.archermind.callstat.firewall.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.archermind.callstat.CallStatApplication;
import com.archermind.callstat.ConfigManager;
import com.archermind.callstat.R;
import com.archermind.callstat.common.CallStatUtils;
import com.archermind.callstat.common.database.CallStatDatabase;
import com.archermind.callstat.firewall.FirewallCoreWorker;
import com.archermind.callstat.firewall.TrafficTopActivity;
import com.archermind.callstat.firewall.bean.NewTrafficDetail;

public class TrafficAdapter extends BaseAdapter {
	private Context mCtx;
	private List<NewTrafficDetail> trafficDetails;
	private LayoutInflater inflater;
	private PackageManager packageManager;
	private boolean isFirewallOn = false;
	private CallStatApplication myApp;
	private Handler handler;

	public TrafficAdapter(Context ctx, ArrayList<NewTrafficDetail> list,
			Handler handler) {
		mCtx = ctx;
		trafficDetails = list;
		this.handler = handler;
		inflater = LayoutInflater.from(mCtx);
		packageManager = mCtx.getPackageManager();
		isFirewallOn = new ConfigManager(mCtx).isFirewallOn();
		myApp = (CallStatApplication) ctx.getApplicationContext();
	}

	public void showFirewallBtn(boolean isFirewallOn) {
		this.isFirewallOn = isFirewallOn;
		this.notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return trafficDetails.size();
	}

	@Override
	public Object getItem(int position) {
		return trafficDetails.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		final NewTrafficDetail detail = (NewTrafficDetail) trafficDetails
				.get(position);
		ViewHolder holder = new ViewHolder();

		if (convertView == null) {
			convertView = inflater.inflate(R.layout.trafficstatistics_item_new,
					null);
			holder.icon = (ImageView) convertView
					.findViewById(R.id.programIcon);
			holder.uploadImage = (ImageView) convertView
					.findViewById(R.id.uploadIcon);
			holder.downloadImage = (ImageView) convertView
					.findViewById(R.id.downloadIcon);
			holder.appName = (TextView) convertView
					.findViewById(R.id.programName);
			holder.gprs = (TextView) convertView
					.findViewById(R.id.uploadTraffic);
			holder.wifi = (TextView) convertView
					.findViewById(R.id.downloadTraffic);
			holder.gprs_btn = (Button) convertView.findViewById(R.id.is_2g);
			holder.wifi_btn = (Button) convertView.findViewById(R.id.is_wifi);
			holder.layout = (RelativeLayout) convertView.findViewById(R.id.res);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		holder.gprs_btn.setTag(detail);
		holder.gprs_btn.setOnClickListener(gprsCheckOnClickListener);

		holder.wifi_btn.setTag(detail);
		holder.wifi_btn.setOnClickListener(wifiCheckOnClickListener);

		holder.layout.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (!isFirewallOn) {
					Toast.makeText(mCtx, "请开启防火墙！", Toast.LENGTH_LONG).show();
				}
			}
		});

		// convertView.setTag(holder);

		if (position % 2 != 0) {
			convertView.setBackgroundResource(R.drawable.new_deep);
		} else {
			convertView.setBackgroundResource(R.drawable.new_light);
		}

		long u_temp = (long) detail.getGprs()/*
											 * detail.getGprsUpload() +
											 * detail.getGprsDownload()
											 */;
		long d_temp = detail.getWifiUpload() + detail.getWifiDownload();
		// long traffic = u_temp + d_temp;

		try {
			holder.icon.setImageDrawable(packageManager
					.getApplicationIcon(detail.getPackageName()));
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		String[] gprs_traffic = CallStatUtils.traffic_unit(u_temp);
		String[] wifi_traffic = CallStatUtils.traffic_unit(d_temp);
		// String[] traffic_unit = CallStatUtils.traffic_unit(traffic);
		holder.appName.setText(detail.getAppName());
		holder.gprs.setText(gprs_traffic[0] + gprs_traffic[1]);
		holder.wifi.setText(wifi_traffic[0] + wifi_traffic[1]);
		// holder.total.setText(traffic_unit[0] + traffic_unit[1]);
		// Log.i("wanglei", "isFirewallOn="+isFirewallOn);
		if (!isFirewallOn) {

			// Log.i("wanglei",
			// "isFirewallOn=detail.isRejected()="+detail.getIsGprsRejected());
			// Log.i("wanglei",
			// "isFirewallOn=detail.isWifiRejected()="+detail.getIsWifiRejected());
			if (detail.getIsGprsRejected()) {
				holder.gprs_btn.setBackgroundResource(R.drawable.button_6);
			} else {
				holder.gprs_btn.setBackgroundResource(R.drawable.button_3);
			}
			if (detail.getIsWifiRejected()) {
				holder.wifi_btn.setBackgroundResource(R.drawable.button_6);
			} else {
				holder.wifi_btn.setBackgroundResource(R.drawable.button_3);
			}
			// holder.gprs_btn.setEnabled(false);
			// holder.wifi_btn.setEnabled(false);

		} else {
			// holder.gprs_btn.setEnabled(true);
			// holder.wifi_btn.setEnabled(true);
			// Log.i("wanglei",
			// "position="+position+";detail.isRejected()="+detail.getIsGprsRejected());
			if (detail.getIsGprsRejected()) {
				holder.gprs_btn.setBackgroundResource(R.drawable.button_8);
			} else {
				holder.gprs_btn.setBackgroundResource(R.drawable.button_1);
			}
			if (detail.getIsWifiRejected()) {
				holder.wifi_btn.setBackgroundResource(R.drawable.button_8);
			} else {
				holder.wifi_btn.setBackgroundResource(R.drawable.button_1);
			}

		}

		return convertView;
	}

	private void updateTrafficDetail(NewTrafficDetail td) {
		new UpdateTrafficDetailThread().execute(td);
	}

	class UpdateTrafficDetailThread extends AsyncTask<Object, Void, Long> {
		@Override
		protected Long doInBackground(Object... params) {
			CallStatDatabase db = CallStatDatabase.getInstance(mCtx);
			NewTrafficDetail td = (NewTrafficDetail) params[0];
			return db.updateNewTrafficDetail(td);
		}
	}

	private View.OnClickListener gprsCheckOnClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			NewTrafficDetail detail = (NewTrafficDetail) v.getTag();
			// Log.i("wanglei",
			// "gprs_btn onClick"+detail.getIsGprsRejected());
			if (CallStatApplication.canFirewallWork) {

				if (isFirewallOn) {

					if (!FirewallCoreWorker.hasRootAccess()) {
						Toast.makeText(mCtx,
								"亲！防火墙需要root权限。请先确保您的手机对应用程序开放了root权限哦！",
								Toast.LENGTH_LONG).show();
					} else {
						if (detail.getIsGprsRejected()) {
							// Log.i("wanglei", "detail.setRejected(false);");
							FirewallCoreWorker.applyRule(detail.getUid(),
									FirewallCoreWorker.RULE_ALLOW,
									FirewallCoreWorker.GPRS_FILTER,
									CallStatApplication.canMyFirewallWork);
							v.setBackgroundResource(R.drawable.button_1);
							detail.setIsGprsRejected(false);
							myApp.rejectedList.remove(Integer.valueOf(detail
									.getUid()));
							handler.sendEmptyMessage(0);
						} else {
							// Log.i("wanglei", "detail.setRejected(true);");
							FirewallCoreWorker.applyRule(detail.getUid(),
									FirewallCoreWorker.RULE_REJECT,
									FirewallCoreWorker.GPRS_FILTER,
									CallStatApplication.canMyFirewallWork);
							v.setBackgroundResource(R.drawable.button_8);
							detail.setIsGprsRejected(true);
							myApp.rejectedList.add(Integer.valueOf(detail
									.getUid()));
							handler.sendEmptyMessage(0);
						}
						updateTrafficDetail(detail);
						int size = myApp.rejectedList.size();
						if (size == 0) {
							TrafficTopActivity.g2_bnt
									.setBackgroundResource(R.drawable.button_9);
						} else if (size == getCount()) {
							TrafficTopActivity.g2_bnt
									.setBackgroundResource(R.drawable.button_16);
						} else {
							TrafficTopActivity.g2_bnt
									.setBackgroundResource(R.drawable.button_10);
						}
					}
				} else {
					Toast.makeText(mCtx, "请开启防火墙！", Toast.LENGTH_LONG).show();
				}
			} else {
				Toast.makeText(mCtx, "您的手机暂不支持防火墙功能", Toast.LENGTH_LONG).show();
			}
		}
	};

	private View.OnClickListener wifiCheckOnClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			NewTrafficDetail detail = (NewTrafficDetail) v.getTag();
			// Log.i("wanglei",
			// "gprs_btn onClick"+detail.getIsGprsRejected());
			if (CallStatApplication.canFirewallWork) {

				if (isFirewallOn) {
					if (!FirewallCoreWorker.hasRootAccess()) {
						Toast.makeText(mCtx,
								"防火墙需要root权限。请先确保您的手机对应用程序开放了root权限哦！",
								Toast.LENGTH_LONG).show();
					} else {

						if (detail.getIsWifiRejected()) {
							FirewallCoreWorker.applyRule(detail.getUid(),
									FirewallCoreWorker.RULE_ALLOW,
									FirewallCoreWorker.WIFI_FILTER,
									CallStatApplication.canMyFirewallWork);
							v.setBackgroundResource(R.drawable.button_1);
							detail.setIsWifiRejected(false);
							myApp.wifiRejectedList.remove(Integer
									.valueOf(detail.getUid()));
							handler.sendEmptyMessage(0);
						} else {
							FirewallCoreWorker.applyRule(detail.getUid(),
									FirewallCoreWorker.RULE_REJECT,
									FirewallCoreWorker.WIFI_FILTER,
									CallStatApplication.canMyFirewallWork);
							v.setBackgroundResource(R.drawable.button_8);
							detail.setIsWifiRejected(true);
							myApp.wifiRejectedList.add(Integer.valueOf(detail
									.getUid()));
							handler.sendEmptyMessage(0);
						}
						updateTrafficDetail(detail);
						int size = myApp.wifiRejectedList.size();
						if (size == 0) {
							TrafficTopActivity.wifi_bnt
									.setBackgroundResource(R.drawable.button_15);
						} else if (size == getCount()) {
							TrafficTopActivity.wifi_bnt
									.setBackgroundResource(R.drawable.button_11);
						} else {
							TrafficTopActivity.wifi_bnt
									.setBackgroundResource(R.drawable.button_12);
						}
					}
				} else {
					Toast.makeText(mCtx, "请开启防火墙！", Toast.LENGTH_LONG).show();
				}
			} else {
				Toast.makeText(mCtx, "您的手机暂不支持防火墙功能", Toast.LENGTH_LONG).show();
			}
		}
	};

	public static class ViewHolder {
		public RelativeLayout layout;
		public ImageView icon;
		public ImageView uploadImage;
		public ImageView downloadImage;
		public TextView appName;
		public TextView gprs;
		public TextView wifi;
		// public TextView total;
		public Button gprs_btn;
		public Button wifi_btn;
	}
}
