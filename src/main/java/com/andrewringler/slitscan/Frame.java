package com.andrewringler.slitscan;

import static com.andrewringler.slitscan.ColorDepth.EIGHT_BIT;
import static com.andrewringler.slitscan.ColorDepth.SIXTEEN_BIT;

import java.awt.image.Raster;

import org.jcodec.common.model.Picture;

import com.andrewringler.slitscan.jcodec.JCodecPictureRGB;

import processing.core.PImage;

public class Frame {
	private final ColorDepth colorDepth;
	private final PImage pImage;
	private final JCodecPictureRGB picture;
	private final Raster raster;
	private final VideoMeta videoMeta;
	
	public final int height;
	public final int width;
	
	public Frame(Raster raster, VideoMeta videoMeta) {
		this.raster = raster;
		this.videoMeta = videoMeta;
		this.width = -1;
		this.height = -1;
		this.pImage = null;
		this.picture = null;
		this.colorDepth = SIXTEEN_BIT;
	}
	
	public Frame(PImage pImage, Picture picture, VideoMeta videoMeta) {
		if (picture != null && picture.isHiBD()) {
			this.colorDepth = SIXTEEN_BIT;
		} else {
			this.colorDepth = EIGHT_BIT;
		}
		this.width = pImage.width;
		this.height = pImage.height;
		this.pImage = pImage;
		this.videoMeta = videoMeta;
		this.raster = null;
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
	
	public Raster getRaster() {
		return raster;
	}
	
	public boolean isRaster() {
		return raster != null;
	}
	
	public VideoMeta getVideoMeta() {
		return videoMeta;
	}
}