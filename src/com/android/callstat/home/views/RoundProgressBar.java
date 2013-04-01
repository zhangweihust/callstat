package com.android.callstat.home.views;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.TextView;

import com.android.callstat.common.DeviceUtils;
import com.archermind.callstat.R;

/**
 * 该类继承于TextView而不是View其实是为了在配置文件里指定背景图能够自适应大小，
 * 继承与View则需要自己去实现，其实也不难，重写OnMeasure方法，
 * 在该方法里调用setMeasuredDimension重新设置视图大小为背景图大小就可以了
 */
public class RoundProgressBar extends TextView {

	private Paint mFramePaint;

	// --------------------
	private Paint mRoundPaints; // 主进度条画笔
	private RectF mRoundOval; // 矩形区域
	private RectF bgRoundOval;
	private RectF bgRoundOval2;
	private float mPaintWidth; // 画笔宽度
	private int mPaintColor; // 画笔颜色

	private Paint bgPaint; // 黑色的线条轮廓

	private int mStartProgress; // 进度条起始位置
	private int mCurProgress; // 主进度条当前位置
	private int mMaxProgress; // 进度条最终位置

	private boolean mBRoundPaintsFill; // 是否填充区域
	// ---------------------
	private int mSidePaintInterval; // 圆环向里缩进的距离

	private Paint mSecondaryPaint; // 辅助进度条画笔

	private int mSecondaryCurProgress; // 辅助进度条当前位置

	private Paint mBottomPaint; // 进度条背景图画笔

	private boolean mBShowBottom; // 是否显示进度条背景色

	// ----------------------
	private Handler mHandler;

	private boolean mBCartoom; // 是否正在作动画

	private Timer mTimer; // 用于作动画的TIMER

	private MyTimerTask mTimerTask; // 动画任务

	private int mSaveMax; // 在作动画时会临时改变MAX值，该变量用于保存值以便恢复

	private int mTimerInterval; // 定时器触发间隔时间(ms)

	private float mCurFloatProcess; // 作动画时当前进度值

	private float mProcessRInterval; // 作动画时每次增加的进度值

	private final static int TIMER_ID = 0x100; // 定时器ID

	private int redius;

	// 构造方法
	public RoundProgressBar(Context context) {
		super(context);

		initParam();
	}

	public RoundProgressBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		float px = DeviceUtils.getDeviceDisplayDensity(getContext()); // 获取手机分辨率
		initParam();

		TypedArray array = context.obtainStyledAttributes(attrs,
				R.styleable.RoundProgressBar);

		mMaxProgress = array.getInt(R.styleable.RoundProgressBar_max, 100);
		mSaveMax = mMaxProgress;
		mBRoundPaintsFill = array.getBoolean(R.styleable.RoundProgressBar_fill,
				true); // 获得是否是填充模式
		if (mBRoundPaintsFill == false) {
			mRoundPaints.setStyle(Paint.Style.STROKE);
			mSecondaryPaint.setStyle(Paint.Style.STROKE);
			mBottomPaint.setStyle(Paint.Style.STROKE);

			bgPaint.setStyle(Paint.Style.STROKE);
		}

		mSidePaintInterval = array.getInt(
				R.styleable.RoundProgressBar_Inside_Interval, 0);// 圆环缩进距离

		// 进度条背景显示
		mBShowBottom = array.getBoolean(
				R.styleable.RoundProgressBar_Show_Bottom, true);

		mPaintWidth = (array.getInt(R.styleable.RoundProgressBar_Paint_Width,
				10)) * px;
		if (mBRoundPaintsFill) // 填充模式则画笔长度改为0
		{
			mPaintWidth = 0;
		}

		mRoundPaints.setStrokeWidth(mPaintWidth);
		bgPaint.setStrokeWidth(1);
		mSecondaryPaint.setStrokeWidth(mPaintWidth);
		mBottomPaint.setStrokeWidth(mPaintWidth);

		mPaintColor = array.getColor(R.styleable.RoundProgressBar_Paint_Color,
				Color.WHITE);
		mRoundPaints.setColor(mPaintColor);
		bgPaint.setColor(R.color.darkgray);
		int color = mPaintColor & 0x00ffffff | 0x66000000;
		mSecondaryPaint.setColor(color);

