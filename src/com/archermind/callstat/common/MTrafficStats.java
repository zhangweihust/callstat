package com.archermind.callstat.common;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import android.net.TrafficStats;

import com.archermind.callstat.CallStatApplication;
import com.archermind.callstat.ILog;

public class MTrafficStats {
	private static final String DEV_FILE = "/proc/self/net/dev";// 系统流量文件
	private static String[] ethdata = { "0", "0", "0", "0", "0", "0", "0", "0",
			"0", "0", "0", "0", "0", "0", "0", "0" };
	private static String[] gprsdata = { "0", "0", "0", "0", "0", "0", "0",
			"0", "0", "0", "0", "0", "0", "0", "0", "0" };
	private static String[] wifidata = { "0", "0", "0", "0", "0", "0", "0",
			"0", "0", "0", "0", "0", "0", "0", "0", "0" };
	private static boolean hasWifi = false;
	private static final String ETHLINE = "  eth0";// 以太网信息所在行
	private static final String GPRSLINE = "rmnet0";
	private static final String WIFILINE = "tiwlan0";
	private static final String WIFILINE4_0 = " wlan0";

	public static final int UNSUPPORTED = -1;

	/**
	 * 根据应用uid获取本应用的下载流量
	 * 
	 * @param uid
	 *            应用的UID
	 * @return 下载字节数
	 */
	public static long getUidRxBytes(int uid) {
		long ReturnLong = UNSUPPORTED;
		if (CallStatApplication.isOsNew) {
			ReturnLong = TrafficStats.getUidRxBytes(uid); // 查询到的结果
			if (ReturnLong != -1) {
				return ReturnLong;
			}
		}

		try {
			String url = "/proc/uid_stat/" + String.valueOf(uid) + "/tcp_rcv";
			File file = new File(url);
			FileInputStream inStream;
			if (file.exists()) {
				inStream = new FileInputStream(file);
				ReturnLong = Long.parseLong(readInStream(inStream).trim());
			}
			return ReturnLong;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return UNSUPPORTED;
		}

		// Log.i(url+"文件并不存在","可能原因为该文件在开机后并没有上过网，所以没有流量记录");
	}

	/**
	 * 获取通过Mobile连接收到的字节总数，不包含WiFi
	 * 
	 * @return 接收字节数
	 */
	public static long getMobileRxBytes() {
		if (CallStatApplication.isOsNew) {
			ILog.LogE(MTrafficStats.class, "zhangj 接收到的Gprs流量为:"+TrafficStats.getMobileRxBytes());
			if (TrafficStats.getMobileRxBytes() != -1) {
				return TrafficStats.getMobileRxBytes();
			}
		}
		readdev();
		return Long.parseLong(gprsdata[0]);
	}

	/**
	 * 获取通过WiFi连接收到的字节总数
	 * 
	 * @return
	 */
	public static long getWifiRxBytes() {
		// long wifiRxBytes = ;
		if (CallStatApplication.isOsNew) {
			if (TrafficStats.getTotalRxBytes() != -1) {
				return TrafficStats.getTotalRxBytes()
						- TrafficStats.getMobileRxBytes();
			}
		}
		readdev();
		if (hasWifi) {
			return Long.parseLong(wifidata[0]);
		} else {
			return Long.parseLong(ethdata[0]);
		}
	}

	/**
	 * 获取通过WiFi连接发送的字节总数
	 * 
	 * @return
	 */
	public static long getWifiTxBytes() {
		// long wifiTxBytes = TrafficStats.getTotalTxBytes()
		// - TrafficStats.getMobileTxBytes();
		if (CallStatApplication.isOsNew) {
			if (TrafficStats.getTotalTxBytes() != -1) {
				return TrafficStats.getTotalTxBytes()
						- TrafficStats.getMobileTxBytes();
			}
		}
		readdev();
		if (hasWifi) {
			return Long.parseLong(wifidata[8]);
		} else {
			return Long.parseLong(ethdata[8]);
		}
	}

	/**
	 * 获取通过Mobile连接发送接收总的字节总数
	 * 
	 * @return
	 */
	public static long getMobileTotalBytes() {
		return getMobileRxBytes() + getMobileTxBytes();
		// readdev();
		// return Long.parseLong(gprsdata[0]) + Long.parseLong(gprsdata[8]);
	}

	/**
	 * 获取通过Wifi连接发送接收总的字节总数
	 * 
	 * @return
	 */
	public static long getWifiTotalBytes() {
		return getWifiRxBytes() + getWifiTxBytes();
		// readdev();
		// if (hasWifi) {
		// return Long.parseLong(wifidata[0]) + Long.parseLong(wifidata[8]);
		// } else {
		// return Long.parseLong(ethdata[0]) + Long.parseLong(ethdata[8]);
		// }
	}

