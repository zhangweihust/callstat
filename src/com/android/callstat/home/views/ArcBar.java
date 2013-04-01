package com.android.callstat.home.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.widget.TextView;

import com.android.callstat.common.DeviceUtils;
import com.archermind.callstat.R;

public class ArcBar extends TextView {

	private Paint mArcPaint;
	private Paint mArcBGPaint;

	private RectF mOval;
	private float mSweep = 360;
	private float warnSweep = 0;
	private float degree = 0;
	private int mCenterX = 0;
	private int mCenterY = 0;
	private float mSpeedArcWidth;
	float redius;
	private float dencity;
	int[] normalColor;
	int[] normalColor1;
	int[] warnColor;
	int[] beyondColor;
	private boolean STOP_DRAW = false;

	public ArcBar(Context context) {
		super(context);
		init();
	}

	public ArcBar(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
		init();
	}

	public void setDegree(float degree) {
		this.degree = degree;
	}

	public float getDegree() {
		return degree;
	}

	public void setWarnDegree(float warndegree) {
		this.warnSweep = warndegree;
	}

	public float getWarnDegree() {
		return warnSweep;
	}

	public void refleshUI() {
		mSweep = 360;
		STOP_DRAW = false;
		postInvalidate();
	}

	private void init() {
		dencity = DeviceUtils.getDeviceDisplayDensity(getContext());
		mSpeedArcWidth = changeToDp(16.5f);
		mArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mArcPaint.setStyle(Paint.Style.STROKE);
		mArcPaint.setStrokeWidth(mSpeedArcWidth - changeToDp(2));
		// mArcPaint.setColor(getResources().getColor(R.color.progress_color));
		// mPaint.setStrokeCap(Paint.Cap.ROUND);
		/*
		 * 
		 * 
		 * BlurMaskFilter mBlur = new BlurMaskFilter(changeToDp(5),
		 * BlurMaskFilter.Blur.INNER); mArcPaint.setMaskFilter(mBlur);
		 */

		mArcBGPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mArcBGPaint.setStyle(Paint.Style.STROKE);
		mArcBGPaint.setStrokeWidth(changeToDp(3));
		mArcBGPaint
				.setColor(getResources().getColor(R.color.progress_bg_color));
		BlurMaskFilter mBGBlur = new BlurMaskFilter(changeToDp(1),
				BlurMaskFilter.Blur.INNER);
		mArcBGPaint.setMaskFilter(mBGBlur);

		normalColor = new int[] { Color.argb(255, 72, 146, 15),
				Color.argb(255, 124, 203, 32) /* , Color.argb(255, 72, 146, 15) */};
		normalColor1 = new int[] { Color.RED, Color.WHITE, Color.BLUE };
		warnColor = new int[] { Color.argb(255, 207, 93, 0),
				Color.argb(255, 252, 141, 0) };
		beyondColor = new int[] { Color.argb(255, 182, 30, 20),
				Color.argb(255, 253, 73, 49) };

		mArcPaint.setShader(new RadialGradient(changeToDp(173) / 2.0f,
				changeToDp(173) / 2.0f, changeToDp(173) / 2.0f - mSpeedArcWidth
						/ 2, normalColor, new float[] { 0.90f, 0.99f },
				Shader.TileMode.MIRROR));
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		mCenterX = w; // remember the center of the screen
		mCenterY = h;
		mOval = new RectF(mSpeedArcWidth / 2, mSpeedArcWidth / 2, mCenterX
				- mSpeedArcWidth / 2, mCenterY - mSpeedArcWidth / 2);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		// Log.i("xx", "onDraw test");
		drawSpeed(canvas);
		calcSpeed();
	}

	private void drawSpeed(Canvas canvas) {
		/* canvas.drawBitmap(bitmap, 0, 0, null); */
		// canvas.drawArc(mOval, 269, 361, false, mArcBGPaint);
		canvas.drawArc(mOval, 270, -mSweep, false, mArcPaint);
		canvas.drawLine(mCenterX / 2.0f, 0.2f, mCenterX / 2.0f, mSpeedArcWidth,
				mArcBGPaint);

	}

	private void calcSpeed() {
		// Log.i("xx", "mSweep = " + mSweep);
		if (mSweep > warnSweep) {
			mArcPaint.setShader(new RadialGradient(mCenterX / 2.0f,
					mCenterY / 2.0f, mCenterX / 2.0f - mSpeedArcWidth / 2,
					normalColor, new float[] { 0.90f, 0.99f },
					Shader.TileMode.MIRROR));
			// Log.i("xx", "mSweep =1 :" + mSweep);

		} else if (mSweep >= 0) {
			mArcPaint.setShader(new RadialGradient(mCenterX / 2.0f,
					mCenterY / 2.0f, mCenterX / 2.0f - mSpeedArcWidth / 2,
					warnColor, new float[] { 0.90f, 0.99f },
					Shader.TileMode.MIRROR));
			// Log.i("xx", "mSweep =2 :" + mSweep);

		} else {
			mArcPaint.setShader(new RadialGradient(mCenterX / 2.0f,
					mCenterY / 2.0f, mCenterX / 2.0f - mSpeedArcWidth / 2,
					beyondColor, new float[] { 0.90f, 0.99f },
					Shader.TileMode.MIRROR));
			// Log.i("xx", "mSweep =3 :" + mSweep);
		}

		if (mSweep >= getDegree()) {
			float a = (mSweep - getDegree()) / 30f;
			if (a <= 0.3f) {
				a = 0.3f;
			}
			mSweep -= a;

			postInvalidateDelayed(10);
		} else {
			if (!STOP_DRAW) {
				postInvalidateDelayed(10);
				STOP_DRAW = true;
			}
			mSweep = getDegree();
		}

	}

	private float changeToDp(float beforeChange) {
		float afterChange = beforeChange * dencity;
		return afterChange;
	}

	// 放大缩小图片
	public static Bitmap zoomBitmap(Bitmap bitmap, int w, int h) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		Matrix matrix = new Matrix();
		float scaleWidth = ((float) w / width);
		float scaleHeight = ((float) h / height);
		matrix.postScale(scaleWidth, scaleHeight);
		Bitmap newBmp = Bitmap.createBitmap(bitmap, 0, 0, width, height,
				matrix, true);
		return newBmp;
	}

}
