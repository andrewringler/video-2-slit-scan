package com.andrewringler.slitscan;

import static com.andrewringler.slitscan.StreamingImageTools.createBlankImage;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;

import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import controlP5.Group;
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
	String outputFileName;
	PImage previewFrame;
	int lastDrawUpdate = 0;
	boolean loadingFirstFrame = false;
	boolean doPause = false;
	
	// Slit generation
	int SLIT_WIDTH = 1;
	boolean generatingSlitScanImage = false;
	boolean initSlit = false;
	float SLIT_LOCATION = 0.5f; // [0-1]
	
	// file writing
	LinkedBlockingQueue<PImage> slitQueue = new LinkedBlockingQueue<PImage>();
	private int totalVideoFrames = 0;
	private final SCIFIO scifio;
	ScheduledExecutorService fileWritingExecutor = Executors.newScheduledThreadPool(1);
	private UpdateTiffOnDisk tiffUpdater;
	
	// UI Controls
	ControlP5 cp5;
	private Textfield videoFileLabel;
	private Textfield videoFileDuration;
	private Textfield videoFrameCountField;
	private Textfield videoFPSField;
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
		outputFileName = year() + "-" + month() + "-" + day() + "_" + hour() + "-" + minute() + "-" + second() + "-slit-scan.tif";
		previewFrame = createImage(width, height, RGB);
		
		cp5 = new ControlP5(this);
		
		// Video load/setup
		Group videoSettingsUI = cp5.addGroup("Video").setPosition(100, 100).setBackgroundHeight(150).setWidth(400).setBackgroundColor(color(30, 30, 30, 220));
		cp5.addButton("selectVideoFile").setLabel("Open video file").setPosition(10, 10).setSize(100, 20).setGroup(videoSettingsUI).onClick(new CallbackListener() {
			@Override
			public void controlEvent(CallbackEvent arg0) {
				selectInput("Select a video to process:", "videoFileSelected");
			}
		});
		videoFileLabel = cp5.addTextfield("videoFileTextField").setLabel("Video File Path").setPosition(10, 40).setSize(350, 20).setUserInteraction(false).setGroup(videoSettingsUI);
		videoFileDuration = cp5.addTextfield("videoFileDuration").setLabel("Duration (s)").setValue("0").setInputFilter(ControlP5.FLOAT).setPosition(10, 80).setSize(60, 20).setUserInteraction(false).setGroup(videoSettingsUI);
		videoFrameCountField = cp5.addTextfield("videoFileFrameCount").setLabel("Total Frames").setValue("0").setInputFilter(ControlP5.INTEGER).setPosition(120, 80).setSize(60, 20).setUserInteraction(false).setGroup(videoSettingsUI);
		videoFPSField = cp5.addTextfield("videoFileFPS").setLabel("FPS").setInputFilter(ControlP5.FLOAT).setValue("30").setPosition(80, 80).setSize(30, 20).setAutoClear(false).setGroup(videoSettingsUI).onChange(new CallbackListener() {
			@Override
			public void controlEvent(CallbackEvent arg0) {
				if (videoFrameCountField != null && videoFPSField != null && videoFileDuration != null) {
					float fps = Float.valueOf(videoFPSField.getText());
					float duration = Float.valueOf(videoFileDuration.getText());
					videoFrameCountField.setValue(String.valueOf((int) (fps * duration)));
				}
			}
		});
		
		// Slit scan setup/run
		cp5.addButton("Generate slit-scan image").setPosition(10, 20).setSize(100, 19).onClick(new CallbackListener() {
			@Override
			public void controlEvent(CallbackEvent arg0) {
				if (!generatingSlitScanImage) {
					if (video != null) {
						generatingSlitScanImage = true;
						initSlit = true;
						
						video.jump(0);
						video.speed(1);
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
				loadingFirstFrame = true;
				video.volume(0);
				video.play();
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
		
		if (doPause && video != null) {
			video.pause();
			doPause = false;
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
		
		if (loadingFirstFrame || ((millis() - lastDrawUpdate) > 150)) {
			updatePreviewFrame();
			lastDrawUpdate = millis();
		}
		
		if (loadingFirstFrame) {
			loadingFirstFrame = false;
			doPause = true;
			videoFileDuration.setValue(String.valueOf(video.duration()));
			
			float fps = Float.valueOf(videoFPSField.getText());
			videoFrameCountField.setText(String.valueOf((int) (fps * video.duration())));
		}
		
		if (initSlit) {
			initSlit = false;
			
			totalVideoFrames = Integer.valueOf(videoFrameCountField.getText());
			
			createBlankImage(scifio, outputFileName, SLIT_WIDTH * totalVideoFrames, video.height);
			
			// cancel any previous rendering
			if (tiffUpdater != null) {
				tiffUpdater.cancel();
			}
			tiffUpdater = new UpdateTiffOnDisk(this, slitQueue, totalVideoFrames, outputFileName, SLIT_WIDTH, video.height);
			ScheduledFuture<?> renderedSlitsFuture = fileWritingExecutor.scheduleWithFixedDelay(tiffUpdater, 2, 5, TimeUnit.SECONDS);
			tiffUpdater.setFuture(renderedSlitsFuture);
		}
		
		if (generatingSlitScanImage) {
			// grab a slit from the middle of the current video frame
			PImage slit = createImage(SLIT_WIDTH, video.height, RGB);
			slit.copy(video, (int) round(video.width * SLIT_LOCATION), 0, SLIT_WIDTH, video.height, 0, 0, slit.width, slit.height);
			slitQueue.add(slit);
			System.out.println("Q: " + video.time() + "/" + video.duration() + " queue size: " + slitQueue.size());
		}
	}
	
	private void updatePreviewFrame() {
		PImage frame = createImage(width, height, RGB);
		frame.copy(video, 0, 0, video.width, video.height, 0, 0, previewFrame.width, previewFrame.height);
		previewFrame = frame;
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
	
	private void cleanup() {
		generatingSlitScanImage = false;
		scifio.getContext().dispose();
		if (video != null) {
			video.dispose();
		}
		fileWritingExecutor.shutdown();
	}
	
	public void dispose() {
		println("dispose");
		cleanup();
	}
	
	//	// need to override exit() method when using Processing from eclipse
	//	// https://forum.processing.org/two/discussion/22292/solved-javaw-process-wont-close-after-the-exit-of-my-program-eclipse
	//	public void exit() {
	//		println("exit");
	//		cleanup();
	//		
	//		noLoop();
	//		
	//		// Perform any code you like but some libraries like minim 
	//		//need to be stopped manually. If so do that here.
	//		
	//		// Now call the overridden method. Since the sketch is no longer looping
	//		// it will call System.exit(0); for you
	//		super.exit();
	//	}
	
	public static void main(String args[]) {
		// https://processing.org/tutorials/eclipse/
		PApplet.main(new String[] { "--bgcolor=#000000", Main.class.getCanonicalName() });
	}
}
