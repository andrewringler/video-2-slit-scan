package com.andrewringler.slitscan;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.api.PictureWithMetadata;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.scale.AWTUtil;

import processing.core.PImage;

public class VideoWrapperFrameGrab {
	Object threadSafe = new Object();
	Video2SlitScan p;
	FrameGrab fg;
	int width = 0;
	int height = 0;
	PictureWithMetadata picture;
	
	int frame = 0;
	volatile boolean playing = false;
	
	VideoWrapperFrameGrab(Video2SlitScan p, String absolutePath) {
		this.p = p;
		File file = new File(absolutePath);
		try {
			fg = FrameGrab.createFrameGrab(NIOUtils.readableChannel(file));
		} catch (IOException | JCodecException e) {
			e.printStackTrace();
		}
	}
	
	public void stop() {
		playing = false;
	}
	
	public void dispose() {
		fg = null;
	}
	
	public void volume(int i) {
	}
	
	public void play() {
		if (!playing) {
			playing = true;
			new Thread(new Runnable() {
				public void run() {
					try {
						PictureWithMetadata p2;
						while (playing && null != (p2 = fg.getNativeFrameWithMetadata())) {
							picture = p2;
							width = picture.getPicture().getWidth();
							height = picture.getPicture().getHeight();
							p.doProcessFrame();
							//									println("got frame");
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
	}
	
	public float duration() {
		if (picture != null) {
			return (float) picture.getDuration();
		}
		return 0;
	}
	
	public void pause() {
		playing = false;
	}
	
	public void jump(float f) {
		try {
			if (f == 0) {
				fg.seekToFramePrecise(0);
			} else {
				fg.seekToSecondPrecise(f);
			}
		} catch (IOException | JCodecException e) {
			e.printStackTrace();
		}
	}
	
	public PImage read() {
		if (picture != null) {
			BufferedImage bufferedImage = AWTUtil.toBufferedImage(picture.getPicture());
			return new PImage(bufferedImage);
		}
		return null;
	}
	
	public float time() {
		if (picture != null) {
			return (float) picture.getTimestamp();
		}
		return 0;
	}
	
	public PImage get() {
		return this.read();
	}
	
	public int width() {
		return width;
	}
	
	public int height() {
		return height;
	}
}