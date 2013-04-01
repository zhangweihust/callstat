package com.android.callstat.common.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.archermind.callstat.R;

public class FloatBar extends LinearLayout {

	private BaseAdapter mAdapter;
	private MyHorizontalScrollView myHorizontalScrollView;
	private ImageView leftImgView;
	private ImageView rigthImgView;
	// private int mCurrentIndex = 0;
	private int mScreenWidth;

	/* package */static final int DEFAULT_SCREEN_WIDTH = 320;

	private int mChildItemWidth;

	private Drawable mSliderDrawable;
	private Drawable mLeftDrawable;
	private Drawable mRightDrawable;
	private OnFloatItemClickListener mClickListener;
	private int mTabCounts;
	private boolean mIsTabArrowEnable;

	private int fixedDiff;

	private View mTempView;

	private boolean isFirst = true;

	public interface OnFloatItemClickListener {
		public void OnItemClick(View view, int index, Object mT);
	}

	/**
	 * @param context
	 */
	public FloatBar(Context context) {
		this(context, null);
	}

	/**
	 * @param context
	 * @param attrs
	 */
	public FloatBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.FloatNaviBar);
		mLeftDrawable = a.getDrawable(R.styleable.FloatNaviBar_tabLeft);
		mRightDrawable = a.getDrawable(R.styleable.FloatNaviBar_tabRight);
		mSliderDrawable = a.getDrawable(R.styleable.FloatNaviBar_slider);
		mTabCounts = a.getInteger(R.styleable.FloatNaviBar_tabCount, 4);
		mIsTabArrowEnable = a.getBoolean(
				R.styleable.FloatNaviBar_tabArrowEnabled, true);
		a.recycle();
		setOrientation(HORIZONTAL);
		setGravity(Gravity.CENTER_VERTICAL);
		ensureParameters(context);
	}

	public void setAdapter(BaseAdapter adapter) {
		this.removeAllViews();
		isFirst = true;
		mAdapter = adapter;
		if (mAdapter.getCount() > mTabCounts) {
			mIsTabArrowEnable = true;
		} else {
			mTabCounts = mAdapter.getCount();
			mIsTabArrowEnable = false;
		}
		initFloatBar();
		bindUI();
	}

	/**
	 * Ensure the screen parameters.
	 * 
	 * @param context
	 *            The context of the application.
	 */
	private void ensureParameters(Context context) {
		final WindowManager manager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		if (manager != null) {
			final Display display = manager.getDefaultDisplay();
			mScreenWidth = display.getWidth();
		} else {
			mScreenWidth = DEFAULT_SCREEN_WIDTH;
		}
	}

	/**
	 * 
	 */
	private void initFloatBar() {
		LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		leftImgView = new ImageView(getContext());
		leftImgView.setPadding(0, 0, 2, 0);
		addView(leftImgView, imgParams);
		leftImgView.setVisibility(INVISIBLE);
		myHorizontalScrollView = new MyHorizontalScrollView(getContext());
		LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		scrollParams.weight = 1.0f;
		addView(myHorizontalScrollView, scrollParams);
		rigthImgView = new ImageView(getContext());
		rigthImgView.setPadding(2, 0, 0, 0);
		addView(rigthImgView, imgParams);

		if (!mIsTabArrowEnable) {
			leftImgView.setVisibility(GONE);
			rigthImgView.setVisibility(GONE);
		}

		leftImgView.setImageDrawable(mLeftDrawable);
		rigthImgView.setImageDrawable(mRightDrawable);
	}

	/**
	 * 
	 */
	private void bindUI() {
		int childCount = mAdapter.getCount();
		for (int i = 0; i < childCount; i++) {
			final View v = mAdapter.getView(i, null, null);
			myHorizontalScrollView.setMyHorizontalScrollView(v, i, childCount);
		}
	}

	/**
	 * display 5 item in the screen
	 * 
	 * @return params
	 */
	private android.widget.LinearLayout.LayoutParams divideChildWidth(int count) {
		if (count == mTabCounts || !mIsTabArrowEnable) {
			mChildItemWidth = mScreenWidth / mTabCounts;
			fixedDiff = mScreenWidth % mTabCounts;
		} else {
			mChildItemWidth = (mScreenWidth - mLeftDrawable.getIntrinsicWidth()
					- mRightDrawable.getIntrinsicWidth() - 4)
					/ mTabCounts;
			fixedDiff = (mScreenWidth - mLeftDrawable.getIntrinsicWidth()
					- mRightDrawable.getIntrinsicWidth() - 4)
					% mTabCounts;
		}
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				mChildItemWidth, LayoutParams.FILL_PARENT);
		params.gravity = Gravity.CENTER_VERTICAL;
		return params;
	}

	public void setOnFloatItemClickListener(
			OnFloatItemClickListener clickListener) {
		mClickListener = clickListener;
	}

	protected class MyHorizontalScrollView extends HorizontalScrollView {

		private LinearLayout mContentView;

		private Rect mNowRect;
		private Rect mEndRect;
		private boolean mSyn = false;
		public boolean mIsStop = false;
		private short SPEED;

		private myThread mThread;

		private static final short RUN_DURATION = 8;

		/**
		 * @param context
		 */
		public MyHorizontalScrollView(Context context) {
			this(context, null);
		}

		/**
		 * @param context
		 * @param attrs
		 */
		public MyHorizontalScrollView(Context context, AttributeSet attrs) {
			this(context, attrs, 0);
		}

		/**
		 * @param context
		 * @param attrs
		 * @param defStyle
		 */
		public MyHorizontalScrollView(Context context, AttributeSet attrs,
				int defStyle) {
			super(context, attrs, defStyle);
			init(context);
			mNowRect = new Rect();
			mEndRect = new Rect();
		}

		/**
		 * @param context
		 */
		private void init(Context context) {
			ensureContentView(context);
		}

		/**
		 * Ensure the content view.
		 * 
		 * @param context
		 *            The context of the application.
		 */
		private void ensureContentView(Context context) {
			if (null == mContentView) {
				final LinearLayout contentView = new LinearLayout(context);
				final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
						android.view.ViewGroup.LayoutParams.FILL_PARENT,
						android.view.ViewGroup.LayoutParams.FILL_PARENT);
				params.gravity = Gravity.CENTER_VERTICAL;
				super.addView(contentView, params);
				mContentView = contentView;
			}
		}

		public void setMyHorizontalScrollView(View view, int index, int count) {
			view.setOnClickListener(new ItemClick(index, mAdapter
					.getItem(index)));
			if (index == count - 1) {
				android.widget.LinearLayout.LayoutParams params = divideChildWidth(count);
				params.width += fixedDiff;
				mContentView.addView(view, params);
			} else {
				mContentView.addView(view, divideChildWidth(count));
			}
		}

		public void setCurrentIndex(int index) {
			// mCurrentIndex = index;
			// new ItemClick(index,mAdapter.getItem(index)).o
		}

		private class ItemClick implements View.OnClickListener {

			private int mIndex;
			private Object mT;

			/**
			 * @param index
			 * @param object
			 */
			public ItemClick(int index, Object object) {
				mIndex = index;
				mT = object;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see android.view.View.OnClickListener#onClick(android.view.View)
			 */
			@Override
			public void onClick(View v) {
				if (null != mTempView && mTempView.hashCode() != v.hashCode()) {
					mTempView.setSelected(false);
				}
				startMove(v);
				if (null != mClickListener) {
					mClickListener.OnItemClick(v, mIndex, mT);
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.widget.HorizontalScrollView#onLayout(boolean, int, int,
		 * int, int)
		 */
		@Override
		protected void onLayout(boolean changed, int l, int t, int r, int b) {
			super.onLayout(changed, l, t, r, b);
			// FIXME we initial the state only at the first time
			if (isFirst) {
				View view = ((ViewGroup) mContentView).getChildAt(0);
				view.getHitRect(mNowRect);
				view.setSelected(true);
				mTempView = view;
				isFirst = false;
			}
		}

		/**
		 * @param v
		 *            dowork
		 */
		public void doWork(final View v) {
			v.getHitRect(this.mEndRect);

			if (this.mNowRect.right < this.mEndRect.right) {
				SPEED = (short) ((mEndRect.right - mNowRect.right) / RUN_DURATION);
				work(new RunForword() {
					public void run() {
						mNowRect.left += SPEED;
						mNowRect.right += SPEED;

						if (mNowRect.right >= mEndRect.right)
							ReachRect(v);
					}
				});
			} else if (this.mNowRect.right > this.mEndRect.right) {
				SPEED = (short) ((mNowRect.right - mEndRect.right) / RUN_DURATION);
				work(new RunForword() {
					public void run() {
						mNowRect.left -= SPEED;
						mNowRect.right -= SPEED;

						if (mNowRect.right <= mEndRect.right)
							ReachRect(v);
					}
				});
			}
		}

		/**
		 * reach the left rect
		 */
		private void ReachRect(final View view) {
			mNowRect.left = mEndRect.left;
			mNowRect.right = mEndRect.right;
			Message msg = new Message();
			msg.obj = view;
			msg.what = 1;
			mViewHandler.sendMessage(msg);
			mIsStop = true;
		}

		Handler mViewHandler = new Handler() {

			/*
			 * (non-Javadoc)
			 * 
			 * @see android.os.Handler#handleMessage(android.os.Message)
			 */
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				if (msg.what == 1) {
					View view = (View) msg.obj;
					mTempView = view;
					view.setSelected(true);
				}
			}
		};

		private void work(RunForword run) {
			this.mIsStop = false;
			while (!this.mIsStop) {
				if (this.mSyn) {
					run.run();
					this.mSyn = false;
					this.postInvalidate();
				}
			}
		}

		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			mSliderDrawable.setBounds(mNowRect);
			mSliderDrawable.draw(canvas);
			this.mSyn = true;
		}

		class myThread extends Thread {
			private View mView;

			public myThread(View v) {
				this.mView = v;
			}

			public void run() {
				doWork(this.mView);
			}
		}

		protected void startMove(View v) {
			stopThread();
			mThread = new myThread(v);
			mThread.start();
		}

		private void stopThread() {
			if (mThread != null) {
				try {
					this.mIsStop = true;
					mThread.join();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.view.View#onScrollChanged(int, int, int, int)
		 */
		@Override
		protected void onScrollChanged(int l, int t, int oldl, int oldt) {
			View view = getChildAt(getChildCount() - 1);
			int diff = (view.getRight() - (getWidth() + getScrollX()));// Calculate
																		// the
																		// scroll
																		// diff
			if (mIsTabArrowEnable) {
				if (diff == 0) { // if diff is zero, then the bottom has been
									// reached
					rigthImgView.setVisibility(INVISIBLE);
				} else {
					rigthImgView.setVisibility(VISIBLE);
				}
				if (view.getLeft() == getScrollX()) {
					leftImgView.setVisibility(INVISIBLE);
				} else {
					leftImgView.setVisibility(VISIBLE);
				}
			}
			super.onScrollChanged(l, t, oldl, oldt);
		}

	}

	protected interface RunForword {
		void run();
	}
}
