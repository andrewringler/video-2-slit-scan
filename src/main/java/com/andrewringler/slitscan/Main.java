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

import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import controlP5.Textfield;
import io.scif.SCIFIO;
import io.scif.media.imageioimpl.plugins.tiff.TIFFImageReaderSpi;
import io.scif.media.imageioimpl.plugins.tiff.TIFFImageWriterSpi;
import processing.core.PApplet;
import processing.core.PImage;
import processing.opengl.PGraphics2D;
import processing.video.Movie;

public class Main extends PApplet {
	// Video
	private Movie video;
	File videoFileName = null;
	String outputFileName = "output.tif";
	PImage previewFrame;
	int lastDrawUpdate = 0;
	
	// Slit generation
	int SLIT_WIDTH = 1;
	PImage slit;
	boolean generatingSlitScanImage = false;
	boolean initSlit = false;
	private int totalVideoFrames = 0;
	int outputXOffsetNext = 0;
	private final SCIFIO scifio;
	float SLIT_LOCATION = 0.5f; // [0-1]
	
	// UI Controls
	ControlP5 cp5;
	private Textfield videoFileLabel;
	int mouseClickedLocationX = -1;
	private boolean draggingSlit;
	
	public Main() {
		// DO NOT PUT ANY PROCESSING CODE HERE!!
		
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
		background(0);
		previewFrame = createImage(width, height, RGB);
		
		cp5 = new ControlP5(this);
		cp5.addButton("selectVideoFile").setLabel("Select video file").setPosition(100, 100).setSize(100, 19).onClick(new CallbackListener() {
			@Override
			public void controlEvent(CallbackEvent arg0) {
				selectInput("Select a video to process:", "videoFileSelected");
			}
		});
		
		videoFileLabel = cp5.addTextfield("videFileTextField").setLabel("Video File").setPosition(220, 100).setSize(300, 19).setAutoClear(false);
		
		cp5.addButton("Generate slit-scan image").setPosition(100, 120).setSize(100, 19).onClick(new CallbackListener() {
			@Override
			public void controlEvent(CallbackEvent arg0) {
				if (!generatingSlitScanImage) {
					if (video != null) {
						println("starting slit-scan image generation");
						generatingSlitScanImage = true;
						initSlit = true;
						video.jump(0);
						video.play();
					} else {
						println("no video loaded!");
					}
				}
			}
		});
	}
	
	public void videoFileSelected(File selection) {
		if (selection == null) {
			println("Window was closed or the user hit cancel.");
		} else {
			videoFileName = selection;
			println("user selected " + videoFileName.getAbsolutePath());
			videoFileLabel.setValue(videoFileName.getAbsolutePath());
			
			println("setting up video " + videoFileName.getAbsolutePath());
			video = new Movie(this, videoFileName.getAbsolutePath());
			
			if (video != null) {
				video.volume(0);
				video.play();
				video.pause();
				
				if (video.available()) {
					video.read();
					updatePreviewFrame();
				}
			} else {
				println("Unable to load video file: " + videoFileName.getAbsolutePath());
				exit();
			}
			
		}
	}
	
	public void draw() {
		image(previewFrame, 0, 0, width, height);
		
		// draw slit location
		noFill();
		strokeWeight(1);
		stroke(255);
		patternLine((int) (SLIT_LOCATION * width), 0, (int) (SLIT_LOCATION * width), height, 0x0300, 1);
		stroke(0);
		patternLine((int) (SLIT_LOCATION * width), 0, (int) (SLIT_LOCATION * width), height, 0x3000, 1);
		
		if (draggingSlit) {
			stroke(255, 255, 0);
			patternLine((int) (SLIT_LOCATION * width), 0, (int) (SLIT_LOCATION * width), height, 0x0300, 1);
			stroke(0, 255, 0);
			patternLine((int) (SLIT_LOCATION * width), 0, (int) (SLIT_LOCATION * width), height, 0x3000, 1);
		}
	}
	
	public void mouseDragged() {
		if (draggingSlit) {
			SLIT_LOCATION = (float) mouseX / width;
		}
	}
	
