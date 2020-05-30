package com.andrewringler.slitscan.jcodec;

import static java.lang.Math.floor;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;
import org.jcodec.javase.scale.AWTUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.andrewringler.slitscan.Frame;
import com.andrewringler.slitscan.FrameReady;
import com.andrewringler.slitscan.VideoWrapper;

import processing.core.PImage;

public class VideoWrapperJCodec implements VideoWrapper {
	private static final Logger LOG = LoggerFactory.getLogger(VideoWrapperJCodec.class);
	
	private FrameReady frameReady;
	private FrameGrab frameGrab;
	AtomicBoolean playing = new AtomicBoolean(false);
	private ExecutorService playThread = Executors.newFixedThreadPool(1);
	private float durationInSeconds = 0;
	private int totalFrames = 0;
	private AtomicInteger currentFrame = new AtomicInteger(0);
	private int width = 0;
	private int height = 0;
	
	public VideoWrapperJCodec(String absolutePath, FrameReady frameReady, boolean startPlaying) {
		this.frameReady = frameReady;
		this.playing.set(startPlaying);
		File file = new File(absolutePath);
		
		try {
			frameGrab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(file));
			durationInSeconds = (float) frameGrab.getVideoTrack().getMeta().getTotalDuration();
			totalFrames = frameGrab.getVideoTrack().getMeta().getTotalFrames();
			width = frameGrab.getVideoTrack().getMeta().getVideoCodecMeta().getSize().getWidth();
			height = frameGrab.getVideoTrack().getMeta().getVideoCodecMeta().getSize().getHeight();
			LOG.info("Video meta: width=" + width + " height=" + height);
		} catch (Exception e) {
			LOG.error("Error trying to load video", e);
		}
		
		doPlay();
	}
	
	private void doPlay() {
		playThread.execute(new Runnable() {
			public void run() {
				Picture picture = null;
				if (frameGrab != null && playing.get()) {
					do {
						try {
							picture = frameGrab.getNativeFrame();
							if (picture != null) {
								LOG.info("Frame meta: width=" + picture.getWidth() + " height=" + picture.getHeight() + " color=" + picture.getColor());
								currentFrame.incrementAndGet();
								BufferedImage bufferedImage = AWTUtil.toBufferedImage(picture);
								frameReady.processFrame(new Frame(new PImage(bufferedImage), picture));
							}
						} catch (IOException e) {
							// nothing
						}
					} while (picture != null && playing.get());
				}
			}
		});
	}
	
	@Override
	public float duration() {
		return durationInSeconds;
	}
	
	@Override
	public float timeSeconds() {
		if (totalFrames > 0) {
			return (float) currentFrame.get() / (float) totalFrames * durationInSeconds;
		}
		return 0;
	}
	
	@Override
	public int width() {
		return width;
	}
	
	@Override
	public int height() {
		return height;
	}
	
	@Override
	public void stop() {
		playing.set(false);
	}
	
	@Override
	public void play() {
		if (playing.get() == false) {
			playing.set(true);
			doPlay();
		}
	}
	
	@Override
	public void pause() {
		playing.set(false);
	}
	
	@Override
	public void jump(float second) {
		try {
			currentFrame.set((int) floor(second / durationInSeconds * totalFrames));
			frameGrab.seekToSecondSloppy(second);
		} catch (IOException | JCodecException e) {
			// nothing
		}
	}
	
	@Override
	public void dispose() {
		playThread.shutdownNow();
	}
	
	@Override
	public void volume(int i) {
	}
	
	@Override
	public void speed(float playSpeed) {
		/* nothing to do, jcodec is always reading at top speed */
	}
}
