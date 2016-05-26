package com.ktc.filemanager.log;

import android.util.Log;

public class PrintLog {

	private static final String TAG = "ktc_filemanager";
	public final static boolean DEBUG = false;
	public static final void Debug(String msg)
	{
		if (DEBUG) {
			Log.d(TAG, msg);
		}
	}
	public static final void Info(String msg)
	{
		if (DEBUG) {
			Log.i(TAG, msg);
		}
	}
	public static final void Err(String msg)
	{
		if (DEBUG) {
			Log.e(TAG, msg);
		}
	}
	public static final void Warn(String msg)
	{
		if (DEBUG) {
			Log.w(TAG, msg);
		}
	}
	
}
