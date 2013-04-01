package com.android.callstat.firewall;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;

import com.android.callstat.CallStatApplication;
import com.android.callstat.firewall.thread.FirewallRunner;

/**
 * reference com.googlecode.droidwall this class contains every method we need
 * for firewall functions sure that only shared programming interfaces we can
 * control ip table rules are defined here
 * 
 * @author longX
 * */
public class FirewallCoreWorker {
	// Preferences
	public static final String PREF_MODE = "BlockMode";
	public static final String PREF_ITFS = "Interfaces";
	// Modes
	public static final String MODE_WHITELIST = "whitelist";
	public static final String MODE_BLACKLIST = "blacklist";
	// Interfaces
	// public static final String FILTER_2G_3G = "2G/3G";
	// public static final String FILTER_WIFI = "wifi";
	// Do we have root access?
	// private static boolean hasroot = false;
	// action
	public static final String ACCEPT = "ACCEPT";
	public static final String REJECT = "REJECT";

	// Rule
	public static final String RULE_ALLOW = "-D";
	public static final String RULE_REJECT = "-A";

	// interface filter
	public static final String WIFI_FILTER = "droidwall-wifi";
	public static final String GPRS_FILTER = "droidwall-3g";

	public static final String BLOCK_SINGLE = "single";
	public static final String BLOCK_ALL = "all_black";
	public static final String LETGO_ALL = "all_white";
	public static final String LETGO_WIFI_DHCP = "wifi_dhcp";

	// /data/data/com.archermind.callstat/databases/iptables_armv5
	public static final String MY_HEADER = "/data/data/com.archermind.callstat/databases/iptables_armv5";

	public static final String HEADER = "iptables";

	public static boolean isFileModified = false;

	// interfaces mode:
	// to block all interfaces:filter = "";
	// to block 2g/3g :filter = "-o rmnet+";
	// to block wifi : filter = "-o tiwlan+";
	public static String generateScript(int uid, String rule) {
		StringBuilder script = new StringBuilder();
		// script header
		// script.append(MY_HEADER + " -F || exit\n");

		if (uid != -1) {
			if (CallStatApplication.canMyFirewallWork) {
				script.append(MY_HEADER + "  -" + rule
						+ " droidwall-3g  -m owner --uid-owner " + uid + " -j "
						+ "droidwall-reject" + "\n");
			} else {
				script.append(HEADER + "  -" + rule
						+ " droidwall-3g  -m owner --uid-owner " + uid + " -j "
						+ "droidwall-reject" + "\n");
			}
		}
		return script.toString();
	}

	// interfaces mode:
	// to block all interfaces:filter = "";
	// to block 2g/3g :filter = "-o rmnet+";
	// to block wifi : filter = "-o tiwlan+";
	public static String generateScript(int uid, String rule, String filter) {
		StringBuilder script = new StringBuilder();
		// script header
		// script.append(MY_HEADER + " -F || exit\n");

		if (uid != -1) {
			if (CallStatApplication.canMyFirewallWork) {
				script.append(MY_HEADER + " " + rule + " " + filter
						+ " -m owner --uid-owner " + uid + " -j "
						+ "droidwall-reject" + "\n");
			} else {
				script.append(HEADER + " " + rule + " " + filter
						+ "  -m owner --uid-owner " + uid + " -j "
						+ "droidwall-reject" + "\n");
			}
		}
		return script.toString();
	}

	public static String generateScript(String script) {
		StringBuilder sb = new StringBuilder();

		if (CallStatApplication.canMyFirewallWork) {
			sb.append(MY_HEADER + script);
		} else {
			sb.append(HEADER + script);
		}
		return sb.toString();
	}

