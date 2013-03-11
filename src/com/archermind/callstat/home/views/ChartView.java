package com.archermind.callstat.home.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Shader.TileMode;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;

import com.archermind.callstat.ILog;
import com.archermind.callstat.R;
import com.archermind.callstat.common.CallStatUtils;
import com.archermind.callstat.common.DeviceUtils;

public class ChartView extends TextView {
	private float[] data_power;
	private int margintop;
	private int marginbottom;
	private int first_x;// 第一个坐标距离左边的距离（dp）
	private int interval; // 两个坐标之间的间隔
	private int height;
	private int width;

	private int textSize;// 字体的大小
	private int textColor = Color.RED; // 字体的颜色
	private int lineColor = Color.YELLOW; // 折线的颜色
	private int pointColor = getResources().getColor(R.color.progress_color)/*
																			 * Color
																			 * .
																			 * RED
																			 */; // 折点颜色
	private int consumeType;// 这个变量保存了ChartView的type，type=0 话费，type=1 流量
	private Paint paint;
	private TextPaint textPaint; // 文字的画笔
	private Paint brokenLinePaint;// 折线画笔
	private Paint pointPaint; // 折点画笔
	private float dencity;

	private DrawHandler drawHandler = new DrawHandler();
	private float[][] highPoints = null;
	private int counter_index = 0;
	private int max_point_index = 0;
	private float max_point_value = -10000f;

	private int emphasize_index = -1;
	private int totaldays_of_month = 30; // 保存这个月有几天的成员变量

	private long max_y_axis_value;// 纵坐标的最大值（根据传进来的值向上取整，使得坐标轴上不会出现小数）

	private OnCoordinateChanged mOnCoordinateChanged;

	// 设置这个月有几天，便于在格线图上根据月份天数的不同而调整
	public void setDaysOfMonth(int TotalDays) {
		totaldays_of_month = TotalDays;
	}

	public void setConsumeType(int type) {
		consumeType = type;
	}

	public int getCurrentDayOfMonth() {
		return data_power.length;
	}

	public float getCurrentDay_consume() {
		return data_power[data_power.length - 1];
	}

	public void refreshUi() {
		postInvalidate();
	}

	public void setCounterIndex(int index) {
		counter_index = index;
	}

	public void maxPoint_and_index(float[][] point, int length) {
		max_point_value = -10000f;
		for (int i = 0; i < length; i++) {
			if (point[i][1] > max_point_value) {
				max_point_value = point[i][1];
				max_point_index = i;
			}
		}
	}

	public void setData(float[] data_power) {
		this.data_power = data_power;
		for (int i = 0; i < this.data_power.length; i++) {
			this.data_power[i] = this.data_power[i] >= 0 ? this.data_power[i]
					: 0;
		}
	}

	public ChartView(Context context) {
		super(context);
		init();
	}

	public ChartView(Context context, AttributeSet attrs) {
		super(context, attrs);
		Log.i("xx", "super(context, attrs)");
		init();
	}

	private void init() {
		dencity = DeviceUtils.getDeviceDisplayDensity(getContext());
		first_x = changeToDp(20);

		textSize = 10;

		margintop = changeToDp(10);
		marginbottom = changeToDp(50);
		/*
		 * data_screen = new int[] { changeToDp(60), changeToDp(30),
		 * changeToDp(120), changeToDp(80) };
		 */
		data_power = new float[] { 0 };
		/*
		 * data_power = new float[]{ 100, 90, 70, 100, 50, 49, 97, 87, 85, 67,
		 * 91, 93, 79, 61, 67, 81, 70, 80, 90, 73, 63, 67, 81, 90, 94, 73, 56,
		 * 70 };
		 */

		paint = new Paint();
		paint.setAntiAlias(true);
		textPaint = new TextPaint();
		textPaint.setAntiAlias(true);
		brokenLinePaint = new Paint();
		brokenLinePaint.setAntiAlias(true);
		pointPaint = new Paint();
		pointPaint.setAntiAlias(true);

	}

