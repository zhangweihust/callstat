package com.archermind.callstat.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IsMobileUtil {
	public static boolean isMobileNo(String mobiles) {
		Pattern p = Pattern.compile("^1[3|4|5|8][0-9]\\d{4,8}$");
		Matcher m = p.matcher(mobiles);
		return m.matches();
	}

	public static boolean isMobileNoAll(String mobiles) {
		Pattern p = Pattern.compile("^1[3|4|5|8][0-9]\\d{8}$");
		Matcher m = p.matcher(mobiles);
		return m.matches();
	}

}