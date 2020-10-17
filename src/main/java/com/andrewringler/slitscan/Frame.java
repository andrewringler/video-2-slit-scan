package com.andrewringler.slitscan;

import processing.core.PImage;

public class Frame {
	private final PImage pImage;
	private final VideoMeta videoMeta;
	
	public final int height;
	public final int width;
	
	public Frame(PImage pImage, VideoMeta videoMeta) {
		this.width = pImage.width;
		this.height = pImage.height;
		this.pImage = pImage;
		this.videoMeta = videoMeta;
	}
	
	public PImage getPImage() {
		return pImage;
	}
	
	public VideoMeta getVideoMeta() {
		return videoMeta;
	}
}