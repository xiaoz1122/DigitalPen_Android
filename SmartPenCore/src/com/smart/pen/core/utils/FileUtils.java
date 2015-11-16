package com.smart.pen.core.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Environment;

/**
 * 
 * @author Xiaoz
 * @date 2015年9月23日 上午10:19:58
 *
 * Description
 */
public class FileUtils {
	
	/**
     * 遍历 "system/etc/vold.fstab” 文件，获取全部的Android的挂载点信息
     * 
     * @return
     */
    private static ArrayList<String> getDevMountList() {
        String[] toSearch = FileUtils.readFile("/system/etc/vold.fstab").split(" ");
        ArrayList<String> out = new ArrayList<String>();
        for (int i = 0; i < toSearch.length; i++) {
            if (toSearch[i].contains("dev_mount")) {
                if (new File(toSearch[i + 2]).exists()) {
                    out.add(toSearch[i + 2]);
                }
            }
        }
        return out;
    }
	
    /**
     * 获取扩展SD卡存储目录
     * 
     * 如果有外接的SD卡，并且已挂载，则返回这个外置SD卡目录
     * 否则：返回内置SD卡目录
     * 
     * @return
     */
    public static String getExternalSdCardPath() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File sdCardFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
            return sdCardFile.getAbsolutePath();
        }
  
        String path = null;
        File sdCardFile = null;
        ArrayList<String> devMountList = getDevMountList();
        for (String devMount : devMountList) {
            File file = new File(devMount);
            if (file.isDirectory() && file.canWrite()) {
                path = file.getAbsolutePath();
                File testWritable = new File(path, "test_" + getDateFormatName());
                if (testWritable.mkdirs()) {
                    testWritable.delete();
                } else {
                    path = null;
                }
            }
        }
  
        if (path != null) {
            sdCardFile = new File(path);
            return sdCardFile.getAbsolutePath();
        }
        return null;
    }
	
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

    public static String getDateFormatName(){
        return getDateFormatName("yyyyMMddHHmmss");
    }
    
    @SuppressLint("SimpleDateFormat")
	public static String getDateFormatName(String pattern){
        SimpleDateFormat date = new SimpleDateFormat(pattern);
        String filename = String.valueOf(date.format(System.currentTimeMillis()));
        return filename;
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
            result = saveByteData(imageData,savePath);
        }
		return result;
	}
	
	/**
	 * 保存byte[]文件
	 * @param data
	 * @param savePath
	 * @return
	 */
	public static boolean saveByteData(byte[] data,String savePath){
		boolean result = false;
		File saveFile = new File(savePath);
        try {
        	FileOutputStream fos = new FileOutputStream(saveFile);
        	fos.write(data);
        	fos.close();
        	
        	result = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
		return result;
	}

	
	/**
     * read file
     * 
     * @param filePath
     * @param charsetName The name of a supported {@link java.nio.charset.Charset </code>charset<code>}
     * @return if file not exist, return null, else return content of file
     * @throws RuntimeException if an error occurs while operator BufferedReader
     */
    public static String readFile(String filePath) {
        String fileContent = "";
        File file = new File(filePath);
        if (file == null || !file.isFile()) {
            return null;
        }
 
        BufferedReader reader = null;
        try {
            InputStreamReader is = new InputStreamReader(new FileInputStream(file));
            reader = new BufferedReader(is);
            String line = null;
            while ((line = reader.readLine()) != null) {
                fileContent += line + " ";
            }
            reader.close();
            return fileContent;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return fileContent;
    }
}
