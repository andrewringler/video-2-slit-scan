package com.andrewringler.slitscan.ffmpeg;

import static processing.core.PApplet.constrain;
import static processing.core.PApplet.floor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.andrewringler.slitscan.RotateVideo;
import com.andrewringler.slitscan.SlitLocations;
import com.andrewringler.slitscan.UserInterface;
import com.andrewringler.slitscan.Video2SlitScan;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.FFmpegProgress;
import com.github.kokorin.jaffree.ffmpeg.FFmpegResultFuture;
import com.github.kokorin.jaffree.ffmpeg.ProgressListener;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;

public class GenerateSlitscanFFMPEG {
	private final AtomicBoolean disposed = new AtomicBoolean(false);
	private final AtomicBoolean playing = new AtomicBoolean(false);
	private final FFmpeg ffmpeg;
	private final Path tempDir;
	private final Path ffmpegBin;
	private final ExecutorService frameSamplingDone = Executors.newFixedThreadPool(1);
	private final File outputFileSixteenBit;
	
	private final Path outputPath;
	private final AtomicLong frameCount = new AtomicLong(0l);
	private final Runnable done;
	
	private int slitWidth;
	private int height;
	private int stagingWidth;
	
	public GenerateSlitscanFFMPEG(Video2SlitScan p, String absolutePath, boolean startPlaying, RotateVideo rotateVideo, int width, int height, float durationInSeconds, int numberOfFrames, SlitLocations slitLocations, UserInterface ui, File outputFileSixteenBit, Runnable done) {
		this.height = height;
		this.outputFileSixteenBit = outputFileSixteenBit;
		this.done = done;
		
		try {
			ffmpegBin = Paths.get(p.dataPath("") + "/ffmpeg-natives");
			tempDir = Files.createTempDirectory("ffmpeg");
			Path videoPath = Paths.get(absolutePath);
			outputPath = Paths.get(tempDir.toString(), "/frame_%09d.tiff");
			
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
			slitWidth = constrain(ui.getSlitWidth(), 1, width);
			stagingWidth = slitWidth % 2 == 0 ? slitWidth : slitWidth + 1;
			int slitLocationX = floor(constrain(slitLocationPct * (float) width, 0, width - 1));
			if (slitLocationX + stagingWidth > width) {
				slitLocationX = width - stagingWidth;
			}
			
			ffmpeg = FFmpeg.atPath(ffmpegBin) //
					.addInput(UrlInput.fromPath(videoPath)) //
					.addArguments("-vf", rotationFilter + "crop=" + stagingWidth + ":" + height + ":" + slitLocationX + ":0") //					
					.addArguments("-pix_fmt", "rgb48le") //
					.addArguments("-vsync", "1") //
					.setProgressListener(new ProgressListener() {
						@Override
						public void onProgress(FFmpegProgress progress) {
							Long nextFrame = progress.getFrame();
							if (nextFrame != null) {
								frameCount.set(nextFrame);
							}
						}
					}) //					
					.addOutput(UrlOutput.toPath(outputPath)); //
			
			play();
		} catch (IOException e) {
			done.run();
			throw new RuntimeException("Unable to create temporary directory for ffmpeg frame writing", e);
		}
	}
	
	private void play() {
		if (!disposed.get() && playing.compareAndSet(false, true)) {
			FFmpegResultFuture frameSamplingDoneFuture = ffmpeg.executeAsync();
			
			frameSamplingDone.execute(new Runnable() {
				public void run() {
					try {
						frameSamplingDoneFuture.get();
						
						String cropString = "";
						if (slitWidth != stagingWidth) {
							cropString = "crop=" + slitWidth + ":" + height + ":0:0,";
						}
						
						if (frameCount.get() > 1) {
							// https://superuser.com/questions/625189/combine-multiple-images-to-form-a-strip-of-images-ffmpeg
							// ffmpeg -i %03d.png -filter_complex scale=120:-1,tile=5x1 output.png
							FFmpeg finalSlitScanOutput = FFmpeg.atPath(ffmpegBin) //
									.addInput(UrlInput.fromPath(outputPath)) //
									.addArguments("-pix_fmt", "rgb48le") //
									.addArguments("-filter_complex", cropString + "tile=" + frameCount.get() + "x1") //
									.addOutput(UrlOutput.toPath(Paths.get(outputFileSixteenBit.toString()))); //
							
							finalSlitScanOutput.execute();
							
							done.run();
						}
						// TODO handle 1 frame
						// TODO throw on 0 frames?
					} catch (ExecutionException e) {
						done.run();
						e.printStackTrace();
						e.getCause().printStackTrace();
						System.err.println("Error trying to generate frame samples ");
					} catch (InterruptedException e) {
						done.run();
						e.printStackTrace();
						System.err.println("Error trying to generate frame samples");
					} catch (Exception e) {
						done.run();
						e.printStackTrace();
						System.err.println("Error trying to generate frame samples");
					} finally {
						// delete temp directory
						try {
							Files.walk(tempDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
							tempDir.toFile().delete();
						} catch (IOException e) {
							// nothing
						}
					}
				}
			});
		}
	}
	
	public void dispose() {
		disposed.set(false);
		playing.set(false);
		frameSamplingDone.shutdownNow();
	}
}