package com.android.callstat.common.views;

/**
 * @author LongXue
 * copy right LongXue 2012-06-15
 */
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.android.callstat.common.views.adapter.NumericWheelAdapter;
import com.archermind.callstat.R;

public class MyTimePicker extends FrameLayout {

	private static final int DEFAULT_START_HOUR = 23;
	private static final int DEFAULT_END_HOUR = 8;
	// TimePickerDialog tpd = new TimePickerDialog(context, callBack, hourOfDay,
	// minute, is24HourView)
	/* UI Components */
	private final WheelView mHourPicker;
	private final WheelView mMinuPicker;

	private boolean mHourWheelScrolling;
	private boolean mMinuWheelScrolling;

	/**
	 * How we notify users the date has changed.
	 */
	private OnTimeSetListener mOnTimeSetListener;

	// private TextView mHour_tv;
	// private TextView mMinu_tv;

	private int mHour;
	private int mMinu;

	private NumericWheelAdapter hourAdapter;
	private NumericWheelAdapter minuAdapter;

	/**
	 * The callback used to indicate the user changes the date.
	 */
	public interface OnTimeSetListener {

		/**
		 * @param view
		 *            The view associated with this listener.
		 * @param year
		 *            The year that was set.
		 * @param monthOfYear
		 *            The month that was set (0-11) for compatibility with
		 *            {@link java.util.Calendar}.
		 * @param dayOfMonth
		 *            The day of the month that was set.
		 */
		void onTimeSet(MyTimePicker view, int hour, int minute);
	}

	public MyTimePicker(Context context) {
		this(context, null);
	}

	public MyTimePicker(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public MyTimePicker(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.time_picker, this, true);
		// int width =

		hourAdapter = new NumericWheelAdapter(0, 23);
		minuAdapter = new NumericWheelAdapter(0, 59);

		mHourPicker = (WheelView) findViewById(R.id.hour_picker);

		// mHour_tv = (TextView) findViewById(R.id.hour_tv);
		// mMinu_tv = (TextView) findViewById(R.id.minu_tv);

		mHourPicker.setAdapter(hourAdapter);
		mHourPicker.setCyclic(true);
		mHourPicker.addScrollingListener(new OnWheelScrollListener() {
			public void onScrollingStarted(WheelView wheel) {
				mHourWheelScrolling = true;
			}

			public void onScrollingFinished(WheelView wheel) {
				mHourWheelScrolling = false;
				mHour = mHourPicker.getCurrentItem();
				updateDate(mHour, mMinu);
			}
		});

		mHourPicker.addChangingListener(new OnWheelChangedListener() {

			@Override
			public void onChanged(WheelView wheel, int oldValue, int newValue) {
				if (!mHourWheelScrolling) {
					// TODO
					mHour = newValue;
					updateDate(mHour, mMinu);
				}
			}
		});
		// mHourPicker.setInterpolator(new AnticipateOvershootInterpolator());

		mMinuPicker = (WheelView) findViewById(R.id.minu_picker);
		mMinuPicker.setAdapter(minuAdapter);
		mMinuPicker.setCyclic(true);
		mMinuPicker.addScrollingListener(new OnWheelScrollListener() {
			public void onScrollingStarted(WheelView wheel) {
				mMinuWheelScrolling = true;
			}

			public void onScrollingFinished(WheelView wheel) {
				mMinuWheelScrolling = false;
				mMinu = mMinuPicker.getCurrentItem();
				updateDate(mHour, mMinu);

			}
		});

		mMinuPicker.addChangingListener(new OnWheelChangedListener() {

			@Override
			public void onChanged(WheelView wheel, int oldValue, int newValue) {
				// Log.i("t", "mMinuPicker addChangingListener");
				if (!mMinuWheelScrolling) {
					// TODO
					// Log.i("t", "onChanged:" + oldValue + " " + newValue);
					mMinu = newValue;
					updateDate(mHour, mMinu);
				}
			}
		});
		// mMinuPicker.setInterpolator(new AnticipateOvershootInterpolator());
	}

	public void setHour(int hour) {
		mHourPicker.setCurrentItem(hour, false);
	}

	public void setMinute(int minute) {
		mMinuPicker.setCurrentItem(minute, false);
	}

	public void setTime(int hour, int minute) {
		mHourPicker.setCurrentItem(hour, false);
		mMinuPicker.setCurrentItem(minute, false);
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		mHourPicker.setEnabled(enabled);
		mMinuPicker.setEnabled(enabled);
	}

	public void updateDate(int hour, int minu) {
		Log.i("t", "updateDate:" + hour + " : " + minu);
		mMinu = minu;
		mHour = hour;
		// mHour_tv.setText(String.valueOf(hour));
		//
		// if (minu < 10)
		// {
		// mMinu_tv.setText("0" + minu);
		// }
		// else
		// {
		// mMinu_tv.setText(String.valueOf(minu));
		// }
		setTime(hour, minu);
		notifyDateChanged();
	}

	/**
	 * Initialize the state.
	 * 
	 * @param year
	 *            The initial year.
	 * @param monthOfYear
	 *            The initial month.
	 * @param dayOfMonth
	 *            The initial day of the month.
	 * @param onDateChangedListener
	 *            How user is notified date is changed by user, can be null.
	 */
	public void init(int hour, int minute, OnTimeSetListener onTimeSetListener) {
		mOnTimeSetListener = onTimeSetListener;
		updateDate(hour, minute);
	}

	public int getMinute() {
		return mMinu;
	}

	public int getHour() {
		return mHour;
	}

	private void notifyDateChanged() {
		if (mOnTimeSetListener != null) {
			mOnTimeSetListener.onTimeSet(MyTimePicker.this, mHour, mMinu);
		}
	}

}