	public void mousePressed() {
		int slitLocationX = (int) (SLIT_LOCATION * width);
		if (mouseX < slitLocationX + 5 && mouseX > slitLocationX - 5) {
			draggingSlit = true;
		}
	}
	
	public void mouseReleased() {
		draggingSlit = false;
	}
	
	public void movieEvent(Movie m) {
		video.read(); // load current frame
		
		if (initSlit) {
			outputXOffsetNext = 0;
			slit = createImage(SLIT_WIDTH, video.height, RGB);
			totalVideoFrames = (int) (video.duration() * 30.0);
			initSlit = false;
		}
		
		if (generatingSlitScanImage) {
			// grab a slit from the middle of the current video frame
			slit.copy(video, (int) round(video.width * SLIT_LOCATION), 0, 1, video.height, 0, 0, slit.width, slit.height);
			
			if (outputXOffsetNext < totalVideoFrames) {
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
				println("processed frame: ", outputXOffsetNext, " / ", totalVideoFrames, " video @", video.time(), "/", video.duration());
				
				if ((millis() - lastDrawUpdate) > 16) {
					updatePreviewFrame();
					lastDrawUpdate = millis();
				}
			} else {
				println("done ", outputXOffsetNext, " / ", totalVideoFrames);
				generatingSlitScanImage = false;
			}
		}
	}
	
	private void updatePreviewFrame() {
		PImage frame = createImage(width, height, RGB);
		frame.copy(video, 0, 0, video.width, video.height, 0, 0, previewFrame.width, previewFrame.height);
		previewFrame = frame;
	}
	
	public void dispose() {
		println("dispose");
		generatingSlitScanImage = false;
		scifio.getContext().dispose();
	}
	
	/*
	 * based on Bresenham's algorithm from wikipedia
	 * http://en.wikipedia.org/wiki/Bresenham's_line_algorithm
	 * https://processing.org/discourse/beta/num_1202486379.html
	 * Some examples:
	 * linePattern = 0x5555 will be a dotted line, alternating one drawn and one skipped pixel.
	 * linePattern = 0x0F0F will be medium sized dashes.
	 * linePattern = 0xFF00 will be large dashes.
	 */
	void patternLine(int xStart, int yStart, int xEnd, int yEnd, int linePattern, int lineScale) {
		int temp, yStep, x, y;
		int pattern = linePattern;
		int carry;
		int count = lineScale;
		
		boolean steep = (abs(yEnd - yStart) > abs(xEnd - xStart));
		if (steep == true) {
			temp = xStart;
			xStart = yStart;
			yStart = temp;
			temp = xEnd;
			xEnd = yEnd;
			yEnd = temp;
		}
		if (xStart > xEnd) {
			temp = xStart;
			xStart = xEnd;
			xEnd = temp;
			temp = yStart;
			yStart = yEnd;
			yEnd = temp;
		}
		int deltaX = xEnd - xStart;
		int deltaY = abs(yEnd - yStart);
		int error = -(deltaX + 1) / 2;
		
		y = yStart;
		if (yStart < yEnd) {
			yStep = 1;
		} else {
			yStep = -1;
		}
		for (x = xStart; x <= xEnd; x++) {
			if ((pattern & 1) == 1) {
				if (steep == true) {
					point(y, x);
				} else {
					point(x, y);
				}
				carry = 0x8000;
			} else {
				carry = 0;
			}
			count--;
			if (count <= 0) {
				pattern = (pattern >> 1) + carry;
				count = lineScale;
			}
			
			error += deltaY;
			if (error >= 0) {
				y += yStep;
				error -= deltaX;
			}
		}
	}
	
	// need to override exit() method when using Processing from eclipse
	// https://forum.processing.org/two/discussion/22292/solved-javaw-process-wont-close-after-the-exit-of-my-program-eclipse
	public void exit() {
		println("exit");
		generatingSlitScanImage = false;
		scifio.getContext().dispose();
		if (video != null) {
			video.dispose();
		}
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
