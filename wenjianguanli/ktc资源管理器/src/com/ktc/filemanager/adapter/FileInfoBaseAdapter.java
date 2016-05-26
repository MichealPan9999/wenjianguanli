package com.ktc.filemanager.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ktc.filemanager.R;
import com.ktc.filemanager.bean.FileInfo;

public class FileInfoBaseAdapter extends BaseAdapter
{
	// 上下文，表示由哪个Activity使用的
	public Context context;
	// 创建List列表，用于显示ListView的各文件信息
	// 列表中数据只能是FileInfo，每一个FileInfo代表一个文件的信息
	List<FileInfo> fileList = new ArrayList<FileInfo>();

	// 构造方法
	public FileInfoBaseAdapter(Context context)
	{
		this.context = context;
	}

	// 添加一个文件项
	public void addFileItem(FileInfo fileitem)
	{
		fileList.add(fileitem);
	}

	// 判断能否选择
	public boolean isAllFileItemCanSelect()
	{
		return false;
	}

	// 判断文件是否选中
	public boolean isSelectable(int position)
	{
		return fileList.get(position).isSelected();

	}

	// 设置文件的列表
	public void setFileList(List<FileInfo> fileList)
	{
		this.fileList = fileList;
	}

	/**
	 * 获取FileInfo的数量
	 */
	@Override
	public int getCount()
	{
		return fileList.size();
	}

	/**
	 * 获取指定位置的文件信息
	 */

	@Override
	public Object getItem(int position)
	{
		return fileList.get(position);

	}

	/**
	 * 根据当前位置返回当前数据项在List中的行ID
	 */
	@Override
	public long getItemId(int position)
	{
		return position;
	}

	/**
	 * 根据当前数据项的位置返回一个显示在适配器控件上的View。但可以重用以前的view的时候
	 * converView不为空，此时仅仅需要改变converView内容再直接返回covertView
	 * ，否则需要重新创建一个view，用于显示。parent为使用当前适配器的适配器控件
	 */
	@Override
	public View getView(int position,View covertView,ViewGroup parent){
		ViewHolder holder;
		LayoutInflater layoutInflater = LayoutInflater.from(context);
		FileInfo fileinfo = fileList.get(position);
		if (covertView == null)
		{
			covertView = layoutInflater.inflate(R.layout.fileitem, null);
			holder = new ViewHolder();
			holder.name = (TextView) covertView.findViewById(R.id.tv_fname);
			holder.size = (TextView) covertView.findViewById(R.id.tv_fsize);
			holder.date = (TextView) covertView.findViewById(R.id.tv_fdate);
			holder.icon = (ImageView) covertView.findViewById(R.id.iv_ficon);
			covertView.setTag(holder);
		}
		else
		{
			holder = (ViewHolder) covertView.getTag();
		}
		holder.name.setText(fileinfo.getFileName());
		holder.size.setText(fileinfo.getFileSize());
		holder.date.setText(fileinfo.getFileLastUpdateTime());
		holder.icon.setImageDrawable(fileinfo.getfileIcon());
		return covertView;
	}

	class ViewHolder
	{
		TextView name;
		TextView size;
		TextView date;
		ImageView icon;
	}

}
