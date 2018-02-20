# Video-2-Slit-Scan
Video-2-Slit-Scan allows you to create a slit-scan image from a video. It provides a graphical interface for adjusting slit position and size. Video-2-Slit-Scan can support very large videos with modest RAM since it streams in the input video and writes out the output image to disk in chunks.

![Slit-scan generated from Wellspring Fords video](documentation/2017-wellspring-fords.jpg)
*slit-scan generated from [Wellspring Fords video](https://andrewringler.com/2017-11-wellspring-fords/), 2017*

## Quickstart
### Install
#### macOS
   * Download [Video-2-Slit-Scan for Mac v0.2.3](https://github.com/andrewringler/video-2-slit-scan/releases/download/v0.2.3/video-2-slit-scan-0.2.3.dmg) (107 mb)
   * Double-click the downloaded file and drag the `Video-2-Slit-Scan` icon to the Applications folder link

*Tested on macOS Sierra 10.12.6. DMG comes bundled with Java 1.8.0_91.*

#### Windows
   * Install [Java 8 JRE](https://www.java.com/en/download/manual.jsp) or later. Be sure to install the 64-bit version if you have 64-bit Windows.
   * Download the [Video-2-Slit-Scan for Windows v0.2.3](https://github.com/andrewringler/video-2-slit-scan/releases/download/v0.2.3/video-2-slit-scan-0.2.3-windows.zip) (49.6 mb)

#### Linux
*Video-2-Slit-Scan is written in [Processing](https://processing.org/) / [Java](https://java.com) / [Gradle](https://gradle.org/) / [Eclipse](https://www.eclipse.org/) so it should be able to run on Linux, but I have not yet been able to verify it working do to issues with the [Processing Video library](https://github.com/processing/processing-video/issues/86) on arm64.*

See [all Releases and Release Notes](https://github.com/andrewringler/video-2-slit-scan/releases).

## Usage
 * Launch the App
 * Click `Open Video File`, choose a video
 * Click `Generate Slit-Scan Image`, a slit-scan image with default settings will saved to your Desktop

## Screenshots
![app screenshot](documentation/ScreenShot2017-12-30Glacier.jpg)

![app screenshot](documentation/ScreenShot2017-06-21.jpg)

## Features
 * Given an input video, create a single output TIF grabbing a single vertical slice from each frame
 * Adjust position and size of slit
 * Manually adjust slit position as video plays
 * Create moving slits over time by specifying keyframes to interpolate between.
 * **Performance**. *Video-2-Slit-Scan* Should support very large input videos and very large output images. Videos are streamed in as needed, output images are not stored in memory but instead written to disk in chunks. *This allow allows previewing the output image using your OS image viewer during generation.*

## Background
Slit-scan photography, imaging and cinematography have a long history in film and digitally. This app allows you to experiment with one slit-scan technique digitally (converting a video to a single image). This app was originally commissioned by [Jan Kubasiewicz](http://jankuba.com/) who has created many beautiful works exploring various slit-scan techniques.

## Developer Notes
### New Developer Setup
 * Install Eclipse Neon
 * Install Gradle
 * run after-pull.sh
 * Import Project Into Eclipse
 
### Build Installers:
    gradle createDmg
    gradle createExe
    gradle distTar
    
#### Debug Windows build.
To get additional JVM logging, launch Video-2-Slit-Scan from the Command Prompt with l4j flag. This will generate an extra launch4j log file with uncaught Runtime Exceptions.

    video-2-slit-scan.exe --l4j-debug-all
    
#### Debug Mac build.
To get additional logging, control-click on application icon, click `Show Package Contents`, browse to Contents > MacOS and double-click `JavaAppLauncher`. Logging will show up in the `Terminal` window.


### Create App Icons
#### Mac
 * Create PNG image 1024x1024
 * Convert to iconset folder with [Icon Generator](https://github.com/onmyway133/IconGenerator)
 * Tweak individual icons if need be
  rename icons to Apple's [latest scheme](https://developer.apple.com/library/content/documentation/GraphicsAnimation/Conceptual/HighResolutionOSX/Optimizing/Optimizing.html)
 * Rename folder to icon.iconset
 * Convert to icns file with
 `iconutil -c icns -o icon.icns icon.iconset`
  then copy into icon.icns into the doc/ directory
  
#### Windows
 * Create PNG images (see [windows docs](https://msdn.microsoft.com/en-us/library/windows/desktop/dn742485%28v=vs.85%29.aspx) for latest sizes), currently 16,32,48,256
 * Convert to .ico using ImageMagick `convert icon-16.png icon-32.png icon-48.png icon-256.png icon.ico`
 * Copy .ico file to doc/ directory
  
