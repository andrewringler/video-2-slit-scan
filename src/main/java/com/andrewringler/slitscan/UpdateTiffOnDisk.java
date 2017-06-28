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
	private final int totalVideoFrames;
	private final PApplet p;
	
	private int outputXOffsetNext = 0;
	private boolean done = false;
	private ScheduledFuture<?> renderedSlitsFuture;
	
	public UpdateTiffOnDisk(PApplet p, LinkedBlockingQueue<PImage> slitQueue, int totalVideoFrames, String outputFileName, int slitWidth, int slitHeight) {
		this.p = p;
		this.slitQueue = slitQueue;
		this.totalVideoFrames = totalVideoFrames;
		this.outputFileWidth = totalVideoFrames * slitWidth;
		this.outputFileName = outputFileName;
		this.slitWidth = slitWidth;
		this.slitHeight = slitHeight;
	}
	
	@Override
	public void run() {
		if (done || slitQueue.isEmpty()) {
			return;
		}
		
		PImage slit;
		
		File outputFile = new File(outputFileName);
		
		int batchSize = slitQueue.size();
		PImage slitsBatchImage = p.createImage(batchSize * slitWidth, slitHeight, PConstants.RGB);
		
		for (int i = 0; i < batchSize; i++) {
			slit = slitQueue.poll();
			slitsBatchImage.copy(slit, 0, 0, slit.width, slit.height, i, 0, slit.width, slit.height);
		}
		
		if (outputXOffsetNext < (outputFileWidth - slitsBatchImage.width + 1)) {
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
	}
	
	public boolean isDone() {
		return done;
	}
	
	public void cancel() {
		if (renderedSlitsFuture != null) {
			done = false;
			renderedSlitsFuture.cancel(false);
		}
	}
	
	public void setFuture(ScheduledFuture<?> renderedSlitsFuture) {
		this.renderedSlitsFuture = renderedSlitsFuture;
	}
	
	public float getProgress() {
		return (float) outputXOffsetNext / (float) totalVideoFrames;
	}
}
