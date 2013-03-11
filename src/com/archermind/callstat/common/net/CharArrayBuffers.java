package com.archermind.callstat.common.net;

import org.apache.http.protocol.HTTP;
import org.apache.http.util.CharArrayBuffer;

class CharArrayBuffers {

	static final char uppercaseAddon = 'a' - 'A';

	/**
	 * Returns true if the buffer contains the given string. Ignores leading
	 * whitespace and case.
	 * 
	 * @param buffer
	 *            to search
	 * @param beginIndex
	 *            index at which we should start
	 * @param str
	 *            to search for
	 */
	static boolean containsIgnoreCaseTrimmed(CharArrayBuffer buffer,
			int beginIndex, final String str) {
		int len = buffer.length();
		char[] chars = buffer.buffer();
		while (beginIndex < len && HTTP.isWhitespace(chars[beginIndex])) {
			beginIndex++;
		}
		int size = str.length();
		boolean ok = len >= beginIndex + size;
		for (int j = 0; ok && (j < size); j++) {
			char a = chars[beginIndex + j];
			char b = str.charAt(j);
			if (a != b) {
				a = toLower(a);
				b = toLower(b);
				ok = a == b;
			}
		}
		return ok;
	}

	/**
	 * Returns index of first occurence ch. Lower cases characters leading up to
	 * first occurrence of ch.
	 */
	static int setLowercaseIndexOf(CharArrayBuffer buffer, final int ch) {

		int beginIndex = 0;
		int endIndex = buffer.length();
		char[] chars = buffer.buffer();

		for (int i = beginIndex; i < endIndex; i++) {
			char current = chars[i];
			if (current == ch) {
				return i;
			} else if (current >= 'A' && current <= 'Z') {
				// make lower case
				current += uppercaseAddon;
				chars[i] = current;
			}
		}
		return -1;
	}

	private static char toLower(char c) {
		if (c >= 'A' && c <= 'Z') {
			c += uppercaseAddon;
		}
		return c;
	}
}
