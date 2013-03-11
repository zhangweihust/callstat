package com.archermind.callstat.common;

import com.archermind.callstat.CallStatApplication;
import com.archermind.callstat.ConfigManager;
import com.archermind.callstat.accounting.ReconciliationInfo;
import com.archermind.callstat.common.database.CallStatDatabase;

public class PhoneBillCaculateUtils {

	public static void addEquationToDatabase(ConfigManager config,
			float fees_remain) {
		if (config.getLastAddEquationTime() == -1) {
			config.setLastAddEquationTime(System.currentTimeMillis());
			return;
		}
		if (CallStatApplication.getCallstatsContext() == null) {
			return;
		}
		if (CallStatDatabase.getInstance(CallStatApplication
				.getCallstatsContext()) == null) {
			return;
		}
		if (config.getFeesRemian() == 100000) {
			return;
		}
		int calledTimes[] = CallStatDatabase.getInstance(
				CallStatApplication.getCallstatsContext())
				.getLatelyLocalCallTime(config.getLastAddEquationTime(),
						System.currentTimeMillis());
		int smsNum = CallStatUtils.getPrevReconcilitaionToNowSmsUsed(config);
		long trafficData = CallStatUtils
				.getPrevReconcilitionToNowGprsUsed(config);
		int deltas[] = getDeltas(config);
		double difference = config.getFeesRemian() - fees_remain;
		if ((smsNum != 0 || trafficData != 0 || calledTimes[0] != 0
				|| calledTimes[1] != 0 || calledTimes[2] != 0
				|| calledTimes[3] != 0 || calledTimes[4] != 0
				|| calledTimes[5] != 0 || !CallStatUtils.isAllZeros(deltas)) // deltas系数不全为0，也往数据库中存放。
				&& CallStatUtils.getNowDateDate() != config.getAccountingDay()
				&& difference > 0) {

			CallStatDatabase.getInstance(
					CallStatApplication.getCallstatsContext()).createTable(
					CallStatDatabase.TABLE_RECONCILIATION_INFO);
			CallStatDatabase
					.getInstance(CallStatApplication.getCallstatsContext())
					.addReconciliationInfo(
							new ReconciliationInfo(Long.parseLong(CallStatUtils
									.getNowTime()), calledTimes[0],
									calledTimes[1], calledTimes[2],
									calledTimes[3], calledTimes[4],
									calledTimes[5], smsNum, trafficData,
									difference, deltas[0], deltas[1],
									deltas[2], deltas[3], deltas[4], deltas[5]));
			config.setLastAddEquationTime(System.currentTimeMillis());
		}

	}

	static int[] getDeltas(ConfigManager config) {
		int[] deltas = new int[] { 0, 0, 0, 0, 0, 0 };
		if (config.getLastAddEquationTime() == -1) {
			return deltas;
		}
		String lastCheck_time = CallStatUtils
				.changeMilliSeconds2YearMonthDayHourMin(config
						.getLastAddEquationTime());
		String hour_minute = lastCheck_time.substring(8,
				lastCheck_time.length());
		int delta_begin_index = CallStatUtils
				.which_delta_begin_to_add(hour_minute);

		long s = (System.currentTimeMillis() - config.getLastAddEquationTime()) / 1000;
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
		return deltas;
	}
}