	public static String getIpTableChain(boolean canMineWork) {
		if (canMineWork) {
			modifyFilePermission();
			String cmd = MY_HEADER + " -L";
			StringBuilder result = new StringBuilder();
			try {
				Log.i("callstats", cmd);
				Process process = Runtime.getRuntime().exec(cmd);

				InputStream in = process.getInputStream();
				Log.i("callstats", (in == null) + " ?InputStream");
				byte[] buf = new byte[1024];

				while ((in.read(buf)) != -1) {
					result.append(new String(buf));
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
			Log.i("callstats", result.toString());
			return result.toString().trim();
		} else {
			String cmd = HEADER + " -L";
			StringBuilder result = new StringBuilder();
			try {
				Log.i("callstats", cmd);
				Process process = Runtime.getRuntime().exec(cmd);

				InputStream in = process.getInputStream();
				Log.i("callstats", (in == null) + " ?InputStream");
				byte[] buf = new byte[1024];

				while ((in.read(buf)) != -1) {
					result.append(new String(buf));
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
			Log.i("callstats", result.toString());
			return result.toString().trim();
		}

	}

	/*-A droidwall-3g -m owner --uid-owner 10063  -j  droidwall-reject
	-D droidwall-3g -m owner --uid-owner 10063  -j  droidwall-reject*/

	public static String generateScriptFromForbiddenList(Set<Integer> uids,
			String filter, boolean canMineWork) {
		if (canMineWork) {
			modifyFilePermission();
			StringBuilder script = new StringBuilder();
			// script header
			// script.append(MY_HEADER + " -F || exit\n");

			if (uids == null || uids.isEmpty()) {
				return script.toString();
			}

			for (Integer uid : uids) {
				if (uid != -1) {
					script.append(MY_HEADER + "  -A droidwall-3g " + filter
							+ " -m owner --uid-owner " + uid + " -j "
							+ "droidwall-reject" + "\n");
				}
			}

			return script.toString();
		} else {
			StringBuilder script = new StringBuilder();
			// script header
			script.append(HEADER + " -F || exit\n");

			if (uids == null || uids.isEmpty()) {
				return script.toString();
			}

			for (Integer uid : uids) {
				if (uid != -1) {
					script.append(HEADER + "  -A droidwall-3g " + filter
							+ " -m owner --uid-owner " + uid + " -j "
							+ "droidwall-reject" + "\n");
				}
			}

			return script.toString();
		}
	}

	// 批量更改
	public static String generateScriptFromForbiddenList(Set<Integer> uids,
			String rule, String filter, boolean canMineWork) {
		if (canMineWork) {
			modifyFilePermission();
			StringBuilder script = new StringBuilder();
			// script header
			// script.append(MY_HEADER + " -F || exit\n");

			if (uids == null || uids.isEmpty()) {
				return script.toString();
			}

			for (Integer uid : uids) {
				if (uid != -1) {
					script.append(MY_HEADER + " " + rule + " " + filter
							+ " -m owner --uid-owner " + uid + " -j "
							+ "droidwall-reject" + "\n");
				}
			}

			return script.toString();
		} else {
			StringBuilder script = new StringBuilder();
			// script header
			script.append(HEADER + " -F || exit\n");

			if (uids == null || uids.isEmpty()) {
				return script.toString();
			}

			for (Integer uid : uids) {
				if (uid != -1) {
					script.append(MY_HEADER + " " + rule + " " + filter
							+ " -m owner --uid-owner " + uid + " -j "
							+ "droidwall-reject" + "\n");
				}
			}

			return script.toString();
		}
	}

	public static int applyRulesWithForbiddenList(Set<Integer> uids,
			String filter, boolean canMineWork) {
		int resultCode = -1;
		try {
			String script = generateScriptFromForbiddenList(uids, filter,
					canMineWork);
			StringBuilder res = new StringBuilder();
			resultCode = runScriptAsRoot(script, res);
			if (CallStatApplication.canFirewallWork) {
				Log.i("callstats", "canFirewallWork: true default");
				if (res != null) {
					Log.i("callstats", "system default firewall works out:"
							+ res.toString());
					String out = res.toString();
					if (out.contains("No chain/target/match by that name")
							|| out.contains("ERROR")) {
						resultCode = 404;
						CallStatApplication.canFirewallWork = false;
					}
				}
			}
		} catch (Exception e) {
		}
		return resultCode;
	}

	public static int applyRulesWithForbiddenList(Set<Integer> uids,
			String rule, String filter, boolean canMineWork) {
		int resultCode = -1;
		try {
			String script = generateScriptFromForbiddenList(uids, rule, filter,
					canMineWork);
			Log.i("callstats", "script:" + script);
			StringBuilder res = new StringBuilder();
			resultCode = runScriptAsRoot(script, res);
			if (CallStatApplication.canFirewallWork) {
				Log.i("callstats", "canFirewallWork: true default");
				if (res != null) {
					Log.i("callstats", "system default firewall works out:"
							+ res.toString());
					String out = res.toString();
					if (out.contains("ERROR")) {
						resultCode = 404;
						CallStatApplication.canFirewallWork = false;
					}
				}
			}
		} catch (Exception e) {
		}
		return resultCode;
	}

	public static String letGoOfWifiAndDhcp(String filter) {
		StringBuilder script = new StringBuilder();
		// script header
		script.append(MY_HEADER + "  -F || exit\n");
		// When "white listing" Wi-fi, we need ensure that the dhcp and wifi
		// users are allowed
		int uid = android.os.Process.getUidForName("dhcp");
		if (uid != -1)
			script.append(MY_HEADER + "  -A OUTPUT " + filter
					+ " -m owner --uid-owner " + uid + " -j ACCEPT || exit\n");

		uid = android.os.Process.getUidForName("wifi");
		if (uid != -1)
			script.append(MY_HEADER + "  -A OUTPUT " + filter
					+ " -m owner --uid-owner " + uid + " -j ACCEPT || exit\n");

		Log.i("i", "script:" + script.toString());
		return script.toString();
	}

	public static String blockAllInterfaces(String filter) {
		StringBuilder script = new StringBuilder();
		// script header
		script.append(MY_HEADER + "  -F || exit\n");

		script.append(MY_HEADER + "  -A OUTPUT " + filter
				+ " -j REJECT || exit\n");

		return script.toString();
	}

	/**
	 * apply a set of rules with returning a code to inform user of apply state
	 * 
	 * @param ctx
	 *            is .. well, you know what it is
	 * @param uid
	 *            the uid of the certain process
	 * @param filter
	 *            which interface you want to apply,such as wifi,2g/3g..
	 * @param rule
	 *            accept or reject
	 * @param blockMode
	 *            the specific action you want to do,such as block single
	 *            process or block all or let go all up above,pass real values
	 *            for what you need and null for what you don't need
	 * */
	public static int applyRule(int uid, String rule, boolean canMineWork) {
		int resultCode = -1;
		try {
			String script = generateScript(uid, rule);
			StringBuilder res = new StringBuilder();
			resultCode = runScriptAsRoot(script, res);
			if (CallStatApplication.canFirewallWork) {
				Log.i("callstats", "canFirewallWork: true default");
				if (res != null) {
					Log.i("callstats", "system default firewall works out:"
							+ res.toString());
					String out = res.toString();
					if (out.contains("ERROR")) {
						resultCode = 404;
						CallStatApplication.canFirewallWork = false;

					}
				}
			}
		} catch (Exception e) {
		}

		return resultCode;
	}

	/**
	 * apply a set of rules with returning a code to inform user of apply state
	 * 
	 * @param ctx
	 *            is .. well, you know what it is
	 * @param uid
	 *            the uid of the certain process
	 * @param filter
	 *            which interface you want to apply,such as wifi,2g/3g..
	 * @param rule
	 *            accept or reject
	 * @param blockMode
	 *            the specific action you want to do,such as block single
	 *            process or block all or let go all up above,pass real values
	 *            for what you need and null for what you don't need
	 * */
	public static int applyRule(int uid, String rule, String filter,
			boolean canMineWork) {
		int resultCode = -1;
		try {
			String script = generateScript(uid, rule, filter);
			StringBuilder res = new StringBuilder();
			resultCode = runScriptAsRoot(script, res);
			if (CallStatApplication.canFirewallWork) {
				Log.i("callstats", "canFirewallWork: true default");
				if (res != null) {
					Log.i("callstats", "system default firewall works out:"
							+ res.toString());
					String out = res.toString();
					if (out.contains("ERROR")) {
						resultCode = 404;
						CallStatApplication.canFirewallWork = false;

					}
				}
			}
		} catch (Exception e) {
		}

		return resultCode;
	}

	public static void initIptables(boolean canMineWork, boolean isFlushRules)
			throws IOException {
		if (FirewallCoreWorker.hasRootAccess()) {
			if (CallStatApplication.canMyFirewallWork) {
				StringBuilder finalScript = new StringBuilder();
				int hasChain;
				if (isFlushRules) {
					// runScriptAsRoot(MY_HEADER + " -F", new StringBuilder());
					finalScript.append(MY_HEADER + " -F \n");
				}

				String script = MY_HEADER + " -L droidwall";

				hasChain = hasChain(script);

				if (hasChain == -1) {
					finalScript.append(MY_HEADER + " -N droidwall \n");
					finalScript
							.append(MY_HEADER + " -A OUTPUT -j droidwall \n");
				} else if (hasChain == 0) {
					finalScript
							.append(MY_HEADER + " -A OUTPUT -j droidwall \n");
				}

				script = MY_HEADER + " -L droidwall-reject";

				hasChain = hasChain(script);

				if (hasChain == -1) {
					finalScript.append(MY_HEADER + " -N droidwall-reject \n");
					finalScript.append(MY_HEADER
							+ " -A droidwall-reject -j REJECT \n");
				} else if (hasChain == 0) {
					finalScript.append(MY_HEADER
							+ " -A droidwall-reject -j REJECT \n");
				}

				script = MY_HEADER + " -L droidwall-3g";

				hasChain = hasChain(script);
				// -o tiwlan+
				if (hasChain == -1) {
					finalScript.append(MY_HEADER + " -N droidwall-3g \n");
					// finalScript.append(MY_HEADER +
					// " -A droidwall -j droidwall-3g \n");
					finalScript.append(MY_HEADER
							+ " -A droidwall -o rmnet+ -j droidwall-3g \n");
					finalScript.append(MY_HEADER
							+ " -A droidwall -o pdp+ -j droidwall-3g \n");
					finalScript.append(MY_HEADER
							+ " -A droidwall -o ppp+ -j droidwall-3g \n");
					finalScript.append(MY_HEADER
							+ " -A droidwall -o uwbr+ -j droidwall-3g \n");
				} else if (hasChain == 0) {
					// finalScript.append(MY_HEADER +
					// " -A droidwall -j droidwall-3g \n");
					finalScript.append(MY_HEADER
							+ " -A droidwall -o rmnet+ -j droidwall-3g \n");
					finalScript.append(MY_HEADER
							+ " -A droidwall -o pdp+ -j droidwall-3g \n");
					finalScript.append(MY_HEADER
							+ " -A droidwall -o ppp+ -j droidwall-3g \n");
					finalScript.append(MY_HEADER
							+ " -A droidwall -o uwbr+ -j droidwall-3g \n");
				}

				script = MY_HEADER + " -L droidwall-wifi";

				hasChain = hasChain(script);

				if (hasChain == -1) {
					finalScript.append(MY_HEADER + " -N droidwall-wifi \n");
					finalScript.append(MY_HEADER
							+ " -A droidwall -o tiwlan+ -j droidwall-wifi \n");
					finalScript.append(MY_HEADER
							+ " -A droidwall -o eth+ -j droidwall-wifi \n");
					finalScript.append(MY_HEADER
							+ " -A droidwall -o wlan+ -j droidwall-wifi \n");
				} else if (hasChain == 0) {
					finalScript.append(MY_HEADER
							+ " -A droidwall -o tiwlan+ -j droidwall-wifi \n");
					finalScript.append(MY_HEADER
							+ " -A droidwall -o eth+ -j droidwall-wifi \n");
					finalScript.append(MY_HEADER
							+ " -A droidwall -o wlan+ -j droidwall-wifi \n");
				}

				if (finalScript.toString().length() > 0) {
					StringBuilder sb = new StringBuilder();
					runScriptAsRoot(finalScript.toString(), sb);
				}
			} else {
				StringBuilder finalScript = new StringBuilder();
				int hasChain;
				if (isFlushRules) {
					// runScriptAsRoot(MY_HEADER + " -F", new StringBuilder());
					finalScript.append(MY_HEADER + " -F \n");
				}

				String script = HEADER + " -L droidwall";

				hasChain = hasChain(script);

				if (hasChain == -1) {
					finalScript.append(HEADER + " -N droidwall \n");
					finalScript.append(HEADER + " -A OUTPUT -j droidwall \n");
				} else if (hasChain == 0) {
					finalScript.append(HEADER + " -A OUTPUT -j droidwall \n");
				}

				script = HEADER + " -L droidwall-reject";

				hasChain = hasChain(script);

				if (hasChain == -1) {
					finalScript.append(HEADER + " -N droidwall-reject \n");
					finalScript.append(HEADER
							+ " -A droidwall-reject -j REJECT \n");
				} else if (hasChain == 0) {
					finalScript.append(HEADER
							+ " -A droidwall-reject -j REJECT \n");
				}

				script = HEADER + " -L droidwall-3g";

				hasChain = hasChain(script);

				if (hasChain == -1) {
					finalScript.append(HEADER + " -N droidwall-3g \n");
					finalScript.append(HEADER
							+ " -A droidwall -o rmnet+ -j droidwall-3g \n");
					finalScript.append(HEADER
							+ " -A droidwall -o pdp+ -j droidwall-3g \n");
					finalScript.append(HEADER
							+ " -A droidwall -o ppp+ -j droidwall-3g \n");
					finalScript.append(HEADER
							+ " -A droidwall -o uwbr+ -j droidwall-3g \n");
				} else if (hasChain == 0) {
					finalScript.append(HEADER
							+ " -A droidwall -o rmnet+ -j droidwall-3g \n");
					finalScript.append(HEADER
							+ " -A droidwall -o pdp+ -j droidwall-3g \n");
					finalScript.append(HEADER
							+ " -A droidwall -o ppp+ -j droidwall-3g \n");
					finalScript.append(HEADER
							+ " -A droidwall -o uwbr+ -j droidwall-3g \n");
				}

				script = HEADER + " -L droidwall-wifi";

				hasChain = hasChain(script);

				if (hasChain == -1) {
					finalScript.append(HEADER + " -N droidwall-wifi \n");
					finalScript.append(HEADER
							+ " -A droidwall -o tiwlan+ -j droidwall-wifi \n");
					finalScript.append(HEADER
							+ " -A droidwall -o eth+ -j droidwall-wifi \n");
					finalScript.append(HEADER
							+ " -A droidwall -o wlan+ -j droidwall-wifi \n");

				} else if (hasChain == 0) {
					finalScript.append(HEADER
							+ " -A droidwall -o tiwlan+ -j droidwall-wifi \n");
					finalScript.append(HEADER
							+ " -A droidwall -o eth+ -j droidwall-wifi \n");
					finalScript.append(HEADER
							+ " -A droidwall -o wlan+ -j droidwall-wifi \n");
				}

				if (finalScript.toString().length() > 0) {
					StringBuilder sb = new StringBuilder();
					runScriptAsRoot(finalScript.toString(), sb);
				}
			}
		}
	}

	/**
	 * @param script
	 *            to be run
	 * @return 0 for 0 references;1 for okay ; -1 for no such chain or target
	 **/
	private static int hasChain(String script) throws IOException {
		StringBuilder res = new StringBuilder();
		runScriptAsRoot(script, res);
		// Log.e("callstats", res.toString());
		if (res.toString().contains("No chain/target/match by that name")) {
			return -1;
		} else if (res.toString().contains("0 references")) {
			return 0;
		} else {
			return 1;
		}
	}

	public static int applyRule(Context ctx, int uid, String filter,
			String rule, String blockMode) {
		int resultCode = -1;
		try {
			if (LETGO_ALL.equals(blockMode)) {
				resultCode = purgeIptables(ctx);
			} else if (LETGO_WIFI_DHCP.equals(blockMode)) {
				String script = letGoOfWifiAndDhcp(filter);
				StringBuilder res = new StringBuilder();
				resultCode = runScriptAsRoot(script, res);
			} else if (BLOCK_ALL.equals(blockMode)) {
				String script = blockAllInterfaces(filter);
				StringBuilder res = new StringBuilder();
				resultCode = runScriptAsRoot(script, res);
			} else if (BLOCK_SINGLE.equals(blockMode)) {
				String script = generateScript(uid, rule);
				StringBuilder res = new StringBuilder();
				resultCode = runScriptAsRoot(script, res);
			}
		} catch (Exception e) {
		}
		return resultCode;
	}

	/**
	 * Display a simple alert box
	 * 
	 * @param ctx
	 *            context
	 * @param msg
	 *            message
	 */
	public static void alert(Context ctx, CharSequence msg) {
		if (ctx != null) {
			new AlertDialog.Builder(ctx)
					.setNeutralButton(android.R.string.ok, null)
					.setMessage(msg).show();
		}
	}

	/**
	 * Runs a script as root (multiple commands separated by "\n") with a
	 * default timeout of 5 seconds.
	 * 
	 * @param script
	 *            the script to be executed
	 * @param res
	 *            the script output response (stdout + stderr)
	 * @return the script exit code
	 * @throws IOException
	 *             on any error executing the script, or writing it to disk
	 */
	public static int runScriptAsRoot(String script, StringBuilder res)
			throws IOException {
		return runScriptAsRoot(script, res, 15000);
	}

	/**
	 * Runs a script as root (multiple commands separated by "\n").
	 * 
	 * @param script
	 *            the script to be executed
	 * @param res
	 *            the script output response (stdout + stderr)
	 * @param timeout
	 *            timeout in milliseconds (-1 for none)
	 * @return the script exit code
	 */
	public static int runScriptAsRoot(String script, StringBuilder res,
			final long timeout) {
		final FirewallRunner runner = new FirewallRunner(script, res);
		runner.start();
		try {
			if (timeout > 0) {
				// see also java.lang.Thread
				runner.join(timeout);
			} else {
				runner.join();
			}
			if (runner.isAlive()) {
				// Timed-out
				runner.interrupt();
				runner.destroy();
				runner.join(1500);
			}
		} catch (InterruptedException ex) {
		}
		return runner.exitcode;
	}

	/**
	 * Display iptables rules output
	 * 
	 * @param ctx
	 *            application context
	 */
	public static void showIptablesRules(Context ctx) {
		try {
			modifyFilePermission();
			final StringBuilder res = new StringBuilder();
			runScriptAsRoot(MY_HEADER + "  -L\n", res);
			Log.i("callstats", "showIptablesRules:" + res);
			alert(ctx, res);
		} catch (Exception e) {
			alert(ctx, "error: " + e);
		}
	}

	/**
	 * Display iptables rules output
	 */
	public static String showIptablesRules(boolean canMineWork) {
		String rules = "";
		if (canMineWork) {
			try {
				modifyFilePermission();
				final StringBuilder res = new StringBuilder();
				runScriptAsRoot(MY_HEADER + "  -L\n", res);
				rules = res.toString();
				Log.i("callstats", "showIptablesRules:" + rules);
			} catch (Exception e) {
			}
		} else {
			try {
				final StringBuilder res = new StringBuilder();
				runScriptAsRoot(HEADER + "  -L\n", res);
				rules = res.toString();
				Log.i("callstats", "showIptablesRules:" + rules);
			} catch (Exception e) {
			}
		}
		return rules;
	}

	/**
	 * Purge all iptables rules.
	 * 
	 * @param ctx
	 *            context optional context for alert messages
	 */
	public static int purgeIptables(Context ctx) {
		StringBuilder res = new StringBuilder();
		int code = -1;
		try {
			code = runScriptAsRoot(MY_HEADER + "  -F || exit\n", res);
			return code;
		} catch (Exception e) {
			return code;
		}
	}

	public static int purgeIptables(boolean canMineWork) {
		StringBuilder res = new StringBuilder();
		int code = -1;
		if (canMineWork) {
			try {
				code = runScriptAsRoot(MY_HEADER + "  -F || exit\n", res);
				return code;
			} catch (Exception e) {
				return code;
			}
		} else {
			try {
				code = runScriptAsRoot(HEADER + "  -F || exit\n", res);
				return code;
			} catch (Exception e) {
				return code;
			}
		}
	}

	/**
	 * Check if we have root access
	 * 
	 * @param ctx
	 *            optional context to display alert messages
	 * @return boolean true if we have root
	 */
	public static boolean hasRootAccess() {
		boolean hasroot = false;
		try {
			// Run an empty script just to check root access
			if (runScriptAsRoot("", null, 10000) == 0) {
				hasroot = true;
			}
		} catch (Exception e) {
			return false;
		}
		return hasroot;
	}

	public static void modifyFilePermission() {
		if (!isFileModified) {
			runScriptAsRoot("chmod 777 " + MY_HEADER, null, 5000);
			isFileModified = true;
		}
	}

	public static void modifyFilePermission(String path) {
		runScriptAsRoot("chmod 777 " + path, null, 5000);
	}
}
