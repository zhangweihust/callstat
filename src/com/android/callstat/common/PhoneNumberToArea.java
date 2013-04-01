package com.android.callstat.common;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.android.callstat.IllegalNumException;

public class PhoneNumberToArea {

	public static byte[] intToByte(int i) {
		byte[] abyte0 = new byte[2];
		abyte0[0] = (byte) (0xff & i);
		abyte0[1] = (byte) ((0xff00 & i) >> 8);
		/*
		 * abyte0[2] = (byte) ((0xff0000 & i) >> 16); abyte0[3] = (byte)
		 * ((0xff000000 & i) >> 24);
		 */
		return abyte0;
	}

	public static int bytesToInt(byte[] bytes) {
		int addr = bytes[0] & 0xFF;
		addr |= ((bytes[1] << 8) & 0xFF00);
		/*
		 * addr |= ((bytes[2] << 16) & 0xFF0000); addr |= ((bytes[3] << 24) &
		 * 0xFF000000);
		 */
		return addr;
	}

	public static int getIndex(String num) throws IllegalNumException {
		int index = -1;

		if (num.length() != 7) {
			throw new IllegalNumException("输入的号码不等于7");
		}
		if (num.startsWith("13") || num.startsWith("14")
				|| num.startsWith("15") || num.startsWith("18")) {
			Integer number = Integer.parseInt(num);
			if (num.startsWith("18")) {
				index = 300000 + (number - 1800000);
			} else {
				index = number - 1300000;
			}
		} else {
			throw new IllegalNumException("非法手机号段：不再13，14，15，18范围内");
		}
		return index;
	}

	public static int getAreaCode(String path, String num) {
		int code = 0;

		RandomAccessFile raf;

		try {
			raf = new RandomAccessFile(path, "r");
			byte[] buff = new byte[2];
			int index = getIndex(num);
			raf.seek(index * 2);
			raf.read(buff, 0, 2);
			code = bytesToInt(buff);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IllegalNumException e) {
			e.printStackTrace();
		}

		return code;
	}

}
