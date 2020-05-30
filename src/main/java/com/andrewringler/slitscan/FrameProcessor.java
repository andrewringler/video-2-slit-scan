package com.andrewringler.slitscan;

import static com.andrewringler.slitscan.StreamingImageTools.createBlankImage;
import static processing.core.PApplet.round;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;

import io.scif.SCIFIO;
import io.scif.media.imageioimpl.plugins.tiff.TIFFImageReaderSpi;
import io.scif.media.imageioimpl.plugins.tiff.TIFFImageWriterSpi;
import processing.core.PApplet;

public class FrameProcessor {
	ScheduledExecutorService fileWritingExecutor = Executors.newScheduledThreadPool(1);
	
	int videoFrameCount = 0;
	boolean initSlit = true;
	private UpdateTiffOnDisk tiffUpdater;
	
	final SCIFIO scifio;
	File outputFile;
	int slitWidth;
	int imageWidth;
	int startingPixel;
	final LinkedBlockingQueue<Slit> slitQueue = new LinkedBlockingQueue<Slit>();
	
	public FrameProcessor() {
		// register SCIFIO TIFF readers and writers for use with Java ImageIO
		scifio = new SCIFIO();
		IIORegistry.getDefaultInstance().registerServiceProvider(new TIFFImageWriterSpi());
		IIORegistry.getDefaultInstance().registerServiceProvider(new TIFFImageReaderSpi());
		ImageIO.scanForPlugins();
	}
	
	public void reset(File outputFile, int slitWidth, int imageWidth, int startingPixel) {
		this.outputFile = outputFile;
		this.slitWidth = slitWidth;
		this.imageWidth = imageWidth;
		this.startingPixel = startingPixel;
		
		// cancel any previous rendering
		if (tiffUpdater != null) {
			tiffUpdater.cancel();
		}
		tiffUpdater = null;
		slitQueue.clear();
		videoFrameCount = 0;
		
		initSlit = true;
	}
	
	public void doProcessFrame(PApplet p, Frame frame, VideoMeta video, SlitLocations slitLocations) {
		if (initSlit) {
			initSlit = false;
			
			// if output file does not exist, populate with blank image
			if (!outputFile.exists()) {
				createBlankImage(scifio, outputFile.getAbsolutePath(), imageWidth, frame.height);
			}
			
			// cancel any previous rendering
			if (tiffUpdater != null) {
				tiffUpdater.cancel();
			}
			tiffUpdater = new UpdateTiffOnDisk(p, slitQueue, startingPixel, outputFile.getAbsolutePath(), imageWidth, slitWidth, frame.height);
			ScheduledFuture<?> renderedSlitsFuture = fileWritingExecutor.scheduleWithFixedDelay(tiffUpdater, 2, 5, TimeUnit.SECONDS);
			tiffUpdater.setFuture(renderedSlitsFuture);
		}
		
		// grab a slit from the middle of the current video frame
		int slitWidth = tiffUpdater.getSlitWidth();
		int slitHeight = frame.height;
		float positionInVideo = video.timeSeconds() / video.duration();
		int slitX = (int) round(frame.width * slitLocations.getSlitLocationNormalized(positionInVideo));
		/* check if slit is too close to the edge */
		if (slitX + slitWidth > frame.width) {
			slitX = frame.width - slitWidth;
		}
		Slit slit = new Slit(slitWidth, slitHeight, slitX, frame);
		slitQueue.add(slit);
		//	LOG.debug("Q: " + video.time() + "/" + video.duration() + " queue size: " + slitQueue.size());
	}
	
	public void done() {
		if (tiffUpdater != null) {
			tiffUpdater.cancel();
		}
	}
	
	public float getProgress() {
		if (tiffUpdater != null && !initSlit) {
			return tiffUpdater.getProgress();
		}
		return 0;
	}
	
	public boolean isDone() {
		if (tiffUpdater != null) {
			return tiffUpdater.isDone();
		}
		return false;
	}
	
	public void cleanup() {
		if (tiffUpdater != null) {
			tiffUpdater.cancel();
		}
		scifio.getContext().dispose();
		fileWritingExecutor.shutdown();
	}
	
}