		array.recycle(); // 一定要调用，否则会有问题

	}

	public void backgroundColor(int color) {
		mBottomPaint.setColor(color);
	}

	public void progressColor(int color) {
		mRoundPaints.setColor(color);
	}

	private void initParam() {
		mFramePaint = new Paint();
		mFramePaint.setAntiAlias(true); // 抗锯齿，如果没有调用这个方法，写上去的字不饱满，不美观，看地不太清楚
		mFramePaint.setStyle(Paint.Style.STROKE); // 画空心圆
		mFramePaint.setStrokeWidth(0); // 画笔的粗细

		mPaintWidth = 0;
		mPaintColor = 0xffffcc00;

		// 主进度画笔
		mRoundPaints = new Paint();
		mRoundPaints.setAntiAlias(true);
		mRoundPaints.setStyle(Paint.Style.FILL);
		mRoundPaints.setStrokeWidth(mPaintWidth);
		mRoundPaints.setColor(mPaintColor);

		bgPaint = new Paint();
		bgPaint.setAntiAlias(true);
		bgPaint.setStyle(Paint.Style.FILL);
		bgPaint.setStrokeWidth(mPaintWidth);
		bgPaint.setColor(R.color.darkgray);

		// 辅助进度条
		mSecondaryPaint = new Paint();
		mSecondaryPaint.setAntiAlias(true);
		mSecondaryPaint.setStyle(Paint.Style.FILL);
		mSecondaryPaint.setStrokeWidth(mPaintWidth);

		int color = mPaintColor & 0x00ffffff | 0x66000000;
		mSecondaryPaint.setColor(color);

		// 进度条背景图画笔
		mBottomPaint = new Paint();
		mBottomPaint.setAntiAlias(true);
		mBottomPaint.setStyle(Paint.Style.FILL);
		mBottomPaint.setStrokeWidth(mPaintWidth);
		mBottomPaint.setColor(Color.GRAY); // 灰色

		mStartProgress = -180; // 主进度条起始位置
		mCurProgress = 0; // 主进度条当前位置
		mMaxProgress = 100; // 主进度条最终位置
		mSaveMax = 100; // 在作动画时会临时改变MAX值，该变量用于保存值以便恢复

		mBRoundPaintsFill = true; // 填充区域
		mBShowBottom = true; // 显示进度条背景颜色

		mSidePaintInterval = 0; // 圆环向里缩进距离

		mSecondaryCurProgress = 0; // 辅助进度条当前位置

		mRoundOval = new RectF(0, 0, 0, 0); // 矩形区域
		bgRoundOval = new RectF(0, 0, 0, 0); // 矩形区域
		bgRoundOval2 = new RectF(0, 0, 0, 0); // 矩形区域

		mTimerInterval = 25; // 定时器触发间隔时间(ms)

		mCurFloatProcess = 0; // 作动画时当前进度值

		mProcessRInterval = 0; // 作动画时每次增加的进度值

		mBCartoom = false; // 是否在作动画

		mHandler = new Handler() {
			public void handleMessage(Message msg) {
				// TODO Auto-generated method stub
				if (msg.what == TIMER_ID) {
					// long now = System.currentTimeMillis();
					// if (mCurTime != 0)
					// {
					// Log.i("", "interval time = " + (now - mCurTime));
					// }
					//
					// mCurTime = now;

					if (mBCartoom == false) {
						return;
					}

					mCurFloatProcess += mProcessRInterval;
					setProgress((int) mCurFloatProcess);

					if (mCurFloatProcess > mMaxProgress) {
						mBCartoom = false;
						mMaxProgress = mSaveMax;
						// 作动画完成后 停止任务
						if (mTimerTask != null) {
							mTimerTask.cancel();
							mTimerTask = null;
						}
					}

				}
			}

		};

		mTimer = new Timer();

	}

	// synchronized 方法加锁
	public synchronized void setProgress(int progress) {
		mCurProgress = progress;
		if (mCurProgress < 0) {
			mCurProgress = 0;
		}

		if (mCurProgress > mMaxProgress) {
			mCurProgress = mMaxProgress;
		}

		invalidate(); //
	}

	public synchronized int getProgress() {
		return mCurProgress;
	}

	public synchronized void setSecondaryProgress(int progress) {
		mSecondaryCurProgress = progress;
		if (mSecondaryCurProgress < 0) {
			mSecondaryCurProgress = 0;
		}

		if (mSecondaryCurProgress > mMaxProgress) {
			mSecondaryCurProgress = mMaxProgress;
		}

		invalidate();
	}

	public synchronized int getSecondaryProgress() {
		return mSecondaryCurProgress;
	}

	public synchronized void setMax(int max) {
		if (max <= 0) {
			return;
		}

		mMaxProgress = max;
		if (mCurProgress > max) {
			mCurProgress = max;
		}

		if (mSecondaryCurProgress > max) {
			mSecondaryCurProgress = max;
		}

		mSaveMax = mMaxProgress;

		invalidate();
	}

	public synchronized int getMax() {
		return mMaxProgress;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		// TODO Auto-generated method stub
		super.onSizeChanged(w, h, oldw, oldh);
		redius = w / 2;
		// Log.i("", "W = " + w + ", H = " + h);

		if (mSidePaintInterval != 0) {
			mRoundOval.set(mPaintWidth / 2 + mSidePaintInterval, mPaintWidth
					/ 2 + mSidePaintInterval, w - mPaintWidth / 2
					- mSidePaintInterval, h - mPaintWidth / 2
					- mSidePaintInterval);
		} else {

			int sl = getPaddingLeft();
			int sr = getPaddingRight();
			int st = getPaddingTop();
			int sb = getPaddingBottom();

			/**
			 * 第一个参数：左边（最左侧，和矩形相切的左边的点，下面的相类似）时画笔中心的X坐标 第二个参数：上面时画笔中心的Y坐标
			 */
			mRoundOval.set(sl + mPaintWidth / 2 + mSidePaintInterval, st
					+ mPaintWidth / 2, w - mPaintWidth / 2 + sr, w
					- mPaintWidth + sb);

			bgRoundOval.set(sl + 1, st + 1, w - 1 + sr, w - 1 + sr);
			bgRoundOval2.set(sl + mPaintWidth - 1, st + mPaintWidth - 1, w + sr
					- mPaintWidth + 1, w + sr - mPaintWidth + 1);
		}

	}

	public synchronized void startCartoom(int time) {
		if (time <= 0 || mBCartoom == true) {
			return;
		}
		mBCartoom = true; // 开始画

		if (mTimerTask != null) {
			mTimerTask.cancel();
			mTimerTask = null;
		}

		setProgress(0);
		setSecondaryProgress(0);

		mSaveMax = mMaxProgress;
		mMaxProgress = (1000 / mTimerInterval) * time;

		mProcessRInterval = (float) mTimerInterval * mMaxProgress
				/ (time * 1000);
		mCurFloatProcess = 0;
		mTimerTask = new MyTimerTask();
		mTimer.schedule(mTimerTask, mTimerInterval, mTimerInterval);

	}

	public synchronized void stopCartoom() {

		mBCartoom = false;
		mMaxProgress = mSaveMax;

		setProgress(0);
		if (mTimerTask != null) {
			mTimerTask.cancel();
			mTimerTask = null;
		}
	}

	public void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.onDraw(canvas);
		// mask = zoomBitmap(
		// BitmapFactory.decodeResource(getResources(),
		// R.drawable.roundpbar_bg),
		// 380, 200);
		// maskPaint = new Paint();
		if (mBShowBottom) {
			// 进度条背景的描画
			// 第一个参数所矩形对象，第二个参数是描画开始的角度，第三个是跨越的角度
			canvas.drawArc(mRoundOval, 180, 180, mBRoundPaintsFill,
					mBottomPaint);
		}

		float secondRate = (float) mSecondaryCurProgress / mMaxProgress;
		float secondSweep = 180 * secondRate;
		canvas.drawArc(mRoundOval, mStartProgress, secondSweep,
				mBRoundPaintsFill, mSecondaryPaint);

		float rate = (float) mCurProgress / mMaxProgress;
		float sweep = 180 * rate;
		canvas.drawArc(mRoundOval, mStartProgress, sweep, mBRoundPaintsFill,
				mRoundPaints);

		// 画 两条 弧线
		canvas.drawArc(bgRoundOval, mStartProgress + sweep, 180 - sweep, false,
				bgPaint);
		canvas.drawArc(bgRoundOval2, mStartProgress + sweep, 180 - sweep,
				false, bgPaint);
		// 画两条直线

		float startX = (float) (redius - redius
				* Math.cos(Math.toRadians(sweep)));
		float startY = (float) (redius - redius
				* Math.sin(Math.toRadians(sweep)));
		float stopX = (float) ((redius - mPaintWidth) * Math.cos(Math
				.toRadians(sweep)));
		float stopY = (float) ((redius - mPaintWidth) * Math.sin(Math
				.toRadians(sweep)));
		// Log.i("i", "sweep=" + sweep);
		// Log.i("i", "Math.cos(45)=" + Math.cos(Math.toRadians(sweep)));
		// Log.i("i", "Math.sin(45)=" + Math.sin(Math.toRadians(sweep)));
		// Log.i("i", "redius=" + redius);
		// Log.i("i", "mPaintWidth=" + mPaintWidth);
		// Log.i("i", "startX=" + startX);
		// Log.i("i", "startY=" + startY);
		canvas.drawLine(startX + 0.5f, startY, redius - stopX + 0.5f, redius
				- stopY - 1, bgPaint);
		canvas.drawLine(2 * redius, redius, 2 * redius - mPaintWidth + 1,
				redius, bgPaint);

	}

	class MyTimerTask extends TimerTask {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			Message msg = mHandler.obtainMessage(TIMER_ID);
			msg.sendToTarget();

		}

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
