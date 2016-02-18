package com.warmtel.music.demo1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.warmtel.music.model.MusicBean;
import com.warmtel.music.R;

import java.io.File;
import java.util.ArrayList;

public class MusicListActivity extends Activity implements OnItemClickListener{
	private ListView mListView;
	private Handler mHandler = new Handler();
	private ArrayList<MusicBean> mMusicList = new ArrayList<>();
	private FileAdapter mAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_music_list_layout);
		mListView = (ListView) findViewById(R.id.music_list_view);
		mListView.setOnItemClickListener(this);
		
		mAdapter = new FileAdapter(this);
		mListView.setAdapter(mAdapter);

		new Thread(new Runnable() {
			@Override
			public void run() {
				scanFileList(Environment.getExternalStorageDirectory());
				/** ListView刷新必须在UI线程中 通过Handler消息机制发送刷新代码到UI主线程执行 */
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						mAdapter.setListData(mMusicList);
					}
				});
			}
		}).start();
	}

	/**
	 * 扫描Sdcard（外部存储）下所有文件
	 */
	public void scanFileList(File parentFile) {
		File[] listFile = parentFile.listFiles();

		if (listFile != null) {
			for (int i = 0; i < listFile.length; i++) {
				File file = listFile[i];
				if (file.isDirectory()) {
					scanFileList(file);
				} else {
					if (file.getName().endsWith("mp3")) {
						MusicBean music = new MusicBean();
						String fileName = file.getName();
						music.setMusicName(fileName.substring(0, fileName.length()-".mp3".length()));
						music.setMusicPath(file.getAbsolutePath());
						mMusicList.add(music);
					}
				}
			}
		}
	}

	public class FileAdapter extends BaseAdapter {
		private ArrayList<MusicBean> list = new ArrayList<>();
		private LayoutInflater layoutInflater;

		public FileAdapter(Context context) {
			layoutInflater = LayoutInflater.from(context);
		}
		public void setListData(ArrayList<MusicBean> list) {
			this.list = list;
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public Object getItem(int position) {
			return list.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = layoutInflater.inflate(R.layout.item_music_list_layout, null);
			}
			MusicBean file = (MusicBean) getItem(position);
			TextView nameTxt = (TextView) convertView;
			nameTxt.setText(file.getMusicName());
			return convertView;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Intent intent = new Intent(this,MusicActivity.class);
		intent.putParcelableArrayListExtra("MUSIC_LIST", mMusicList);
		intent.putExtra("CURRENT_POSTION", position);
		
		startActivity(intent);
		
	}
}
