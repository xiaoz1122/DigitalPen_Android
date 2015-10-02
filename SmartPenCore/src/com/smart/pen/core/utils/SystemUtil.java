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
}
