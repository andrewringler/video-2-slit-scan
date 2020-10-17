package com.andrewringler.slitscan;

import java.awt.image.BufferedImage;
import java.util.concurrent.LinkedBlockingQueue;

import processing.core.PConstants;
import processing.core.PImage;

public class SlitBatchImage {
	private final PImage batchedPImage;
	private final ColorDepth colorDepth;
	private final int width;
	private final int height;
	
	SlitBatchImage(LinkedBlockingQueue<Slit> slitQueue, ColorDepth colorDepth, int slitWidth, int width, int height) {
		this.colorDepth = colorDepth;
		this.width = width;
		this.height = height;
		this.batchedPImage = new PImage(width, height, PConstants.RGB);
		int batchSize = slitQueue.size();
		for (int i = 0; i < batchSize; i++) {
			PImage slit = slitQueue.poll().getPImage();
			batchedPImage.copy(slit, 0, 0, slit.width, slit.height, i * slitWidth, 0, slit.width, slit.height);
		}
	}
	
	public boolean isSixteenBit() {
		return colorDepth.isSixteenBit();
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	public BufferedImage getBufferedImage() {
		return (BufferedImage) batchedPImage.getNative();
	}
}
