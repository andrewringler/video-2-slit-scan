package com.andrewringler.slitscan;

public enum RotateVideo {
	NO_ROTATION(0),
	NINETY_DEGREES(90),
	ONE_EIGHTY_DEGREES(180),
	TWO_SEVENTY_DEGREES(270);
	
	private final int degrees;
	
	RotateVideo(int degrees) {
		this.degrees = degrees;
	}
	
	public static RotateVideo rotateVideo(int rotation) {
		switch (rotation) {
			case 90:
				return NINETY_DEGREES;
			case 180:
				return ONE_EIGHTY_DEGREES;
			case 270:
				return TWO_SEVENTY_DEGREES;
			default:
				return NO_ROTATION;
		}
	}
	
	public boolean hasRotation() {
		return !this.equals(NO_ROTATION);
	}
	
	public Dimensions rotatedDimensions(Dimensions videoDimensions) {
		switch (this) {
			case NINETY_DEGREES:
			case TWO_SEVENTY_DEGREES:
				return new Dimensions(videoDimensions.height, videoDimensions.width);
			default:
				/* no rotation or 180 degrees */
				return videoDimensions;
		}
	}
	
	public String degreesString() {
		System.out.println(degrees);
		return String.valueOf(degrees);
	}
	
	public int degrees() {
		return degrees;
	}
}
