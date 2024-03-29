package com.andrewringler.slitscan.ffmpeg;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.andrewringler.slitscan.VideoMeta;
import com.github.kokorin.jaffree.ffmpeg.Frame;
import com.github.kokorin.jaffree.ffmpeg.FrameConsumer;

import processing.core.PImage;

class ConsumeAllFrames implements FrameConsumer {
	AtomicLong trackCounter = new AtomicLong();
	AtomicLong frameCounter = new AtomicLong();
	VideoWrapperFFMPEG videoWrapperFFMPEG;
	
	ConsumeAllFrames(VideoWrapperFFMPEG videoWrapperFFMPEG) {
		this.videoWrapperFFMPEG = videoWrapperFFMPEG;
	}
	
	@Override
	public void consumeStreams(List<com.github.kokorin.jaffree.ffmpeg.Stream> tracks) {
		trackCounter.set(tracks.size());
	}
	
	@Override
	public void consume(Frame frame) {
		if (frame != null) {
			frameCounter.incrementAndGet();
			BufferedImage image = frame.getImage();
			VideoMeta videoMeta = new VideoMeta(this.videoWrapperFFMPEG.duration, this.videoWrapperFFMPEG.timeSeconds(), this.videoWrapperFFMPEG.widthDisplay(), this.videoWrapperFFMPEG.heightDisplay());
			this.videoWrapperFFMPEG.frameReady.processFrame(new com.andrewringler.slitscan.Frame(new PImage(image), videoMeta));
		}
		
	}
}