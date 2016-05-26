package com.ktc.filemanager.tools;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.content.Context;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.util.Log;

/**
 * @description Get the internal or external storage path. This class used three
 *              ways to obtain the storage path. reflect: major method is
 *              getVolumePaths and getVolumeState. this two method is hidden for
 *              programmer, so we must to use this way. if either getVolumePaths
 *              or getVolumeState can not be found (e.g. in some sdk version),
 *              then use next way. command: By filter the output of command
 *              "mount", may be we can get the storage path that we want. if
 *              didn't, then use next way. Api: As is known to all, we use
 *              getExternalStorageDirectory method.
 */

public class StoragePathsManager
{
	private static final String LOG_TAG = "StoragePathsManager";

	private Context mContext;
	private StorageManager mStorageManager;
	private Method mMethodGetPaths;
	private Method mMethodGetPathsState;
	private boolean mIsReflectValide = true;
	private List<String> mAllStoragePathsByMountCommand = new ArrayList<String>();

	public StoragePathsManager(Context context)
	{
		mContext = context;
		init();
	}

	private void init()
	{
		Log.i(LOG_TAG, "init");
		mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
		try
		{
			mMethodGetPaths = mStorageManager.getClass().getMethod("getVolumePaths");
			mMethodGetPathsState = mStorageManager.getClass().getMethod("getVolumeState", String.class);
		} catch (NoSuchMethodException ex)
		{
			ex.printStackTrace();
		}

		if (mMethodGetPaths == null || mMethodGetPathsState == null)
		{
			mIsReflectValide = false;
		}

		if (false == mIsReflectValide)
		{
			Set<String> set = getStoragePathsByCommand();
			mAllStoragePathsByMountCommand.addAll(set);
			for (String s : mAllStoragePathsByMountCommand)
			{
				Log.i(LOG_TAG, "abtain by command: " + s);
			}
			if (mAllStoragePathsByMountCommand.size() == 0)
			{
				if (Environment.getExternalStorageDirectory().getPath() != null)
				{
					mAllStoragePathsByMountCommand.add(Environment.getExternalStorageDirectory().getPath());
				}
			}
			for (String s : mAllStoragePathsByMountCommand)
			{
				Log.i(LOG_TAG, "abtain by Environment: " + s);
			}
		}
	}

	private HashSet<String> getStoragePathsByCommand()
	{
		final HashSet<String> out = new HashSet<String>();
		String reg = "(?i).*vold.*(vfat|ntfs|exfat|fat32|ext3|ext4).*rw.*";
		String s = "";
		try
		{
			final Process process = new ProcessBuilder().command("mount").redirectErrorStream(true).start();
			process.waitFor();
			final InputStream is = process.getInputStream();
			final byte[] buffer = new byte[1024];
			while (is.read(buffer) != -1)
			{
				s = s + new String(buffer);
			}
			is.close();
		} catch (final Exception e)
		{
			e.printStackTrace();
		}

		// parse output
		final String[] lines = s.split("\n");
		for (String line : lines)
		{
			if (!line.toLowerCase(Locale.US).contains("asec"))
			{
				if (line.matches(reg))
				{
					String[] parts = line.split(" ");
					for (String part : parts)
					{
						if (part.startsWith("/"))
							if (!part.toLowerCase(Locale.US).contains("vold"))
								out.add(part);
					}
				}
			}
		}
		return out;
	}

	/**
	 * @return String. for example /mnt/sdcard
	 */
	public String getExternalStoragePath()
	{
		String path = null;
		List<String> allMountedPaths = getMountedStoragePaths();
		String internal = getInternalStoragePath();
		for (String s : allMountedPaths)
		{
			if (!s.equals(internal))
			{
				path = s;
				break;
			}
		}

		return path;
	}

	public String getInternalStoragePath()
	{
		// get external path
		String pathExtNotRemovable = null;
		String pathExtRemovable = null;
		String ext = Environment.getExternalStorageDirectory().getPath();
		// if it is removable, the storage is external storage, otherwise
		// internal storage.
		boolean isExtRemovable = Environment.isExternalStorageRemovable();
		List<String> allMountedPaths = getMountedStoragePaths();
		for (String s : allMountedPaths)
		{
			if (s.equals(ext))
			{
				if (isExtRemovable)
				{
					pathExtRemovable = s;
				} else
				{
					pathExtNotRemovable = s;
				}
				break;
			}
		}

		String intr = null;

		String refPath = null;
		if (pathExtRemovable != null)
		{
			refPath = pathExtRemovable;
		} else if (pathExtNotRemovable != null)
		{
			intr = pathExtNotRemovable;
			return intr;
		}

		for (String s : allMountedPaths)
		{
			if (!s.equals(refPath))
			{
				intr = s;
				break;
			}
		}

		return intr;
	}

	/**
	 * @return /data/data/com.xxx.xxx/files
	 */
	public String getAppStoragePath()
	{
		String path = mContext.getApplicationContext().getFilesDir().getAbsolutePath();
		Log.i(LOG_TAG, "getAppStoragePath: " + path);
		return path;
	}

	public List<String> getMountedStoragePaths()
	{
		if (false == mIsReflectValide)
		{
			return mAllStoragePathsByMountCommand;
		}

		List<String> mountedPaths = new ArrayList<String>();
		String[] paths = getAllStoragePaths();
		Log.i(LOG_TAG, "all paths:");
		if (paths != null)
		{
			for (String path : paths)
			{
				Log.i(LOG_TAG, "-- path: " + path);
			}
		}

		for (String path : paths)
		{
			if (isMounted(path))
			{
				Log.i(LOG_TAG, "path: " + path + " is mounted");
				mountedPaths.add(path);
			}
		}

		return mountedPaths;
	}

	private String[] getAllStoragePaths()
	{
		String[] paths = null;
		try
		{
			paths = (String[]) mMethodGetPaths.invoke(mStorageManager);
		} catch (IllegalArgumentException ex)
		{
			ex.printStackTrace();
		} catch (IllegalAccessException ex)
		{
			ex.printStackTrace();
		} catch (InvocationTargetException ex)
		{
			ex.printStackTrace();
		}

		return paths;
	}

	private String getVolumeState(String mountPoint)
	{
		String status = null;
		try
		{
			status = (String) mMethodGetPathsState.invoke(mStorageManager, mountPoint);
		} catch (IllegalArgumentException ex)
		{
			ex.printStackTrace();
		} catch (IllegalAccessException ex)
		{
			ex.printStackTrace();
		} catch (InvocationTargetException ex)
		{
			ex.printStackTrace();
		}
		return status;
	}

	private boolean isMounted(String mountPoint)
	{
		String status = null;
		boolean result = false;
		status = getVolumeState(mountPoint);
		if (Environment.MEDIA_MOUNTED.equals(status))
		{
			result = true;
		}
		return result;
	}
}
