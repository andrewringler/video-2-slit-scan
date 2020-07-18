package com.andrewringler.slitscan;

public enum ColorDepth {
	EIGHT_BIT,
	SIXTEEN_BIT;
	
	boolean isEightBit() {
		return this.equals(EIGHT_BIT);
	}
	
	boolean isSixteenBit() {
		return this.equals(SIXTEEN_BIT);
	}
}
