package com.ktc.filemanager.bean;

import android.graphics.drawable.Drawable;

/**
 * 代表一个文件夹或文件的信息，实现Comparable接口，用于比较文件名称
 * 
 * @author Administrator
 * 
 */
public class FileInfo implements Comparable<FileInfo> {
	// 文件名称
	private String fileName = "";
	// 文件上次修改的时间
	private String fileLastUpdateTime = "";
	//文件的大小
	private String fileSize="";
	// 文件的图标
	private Drawable fileIcon = null;
	// 能否被选中
	private boolean Selectable = true;

	// 默认的构造方法
	public FileInfo() {

	}

	// 构造方法
	public FileInfo(String fileName, String fileLastUpdateTime,String fileSize,
			Drawable fileIcon) {
		super();
		this.fileName = fileName;
		this.fileLastUpdateTime = fileLastUpdateTime;
		this.fileIcon = fileIcon;
		this.fileSize=fileSize;

	}

	// 获得文件名称
	public String getFileName() {
		return fileName;

	}

	// 设置文件的名称
	public String setFileName() {
		return fileName;

	}

	// 获得文件上次修改的时间
	public String getFileLastUpdateTime() {
		return fileLastUpdateTime;
	}

	// 设置文件的上次修改的时间
	public String setFileLastUpdateTime() {
		return fileLastUpdateTime;
	}
	
	// 获得文件大小
	public String getFileSize() {
		return fileSize;
	}

	// 设置文件的大小
	public String setFileSize(){
		return fileSize;
	}

	// 获得文件的图标
	public Drawable getfileIcon() {
		return fileIcon;
	}

	// 设置文件的图标
	public void setfileIcon(Drawable fileIcon) {
		this.fileIcon = fileIcon;
	}

	// 是否可以选中
	public boolean isSelected() {
		return Selectable;
	}

	// 设置是否可以选中
	public void setSelectable(boolean selectable) {
		Selectable = selectable;
	}

	@Override
	public int compareTo(FileInfo another) {
		// 如果当前文件不为空且比较的文件不为空
		if (this.fileName != null && another != null) {
			// 比较文件名
			return this.fileName.compareTo(another.getFileName());
		} else {
			throw new IllegalArgumentException("文件不存在");
		}

	}

}
