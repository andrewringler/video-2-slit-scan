package com.andrewringler.slitscan;

import processing.core.PImage;

public interface FrameReady {
	public void processFrame(PImage frame);
}