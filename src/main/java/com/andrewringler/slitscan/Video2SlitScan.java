package com.andrewringler.slitscan;

import static com.andrewringler.slitscan.vlcj.VLCJUtil.vlcInstallationFound;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;

import java.awt.Image;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.andrewringler.slitscan.UserInterface.PreviewMode;
import com.andrewringler.slitscan.jcodec.VideoWrapperJCodec;
import com.andrewringler.slitscan.vlcj.VideoWrapperVLCJ;

import processing.core.PApplet;
import processing.core.PImage;
import processing.opengl.PGraphics2D;
import processing.opengl.PJOGL;

public class Video2SlitScan extends PApplet {
	private static final boolean useJCodec = false;
	private static final Logger LOG = LoggerFactory.getLogger(Video2SlitScan.class);
	private static final String APP_NAME = "Video-2-Slit-Scan";
	private static final String[] APP_ICON_FILENAMES = { "icon-16.png", "icon-32.png", "icon-48.png", "icon-64.png", "icon-128.png", "icon-256.png", "icon-512.png" };
	
	VideoWrapper video;
	File videoFileName = null;
	File outputFile = null;
	PImage previewFrame;
	Position previewFrameOffsets = new Position(0, 0);
	boolean loadingFirstFrame = false;
	boolean isPausedGeneratingSlice = false;
	boolean doPause = false;
	boolean doResume = false;
	private float previewFrameTimecode = 0;
	int lastDrawUpdate = 0;
	int timeOfLastVideoFrameRead = 0;
	
	final FrameProcessor frameProcessor;
	
	// Slit generation
	boolean generatingSlitScanImage = false;
	boolean generatingSlicePause = false;
	SlitLocations slitLocations;
	
	private UserInterface ui;
	
