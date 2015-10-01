package com.smart.pen.core.services;

import java.util.ArrayList;
import java.util.List;

import com.smart.pen.core.common.Listeners.OnConnectStateListener;
import com.smart.pen.core.common.Listeners.OnFixedPointListener;
import com.smart.pen.core.common.Listeners.OnPointChangeListener;
import com.smart.pen.core.common.Listeners.OnScanDeviceListener;
import com.smart.pen.core.model.PointObject;
import com.smart.pen.core.symbol.ConnectState;
import com.smart.pen.core.symbol.Keys;
import com.smart.pen.core.symbol.LocationState;
import com.smart.pen.core.symbol.SceneType;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * 
 * @author Xiaoz
 * @date 2015年9月30日 上午10:40:28
 *
 * Description
 */
public abstract class PenService extends Service{
	public static final String TAG = PenService.class.getSimpleName();
	
	/**
	 * 自定义纸张尺寸最小值
	 */
	public static final int SETTING_SIZE_MIN = 500;
	/**
	 * 自定义纸张尺寸最大值
	 */
	public static final int SETTING_SIZE_MAX = 20000;
	
	protected OnScanDeviceListener onScanDeviceListener = null;
	protected OnConnectStateListener onConnectStateListener = null;
	protected OnPointChangeListener onPointChangeListener = null;
	protected OnFixedPointListener onFixedPointListener = null;

	/**是否发送广播信息**/
	protected boolean isBroadcast;
	/**是否正在扫描**/
	protected boolean isScanning;
	/**扫描时间**/
	protected int mScanTime = 10000;

	/**场景坐标对象**/
	protected PointObject mScenePointObject = new PointObject(); 
	/**固定点停留计算次数**/
	private static final int FIXED_POINT_COUNT = 50;
	private ArrayList<Short> mFixedPointX = new ArrayList<Short>(FIXED_POINT_COUNT);
	private ArrayList<Short> mFixedPointY = new ArrayList<Short>(FIXED_POINT_COUNT);
	
	protected PointObject mFirstPointObject = null;
	protected PointObject mSecondPointObject = null;

	private final IBinder mBinder = new LocalBinder();
	
	/**
	 * 判断定位第一个坐标按下状态<br />
	 * 这个值用来防止程序判断完成第一个坐标定位后，立即进入第二个坐标判断
	 * **/
	protected boolean mFirstPointDown = false;
	
	/**检查设备连接状态**/
	abstract public ConnectState checkDeviceConnect();

	/**断开设备连接**/
	abstract public ConnectState disconnectDevice();
	
	/**发送笔状态**/
	abstract public void sendFixedPointState(LocationState state);

	/**
	 * 扫描设备
	 * @param listener
	 */
	abstract public boolean scanDevice(OnScanDeviceListener listener);
	
