package com.archermind.callstat.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;

public final class StringUtil {

	/**
	 * The max length for file name.
	 */
	public static final int MAX_PATH = 256;

	/**
	 * Illegal file name chars.
	 */
	public static final Pattern ILLEGAL_FILE_NAME_CHARS = Pattern
			.compile("[\\\\/:*?<>|]+");

	/**
	 * �հ��ַ�
	 */
	private static final char[] WhitespaceChars = new char[] { '\u0000',
			'\u0001', '\u0002', '\u0003', '\u0004', '\u0005', '\u0006',
			'\u0007', '\u0008', '\u0009', '\n', '\u000b', '\u000c', '\r',
			'\u000e', '\u000f', '\u0010', '\u0011', '\u0012', '\u0013',
			'\u0014', '\u0015', '\u0016', '\u0017', '\u0018', '\u0019',
			'\u001a', '\u001b', '\u001c', '\u001d', '\u001e', '\u001f',
			'\u0020'

	};
	private static final String TAG = "StringUtil";

	private static final int BUFFER_SIZE = 4096;

	private static final Pattern EMAIL_PATTERN = Pattern.compile(
			"^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}$",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern ID_CARD = Pattern
			.compile("\\d{17}[\\d|X]|\\d{15}");
	private static final Pattern PHONE_NO = Pattern.compile("^[1]\\d{10}$");

	/**
	 * �жϸ���ַ����飨���������Ƿ�����ַ�
	 * 
	 * @param chars
	 *            �Ѿ�������ַ����顣
	 * @param ch
	 *            Ҫ�����ַ�
	 * @return ��� ch ������ chars ���򷵻� true�����򷵻� false��
	 */
	public static final boolean containsChar(final char[] chars, final char ch) {
		return Arrays.binarySearch(chars, ch) >= 0;
	}

	/**
	 * ���ز����е�һ���� {@code null} �Ĳ���
	 * 
	 * @param args
	 *            Ҫ���Ĳ���
	 * @return ��Ĳ����е�һ���� {@code null} �Ĳ���������Ϊ {@code null}���򷵻�
	 *         {@code null}��
	 */
	public static <T> T firstNotNull(final T... args) {
		for (final T object : args) {
			if (object != null) {
				return object;
			}
		}
		return null;
	}

	/**
	 * ���ز����е�һ���� {@code null} �Ĳ���
	 * 
	 * @param args
	 *            Ҫ���Ĳ���
	 * @return ��Ĳ����е�һ���� {@code null} �Ĳ���������Ϊ {@code null}���򷵻�
	 *         {@code null}��
	 */
	public static <T extends CharSequence> T firstNotEmpty(final T... args) {
		for (final T object : args) {
			if (!TextUtils.isEmpty(object)) {
				return object;
			}
		}
		return null;
	}

	/**
	 * ���ز����е�һ������Ĳ���
	 * 
	 * @param args
	 *            Ҫ���Ĳ���
	 * @return ��Ĳ����е�һ������Ĳ���������Ϊ�㣬�򷵻��㡣
	 */
	public static int firstNonZeroInt(final int... args) {
		for (final int object : args) {
			if (object != 0) {
				return object;
			}
		}
		return 0;
	}

	/**
	 * ���ز����е�һ���Ǹ���Ĳ���
	 * 
	 * @param args
	 *            Ҫ���Ĳ���
	 * @return ��Ĳ����е�һ���Ǹ���������Ϊ�����򷵻� {@linkplain Integer#MIN_VALUE}��
	 */
	public static int firstNonNegativeInt(final int... args) {
		for (final int object : args) {
			if (object >= 0) {
				return object;
			}
		}
		return Integer.MIN_VALUE;
	}

	/**
	 * ���ز����е�һ������Ĳ���
	 * 
	 * @param args
	 *            Ҫ���Ĳ���
	 * @return ��Ĳ����е�һ������������Ϊ������㣬�򷵻��㡣
	 */
	public static int firstPostiveInt(final int... args) {
		for (final int object : args) {
			if (object > 0) {
				return object;
			}
		}
		return 0;
	}

	/**
	 * ���ز����е�һ������Ĳ���
	 * 
	 * @param args
	 *            Ҫ���Ĳ���
	 * @return ��Ĳ����е�һ������Ĳ���������Ϊ�㣬�򷵻��㡣
	 */
	public static long firstNonZeroLong(final long... args) {
		for (final long object : args) {
			if (object != 0) {
				return object;
			}
		}
		return 0;
	}

	/**
	 * ���ز����е�һ���Ǹ���Ĳ���
	 * 
	 * @param args
	 *            Ҫ���Ĳ���
	 * @return ��Ĳ����е�һ���Ǹ���������Ϊ�����򷵻� {@linkplain longeger#MIN_VALUE}
	 *         ��
	 */
	public static long firstNonNegativeLong(final long... args) {
		for (final long object : args) {
			if (object >= 0) {
				return object;
			}
		}
		return Long.MIN_VALUE;
	}

	/**
	 * ���ز����е�һ������Ĳ���
	 * 
	 * @param args
	 *            Ҫ���Ĳ���
	 * @return ��Ĳ����е�һ������������Ϊ������㣬�򷵻��㡣
	 */
	public static long firstPostiveLong(final long... args) {
		for (final long object : args) {
			if (object > 0) {
				return object;
			}
		}
		return 0;
	}

	/**
	 * ���ز����е�һ������Ĳ���
	 * 
	 * @param args
	 *            Ҫ���Ĳ���
	 * @return ��Ĳ����е�һ������Ĳ���������Ϊ�㣬�򷵻��㡣
	 */
	public static double firstNonZero(final double... args) {
		for (final double object : args) {
			if (object != 0) {
				return object;
			}
		}
		return 0;
	}

	/**
	 * ���ز����е�һ����Ĳ���
	 * 
	 * @param args
	 *            Ҫ���Ĳ���
	 * @return ��Ĳ����е�һ����������Ϊ {@linkplain Double#NaN}���򷵻�
	 *         {@linkplain Double#NaN}��
	 */
	public static double firstDouble(final double... args) {
		for (final double object : args) {
			if (!Double.isNaN(object)) {
				return object;
			}
		}
		return Double.NaN;
	}

	/**
	 * ���ز����е�һ��������Ĳ���
	 * 
	 * @param args
	 *            Ҫ���Ĳ���
	 * @return ��Ĳ����е�һ��������������Ϊ {@linkplain Double#NaN} ��������򷵻�
	 *         {@linkplain Double#NaN}��
	 */
	public static double firstFinite(final double... args) {
		for (final double object : args) {
			if (!Double.isNaN(object) && !Double.isInfinite(object)) {
				return object;
			}
		}
		return Double.NaN;
	}

	/**
	 * ���ز����е�һ���Ǹ���Ĳ���
	 * 
	 * @param args
	 *            Ҫ���Ĳ���
	 * @return ��Ĳ����е�һ���Ǹ���������Ϊ�����򷵻� {@linkplain Double#NaN}��
	 */
	public static double firstNonNegative(final double... args) {
		for (final double object : args) {
			if (object >= 0) {
				return object;
			}
		}
		return Double.NaN;
	}

	/**
	 * ���ز����е�һ������Ĳ���
	 * 
	 * @param args
	 *            Ҫ���Ĳ���
	 * @return ��Ĳ����е�һ������������Ϊ������㣬�򷵻��㡣
	 */
	public static double firstPostive(final double... args) {
		for (final double object : args) {
			if (object > 0) {
				return object;
			}
		}
		return 0;
	}

	/**
	 * ��ȡһ������ȫ�޶���ļ���
	 * 
	 * @param className
	 *            �����ȫ�޶���
	 * @return className �ļ���
	 */
	public static final String getSimpleClassName(final String className) {
		if (isNullOrEmpty(className)) {
			return className;
		}
		final int index = className.lastIndexOf('.');
		if (-1 == index) {
			return className.substring(index);
		}
		return className;
	}

	/**
	 * ��ȡ�쳣��Ϣ�Ķ�ջ������Ϣ��
	 * 
	 * @param throwable
	 *            Ҫ�������쳣��Ϣ��
	 * @return ���쳣��Ϣ�Ķ�ջ������Ϣ��
	 */
	public static final String getThrowableStackTrace(final Throwable throwable) {
		final StringWriter sWriter = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(sWriter);
		throwable.printStackTrace(printWriter);
		printWriter.flush();
		printWriter.close();
		final String message = sWriter.toString();
		return message;
	}

	/**
	 * ��ȡ���ַ��У�ĳЩ�ض��ַ�ĵ�һ������
	 * 
	 * @param value
	 *            Ҫ�������ַ�
	 * @param chars
	 *            Ҫ���ҵ��ַ�
	 * @return value �е�һ�����ֵ� chars �������ַ������ ��� value �в����� chars
	 *         �е������ַ��򷵻� -1��
	 */
	public static int indexOfAny(final String value, final char... chars) {
		return indexOfAny(value, 0, chars);
	}

	/**
	 * ��ȡ���ַ��У�ĳЩ�ض��ַ�ĵ�һ������
	 * 
	 * @param value
	 *            Ҫ�������ַ�
	 * @param chars
	 *            Ҫ���ҵ��ַ�
	 * @param start
	 *            ���ҵ���ʼ�㡣
	 * @return value �У��� start ���һ�����ֵ� chars �������ַ������ ��� value
	 *         �в����� chars �е������ַ��򷵻� -1��
	 */
	public static int indexOfAny(final String value, final int start,
			final char... chars) {
		if (null == value || value.length() == 0) {
			return -1;
		}
		final int i = 0, n = chars.length;
		int index = -1;
		while (i < n || -1 == index) {
			index = value.indexOf(chars[i], start);
		}
		return index;
	}

	/**
	 * �ж�һ���ַ��Ƿ�Ϊ ""��
	 * 
	 * @param ������ַ� ��
	 * @return ����ַ�Ϊ ""���򷵻� true�����򷵻� false��
	 */
	public static final boolean isEmpty(final String value) {
		return null != value && 0 == value.length();
	}

	/**
	 * �ж�һ���ַ��Ƿ�Ϊ null ���� ""��
	 * 
	 * @param ������ַ� ��
	 * @return ����ַ�Ϊ null ���� ""���򷵻� true�����򷵻� false��
	 */
	public static final boolean isNullOrEmpty(final String value) {
		if (TextUtils.isEmpty(value) || value.equals("null")) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * �ж�һ���ַ��Ƿ�Ϊ null ����ֻ��հ��ַ�ո񡢻س������еȣ���
	 * 
	 * @param ������ַ� ��
	 * @return ����ַ�Ϊ null ����ֻ��հ��ַ��򷵻� true�����򷵻� false��
	 */
	public static final boolean isNullOrWhitespaces(final String value) {
		return null == value || 0 == value.trim().length()
				|| "null".equals(value);
	}

	/**
	 * �ж�һ���ַ��Ƿ�ֻ��հ��ַ�ո񡢻س������еȣ���
	 * 
	 * @param ������ַ� ��
	 * @return ����ַ�ֻ��հ��ַ��򷵻� true�����򷵻� false��
	 */
	public static final boolean isWhitespaces(final String value) {
		return null != value && 0 == value.trim().length();
	}

	/**
	 * ��ȡ���ַ��У�ĳЩ�ض��ַ�����һ������
	 * 
	 * @param value
	 *            Ҫ�������ַ�
	 * @param chars
	 *            Ҫ���ҵ��ַ�
	 * @return value �����һ�����ֵ� chars �������ַ������ ��� value �в����� chars
	 *         �е������ַ��򷵻� -1��
	 */
	public static final int lastIndexOfAny(final String value,
			final char... chars) {
		return lastIndexOfAny(value, value.length() - 1, chars);
	}

	/**
	 * ��ȡ���ַ��У�ĳЩ�ض��ַ�����һ������
	 * 
	 * @param value
	 *            Ҫ�������ַ�
	 * @param chars
	 *            Ҫ���ҵ��ַ�
	 * @param start
	 *            ���ҵ���ʼ�㣬���������ʼ��ǰ������
	 * @return value �У��� start �����һ�����ֵ� chars �������ַ������ ��� value
	 *         �в����� chars �е������ַ��򷵻� -1��
	 */
	public static final int lastIndexOfAny(final String value, final int start,
			final char... chars) {
		if (null == value || value.length() == 0) {
			return -1;
		}
		final int i = 0, n = chars.length;
		int index = -1;
		while (i < n || -1 == index) {
			index = value.lastIndexOf(chars[i], start);
		}
		return index;
	}

	/**
	 * �淶���ַ�
	 * 
	 * @param s
	 *            Ҫ�淶�����ַ�
	 * @return ����ַ�Ϊ null ���߳��ȴ����㣬�򷵻� s������ַ���Ϊ�㣬�򷵻� null��
	 */
	public final static String normalize(final String s) {
		if (s == null || s.length() > 0) {
			return s;
		}
		return null;
	}

	/**
	 * ��ȡ�����ȡ���е����ݡ�
	 * 
	 * @param bufferedReader
	 *            Ҫ��ȡ�Ļ����ȡ����
	 * @return ���ȡ���е����ݡ�
	 */
	public static String stringFromBufferedReader(
			final BufferedReader bufferedReader) {
		if (bufferedReader == null) {
			return null;
		}
		final StringBuffer result = new StringBuffer();
		String line = null;
		try {
			while ((line = bufferedReader.readLine()) != null) {
				result.append(line);
			}
		} catch (final IOException e) {
			e.printStackTrace();
			return null;
		}
		return result.toString();
	}

	/**
	 * ��ȡһ���������е����ݣ���������ַ�
	 * 
	 * @param inputStream
	 *            Ҫ��ȡ����������
	 * @param encoding
	 *            �����õ��ַ���롣
	 * @return �����������ʾ���ַ�
	 * @throws UnsupportedEncodingException
	 *             ��֧�ִ˱��롣
	 */
	public static String stringFromInputStream(final InputStream inputStream,
			final String encoding) throws UnsupportedEncodingException {
		final InputStreamReader reader = new InputStreamReader(inputStream,
				encoding);
		return stringFromBufferedReader(new BufferedReader(reader));
	}

	/**
	 * �Ƴ���ַ�ͷ�ͽ�β�����ض��ַ�
	 * 
	 * @param value
	 *            Ҫ�޼����ַ�
	 * @param chars
	 *            Ҫ�Ƴ���ַ�
	 * @return �޼�����ַ�
	 */
	public static String trim(final String value, final char... chars) {
		if (null == value || value.length() == 0) {
			return value;
		}
		Arrays.sort(chars);
		int startIndex = 0;
		int endIndex = value.length() - 1;
		boolean flag = containsChar(chars, value.charAt(startIndex));
		while (flag && startIndex <= endIndex) {
			startIndex++;
			flag = containsChar(chars, value.charAt(startIndex));
		}

		flag = containsChar(chars, value.charAt(endIndex));
		while (flag && startIndex <= endIndex) {
			endIndex--;
			flag = containsChar(chars, value.charAt(endIndex));
		}
		if (startIndex >= endIndex) {
			return "";
		}
		return value.substring(startIndex, endIndex + 1);
	}

	/**
	 * �Ƴ���ַ��β�Ŀհ��ַ�
	 * 
	 * @param value
	 *            Ҫ�޼����ַ�
	 * @return �޼�����ַ�
	 */
	public static final String trimEnd(final String value) {
		return trimEnd(value, WhitespaceChars);
	}

	/**
	 * �Ƴ���ַ��β�����ض��ַ�
	 * 
	 * @param value
	 *            Ҫ�޼����ַ�
	 * @param chars
	 *            Ҫ�Ƴ���ַ�
	 * @return �޼�����ַ�
	 */
	public static String trimEnd(final String value, final char... chars) {
		if (null == value || value.length() == 0) {
			return value;
		}
		Arrays.sort(chars);
		int endIndex = value.length() - 1;
		boolean flag = containsChar(chars, value.charAt(endIndex));
		while (flag) {
			endIndex--;
			flag = containsChar(chars, value.charAt(endIndex));
		}
		if (0 >= endIndex) {
			return "";
		}
		return value.substring(0, endIndex + 1);
	}

	/**
	 * �Ƴ���ַ�ͷ�Ŀհ��ַ�
	 * 
	 * @param value
	 *            Ҫ�޼����ַ�
	 * @return �޼�����ַ�
	 */
	public static final String trimStart(final String value) {
		return trimStart(value, WhitespaceChars);
	}

	/**
	 * �Ƴ���ַ�ͷ���ض��ַ�
	 * 
	 * @param value
	 *            Ҫ�޼����ַ�
	 * @param chars
	 *            Ҫ�Ƴ���ַ�
	 * @return �޼�����ַ�
	 */
	public static String trimStart(final String value, final char... chars) {
		if (null == value || value.length() == 0) {
			return value;
		}
		Arrays.sort(chars);
		int startIndex = 0;
		boolean flag = containsChar(chars, value.charAt(startIndex));
		while (flag) {
			startIndex++;
			flag = containsChar(chars, value.charAt(startIndex));
		}
		if (startIndex >= value.length()) {
			return "";
		}
		return value.substring(startIndex);
	}

	private StringUtil() {
	}

	/**
	 * Convert an {@link InputStream} to String.
	 * 
	 * @param stream
	 *            the stream that contains data.
	 * @param encoding
	 *            the encoding of the data.
	 * @return the result string.
	 * @throws IOException
	 *             an I/O error occurred.
	 */
	public static String stringFromInputStream2(final InputStream stream,
			String encoding) throws IOException {
		if (null == stream) {
			throw new IllegalArgumentException("stream may not be null.");
		}
		if (TextUtils.isEmpty(encoding)) {
			encoding = System.getProperty("file.encoding", "utf-8");
		}
		String result;
		final InputStreamReader reader = new InputStreamReader(stream, encoding);
		final StringWriter writer = new StringWriter();
		final char[] buffer = new char[BUFFER_SIZE];
		int charRead = reader.read(buffer);
		while (charRead > 0) {
			writer.write(buffer, 0, charRead);
			charRead = reader.read(buffer);
		}
		result = writer.toString();
		return result;
	}

	/**
	 * Convert an {@link InputStream} to String.
	 * 
	 * @param stream
	 *            the stream that contains data.
	 * @return the result string.
	 * @throws IOException
	 *             an I/O error occurred.
	 */
	public static String stringFromInputStream2(final InputStream stream)
			throws IOException {
		return stringFromInputStream2(stream, "utf-8");
	}

	/**
	 * Tiny a given string, return a valid file name.
	 * 
	 * @param fileName
	 *            the file name to clean.
	 * @return the valid file name.
	 */
	public static String safeFileName(String fileName) {
		if (TextUtils.isEmpty(fileName)) {
			return "";
		}
		if (fileName.length() > MAX_PATH) {
			fileName = fileName.substring(0, MAX_PATH);
		}
		final Matcher matcher = ILLEGAL_FILE_NAME_CHARS.matcher(fileName);
		fileName = matcher.replaceAll("_");
		return fileName;
	}

	/**
	 * Html-encode the string.
	 * 
	 * @param s
	 *            the string to be encoded
	 * @return the encoded string
	 */
	public static String htmldecode(String text) {
		text = text.replaceAll("&amp;", "&");
		text = text.replaceAll("&lt;", "<");
		text = text.replaceAll("&gt;", ">");
		text = text.replaceAll("&quot;", "\"");
		text = text.replaceAll("&apos;", "\'");
		return text;
	}

	public static final String optString(final JSONObject object,
			final String key) {
		final Object o = object.opt(key);
		return !JSONObject.NULL.equals(o) ? o.toString() : null;
	}

	/**
	 * Error message presented when a user tries to treat an opaque URI as
	 * hierarchical.
	 */
	private static final String NOT_HIERARCHICAL = "This isn't a hierarchical URI.";

	/**
	 * Get query parameter from {@linkplain Uri} correctly.
	 * 
	 * @param uri
	 *            the {@linkplain Uri}
	 * @param key
	 *            the query key.
	 * @return the decode query value of with key, or {@code null} if not found.
	 */
	public static final String getQueryParameter(final Uri uri, final String key) {
		if (null == uri) {
			throw new NullPointerException("uri");
		}
		if (uri.isOpaque()) {
			throw new UnsupportedOperationException(NOT_HIERARCHICAL);
		}
		if (key == null) {
			throw new NullPointerException("key");
		}

		final String query = uri.getEncodedQuery();
		if (query == null) {
			return null;
		}

		final String encodedKey = Uri.encode(key, null);
		final int encodedKeyLength = encodedKey.length();

		int encodedKeySearchIndex = 0;
		final int encodedKeySearchEnd = query.length() - (encodedKeyLength + 1);

		while (encodedKeySearchIndex <= encodedKeySearchEnd) {
			final int keyIndex = query.indexOf(encodedKey,
					encodedKeySearchIndex);
			if (keyIndex == -1) {
				break;
			}
			final int equalsIndex = keyIndex + encodedKeyLength;
			if (equalsIndex >= query.length()) {
				break;
			}
			if (query.charAt(equalsIndex) != '=') {
				encodedKeySearchIndex = equalsIndex + 1;
				continue;
			}
			if (keyIndex == 0 || query.charAt(keyIndex - 1) == '&') {
				int end = query.indexOf('&', equalsIndex);
				if (end == -1) {
					end = query.length();
				}
				try {
					return URLDecoder.decode(
							query.substring(equalsIndex + 1, end), HTTP.UTF_8);
				} catch (final UnsupportedEncodingException e) {
					// We never get here.
					return null;
				}
			} else {
				encodedKeySearchIndex = equalsIndex + 1;
			}
		}
		return null;
	}

	/**
	 * Searches the query string for parameter values with the given key.
	 * 
	 * @param key
	 *            which will be encoded
	 * 
	 * @throws UnsupportedOperationException
	 *             if this isn't a hierarchical URI
	 * @throws NullPointerException
	 *             if key is null
	 * 
	 * @return a list of decoded values
	 */
	public List<String> getQueryParameters(Uri uri, String key) {
		if (null == uri) {
			throw new NullPointerException("uri");
		}
		if (uri.isOpaque()) {
			throw new UnsupportedOperationException(NOT_HIERARCHICAL);
		}

		String query = uri.getEncodedQuery();
		if (query == null) {
			return Collections.emptyList();
		}

		String encodedKey;
		try {
			encodedKey = URLEncoder.encode(key, HTTP.UTF_8);
		} catch (final UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}

		// Prepend query with "&" making the first parameter the same as the
		// rest.
		query = "&" + query;

		// Parameter prefix.
		final String prefix = "&" + encodedKey + "=";

		final ArrayList<String> values = new ArrayList<String>();

		int start = 0;
		final int length = query.length();
		while (start < length) {
			start = query.indexOf(prefix, start);

			if (start == -1) {
				// No more values.
				break;
			}

			// Move start to start of value.
			start += prefix.length();

			// Find end of value.
			int end = query.indexOf('&', start);
			if (end == -1) {
				end = query.length();
			}

			final String value = query.substring(start, end);
			try {
				values.add(URLDecoder.decode(value, HTTP.UTF_8));
			} catch (final UnsupportedEncodingException e) {
				throw new AssertionError(e);
			}

			start = end;
		}

		return Collections.unmodifiableList(values);
	}

	/**
	 * transform file bytes to string with unit
	 * 
	 * @param bytes
	 * @return
	 */
	public static String formatFileSize(final Context context, final long bytes) {
		return Formatter.formatFileSize(context, bytes);
	}

	/**
	 * Determine whether two objects are equal.
	 * 
	 * @param <T>
	 *            the type of the the objects.
	 * @param one
	 *            the first object.
	 * @param other
	 *            the second object.
	 */
	public static <T> boolean equals(final T one, final T other) {
		if (one != null && other != null) {
			return one.equals(other);
		}
		return one == other;
	}

	/**
	 * Determine whether two strings are equal.
	 * 
	 * @param one
	 *            the first object.
	 * @param other
	 *            the second object.
	 */
	public static boolean equals(final String one, final String other) {
		if (one != null && other != null) {
			return one.equals(other);
		}
		return one == other;
	}

	/**
	 * Determine whether two strings are equal, ignoring case.
	 * 
	 * @param one
	 *            the first object.
	 * @param other
	 *            the second object.
	 */
	public static boolean equalsIgnoreCase(final String one, final String other) {
		if (one != null && other != null) {
			return one.equalsIgnoreCase(other);
		}
		return one == other;
	}

	/**
	 * Retrieve string value of a {@linkplain CharSequence}.
	 * 
	 * @param value
	 *            the {@linkplain CharSequence} which value to retrieve.
	 * @return the value of the {@code value}.
	 */
	public static String valueOf(CharSequence value) {
		if (null == value) {
			return null;
		}
		return value.toString();
	}

	/**
	 * Check whether the email address is a valid email address.
	 * 
	 * @param email
	 *            the email address to check.
	 * @return true if the email address is valid, false otherwise.
	 */
	public static boolean isEmailValid(final String email) {
		if (TextUtils.isEmpty(email)) {
			return false;
		}
		final Matcher matcher = EMAIL_PATTERN.matcher(email);
		return matcher.matches();
	}

	public static boolean isPwdUnValid(final String pwd) {
		if (pwd.length() >= 6 && pwd.length() <= 16) {
			return false;
		}
		return true;
	}

	public static boolean isIdetityCardValid(final String idcard) {
		if (TextUtils.isEmpty(idcard)) {
			return false;
		}
		final Matcher matcher = ID_CARD.matcher(idcard);
		return matcher.matches();
	}

	public static boolean isPhoneNOValid(final String phone) {
		if (TextUtils.isEmpty(phone)) {
			return false;
		}
		final Matcher matcher = PHONE_NO.matcher(phone);
		return matcher.matches();
	}

	public static String urlEncode(String str) {
		StringBuffer buf = new StringBuffer();
		byte c;
		byte[] utfBuf;
		try {
			utfBuf = str.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			Log.d("", "URLEncode: Failed to get UTF-8 bytes from string.", e);
			utfBuf = str.getBytes();
		}
		for (int i = 0; i < utfBuf.length; i++) {
			c = utfBuf[i];
			if ((c >= '0' && c <= '9')
					|| (c >= 'A' && c <= 'Z')
					|| (c >= 'a' && c <= 'z')
					|| (c == '.' || c == '-' || c == '*' || c == '_')
					|| (c == ':' || c == '/' || c == '=' || c == '?' || c == '&')// ||c=='%')
			) {
				buf.append((char) c);
			} else {
				buf.append("%").append(Integer.toHexString((0x000000FF & c)));
			}
		}
		return buf.toString();
	}

	// ��֤�Ƿ��������ַ�
	public static boolean StringFilter(String str) {
		String regEx = "[`~!@#$%^&*+=|{}':;',\\[\\].<>/?~��@#��%����&*��������+|{}������������������������]";
		Pattern pattern = Pattern.compile(regEx);
		Matcher matcher = pattern.matcher(str);
		return matcher.find();
	}

	public static boolean isSocialNumber(String str) {
		String regEx = "^\\d{14}(\\d{1}|\\d{4}|(\\d{3}[xX]))$";
		Pattern pattern = Pattern.compile(regEx);
		Matcher matcher = pattern.matcher(str);

		return matcher.matches();
	}

	// ��֤��һ���ַ�����ĸ
	public static boolean StringFilterUsername(String str) {
		String regEx = "[a-zA-Z][a-zA-Z0-9|_]*";
		Pattern pattern = Pattern.compile(regEx);
		Matcher matcher = pattern.matcher(str);
		return matcher.matches();
	}

}
