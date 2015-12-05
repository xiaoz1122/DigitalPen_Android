package com.smart.pen.core.module;

import android.graphics.Bitmap;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.smart.pen.core.symbol.RecordLevel;
import com.smart.pen.core.utils.BitmapUtil;
import com.smart.pen.core.utils.FFMergePictureUtils;
import com.smart.pen.core.utils.FileUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 图片录制工具
 * Created by Xiaoz on 15/9/11.
 */
public class ImageRecordModule {
    public static final String TAG = ImageRecordModule.class.getSimpleName();

    private static final int MSG_RECORD_SECONDS = 1001;

    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;

    // 设置音频采样率，44100是目前的标准，但是某些设备仍然支持22050,16000,11025
    private static final int AUDIO_RATE_HZ = 16000;

    // 设置音频的录制的声道CHANNEL_IN_STEREO为双声道，CHANNEL_CONFIGURATION_MONO为单声道
    private static final int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_MONO;

    // 音频数据格式:PCM 16位每个样本。保证设备支持。PCM 8位每个样本。不一定能得到设备支持。
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private String mSaveVideoDir = "/";
    private String mSavePhotoDir = "/";
    
    /**计时器计数**/
    private int mTimerNum = 0;

    private int mRecordLevel = RecordLevel.level_13;
    private int mQuality = 60;
    
    /**输出视频尺寸**/
    private int mVideoWidth,mVideoHeight;
    /**输入图片尺寸**/
    private int mInputWidth,mInputHeight;

    /**保存文件名称的前缀**/
    private String mNamePrefix = "MC";

    /**计时线程**/
    private ScheduledExecutorService mTimerThreadExecutor;

    /**缓存线程**/
    private ExecutorService mCacheThreadExecutor;

    private AudioRecord mAudioRecord;
    private FFMergePictureUtils mFFMergePictureUtils;
    
    /**单个图片数据缓存**/
    private ByteBuffer mImageBuffer;
    /**单个图片临时缓存**/
    private Bitmap mTmpBitmap;
    
    private ArrayList<byte[]> mAudioBuffers = new ArrayList<byte[]>();
    private ArrayList<byte[]> mImageBuffers = new ArrayList<byte[]>();

    private boolean isPause;
    private boolean isRecording = false;
    private ImageRecordInterface mGetImageInterface;

    public ImageRecordModule(ImageRecordInterface _interface){
        mGetImageInterface = _interface;
    }

    public void init(){
        mAudioBuffers.clear();
        mImageBuffers.clear();
        isPause = false;

        if(mTimerThreadExecutor == null || mTimerThreadExecutor.isShutdown())
            mTimerThreadExecutor = Executors.newScheduledThreadPool(1);

        if(mCacheThreadExecutor == null || mCacheThreadExecutor.isShutdown())
            mCacheThreadExecutor = Executors.newFixedThreadPool(1);//写入缓存线程定长1，防止某些进程过快导致帧顺序错乱

        if(mFFMergePictureUtils == null)
            mFFMergePictureUtils = new FFMergePictureUtils();

        if(mAudioRecord == null)
            initAudioRecord();
    }


    private void initAudioRecord() {
        int bufferSize = AudioRecord.getMinBufferSize(AUDIO_RATE_HZ, AUDIO_CHANNEL, AUDIO_FORMAT);
        Log.v(TAG, "initAudioRecord minbuffer size:" + bufferSize);
        
        mAudioRecord = new AudioRecord(
                AUDIO_SOURCE,
                AUDIO_RATE_HZ,
                AUDIO_CHANNEL,
                AUDIO_FORMAT,
                bufferSize);
    }
    
    /**
     * 释放资源
     */
    public void releaseImageRes(){
    	if(mImageBuffer != null){
        	mImageBuffer.clear();
        	mImageBuffer = null;
        }
        
        if(mTmpBitmap != null){
        	Bitmap bmp = mTmpBitmap;
        	mTmpBitmap = null;
        	
        	if(bmp != null){
        		if(!bmp.isRecycled())bmp.recycle();
        		bmp = null;
        	}
        }
    }
    
    /**设置视频保存文件夹**/
    public void setSaveVideoDir(String value){
    	this.mSaveVideoDir = value;
    }
    
