package com.ktc.filemanager.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.ktc.filemanager.R;
import com.ktc.filemanager.activity.DeviceManagerActivity;

/**
 * 文件操作类
 * 
 * @author Administrator
 * 
 */
public class FileOperaUtil
{
	/**
	 * 递归删除文件
	 * 
	 * @param file
	 *            要删除的文件或者文件夹
	 * @throws IOException
	 *             文件找不到或者删除错误的时候抛出
	 * */
	public static void deleteAll(File file) throws IOException
	{
		// 文件夹不存在不存在
		if (file == null || !file.exists())
		{
			// throw new IOException("指定目录不存在:"+file.getName());
			Tools.toastMessage(DeviceManagerActivity.cts, (DeviceManagerActivity.cts.getResources().getString(R.string.folderNotExist) + file.getName()), 0);
			return;
		}
		boolean rslt = true;// 保存中间结果
		if (!(rslt = file.delete()))
		{// 先尝试直接删除
			// 若文件夹非空。枚举、递归删除里面内容
			File subs[] = file.listFiles();
			if (subs != null)
			{
				for (int i = 0; i <= subs.length - 1; i++)
				{
					if (subs[i].isDirectory())
						deleteAll(subs[i]);// 递归删除子文件夹内容
					rslt = subs[i].delete();// 删除子文件夹本身
				}
				rslt = file.delete();// 删除此文件夹本身
			}
		}
		if (!rslt)
		{
			Tools.toastMessage(DeviceManagerActivity.cts, DeviceManagerActivity.cts.getResources().getString(R.string.cannotremove), 0);
			throw new IOException("无法删除:" + file.getName());
		}
		return;
	}

	/**
	 * 移动文件
	 * 
	 * @param source
	 *            需要移动的文件的路径
	 * @param destination
	 *            目标路径
	 */
	public static void moveFile(String source, String destination)
	{
		new File(source).renameTo(new File(destination));
	}

	/**
	 * 移动文件
	 * 
	 * @param source
	 *            需要移动的文件
	 * @param destination
	 *            目标文件
	 */
	public static void moveFile(File source, File destination)
	{
		source.renameTo(destination);
	}

	@SuppressWarnings("resource")
	public static int getFileSize(File file)
	{

		FileInputStream fis;
		int fileLen = 0;
		if (file.isDirectory())
		{
			for (File subfile : file.listFiles())
			{
				fileLen += getFileSize(subfile);
			}
		} else
		{
			try
			{
				fis = new FileInputStream(file);
				fileLen = fis.available();
			} catch (FileNotFoundException e)
			{
				e.printStackTrace();
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return fileLen;
	}
}
