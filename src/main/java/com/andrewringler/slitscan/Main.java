package com.andrewringler.slitscan;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.spi.IIORegistry;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import io.scif.SCIFIO;
import io.scif.media.imageioimpl.plugins.tiff.TIFFImageReaderSpi;
import io.scif.media.imageioimpl.plugins.tiff.TIFFImageWriterSpi;
import processing.core.PApplet;
import processing.core.PImage;
import processing.opengl.PGraphics2D;
import processing.video.Movie;

public class Main extends PApplet {
	private final SCIFIO scifio;
	
	private Movie video;
	private int totalVideoFrames = 0;
	int outputXOffsetNext = 0;
	
	//	String videoFileName = "screengrab1.mov";
	File videoFileName = null;
	String outputFileName = "output.tif";
	PImage slit;
	
	public Main() {
		// register SCIFIO TIFF readers and writers for use with Java ImageIO
		scifio = new SCIFIO();
		IIORegistry.getDefaultInstance().registerServiceProvider(new TIFFImageWriterSpi());
		IIORegistry.getDefaultInstance().registerServiceProvider(new TIFFImageReaderSpi());
		ImageIO.scanForPlugins();
	}
	
	public void settings() {
		size((int) (1920 * 0.5), (int) (1080 * 0.5), PGraphics2D.class.getCanonicalName());
	}
	
	public void setup() {
		selectInput("Select a video to process:", "videoFileSelected");
	}
	
	public void videoFileSelected(File selection) {
		if (selection == null) {
			println("Window was closed or the user hit cancel.");
		} else {
			videoFileName = selection;
			println("User selected " + videoFileName.getAbsolutePath());
			
			video = new Movie(this, videoFileName.getAbsolutePath());
			
			if (video != null) {
				video.speed(1000); // something fast
				video.noLoop();
				video.play();
			} else {
				println("Unable to load video file: " + videoFileName.getAbsolutePath());
				exit();
			}
		}
	}
	
	public void draw() {
		if (videoFileName == null) {
			return; // nothing to do yet
		}
		
		if (outputXOffsetNext >= totalVideoFrames) {
			exit();
		}
		
		if (video != null) {
			image(video, 0, 0, width, height);
		}
	}
	
	public void movieEvent(Movie m) {
		if (videoFileName == null) {
			return; // nothing to do yet
		}
		
		m.read(); // read current frame
		
		if (slit == null) {
			// estimate total frames
			totalVideoFrames = (int) Math.floor(video.duration() * 60.0);
			slit = createImage(1, video.height, RGB);
			
			println("Video is " + video.duration() + " seconds long, estimating " + totalVideoFrames + " total frames");
			
			// create large blank-ish image to hold our output
			StreamingImageTools.createBlankImage(scifio, outputFileName, totalVideoFrames, video.height);
		}
		
		if (outputXOffsetNext >= totalVideoFrames) {
			exit();
		}
		
		// grab a slit from the middle of the current video frame
		slit.copy(video, video.width / 2, 0, 1, video.height, 0, 0, slit.width, slit.height);
		
		// write current slit to disk
		File outputFile = new File(outputFileName);
		try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(outputFile)) {
			ImageReader imageReader = ImageIO.getImageReaders(imageInputStream).next();
			ImageWriter imageWriter = ImageIO.getImageWriter(imageReader);
			try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputFile)) {
				imageWriter.setOutput(imageOutputStream);
				imageWriter.prepareReplacePixels(0, new Rectangle(outputXOffsetNext, 0, 1, video.height));
				ImageWriteParam param = imageWriter.getDefaultWriteParam();
				param.setDestinationOffset(new Point(outputXOffsetNext, 0));
				imageWriter.replacePixels((BufferedImage) slit.getNative(), param);
				outputXOffsetNext += slit.width;
			}
			
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		
		if (outputXOffsetNext >= totalVideoFrames) {
			exit();
		}
	}
	
	public void dispose() {
		scifio.getContext().dispose();
	}
	
	// need to override exit() method when using Processing from eclipse
	// https://forum.processing.org/two/discussion/22292/solved-javaw-process-wont-close-after-the-exit-of-my-program-eclipse
	public void exit() {
		noLoop();
		
		// Perform any code you like but some libraries like minim 
		//need to be stopped manually. If so do that here.
		
		// Now call the overridden method. Since the sketch is no longer looping
		// it will call System.exit(0); for you
		super.exit();
	}
	
	public static void main(String args[]) {
		// https://processing.org/tutorials/eclipse/
		PApplet.main(new String[] { "--bgcolor=#000000", Main.class.getCanonicalName() });
	}
}
