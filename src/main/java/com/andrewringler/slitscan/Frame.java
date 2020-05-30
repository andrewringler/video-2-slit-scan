package com.andrewringler.slitscan;

import static com.andrewringler.slitscan.ColorDepth.EIGHT_BIT;
import static com.andrewringler.slitscan.ColorDepth.SIXTEEN_BIT;

import org.jcodec.common.model.Picture;

import com.andrewringler.slitscan.jcodec.JCodecPictureRGB;

import processing.core.PImage;

public class Frame {
	private final ColorDepth colorDepth;
	private final PImage pImage;
	private final JCodecPictureRGB picture;
	
	public final int height;
	public final int width;
	
	public Frame(PImage pImage, Picture picture) {
		if (picture != null && picture.isHiBD()) {
			this.colorDepth = SIXTEEN_BIT;
		} else {
			this.colorDepth = EIGHT_BIT;
		}
		this.width = pImage.width;
		this.height = pImage.height;
		this.pImage = pImage;
		if (picture != null) {
			this.picture = new JCodecPictureRGB(picture);
		} else {
			this.picture = null;
		}
	}
	
	public ColorDepth getColorDepth() {
		return colorDepth;
	}
	
	public PImage getPImage() {
		return pImage;
	}
	
	public JCodecPictureRGB getPicture() {
		return picture;
	}
}