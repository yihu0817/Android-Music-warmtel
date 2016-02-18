package com.warmtel.music.demo3;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.warmtel.music.R;
import com.warmtel.music.model.Music;
import com.warmtel.music.util.Constances;
import com.warmtel.music.view.CDView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ThreeMusicActivity extends BaseActivity implements OnClickListener,
        OnSeekBarChangeListener {
    private ImageView mPlayerPreImg, mPlayerNextImg, mPlayerImg;
    private TextView mCurrentTimeTxt, mTotalTimeTxt,mMusicNameTxt;
    private SeekBar mSeekBar;
    private CDView mCdView;

    private SimpleDateFormat format = new SimpleDateFormat("mm:ss");
    private Handler mHandler = new Handler();
    private boolean mThreadFlag = true;
    private int mProgress;
    private Music mMusic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_two_music_main_layout);
        initView();

        /** 负责更新播放进度和时间 */
        new Thread(new PlayerThread()).start();

        mMusic = Constances.mMediaLists.get(Constances.mCurrentPostion);
        // 设置默认播放
        mPlayerImg.setImageResource(R.drawable.player_pause);
        mMusicNameTxt.setText(mMusic.getTitle());

        inintCdView(mMusic);
    }
    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, ThreeMusicService.class);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
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
        mMusicNameTxt = (TextView) findViewById(R.id.player_music_name_txt);
        mSeekBar = (SeekBar) findViewById(R.id.player_seekbar);
        mCdView = (CDView) findViewById(R.id.player_cdview);

        mPlayerPreImg.setOnClickListener(this);
        mPlayerNextImg.setOnClickListener(this);
        mPlayerImg.setOnClickListener(this);
        mSeekBar.setOnSeekBarChangeListener(this);
    }

    private void inintCdView(Music music){
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        int sScreenWidth = dm.widthPixels;
        int sScreenHeight = dm.heightPixels;

        Bitmap bmp = BitmapFactory.decodeFile(music.getImage());
        if(bmp == null) bmp = BitmapFactory.decodeResource(getResources(), R.drawable.m4);
        mCdView.setImage(Bitmap.createScaledBitmap(bmp, (int) (sScreenWidth * 0.7), (int) (sScreenWidth * 0.7), true));
        mCdView.start();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.player_pause_img:
                playerMusicByIBinder();
                break;
            case R.id.player_pre_img:

                mIMusicService.iPlayPre();
                mMusic = Constances.mMediaLists.get(Constances.mCurrentPostion);
                inintCdView(mMusic);
                break;
            case R.id.player_next_img:
                mIMusicService.iPlayNext();
                mMusic = Constances.mMediaLists.get(Constances.mCurrentPostion);
                inintCdView(mMusic);
                break;
        }
    }

    /**
     * 播放音乐通过Binder接口实现
     */
    public void playerMusicByIBinder() {
        boolean playerState = mIMusicService.iPlayerMusic();
        if (playerState) {
            mCdView.start();
            mPlayerImg.setImageResource(R.drawable.player_pause);
        } else {
            mCdView.pause();
            mPlayerImg.setImageResource(R.drawable.player_play);
        }
    }

    public class PlayerThread implements Runnable {
        @Override
        public void run() {
            while (mThreadFlag) {
                if (mIMusicService != null && mIMusicService.isPlayering()) {
                    mHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            int currentTime = mIMusicService.iPlayCurrentTime();
                            int totalTime = mIMusicService.iPlayTotalTime();
                            mSeekBar.setMax(totalTime);
                            mSeekBar.setProgress(currentTime);

                            String current = format .format(new Date(currentTime));
                            String total = format.format(new Date(totalTime));

                            mCurrentTimeTxt.setText(current);
                            mTotalTimeTxt.setText(total);

                            mMusicNameTxt.setText(mIMusicService.iPlayerMusicName());
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


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
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
            unbindService(mServiceConnection);
        }
        if(mCdView != null) {
            mCdView.pause();
        }
        mThreadFlag = false;
    }

}
