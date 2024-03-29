package com.andrewringler.slitscan;

import static com.andrewringler.slitscan.ImageFormat.PNG;
import static com.andrewringler.slitscan.ImageFormat.TIFF;
import static com.andrewringler.slitscan.RotateVideo.rotateVideo;

import java.util.List;

import controlP5.Button;
import controlP5.CColor;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlFont;
import controlP5.ControlP5;
import controlP5.ControllerInterface;
import controlP5.Group;
import controlP5.RadioButton;
import controlP5.Slider;
import controlP5.Textfield;
import processing.core.PConstants;
import processing.core.PFont;

public class UserInterface {
	private final ControlP5 cp5;
	private final Video2SlitScan p;
	private final Textfield videoFileLabel;
	private final Textfield videoFileDuration;
	private final Textfield videoFileWidth;
	private final Textfield videoFileHeight;
	private final Textfield videoFrameCountField;
	private final Textfield videoFPSField;
	private final Slider generationProgressSlider;
	private final RadioButton rotateVideo;
	private final RadioButton colorDepth;
	private final Textfield outputFileLabel;
	private final Textfield startPixelField;
	private final Textfield imageWidthField;
	private final Textfield imageHeightField;
	private final Textfield slitWidthField;
	private final Button selectVideoFile;
	private final Button selectOutputFile;
	private final Button generateSlitScanImageButton;
	private final Button pauseGenerateSlitScanImageButton;
	private final Slider videoScrubber;
	private final RadioButton slitSelectionMode;
	private final RadioButton imageSelectionMode;
	private final int videoScrubberWidth;
	private final int videoScrubberXOffset;
	
	private float videoDrawWidth = 0;
	private float videoDrawHeight = 0;
	private int videoWidth = 0;
	private boolean scrubbing = false;
	private int videoScrubberYOffset;
	public PFont robotoMono;
	private long lastUserScrubMillis;
	
