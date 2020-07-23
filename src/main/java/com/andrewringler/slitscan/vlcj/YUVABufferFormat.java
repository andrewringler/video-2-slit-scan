package com.andrewringler.slitscan.vlcj;

import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;

public class YUVABufferFormat extends BufferFormat {
	
	public YUVABufferFormat(int width, int height) {
		super("RGA4", width, height, new int[] { width * 8 }, new int[] { height });
	}
}
