package com.andrewringler.slitscan.vlcj;

import static uk.co.caprica.vlcj.media.MediaParsedStatus.DONE;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.andrewringler.slitscan.Dimensions;
import com.andrewringler.slitscan.FrameReady;
import com.andrewringler.slitscan.RotateVideo;
import com.andrewringler.slitscan.Video2SlitScan;
import com.andrewringler.slitscan.VideoWrapper;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.media.Media;
import uk.co.caprica.vlcj.media.MediaEventAdapter;
import uk.co.caprica.vlcj.media.MediaParsedStatus;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;

public class VideoWrapperVLCJ implements VideoWrapper {
	private static final Logger LOG = LoggerFactory.getLogger(VideoWrapperVLCJ.class);
	
	Video2SlitScan p;
	SaveFrameCallback saveFrame = new SaveFrameCallback();
	RV32BufferFormatCallback bufferFormat = new RV32BufferFormatCallback();
	EmbeddedMediaPlayer m;
	float durationSeconds = 0;
	Dimensions videoDimensions = new Dimensions(0, 0);
	int[] buffer = null;
	volatile boolean ready = false;
	volatile boolean playing = false;
	private final FrameReady frameReady;
	
	class SaveFrameCallback implements RenderCallback {
		@Override
		public void display(MediaPlayer mediaPlayer, ByteBuffer[] nativeBuffers, BufferFormat bufferFormat) {
			PImageFromIntBuffer sourceFrame = new PImageFromIntBuffer(videoDimensions.width, videoDimensions.height, nativeBuffers[0].asIntBuffer());
			//			PImageFromIntBuffer rotatedFrame = sourceFrame.rotate(rotateVideo);
			frameReady.processFrame(sourceFrame);
		}
	}
	
	class RV32BufferFormatCallback implements BufferFormatCallback {
		@Override
		public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
			if (buffer == null) {
				buffer = new int[sourceWidth * sourceHeight];
				videoDimensions = new Dimensions(sourceWidth, sourceHeight);
			}
			return new RV32BufferFormat(sourceWidth, sourceHeight);
		}
	}
	
	public VideoWrapperVLCJ(Video2SlitScan p, String absolutePath, FrameReady frameReady, boolean startPlaying, RotateVideo rotateVideo) {
		this.p = p;
		this.frameReady = frameReady;
		this.playing = startPlaying;
		
		String[] mediaPlayerArgs = {};
		if (rotateVideo.hasRotation()) {
			// vlc --transform-type one of: 90,180,270,hflip,vflip
			mediaPlayerArgs = new String[] { "--video-filter=transform", "--transform-type=" + rotateVideo.degreesString() };
		}
		
		MediaPlayerFactory factory = new MediaPlayerFactory(mediaPlayerArgs);
		m = factory.mediaPlayers().newEmbeddedMediaPlayer();
		m.videoSurface().set(factory.videoSurfaces().newVideoSurface(bufferFormat, saveFrame, true));
		
		m.events().addMediaEventListener(new MediaEventAdapter() {
			@Override
			public void mediaParsedChanged(Media media, MediaParsedStatus newStatus) {
				if (newStatus == DONE && media.info().videoTracks().size() > 0) {
					ready = true;
					durationSeconds = (float) ((float) media.info().duration() / 1000.0);
					if (playing) {
						play();
					}
				} else {
					LOG.error("unable to load video " + newStatus.toString());
				}
			}
		});
		m.media().prepare(absolutePath);
		m.media().parsing().parse();
	}
	
	@Override
	public void stop() {
		if (ready()) {
			playing = false;
			m.controls().stop();
		}
	}
	
	public void dispose() {
		playing = false;
		m = null;
	}
	
	public void volume(int i) {
	}
	
	@Override
	public void play() {
		playing = true;
		if (ready()) {
			m.controls().play();
		}
	}
	
	@Override
	public float duration() {
		if (ready()) {
			return durationSeconds;
		}
		return 0;
	}
	
	@Override
	public void pause() {
		if (ready()) {
			m.controls().pause();
		}
	}
	
	@Override
	public void jump(float f) {
		if (ready()) {
			m.controls().setTime(Math.round(f * 1000.0));
		}
	}
	
	@Override
	public float timeSeconds() {
		if (ready()) {
			return m.status().position() * duration();
		}
		return 0;
	}
	
	@Override
	public int width() {
		return videoDimensions.width;
	}
	
	@Override
	public int height() {
		return videoDimensions.height;
	}
	
	@Override
	public void speed(float playSpeed) {
		if (ready()) {
			m.controls().setRate(playSpeed);
		}
	}
	
	public boolean ready() {
		return ready;
	}
}