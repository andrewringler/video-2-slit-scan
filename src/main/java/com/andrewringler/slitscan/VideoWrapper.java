package com.andrewringler.slitscan;

public interface VideoWrapper {
	
	void stop();
	
	void play();
	
	void pause();
	
	void jump(float f);
	
	void dispose();
	
	void volume(int i);
	
	void speed(float playSpeed);
	
	float duration();
	
	float timeSeconds();
	
	int widthCoded();
	
	int heightCoded();
	
	int widthDisplay();
	
	int heightDisplay();
	
}