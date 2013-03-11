package com.archermind.callstat.home.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.archermind.callstat.R;
import com.archermind.callstat.common.CallStatUtils;
import com.archermind.callstat.service.CallStatSMSService;

public class IsCancelVIPDialog extends Activity implements OnClickListener {

	private Button cancel_btn, ok_btn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.is_vip_dialog);
		cancel_btn = (Button) findViewById(R.id.cancel_btn);
		ok_btn = (Button) findViewById(R.id.ok_btn);
		cancel_btn.setOnClickListener(this);
		ok_btn.setOnClickListener(this);
	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		switch (arg0.getId()) {
		case R.id.cancel_btn:
			finish();
			break;
		case R.id.ok_btn:
			removeVIP();
			finish();
			break;
		default:
			break;
		}
	}

	private void removeVIP() {
		if (CallStatUtils.isNetworkAvailable(this)) {
			goToService(CallStatSMSService.PHONE_UNBINDIND_STRING);
		} else {
			Toast.makeText(this, "网络不可用，请检查网络情况", Toast.LENGTH_SHORT).show();
		}

	}

	private void goToService(String action) {
		Intent intent = new Intent(this, CallStatSMSService.class);
		intent.setAction(action);
		startService(intent);
	}

}
