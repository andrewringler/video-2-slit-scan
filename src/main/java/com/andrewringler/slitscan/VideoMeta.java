package com.andrewringler.slitscan;

public class VideoMeta {
	final float duration;
	final float timeSeconds;
	final int width;
	final int height;
	
	public VideoMeta(float duration, float timeSeconds, int width, int height) {
		this.duration = duration;
		this.timeSeconds = timeSeconds;
		this.width = width;
		this.height = height;
	}
	
	public float duration() {
		return duration;
	}
	
	public float timeSeconds() {
		return timeSeconds;
	}
	
	public int width() {
		return width;
	}
	
	public int height() {
		return height;
	}
}
