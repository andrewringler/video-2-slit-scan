package com.andrewringler.slitscan;

public enum ColorDepth {
	EIGHT_BIT,
	SIXTEEN_BIT;
	
	public boolean isEightBit() {
		return this.equals(EIGHT_BIT);
	}
	
	public boolean isSixteenBit() {
		return this.equals(SIXTEEN_BIT);
	}
}
