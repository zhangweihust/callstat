package com.archermind.callstat.home;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;

import com.archermind.callstat.ILog;
import com.archermind.callstat.R;

public class HomeGuidePrivacyActivity extends Activity implements
		OnClickListener {

	private Button goBtn;
	private Button noBtn;
	private Button backBtn;
	private String activity_state;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.home_guide_privacy);

		Intent intent = getIntent();
		activity_state = intent.getStringExtra("activity_state");

		goBtn = (Button) findViewById(R.id.yes_btn);
		noBtn = (Button) findViewById(R.id.no_btn);
		backBtn = (Button) findViewById(R.id.back_btn);

		if (activity_state != null && activity_state.equals("about")) {
			goBtn.setVisibility(View.GONE);
			noBtn.setVisibility(View.GONE);
			backBtn.setVisibility(View.VISIBLE);
			backBtn.setOnClickListener(this);
		} else {
			backBtn.setVisibility(View.GONE);
			goBtn.setOnClickListener(this);
			noBtn.setOnClickListener(this);
		}

	}

	@Override
	public void onClick(View v) {
		if (v == noBtn) {
			finish();
		} else if (v == goBtn) {
			goToHome();
		} else if (v == backBtn) {
			finish();
		}
	}

	private void goToHome() {
		try {
			Intent intent = new Intent(HomeGuidePrivacyActivity.this,
					NewInitInfoSettingActivity.class);
			startActivity(intent);
			HomeGuideActivity.instance.finish();
			finish();
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}
}
