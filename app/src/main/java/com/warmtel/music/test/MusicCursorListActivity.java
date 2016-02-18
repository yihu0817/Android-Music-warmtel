package com.warmtel.music.test;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.OnScanCompletedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.warmtel.music.R;
import com.warmtel.music.util.Logs;

import java.io.File;
import java.io.UnsupportedEncodingException;

/**
 * MediaStore.Audio.Media.EXTERNAL_CONTENT_URI 对应字段
 * 歌曲ID：MediaStore.Audio.Media._ID 
 * 歌曲的名称：MediaStore.Audio.Media.TITLE
 * 歌曲的专辑名：MediaStore.Audio.Media.ALBUM 
 * 歌曲的歌手名：MediaStore.Audio.Media.ARTIST
 * 歌曲文件的路径：MediaStore.Audio.Media.DATA 
 * 歌曲的总播放时长：MediaStore.Audio.Media.DURATION
 * 歌曲文件的大小：MediaStore.Audio.Media.SIZE
 * 
 */

public class MusicCursorListActivity extends Activity {
	private ListView mListView;
	private FileCursorAdapter mAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_music_list_layout);
		mListView = (ListView) findViewById(R.id.music_list_view);

		/**
		 * Android自身具有维护媒体库的功能 
		 * 1.系统创建了一个SQLITE数据库存放所有音乐资源
		 * 2.MediaScaner类负责扫描系统文件，添加音乐资源到数据库 。
		 * 什么时间执行扫描操作：1.启动手机,2.插入拔出Sdcard时,3.接收到扫描广播时
		 */
		Cursor cursor = getContentResolver().query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,
				MediaStore.Audio.Media.TITLE_KEY);

		mAdapter = new FileCursorAdapter(this, cursor);

		mListView.setAdapter(mAdapter);

	//	deleteMediaFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)+ "/jinsha.mp3");
//		scanMedia();
	}

	@Override
	protected void onResume() {
		super.onResume();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
		filter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);

		registerReceiver(receiver, filter);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (receiver != null) {
			unregisterReceiver(receiver);
		}
	}
	/**
	 * 单独扫描文件成功，扫描文件夹不成功
	 */
	public void scanMedia() {
		// scanMediaByMediaScanner(""+Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC));

		// scanMediaByRecerver(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)+"/name.mp3");

		MediaScanner mediaScaner = new MediaScanner(this);
		String[] paths = new String[]{Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)+ "/jinsha.mp3"};
		mediaScaner.scanFile(paths, "audio/mp3");
	}

	/**
	 * 测试成功   
	 * 
	 * @param pathFile
	 */
	public void scanMediaByRecerver(String pathFile) {
		String saveAs = pathFile;
		Uri contentUri = Uri.fromFile(new File(saveAs));
		Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri);
		sendBroadcast(mediaScanIntent);
	}

	/**
	 * 测试失败
	 * 
	 * @param path
	 */
	public void scanFileDirMount(String path) {
		sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse(path)));
	}

	/**
	 * 测试报错 has leaked ServiceConnection
	 * android.media.MediaScannerConnection@53567a88 that was originally bound
	 * here
	 * 
	 * @param filePath   /mnt/sdcard/music/xx.mp3
	 */
	public void scanMediaByMediaScanner(String filePath) {
		MediaScannerConnection.scanFile(this, new String[] { filePath }, null,
				new OnScanCompletedListener() {

					@Override
					public void onScanCompleted(String path, Uri uri) {
						Toast.makeText(MusicCursorListActivity.this,
								"扫描完成" + path, Toast.LENGTH_SHORT).show();
					}
				});
	}

	/**
	 * 删除媒体库音乐资源   file://mnt/sdcard/music/xx.mp3
	 */
	public void deleteMediaFile(String path) {
		Logs.v("删除媒体库音乐资源 >>> :" + path);

		File file = new File(path);
		if (file.exists()) {
			file.delete();
		}
		getContentResolver().delete(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				MediaStore.Audio.Media.DATA + " = ?", new String[] { path });
	}
	
	public BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_MEDIA_SCANNER_STARTED)) {
				Toast.makeText(context, "开始扫描文件", Toast.LENGTH_SHORT).show();
				Logs.v("开始扫描文件 >>>>");
			} else if (intent.getAction().equals(
					Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
				Toast.makeText(context, "完成扫描文件", Toast.LENGTH_SHORT).show();
				Logs.v("完成扫描文件 >>>>");
			}
		}

	};

	public class FileCursorAdapter extends CursorAdapter {
		private LayoutInflater layoutInflater;

		public FileCursorAdapter(Context context, Cursor c) {
			super(context, c, FLAG_AUTO_REQUERY);
			layoutInflater = LayoutInflater.from(context);
		}

		@Override
		public void bindView(View view, Context arg1, Cursor cursor) {
			TextView titleTxt = (TextView) view
					.findViewById(R.id.music_title_txt);
			TextView articsTxt = (TextView) view
					.findViewById(R.id.music_artics_txt);

			String title = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.TITLE));
			Logs.d("title >>> :" + title);
			try {
				title = new String(title.getBytes("ISO-8859-1"), "GBK");
				Logs.d("111 title >>> :" + title);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			titleTxt.setText(title);
			articsTxt.setText(cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
		}

		@Override
		public View newView(Context arg0, Cursor arg1, ViewGroup arg2) {
			return layoutInflater.inflate(R.layout.item_music_cursor_layout,
					null);
		}

	}

}
