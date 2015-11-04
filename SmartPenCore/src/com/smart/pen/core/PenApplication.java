package com.smart.pen.core;

import com.smart.pen.core.services.PenService;
import com.smart.pen.core.services.SmartPenService;
import com.smart.pen.core.services.UsbPenService;
import com.smart.pen.core.symbol.Keys;
import com.smart.pen.core.symbol.RecordLevel;

import android.app.ActivityManager;
import android.app.Application;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

/**
 * 
 * @author Xiaoz
 * @date 2015年10月1日 下午6:15:17
 *
 * Description
 */
public class PenApplication extends Application{
	private static final String TAG = PenApplication.class.getSimpleName();
	
	/**
     * 获取录制级别
     * @return
     */
    public int getRecordLevel(){
        SharedPreferences preferences = this.getSharedPreferences(Keys.RECORD_SETTING_KEY, Context.MODE_PRIVATE);
        int type = preferences.getInt(Keys.RECORD_LEVEL_KEY, RecordLevel.level_13);
        return type;
    }

    /**
     * 设置录制级别
     * @param value
     * @return
     */
    public boolean setRecordLevel(int value){
        SharedPreferences preferences = this.getSharedPreferences(Keys.RECORD_SETTING_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(Keys.RECORD_LEVEL_KEY, value);
        boolean result = editor.commit();

        return result;
    }
	
	/**
     * 获取笔服务
     * @return
     */
    public PenService getPenService(){
    	return mPenService;
    }
    
    
    private PenService mPenService;
	public boolean isBindPenService = false;
    private Intent mPenServiceIntent;
    public Intent getPenServiceIntent(String svrName){
		if(mPenServiceIntent == null){
			if(Keys.APP_PEN_SERVICE_NAME.equals(svrName)){
				mPenServiceIntent = new Intent(this, SmartPenService.class);
			}else if(Keys.APP_USB_SERVICE_NAME.equals(svrName)){
				mPenServiceIntent = new Intent(this, UsbPenService.class);
			}
		}
		return mPenServiceIntent;
	}
	
	private ServiceConnection mPenServiceConnection;
	private ServiceConnection getPenServiceConnection(){
		if(mPenServiceConnection == null){
			mPenServiceConnection = new ServiceConnection() {
				public void onServiceConnected(ComponentName className,
						IBinder rawBinder) {
					mPenService = ((PenService.LocalBinder) rawBinder).getService();
					Log.v(TAG, "onServiceConnected:"+mPenService.getSvrTag());
				}
		
				public void onServiceDisconnected(ComponentName classname) {
					Log.v(TAG, "onServiceDisconnected");
					mPenService = null;
					mPenServiceConnection = null;
				}
			};
		}
		return mPenServiceConnection;
	}

	/**开始后台服务**/
	protected void startPenService(String svrName){
		Log.v(TAG, "startPenService name:"+svrName);
		startService(getPenServiceIntent(svrName));
	}
	/**停止后台服务**/
	public void stopPenService(String svrName){
		Log.v(TAG, "stopPenService");
		stopService(getPenServiceIntent(svrName));
	}

	/**绑定后台服务,如果没有启动则启动服务再绑定**/
	public void bindPenService(String svrName){
		if(!isServiceRunning(svrName)){
			isBindPenService = false;
			this.startPenService(svrName);
		}
		if(!isBindPenService){
			mPenService = null;
			isBindPenService = bindService(getPenServiceIntent(svrName), getPenServiceConnection(), Context.BIND_AUTO_CREATE);
			Log.v(TAG, "bindService "+svrName);
		}
	}
	/**解除绑定后台服务**/
	public void unBindPenService(){
		if(isBindPenService){
			if(mPenServiceConnection != null){
				Log.v(TAG, "unBindPenService");
				unbindService(mPenServiceConnection);
				mPenServiceIntent = null;
			}
			isBindPenService = false;
		}
	}

    
    /**查询后台服务是否已开启**/
	private boolean isServiceRunning(String serviceName) {
	    ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (service.service.getClassName().compareTo(serviceName) == 0) {
	            return true;
	        }
	    }
	    return false;
	}
}
