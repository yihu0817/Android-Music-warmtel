package com.warmtel.music;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.warmtel.music.service.MusicService;
import com.warmtel.music.view.CDView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MusicActivity extends Activity implements OnClickListener,
		OnSeekBarChangeListener {
	private ImageView mPlayerPreImg, mPlayerNextImg, mPlayerImg;
	private TextView mCurrentTimeTxt, mTotalTimeTxt;
	private ViewPager mViewPager;
	private SeekBar mSeekBar;

	private MusicService.IMusicService mIMusicService;
	private CDView cdView;
	private boolean playerStateFlag = true;
	/** true: 正在播放 , false: 暂停播放 */
	private boolean mThreadFlag = true;
	private int mProgress = 0;
	/** seekTo 解决延迟问题 */

	private static final int TITME = 2000;
	/** 循环时间 */
	private static final int MSG_NEXT_IMAGE = 1;
	/** 下一张图片 */
	private static final int MSG_PAGE_CHANGED = 2;
	/** 设置当前页号 */

	private SimpleDateFormat format = new SimpleDateFormat("mm:ss");
	private Handler mHandler = new Handler();
	private Handler mPagerHandler = new Handler() {
		boolean flag = true;
		int currentPagerNo = 0; // 表示页号

		public void handleMessage(android.os.Message msg) {
			// 检查消息队列并移除未发送的消息，这主要是避免在复杂环境下消息出现重复等问题。
			// if (mPagerHandler.hasMessages(MSG_NEXT_IMAGE) && !flag) {
			// mPagerHandler.removeMessages(MSG_NEXT_IMAGE);
			// flag = false;
			// }

			switch (msg.what) {
			case MSG_NEXT_IMAGE:
				currentPagerNo++;
				mViewPager.setCurrentItem(currentPagerNo);
				mPagerHandler.sendEmptyMessageDelayed(MSG_NEXT_IMAGE, TITME);
				break;
			case MSG_PAGE_CHANGED:
				// 记录当前的页号，避免播放的时候页面显示不正确。
				currentPagerNo = msg.arg1;
				break;
			}
		}
	};
	private int sScreenWidth;
	private int sScreenHeight;
	public ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mIMusicService = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mIMusicService = (MusicService.IMusicService) service;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_music_main_layout);
		initView();

		/** 接收音乐列表资源 */
		Intent intent = getIntent();
		ArrayList<MusicBean> musicList = intent.getParcelableArrayListExtra("MUSIC_LIST");
		int currentPostion = intent.getIntExtra("CURRENT_POSTION", 0);

		/** 构造启动音乐播放服务的Intent，设置音乐资源 */
		intent = new Intent(this, MusicService.class);
		intent.putParcelableArrayListExtra("MUSIC_LIST", musicList);
		intent.putExtra("CURRENT_POSTION", currentPostion);

		/** 启动服务把音乐资源传给服务 */
		startService(intent);
		/** 绑定服务，获取交互实例 */
		bindService(intent, mServiceConnection, BIND_AUTO_CREATE);

		/** 负责更新播放进度和时间 */
		// new Thread(new PlayerThread()).start();

		// 设置默认播放
		mPlayerImg.setImageResource(R.drawable.player_pause);

		WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics dm = new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(dm);
		sScreenWidth = dm.widthPixels;
		sScreenHeight = dm.heightPixels;

		initViewPager();
	}

	@Override
	protected void onResume() {
		super.onResume();
		IntentFilter filter = new IntentFilter(ACTION_PROGRESS_RECEIVER);
		registerReceiver(progressReceiver, filter);
		
		IntentFilter filters = new IntentFilter(MusicService.ACTION_PLAYER_MUSIC);
		registerReceiver(playerReceiver, filters);
		
		
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (progressReceiver != null) {
			unregisterReceiver(progressReceiver);
		}
		if (playerReceiver != null) {
			unregisterReceiver(playerReceiver);
		}
	}

	/**
	 * 初始化View
	 */
	public void initView() {
		mPlayerPreImg = (ImageView) findViewById(R.id.player_pre_img);
		mPlayerNextImg = (ImageView) findViewById(R.id.player_next_img);
		mPlayerImg = (ImageView) findViewById(R.id.player_pause_img);
		mCurrentTimeTxt = (TextView) findViewById(R.id.player_current_time_txt);
		mTotalTimeTxt = (TextView) findViewById(R.id.player_total_time_txt);
		mSeekBar = (SeekBar) findViewById(R.id.player_seekbar);
		mViewPager = (ViewPager) findViewById(R.id.player_viewpager);

		mPlayerPreImg.setOnClickListener(this);
		mPlayerNextImg.setOnClickListener(this);
		mPlayerImg.setOnClickListener(this);
		mSeekBar.setOnSeekBarChangeListener(this);
	}

	/**
	 * 初始化ViewPager，循环滑动
	 */
	public void initViewPager() {
		LayoutInflater inflater = LayoutInflater.from(this);
//		View lrv1 = inflater.inflate(R.layout.music_viewpager_item1_layout, null);
		View cdv2 = inflater.inflate(R.layout.music_viewpager_item2_layout, null);
		View v3 = inflater.inflate(R.layout.music_viewpager_item3_layout, null);
//		final LyricView lyricView = (LyricView) lrv1.findViewById(R.id.music_lyricview);

		cdView = (CDView) cdv2.findViewById(R.id.music_cd_img);

		Bitmap bmp =null;
		if(bmp == null) bmp = BitmapFactory.decodeResource(getResources(), R.drawable.m4);
//		cdView.setImage(bmp);
		cdView.setImage(Bitmap.createScaledBitmap(bmp, (int)(sScreenWidth * 0.7), (int)(sScreenWidth * 0.7), true));

		ArrayList<View> data = new ArrayList<>();
//		data.add(lrv1);
		data.add(cdv2);
		data.add(v3);

		MyPagerAdapter viewPagerAdapter = new MyPagerAdapter();
		mViewPager.setAdapter(viewPagerAdapter);

		viewPagerAdapter.setData(data);

		mViewPager.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageSelected(int postion) {
				mPagerHandler.sendMessage(Message.obtain(mPagerHandler,
						MSG_PAGE_CHANGED, postion, 0));
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
			}
		});

		/**
		 * 2147483647 / 2 = 1073741820 - 1
		 * 设置ViewPager的当前项为一个比较大的数，以便一开始就可以左右循环滑动
		 */
		int n = Integer.MAX_VALUE / 2 % data.size();
		int currentPager = Integer.MAX_VALUE / 2 - n;

		mViewPager.setCurrentItem(currentPager);

		/** 停止自动滑动 */
		// mPagerHandler.sendEmptyMessageDelayed(MSG_NEXT_IMAGE, TITME);

