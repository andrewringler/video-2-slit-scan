package com.andrewringler.slitscan;

import static processing.core.PApplet.round;

import java.util.concurrent.ConcurrentSkipListSet;

public class SlitLocations {
	private final Video2SlitScan p;
	private final UserInterface ui;
	private final ConcurrentSkipListSet<SlitLocationKeyframe> slitLocations = new ConcurrentSkipListSet<SlitLocationKeyframe>();
	
	private boolean draggingSlit = false;
	private float slitLocationUI = 0.5f; // [0-1]
	private SlitLocationKeyframe editingKeyframe;
	
	public SlitLocations(Video2SlitScan p, UserInterface ui, float initialSlitLocation) {
		this.p = p;
		this.ui = ui;
		slitLocations.add(new SlitLocationKeyframe(0f, initialSlitLocation));
		slitLocations.add(new SlitLocationKeyframe(1f, initialSlitLocation));
	}
	
	public void mouseDragged() {
		if (draggingSlit) {
			// if we are using keyframes, we can only edit the slit location
			// if there is a keyframe present at the current scrub location
			if (ui.slitLocationKeyframes() && editingKeyframe == null) {
				draggingSlit = false;
				return;
			}
			
			int slitWidth = ui.getSlitWidth();
			float maxSlitLocation = ((float) ui.getVideoWidth() - (float) slitWidth) / (float) ui.getVideoWidth();
			float newSlitLocation = (float) p.mouseX / (float) ui.getVideoDrawWidth();
			if (newSlitLocation > maxSlitLocation) {
				slitLocationUI = maxSlitLocation;
			} else if (newSlitLocation < 0) {
				slitLocationUI = 0;
			} else {
				slitLocationUI = newSlitLocation;
			}
		}
	}
	
	public void mousePressed() {
		// if we are using keyframes, we can only edit the slit location
		// if there is a keyframe present at the current scrub location
		if (ui.slitLocationKeyframes() && editingKeyframe == null) {
			draggingSlit = false;
			return;
		}
		
		int slitLocationX = (int) (slitLocationUI * ui.getVideoDrawWidth());
		int slitWidth = ui.getSlitWidth();
		if (p.mouseX < slitLocationX + slitWidth + 5 && p.mouseX > slitLocationX - 5) {
			draggingSlit = true;
		}
	}
	
	public void mouseReleased() {
		draggingSlit = false;
	}
	
	public void draw() {
		if (!ui.slitLocationFixed()) {
			/* cancel keyframe editing, user has scrubbed away from current keyframe */
			if (editingKeyframe != null && ui.getVideoPlayhead() != editingKeyframe.getPositionInVideo() * p.video.duration()) {
				editingKeyframe = null;
			}
			/* user has moved the slit location for the selected keyframe */
			if (editingKeyframe != null && editingKeyframe.getLocationInFrame() != slitLocationUI) {
				slitLocations.remove(editingKeyframe);
				SlitLocationKeyframe newKeyframe = editingKeyframe.withNewLocationInFrame(slitLocationUI);
				slitLocations.add(newKeyframe);
				editingKeyframe = newKeyframe;
			}
			/* user has scrubbed to an existing keyframe */
			if (editingKeyframe == null) {
				for (SlitLocationKeyframe keyframe : slitLocations) {
					float videoPlayhead = keyframe.getPositionInVideo() * p.video.duration();
					if (ui.getVideoPlayhead() == videoPlayhead) {
						ui.setVideoPlayhead(videoPlayhead);
						slitLocationUI = keyframe.getLocationInFrame();
						editingKeyframe = keyframe;
						break;
					}
				}
			}
			
			// draw keyframe locations (in time) on scrubber as diamonds
			float scale = 15;
			for (SlitLocationKeyframe keyframe : slitLocations) {
				float x = ui.getVideoScrubberXOffset() + ui.getVideoScrubberWidth() * keyframe.getPositionInVideo();
				float y = ui.getVideoScrubberYOffset() - 20;
				p.strokeWeight(1f / scale);
				p.stroke(0);
				if (p.mouseX < x + scale && p.mouseX > x - scale && p.mouseY < y + scale && p.mouseY > y - scale) {
					p.fill(255, 200, 0);
					if (p.mousePressed) {
						if (!p.generatingSlitScanImage) {
							ui.setVideoPlayhead(keyframe.getPositionInVideo() * p.video.duration());
							slitLocationUI = keyframe.getLocationInFrame();
							editingKeyframe = keyframe;
						}
					}
				} else if (editingKeyframe == keyframe) {
					p.fill(255, 200, 0);
				} else {
					p.fill(255);
				}
				
				p.pushMatrix();
				p.translate(x - scale / 2f, y - scale / 2f);
				p.scale(scale);
				p.quad(0.5f, 0f, 1f, 0.5f, 0.5f, 1f, 0f, 0.5f); // diamond
				p.popMatrix();
			}
		}
		
		// Draw slit location
		p.noFill();
		p.strokeWeight(1);
		p.stroke(255);
		p.patternLine(getScaledSlitLocation(), 0, getScaledSlitLocation(), (int) ui.getVideoDrawHeight(), 0x0300, 1);
		p.stroke(0);
		p.patternLine(getScaledSlitLocation(), 0, getScaledSlitLocation(), (int) ui.getVideoDrawHeight(), 0x3000, 1);
		
		if (draggingSlit) {
			p.stroke(255, 255, 0);
			p.patternLine(getScaledSlitLocation(), 0, getScaledSlitLocation(), (int) ui.getVideoDrawHeight(), 0x0300, 1);
			p.stroke(0, 255, 0);
			p.patternLine(getScaledSlitLocation(), 0, getScaledSlitLocation(), (int) ui.getVideoDrawHeight(), 0x3000, 1);
		}
	}
	
	private int getScaledSlitLocation() {
		return (int) round(slitLocationUI * ui.getVideoDrawWidth());
	}
	
	public float getSlitLocationNormalized(float positionInVideo) {
		return slitLocationUI;
	}
	
}
