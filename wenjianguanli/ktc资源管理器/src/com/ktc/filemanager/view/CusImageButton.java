package com.ktc.filemanager.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class CusImageButton extends RelativeLayout
{

	private TextView text;
	private ImageView image;

	public void setText(int resId)
	{
		this.text.setText(resId);
	}
	public void setText(String str)
	{
		this.text.setText(str);
	}
	public void setTextColor(int color)
	{
		this.text.setText(color);
	}

	public void setImageResource(int resId)
	{
		this.image.setImageResource(resId);
	}

	public CusImageButton(Context context,int imageResId, int textResId,String msg)
	{
		super(context);
		text = new TextView(context);
		image = new ImageView(context);
		text.setId(0x10000010);
		image.setId(0x10000011);
		setImageResource(imageResId);
		LayoutParams layoutOne = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		layoutOne.addRule(RelativeLayout.ALIGN_LEFT,RelativeLayout.TRUE);
		image.setMaxHeight(66);
		image.setMaxWidth(66);
		image.setScaleType(ImageView.ScaleType.CENTER);
		image.setAdjustViewBounds(true);
		image.setLayoutParams(layoutOne);
		this.addView(image);
		
		LayoutParams layoutTwo = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		layoutTwo.addRule(RelativeLayout.RIGHT_OF, image.getId());
		layoutTwo.leftMargin = 20;
		layoutTwo.addRule(RelativeLayout.CENTER_IN_PARENT);
		text.setLayoutParams(layoutTwo);
		if (textResId == -1)
		{
			text.setText(msg);
		}
		else
		{
			text.setText(textResId);
		}
		text.setTextSize(22);
		text.setTextColor(ColorStateList.valueOf(0xFF000000));
		this.addView(text);
	}
	
	

	
}
