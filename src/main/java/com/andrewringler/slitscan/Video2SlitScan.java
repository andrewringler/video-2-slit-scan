package com.andrewringler.slitscan;

import static com.andrewringler.slitscan.StreamingImageTools.createBlankImage;

import java.awt.Image;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.swing.ImageIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.andrewringler.slitscan.UserInterface.PreviewMode;

import io.scif.SCIFIO;
import io.scif.media.imageioimpl.plugins.tiff.TIFFImageReaderSpi;
import io.scif.media.imageioimpl.plugins.tiff.TIFFImageWriterSpi;
import processing.core.PApplet;
import processing.core.PImage;
import processing.opengl.PGraphics2D;
import processing.opengl.PJOGL;
import processing.video.Movie;

public class Video2SlitScan extends PApplet {
	private static final Logger LOG = LoggerFactory.getLogger(Video2SlitScan.class);
	private static final String APP_NAME = "Video-2-Slit-Scan";
	private static final String[] APP_ICON_FILENAMES = { "icon-16.png", "icon-32.png", "icon-48.png", "icon-64.png", "icon-128.png", "icon-256.png", "icon-512.png" };
	
	// Video
	//	PVideoWrapper video;
	//	Movie video;
	VideoWrapperFrameGrab video;
	File videoFileName = null;
	File outputFile = null;
	PImage previewFrame;
	int lastDrawUpdate = 0;
	int videoFrameCount = 0;
	int lastProcessedVideoFrame = 0;
	int droppedFrames = 0;
	boolean loadingFirstFrame = false;
	boolean isPausedGeneratingSlice = false;
	boolean doPause = false;
	boolean doResume = false;
	private float previewFrameTimecode = 0;
	long timeOfLastVideoFrameRead = 0;
	
	// Slit generation
	boolean generatingSlitScanImage = false;
	boolean generatingSlicePause = false;
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
		LOG.info("video-2-slit-scan launched");
		// register SCIFIO TIFF readers and writers for use with Java ImageIO
		scifio = new SCIFIO();
		IIORegistry.getDefaultInstance().registerServiceProvider(new TIFFImageWriterSpi());
		IIORegistry.getDefaultInstance().registerServiceProvider(new TIFFImageReaderSpi());
		ImageIO.scanForPlugins();
		