    /**设置图片保存文件夹**/
    public void setSavePhotoDir(String value){
    	this.mSavePhotoDir = value;
    }
    
    /**设置压缩质量**/
    public void setQuality(int value){
    	this.mQuality = value;
    }
    
    /**设置录制文件名称前缀**/
    public void setNamePrefix(String value){
    	this.mNamePrefix = value;
    }
    
    /**设置录制质量级别**/
    public void setRecordLevel(int level){
        this.mRecordLevel = level;
    }

    /**获取录制质量级别**/
    public int getRecordLevel(){
        return mRecordLevel;
    }
    
    /**获取是否正在录制**/
    public boolean getIsRecording(){
    	return isRecording;
    }

    /**设置暂停**/
    public void setIsPause(boolean value){
        isPause = value;
    }
    
    /**获取是否暂停**/
    public boolean getIsPause(){
        return isPause;
    }
    

    /**
     * 设置录制尺寸信息，输入图片尺寸与输出视频尺寸相同
     * @param inWidth 输入宽
     * @param inHeight 输入高
     * @return
     */
    public boolean setRecordSize(int inImageWidth,int inImageHeight){
    	return setRecordSize(inImageWidth,inImageHeight,0,0);
    }

    /**
     * 设置录制尺寸信息
     * @param inWidth			输入图片宽
     * @param inHeight			输入图片高
     * @param outVideoWidth		输出视频宽
     * @param outVideoHeight	输出视频高
     * @return
     */
    public boolean setRecordSize(int inImageWidth,int inImageHeight,int outVideoWidth,int outVideoHeight){
    	boolean result = false;
    	
    	this.mInputWidth = inImageWidth;
    	this.mInputHeight = inImageHeight;

        this.mVideoWidth = outVideoWidth > 0?outVideoWidth:inImageWidth;
        this.mVideoHeight = outVideoHeight > 0?outVideoHeight:inImageHeight;
        
        releaseImageRes();
        
        try{
	        mImageBuffer = ByteBuffer.allocateDirect(mInputWidth * mInputHeight * 4);
	        mTmpBitmap = Bitmap.createBitmap(mInputWidth, mInputHeight, Bitmap.Config.ARGB_8888);
	        result = true;
        }catch(Exception e){
        	e.printStackTrace();
        	result = false;
        }
        
        return result;
    }

    /**
     * 获取输出视频宽度
     * @return
     */
    public int getVideoWidth(){
        return mVideoWidth;
    }

    /**
     * 获取输出视频高度
     * @return
     */
    public int getVideoHeight(){
        return mVideoHeight;
    }

    /**
     * 开始录制
     */
    public boolean startRecord(){
        if(isRecording)return true;
        init();

        int flag = startRecordHnadler();
        if(flag == 0) {
            isRecording = true;
            //开启获取声音数据线程
            new GetAudioTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            
            //运行计时器
            int gap = 1000 / RecordLevel.getFrameRate(mRecordLevel);
            mTimerThreadExecutor.scheduleAtFixedRate(mRecordTimerTask, 0, gap, TimeUnit.MILLISECONDS);
            
            //开始合并视频线程
            new MergeVideoTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            return true;
        }
        return false;
    }

    /**
     * 结束录制<br />
     * 结束后并不表示录制完全结束，需要等待ImageRecordInterface.videoCodeState返回的progress=100后才表示完全压缩完成。<br />
     * 如果录制完成后需等待压缩时间很长，那么可能导致音视频时间不同步，需降低RecordLevel参数
     */
    public void endRecord(){
        isRecording = false;
        isPause = false;
        mTimerThreadExecutor.shutdownNow();
    }

    /**
     * 保存截图
     */
    public boolean saveSnapshot(){
    	String savePath = mSavePhotoDir + FileUtils.getDateFormatName() + ".jpg";
    	return saveSnapshot(savePath);
    }
    
