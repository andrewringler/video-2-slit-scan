package com.andrewringler.slitscan.jcodec;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.PictureHiBD;
import org.jcodec.common.model.Rect;
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.Transform;
import org.jcodec.scale.highbd.ColorUtilHiBD;
import org.jcodec.scale.highbd.TransformHiBD;

public class JCodecPictureRGB {
	final Picture picture;
	final PictureHiBD pictureHiBD;
	
	public JCodecPictureRGB(Picture picture, PictureHiBD pictureHiBD) {
		this.picture = picture;
		this.pictureHiBD = pictureHiBD;
	}
	
	private static PictureHiBD toRGB_HiBD(Picture src) {
		PictureHiBD rgb;
		if (src.getCrop() != null) {
			rgb = PictureHiBD.createCropped(src.getWidth(), src.getHeight(), ColorSpace.RGB, src.getCrop());
		} else {
			rgb = PictureHiBD.create(src.getWidth(), src.getHeight(), ColorSpace.RGB);
		}
		TransformHiBD transform = ColorUtilHiBD.getTransform(src.getColor(), ColorSpace.RGB);
		if (transform == null) {
			throw new UnsupportedOperationException("Transform not found for " + src.getColor());
		}
		transform.transform(src.toPictureHiBD(), rgb);
		return rgb;
	}
	
	private static Picture toRGB(Picture src) {
		if (src.isHiBD()) {
			return Picture.fromPictureHiBD(toRGB_HiBD(src));
		} else {
			Picture rgb;
			if (src.getCrop() != null) {
				rgb = Picture.createCropped(src.getWidth(), src.getHeight(), ColorSpace.RGB, src.getCrop());
			} else {
				rgb = Picture.create(src.getWidth(), src.getHeight(), ColorSpace.RGB);
			}
			Transform transform = ColorUtil.getTransform(src.getColor(), ColorSpace.RGB);
			if (transform == null) {
				// try an HiBD transform
				return Picture.fromPictureHiBD(toRGB_HiBD(src));
			}
			transform.transform(src, rgb);
			return rgb;
		}
	}
	
	public JCodecPictureRGB(Picture picture) {
		if (picture.isHiBD()) {
			this.pictureHiBD = toRGB_HiBD(picture);
		} else {
			this.pictureHiBD = null;
		}
		
		this.picture = toRGB(picture);
	}
	
	public boolean isHiBD() {
		return pictureHiBD != null;
	}
	
	public JCodecPictureRGB cloneCropped(Rect crop) {
		PictureHiBD pictureHiBDCropped = null;
		Picture pictureCropped = null;
		
		if (isHiBD()) {
			PictureHiBD copy = pictureHiBD.cropped();
			copy.setCrop(crop);
			pictureHiBDCropped = copy.cropped();
		} else {
			Picture copy = picture.cloneCropped();
			copy.setCrop(crop);
			pictureCropped = copy.cloneCropped();
		}
		
		return new JCodecPictureRGB(pictureCropped, pictureHiBDCropped);
	}
}
