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
    private int mVideoWidth,mVideoHeight;

    /**保存文件名称的前缀**/
    private String mNamePrefix = "MC";

    /**计时线程**/
    private ScheduledExecutorService mTimerThreadExecutor;

    /**缓存线程**/
    private ExecutorService mCacheThreadExecutor;

    private AudioRecord mAudioRecord;
    private FFMergePictureUtils mFFMergePictureUtils;

    private ArrayList<byte[]> mAudioBuffer = new ArrayList<byte[]>();
    private ArrayList<byte[]> mImageBuffer = new ArrayList<byte[]>();

    private boolean isPause;
    private boolean isRecording = false;
    private ImageRecordInterface mGetImageInterface;

    public ImageRecordModule(ImageRecordInterface _interface){
        mGetImageInterface = _interface;
    }

    public void init(){
        mAudioBuffer.clear();
        mImageBuffer.clear();
        isPause = false;

        if(mTimerThreadExecutor == null || mTimerThreadExecutor.isShutdown())
            mTimerThreadExecutor = Executors.newScheduledThreadPool(3);

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

    /**设置视频尺寸**/
    public void setVideoSize(int widht,int height){
        this.mVideoWidth = widht;
        this.mVideoHeight = height;
    }

    /**
     * 获取视频宽度
     * @return
     */
    public int getVideoWidth(){
        return mVideoWidth;
    }

    /**
     * 获取视频高度
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
     * 结束录制
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
        boolean result = false;
        Bitmap bitmap = mGetImageInterface.getImage();
        if(bitmap != null){
            String savePath = mSavePhotoDir + FileUtils.getDateFormatName() + ".jpg";
            result = com.smart.pen.core.utils.FileUtils.saveBitmap(bitmap,savePath);
            if(!bitmap.isRecycled())bitmap.recycle();
            bitmap = null;
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
        mAudioBuffer.clear();
        mImageBuffer.clear();
        mGetImageInterface.videoCodeState(100);

    }

    private class WriteCacheRunnable implements Runnable{
        private Bitmap bitmap;

        public WriteCacheRunnable(Bitmap bitmap){
            this.bitmap = bitmap;
        }

        @Override
        public void run() {
            if(bitmap != null) {
                //裁剪图片
                bitmap = BitmapUtil.zoomBitmap(bitmap, getVideoWidth(), getVideoHeight());
                byte[] data = BitmapUtil.bitmap2Bytes(bitmap,mQuality);
                mImageBuffer.add(data);

                if(!bitmap.isRecycled())bitmap.recycle();

                data = null;
                bitmap = null;
            }
        }
    }

    private class MergeVideoTask extends AsyncTask<Void, Integer, Integer>{
 
        @Override
        protected Integer doInBackground(Void... params) {
            int progress = 0;

            while (isRecording || mAudioBuffer.size() > 0 || mImageBuffer.size() > 0) {
				if(mFFMergePictureUtils.getTimeDifference() <= 0){
					if(mImageBuffer.size() > 0){
						byte[] data = null;
						try{
							data = mImageBuffer.remove(0);
						}catch(IndexOutOfBoundsException e){}
						
						if(data != null){
							mFFMergePictureUtils.appendImage(data, data.length);
						}
		                
		                if(!isRecording){//录制结束后发送当前进度
			                progress = (int)(((float)(mTimerNum - mImageBuffer.size()) / mTimerNum) * 100);
			                if(progress < 100)publishProgress(progress);
		                }
		                		
					}else if(!isRecording){
						//如果不是录制状态，而且图片数据也没了，那么丢掉剩余音频数据
						mAudioBuffer.clear();
					}
				}else{
					if(mAudioBuffer.size() > 0){
						byte[] audioData = new byte[GetAudioTask.BUFFER_LENGTH];
						int len = 0;
						//将audioData调整到2048长度后再发送，因为部分机型会无法一次读满buffer
						while(len < GetAudioTask.BUFFER_LENGTH){
							try {
		                        Thread.sleep(1);
		                    } catch (Exception e) {
		                        e.printStackTrace();
		                    }
							
							if(mAudioBuffer.size() > 0){
								byte[] data = null;
								try{
									data = mAudioBuffer.remove(0);
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
										
										//获取没有读完的数据存入mAudioBuffer
										byte[] ext = new byte[Math.abs(gap)];
										System.arraycopy(data, data.length + gap, ext, 0, Math.abs(gap));
										mAudioBuffer.add(0, ext);
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
						mImageBuffer.clear();
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
                data = new byte[len];
                System.arraycopy(readData, 0, data, 0, len);
                mAudioBuffer.add(data);
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

            Bitmap bmp = mGetImageInterface.getImage();

            Log.v(TAG, "RecordTimerTask num:" + mTimerNum+",buffer size:"+mImageBuffer.size());
            try {
                mCacheThreadExecutor.execute(new WriteCacheRunnable(bmp));
            }catch (RejectedExecutionException e){
                e.printStackTrace();
            }catch (NullPointerException e){
                e.printStackTrace();
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
         * 获取一张图片
         * @return
         */
        Bitmap getImage();

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