	public UserInterface(Video2SlitScan p) {
		this.p = p;
		lastUserScrubMillis = p.millis();
		cp5 = new ControlP5(p);
		int COLOR_READONLY = p.color(80, 80, 80);
		
		robotoMono = p.createFont(p.dataFile("RobotoMono-Medium.ttf").getPath(), 10, true);
		ControlFont font = new ControlFont(robotoMono, 10);
		cp5.setFont(font);
		
		// Video load/setup
		Group videoSettingsUI = cp5.addGroup("Video").setMoveable(true).setPosition(10, 30).setBackgroundHeight(205).setWidth(400).setBackgroundColor(p.color(30, 30, 30, 240)).setBarHeight(20);
		selectVideoFile = cp5.addButton("selectVideoFile").setLabel("Open video file").setColorBackground(p.color(255, 255, 0)).setColorLabel(p.color(0)).setPosition(10, 10).setSize(120, 20).setGroup(videoSettingsUI).onClick(new CallbackListener() {
			@Override
			public void controlEvent(CallbackEvent arg0) {
				if (!p.generatingSlitScanImage) {
					p.videoFileSelector();
				}
			}
		});
		videoFileLabel = cp5.addTextfield("videoFileTextField").setLabel("Video File Path").setPosition(10, 40).setSize(350, 20).setUserInteraction(false).setColorForeground(COLOR_READONLY).setColorBackground(COLOR_READONLY).setGroup(videoSettingsUI);
		videoFileDuration = cp5.addTextfield("videoFileDuration").setLabel("Duration (s)").setValue("1").setInputFilter(ControlP5.FLOAT).setPosition(10, 95).setSize(60, 20).setUserInteraction(false).setAutoClear(false).setGroup(videoSettingsUI);
		videoFrameCountField = cp5.addTextfield("videoFileFrameCount").setLabel("Total Frames").setValue("0").setInputFilter(ControlP5.INTEGER).setAutoClear(false).setPosition(160, 95).setSize(60, 20).setGroup(videoSettingsUI);
		videoFileWidth = cp5.addTextfield("videoFileWidth").setLabel("Width").setValue("0").setInputFilter(ControlP5.INTEGER).setPosition(240, 95).setSize(60, 20).setUserInteraction(false).setAutoClear(false).setGroup(videoSettingsUI);
		videoFileHeight = cp5.addTextfield("videoFileHeight").setLabel("Height").setValue("0").setInputFilter(ControlP5.INTEGER).setPosition(320, 95).setSize(60, 20).setUserInteraction(false).setAutoClear(false).setGroup(videoSettingsUI);
		videoFPSField = cp5.addTextfield("videoFileFPS").setLabel("FPS").setInputFilter(ControlP5.FLOAT).setValue("29.97").setPosition(100, 95).setSize(30, 20).setAutoClear(false).setGroup(videoSettingsUI);
		videoFPSField.onChange(new CallbackListener() {
			@Override
			public void controlEvent(CallbackEvent arg0) {
				float fps = Float.valueOf(videoFPSField.getText());
				float duration = Float.valueOf(videoFileDuration.getText());
				videoFrameCountField.setValue(String.valueOf((int) (fps * duration)));
				videoFrameCountUpdated();
			}
		});
		
		cp5.addTextlabel("rotateVideoLabel").setText("Rotate Video (re-Open video to take effect)").setPosition(6, 150).setSize(100, 20).setGroup(videoSettingsUI);
		rotateVideo = cp5.addRadioButton("rotateVideo").setPosition(10, 170).setNoneSelectedAllowed(false).setItemsPerRow(4).setSpacingColumn(50).addItem("0°", 0).addItem("90°", 90).addItem("180°", 180).addItem("270°", 270).setGroup(videoSettingsUI);
		rotateVideo.activate(0);
		
		// Slit scan setup/run
		Group slitGenerationUI = cp5.addGroup("Slit-Scan").setMoveable(true).setPosition(10, 275).setBackgroundHeight(350).setWidth(400).setBackgroundColor(p.color(30, 30, 30, 240)).setBarHeight(20);
		
		cp5.addTextlabel("imageSelectionModeLabel").setText("Output Image Format").setPosition(6, 170).align(ControlP5.LEFT_OUTSIDE, ControlP5.BASELINE, ControlP5.LEFT_OUTSIDE, ControlP5.BASELINE).setSize(30, 20).setGroup(slitGenerationUI);
		imageSelectionMode = cp5.addRadioButton("imageSelectionMode").setPosition(10, 185).setNoneSelectedAllowed(true).setItemsPerRow(2).setSpacingColumn(50).addItem("Tiff", 1).addItem("PNG", 2).setGroup(slitGenerationUI);
		imageSelectionMode.activate(0);
		
		selectOutputFile = cp5.addButton("selectOutputFile").setLabel("Set Output File (tif,png)").setPosition(10, 40).setSize(180, 20).setGroup(slitGenerationUI).onClick(new CallbackListener() {
			@Override
			public void controlEvent(CallbackEvent arg0) {
				if (!p.generatingSlitScanImage) {
					p.outputFileSelector();
				}
			}
		});
		outputFileLabel = cp5.addTextfield("outputFileLabel").setLabel("Output File Path").setPosition(10, 70).setSize(350, 20).setColorForeground(COLOR_READONLY).setColorBackground(COLOR_READONLY).setAutoClear(false).setGroup(slitGenerationUI).setUserInteraction(false);
		
		startPixelField = cp5.addTextfield("startFrameField").setLabel("Start Frame").setText("0").setInputFilter(ControlP5.INTEGER).setAutoClear(false).setPosition(10, 120).setSize(60, 20).setGroup(slitGenerationUI);
		startPixelField.onChange(new CallbackListener() {
			@Override
			public void controlEvent(CallbackEvent theEvent) {
				Integer startingPixel = Integer.valueOf(startPixelField.getText());
				Integer imageWidth = Integer.valueOf(imageWidthField.getText());
				Integer slitWidth = Integer.valueOf(slitWidthField.getText());
				
				if (startingPixel + slitWidth >= imageWidth) {
					startPixelField.setText(String.valueOf(imageWidth - slitWidth));
				}
				if (startingPixel < 0) {
					startPixelField.setText("0");
				}
			}
		});
		
		slitWidthField = cp5.addTextfield("slitWidthField").setLabel("Slit Width").setText("1").setInputFilter(ControlP5.INTEGER).setAutoClear(false).setPosition(100, 120).setSize(40, 20).setGroup(slitGenerationUI);
		slitWidthField.onChange(new CallbackListener() {
			@Override
			public void controlEvent(CallbackEvent theEvent) {
				Integer slitWidth = 1;
				if (slitWidthField.getText() == null || slitWidthField.getText() == "") {
					slitWidthField.setText(String.valueOf(slitWidth));
				} else {
					slitWidth = Integer.valueOf(slitWidthField.getText());
					if (slitWidth <= 0) {
						slitWidth = 1;
						slitWidthField.setText(String.valueOf(slitWidth));
					}
				}
				
				//				if (videoWidth > 0) {
				//					float maxSlitLocation = ((float) videoWidth - (float) slitWidth) / (float) videoWidth;
				//					if (SLIT_LOCATION > maxSlitLocation) {
				//						SLIT_LOCATION = maxSlitLocation;
				//					}
				//				}
				
				Integer frameCount = Integer.valueOf(videoFrameCountField.getText());
				int newWidth = slitWidth * frameCount;
				
				imageWidthField.setText(String.valueOf(newWidth));
			}
		});
		
		imageWidthField = cp5.addTextfield("imageWidthField").setLabel("Width").setText("0").setInputFilter(ControlP5.INTEGER).setAutoClear(false).setUserInteraction(true).setPosition(170, 120).setSize(60, 20).setGroup(slitGenerationUI);
		imageHeightField = cp5.addTextfield("imageHeightField").setLabel("Height").setText("0").setInputFilter(ControlP5.INTEGER).setAutoClear(false).setUserInteraction(false).setPosition(240, 120).setSize(60, 20).setGroup(slitGenerationUI);
		
		cp5.addTextlabel("slitSelectionModeLabel").setText("Slit Location").setPosition(6, 200).align(ControlP5.LEFT_OUTSIDE, ControlP5.BASELINE, ControlP5.LEFT_OUTSIDE, ControlP5.BASELINE).setSize(30, 20).setGroup(slitGenerationUI);
		slitSelectionMode = cp5.addRadioButton("slitSelectionMode").setPosition(10, 215).setNoneSelectedAllowed(true).setItemsPerRow(2).setSpacingColumn(50).addItem("Fixed", 1).addItem("Keyframes (NOT-SUPPORTED)", 2).setGroup(slitGenerationUI);
		slitSelectionMode.activate(0);
		
		cp5.addLabel("a: add keyframe, d: delete keyframe").setPosition(6, 230).setGroup(slitGenerationUI);
		
		cp5.addTextlabel("colorDepthLabel").setText("Color Depth (bits-per-channel)").setPosition(6, 250).setSize(100, 20).setGroup(slitGenerationUI);
		colorDepth = cp5.addRadioButton("colorDepth").setPosition(10, 265).setNoneSelectedAllowed(true).setItemsPerRow(2).setSpacingColumn(50).addItem("8-bit", 8).addItem("16-bit ie 48bit-RGB", 16).setGroup(slitGenerationUI);
		colorDepth.activate(0);
		
		generateSlitScanImageButton = cp5.addButton("generateSlitScanImageButton").setLabel("Generate slit-scan image").setPosition(10, 300).setSize(200, 20).setGroup(slitGenerationUI).onClick(new CallbackListener() {
			@Override
			public void controlEvent(CallbackEvent arg0) {
				if (!p.generatingSlitScanImage) {
					p.generateSlitScan();
				}
			}
		});
		pauseGenerateSlitScanImageButton = cp5.addButton("pauseGenerateSlitScanImageButton").setLabel("Cancel").setPosition(220, 300).setSize(70, 20).setGroup(slitGenerationUI).onClick(new CallbackListener() {
			@Override
			public void controlEvent(CallbackEvent arg0) {
				if (p.generatingSlitScanImage) {
					if (!p.isCancelledGeneratingSlice()) {
						p.doCancelGeneratingSlice();
					}
				}
			}
		});
		generationProgressSlider = cp5.addSlider("progress").setLabel("Progress").setPosition(10, 330).setSize(300, 15).setRange(0, 100).setUserInteraction(false).setGroup(slitGenerationUI);
		
		videoScrubberWidth = p.width - 150;
		videoScrubberXOffset = 50;
		videoScrubberYOffset = p.height - videoScrubberXOffset;
		videoScrubber = cp5.addSlider("videoScrubber").setLabel("").setPosition(videoScrubberXOffset, videoScrubberYOffset).setSize(videoScrubberWidth, 30).setRange(0f, 1f).setSliderMode(Slider.FLEXIBLE);
		videoScrubber.addCallback(new CallbackListener() {
			@Override
			public void controlEvent(CallbackEvent theEvent) {
				checkScrubbing();
			}
		});
		
		cp5.addCallback(new CallbackListener() {
			@Override
			public void controlEvent(CallbackEvent e) {
				if (e.getController().getName() == "Tiff" || e.getController().getName() == "PNG") {
					p.ensureOutputFileMatchesFormat();
				}
			}
		});
	}
	
