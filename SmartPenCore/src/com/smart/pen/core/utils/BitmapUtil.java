package com.smart.pen.core.utils;

import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap;
import android.graphics.Matrix;

/**
 * 
 * @author Xiaoz
 * @date 2015年10月2日 下午8:08:32
 *
 * Description
 */
public class BitmapUtil {
	
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
