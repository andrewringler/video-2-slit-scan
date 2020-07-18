package com.andrewringler.slitscan;

import java.awt.image.BufferedImage;
import java.util.concurrent.LinkedBlockingQueue;

import com.andrewringler.slitscan.jcodec.JCodecPictureRGB;

import processing.core.PConstants;
import processing.core.PImage;

public class SlitBatchImage {
	private final PImage batchedPImage;
	private final JCodecPictureRGB batchedPicture;
	private final ColorDepth colorDepth;
	private final int width;
	private final int height;
	
	SlitBatchImage(LinkedBlockingQueue<Slit> slitQueue, ColorDepth colorDepth, int slitWidth, int width, int height) {
		this.colorDepth = colorDepth;
		this.width = width;
		this.height = height;
		
		if (colorDepth.isSixteenBit()) {
			this.batchedPImage = null;
			this.batchedPicture = slitQueue.poll().getPicture();
		} else {
			this.batchedPicture = null;
			this.batchedPImage = new PImage(width, height, PConstants.RGB);
			int batchSize = slitQueue.size();
			for (int i = 0; i < batchSize; i++) {
				PImage slit = slitQueue.poll().getPImage();
				batchedPImage.copy(slit, 0, 0, slit.width, slit.height, i * slitWidth, 0, slit.width, slit.height);
			}
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
	
	public JCodecPictureRGB getPicture() {
		return batchedPicture;
	}
	
	public BufferedImage getBufferedImage() {
		return (BufferedImage) batchedPImage.getNative();
	}
}