//		updateLrView();
	}
//	public void updateLrView(){
//		new Thread(new Runnable() {
//			long time = 100; // 开始 的时间，不能为零，否则前面几句歌词没有显示出来
//
//			@Override
//			public void run() {
//				while (true) {
//					if (mIMusicService != null && mIMusicService.isPlayering()) {
//						long sleeptime = lyricView.updateIndex(time);
//						time += sleeptime;
//						mHandler.post(new Runnable() {
//							public void run() {
//								lyricView.invalidate(); // 更新视图
//							}
//						});
//						if (sleeptime == -1)
//							return;
//						try {
//							Thread.sleep(sleeptime);
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//						}
//					}
//				}
//
//			}
//		}).start();
//	}
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.player_pause_img:
			Intent intent = new Intent(MusicService.ACTION_PLAYER_MUSIC);
			if (playerStateFlag) {
				intent.putExtra("PLAYSER_STATE", false);
				playerStateFlag = false;
				mPlayerImg.setImageResource(R.drawable.player_play);

				cdView.pause();
			} else {
				intent.putExtra("PLAYSER_STATE", true);
				playerStateFlag = true;
				mPlayerImg.setImageResource(R.drawable.player_pause);

				cdView.start();
			}
			sendBroadcast(intent);

			break;
		case R.id.player_pre_img:
			mIMusicService.iPlayPre();
			break;
		case R.id.player_next_img:
			mIMusicService.iPlayNext();
			break;
		}
	}

	
	/**
	 * 定义广播接收者
	 */
	public BroadcastReceiver playerReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
			if(MusicService.ACTION_PLAYER_MUSIC.equals(intent.getAction())){
				boolean playerState = intent.getBooleanExtra("PLAYSER_STATE", true);
				if (playerState) {
					playerStateFlag = true;
					mPlayerImg.setImageResource(R.drawable.player_pause);
				} else {
					playerStateFlag = false;
					mPlayerImg.setImageResource(R.drawable.player_play);
				}
			}
		}
		
	};
	
	/**
	 * 播放音乐通过Binder接口实现
	 */
	public void playerMusicByIBinder() {
		boolean playerState = mIMusicService.iPlayerMusic();
		if (playerState) {
			mPlayerImg.setImageResource(R.drawable.player_pause);
		} else {
			mPlayerImg.setImageResource(R.drawable.player_play);
		}
	}

	public class PlayerThread implements Runnable {
		@Override
		public void run() {
			while (mThreadFlag) {
				if (mIMusicService != null) {
					mHandler.post(new Runnable() {

						@Override
						public void run() {
							int currentTime = mIMusicService.iPlayCurrentTime();
							int totalTime = mIMusicService.iPlayTotalTime();
							mSeekBar.setMax(totalTime);
							mSeekBar.setProgress(currentTime);

							String current = format
									.format(new Date(currentTime));
							String total = format.format(new Date(totalTime));

							mCurrentTimeTxt.setText(current);
							mTotalTimeTxt.setText(total);

						}
					});

					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * 定义更新进度广播.
	 */
	public static final String ACTION_PROGRESS_RECEIVER = "com.scxh.android1503.ACTION_PROGRESS_RECEIVER";
	public BroadcastReceiver progressReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (ACTION_PROGRESS_RECEIVER.equals(intent.getAction())) {

				int currentTime = intent.getIntExtra("CURRENT_TIME", 0);
				int totalTime = intent.getIntExtra("TOTALE_TIME", 0);
				mSeekBar.setMax(totalTime);
				mSeekBar.setProgress(currentTime);

				String current = format.format(new Date(currentTime));
				String total = format.format(new Date(totalTime));

				mCurrentTimeTxt.setText(current);
				mTotalTimeTxt.setText(total);
			}
		}

	};

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		if (mIMusicService != null) {
			mProgress = progress;
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		if (mIMusicService != null) {
			mIMusicService.iSeekTo(mProgress);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mServiceConnection != null) {
			mThreadFlag = false;
			unbindService(mServiceConnection);
		}
	}

	/**
	 * ViewPager循环滑动
	 * 
	 */
	public class MyPagerAdapter extends PagerAdapter {
		private ArrayList<View> data = new ArrayList<View>();

		public void setData(ArrayList<View> data) {
			this.data = data;
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return Integer.MAX_VALUE;
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == arg1;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {

		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			position = position % data.size(); // 0%3=0,1%3=1,2%3=2,3%3=0,4%3=1,5%3=2,6%3=0,........

			View v = data.get(position);
			if (v.getParent() != null) {
				container.removeView(v);
			}

			container.addView(v);

			return v;
		}

	}

}