		// Set App icons for Mac
		// 16, 32, 48, 64, 128, 256, 512
		// PSurfaceJOGL.java loads these in order
		PJOGL.setIcon(APP_ICON_FILENAMES);
	}
	
	public void settings() {
		size(1200, 750, PGraphics2D.class.getCanonicalName());
	}
	
	public void setup() {
		// Add Windows Icons
		// task-switcher, titlebar
		List<Image> appIconImages = new ArrayList<Image>();
		for (String appIconFilename : APP_ICON_FILENAMES) {
			appIconImages.add(new ImageIcon(loadBytes(appIconFilename)).getImage());
		}
		frame.setIconImages(appIconImages);
		
		// TODO still need to deal with moving scrubber
		// if we want to allow resizing window
		//surface.setResizable(true);
		background(0);
		
		surface.setTitle(APP_NAME);
		
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
			LOG.info("output file selection dialog: windows was closed or user hit cancel");
		} else {
			if (selection.getAbsolutePath().endsWith(".tif")) {
				outputFile = selection;
			} else {
				LOG.info("output file selection dialog: invalid file selected");
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
			LOG.info("video file selection: window was closed or user hit cancel");
		} else {
			videoFileName = selection;
			ui.videoFileSelected(videoFileName.getAbsolutePath());
			
			LOG.info("video file selection: loading video '" + videoFileName.getAbsolutePath() + "'");
			// reset keyframes
			slitLocations = new SlitLocations(this, ui, 0.5f);
			
			if (video != null) {
				// cleanup old video
				video.stop();
				video.dispose();
				video = null;
			}
			//			video = new PVideoWrapper(new Movie(this, videoFileName.getAbsolutePath()));
			video = new VideoWrapperFrameGrab(this, videoFileName.getAbsolutePath());
			//			video = new PVideoWrapper(new Movie(this, videoFileName.getAbsolutePath()));
			videoFrameCount = 0;
			lastProcessedVideoFrame = 0;
			loadingFirstFrame = true;
			if (video != null) {
				video.volume(0);
				video.play();
			}
		}
	}
	
	public void generateSlitScan() {
		if (!generatingSlitScanImage) {
			if (video != null && outputFile != null) {
				generatingSlitScanImage = true;
				droppedFrames = 0;
				ui.startGeneratingSlitScan();
				initSlit = true;
				
				//				video.speed(ui.getPlaySpeed());
				video.jump(0);
				video.play();
				previewFrameTimecode = 0;
			} else {
				LOG.info("cannot generate slit scan: no video loaded");
			}
		}
	}
	
	public void doPauseGeneratingSlice() {
		isPausedGeneratingSlice = true;
		doPause = true;
	}
	
	public void doResumeGeneratingSlice() {
		isPausedGeneratingSlice = false;
		doResume = true;
	}
	
	public boolean isPausedGeneratingSlice() {
		return isPausedGeneratingSlice;
	}
	
	public void draw() {
		//		doProcessFrame();
		background(100);
		
		if (generatingSlitScanImage && tiffUpdater != null && timeOfLastVideoFrameRead > 0) {
			if (millis() - timeOfLastVideoFrameRead > 5000) {
				println("taking too long to read new frame, canceling video play");
				tiffUpdater.cancel();
			}
		}
		
		if (previewFrame != null && video != null) {
			ui.updateVideoDrawDimesions(previewFrame.width, previewFrame.height, video.width(), video.height());
			
			if (ui.previewModeFrame()) {
				// fit video in our window
				int yOffset = 0;
				if (ui.getVideoDrawHeight() < height) {
					yOffset = (int) Math.round((height - ui.getVideoDrawHeight()) / 2.0);
				}
				image(previewFrame, 0, yOffset, ui.getVideoDrawWidth(), ui.getVideoDrawHeight());
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
			//			strokeWeight(1);
			//			stroke(100, 100, 100);
			//			noFill();
			//			rect(0, 0, ui.getVideoDrawWidth() + 1, ui.getVideoDrawHeight() + 1);
		}
		
		if (doPause && video != null) {
			video.pause();
			doPause = false;
		}
		
		if (doResume && video != null && generatingSlitScanImage) {
			// resume after pause
			video.play();
			doResume = true;
		}
		
		if (generatingSlitScanImage && !initSlit && tiffUpdater != null) {
			ui.updateProgress(tiffUpdater.getProgress() * 100f);
			ui.updatePlayhead(previewFrameTimecode);
			if (tiffUpdater.isDone()) {
				generatingSlitScanImage = false;
				ui.doneGeneratingSlitScan();
				
				/* pick a new file so we don't overwrite it */
				setNewOutputFile();
			}
		} else if (ui.scrubbing() && video != null && previewFrame != null) {
			//			float doScrubDelta = video.duration() < 2.0 ? 0.001f : 0.1f;
			float doScrubDelta = 0.5f;
			if (abs(ui.getVideoPlayhead() - previewFrameTimecode) > doScrubDelta) {
				println("scrubbing");
				if (ui.getVideoPlayhead() == video.duration()) {
					video.jump(video.duration() - doScrubDelta);
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
		video.read();
		videoFrameCount++;
		doProcessFrame();
	}
	
	protected void doProcessFrame() {
		if (videoFrameCount == lastProcessedVideoFrame) {
			return;
		}
		if (generatingSlitScanImage && videoFrameCount > (lastProcessedVideoFrame + 1)) {
			droppedFrames++;
			println("dropped frames: " + droppedFrames);
		}
		lastProcessedVideoFrame = videoFrameCount;
		
		if (videoFrameCount == 2) {
			video.get().save(sketchPath() + "-frame.png");
		}
		
		// load current frame
		timeOfLastVideoFrameRead = millis();
		
		if (ui.scrubbing() || loadingFirstFrame || ((millis() - lastDrawUpdate) > 150)) {
			//			float scalingFactor = video.width() > 640 ? (float) video.width() / 640f : 1f;
			float scalingFactor = Math.min(width / (float) video.width(), height / (float) video.height());
			previewFrameTimecode = video.time();
			/* skip preview frame generation for performance */
			if (!ui.previewMode().equals(PreviewMode.NONE)) {
				//				if(previewFrame == null) {
				if (previewFrame == null) {
					previewFrame = createImage((int) (video.width() * scalingFactor), (int) (video.height() * scalingFactor), RGB);
				}
				updatePreviewFrame();
			}
			lastDrawUpdate = millis();
		}
		
		if (loadingFirstFrame) {
			loadingFirstFrame = false;
			doPause = true;
			ui.setVideoInfo(video.duration(), video.width(), video.height(), 60);
			LOG.info("video is " + video.duration() + " seconds [" + video.width() + "x" + video.height() + " @ ?fps], preview frame is [" + previewFrame.width + "x" + previewFrame.height, "]");
		}
		
		if (initSlit) {
			initSlit = false;
			
			int imageWidth = ui.getImageWidth();
			
			// if output file does not exist, populate with blank image
			if (!outputFile.exists()) {
				createBlankImage(scifio, outputFile.getAbsolutePath(), imageWidth, video.height());
			}
			
			// cancel any previous rendering
			if (tiffUpdater != null) {
				tiffUpdater.cancel();
			}
			tiffUpdater = new UpdateTiffOnDisk(this, slitQueue, ui.getStartingPixel(), outputFile.getAbsolutePath(), imageWidth, ui.getSlitWidth(), video.height());
			ScheduledFuture<?> renderedSlitsFuture = fileWritingExecutor.scheduleWithFixedDelay(tiffUpdater, 2, 5, TimeUnit.SECONDS);
			tiffUpdater.setFuture(renderedSlitsFuture);
		}
		
		if (generatingSlitScanImage) {
			// grab a slit from the middle of the current video frame
			PImage slit = createImage(tiffUpdater.getSlitWidth(), video.height(), RGB);
			float positionInVideo = video.time() / video.duration();
			int slitX = (int) round(video.width() * slitLocations.getSlitLocationNormalized(positionInVideo));
			/* check if slit is too close to the edge */
			if (slitX + slit.width > video.width()) {
				slitX = video.width() - slit.width;
			}
			slit.copy(video.get(), slitX, 0, slit.width, video.height(), 0, 0, slit.width, slit.height);
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
		//		PImage frame = createImage(previewFrame.width, previewFrame.height, RGB);
		previewFrame.copy(video.get(), 0, 0, video.width(), video.height(), 0, 0, previewFrame.width, previewFrame.height);
		//		previewFrame = frame;
		previewFrame.get().save(sketchPath() + "/frame.png");
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
		LOG.info("video-2-slit-scan quiting");
		
		generatingSlitScanImage = false;
		scifio.getContext().dispose();
		if (video != null) {
			video.stop();
		}
		video = null;
		fileWritingExecutor.shutdown();
	}
	
	public void dispose() {
		cleanup();
	}
	
	// need to override exit() method when using Processing from eclipse
	// https://forum.processing.org/two/discussion/22292/solved-javaw-process-wont-close-after-the-exit-of-my-program-eclipse
	public void exit() {
		println("exit");
		cleanup();
		
		noLoop();
		
		// Perform any code you like but some libraries like minim 
		//need to be stopped manually. If so do that here.
		
		// Now call the overridden method. Since the sketch is no longer looping
		// it will call System.exit(0); for you
		super.exit();
	}
	
	public static void main(String args[]) {
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("com.apple.mrj.application.apple.menu.about.name", APP_NAME);
		System.setProperty("apple.awt.application.name", APP_NAME);
		
		// https://processing.org/tutorials/eclipse/
		PApplet.main(new String[] { "--bgcolor=#000000", Video2SlitScan.class.getCanonicalName() });
	}
}
