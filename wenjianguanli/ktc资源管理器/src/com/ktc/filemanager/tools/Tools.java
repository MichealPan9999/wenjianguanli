package com.ktc.filemanager.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Debug;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

public class Tools
{

	public static String getNameByPackageName(Context context, String packageName)
	{

		PackageManager pm = context.getPackageManager();
		String name = "";
		try
		{
			name = pm.getApplicationLabel(pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)).toString();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return name;
	}

	public static ArrayList<String> getRunningTaskInfo(ActivityManager am, int maxNum)
	{
		// 新建一个String类型的ArrayList，用来存放数据
		ArrayList<String> arrayListPro = new ArrayList<String>();
		List<ActivityManager.RunningTaskInfo> mRunningTasks = am.getRunningTasks(maxNum);
		// 顺序枚举每个元素，使用ArrayList<String>类型的add方法添加元素
		for (ActivityManager.RunningTaskInfo amTask : mRunningTasks)
			arrayListPro.add(amTask.baseActivity.getClassName() + "(ID=" + amTask.id + ")");
		return arrayListPro;
	}

	public static ArrayList<String> getRunningServiceInfo(ActivityManager am, int maxNum)
	{
		// 新建一个String类型的ArrayList，用来存放数据
		ArrayList<String> arrayListPro = new ArrayList<String>();
		List<ActivityManager.RunningServiceInfo> mRunningServices = am.getRunningServices(maxNum);
		// 顺序枚举每个元素，使用ArrayList<String>类型的add方法添加元素
		for (ActivityManager.RunningServiceInfo amService : mRunningServices)
			arrayListPro.add("服务所在的进程名: " + amService.process + "(ID=" + amService.pid + "\nUID：" + amService.uid + ")");
		return arrayListPro;
	}

	public static ArrayList<String> getAllRunningAppProgressInfo(Context context, ActivityManager am)
	{
		List<ActivityManager.RunningAppProcessInfo> mRunningPros = am.getRunningAppProcesses();
		ArrayList<String> arrayListpro = new ArrayList<String>();
		for (RunningAppProcessInfo runInfo : mRunningPros)
		{
			// 获取该进程占用的内存
			int[] mMemId = new int[] { runInfo.pid };
			//
			Debug.MemoryInfo[] memoryInfos = am.getProcessMemoryInfo(mMemId);
			double memSize = memoryInfos[0].dalvikPrivateDirty / 1024.0;
			int temp = (int) (memSize * 100);
			memSize = temp / 100.0;
			String ProInfo = "";
			String packageName = runInfo.processName;
			String apkName = getNameByPackageName(context, packageName);
			ProInfo += " Name: " + packageName + (!TextUtils.isEmpty(apkName) ? "\r\n apkName: " + (apkName) : "") + "\r\n ID: " + runInfo.pid + "\r\n Memory: " + memSize + " MB\r\n";
			arrayListpro.add(ProInfo);
		}
		return arrayListpro;
	}

	/**
	 * 获取总的运行内存大小，通过读取/proc/meminfo中的配置信息
	 * 
	 * @return
	 */
	public static long getTotalRam()
	{
		try
		{
			File file = new File("/proc/meminfo");
			FileInputStream fis = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			String totalRam = br.readLine();
			StringBuffer sbuffer = new StringBuffer();
			char[] cs = totalRam.toCharArray();
			for (char c : cs)
			{
				if (c >= '0' && c <= '9')
				{
					sbuffer.append(c);
				}
			}
			long result = Long.parseLong(sbuffer.toString()) * 1024;
			if (br != null)
			{
				br.close();
				br = null;
			}
			if (fis != null)
			{
				fis.close();
				fis = null;
			}
			return result;
		} catch (NumberFormatException e)
		{
			e.printStackTrace();
			return 0;
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
			return 0;
		} catch (IOException e)
		{
			e.printStackTrace();
			return 0;
		}
	}


	public static boolean isRoot()
	{
		boolean bool = false;
		if ((!new File("/system/bin/su").exists()) && (!new File("system/xbin/su").exists()))
		{
			bool = false;
		} else
		{
			bool = true;
		}
		return bool;
	}

	/**
	 * 使用toast方法显示消息
	 * @param context
	 * @param msg 要显示的内容
	 * @param duration 显示持续时间 0表示Toast.LENGTH_SHORT，1表示 Toast.LENGTH_LONG
	 */
	public static void toastMessage(Context context,String msg,int duration)
	{
		if (duration == 0)
		{
			Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
			
		}
		else if (duration == 1)
		{
			Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
		}
	}
	
	/**
	 * 创建并显示提示信息的对话框
	 * 
	 * @param title
	 *            标题
	 * @param message
	 *            显示的内容
	 */
	public static void showMessageDialog(Context context,String title, String message)
	{
		Builder builder = new Builder(context);
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.cancel();
			}
		});
		builder.setCancelable(false);
		builder.create();
		builder.show();
	}
	
	/**
	 * 弹出确认对话框
	 * 
	 * @param title
	 *            标题
	 * @param dialogview
	 *            显示的布局
	 * @param positiveButtonEventHandle点击确认按钮后需要做的事件处理类
	 * @param negativeButtonEventHandle点击否定按钮后需要做的事件处理类
	 * @param cancleButtonEventHandle点击取消按钮后需要做的事件处理类
	 */
	public static void showCustomDialog(Context context,String title, View dialogview, DialogInterface.OnClickListener positiveButtonEventHandle, DialogInterface.OnClickListener negativeButtonEventHandle, DialogInterface.OnCancelListener cancelButtonEventHandle)
	{
		Builder builder = new Builder(context);
		builder.setTitle(title);
		builder.setView(dialogview);
		// 确定按钮的监听事件
		builder.setPositiveButton(android.R.string.ok, positiveButtonEventHandle);
		// 取消按钮的事件监听
		builder.setNegativeButton(android.R.string.cancel, negativeButtonEventHandle);
		builder.setOnCancelListener(cancelButtonEventHandle);
		builder.create();
		builder.show();
	}
	
	/**
	 * 弹出确认对话框
	 * 
	 * @param title
	 *            标题
	 * @param message
	 *            显示的内容
	 * @param positiveButtonEventHandle点击确认按钮后需要做的事件处理类
	 * @param negativeButtonEventHandle点击否定按钮后需要做的事件处理类
	 */
	public static void confirmDialog(Context context,String title, String message, DialogInterface.OnClickListener positiveButtonEventHandle, DialogInterface.OnClickListener negativeButtonEventHandle)
	{
		Builder builder = new Builder(context);
		builder.setTitle(title);
		builder.setMessage(message);
		// 确定按钮的监听事件
		builder.setPositiveButton(android.R.string.ok, positiveButtonEventHandle);
		// 取消按钮的事件监听
		builder.setNegativeButton(android.R.string.cancel, negativeButtonEventHandle);
		builder.setCancelable(false);
		builder.create().show();
	}

}