	/**
	 * 获取通过Mobile连接接收的字节总数
	 * 
	 * @return
	 */
	public static long getMobileTxBytes() {
		if (CallStatApplication.isOsNew) {
			if (TrafficStats.getMobileTxBytes() != -1) {
				return TrafficStats.getMobileTxBytes();
			}
		}
		readdev();
		return Long.parseLong(gprsdata[8]);
	}

	/**
	 * 获取通过Mobile和Wifi连接接收的字节总数
	 * 
	 * @return
	 */
	public static long getTotalRxBytes() {

		return getMobileRxBytes() + getWifiRxBytes();
		// readdev();
		// if (hasWifi) {
		// return Long.parseLong(gprsdata[0]) + Long.parseLong(wifidata[0]);
		// } else {
		// return Long.parseLong(gprsdata[0]) + Long.parseLong(ethdata[0]);
		// }
	}

	/**
	 * 获取通过Mobile和Wifi连接发送的字节总数
	 * 
	 * @return
	 */
	public static long getTotalTxBytes() {
		return getMobileTxBytes() + getWifiTxBytes();
		// readdev();
		// if (hasWifi) {
		// return Long.parseLong(gprsdata[8]) + Long.parseLong(wifidata[8]);
		// } else {
		// return Long.parseLong(gprsdata[8]) + Long.parseLong(ethdata[8]);
		// }
	}

	/**
	 * 读取系统流量日志
	 */
	private static void readdev() {
		FileReader fstream = null;
		try {
			fstream = new FileReader(DEV_FILE);

		} catch (FileNotFoundException e) {
			// DisplayToast("Could not read " + DEV_FILE);

		}
		BufferedReader in = new BufferedReader(fstream, 500);
		String line;
		String[] segs;
		String[] netdata;

		int count = 0;
		int k;
		int j;
		try {
			while ((line = in.readLine()) != null) {
				segs = line.trim().split(":");
				if (line.startsWith(ETHLINE)) {

					netdata = segs[1].trim().split(" ");
					for (k = 0, j = 0; k < netdata.length; k++) {
						if (netdata[k].length() > 0) {

							ethdata[j] = netdata[k];
							j++;
						}
					}
				} else if (line.startsWith(GPRSLINE)) {

					netdata = segs[1].trim().split(" ");
					for (k = 0, j = 0; k < netdata.length; k++) {
						if (netdata[k].length() > 0) {

							gprsdata[j] = netdata[k];
							j++;
						}
					}
				} else if (line.startsWith(WIFILINE)) {

					netdata = segs[1].trim().split(" ");
					for (k = 0, j = 0; k < netdata.length; k++) {
						if (netdata[k].length() > 0) {

							wifidata[j] = netdata[k];
							j++;
						}
					}
					if (!hasWifi) {
						hasWifi = true;
					}
				} else if (line.startsWith(WIFILINE4_0)) {
					netdata = segs[1].trim().split(" ");
					for (k = 0, j = 0; k < netdata.length; k++) {
						if (netdata[k].length() > 0) {

							wifidata[j] = netdata[k];
							j++;
						}
					}
					if (!hasWifi) {
						hasWifi = true;
					}
				}

				count++;
			}
			fstream.close();

		} catch (IOException e) {
			// DisplayToast(e.toString());
		}
	}

	private static String readInStream(FileInputStream inStream) {
		try {
			ByteArrayOutputStream outStream = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int length = -1;
			while ((length = inStream.read(buffer)) != -1) {
				outStream.write(buffer, 0, length);
			}
			outStream.close();
			inStream.close();
			return outStream.toString();
		} catch (IOException e) {
			// Log.i("FileTest", e.getMessage());
		}
		return null;
	}

	// //////////////////////////////////根据uid获取进程的上传流量//////////////////////////////////////
	/**
	 * 根据uid获取进程的上传流量
	 */
	public static long getUidTxBytes(int uid) {

		long ReturnLong = UNSUPPORTED;
		if (CallStatApplication.isOsNew) {
			ReturnLong = TrafficStats.getUidTxBytes(uid); // 查询到的结果
			if (ReturnLong != -1) {
				return ReturnLong;
			}
		}
		try {
			String url = "/proc/uid_stat/" + String.valueOf(uid) + "/tcp_snd";
			File file = new File(url);
			if (file.exists()) {
				FileInputStream inStream = new FileInputStream(file);
				ReturnLong = Long.parseLong(readInStream(inStream).trim());
			}
			return ReturnLong;
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return UNSUPPORTED;
		}
	}
}
