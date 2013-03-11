package com.archermind.callstat.home.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TabHost;

import com.archermind.callstat.R;

public class CustomTabHost extends TabHost {
	private Animation slideLeftIn;
	private Animation slideLeftOut;
	private Animation slideRightIn;
	private Animation slideRightOut;
	private boolean IS_FIRSTTIME = false;

	private int tabCount;//

	public CustomTabHost(Context context, AttributeSet attrs) {
		super(context, attrs);
		slideLeftIn = AnimationUtils.loadAnimation(context,
				R.anim.push_right_in);
		slideLeftOut = AnimationUtils.loadAnimation(context,
				R.anim.push_right_out);
		slideRightIn = AnimationUtils.loadAnimation(context,
				R.anim.push_left_in);
		slideRightOut = AnimationUtils.loadAnimation(context,
				R.anim.push_left_out);
	}

	public int getTabCount() {
		return tabCount;
	}

	public void setCount_True(boolean a) {
		IS_FIRSTTIME = a;
	}

	@Override
	public void addTab(TabSpec tabSpec) {
		tabCount++;
		super.addTab(tabSpec);
	}

	@Override
	public void setCurrentTab(int index) {
		// index为要切换到的tab页索引，currentTabIndex为现在要当前tab页的索引
		int currentTabIndex = getCurrentTab();

		// 设置当前tab页退出时的动画
		if (null != getCurrentView()) { // 第一次进入MainActivity时，getCurrentView()取得的值为空
			if (currentTabIndex == (tabCount - 1) && index == 0) {// 处理边界滑动
				getCurrentView().startAnimation(slideLeftOut);
			} else if (currentTabIndex == 0 && index == (tabCount - 1)) {// 处理边界滑动
				getCurrentView().startAnimation(slideRightOut);
			} else if (index > currentTabIndex) {// 非边界情况下从右往左fleep
				getCurrentView().startAnimation(slideLeftOut);
			} else if (index < currentTabIndex) {// 非边界情况下从左往右fleep
				getCurrentView().startAnimation(slideRightOut);
			}
		}

		super.setCurrentTab(index);

		// 设置即将显示的tab页的动画
		if (IS_FIRSTTIME) {
			if (currentTabIndex == (tabCount - 1) && index == 0) {// 处理边界滑动
				getCurrentView().startAnimation(slideLeftIn);
			} else if (currentTabIndex == 0 && index == (tabCount - 1)) {// 处理边界滑动
				getCurrentView().startAnimation(slideRightIn);
			} else if (index > currentTabIndex) {// 非边界情况下从右往左fleep
				getCurrentView().startAnimation(slideLeftIn);
			} else if (index < currentTabIndex) {// 非边界情况下从左往右fleep
				getCurrentView().startAnimation(slideRightIn);
			}
		}

	}
}
