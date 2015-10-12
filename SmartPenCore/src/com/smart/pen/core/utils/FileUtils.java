package com.smart.pen.core.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.graphics.Bitmap;

/**
 * 
 * @author Xiaoz
 * @date 2015年9月23日 上午10:19:58
 *
 * Description
 */
public class FileUtils {
	/**
	 * 检查文件夹是否存在，不存在则创建
	 * @param dir
	 * @return
	 */
	public static boolean isDirectory(String dirPath){
        File file = new File(dirPath);
        file.mkdirs();
        return file.isDirectory();
    }
	
	/**
	 * 保存Bitmap文件
	 * @param bitmap
	 * @param savePath
	 * @return
	 */
	public static boolean saveBitmap(Bitmap bitmap,String savePath){
		boolean result = false;
		if(bitmap != null) {
            byte[] imageData = BitmapUtil.bitmap2Bytes(bitmap,100);
            File saveFile = new File(savePath);
            try {
            	FileOutputStream fos = new FileOutputStream(saveFile);
            	fos.write(imageData);
            	fos.close();
            	
            	result = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
		return result;
	}

}
