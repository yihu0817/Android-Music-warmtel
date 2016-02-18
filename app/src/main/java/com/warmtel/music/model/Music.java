package com.warmtel.music.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Music implements Parcelable{
	private int id; // 音乐id
	private String title; // 音乐标题
	private String uri; // 音乐路径
	private int length; // 长度
	private String image; // icon
	private String artist; // 艺术家

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(id);
		dest.writeString(title);
		dest.writeString(uri);
		dest.writeInt(length);
		dest.writeString(image);
		dest.writeString(artist);
	}

	/**
	 * 必须用 public static final 修饰符
	 * 对象必须用 CREATOR
	 */
	public static final Parcelable.Creator<Music> CREATOR = new Parcelable.Creator<Music>() {

		@Override
		public Music createFromParcel(Parcel source) {
			Music music = new Music();
			music.setId(source.readInt());
			music.setTitle(source.readString());
			music.setUri(source.readString());
			music.setLength(source.readInt());
			music.setImage(source.readString());
			music.setArtist(source.readString());
			return music;
		}

		@Override
		public Music[] newArray(int size) {
			return new Music[size];
		}

	};
}
