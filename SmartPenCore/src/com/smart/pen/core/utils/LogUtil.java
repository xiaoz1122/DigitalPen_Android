package com.smart.pen.core.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.annotation.SuppressLint;
import android.os.Environment;

/**
 * 
 * @author Xiaoz
 * @date 2015年9月23日 上午10:09:54
 *
 * Description
 */
public class LogUtil {
	@SuppressLint("SimpleDateFormat")
	private static SimpleDateFormat mTimeformat = new SimpleDateFormat("yyyyMMddHHmmss");
    private static Writer mWriter;

	public static void addLog(String log){
		addLog(log,null);
	}
	public static void addLog(String log,String dir){
		String path = Environment.getExternalStorageDirectory().getPath() + "/";
		if(dir == null){
			dir = "com.smart.pen.core";
		}
		path = path + dir+"/";
		
		//检查文件夹是否存在
		FileUtils.isDirectory(path);
		
		Date curDate = new Date(System.currentTimeMillis());
		String logDate = mTimeformat.format(curDate);
		String fileName = "log_"+logDate.substring(0, 12)+".txt";
		
		try {
			mWriter = new BufferedWriter(new FileWriter(path + fileName,true), 256);
			mWriter.write(logDate.substring(10, 14)+"	");
			mWriter.write(log);
	        mWriter.write("\r\n");
	        mWriter.flush(); 
	        mWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