	private void checkScrubbing() {
		lastUserScrubMillis = p.millis();
		if (!p.generatingSlitScanImage) {
			scrubbing = true;
		}
	}
	
	public void updateProgress(float progress) {
		generationProgressSlider.setValue(progress);
	}
	
	public void setVideoInfo(float videoDuration, int width, int height, float fps) {
		videoFileDuration.setValue(String.valueOf(videoDuration));
		videoFPSField.setValue(String.valueOf(fps));
		videoFileWidth.setValue(String.valueOf(width));
		videoFileHeight.setValue(String.valueOf(height));
		
		//		float fps = Float.valueOf(videoFPSField.getText());
		videoFrameCountField.setValue(String.valueOf((int) Math.ceil((fps * videoDuration))));
		videoFrameCountUpdated();
	}
	
	public Integer getTotalVideoFrames() {
		return Integer.valueOf(videoFrameCountField.getText());
	}
	
	public RotateVideo getRotateVideo() {
		return rotateVideo((int) rotateVideo.getValue());
	}
	
	public void videoFileSelected(String videoFilePath) {
		videoFileLabel.setValue(videoFilePath);
		selectVideoFile.setColor(new CColor()); // restore default
		generateSlitScanImageButton.setColorBackground(p.color(255, 255, 0)).setColorLabel(p.color(0));
	}
	