	/**
	 * 停止扫描
	 */
	abstract public void stopScanDevice();

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return mBinder;
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		Log.v(TAG, "onUnbind");
		return super.onUnbind(intent);
	}
	
	/**
	 * 设置是否开启广播，默认为false。如果需要通过广播的方式与Service交互，那么请设置为true。
	 * @param value
	 */
	public void setBroadcastEnabled(boolean value){
		this.isBroadcast = value;
	}
	
	/**
	 * 设置扫描持续时间
	 * @param millisecond
	 */
	public void setScanTime(int millisecond){
		this.mScanTime = millisecond;
	}
	
	/**
	 * 设置笔坐标变更监听
	 * @param listener
	 */
	public void setOnPointChangeListener(OnPointChangeListener listener){
		this.onPointChangeListener = listener;
	}
	
	/**
	 * 设置坐标定点监听
	 * @param listener
	 */
	public void setOnFixedPointListener(OnFixedPointListener listener){
		this.onFixedPointListener = listener;
	}
	

	/**
	 * 发送连接状态
	 * @param address
	 * @param state
	 */
	public void sendConnectState(String address,ConnectState state){
		if(onConnectStateListener != null)
			onConnectStateListener.stateChange(address,state);
		
		if(isBroadcast){
			Intent intent = new Intent(Keys.ACTION_SERVICE_BLE_CONNECT_STATE);
			intent.putExtra(Keys.KEY_DEVICE_ADDRESS, address);
			intent.putExtra(Keys.KEY_CONNECT_STATE, state.getValue());
			sendBroadcast(intent);
		}
	}
	
	
	/**
	 * 设置当前场景类型
	 * @param value
	 * @return
	 */
	public boolean setSceneType(SceneType value){
		return setSceneType(value,0,0);
	}
	
	/**
	 * 设置当前场景类型
	 * @param value
	 * @param width
	 * @param height
	 */
	public boolean setSceneType(SceneType value,int width,int height){
		return setSceneType(value,width,height,0,0);
	}
	
	/**
	 * 设置当前场景类型
	 * @param value
	 * @param width
	 * @param height
	 * @param offsetX	x偏移量
	 * @param offsetY	y偏移量
	 * @return
	 */
	public boolean setSceneType(SceneType value,int width,int height,int offsetX,int offsetY){
		boolean result = false;
		SharedPreferences preferences = this.getSharedPreferences(Keys.DEFAULT_SETTING_KEY, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt(Keys.DEFAULT_SCENE_KEY, value.getValue());
		editor.putInt(Keys.DEFAULT_SCENE_WIDTH_KEY, width);
		editor.putInt(Keys.DEFAULT_SCENE_HEIGHT_KEY, height);
		editor.putInt(Keys.DEFAULT_SCENE_OFFSET_X_KEY, offsetX);
		editor.putInt(Keys.DEFAULT_SCENE_OFFSET_Y_KEY, offsetY);
		if(result = editor.commit()){
			mScenePointObject = new PointObject();
			mScenePointObject.setSceneType(value);
			
			if(value == SceneType.CUSTOM)
				mScenePointObject.setCustomScene((short)width, (short)height,(short)offsetX,(short)offsetY);
		}
		return result;
	}
	
	/**
	 * 获取场景宽度
	 * @return
	 */
	public short getSceneWidth(){
		return mScenePointObject.getWidth();
	}
	
	/**
	 * 获取场景高度
	 * @return
	 */
	public short getSceneHeight(){
		return mScenePointObject.getHeight();
	}
	
	/**
	 * 获取当前场景类型
	 * @return
	 */
	public SceneType getSceneType(){
		SharedPreferences preferences = this.getSharedPreferences(Keys.DEFAULT_SETTING_KEY, Context.MODE_PRIVATE);
		SceneType type = SceneType.toSceneType(preferences.getInt(Keys.DEFAULT_SCENE_KEY, SceneType.NOTHING.getValue()));
		
		if(type != SceneType.NOTHING){
			mScenePointObject = new PointObject();
			mScenePointObject.setSceneType(type);
			if(type == SceneType.CUSTOM){
				short width = (short)preferences.getInt(Keys.DEFAULT_SCENE_WIDTH_KEY, 0);
				short height = (short)preferences.getInt(Keys.DEFAULT_SCENE_HEIGHT_KEY, 0);
				short offsetX = (short)preferences.getInt(Keys.DEFAULT_SCENE_OFFSET_X_KEY, 0);
				short offsetY = (short)preferences.getInt(Keys.DEFAULT_SCENE_OFFSET_Y_KEY, 0);
				
				mScenePointObject.setCustomScene(width, height, offsetX, offsetY);
			}
		}
		return type;
	}
	
	
	protected void handlerPointList(List<PointObject> pointList){
		PointObject item = null;
		if(pointList != null && pointList.size() > 0){
			for(int i = 0;i<pointList.size();i++){
				item = pointList.get(i);
				if(mScenePointObject.getSceneType() == SceneType.CUSTOM){
					item.setCustomScene(mScenePointObject.getWidth(), 
										mScenePointObject.getHeight(),
										mScenePointObject.getOffsetX(),
										mScenePointObject.getOffsetY());
				}else{
					item.setSceneType(mScenePointObject.getSceneType());
				}
				sendPointInfo(item);
				//Log.v(TAG, "out point:"+item.toString());
				addFixedPoint(item);
				
				//定位完第一个坐标，笔被抬起后，记录状态
				if(mFirstPointDown && !item.isRoute)mFirstPointDown = false;
			}
			
			handlerFixedPointInfo(item);
		}
	}
	
	
	/**
	 * 发送笔信息
	 * @param point
	 */
	public void sendPointInfo(PointObject point){

		if(onPointChangeListener != null)
			onPointChangeListener.change(point);
		
		if(isBroadcast){
			//发送笔迹JSON格式广播包
			Intent intent = new Intent(Keys.ACTION_SERVICE_SEND_POINT);
			intent.putExtra(Keys.KEY_PEN_POINT, point.toJsonString());
			sendBroadcast(intent);
		}
	}
	

	
	/**
	 * 添加固定点记录
	 * @param x
	 * @param y
	 */
	protected void addFixedPoint(PointObject point){
		if(point.isRoute){
			mFixedPointX.add(point.originalX);
			mFixedPointY.add(point.originalY);
			
			while(mFixedPointX.size() > FIXED_POINT_COUNT){
				mFixedPointX.remove(0);
			}
			while(mFixedPointY.size() > FIXED_POINT_COUNT){
				mFixedPointY.remove(0);
			}
		}else{
			mFixedPointX.clear();
			mFixedPointY.clear();
		}
	}
	
	/**
	 * 当前状态是否是固定在一个点上
	 * @return
	 */
	protected boolean isFixedPoint(){
		int result = 0;
		if(mFixedPointX.size() >= FIXED_POINT_COUNT && mFixedPointY.size() >= FIXED_POINT_COUNT){
			int sumX = 0;
			int sumY = 0;
			for(int i = 0;i < mFixedPointX.size();i++){
				sumX += mFixedPointX.get(i);
			}
			for(int i = 0;i < mFixedPointY.size();i++){
				sumY += mFixedPointY.get(i);
			}
			
			int gapX = (sumX / mFixedPointX.size()) - mFixedPointX.get(0);
			int gapY = (sumY / mFixedPointY.size()) - mFixedPointY.get(0);
			
			if(Math.abs(gapX) < 50)result++;
			if(Math.abs(gapY) < 50)result++;
		}
		return result == 2;
	}

	/**
	 * 应用自定义坐标
	 */
	public void applyFixedPoint(){
		if(mFirstPointObject != null && mSecondPointObject != null){
			int width = Math.abs(mSecondPointObject.originalX - mFirstPointObject.originalX);
			//根据定位规则，第2个点Y必须大于第1个点Y
			int height = mSecondPointObject.originalY - mFirstPointObject.originalY;
			int offsetX = width / 2 - mSecondPointObject.originalX;

			setSceneType(SceneType.CUSTOM,width,height,offsetX,mFirstPointObject.originalY);
		}
		againFixedPoint();
	}
	
	/**
	 * 重新定位纸张尺寸
	 */
	public void againFixedPoint(){
		mFirstPointObject = null;
		mSecondPointObject = null;
		
		mFirstPointDown = false;
		
		mFixedPointX.clear();
		mFixedPointY.clear();
	}
	
	/**
	 * 处理固定点坐标信息
	 * @param point
	 */
	protected void handlerFixedPointInfo(PointObject point){
		if(onFixedPointListener == null)return;
		
		LocationState state = LocationState.SecondComp;
		if(mFirstPointObject == null){
			if(isFixedPoint()){
				mFirstPointObject = point;
				mFirstPointDown = true;
				state = LocationState.FirstComp;
			}else{
				state = LocationState.DontLocation;
			}
		}else if(mSecondPointObject == null){
			if(!mFirstPointDown && isFixedPoint()){
				//判断第二个点只能出现在右下角
				if(point.originalX < mFirstPointObject.originalX || point.originalY < mFirstPointObject.originalY){
					//如果不是，那么提示已定位第一个点，请在右下角定位第二个点
					state = LocationState.FirstComp;
				}else if(point.originalX - mFirstPointObject.originalX < SETTING_SIZE_MIN
						|| point.originalY - mFirstPointObject.originalY < SETTING_SIZE_MIN){
					state = LocationState.LocationSmall;
				}else{
					mSecondPointObject = point;
					state = LocationState.SecondComp;
				}
			}else{
				state = LocationState.FirstComp;
			}
		}
		sendFixedPointState(state);
	}
	

	public class LocalBinder extends Binder {
		/**获取服务对象**/
		public PenService getService() {
			return PenService.this;
		}
	}
}
