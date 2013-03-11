package com.archermind.callstat.home.views;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.archermind.callstat.R;

public class CustomListDialog implements OnClickListener, OnItemClickListener {
	private Dialog dialog;
	private Context context;
	private Button buttonOk;
	private String[] data;
	private int index;
	private ListView listView;
	private TextView title;
	private CustonListAdapter custonListAdapter;

	private DialogInterface.OnClickListener onClickListener_ok,
			onClickListener_select;

	public CustomListDialog(Context context) {
		this.context = context;
		dialog = new Dialog(context, R.style.myDialogTheme);
		dialog.setContentView(R.layout.custom_list_dialog);
		initView();
	}

	private void initView() {
		buttonOk = (Button) dialog.findViewById(R.id.ok_btn);
		buttonOk.setOnClickListener(this);
		title = (TextView) dialog.findViewById(R.id.title);
		listView = (ListView) dialog.findViewById(R.id.listView);
		listView.setOnItemClickListener(this);
	}

	private void setListViewHeightBasedOnChildren(ListView listView) {
		ListAdapter listAdapter = listView.getAdapter();
		if (listAdapter == null) {
			// pre-condition
			return;
		}
		int count = listAdapter.getCount();
		if (count > 5) {
			count = 5;
		}
		int totalHeight = 0;
		for (int i = 0; i < count; i++) {
			View listItem = listAdapter.getView(i, null, listView);
			listItem.measure(0, 0);
			totalHeight += listItem.getMeasuredHeight();
		}

		ViewGroup.LayoutParams params = listView.getLayoutParams();
		params.height = totalHeight
				+ (listView.getDividerHeight() * (listAdapter.getCount() - 1));
		listView.setLayoutParams(params);
	}

	public void setTitle(int resId) {
		this.title.setText(context.getString(resId));
	}

	public void setTitle(String title) {
		this.title.setText(title);
	}

	public void setPositiveButton(String buttonOk,
			DialogInterface.OnClickListener onClickListener_ok) {
		this.buttonOk.setText(buttonOk);
		this.onClickListener_ok = onClickListener_ok;
	}

	public void setSingleChoiceItems(String[] data, int position,
			DialogInterface.OnClickListener onClickListener_select) {
		this.data = data;
		this.index = position;
		this.onClickListener_select = onClickListener_select;
	}

	public void setSingleChoiceItems(int resId, int position,
			DialogInterface.OnClickListener onClickListener_select) {
		this.data = context.getResources().getStringArray(resId);
		this.index = position;
		this.onClickListener_select = onClickListener_select;
	}

	public Dialog getDialog() {
		return dialog;
	}

	public void show() {
		custonListAdapter = new CustonListAdapter();
		listView.setAdapter(custonListAdapter);
		setListViewHeightBasedOnChildren(listView);
		listView.setSelectionFromTop(index, 0);
		onClickListener_select.onClick(dialog, index);
		dialog.show();
	}

	public void dismiss() {
		dialog.dismiss();
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.ok_btn:
			onClickListener_ok.onClick(dialog, index);
			dismiss();
			break;

		}

	}

	class CustonListAdapter extends BaseAdapter {
		private LayoutInflater inflater;

		public CustonListAdapter() {
			inflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return data.length;
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return data[position];
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			ViewHodler hodler = null;
			// if(convertView == null){
			hodler = new ViewHodler();
			convertView = inflater.inflate(R.layout.custom_list_dialog_item,
					null);
			hodler.titleText = (TextView) convertView
					.findViewById(R.id.textView);
			hodler.layout = (LinearLayout) convertView
					.findViewById(R.id.custom_layout);
			hodler.radioButton = (RadioButton) convertView
					.findViewById(R.id.radio_button);
			convertView.setTag(hodler);
			// }else{
			// hodler = (ViewHodler) convertView.getTag();
			// }

			hodler.titleText.setText(data[position]);
			if (index == position) {
				hodler.radioButton.setChecked(true);
			} else {
				hodler.radioButton.setChecked(false);
			}

			return convertView;
		}

		class ViewHodler {
			private LinearLayout layout;
			private TextView titleText;
			private RadioButton radioButton;
		}

	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		// TODO Auto-generated method stub
		index = position;
		custonListAdapter.notifyDataSetChanged();
		onClickListener_select.onClick(dialog, position);
	}

}