    /**
     * 保存截图
     * @param savePath
     * @return
     */
    public boolean saveSnapshot(String savePath){
        boolean result = false;
        
        ByteBuffer buffer = null;
        Bitmap bmp = null;
        try{
        	//监听Exception 防止createBitmap占用内存过大导致oom
        	buffer = ByteBuffer.allocateDirect(mInputWidth * mInputHeight * 4);
        	bmp = Bitmap.createBitmap(mInputWidth, mInputHeight, Bitmap.Config.ARGB_8888);
        }catch(IllegalArgumentException e){
        	e.printStackTrace();
        	buffer = null;
        }catch(Exception e){
        	e.fillInStackTrace();
        	buffer = null;
        }
        
        if(buffer != null){
        	if(bmp != null){
		        int len = mGetImageInterface.fillImageBuffer(buffer);
		        if(len > 0){
		        	//将buffer的下一读写位置置为0
		        	buffer.position(0);
		        	bmp.copyPixelsFromBuffer(buffer);
		        	result = FileUtils.saveBitmap(bmp, savePath);
		        }
		        bmp.recycle();
		        bmp = null;
        	}
	        buffer.clear();
	        buffer = null;
        }
        return result;
    }

    private int startRecordHnadler(){
        String fileName = mNamePrefix +"_"+FileUtils.getDateFormatName("yyyyMMdd_HHmmss")+".mp4";
        String outFile = mSaveVideoDir + fileName;
        mFFMergePictureUtils.setVideoRate(RecordLevel.getFrameRate(mRecordLevel));
        int flag = mFFMergePictureUtils.start(outFile, getVideoWidth(), getVideoHeight());
        return flag;
    }

    private void stopRecordHandler(){
        if(!mCacheThreadExecutor.isShutdown()) {
            mCacheThreadExecutor.shutdown();
        }

        mFFMergePictureUtils.end();
        mTimerNum = 0;
        mAudioBuffers.clear();
        mImageBuffers.clear();
        mGetImageInterface.videoCodeState(100);

        releaseImageRes();
    }

    private class WriteBufferRunnable implements Runnable{
        private byte[] bitmapData;

        public WriteBufferRunnable(byte[] bitmapData){
            this.bitmapData = bitmapData;
        }

