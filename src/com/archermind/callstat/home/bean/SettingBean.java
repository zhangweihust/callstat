package com.archermind.callstat.home.bean;

public class SettingBean {
	private int image;
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public SettingBean() {

	}

	public SettingBean(String name) {
		this.name = name;
	}

	public int getImage() {
		return image;
	}

	public void setImage(int image) {
		this.image = image;
	}

}
