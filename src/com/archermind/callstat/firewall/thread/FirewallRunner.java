package com.archermind.callstat.firewall.thread;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * this class is a runner for a set script of ip rules it runs as a thread to
 * avoid blocking other components
 * 
 * @Author longX
 **/
public class FirewallRunner extends Thread {

	private final String script;
	private final StringBuilder res;
	public int exitcode = -1;
	private Process exec;

	/**
	 * Creates a new script runner.
	 * 
	 * @param script
	 *            script to run
	 * @param res
	 *            response output
	 */
	public FirewallRunner(String script, StringBuilder res) {
		this.script = script;
		this.res = res;
	}

	@Override
	public void run() {
		try {
			// Create the "su" request to run the command
			// using "su" as root operator in linux os
			// note that this will create a shell that we must interact to
			// (using stdin/stdout)
			exec = Runtime.getRuntime().exec("su");
			final OutputStreamWriter out = new OutputStreamWriter(
					exec.getOutputStream());
			// Write the script to be executed
			// Log.e("callstats", "final script:" + script);
			out.write(script);
			// Ensure that the last character is an "enter"
			if (!script.endsWith("\n"))
				out.write("\n");
			out.flush();
			// Terminate the "su" process
			out.write("exit\n");
			out.flush();
			final char buf[] = new char[1024];
			// Consume the "stdout"
			InputStreamReader r = new InputStreamReader(exec.getInputStream());
			int read = 0;
			while ((read = r.read(buf)) != -1) {
				if (res != null)
					res.append(buf, 0, read);
			}
			// Consume the "stderr"
			r = new InputStreamReader(exec.getErrorStream());
			read = 0;
			while ((read = r.read(buf)) != -1) {
				if (res != null)
					res.append(buf, 0, read);
			}
			// get the process exit code
			if (exec != null)
				this.exitcode = exec.waitFor();
		} catch (InterruptedException ex) {
			if (res != null)
				res.append("\nOperation timed-out");
		} catch (Exception ex) {
			if (res != null)
				res.append("\n" + ex);
		} finally {
			destroy();
		}
	}

	/**
	 * Destroy this script runner
	 */
	public synchronized void destroy() {
		if (exec != null)
			exec.destroy();
		exec = null;
	}

}
