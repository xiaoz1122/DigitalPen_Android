package com.smart.pen.core.utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;

/**
 * 
 * @author Xiaoz
 * @date 2015年10月2日 下午8:08:32
 *
 * Description
 */
public class BitmapUtil {
	
	/**
	 * 从一个uri中获取图片数据
	 * @param context
	 * @param uri
	 * @return
	 */
	public static Bitmap decodeUriAsBitmap(Context context,Uri uri) {
		Bitmap bitmap = null;
		try {
			bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return bitmap;
	}
	
	/**
	 * 安全的获取并压缩一张图片
	 * 
	 * @param uri
	 * @param width
	 * @param height
	 * @return
	 * @throws FileNotFoundException
	 */
	public static Bitmap safeDecodeStream(Context context,Uri uri, int width, int height)
			throws FileNotFoundException {
		int scale = 1;
		BitmapFactory.Options options = new BitmapFactory.Options();
		android.content.ContentResolver resolver = context.getContentResolver();

		if (width > 0 || height > 0) {
			// Decode image size without loading all data into memory
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(
					new BufferedInputStream(resolver.openInputStream(uri),
							16 * 1024), null, options);

			int w = options.outWidth;
			int h = options.outHeight;
			while (true) {
				if ((width > 0 && w / 2 < width)
						|| (height > 0 && h / 2 < height)) {
					break;
				}
				w /= 2;
				h /= 2;
				scale *= 2;
			}
		}

		// Decode with inSampleSize option
		options.inJustDecodeBounds = false;
		options.inSampleSize = scale;
		return BitmapFactory.decodeStream(
				new BufferedInputStream(resolver.openInputStream(uri),
						16 * 1024), null, options);
	}
	
	/**
	 * 图片缩放 按照refSize自动判断要按高度缩放还是宽度
	 * @param bitmap
	 * @param refSize
	 * @return
	 */
	public static Bitmap zoomBitmap(Bitmap bitmap, int refSize) {
		int newWidth,newHeight;
        int width = newWidth = bitmap.getWidth();
        int height =  newHeight = bitmap.getHeight();
        if(width > height){
            if(height > refSize){
            	newHeight = refSize;
            	newWidth = (int)((float)refSize / (float)height * width);
            }
        }else{
            if(width > refSize){
            	newWidth = refSize;
            	newHeight = (int)((float)refSize / (float)width * height);
            }
        }
        return zoomBitmap(bitmap,newWidth,newHeight);
	}
	
	/**
	 * 图片缩放
	 * **/
	public static Bitmap zoomBitmap(Bitmap bitmap, int width, int height) {
		int w = bitmap.getWidth();
		int h = bitmap.getHeight();
		if(w != width || h != height){
			Matrix matrix = new Matrix();
			float scaleWidth = ((float) width / w);
			float scaleHeight = ((float) height / h);
			matrix.postScale(scaleWidth, scaleHeight);
			Bitmap newbmp = Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);
			return newbmp;
		}else{
			return bitmap;
		}
	}
	
	/**
	 * bitmap转Bytes
	 * @param bm
	 * @return
	 */
	public static byte[] bitmap2Bytes(Bitmap bm,int quality) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        return baos.toByteArray();
    }
}
