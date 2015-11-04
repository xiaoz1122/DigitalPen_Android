package com.smart.pen.core.views;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.smart.pen.core.utils.BitmapUtil;
import com.smart.pen.core.utils.SystemUtil;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * 摄像头SurfaceView
 * @author Xiaoz
 * @date 2015年11月2日 上午12:00:43
 *
 * Description
 */
public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback, PreviewCallback{
	public static final String TAG = CameraSurfaceView.class.getSimpleName();
	
	private int mWidth = 320;
	private int mHeight = 240;
	private int currOrientation;
	private Bitmap mLastBitmap = null;
	
    private Camera mCamera;//声明相机
    private SurfaceHolder mCameraHolder;

	public CameraSurfaceView(Context context) {
        super(context);
        initView(context);
    }
    public CameraSurfaceView(Context context, AttributeSet attrs){
        super(context, attrs);
        initView(context);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
        initView(context);
    }

    private void initView(Context context){
    	currOrientation = getResources().getConfiguration().orientation;
    	mCameraHolder = this.getHolder();
        mCameraHolder.addCallback(this);//添加回调
        mCameraHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }
    
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		//mCamera.stopPreview();
        //mCamera.setPreviewCallback(this);//添加预览回调
        //mCamera.startPreview();
	}
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.v(TAG,"surfaceCreated");
        if(mCamera == null) {
            int faceId = SystemUtil.getFaceCameraIndex();
            if(faceId >= 0){
                mCamera = Camera.open(faceId);
            }else {
                mCamera = Camera.open();
            }
            Camera.Parameters params = mCamera.getParameters();
        	params.setPreviewSize(mWidth,mHeight);
            mCamera.setParameters(params);
        	mCamera.setDisplayOrientation(currOrientation == Configuration.ORIENTATION_PORTRAIT?90:0);
        	
            try {
                mCamera.setPreviewDisplay(holder);//通过surfaceview显示取景画面
                mCamera.startPreview();//开始预览
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.v(TAG,"surfaceDestroyed");
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
        holder = null;
        
        if(mLastBitmap != null){
        	if(!mLastBitmap.isRecycled())mLastBitmap.recycle();
        	mLastBitmap = null;
        }
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		Log.v(TAG,"onPreviewFrame");
		new DecodeTask(data).execute();
	}
	
	/**获取视频宽**/
	public int getCameraWidth(){
		return mWidth;
	}
	
	/**获取视频高**/
	public int getCameraHeight(){
		return mHeight;
	}
	
	public void takePicture(){
		if(mCamera != null){
	        mCamera.setOneShotPreviewCallback(this);
		}
	}
	
	/**获取捕获到的图像**/
	public Bitmap getCameraBitmap(){
		if(mLastBitmap != null){
			return mLastBitmap;
		}
		return null;
	}
	
	private class DecodeTask extends AsyncTask<Void, Void, Void>{
		private byte[] mData;
		//构造函数
		DecodeTask(byte[] data){
		    this.mData = data;
		}
		@Override
		protected Void doInBackground(Void... params) {
			if(mData != null){
				YuvImage image = new YuvImage(mData, ImageFormat.NV21, mWidth, mHeight, null);
				ByteArrayOutputStream os = new ByteArrayOutputStream(mData.length);
			    if(image.compressToJpeg(new Rect(0, 0, mWidth, mHeight), 100, os)){
			    	byte[] tmp = os.toByteArray();
			    	Bitmap bmp = BitmapFactory.decodeByteArray(tmp, 0, tmp.length);
					if(currOrientation == Configuration.ORIENTATION_PORTRAIT){
						bmp = BitmapUtil.adjustBitmapRotation(bmp, -90);
					}
					if(bmp != null)mLastBitmap = bmp;
			    }
			}
		  return null;
		}
        
    }   
}
