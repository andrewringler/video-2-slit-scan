package com.andrewringler.slitscan;

import processing.core.PImage;

public interface VideoWrapper {
	
	void stop();
	
	void play();
	
	float duration();
	
	void pause();
	
	void jump(float f);
	
	float timeSeconds();
	
	PImage get();
	
	int width();
	
	int height();
	
	void dispose();
	
	void volume(int i);
	
	void speed(float playSpeed);
	
}