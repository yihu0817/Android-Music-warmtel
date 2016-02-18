package com.warmtel.music.demo3;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Binder;
import android.os.IBinder;

import com.warmtel.music.util.Constances;

import java.io.IOException;

public class ThreeMusicService extends Service {
	private MediaPlayer mMediaPlayer;

	public interface IMusicService {
		 boolean iPlayerMusic();

		 int iPlayCurrentTime();

		 int iPlayTotalTime();

		 void iSeekTo(int msec);

		 void iPlayPre();

		 void iPlayNext();
		
		 boolean isPlayering();

		 String iPlayerMusicName();

		 void iInitMusic();
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
			if (--Constances.mCurrentPostion < 0) {
				Constances.mCurrentPostion = 0;
			}
			initMusic();
			playerMusic();
		}

		@Override
		public void iPlayNext() {
			if (++Constances.mCurrentPostion > Constances.mMediaLists.size() - 1) {
				Constances.mCurrentPostion = Constances.mMediaLists.size() - 1;
			}
			initMusic();
			playerMusic();
		}

		@Override
		public boolean isPlayering() {
			if(mMediaPlayer!=null && mMediaPlayer.isPlaying()){
				return true;
			}else{
				return false;
			}
			
		}

		@Override
		public String iPlayerMusicName() {
			if(Constances.mMediaLists != null)
				return Constances.mMediaLists.get(Constances.mCurrentPostion).getTitle();
			else
				return "";
		}

		@Override
		public void iInitMusic() {
			initMusic();
		}

	}

	public MusiceServiceBinder mMusiceServiceBinder = new MusiceServiceBinder();

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
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
			mMediaPlayer.setDataSource(Constances.mMediaLists.get(Constances.mCurrentPostion).getUri());
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
