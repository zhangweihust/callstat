package com.android.callstat.monitor;

import java.text.DecimalFormat;
import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.callstat.ConfigManager;
import com.android.callstat.accounting.ReconciliationInfo;
import com.android.callstat.common.CallStatUtils;
import com.android.callstat.common.database.CallStatDatabase;
import com.android.callstat.common.download.CacheFileManager;
import com.android.callstat.monitor.bean.CallLog;

public class PhonebillCaculateThread extends Thread {
	static {
		System.loadLibrary("BillCaculate");
	}
	String log = "debug";

	public PhonebillCaculateThread(Context context, Handler handler) {
		this.context = context;
		SendHandler = new Handler();
		haveSolution = false;
		equationNull = false;
		setReceiveHandler(handler);
		SmsPrice = new ConfigManager(context).getMsgUnitPrice();
	}

	private Handler SendHandler = null;
	public Handler ReceiveHandler = null;

	public void setReceiveHandler(Handler handler) {
		ReceiveHandler = handler;
	}

	public static double AccountConsume = 0;// 消费多少的记录变量
	public static double AccountBalance = 0;// 余额多少的记录变量
	public static int Dimension = 14;// 方程有几元这个值就是几
	private Context context;
	private boolean haveSolution = false; // 方程组有解的标志变量
	private boolean equationNull = false; // 方程组为空的标志变量
	public static double SmsPrice;// 短信的资费标准暂定为0.1元/条

	public static double local_max = 0.66;// 本地主叫的最大资费标准暂定为0.66元/分钟
	public static double long_distance_max = 0.77;// 本地拨打长途的最大资费标准暂定为0.77元/分钟
	public static double roaming_max = 0.66;// 漫游的最大资费暂定为0.66元/分钟
	public static double unknown_max = 3.0;// 未知号码的最大资费标准暂定为3.0元/分钟
	public static double ip_max = 1.1;// ip拨号的最大资费暂定为1.1元/分钟
	public static double short_max = 0.5;// 短号的最大资费暂定为0.5元/分钟
	public static double sms_max = 0.15;// 短信最大资费暂定为0.15元/条
	public static double traffic_max = 1.0 / 1024 / 1024;// 超过部分的GPRS资费暂定为1元/M
	public static double delta1_max = 10000;// 对于所加的几个delta的范围，暂时不做限制所以最大值取的尽可能大
	public static double delta2_max = 10000;
	public static double delta3_max = 10000;
	public static double delta4_max = 10000;
	public static double delta5_max = 10000;
	public static double delta6_max = 10000;