	/**
	 * 绘制坐标，包括刻度和坐标值
	 * 
	 * @param canvas
	 */
	public void drawAxis(Canvas canvas) {
		paint.setColor(getResources().getColor(R.color.progress_color));
		paint.setStrokeWidth(changeToDp((float) 0.5));
		// textPaint.setColor(Color.BLACK);
		textPaint.setColor(getResources()
				.getColor(R.color.grid_lines_axis_text));
		textPaint.setTextSize(changeToDp(textSize));
		// canvas.drawLine(0, height, changeToDp(data_power.length * 35),
		// height,
		// paint);
		// canvas.drawLine(30, 20, 30, 300, paint) ;

		int x = first_x; // x轴上第一个坐标
		// int y = changeToDp(250);

		/* 画x轴坐标和坐标值 */
		final int len = totaldays_of_month;
		float a = 0;
		for (int i = 1; i <= len/* data_power.length */; i++) {
			if (i >= 10) {
				a = 4.5f;
			} else {
				a = 2.5f;
			}

			if (i == 1) {
				// canvas.drawLine(x+interval, height - marginbottom, x, height
				// - marginbottom - changeToDp(3), paint);
				canvas.drawText(i + "", x + interval - changeToDp(a), height
						- marginbottom + changeToDp(10), textPaint);
			} else if (i % 5 == 0) {
				/* 绘制刻度 */
				// canvas.drawLine(x, height - marginbottom, x, height
				// - marginbottom - changeToDp(3), paint);

				/* 绘制坐标值 5-30 */
				canvas.drawText(i + "", x + interval - changeToDp(a), height
						- marginbottom + changeToDp(10), textPaint);
			}

			x += interval;
		}
		/* 绘制x坐标轴 */
		// canvas.drawLine(first_x, height-marginbottom, x, height-marginbottom,
		// paint);
		/* 画Y轴 */

		/*
		 * for (int i = 0; i < 5; i++) { canvas.drawText(50 * (i + 1) + "", 0,
		 * y, paint) ; canvas.drawLine(30, y, 34, y, paint); y -= 50 ; }
		 */
		// float max_y_axis = max_point_value;
		// NumberFormat ddf1=NumberFormat.getNumberInstance() ;
		// ddf1.setMaximumFractionDigits(2);
		switch (consumeType) {
		case 0: // 话费界面纵坐标轴刻度的显示
			// canvas.drawLine(first_x / 2, margintop,
			// first_x, margintop, paint);
			canvas.drawText("元", first_x - changeToDp(9), margintop
					- changeToDp(2), textPaint);
			canvas.drawText(String.valueOf(max_y_axis_value), first_x
					- changeToDp(8), margintop + changeToDp(10), textPaint);
			// canvas.drawLine(first_x / 2, (margintop+ height - marginbottom) /
			// 2,
			// first_x, (margintop + height - marginbottom) / 2,
			// paint);
			canvas.drawText(String.valueOf(max_y_axis_value / 3 * 2), first_x
					- changeToDp(8), (height - marginbottom + 2 * margintop)
					/ 3 + changeToDp(10), textPaint);
			canvas.drawText(String.valueOf(max_y_axis_value / 3), first_x
					- changeToDp(8),
					(2 * height - 2 * marginbottom + margintop) / 3
							+ changeToDp(10), textPaint);
			break;
		case 1:
			String[] showTextMax_append = CallStatUtils
					.traffic_unit1((long) max_y_axis_value);
			if (showTextMax_append[0].contains(".")) {
				showTextMax_append[0] = showTextMax_append[0].substring(0,
						showTextMax_append[0].indexOf("."));
			}
			String showTextMax = showTextMax_append[0] + showTextMax_append[1];
			int aa = backspaceAccLen(showTextMax_append[0]);

			String[] showTextMax_append2 = CallStatUtils
					.traffic_unit1((long) max_y_axis_value / 3 * 2);
			if (showTextMax_append2[0].contains(".")) {
				showTextMax_append2[0] = showTextMax_append2[0].substring(0,
						showTextMax_append2[0].indexOf("."));
			}
			String showTextMax_2 = showTextMax_append2[0]
					+ showTextMax_append2[1];
			int bb = backspaceAccLen(showTextMax_append2[0]);

			String[] showTextMax_append3 = CallStatUtils
					.traffic_unit1((long) max_y_axis_value / 3);
			if (showTextMax_append3[0].contains(".")) {
				showTextMax_append3[0] = showTextMax_append3[0].substring(0,
						showTextMax_append3[0].indexOf("."));
			}
			String showTextMax_3 = showTextMax_append3[0]
					+ showTextMax_append3[1];
			int cc = backspaceAccLen(showTextMax_append3[0]);
			// canvas.drawLine(first_x / 2, height -
			// highPoints[max_point_index][1],
			// first_x, height - highPoints[max_point_index][1], paint);
			canvas.drawText(showTextMax, first_x - changeToDp(aa), margintop
					+ changeToDp(10), textPaint);
			// canvas.drawLine(first_x / 2, (height -
			// highPoints[max_point_index][1]
			// + height - marginbottom) / 2, first_x, (height
			// - highPoints[max_point_index][1] + height - marginbottom) / 2,
			// paint);
			canvas.drawText(showTextMax_2, first_x - changeToDp(bb), (height
					- marginbottom + 2 * margintop)
					/ 3 + changeToDp(10), textPaint);
			canvas.drawText(showTextMax_3, first_x - changeToDp(cc), (2
					* height - 2 * marginbottom + margintop)
					/ 3 + changeToDp(10), textPaint);
			break;
		}

	}

