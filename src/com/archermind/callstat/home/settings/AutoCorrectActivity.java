package com.archermind.callstat.home.settings;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.archermind.callstat.ConfigManager;
import com.archermind.callstat.R;
import com.archermind.callstat.home.views.CustomListDialog;

public class AutoCorrectActivity extends Activity implements OnClickListener {

	private RelativeLayout on_off;
	private LinearLayout select_freq;
	private RelativeLayout slider_bed;
	private ImageView left_slider;
	private ImageView right_slider;

	private Button positiveBtn;
	private Button nagetiveBtn;
	private TextView frequencyTextView;

	private ConfigManager config;
	String frequencyStr;

	Boolean checkswitch;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.traffic_correct_dialog);
		config = new ConfigManager(this);
		initUI();
	}

	private void initUI() {
		on_off = (RelativeLayout) findViewById(R.id.relativeLayout1);
		select_freq = (LinearLayout) findViewById(R.id.freq_tv);
		slider_bed = (RelativeLayout) findViewById(R.id.traffic_correct_slider_rl);

		left_slider = (ImageView) findViewById(R.id.left_slider_correct);
		right_slider = (ImageView) findViewById(R.id.right_slider_correct);

		positiveBtn = (Button) findViewById(R.id.ok_btn);
		nagetiveBtn = (Button) findViewById(R.id.cancel_btn);
		frequencyTextView = (TextView) findViewById(R.id.frequencyTextView);
		checkswitch = config.isTrafficAutoCheckOn();

		frequencyTextView.setText(intToString(config.getAccountFrequency()));

		on_off.setOnClickListener(this);
		select_freq.setOnClickListener(this);
		positiveBtn.setOnClickListener(this);
		nagetiveBtn.setOnClickListener(this);

		if (checkswitch) {
			left_slider.setVisibility(View.GONE);
			slider_bed.setBackgroundResource(R.drawable.yqy_setting);
			right_slider.setVisibility(View.VISIBLE);
			select_freq.setEnabled(true);
		} else {
			left_slider.setVisibility(View.VISIBLE);
			slider_bed.setBackgroundResource(R.drawable.wqy_setting);
			right_slider.setVisibility(View.GONE);
			select_freq.setEnabled(false);
		}

	}

	private void slide() {
		boolean flag = checkswitch;
		if (flag) {
			checkswitch = false;
			left_slider.setVisibility(View.VISIBLE);
			slider_bed.setBackgroundResource(R.drawable.wqy_setting);
			right_slider.setVisibility(View.GONE);
			select_freq.setEnabled(false);
		} else {
			checkswitch = true;
			left_slider.setVisibility(View.GONE);
			slider_bed.setBackgroundResource(R.drawable.yqy_setting);
			right_slider.setVisibility(View.VISIBLE);
			select_freq.setEnabled(true);
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

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.relativeLayout1:
			slide();

			break;
		case R.id.freq_tv:
			show_frequency_dialog();
			break;
		case R.id.ok_btn:
			if (frequencyStr == null) {
				frequencyStr = frequencyTextView.getText().toString();
			}
			config.setAccountFrequency(toInt(frequencyStr));
			config.setTrafficAutoCheckOn(checkswitch);
			finish();
			break;
		case R.id.cancel_btn:
			finish();
			break;

		default:
			break;
		}
	}

}
