package com.warmtel.music.demo2;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Binder;
import android.os.IBinder;

import com.warmtel.music.model.Music;

import java.io.IOException;
import java.util.ArrayList;

public class TwoMusicService extends Service {
	private MediaPlayer mMediaPlayer;
	private ArrayList<Music> musicPathLists;
	private int currentPostion = 0;

	public interface IMusicService {
		public boolean iPlayerMusic();

		public int iPlayCurrentTime();

		public int iPlayTotalTime();

		public void iSeekTo(int msec);

		public void iPlayPre();

		public void iPlayNext();
		
		public boolean isPlayering();

		public String iPlayerMusicName();
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
			initMusic();
			playerMusic();
		}

		@Override
		public void iPlayNext() {
			if (++currentPostion > musicPathLists.size() - 1) {
				currentPostion = musicPathLists.size() - 1;
			}
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

		@Override
		public String iPlayerMusicName() {
			if(musicPathLists != null)
				return musicPathLists.get(currentPostion).getTitle();
			else
				return "";
		}

	}

	public MusiceServiceBinder mMusiceServiceBinder = new MusiceServiceBinder();

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		musicPathLists = intent.getParcelableArrayListExtra("MUSIC_LIST");
		currentPostion = intent.getIntExtra("CURRENT_POSTION", 0);

		initMusic();

		playerMusic();

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mMusiceServiceBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mMediaPlayer != null) {
			if (mMediaPlayer.isPlaying()) {
				mMediaPlayer.stop();
			}
			mMediaPlayer.release();
		}
	}

	public void initMusic() {
		if (mMediaPlayer == null)
			mMediaPlayer = new MediaPlayer();
		try {
			mMediaPlayer.reset();
			mMediaPlayer.setDataSource(musicPathLists.get(currentPostion).getUri());
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
			return true;
		}
	}

}
