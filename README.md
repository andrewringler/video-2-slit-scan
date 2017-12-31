# Video-2-Slit-Scan
Video-2-Slit-Scan is an desktop application to create a single (high resolution) static image from slits of video.

Download the latest build for [Mac](https://github.com/andrewringler/video-2-slit-scan/releases/latest). Video-2-Slit-Scan should also easily run on Windows and Linux, but I have not created and tested a build yet.

## Build Mac Bundle:
gradle createDmg

## New Developer Setup
 * Install Eclipse Neon
 * Install Gradle
 * run after-pull
 * Import Project Into Eclipse

## Create App Icon
 * Create PNG image 1024x1024
 * Convert to iconset folder with [Icon Generator](https://github.com/onmyway133/IconGenerator)
 * Tweak individual icons if need be
  rename icons to Apple's [latest scheme](https://developer.apple.com/library/content/documentation/GraphicsAnimation/Conceptual/HighResolutionOSX/Optimizing/Optimizing.html)
 * Rename folder to icon.iconset
 * Convert to icns file with
 `iconutil -c icns -o icon.icns icon.iconset`
  then copy into icon.icns into the doc/ directory
  
