package com.android.callstat.home.bean;

public class HelpBean {

	private boolean isQueryItem = true;

	private String question;

	private String answer;

	private int map;

	public boolean isQueryItem() {
		return isQueryItem;
	}

	public void setQueryItem(boolean isQueryItem) {
		this.isQueryItem = isQueryItem;
	}

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public String getAnswer() {
		return answer;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}

	public int getMap() {
		return map;
	}

	public void setMap(int map) {
		this.map = map;
	}

	public HelpBean(String question, String answer, boolean isQueryItem, int map) {
		this.question = question;
		this.answer = answer;
		this.isQueryItem = isQueryItem;
		this.map = map;
	}

}
