package com.andrewringler.slitscan;

import processing.core.PConstants;
import processing.core.PImage;

public class Slit {
	final int width;
	final int height;
	final PImage pImage;
	
	public Slit(int width, int height, int slitX, Frame frame) {
		this.width = width;
		this.height = height;
		pImage = new PImage(width, height, PConstants.RGB);
		pImage.copy(frame.getPImage(), slitX, 0, width, frame.height, 0, 0, width, height);
	}
	
	public PImage getPImage() {
		return pImage;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
}
