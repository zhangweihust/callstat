package com.archermind.callstat.home.settings;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.archermind.callstat.ConfigManager;
import com.archermind.callstat.R;
import com.archermind.callstat.home.views.CustomListDialog;

public class AutoMoneyInputDialog extends Activity implements OnClickListener {

	private Button cancleBtn, okBtn;
	private LinearLayout freLayout;
	private TextView frequencyTextView;
	private RelativeLayout onOffLayout, onffItemLayout;

	private String frequencyStr;
	private ConfigManager config;

	Boolean checkswitch;

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		switch (arg0.getId()) {
		case R.id.cancel_btn:
			finish();
			break;
		case R.id.ok_btn:
			if (frequencyStr == null) {
				frequencyStr = frequencyTextView.getText().toString();
			}
			config.setAccountFrequency(toInt(frequencyStr));
			config.setAutoCheck(checkswitch);
			finish();
			break;
		case R.id.relativeLayout1:
			if (checkswitch) {
				switchSlider(false, onffItemLayout);
				checkswitch = false;
				freLayout.setEnabled(false);
			} else {
				switchSlider(true, onffItemLayout);
				checkswitch = true;
				freLayout.setEnabled(true);
			}

			break;
		case R.id.freq_tv:
			show_frequency_dialog();
			break;
		default:
			break;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.auto_money_input);
		config = new ConfigManager(this);
		init();

	}

	private void init() {
		cancleBtn = (Button) findViewById(R.id.cancel_btn);
		okBtn = (Button) findViewById(R.id.ok_btn);
		freLayout = (LinearLayout) findViewById(R.id.freq_tv);
		frequencyTextView = (TextView) findViewById(R.id.check_time_tv);
		onOffLayout = (RelativeLayout) findViewById(R.id.relativeLayout1);
		onffItemLayout = (RelativeLayout) findViewById(R.id.firewall_item_slider_rl);
		frequencyTextView.setText(intToString(config.getAccountFrequency()));

		checkswitch = config.isAutoCheck();

		cancleBtn.setOnClickListener(this);
		okBtn.setOnClickListener(this);
		onOffLayout.setOnClickListener(this);
		freLayout.setOnClickListener(this);

		switchSlider(config.isAutoCheck(), onffItemLayout);
		if (checkswitch) {
			freLayout.setEnabled(true);
		} else {
			freLayout.setEnabled(false);
		}
	}

	private void show_frequency_dialog() {
		CustomListDialog dialog = new CustomListDialog(this);
		dialog.setTitle("选择对账频率");
		// 0: 默认第一个单选按钮被选中
		dialog.setSingleChoiceItems(R.array.account_frequency,
				getFrequency_index(), new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
					}
				});
		dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				frequencyStr = getResources().getStringArray(
						R.array.account_frequency)[which];
				// ACOUNT_FREQUENCY_ISCLICK = true;
				frequencyTextView.setText(frequencyStr);
				dialog.dismiss();
			}
		});
		dialog.show();
	}

	private int getFrequency_index() {
		int index = 1;
		if ("每4小时".equals(frequencyTextView.getText().toString())) {// 2
			index = 0;
		} else if ("每8小时".equals(frequencyTextView.getText().toString())) {// 4
			index = 1;
		} else if ("每12小时".equals(frequencyTextView.getText().toString())) {// 6
			index = 2;
		} else if ("每24小时".equals(frequencyTextView.getText().toString())) {// 8
			index = 3;
		}
		return index;
	}

	private String intToString(int i) {
		String str;
		switch (i) {
		case 4:
			str = "每4小时";
			break;
		case 8:
			str = "每8小时";
			break;
		case 12:
			str = "每12小时";
			break;
		case 24:
			str = "每24小时";
			break;
		default:
			str = "";
			break;
		}
		return str;
	}

	private int toInt(String str) {
		int temp = 8;
		if ("每4小时".equals(str)) {
			temp = 4;
		} else if ("每8小时".equals(str)) {
			temp = 8;
		} else if ("每12小时".equals(str)) {
			temp = 12;
		} else if ("每24小时".equals(str)) {
			temp = 24;
		}
		return temp;
	}

	private void switchSlider(boolean isOn, RelativeLayout rlLayout) {
		ImageView leftImageView = (ImageView) rlLayout.getChildAt(1);
		ImageView rightImageView = (ImageView) rlLayout.getChildAt(0);

		if (isOn) {
			rlLayout.setBackgroundResource(R.drawable.yqy_setting);
			leftImageView.setVisibility(View.GONE);
			rightImageView.setVisibility(View.VISIBLE);
		} else {
			rlLayout.setBackgroundResource(R.drawable.wqy_setting);
			leftImageView.setVisibility(View.VISIBLE);
			rightImageView.setVisibility(View.GONE);
		}

	}

}
