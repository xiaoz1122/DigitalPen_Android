package com.smart.pen.core.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.smart.pen.core.common.Listeners.OnConnectStateListener;
import com.smart.pen.core.common.Listeners.OnFixedPointListener;
import com.smart.pen.core.common.Listeners.OnPointChangeListener;
import com.smart.pen.core.common.Listeners.OnScanDeviceListener;
import com.smart.pen.core.model.DeviceObject;
import com.smart.pen.core.model.PointObject;
import com.smart.pen.core.symbol.ConnectState;
import com.smart.pen.core.symbol.Keys;
import com.smart.pen.core.symbol.LocationState;
import com.smart.pen.core.symbol.SceneType;
import com.smart.pen.core.utils.BlePenUtil;


/**
 * 智能笔后台服务
 * @author Xiaoz
 * @date 2015年6月11日 上午11:46:19
 *
 */
public class SmartPenService extends Service{
	public static final String TAG = SmartPenService.class.getSimpleName();
	
	/**
	 * 初始读取笔数据次数，用于清除接收器中的缓存
	 */
	public static final int INIT_READ_DATA_NUM = 10;
	/**
	 * 自定义纸张尺寸最小值
	 */
	public static final int SETTING_SIZE_MIN = 500;
	/**
	 * 自定义纸张尺寸最大值
	 */
	public static final int SETTING_SIZE_MAX = 20000;

	private static final UUID SERVICE_UUID = UUID.fromString("0000FE03-0000-1000-8000-00805f9b34fb");
	private static final UUID NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
	private static final UUID PEN_DATA_UUID = UUID.fromString("0000ffc1-0000-1000-8000-00805f9b34fb");
	
	
	private boolean isScanning;
	private int mScanTime = 10000;
	private int mReadyNumber = INIT_READ_DATA_NUM;
	
	/**固定点停留计算次数**/
	private static final int FIXED_POINT_COUNT = 50;
	private ArrayList<Short> mFixedPointX = new ArrayList<Short>(FIXED_POINT_COUNT);
	private ArrayList<Short> mFixedPointY = new ArrayList<Short>(FIXED_POINT_COUNT);
	private PointObject mFirstPointObject = null;
	private PointObject mSecondPointObject = null;
	
	/**
	 * 判断定位第一个坐标按下状态<br />
	 * 这个值用来防止程序判断完成第一个坐标定位后，立即进入第二个坐标判断
	 * **/
	private boolean mFirstPointDown = false;

	/**场景坐标对象**/
	private PointObject mScenePointObject = new PointObject(); 
	
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothDevice mBluetoothDevice;
	private BluetoothGatt mBluetoothGatt;
	private BluetoothGattCharacteristic mPenDataCharacteristic;
	private BluetoothGattCallback mBluetoothGattCallback = new DeviceGattCallback();
	
	private OnScanDeviceListener onScanDeviceListener = null;
	private OnConnectStateListener onConnectStateListener = null;
	private OnPointChangeListener onPointChangeListener = null;
	private OnFixedPointListener onFixedPointListener = null;
	
	/**缓存扫描设备**/
	private HashMap<String,DeviceObject> mBufferDevices = new HashMap<String,DeviceObject>();
	private ScanDeviceCallback mScanDeviceCallback = new ScanDeviceCallback();
	private final IBinder mBinder = new LocalBinder();

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
	
	/**判断设备是否支持蓝牙**/
	public boolean isBluetoothAdapterNormal(){
		boolean result = true;
		if(mBluetoothAdapter == null){
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			if(mBluetoothAdapter == null){
				result = false;
			}
		}
		return result;
	}
	
