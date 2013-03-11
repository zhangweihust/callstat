/**     
 * HomeGuideActivity.java Create on 20120913  
 *     
 * @author long.xue   
 *    
 */
package com.archermind.callstat.home;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.archermind.callstat.R;
import com.archermind.callstat.home.views.ScrollLayout;
import com.archermind.callstat.home.views.ScrollLayout.OnScreenChangeListener;

public class HomeGuideActivity extends Activity implements
		OnScreenChangeListener, OnClickListener {

	private ScrollLayout workspace;
	private Button goBtn;
	private Button noBtn;
	private RelativeLayout terms_of_service;
	public static HomeGuideActivity instance;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.home_guide);

		instance = this;

		workspace = (ScrollLayout) findViewById(R.id.guide);
		workspace.setScreenChangeListener(this);
		goBtn = (Button) findViewById(R.id.yes_btn);
		noBtn = (Button) findViewById(R.id.no_btn);
		terms_of_service = (RelativeLayout) findViewById(R.id.home_center_rl);
		goBtn.setOnClickListener(this);
		noBtn.setOnClickListener(this);
		terms_of_service.setOnClickListener(this);
	}

	@Override
	public void screenIndex(int index) {
		if (index == 3) {

		}
	}

	/**
	 * 
	 */
	private void goToHome() {
		Intent intent = new Intent(HomeGuideActivity.this,
				NewInitInfoSettingActivity.class);
		startActivity(intent);
		finish();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {
		if (v == noBtn) {
			finish();
		} else if (v == goBtn) {
			goToHome();
		} else if (v == terms_of_service) {
			startActivity(new Intent(HomeGuideActivity.this,
					HomeGuidePrivacyActivity.class));
		}
	}
}
