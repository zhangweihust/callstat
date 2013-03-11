package com.archermind.callstat;

/**
 * This class is a container for all of the debug flags used in the Java
 * components of CallStat. These flags must be final in order to ensure that the
 * compiler optimizes the code that uses them out of the final executable.
 * 
 * The name of each flags maps directly to the name of the class in which that
 * flag is used.
 * 
 */
public class DebugFlags {
	public final static String LOGTAG = "callstats";

	public static final boolean CONNECTION_THREAD = true;
	public static final boolean CALLSTAT = true;
	public static final boolean REQUEST_QUEUE = true;
	public static final boolean SYSTEM_FACADE = true;
	public static final boolean CALLBACK_PROXY = true;
	public static final boolean CACHE_MANAGER = true;
	public static final boolean HEADS = true;
	public static final boolean LOAD_LISTENER = true;

}