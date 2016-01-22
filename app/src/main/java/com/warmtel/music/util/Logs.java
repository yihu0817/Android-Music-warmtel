package com.warmtel.music.util;

import android.util.Log;

public class Logs {
	public static final String tag = "tag";
	public static boolean flag = true; // if==true 显示日志; else 关闭日志.

	public static void v(String msg) {
		if (flag) {
			Log.v(tag, msg);
		}
	}
	
	public static void i(String msg) {
		if (flag) {
			Log.i(tag, msg);
		}
	}
	
	public static void d(String msg) {
		if (flag) {
			Log.d(tag, msg);
		}
	}
	
	public static void w(String msg) {
		if (flag) {
			Log.w(tag, msg);
		}
	}
	
	public static void e(String msg) {
		if (flag) {
			Log.e(tag, msg);
		}
	}
}
