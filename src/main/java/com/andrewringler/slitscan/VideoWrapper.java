package com.andrewringler.slitscan;

public interface VideoWrapper {
	
	void stop();
	
	void play();
	
	float duration();
	
	void pause();
	
	void jump(float f);
	
	float timeSeconds();
	
	int width();
	
	int height();
	
	void dispose();
	
	void volume(int i);
	
	void speed(float playSpeed);
	
}