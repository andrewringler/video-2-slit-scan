package com.andrewringler.slitscan.ffmpeg;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.andrewringler.slitscan.Video2SlitScan;

public class FFMPEGBinary {
	private static final Logger LOG = LoggerFactory.getLogger(FFMPEGBinary.class);
	
	public static Path ffmpegBin(Video2SlitScan p) {
		String os = System.getProperty("os.name").toLowerCase();
		String nativeFolder = "ffmpeg-macos";
		if (os.contains("windows")) {
			nativeFolder = "ffmpeg-windows";
		}
		if (os.contains("linux")) {
			nativeFolder = "ffmpeg-linux-amd64";
		}
		
		File ffmpeg = new File(p.dataPath("") + "/../" + nativeFolder);
		if (ffmpeg.isDirectory()) {
			return Paths.get(ffmpeg.getPath());
		}
		
		ffmpeg = new File(p.dataPath(nativeFolder));
		if (ffmpeg.isDirectory()) {
			return Paths.get(ffmpeg.getPath());
		}
		
		ffmpeg = new File(p.dataPath("") + "/../ffmpeg/" + nativeFolder);
		if (ffmpeg.isDirectory()) {
			return Paths.get(ffmpeg.getPath());
		}
		
		ffmpeg = new File(p.dataPath("") + "/../lib/ffmpeg/" + nativeFolder);
		if (ffmpeg.isDirectory()) {
			return Paths.get(ffmpeg.getPath());
		}
		
		LOG.error("Unable to find ffmpeg native binaries");
		throw new RuntimeException("Unable to find ffmpeg native binaries, exiting…");
	}
}