	public Video2SlitScan() {
		// DO NOT PUT ANY PROCESSING CODE HERE!!
		LOG.info("video-2-slit-scan launched");
		
		frameProcessor = new FrameProcessor();
		
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
	
	class FrameReadyProcess implements FrameReady {
		@Override
		public void processFrame(Frame frame) {
			timeOfLastVideoFrameRead = millis();
			
			doProcessPreviewFrame(frame);
			
			if (loadingFirstFrame) {
				loadingFirstFrame = false;
				doPause = true;
				ui.setVideoInfo(video.duration(), frame.width, frame.height, 60);
				LOG.info("video is " + video.duration() + " seconds [" + frame.width + "x" + frame.height + " @ ?fps], preview frame is [" + previewFrame.width + "x" + previewFrame.height + "], " + frame.getColorDepth());
				return;
			}
			
			if (generatingSlitScanImage) {
				frameProcessor.doProcessFrame(Video2SlitScan.this, frame, video, slitLocations);
			}
		}
	}
	
	public void videoFileSelected(File selection) {
		if (selection == null) {
			LOG.info("video file selection: window was closed or user hit cancel");
		} else {
			videoFileName = selection;
			ui.videoFileSelected(videoFileName.getAbsolutePath());
			ui.updateProgress(0);
			ui.updatePlayhead(0);
			previewFrame = null;
			previewFrameTimecode = 0;
			generatingSlitScanImage = false;
			
			LOG.info("video file selection: loading video '" + videoFileName.getAbsolutePath() + "'");
			// reset keyframes
			slitLocations = new SlitLocations(this, ui, 0.5f);
			
			if (video != null) {
				// cleanup old video
				video.stop();
				video.dispose();
				video = null;
			}
			
			loadingFirstFrame = true;
			
			LOG.info("starting initial video play");
			if (useJCodec) {
				video = new VideoWrapperJCodec(videoFileName.getAbsolutePath(), new FrameReadyProcess(), true);
			} else {
				video = new VideoWrapperVLCJ(this, videoFileName.getAbsolutePath(), new FrameReadyProcess(), true, ui.getRotateVideo());
			}
		}
	}
	
	private void doProcessPreviewFrame(Frame frame) {
		if (ui.scrubbing() || loadingFirstFrame || ((millis() - lastDrawUpdate) > 150)) {
			//			float scalingFactor = video.width() > 640 ? (float) video.width() / 640f : 1f;
			float scalingFactor = Math.min(width / (float) frame.width, height / (float) frame.height);
			previewFrameTimecode = video.timeSeconds();
			/* skip preview frame generation for performance */
			if (!ui.previewMode().equals(PreviewMode.NONE)) {
				if (previewFrame == null) {
					int previewFrameWidth = (int) (frame.width * scalingFactor);
					int previewFrameHeight = (int) (frame.height * scalingFactor);
					LOG.info("Frame: " + frame.width + "x" + frame.height + " " + "preview: " + previewFrameWidth + "x" + previewFrameHeight);
					previewFrame = createImage(previewFrameWidth, previewFrameHeight, RGB);
				}
				updatePreviewFrame(frame);
			}
			lastDrawUpdate = millis();
		}
	}
	
	public void generateSlitScan() {
		if (!generatingSlitScanImage) {
			if (video != null && outputFile != null) {
				LOG.info("Starting generating slit scan");
				generatingSlitScanImage = true;
				timeOfLastVideoFrameRead = 0;
				ui.startGeneratingSlitScan();
				
				frameProcessor.reset(outputFile, ui.getSlitWidth(), ui.getImageWidth(), ui.getStartingPixel());
				
				video.speed(ui.getPlaySpeed());
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
		background(220, 242, 254);
		
		if (generatingSlitScanImage && timeOfLastVideoFrameRead > 0) {
			if (millis() - timeOfLastVideoFrameRead > 5000) {
				println("taking too long to read new frame, canceling video play");
				generatingSlitScanImage = false;
				doPause = true;
				frameProcessor.done();
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
				int xOffset = 0;
				if (ui.getVideoDrawWidth() < width) {
					xOffset = (int) Math.round((width - ui.getVideoDrawWidth()) / 2.0);
				}
				previewFrameOffsets = new Position(xOffset, yOffset);
				image(previewFrame, xOffset, yOffset, ui.getVideoDrawWidth(), ui.getVideoDrawHeight());
			} else if (ui.previewModeSlit()) {
				float positionInVideo = previewFrameTimecode / video.duration();
				float slitLocationNormalized = slitLocations.getSlitLocationNormalized(positionInVideo);
				int slitLocationInPreviewFrame = (int) round(previewFrame.width * slitLocationNormalized);
				int slitLocationOnVideoDrawSize = (int) round(ui.getVideoDrawWidth() * slitLocationNormalized);
				
				copy(previewFrame, slitLocationInPreviewFrame, 0, ui.getSlitWidth(), previewFrame.height, slitLocationOnVideoDrawSize, 0, ui.getSlitWidth(), (int) ui.getVideoDrawHeight());
			}
			
			float positionInVideo = previewFrameTimecode / video.duration();
			slitLocations.draw(positionInVideo);
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
		
		if (generatingSlitScanImage) {
			ui.updateProgress(frameProcessor.getProgress() * 100f);
			ui.updatePlayhead(previewFrameTimecode);
			if (frameProcessor.isDone()) {
				generatingSlitScanImage = false;
				ui.doneGeneratingSlitScan();
				
				/* pick a new file so we don't overwrite it */
				setNewOutputFile();
			}
		} else if (ui.scrubbing() && video != null && previewFrame != null) {
			float doScrubDelta = 0.5f;
			if (abs(ui.getVideoPlayhead() - previewFrameTimecode) > doScrubDelta) {
				LOG.info("scrubbing");
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
	
	private void setNewOutputFile() {
		// on a Mac?
		String home = System.getProperty("user.home");
		if (new File(home + "/Desktop").exists()) {
			outputFile = new File(home + "/Desktop/" + year() + "-" + month() + "-" + day() + "_" + hour() + "-" + minute() + "-" + second() + "-slit-scan.tif");
			ui.outputFileSelected(outputFile.getAbsolutePath());
		}
	}
	
	private void updatePreviewFrame(Frame frame) {
		previewFrame.copy(frame.getPImage(), 0, 0, frame.width, frame.height, 0, 0, previewFrame.width, previewFrame.height);
	}
	
	private void cleanup() {
		LOG.info("video-2-slit-scan quiting");
		
		generatingSlitScanImage = false;
		if (video != null) {
			video.stop();
		}
		video = null;
		frameProcessor.cleanup();
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
		
		if (vlcInstallationFound()) {
			// https://processing.org/tutorials/eclipse/
			PApplet.main(new String[] { "--bgcolor=#000000", Video2SlitScan.class.getCanonicalName() });
		} else {
			LOG.error("Could not find any installation of VLC");
			showMessageDialog(null, "Could not find any installation of VLC.\nPlease install the VLC Player (https://www.videolan.org/)\nin the default location, or set the\nenvironment variable VLC_PLUGIN_PATH to the location\nof your VLC installation.", "VLC Missing", ERROR_MESSAGE);
		}
	}
}
