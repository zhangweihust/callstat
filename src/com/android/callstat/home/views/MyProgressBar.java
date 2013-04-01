package com.android.callstat.home.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.NinePatch;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import com.archermind.callstat.R;

public class MyProgressBar extends ProgressBar {

	private int progress;
	private int width = 0;
	private int height;
	private Bitmap bmp;
	private Rect rect = new Rect(0, 0, 0, 0);

	public MyProgressBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public MyProgressBar(Context context) {
		super(context);
		init();
	}

	private void init() {
		bmp = BitmapFactory.decodeResource(getResources(),
				R.drawable.lo_bf_green);
	}

	@Override
	public synchronized int getProgress() {
		// TODO Auto-generated method stub
		return progress;
	}

	@Override
	public synchronized void setProgress(int progress) {
		super.setProgress(progress);
		this.progress = progress;
	}

	@Override
	protected synchronized void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		setpro();
		drawNinepath(canvas, rect);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		width = w;
		height = h;
	}

	public void drawNinepath(Canvas c, Rect r1) {
		NinePatch patch = new NinePatch(bmp, bmp.getNinePatchChunk(), null);
		patch.draw(c, r1);
	}

	public void setpro() {
		int len = (int) Math.round(getProgress() / 100f * width);
		rect = new Rect(0, 0, len, height);
	}

}
