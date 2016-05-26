package com.ktc.filemanager.activity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.ktc.filemanager.R;
import com.ktc.filemanager.adapter.FileInfoBaseAdapter;
import com.ktc.filemanager.bean.FileInfo;
import com.ktc.filemanager.log.PrintLog;
import com.ktc.filemanager.tools.FileOperaUtil;
import com.ktc.filemanager.tools.StoragePathsManager;
import com.ktc.filemanager.tools.Tools;
import com.ktc.filemanager.view.CusImageButton;
import com.ktc.filemanager.view.DialogLayout;

public class DeviceManagerActivity extends Activity
{
	// private LinearLayout layout;

	// 表示当前用于显示文件列表的ListView对象
	private ListView fileListView = null;

	// 临时文件，用于粘贴，复制时候使用
	private File myTmpFile = null;
	// 判断是否是粘贴，不是复制
	private boolean isCut = false;

	// 文件列表，存放当前目录下的全部文件
	private List<FileInfo> fileList = new ArrayList<FileInfo>();
	// private final String ROOTPARH = "/";
	// 当前目录，默认根目录
	private File nowDirectory = new File("/");
	// 退出的标识
	private boolean isExit;

	// 菜单按钮标识
	private final int MENU_EXIT = 0;
	private Context mContext;
	public static Context cts;
	// 顶部自定义按键
	private RelativeLayout layoutTopUpLevel;
	private LinearLayout layoutTopSdcard;

	// 检测usb设备
	private StoragePathsManager mStorageManager;
	private List<String> allMountedStorage;
	private CusImageButton[] BtnUsbDevice;
	private int deviceLen = 0;
	private static final int BUTTON_ID = 0x10001111;

	// 菜单对话框相关
	private LayoutInflater mInflater;
	private File CurrentSelectedItem;
	// 用于mHandler的刷新
	private static final int EXIT = 0;
	private static final int REFRESHDIR = 1;
	private ProgressDialog progressDialog;
	private CopyFileAsyncTask copyTask;

