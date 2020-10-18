package com.andrewringler.slitscan.ffmpeg;

import static com.github.underscore.U.debounce;
import static java.lang.Math.min;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.andrewringler.slitscan.Dimensions;
import com.andrewringler.slitscan.FrameReady;
import com.andrewringler.slitscan.RotateVideo;
import com.andrewringler.slitscan.Video2SlitScan;
import com.andrewringler.slitscan.VideoWrapper;
import com.github.kokorin.jaffree.Rational;
import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.FFmpegResultFuture;
import com.github.kokorin.jaffree.ffmpeg.FrameOutput;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import com.github.kokorin.jaffree.ffprobe.Stream;
import com.github.underscore.Supplier;

public class VideoWrapperFFMPEG implements VideoWrapper {
	private final AtomicBoolean playing = new AtomicBoolean(false);
	final float duration;
	final Dimensions dimension;
	final Dimensions codedDimension;
	private final Integer numberOfFrames;
	private final Rational avgFrameRate;
	final FrameReady frameReady;
	private final Path ffmpegBin;
	private final Path videoPath;
	private final ExecutorService playThread = Executors.newFixedThreadPool(1);
	private final AtomicReference<FFmpegResultFuture> resultFuture = new AtomicReference<FFmpegResultFuture>();
	final AtomicLong seekLocationSeconds = new AtomicLong(0l);
	private final RotateVideo rotateVideo;
	
	private final ConsumeAllFrames consumeAllFrames = new ConsumeAllFrames(this);
	private final ConsumeOneFrame consumeOneFrame = new ConsumeOneFrame(this);
	
	public VideoWrapperFFMPEG(Video2SlitScan p, String absolutePath, FrameReady frameReady, boolean startPlaying, RotateVideo rotateVideo) {
		this.frameReady = frameReady;
		this.rotateVideo = rotateVideo;
		ffmpegBin = Paths.get(p.dataPath("") + "/ffmpeg-natives");
		
		FFprobe ffprobe;
		if (ffmpegBin != null) {
			ffprobe = FFprobe.atPath(ffmpegBin);
		} else {
			ffprobe = FFprobe.atPath();
		}
		videoPath = Paths.get(absolutePath);
		
		ffprobe = ffprobe //
				.setShowStreams(true) //
				.setSelectStreams(StreamType.VIDEO) //
				.setInput(videoPath); //
		FFprobeResult result = ffprobe.execute();
		if (result.getStreams().size() > 0) {
			Stream stream = result.getStreams().get(0);
			Float streamDurationSeconds = stream != null ? stream.getDuration() : null;
			Float containerDurationSeconds = result.getFormat() != null ? result.getFormat().getDuration() : null;
			duration = streamDurationSeconds != null ? streamDurationSeconds : containerDurationSeconds != null ? containerDurationSeconds : 0;
			int width = stream.getWidth() == null ? 0 : stream.getWidth();
			int height = stream.getHeight() == null ? 0 : stream.getHeight();
			int codedWidth = stream.getCodedWidth() == null ? 0 : stream.getCodedWidth();
			int codedHeight = stream.getCodedHeight() == null ? 0 : stream.getCodedHeight();
			dimension = rotateVideo.rotatedDimensions(new Dimensions(width, height));
			codedDimension = rotateVideo.rotatedDimensions(new Dimensions(codedWidth, codedHeight));
			numberOfFrames = stream.getNbFrames();
			avgFrameRate = stream.getAvgFrameRate();
			play();
		} else {
			duration = 0;
			dimension = new Dimensions(0, 0);
			codedDimension = new Dimensions(0, 0);
			numberOfFrames = null;
			avgFrameRate = null;
		}
	}
	
	@Override
	public void stop() {
		FFmpegResultFuture future = resultFuture.get();
		if (future != null) {
			if (playing.compareAndSet(true, false)) {
				playing.set(false);
				future.graceStop();
			}
		}
	}
	
