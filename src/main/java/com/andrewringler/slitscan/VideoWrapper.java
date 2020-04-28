package com.andrewringler.slitscan;

public interface VideoWrapper extends VideoMeta {
	
	void stop();
	
	void play();
	
	void pause();
	
	void jump(float f);
	
	void dispose();
	
	void volume(int i);
	
	void speed(float playSpeed);
	
}