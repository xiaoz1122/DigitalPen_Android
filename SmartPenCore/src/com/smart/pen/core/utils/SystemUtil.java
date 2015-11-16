package com.smart.pen.core.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;

/**
 * 系统工具
 * @author Xiaoz
 * @date 2015年8月6日 下午5:19:07
 *
 * Description
 */
public class SystemUtil {
	
	/**dip转换为px**/
	public static int dip2px(Context context, float dipValue){   
        final float scale = context.getResources().getDisplayMetrics().density;   
        return (int)(dipValue * scale + 0.5f);   
	}

	/**检查是否支持摄像头**/
    public static boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            return true;
        } else {
            return false;
        }
    }
    
    /**获取前置摄像头索引**/
    public static int getFaceCameraIndex(){
        int result = -1;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();
        for(int i = 0;i < cameraCount; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if(cameraInfo.facing  == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = 1;
                break;
            }
        }
        return result;
    }
}
