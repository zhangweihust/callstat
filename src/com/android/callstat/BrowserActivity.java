package com.android.callstat;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SlidingDrawer;

import com.android.callstat.common.CallStatUtils;
import com.android.callstat.common.StringUtil;
import com.archermind.callstat.R;


public class BrowserActivity extends Activity implements
		SlidingDrawer.OnDrawerOpenListener,
		SlidingDrawer.OnDrawerCloseListener, ImageButton.OnClickListener {
	/** Called when the activity is first created. */
	private static final String OPENWABKEY = "WAPURL";
	private static final String OPENWABTITLE = "WAPTITLE";
	private RelativeLayout back;
	private WebView mWebView;
	private SlidingDrawer mSlidingDrawer;
	private ImageView mHandle;
	private ImageButton mBtnHome;
	private ImageButton mBtnRefersh;
	private ImageButton mBtnCancel;
	private ImageButton mBtnBack;
	private ImageButton mBtnPrevious;
	private ProgressBar mHorizontalProgress;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Intent intent = getIntent();
		String url = intent.getStringExtra(OPENWABKEY);
		//String title = intent.getStringExtra(OPENWABTITLE);
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
		setContentView(R.layout.browser);
		
		back = (RelativeLayout) findViewById(R.id.charge_back_rl);
		mWebView = (WebView) findViewById(R.id.webview);
		mSlidingDrawer = (SlidingDrawer) findViewById(R.id.drawer);
		mHandle = (ImageView) findViewById(R.id.handle);
		mBtnHome = (ImageButton) findViewById(R.id.btn_home);
		mBtnRefersh = (ImageButton) findViewById(R.id.btn_refersh);
		mBtnCancel = (ImageButton) findViewById(R.id.btn_cancel);
		mBtnBack = (ImageButton) findViewById(R.id.btn_back);
		mBtnPrevious = (ImageButton) findViewById(R.id.btn_previous);
		mHorizontalProgress = (ProgressBar) findViewById(R.id.progress_horizontal);

		mSlidingDrawer.open();
		onDrawerOpened();
		back.setOnClickListener(this);
		mSlidingDrawer.setOnDrawerOpenListener(this);
		mSlidingDrawer.setOnDrawerCloseListener(this);
		mBtnHome.setOnClickListener(this);
		mBtnRefersh.setOnClickListener(this);
		mBtnCancel.setOnClickListener(this);
		mBtnBack.setOnClickListener(this);
		mBtnPrevious.setOnClickListener(this);
		setWebView(mWebView);
		if(url != null && url != "") {
			mWebView.loadUrl(url);
		}
	}

	private void setWebView(WebView webview) {
		WebSettings webSettings = webview.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
		webSettings.setSupportZoom(true);
		webSettings.setUseWideViewPort(true);
		webSettings.setUserAgentString(null);
		if (!CallStatUtils.isOMS()) {
			setAPN();
		}
		mWebView.setWebViewClient(new WebViewClient() {
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				view.loadUrl(url);
				return true;
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				mHorizontalProgress.setVisibility(View.GONE);
			}

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				super.onPageStarted(view, url, favicon);
				mHorizontalProgress.setVisibility(View.VISIBLE);
			}
		});

		mWebView.setWebChromeClient(new WebChromeClient() {
			@Override
			public void onProgressChanged(WebView view, int newProgress) {
				mHorizontalProgress.setProgress(newProgress);
				super.onProgressChanged(view, newProgress);
			}
		});
		mWebView.setInitialScale(1);
	}
	
	/**
	 * 
	 */
	private void setAPN() {
		try {
			if (CallStatUtils.isMOBILE(this)) {
				String apn = CallStatUtils.getAPN(this);
				if (!StringUtil.isNullOrWhitespaces(apn)) {
					if (apn.equals("CMWAP")) {
						WebView.enablePlatformNotifications();
						mWebView.setHttpAuthUsernamePassword("10.0.0.172","","","");
					}
				}
			}
		} catch (Exception e) {
		}
	}
	

	@Override
	public void onDrawerOpened() {
		mHandle.getDrawable().setLevel(1);
		ILog.LogI(getClass(), "onDrawerOpened");
	}

	@Override
	public void onDrawerClosed() {
		mHandle.getDrawable().setLevel(0);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.charge_back_rl:
			finish();
			overridePendingTransition(R.anim.push_right_in,
					R.anim.push_right_out);
			break;
		case R.id.btn_home:
			finish();
			overridePendingTransition(R.anim.push_right_in,
					R.anim.push_right_out);
			break;
		case R.id.btn_refersh:
			mWebView.reload();
			break;
		case R.id.btn_cancel:
			mWebView.stopLoading();
			break;
		case R.id.btn_back:
			if (mWebView.canGoBack()) {
				mWebView.goBack();
			}
			break;
		case R.id.btn_previous:
			if (mWebView.canGoForward()) {
				mWebView.goForward();
			}
			break;
		}
	}
}