	// 根据字符串的长度确定应该退多远，便于显示美观的函数
	public int backspaceAccLen(String str) {
		if (str.length() == 1) {
			return 11;
		} else if (str.length() == 2) {
			return 15;
		} else if (str.length() == 3) {
			return 19;
		} else if (str.length() == 4) {
			return 23;
		}
		return 0;
	}

	public void calcHighPoints() {
		highPoints = new float[data_power.length][2];
		for (int i = 0; i < data_power.length; i++) {
			highPoints[i][1] = data_power[i];
		}
		maxPoint_and_index(highPoints, highPoints.length);
		int x = first_x;

		if (consumeType == 0) {// 如果是话费显示，坐标的处理
			max_y_axis_value = (long) max_point_value + 1;
			if (max_y_axis_value % 3 != 0) {
				if (max_y_axis_value % 3 == 1) {
					max_y_axis_value += 2;
				} else if (max_y_axis_value % 3 == 2) {
					max_y_axis_value += 1;
				}
			}
			for (int i = 0; i < data_power.length; i++) {
				highPoints[i][0] = x + interval;
				if (highPoints[i][1] <= 0) {
					highPoints[i][1] = marginbottom;
				} else {
					highPoints[i][1] = (float) (data_power[i]
							* (height - marginbottom - margintop) * 1.0
							/ max_y_axis_value + marginbottom);
				}
				x += interval;
			}
		} else if (consumeType == 1) { // 如果是流量显示，坐标的处理
			String[] max_value_with_unit = CallStatUtils
					.traffic_unit1((long) max_point_value);
			max_y_axis_value = (long) Double
					.parseDouble(max_value_with_unit[0]) + 1;
			if (max_y_axis_value % 3 != 0) {
				if (max_y_axis_value % 3 == 1) {
					max_y_axis_value += 2;
				} else if (max_y_axis_value % 3 == 2) {
					max_y_axis_value += 1;
				}
			}
			if (max_value_with_unit[1].equalsIgnoreCase("K")) {
				max_y_axis_value = max_y_axis_value * 1024;
			} else if (max_value_with_unit[1].equalsIgnoreCase("M")) {
				max_y_axis_value = max_y_axis_value * 1024 * 1024;
			} else if (max_value_with_unit[1].equalsIgnoreCase("G")) {
				max_y_axis_value = max_y_axis_value * 1024 * 1024 * 1024;
			} else if (max_value_with_unit[1].equalsIgnoreCase("T")) {
				max_y_axis_value = max_y_axis_value * 1024 * 1024 * 1024 * 1024;
			}
			for (int i = 0; i < data_power.length; i++) {
				highPoints[i][0] = x + interval;
				if (highPoints[i][1] <= 0) {
					highPoints[i][1] = marginbottom;
				} else {
					highPoints[i][1] = (float) (data_power[i]
							* (height - marginbottom - margintop) * 1.0
							/ max_y_axis_value + marginbottom);
				}
				x += interval;
			}
		}
	}

