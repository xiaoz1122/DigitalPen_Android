package com.smart.pen.core.utils;

import java.io.File;

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

}
