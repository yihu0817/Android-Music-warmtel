package com.warmtel.music.demo3;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.warmtel.music.R;
import com.warmtel.music.model.Music;
import com.warmtel.music.util.Constances;

import java.io.File;
import java.util.ArrayList;

public class ThreeMusicListActivity extends BaseActivity implements OnItemClickListener, View.OnClickListener {
    private ListView mListView;
    private ImageView mMusicIcon;
    private TextView mMusicTitle;
    private TextView mMusicArtist;

    private ImageView mPreImageView;
    private ImageView mPlayImageView;
    private ImageView mNextImageView;

    private SeekBar mSeekBar;
    private MusicListAdapter mAdapter;
    private Handler mHandler = new Handler();
    private boolean mThreadFlag = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_three_list_layout);
        setupViews();
        binderService();
        asyncQueryMedia();
        /** 负责更新播放进度和时间 */
        new Thread(new PlayerThread()).start();
    }

    private void binderService() {
        Intent intent = new Intent(this, ThreeMusicService.class);
        startService(intent);
        /** 绑定服务，获取交互实例 */
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private void setupViews() {
        mListView = (ListView) findViewById(R.id.music_list_view);
        mMusicIcon = (ImageView) findViewById(R.id.iv_play_icon);
        mMusicTitle = (TextView) findViewById(R.id.tv_play_title);
        mMusicArtist = (TextView) findViewById(R.id.tv_play_artist);

        mPreImageView = (ImageView) findViewById(R.id.iv_pre);
        mPlayImageView = (ImageView) findViewById(R.id.iv_play);
        mNextImageView = (ImageView) findViewById(R.id.iv_next);

        mSeekBar = (SeekBar) findViewById(R.id.play_progress);

        mAdapter = new MusicListAdapter(this);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);

        mMusicIcon.setOnClickListener(this);
        mPreImageView.setOnClickListener(this);
        mPlayImageView.setOnClickListener(this);
        mNextImageView.setOnClickListener(this);
    }

    public void asyncQueryMedia() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Constances.mMediaLists.clear();
                queryMusic(Environment.getExternalStorageDirectory() + File.separator);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.setListData(Constances.mMediaLists);
                        Constances.mCurrentPostion = 0;
                        mIMusicService.iInitMusic();
                        changeBottomMusicView();
                    }
                });
            }
        }).start();
    }

    /**
     * 获取目录下的歌曲
     *
     * @param dirName
     */
    public void queryMusic(String dirName) {
        Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                MediaStore.Audio.Media.DATA + " like ?",
                new String[]{dirName + "%"},
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER);

        if (cursor == null) return;

        // id title singer data time image
        Music music;
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            // 如果不是音乐
            String isMusic = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC));
            if (isMusic != null && isMusic.equals("")) continue;

            String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
            String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));

            if (isRepeat(title, artist)) continue;

            music = new Music();
            music.setId(cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)));
            music.setTitle(title);
            music.setArtist(artist);
            music.setUri(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)));
            music.setLength(cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)));
            music.setImage(getAlbumImage(cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))));

            Constances.mMediaLists.add(music);
        }

        cursor.close();
    }

    /**
     * 根据音乐名称和艺术家来判断是否重复包含了
     *
     * @param title
     * @param artist
     * @return
     */
    private boolean isRepeat(String title, String artist) {
        for (Music music : Constances.mMediaLists) {
            if (title.equals(music.getTitle()) && artist.equals(music.getArtist())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据歌曲id获取图片
     *
     * @param albumId
     * @return
     */
    private String getAlbumImage(int albumId) {
        String result = "";
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
                    Uri.parse("content://media/external/audio/albums/"
                            + albumId), new String[]{"album_art"}, null,
                    null, null);
            for (cursor.moveToFirst(); !cursor.isAfterLast(); ) {
                result = cursor.getString(0);
                break;
            }
        } catch (Exception e) {
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }

        return null == result ? null : result;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_play_icon:
                startActivity(new Intent(this, ThreeMusicActivity.class));
                break;
            case R.id.iv_play:
                if (mIMusicService.iPlayerMusic()) {
                    mPlayImageView.setImageResource(android.R.drawable.ic_media_pause);
                } else {
                    mPlayImageView.setImageResource(android.R.drawable.ic_media_play);
                }
                break;
            case R.id.iv_next:
                mIMusicService.iPlayNext(); // 下一曲
                mPlayImageView.setImageResource(android.R.drawable.ic_media_pause);
                changeBottomMusicView();
                break;
            case R.id.iv_pre:
                mIMusicService.iPlayPre(); // 上一曲
                mPlayImageView.setImageResource(android.R.drawable.ic_media_pause);
                changeBottomMusicView();
                break;
        }
    }

    public class MusicListAdapter extends BaseAdapter {
        private ArrayList<Music> list = new ArrayList<>();
        private Context context;
        private int mPlayingPosition;

        public void setPlayingPosition(int position) {
            mPlayingPosition = position;
            notifyDataSetChanged();
        }

        public MusicListAdapter(Context context) {
            this.context = context;
        }

        public void setListData(ArrayList<Music> list) {
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
            final ViewHolder holder;

            if (convertView == null) {
                convertView = View.inflate(context, R.layout.music_list_item, null);
                holder = new ViewHolder();
                holder.title = (TextView) convertView.findViewById(R.id.tv_music_list_title);
                holder.artist = (TextView) convertView.findViewById(R.id.tv_music_list_artist);
                holder.icon = (ImageView) convertView.findViewById(R.id.music_list_icon);
                holder.mark = convertView.findViewById(R.id.music_list_selected);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            if (mPlayingPosition == position)
                holder.mark.setVisibility(View.VISIBLE);
            else
                holder.mark.setVisibility(View.INVISIBLE);

            Music music = (Music) getItem(position);

            Bitmap icon = BitmapFactory.decodeFile(music.getImage());
            holder.icon.setImageBitmap(icon == null ? BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher) : icon);
            holder.title.setText(music.getTitle());
            holder.artist.setText(music.getArtist());

            return convertView;
        }

        class ViewHolder {
            ImageView icon;
            TextView title;
            TextView artist;
            View mark;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        MusicListAdapter adapter = (MusicListAdapter) parent.getAdapter();
        adapter.setPlayingPosition(position);
        Constances.mCurrentPostion = position;
        mPlayImageView.setImageResource(android.R.drawable.ic_media_pause);
        mIMusicService.iInitMusic();
        mIMusicService.iPlayerMusic();
        changeBottomMusicView();
    }

    /**
     * 设置当前显示音乐信息
     */
    public void changeBottomMusicView(){
        Music music = Constances.mMediaLists.get(Constances.mCurrentPostion);
        Bitmap icon = BitmapFactory.decodeFile(music.getImage());
        mMusicIcon.setImageBitmap(icon == null ? BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher) : icon);
        mMusicTitle.setText(music.getTitle());
        mMusicArtist.setText(music.getArtist());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mServiceConnection != null) {
            unbindService(mServiceConnection);
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
}