	@Override
	public void play() {
		if (playing.compareAndSet(false, true)) {
			String rotationFilter = "";
			switch (rotateVideo) {
				case NINETY_DEGREES:
					rotationFilter = "transpose=1";
					break;
				case ONE_EIGHTY_DEGREES:
					rotationFilter = "transpose=2,transpose=2";
					break;
				case TWO_SEVENTY_DEGREES:
					rotationFilter = "transpose=2";
					break;
				default:
					// nothing
			}
			
			FFmpeg ffmpegConfig = FFmpeg //
					.atPath(ffmpegBin) //
					.addInput(UrlInput.fromPath(videoPath));
			if (rotateVideo.hasRotation()) {
				ffmpegConfig = ffmpegConfig.addArguments("-vf", rotationFilter);
			}
			ffmpegConfig = ffmpegConfig.addOutput(FrameOutput.withConsumer(consumeAllFrames));
			resultFuture.set(ffmpegConfig.executeAsync());
			
			playThread.execute(new Runnable() {
				@Override
				public void run() {
					FFmpegResultFuture future = resultFuture.get();
					if (future != null) {
						try {
							future.get();
						} catch (InterruptedException e) {
							// nothing
						} catch (ExecutionException e) {
							// nothing
						}
					}
				}
			});
		}
	}
	
	@Override
	public void pause() {
		FFmpegResultFuture future = resultFuture.get();
		if (future != null) {
			if (playing.compareAndSet(true, false)) {
				playing.set(false);
				future.graceStop();
			}
		}
	}
	
	@Override
	public void jump(float f) {
		seekOneFrame(f);
	}
	
	private final Supplier<Void> seekOneFrameDebounced = debounce(new Supplier<Void>() {
		@Override
		public Void get() {
			long seekSeconds = seekLocationSeconds.get();
			
			String rotationFilter = "";
			switch (rotateVideo) {
				case NINETY_DEGREES:
					rotationFilter = "transpose=1";
					break;
				case ONE_EIGHTY_DEGREES:
					rotationFilter = "transpose=2,transpose=2";
					break;
				case TWO_SEVENTY_DEGREES:
					rotationFilter = "transpose=2";
					break;
				default:
					// nothing
			}
			
			FFmpeg config = FFmpeg.atPath(ffmpegBin) //
					.addInput(UrlInput.fromPath(videoPath).setPosition(seekSeconds, SECONDS));
			if (rotateVideo.hasRotation()) {
				config = config.addArguments("-vf", rotationFilter);
			}
			FFmpegResultFuture oneFrameResult = config.addOutput(FrameOutput.withConsumer(consumeOneFrame).setFrameCount(StreamType.VIDEO, 1l)) //
					.executeAsync();
			playThread.execute(new Runnable() {
				@Override
				public void run() {
					try {
						oneFrameResult.get();
					} catch (InterruptedException e) {
						// nothing
					} catch (ExecutionException e) {
						// nothing
					}
				}
			});
			
			return null;
		}
	}, 800);
	
	public void seekOneFrame(float f) {
		stop();
		seekLocationSeconds.set((long) f);
		seekOneFrameDebounced.get();
	}
	
	@Override
	public void dispose() {
		playThread.shutdownNow();
	}
	
	@Override
	public void volume(int i) {
	}
	
	@Override
	public void speed(float playSpeed) {
	}
	
	@Override
	public float duration() {
		return duration;
	}
	
	@Override
	public float timeSeconds() {
		if (numberOfFrames != null && numberOfFrames > 0) {
			return numberOfFrames / consumeAllFrames.frameCounter.get() * duration();
		}
		float timeSeconds = consumeAllFrames.frameCounter.get() * frameRate();
		return min(timeSeconds, duration());
	}
	
	@Override
	public Integer exactFrameCount() {
		return numberOfFrames;
	}
	
	public float frameRate() {
		return avgFrameRate == null ? 60 : avgFrameRate.floatValue();
	}
	
	@Override
	public int widthCoded() {
		return codedDimension.width;
	}
	
	@Override
	public int heightCoded() {
		return codedDimension.height;
	}
	
	@Override
	public int widthDisplay() {
		return dimension.width;
	}
	
	@Override
	public int heightDisplay() {
		return dimension.height;
	}
}
