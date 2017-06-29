package com.andrewringler.slitscan;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;

public class UpdateTiffOnDisk implements Runnable {
	private final LinkedBlockingQueue<PImage> slitQueue;
	private final int outputFileWidth;
	private final String outputFileName;
	private final int slitWidth;
	private final int slitHeight;
	private final PApplet p;
	
	private int outputXOffsetNext = 0;
	private boolean done = false;
	private ScheduledFuture<?> renderedSlitsFuture;
	
	public UpdateTiffOnDisk(PApplet p, LinkedBlockingQueue<PImage> slitQueue, int startingPixel, String outputFileName, int outputFileWidth, int slitWidth, int slitHeight) {
		this.p = p;
		this.slitQueue = slitQueue;
		this.outputFileWidth = outputFileWidth;
		this.outputFileName = outputFileName;
		this.slitWidth = slitWidth;
		this.slitHeight = slitHeight;
		this.outputXOffsetNext = startingPixel;
	}
	
	@Override
	public void run() {
		if (done || slitQueue.isEmpty()) {
			return;
		}
		if (outputXOffsetNext + 1 > outputFileWidth) {
			done = true;
			return;
		}
		
		PImage slit;
		File outputFile = new File(outputFileName);
		int batchSize = slitQueue.size();
		int batchImagePixelWidth = batchSize * slitWidth;
		
		/*
		 * if the video has more frames than we have allocated for our output image
		 * then the queue might have more frames in it, than we can actually write out to disk
		 */
		int pixelsRemaining = outputFileWidth - outputXOffsetNext;
		if (batchImagePixelWidth > pixelsRemaining) {
			batchSize = pixelsRemaining / slitWidth; // integer division
			batchImagePixelWidth = batchSize * slitWidth;
		}
		
		PImage slitsBatchImage = p.createImage(batchImagePixelWidth, slitHeight, PConstants.RGB);
		
		for (int i = 0; i < batchSize; i++) {
			slit = slitQueue.poll();
			slitsBatchImage.copy(slit, 0, 0, slit.width, slit.height, i, 0, slit.width, slit.height);
		}
		
		try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(outputFile)) {
			ImageReader imageReader = ImageIO.getImageReaders(imageInputStream).next();
			ImageWriter imageWriter = ImageIO.getImageWriter(imageReader);
			try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputFile)) {
				imageWriter.setOutput(imageOutputStream);
				imageWriter.prepareReplacePixels(0, new Rectangle(outputXOffsetNext, 0, slitsBatchImage.width, slitsBatchImage.height));
				ImageWriteParam param = imageWriter.getDefaultWriteParam();
				param.setDestinationOffset(new Point(outputXOffsetNext, 0));
				imageWriter.replacePixels((BufferedImage) slitsBatchImage.getNative(), param);
				outputXOffsetNext += slitsBatchImage.width;
			}
		} catch (IOException e) {
			System.out.println("unable to write: " + e.getMessage());
			throw new IllegalStateException(e.getMessage(), e);
		}
		//			System.out.println("W: " + outputXOffsetNext + " / " + totalVideoFrames + " queue size: " + slitQueue.size());
	}
	
	public boolean isDone() {
		return done;
	}
	
	public void cancel() {
		if (renderedSlitsFuture != null) {
			done = true;
			renderedSlitsFuture.cancel(false);
		}
	}
	
	public void setFuture(ScheduledFuture<?> renderedSlitsFuture) {
		this.renderedSlitsFuture = renderedSlitsFuture;
	}
	
	public float getProgress() {
		if (outputXOffsetNext >= outputFileWidth || done) {
			return 1f;
		}
		return (float) outputXOffsetNext / (float) outputFileWidth;
	}
}
