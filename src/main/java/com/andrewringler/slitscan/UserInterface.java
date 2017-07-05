package com.andrewringler.slitscan;

import java.io.File;

import controlP5.Button;
import controlP5.CColor;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlFont;
import controlP5.ControlP5;
import controlP5.Group;
import controlP5.RadioButton;
import controlP5.Slider;
import controlP5.Textfield;
import processing.core.PFont;

public class UserInterface {
	private final ControlP5 cp5;
	private final Video2SlitScan p;
	private final Textfield videoFileLabel;
	private final Textfield videoFileDuration;
	private final Textfield videoFrameCountField;
	private final Textfield videoFPSField;
	private final Slider generationProgressSlider;
	private final RadioButton playSpeed;
	private final Textfield outputFileLabel;
	private final Textfield startPixelField;
	private final Textfield imageWidthField;
	private final Textfield imageHeightField;
	private final Textfield slitWidthField;
	private final Button selectVideoFile;
	private final Button generateSlitScanImageButton;
	
	public boolean draggingSlit = false;
	public float SLIT_LOCATION = 0.5f; // [0-1]
	
	public UserInterface(Video2SlitScan p) {
		this.p = p;
		cp5 = new ControlP5(p);
		
		PFont pfont = p.createFont("RobotoMono-Medium", 10, true); // use true/false for smooth/no-smooth
		ControlFont font = new ControlFont(pfont, 10);
		cp5.setFont(font);
		
		// Video load/setup
		Group videoSettingsUI = cp5.addGroup("Video").setMoveable(true).setPosition(10, 30).setBackgroundHeight(165).setWidth(400).setBackgroundColor(p.color(30, 30, 30, 240)).setBarHeight(20);
		selectVideoFile = cp5.addButton("selectVideoFile").setLabel("Open video file").setColorBackground(p.color(255, 255, 0)).setColorLabel(p.color(0)).setPosition(10, 10).setSize(120, 20).setGroup(videoSettingsUI).onClick(new CallbackListener() {
			@Override
			public void controlEvent(CallbackEvent arg0) {
				p.videoFileSelector();
			}
		});
		videoFileLabel = cp5.addTextfield("videoFileTextField").setLabel("Video File Path").setPosition(10, 40).setSize(350, 20).setGroup(videoSettingsUI);
		videoFileDuration = cp5.addTextfield("videoFileDuration").setLabel("Duration (s)").setValue("0").setInputFilter(ControlP5.FLOAT).setPosition(10, 95).setSize(60, 20).setUserInteraction(false).setAutoClear(false).setGroup(videoSettingsUI);
		videoFrameCountField = cp5.addTextfield("videoFileFrameCount").setLabel("Total Frames").setValue("0").setInputFilter(ControlP5.INTEGER).setAutoClear(false).setPosition(160, 95).setSize(60, 20).setGroup(videoSettingsUI);
		videoFPSField = cp5.addTextfield("videoFileFPS").setLabel("FPS").setInputFilter(ControlP5.FLOAT).setValue("30").setPosition(100, 95).setSize(30, 20).setAutoClear(false).setGroup(videoSettingsUI);
		videoFPSField.onChange(new CallbackListener() {
			@Override
			public void controlEvent(CallbackEvent arg0) {
				float fps = Float.valueOf(videoFPSField.getText());
				float duration = Float.valueOf(videoFileDuration.getText());
				videoFrameCountField.setValue(String.valueOf((int) (fps * duration)));
				videoFrameCountUpdated();
			}
		});
		
		// Slit scan setup/run
		Group slitGenerationUI = cp5.addGroup("Slit-Scan").setMoveable(true).setPosition(10, 230).setBackgroundHeight(250).setWidth(400).setBackgroundColor(p.color(30, 30, 30, 240)).setBarHeight(20);
		cp5.addTextlabel("playSpeedLabel").setText("Play Speed").setPosition(6, 10).align(ControlP5.LEFT_OUTSIDE, ControlP5.BASELINE, ControlP5.LEFT_OUTSIDE, ControlP5.BASELINE).setSize(30, 20).setGroup(slitGenerationUI);
		playSpeed = cp5.addRadioButton("chooseRenderSpeed").setPosition(10, 25).setNoneSelectedAllowed(true).setItemsPerRow(4).setSpacingColumn(20).addItem("1x", 1).addItem("2x", 2).addItem("4x", 4).addItem("8x", 8).setGroup(slitGenerationUI);
		playSpeed.activate(0);
		
		cp5.addButton("selectOutputFile").setLabel("Set Output File (*.tif)").setPosition(10, 40).setSize(180, 20).setGroup(slitGenerationUI).onClick(new CallbackListener() {
			@Override
			public void controlEvent(CallbackEvent arg0) {
				p.outputFileSelector();
			}
		});
		outputFileLabel = cp5.addTextfield("outputFileLabel").setLabel("Output File Path").setPosition(10, 70).setSize(350, 20).setAutoClear(false).setGroup(slitGenerationUI);
		outputFileLabel.onChange(new CallbackListener() {
			@Override
			public void controlEvent(CallbackEvent theEvent) {
				// verify
				p.outputFileSelected(new File(outputFileLabel.getText()));
			}
		});
		
		startPixelField = cp5.addTextfield("startFrameField").setLabel("Start Pixel").setText("0").setInputFilter(ControlP5.INTEGER).setAutoClear(false).setPosition(10, 120).setSize(60, 20).setGroup(slitGenerationUI);
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
				Integer slitWidth = Integer.valueOf(slitWidthField.getText());
				if (slitWidth <= 0) {
					slitWidth = 1;
					slitWidthField.setText(String.valueOf(slitWidth));
				}
				
				Integer frameCount = Integer.valueOf(videoFrameCountField.getText());
				int newWidth = slitWidth * frameCount;
				
				imageWidthField.setText(String.valueOf(newWidth));
			}
		});
		
		imageWidthField = cp5.addTextfield("imageWidthField").setLabel("Width").setText("0").setInputFilter(ControlP5.INTEGER).setAutoClear(false).setUserInteraction(true).setPosition(170, 120).setSize(60, 20).setGroup(slitGenerationUI);
		imageHeightField = cp5.addTextfield("imageHeightField").setLabel("Height").setText("0").setInputFilter(ControlP5.INTEGER).setAutoClear(false).setUserInteraction(false).setPosition(240, 120).setSize(60, 20).setGroup(slitGenerationUI);
		
		generateSlitScanImageButton = cp5.addButton("generateSlitScanImageButton").setLabel("Generate slit-scan image").setPosition(10, 200).setSize(200, 20).setGroup(slitGenerationUI).onClick(new CallbackListener() {
			@Override
			public void controlEvent(CallbackEvent arg0) {
				p.generateSlitScan();
			}
		});
		generationProgressSlider = cp5.addSlider("progress").setLabel("Progress").setPosition(10, 230).setSize(300, 15).setRange(0, 100).setUserInteraction(false).setGroup(slitGenerationUI);
	}
	
	public float getPlaySpeed() {
		int playSpeedI = (int) playSpeed.getValue();
		if (playSpeedI > 0 && playSpeedI < 20) {
			return playSpeedI;
		}
		return 1;
	}
	
	public void updateProgress(float progress) {
		generationProgressSlider.setValue(progress);
	}
	
	public void mouseDragged() {
		if (draggingSlit) {
			SLIT_LOCATION = (float) p.mouseX / p.width;
		}
	}
	
	public void mousePressed() {
		int slitLocationX = (int) (SLIT_LOCATION * p.width);
		if (p.mouseX < slitLocationX + 5 && p.mouseX > slitLocationX - 5) {
			draggingSlit = true;
		}
	}
	
	public void mouseReleased() {
		draggingSlit = false;
	}
	
	public void setVideoDuration(float videoDuration) {
		videoFileDuration.setValue(String.valueOf(videoDuration));
		
		float fps = Float.valueOf(videoFPSField.getText());
		videoFrameCountField.setValue(String.valueOf((int) (fps * videoDuration)));
		videoFrameCountUpdated();
	}
	
	public Integer getTotalVideoFrames() {
		return Integer.valueOf(videoFrameCountField.getText());
	}
	
	public void videoFileSelected(String videoFilePath) {
		videoFileLabel.setValue(videoFilePath);
		selectVideoFile.setColor(new CColor()); // restore default
		generateSlitScanImageButton.setColorBackground(p.color(255, 255, 0)).setColorLabel(p.color(0));
	}
	
	public void outputFileSelected(String outputFile) {
		outputFileLabel.setValue(outputFile);
	}
	
	private void videoFrameCountUpdated() {
		Integer frameCount = Integer.valueOf(videoFrameCountField.getText());
		Integer startingPixel = Integer.valueOf(startPixelField.getText());
		Integer slitWidth = Integer.valueOf(slitWidthField.getText());
		
		if (startingPixel + slitWidth >= frameCount) {
			startPixelField.setText(String.valueOf(frameCount - slitWidth));
		}
		imageWidthField.setText(String.valueOf(frameCount * slitWidth));
		imageHeightField.setText(String.valueOf(p.video.height));
	}
	
	public Integer getStartingPixel() {
		return Integer.valueOf(startPixelField.getText());
	}
	
	public Integer getImageWidth() {
		return Integer.valueOf(imageWidthField.getText());
	}
	
	public int getSlitWidth() {
		return Integer.valueOf(slitWidthField.getText());
	}
}
