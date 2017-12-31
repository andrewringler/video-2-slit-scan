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

import com.andrewringler.slitscan.UserInterface.PreviewMode;

import io.scif.SCIFIO;
import io.scif.media.imageioimpl.plugins.tiff.TIFFImageReaderSpi;
import io.scif.media.imageioimpl.plugins.tiff.TIFFImageWriterSpi;
import processing.core.PApplet;
import processing.core.PImage;
import processing.opengl.PGraphics2D;
import processing.video.Movie;

public class Video2SlitScan extends PApplet {
	// Video
	Movie video;
	File videoFileName = null;
	File outputFile = null;
	PImage previewFrame;
	int lastDrawUpdate = 0;
	boolean loadingFirstFrame = false;
	boolean doPause = false;
	private float previewFrameTimecode = 0;
	
	// Slit generation
	boolean generatingSlitScanImage = false;
	boolean initSlit = false;
	SlitLocations slitLocations;
	
	// file writing
	LinkedBlockingQueue<PImage> slitQueue = new LinkedBlockingQueue<PImage>();
	private final SCIFIO scifio;
	ScheduledExecutorService fileWritingExecutor = Executors.newScheduledThreadPool(1);
	private UpdateTiffOnDisk tiffUpdater;
	private UserInterface ui;
	
	public Video2SlitScan() {
		// DO NOT PUT ANY PROCESSING CODE HERE!!
		
		// register SCIFIO TIFF readers and writers for use with Java ImageIO
		scifio = new SCIFIO();
		IIORegistry.getDefaultInstance().registerServiceProvider(new TIFFImageWriterSpi());
		IIORegistry.getDefaultInstance().registerServiceProvider(new TIFFImageReaderSpi());
		ImageIO.scanForPlugins();
	}
	
	public void settings() {
		size(1200, 750, PGraphics2D.class.getCanonicalName());
	}
	
	public void setup() {
		// TODO still need to deal with moving scrubber
		// if we want to allow resizing window
		//surface.setResizable(true);
		background(0);
		
		ui = new UserInterface(this);
		slitLocations = new SlitLocations(this, ui, 0.5f);
		
		setNewOutputFile();
	}
	
	public void outputFileSelector() {
		if (outputFile != null) {
			selectOutput("Choose output file *.tif", "outputFileSelected", outputFile);
		} else {
			selectOutput("Choose output file *.tif", "outputFileSelected");
		}
	}
	
	public void outputFileSelected(File selection) {
		if (selection == null) {
			println("Window was closed or the user hit cancel.");
		} else {
			if (selection.getAbsolutePath().endsWith(".tif")) {
				outputFile = selection;
			} else {
				println("invalid file " + selection.getAbsolutePath());
				// not updating outputFile
			}
			ui.outputFileSelected(outputFile.getAbsolutePath());
		}
	}
	
	public void videoFileSelector() {
		selectInput("Select a video to process", "videoFileSelected");
	}
	