	/**
	 * 判断蓝牙设备是否打开
	 * @return
	 */
	public boolean isBluetoothAdapterEnabled(){
		if(isBluetoothAdapterNormal())
			return mBluetoothAdapter.isEnabled();
		return false;
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
	 * 获取最后一次连接的设备
	 * @return
	 */
	public String getLastDevice(){
		SharedPreferences preferences = this.getSharedPreferences(Keys.DEFAULT_SETTING_KEY, Context.MODE_PRIVATE);
		String address = preferences.getString(Keys.DEFAULT_LAST_DEVICE_KEY, null);
		return address;
	}
	
	/**
	 * 保存最后一次连接成功的设备
	 * @param address
	 */
	private void saveLastDevice(String address){
		SharedPreferences preferences = this.getSharedPreferences(Keys.DEFAULT_SETTING_KEY, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(Keys.DEFAULT_LAST_DEVICE_KEY, address);
		editor.commit();
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
	 * 设置扫描持续时间
	 * @param millisecond
	 */
	public void setScanTime(int millisecond){
		this.mScanTime = millisecond;
	}
	
	/**
	 * 扫描设备
	 * @param listener
	 */
	public boolean scanDevice(OnScanDeviceListener listener){
		return scanDevice(listener,null);
	}
	
	/**
	 * 扫描设备
	 * @param listener
	 * @param prefix
	 */
	public boolean scanDevice(OnScanDeviceListener listener,String prefix){
		boolean flag = isBluetoothAdapterNormal();
		if(flag){
			if(isScanning)mBluetoothAdapter.stopLeScan(mScanDeviceCallback);
			
			//清除扫描缓存
			this.mBufferDevices.clear();
			this.onScanDeviceListener = listener;
			this.mScanDeviceCallback.prefixName = prefix;
			this.isScanning = mBluetoothAdapter.startLeScan(mScanDeviceCallback);
			this.mHandler.sendEmptyMessageDelayed(Keys.MSG_DISCOVERY_END, mScanTime);
		}
		return flag;
	}
	
	/**
	 * 停止扫描
	 */
	public void stopScanDevice(){
		if(isScanning){
			this.mHandler.removeMessages(Keys.MSG_DISCOVERY_END);
			mBluetoothAdapter.stopLeScan(mScanDeviceCallback);
			isScanning = false;
			//发送扫描结果
			if(onScanDeviceListener != null)
				onScanDeviceListener.complete(mBufferDevices);
		}
	}
	
	/**
	 * 连接设备
	 * @param listener
	 * @param address
	 * @return 连接状态
	 */
	public ConnectState connectDevice(OnConnectStateListener listener,String address){
		this.onConnectStateListener = listener;
		if(mBufferDevices.containsKey(address)){
			mReadyNumber = INIT_READ_DATA_NUM;
			mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
			if(mBluetoothDevice != null && !mBluetoothDevice.getAddress().isEmpty()){
				this.closeBluetoothGatt();
				this.mBluetoothGatt = mBluetoothDevice.connectGatt(SmartPenService.this, false, mBluetoothGattCallback);
				return ConnectState.CONNECTING;
			}else{
				return ConnectState.CONNECT_FAIL;
			}
		}else{
			return ConnectState.NOTHING;
		}
	}

	/**
	 * 断开设备连接
	 * @return
	 */
	public ConnectState disconnectDevice(){
		if(mBluetoothGatt != null){
			mBluetoothGatt.disconnect();
			return ConnectState.DISCONNECTING;
		}else{
			return ConnectState.DISCONNECTED;
		}
	}
	
	/***
	 * 开始初始化笔数据
	 */
	public void startInitPenData(){
		if(mReadyNumber > 0){
			mReadyNumber--;
			readPenData();
		}else{
			Message msg = Message.obtain(mHandler, Keys.MSG_PEN_INIT_COMPLETE);
			msg.sendToTarget();
		}
	}
	
	/**发现蓝牙服务**/
	private ConnectState servicesDiscovered(){
		if(mBluetoothGatt.discoverServices()){
			return ConnectState.SERVICES_START;
		}else{
			return ConnectState.SERVICES_FAIL;
		}
	}
	
	/**
	 * 读取笔数据
	 * @return
	 */
	private boolean readPenData(){
		return readData(mPenDataCharacteristic);
	}
	
	/**
	 * 读取通道数据
	 * @param characteristic
	 * @return
	 */
	private boolean readData(BluetoothGattCharacteristic characteristic){
		boolean result = false;
		
		//延时50毫秒后执行，防止部分机器反应不过来
		try {
			Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
		
		if(mBluetoothGatt != null && characteristic != null){
			result = mBluetoothGatt.readCharacteristic(characteristic);
		}
		return result;
	}
	
	/**设置通知**/
	private boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
		boolean result = false;
		mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
		
		BluetoothGattDescriptor descriptor = characteristic.getDescriptor(NOTIFICATION_DESCRIPTOR_UUID);
		if(descriptor == null){
			int step = 0;
			while(descriptor == null){
				if(step >= 20)
					break;
				
				step++;
				try {
					Thread.sleep(10);
	            } catch (InterruptedException e) {
	                e.printStackTrace();
	            }
				descriptor = characteristic.getDescriptor(NOTIFICATION_DESCRIPTOR_UUID);
			}
		}
		
		if(descriptor != null){
			descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
			result = mBluetoothGatt.writeDescriptor(descriptor);
		}else{
			Log.e(TAG, "setCharacteristicNotification descriptor is null");
		}
		return result;
	}

	
	/**
	 * 添加固定点记录
	 * @param x
	 * @param y
	 */
	private void addFixedPoint(PointObject point){
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
	private boolean isFixedPoint(){
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
	 * 发送固定点坐标信息
	 * @param point
	 */
	private void sendFixedPointInfo(PointObject point){
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
		Message msg = Message.obtain(mHandler, Keys.MSG_OUT_FIXED_INFO);
		msg.obj = state;
		msg.sendToTarget();
	}
	
	public class LocalBinder extends Binder {
		/**获取服务对象**/
		public SmartPenService getService() {
			return SmartPenService.this;
		}
	}
	

	private Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Keys.MSG_PEN_READY:
				String address = (String)msg.obj;
				
				if(onConnectStateListener != null)
					onConnectStateListener.stateChange(address,ConnectState.PEN_READY);
				
				//保存连接设备
				saveLastDevice(address);
				
				//清除BLE数据缓存
				BlePenUtil.clearBleDataBuffer();
				
				//开始初始化笔数据
				startInitPenData();
				break;
			case Keys.MSG_PEN_INIT_COMPLETE:
				if(onConnectStateListener != null)
					onConnectStateListener.stateChange((String)msg.obj,ConnectState.PEN_INIT_COMPLETE);
				break;
			case Keys.MSG_DISCOVERY_DEVICE:
				//返回发现的新设备
				if(onScanDeviceListener != null)
					onScanDeviceListener.find(mBufferDevices.get((String)msg.obj));
				break;
			case Keys.MSG_DISCOVERY_END:
				stopScanDevice();
				break;
			case Keys.MSG_CONNECTED:
				if(onConnectStateListener != null)
					onConnectStateListener.stateChange((String)msg.obj,ConnectState.CONNECTED);
				break;
			case Keys.MSG_DISCONNECTED:
				if(onConnectStateListener != null)
					onConnectStateListener.stateChange((String)msg.obj,ConnectState.DISCONNECTED);
				break;
			case Keys.MSG_CONNECT_FAIL:
				if(onConnectStateListener != null)
					onConnectStateListener.stateChange((String)msg.obj,ConnectState.CONNECT_FAIL);
				break;
			case Keys.MSG_SERVICES_READY:
				if(onConnectStateListener != null)
					onConnectStateListener.stateChange((String)msg.obj,ConnectState.SERVICES_READY);
				break;
			case Keys.MSG_SERVICES_FAIL:
				if(onConnectStateListener != null)
					onConnectStateListener.stateChange((String)msg.obj,ConnectState.SERVICES_FAIL);
				break;
			case Keys.MSG_OUT_POINT:
				if(onPointChangeListener != null)
					onPointChangeListener.change((PointObject)msg.obj);
				break;
			case Keys.MSG_OUT_FIXED_INFO:
				if(onFixedPointListener != null)
					onFixedPointListener.location(mFirstPointObject, mSecondPointObject, (LocationState)msg.obj);
				break;
			default:
				super.handleMessage(msg);
				break;
			}
		}
	};
	
	private class DeviceGattCallback extends BluetoothGattCallback{
		
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status,
				int newState) {
			Log.v(TAG, "onConnectionStateChange status:"+status);
			String address = gatt.getDevice().getAddress();
			Message msg = null;
			
			if(status == 133){
				closeBluetoothGatt();
				msg = Message.obtain(mHandler, Keys.MSG_CONNECT_FAIL);
			}else if (newState == BluetoothProfile.STATE_CONNECTED) {
				msg = Message.obtain(mHandler, Keys.MSG_CONNECTED);
				
				ConnectState disState = servicesDiscovered();
				if(disState != ConnectState.SERVICES_START){
					msg = Message.obtain(mHandler, Keys.MSG_SERVICES_FAIL);
				}
			}else if(newState == BluetoothProfile.STATE_DISCONNECTED){
				closeBluetoothGatt();
				msg = Message.obtain(mHandler, Keys.MSG_DISCONNECTED);
			}
			
			if(msg != null){
				msg.obj = address;
				msg.sendToTarget();
			}
		}
		
		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			String address = gatt.getDevice().getAddress();
			Message msg = null;
			if (status == BluetoothGatt.GATT_SUCCESS) {
				msg = Message.obtain(mHandler, Keys.MSG_SERVICES_READY);
				
				try {
					Thread.sleep(50);
	            } catch (InterruptedException e) {
	                e.printStackTrace();
	            }
				
				BluetoothGattService service = mBluetoothGatt.getService(SERVICE_UUID);
				mPenDataCharacteristic = service.getCharacteristic(PEN_DATA_UUID);
				setCharacteristicNotification(mPenDataCharacteristic,true);
			}else{
				msg = Message.obtain(mHandler, Keys.MSG_SERVICES_FAIL);
			}
			msg.obj = address;
			msg.sendToTarget();
		}

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                int status) {
			String address = gatt.getDevice().getAddress();
			Log.v(TAG, "onDescriptorWrite status:"+status);
			Message msg;
			if (status == BluetoothGatt.GATT_SUCCESS){
				msg = Message.obtain(mHandler, Keys.MSG_PEN_READY);
				
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}else{
				msg = Message.obtain(mHandler, Keys.MSG_CONNECT_FAIL);
			}
			msg.obj = address;
			msg.sendToTarget();
		}
		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			if(status == BluetoothGatt.GATT_SUCCESS){
				Log.v(TAG, "onCharacteristicRead status:"+status+",readyNumber:"+mReadyNumber);
				startInitPenData();
			}else if(status == 133){
				Message msg = Message.obtain(mHandler, Keys.MSG_CONNECT_FAIL);
				msg.obj = gatt.getDevice().getAddress();
				msg.sendToTarget();
			}else{
				startInitPenData();
			}
		}
		

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
//			String readData = BlePenUtil.getCharacteristicValue(characteristic).replace(" ", "");
//			Log.v(TAG, "onCharacteristicWrite value: " + readData);
			
			DeviceObject device = mBufferDevices.get(gatt.getDevice().getAddress());
			
			List<PointObject> pointList = BlePenUtil.getPointList(device,characteristic.getValue());
			PointObject item = null;
			if(pointList.size() > 0){
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
					
					Message msg = Message.obtain(mHandler, Keys.MSG_OUT_POINT);
					msg.obj = item;
					msg.sendToTarget();
					
					//Log.v(TAG, "out point:"+item.toString());
					addFixedPoint(item);
					
					//定位完第一个坐标，笔被抬起后，记录状态
					if(mFirstPointDown && !item.isRoute)mFirstPointDown = false;
				}
				
				sendFixedPointInfo(item);
			}
		}
	}
	
	/**
	 * 关闭mBluetoothGatt
	 */
	private void closeBluetoothGatt(){
		mReadyNumber = INIT_READ_DATA_NUM;
		BlePenUtil.clearBleDataBuffer();
		
		if(mBluetoothGatt != null){
			Log.v(TAG, "closeBluetoothGatt");
			mBluetoothGatt.close();
			mBluetoothGatt = null;
		}
		
		System.gc();
	}
	
	private class ScanDeviceCallback implements BluetoothAdapter.LeScanCallback{
		public String prefixName = null;
		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
			//23:680T 广播包
			//27:DigitalPen-xxxxx 广播包
			//29:PenXXXXXXXXXXXX 广播包
			int[] record = {23,27,29};
			int startIndex = 0;
			
			//开始判断是否是数码笔广播包
			boolean isPen = false;
			for(int i = 0;i < record.length;i++){
				startIndex = record[i];
				if(scanRecord[startIndex] == 0x44 && scanRecord[startIndex + 1] == 0x50){
					isPen = true;
					break;
				}
			}
			
			if(isPen){
				if(prefixName != null){
					//如果不是指定的前缀名称，那么忽略
					if(!device.getName().startsWith(prefixName))
						return;
				}
				
				String address = device.getAddress();
				if(!mBufferDevices.containsKey(address)){
					DeviceObject deviceObject = new DeviceObject(device);
					deviceObject.verMajor = Integer.valueOf(scanRecord[startIndex + 2]);
					deviceObject.verMinor = Integer.valueOf(scanRecord[startIndex + 3]);
					
					mBufferDevices.put(address, deviceObject);
					
					Message msg = Message.obtain(mHandler, Keys.MSG_DISCOVERY_DEVICE);
					msg.obj = address;
					msg.sendToTarget();
				}
			}
		}
	}
}