	@Override
	public void run() {
		String logStr = "进入话费实时预测模块.....\n";
		long ms = System.currentTimeMillis();
		String str1 = CallStatUtils.getNowTime();

		ArrayList<ReconciliationInfo> List = CallStatDatabase.getInstance(
				context).getReconciliationInfoList();
		if (List == null || List.isEmpty()) {
			Log.e(log, "获取历史方程组为空!");
			CacheFileManager.getInstance().logAccounting("获取历史方程组为空!");
			equationNull = true;
			SendHandler.post(runable);
			return;
		}
		Log.e(log, "当前时间为	" + str1);
		Log.e(log, "获取历史方程组成功!获取的历史方程记录条数为" + List.size());
		Log.e(log, "计算过程开始.............................");
		CacheFileManager.getInstance().logAccounting(
				"当前时间为	" + str1 + "\n" + "获取历史方程组成功!获取的历史方程记录条数为" + List.size()
						+ "\n" + "计算过程开始.............................");
		ArrayList<String> ListA = new ArrayList<String>();
		ArrayList<String> ListA_ext = new ArrayList<String>();
		int i = 0;
		int rA = 0;

		CallStatDatabase callStatDataBase = CallStatDatabase
				.getInstance(context);
		ConfigManager configManager = new ConfigManager(context);

		do {
			ReconciliationInfo recoInfo = List.get(i);
			double[] alpha_i = new double[] {
					recoInfo.getThisLocalityDialingTimes(), // 1
					recoInfo.getLongDistanceDialingTimes(), // 2
					recoInfo.getRoamingTimes(),// 3
					recoInfo.getUnkonwDialingTimes(), // 4
					recoInfo.getIPDialingTimes(), // 5
					recoInfo.getShortDialingTimes(), // 6
					recoInfo.getSendSmsNum(),// 7
					recoInfo.getTrafficData(), // 8
					recoInfo.getCoeffDelta1(), // 9
					recoInfo.getCoeffDelta2(), // 10
					recoInfo.getCoeffDelta3(), // 11
					recoInfo.getCoeffDelta4(), // 12
					recoInfo.getCoeffDelta5(), // 13
					recoInfo.getCoeffDelta6() }; // 14
			double[] fi = new double[] { recoInfo.getDifference()
			/*- recoInfo.getSendSmsNum() * SmsPrice*/};
			i++;

			String str_alpha_i = "";
			for (int j = 0; j < Dimension; j++) {
				str_alpha_i += alpha_i[j];
				str_alpha_i += "	";
			}
			Log.e(log, "第" + i + "条记录为	" + str_alpha_i);
			Log.e(log, "第" + i + "条记录的常数项为	" + fi[0]);

			logStr += "第" + i + "条记录为	" + str_alpha_i + "\n" + "第" + i
					+ "条记录的常数项为	" + fi[0] + "\n";

			double[] alpha_i_tmp = new double[Dimension];
			for (int j = 0; j < Dimension; j++) {
				alpha_i_tmp[j] = alpha_i[j];
			}

			if (Rank(alpha_i_tmp, 1, Dimension) == 0) {
				Log.e(log, "第" + i + "条记录的系数全为0，故舍弃掉，进入下一条记录的提取");
				logStr += "第" + i + "条记录的系数全为0，故舍弃掉，进入下一条记录的提取\n";
				continue;
			}
			if (fi[0] < 0) {
				Log.e(log,
						"第"
								+ i
								+ "条记录对账消费额度小于按正常资费算得的短信消费额度，导致方程右边的常数项为负，舍弃掉，进入下一条记录的提取");
				logStr += "第"
						+ i
						+ "条记录对账消费额度小于按正常资费算得的短信消费额度，导致方程右边的常数项为负，舍弃掉，进入下一条记录的提取\n";
				continue;
			}
			/*
			 * if (alpha_i[0] * local_max + alpha_i[1] * long_distance_max +
			 * alpha_i[2] * called_max + alpha_i[3] * unknown_max + alpha_i[4] *
			 * ip_max + alpha_i[5] * short_max + alpha_i[6] traffic_max < fi[0])
			 * { Log.e(log, "第" + i + "条记录按最大资费标准计算消费应为：" + alpha_i[0] + "*" +
			 * local_max + "+" + alpha_i[1] + "*" + long_distance_max + "+" +
			 * alpha_i[2] + "*" + called_max + "+" + alpha_i[3] + "*" +
			 * unknown_max + "+" + alpha_i[4] + "*" + ip_max + "+" + alpha_i[5]
			 * + "*" + short_max + "+" + alpha_i[6] + "*" + traffic_max + "<" +
			 * fi[0] + ",故舍弃掉，进入下一条记录的提取。"); logStr += "第" + i +
			 * "条记录按最大资费标准计算消费应为：" + alpha_i[0] + "*" + local_max + "+" +
			 * alpha_i[1] + "*" + long_distance_max + "+" + alpha_i[2] + "*" +
			 * called_max + "+" + alpha_i[3] + "*" + unknown_max + "+" +
			 * alpha_i[4] + "*" + ip_max + "+" + alpha_i[5] + "*" + short_max +
			 * "+" + alpha_i[6] + "*" + traffic_max + "<" + fi[0] +
			 * ",故舍弃掉，进入下一条记录的提取。\n"; continue; }
			 */

			// 每来一个合乎要求的方程，将方程的常数项放到队列ListA_ext里面去。
			double[] A_ext = new double[ListA_ext.size() + 1];
			for (int j = 0; j < ListA_ext.size(); j++) {
				A_ext[j] = Double.parseDouble(ListA_ext.get(j));
			}
			for (int j = 0; j < 1; j++) {
				A_ext[ListA_ext.size() + j] = fi[j];
			}

			Log.e(log, "方程组的常数列向量为：");
			logStr += "方程组的常数列向量为:\n";
			String A_extStr = "";
			for (int j = 0; j < A_ext.length; j++) {
				Log.e(log, A_ext[j] + "");
				A_extStr += A_ext[j] + "\n";
			}
			logStr += A_extStr;
			double[] A_alphai = new double[ListA.size() + Dimension];
			for (int j = 0; j < ListA.size(); j++) {
				A_alphai[j] = Double.parseDouble(ListA.get(j));
			}
			for (int j = 0; j < Dimension; j++) {
				A_alphai[ListA.size() + j] = alpha_i[j];
			}

			Log.e(log, "方程组的系数矩阵为：");
			logStr += "方程组的系数矩阵为：\n";
			String tmp1Str = "";
			for (int j = 0; j < rA + 1; j++) {
				String tmp1 = "";
				for (int k = 0; k < Dimension; k++) {
					tmp1 += A_alphai[j * Dimension + k];
					tmp1 += "	";
				}
				Log.e(log, tmp1);
				tmp1Str += tmp1 + "\n";
			}
			logStr += tmp1Str;
			double[] A_alphai_tmp = new double[ListA.size() + Dimension];
			for (int j = 0; j < A_alphai_tmp.length; j++) {
				A_alphai_tmp[j] = A_alphai[j];
			}

			if (!(Rank(A_alphai_tmp, rA + 1, Dimension) == rA + 1)) {
				Log.e(log, "第" + i + "条记录和历史记录线性相关，故舍弃掉,进入下一条记录的提取");
				logStr += "第" + i + "条记录和历史记录线性相关，故舍弃掉,进入下一条记录的提取\n";
				continue;
			}
			int[] latelyTime = callStatDataBase.getLatelyLocalCallTime(
					configManager.getLastCheckTime(), ms);

			if (CallLogObserver.numType == CallLog.CALL_LOCAL) {
				Log.e(log, "本次通话类型为本地市话，通话时长为"
						+ CallLogObserver.currentDuration + "分钟");
				logStr += "本次通话类型为本地市话，通话时长为" + CallLogObserver.currentDuration
						+ "分钟\n";
				latelyTime[0] = latelyTime[0] + CallLogObserver.currentDuration;
			} else if (CallLogObserver.numType == CallLog.CALL_LONG_DISTANCE) {
				Log.e(log, "本次通话类型为本地长途，通话时长为"
						+ CallLogObserver.currentDuration + "分钟");
				logStr += "本次通话类型为本地长途，通话时长为" + CallLogObserver.currentDuration
						+ "分钟\n";
				latelyTime[1] = latelyTime[1] + CallLogObserver.currentDuration;
			} else if (CallLogObserver.numType == CallLog.CALL_ROAMING) {
				Log.e(log, "本次通话类型为漫游，通话时长为" + CallLogObserver.currentDuration
						+ "分钟");
				logStr += "本次通话类型为漫游，通话时长为" + CallLogObserver.currentDuration
						+ "分钟\n";
				latelyTime[2] = latelyTime[2] + CallLogObserver.currentDuration;
			} else if (CallLogObserver.numType == CallLog.CALL_UNKONW) {
				Log.e(log, "本次通话类型为未知电话，通话时长为"
						+ CallLogObserver.currentDuration + "分钟");
				logStr += "本次通话类型为未知电话，通话时长为" + CallLogObserver.currentDuration
						+ "分钟\n";
				latelyTime[3] = latelyTime[3] + CallLogObserver.currentDuration;
			} else if (CallLogObserver.numType == CallLog.CALL_IP) {
				Log.e(log, "本次通话类型为IP电话，通话时长为"
						+ CallLogObserver.currentDuration + "分钟");
				logStr += "本次通话类型为IP电话，通话时长为" + CallLogObserver.currentDuration
						+ "分钟\n";
				latelyTime[4] = latelyTime[4] + CallLogObserver.currentDuration;
			} else if (CallLogObserver.numType == CallLog.CALL_SHORT) {
				Log.e(log, "本次通话类型为短号，通话时长为" + CallLogObserver.currentDuration
						+ "分钟");
				logStr += "本次通话类型为短号，通话时长为" + CallLogObserver.currentDuration
						+ "分钟\n";
				latelyTime[5] = latelyTime[5] + CallLogObserver.currentDuration;
			}

			int deltas[] = { 0, 0, 0, 0, 0, 0 };

			if (configManager.getLastCheckHasYeTime() != -1) {// 上次余额成功对出的时刻存在
				String lastCheck_time = CallStatUtils
						.changeMilliSeconds2YearMonthDayHourMin(configManager
								.getLastCheckHasYeTime());
				String hour_minute = lastCheck_time.substring(8,
						lastCheck_time.length());
				int delta_begin_index = CallStatUtils
						.which_delta_begin_to_add(hour_minute);

				long s = (System.currentTimeMillis() - configManager
						.getLastCheckHasYeTime()) / 1000;
				long steps = s / (4 * 3600);// 求出时间差跨了几个4小时，若1个就没有跨，则deltas数组每个元素均为0；

				// 求所加各个delta对应的各个系数的计算方法
				for (int ii = 1; ii <= steps; ii++) {
					if (ii % 6 == 1) {
						deltas[delta_begin_index % 6]++;
					} else if (ii % 6 == 2) {
						deltas[(delta_begin_index + 1) % 6]++;
					} else if (ii % 6 == 3) {
						deltas[(delta_begin_index + 2) % 6]++;
					} else if (ii % 6 == 4) {
						deltas[(delta_begin_index + 3) % 6]++;
					} else if (ii % 6 == 5) {
						deltas[(delta_begin_index + 4) % 6]++;
					} else if (ii % 6 == 0) {
						deltas[(delta_begin_index + 5) % 6]++;
					}
				}
			}

			double[] beta = new double[] { (double) latelyTime[0], // 本地主叫 1
					(double) latelyTime[1], // 本地长途 2
					(double) latelyTime[2], // 漫游 3
					(double) latelyTime[3], // 未知号码 4
					(double) latelyTime[4], // IP拨号 5
					(double) latelyTime[5], // 短号 6
					(double) CallStatUtils
							.getPrevReconcilitaionToNowSmsUsed(configManager),// 自上次对帐到现在所消费的短信
																				// 7
					(double) CallStatUtils
							.getPrevReconcilitionToNowGprsUsed(configManager), // 自上次对帐到现在所消费的GPRS流量
																				// 8
					(double) deltas[0], // delta_1 9
					(double) deltas[1], // delta_2 10
					(double) deltas[2], // delta_3 11
					(double) deltas[3], // delta_4 12
					(double) deltas[4], // delta_5 13
					(double) deltas[5] // delta_6 14
			};
			Log.e(log, "待求表达式的系数组成的行向量为：");
			logStr += "待求表达式的系数组成的行向量为：\n";
			String str_beta = "";
			for (int j = 0; j < Dimension; j++) {
				str_beta += beta[j];
				str_beta += "	";
			}
			Log.e(log, str_beta);
			logStr += str_beta + "\n";
			double[] A_alphai_beta = new double[ListA.size() + Dimension
					+ Dimension];
			for (int j = 0; j < ListA.size() + Dimension; j++) {
				A_alphai_beta[j] = A_alphai[j];
			}
			for (int j = 0; j < Dimension; j++) {
				A_alphai_beta[ListA.size() + Dimension + j] = beta[j];
			}
			Log.e(log, "待求表达式和方程组组成的系数矩阵为:");
			logStr += "待求表达式和方程组组成的系数矩阵为:\n";
			String ttmpStr = "";
			for (int j = 0; j < rA + 2; j++) {
				String ttmp = "";
				for (int k = 0; k < Dimension; k++) {
					ttmp += A_alphai_beta[j * Dimension + k];
					ttmp += "	";
				}
				Log.e(log, ttmp);
				ttmpStr += ttmp + "\n";
			}
			logStr += ttmpStr;
			double[] A_alphai_beta_tmp = new double[ListA.size() + Dimension
					+ Dimension];
			for (int j = 0; j < A_alphai_beta_tmp.length; j++) {
				A_alphai_beta_tmp[j] = A_alphai_beta[j];
			}

			if (Rank(A_alphai_beta_tmp, rA + 2, Dimension) == rA + 1
					|| Rank(A_alphai_tmp, rA + 1, Dimension) == Dimension) {

				Log.e(log, "待求表达式和方程组组成新的方程组的秩为" + (rA + 1));
				Log.e(log, "待求表达式可由方程组线性表示，故解存在.");
				logStr += "待求表达式和方程组组成新的方程组的秩为" + (rA + 1) + "\n"
						+ "待求表达式可由方程组线性表示，故解存在.\n";

				double[] double_beta = new double[Dimension];
				for (int j = 0; j < Dimension; j++) {
					double_beta[j] = beta[j];
				}

				// //////////////
				// 原先解的合法性校验部分是根据求出来的各个资费（若能求出），则判断是否在合理范围内完成的，现在用来实现计算资费的功能////////////////////
				double[][] verify_matrix = new double[rA + 1][Dimension];
				for (int l = 0; l < rA + 1; l++) {
					for (int m = 0; m < Dimension; m++) {
						verify_matrix[l][m] = A_alphai_beta[l * Dimension + m];
					}
				}
				double actual_dimension = 0;
				ArrayList<Integer> column_index = new ArrayList<Integer>();
				for (int m = 0; m < Dimension; m++) {
					for (int l = 0; l < rA + 1; l++) {
						if (Math.abs(verify_matrix[l][m]) > 1e-10) {
							column_index.add(new Integer(m));
							actual_dimension++;
							break;
						}
					}
				}
				Log.e(log, "线性方程组的实际元数是：" + actual_dimension);
				logStr += "线性方程组的实际元数是：" + actual_dimension + "\n";
				// 如果方程组实际元数已经和矩阵的秩相等或者是历史方程组成的矩阵已达到维数满秩，即秩等于Dimension，则各个元是必定有解且可求得的，用下述计算方法求出。
				if (actual_dimension == rA + 1
						|| Rank(A_alphai_tmp, rA + 1, Dimension) == Dimension) {
					Log.e(log, "可求出几个元的费率，计算过程开始.....");
					logStr += "可求出几个元的费率，计算过程开始.....\n";
					double[] inv_able_matrix = new double[(rA + 1) * (rA + 1)];
					int j = 0;
					int size = column_index.size();
					for (int l = 0; l < rA + 1; l++) {
						for (int c = 0; c < size; c++) {
							int m = column_index.get(c).intValue();
							inv_able_matrix[j++] = verify_matrix[l][m];
						}
					}
					double[] Verify_X = MultiMatrix(
							Inverse(inv_able_matrix, rA + 1), rA + 1, A_ext,
							rA + 1, 1);
					double[] max_Verify_x = new double[Verify_X.length];
					int max_Verify_index = 0;
					for (int c = 0; c < size; c++) {
						int m = column_index.get(c).intValue();
						switch (m) {
						case 0:
							max_Verify_x[max_Verify_index++] = local_max;
							if (Verify_X[max_Verify_index - 1] >= 0
									&& Verify_X[max_Verify_index - 1] <= local_max) {
								logStr += "计算出本地市话费率为:"
										+ Verify_X[max_Verify_index - 1]
										+ "元/分钟";
								configManager
										.setLocalRates((float) Verify_X[max_Verify_index - 1]);
							}
							break;
						case 1:
							max_Verify_x[max_Verify_index++] = long_distance_max;
							if (Verify_X[max_Verify_index - 1] >= 0
									&& Verify_X[max_Verify_index - 1] <= long_distance_max) {
								logStr += "计算出本地长途费率为:"
										+ Verify_X[max_Verify_index - 1]
										+ "元/分钟";
								configManager
										.setLongRates((float) Verify_X[max_Verify_index - 1]);
							}
							break;
						case 2:
							max_Verify_x[max_Verify_index++] = roaming_max;
							if (Verify_X[max_Verify_index - 1] >= 0
									&& Verify_X[max_Verify_index - 1] <= roaming_max) {
								logStr += "计算出漫游通话费率为:"
										+ Verify_X[max_Verify_index - 1]
										+ "元/分钟";
								configManager
										.setRoamingRates((float) Verify_X[max_Verify_index - 1]);
							}
							break;
						case 3:
							max_Verify_x[max_Verify_index++] = unknown_max;
							break;
						case 4:
							max_Verify_x[max_Verify_index++] = ip_max;
							if (Verify_X[max_Verify_index - 1] >= 0
									&& Verify_X[max_Verify_index - 1] <= ip_max) {
								logStr += "计算出ip通话资费为:"
										+ Verify_X[max_Verify_index - 1]
										+ "元/分钟";
								configManager
										.setRatesIP((float) Verify_X[max_Verify_index - 1]);
							}
							break;
						case 5:
							max_Verify_x[max_Verify_index++] = short_max;
							if (Verify_X[max_Verify_index - 1] >= 0
									&& Verify_X[max_Verify_index - 1] <= short_max) {
								logStr += "计算出短号资费为:"
										+ Verify_X[max_Verify_index - 1]
										+ "元/分钟";
								configManager
										.setRatesShort((float) Verify_X[max_Verify_index - 1]);
							}
							break;
						case 6:
							max_Verify_x[max_Verify_index++] = sms_max;
							if (Verify_X[max_Verify_index - 1] >= 0
									&& Verify_X[max_Verify_index - 1] <= sms_max) {
								logStr += "计算出短信资费为:"
										+ Verify_X[max_Verify_index - 1]
										+ "元/条";
								configManager
										.setRatesSms((float) Verify_X[max_Verify_index - 1]);
							}
							break;
						case 7:
							max_Verify_x[max_Verify_index++] = traffic_max;
							break;
						case 8:
							max_Verify_x[max_Verify_index++] = delta1_max;
							break;
						case 9:
							max_Verify_x[max_Verify_index++] = delta2_max;
							break;
						case 10:
							max_Verify_x[max_Verify_index++] = delta3_max;
							break;
						case 11:
							max_Verify_x[max_Verify_index++] = delta4_max;
							break;
						case 12:
							max_Verify_x[max_Verify_index++] = delta5_max;
							break;
						case 13:
							max_Verify_x[max_Verify_index++] = delta6_max;
							break;
						default:
							break;
						}
					}

					boolean solution_not_valid = false;
					int kk = 0;
					for (kk = 0; kk < Verify_X.length; kk++) {
						if (Verify_X[kk] < 0 || Verify_X[kk] > max_Verify_x[kk]) {
							solution_not_valid = true;
							break;
						}
					}
					if (solution_not_valid) {
						Log.e(log, "方程组有数学解且待求的几个元可以求出，但是由于解出来资费不符合实际Verify_X["
								+ kk + "]=" + Verify_X[kk]
								+ "所以仍然发送无解消息给上层显示模块");
						logStr += "方程组有数学解且待求的几个元可以求出，但是由于解出来资费不符合实际Verify_X["
								+ kk + "]=" + Verify_X[kk]
								+ "所以仍然发送无解消息给上层显示模块\n";
						haveSolution = false;
						break;
					}
				}
				// ////////////////////////////////////// 和上面的部分一起组成实现资费计算的部分
				// //////////////////////////////////////////////////////////

				double[] res1 = MultiMatrix(double_beta, 1,
						Transform(A_alphai, rA + 1, Dimension), Dimension,
						rA + 1);
				Log.e(log, "计算出的beta*A'为:");
				String str_res1 = "";
				for (int j = 0; j < rA + 1; j++) {
					str_res1 += res1[j];
					str_res1 += "	";
				}
				Log.e(log, str_res1);

				Log.e(log, "计算出的inv(A*A')为:");
				double[] res2 = Inverse(
						MultiMatrix(A_alphai, rA + 1,
								Transform(A_alphai, rA + 1, Dimension),
								Dimension, rA + 1), rA + 1);
				String str_res2Str = "";
				for (int j = 0; j < rA + 1; j++) {
					String str_res2 = "";
					for (int k = 0; k < rA + 1; k++) {
						str_res2 += res2[j * (rA + 1) + k];
						str_res2 += "	";
					}
					Log.e(log, str_res2);
					str_res2Str += str_res2 + "\n";
				}
				logStr += "计算出的beta*A'为:" + "\n" + str_res1 + "\n"
						+ "计算出的inv(A*A')为:" + "\n" + str_res2Str
						+ "解出的组合系数为:\n";
				double[] coeff = MultiMatrix(res1, 1, res2, rA + 1, rA + 1);
				Log.e(log, "解出的组合系数为:");
				String str_coeff = "";
				for (int j = 0; j < rA + 1; j++) {
					str_coeff += coeff[j];
					str_coeff += "	";
				}
				Log.e(log, str_coeff);
				logStr += str_coeff + "\n";
				Log.e(log, "方程右边的常数列向量为:");
				logStr += "方程右边的常数列向量为:" + "\n";
				for (int j = 0; j < A_ext.length; j++) {
					Log.e(log, A_ext[j] + "");
					logStr += A_ext[j] + "" + "\n";
				}

				double returnValue = MultiMatrix(coeff, 1, A_ext, rA + 1, 1)[0];

				if (returnValue < 0) {
					Log.e(log, "虽然方程组有数学解，但是由于解出来的消费额度为" + returnValue
							+ "小于0，所以仍然发送无解消息给上层显示模块");
					logStr += "虽然方程组有数学解，但是由于解出来的消费额度为" + returnValue
							+ "小于0，所以仍然发送无解消息给上层显示模块\n";
					haveSolution = false;
					break;
				}

				/*
				 * 原先的7元计算中需要用以下部分代码做一个解的合法性校验. double max_consume = beta[0] *
				 * local_max + beta[1] long_distance_max + beta[2] * called_max
				 * + beta[3] unknown_max + beta[4] * ip_max + beta[5] *
				 * short_max + beta[6] * traffic_max; Log.e(log,
				 * "消费方程按最大资费标准计算消费应为：" + beta[0] + "*" + local_max + "+" +
				 * beta[1] + "*" + long_distance_max + "+" + beta[2] + "*" +
				 * called_max + "+" + beta[3] + "*" + unknown_max + "+" +
				 * beta[4] + "*" + ip_max + "+" + beta[5] + "*" + short_max +
				 * "+" + beta[6] + "*" + traffic_max + "==" + max_consume);
				 * logStr += "消费方程按最大资费标准计算消费应为：" + beta[0] + "*" + local_max +
				 * "+" + beta[1] + "*" + long_distance_max + "+" + beta[2] + "*"
				 * + called_max + "+" + beta[3] + "*" + unknown_max + "+" +
				 * beta[4] + "*" + ip_max + "+" + beta[5] + "*" + short_max +
				 * "+" + beta[6] + "*" + traffic_max + "==" + max_consume +
				 * "\n";
				 * 
				 * if (returnValue > max_consume) { Log.e(log,
				 * "虽然方程组有数学解，但是由于解出来的消费额度为" + returnValue +
				 * "大于按最大资费求得的消费，所以仍然发送无解消息给上层显示模块"); logStr +=
				 * "虽然方程组有数学解，但是由于解出来的消费额度为" + returnValue +
				 * "大于按最大资费求得的消费，所以仍然发送无解消息给上层显示模块\n"; haveSolution = false;
				 * break; }
				 */

				AccountConsume = returnValue;

				Log.e(log, "计算出来的自上次对账到此刻的消费额度为:" + AccountConsume + "元。");

				logStr += "计算出来的自上次对账到此刻的消费额度为:" + AccountConsume + "元。\n";

				if (configManager.getFeesRemian() != 100000) {
					AccountBalance = configManager.getFeesRemian()
							- AccountConsume;

					DecimalFormat df = new DecimalFormat("#.##");
					AccountBalance = Double.parseDouble(df
							.format(AccountBalance));
					float lastBalance = configManager
							.getCalculateFeeAvailable();
					if (lastBalance < AccountBalance) {
						Log.e(log, "lastGuess:" + lastBalance + "now guess:"
								+ AccountBalance + "本次预估值大于上次预估值，故不显示预估值。");
						logStr += "lastGuess:" + lastBalance + "now guess:"
								+ AccountBalance + "本次预估值大于上次预估值，故不显示预估值。\n";
						haveSolution = false;
						break;
					} else {
						configManager
								.setCalculateFeeAvailable((float) AccountBalance);
						// CacheFileManager.getInstance().logAccounting(
						// "本次预估余额为:" + AccountBalance + "元.\n");
						haveSolution = true;
						break;
					}

				} else {
					Log.e(log, "上一次对账的余额未知，故根据此次消费的额度无法预测出话费余额！");
					logStr += "上一次对账的余额未知，故根据此次消费的额度无法预测出话费余额！\n";
				}
				break;

			} else {
				Log.e(log, "待求表达式的值不确定，故将第" + i + "条记录存放进队列(系数和常数项分别存两个队列)");
				logStr += "待求表达式的值不确定，故将第" + i + "条记录存放进队列(系数和常数项分别存两个队列)\n";
				for (int j = 0; j < Dimension; j++) {
					ListA.add(alpha_i[j] + "");
				}
				ListA_ext.add(fi[0] + "");
				rA++;
				// Log.e(log, "ListA(存放方程系数的队列)的大小为 " + ListA.size());
				// Log.e(log, "ListA_ext(存放方程的常数项的队列)的大小为	" + ListA_ext.size());
			}

		} while (!(i == List.size() || haveSolution));

		if (haveSolution) {
			Log.e(log, "本次预估余额为:" + AccountBalance + "元");
			Log.e(log, "遍历至第" + i + "条记录时解已经可以求出!");
			Log.e(log, "成功计算出计算结果，过程完毕!");
			logStr += "本次预估余额为:" + AccountBalance + "元.\n" + "遍历至第" + i
					+ "条记录时解已经可以求出!" + "	" + "成功计算出计算结果，过程完毕!\n";
			SendHandler.post(runable);
			// 向UI显示线程发送一个有解的消息并把解显出出来
		} else {
			Log.e(log, "根据给出的信息无法计算出结果,过程完毕!");
			logStr += "根据给出的信息无法计算出结果,过程完毕!\n";
			SendHandler.post(runable);
			// 向UI显示线程发送一个无解的消息
		}
		String caculateConsumeTime = "未知";
		if (System.currentTimeMillis() - ms > 1000) {
			caculateConsumeTime = (System.currentTimeMillis() - ms) / 1000
					+ "秒钟零" + (System.currentTimeMillis() - ms) % 1000 + "毫秒";
		} else {
			caculateConsumeTime = System.currentTimeMillis() - ms + "毫秒";
		}
		Log.e(log, "此次话费预测计算共耗时 " + caculateConsumeTime);
		logStr += "此次话费预测计算共耗时 " + caculateConsumeTime + "\n";
		CacheFileManager.getInstance().logAccounting(logStr);
	}