	public void videoFileSelected(File selection) {
		if (selection == null) {
			println("Window was closed or the user hit cancel.");
		} else {
			videoFileName = selection;
			println("user selected " + videoFileName.getAbsolutePath());
			ui.videoFileSelected(videoFileName.getAbsolutePath());
			
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
	
	public void generateSlitScan() {
		if (!generatingSlitScanImage) {
			if (video != null && outputFile != null) {
				generatingSlitScanImage = true;
				ui.startGeneratingSlitScan();
				initSlit = true;
				
				video.speed(ui.getPlaySpeed());
				video.jump(0);
				video.play();
			} else {
				println("no video loaded!");
			}
		}
	}
	
	public void draw() {
		background(0);
		
		if (previewFrame != null) {
			ui.updateVideoDrawDimesions(previewFrame.width, previewFrame.height, video.width, video.height);
			
			if (ui.previewModeFrame()) {
				// fit video in our window
				image(previewFrame, 0, 0, ui.getVideoDrawWidth(), ui.getVideoDrawHeight());
			} else if (ui.previewModeSlit()) {
				float positionInVideo = previewFrameTimecode / video.duration();
				float slitLocationNormalized = slitLocations.getSlitLocationNormalized(positionInVideo);
				int slitLocationInPreviewFrame = (int) round(previewFrame.width * slitLocationNormalized);
				int slitLocationOnVideoDrawSize = (int) round(ui.getVideoDrawWidth() * slitLocationNormalized);
				
				copy(previewFrame, slitLocationInPreviewFrame, 0, ui.getSlitWidth(), previewFrame.height, slitLocationOnVideoDrawSize, 0, ui.getSlitWidth(), (int) ui.getVideoDrawHeight());
			}
			
			float positionInVideo = previewFrameTimecode / video.duration();
			slitLocations.draw(positionInVideo);
			
			// Video frame border
			strokeWeight(1);
			stroke(100, 100, 100);
			noFill();
			rect(0, 0, ui.getVideoDrawWidth() + 1, ui.getVideoDrawHeight() + 1);
		}
		
		if (doPause && video != null) {
			video.pause();
			doPause = false;
		}
		
		if (generatingSlitScanImage) {
			ui.updateProgress(tiffUpdater.getProgress() * 100f);
			ui.updatePlayhead(previewFrameTimecode);
			if (tiffUpdater.isDone()) {
				generatingSlitScanImage = false;
				ui.doneGeneratingSlitScan();
				
				/* pick a new file so we don't overwrite it */
				setNewOutputFile();
			}
		} else if (ui.scrubbing() && video != null && previewFrame != null) {
			if (abs(ui.getVideoPlayhead() - previewFrameTimecode) > 0.1) {
				if (ui.getVideoPlayhead() == video.duration()) {
					video.jump(video.duration() - 0.1f);
				} else {
					video.jump(ui.getVideoPlayhead());
				}
				video.play();
			} else {
				ui.doneScrubbing();
				video.pause();
			}
		}
	}
	
	public void mouseDragged() {
		slitLocations.mouseDragged();
	}
	
	public void mousePressed() {
		slitLocations.mousePressed();
		ui.mousePressed();
	}
	
	public void mouseReleased() {
		slitLocations.mouseReleased();
		ui.mouseReleased();
	}
	
	public void keyPressed() {
		ui.keyPressed();
	}
	
	public void movieEvent(Movie m) {
		video.read(); // load current frame
		
		if (ui.scrubbing() || loadingFirstFrame || ((millis() - lastDrawUpdate) > 150)) {
			float scalingFactor = video.width > 640 ? (float) video.width / 640f : 1f;
			previewFrameTimecode = video.time();
			/* skip preview frame generation for performance */
			if (!ui.previewMode().equals(PreviewMode.NONE)) {
				previewFrame = createImage((int) (video.width / scalingFactor), (int) (video.height / scalingFactor), RGB);
				updatePreviewFrame();
			}
			lastDrawUpdate = millis();
		}
		
		if (loadingFirstFrame) {
			loadingFirstFrame = false;
			doPause = true;
			ui.setVideoDuration(video.duration());
			println("video is [", video.width, "x", video.height, "], preview frame is [", previewFrame.width, "x", previewFrame.height, "]");
		}
		
		if (initSlit) {
			initSlit = false;
			
			int imageWidth = ui.getImageWidth();
			
			// if output file does not exist, populate with blank image
			if (!outputFile.exists()) {
				createBlankImage(scifio, outputFile.getAbsolutePath(), imageWidth, video.height);
			}
			
			// cancel any previous rendering
			if (tiffUpdater != null) {
				tiffUpdater.cancel();
			}
			tiffUpdater = new UpdateTiffOnDisk(this, slitQueue, ui.getStartingPixel(), outputFile.getAbsolutePath(), imageWidth, ui.getSlitWidth(), video.height);
			ScheduledFuture<?> renderedSlitsFuture = fileWritingExecutor.scheduleWithFixedDelay(tiffUpdater, 2, 5, TimeUnit.SECONDS);
			tiffUpdater.setFuture(renderedSlitsFuture);
		}
		
		if (generatingSlitScanImage) {
			// grab a slit from the middle of the current video frame
			PImage slit = createImage(tiffUpdater.getSlitWidth(), video.height, RGB);
			float positionInVideo = video.time() / video.duration();
			int slitX = (int) round(video.width * slitLocations.getSlitLocationNormalized(positionInVideo));
			/* check if slit is too close to the edge */
			if (slitX + slit.width > video.width) {
				slitX = video.width - slit.width;
			}
			slit.copy(video, slitX, 0, slit.width, video.height, 0, 0, slit.width, slit.height);
			slitQueue.add(slit);
			//			System.out.println("Q: " + video.time() + "/" + video.duration() + " queue size: " + slitQueue.size());
		}
	}
	
	private void setNewOutputFile() {
		// on a Mac?
		String home = System.getProperty("user.home");
		if (new File(home + "/Desktop").exists()) {
			outputFile = new File(home + "/Desktop/" + year() + "-" + month() + "-" + day() + "_" + hour() + "-" + minute() + "-" + second() + "-slit-scan.tif");
			ui.outputFileSelected(outputFile.getAbsolutePath());
		}
	}
	
	private void updatePreviewFrame() {
		PImage frame = createImage(previewFrame.width, previewFrame.height, RGB);
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
	public void patternLine(int xStart, int yStart, int xEnd, int yEnd, int linePattern, int lineScale) {
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
		PApplet.main(new String[] { "--bgcolor=#000000", Video2SlitScan.class.getCanonicalName() });
	}
}
