package com.archermind.callstat.home.views;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

/**
 * this class is used to show tutorial images
 * 
 * @author longX
 * 
 */
public class ScrollLayout extends ViewGroup {

	private static final String TAG = "ScrollLayout";
	private static final int SNAP_VELOCITY = 600;
	private static final int TOUCH_STATE_REST = 0;
	private static final int TOUCH_STATE_SCROLLING = 1;

	private Context mContext;
	private int mCurScreen;
	private int mDefaultScreen = 0;
	private float mLastMotionX;
	private float mLastMotionY;
	private Scroller mScroller;
	private VelocityTracker mVelocityTracker;
	private int mTouchState = TOUCH_STATE_REST;
	private int mTouchSlop;

	private boolean snapStatus = true;
	private boolean touchStatus = false;
	private boolean mEndSnap = false;

	public interface OnScreenChangeListener {
		public void screenIndex(int index);
	}

	private OnScreenChangeListener mOnScreenChangeListener;

	/*****/

	public ScrollLayout(Context paramContext) {
		super(paramContext);
		Context localContext = (Context) new WeakReference(paramContext).get();
		mContext = localContext;
		Scroller localScroller = new Scroller(localContext);
		this.mScroller = localScroller;
		int i = this.mDefaultScreen;
		this.mCurScreen = i;
		int j = ViewConfiguration.get(getContext()).getScaledTouchSlop();
		this.mTouchSlop = j;
		Log.d(TAG, "ScrollLayout");
	}

	public ScrollLayout(Context paramContext, AttributeSet paramAttributeSet) {
		this(paramContext, paramAttributeSet, 0);
	}

	public ScrollLayout(Context paramContext, AttributeSet paramAttributeSet,
			int paramInt) {
		super(paramContext, paramAttributeSet, paramInt);
		Context localContext = (Context) new WeakReference(paramContext).get();
		mContext = localContext;
		Scroller localScroller = new Scroller(localContext);
		this.mScroller = localScroller;
		int i = this.mDefaultScreen;
		this.mCurScreen = i;
		int j = ViewConfiguration.get(getContext()).getScaledTouchSlop();
		this.mTouchSlop = j;
	}

	public void setScreenChangeListener(
			OnScreenChangeListener onScreenChangeListener) {
		mOnScreenChangeListener = onScreenChangeListener;
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// TODO Auto-generated method stub
		if (!changed)
			return;
		int childLeft = 0;
		final int childCount = this.getChildCount();
		for (int i = 0; i < childCount; i++) {
			final View childView = getChildAt(i);
			if (childView.getVisibility() != View.GONE) {
				final int childWidth = childView.getMeasuredWidth();
				childView.layout(childLeft, 0, childLeft + childWidth,
						childView.getMeasuredHeight());
				childLeft += childWidth;

			}
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		// /////////////////////////////////////////
		final int width = MeasureSpec.getSize(widthMeasureSpec);
		final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		if (widthMode != MeasureSpec.EXACTLY) {
			throw new IllegalStateException(
					"ScrollLayout only canmCurScreen run at EXACTLY mode!");
		}

		final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		if (heightMode != MeasureSpec.EXACTLY) {
			throw new IllegalStateException(
					"ScrollLayout only can run at EXACTLY mode!");
		}

		// The children are given the same width and height as the scrollLayout
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
		}
		scrollTo(mCurScreen * width, 0);
	}