	Runnable runable = new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			if (equationNull) {
				Log.e("callstats", "历史方程组为空");
				if (ReceiveHandler != null) {
					Message msg = new Message();
					Bundle bd = new Bundle();
					bd.putString("haveSolutionFlag", "null");
					msg.setData(bd);
					ReceiveHandler.sendMessage(msg);
				} else {
					Log.e("callstats", "ReceiveHandler == null");
				}
				return; // 既然发送了方程组为空的消息，那边是按无解的消息显示处理的，就没有必要再接下去判断是有解还是无解的消息了，直接return；
			}

			if (haveSolution) {
				if (ReceiveHandler != null) {
					Log.e("callstats", "haveSolution");
					Message msg = new Message();
					Bundle bd = new Bundle();
					bd.putString("haveSolutionFlag", "true");
					msg.setData(bd);
					ReceiveHandler.sendMessage(msg);
				} else {
					Log.e("callstats", "ReceiveHandler == null");
				}
			} else {
				if (ReceiveHandler != null) {
					Log.e("callstats", "no haveSolution");
					Message msg = new Message();
					Bundle bd = new Bundle();
					bd.putString("haveSolutionFlag", "false");
					msg.setData(bd);
					ReceiveHandler.sendMessage(msg);
				} else {
					Log.e("callstats", "ReceiveHandler == null");
				}
			}
		}

	};

	public native int Rank(double A[], int m, int n);// 求矩阵A的秩，m为矩阵的行数，n为矩阵的列数；

	public native double[] Transform(double A[], int m, int n);// 求矩阵A的转置矩阵；

	public native double[] Inverse(double A[], int m);// 求可逆方阵A的逆矩阵,m为行(列)数;

	public native double[] MultiMatrix(double A[], int m, double B[], int t,
			int n);// 求矩阵A和矩阵B的乘积，m为A的行数，t为B的行数(A的列数)，n为B的列数；

}