	public void outputFileSelected(String outputFile) {
		outputFileLabel.setValue(outputFile);
	}
	
	//	public void slitDimensionsUpdates(int slitWidth, int slitHeight) {
	//		Integer frameCount = Integer.valueOf(videoFrameCountField.getText());
	//		imageWidthField.setText(String.valueOf(frameCount * slitWidth));
	//		imageHeightField.setText(String.valueOf(slitHeight));
	//	}
	
	private void videoFrameCountUpdated() {
		Integer frameCount = Integer.valueOf(videoFrameCountField.getText());
		Integer startingPixel = Integer.valueOf(startPixelField.getText());
		Integer slitWidth = Integer.valueOf(slitWidthField.getText());
		
		if (startingPixel + slitWidth >= frameCount) {
			startPixelField.setText(String.valueOf(frameCount - slitWidth));
		}
		imageWidthField.setText(String.valueOf(frameCount * slitWidth));
		if (p.video != null) {
			imageHeightField.setText(String.valueOf(p.video.heightDisplay()));
		}
		
		videoScrubber.setValue(0);
		if (p.video != null) {
			videoScrubber.setRange(0, p.video.duration());
		}
	}
	
	public Integer getStartingPixel() {
		return Integer.valueOf(startPixelField.getText());
	}
	
	public Integer getImageWidth() {
		return Integer.valueOf(imageWidthField.getText());
	}
	
	public Integer getImageHeight() {
		return Integer.valueOf(imageHeightField.getText());
	}
	
	public int getSlitWidth() {
		if (slitWidthField.getText() != null && !slitWidthField.getText().isEmpty()) {
			return Integer.valueOf(slitWidthField.getText());
		}
		return 1;
	}
	
	public enum PreviewMode {
		FRAME,
		SLIT,
		NONE;
	}
	
	public boolean slitLocationFixed() {
		int slitLocationI = (int) slitSelectionMode.getValue();
		if (slitLocationI == 1) {
			return true;
		}
		return false;
	}
	
	public boolean slitLocationKeyframes() {
		return !slitLocationFixed();
	}
	
	public void updateVideoDrawDimesions(int previewFrameWidth, int previewFrameHeight, int videoWidth, int videoHeight) {
		this.videoWidth = videoWidth;
		//		float scalingFactor = previewFrameWidth < p.width ? (float) p.width / (float) previewFrameWidth : 1f;
		//		float scalingFactor = Math.min(p.width / (float) previewFrameWidth, p.height / (float) previewFrameWidth);
		//		float newVideoDrawWidth = previewFrameWidth;
		//		float newVideoDrawHeight = previewFrameHeight;
		//		float aspectRatio = newVideoDrawWidth / newVideoDrawHeight;
		//		if (newVideoDrawWidth > p.width) {
		//			newVideoDrawWidth = p.width;
		//			newVideoDrawHeight = (int) (newVideoDrawWidth / aspectRatio);
		//		}
		//		if (newVideoDrawHeight > p.height) {
		//			newVideoDrawHeight = p.height;
		//			newVideoDrawWidth = (int) (newVideoDrawHeight * aspectRatio);
		//		}
		
		//		videoDrawWidth = newVideoDrawWidth;
		//		videoDrawHeight = newVideoDrawHeight;
		videoDrawWidth = previewFrameWidth;
		videoDrawHeight = previewFrameHeight;
	}
	
	public float getVideoDrawWidth() {
		return videoDrawWidth;
	}
	
	public float getVideoDrawHeight() {
		return videoDrawHeight;
	}
	
	public float getVideoPlayhead() {
		return videoScrubber.getValue();
	}
	
