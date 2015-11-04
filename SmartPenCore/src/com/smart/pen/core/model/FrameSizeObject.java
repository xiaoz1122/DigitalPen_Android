package com.smart.pen.core.model;
/**
 * 
 * @author Xiaoz
 * @date 2015年8月7日 上午10:29:20
 *
 * Description
 */
public class FrameSizeObject {
	//显示画布的区域尺寸
	/**
	 * 显示画布区域宽
	 */
	public int frameWidth;
	/**
	 * 显示画布区域高
	 */
	public int frameHeight;
	
	
	//纸张尺寸
	/**
	 * 纸张可写区域宽
	 */
	public int sceneWidth;
	/**
	 * 纸张可写区域高
	 */
	public int sceneHeight;
	

	/**
	 * 显示可写区域宽
	 */
	public int windowWidth;
	
	/**
	 * 显示可写区域高
	 */
	public int windowHeight;
	public int windowLeft;
	public int windowTop;
	
	/**
	 * 缩放宽
	 */
	public int zoomWidth;
	
	/**
	 * 缩放高
	 */
	public int zoomHeight;
	
	
	/**
	 * 设置画布可写区域尺寸
	 * @return
	 */
	public void initWindowSize(){
		if(sceneWidth > sceneHeight){
			windowWidth = frameWidth;
			windowHeight = (int)((float)sceneHeight * ((float)frameWidth / (float)sceneWidth));
			
			if(windowHeight > frameHeight){
				windowWidth = (int)((float)windowWidth * ((float)frameHeight / (float)windowHeight));
				windowHeight =  frameHeight;

				windowTop = 0;
				windowLeft = (frameWidth - windowWidth) / 2;
			}else{
				windowLeft = 0;
				windowTop = (frameHeight - windowHeight) / 2;
			}
		}else{
			windowHeight = frameHeight;
			windowWidth = (int)((float)sceneWidth * ((float)windowHeight / (float)sceneHeight));
			
			if(windowWidth > frameWidth){
				windowHeight = (int)((float)windowHeight * ((float)frameWidth / (float)windowWidth));
				windowWidth = frameWidth;
				
				windowLeft = 0;
				windowTop = (frameHeight - windowHeight) / 2;
			}else{
				windowTop = 0;
				windowLeft = (frameWidth - windowWidth) / 2;
			}
		}
	}
	
	
	/**
	 * 设置窗口缩放尺寸，需要在initWindowSize之后执行
	 * @param refSize
	 * @return
	 */
	public void setWindowZoomSize(int refSize){
		zoomWidth = windowWidth;
		zoomHeight = windowHeight;
	
	    if(windowWidth > windowHeight){
	        if(windowHeight > refSize){
	        	zoomHeight = refSize;
	        	zoomWidth = (int)((float)refSize / (float)windowHeight * windowWidth);
	        }
	    }else{
	        if(windowWidth > refSize){
	        	zoomWidth = refSize;
	        	zoomHeight = (int)((float)refSize / (float)windowWidth * windowHeight);
	        }
	    }
	}
}
