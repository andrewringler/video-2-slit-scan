package com.andrewringler.slitscan;

public class ScrubbingHandler {
	private final Video2SlitScan p;
	private final UserInterface ui;
	private int lastScrubRequestMillis;
	private float lastScrubVideoPlayheadTarget = 0;
	
	public ScrubbingHandler(Video2SlitScan p, UserInterface ui) {
		this.p = p;
		this.ui = ui;
		lastScrubRequestMillis = p.millis();
	}
	
	public void handleScrub(VideoWrapper video) {
		if (ui.scrubbing() && video != null) {
			if (lastScrubVideoPlayheadTarget != ui.getVideoPlayhead()) {
				lastScrubVideoPlayheadTarget = ui.getVideoPlayhead();
				lastScrubRequestMillis = p.millis();
				video.jump(lastScrubVideoPlayheadTarget);
			}
			
			boolean maxTimeElapsedForScrubWait = p.millis() - lastScrubRequestMillis > 200;
			boolean frameReceivedSinceJump = p.timeOfLastVideoFrameRead > lastScrubRequestMillis;
			if (maxTimeElapsedForScrubWait || frameReceivedSinceJump) {
				ui.doneScrubbing();
				video.pause();
			}
		}
	}
}
