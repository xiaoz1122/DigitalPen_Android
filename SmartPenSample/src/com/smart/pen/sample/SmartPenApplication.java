package com.smart.pen.sample;

import com.smart.pen.core.PenApplication;

/**
 * 
 * @author Xiaoz
 * @date 2015年6月12日 上午11:39:48
 *
 * Description
 */
public class SmartPenApplication extends PenApplication{
    private static SmartPenApplication instance = null;
    
	 @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static SmartPenApplication getInstance() {
        return instance;
    }
}
