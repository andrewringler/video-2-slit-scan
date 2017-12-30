** Build Mac Bundle:
gradle createDmg

** New Developer Setup
- Install Eclipse Neon
- Install Gradle
- run after-pull
- Import Project Into Eclipse

** Create App Icon
- Create PNG image 1024x1024
- Convert to iconset folder with <https://github.com/onmyway133/IconGenerator>
- Tweak individual icons if need be
  rename icons to latest scheme <https://developer.apple.com/library/content/documentation/GraphicsAnimation/Conceptual/HighResolutionOSX/Optimizing/Optimizing.html>
- Rename folder to icon.iconset
- Convert to icns file with
  iconutil -c icns -o icon.icns icon.iconset
  copy into icon.icns into the doc/ directory
  