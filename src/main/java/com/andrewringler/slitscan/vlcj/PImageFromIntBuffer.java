package com.andrewringler.slitscan.vlcj;

import java.nio.IntBuffer;

import processing.core.PConstants;
import processing.core.PImage;

class PImageFromIntBuffer extends PImage {
	public PImageFromIntBuffer(int width, int height, IntBuffer intBuffer) {
		super(width, height, PConstants.ARGB);
		intBuffer.get(pixels);
		updatePixels();
	}
}