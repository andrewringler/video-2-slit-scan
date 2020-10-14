package com.andrewringler.slitscan.vlcj;

import static com.andrewringler.slitscan.ImageTransforms.rotateImage;
import static uk.co.caprica.vlcj.media.MediaParsedStatus.DONE;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.andrewringler.slitscan.Dimensions;
import com.andrewringler.slitscan.Frame;
import com.andrewringler.slitscan.FrameReady;
import com.andrewringler.slitscan.RotateVideo;
import com.andrewringler.slitscan.Video2SlitScan;
import com.andrewringler.slitscan.VideoMeta;
import com.andrewringler.slitscan.VideoWrapper;

import processing.core.PImage;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.media.Media;
import uk.co.caprica.vlcj.media.MediaEventAdapter;
import uk.co.caprica.vlcj.media.MediaParsedStatus;
import uk.co.caprica.vlcj.media.VideoTrackInfo;
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
	private final RotateVideo rotateVideo;
	Dimensions videoDimensionsDisplay = new Dimensions(0, 0);
	
	class SaveFrameCallback implements RenderCallback {
		@Override
		public void display(MediaPlayer mediaPlayer, ByteBuffer[] nativeBuffers, BufferFormat bufferFormat) {
			PImageFromIntBuffer sourceFrame = new PImageFromIntBuffer(videoDimensions.width, videoDimensions.height, nativeBuffers[0].asIntBuffer());
			VideoMeta videoMeta = new VideoMeta(duration(), timeSeconds(), widthDisplay(), heightDisplay());
			if (rotateVideo.hasRotation()) {
				frameReady.processFrame(new Frame(new PImage(rotateImage((BufferedImage) sourceFrame.getNative(), rotateVideo.degrees())), null, videoMeta));
			} else {
				frameReady.processFrame(new Frame(sourceFrame, null, videoMeta));
			}
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
			//			return new YUVABufferFormat(sourceWidth, sourceHeight);
		}
		
		@Override
		public void allocatedBuffers(ByteBuffer[] buffers) {
		}
	}
	
	public VideoWrapperVLCJ(Video2SlitScan p, String absolutePath, FrameReady frameReady, boolean startPlaying, RotateVideo rotateVideo) {
		this.p = p;
		this.frameReady = frameReady;
		this.playing = startPlaying;
		this.rotateVideo = rotateVideo;
		
		String[] mediaPlayerArgs = {};
		// BUG: vlc is error-ing trying to rotate 6144 × 10 ProRes video when applying a transform
		// we'll do the rotation ourselves for now
		//		if (rotateVideo.hasRotation()) {
		//			// vlc --transform-type one of: 90,180,270,hflip,vflip
		//			mediaPlayerArgs = new String[] { "--video-filter=transform", "--transform-type=" + rotateVideo.degreesString() };
		//		}
		
		MediaPlayerFactory factory = new MediaPlayerFactory(mediaPlayerArgs);
		m = factory.mediaPlayers().newEmbeddedMediaPlayer();
		m.videoSurface().set(factory.videoSurfaces().newVideoSurface(bufferFormat, saveFrame, true));
		
		m.events().addMediaEventListener(new MediaEventAdapter() {
			@Override
			public void mediaParsedChanged(Media media, MediaParsedStatus newStatus) {
				if (newStatus == DONE && media.info().videoTracks().size() > 0) {
					ready = true;
					VideoTrackInfo videoTrackInfo = media.info().videoTracks().get(0);
					int displayWidth = videoTrackInfo.width();
					int displayHeight = videoTrackInfo.height();
					videoDimensionsDisplay = new Dimensions(displayWidth, displayHeight);
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
		LOG.info("do stop");
		if (ready()) {
			playing = false;
			try {
				m.controls().stop();
			} catch (Exception e) {
				// ignore
			}
		}
	}
	
	public void dispose() {
		playing = false;
		m = null;
	}
	
	public void volume(int i) {
		// we don't need volume
	}
	
	@Override
	public void play() {
		LOG.info("do play");
		playing = true;
		if (ready()) {
			try {
				m.controls().play();
			} catch (Exception e) {
				// ignore
			}
		}
	}
	
	@Override
	public float duration() {
		if (ready()) {
			try {
				return durationSeconds;
			} catch (Exception e) {
				// ignore
			}
		}
		return 0;
	}
	
	@Override
	public void pause() {
		LOG.info("do pause");
		if (ready()) {
			try {
				m.controls().pause();
			} catch (Exception e) {
				// ignore
			}
		}
	}
	
	@Override
	public void jump(float f) {
		LOG.info("do jump: " + f);
		if (ready()) {
			try {
				m.controls().setTime(Math.round(f * 1000.0));
			} catch (Exception e) {
				// ignore
			}
		}
	}
	
	@Override
	public float timeSeconds() {
		if (ready()) {
			try {
				return m.status().position() * duration();
			} catch (Exception e) {
				// ignore
			}
		}
		return 0;
	}
	
	@Override
	public int widthCoded() {
		return rotateVideo.rotatedDimensions(videoDimensions).getWidth();
	}
	
	@Override
	public int heightCoded() {
		return rotateVideo.rotatedDimensions(videoDimensions).getHeight();
	}
	
	public int widthDisplay() {
		return rotateVideo.rotatedDimensions(videoDimensionsDisplay).getWidth();
	}
	
	public int heightDisplay() {
		return rotateVideo.rotatedDimensions(videoDimensionsDisplay).getHeight();
	}
	
	@Override
	public void speed(float playSpeed) {
		if (ready()) {
			try {
				m.controls().setRate(playSpeed);
			} catch (Exception e) {
				// ignore
			}
		}
	}
	
	public boolean ready() {
		return ready;
	}
}