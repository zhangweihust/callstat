package com.android.callstat.common;

import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/**
 * ImageUtil of MoboTap Client.
 */

public class ImageUtil {

	/**
	 * Convert drawable to bitmap
	 * 
	 * @param drawable
	 * @return
	 */
	public static Bitmap drawableToBitmap(final Drawable drawable) {
		Bitmap bitmap = null;
		if (null != drawable) {
			if (drawable instanceof BitmapDrawable) {
				bitmap = ((BitmapDrawable) drawable).getBitmap();
			} else {
				bitmap = Bitmap
						.createBitmap(
								drawable.getIntrinsicWidth(),
								drawable.getIntrinsicHeight(),
								drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
										: Bitmap.Config.RGB_565);
				final Canvas canvas = new Canvas(bitmap);
				drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
						drawable.getIntrinsicHeight());
				drawable.draw(canvas);
			}
		}
		return bitmap;
	}

	/**
	 * convert bitmap to byte array
	 * 
	 * @param bitmap
	 * @return
	 */
	public static byte[] bitmapToBytes(final Bitmap bitmap) {
		if (null == bitmap) {
			throw new IllegalArgumentException("Bitmap must not be null");
		}
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 0, baos);
		return baos.toByteArray();
	}

	/**
	 * Convert drawable to bytes
	 * 
	 * @param drawable
	 * @return
	 */
	public static byte[] drawableToBytes(final Drawable drawable) {
		final Bitmap bitmap = drawableToBitmap(drawable);
		return bitmapToBytes(bitmap);
	}

	/**
	 * convert an byte array to drawable
	 * 
	 * @param imageBytes
	 * @return
	 */
	public static Drawable bytesToDrawable(final byte[] imageBytes) {
		if (null == imageBytes) {
			throw new IllegalArgumentException("image bytes must not be null");
		}
		final Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0,
				imageBytes.length);
		final Drawable drawable = new BitmapDrawable(bitmap);
		return drawable;
	}

}
