package com.archermind.callstat.accounting;

import java.util.ArrayList;
import java.util.HashMap;

import com.archermind.callstat.ConfigManager;
import com.archermind.callstat.ILog;
import com.archermind.callstat.common.download.CacheFileManager;

public class MessageManager {
	public static final String num = "0123456789.-";
	public static final String pure_num_str = "0123456789";
	public static final String[] pure_num = new String[] { "0", "1", "2", "3",
			"4", "5", "6", "7", "8", "9" };
	public static final int TYPE_CONSUME_FEE = 0;
	public static final int TYPE_AVAIL_FEE = 1;
	public static final int TYPE_CONSUME_TRAFFIC = 2;
	public static final int TYPE_AVAIL_TRAFFIC = 3;

	public static HashMap<Integer, String> accounting_proc(String origin_msg,
			ConfigManager configManager, ArrayList<String> keywordList,
			int accounting_from_receiver_or_inbox) {
		ILog.LogD(MessageManager.class, "开始accounting_proc");
		//configManager.setInboxOrReceiver(accounting_from_receiver_or_inbox);
		HashMap<Integer, String> has = new HashMap<Integer, String>();
		origin_msg = origin_msg.replace(",", "，").replace(";", "；")
				.replace(" ", "");
		String[] exclude_key_words = getExcludeKeyword(keywordList);
		origin_msg = preProcessMsgByExcludeKeyWords(origin_msg,
				exclude_key_words);

		String[] loops_str = configManager.getSmsSplitRule().split("#");
		int match_loops = loops_str.length;

		for (int ll = 0; ll < match_loops; ll++) {
			char[] arr = loops_str[ll].toCharArray();
			String[] match_loops_sepretor = new String[arr.length + 2];
			for (int var = 0; var < arr.length; var++) {
				match_loops_sepretor[var] = arr[var] + "";
			}
			match_loops_sepretor[arr.length] = "\n" + "";
			match_loops_sepretor[arr.length + 1] = "\r" + "";

			String[] loopStr = MessageManager.splitMsgBySeperarotArray(
					origin_msg, match_loops_sepretor);
			for (int pp = 0; pp < loopStr.length; pp++) {
				for (int keyword_type = 0; keyword_type < keywordList.size(); keyword_type++) {

					String data = "-1000000";
					if (keyword_type == TYPE_CONSUME_FEE
							|| keyword_type == TYPE_AVAIL_FEE) {
						data = MessageManager.amountCost(loopStr[pp],
								keywordList.get(keyword_type), 0);
					}
					if (keyword_type == TYPE_CONSUME_TRAFFIC
							|| keyword_type == TYPE_AVAIL_TRAFFIC) {
						data = MessageManager.amountCost(loopStr[pp],
								keywordList.get(keyword_type), 1);
					}
					String print_type = "";
					switch(keyword_type){
					case 0:
						print_type = "已用话费";
						break;
					case 1:
						print_type = "可用话费";
						break;
					case 2:
						print_type = "已用流量";
						break;
					case 3:
						print_type = "可用流量";
						break;
					}
					//ILog.LogD(MessageManager.class, "result data is" + data);
					if (!data.equalsIgnoreCase("-1000000")) {
						CacheFileManager.getInstance().logAccounting("当前对出来的结果是："+data+"，类型是："+print_type+",参与匹配的短信片段是："+loopStr[pp]);
						if (has.get(keyword_type) == null) {
							has.put(keyword_type, data);
						}

					}

				}
			}

		}
		return has;
	}

	/**
	 * @param ArrayList
	 *            <String> keywordList
	 * @return String[] 需要删除后面数字的需要排除的关键字
	 **/
	public static String[] getExcludeKeyword(ArrayList<String> keywordList) {
		ArrayList<String> stringList = new ArrayList<String>();
		for (int i = 0; i < keywordList.size(); i++) {
			String str_key_word = keywordList.get(i);
			String[] str_key_word_split_by_douhao = str_key_word.split(",");
			for (int j = 0; j < str_key_word_split_by_douhao.length; j++) {
				if (str_key_word_split_by_douhao[j].contains("!")) {
					stringList
							.add(str_key_word_split_by_douhao[j].substring(1));
				}
			}

		}
		if (stringList.size() == 0) {
			return null;
		}
		String[] ret_String_Array = new String[stringList.size()];
		for (int i = 0; i < ret_String_Array.length; i++) {
			ret_String_Array[i] = stringList.get(i);
		}
		return ret_String_Array;
	}

	/**
	 * @param String
	 *            origin_msg //待预处理的原始短信息
	 * @param String
	 *            [] exclude_key_words
	 * @return String process_msg //预处理之后的短信息
	 **/
	public static String preProcessMsgByExcludeKeyWords(String origin_msg,
			String[] exclude_key_words) {
		if (exclude_key_words == null) {
			return origin_msg;
		}
		String msg = origin_msg;
		for (int i = 0; i < exclude_key_words.length; i++) {
			int index = msg.indexOf(exclude_key_words[i]);
			if (index != -1) {// 说明msg中含有排除关键字
				String msg_suffix = msg.substring(0, index);
				String msg_appendix = msg.substring(index);
				String ret = amountExCludeKeyWord(msg_appendix,
						exclude_key_words[i]);
				if (!ret.equalsIgnoreCase("-1000000")) {
					msg_appendix = msg_appendix.replaceFirst(ret, "");
				}
				msg = msg_suffix + msg_appendix;
			}
		}
		return msg;
	}

