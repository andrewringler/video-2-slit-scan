package com.andrewringler.slitscan;

public enum ImageFormat {
	TIFF,
	PNG;
	
	public boolean tiff() {
		return equals(TIFF);
	}
	
	public boolean png() {
		return equals(PNG);
	}
}
