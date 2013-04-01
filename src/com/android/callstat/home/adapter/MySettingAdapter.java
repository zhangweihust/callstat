package com.android.callstat.home.adapter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.callstat.home.bean.SettingBean;
import com.archermind.callstat.R;

public class MySettingAdapter extends BaseAdapter {
	private Context mCtx;
	private List<SettingBean> settingBeans;
	private LayoutInflater inflater;

	public MySettingAdapter(Context ctx, List<SettingBean> settingBeans) {
		mCtx = ctx;
		this.settingBeans = settingBeans;
		inflater = LayoutInflater.from(ctx);
	}

	@Override
	public int getCount() {
		return settingBeans.size();
	}

	@Override
	public Object getItem(int position) {
		return settingBeans.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		SettingBean sb = settingBeans.get(position);
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.setting_popup_window_item,
					null);
			TextView name = (TextView) convertView.findViewById(R.id.iv);
			ImageView image = (ImageView) convertView.findViewById(R.id.bt);
			name.setText(sb.getName());
			if (position == 0) {
				image.setBackgroundResource(R.drawable.more_system_btn_selector);
			}
			if (position == 1) {
				image.setBackgroundResource(R.drawable.more_share_btn_selector);
			}
		}

		return convertView;
	}

}