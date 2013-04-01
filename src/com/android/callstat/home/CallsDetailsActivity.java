package com.android.callstat.home;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.callstat.ConfigManager;
import com.android.callstat.ILog;
import com.android.callstat.accounting.ReconciliationUtils;
import com.android.callstat.common.CallStatUtils;
import com.android.callstat.common.database.CallStatDatabase;
import com.android.callstat.home.settings.PackageSettingActivity;
import com.android.callstat.home.views.MyProgressBar;
import com.archermind.callstat.R;

public class CallsDetailsActivity extends Activity implements OnClickListener,
		OnTouchListener {

	private ConfigManager configManager;

	/* init */
	private RelativeLayout backBtn; // 返回按钮
	private MyProgressBar callTimeProgressBar; // 包月通话进度条
	private MyProgressBar smsProgressBar; // 包月短信进度条

	private TextView lastCheckTextView; // 上次对账
	private TextView lastGuessTextView; // 上次预估
	private TextView errorTextView; // 本月主叫

	private RelativeLayout callTimeSet_layout;
	private RelativeLayout smsSet_layout;

	private TextView calls_usedTextView;// 话费已用
	private TextView calls_used_unitTextView;// 话费已用 单位

	private TextView callpackageTextView; // 通话“套餐“
	private TextView total_callTimeTextView; // 通话套餐时长

	private TextView native_callTimeTextView; // 本地通话时长
	private TextView native_usedTextView; // 本地通话时长消费
	private TextView native_used_unitTextView; // 本地通话时长消费单位

	private TextView distance_callTimeTextView; // 长途通话时长
	private TextView distance_usedTextView; // 长途通话时长消费
	private TextView distance_used_unitTextView; // 长途通话时长消费 单位

	private TextView roam_callTimeTextView; // 漫游通话时长
	private TextView roam_usedTextView; // 漫游通话时长消费
	private TextView roam_used_unitTextView; // 漫游通话时长消费 单位

	private TextView sms_sendTextView; // 短信已发
	private TextView sms_usedTextView; // 短信消费
	private TextView sms_used_unitTextView; // 短信消费 单位

	private TextView total_smsTextView; // 包月短信
	private TextView total_smsStringTextView; // 短信消费

	private TextView rentTextView; // 套餐月租
	private TextView rent_unitTextView; // 套餐月租单位

	private TextView checkTimeTextView; // 对账时间

	private Button calltime_arrow;
	private Button sms_arrow;

	/* data */
	private float calls_used;

	float start_progress;
	float start_progress2;
	float callProgress = 0;
	float smsProgress = 0;

	boolean CALL_PROGRESS = false;
	boolean SMS_PROGRESS = false;
	final static int MESSAGE_WHAT_DETAILS = 0;

	ProgressDialog progressDialog;

	private java.text.DecimalFormat df = new java.text.DecimalFormat("#.##");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.calls_details);
		configManager = new ConfigManager(this);
		details_init();
	}

	@Override
	protected void onResume() {
		super.onResume();
		try {
			InitDetailsThread detailsThread = new InitDetailsThread();
			detailsThread.execute();
			detailsData();
			// 用户行为采集
			if (mHandler.hasMessages(MESSAGE_WHAT_DETAILS)) {
				mHandler.removeMessages(MESSAGE_WHAT_DETAILS);
			}
			mHandler.sendEmptyMessageDelayed(MESSAGE_WHAT_DETAILS, 5000);
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	/* init */
	private void details_init() {
		backBtn = (RelativeLayout) findViewById(R.id.details_rl);
		callTimeProgressBar = (MyProgressBar) findViewById(R.id.calltime_pbar);
		smsProgressBar = (MyProgressBar) findViewById(R.id.sms_pbar);

		lastCheckTextView = (TextView) findViewById(R.id.details_lastCheck);
		lastGuessTextView = (TextView) findViewById(R.id.details_guess);
		errorTextView = (TextView) findViewById(R.id.details_error_sent);
		callTimeSet_layout = (RelativeLayout) findViewById(R.id.details_calltime_layout);
		smsSet_layout = (RelativeLayout) findViewById(R.id.details_sms_layout);

		calls_usedTextView = (TextView) findViewById(R.id.details_used);
		calls_used_unitTextView = (TextView) findViewById(R.id.details_used_unit);
		callpackageTextView = (TextView) findViewById(R.id.total_callTime_string);
		total_callTimeTextView = (TextView) findViewById(R.id.total_callTime);

		native_callTimeTextView = (TextView) findViewById(R.id.details_native_calltime);
		native_usedTextView = (TextView) findViewById(R.id.details_native_used);
		native_used_unitTextView = (TextView) findViewById(R.id.details_native_used_unit);

		distance_callTimeTextView = (TextView) findViewById(R.id.details_distance_callout_time);
		distance_usedTextView = (TextView) findViewById(R.id.details_distance_used);
		distance_used_unitTextView = (TextView) findViewById(R.id.details_distance_used_unit);

		roam_callTimeTextView = (TextView) findViewById(R.id.details_roam_callout_time);
		roam_usedTextView = (TextView) findViewById(R.id.details_roam_used);
		roam_used_unitTextView = (TextView) findViewById(R.id.details_roam_used_unit);

		sms_sendTextView = (TextView) findViewById(R.id.details_sms_send_count);
		sms_usedTextView = (TextView) findViewById(R.id.details_sms_used);
		sms_used_unitTextView = (TextView) findViewById(R.id.details_sms_used_unit);

		total_smsTextView = (TextView) findViewById(R.id.total_sms);
		total_smsStringTextView = (TextView) findViewById(R.id.total_sms_string);

		rentTextView = (TextView) findViewById(R.id.details_rent);
		rent_unitTextView = (TextView) findViewById(R.id.details_rent_unit);

		checkTimeTextView = (TextView) findViewById(R.id.check_time);
		calltime_arrow = (Button) findViewById(R.id.callsdetails_right_arrow);
		sms_arrow = (Button) findViewById(R.id.sms_right_arrow);

		backBtn.setOnClickListener(this);
		callTimeSet_layout.setOnClickListener(this);
		smsSet_layout.setOnClickListener(this);
		callTimeSet_layout.setOnTouchListener(this);
		smsSet_layout.setOnTouchListener(this);
	}

	/* calls details data */
	private void detailsData() {
		try {
			if (configManager == null) {
				return;
			}

			/* 已用话费 */
			String usedString = "";
			if (configManager.getFeeSpent() != 100000f) {
				calls_used = configManager.getFeeSpent();
				usedString = CallStatUtils.changeFloat(calls_used);
			} else {
				double feeSpendTmp = 0;
				if (CallsDetailsActivity.this != null) {
					double[] ret = CallStatDatabase.getInstance(
							CallsDetailsActivity.this).getThisMonthTotalSpend();
					feeSpendTmp = ret[0];
				}
				calls_used = (float) feeSpendTmp;
				if (calls_used < 0) {
					calls_used = 0f;
				}
				usedString = df.format(calls_used);
			}
			calls_usedTextView.setText(usedString);

			/* 误差 */
			if (configManager.getFeesRemian() != 100000) {
				lastCheckTextView.setText(df.format(configManager
						.getFeesRemian()) + "元");
				lastCheckTextView.setTextColor(Color.parseColor("#5379b9"));
			} else {
				lastCheckTextView.setText("未对账");
				lastCheckTextView.setTextColor(Color.parseColor("#5379b9"));
			}

			if (configManager.getCalculateFeeAvailable() != 100000) {
				lastGuessTextView.setText(df.format(configManager
						.getCalculateFeeAvailable()) + "元");
				lastGuessTextView.setTextColor(Color.parseColor("#5379b9"));
			} else {
				lastGuessTextView.setText("未估值");
				lastGuessTextView.setTextColor(Color.parseColor("#5379b9"));
			}

			if (configManager.getFeesRemian() != 100000
					&& configManager.getCalculateFeeAvailable() != 100000) {
				float guess = configManager.getCalculateFeeAvailable()
						- configManager.getFeesRemian();
				float c = (float) Math.abs(guess);
				if (guess < -20f) {
					errorTextView.setText(df.format(c) + "元(话费充值或话费返还)");
				} else {
					errorTextView.setText(df.format(c) + "元");
				}
				errorTextView.setTextColor(Color.parseColor("#5379b9"));
			} else {
				errorTextView.setText("未估值");
				errorTextView.setTextColor(Color.parseColor("#5379b9"));
			}

			/* 包月通话 */
			int totalTime = 0;
			if (configManager.getFreeCallTime() != 100000) {
				totalTime = configManager.getFreeCallTime();
				callpackageTextView.setVisibility(View.VISIBLE);
				total_callTimeTextView.setText(totalTime + "分钟");
			} else {
				callpackageTextView.setVisibility(View.GONE);
				total_callTimeTextView.setText("未设置");
			}

			/* 包月短信 */
			int totalmessage = 0;
			if (configManager.getFreeMessages() != 100000) {
				totalmessage = configManager.getFreeMessages();
				total_smsStringTextView.setVisibility(View.VISIBLE);
				total_smsTextView.setText(totalmessage + "条");
			} else {
				total_smsStringTextView.setVisibility(View.GONE);
				total_smsTextView.setText("未设置");
			}

			// 对账时间
			if (configManager.getLastCheckHasYeTime() != -1) {
				String[] strings = CallStatUtils.getDate(
						configManager.getLastCheckHasYeTime(),
						System.currentTimeMillis());
				if (Integer.parseInt(strings[0]) == 0) {
					checkTimeTextView.setText("" + strings[1]);
				} else {
					if (Integer.parseInt(strings[0]) > 0) {
						if (Integer.parseInt(strings[0]) == 1) {
							checkTimeTextView.setText("昨天");
						} else if (Integer.parseInt(strings[0]) == 2) {
							checkTimeTextView.setText("前天");
						} else {
							checkTimeTextView.setText(strings[0] + "天前");
						}
					} else {
						checkTimeTextView.setText(strings[1]); // 修改了系统的日期，导致现在的时间比对账的时间早
					}
				}
			} else {
				if (ReconciliationUtils.IsCheckingAccount) {
					checkTimeTextView.setText("正在对账");
				} else {
					checkTimeTextView.setText("上次对账失败");
				}

			}

		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	/* 刷新通话时长的数据 */
	private void callTime_data(Integer[] count) {
		try {
			Integer[] time = count;
			native_callTimeTextView.setText(time[0] + "分钟");
			distance_callTimeTextView.setText(time[1] + "分钟");
			roam_callTimeTextView.setText(time[2] + "分钟");
			sms_sendTextView.setText(time[3] + "条");

			// 本地拨号消费
			if (time[0] != 0) {
				if (configManager != null
						&& configManager.getLocalRates() != -1) { // 本地拨号的单价已经计算出来
					float local_rates = configManager.getLocalRates() * time[0];
					native_usedTextView.setVisibility(View.VISIBLE);
					native_usedTextView.setText(df.format(local_rates) + "");
					native_used_unitTextView.setText("元");
				} else {
					native_usedTextView.setVisibility(View.GONE);
					native_used_unitTextView.setText("未估值");
				}
			} else {
				native_usedTextView.setVisibility(View.VISIBLE);
				native_usedTextView.setText("0");
				native_used_unitTextView.setText("元");
			}

			// 长途拨打消费
			if (time[1] != 0) {
				if (configManager != null && configManager.getLongRates() != -1) { // 本地拨号的单价已经计算出来
					float long_rates = configManager.getLongRates() * time[1];
					distance_usedTextView.setVisibility(View.VISIBLE);
					distance_usedTextView.setText(df.format(long_rates) + "");
					distance_used_unitTextView.setText("元");
				} else {
					distance_usedTextView.setVisibility(View.GONE);
					distance_used_unitTextView.setText("未估值");
				}
			} else {
				distance_usedTextView.setVisibility(View.VISIBLE);
				distance_usedTextView.setText("0");
				distance_used_unitTextView.setText("元");
			}

			// 漫游拨打消费
			if (time[2] != 0) {
				if (configManager != null
						&& configManager.getRoamingRates() != -1) { // 本地拨号的单价已经计算出来
					float roaming_rates = configManager.getRoamingRates()
							* time[2];
					roam_usedTextView.setVisibility(View.VISIBLE);
					roam_usedTextView.setText(df.format(roaming_rates) + "");
					roam_used_unitTextView.setText("元");
				} else {
					roam_usedTextView.setVisibility(View.GONE);
					roam_used_unitTextView.setText("未估值");
				}
			} else {
				roam_usedTextView.setVisibility(View.VISIBLE);
				roam_usedTextView.setText("0");
				roam_used_unitTextView.setText("元");
			}

			// 短信消费
			String smsFees = null;
			if (configManager != null
					&& configManager.getFreeMessages() != 100000) {
				if (time[3] <= configManager.getFreeMessages()) {
					smsFees = "0";
				} else {
					smsFees = smsFees(
							time[3] - configManager.getFreeMessages(), 0.1f);
				}
			} else {
				smsFees = smsFees(time[3], 0.1f);
			}
			sms_usedTextView.setText(smsFees);

			// 月租消费

			// 通话时间的进度条
			if (configManager != null
					&& configManager.getFreeCallTime() != 100000) {
				int total = configManager.getFreeCallTime();
				callProgress = (time[0] + time[1] + time[2]) / (float) total
						* 100;
				if (callProgress > 100) {
					callProgress = 100;
				}
				CALL_PROGRESS = true;
			} else {
				callProgress = 0;
				CALL_PROGRESS = false;
			}

			// 短信进度条
			if (configManager != null
					&& configManager.getFreeMessages() != 100000) {
				int total_sms = configManager.getFreeMessages();
				smsProgress = time[3] / (float) total_sms * 100;
				if (smsProgress > 100) {
					smsProgress = 100;
				}
				SMS_PROGRESS = true;
			} else {
				smsProgress = 0;
				SMS_PROGRESS = false;
			}

			start_progress = 0;
			start_progress = 0;
			if (CALL_PROGRESS) {
				calltimeHandler.sendEmptyMessage(0);
			} else {
				calltimeHandler.sendEmptyMessage(1);
			}

			if (SMS_PROGRESS) {
				smsHandler.sendEmptyMessage(2);
			} else {
				smsHandler.sendEmptyMessage(3);
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		/* 返回 */
		case R.id.details_rl:
			try {
				finish();
				overridePendingTransition(R.anim.push_right_in,
						R.anim.push_right_out);
			} catch (Exception e) {
				ILog.logException(this.getClass(), e);
			}
			break;
		/* 包月通话设置 */
		case R.id.details_calltime_layout:
			try {
				Intent intent1 = new Intent(CallsDetailsActivity.this,
						PackageSettingActivity.class);
				intent1.putExtra(CallStatUtils.PACKAGE_SET, 1);
				startActivityForResult(intent1, 0);
				overridePendingTransition(R.anim.push_left_in,
						R.anim.push_left_out);
			} catch (Exception e) {
				ILog.logException(this.getClass(), e);
			}
			break;
		/* 包月短信设置 */
		case R.id.details_sms_layout:
			try {
				Intent intent2 = new Intent(CallsDetailsActivity.this,
						PackageSettingActivity.class);
				intent2.putExtra(CallStatUtils.PACKAGE_SET, 2);
				startActivityForResult(intent2, 0);
				overridePendingTransition(R.anim.push_left_in,
						R.anim.push_left_out);
			} catch (Exception e) {
				ILog.logException(this.getClass(), e);
			}
			break;

		default:
			break;
		}
	}

	/* 进度条进度控制 */
	private int minProgress(float progress) {
		int minProgress = (int) Math.round(progress);
		if (progress <= 13 && progress > 0) {
			minProgress = 13;
		}

		if (progress > 97 && progress < 100) {
			minProgress = 97;
		}

		return minProgress;
	}

	Handler calltimeHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);

			switch (msg.what) {
			case 0:
				try {
					callTimeProgressBar
							.setProgress(minProgress(start_progress));
					if (start_progress < callProgress) {
						float a = (callProgress - start_progress) / 30f;
						if (a < 0.5f) {
							a = 0.5f;
						}
						start_progress += a;
						if (calltimeHandler.hasMessages(0)) {
							calltimeHandler.removeMessages(0);
						}
						calltimeHandler.sendEmptyMessageDelayed(0, 20);
					} else {
						calltimeHandler.sendEmptyMessage(1);
					}
				} catch (Exception e) {
					ILog.logException(this.getClass(), e);
				}
				break;
			case 1:
				try {
					callTimeProgressBar.setProgress(minProgress(callProgress));
				} catch (Exception e) {
					ILog.logException(this.getClass(), e);
				}
				break;

			default:
				break;
			}
		}

	};

	Handler smsHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);

			switch (msg.what) {
			case 2:
				try {
					smsProgressBar.setProgress(minProgress(start_progress2));
					if (start_progress2 < smsProgress) {
						float a = (smsProgress - start_progress2) / 30f;
						if (a < 0.5f) {
							a = 0.5f;
						}
						start_progress2 += a;
						if (smsHandler.hasMessages(2)) {
							smsHandler.removeMessages(2);
						}
						smsHandler.sendEmptyMessageDelayed(2, 20);
					} else {
						smsHandler.sendEmptyMessage(3);
					}
				} catch (Exception e) {
					ILog.logException(this.getClass(), e);
				}
				break;
			case 3:
				try {
					smsProgressBar.setProgress(minProgress(smsProgress));
				} catch (Exception e) {
					ILog.logException(this.getClass(), e);
				}
				break;
			default:
				break;
			}
		}

	};

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

	class InitDetailsThread extends AsyncTask<Void, Void, Integer[]> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressDialog = ProgressDialog.show(CallsDetailsActivity.this,
					null, "正在努力加载数据.....", true);
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
		protected Integer[] doInBackground(Void... params) {

			while (CallsManagerActivity.initLogThread != null
					&& CallsManagerActivity.initLogThread.isAlive()) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			Integer[] integers = new Integer[4];
			try {
				int[] time = CallStatDatabase.getInstance(
						CallsDetailsActivity.this).getCallsDetails();
				int sms_count = CallStatDatabase.getInstance(
						CallsDetailsActivity.this).getDetailsMessages();
				if (time != null) {
					for (int i = 0; i < 3; i++) {
						integers[i] = time[i];
					}
				} else {
					integers[0] = 0;
					integers[1] = 0;
					integers[2] = 0;
				}

				if (sms_count != -1) {
					integers[3] = sms_count;
				} else {
					integers[3] = 0;
				}

				ILog.LogI(this.getClass(), "本地通话时间：" + time[0] + " 长途通话时间："
						+ time[1] + " 漫游通话时间：" + time[2]);
				ILog.LogI(this.getClass(), "短信条数：" + sms_count);
			} catch (Exception e) {
				ILog.logException(this.getClass(), e);
			}

			return integers;
		}

		@Override
		protected void onPostExecute(Integer[] result) {
			super.onPostExecute(result);
			if (progressDialog != null && progressDialog.isShowing()) {
				progressDialog.dismiss();
			}
			callTime_data(result);
		}

	}

	// 计算短信的消费
	private String smsFees(int count, float price) {
		String string = null;
		float cost = (float) Math.round(count * price * 10) / 10;
		if (cost == (int) cost) {
			string = (int) cost + "";
		} else {
			string = cost + "";
		}
		return string;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (v == callTimeSet_layout) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				calltime_arrow
						.setBackgroundResource(R.drawable.arrow_right_light);
				return false;
			case MotionEvent.ACTION_UP:
				calltime_arrow.setBackgroundResource(R.drawable.arrow_right);
				return false;
			case MotionEvent.ACTION_CANCEL:
				calltime_arrow.setBackgroundResource(R.drawable.arrow_right);
				return false;
			default:
				break;
			}
		} else if (v == smsSet_layout) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				sms_arrow.setBackgroundResource(R.drawable.arrow_right_light);
				return false;
			case MotionEvent.ACTION_UP:
				sms_arrow.setBackgroundResource(R.drawable.arrow_right);
				return false;
			case MotionEvent.ACTION_CANCEL:
				sms_arrow.setBackgroundResource(R.drawable.arrow_right);
				return false;
			default:
				break;
			}
		}

		return false;
	}

	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case MESSAGE_WHAT_DETAILS:
				ILog.LogI(this.getClass(), "用户行为采集: 话费详单界面次数 + 1");
				CallStatDatabase.getInstance(CallsDetailsActivity.this)
						.updateActivityStatistic(CallsDetailsActivity.this.getClass().getSimpleName(),
								configManager.getVersionName());
				break;
			default:
				break;
			}
		}
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
		ILog.LogI(this.getClass(), "话费详单界面 onDestroy");
		if (mHandler.hasMessages(MESSAGE_WHAT_DETAILS)) {
			mHandler.removeMessages(MESSAGE_WHAT_DETAILS);
		}
	}

}