        @Override
        public void run() {
            if(bitmapData != null) {

                //Log.v(TAG, "WriteBuffer 1 time:"+System.currentTimeMillis());
            	//mTmpBitmap.eraseColor(0);
            	mTmpBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(bitmapData));
            	
            	byte[] data;
            	if(mInputWidth != mVideoWidth || mInputHeight != mVideoHeight){
            		Bitmap bmp = BitmapUtil.zoomBitmap(mTmpBitmap, mVideoWidth, mVideoHeight);
            		data = BitmapUtil.bitmap2Bytes(bmp,mQuality);
            		bmp = null;
            	}else{
            		data = BitmapUtil.bitmap2Bytes(mTmpBitmap,mQuality);
            	}
            	
                mImageBuffers.add(data);
                
                //Log.v(TAG, "WriteBuffer 2 time:" + System.currentTimeMillis());
                //Log.v(TAG, " ");
            }
        }
    }

    private class MergeVideoTask extends AsyncTask<Void, Integer, Integer>{
 
        @Override
        protected Integer doInBackground(Void... params) {
            int progress = 0;

            while (isRecording || mAudioBuffers.size() > 0 || mImageBuffers.size() > 0) {
				if(mFFMergePictureUtils.getTimeDifference() <= 0){
					if(mImageBuffers.size() > 0){
						byte[] data = null;
						try{
							data = mImageBuffers.remove(0);
						}catch(IndexOutOfBoundsException e){}
						
						if(data != null){
							mFFMergePictureUtils.appendImage(data, data.length);
						}
		                
		                if(!isRecording){//录制结束后发送当前进度
			                progress = (int)(((float)(mTimerNum - mImageBuffers.size()) / mTimerNum) * 100);
			                if(progress < 100)publishProgress(progress);
		                }
		                		
					}else if(!isRecording){
						//如果不是录制状态，而且图片数据也没了，那么丢掉剩余音频数据
						mAudioBuffers.clear();
					}
				}else{
					if(mAudioBuffers.size() > 0){
						byte[] audioData = new byte[GetAudioTask.BUFFER_LENGTH];
						int len = 0;
						//将audioData调整到2048长度后再发送，因为部分机型会无法一次读满buffer
						while(len < GetAudioTask.BUFFER_LENGTH){
							try {
		                        Thread.sleep(1);
		                    } catch (Exception e) {
		                        e.printStackTrace();
		                    }
							
							if(mAudioBuffers.size() > 0){
								byte[] data = null;
								try{
									data = mAudioBuffers.remove(0);
								}catch(IndexOutOfBoundsException e){}
								
								if(data != null && data.length > 0){
									int gap = GetAudioTask.BUFFER_LENGTH - (data.length + len);
									if(gap >= 0){
										System.arraycopy(data, 0, audioData, len, data.length);
										len += data.length;
									}else{
										//获取有效数据
										System.arraycopy(data, 0, audioData, len, data.length + gap);
										len += data.length + gap;
										
										//获取没有读完的数据存入mAudioBuffers
										byte[] ext = new byte[Math.abs(gap)];
										System.arraycopy(data, data.length + gap, ext, 0, Math.abs(gap));
										mAudioBuffers.add(0, ext);
									}
								}
							}else{
								//如果已经停止并且没有buffer了，那么退出
								if(!isRecording)break;
							}
						}
						if(len > 0){
							mFFMergePictureUtils.appendAudio(audioData, audioData.length);
						}
						audioData = null;
					}else if(!isRecording){
						//如果不是录制状态，而且音频数据也没了，那么丢掉剩余图像数据
						mImageBuffers.clear();
					}
				}
				try {
                    Thread.sleep(1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
			}
            Log.v(TAG, "MergeVideoTask end");
            return progress;
        }
        @Override
        protected void onProgressUpdate(Integer... progresses) {
            mGetImageInterface.videoCodeState(progresses[0]);
        }
        @Override
        protected void onPostExecute(Integer result) {
        	stopRecordHandler();
        }
    }
    
    private class GetAudioTask extends AsyncTask<Void, Void, Void>{
    	public static final int BUFFER_LENGTH = 2048;
        @Override
        protected Void doInBackground(Void... params) {
        	byte[] data;
            byte[] readData = new byte[BUFFER_LENGTH];
  
            //开始录制音频
            mAudioRecord.startRecording();
            while (isRecording) {
            	
                //如果暂停或还没开始，那么停止采样
                if(isPause || mTimerNum <= 0) {
                    try {
                        Thread.sleep(1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                //获取音频数据
                int len = mAudioRecord.read(readData, 0, BUFFER_LENGTH);
                //Log.v(TAG, "GetAudioTask len:"+len);
                if(len > 0){
	                data = new byte[len];
	                System.arraycopy(readData, 0, data, 0, len);
	                mAudioBuffers.add(data);
                }
            }
            //停止录音
            mAudioRecord.stop();
			return null;
        }
    }

    private TimerTask mRecordTimerTask = new TimerTask() {
        @Override
        public void run() {

            if(isPause)return;

            int seconds = mTimerNum / RecordLevel.getFrameRate(mRecordLevel);

            Message msg = Message.obtain(mHandler, MSG_RECORD_SECONDS);
            msg.arg1 = seconds;
            msg.sendToTarget();

            
            mImageBuffer.clear();
            int len = mGetImageInterface.fillImageBuffer(mImageBuffer);
            if(len > 0){
	            byte[] bitmapData = mImageBuffer.array();
	            
	            Log.v(TAG, "RecordTimerTask num:" + mTimerNum+",buffer size:"+mImageBuffers.size());
	            try {
	                mCacheThreadExecutor.execute(new WriteBufferRunnable(bitmapData));
	            }catch (RejectedExecutionException e){
	                e.printStackTrace();
	            }catch (NullPointerException e){
	                e.printStackTrace();
	            }
            }
            
            mTimerNum++;
        }
    };

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_RECORD_SECONDS:
                    mGetImageInterface.recordTimeChange(msg.arg1);
                    break;
            }
        }
    };

    /**
     * 图片录制接口
     */
    public interface ImageRecordInterface {
        /**
         * 获取需要录制的截图缓存
         * @param buffer
         * @return 返回读取长度
         */
        int fillImageBuffer(ByteBuffer buffer);

        /**
         * 录制时间
         * @param second
         */
        void recordTimeChange(int second);

        /**
         * 视频编码状态
         * @param progress
         */
        void videoCodeState(int progress);
    }
}