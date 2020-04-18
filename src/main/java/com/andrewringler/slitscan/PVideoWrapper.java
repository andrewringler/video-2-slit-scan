package com.andrewringler.slitscan;

import processing.core.PImage;
import processing.video.Movie;

public class PVideoWrapper {
	private final Movie video;
	
	PVideoWrapper(Movie video) {
		this.video = video;
	}
	
	int width() {
		return video.width;
	}
	
	int height() {
		return video.height;
	}
	
	float frameRate() {
		return video.frameRate;
	}
	
	void stop() {
		video.stop();
	}
	
	void dispose() {
		video.dispose();
	}
	
	void volume(int volume) {
		video.volume(volume);
	}
	
	void play() {
		video.play();
	}
	
	void pause() {
		video.pause();
	}
	
	void read() {
		video.read();
	}
	
	PImage get() {
		return video.get();
	}
	
	float time() {
		return video.time();
	}
	
	void speed(float speed) {
		video.speed(speed);
	}
	
	void jump(float jump) {
		video.jump(jump);
	}
	
	float duration() {
		return video.duration();
	}
	
	public boolean available() {
		return video.available();
	}
}
