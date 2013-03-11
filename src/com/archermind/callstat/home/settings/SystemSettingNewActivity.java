package com.archermind.callstat.home.settings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.protocol.HTTP;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.archermind.callstat.CallStatApplication;
import com.archermind.callstat.ConfigManager;
import com.archermind.callstat.ILog;
import com.archermind.callstat.R;
import com.archermind.callstat.accounting.AccountingKeyWordsBean;
import com.archermind.callstat.common.CallStatUtils;
import com.archermind.callstat.common.ResourceLoader.OnLoadFinishListener;
import com.archermind.callstat.common.database.CallStatDatabase;
import com.archermind.callstat.common.json.AbstractWebLoadManager.OnWebLoadListener;
import com.archermind.callstat.common.net.MyHttpPostHelper;
import com.archermind.callstat.home.bean.SmsVerifyBean;
import com.archermind.callstat.home.json.SmsVerifyManager;
import com.archermind.callstat.home.views.ToastFactory;
import com.archermind.callstat.service.CallStatSMSService;
import com.archermind.callstat.service.json.AccountingDatabaseUpdater;

public class SystemSettingNewActivity extends Activity implements
		OnClickListener, OnTouchListener {

	private RelativeLayout mBackLayout, mMyNOLayout, mAddressLayout,
			mPackageLayout, mBudgetLayout, mCallsLayout, mFlowLayout,
			mUpdateLayout, mModifyLayout, mOnOffLayout, mShowDeskLayout,
			mShowBalanceLayout, mShowFlowLayout, mShowSpeedLayout,
			mNoticeLayout, mDisturbLayout, mFeedbackLayout, mAboutLayout,
			mExitLayout, flowerImageRL, showDeskRL, showMoneyRL, showFlowRL,
			showSpeedRL, noticeRL, zhuanxiangRL, zhuanxiangOnoffLayout,
			backstageLayout, backstageRL;// mShareLayout,
	private TextView mTelNOView, mBindingView, mAddressView, mPackageView,
			mMoneyView, mUpdateView, mDisturbView;
	private TextView mShowDeskView, mShowMoneyView, mShowFlowView,
			mShowSpeedView, mFlowLayoutTextView, mPhoneTextView,
			mAddressTextView, mPackageTextView, mMoneyTextView, mHFJZTextView,
			mUpdateTextView, mModifyTextView, mOnOffTextView, mNoticeTextView,
			mDisturbTextView, mFKTextView, mAboutTextView, mBackTextView,
			zhuanxiangTextView, yanzhengTextView, backstageTextView;
	private RelativeLayout chargeOnlineRl;
	private TextView chargeOnlineTv, chargeDescTv;
	private ImageView mChargeIcon, mCharegeArrow;

	private ImageView mFlowLayoutImageView, mPhoneImageView, mAddressImageView,
			mPackageImageView, mMoneyImageView, mHFJZImageView,
			mUpdateImageView, mModifyImageView, mOnOffImageView,
			mNoticeImageView, mDisturbImageView, mFKImageView, mAboutImageView,
			mBackImageView, /* mExitImageView, */zhuanxiangImageView,
			backstageImageView, mTelNoArrow, mTelAddressArrow,
			mTelPackageArrow, mTelMoneyArrow, mTelHfjzArrow, mTelLljzArrow,
			mTelModifyArrow, mTelDisturbArrow, mTelFkArrow, mTelAboutArrow;

	private ConfigManager config;
	private ProgressDialog mPd;
	private CallStatApplication callstatApplication;
	private AccountingDatabaseUpdater accountingDatabaseUpdater;

	private String mPhoneNO, mProvinceStr, mCityStr, mOperatorStr, mPackageStr;
	private int mFreeCallTime, mFreeMSG;
	private int mFreeFlow;
	private String mFreeCallTimeStr, mFreeMSGStr, mFreeFlowStr;
	public static final String FLOATWINDOW_BROADCAST_ACTION = "com.archermind.broadcastreceiver.floatwindow_broadcast_action";

	public static final String ACTION_EXIT = "exit_action";

	private CallStatSMSService.SMSBinder binder;
	private ServiceConnection conn = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {

		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			binder = (CallStatSMSService.SMSBinder) service;
		}
	};

	private BroadcastReceiver floatWindowReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// Log.i("123", "onReceive");
			if (intent.getAction().equals(
					CallStatSMSService.ACTION_VERIFICATION_RESULT)) {
				// yanzhengTextView.setText("正在绑定中");
				goToService(CallStatSMSService.PHONE_BINDIND_STRING);
			} else if (intent.getAction().equals(
					CallStatSMSService.SEND_BIND_HANDLE)) {
				inityanzheng();
			} else if (intent.getAction().equals(FLOATWINDOW_BROADCAST_ACTION)) {
				showFloatWindow();
			} else if (intent.getAction().equals(
					CallStatSMSService.PHONE_BINDIND_RESULT)) {
				binder.bindRemoveHandler();
				initShowTextView();
				inityanzheng();
			} else if (intent.getAction().equals(
					CallStatSMSService.PHONE_UNBINDIND_RESULT)) {
				initShowTextView();
				inityanzheng();
			} else if (intent.getAction().equals(ACTION_EXIT))
				;
			{

			}
		}
	};

	private void goToService(String action) {
		Intent intent = new Intent(this, CallStatSMSService.class);
		intent.setAction(action);
		startService(intent);
	}

	private void inityanzheng() {
		switch (config.getPhoneBindingStatus()) {
		case -1:
			zhuanxiangOnoffLayout.setVisibility(View.VISIBLE);
			yanzhengTextView.setVisibility(View.GONE);
			switchSlider(true, false, zhuanxiangOnoffLayout);
			zhuanxiangTextView.setTextColor(getResources().getColor(
					R.color.setting_text_black));
			break;
		case 0:
			// zhuanxiangOnoffLayout.setVisibility(View.GONE);
			// yanzhengTextView.setVisibility(View.VISIBLE);
			// yanzhengTextView.setText("正在验证中");
			zhuanxiangOnoffLayout.setVisibility(View.VISIBLE);
			yanzhengTextView.setVisibility(View.GONE);
			switchSlider(true, false, zhuanxiangOnoffLayout);
			zhuanxiangTextView.setTextColor(getResources().getColor(
					R.color.setting_text_unclick));
			break;
		case 1:
			zhuanxiangOnoffLayout.setVisibility(View.VISIBLE);
			yanzhengTextView.setVisibility(View.GONE);
			switchSlider(true, true, zhuanxiangOnoffLayout);
			zhuanxiangTextView.setTextColor(getResources().getColor(
					R.color.setting_text_black));
			break;
		default:
			break;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		// callstatApplication = (CallStatApplication) getApplication();
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		if (getIntent().getExtras() != null) {
			switch (getIntent().getExtras().getInt("anim_type")) {
			case 1:
				overridePendingTransition(R.anim.enlarge_right_in,
						R.anim.center_disappear);
				break;
			case 2:
				overridePendingTransition(R.anim.push_right_in,
						R.anim.push_right_out);
				break;
			default:
				break;
			}
		}
		callstatApplication = (CallStatApplication) getApplication();
		setContentView(R.layout.new_main_setting);
		config = new ConfigManager(this);
		initView();

		bindService();// 绑定服务
		registerMyReceiver();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		initShowTextView();
		initImageView();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(conn); // 解除绑定服务
		unRegisterMyReceiver();
	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		switch (arg0.getId()) {
		case R.id.back_rl:// 返回按钮
			finish();
			overridePendingTransition(R.anim.center_appear,
					R.anim.reduce_right_disappear);
			break;
		case R.id.tel_no_rl:// 电话号码一栏
			toOtherActivity(PhoneNumberSettingActivity.class);
			break;
		case R.id.tel_zhuanxiang_rl:// 专享服务一栏
			zhuanxiangOnclick();
			break;
		case R.id.tel_address_rl:// 手机运营商一栏
			toOtherActivity(OperatorsSettingActivity.class);
			break;
		case R.id.tel_package_rl:// 套餐设置一栏
			toOtherActivity(PackageSettingActivity.class);
			break;
		case R.id.tel_money_rl:// 话费预算一栏
			toOtherActivity(CallsBudgetSettingActivity.class);
			break;
		case R.id.tel_hfjz_rl:// 话费校正一栏
			toOtherActivity(CallsCheckActivity.class);
			break;
		case R.id.tel_lljz_rl:// 流量校正一栏
			toOtherActivity(TrafficCheckActivity.class);
			break;
		case R.id.tel_update_rl:// 联网更新一栏
			checkAccountingCodeUpdate();
			break;
		case R.id.tel_modify_rl:// 手动修改一栏
			toOtherActivity(ManuallyModifyActivity.class);
			break;
		case R.id.tel_onoff_rl:// 开启桌面悬浮窗一栏
			floatWindowOnClick();
			break;
		case R.id.show_desk_rl:// 仅桌面显示一栏
			showDeskOnlyOnClick();
			break;
		case R.id.show_balance_rl:// 仅显示账户余额一栏
			floatShowMoney();
			break;
		case R.id.show_flow_rl:// 仅显示剩余流量一栏
			floatShowFlow();
			break;
		case R.id.show_speed_rl:// 仅显示当前网络一栏
			floatShowSpeed();
			break;
		case R.id.tel_notice_rl:// 启动通知栏一栏
			openNotice();
			break;
		case R.id.tel_disturb_rl:// 防打扰防辐射一栏
			toOtherActivity(DisturbActivity.class);
			break;
		case R.id.charge_online_rl:// 在线充值一栏
			charegeOnline();
			break;
		case R.id.tel_houtai_rl:// 开机启动一栏
			backstage();
			break;
		case R.id.relativeLayout_fk:// 反馈一栏
			// toOtherActivity(NewFeedbackActivity.class);
			Intent intent = new Intent();
			intent.setClass(SystemSettingNewActivity.this,
					NewFeedbackActivity.class);
			startActivityForResult(intent, 0);
			overridePendingTransition(R.anim.push_up_in, R.anim.push_up_out);
			break;
		case R.id.relativeLayout_about:// 关于一栏
			toOtherActivity(NewAboutUsActivity.class);
			break;
		case R.id.relativeLayout_exit:// 退出一栏
			exit();
			break;
		default:
			break;
		}
	}

	private void charegeOnline() {
		String url = getString(R.string.chargeOnline_url);
		// Log.e("callstats", "url:" + url);
		/*Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(url));
		startActivity(intent);*/
		CallStatUtils.openURL(this, url, "");
	}

	private void backstage() {
		if (config.isServicesStartOnBootComplete()) {
			config.setServicesStartOnBootComplete(false);
			switchSlider(true, false, backstageRL);
		} else {
			config.setServicesStartOnBootComplete(true);
			switchSlider(true, true, backstageRL);
		}
	}

	private void initView() {
		// 初始18个RelativeLayout控件，并赋予监听器
		mBackLayout = (RelativeLayout) findViewById(R.id.back_rl);
		mMyNOLayout = (RelativeLayout) findViewById(R.id.tel_no_rl);
		mAddressLayout = (RelativeLayout) findViewById(R.id.tel_address_rl);
		mPackageLayout = (RelativeLayout) findViewById(R.id.tel_package_rl);
		mBudgetLayout = (RelativeLayout) findViewById(R.id.tel_money_rl);
		mCallsLayout = (RelativeLayout) findViewById(R.id.tel_hfjz_rl);
		mFlowLayout = (RelativeLayout) findViewById(R.id.tel_lljz_rl);
		mUpdateLayout = (RelativeLayout) findViewById(R.id.tel_update_rl);
		mModifyLayout = (RelativeLayout) findViewById(R.id.tel_modify_rl);
		mOnOffLayout = (RelativeLayout) findViewById(R.id.tel_onoff_rl);
		mShowDeskLayout = (RelativeLayout) findViewById(R.id.show_desk_rl);
		mShowBalanceLayout = (RelativeLayout) findViewById(R.id.show_balance_rl);
		mShowFlowLayout = (RelativeLayout) findViewById(R.id.show_flow_rl);
		mShowSpeedLayout = (RelativeLayout) findViewById(R.id.show_speed_rl);
		mNoticeLayout = (RelativeLayout) findViewById(R.id.tel_notice_rl);
		mDisturbLayout = (RelativeLayout) findViewById(R.id.tel_disturb_rl);
		chargeOnlineRl = (RelativeLayout) findViewById(R.id.charge_online_rl);
		// mShareLayout = (RelativeLayout)
		// findViewById(R.id.relativeLayout_share);
		mFeedbackLayout = (RelativeLayout) findViewById(R.id.relativeLayout_fk);
		mAboutLayout = (RelativeLayout) findViewById(R.id.relativeLayout_about);
		mExitLayout = (RelativeLayout) findViewById(R.id.relativeLayout_exit);
		backstageLayout = (RelativeLayout) findViewById(R.id.tel_houtai_rl);

		flowerImageRL = (RelativeLayout) findViewById(R.id.flowerwindow_on_off_rl);
		showDeskRL = (RelativeLayout) findViewById(R.id.desk_on_off_rl);
		showMoneyRL = (RelativeLayout) findViewById(R.id.showmoney_on_off_rl);
		showFlowRL = (RelativeLayout) findViewById(R.id.showflow_on_off_rl);
		showSpeedRL = (RelativeLayout) findViewById(R.id.showspeed_on_off_rl);
		noticeRL = (RelativeLayout) findViewById(R.id.notice_on_off_rl);
		zhuanxiangRL = (RelativeLayout) findViewById(R.id.tel_zhuanxiang_rl);
		zhuanxiangOnoffLayout = (RelativeLayout) findViewById(R.id.zhuanxiang_on_off_rl);
		backstageRL = (RelativeLayout) findViewById(R.id.houtai_on_off_rl);

		// textview
		mPhoneTextView = (TextView) findViewById(R.id.tel_no_name);
		mAddressTextView = (TextView) findViewById(R.id.tel_address_name);
		mPackageTextView = (TextView) findViewById(R.id.tel_package_name);
		mMoneyTextView = (TextView) findViewById(R.id.tel_money_name);
		mHFJZTextView = (TextView) findViewById(R.id.tel_hfjz_tv);
		mFlowLayoutTextView = (TextView) findViewById(R.id.tel_lljz_tv);
		mUpdateTextView = (TextView) findViewById(R.id.tel_update_name);
		mModifyTextView = (TextView) findViewById(R.id.tel_modify_tv);
		mOnOffTextView = (TextView) findViewById(R.id.tel_onoff_tv);
		mNoticeTextView = (TextView) findViewById(R.id.tel_notice_tv);
		mDisturbTextView = (TextView) findViewById(R.id.tel_disturb_name);
		mFKTextView = (TextView) findViewById(R.id.textView_fk);
		mAboutTextView = (TextView) findViewById(R.id.textView_about);
		mBackTextView = (TextView) findViewById(R.id.textView_exit);
		zhuanxiangTextView = (TextView) findViewById(R.id.tel_zhuanxiang_tv);
		yanzhengTextView = (TextView) findViewById(R.id.yanzheng);
		backstageTextView = (TextView) findViewById(R.id.tel_houtai_tv);

		chargeOnlineTv = (TextView) findViewById(R.id.tel_charge_name);
		chargeDescTv = (TextView) findViewById(R.id.tel_charege_desc_tv);

		// imageview
		mPhoneImageView = (ImageView) findViewById(R.id.tel_no_iv);
		mAddressImageView = (ImageView) findViewById(R.id.tel_address_iv);
		mPackageImageView = (ImageView) findViewById(R.id.tel_package_iv);
		mMoneyImageView = (ImageView) findViewById(R.id.tel_money_iv);
		mHFJZImageView = (ImageView) findViewById(R.id.tel_hfjz_iv);
		mFlowLayoutImageView = (ImageView) findViewById(R.id.tel_lljz_iv);
		mUpdateImageView = (ImageView) findViewById(R.id.tel_update_iv);
		mModifyImageView = (ImageView) findViewById(R.id.tel_modify_iv);
		mOnOffImageView = (ImageView) findViewById(R.id.tel_onoff_iv);
		mNoticeImageView = (ImageView) findViewById(R.id.tel_notice_iv);
		mDisturbImageView = (ImageView) findViewById(R.id.tel_disturb_iv);
		mFKImageView = (ImageView) findViewById(R.id.imageView_fk);
		mAboutImageView = (ImageView) findViewById(R.id.imageView_about);
		mBackImageView = (ImageView) findViewById(R.id.imageView_exit);
		// mExitImageView = (ImageView) findViewById(R.id.refresh_img);
		zhuanxiangImageView = (ImageView) findViewById(R.id.tel_zhuanxiang_iv);
		backstageImageView = (ImageView) findViewById(R.id.tel_houtai_iv);

		mTelNoArrow = (ImageView) findViewById(R.id.tel_no_arrow);
		mTelAddressArrow = (ImageView) findViewById(R.id.tel_address_arrow);
		mTelPackageArrow = (ImageView) findViewById(R.id.tel_package_arrow);
		mTelMoneyArrow = (ImageView) findViewById(R.id.tel_money_arrow);
		mTelHfjzArrow = (ImageView) findViewById(R.id.tel_hfjz_arrow);
		mTelLljzArrow = (ImageView) findViewById(R.id.tel_lljz_arrow);
		mTelModifyArrow = (ImageView) findViewById(R.id.tel_modify_arrow);
		mTelDisturbArrow = (ImageView) findViewById(R.id.tel_disturb_arrow);
		mTelFkArrow = (ImageView) findViewById(R.id.fk_arrow);
		mTelAboutArrow = (ImageView) findViewById(R.id.about_arrow);
		mCharegeArrow = (ImageView) findViewById(R.id.tel_charge_arrow);
		mChargeIcon = (ImageView) findViewById(R.id.tel_charge_iv);

		mTelNoArrow.setOnTouchListener(this);
		mTelAddressArrow.setOnTouchListener(this);
		mTelPackageArrow.setOnTouchListener(this);
		mTelMoneyArrow.setOnTouchListener(this);
		mTelHfjzArrow.setOnTouchListener(this);
		mTelLljzArrow.setOnTouchListener(this);
		mTelModifyArrow.setOnTouchListener(this);
		mTelDisturbArrow.setOnTouchListener(this);
		mTelFkArrow.setOnTouchListener(this);
		mTelAboutArrow.setOnTouchListener(this);

		mBackLayout.setOnTouchListener(this);
		mMyNOLayout.setOnTouchListener(this);
		mAddressLayout.setOnTouchListener(this);
		mPackageLayout.setOnTouchListener(this);
		mBudgetLayout.setOnTouchListener(this);
		mCallsLayout.setOnTouchListener(this);
		mFlowLayout.setOnTouchListener(this);
		mUpdateLayout.setOnTouchListener(this);
		mModifyLayout.setOnTouchListener(this);
		mOnOffLayout.setOnTouchListener(this);
		mShowDeskLayout.setOnTouchListener(this);
		mShowBalanceLayout.setOnTouchListener(this);
		mShowFlowLayout.setOnTouchListener(this);
		mShowSpeedLayout.setOnTouchListener(this);
		mNoticeLayout.setOnTouchListener(this);
		mDisturbLayout.setOnTouchListener(this);
		chargeOnlineRl.setOnTouchListener(this);
		mFeedbackLayout.setOnTouchListener(this);
		mAboutLayout.setOnTouchListener(this);
		mExitLayout.setOnTouchListener(this);
		mBackLayout.setOnTouchListener(this);
		zhuanxiangRL.setOnTouchListener(this);
		backstageLayout.setOnTouchListener(this);

		backstageLayout.setOnClickListener(this);
		zhuanxiangRL.setOnClickListener(this);
		mBackLayout.setOnClickListener(this);
		mMyNOLayout.setOnClickListener(this);
		mAddressLayout.setOnClickListener(this);
		mPackageLayout.setOnClickListener(this);
		mBudgetLayout.setOnClickListener(this);
		mCallsLayout.setOnClickListener(this);
		mFlowLayout.setOnClickListener(this);
		mUpdateLayout.setOnClickListener(this);
		mModifyLayout.setOnClickListener(this);
		mOnOffLayout.setOnClickListener(this);
		mShowDeskLayout.setOnClickListener(this);
		mShowBalanceLayout.setOnClickListener(this);
		mShowFlowLayout.setOnClickListener(this);
		mShowSpeedLayout.setOnClickListener(this);
		mNoticeLayout.setOnClickListener(this);
		mDisturbLayout.setOnClickListener(this);
		chargeOnlineRl.setOnClickListener(this);
		// mShareLayout.setOnClickListener(this);
		mFeedbackLayout.setOnClickListener(this);
		mAboutLayout.setOnClickListener(this);
		mExitLayout.setOnClickListener(this);

		// 7个需要实时更新的TextView
		mTelNOView = (TextView) findViewById(R.id.tel_no_tv);
		mBindingView = (TextView) findViewById(R.id.is_binding);
		mAddressView = (TextView) findViewById(R.id.tel_address_tv);
		mPackageView = (TextView) findViewById(R.id.tel_package_tv);
		mMoneyView = (TextView) findViewById(R.id.tel_money_tv);
		mUpdateView = (TextView) findViewById(R.id.tel_update_tv);
		mDisturbView = (TextView) findViewById(R.id.tel_disturb_tv);
		mTelNOView.setSelected(true);
		mAddressView.setSelected(true);
		mPackageView.setSelected(true);
		mUpdateView.setSelected(true);
		mDisturbView.setSelected(true);

		// 4个字体颜色需要改变的TextView
		mShowDeskView = (TextView) findViewById(R.id.show_desk_tv);
		mShowMoneyView = (TextView) findViewById(R.id.show_balance_tv);
		mShowFlowView = (TextView) findViewById(R.id.show_flow_tv);
		mShowSpeedView = (TextView) findViewById(R.id.show_speed_tv);
	}

	private void initShowTextView() {
		mPhoneNO = config.getTopEightNum();
		mTelNOView.setText(mPhoneNO.substring(0, 3) + "****"
				+ mPhoneNO.substring(7, mPhoneNO.length()));

		if (config.getPhoneBindingStatus() == 1) {
			mBindingView.setText("已绑定");
		} else {
			mBindingView.setText("未绑定");
		}

		mProvinceStr = config.getProvince();
		mCityStr = config.getCity();
		mOperatorStr = config.getOperator();
		mPackageStr = config.getPackageBrand();
		mAddressView.setText(mProvinceStr + "，" + mCityStr + "，" + mOperatorStr
				+ "，" + mPackageStr);

		mFreeCallTime = config.getFreeCallTime();
		mFreeMSG = config.getFreeMessages();
		mFreeFlow = (int) config.getFreeGprs();
		if (mFreeCallTime == 100000) {
			mFreeCallTimeStr = "通话未设置，";
		} else {
			mFreeCallTimeStr = "通话" + mFreeCallTime + "分钟，";
		}
		if (mFreeMSG == 100000) {
			mFreeMSGStr = "短信未设置，";
		} else {
			mFreeMSGStr = "短信" + mFreeMSG + "条，";
		}
		if (mFreeFlow == 100000) {
			mFreeFlowStr = "流量未设置";
		} else {
			mFreeFlowStr = "流量" + mFreeFlow + "MB";
		}
		mPackageView.setText(mFreeCallTimeStr + mFreeMSGStr + mFreeFlowStr);

		float money = config.getCallsBudget();

		if (money == 100000) {
			mMoneyView.setText("话费预算未设置");
		} else {
			mMoneyView.setText(CallStatUtils.changeFloat(money) + "元");
		}

		if (getLastCheckTime() != null) {
			mUpdateView.setText("上次更新：" + getLastCheckTime() + "前");
		} else {
			mUpdateView.setText("从未更新,点击获取最新对帐指令");
		}

		if (config.isAirmodeSwitch()) {
			mDisturbView.setText("已启用，在" + gettime(config.getAirmodeOpenTime())
					+ "～" + gettime(config.getAirmodeCloseTime())
					+ "期间，将不能正常使用电话等功能。");
		} else {
			mDisturbView.setText("已关闭");
		}
	}

	private void initImageView() {
		// 启动通知栏的图片
		switchSlider(true, config.getStatusKeepNotice(), noticeRL);
		// 悬浮窗图片
		showFloatWindow();
		// 专享服务
		inityanzheng();
		// 开机启动
		switchSlider(true, config.isServicesStartOnBootComplete(), backstageRL);

	}

	private void switchSlider(boolean isFloatOn, boolean isOn,
			RelativeLayout rlLayout) {
		ImageView leftImageView = (ImageView) rlLayout.getChildAt(0);
		ImageView rightImageView = (ImageView) rlLayout.getChildAt(1);
		if (isFloatOn) {
			if (isOn) {
				rlLayout.setBackgroundResource(R.drawable.yqy_setting);
				leftImageView.setVisibility(View.GONE);
				rightImageView.setVisibility(View.VISIBLE);
			} else {
				rlLayout.setBackgroundResource(R.drawable.wqy_setting);
				leftImageView.setVisibility(View.VISIBLE);
				rightImageView.setVisibility(View.GONE);
			}
		} else {
			rlLayout.setBackgroundResource(R.drawable.wqy_setting_h);
			leftImageView.setVisibility(View.VISIBLE);
			rightImageView.setVisibility(View.GONE);
		}

	}

	/** 更新悬浮窗相关图片及字体颜色 */
	private void showFloatWindow() {
		boolean isFloatOn = config.getFloatWindowOpen();
		if (isFloatOn) {
			switchSlider(true, isFloatOn, flowerImageRL);
			onlyShowDesk();
			floatShowWhich();

		} else {
			switchSlider(true, isFloatOn, flowerImageRL);
			switchSlider(isFloatOn, isFloatOn, showDeskRL);
			switchSlider(isFloatOn, isFloatOn, showMoneyRL);
			switchSlider(isFloatOn, isFloatOn, showSpeedRL);
			switchSlider(isFloatOn, isFloatOn, showFlowRL);

			mShowDeskView.setTextColor(getResources().getColor(
					R.color.setting_text_unclick));
			mShowMoneyView.setTextColor(getResources().getColor(
					R.color.setting_text_unclick));
			mShowFlowView.setTextColor(getResources().getColor(
					R.color.setting_text_unclick));
			mShowSpeedView.setTextColor(getResources().getColor(
					R.color.setting_text_unclick));
		}
	}

	private void floatShowWhich() {
		mShowDeskView.setTextColor(getResources().getColor(
				R.color.setting_text_black));
		mShowMoneyView.setTextColor(getResources().getColor(
				R.color.setting_text_black));
		mShowSpeedView.setTextColor(getResources().getColor(
				R.color.setting_text_black));
		boolean isSpeedOn;
		if (config.getFreeGprs() != 100000) {
			mShowFlowView.setTextColor(getResources().getColor(
					R.color.setting_text_black));
			isSpeedOn = true;
		} else {
			mShowFlowView.setTextColor(getResources().getColor(
					R.color.setting_text_unclick));
			isSpeedOn = false;
		}
		int temp = config.getFloatWindowIndex();
		switch (temp) {
		case 1:
			switchSlider(true, true, showMoneyRL);
			switchSlider(true, false, showSpeedRL);
			switchSlider(isSpeedOn, false, showFlowRL);
			break;
		case 2:
			if (config.getFreeGprs() != 100000) {
				switchSlider(true, true, showFlowRL);
				switchSlider(true, false, showMoneyRL);
			} else {
				switchSlider(true, true, showMoneyRL);
				switchSlider(false, false, showFlowRL);
				config.setFloatWindowIndex(1);
			}
			switchSlider(true, false, showSpeedRL);
			break;
		case 3:
			switchSlider(true, false, showMoneyRL);
			switchSlider(true, true, showSpeedRL);
			switchSlider(isSpeedOn, false, showFlowRL);
			break;
		default:
			break;
		}
	}

	private void onlyShowDesk() {
		switchSlider(config.getFloatWindowOpen(),
				config.getFloatShowDeskOnly(), showDeskRL);

	}

	private void openNotice() {
		if (config.getStatusKeepNotice()) {
			config.setStatusKeepNotice(false);
			switchSlider(true, false, noticeRL);
			if (binder != null) {
				binder.close_notification();
			}

		} else {
			config.setStatusKeepNotice(true);
			switchSlider(true, true, noticeRL);
			/* 打开通知栏 */
			if (binder != null) {
				binder.open_notification();
			}
		}
	}

	private void floatWindowOnClick() {
		if (config.getFloatWindowOpen()) {
			config.setFloatWindowOpen(false);
			startFloatWindow();
		} else {
			config.setFloatWindowOpen(true);
			startFloatWindow();
		}
		showFloatWindow();
	}

	private void showDeskOnlyOnClick() {
		if (config.getFloatWindowOpen()) {
			if (config.getFloatShowDeskOnly()) {
				config.setFloatShowDeskOnly(false);
				startFloatWindow();
			} else {
				config.setFloatShowDeskOnly(true);
				startFloatWindow();
			}
			showFloatWindow();
		}
	}

	private void startFloatWindow() {
		if (binder != null) {
			binder.showDeskOnly();
		}
	}

	private void setFloatData() {
		if (binder != null) {
			binder.open_floatWindow();
		}
	}

	private void floatShowMoney() {
		if (config.getFloatWindowOpen()) {
			config.setFloatWindowIndex(1);
			setFloatData();
			showFloatWindow();
		}
	}

	private void floatShowFlow() {
		if (config.getFloatWindowOpen()) {
			if (config.getFreeGprs() != 100000) {
				config.setFloatWindowIndex(2);
				showFloatWindow();
				setFloatData();
			}
		}
		if (config.getFloatWindowOpen() && config.getFreeGprs() == 100000) {
			toast("请先设置流量套餐");
		}
	}

	private void floatShowSpeed() {
		if (config.getFloatWindowOpen()) {
			config.setFloatWindowIndex(3);
			setFloatData();
			showFloatWindow();
			// Log.i("wanglei", "floatShowSpeed+++");
			startFloatWindow();
			// Log.i("wanglei", "floatShowSpeed---");
		}
	}

	@SuppressWarnings("rawtypes")
	private void toOtherActivity(Class class1) {
		Intent intent = new Intent();
		intent.setClass(SystemSettingNewActivity.this, class1);
		startActivityForResult(intent, 0);
		overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
	}

	private void exit() {
		CallStatUtils.stopServices(this, CallStatUtils.callStatServices);
		CallStatApplication app = (CallStatApplication) getApplication();
		if (app.activities.size() > 0) {
			for (Activity activity : app.activities) {
				activity.finish();
			}
		}
		finish();
		// android.os.Process.killProcess(android.os.Process.myPid());
	}

	private void checkAccountingCodeUpdate() {
		if (CallStatUtils.isNetworkAvailable(this)) {
			String url = getString(R.string.update_code_by_city_url);
			// String updateAllUrl =
			// "http://acall.archermind.com/ci/index.php/callstats/getKeyWordbyProvince";
			ILog.LogI(this.getClass(), url);
			if (accountingDatabaseUpdater == null) {
				accountingDatabaseUpdater = new AccountingDatabaseUpdater(
						getBaseContext(), url);
			}
			Map<String, String> keyValuePairs = new HashMap<String, String>();
			String province = config.getProvince();
			String city = config.getCity();
			String operator = config.getOperator();
			String brand = config.getPackageBrand();
			ILog.LogI(this.getClass(), province + " " + operator + " " + brand);
			keyValuePairs.put("province", province);
			keyValuePairs.put("city", city);
			keyValuePairs.put("operator", operator);
			keyValuePairs.put("brand", brand);
			HttpEntity entity = MyHttpPostHelper.buildUrlEncodedFormEntity(
					keyValuePairs, HTTP.UTF_8);
			accountingDatabaseUpdater
					.setManagerListener(accountingDatabaseUpdateListener);
			accountingDatabaseUpdater.startManager(entity);
			if (mPd == null || !mPd.isShowing()) {
				mPd = ProgressDialog.show(this, null, "正在请求更新，请稍候...");
				mPd.setCancelable(true);
			}

		} else {
			toast("当前网络不可用，请检查是否开启网络功能");
		}
	}

	private OnWebLoadListener<List<AccountingKeyWordsBean>> accountingDatabaseUpdateListener = new OnWebLoadListener<List<AccountingKeyWordsBean>>() {

		@Override
		public void OnStart() {

		}

		@Override
		public void OnCancel() {
			if (mPd != null && mPd.isShowing()) {
				mPd.dismiss();
			}
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
				if (mPd != null && mPd.isShowing()) {
					mPd.dismiss();
				}
				toast("当前网络不可用，请检查是否开启网络功能");
				break;
			case OnLoadFinishListener.ERROR_REQUEST_FAILED:
				break;
			case OnLoadFinishListener.ERROR_REQ_REFUSED:
				if (mPd != null && mPd.isShowing()) {
					mPd.dismiss();
				}
				toast("当前网络不可用，请检查是否被防火墙禁用了网络");
				break;
			default:
				break;
			}
		}

		@Override
		public void OnPaserComplete(List<AccountingKeyWordsBean> list) {
			if (mPd != null && mPd.isShowing()) {
				mPd.setMessage("数据请求完成，正在解析...");
			}
			if (list == null || list.size() == 0) {
				if (mPd != null && mPd.isShowing()) {
					mPd.dismiss();
				}
				toast("当前已是最新数据");
			} else {
				new UpdateReconciliationCodeThread().execute(list);
				// getCode();
			}
			long now = System.currentTimeMillis();
			config.setCodeUpdateTime(now);
			mUpdateView.setText("上次更新：刚刚");

		}
	};

	class UpdateReconciliationCodeThread extends AsyncTask<Object, Void, Void> {

		@Override
		protected void onCancelled() {
			super.onCancelled();
			if (mPd != null && mPd.isShowing()) {
				mPd.dismiss();
			}
			toast("数据更新被取消");
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if (mPd != null && mPd.isShowing()) {
				mPd.setMessage("正在保存数据到本地...");
			}
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			CallStatDatabase.getInstance(SystemSettingNewActivity.this)
					.initReconciliationInfo2ConfigXml(config.getProvince(),
							config.getCity(), config.getOperator(),
							config.getPackageBrand(), 0);
			if (mPd != null && mPd.isShowing()) {
				mPd.dismiss();
			}
			toast("数据更新成功");
		}

		@Override
		protected Void doInBackground(Object... params) {
			List<AccountingKeyWordsBean> list = (List<AccountingKeyWordsBean>) params[0];
			CallStatDatabase.getInstance(SystemSettingNewActivity.this)
					.updateReconciliationCodeList(list);
			return null;
		}

	}

	private void toast(String str) {
		try {
			if (CallStatUtils.isMyAppOnDesk(this)) {
				ToastFactory.getToast(SystemSettingNewActivity.this, str,
						Toast.LENGTH_LONG).show();
			}
		} catch (Exception e) {
			ILog.logException(this.getClass(), e);
		}
	}

	private void bindService() {
		Intent intent = new Intent(this, CallStatSMSService.class);
		if (!CallStatUtils.isServiceRunning(this,
				"com.archermind.callstat.service.CallStatSMSService")) {
			startService(intent);
		}
		bindService(intent, conn, Context.BIND_AUTO_CREATE);
	}

	private String getLastCheckTime() {
		long now = System.currentTimeMillis();
		long last = config.getCodeUpdateTime();
		// Log.i("x", "last ---------------------------= " + last);
		String lastCheckTime = null;
		if (last != 0) {
			int time = (int) ((now - last) / 60000);
			int day = time / 1440;
			int hour = time / 60;
			if (day > 0) {
				lastCheckTime = day + "天";
			} else {
				if (hour > 0) {
					lastCheckTime = (time / 60) + "小时";
				} else {
					if (time > 0) {
						lastCheckTime = time + "分钟";
					} else {
						if (now - last >= 0) {
							lastCheckTime = (int) ((now - last) / 1000) + "秒";
						} else {
							lastCheckTime = "0秒";
						}

					}

				}
			}
		}

		return lastCheckTime;

	}

	private void registerMyReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(FLOATWINDOW_BROADCAST_ACTION);
		filter.addAction(CallStatSMSService.ACTION_VERIFICATION_RESULT);
		filter.addAction(CallStatSMSService.SEND_BIND_HANDLE);
		filter.addAction(CallStatSMSService.PHONE_BINDIND_RESULT);
		filter.addAction(CallStatSMSService.PHONE_UNBINDIND_RESULT);
		// filter.setPriority(Integer.MAX_VALUE);
		registerReceiver(floatWindowReceiver, filter);
	}

	private void unRegisterMyReceiver() {
		unregisterReceiver(floatWindowReceiver);
	}

	private String gettime(int time) {
		String sTime;
		if (time % 100 >= 10) {
			if (time / 100 >= 10) {
				sTime = time / 100 + ":" + time % 100;
			} else {
				sTime = "0" + time / 100 + ":" + time % 100;
			}

		} else {
			if (time / 100 >= 10) {
				sTime = time / 100 + ":" + "0" + time % 100;
			} else {
				sTime = "0" + time / 100 + ":" + "0" + time % 100;
			}

		}
		return sTime;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (v.getId()) {
		case R.id.tel_lljz_rl:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mFlowLayoutImageView
						.setImageResource(R.drawable.icon_liuliang_pre);
				mFlowLayoutTextView.setTextColor(getResources().getColor(
						R.color.white));
				mFlowLayout.setBackgroundResource(R.drawable.set_biglist_blue);
				mTelLljzArrow.setImageResource(R.drawable.arrow_right_light);
				return false;

			case MotionEvent.ACTION_UP:
				mFlowLayoutImageView
						.setImageResource(R.drawable.icon_liuliang_nor);
				mFlowLayoutTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				mFlowLayout.setBackgroundResource(R.drawable.set_biglist_bar);
				mTelLljzArrow.setImageResource(R.drawable.arrow_right);
				return false;
			case MotionEvent.ACTION_CANCEL:
				mFlowLayoutImageView
						.setImageResource(R.drawable.icon_liuliang_nor);
				mFlowLayoutTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				mFlowLayout.setBackgroundResource(R.drawable.set_biglist_bar);
				mTelLljzArrow.setImageResource(R.drawable.arrow_right);
				return false;
			default:
				break;
			}

			break;
		case R.id.tel_no_rl:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mPhoneImageView.setImageResource(R.drawable.icon_haoma_pre);
				mPhoneTextView.setTextColor(getResources().getColor(
						R.color.white));
				mBindingView.setTextColor(getResources()
						.getColor(R.color.white));
				mTelNOView.setTextColor(getResources().getColor(R.color.white));
				mMyNOLayout.setBackgroundResource(R.drawable.set_biglist_blue);
				mTelNoArrow.setImageResource(R.drawable.arrow_right_light);
				return false;

			case MotionEvent.ACTION_UP:
				mPhoneImageView.setImageResource(R.drawable.icon_haoma_nor);
				mPhoneTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				mBindingView.setTextColor(getResources().getColor(
						R.color.setting_text));
				mTelNOView.setTextColor(getResources().getColor(
						R.color.setting_text));
				mMyNOLayout.setBackgroundResource(R.drawable.set_biglist_bar);
				mTelNoArrow.setImageResource(R.drawable.arrow_right);
				return false;

			case MotionEvent.ACTION_CANCEL:
				mPhoneImageView.setImageResource(R.drawable.icon_haoma_nor);
				mPhoneTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				mBindingView.setTextColor(getResources().getColor(
						R.color.setting_text));
				mTelNOView.setTextColor(getResources().getColor(
						R.color.setting_text));
				mMyNOLayout.setBackgroundResource(R.drawable.set_biglist_bar);
				mTelNoArrow.setImageResource(R.drawable.arrow_right);
				return false;
			default:
				break;
			}

			break;

		case R.id.tel_zhuanxiang_rl:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (config.getPhoneBindingStatus() == 0) {

				} else {
					zhuanxiangImageView
							.setImageResource(R.drawable.icon_zhuanxiang_pre);
					zhuanxiangTextView.setTextColor(getResources().getColor(
							R.color.white));
					yanzhengTextView.setTextColor(getResources().getColor(
							R.color.white));
					zhuanxiangRL
							.setBackgroundResource(R.drawable.set_biglist_blue);
				}

				return false;

			case MotionEvent.ACTION_UP:
				if (config.getPhoneBindingStatus() == 0) {

				} else {
					zhuanxiangImageView
							.setImageResource(R.drawable.icon_zhuanxiang_nor);
					zhuanxiangTextView.setTextColor(getResources().getColor(
							R.color.setting_text_black));
					yanzhengTextView.setTextColor(getResources().getColor(
							R.color.setting_text));
					zhuanxiangRL
							.setBackgroundResource(R.drawable.set_biglist_bar);
				}

				return false;

			case MotionEvent.ACTION_CANCEL:
				if (config.getPhoneBindingStatus() == 0) {

				} else {
					zhuanxiangImageView
							.setImageResource(R.drawable.icon_zhuanxiang_nor);
					zhuanxiangTextView.setTextColor(getResources().getColor(
							R.color.setting_text_black));
					yanzhengTextView.setTextColor(getResources().getColor(
							R.color.setting_text));
					zhuanxiangRL
							.setBackgroundResource(R.drawable.set_biglist_bar);
				}
				return false;
			default:
				break;
			}

			break;
		case R.id.tel_address_rl:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mAddressImageView.setImageResource(R.drawable.icon_yys_pre);
				mAddressTextView.setTextColor(getResources().getColor(
						R.color.white));
				mAddressView.setTextColor(getResources()
						.getColor(R.color.white));
				mAddressLayout
						.setBackgroundResource(R.drawable.set_biglist_blue);
				mTelAddressArrow.setImageResource(R.drawable.arrow_right_light);
				return false;

			case MotionEvent.ACTION_UP:
				mAddressImageView.setImageResource(R.drawable.icon_yys_nor);
				mAddressTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				mAddressView.setTextColor(getResources().getColor(
						R.color.setting_text));
				mAddressLayout
						.setBackgroundResource(R.drawable.set_biglist_bar);
				mTelAddressArrow.setImageResource(R.drawable.arrow_right);
				return false;

			case MotionEvent.ACTION_CANCEL:
				mAddressImageView.setImageResource(R.drawable.icon_yys_nor);
				mAddressTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				mAddressView.setTextColor(getResources().getColor(
						R.color.setting_text));
				mAddressLayout
						.setBackgroundResource(R.drawable.set_biglist_bar);
				mTelAddressArrow.setImageResource(R.drawable.arrow_right);
				return false;
			default:
				break;
			}

			break;
		case R.id.tel_package_rl:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mPackageImageView.setImageResource(R.drawable.icon_taocan_pre);
				mPackageTextView.setTextColor(getResources().getColor(
						R.color.white));
				mPackageView.setTextColor(getResources()
						.getColor(R.color.white));
				mPackageLayout
						.setBackgroundResource(R.drawable.set_biglist_blue);
				mTelPackageArrow.setImageResource(R.drawable.arrow_right_light);
				return false;

			case MotionEvent.ACTION_UP:
				mPackageImageView.setImageResource(R.drawable.icon_taocan_nor);
				mPackageTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				mPackageView.setTextColor(getResources().getColor(
						R.color.setting_text));
				mPackageLayout
						.setBackgroundResource(R.drawable.set_biglist_bar);
				mTelPackageArrow.setImageResource(R.drawable.arrow_right);
				return false;

			case MotionEvent.ACTION_CANCEL:
				mPackageImageView.setImageResource(R.drawable.icon_taocan_nor);
				mPackageTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				mPackageView.setTextColor(getResources().getColor(
						R.color.setting_text));
				mPackageLayout
						.setBackgroundResource(R.drawable.set_biglist_bar);
				mTelPackageArrow.setImageResource(R.drawable.arrow_right);
				return false;
			default:
				break;
			}

			break;

		case R.id.tel_money_rl:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mMoneyImageView.setImageResource(R.drawable.icon_yusuan_pre);
				mMoneyTextView.setTextColor(getResources().getColor(
						R.color.white));
				mMoneyView.setTextColor(getResources().getColor(R.color.white));
				mBudgetLayout
						.setBackgroundResource(R.drawable.set_biglist_blue);
				mTelMoneyArrow.setImageResource(R.drawable.arrow_right_light);
				return false;

			case MotionEvent.ACTION_UP:
				mMoneyImageView.setImageResource(R.drawable.icon_yusuan_nor);
				mMoneyTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				mMoneyView.setTextColor(getResources().getColor(
						R.color.setting_text));
				mBudgetLayout.setBackgroundResource(R.drawable.set_biglist_bar);
				mTelMoneyArrow.setImageResource(R.drawable.arrow_right);
				return false;

			case MotionEvent.ACTION_CANCEL:
				mMoneyImageView.setImageResource(R.drawable.icon_yusuan_nor);
				mMoneyTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				mMoneyView.setTextColor(getResources().getColor(
						R.color.setting_text));
				mBudgetLayout.setBackgroundResource(R.drawable.set_biglist_bar);
				mTelMoneyArrow.setImageResource(R.drawable.arrow_right);
				return false;
			default:
				break;
			}

			break;

		case R.id.tel_hfjz_rl:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mHFJZImageView.setImageResource(R.drawable.icon_huafei_pre);
				mHFJZTextView.setTextColor(getResources().getColor(
						R.color.white));
				mCallsLayout.setBackgroundResource(R.drawable.set_biglist_blue);
				mTelHfjzArrow.setImageResource(R.drawable.arrow_right_light);
				return false;

			case MotionEvent.ACTION_UP:
				mHFJZImageView.setImageResource(R.drawable.icon_huafei_nor);
				mHFJZTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				mCallsLayout.setBackgroundResource(R.drawable.set_biglist_bar);
				mTelHfjzArrow.setImageResource(R.drawable.arrow_right);
				return false;

			case MotionEvent.ACTION_CANCEL:
				mHFJZImageView.setImageResource(R.drawable.icon_huafei_nor);
				mHFJZTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				mCallsLayout.setBackgroundResource(R.drawable.set_biglist_bar);
				mTelHfjzArrow.setImageResource(R.drawable.arrow_right);
				return false;
			default:
				break;
			}

			break;

		case R.id.tel_update_rl:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mUpdateImageView.setImageResource(R.drawable.icon_lianwang_pre);
				mUpdateTextView.setTextColor(getResources().getColor(
						R.color.white));
				mUpdateView
						.setTextColor(getResources().getColor(R.color.white));
				mUpdateLayout
						.setBackgroundResource(R.drawable.set_biglist_blue);
				return false;

			case MotionEvent.ACTION_UP:
				mUpdateImageView.setImageResource(R.drawable.icon_lianwang_nor);
				mUpdateTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				mUpdateView.setTextColor(getResources().getColor(
						R.color.setting_text));
				mUpdateLayout.setBackgroundResource(R.drawable.set_biglist_bar);
				return false;

			case MotionEvent.ACTION_CANCEL:
				mUpdateImageView.setImageResource(R.drawable.icon_lianwang_nor);
				mUpdateTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				mUpdateView.setTextColor(getResources().getColor(
						R.color.setting_text));
				mUpdateLayout.setBackgroundResource(R.drawable.set_biglist_bar);
				return false;
			default:
				break;
			}

			break;

		case R.id.tel_modify_rl:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mModifyImageView.setImageResource(R.drawable.icon_shoudong_pre);
				mModifyTextView.setTextColor(getResources().getColor(
						R.color.white));
				mModifyLayout
						.setBackgroundResource(R.drawable.set_biglist_blue);
				mTelModifyArrow.setImageResource(R.drawable.arrow_right_light);
				return false;

			case MotionEvent.ACTION_UP:
				mModifyImageView.setImageResource(R.drawable.icon_shoudong_nor);
				mModifyTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				mModifyLayout.setBackgroundResource(R.drawable.set_biglist_bar);
				mTelModifyArrow.setImageResource(R.drawable.arrow_right);
				return false;

			case MotionEvent.ACTION_CANCEL:
				mModifyImageView.setImageResource(R.drawable.icon_shoudong_nor);
				mModifyTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				mModifyLayout.setBackgroundResource(R.drawable.set_biglist_bar);
				mTelModifyArrow.setImageResource(R.drawable.arrow_right);
				return false;
			default:
				break;
			}

			break;

		case R.id.tel_onoff_rl:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mOnOffImageView.setImageResource(R.drawable.icon_zhuomian_pre);
				mOnOffTextView.setTextColor(getResources().getColor(
						R.color.white));
				mOnOffLayout.setBackgroundResource(R.drawable.set_biglist_blue);
				return false;

			case MotionEvent.ACTION_UP:
				mOnOffImageView.setImageResource(R.drawable.icon_zhuomian_nor);
				mOnOffTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				mOnOffLayout.setBackgroundResource(R.drawable.set_biglist_bar);
				return false;

			case MotionEvent.ACTION_CANCEL:
				mOnOffImageView.setImageResource(R.drawable.icon_zhuomian_nor);
				mOnOffTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				mOnOffLayout.setBackgroundResource(R.drawable.set_biglist_bar);
				return false;
			default:
				break;
			}

			break;

		case R.id.show_desk_rl:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (config.getFloatWindowOpen()) {
					mShowDeskView.setTextColor(getResources().getColor(
							R.color.white));
					mShowDeskLayout
							.setBackgroundResource(R.drawable.set_biglist_blue);
				}

				return false;

			case MotionEvent.ACTION_UP:
				if (config.getFloatWindowOpen()) {
					mShowDeskView.setTextColor(getResources().getColor(
							R.color.setting_text_black));
					mShowDeskLayout
							.setBackgroundResource(R.drawable.set_biglist_bar);
				} else {
					// mShowDeskView.setTextColor(getResources().getColor(R.color.setting_text_unclick));
					// mShowDeskLayout.setBackgroundResource(R.drawable.set_biglist_bar);
				}

				return false;

			case MotionEvent.ACTION_CANCEL:
				if (config.getFloatWindowOpen()) {
					mShowDeskView.setTextColor(getResources().getColor(
							R.color.setting_text_black));
					mShowDeskLayout
							.setBackgroundResource(R.drawable.set_biglist_bar);
				} else {
					// mShowDeskView.setTextColor(getResources().getColor(R.color.setting_text_unclick));
					// mShowDeskLayout.setBackgroundResource(R.drawable.set_biglist_bar);
				}
				return false;
			default:
				break;
			}

			break;

		case R.id.show_balance_rl:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (config.getFloatWindowOpen()) {
					mShowMoneyView.setTextColor(getResources().getColor(
							R.color.white));
					mShowBalanceLayout
							.setBackgroundResource(R.drawable.set_biglist_blue);
				}

				return false;

			case MotionEvent.ACTION_UP:
				if (config.getFloatWindowOpen()) {
					mShowMoneyView.setTextColor(getResources().getColor(
							R.color.setting_text_black));
					mShowBalanceLayout
							.setBackgroundResource(R.drawable.set_biglist_bar);
				} else {
					// mShowMoneyView.setTextColor(getResources().getColor(R.color.setting_text_unclick));
					// mShowBalanceLayout.setBackgroundResource(R.drawable.set_biglist_bar);
				}

				return false;

			case MotionEvent.ACTION_CANCEL:
				if (config.getFloatWindowOpen()) {
					mShowMoneyView.setTextColor(getResources().getColor(
							R.color.setting_text_black));
					mShowBalanceLayout
							.setBackgroundResource(R.drawable.set_biglist_bar);
				} else {
					// mShowMoneyView.setTextColor(getResources().getColor(R.color.setting_text_unclick));
					// mShowBalanceLayout.setBackgroundResource(R.drawable.set_biglist_bar);
				}
				return false;
			default:
				break;
			}

			break;

		case R.id.show_flow_rl:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (config.getFreeGprs() != 100000) {
					if (config.getFloatWindowOpen()) {
						mShowFlowView.setTextColor(getResources().getColor(
								R.color.white));
						mShowFlowLayout
								.setBackgroundResource(R.drawable.set_biglist_blue);
					}

				}

				return false;

			case MotionEvent.ACTION_UP:
				if (config.getFreeGprs() != 100000) {
					if (config.getFloatWindowOpen()) {

						mShowFlowView.setTextColor(getResources().getColor(
								R.color.setting_text_black));
						mShowFlowLayout
								.setBackgroundResource(R.drawable.set_biglist_bar);
					} else {
						// mShowFlowView.setTextColor(getResources().getColor(R.color.setting_text_unclick));
						// mShowFlowLayout.setBackgroundResource(R.drawable.set_biglist_bar);
					}
				}

				return false;

			case MotionEvent.ACTION_CANCEL:
				if (config.getFreeGprs() != 100000) {
					if (config.getFloatWindowOpen()) {

						mShowFlowView.setTextColor(getResources().getColor(
								R.color.setting_text_black));
						mShowFlowLayout
								.setBackgroundResource(R.drawable.set_biglist_bar);
					} else {
						// mShowFlowView.setTextColor(getResources().getColor(R.color.setting_text_unclick));
						// mShowFlowLayout.setBackgroundResource(R.drawable.set_biglist_bar);
					}
				}
				return false;
			default:
				break;
			}

			break;
		case R.id.show_speed_rl:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (config.getFloatWindowOpen()) {
					mShowSpeedView.setTextColor(getResources().getColor(
							R.color.white));
					mShowSpeedLayout
							.setBackgroundResource(R.drawable.set_biglist_blue);
				}

				return false;

			case MotionEvent.ACTION_UP:
				if (config.getFloatWindowOpen()) {
					mShowSpeedView.setTextColor(getResources().getColor(
							R.color.setting_text_black));
					mShowSpeedLayout
							.setBackgroundResource(R.drawable.set_biglist_bar);
				} else {
					// mShowSpeedView.setTextColor(getResources().getColor(R.color.setting_text_unclick));
					// mShowSpeedLayout.setBackgroundResource(R.drawable.set_biglist_bar);
				}

				return false;

			case MotionEvent.ACTION_CANCEL:
				if (config.getFloatWindowOpen()) {
					mShowSpeedView.setTextColor(getResources().getColor(
							R.color.setting_text_black));
					mShowSpeedLayout
							.setBackgroundResource(R.drawable.set_biglist_bar);
				} else {
					// mShowSpeedView.setTextColor(getResources().getColor(R.color.setting_text_unclick));
					// mShowSpeedLayout.setBackgroundResource(R.drawable.set_biglist_bar);
				}
				return false;
			default:
				break;
			}

			break;

		case R.id.tel_notice_rl:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mNoticeImageView.setImageResource(R.drawable.icon_tongzhi_pre);
				mNoticeTextView.setTextColor(getResources().getColor(
						R.color.white));
				mNoticeLayout
						.setBackgroundResource(R.drawable.set_biglist_blue);
				return false;

			case MotionEvent.ACTION_UP:
				mNoticeImageView.setImageResource(R.drawable.icon_tongzhi_nor);
				mNoticeTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				mNoticeLayout.setBackgroundResource(R.drawable.set_biglist_bar);

				return false;

			case MotionEvent.ACTION_CANCEL:
				mNoticeImageView.setImageResource(R.drawable.icon_tongzhi_nor);
				mNoticeTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				mNoticeLayout.setBackgroundResource(R.drawable.set_biglist_bar);
				return false;
			default:
				break;
			}

			break;

		case R.id.tel_disturb_rl:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mDisturbImageView
						.setImageResource(R.drawable.icon_fangfushe_pre);
				mDisturbTextView.setTextColor(getResources().getColor(
						R.color.white));
				mDisturbView.setTextColor(getResources()
						.getColor(R.color.white));
				mDisturbLayout
						.setBackgroundResource(R.drawable.set_biglist_blue);
				mTelDisturbArrow.setImageResource(R.drawable.arrow_right_light);
				return false;

			case MotionEvent.ACTION_UP:
				mDisturbImageView
						.setImageResource(R.drawable.icon_fangfushe_nor);
				mDisturbTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				mDisturbLayout
						.setBackgroundResource(R.drawable.set_biglist_bar);
				mDisturbView.setTextColor(getResources().getColor(
						R.color.setting_text));
				mTelDisturbArrow.setImageResource(R.drawable.arrow_right);
				return false;

			case MotionEvent.ACTION_CANCEL:
				mDisturbImageView
						.setImageResource(R.drawable.icon_fangfushe_nor);
				mDisturbTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				mDisturbLayout
						.setBackgroundResource(R.drawable.set_biglist_bar);
				mDisturbView.setTextColor(getResources().getColor(
						R.color.setting_text));
				mTelDisturbArrow.setImageResource(R.drawable.arrow_right);
				return false;
			default:
				break;
			}

			break;

		case R.id.charge_online_rl:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mChargeIcon.setImageResource(R.drawable.icon_yusuan_pre);
				chargeOnlineTv.setTextColor(getResources().getColor(
						R.color.white));
				chargeDescTv.setTextColor(getResources()
						.getColor(R.color.white));
				chargeOnlineRl
						.setBackgroundResource(R.drawable.set_biglist_blue);
				mCharegeArrow.setImageResource(R.drawable.arrow_right_light);
				return false;

			case MotionEvent.ACTION_UP:
				mChargeIcon.setImageResource(R.drawable.icon_yusuan_nor);
				chargeOnlineTv.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				chargeOnlineRl
						.setBackgroundResource(R.drawable.set_biglist_bar);
				chargeDescTv.setTextColor(getResources().getColor(
						R.color.setting_text));
				mCharegeArrow.setImageResource(R.drawable.arrow_right);
				return false;

			case MotionEvent.ACTION_CANCEL:
				mChargeIcon.setImageResource(R.drawable.icon_yusuan_nor);
				chargeOnlineTv.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				chargeOnlineRl
						.setBackgroundResource(R.drawable.set_biglist_bar);
				chargeDescTv.setTextColor(getResources().getColor(
						R.color.setting_text));
				mCharegeArrow.setImageResource(R.drawable.arrow_right);
				return false;
			default:
				break;
			}

			break;
		case R.id.relativeLayout_fk:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mFKImageView.setImageResource(R.drawable.icon_fankui_pre);
				mFKTextView
						.setTextColor(getResources().getColor(R.color.white));
				mFeedbackLayout
						.setBackgroundResource(R.drawable.set_biglist_blue);
				mTelFkArrow.setImageResource(R.drawable.arrow_right_light);
				return false;

			case MotionEvent.ACTION_UP:
				mFKImageView.setImageResource(R.drawable.icon_fankui_nor);
				mFKTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				mFeedbackLayout
						.setBackgroundResource(R.drawable.set_biglist_bar);
				mTelFkArrow.setImageResource(R.drawable.arrow_right);
				return false;

			case MotionEvent.ACTION_CANCEL:
				mFKImageView.setImageResource(R.drawable.icon_fankui_nor);
				mFKTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				mFeedbackLayout
						.setBackgroundResource(R.drawable.set_biglist_bar);
				mTelFkArrow.setImageResource(R.drawable.arrow_right);
				return false;
			default:
				break;
			}

			break;

		case R.id.relativeLayout_about:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mAboutImageView.setImageResource(R.drawable.icon_guanyu_pre);
				mAboutTextView.setTextColor(getResources().getColor(
						R.color.white));
				mAboutLayout.setBackgroundResource(R.drawable.set_biglist_blue);
				mTelAboutArrow.setImageResource(R.drawable.arrow_right_light);
				return false;

			case MotionEvent.ACTION_UP:
				mAboutImageView.setImageResource(R.drawable.icon_guanyu_nor);
				mAboutTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				mAboutLayout.setBackgroundResource(R.drawable.set_biglist_bar);
				mTelAboutArrow.setImageResource(R.drawable.arrow_right);
				return false;

			case MotionEvent.ACTION_CANCEL:
				mAboutImageView.setImageResource(R.drawable.icon_guanyu_nor);
				mAboutTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				mAboutLayout.setBackgroundResource(R.drawable.set_biglist_bar);
				mTelAboutArrow.setImageResource(R.drawable.arrow_right);
				return false;
			default:
				break;
			}

			break;

		case R.id.relativeLayout_exit:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mBackImageView.setImageResource(R.drawable.icon_tuichu_pre);
				mBackTextView.setTextColor(getResources().getColor(
						R.color.white));
				mExitLayout.setBackgroundResource(R.drawable.set_biglist_blue);
				return false;

			case MotionEvent.ACTION_UP:
				mBackImageView.setImageResource(R.drawable.icon_tuichu_nor);
				mBackTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				mExitLayout.setBackgroundResource(R.drawable.set_biglist_bar);
				return false;

			case MotionEvent.ACTION_CANCEL:
				mBackImageView.setImageResource(R.drawable.icon_tuichu_nor);
				mBackTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				mExitLayout.setBackgroundResource(R.drawable.set_biglist_bar);
				return false;
			default:
				break;
			}

			break;

		case R.id.tel_houtai_rl:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				backstageImageView.setImageResource(R.drawable.icon_hotai_pre);
				backstageTextView.setTextColor(getResources().getColor(
						R.color.white));
				backstageLayout
						.setBackgroundResource(R.drawable.set_biglist_blue);
				return false;

			case MotionEvent.ACTION_UP:
				backstageImageView.setImageResource(R.drawable.icon_hotai_nor);
				backstageTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				backstageLayout
						.setBackgroundResource(R.drawable.set_biglist_bar);
				return false;

			case MotionEvent.ACTION_CANCEL:
				backstageImageView.setImageResource(R.drawable.icon_hotai_nor);
				backstageTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				backstageLayout
						.setBackgroundResource(R.drawable.set_biglist_bar);
				return false;
			default:
				break;
			}

			break;
		case R.id.back_rl:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				// mExitImageView.setImageResource(R.drawable.back_arrow_x);
				mBackLayout.setBackgroundResource(R.drawable.list_view_bg);
				return false;

			case MotionEvent.ACTION_UP:
				// mExitImageView.setImageResource(R.drawable.back_arrow);
				mBackLayout.setBackgroundResource(0);
				return false;

			case MotionEvent.ACTION_CANCEL:
				// mExitImageView.setImageResource(R.drawable.back_arrow);
				mBackLayout.setBackgroundResource(0);
				return false;
			default:
				break;
			}

			break;
		default:
			break;
		}
		return false;
	}

	private SmsVerifyManager svm;
	private OnWebLoadListener<SmsVerifyBean> smsVerifyListener = new OnWebLoadListener<SmsVerifyBean>() {

		@Override
		public void OnStart() {
			// Log.e("callstats", "SmsVerify OnStart");
		}

		@Override
		public void OnCancel() {

		}

		@Override
		public void OnLoadComplete(int statusCode) {
			switch (statusCode) {
			case OnLoadFinishListener.ERROR_BAD_URL:
				ILog.LogI(this.getClass(), "ERROR_BAD_URL");
				zhuanxiangTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				break;
			case OnLoadFinishListener.ERROR_REQUEST_FAILED:
				ILog.LogI(this.getClass(), "ERROR_REQUEST_FAILED");
				zhuanxiangTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				break;
			case OnLoadFinishListener.ERROR_TIMEOUT:
				// 请求超时
				// Log.e("callstats", "ERROR_TIMEOUT");
				overSend("请求超时，请检查网络稍后再试！");
				zhuanxiangTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
				break;
			case OnLoadFinishListener.OK:
				// Log.e("callstats", "OK");

				break;
			default:
				break;
			}
		}

		@Override
		public void OnPaserComplete(SmsVerifyBean svb) {
			if (svb != null) {

				switch (svb.getSmsID()) {
				case SmsVerifyBean.SMS_ID_CODE_INVALIDATE:
					// 无效的smsID
					ILog.LogI(this.getClass(), "SMS_ID_CODE_INVALIDATE");
					zhuanxiangTextView.setTextColor(getResources().getColor(
							R.color.setting_text_black));
					return;
				case SmsVerifyBean.SMS_ID_CODE_REPEAT:
					// 多次点击绑定
					overSend("验证过于频繁，请稍后再试");
					return;
				default:
					break;
				}
				CallStatApplication.token = svb.getSmsID();
				// Log.i("wanglei", "getSmsGateNum()" + svb.getSmsGateNum());
				config.setSmsVerifyNumber(svb.getSmsGateNum());
				// succSend();
			}
		}
	};

	private void smsVerify() {
		String url = getString(R.string.sms_verify_req_url);
		svm = new SmsVerifyManager(this, url, HTTP.UTF_8);
		// Log.e("callstats", "smsVerify url:" + url);
		Map<String, String> keyValuePairs = new HashMap<String, String>();
		keyValuePairs.put("appid", "2");
		keyValuePairs.put("Mobile", config.getTopEightNum());
		keyValuePairs.put("Action", "default");
		// Log.i("wanglei",
		// "config.getPhoneBindingStatus()"
		// + config.getPhoneBindingStatus());
		HttpEntity entity = MyHttpPostHelper
				.buildUrlEncodedFormEntity(keyValuePairs);
		svm.setManagerListener(smsVerifyListener);
		svm.startManager(entity);
	}

	private void zhuanxiangOnclick() {
		if (config.getPhoneBindingStatus() == -1) {
			if (CallStatUtils.isNetworkAvailable(this)) {
				config.setPhoneBindingStatus(0);
				// zhuanxiangOnoffLayout.setVisibility(View.GONE);
				// yanzhengTextView.setVisibility(View.VISIBLE);
				// yanzhengTextView.setText("发送请求中");
				zhuanxiangTextView.setTextColor(getResources().getColor(
						R.color.setting_text_unclick));
				binder.bindSendHandler();
				toast("后台努力申请中，需要1分钟左右，请关注申请结果通知！");
				smsVerify();
			} else {
				toast("当前网络不可用，请检查网络稍后再试！");
				zhuanxiangTextView.setTextColor(getResources().getColor(
						R.color.setting_text_black));
			}

		} else if (config.getPhoneBindingStatus() == 0) {
			toast("后台正在努力申请中！");

		} else if (config.getPhoneBindingStatus() == 1) {
			startActivity(new Intent(this, IsCancelVIPDialog.class));

		}
	}

	private void overSend(String str) {
		toast(str);
		config.setPhoneBindingStatus(-1);
		zhuanxiangOnoffLayout.setVisibility(View.VISIBLE);
		yanzhengTextView.setVisibility(View.GONE);
	}

	/*
	 * private void succSend() { yanzhengTextView.setText("正在验证中"); }
	 */
	// 监听返回键
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			finish();
			overridePendingTransition(R.anim.center_appear,
					R.anim.reduce_right_disappear);
			return false;
		}
		return false;
	}
}