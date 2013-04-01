package com.android.callstat.common.net;

interface RequestFeeder {

	Request getRequest();

	Request getRequest(int requestId);

	/**
	 * @return true if a request for this host is available
	 */
	boolean haveRequest(int requestId);

	/**
	 * Put request back on head of queue
	 */
	void requeueRequest(Request request);

}
