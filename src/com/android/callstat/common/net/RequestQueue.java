package com.android.callstat.common.net;

import java.util.LinkedList;
import java.util.ListIterator;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.android.callstat.DebugFlags;

public class RequestQueue extends Handler implements RequestFeeder {

	/**
	 * Requests, indexed by Integer (activity id)
	 */
	private final LinkedList<Request> mPending;
	private final Context mContext;
	private final ActivePool mActivePool;

	/* default simultaneous connection count */
	private static final int CONNECTION_COUNT = 4;

	static final int STATUS_GOING = 1;
	static final int STATUS_CANCAL_EXCEPT_GROUP = 2;
	static final int STATUS_CANCAL_SPECIFY_GROUP = 3;
	int mCurrentStatus;

	volatile int mRequestingGroup = -1;

	/**
	 * This class maintains active connection threads
	 */
	class ActivePool {
		/** Threads used to process requests */

		ConnectionThread[] mThreads;
		private int mConnectionCount;

		ActivePool(int connectionCount) {
			mConnectionCount = connectionCount;

			mThreads = new ConnectionThread[mConnectionCount];
			for (int i = 0; i < mConnectionCount; i++) {
				mThreads[i] = new ConnectionThread(mContext, i, null,
						RequestQueue.this);
			}
		}

		void startup() {
			for (int i = 0; i < mConnectionCount; i++) {
				mThreads[i].start();
			}
		}

		void shutdown() {
			for (int i = 0; i < mConnectionCount; i++) {
				mThreads[i].requestStop();
			}
		}

		void startConnectionThread() {
			synchronized (RequestQueue.this) {
				RequestQueue.this.notify();
			}
		}

	}

	public RequestQueue(Context context) {
		this(context, CONNECTION_COUNT);
	}

	public RequestQueue(Context context, int connectionCount) {
		mContext = context;
		mPending = new LinkedList<Request>();
		mActivePool = new ActivePool(connectionCount);
		mActivePool.startup();
	}

	public synchronized void queueRequest(Request request, boolean head) {

		if (!mPending.contains(request)) {
			if (head) {
				mPending.addFirst(request);
			} else {
				mPending.add(request);
			}
		}

		mActivePool.startConnectionThread();
	}

	public synchronized void removeRequest(int requestId) {
		ListIterator<Request> it = mPending.listIterator();

		while (it.hasNext()) {
			if (it.next().getRequestId() == requestId) {
				it.remove();
			}
		}
		if (DebugFlags.REQUEST_QUEUE) {
			Log.v(DebugFlags.LOGTAG, "removeRequests: groupID = " + requestId);
		}
	}

	public synchronized void removeAllRequest() {
		mPending.clear();
	}

	/*
	 * RequestFeeder implementation
	 */
	@Override
	public synchronized Request getRequest() {
		Request ret = null;

		if (!mPending.isEmpty()) {
			ret = mPending.removeFirst();
			if (DebugFlags.REQUEST_QUEUE)
				HttpLog.v("DEBUG_TEMP:removeFirst:" + mRequestingGroup);
		}
		if (DebugFlags.REQUEST_QUEUE) {
			HttpLog.v("RequestQueue.getRequest() => " + ret);
		}
		return ret;
	}

	/**
	 * @return a request for given host if possible
	 */
	@Override
	public synchronized Request getRequest(int requestId) {
		Request ret = null;
		ListIterator<Request> it = mPending.listIterator();

		while (it.hasNext()) {
			ret = it.next();
			if (ret.getRequestId() == requestId) {
				ret = it.next();
				it.remove();
			}
		}
		if (DebugFlags.REQUEST_QUEUE)
			HttpLog.v("RequestQueue.getRequest(" + requestId + ") => " + ret);
		return ret;
	}

	/**
	 * @return true if a request for this group is available
	 */
	@Override
	public synchronized boolean haveRequest(int requestId) {
		ListIterator<Request> it = mPending.listIterator();

		while (it.hasNext()) {
			if (it.next().getRequestId() == requestId) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Put request back on head of queue
	 */
	@Override
	public synchronized void requeueRequest(Request request) {
		queueRequest(request, true);
	}

	/**
	 * This must be called to cleanly shutdown RequestQueue
	 */
	public void shutdown() {
		mActivePool.shutdown();
	}
}