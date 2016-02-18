package com.warmtel.music.demo3;


import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;


public abstract class BaseActivity extends FragmentActivity {
    public ThreeMusicService.IMusicService mIMusicService;
    public ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mIMusicService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mIMusicService = (ThreeMusicService.IMusicService) service;
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }
}