	/**
	 * @param String
	 *            msg //待预处理的原始短信息
	 * @return String process_msg //预处理之后的短信息(将所有数字变成空格之后的信息)
	 **/
	public static String replacePureNumInStringToNull(String msg) {
		String msg_proc = msg;
		for (int i = 0; i < pure_num.length; i++) {
			msg_proc = msg_proc.replace(pure_num[i], "");
		}
		return msg_proc;
	}

	// 寻找排除关键字之后存在的数字组成的字符串
	// 待匹配的短信段 待匹配的单个排除关键字
	public static String amountExCludeKeyWord(String msg, String str) {
		int index = msg.indexOf(str);
		if (index != -1) {
			StringBuffer strb = new StringBuffer();
			char[] message = msg.toCharArray();
			for (int i = index + str.length(); i < msg.length(); i++) {
				if (pure_num_str.indexOf(String.valueOf(message[i])) != -1) {
					for (int j = i; j < msg.length(); j++) {
						if (pure_num_str.indexOf(String.valueOf(message[j])) != -1) {
							strb.append(message[j]);
						} else {
							return strb.toString();
						}
					}

				}
			}
		}
		return "-1000000";
	}

	/**
	 * 根据消息及关键字查询话费信息
	 * 
	 * @param msg
	 *            （按回车、句号、逗号分割之后得到的msg） 传进的消息
	 * @param str
	 *            同一类的关键字集合组成的串（逗号分割
	 *            如可用上网流量，可用(流量#上网)---这个地方可用是主关键字，流量、上网是辅关键字,主关键字存在
	 *            ，辅关键字也存在的情况需要两者都匹配上才算匹配成功）
	 * @param type
	 *            type =0 金额 type =1 表示流量
	 * @return 
	 *         所获得的信息(由于要考虑流量单位既有M又有K的时候以K为准，所以这里返回字符串，是流量的时候返回***K或者***M,其他时返回**
	 *         *);
	 */
	public static String amountCost(String msg, String str, int type) {
		// Log.e(TestAccountingActivity.class.toString(),
		// "amountCost msg="+msg+"   String"+str);
		try {
			str = str.replace("，", ",");// 将str里面的中文逗号替换成英文的逗号
			String key[] = str.split(",");
			abc: for (int k = 0; k < key.length; k++) {
				String[] assist_key_word = null;
				if (key[k].contains("(") && key[k].contains(")")) { // 如果key[k]包含左右括号，说明这个关键字含有辅助关键字。
					assist_key_word = key[k].substring(key[k].indexOf("(") + 1,
							key[k].indexOf(")")).split("#");
					key[k] = key[k].substring(0, key[k].indexOf("("));
				}
				if (assist_key_word != null) {
					// ILog.LogE(MessageManager.class,
					// "主关键字为："+key[k]+"辅助关键字的个数为："+assist_key_word.length+"待匹配的短信为"+msg);
				} else {
					// ILog.LogE(MessageManager.class,
					// "主关键字为："+key[k]+"没有辅助关键字"+"待匹配的短信为"+msg);
				}

				int index = msg.indexOf(key[k]);
				boolean msg_contains_assis_key_word = false;
				if (assist_key_word != null) {
					for (int kk = 0; kk < assist_key_word.length; kk++) {
						if (msg.contains(assist_key_word[kk])) {
							msg_contains_assis_key_word = true;
							break;
						}
					}
				}
				if (index != -1) {
					if (assist_key_word != null
							&& msg_contains_assis_key_word == false) {
						continue abc;
					}

					StringBuffer strb = new StringBuffer();
					char[] message = msg.toCharArray();
					for (int i = index + key[k].length(); i < msg.length(); i++) {
						if (num.indexOf(String.valueOf(message[i])) != -1) {
							for (int j = i; j < msg.length(); j++) {
								if (num.indexOf(String.valueOf(message[j])) != -1) {
									strb.append(message[j]);
								} else {
									if (message[j] == ' ') {
										continue;
									}

									if (type == 0) {
										if (message[j] == '条'
												|| message[j] == '分') {
											return "-1000000";
										} else {
											return strb.toString();
										}
									} else if (type == 1) {
										if (message[j] == 'm'
												|| message[j] == 'M') {

											strb.append(message[j]);

											return strb.toString();
										} else if (message[j] == 'k'
												|| message[j] == 'K') {

											strb.append(message[j]);

											return strb.toString();
										} else if (message[j] == 'G'
												|| message[j] == 'g') {

											strb.append(message[j]);

											return strb.toString();
										} else if (message[j] == 'T'
												|| message[j] == 't') {
											strb.append(message[j]);
											return strb.toString();
										} else {
											return "-1000000";
										}
									}

								}
							}
							if (!strb.toString().equalsIgnoreCase("")) {
								return strb.toString();
							}
						}
					}
				}
			}

			return "-1000000";
		} catch (Exception e) {
			return "-1000000";
		}

	}

	public static boolean isPureNumber(String ObjString) {
		boolean retVal = true;
		char[] obj = ObjString.toCharArray();
		for (int i = 0; i < ObjString.length(); i++) {
			int index = num.indexOf(obj[i]);
			if (index == -1) {
				retVal = false;
				break;
			}
		}
		return retVal;
	}

	public static String[] splitMsgBySeperarotArray(String msg,
			String[] seperator) {
		if (msg == "" || seperator == null) {
			return null;
		}
		String rep = msg;
		for (int i = 0; i < seperator.length; i++) {
			rep = rep.replace(seperator[i], "#");
		}
		return rep.split("#");
	}
}