	public float getVideoDuration() {
		return videoFileDuration.getValue();
	}
	
	public void setVideoPlayhead(float newVideoPlayhead) {
		videoScrubber.setValue(newVideoPlayhead);
		checkScrubbing();
	}
	
	public boolean scrubbing() {
		return scrubbing;
	}
	
	public void doneScrubbing() {
		scrubbing = false;
	}
	
	public void updatePlayhead(float time) {
		videoScrubber.setValue(time);
	}
	
	public int getVideoScrubberYOffset() {
		return videoScrubberYOffset;
	}
	
	public void setVideoScrubberYOffset(int videoScrubberYOffset) {
		this.videoScrubberYOffset = videoScrubberYOffset;
	}
	
	public int getVideoScrubberWidth() {
		return videoScrubberWidth;
	}
	
	public int getVideoScrubberXOffset() {
		return videoScrubberXOffset;
	}
	
	public int getVideoWidth() {
		return videoWidth;
	}
	
	public long lastUserScrubMillis() {
		return lastUserScrubMillis;
	}
	
	public ColorDepth colorDepth() {
		if (colorDepth.getValue() == 16) {
			return ColorDepth.SIXTEEN_BIT;
		}
		return ColorDepth.EIGHT_BIT;
	}
	
	public ImageFormat outputImageFormat() {
		int slitLocationI = (int) imageSelectionMode.getValue();
		if (slitLocationI == 1) {
			return TIFF;
		}
		return PNG;
	}
	
	public void setOutputFileFormat(ImageFormat imageFormat) {
		if (imageFormat.tiff()) {
			imageSelectionMode.activate(0);
		} else {
			imageSelectionMode.activate(1);
		}
	}
	
	public void startGeneratingSlitScan() {
		// disable fields that shouldn't be changed during generation
		selectVideoFile.setLock(true);
		videoFPSField.setUserInteraction(false);
		videoFrameCountField.setUserInteraction(false);
		outputFileLabel.setUserInteraction(false);
		selectOutputFile.setLock(true);
		startPixelField.setUserInteraction(false);
		slitWidthField.setUserInteraction(false);
		imageWidthField.setUserInteraction(false);
		imageHeightField.setUserInteraction(false);
		generateSlitScanImageButton.setLock(true);
		videoScrubber.setUserInteraction(false);
		//		rotateVideo.setUserInteraction(true);
		//		pauseGenerateSlitScanImageButton.setLock(false);
	}
	
	public void doneGeneratingSlitScan() {
		// re-enable fields that were disabled during generation
		selectVideoFile.setLock(false);
		videoFPSField.setUserInteraction(true);
		videoFrameCountField.setUserInteraction(true);
		outputFileLabel.setUserInteraction(true);
		selectOutputFile.setLock(false);
		startPixelField.setUserInteraction(true);
		slitWidthField.setUserInteraction(true);
		imageWidthField.setUserInteraction(true);
		imageHeightField.setUserInteraction(true);
		generateSlitScanImageButton.setLock(false);
		videoScrubber.setUserInteraction(true);
		//		pauseGenerateSlitScanImageButton.setLock(true);
	}
	
	// adapted from
	// https://forum.processing.org/one/topic/controlp5-smart-textfields.html
	Textfield currentFocusedTf;
	Textfield previousFocusedTf;
	
	public void mouseReleased() {
		List<ControllerInterface<?>> ctrl = cp5.getAll();
		previousFocusedTf = currentFocusedTf;
		currentFocusedTf = null;
		for (ControllerInterface<?> ct : ctrl) {
			if (ct instanceof Textfield) {
				Textfield tf = (Textfield) ct;
				if (tf.isFocus() && tf.isUserInteraction()) {
					currentFocusedTf = tf;
					return;
				}
			}
		}
	}
	
	public void mousePressed() {
		if (currentFocusedTf != null && currentFocusedTf.isUserInteraction()) {
			if (currentFocusedTf.getText().length() <= 0) {
				//
			} else {
				currentFocusedTf.submit();
			}
		} else if (previousFocusedTf != null && previousFocusedTf.isUserInteraction()) {
			previousFocusedTf.submit();
		}
	}
	
	public void keyPressed() {
		if (p.keyCode == PConstants.TAB) {
			// lose focus of UI element on tab and submit it
			if (currentFocusedTf != null) {
				currentFocusedTf.setFocus(false);
				if (currentFocusedTf != null && currentFocusedTf.isUserInteraction()) {
					if (currentFocusedTf.getText().length() <= 0) {
						//
					} else {
						currentFocusedTf.submit();
					}
				}
			}
		}
	}
}
