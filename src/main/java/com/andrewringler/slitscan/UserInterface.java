package com.andrewringler.slitscan;

import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import controlP5.Group;
import controlP5.RadioButton;
import controlP5.Slider;
import controlP5.Textfield;

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
	
	public boolean draggingSlit = false;
	public float SLIT_LOCATION = 0.5f; // [0-1]
	
	public UserInterface(Video2SlitScan p) {
		this.p = p;
		cp5 = new ControlP5(p);
		
		// Video load/setup
		Group videoSettingsUI = cp5.addGroup("Video").setPosition(10, 20).setBackgroundHeight(150).setWidth(400).setBackgroundColor(p.color(30, 30, 30, 240));
		cp5.addButton("selectVideoFile").setLabel("Open video file").setPosition(10, 10).setSize(100, 20).setGroup(videoSettingsUI).onClick(new CallbackListener() {
			@Override
			public void controlEvent(CallbackEvent arg0) {
				p.videoFileSelector();
			}
		});
		videoFileLabel = cp5.addTextfield("videoFileTextField").setLabel("Video File Path").setPosition(10, 40).setSize(350, 20).setGroup(videoSettingsUI);
		videoFileDuration = cp5.addTextfield("videoFileDuration").setLabel("Duration (s)").setValue("0").setInputFilter(ControlP5.FLOAT).setPosition(10, 80).setSize(60, 20).setUserInteraction(false).setGroup(videoSettingsUI);
		videoFrameCountField = cp5.addTextfield("videoFileFrameCount").setLabel("Total Frames").setValue("0").setInputFilter(ControlP5.INTEGER).setPosition(120, 80).setSize(60, 20).setUserInteraction(false).setGroup(videoSettingsUI);
		videoFPSField = cp5.addTextfield("videoFileFPS").setLabel("FPS").setInputFilter(ControlP5.FLOAT).setValue("30").setPosition(80, 80).setSize(30, 20).setAutoClear(false).setGroup(videoSettingsUI);
		videoFPSField.onChange(new CallbackListener() {
			@Override
			public void controlEvent(CallbackEvent arg0) {
				float fps = Float.valueOf(videoFPSField.getText());
				float duration = Float.valueOf(videoFileDuration.getText());
				videoFrameCountField.setValue(String.valueOf((int) (fps * duration)));
			}
		});
		
		// Slit scan setup/run
		Group slitGenerationUI = cp5.addGroup("Slit-Scan").setPosition(10, 190).setBackgroundHeight(200).setWidth(400).setBackgroundColor(p.color(30, 30, 30, 240));
		cp5.addTextlabel("playSpeedLabel").setText("Play Speed").setPosition(6, 10).align(ControlP5.LEFT_OUTSIDE, ControlP5.BASELINE, ControlP5.LEFT_OUTSIDE, ControlP5.BASELINE).setSize(30, 20).setGroup(slitGenerationUI);
		playSpeed = cp5.addRadioButton("chooseRenderSpeed").setPosition(10, 25).setNoneSelectedAllowed(true).setItemsPerRow(4).setSpacingColumn(20).addItem("1x", 1).addItem("2x", 2).addItem("4x", 4).addItem("8x", 8).setGroup(slitGenerationUI);
		playSpeed.activate(0);
		
		cp5.addButton("selectOutputFile").setLabel("Set Output File (*.tif)").setPosition(10, 40).setSize(100, 20).setGroup(slitGenerationUI).onClick(new CallbackListener() {
			@Override
			public void controlEvent(CallbackEvent arg0) {
				p.outputFileSelector();
			}
		});
		outputFileLabel = cp5.addTextfield("outputFileLabel").setLabel("Output File Path").setPosition(10, 70).setSize(350, 20).setGroup(slitGenerationUI);
		
		cp5.addButton("Generate slit-scan image").setPosition(10, 150).setSize(150, 20).setGroup(slitGenerationUI).onClick(new CallbackListener() {
			@Override
			public void controlEvent(CallbackEvent arg0) {
				p.generateSlitScan();
			}
		});
		generationProgressSlider = cp5.addSlider("progress").setLabel("Progress").setPosition(10, 180).setRange(0, 100).setUserInteraction(false).setGroup(slitGenerationUI);
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
		videoFrameCountField.setText(String.valueOf((int) (fps * videoDuration)));
	}
	
	public Integer getTotalVideoFrames() {
		return Integer.valueOf(videoFrameCountField.getText());
	}
	
	public void videoFileSelected(String videoFilePath) {
		videoFileLabel.setValue(videoFilePath);
	}
	
	public void outputFileSelected(String outputFile) {
		outputFileLabel.setValue(outputFile);
	}
}
