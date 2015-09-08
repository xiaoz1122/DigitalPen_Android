package com.smart.pen.core.symbol;
/**
 * 
 * @author Xiaoz
 * @date 2015年6月17日 下午2:56:03
 *
 * Description
 */
public enum LocationState {
	/** 已经确定第一个坐标 **/
	FirstComp,
	/** 已经完成定位 **/
	SecondComp,
	/** 没有定位 **/
	DontLocation,
	/** 定位范围过小 **/
	LocationSmall
}