	@Override
	public void computeScroll() {
		if (!this.mScroller.computeScrollOffset())
			return;
		int i = this.mScroller.getCurrX();
		int j = this.mScroller.getCurrY();
		scrollTo(i, j);
		postInvalidate();
		if (null != mOnScreenChangeListener) {
			mOnScreenChangeListener.screenIndex(mCurScreen);
		}
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		// TODO Auto-generated method stub
		final int action = ev.getAction();
		if ((action == MotionEvent.ACTION_MOVE)
				&& (mTouchState != TOUCH_STATE_REST)) {
			return true;
		}

		final float x = ev.getX();
		final float y = ev.getY();

		switch (action) {
		case MotionEvent.ACTION_MOVE:
			final int xDiff = (int) Math.abs(mLastMotionX - x);
			if (xDiff > mTouchSlop) {
				mTouchState = TOUCH_STATE_SCROLLING;
			}
			break;

		case MotionEvent.ACTION_DOWN:
			mLastMotionX = x;
			mLastMotionY = y;
			mTouchState = mScroller.isFinished() ? TOUCH_STATE_REST
					: TOUCH_STATE_SCROLLING;
			break;

		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			mTouchState = TOUCH_STATE_REST;
			break;
		}
		return mTouchState != TOUCH_STATE_REST;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub

		if (mVelocityTracker == null) {
			VelocityTracker localVelocityTracker1 = VelocityTracker.obtain();
			this.mVelocityTracker = localVelocityTracker1;
		}
		mVelocityTracker.addMovement(event);
		this.touchStatus = true;
		final int action = event.getAction();
		final float x = event.getX();
		final float y = event.getY();

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			if (!mScroller.isFinished()) {
				mScroller.abortAnimation();
			}
			mLastMotionX = x;
			break;

		case MotionEvent.ACTION_MOVE:
			int deltaX = (int) (mLastMotionX - x);
			mLastMotionX = x;
			scrollBy(deltaX, 0);
			break;

		case MotionEvent.ACTION_UP:
			final VelocityTracker velocityTracker = mVelocityTracker;
			velocityTracker.computeCurrentVelocity(1000);
			int velocityX = (int) velocityTracker.getXVelocity();

			if (velocityX > SNAP_VELOCITY && this.mCurScreen > 0) {
				// Fling enough to move left
				int i2 = this.mCurScreen - 1;
				snapToScreen(i2);
			} else if (velocityX < -SNAP_VELOCITY
					&& this.mCurScreen < getChildCount() - 1) {
				// Fling enough to move right
				int i3 = this.mCurScreen + 1;
				snapToScreen(i3);
			} else {
				snapToDestination();
			}
			if (mVelocityTracker != null) {
				mVelocityTracker.recycle();
				mVelocityTracker = null;
			}
			// }
			mTouchState = TOUCH_STATE_REST;
			break;
		case MotionEvent.ACTION_CANCEL:
			mTouchState = TOUCH_STATE_REST;
			break;
		}

		return true;
	}

	/**
	 * According to the position of current layout scroll to the destination
	 * page.
	 */
	public void snapToDestination() {
		final int screenWidth = getWidth();
		final int destScreen = (getScrollX() + screenWidth / 2) / screenWidth;
		snapToScreen(destScreen);

	}

	public void snapToScreen(int whichScreen) {
		int k = getChildCount() - 1;
		int l = getChildCount() - 1;
		int i1 = Math.min(whichScreen, l);
		whichScreen = Math.max(0, i1);
		if (!this.snapStatus)
			whichScreen = this.mCurScreen;

		int i4 = getWidth() * whichScreen;
		int i5 = getScrollX();
		int i6 = i4 - i5;
		Scroller localScroller = this.mScroller;
		int i8 = Math.abs(i6) * 2;

		localScroller.startScroll(i5, 0, i6, 0, i8);
		this.mCurScreen = whichScreen;
		invalidate();

	}

	/**
	 * @param paramInt
	 */
	public void setToScreen(int paramInt) {
		int i = getChildCount() - 1;
		int j = Math.min(paramInt, i);
		int k = Math.max(0, j);
		this.mCurScreen = paramInt;
		int l = getWidth() * paramInt;
		scrollTo(l, 0);
	}

	public void setTouchStatus(boolean paramBoolean) {
		this.touchStatus = paramBoolean;
	}

	public void setSnapStatus(boolean paramBoolean) {
		this.snapStatus = paramBoolean;
	}

	public int getCurScreen() {
		return this.mCurScreen;
	}

	public boolean getEndSnapStatus() {
		return this.mEndSnap;
	}

	public boolean getTouchStatus() {
		return this.touchStatus;
	}

}
