package com.andrewringler.slitscan;

import java.io.IOException;

import io.scif.FormatException;
import io.scif.ImageMetadata;
import io.scif.Metadata;
import io.scif.Plane;
import io.scif.Reader;
import io.scif.SCIFIO;
import io.scif.Writer;
import io.scif.io.Location;
import net.imagej.axis.Axes;

/*
 * Adapted from SCIFIO tutorials
 * https://github.com/scifio/scifio-tutorials/blob/master/core/src/main/java/io/scif/tutorials/core/T1dSavingImagePlanes.java
 * https://github.com/scifio/scifio-tutorials/blob/master/core/src/main/java/io/scif/tutorials/core/T1cReadingTilesGood.java
 */
public class StreamingImageTools {
	public static boolean createBlankImage(SCIFIO scifio, String outPath, int width, int height, ColorDepth colorDepth) {
		try {
			// 3-channel RGB, a synthetic image, specified entirely by this string
			String sampleImage = "8bit-signed&pixelType=int8&lengths=3," + width + "," + height + ",1&axes=Channel,X,Y,Time&planarDims=3.fake";
			if (colorDepth.isSixteenBit()) {
				//				sampleImage = "16bit-unsigned&pixelType=uint16&lengths=3," + width + "," + height + ",1&axes=Channel,X,Y,Time&planarDims=3.fake";
				sampleImage = "16bit-signed&pixelType=int16&lengths=3," + width + "," + height + ",1&axes=Channel,X,Y,Time&planarDims=3.fake";
			}
			
			// Clear the file if it already exists.
			final Location l = new Location(scifio.getContext(), outPath);
			if (l.exists())
				l.delete();
			
			// we use the reader to inspect the image we actually created
			final Reader reader = scifio.initializer().initializeReader(sampleImage);
			final Metadata meta = reader.getMetadata();
			
			// we will use the writer to fill the image with blank-ish data
			final Writer writer = scifio.initializer().initializeWriter(sampleImage, outPath);
			
			// Note that these initialize methods are used for convenience.
			// Initializing a reader and a writer requires that you set the source
			// and metadata properly. Also note that the Metadata attached to a writer
			// describes how to interpret the incoming Planes, but may not reflect
			// the image on disk - e.g. if planes were saved in a different order
			// than on the input image. For accurate Metadata describing the saved
			// image, you need to re-parse it from disk.
			
			// Anyway, now that we have a reader and a writer, we can save all the
			// planes. We simply iterate over each image, and then each plane, writing
			// the planes out in order.
			for (int imageIndex = 0; imageIndex < reader.getImageCount(); imageIndex++) {
				ImageMetadata iMeta = meta.get(imageIndex);
				final long optimalTileWidth = reader.getOptimalTileWidth(imageIndex);
				final long optimalTileHeight = reader.getOptimalTileHeight(imageIndex);
				final long[] offsets = new long[iMeta.getPlanarAxisCount()];
				final long[] extents = iMeta.getAxesLengthsPlanar();
				final int xAxis = iMeta.getAxisIndex(Axes.X);
				final int yAxis = iMeta.getAxisIndex(Axes.Y);
				final long tilesWide = (long) Math.ceil((double) iMeta.getAxisLength(Axes.X) / optimalTileWidth);
				final long tilesHigh = (long) Math.ceil((double) iMeta.getAxisLength(Axes.Y) / optimalTileHeight);
				
				// Now we can open each tile, one at a time, for each plane in this image
				for (int planeIndex = 0; planeIndex < iMeta.getPlaneCount(); planeIndex++) {
					for (int tileX = 0; tileX < tilesWide; tileX++) {
						for (int tileY = 0; tileY < tilesHigh; tileY++) {
							
							// These are pointers to the offsets in the current plane,
							// from where we will start reading the next tile
							offsets[xAxis] = tileX * optimalTileWidth;
							offsets[yAxis] = tileY * optimalTileHeight;
							
							// We also need to check the lengths of our tile, to see
							// if they would run outside the image - due to the plane
							// not being perfectly divisible by the tile dimensions.
							extents[xAxis] = Math.min(optimalTileWidth, iMeta.getAxisLength(Axes.X) - offsets[xAxis]);
							extents[yAxis] = Math.min(optimalTileHeight, iMeta.getAxisLength(Axes.Y) - offsets[yAxis]);
							
							// Finally we open the current plane, using an openPlane signature
							// that allows us to specify a sub-region of the current plane.
							Plane p = reader.openPlane(imageIndex, planeIndex, offsets, extents);
							
							// Here we would do any necessary processing of each tile's bytes.
							// In this, we'll just print out the plane and position.
							//							System.out.println("Image:" + (imageIndex + 1) + " Plane:" + (planeIndex + 1) + " Tile:" + ((tileX * tilesWide) + tileY + 1) + " -- " + p);
							
							writer.savePlane(imageIndex, planeIndex, p, offsets, extents);
							
							// NB: the openPlane signature we used creates a new plane each
							// time. If there are a significant number of tiles being read, it
							// may be more efficient to create a Plane ahead of time using the
							// reader.createPlane method, and then just reuse it for all tiles
							// of that size.
						}
					}
				}
				
				//				for (int j = 0; j < reader.getPlaneCount(i); j++) {
				//					writer.savePlane(i, j, reader.openPlane(i, j));
				//				}
			}
			
			// Note that this code is for illustration purposes only.
			// A more general solution would need higher level API that could account
			// for larger planes, etc..
			
			// close our components now that we're done. This is a critical step, as
			// many formats have footer information that is written when the writer is
			// closed.
			reader.close();
			writer.close();
			
			// That's it! There should be a new SCIFIOTutorial image in whichever
			// directory you ran this tutorial from.
			return true;
		} catch (FormatException e) {
			System.out.println(e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
		return false;
	}
}
