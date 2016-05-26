package com.ktc.filemanager.activity;

import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Formatter;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.ktc.filemanager.R;
import com.ktc.filemanager.log.PrintLog;
import com.ktc.filemanager.tools.Tools;
import com.ktc.filemanager.view.ScaleAnimEffect;

public class FirstPageActivity extends Activity
{
	public static final int COLUMN_IMAGE_COUNT = 2;
	private ImageView columnImages[] = new ImageView[COLUMN_IMAGE_COUNT];
	private FrameLayout mFrameLayout[] = new FrameLayout[COLUMN_IMAGE_COUNT];
	private ImageView image_clean;
	private ScaleAnimEffect animEffect;
	private Context mContext;
	private Animation anim_clean;
	private Animation anim_clean_bg;
	public static final int MEMORY_RESULT = 1;

	// 用于清理进程的变量
	private List<ActivityManager.RunningAppProcessInfo> mRunningPros;
	private ActivityManager mActivityManager;
	private int TotalProcess = 0;
	private long availMem = 0;
	private long totalMem = 0;
	private ActivityManager.MemoryInfo memoryInfo;
	public static final int AFTER_CLEAN = 1;
	private int killedProgressNum = 0;

	// 按返回键退出
	private boolean isExit;// 退出的标识
	public static final int EXIT_PRESS = 2;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_firstpage);
		mContext = this;
		animEffect = new ScaleAnimEffect();
		mActivityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		findView();
		totalMem = getTotalMemInfo();
		PrintLog.Info("总大小 " + Formatter.formatFileSize(mContext, totalMem));
		availMem = getAvaiMemInfo();
		for (int i = 0; i < COLUMN_IMAGE_COUNT; i++)
		{
			columnImages[i].setOnFocusChangeListener(foucusChange);
		}
		/**
		 * 一键清理操作
		 */
		columnImages[0].setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				TotalProcess = updateTotalProcessNumb();
				startCleanEffect();
				mRunningPros = mActivityManager.getRunningAppProcesses();
				for (ActivityManager.RunningAppProcessInfo runProInfo : mRunningPros)
				{
					String progressName = runProInfo.processName;
					mActivityManager.killBackgroundProcesses(progressName);
					PrintLog.Debug("杀死进程 :" + progressName);
				}
				killedProgressNum = TotalProcess - updateTotalProcessNumb();
				mHandler.sendEmptyMessageDelayed(AFTER_CLEAN, 2000);
			}

		});

		/**
		 * 进入设备管理
		 */
		columnImages[1].setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				Intent memoryIntent = new Intent();
				memoryIntent.setClass(mContext, DeviceManagerActivity.class);
				startActivityForResult(memoryIntent, MEMORY_RESULT);
			}
		});

	}

	/**
	 * 处理界面显示，显示剩余内存
	 */
	Handler mHandler = new Handler()
	{

		@Override
		public void handleMessage(Message msg)
		{
			super.handleMessage(msg);
			switch (msg.what)
			{
			case AFTER_CLEAN:

				if (killedProgressNum > 0)
				{
					Tools.toastMessage(mContext,
							getResources().getString(R.string.cleaned) + " " + killedProgressNum + " " + getResources().getString(R.string.progress) + " , " + getResources().getString(R.string.release) +" "+ Formatter.formatFileSize(mContext, Math.abs(availMem - getAvaiMemInfo())),
							1);
					availMem = getAvaiMemInfo();
				} else
				{
					Tools.toastMessage(mContext, getResources().getString(R.string.NoCleanUp), 0);
				}
				if (mFrameLayout[0].hasFocus())
				{
					showOnFocusAnimation(0);
				}
				break;
			case EXIT_PRESS:
				isExit = false;
				break;

			default:
				break;
			}
		}

	};

	/**
	 * 初始化界面
	 */
	private void findView()
	{
		columnImages[0] = (ImageView) findViewById(R.id.iv_bg_clean);
		columnImages[1] = (ImageView) findViewById(R.id.iv_bg_memory);
		mFrameLayout[0] = (FrameLayout) findViewById(R.id.fl_clean);
		mFrameLayout[1] = (FrameLayout) findViewById(R.id.fl_memory);
		image_clean = (ImageView) findViewById(R.id.iv_clean_go);
		// startCleanEffect();

	}

	/**
	 * 显示清理 效果
	 */
	private void startCleanEffect()
	{
		//
		anim_clean = AnimationUtils.loadAnimation(mContext, R.anim.go);
		anim_clean.setRepeatCount(1);
		columnImages[0].startAnimation(anim_clean);
		anim_clean_bg = AnimationUtils.loadAnimation(mContext, R.anim.bg_go);
		anim_clean_bg.setRepeatCount(1);
		image_clean.startAnimation(anim_clean_bg);
	}

	/**
	 * 获取焦点是图片放大效果
	 */
	private OnFocusChangeListener foucusChange = new OnFocusChangeListener()
	{

		@Override
		public void onFocusChange(View v, boolean hasFocus)
		{
			switch (v.getId())
			{
			case R.id.iv_bg_clean:
				if (hasFocus)
				{
					showOnFocusAnimation(0);
					return;
				}
				showLooseFocusAinimation(0);
				return;
			case R.id.iv_bg_memory:
				if (hasFocus)
				{
					showOnFocusAnimation(1);
					return;
				}
				showLooseFocusAinimation(1);
				return;

			default:
				break;
			}
		}
	};

	/**
	 * 获取焦点后执行的方法
	 * 
	 * @param i
	 */
	private void showOnFocusAnimation(int i)
	{
		// 将该控件移到最前面
		mFrameLayout[i].bringToFront();
		this.animEffect.setAttributs(1.0F, 1.27F, 1.0F, 1.27F, 100L);
		Animation localAnimation = this.animEffect.createAnimation();
		this.mFrameLayout[i].startAnimation(localAnimation);
	}

	/**
	 * 失去焦点后执行的方法
	 * 
	 * @param i
	 */
	private void showLooseFocusAinimation(int i)
	{
		animEffect.setAttributs(1.27F, 1.0F, 1.27F, 1.0F, 0L);
		mFrameLayout[i].startAnimation(animEffect.createAnimation());
	}

	/**
	 * 获取所有正在运行的进程显示在ListView中
	 */
	public int updateTotalProcessNumb()
	{
		int totalProcNum = mActivityManager.getRunningAppProcesses().size();
		PrintLog.Info("总的进程个数:" + totalProcNum);
		return totalProcNum;
	}

	/**
	 * 读取剩余可用的的内存
	 */
	public long getAvaiMemInfo()
	{
		memoryInfo = new ActivityManager.MemoryInfo();
		mActivityManager.getMemoryInfo(memoryInfo);
		return memoryInfo.availMem;
	}

	/**
	 * 读取到总的内存
	 */
	public long getTotalMemInfo()
	{
		memoryInfo = new ActivityManager.MemoryInfo();
		mActivityManager.getMemoryInfo(memoryInfo);
		return memoryInfo.totalMem;
	}

	/**
	 * 检测遥控器按键，按返回键的效果
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			exit();
			return false;
		} else
		{
			return super.onKeyDown(keyCode, event);
		}
	}

	// 退出的方法
	public void exit()
	{
		if (!isExit)
		{
			isExit = true;
			Tools.toastMessage(mContext, getResources().getString(R.string.onceAgainExit), 0);
			mHandler.sendEmptyMessageDelayed(EXIT_PRESS, 2000);
		} else
		{
			System.exit(0);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
	}

}
