package com.andrewringler.slitscan.ffmpeg;

import static processing.core.PApplet.constrain;
import static processing.core.PApplet.floor;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import com.andrewringler.slitscan.ColorDepth;
import com.andrewringler.slitscan.RotateVideo;
import com.andrewringler.slitscan.SlitLocations;
import com.andrewringler.slitscan.UserInterface;
import com.andrewringler.slitscan.Video2SlitScan;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.FFmpegProgress;
import com.github.kokorin.jaffree.ffmpeg.NullOutput;
import com.github.kokorin.jaffree.ffmpeg.ProgressListener;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;

public class GenerateSlitscanFFMPEG {
	private final ExecutorService ffmpegThread = Executors.newFixedThreadPool(1);
	
	public GenerateSlitscanFFMPEG(Video2SlitScan p, ColorDepth colorDepth, String videoFileAbsolutePath, boolean startPlaying, RotateVideo rotateVideo, int width, int height, float durationInSeconds, SlitLocations slitLocations, UserInterface ui, File slitscanOutputFile, Runnable done) {
		Path ffmpegBin = Paths.get(p.dataPath("") + "/ffmpeg-natives");
		Path videoPath = Paths.get(videoFileAbsolutePath);
		AtomicLong frameCount = new AtomicLong(0);
		
		ffmpegThread.execute(new Runnable() {
			@Override
			public void run() {
				/* Count Frames */
				FFmpeg.atPath(ffmpegBin) //
						.addInput(UrlInput.fromPath(videoPath)) //
						.setProgressListener(new ProgressListener() {
							@Override
							public void onProgress(FFmpegProgress progress) {
								Long nextFrame = progress.getFrame();
								if (nextFrame != null) {
									frameCount.set(nextFrame);
								}
							}
						}) //		
						.addOutput(new NullOutput()) //
						.execute();
						
				if (frameCount.get() > 0) {
					String rotationFilter = "";
					switch (rotateVideo) {
						case NINETY_DEGREES:
							rotationFilter = "transpose=1,";
							break;
						case ONE_EIGHTY_DEGREES:
							rotationFilter = "transpose=2,transpose=2,";
							break;
						case TWO_SEVENTY_DEGREES:
							rotationFilter = "transpose=2,";
							break;
						default:
							// nothing
					}
					
					float slitLocationPct = slitLocations.getSlitLocationNormalized(0);
					int slitWidth = constrain(ui.getSlitWidth(), 1, width);
					int slitLocationX = floor(constrain(slitLocationPct * (float) width, 0, width - 1));
					if (slitLocationX + slitWidth > width) {
						slitLocationX = width - slitWidth;
					}
					String pixelFormat = colorDepth.isSixteenBit() ? "rgb48le" : "rgba";
					
					/*
					 * Generate Slit Scan
					 * ref: https://superuser.com/questions/625189/combine-multiple-images-to-form-a-strip-of-images-ffmpeg
					 * ref: https://trac.ffmpeg.org/ticket/7818 (cropping exact=1)
					 */
					FFmpeg.atPath(ffmpegBin) //
							.addInput(UrlInput.fromPath(videoPath)) //
							.addArguments("-filter_complex", //
									rotationFilter + //
					"crop=" + slitWidth + ":" + height + ":" + slitLocationX + ":0:exact=1," + //
					"tile=" + (int) frameCount.get() + "x1") //					
							.addArguments("-pix_fmt", pixelFormat) //
							.addOutput(UrlOutput.toPath(Paths.get(slitscanOutputFile.toString()))) //
							.execute(); //
					
					done.run();
					dispose();
				}
			}
		});
	}
	
	public void dispose() {
		ffmpegThread.shutdownNow();
	}
}