	// 用于复制文件夹
	long targetFileSize = 0;
	long srcFileSize = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mContext = DeviceManagerActivity.this;
		setContentView(R.layout.activity_main);
		cts = this;
		PrintLog.Info("是否获取root权限" + Tools.isRoot());
		// 初始化组件
		initViews();
		initProgressDialog(mContext);
		// 初始化浏览目录
		listItemclick();
		listItemSelected();
	}

	/**
	 * 初始化进度条对话框
	 */
	private void initProgressDialog(Context context)
	{
		progressDialog = new ProgressDialog(context);

		progressDialog.setOnCancelListener(new OnCancelListener()
		{
			public void onCancel(DialogInterface dialog)
			{
				dialog.cancel();
				openOrBrowseTheFile(nowDirectory);
				Tools.toastMessage(mContext, getResources().getString(R.string.gotobacak), 1);
			}
		});

		progressDialog.setCancelable(true);
		progressDialog.setMax(100);
		progressDialog.setMessage(getResources().getString(R.string.copydialogmessage));
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	}

	/**
	 * 顶部自定义按键以及ListView初始化
	 */
	private void initViews()
	{
		registerSdcardReceiver();
		layoutTopUpLevel = (RelativeLayout) findViewById(R.id.layout_top_uplevel);

		CusImageButton ButtonUpLevel = new CusImageButton(mContext, R.drawable.uponelevel, R.string.up_one_level, null);
		layoutTopUpLevel.addView(ButtonUpLevel);
		layoutTopUpLevel.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				upOnLevel();
			}
		});
		mStorageManager = new StoragePathsManager(mContext);
		refreshUsbDeviceList();

		// 获取ListView
		fileListView = (ListView) findViewById(R.id.lv_files);
		fileListView.setVisibility(View.VISIBLE);
	}

	/**
	 * 是否处于usb设备根目录下
	 * 
	 * @return
	 */
	private boolean atRootPath(File filepath)
	{
		if (filepath.getAbsoluteFile().toString().equals("/"))
		{
			return true;
		}
		if (allMountedStorage != null && allMountedStorage.size() > 0)
		{
			String currentPath = filepath.getAbsoluteFile().toString();
			for (String mountedStorage : allMountedStorage)
			{
				if (currentPath.equals(mountedStorage))
				{
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 检测并更新 usb设备的列表 列出所有usb设备
	 */
	private void refreshUsbDeviceList()
	{
		// 检测所有挂载的设备
		layoutTopSdcard = (LinearLayout) findViewById(R.id.layout_top_sdcard1);
		layoutTopSdcard.setVisibility(View.GONE);
		layoutTopSdcard.removeAllViews();

		if (allMountedStorage != null && allMountedStorage.size() > 0)
		{
			allMountedStorage.clear();
		}
		allMountedStorage = mStorageManager.getMountedStoragePaths();
		deviceLen = allMountedStorage.size();
		if (deviceLen > 0)
		{
			BtnUsbDevice = new CusImageButton[deviceLen];
			for (int i = 0; i < deviceLen; i++)
			{
				final String currentDevice = allMountedStorage.get(i);
				BtnUsbDevice[i] = new CusImageButton(mContext, R.drawable.goroot, -1, currentDevice);
				BtnUsbDevice[i].setId(BUTTON_ID + i);
				layoutTopSdcard.addView(BtnUsbDevice[i]);
				BtnUsbDevice[i].setFocusable(true);
				BtnUsbDevice[i].setClickable(true);
				BtnUsbDevice[i].setBackground(getResources().getDrawable(R.drawable.list_selector_background));
				BtnUsbDevice[i].setOnClickListener(new OnClickListener()
				{

					@Override
					public void onClick(View v)
					{
						nowDirectory = new File(currentDevice);
						openOrBrowseTheFile(nowDirectory);
						CurrentSelectedItem = nowDirectory;
					}
				});
				/*
				 * 用于按键获取焦点时获取到刷新当前界面
				 * BtnUsbDevice[i].setOnFocusChangeListener(new
				 * OnFocusChangeListener() {
				 * 
				 * @Override public void onFocusChange(View v, boolean hasFocus)
				 * { if (hasFocus) { nowDirectory = new File(currentDevice);
				 * openOrBrowseTheFile(nowDirectory); CurrentSelectedItem =
				 * nowDirectory; } } });
				 */
			}
			layoutTopSdcard.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * ListView选中以后的监听操作，获取到当前路径 将当前获取焦点的地址保存在全局变量 CurrentSelectedItem 中。
	 */
	private void listItemSelected()
	{
		fileListView.setOnItemSelectedListener(new OnItemSelectedListener()
		{

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
			{
				CurrentSelectedItem = getCurrentItemPath(position);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent)
			{
				PrintLog.Debug("onNothingSelected ---");
			}
		});
	}

	/**
	 * 注册ListView的事件监听动作 单击ListView中某个Item项，如果是文件夹则进入文件夹，如果是文件则执行对文件操作
	 */
	private void listItemclick()
	{
		fileListView.setOnItemClickListener(new OnItemClickListener()
		{

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{

				File clickedFile = getCurrentItemPath(position);
				if (clickedFile != null)
				{
					if (clickedFile.isDirectory() && clickedFile.listFiles() != null)
					{
						openOrBrowseTheFile(clickedFile);

					} else if (clickedFile.isDirectory() && clickedFile.listFiles() == null)
					{
						Toast.makeText(mContext, R.string.limits_of_authority, Toast.LENGTH_LONG).show();
					} else
					{
						openFileOperateMenu(clickedFile);
					}
				}

			}

		});
	}

	/**
	 * 通过判断文件名的后缀判断文件是什么类型的文件
	 */
	public boolean checkFileType(String fileName, String[] extendName)
	{
		// 遍历后缀名称集合 aEnd是临时变量 遍历
		for (String aEnd : extendName)
		{
			// 判断后缀名名称是否存在在数组中
			if (fileName.toLowerCase(Locale.getDefault()).endsWith(aEnd))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * 创建菜单，换回主界面
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_EXIT, 0, getResources().getString(R.string.backhome));
		return true;
	}

	/**
	 * 点击菜单选项触发的事件
	 */
	public boolean onOptionsItemSelected(MenuItem item)
	{
		super.onOptionsItemSelected(item);
		switch (item.getItemId())
		{
		case MENU_EXIT:
			Intent exitIntent = new Intent(Intent.ACTION_MAIN);
			exitIntent.addCategory(Intent.CATEGORY_HOME);
			setResult(RESULT_OK, exitIntent);
			finish();
			break;
		}

		return false;
	}

	/**
	 * 浏览根目录所有文件
	 */
	public void browseTheRootAllFile()
	{
		openOrBrowseTheFile(nowDirectory);
	}

	/**
	 * 返回上一级目录 如果当前是根目录或者usb设备根目录则点击返回上一级无效
	 */
	public void upOnLevel()
	{
		if (atRootPath(nowDirectory))
		{
			return;
		}
		// 如果目录不为空
		if (this.nowDirectory.getParent() != null)
		{
			// 浏览父文件目录
			this.openOrBrowseTheFile(this.nowDirectory.getParentFile());
		}
	}

	/**
	 * 显示处理文件的菜单，包括打开，重命名等
	 */
	private void openFileOperateMenu(File file)
	{
		// 去掉打开功能
		String[] menu = getResources().getStringArray(R.array.operate_opt);
		new AlertDialog.Builder(DeviceManagerActivity.this).setTitle(getResources().getString(R.string.filetitle)).setItems(menu, new FileClickListener(file)).show();

	}

	/**
	 * 打开文件，或者是浏览文件
	 */
	public void openOrBrowseTheFile(File file)
	{
		if (file.isDirectory())// 如果是文件目录
		{
			if (file.listFiles() != null)
			{
				// 将当前目录更新为指定浏览的文件夹
				this.nowDirectory = file;
				// 将标题设置为当前目录
				this.setTitle(file.getAbsolutePath());
				fillListView(file.listFiles());
			} else if (!file.canRead())
			{
				Tools.toastMessage(mContext, getResources().getString(R.string.limits_of_authority), 1);
			}

		}
	}

	/**
	 * 将指定的子文件全部装入列表ListView中
	 * 
	 * @param files
	 */
	private void fillListView(File[] files)
	{
		// 清空列表
		this.fileList.clear();

		// 当前图标，显示在ListView
		Drawable currentIcon = null;
		// 创建SimpleDateFormat对象，用于格式化时间
		SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		// 遍历文件数组
		for (File file : files)
		{
			// 获取文件名
			String fileName = file.getName();
			// 判断是一个文件夹还是一个文件
			if (file.isDirectory())
			{
				// 如果是一个文件夹，则设置图片为文件夹图片
				currentIcon = getResources().getDrawable(R.drawable.folder);

			} else
			{
				// 判断文件是否为音乐文件
				if (checkFileType(fileName, getResources().getStringArray(R.array.fileEndingAudio)))
				{
					// 设置音乐的图标
					currentIcon = getResources().getDrawable(R.drawable.audio);
				}
				// 判断文件是否为视频文件
				else if (checkFileType(fileName, getResources().getStringArray(R.array.fileEndingVideo)))
				{
					// 设置视频图标
					currentIcon = getResources().getDrawable(R.drawable.video);
				}
				// 判断文件是否为图片文件
				else if (checkFileType(fileName, getResources().getStringArray(R.array.fileEndingImage)))
				{
					// 设置图片的图标
					currentIcon = getResources().getDrawable(R.drawable.image);
				}

				// 判断文件是否为网页文件
				else if (checkFileType(fileName, getResources().getStringArray(R.array.fileEndingWebText)))
				{
					// 设置网页的图标
					currentIcon = getResources().getDrawable(R.drawable.webtext);
				}

				// 判断文件是否为压缩包文件
				else if (checkFileType(fileName, getResources().getStringArray(R.array.fileEndingPackage)))
				{
					currentIcon = getResources().getDrawable(R.drawable.packed);
				}
				// 判断是否为APK文件
				else if (checkFileType(fileName, getResources().getStringArray(R.array.fileEndingAPK)))
				{

					currentIcon = getResources().getDrawable(R.drawable.apk);
					// 在这里显示apk文件的图标，一定要注意拔出usb设备的情况
					/*
					 * String ApkFilePath = nowDirectory + "/" +
					 * file.getName();// 安裝包路徑 boolean dosFileExist = new
					 * File(ApkFilePath).exists(); if (dosFileExist) {
					 * currentIcon = getApkIcon(mContext, ApkFilePath); } else {
					 * currentIcon = getResources().getDrawable(R.drawable.pkg);
					 * }
					 */
				}
				// 判断文件是否为其他文件
				else
				{
					// 默认图标
					currentIcon = getResources().getDrawable(R.drawable.text);
				}

			}

			// 获取上次修改的时间
			Date date = new Date(file.lastModified());
			// 格式化数据显示
			String updateTime = dateformat.format(date);
			// 获得文件的大小
			long TmpFileSize = file.length();
			String fileSize = Formatter.formatFileSize(mContext, TmpFileSize);
			// 添加FileInfo对象
			if (!file.isDirectory())
			{
				this.fileList.add(new FileInfo(fileName, updateTime, fileSize, currentIcon));
			} else
			{
				this.fileList.add(new FileInfo(fileName, updateTime, getResources().getString(R.string.document_type), currentIcon));
			}

		}// for循环结束

		// 排序，按文件字母顺序
		Collections.sort(this.fileList);
		FileInfoBaseAdapter adapter = new FileInfoBaseAdapter(this);
		// 将列表fileList设置到ListAdapter中，当做ListView的数据源
		adapter.setFileList(this.fileList);
		// 为ListView添加适配器
		fileListView.setAdapter(adapter);
	}

	/*
	 * 由于U盘经常被拔插，此方法在此不能用
	 * 采用了新的办法获取APK图标，之前的失败是因为android中存在的一个BUG,通过 appInfo.publicSourceDir =
	 * apkPath;来修正这个问题，详情参见:
	 * http://code.google.com/p/android/issues/detail?id=9151
	 */
	public Drawable getApkIcon(Context context, String apkPath)
	{
		PackageManager pm = context.getPackageManager();
		PackageInfo info = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
		File apkfile = new File(apkPath);
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && apkfile.exists())
		{
			if (info != null)
			{
				ApplicationInfo appInfo = info.applicationInfo;
				appInfo.sourceDir = apkPath;
				appInfo.publicSourceDir = apkPath;
				try
				{
					return appInfo.loadIcon(pm);
				} catch (OutOfMemoryError e)
				{
					Log.e("ApkIconLoader", e.toString());
				}
			}

		}
		return null;
	}

	/**
	 * 重命名操作
	 * 
	 * @param file
	 *            需要重命名的文件
	 */
	public void renameByFile(final File file)
	{
		// 创建布局对象
		final DialogLayout layout = new DialogLayout(this);
		// 设置初始化的值
		layout.getMessageTextView().setText(getResources().getString(R.string.newname));
		layout.getInputEditText().setText(file.getName());
		final String titleName = getResources().getStringArray(R.array.operate_opt)[0];
		Tools.showCustomDialog(mContext, titleName, layout,
		// 点击确定时
				new DialogInterface.OnClickListener()
				{

					public void onClick(DialogInterface dialog, int which)
					{
						// 获取新设置的名称
						String newName = layout.getInputEditText().getText().toString();
						// 判断新名称是否与旧名称一样，
						// 如果不一样判断新名词是否已经存在于当前目录了
						if (!newName.equals(file.getName()))
						{
							String currentDirectory = nowDirectory.getAbsolutePath();
							if (!currentDirectory.equals("/"))
							{
								currentDirectory += "/";
							}
							final String allName = currentDirectory + newName;
							// 判断是否重名
							if (new File(allName).exists())
							{
								// 弹出对话框判断是否覆盖
								Tools.confirmDialog(mContext, titleName, getResources().getString(R.string.fileNameRepetition), new DialogInterface.OnClickListener()
								{
									public void onClick(DialogInterface dialog, int which)
									{
										// 重命名操作
										boolean flag = file.renameTo(new File(allName));
										if (flag == true)
										{
											// 重命名之后，刷新
											openOrBrowseTheFile(nowDirectory);
										} else
										{
											// 提示失败
											Tools.toastMessage(mContext, getResources().getString(R.string.renameFail), 0);
										}
									}
								}, new DialogInterface.OnClickListener()
								{
									public void onClick(DialogInterface dialog, int which)
									{
										dialog.cancel();
									}
								});

							} else
							{
								boolean flag = file.renameTo(new File(allName));
								if (flag == true)
								{
									// 重命名之后，刷新
									openOrBrowseTheFile(nowDirectory);
								} else
								{
									// 提示失败
									Tools.toastMessage(mContext, getResources().getString(R.string.renameFail), 0);
								}
							}
						}
					}
				}, new DialogInterface.OnClickListener()
				{

					public void onClick(DialogInterface dialog, int which)
					{
						dialog.cancel();

					}
				}, new DialogInterface.OnCancelListener()
				{

					public void onCancel(DialogInterface dialog)
					{
						dialog.cancel();

					}
				}

		);
	}

	/**
	 * 删除文件
	 * 
	 * @param file
	 *            要删除的文件
	 */
	private void deleteFile(final File file)
	{
		final String titleName = getResources().getStringArray(R.array.operate_opt)[1];
		Tools.confirmDialog(mContext, titleName, getResources().getString(R.string.fileRemove), new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				// 确定删除
				try
				{
					// 删除文件
					FileOperaUtil.deleteAll(file);
					// 删除成功，则弹出提示
					Tools.toastMessage(mContext, titleName + file.getName() + getResources().getString(R.string.success), 0);
					// 删除成功，则刷新当前目录
					openOrBrowseTheFile(nowDirectory);
				} catch (IOException e)
				{
					e.printStackTrace();
					// 删除失败，则弹出失败对话框
					Tools.toastMessage(mContext, titleName + file.getName() + getResources().getString(R.string.fail), 0);
				}

			}
		}, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.cancel();
			}
		});
	}

	/**
	 * 内部类，作为事件监听器
	 */
	class FileClickListener implements DialogInterface.OnClickListener
	{
		File file;

		// 构造方法
		public FileClickListener(File choosefile)
		{
			file = choosefile;
		}

		@Override
		public void onClick(DialogInterface dialog, int which)
		{
			// 判断操作的是哪一个
			/*
			 * if (which == 0) { openFile(file); } else
			 */
			if (which == 0)
			{
				renameByFile(file);
			} else if (which == 1)
			{
				deleteFile(file);
			} else if (which == 2)
			{
				myTmpFile = file;
				Tools.toastMessage(mContext, getResources().getString(R.string.copytoclipboard), 0);
				isCut = false;
			} else if (which == 3)
			{
				myTmpFile = file;
				Tools.toastMessage(mContext, getResources().getString(R.string.cuttoclipboard), 0);
				isCut = true;
			}
		}
	}

	/**
	 * 新建文件夹
	 */
	private void CreateNewFile()
	{
		// 创建布局对象
		final DialogLayout layout = new DialogLayout(this);
		// 设置初始化的值
		layout.getMessageTextView().setText(getResources().getString(R.string.newname));
		final String titleName = getResources().getString(R.string.newfolder);
		Tools.showCustomDialog(mContext, titleName, layout, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				// 获取目录的目录
				String pathName = layout.getInputEditText().getText().toString();
				if (TextUtils.isEmpty(pathName))
				{
					Tools.toastMessage(mContext, getResources().getString(R.string.cannotEmptyName), 0);
					return;
				}
				String currentDirectory = nowDirectory.getAbsolutePath();
				if (!currentDirectory.equals("/"))
				{
					currentDirectory += "/";
				}
				boolean doesFileCanWrite = new File(currentDirectory).canWrite();
				if (!doesFileCanWrite)
				{
					Tools.showMessageDialog(mContext, getResources().getString(R.string.tip), getResources().getString(R.string.Createfolderfailed));
					return;
				}
				// 获取全名
				final String allName = currentDirectory + pathName;
				// 创建File对象
				final File file = new File(allName);
				// 判断是否重名
				if (file.exists())
				{
					Tools.toastMessage(mContext, getResources().getString(R.string.fileExist), 0);
				} else
				{
					boolean creadok = file.mkdirs();// 创建目录
					if (creadok)// 如果创建成功，刷新当前目录
					{
						// 创建成功
						Tools.toastMessage(mContext,pathName+" "+getResources().getString(R.string.createFileSuccess), 1);
						// 刷新当前目录
						openOrBrowseTheFile(nowDirectory);
					} else
					{
						Tools.toastMessage(mContext, pathName+" "+getResources().getString(R.string.createFilefail), 0);
					}
				}

			}
		}, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.cancel();
			}
		}, new DialogInterface.OnCancelListener()
		{
			public void onCancel(DialogInterface dialog)
			{
				dialog.cancel();
			}
		});
	}

	/**
	 * 删除目录
	 */
	private void DeleteFile()
	{
		// 取得当前目录
		final File currentDirectroy = CurrentSelectedItem;
		if (!currentDirectroy.isDirectory())
		{
			Tools.toastMessage(mContext, getResources().getString(R.string.notDirectory), 0);
			return;
		}
		// 如果当前路径
		// TODO 2016年5月4日11:49:57
		if (nowDirectory.listFiles().length <= 0)
		{
			Tools.toastMessage(mContext, getResources().getString(R.string.noFileToDelete), 0);
			return;
		}
		if (currentDirectroy.getName().equals("/"))
		{
			// 提示删除失败
			Tools.toastMessage(mContext, getResources().getString(R.string.cannotDeleteRoot), 0);
			return;
		}
		// final File upFile = currentDirectroy.getParentFile();
		// 判断是否确定删除当前文件
		final String titleName = getResources().getString(R.string.delfolder);
		Tools.confirmDialog(mContext, titleName, getResources().getString(R.string.deleteCurrentDir) + " " + currentDirectroy.getAbsolutePath() + " ?", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				try
				{
					FileOperaUtil.deleteAll(currentDirectroy);
					Tools.toastMessage(mContext, titleName +" "+currentDirectroy.getName()+" "+ getResources().getString(R.string.success), 0);
					// nowDirectory = upFile;// 修改当前目录为上一级目录
					openOrBrowseTheFile(nowDirectory);// 刷新

				} catch (IOException e)
				{
					e.printStackTrace();
					Tools.toastMessage(mContext,getResources().getString(R.string.cannotremove) , 0);
				}
			}
		}, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.cancel();
			}
		});
	}

	/**
	 * 粘贴文件
	 */
	private void PasteFile()
	{
		if (myTmpFile == null)
		{
			Tools.showMessageDialog(mContext, getResources().getString(R.string.tip), getResources().getString(R.string.noCopyShear));
		} else
		{
			String currentDirectory = nowDirectory.getAbsolutePath();
			if (!currentDirectory.equals("/"))
			{
				currentDirectory += "/";
			}
			// 获取全名
			final String allName = currentDirectory + myTmpFile.getName();
			final File targetFile = new File(allName);
			// 判断是复制还是剪切
			if (!isCut)
			{
				// 如果当前文件已存在
				if (targetFile.exists())
				{
					// 判断是否需要覆盖
					final String titleName = getResources().getStringArray(R.array.operate_opt)[2];
					Tools.confirmDialog(mContext, titleName, getResources().getString(R.string.fileNameRepetition), new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							if (myTmpFile.getAbsolutePath().equals(targetFile.getAbsolutePath()))
							{
								FileOperaUtil.moveFile(myTmpFile, targetFile);
							} else
							{
								progressDialog.show();
								srcFileSize = FileOperaUtil.getFileSize(myTmpFile);
								copyFile(myTmpFile, targetFile);
							}
						}
					}, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							dialog.cancel();
						}
					});

				} else
				{
					progressDialog.show();
					srcFileSize = FileOperaUtil.getFileSize(myTmpFile);
					copyFile(myTmpFile, targetFile);
				}
			} else
			{
				final String titleName = getResources().getStringArray(R.array.operate_opt)[3];
				// 如果當前文件夾已經存在該文件
				if (targetFile.exists())
				{
					// 判斷是否需要覆蓋
					Tools.confirmDialog(mContext, titleName, getResources().getString(R.string.fileNameRepetition), new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							FileOperaUtil.moveFile(myTmpFile, targetFile);
							// 剪切成功
							Tools.toastMessage(mContext, getResources().getString(R.string.cutsuccess), 0);
							// 剪切文件成功，刷新当前目录
							openOrBrowseTheFile(nowDirectory);
							myTmpFile = null;// 设置为空

						}
					}, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							dialog.cancel();
						}
					});
				} else
				{
					FileOperaUtil.moveFile(myTmpFile, targetFile);
					// 剪切成功
					Tools.toastMessage(mContext, getResources().getString(R.string.cutsuccess), 0);
					// 剪切文件成功，刷新当前目录
					openOrBrowseTheFile(nowDirectory);
					myTmpFile = null;// 设置为空
				}
			}
		}

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			if (!atRootPath(nowDirectory))
			{
				upOnLevel();
			} else
			{
				exit();
			}
			return false;
		} else if (keyCode == KeyEvent.KEYCODE_MENU)
		{

			PrintLog.Warn("按了菜单键！");
			if (CurrentSelectedItem != null)
			{
				PrintLog.Info("当前文件路径 " + CurrentSelectedItem.getAbsolutePath());
				showMenuDialog();
			}
			return super.onKeyDown(keyCode, event);
		} else
		{
			return super.onKeyDown(keyCode, event);
		}
	}

	/**
	 * 显示菜单按键
	 */
	private void showMenuDialog()
	{
		// 弹出对话框提示
		int buttonLen = 6;
		int[] textIds = { R.string.newfolder, R.string.delfolder, R.string.copyfolder, R.string.cutfolder, R.string.pastehere, R.string.backhome };
		int[] imageIds = { R.drawable.addfolderr, R.drawable.delete, R.drawable.copyfolder, R.drawable.cutfolder, R.drawable.paste, R.drawable.backhome };
		mInflater = LayoutInflater.from(mContext);
		View view = mInflater.inflate(R.layout.dialog_dir_op, null);
		LinearLayout showMenuItem = (LinearLayout) view.findViewById(R.id.showMenuItem);
		showMenuItem.removeAllViews();
		CusImageButton[] buttons = new CusImageButton[buttonLen];
		for (int i = 0; i < buttons.length; i++)
		{
			buttons[i] = new CusImageButton(mContext, imageIds[i], textIds[i], null);
			buttons[i].setBackground(getResources().getDrawable(R.drawable.list_selector_background));
			buttons[i].setFocusable(true);
			buttons[i].setClickable(true);
			showMenuItem.addView(buttons[i]);
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setTitle(getResources().getString(R.string.FolderAction));
		builder.setView(view);
		// builder.create().show();
		final Dialog dialog = builder.create();
		dialog.show();
		// 新建文件夹
		buttons[0].setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				CreateNewFile();
				dialog.dismiss();
			}
		});
		// 删除文件夹
		buttons[1].setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				DeleteFile();
				// deleteFile(CurrentSelectedItem);
				dialog.dismiss();
			}
		});

		// 复制文件夹
		buttons[2].setOnClickListener(new View.OnClickListener()
		{

			File currentDirectroy = CurrentSelectedItem;

			@Override
			public void onClick(View v)
			{
				if (currentDirectroy.isDirectory())
				{
					myTmpFile = currentDirectroy;
					Tools.toastMessage(mContext, getResources().getString(R.string.copytoclipboard), 0);
					isCut = false;
				} else
				{
					Tools.toastMessage(mContext, getResources().getString(R.string.notDirectory), 0);
				}
				dialog.dismiss();
			}
		});

		// 剪切文件夹

		buttons[3].setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				File currentDirectroy = CurrentSelectedItem;
				if (currentDirectroy.isDirectory())
				{
					myTmpFile = currentDirectroy;
					Tools.toastMessage(mContext, getResources().getString(R.string.cuttoclipboard), 0);
					isCut = true;
				} else
				{
					Tools.toastMessage(mContext, getResources().getString(R.string.notDirectory), 0);
				}
				dialog.dismiss();
			}
		});

		// 粘贴到此文件夹
		buttons[4].setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				PasteFile();// 这里是耗时的操作，得用异步处理
				dialog.dismiss();
			}
		});
		// 返回主界面
		buttons[5].setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				dialog.dismiss();
				finish();
			}
		});
	}

	// 退出的方法
	public void exit()
	{
		if (!isExit)
		{
			isExit = true;
			Tools.toastMessage(mContext, getResources().getString(R.string.TotheMainInterface), 0);
			mHandler.sendEmptyMessageDelayed(EXIT, 2000);
		} else
		{
			finish();
		}
	}

	Handler mHandler = new Handler()
	{

		@Override
		public void handleMessage(Message msg)
		{
			super.handleMessage(msg);
			if (msg.what == EXIT)
			{
				isExit = false;
			} else if (msg.what == REFRESHDIR)
			{
				// 复制成功
				Tools.toastMessage(mContext, getResources().getString(R.string.success), 0);
				// 复制成功，刷新当前目录
				openOrBrowseTheFile(nowDirectory);
				myTmpFile = null;
			}
		}

	};

	/**
	 * 注册监听usb设备的广播
	 */
	private void registerSdcardReceiver()
	{
		// 在IntentFilter中选择你要监听的行为
		IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);// sd卡被插入，且已经挂载
		intentFilter.setPriority(1000);// 设置最高优先级
		intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);// sd卡存在，但还没有挂载
		intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);// sd卡被移除
		intentFilter.addAction(Intent.ACTION_MEDIA_SHARED);// sd卡作为
															// USB大容量存储被共享，挂载被解除
		intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);// sd卡已经从sd卡插槽拔出，但是挂载点还没解除
		intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);// 开始扫描
		intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);// 扫描完成
		intentFilter.addDataScheme("file");
		registerReceiver(broadcastRec, intentFilter);// 注册监听函数
	}

	private void unregisterSdcardReceiver()
	{
		unregisterReceiver(broadcastRec);
	}

	/**
	 * 定义广播接收者来监听sd卡的状态,usb设备插入时更新显示usb设备，usb设备拔出是跳转到最前面那个usb设备目录中
	 */
	private final BroadcastReceiver broadcastRec = new BroadcastReceiver()
	{

		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();
			if (action.equals("android.intent.action.MEDIA_MOUNTED"))// SD
			// 卡已经成功挂载
			{
				refreshUsbDeviceList();
				File firstMountedStorageFile = getFirstMountedStorage();
				if (firstMountedStorageFile != null)
				{
					nowDirectory = firstMountedStorageFile;
					openOrBrowseTheFile(nowDirectory);
					fileListView.setVisibility(View.VISIBLE);
				} else
				{
					fileListView.setVisibility(View.GONE);
				}
			} else if (action.equals("android.intent.action.MEDIA_REMOVED")// 各种未挂载状态
					|| action.equals("android.intent.action.ACTION_MEDIA_UNMOUNTED") || action.equals("android.intent.action.ACTION_MEDIA_BAD_REMOVAL"))
			{
				refreshUsbDeviceList();
				File firstMountedStorageFile = getFirstMountedStorage();
				if (firstMountedStorageFile != null)
				{
					nowDirectory = firstMountedStorageFile;
					openOrBrowseTheFile(nowDirectory);
				} else
				{
					fileListView.setVisibility(View.GONE);
				}
			} else if (action.equals(Intent.ACTION_MEDIA_SCANNER_STARTED))
			{// 开始扫描
				// Toast.makeText(context, "开始扫描...",
				// Toast.LENGTH_SHORT).show();
			} else if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED))
			{// 扫描完成
				// Toast.makeText(context, "扫描完成...",
				// Toast.LENGTH_SHORT).show();
			} else if (action.equals(Intent.ACTION_MEDIA_SHARED))
			{// 扩展介质的挂载被解除 (unmount)。因为它已经作为 USB 大容量存储被共享
				// Toast.makeText(context, " USB 大容量存储被共享...",
				// Toast.LENGTH_SHORT).show();
			} /*
			 * else { Toast.makeText(context, "其他状态...",
			 * Toast.LENGTH_SHORT).show(); }
			 */
		}

		/**
		 * 得到第一个被挂挂载的文件目录
		 * 
		 * @return
		 */
		private File getFirstMountedStorage()
		{
			if (allMountedStorage != null && allMountedStorage.size() > 0)
			{
				for (String MountedStorage : allMountedStorage)
				{
					File firstMountedStorageFile = new File(MountedStorage);
					return firstMountedStorageFile;
				}
			}
			return null;
		}
	};

	@Override
	protected void onStop()
	{
		super.onStop();
		unregisterSdcardReceiver();
	}

	/**
	 * 实时获取当前选中Item的路径
	 * 
	 * @param position
	 *            item 的位置
	 * @return
	 */
	private File getCurrentItemPath(int position)
	{
		File clickedFile = null;
		String currentPath = nowDirectory.getAbsolutePath();
		if (!currentPath.equals("/"))
		{
			// 如果是非根目录的话，文件夹表示为/xxx,后面添加文件名之前要加上/,最后表示为/xxx/yy.zzz，如果是根目录不需要
			currentPath += "/";
		}
		// 根据当前目录的绝对路径和文件名创建相应的File对象
		clickedFile = new File(currentPath + fileList.get(position).getFileName());
		return clickedFile;
	}

	/**
	 * 复制文件
	 * 
	 * @param src
	 *            需要复制的文件
	 * @param target
	 *            目标文件
	 */
	public synchronized void copyFile(File src, File target)
	{

		int srcSize = FileOperaUtil.getFileSize(src);
		if (src.isDirectory())
		{
			if (!target.exists())
			{
				target.mkdir();
			}
			PrintLog.Debug("====要复制的文件大小 ==== " + srcSize);
			// 复制文件夹
			File[] currentFiles;
			currentFiles = src.listFiles();
			for (int i = 0; i < currentFiles.length; i++)
			{
				// 如果当前为子目录则递归
				if (currentFiles[i].isDirectory())
				{
					copyFile(new File(currentFiles[i] + "/"), new File(target.getAbsolutePath() + "/" + currentFiles[i].getName() + "/"));
				} else
				{
					copyFile(currentFiles[i], new File(target.getAbsolutePath() + "/" + currentFiles[i].getName()));
				}
			}

		} else
		{
			if (copyTask == null)
			{
				copyTask = new CopyFileAsyncTask(src, target);
				copyTask.execute();
				copyTask = null;
			}
		}

	}

	class CopyFileAsyncTask extends AsyncTask<String, Integer, String>
	{

		private File srcFile;
		private File targetFile;
		InputStream in = null;
		OutputStream out = null;
		BufferedInputStream bin = null;
		BufferedOutputStream bout = null;

		public CopyFileAsyncTask(File src, File target)
		{
			super();
			this.srcFile = src;
			this.targetFile = target;
		}

		@Override
		protected String doInBackground(String... params)
		{
			String str = null;
			try
			{
				in = new FileInputStream(srcFile);
				out = new FileOutputStream(targetFile);
				bin = new BufferedInputStream(in);
				bout = new BufferedOutputStream(out);
				byte[] b = new byte[2048];
				int len = 0;
				PrintLog.Info("正在拷贝 ...." + srcFile);
				while ((len = bin.read(b)) != -1)
				{
					bout.write(b, 0, len);
					targetFileSize += len;
					publishProgress((int) (targetFileSize * 100 / srcFileSize));
				}
				if (in != null)
				{
					in.close();
					in = null;
				}
				if (out != null)
				{
					out.close();
					out = null;
				}
				if (bin != null)
				{
					bin.close();
					bin = null;
				}
				if (bout != null)
				{
					bout.close();
					bout = null;
				}
				str = " " + srcFile.getAbsolutePath();
			} catch (FileNotFoundException e)
			{
				e.printStackTrace();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
			return str;
		}

		@Override
		protected void onPreExecute()
		{
			super.onPreExecute();
			progressDialog.setMessage(getResources().getString(R.string.copydialogmessage));
			PrintLog.Info(" 准备执行 onPreExecute()....");
		}

		@Override
		protected void onPostExecute(String result)
		{
			super.onPostExecute(result);
			PrintLog.Info(" 执行完 onPostExecute()....");
			PrintLog.Debug("currentSize ======= " + targetFileSize);
			PrintLog.Warn(srcFile + " 拷贝成功.......");
			if (Math.abs(targetFileSize - srcFileSize) <= 1024)
			{
				progressDialog.dismiss();
				targetFileSize = 0;
				PrintLog.Debug("所有文件拷贝完成");
				openOrBrowseTheFile(nowDirectory);
				Tools.toastMessage(mContext, getResources().getString(R.string.copyfinished), 0);
				myTmpFile = null;
			}
		}

		int temp = -1;

		@Override
		protected void onProgressUpdate(Integer... values)
		{
			super.onProgressUpdate(values);
			// PrintLog.Debug("进度显示UIonProgressUpdate()");
			progressDialog.setProgress(values[0]);
			if (values[0] != temp)
			{
				PrintLog.Debug("values[0] ======" + values[0]);
				temp = values[0];
			}
		}

	}

}
