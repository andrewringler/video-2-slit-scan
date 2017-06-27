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

public class UpdateTiffOnDisk implements Runnable {
	private final LinkedBlockingQueue<BufferedImage> slitQueue;
	private final int totalVideoFrames;
	private final String outputFileName;
	private final int slitWidth;
	private final int slitHeight;
	
	private int outputXOffsetNext = 0;
	private boolean done = false;
	private ScheduledFuture<?> renderedSlitsFuture;
	
	public UpdateTiffOnDisk(LinkedBlockingQueue<BufferedImage> slitQueue, int totalVideoFrames, String outputFileName, int slitWidth, int slitHeight) {
		this.slitQueue = slitQueue;
		this.totalVideoFrames = totalVideoFrames;
		this.outputFileName = outputFileName;
		this.slitWidth = slitWidth;
		this.slitHeight = slitHeight;
	}
	
	@Override
	public void run() {
		BufferedImage slit;
		
		while (!done && (slit = slitQueue.poll()) != null) {
			if (outputXOffsetNext < totalVideoFrames) {
				// write current slit to disk
				File outputFile = new File(outputFileName);
				try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(outputFile)) {
					ImageReader imageReader = ImageIO.getImageReaders(imageInputStream).next();
					ImageWriter imageWriter = ImageIO.getImageWriter(imageReader);
					try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputFile)) {
						imageWriter.setOutput(imageOutputStream);
						imageWriter.prepareReplacePixels(0, new Rectangle(outputXOffsetNext, 0, 1, slitHeight));
						ImageWriteParam param = imageWriter.getDefaultWriteParam();
						param.setDestinationOffset(new Point(outputXOffsetNext, 0));
						imageWriter.replacePixels((BufferedImage) slit, param);
						outputXOffsetNext += slitWidth;
					}
				} catch (IOException e) {
					System.out.println("unable to write: " + e.getMessage());
					throw new IllegalStateException(e.getMessage(), e);
				}
				System.out.println("W: " + outputXOffsetNext + " / " + totalVideoFrames + " queue size: " + slitQueue.size());
				
			} else {
				System.out.println("Done: " + outputXOffsetNext + " / " + totalVideoFrames);
				done = true;
			}
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
}
