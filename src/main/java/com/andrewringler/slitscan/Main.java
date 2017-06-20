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
	
	String videoFileName = "screengrab1.mov";
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
		video = new Movie(this, videoFileName);
		
		video.speed(1000); // something fast
		video.noLoop();
		video.play();
	}
	
	public void draw() {
		if (outputXOffsetNext >= totalVideoFrames) {
			exit();
		}
		
		image(video, 0, 0, width, height);
	}
	
	public void movieEvent(Movie m) {
		m.read(); // read current frame
		
		if (slit == null) {
			// estimate total frames
			totalVideoFrames = (int) Math.floor(video.duration() * 60.0);
			slit = createImage(1, video.height, RGB);
			
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
	
	public static void main(String args[]) {
		// https://processing.org/tutorials/eclipse/
		PApplet.main(new String[] { "--bgcolor=#000000", Main.class.getCanonicalName() });
	}
}