	/**
	 * 绘制折线图，包括折点
	 * 
	 * @param canvas
	 */

	public void drawHighLines(Canvas canvas) {
		textPaint.setColor(textColor);
		textPaint.setTextSize(changeToDp(textSize));
		paint.setColor(Color.RED);
		paint.setStrokeWidth(changeToDp(1));
		pointPaint.setColor(pointColor);
		pointPaint.setStrokeWidth(changeToDp(3));

		// draw the cubic line
		Paint paint1 = new Paint(Paint.ANTI_ALIAS_FLAG
				| Paint.FILTER_BITMAP_FLAG);
		paint1.setColor(getResources().getColor(R.color.progress_color));
		paint1.setStrokeCap(Paint.Cap.ROUND);
		Path path = new Path();
		path.moveTo(highPoints[0][0], height - highPoints[0][1]);

		if (counter_index > data_power.length - 2 && data_power.length > 2) {
			counter_index = data_power.length - 2;
		}

		if (data_power.length == 1) {
			canvas.drawCircle(highPoints[0][0], height - highPoints[0][1],
					changeToDp(2f), pointPaint);

		} else if (data_power.length == 2) {
			path.cubicTo((highPoints[0][0] + highPoints[0 + 1][0]) / 2.0f,
					height - highPoints[0][1],
					(highPoints[0][0] + highPoints[0 + 1][0]) / 2.0f, height
							- highPoints[0 + 1][1], highPoints[0 + 1][0],
					height - highPoints[0 + 1][1]);
			path.lineTo(highPoints[0 + 1][0], height - marginbottom);
			path.lineTo(highPoints[0][0], height - marginbottom);
			path.lineTo(highPoints[0][0], height - highPoints[0][1]);
			paint1.setShader(new LinearGradient(highPoints[max_point_index][0],
					height - highPoints[max_point_index][1],
					highPoints[max_point_index][0], height - marginbottom,
					getResources().getColor(R.color.progress_color),
					getResources().getColor(R.color.day_spend_bg_color),
					TileMode.REPEAT));

			paint1.setAlpha(128);
			canvas.drawPath(path, paint1);
			Paint paintLine = new Paint(Paint.ANTI_ALIAS_FLAG
					| Paint.FILTER_BITMAP_FLAG);
			paintLine.setColor(getResources().getColor(R.color.progress_color));
			paintLine.setStrokeWidth(2);
			paintLine.setStyle(Style.STROKE);
			Path pathLine = new Path();
			pathLine.moveTo(highPoints[0][0], height - highPoints[0][1]);
			pathLine.cubicTo((highPoints[0][0] + highPoints[0 + 1][0]) / 2.0f,
					height - highPoints[0][1],
					(highPoints[0][0] + highPoints[0 + 1][0]) / 2.0f, height
							- highPoints[0 + 1][1], highPoints[0 + 1][0],
					height - highPoints[0 + 1][1]);
			canvas.drawPath(pathLine, paintLine);
			canvas.drawCircle(highPoints[0][0], height - highPoints[0][1],
					changeToDp(2f), pointPaint);
			canvas.drawCircle(highPoints[1][0], height - highPoints[1][1],
					changeToDp(2f), pointPaint);
		} else {
			for (int j = 0; j < counter_index + 1; j++) {
				if (counter_index < data_power.length - 2) {

					path.cubicTo(
							(highPoints[j][0] + highPoints[j + 1][0]) / 2.0f,
							height - highPoints[j][1],
							(highPoints[j][0] + highPoints[j + 1][0]) / 2.0f,
							height - highPoints[j + 1][1],
							highPoints[j + 1][0], height - highPoints[j + 1][1]);

					paint1.setStrokeWidth(2);
					paint1.setStyle(Style.STROKE);
					if (j == counter_index - 1) {
						canvas.drawPath(path, paint1);
					}

				} else {
					paint1.setShader(new LinearGradient(
							highPoints[max_point_index][0], height
									- highPoints[max_point_index][1],
							highPoints[max_point_index][0], height
									- marginbottom, getResources().getColor(
									R.color.progress_color), getResources()
									.getColor(R.color.day_spend_bg_color),
							TileMode.REPEAT));
					paint1.setAlpha(128);
					path.cubicTo(
							(highPoints[j][0] + highPoints[j + 1][0]) / 2.0f,
							height - highPoints[j][1],
							(highPoints[j][0] + highPoints[j + 1][0]) / 2.0f,
							height - highPoints[j + 1][1],
							highPoints[j + 1][0], height - highPoints[j + 1][1]);

					if (j == counter_index) {
						path.lineTo(highPoints[j + 1][0], height - marginbottom);
						path.lineTo(highPoints[0][0], height - marginbottom);
						path.lineTo(highPoints[0][0], height - highPoints[0][1]);
						paint1.setStrokeWidth(2);
						paint1.setStyle(Style.FILL);
						canvas.drawPath(path, paint1);

						for (int i = 0; i <= counter_index + 1; i++) {
							canvas.drawCircle(highPoints[i][0], height
									- highPoints[i][1], changeToDp(2f),
									pointPaint);
						}

						Paint paintLine = new Paint(Paint.ANTI_ALIAS_FLAG
								| Paint.FILTER_BITMAP_FLAG);
						paintLine.setColor(getResources().getColor(
								R.color.progress_color));
						paintLine.setStrokeWidth(2);
						paintLine.setStyle(Style.STROKE);
						Path pathLine = new Path();
						pathLine.moveTo(highPoints[0][0], height
								- highPoints[0][1]);
						for (int k = 0; k < counter_index + 1; k++) {
							pathLine.cubicTo(
									(highPoints[k][0] + highPoints[k + 1][0]) / 2.0f,
									height - highPoints[k][1],
									(highPoints[k][0] + highPoints[k + 1][0]) / 2.0f,
									height - highPoints[k + 1][1],
									highPoints[k + 1][0], height
											- highPoints[k + 1][1]);
						}
						canvas.drawPath(pathLine, paintLine);
					}
				}

			}
		}

	}

