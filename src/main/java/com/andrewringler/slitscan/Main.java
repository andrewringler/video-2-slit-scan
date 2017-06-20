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
import processing.video.Movie;

public class Main extends PApplet {
	private final SCIFIO scifio;
	
	private Movie video;
	private int totalVideoFrames = 0;
	boolean framesCounted = false;
	boolean blankImageCreated = false;
	int outputXOffsetNext = 0;
	
	String videoFileName = "screengrab1.mov";
	String outputFileName = "output.tif";
	
	public Main() {
		// register SCIFIO TIFF readers and writers for use with Java ImageIO
		scifio = new SCIFIO();
		IIORegistry.getDefaultInstance().registerServiceProvider(new TIFFImageWriterSpi());
		IIORegistry.getDefaultInstance().registerServiceProvider(new TIFFImageReaderSpi());
		ImageIO.scanForPlugins();
	}
	
	public void settings() {
		size(100, 100, "processing.opengl.PGraphics3D");
	}
	
	public void setup() {
		size(100, 100);
		
		video = new Movie(this, videoFileName);
		video.speed(1000); // something fast
		video.noLoop();
		video.play();
	}
	
	public void draw() {
		if (!framesCounted && video.time() >= video.duration()) {
			framesCounted = true;
			video.stop();
			println(" counted video frames: " + totalVideoFrames);
		}
		
		if (framesCounted && !blankImageCreated) {
			StreamingImageTools.createBlankImage(scifio, outputFileName, totalVideoFrames, video.height);
			blankImageCreated = true;
			
			// restart the video
			video.jump(0);
			video.play();
		}
		
		if (blankImageCreated && outputXOffsetNext >= totalVideoFrames) {
			exit();
		}
	}
	
	public void movieEvent(Movie m) {
		m.read();
		if (!framesCounted) {
			totalVideoFrames++;
		}
		
		// append slit to blank image
		if (blankImageCreated) {
			// grab slit from video image
			PImage slit = createImage(1, video.height, RGB);
			// grab a slit from the exact middle of the current video frame 
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
					outputXOffsetNext += slit.width;
					imageWriter.replacePixels((BufferedImage) slit.getNative(), param);
				}
				
			} catch (IOException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
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
