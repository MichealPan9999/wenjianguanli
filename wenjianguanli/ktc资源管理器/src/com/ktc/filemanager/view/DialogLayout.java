package com.ktc.filemanager.view;

import android.content.Context;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DialogLayout extends LinearLayout {
	private TextView messageTextView;
	private EditText inputEditText;

	public DialogLayout(Context context) {
		super(context);
		// 设置布局为垂直布局
		this.setOrientation(LinearLayout.VERTICAL);
		// 设置对齐方式
		this.setGravity(Gravity.CENTER_HORIZONTAL);

		messageTextView = new TextView(context);
		inputEditText=new EditText(context);
		inputEditText.setSingleLine();
		inputEditText.setSelectAllOnFocus(true);

		// 添加TextView，EditText
		this.addView(messageTextView, new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		this.addView(inputEditText, new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

	}
	//获取TextView对象
	public TextView getMessageTextView(){
		return messageTextView;
	}
	//获取EditText对象
	public EditText getInputEditText(){
		return inputEditText;
	}

}
