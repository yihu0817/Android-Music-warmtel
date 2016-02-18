package com.warmtel.music.demo1;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.warmtel.music.model.MusicBean;
import com.warmtel.music.R;
import com.warmtel.music.util.Logs;

import java.io.IOException;
import java.util.ArrayList;

public class MusicService extends Service {
	public static final String ACTION_PLAYER_MUSIC = "com.scxh.android1503.ACTION_PLAYER_MUSIC";
	private MediaPlayer mMediaPlayer;
	private ArrayList<MusicBean> musicPathLists;
	private int currentPostion = 0;

	public interface IMusicService {
		public boolean iPlayerMusic();

		public int iPlayCurrentTime();

		public int iPlayTotalTime();

		public void iSeekTo(int msec);

		public void iPlayPre();

		public void iPlayNext();
		
		public boolean isPlayering();
	}

	public class MusiceServiceBinder extends Binder implements IMusicService {

		@Override
		public boolean iPlayerMusic() {
			return playerMusic();
		}

		@Override
		public int iPlayCurrentTime() {
			if (mMediaPlayer != null) {
				return mMediaPlayer.getCurrentPosition();
			} else {
				return 0;
			}
		}

		@Override
		public int iPlayTotalTime() {
			if (mMediaPlayer != null) {
				return mMediaPlayer.getDuration();
			} else {
				return 0;
			}

		}

		@Override
		public void iSeekTo(int msec) {
			if (mMediaPlayer != null) {
				mMediaPlayer.seekTo(msec);
			}
		}

		@Override
		public void iPlayPre() {
			if (--currentPostion < 0) {
				currentPostion = 0;
			}
			mMediaPlayer.reset();
			initMusic();
			playerMusic();
		}

		@Override
		public void iPlayNext() {
			if (++currentPostion > musicPathLists.size() - 1) {
				currentPostion = musicPathLists.size() - 1;
			}
			mMediaPlayer.reset();
			initMusic();
			playerMusic();
		}

		@Override
		public boolean isPlayering() {
			if(mMediaPlayer.isPlaying()){
				return true;
			}else{
				return false;
			}
			
		}

	}

	public MusiceServiceBinder mMusiceServiceBinder = new MusiceServiceBinder();

	@Override
	public void onCreate() {
		super.onCreate();
		Logs.v("MusicService onCreate >>>>>  ");
		
		/**注册广播*/
		IntentFilter filter = new IntentFilter(ACTION_PLAYER_MUSIC);
		registerReceiver(playerReceiver, filter);
		
		new Thread(new PlayerServiceThread()).start();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Logs.v("MusicService  onStartCommand  >>>>>>>>>");
		if(intent ==null)
			return super.onStartCommand(intent, flags, startId);

		if(musicPathLists == null){
			musicPathLists = intent.getParcelableArrayListExtra("MUSIC_LIST");
		}
		currentPostion = intent.getIntExtra("CURRENT_POSTION", 0);
		
		initMusic();
		
		playerMusic();
		
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		Logs.v("MusicService  onBind  >>>>>>>>>");
		return mMusiceServiceBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Logs.v("MusicService  onUnbind  >>>>>>>>>");
		return super.onUnbind(intent);
	}

	@Override
	public void onDestroy() {
		Logs.v("MusicService  onDestroy  >>>>>>>>>");
		super.onDestroy();
		if (mMediaPlayer != null) {
			if (mMediaPlayer.isPlaying()) {
				mMediaPlayer.stop();
			}
			mMediaPlayer.release();
		}
		/**注销广播*/
		if(playerReceiver != null){
			unregisterReceiver(playerReceiver);
		}
		/**结束更新线程*/
		playerThreadFlag = false;
	}

	public void initMusic() {
		if (mMediaPlayer == null)
			mMediaPlayer = new MediaPlayer();
		try {
	
			mMediaPlayer.reset();
			
			mMediaPlayer.setDataSource(musicPathLists.get(currentPostion).getMusicPath());
			mMediaPlayer.prepare();

			mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {

				@Override
				public void onCompletion(MediaPlayer mp) {
					mp.start();
				}
			});

		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean playerMusic() {
		if (mMediaPlayer.isPlaying()) {
			mMediaPlayer.pause();
			return false;
		} else {
			mMediaPlayer.start();
			notificationMusicPlayer();
			return true;
		}
	}

	public BroadcastReceiver playerReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
			if(ACTION_PLAYER_MUSIC.equals(intent.getAction())){
				boolean playerState = intent.getBooleanExtra("PLAYSER_STATE", true);
				if (playerState) {
					mMediaPlayer.start();
					notificationMusicPlayer();
				} else {
					mMediaPlayer.pause();
					notificationMusicPlayer();
				}
			}
		}
		
	};
	private boolean playerThreadFlag = true;
	public class PlayerServiceThread implements Runnable{
		@Override
		public void run() {
				while(playerThreadFlag ){
					if(mMediaPlayer != null && mMediaPlayer.isPlaying()){
						int currentTime = mMediaPlayer.getCurrentPosition();
						int totalTime = mMediaPlayer.getDuration();
					
						Intent intent = new Intent(MusicActivity.ACTION_PROGRESS_RECEIVER);
						intent.putExtra("CURRENT_TIME", currentTime);
						intent.putExtra("TOTALE_TIME", totalTime);
						sendBroadcast(intent);
					}

					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
		}
	}
	/**
	 * 音乐播放通知
	 */
	public void notificationMusicPlayer(){
		NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setSmallIcon(R.drawable.m4);
		RemoteViews views = new RemoteViews(getPackageName(), R.layout.notification);
		builder.setContent(views);
		
		if(mMediaPlayer.isPlaying()){
			views.setImageViewResource(R.id.iv_pause, R.drawable.nc_pause);
		}else{
			views.setImageViewResource(R.id.iv_pause, R.drawable.nc_play);
		}
		
		/**notification通知行为*/
		Intent intent = new Intent(MusicService.ACTION_PLAYER_MUSIC);
		if(mMediaPlayer.isPlaying()){
			intent.putExtra("PLAYSER_STATE", false);
		}else{
			intent.putExtra("PLAYSER_STATE", true);
		}
		PendingIntent pIntent = PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.iv_pause, pIntent);
		
		
		manager.notify(111, builder.build());
	}

}