	public void drawGridLines(Canvas canvas) {
		int b = 0;
		if (height > 300) {
			b = 45;
		} else {
			b = 20;
		}

		Bitmap bitmap_h = BitmapFactory.decodeResource(getResources(),
				R.drawable.line_h);
		Bitmap bitmap_v = BitmapFactory.decodeResource(getResources(),
				R.drawable.line_s);
		Matrix matrix = new Matrix();
		Matrix matrix_v = new Matrix();
		matrix.postScale(
				(width + changeToDp(20)) / (float) bitmap_h.getWidth(), 1);
		Bitmap bitmap_h_new = Bitmap.createBitmap(bitmap_h, 0, 0,
				bitmap_h.getWidth(), bitmap_h.getHeight(), matrix, true);
		matrix_v.postScale(1,
				(height + changeToDp(b)) / (float) bitmap_v.getHeight());
		Bitmap bitmap_v_new = Bitmap.createBitmap(bitmap_v, 0, 0,
				bitmap_v.getWidth(), bitmap_v.getHeight(), matrix_v, true);

		canvas.drawBitmap(bitmap_h_new, 0, margintop, pointPaint);
		canvas.drawBitmap(bitmap_h_new, 0,
				(height - marginbottom + 2 * margintop) / 3, pointPaint);
		canvas.drawBitmap(bitmap_h_new, 0,
				(2 * height - 2 * marginbottom + margintop) / 3, pointPaint);
		canvas.drawBitmap(bitmap_h_new, 0, height - marginbottom, pointPaint);

		canvas.drawBitmap(bitmap_v_new, first_x, changeToDp(-10), pointPaint);
		canvas.drawBitmap(bitmap_v_new, first_x + 5 * interval,
				changeToDp(-10), pointPaint);
		canvas.drawBitmap(bitmap_v_new, first_x + 10 * interval,
				changeToDp(-10), pointPaint);
		canvas.drawBitmap(bitmap_v_new, first_x + 15 * interval,
				changeToDp(-10), pointPaint);
		canvas.drawBitmap(bitmap_v_new, first_x + 20 * interval,
				changeToDp(-10), pointPaint);
		canvas.drawBitmap(bitmap_v_new, first_x + 25 * interval,
				changeToDp(-10), pointPaint);
		canvas.drawBitmap(bitmap_v_new, first_x + 30 * interval,
				changeToDp(-10), pointPaint);

	}

