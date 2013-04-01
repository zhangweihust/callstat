package com.android.callstat.home.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.callstat.ConfigManager;
import com.android.callstat.common.CallStatUtils;
import com.android.callstat.firewall.FirewallUtils;
import com.android.callstat.home.views.ToastFactory;
import com.archermind.callstat.R;

public class TrafficWarningActivity extends Activity implements OnClickListener {

	ConfigManager configManager;

	private TextView trafficUsedTextView;
	private TextView trafficRemainTextView;

	private Button cancelButton;
	private Button commitButton;

	float totalGprs;
	long month_traffic_used;
	long month_traffic_remain;

	String remainString = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.traffic_warnning);
		configManager = new ConfigManager(this);
		init();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Log.i("xx", "TrafficWarningActivity---");
		// data();
	}

	private void init() {
		/*
		 * trafficUsedTextView = (TextView)
		 * findViewById(R.id.traffic_warn_used); trafficRemainTextView =
		 * (TextView) findViewById(R.id.traffic_warn_remain);
		 */
		cancelButton = (Button) findViewById(R.id.traffic_warn_cancel);
		commitButton = (Button) findViewById(R.id.traffic_warn_commit);

		cancelButton.setOnClickListener(this);
		commitButton.setOnClickListener(this);
	}

	private void data() {

		/* 已用流量 */
		month_traffic_used = configManager.getTotalGprsUsed()
				+ configManager.getTotalGprsUsedDifference();

		/* 总流量 */
		if (configManager.getFreeGprs() != 100000) {
			totalGprs = configManager.getFreeGprs();
			if (month_traffic_used <= totalGprs * 1024f * 1024f) {
				month_traffic_remain = (long) Math.round(totalGprs * 1024f
						* 1024f - month_traffic_used);
			} else {
				month_traffic_remain = 0;
			}
			remainString = CallStatUtils.traffic_unit(month_traffic_remain)[0]
					+ CallStatUtils.traffic_unit(month_traffic_remain)[1];

			trafficRemainTextView.setText(remainString);
			trafficUsedTextView.setText(CallStatUtils
					.traffic_unit(month_traffic_used)[0]
					+ CallStatUtils.traffic_unit(month_traffic_used)[1]);
		}

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.traffic_warn_cancel:
			finish();
			break;
		case R.id.traffic_warn_commit:
			boolean isGprsStillOpen = true;
			try {
				isGprsStillOpen = FirewallUtils
						.shiftGprs(TrafficWarningActivity.this);
			} catch (Exception e) {
				isGprsStillOpen = true;
			}
			if (!isGprsStillOpen) {
				showToast("GPRS网络已关闭");
				finish();
			} else {
				showToast("请手动关闭GPRS网络");
				startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
				finish();
			}
			break;
		default:
			break;
		}
	}

	private void showToast(String string) {
		ToastFactory.getToast(this, string, Toast.LENGTH_LONG).show();
	}
}
