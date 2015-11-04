package com.smart.pen.core.utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;

/**
 * Bitmap处理工具
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
	 * 调整图片角度
	 * @param bm
	 * @param orientationDegree
	 * @return
	 */
	public static Bitmap adjustBitmapRotation(Bitmap bm, int rotate){

		Matrix m = new Matrix();
		m.setRotate(rotate);
		float targetX, targetY;
		if (rotate == -90) {
			targetX = 0;
			targetY = bm.getWidth();
		}else if (rotate == 90) {
			targetX = bm.getHeight();
			targetY = 0;
		} else {
			targetX = bm.getHeight();
			targetY = bm.getWidth();
		}
	    m.postTranslate(targetX, targetY);

	    Bitmap bm1 = Bitmap.createBitmap(bm.getHeight(), bm.getWidth(), Bitmap.Config.ARGB_8888);
	    Paint paint = new Paint();
	    Canvas canvas = new Canvas(bm1);
	    canvas.drawBitmap(bm, m, paint);

	    return bm1;
	}
	
	/**
	 * 合并两张bitmap为一张
	 * @param background
	 * @param foreground
	 * @param fLeft
	 * @param fTop
	 * @return
	 */
	public static Bitmap combineBitmap(Bitmap background, Bitmap foreground,int fLeft,int fTop) { 
		return combineBitmap(background,foreground,fLeft,fTop,Bitmap.Config.RGB_565);
	}
	
	public static Bitmap combineBitmap(Bitmap background, Bitmap foreground,int fLeft,int fTop,Bitmap.Config config) {  
	    if (background == null) {  
	        return null;  
	    }  
	    int bgWidth = background.getWidth();  
	    int bgHeight = background.getHeight();
	    Bitmap newmap = Bitmap.createBitmap(bgWidth, bgHeight, config);  
	    Canvas canvas = new Canvas(newmap);  
	    canvas.drawBitmap(background, 0, 0, null);  
	    canvas.drawBitmap(foreground, fLeft,fTop, null);  
	    canvas.save(Canvas.ALL_SAVE_FLAG);  
	    canvas.restore();  
	    return newmap;  
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
	
	/**
	 * 获取角度
	 * @param filepath
	 * @return
	 */
	public static int getExifOrientation(String filepath) {
       int degree = 0;
       ExifInterface exif = null;
       try {
           exif = new ExifInterface(filepath);
       } catch (IOException ex) {
          // MmsLog.e(ISMS_TAG, "getExifOrientation():", ex);
       }

       if (exif != null) {
           int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
           if (orientation != -1) {
               // We only recognize a subset of orientation tag values.
               switch (orientation) {
               case ExifInterface.ORIENTATION_ROTATE_90:
                   degree = 90;
                   break;
               case ExifInterface.ORIENTATION_ROTATE_180:
                   degree = 180;
                   break;
               case ExifInterface.ORIENTATION_ROTATE_270:
                   degree = 270;
                   break;
               default:
                   break;
               }
           }
       }
       return degree;
   }
}
