package com.andrewringler.slitscan;

import org.jcodec.common.model.Rect;

import com.andrewringler.slitscan.jcodec.JCodecPictureRGB;

import processing.core.PConstants;
import processing.core.PImage;

public class Slit {
	final int width;
	final int height;
	final PImage pImage;
	final JCodecPictureRGB picture;
	
	public Slit(int width, int height, int slitX, Frame frame) {
		this.width = width;
		this.height = height;
		pImage = new PImage(width, height, PConstants.RGB);
		pImage.copy(frame.getPImage(), slitX, 0, width, frame.height, 0, 0, width, height);
		if (frame.getPicture() != null) {
			picture = frame.getPicture().cloneCropped(new Rect(slitX, 0, width, height));
		} else {
			picture = null;
		}
	}
	
	public PImage getPImage() {
		return pImage;
	}
	
	public JCodecPictureRGB getPicture() {
		return picture;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
}