	class DrawHandler extends Handler {
		public DrawHandler() {
		}

		public DrawHandler(Looper looper) {
			super(looper);
		}

		public void handleMessage(Message msg) {
			// TODO
			invalidate();
		}
	}

	@Override
	public void onDraw(Canvas canvas) {
		calcHighPoints();
		drawGridLines(canvas);
		drawAxis(canvas);
		drawHighLines(canvas);

		if (counter_index < highPoints.length - 2 && highPoints.length > 2) {
			drawHandler.sendEmptyMessageDelayed(counter_index, 10);
		}
		counter_index = counter_index + 1;

		if (emphasize_index != -1) {
			paint.setColor(pointColor);
			canvas.drawLine(highPoints[emphasize_index][0], 0,
					highPoints[emphasize_index][0], height, paint);
			canvas.drawLine(0, height - highPoints[emphasize_index][1], width,
					height - highPoints[emphasize_index][1], paint);
			emphasize_index = -1;
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		// TODO Auto-generated method stub
		super.onSizeChanged(w, h, oldw, oldh);
		width = w - changeToDp(20);
		height = h - changeToDp(15);
		interval = (width - changeToDp(8)) / 30;
	}

	private int changeToDp(float beforeChange) {
		int afterChange = (int) Math.round(beforeChange * dencity);
		return afterChange;
	}

	private float getMax(float[] data) {
		float max = data[0];
		for (int i = 0; i < data.length; i++) {
			if (i + 1 < data.length) {
				max = (max >= data[i]) ? max : data[i];
			}
		}
		return max;
	}

	// 判断点击的位置是否在某两个日期的范围之内，如果超出了日期的坐标范围，则返回-1.
	private int isWithinDomain(float x) {
		if (x < highPoints[0][0] || x > highPoints[highPoints.length - 1][0]) {
			return -1;// 此种情况表示鼠标点击的范围已经超出了点的横坐标范围。
		}
		for (int i = 0; i < highPoints.length - 1; i++) {
			if (x >= highPoints[i][0] && x <= highPoints[i + 1][0]) {
				if (x - highPoints[i][0] <= highPoints[i + 1][0] - x) {
					return i;
				} else {
					return i + 1;
				}

			}
		}

		return 0;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_CANCEL) {
			ILog.LogE(this.getClass(), "MotionEvent.ACTION_CANCEL is in");
			emphasize_index = -1;
			invalidate();
			mOnCoordinateChanged.onCoordinateChanged(emphasize_index + 1,
					emphasize_index, true);
		}
		if (counter_index < data_power.length - 2) {
			return super.onTouchEvent(event);
		}
		final int pointerCount = event.getPointerCount();

		int action = event.getAction();

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			emphasize_index = isWithinDomain(event.getX());
			if (emphasize_index != -1) {
				invalidate();
				mOnCoordinateChanged.onCoordinateChanged(emphasize_index + 1,
						data_power[emphasize_index], false);
			}

			break;
		case MotionEvent.ACTION_MOVE: // 这里处理滑动
			emphasize_index = isWithinDomain(event.getX());
			if (emphasize_index != -1) {
				invalidate();
				mOnCoordinateChanged.onCoordinateChanged(emphasize_index + 1,
						data_power[emphasize_index], false);
			}

			break;
		case MotionEvent.ACTION_UP:
			emphasize_index = -1;
			invalidate();
			mOnCoordinateChanged.onCoordinateChanged(emphasize_index + 1,
					emphasize_index, true);
			break;
		default:
			break;
		}

		return true;
	}

	public interface OnCoordinateChanged {
		public void onCoordinateChanged(int x, float y, boolean isUp);
	}

	public void setmOnCoordinateChanged(OnCoordinateChanged mOnCoordinateChanged) {
		this.mOnCoordinateChanged = mOnCoordinateChanged;
	}

}
