package com.android.callstat.home.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.archermind.callstat.R;

public class SettingFloatBarAdapter extends ArrayAdapter<String> {
	LayoutInflater mInflater;
	Context context;

	/**
	 * @param context
	 * @param textViewResourceId
	 */
	public SettingFloatBarAdapter(Context context, String[] list) {
		super(context, 0, list);
		this.context = context;
		mInflater = LayoutInflater.from(context);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		String string = getItem(position);
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.floatbar_item, null);
		}
		TextView title_tv = (TextView) convertView.findViewById(R.id.title_tv);
		title_tv.setText(string);
		return convertView;
	}
}
