package com.smart.pen.core.utils;

import com.smart.pen.core.model.FrameSizeObject;

import android.content.Context;

/**
 * 
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
	
	/**
	 * 获取画布可写区域尺寸
	 * @param obj
	 * @return
	 */
	public static FrameSizeObject getWindowSize(FrameSizeObject obj){
		if(obj != null){
			if(obj.sceneWidth > obj.sceneHeight){
				obj.windowWidth = obj.frameWidth;
				obj.windowHeight = (int)((float)obj.sceneHeight * ((float)obj.frameWidth / (float)obj.sceneWidth));
				
				obj.windowLeft = 0;
				obj.windowTop = (obj.frameHeight - obj.windowHeight) / 2;
			}else{
				obj.windowHeight = obj.frameHeight;
				obj.windowWidth = (int)((float)obj.sceneWidth * ((float)obj.windowHeight / (float)obj.sceneHeight));
				
				
				if(obj.windowWidth > obj.frameWidth){
					obj.windowHeight = (int)((float)obj.windowHeight * ((float)obj.frameWidth / (float)obj.windowWidth));
					obj.windowWidth = obj.frameWidth;
					
					obj.windowLeft = 0;
					obj.windowTop = (obj.frameHeight - obj.windowHeight) / 2;
				}else{
					obj.windowTop = 0;
					obj.windowLeft = (obj.frameWidth - obj.windowWidth) / 2;
				}
			}
		}
		return obj;
	}
}
