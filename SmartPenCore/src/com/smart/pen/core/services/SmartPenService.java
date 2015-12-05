package com.smart.pen.core.services;

import java.util.HashMap;
import java.util.List;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.smart.pen.core.common.Listeners.OnConnectStateListener;
import com.smart.pen.core.common.Listeners.OnScanDeviceListener;
import com.smart.pen.core.model.AutoFindConfig;
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
@SuppressLint("NewApi")
public class SmartPenService extends PenService{
	public static final String TAG = SmartPenService.class.getSimpleName();
	
	/**
	 * 初始读取笔数据次数，用于清除接收器中的缓存
	 */
	public static final int INIT_READ_DATA_NUM = 10;

	private static final UUID SERVICE_UUID = UUID.fromString("0000FE03-0000-1000-8000-00805f9b34fb");
	private static final UUID NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
	private static final UUID PEN_DATA_UUID = UUID.fromString("0000ffc1-0000-1000-8000-00805f9b34fb");
	
	private int mReadyNumber = INIT_READ_DATA_NUM;

	private PenServiceReceiver mPenServiceReceiver;
	
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothDevice mBluetoothDevice;
	private BluetoothGatt mBluetoothGatt;
	private BluetoothGattCharacteristic mPenDataCharacteristic;
	private BluetoothGattCallback mBluetoothGattCallback;

	private long mLastFindTime;
	/**自动发现设备线程**/
    private ScheduledExecutorService mTimerThreadExecutor;
	
	/**缓存扫描设备**/
	private HashMap<String,DeviceObject> mBufferDevices = new HashMap<String,DeviceObject>();
	private ScanDeviceCallback mScanDeviceCallback;
	
	@Override
	public void onCreate() {
		super.onCreate();
        Log.v(TAG, "onCreate");
        
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2)return;
        
        mBluetoothGattCallback = new DeviceGattCallback();
        mScanDeviceCallback = new ScanDeviceCallback();
		mPenServiceReceiver = new PenServiceReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Keys.ACTION_SERVICE_SETTING_SEND_RECEIVER);
		intentFilter.addAction(Keys.ACTION_SERVICE_SETTING_SCENE_TYPE);
		intentFilter.addAction(Keys.ACTION_SERVICE_BLE_SCAN);
		intentFilter.addAction(Keys.ACTION_SERVICE_BLE_CONNECT);
		intentFilter.addAction(Keys.ACTION_SERVICE_BLE_DISCONNECT);
		registerReceiver(mPenServiceReceiver, intentFilter);
		
		initFindTimerThread();
	}
	
	@Override
	public void onDestroy() {
		unregisterReceiver(mPenServiceReceiver);
		super.onDestroy();
	}

	@Override
	public String getSvrTag() {
		return Keys.APP_PEN_SERVICE_NAME;
	}

	@Override
	public short getReceiverGapHeight() {
		return 880;
	}

	@Override
	public DeviceObject getConnectDevice() {
		if(checkDeviceConnect() == ConnectState.CONNECTED){
			return mBufferDevices.get(mBluetoothGatt.getDevice().getAddress());
		}
		return null;
	}
	
	@Override
	public ConnectState checkDeviceConnect() {
		ConnectState result = ConnectState.NOTHING;
		if(mBluetoothGatt != null){
			result = ConnectState.CONNECTED;
		}
		return result;
	}

	@Override
	public ConnectState disconnectDevice(){
		if(mBluetoothGatt != null){
			mBluetoothGatt.disconnect();
			return ConnectState.DISCONNECTING;
		}else{
			return ConnectState.DISCONNECTED;
		}
	}

	@Override
	public void sendFixedPointState(LocationState state) {
		Message msg = Message.obtain(mHandler, Keys.MSG_OUT_FIXED_INFO);
		msg.obj = state;
		msg.sendToTarget();
	}

	@Override
	public void handlerPointInfo(PointObject point) {
		Message msg = Message.obtain(mHandler, Keys.MSG_OUT_POINT);
		msg.obj = point;
		msg.sendToTarget();
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
	 * 获取是否自动发现
	 * @return
	 */
	public AutoFindConfig getAutoFindConfig(){
		AutoFindConfig result = new AutoFindConfig();
		SharedPreferences preferences = this.getSharedPreferences(Keys.DEFAULT_SETTING_KEY, Context.MODE_PRIVATE);
		result.isAutoFind = preferences.getBoolean(Keys.DEFAULT_AUTO_FIND_DEVICE_KEY, false);
		result.scanTime = preferences.getInt(Keys.DEFAULT_AUTO_FIND_SCAN_KEY, 0);
		result.gapTime = preferences.getInt(Keys.DEFAULT_AUTO_FIND_GAP_KEY, 0);
		return result;
	}
	
	/**
	 * 设置是否自动发现设备
	 * @param value
	 */
	public void setAutoFindConfig(AutoFindConfig config){
		SharedPreferences preferences = this.getSharedPreferences(Keys.DEFAULT_SETTING_KEY, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean(Keys.DEFAULT_AUTO_FIND_DEVICE_KEY, config.isAutoFind);
		editor.putInt(Keys.DEFAULT_AUTO_FIND_SCAN_KEY, config.scanTime);
		editor.putInt(Keys.DEFAULT_AUTO_FIND_GAP_KEY, config.gapTime);
		editor.commit();
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

	@Override
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
			stopScanDevice();
			
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
	@Override
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
	 * 获取搜索缓存中的设备
	 * @param address
	 * @return
	 */
	public DeviceObject getBufferDevice(String address){
		return mBufferDevices.get(address);
	}
	
	/**
	 * 连接设备
	 * @param listener
	 * @param address
	 * @return 连接状态
	 */
	public ConnectState connectDevice(OnConnectStateListener listener,String address){
		this.onConnectStateListener = listener;
		return connectDevice(address);
	}
	
	/**
	 * 连接设备
	 * @param address
	 * @return
	 */
	public ConnectState connectDevice(String address){
		Log.v(TAG, "connectDevice:"+address);
		if(mBufferDevices.containsKey(address)){
			mReadyNumber = INIT_READ_DATA_NUM;
			mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
			if(mBluetoothDevice != null && !mBluetoothDevice.getAddress().isEmpty()){
				this.closeBluetoothGatt();
				this.mBluetoothGatt = mBluetoothDevice.connectGatt(SmartPenService.this, false, mBluetoothGattCallback);
				
				//发送正在连接消息
				Message msg = Message.obtain(mHandler, Keys.MSG_CONNECTING);
				msg.obj = address;
				msg.sendToTarget();
				
				return ConnectState.CONNECTING;
			}else{
				return ConnectState.CONNECT_FAIL;
			}
		}else{
			return ConnectState.NOTHING;
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
			Message msg = Message.obtain(mHandler, Keys.MSG_PEN_READY);
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
	public boolean readPenData(){
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
	public boolean setCharacteristicNotification(){
		return setCharacteristicNotification(mPenDataCharacteristic,true);
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
	
	/**发送搜索到的设备**/
	public void sendDiscoveryDevice(DeviceObject deviceObject){
		
		if(onScanDeviceListener != null)
			onScanDeviceListener.find(deviceObject);
		
		if(isBroadcast){
			//发送笔迹JSON格式广播包
			Intent intent = new Intent(Keys.ACTION_SERVICE_BLE_DISCOVERY_DEVICE);
			intent.putExtra(Keys.KEY_DEVICE_ADDRESS, deviceObject.address);
			intent.putExtra(Keys.KEY_DEVICE_NAME, deviceObject.name);
			sendBroadcast(intent);
		}
	}
	
	/**
	 * 初始自动查找定时器
	 * @param gap 查找间隔，单位毫秒
	 */
	public void initFindTimerThread(){
		//检查是否需要开启自动查找
		AutoFindConfig config = getAutoFindConfig();
		if(!config.isAutoFind){
			if(mTimerThreadExecutor != null)mTimerThreadExecutor.shutdownNow();
			return;
		}

		long currTime = System.currentTimeMillis();
		//如果计时器正在执行
		if(currTime - mLastFindTime < config.gapTime){
			return;
		}
		
		if(mTimerThreadExecutor != null)mTimerThreadExecutor.shutdownNow();
		
		if(mTimerThreadExecutor == null || mTimerThreadExecutor.isShutdown())
            mTimerThreadExecutor = Executors.newScheduledThreadPool(1);
		
		mTimerThreadExecutor.scheduleAtFixedRate(new FindTimerTask(config.scanTime), 0, config.gapTime, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * 是否自动连接设备处理
	 * @param address
	 */
	private void isAutoConnectDeviceHandler(String address){
		//检查是否需要自动链接
		AutoFindConfig config = getAutoFindConfig();
		if(config.isAutoFind){
			String lastAddress = getLastDevice();
			if(address.equals(lastAddress)){
				//停止扫描
				stopScanDevice();
				//连接匹配设备
				connectDevice(address);
			}
		}
	}

	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Keys.MSG_PEN_READY:
				String address = (String)msg.obj;
				sendConnectState(address,ConnectState.PEN_READY);
				
				//设置广播
				setCharacteristicNotification(mPenDataCharacteristic,true);
				break;
			case Keys.MSG_PEN_INIT_COMPLETE:
				sendConnectState((String)msg.obj,ConnectState.PEN_INIT_COMPLETE);
				//保存连接设备
				saveLastDevice((String)msg.obj);
				break;
			case Keys.MSG_DISCOVERY_DEVICE:
				String findAddress = (String)msg.obj;
				//发送发现的新设备
				sendDiscoveryDevice(mBufferDevices.get(findAddress));
				isAutoConnectDeviceHandler(findAddress);
				break;
			case Keys.MSG_DISCOVERY_END:
				stopScanDevice();
				break;
			case Keys.MSG_CONNECTING:
				sendConnectState((String)msg.obj,ConnectState.CONNECTING);
				break;
			case Keys.MSG_CONNECTED:
				sendConnectState((String)msg.obj,ConnectState.CONNECTED);
				break;
			case Keys.MSG_DISCONNECTED:
				sendConnectState((String)msg.obj,ConnectState.DISCONNECTED);
				break;
			case Keys.MSG_CONNECT_FAIL:
				sendConnectState((String)msg.obj,ConnectState.CONNECT_FAIL);
				break;
			case Keys.MSG_SERVICES_READY:
				sendConnectState((String)msg.obj,ConnectState.SERVICES_READY);

				//清除BLE数据缓存
				BlePenUtil.clearDataBuffer();
				
				//开始初始化笔数据
				startInitPenData();
				break;
			case Keys.MSG_SERVICES_FAIL:
				sendConnectState((String)msg.obj,ConnectState.SERVICES_FAIL);
				break;
			case Keys.MSG_OUT_POINT:
				if(msg.obj != null){
					sendPointInfo((PointObject)msg.obj);
				}
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
	
	/**
	 * 查找设备定时器任务
	 */
	private class FindTimerTask extends TimerTask{
		private int scanTime;
		public FindTimerTask(int scanTime){
			this.scanTime = scanTime;
		}
		@Override
		public void run() {
        	Log.v(TAG, "FindTimerTask run");
			mLastFindTime = System.currentTimeMillis();
        	
        	//检查是否已连接
        	if(getConnectDevice() != null)return;
        	
        	if(isScanning)return;
        	
        	//扫描设备
        	setScanTime(scanTime);
        	scanDevice(null);
		}
	}
	
	private class PenServiceReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(Keys.ACTION_SERVICE_BLE_SCAN)){
				boolean isStart = intent.getBooleanExtra(Keys.KEY_VALUE, true);
				if(isStart){
					//扫描设备
					scanDevice(null);
				}else{
					stopScanDevice();
				}
			}else if (action.equals(Keys.ACTION_SERVICE_BLE_CONNECT)){
				//连接设备
				String address = intent.getStringExtra(Keys.KEY_DEVICE_ADDRESS);
				ConnectState state = connectDevice(null,address);
				sendConnectState(address,state);
			}else if (action.equals(Keys.ACTION_SERVICE_BLE_DISCONNECT)){
				//断开当前连接的设备
				disconnectDevice();
			}else if(action.equals(Keys.ACTION_SERVICE_SETTING_SCENE_TYPE)){
				//设置纸张场景
				SceneType type = SceneType.toSceneType(intent.getIntExtra(Keys.DEFAULT_SCENE_KEY, 0));
				int width = intent.getIntExtra(Keys.DEFAULT_SCENE_WIDTH_KEY, 0);
				int height = intent.getIntExtra(Keys.DEFAULT_SCENE_HEIGHT_KEY, 0);
				int offsetX = intent.getIntExtra(Keys.DEFAULT_SCENE_OFFSET_X_KEY, 0);
				int offsetY = intent.getIntExtra(Keys.DEFAULT_SCENE_OFFSET_Y_KEY, 0);
				setSceneType(type,width,height,offsetX,offsetY);
			}else if (action.equals(Keys.ACTION_SERVICE_SETTING_SEND_RECEIVER)){
				//设置打开广播发送信息
				boolean value = intent.getBooleanExtra(Keys.KEY_VALUE,true);
				setBroadcastEnabled(value);
			}		
		}
	}
	
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
				//setCharacteristicNotification(mPenDataCharacteristic,true);
				
				
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
				msg = Message.obtain(mHandler, Keys.MSG_PEN_INIT_COMPLETE);
				
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
			handlerPointList(pointList);
		}
	}
	
	/**
	 * 关闭mBluetoothGatt
	 */
	private void closeBluetoothGatt(){
		mReadyNumber = INIT_READ_DATA_NUM;
		BlePenUtil.clearDataBuffer();
		
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
			int startIndex = 0;
			
			//开始判断是否是数码笔广播包
			boolean isPen = false;
			//从广播包第20位开始检查
			if(scanRecord.length > 20){
				for(int i = 20;i < scanRecord.length - 1;i++){
					if(scanRecord[i] == 0x44 && scanRecord[i + 1] == 0x50){
						startIndex = i;
						isPen = true;
						break;
					}
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
					msg.obj = deviceObject.address;
					msg.sendToTarget();
				}
			}
		}
	}
	
	public void sendTestData(){
//		String testData = "808823c23f2b808821c4be2a808806c61d2a8088fcc7ce2980880eca5d298080000000808088ce0d471180884d0d9a108088a20ce60f8088b90b350f80887f0aa50e80881509360e80888707f10d8088eb05d90d80884904e30d8088a102050e80880e014f0e8088beff830e8088a7fe890e8088c9fd400e8088fafcaa0d80883afce00c808898fbee0b808820fbfa0a8088c4fa270a80887dfa820980884bfa0609808832fab108808827fa7008808834fa430880884ffa3008808858fa270880887bfa5608808869fabf08808840fa1809808825fa370980882ffa0e09808840faf10880883bfae008808839fad908808832fae108808831fae308808838fad708808839fac508808838faa80880883bfa860880883efa6008808841fa4208808844fa2f08808848fa2e08808853fa400880884efa7108808846faa608808842fad10880884cfae708808865faf108808882faf10880889cfadb088088a2fabd088088a0faa608808893faa30880887cfab408808862facf08808848faf408808830fa0e0980881ffa1809808812fa190980880cfa110980880afa010980880afaf008808814fae008808822fad30880883cfabe08808852faac08808160faae0880815bfab608808155fac308808154fac908808137fa030980811bfa32098081e5f983098081a2f9d409808177f9070a808160f9160a80815bf9130a808867f9fb09808883f9c5098088a5f99d098088d1f975098088edf96a098081eef974098081ecf97d098081eef991098081ebf9d1098081e8f9130a8081e4f9530a8081e7f9710a8081e7f9780a8088eaf94e0a8088f2f9f409808804faac09808818fa8c0980810ffa980980810efaa109808115faa509808139fa990980815cfa780980817efa5f0980818cfa5609808195fa570980819dfa62098081a1fa8a098081a3fac5098081a0fa070a80819bfa430a80819afa5e0a808198fa5e0a808193fa5c0a80817bfa420a808860fa1e0a808841fa000a808826fae30980881cfad609808111fae209808110faea09808119faf509808130fafb0980814efaf00980816afae109808179fada0980887efadd09808874fae40980885bfaea0980883ffaf709808823fa140a80880efa390a8081f6f9680a8081ebf96d0a8081ebf96d0a8081fcf9690a808121fa600a80814afa4f0a80816efa440a808188fa380a808191fa370a808193fa320a808192fa280a808874fafa09808864fab20980889efa6c0980880bfb1309808893fbb008808818fc6308808890fc2b088088ecfc1308808828fd120880883bfd1d08808829fd4508808814fd69088088e2fc96088088c4fcb4088088b5fcd7088088aefcff088088a8fc250980889afc4f09808897fc5d0980889bfc4d098088a3fc39098088abfc38098088a6fc4b098088a0fc5609808896fc5b09808889fc560980887ffc4c0980887afc4709808874fc480980886bfc4509808861fc4309808858fc4009808852fc3509808852fc2809808857fc1c0980815cfc1909808156fc1909808151fc1e09808151fc2409808150fc570980814ffc9d0980814ffcd50980814efc010a808851fcff0980884bfcd50980884efc910980885cfc4609808873fc1609808173fc0f09808170fc100980817bfc0f0980819cfc0a098081c6fc04098081e9fcf6088081f7fced088081f7fcec088081f7fcf0088081f5fcf9088081f1fc31098081f2fc6d098081f4fcb3098081f9fced098081fafc110a8081fbfc1b0a8081f8fc180a8081effc0f0a8081e3fcff098081d2fcf0098088befccd098088a4fca00980888ffc7d09808882fc6c0980887dfc7809808176fc8309808174fc8909808179fc8a0980818bfc8e098081abfc87098081c1fc7f098081d1fc81098081dafc89098088d5fc92098088b9fc9809808899fca609808881fcbd09808872fce009808868fcfe09808159fc100a808158fc130a80815ffc130a80817efc180a8081a6fc120a8081d0fc030a8081f6fcf609808106fdf00980810bfdeb0980810afdec098088f6fcda098088d4fcb1098088defc7e09808835fd39098088b4fded0880883afeaa088088b4fe800880881eff6c08808867ff6708808881ff6708808877ff6808808851ff7a0880882dff9308808803ffb3088088dafec4088088c2febb088088b7febe088088b1fec0088088b4fec0088088b8fec5088088befecb088088c1fec5088088c2febf088088c3feb7088088c8feb0088088cdfeaa088088d1fea5088088d4fe9c088088d4fe98088088cefe9f088088c9feaf088088bffec0088088b5fed1088088a5fee808808892fef908808884fe090980887bfe1e09808876fe3509808878fe470980887bfe4909808879fe4509808877fe4009808871fe3c0980886efe3e09808870fe3f09808875fe3c0980887dfe3709808889fe3009808895fe23098088a2fe14098088b0fe00098088cdfedc088088fcfeab08808834ff7708808867ff4f08808899ff26088088befffb078088cfffdb078088cdffca078088beffc4078088acffc3078088a0ffc107808897ffc807808890ffd507808890ffe20780889affe7078088a8fff0078088baffff078088cbff18088088cfff35088088c8ff5b088088b9ff7c088088abff90088088a0ffa008808897ffa608808893ffa208808896ff9508808898ff8e08808897ff8f08808890ff9708808886ffa20880887bffab0880886fffb108808862ffb508808857ffb50880884fffaf0880884affab08808847ffab08808843ffaf08808841ffb708808839ffbf0880882effc708808822ffcb08808819ffcb08808817ffc60880881bffbd08808821ffb30880882effa908808845ff9808808856ff860880815aff9108808157ff9408808156ff9808808154ff9b0880814cffa90880812bffe8088081fdfe21098081cdfe5b098081b2fe72098088acfe76098088b2fe5c098088c3fe31098088e4fe0a09808809ffec08808823ffec08808125ff0409808121ff1d0980811eff480980811bff8c0980811dffd509808122ff0a0a808120ff290a808120ff2d0a80811eff240a80881dffea0980882aff7a0980884bff0d0980887dffb1088088a4ff7d088081a8ff74088081a6ff79088081a4ff7d088081b4ff84088081d0ff92088081e5ffa4088088e3ffb0088088c2ffb90880888effce0880886dffe30880815dfff90880815afffd0880816bfffd0880819dffe8088081d5ffd50880810000c70880811200c20880881200c90880880000d0088088edffd9088081cefff8088081a4ff2f0980816cff7f09808138ffc309808126ffd909808127ffde09808838ffc109808853ff9309808867ff7e09808172ff7d09808179ff9609808179ffcd0980817bff090a808186ff290a80818eff290a80819dff1c0a8081bcffe1098081e5ff8109808110002c0980812d00f60880883000f308808122000b0980810c0039098081f9ff68098088ecff87098088dbff8b098088cfff7f098088c8ff6e098088bcff63098088b1ff59098088a8ff4b098088a1ff410980889cff3d09808194ff3f09808191ff4409808192ff480980819aff5d098081aeff73098081c9ff8a098081e8ffb50980810c00d80980812f00f909808156001b0a80816e002a0a80817c00320a80818000320a80818000310a80818000300a80818000300a808181002f0a80818100300a80889600360a80889600240a8088d700c10980887101440980881302e9088088a102ac0880880d038f0880884f038c0880886b03900880886303990880884803ad0880882f03be0880883203bd0880884c03b30880887403b50880889603ce088088ae03ee088088b90309098088a603280980887a034c0980884203670980880d036b098088e90255098088d7022c098088da020a098088cf024d098088a102b9098088a002dd0980889f02e40980889402f70980888f02070a80888d020e0a80888c02110a80888802100a808887020c0a80888402040a80888402f90980888602f30980888502f00980888402f10980888502ef0980888702e90980888902df0980888902d30980888a02c30980888d02b30980889002a30980889202930980889502840980889b0275098088a10266098088a70250098088ac023b098088ae022a098088aa0217098088a70208098088a202f70880889e02e30880889f02d5088088a002c9088088a102c3088088a002be0880889f02be0880889b02c10880889a02c10880889602c20880889102c10880888f02c30880888d02c50880888d02c40880888e02c20880888f02c30880889102c20880889202c60880889102ca0880888f02cd0880888e02d20880888d02d20880888902d50880888502d70880887e02d70880887b02d20880887a02ca0880887702c40880886f02c00880886802b50880886002a60880884f02a80880883702b70880882802bc0880881f02c40880882602cb0880883702d10880884f02dc0880886b02e50880888b02e7088088a802ef088088b902fd088088bd0214098088b7022e098088a9024d0980889602710980888802950980888e02aa0980889f02be098088a902c0098088b702bd098088bb02b3098088bc02a4098088b50294098088ab0283098088a302740980889802690980888b02620980888302570980887c024a09808875024209808870023c0980886f023009808871021c0980887702070980888002f00880888c02d80880818902d30880818402d50880818202db0880818602f20880819102250980819a02660980819b02ab0980819802e60980819202060a80818d02070a80887702f00980886702b40980886102950980815b029509808154029d0980815102af0980814702e60980813e021c0a808136023e0a808132024a0a80813402450a808141023f0a80816302290a808190020e0a8081c302f8098081e602f1098081f202f6098088f402f5098088f402e7098088fe02c50980880e03a109808819038809808818037409808114037f09808110038109808111038209808117039f0980811b03ce0980811b03fe0980811d030d0a80811e03140a80881803fd0980882203bc0980885e0359098088cb03f10880884b049c088088cc046d0880885a0551088088d9055408808841066c08808882068b0880889806a70880888206c30880885306d40880881406e2088088d205f0088088a105f30880887e05f00880886b05e80880886005da0880885c05c50880885d05b40880885a05a90880885005a50880884005a60880883405a70880882705a50880881d05a10880881705980880880e058f0880880605830880810305840880810305810880811305790880813e05700880816d056a08808193056b088081ab0577088081ac057f0880819d059d0880817405bf0880813705e20880810905ff0880810005040980810405040980810c05020980812805fa0880814805ee0880816c05e30880818505de0880888e05de0880887c05d80880885705ca0880882e05b00880881a059808808110059e0880810905a30880810505b20880810305c9088081f20409098081dd044b098081c4048e098081ab04c00980819c04d20980819304d40980888f04c4098088a20496098088c30463098088e704350980880405280980810b05360980811305490980812a056509808152057f098081800589098081a50590098081b60592098081b40594098081b50594098088a8059809808892059a0980887d059809808868059609808850059a0980883505990980881d059a0980880e05a10980880305af098081f804ba098081f704bf098081fc04c60980810e05da0980812105e70980812c05ed0980813605ed0980813805f00980883305f30980882405ff0980881b05100a80881905150a80881905180a80881205210a80880605310a8088fd04430a8088f804580a8081f1045e0a8081f504630a808104056e0a80811805800a808125058b0a80813705940a808143059b0a808146059c0a80814b059c0a808148059c0a80814d059a0a80883905800a80882c054c0a80884705270a80889c05190a8088ea05350a80881b066a0a80882c06b10a80882706000b80882506300b80882f06210b80884a06e20a80886706840a80888b061f0a8088a506c2098088b60677098088c0064c098088c30633098088be0623098088ad061d09808888062109808862062509808840061e0980882a06150980881706070980880a06f7088088fd05ef088088f505e9088088ea05ef088088db0515098088bf05680980889105ed0980884d058c0a8088f904390b80888b04fc0b8088f603e40c80883a03e80d80885f02f30e80887501f40f80887900e41080887fffbf11808884fe8112808899fd20138088c3fc9b138088fdfbfe13808844fb49148088a4fa741480881ffa75148088c6f95a14808883f92714808862f9e41380885df99113808873f92c138088a0f9b6128088f5f92612808867fa78118088d8fad010808854fb51108088fcfbd00f8088cefc500f8088a0fdeb0e80885bfea70e808805ff760e8088b3ff470e80885600110e8088f300d60d80888401900d80880f024d0d8088d5034d0d80885d042a0d8088ef042b0d80888d05520d80882e06930d8088e506e50d80889807420e8088b406d60e80883a072d0f80889c076d0f8088ec07950f80882808ae0f80884408b70f80884308ae0f80883308940f80880f08740f8088d3074f0f80889d07220f80886107f20e80883007c60e80883007c60e8080000000808088de06930e8088de06930e80883f08550e80883108530e80881708570e808803085a0e8088f4074e0e8088d607380e8088be070e0e8088a807d50d80889507890d80887a07240d80885f07a20c808841070a0c8088d705820b8088e905ce0a80881f060e0a8088640664098088b006d4088088ff06570880884507e1078088850753078088a907a7068088ad070b068088b407ff058088be0720068088a907060680888807e90580886a07e70580885607eb0580884b07e90580884507eb0580883e07f20580883b07f70580883b07f60580883a07f30580883a07f20580883a07f20580883e07f60580884507fb0580884a07000680884c07000680884e07fb0580884d07f20580884707e20580883e07d50580883407cd0580882e07ca0580882f07c90580883407cc0580883b07ca0580883d07cd0580883807cf0580883507cf0580883007d00580882e07d20580882c07da0580882e07e10580882e07eb0580883007f20580883107f80580883007fe05808832070206808832070606808831070d06808832070f0680883107150680882e07190680882807150680882207130680881c070c0680881607010680881207f80580880c07ec0580880607e20580880107d5058088f806cd058088f006c5058088e606bd058088dd06b6058088d806b1058088d006ae058088ca06b1058088bf06bf058088b206da058088a106000680888f06310680887c066a0680886c06a90680886006ec06808856062e0780884c06710780884106a60780883506c90780883406e10780884106ff0780885e061b088088870644088088ab068d088088cb060d098088e606d00980880307cb0a80880307cb0a80800000008080883a07300d80883a07300d80886307790e80887407c00f80888607f11080888807f51180887907be1280883f0739138088d9065a138088620632138088e005e7128088620581128088f304241280888904e51180882e04c5118088e103ce1180889b030412808868035b1280882f03cc128088ec0242138088b802af1380886e0210148088170258148088cf01891480886f019d1480880b0199148088a300811480883f0060148088e6ff3b1480888dff1014808840ffdd1380880dffa6138088e3fe64138088bffe011380882600fc12808820008b12808850003b128088a700061280881a01d71180889d01ac11808825027c118088b802441180883d03061180884c0285108088ab0237108088fd02e80f80883b039b0f80886003490f80887003fd0e80887703b90e80886f03880e808860036b0e80884c035e0e80883e035f0e80883b03670e80883f036d0e80884c03730e80885d03760e80887103780e80888803750e80889b036c0e8088b003620e8088c2035a0e8088cd03510e8088d4034b0e8088da03400e8088d7033b0e8088d4033b0e8088cf033b0e8088ce03380e8088ba033b0e8088b903360e8088b303350e8088ad03320e8088a9032a0e8088a403210e80889b03120e80889b03fb0d80889b03fb0d80800000008080889603db0d8080000000808088f2048b0d808801053d0d80881d05ec0c8088f503850c808812042d0c80881704c90b80880604550b8088f203ea0a8088df03880a8088d303240a8088e303c309808823047b0980887a0453098088c3045e098088ff048a0980882e05bb0980885705e10980887905fd0980888605fc0980889305000a80887f05220a808867054b0a80884e05790a80883c059d0a80883905ab0a80883e05a90a80883d05ac0a80883205b10a80882205b60a80880b05c80a8088e604ed0a8088ae04290b80886004760b8088f803d10b80887603390c8088d902a80c80881b022a0d80884301b90d80885a00470e80886affd10e808876fe490f80888afda90f8088b0fce60f8088ecfb051080883bfb13108088a2fa0b10808822fae80f8088c1f9a60f808882f9490f80885af9eb0e80884af98f0e808847f9320e808851f9d50d80886df9860d808894f94e0d8088c0f92c0d8088ebf91c0d80881cfa100d808857fa000d808896faea0c8088deface0c80882ffbad0c80888efb8d0c80880afc720c8088aefc510c808880fd210c80887dfed20b8088a0ff640b8088de00f20a80882b02820a80886603280a80887b04f60980886405db0980882a06c4098088ca06ad09808842079c09808870078e0980887c079f0980885207e10980881707190a8088f406250a8088d406210a8088ae061a0a80888206090a80885e06f40980883c06df0980881c06cd098088f805c4098088d405bd098088aa05b20980887e05a609808854059b0980883d05860980883805780980883c05650980884b05580980884005700980883e05770980883b057c09808837058509808830058c0980882a05920980882105950980881b059709808817059c0980881805a00980881705a30980881505a60980881205a90980880e05af0980880d05ad0980880c05ab0980880905a60980880705a30980880305a60980880205a90980880105ab0980880305a90980880205a80980880405a90980880205a90980880205a70980880005a7098088ff04a5098088fe04a3098088fe04a1098088fb049b098081fd04a1098081fb04a0098081f9049f098081f8049f098081f6049e098081f9049f098081f904a1098081f904a0098081f904a4098081f904a0098081f804a4098081f804a1098081f704a0098081f804a1098081f604a0098081f804a0098081f3049e098081f704a0098081f704a4098081f704a10980810305b0098081fa04a60980811205bc0980810205ae0980811d05c60980810a05b50980811e05c80980811005bb0980811e05c80980811205bd0980812105cc0980811505bf0980812205cd0980811605c10980812305ce0980812205cc0980813205dc0980812605d00980813705df0980812a05d40980813705e00980812d05d60980813a05e30980812f05d80980813a05e50980813005d90980813a05e20980883405d109808839059b098088810597098088ef05ce0980884706150a808882065d0a8088ae06950a8088be06c60a8088b606d80a80889706cf0a80887406b10a808857068a0a808849066a0a808846064f0a80884606370a808844061d0a80883f06010a80883806ea0980883906db0980883b06d30980883906c90980883606bc0980883206b40980883406ad0980883006ae0980882d06bb0980882a06c60980882c06cb0980883106ca0980883606c60980883606c70980883406c60980883406c30980883406ca0980883206d30980883406d50980883906d20980884006cc0980884306c90980884706c80980884906c70980884a06c30980884906bd0980884506bd0980883d06c00980883a06c40980883a06c50980883c06c60980884006c60980884306c20980884406be0980884306bc0980884106ba0980883e06bb0980883b06be0980883d06bd0980883c06c00980883b06c00980883b06c20980883c06c30980883c06c20980883e06c70980883f06ce0980884106d50980884406df0980884606ea0980884b06f90980884d06190a808852064b0a80885c06900a80886906ec0a80888406610b8088b706fd0b8088b706fd0b8080000000808088f706ea0c80800000008080888b076b0f8088b307ef108088e8077e128088150a9e138088750ad0148088c00aa9158088c00aa9158080000000808088d50a2f1680800000008080887b03f5118088330262118088eb00f1108088c6ff8310808886fd8d0f8088b5fc410f8088f7fb200f808846fb1d0f8088a3fa2d0f808810fa2e0f80888bf9280f808809f9200f80889af8200f80884af81f0f808825f8100f808820f8f50e80883af8d50e808868f8b50e8088a0f89e0e8088e5f8880e808825f9700e808862f9610e8088aff9520e8088dcf9330e8088e4f9250e8088d4f91d0e8088ccf91b0e8088d3f9150e8088eff9050e808812fae30d80883ffab00d808864fa810d808874fa620d80886bfa4e0d808854fa440d80883afa2b0d808822fafd0c80881afae40c80881cfad70c8088f9f9130d8088e5f9520d8088f9f9590d808803fa6e0d80880ffa850d80881cfa920d808839fa8c0d808852fa7d0d808860fa6d0d808867fa610d80885ffa610d808852fa670d808844fa700d80883cfa790d808832fa7d0d808827fa810d808823fa7b0d808821fa6d0d808822fa630d808827fa590d80882bfa510d808832fa470d808838fa3e0d80883afa370d80883afa340d808838fa350d808834fa390d808830fa3b0d80882bfa3e0d80882afa3d0d808828fa390d808827fa360d808829fa330d80882afa300d80882cfa2c0d80882efa230d808833fa1a0d808839fa0e0d808845fa010d80884dfaf70c808852faee0c808857faed0c808854faef0c80884ffaf60c808848fafe0c808842fa050d80883bfa0f0d808835fa1a0d80882ffa280d80882afa340d808825fa3f0d808825fa450d808826fa460d80882cfa440d808832fa400d808839fa3c0d80883cfa3b0d80883cfa3d0d808835fa440d808830fa4d0d808829fa540d808829fa5a0d80882afa5f0d80882bfa600d80882ffa610d808830fa5e0d808833fa5a0d808837fa590d808838fa590d808839fa5b0d808837fa5c0d808836fa5e0d80882cfa630d808824fa650d808821fa640d80881cfa600d808817fa5b0d808812fa540d80880efa4b0d80880efa430d808809fa3d0d808805fa360d808801fa320d8088fdf92e0d8088f5f92d0d8088edf92e0d8088e6f9300d8088d7f9350d8088c4f9420d8088b0f94e0d80889ef9530d808895f94f0d808898f9480d808197f9490d80819af94b0d8081a4f9520d8081c5f94f0d8081f6f9440d80812bfa310d80815bfa1c0d80816ffa170d808871fa1b0d80884efa2c0d808829fa2b0d808814fa0c0d808809faeb0c808808fade0c8081faf9ec0c8081f2f9fc0c8081eaf91e0d8081e4f9560d8081ebf9880d8081f2f9a10d808804fa9a0d808810fa5a0d808820fa0d0d808841fac90c80814efabe0c80814ffacb0c80814ffadc0c80814efaf90c80814bfa250d808143fa440d808126fa6f0d8088f0f99f0d8088bdf9ba0d808895f9cc0d808187f9cf0d808184f9c90d808196f9c90d8081b7f9c00d8081eaf9aa0d808132fa8e0d80817bfa770d8081b3fa6b0d8081c9fa6c0d8088c1fa780d808883faa00d808837faae0d8088f6f9c10d8088c8f9db0d8088b7f9ef0d8081a5f9110e8081a3f91d0e8081a1f92c0e8081a2f9460e8081aaf96f0e8081b6f9890e8088c6f9890e8088c4f9690e8088c7f9420e8088d1f91e0e8081d2f91f0e8081d9f9190e8081eef9130e80811afafe0d808152faeb0d808182fadd0d80819dfae20d8081a6fa020e80819afa3e0e808183fa7c0e808173faa40e808161fab90e80884bfaaf0e808827fa940e808801fa810e8088e6f9720e8081d4f9730e8081d2f9700e8081def96b0e808103fa5a0e80812afa3d0e808151fa2a0e808172fa200e808881fa1c0e808876fa0b0e808867fadb0d808861fab00d808858fab70d808146fac90d808139fad90d808134fa0a0e808133fa330e808123fa6f0e808801faa20e8088dcf9b90e8081c2f9d00e8081bdf9d10e8081c8f9cc0e8081f2f9b60e80811efa980e80813ffa860e80814bfa820e808147fa880e808130faa30e80810cfae10e8081e3f91b0f8081d1f93b0f8088cff9340f8088e2f90f0f808803fae80e808827facd0e808848fad30e808152fae60e808155fafd0e808164fa170f80817efa230f808199fa3b0f8081acfa450f8081b3fa4c0f8081b4fa4c0f8081b7fa480f8088a6fa260f80889cfad60e8088d7fa700e80883efb260e8088aafbf90d80881cfcdf0d808887fcd00d8088d9fcc10d80880cfdc20d808819fdd40d8088fffcd00d8088c6fcc60d808876fcd80d808840fccc0d808829fcaa0d808817fc910d80880cfc7c0d808804fc6a0d8088fbfb5c0d8088f4fb540d8088eafb580d8088e2fb560d8088dafb550d8088d2fb540d8088ccfb4e0d8088c9fb460d8088cafb3f0d8088cffb340d8088d3fb2f0d8088d5fb2e0d8088d2fb2f0d8088cdfb300d8088cefb280d8088d0fb230d8088cdfb210d8088c7fb250d8088befb2e0d8088b9fb390d8088b3fb3f0d8088b4fb3e0d8088b7fb360d8088c2fb270d8081bcfb330d8081bafb440d8081bdfb560d8081c7fb640d8081d7fb770d8081e9fb860d8081f6fb8d0d8088fafb980d8088edfba40d8088d6fb9e0d8088c9fb920d8088c4fb820d8088c0fb870d8088b0fba90d8081a3fbb00d80819ffbb80d8081a1fbba0d8081affbd50d8081c4fbf00d8081d3fb100e8088d5fb380e8088c8fb4d0e8088bbfb690e8088b6fb860e8088bdfbab0e8081c1fbcd0e8081bcfbd50e8081bafbd90e8081bcfbd60e8081c5fbcd0e8081e0fba60e808103fc740e808825fc430e80883cfc0f0e808851fcd90d808868fca50d80886afc930d808863fc920d808156fc9b0d808159fc9b0d80816dfc870d80819dfc690d8081d0fc4b0d808104fd3a0d808121fd390d808827fd470d808806fd610d8088d6fc700d8088a4fc700d808889fc7f0d808874fc9d0d80815bfcc70d808142fcec0d808145fc000e80814ffc040e808869fce20d808877fcb40d80817afcad0d808185fcaa0d808195fcb10d8081a9fcbb0d8081bafcc90d8081bafcdc0d8081a7fcf10d80817dfc170e80815efc250e808154fc290e808158fc250e808172fc190e808191fc060e8081b6fcf80d8081e0fce10d8088f8fccc0d8088f7fcb50d8088fafc990d8088fafc830d8088fbfc850d8081f1fc8d0d8081ecfc910d8081f0fc9c0d8081f8fcc80d8081fbfc0c0e8081fcfc550e8081f7fca10e8081f2fcd50e8081eafcf80e8081e8fc0b0f8081e1fc0d0f8081d1fc0e0f8081bafc000f808193fce90e80817ffcc20e80887efc9b0e80889afc660e8088d8fc1e0e808826fdcf0d80886cfd980d8088a4fd790d8088d1fd6c0d8088f1fd710d808813fe850d808850fe960d8088a1fe9c0d808803ff980d80885bffa30d808894ffbb0d8088a8ffd80d808897fffc0d808878ff180e80885aff260e80883dff1c0e80883effea0d808814ff070e808803ff3d0e808814ff330e80882dff0c0e80882efffd0d80882effee0d80882bfff50d808824ff050e808823ff130e80882aff160e80883cff0b0e808854ffef0d808157ffe70d808154ffe30d808152ffe90d80814effee0d808140ff050e808125ff410e808109ff6b0e8081ecfe920e8088e0fe8e0e8088d9fe640e8088dcfe220e8088effece0d808808ff860d808820ff530d808124ff550d808121ff5c0d808121ff660d80813bff8e0d80815cffc10d808185fffa0d8081aaff340e8081c9ff640e8081eaff840e80810c00980e80812b00a20e80814000a60e80814700a70e808847009b0e80883e006b0e808870000d0e8088ed008a0d80886e011c0d8088de01d30c80883602b10c80886f02ab0c80889602af0c8088a702bc0c8088c002cf0c8088da02e00c8088e002f50c8088ee02020d8088f102150d8088e6022c0d8088d2023e0d8088b702490d80889902580d80888802600d808887026b0d80888b02740d80888a02730d80887302710d808855026f0d80883502660d80881702630d8088fe015b0d8088f0014f0d8088ec01400d8088ec01330d8088f1011f0d8081f001230d8081ea012b0d8081ea01330d8081f7014a0d80811402600d80812e026f0d80814202800d80814802920d80883b02a00d80881b02a40d80880502970d8088fa018e0d8081f1019d0d8081ea01a80d8081e901b10d8081f201c00d8081fe01cc0d80811002df0d80812002f60d80882a020a0e80881e021d0e80880c02370e808800025d0e8088fb01840e8081ec01960e8081e7019d0e8081e9019e0e8081ed01970e80810802770e80812a02420e80814d02060e80886402c20d808872028b0d80888a02590d8088a0022b0d8088b402090d8088bd02f00c8081b602f70c8081b302fa0c8081af02010d8081ac020b0d80819702480d80818302870d80817202bc0d80886f02cb0d808881029d0d80889902630d8088b602350d8081c202350d8081c3023b0d8081d9023f0d8081f5023d0d80811b033d0d80812e03490d80882d035b0d80881b035c0d80880003500d8088f002460d8088dd02570d8081bf028b0d8081a102bd0d80818302000e80817a023c0e80817a025c0e80817902690e80817c026a0e80818902710e8081b0026b0e8081ec025c0e80811b03510e808138034d0e808140034c0e80883003420e80881703250e8088fd02f70d8088e802c40d8088d502b00d8088be02a10d8081ae029e0d8081ab02980d8081b7028e0d8081df027c0d80810803720d80812803700d80813303740d80813603770d80813503790d808135037a0d80813803940d80813603b10d80812d03e50d808124032f0e80811b03790e80811b03ab0e80811803c50e80811503cd0e80811403cd0e80810d03cc0e8081f602cb0e8081e002c00e8081c402ab0e8088a302810e80888502450e80887402170e80886802fd0d80885702fb0d80884002040e808832020f0e80812a02120e80812702110e80813d020e0e80816802fe0d8081a002e00d8081db02cf0d80810a03c40d80812403c20d80812b03c50d80812b03d30d80881803c90d80880f03a70d808811038f0d80880503880d8081f402960d8081eb02990d8081e9029c0d8081ea029e0d8081ed02b10d8081f002cd0d8081f202df0d8088e902f90d8088e302fc0d8088df02fb0d8088de02f90d8088dc02020e8081d402150e8081d002200e8081d402300e8081db02400e8081e8024d0e8081dc02350e8081f602520e8081e902480e8088f6023a0e8088f502080e80880f03ac0d80885a032d0d8088cf03ba0c80884204710c80889d04550c8088dd04550c80880605600c80881e05700c808830057f0c80883d058b0c808839059a0c80882305b30c80880405d30c8088ea04f90c8088d104140d8088c9041c0d8088b2043d0d8088ab04540d8088ae04620d8088b4046d0d8088c2046e0d8088c4046d0d8088be04710d8088b104770d80889f04800d80889004860d80888304850d80887b047a0d80887704680d80887004530d80886a04400d80886404310d80816104400d80815c04490d80815e04530d80816a04660d808177046e0d80819204830d8081a604890d8081ae04930d8081b3049a0d80889b04a70d80887c04a10d80886c049d0d80886404980d80815b04a80d80815604b00d80815704ba0d80815d04cd0d80816904da0d80817904f00d80818504030e80888904170e80888004240e80887a04300e80887604460e808870046a0e80886204970e80814c04bc0e80814104ce0e80814204dd0e80814a04df0e80815604e60e80814e04e10e80816b04f50e80815f04ed0e80817704ed0e80819604c10e8081b904870e8088d5042e0e8088f004cd0d80881905860d80882d056c0d808829057e0d80881c05930d80881505a20d80881305a50d808817059d0d808818058c0d80881505760d80880d05680d80880705570d808806053c0d808103052f0d808107052b0d80811105300d80811d05350d80813c05380d80815a05390d80816c05380d80817505410d80886005500d80883605660d80880d05780d8088f7048f0d8088ed04a20d8081e604a10d8081eb04a20d80810e05950d80813705830d808158057f0d80816f057c0d80816d05800d808166058b0d80814205b80d80811e05eb0d80810505150e808102051a0e80810405170e80810d05140e80812c05020e80815505e20d80817b05c00d80819505a90d8081a1059b0d8081a7059a0d8081aa059c0d8081aa059d0d8081af05ab0d8081ad05a90d8081b405da0d8081af05bd0d8081a705020e80889105130e808877051c0e80885e051c0e80884f050e0e80884605070e80883c050c0e808835051c0e80812e05310e80812405330e80811f053d0e80811605580e80810a05800e80810605960e80810205ab0e80810805ab0e80881605890e80882f055b0e80884f052f0e80885a051f0e80815705280e808153052a0e808154052d0e808156054a0e808157056c0e80815905920e808857059a0e80885c057f0e80886805520e80887c051b0e80888c05f80d80888d05ec0d80818705f50d80818405fa0d80818a05040e80818e05200e80819005480e80818f05750e80818f05a20e80819805c30e8081a105d20e8081ac05d90e8081bc05e30e8081c505e30e8081e305de0e8081db05e00e80812006d20e80813d06d40e80815406c60e80815c06bb0e80815806a40e80815206570e80883b06040e80881b06b30d808815066a0d808823065d0d80883106810d80882f06bf0d80881f06f20d80881d060a0e80883006070e80884c06ec0d80886b06bb0d80888706820d80889d06480d8088ac061a0d8088a906fa0c80889606d90c80887c06bc0c808873068b0c808888064f0c80889f06280c8088ac06160c8088b106120c8088ab06190c80889b06220c80888906270c80887706230c80886406200c80885506200c80884806200c808848061b0c80884a06150c80884a06130c808846061a0c80884606280c808843063e0c80884806520c808848065d0c808850065b0c80885b064c0c808866063d0c80886e06340c808875062d0c808878062b0c80887906240c808877061f0c808870061d0c808867061c0c80886406190c80886106180c80886106180c80885f06190c80886406180c80886a06160c80886b06140c80886c06160c80886c061a0c808873061f0c80887a061f0c80888206200c80888c06210c80889506280c80889b062b0c8088a0062d0c8088ad062d0c8088bd06290c8088cd06260c8088dd061e0c8088e906150c8088ef06110c8088ed060f0c8088ea060f0c8088e206110c8088d606170c8088d0061f0c8088ca06280c8088c606310c8088ca06370c8088ca063a0c8088d1063a0c8088d206380c8088d406370c8088cf06370c8088ca06380c8088c5063e0c8088c006430c8088b906490c8088b6064e0c8088b306510c8088b906510c8088bc064f0c8088c9064c0c8088d7064c0c8088e5064e0c8088ee06510c8088f606540c8088fb06550c8088ff06540c80880107500c8088ff064d0c80880107480c80880407430c80880607430c80880807410c80880c07400c80880d073e0c80880e073a0c80880f07360c80880e07300c80880d072a0c80881507250c80881a07210c808824071e0c80882d071e0c80883707210c80883f07230c80884207230c80884507230c80884907210c80884a07220c80884c07270c808854072c0c80885b07330c80886a07390c80887b073c0c80888b07450c80889b07480c8088aa07490c8088c0074a0c8088cd07490c8088d107500c8088cd075e0c8088c8076c0c8088c3077a0c8088c507800c8088c7077f0c8088cd07760c8088d507680c8088d9075d0c8088d607520c8088d0074b0c8088c6074c0c8088c1074d0c8088b507540c8088ab07560c8088a007590c80889a075c0c808896075f0c80889507610c80889a07620c80889c07620c80889d07610c808899075d0c80889107560c80888d074f0c808885074d0c808886074c0c808884074c0c808887074e0c80888607500c80888b07500c80888a07520c80888507500c808884074c0c808882074b0c80888607480c80888d07430c808890073f0c80889a073a0c8088a407370c8088b207320c8088bd072d0c8088bf07250c8088c007180c8088cf07050c8088e207e50b8088fa07b80b80880f088a0b80882408630b80883e08460b80885108280b80885e08090b80885f08f00a80885908df0a80884f08dd0a80884008ec0a80883008fe0a80882808070b80882f08000b80884408f80a80885808fa0a80886d08fe0a80888008fe0a80889508ed0a80889d08df0a8088aa08db0a8088c508e40a8088f608e70a80882f09e50a80886409e00a80889409d60a8088bc09d20a8088e409cc0a8088040ac70a8088270abd0a8088420aba0a80885d0ab80a80887a0ab80a8088990ab30a8088b10aae0a8088bc0aaa0a8088bd0aa20a8088c00a9a0a8088c40a940a8088cd0a900a8088d60a8f0a8088df0a8a0a8088e00a890a8088e40a840a8088e30a7f0a8088e00a7f0a8088df0a830a8088dc0a8e0a8088e10a990a8088e10aa30a8088d70ab30a8088c90ac20a8088b90ad40a8088aa0ae50a80889d0af80a8088950a050b8088880a150b8088710a260b8088530a3b0b8088300a4b0b80880f0a520b8088fb094e0b8088e309440b8088cb09420b8088ac094c0b80888d09540b80887209550b80886009460b80884d09350b808832092d0b808818092a0b80880509280b8088ff081d0b8088ff08110b808803090d0b80880209130b8088fc081d0b8088f808230b8088f5082b0b8088f808310b8088f808380b8088f808400b8088fa08460b808800094c0b80880609550b80880909610b80880d096c0b80881109720b80881409720b80881b09710b80881e09720b80882309710b80882209730b80881f09710b808820096e0b80881d09680b80881709640b808814095f0b80880c095f0b80880f095f0b80881209620b80881609630b80881c09650b80882109630b80882609620b80882b095e0b80882a095b0b80882c095a0b80882809590b80882c09540b80882d09520b80882c094e0b80882b094b0b80882809470b80882509440b80882809420b80882609410b80882709410b80882909420b80882909440b80882b09480b80882b094c0b80882e094a0b80882c09470b80882b09420b80882709420b80881d09410b80881409440b808808094c0b8088fc085c0b8088ee08750b8088d608970b8088be08c90b8088a908000c8088af081e0c8088bd083a0c8088b8086b0c8088a308ab0c80889408010d80888b087e0d8088a308310e808804092a0f808866094f10808866094f108080000000808088d30992118080000000808088580be21580889e0b61168088b90b9b168088930b95168088930b951680800000008080881006c9178088d3059b1780888905511780883f05ea16808810034b168088c202b01580886102fd148088e8013f1480886d0194138088f800121380888000bb1280880f00711280880c01971280887b006f128088dfff4b128088c5fd9a1180880bfd6b11808847fc4b11808881fb4c118088d2fa7b11808846fab0118088d8f9c011808890f9b51180885af9a911808837f9a21180882df99d11808836f9a01180884bf9ba11808865f9db1180888bf9f3118088b9f9fb118088dff9fd118088f3f9fe118088f3f9f9118088f5f9ec118088faf9cf1180880ffaa31180881bfa8b11808820fa8711808822fa8711808827fa861180882bfa7a11808827fa6b1180881efa6111808819fa5611808816fa4b11808816fa3c1180881afa2d1180881ffa241180881ffa1c11808820fa1511808821fa0d1180882cfa0411808832fafd10808837faf710808839faf610808838fafb10808834fafe10808831fa001180882bfa0211808827fa0711808822fa0c1180881ffa081180881dfa0111808819faf41080881efae61080881dfad41080881ffac31080881ffaaf1080881afa9e10808813fa9210808804fa90108088f5f99b108088eaf9a7108088e4f9b4108088ddf9c2108088d9f9d4108088d5f9e4108088d4f9e6108088daf9e1108088dff9d6108088def9cc108088e2f9bd108088e9f9a4108088eaf992108088ecf983108088ecf97c108088edf97d108088e4f98a108088d7f9a4108088c7f9b6108088b9f9c6108088aff9d8108088a9f9ec108088a5f9ff108088a8f90f118088b6f916118088c2f914118088cff90f118088d1f90c118088d0f90c118088caf90b118088bef90f118088b5f90e118088a6f9111180889ef9151180889af9151180889df90c118088a6f9f5108081a9f9f9108081a4f9fa108081a2f9021180819ff90711808192f9251180817df94e11808168f96e1180815cf97e11808862f97a11808879f9461180889ff90a118088d0f9da108081e0f9e5108081f1f9fd108081f5f90511808110fa0f11808129fa1d11808837fa2a1180882cfa2a11808823fa2411808816fa25118081f8f93b118081d9f94f118081b5f9751180818df9a911808166f9e111808151f90e1280884ff92312808856f90d12808871f9d011808898f991118088c4f979118081d7f988118081e8f99911808104faa211808126fab01180813bfab011808143fab411808829fac4118088f5f9d0118088c4f9d8118088b2f9d3118081a7f9e4118081a2f9e9118081a1f9fc118081a3f920128081abf940128088b6f956128088b8f948128088b3f920128088baf9fa118081b6f9f9118081baf9fa118081cef9fd118081eff9f811808109fafe11808107fafe118081faf914128081ccf940128081a4f95812808196f96112808198f95e128081b0f956128081d7f93d12808100fa2912808120fa191280882efa0e1280882cfae311808835fa9c1180884bfa511180885ffa0d11808870fae31080887afacd10808177facb10808178fad310808170fad810808170fade10808166faff10808153fa3b1180813efa7c1180883afa9311808842fa7111808858fa3e11808170fa181180818dfa02118081acfaf8108081d3fafa108081e8faff108081f2fa0b118081f1fa1a118088cffa42118088bcfa3b118088aefa2f118081a2fa2b1180819afa3211808188fa5911808174fa9811808159fad71180814afaff11808845fa0e12808847faf311808859fac711808870faa611808884faa51180818afab811808193fad1118081abfae5118081c8fafd118081e9fa0e128081fefa101280810bfb1112808805fb1112808818fbf011808861fba5118088c3fb6c1180883efc45118088b4fc281180880bfd1e11808834fd1a1180882afd16118088fcfc16118088c5fc0b118088a4fced10808886fcd510808872fcbd1080886dfca31080886cfc8c1080886efc751080887bfc5b10808178fc5d10808170fc6610808171fc6e10808176fc7e10808184fc9a10808191fcba10808884fcd510808862fcdd10808843fcdb1080882bfccd1080881ffccc10808112fcd41080810cfce410808106fcf810808109fc1a11808116fc3911808825fc4411808829fc201180882afcf81080882ffcd010808132fcd410808139fcd310808150fccb10808188fcbb108081bffcac108081f7fc9e10808115fd9c10808114fda6108081fefcc1108088d4fce8108088c1fcea108088b9fcde108081b1fcd7108081a5fcd91080818efce71080816afc101180814afc4b11808139fc7211808844fc721180885cfc4311808887fc0f118088bbfcf2108081cffc03118081d7fc0a118081e4fc11118081f1fc16118081f4fc17118088defc2f118088a8fc581180886dfc881180884afc9e1180813cfca31180813dfc9f11808143fc9a11808165fc8611808190fc77118081b9fc71118081e3fc71118081fafc7e11808101fda2118081f4fcd4118081e1fc03128081d3fc17128081c4fc1c128081b5fc121280819efcf711808892fcc51180889bfc84118088acfc54118088b8fc38118081b1fc3c118081aefc40118081acfc42118081aafc43118081a8fc511180819afc8311808183fcb111808162fcf711808848fc221280883ffc0e1280884cfce011808878fc9211808893fc5f118088adfc36118088cafc0d118088f0fce210808827fdbc10808861fd9c10808899fd7f108088d8fd631080880cfe5610808829fe6910808832fea01080883cfeda10808856fe061180887ffe21118088acfe43118088e3fe5a11808817ff5c11808833ff4511808835ff3a1180883bff3c1180884cff461180885aff4111808856ff331180884cff2011808844ff1811808842ff1111808843ff0611808842fff81080883dffeb10808835ffdd10808832ffcc1080883affb910808846ff9d10808851ff841080885aff8410808150ff8d1080814dff951080814aff9d1080814affb910808145ffd610808134fff610808115ff1e118081e8fe44118088bcfe58118088a7fe56118088a0fe50118088a2fe55118088a2fe63118088a7fe71118081a1fe7d118081a2fe7e118081befe7a118081effe6a1180813dff4a11808191ff2c118081c9ff1f118081dfff1c118081dfff1f118081cdff27118088a8ff2c11808894ff2611808889ff181180887cff081180886ffffe10808861fff710808156fffd10808153fffd10808152fffe10808157ff1011808159ff3711808158ff6e11808153ffb411808155ffe611808157ff0312808156ff0a12808157ff0512808157ff0012808155ffed11808855ffc01180886cff80118088b4ff251180881900c91080888d008a108088000161108088780149108088f60138108088730220108088e802181080883f032210808874033a1080888e034a10808891035210808877035c1080884b036710808820036f10808802037e108088f10294108088db02a9108088ca02b4108088c102bb108088bd02b7108088b002b11080889f02ae1080888702b41080886502c11080883f02d61080881d02eb1080880702f81080880002fc1080880002f91080880602ef1080880c02de1080880c02d11080880d02c61080880b02b91080880d02a61080880e02941080880c0286108081080289108081fe0190108081fa0198108081f8019f108081f301d1108081f401f6108081f5011c118081f6013b118088fd01391180880502141180880d02e21080881a02b01080811902a81080811802a41080812002a110808131029810808154028e1080817e027b108081a6026e108081c00269108081c8026a108081c7026d108081c60272108081c7027d108081c202b3108081b902e7108081b70219118081b50236118081b4023e118081b7023f118081ac0239118081a202351180887a02131180885a02f91080883f02f01080882102e31080811302e91080811102e91080811102ea1080811c02eb1080813d02e01080816002d81080818502ce1080819a02cf108081a202d71080889302ea1080886e020c1180883e02271180881a023f11808104024f11808100025211808101025111808110024d1180813a024011808165022c1180818e02251180819f0227118081a90228118081a8022d1180889102281180887d02101180887402e11080887402ab1080887402991080816b02991080816a02a01080816b02c11080816d02f310808166023d1180816502801180815e02b11180815e02c31180815d02c51180885402bc1180884402a511808838028f1180882a028011808120028b11808116028a11808118028c1180811c028e11808141028b11808163028411808185027f1180819a027b118081a1027d1180889e028a1180888102951180885a029c1180883802a01180881502aa118088f101c5118088d401de118088ba01e9118081b601ef118081b701ee118081ca01ec118081e601e71180811502dd1180815302d81180819702d1118081d302ca1180810403c31180812903c31180813703c21180813703c01180813703c01180813403c11180881503b711808804038f11808813034c1180884b03fb1080889503ba108088d6039210808802048d1080882804a01080884404bf1080885304cc1080885604c61080885a04ba1080886904a41080888e048c108088ce047c10808814057710808852058b1080888505a510808895059e108088a9059b1080887d05d91080885d050c11808851050e1180883e05031180882e05f11080882305d91080881c05c91080881005c41080880005c6108088eb04cb108088da04cd108088cb04ca108088c204c1108088bb04b1108088b5049e108088b10489108081ab0495108081a8049a108081a804a9108081a604c2108081a504ef108081a00428118081970451118081940470118081980477118088a10474118088b00449118088bd040f118088cd04d1108088da04ac108081d304a3108081d20495108081d20496108081e7048e10808113057910808141056e1080815b056b10808161056a1080815f05691080815f056b1080815f05731080816005941080815f05be1080815b05fa1080815c053f1180815e057d1180815905aa1180815905b51180815805bd1180815805bc1180815505b91180815505ba1180814f05b71180815005b61180813205981180811d057c118088000549118088ec041a118088dd04fa108088d404e8108088cc04eb108088c704f0108081cc04f7108081d204f6108081e004ef108081f604ea1080811505e11080812c05de1080813505dc1080813b05e31080883005f01080881805fd108088ff040d118088ed041d118088db0431118088d10442118088c50452118081ba0451118081ba0452118081bc044f118081d80448118081f504381180810a05361180810f05361180811405371180811805391180881305451180880c05461180880a05411180880d0541118088fe044d118088e90467118088d4047f118088bf048d118088b40496118088a8049c1180889d04a61180819604a91180819704ab1180819804ab118081a004a8118081b9049b118081d40488118081e6047e11808104057011808118056b1180812705661180812805661180812805681180812d056b1180882005681180880f054c1180882405051180888905961080881a0637108088ae06f00f80883807bb0f8088a2079a0f8088f0078c0f80882c08800f808869086b0f8088a8084e0f8088d908360f8088fb08190f80881209f70e80882709db0e80883a09cd0e80883f09c10e80883209bf0e80881d09c80e8088fb08de0e8088da08f40e8088c108090f8088aa081e0f808896082a0f80887e08300f808868082d0f80885808240f80884a081d0f80883b08190f80882c08190f808819081b0f80880a08190f80880508140f8088fb07130f8088ee07130f8088e507180f8088e1071f0f8088e5071d0f8088e307220f8088dc071e0f8088d307190f8088ce07110f8088cb070b0f8088ce07040f8088db07fa0e8088e907f10e8088f807ea0e8088fa07eb0e8088f707ee0e8088f407f00e8088f507ec0e8088f907e70e8088f707de0e8088f407dc0e8088ed07dc0e8088ed07da0e8088ef07d70e8088f407cf0e8088f607c70e8088f707c30e8088ee07c20e8088f007c00e8088ec07bf0e8088f007bc0e8088f407bd0e8088fa07bf0e8088fa07c50e8088f907cd0e8088f407d60e8088ef07e10e8088eb07eb0e8088ed07f90e8088e907120f8088e707300f8088e407580f8088e707840f80880f088e0f808858086f0f8088a508520f8088b008720f80885008d50f8088b00754108088f106e710808824067d1180883605231280883b04ce1280884603601380885702d1138088800126148088a50051148088dfff451480881dff1014808856fec2138088b9fd6d13808833fd1a138088acfcde12808821fcbf1280889dfbb012808829fbaa128088c6faba1280886bfad812808829fa0013808801fa29138088eaf962138088d5f9a1138088c2f9e6138088abf9321480889df9761480889df976148080000000808088aaf993148088aaf993148088b6f9a5148088bef992148088bff975148088cef951148088edf936148088fef93014808808fa1c14808803fa10148088faf906148088f3f9fd138088e1f9fd138088cef9f7138088c2f9ee138088bef9e9138088baf9ea138088b4f9ed138088acf9ed1380889ff9e91380818cf9e613808181f9ec1380817ef9f413808182f9f7138081b5f9f0138081f1f9e51380812efac913808150fabc1380815cfabe1380884cfacd13808824fa02148088f7f91d148088dff922148088cbf919148088b9f91a148088a9f92514808191f93614808187f93f14808187f9471480818af95d1480818ef97414808193f99a14808196f9b11480889bf9b91480889df9a2148088aaf972148088b3f946148081b2f947148081b6f948148081d1f941148081fcf92b1480812cfa1014808150fa0714808160fa0714808161fa1f14808151fa4c14808148fa861480813bfab314808139facd14808129fad714808816faca148088f8f9b1148088dff996148081c7f987148081bcf986148081b4f989148081bbf98d148081caf985148081eff97614808118fa6a14808129fa6c14808821fa83148088fff9b5148088ccf9e4148081abf9f5148081a0f9f5148081a1f9f3148081b2f9e6148081d3f9cb148081fdf9b21480811ffa9e1480812efa9814808835fa8d14808829fa7d1480881ffa5514808818fa271480880efa14148081fff910148081fff913148081faf915148081f6f953148081eef99a148081dbf9ee148081c6f931158081b5f95c1580819ff9711580888ff97515808880f96515808876f94b15808871f92d1580886df9201580886cf91a15808166f92415808166f92815808173f92f15808191f938158081bbf941158081e3f94a15808115fa591580813bfa6515808158fa6815808173fa6a1580818afa67158081a3fa66158088c5fa4e15808800fb1415808860fbaa148088befb4d1480880ffc151480884dfcfd1380886ffcf113808882fce01380888dfcc7138088a1fca5138088adfc89138088b5fc7c138088b4fc84138088b8fc94138088b8fca6138088aafcb613808890fcdf13808867fc0514808853fc0914808843fc001480883efcf61380883efceb1380883efcd713808843fcbc13808847fcab13808848fca713808848fcb91380813efcc51380813efcdf1380813dfc091480813ffc481480813ffc8b14808145fcbc14808849fccf14808848fcad14808849fc7e14808853fc4e14808156fc4a14808154fc4c14808163fc5314808175fc561480819bfc54148081b8fc4f148081cffc47148088ccfc4e148088a7fc6314808873fc7c14808841fc9814808819fcb0148088f8fbce148088d5fbf4148081bafb13158081b0fb18158081b0fb17158081c1fb13158081e6fb061580811ffcf31480815bfce7148081a2fcdb148081d4fcd3148081fbfcc814808112fdc514808112fdc314808105fdbf148088dffcaf148088c6fc7c148088cbfc1b148088f2fcad1380882cfd581380886afd2c138088aafd25138088e1fd3413808818fe4113808855fe5513808898fe75138088ebfe941380883fffa51380888affa9138088ceffab138088f9ffb31380880800c21380880100ba138088f2ffc3138088d9ffe5138088caff15148088b4ff23148088a1ff1a1480887dff2514808858ff301480883bff3b14808826ff4a14808813ff52148088fafe5d148088dcfe62148088c4fe5c148088b3fe53148088a7fe4b14808198fe4914808195fe4c14808191fe501480819cfe5b148081bbfe5f148081eafe6014808125ff5014808152ff411480816bff3d14808173ff3a14808177ff3814808175ff3914808865ff291480886ffffe138088aeffc41380881a007b13808898003f1380881c01191380888f010c138088ec011a13808825023c1380883802731380882602ac1380881a02b3138088fa01d0138088eb01da138088f501cc1380880802c41380881d02c81380883602d21380884e02d61380885c02d31380886002cc1380885902c11380885402b41380884b02a41380883b029a1380882d0299138088220295138088120297138088050297138081f40192138081ee018e138081ec0190138081f8018f13808129027e1380816102721380818702711380819f02741380819e027a1380819602811380816502ad1380813302d51380810e02ef138081fa01f2138081fa01f1138081fa01ec1380810b02df1380813102cd1380815202bf1380817102b61380818402b41380888502b81380887002bd1380884602b61380882402a413808811029c1380880302a1138081f901aa138081ea01bb138081dd01e7138081d10128148081ca016f148081c101b7148081ac01f61480819e011315808199011a1580889c010b158088aa01d4148088c30193148088de0155148088fb0131148081fd012c148081ff013314808116022d1480813e021f14808166020e1480818602031480818e02031480887a021914808850022a148088250240148088060258148088f20174148081ec0177148081eb017a148081f6017b14808120026a1480814702531480816902421480817202451480816f024614808168024d1480814402801480811e02b6148081ff01ed148081f50103158081f90106158081fc01051580811a02f51480813c02e21480815c02d11480817502c11480818502bf1480889002b414808896029a14808899027c1480889b02631480889702601480819302671480819302701480819c0282148081ad029e148081b602bf148081c102da148081cc02f1148081d40200158081d602ff148081d602fc148081d502f8148088c702dc148088c102a9148088dc026214808808030c1480883c03be13808871037e138088b0034f138088ec033813808827042b1380885f04161380888404f01280889f04c7128088b204a5128088c4048f128088e7048a12808819058d1280884a05a41280886705cf1280885f050c1380885d052213808842054a1380881a05871380880505b7138088f404d0138088dd04d2138088bb04d71380889f04d11380889004c51380889004b01380889204a013808892049a1380888b04941380888404901380887c048a1380887704871380887504851380887704871380887a048c1380887d04901380888004911380887e04921380887a04941380887704981380887204951380886d048a1380886b048213808866047a1380886204751380886104741380886004751380885f04781380885c04721380885b04691380885d04621380885d045d13808859045b1380885e04581380885e04521380885d045313808154045413808151045613808153045d1380815b046113808167046f1380815b04611380816f047113808167046c13808184048613808177047a1380818d048f1380818404871380889b04ac1380889e04ac138088a604a3138088b4048c138088bd0476138088bc0471138088b80476138088b2047b138088af0481138088af047f138088af047d138088ad047a138088ab0475138088ac0472138088a9046e138088a7046a138088a604671380889f046913808898046a1380889204681380888c04641380888404671380887d046c1380887604741380887604751380887804751380887c04751380887d04741380888104721380887d046f1380887c046a1380887b046d1380887c04711380887c04761380887e047a1380887e047c1380887e047e1380887d047c1380887a04781380887a047a13808877047d13808878048613808873049113808872049a1380886d04a11380886b04a71380886804b01380886504b51380886604b81380886a04bb1380887104bb1380887b04b91380888e04b4138088a404ab138088bd04a4138088d50497138088e0048e138088e1048c138088d8048f138088cd0496138088c4049c138088c0049e138088b904a1138088b204a6138088ac04ab138088a404ab1380889f04a91380889a04ab1380889604b01380888e04b61380888404bc1380887504c31380886904cc1380885e04d01380885e04cf1380885d04ce1380885b04d11380885804d71380885604dc1380885504e01380884e04e21380884904e21380883f04e41380883204e21380882804df1380881e04e01380881104e61380880104ed138088f803f6138088ef03fe138088eb0307148088e6030a148088e7030b148088e7030a148088ea0307148088eb0306148088ea0307148088e60309148088de030e148088d6030e148088cd0310148088c0030f148088b8030d148081b0030e148081b30311148081b20318148081c50314148081ed03121480811204001480813904f81380814b04f41380815904f21380815604f51380884e04f11380883c04da1380883304b91380882f04941380883104781380882a04661380812004691380811904671380811804691380811a04781380811f04ab1380811d04001480811504601480811104b11480810c04e01480810e04f61480810b04f81480810f04f31480810704de1480810404a11480880b044b14808813041a1480881104061480810c040514808107040b148081f50322148081e30352148081cf037c148081cb0392148088d5038e148088e10369148088f9034014808814041d1480882704131480812d041e14808139042e14808150043e1480816b04521480817e04661480818f04731480818e04731480818f04711480818e046d1480888c045c148088910424148088b404df138088e504a11380880f057213808824055013808826053d1380881e05391380880f053913808105053113808106052f13808101053313808107053c13808105054a13808105053b138081020551138081f60476138081e70494138088cc04b6138088bc04bc138088ba04af138088c3049d138088c50496138088c40499138088bc04a3138088b404b0138088a904bc1380889f04c21380889904c31380888f04c01380888804bf1380887b04c51380817204d21380817504d81380818804e0138081b204d8138081e404ce1380810605c91380812105be1380812405c01380881f05be1380880a05b7138088f904a4138088f40485138088f10469138088e9045a138088df044c138081d3043f138081cf0436138081ce043b138081d00443138081d80475138081d804c0138081d7040c148081d60436148081d40444148081d60446148088d10433148088d3040a148088d104e6138088cb04cd138081c604d6138081bd04dd138081b404f2138081a104161480819d0426148088a10429148088b10406148088cd04de138088ed04cb138081f704da1380810405e71380811405ef1380812505fa1380813c05fc13808842050914808836051b14808821052014808817051514808811050e148081060515148081f5041f148081de0439148081c8045e148081b204811480819b049f1480819204a91480819204a81480819504a51480819c04a9148081c404bc148081e804cb1480810c05da1480812105e11480812a05e91480882805e31480882205c51480882805971480883c056a14808840055a14808832055f1480811f05711480811205751480811005761480810a057b148081e904a2148081bf04d11480819e04eb1480818c04fd1480888204f11480888004cf1480888904ae1480888e049714808889048c1480887e048614808872048214808871047d14808871047c14808879047914808173048014808177047f148081920480148081c2046c148081fc04661480812c056714808154056214808169055514808172055314808172055514808173055814808174055a1480886e055e1480884d05561480883f05291480885b05e313808895059b138088d205701380880a065413808839063e13808858062b1380886506161380887606f31280889006c3128088be0691128088db067a128088ed0679128088f60682128088f60693128088f106a6128088e106bd128088cf06d1128088c306e4128088bb06f2128088be06ee128088c706e4128088d106df128088d306db128088d406d7128088cf06d3128088c506d4128088c206e0128088c706e6128088df06da1280880207c71280881a07c41280881d07d61280880607ed128088f306f0128088f006de128088fb06be12808814079f12808830078712808844077812808859076c1280886907631280887807591280887d07541280887f07521280887b07541280887607591280887a075e1280888b075b1280889d0752128088b10743128088b90732128088c1071e128088bc070c128088b507fb118088b107f0118088b207e1118088b507d7118088bd07cc118088c507c0118088cf07b4118088d307ae118088d607ac118088db07ab118088e307a7118088ed07a2118088f1079e118088f2079b118088eb079a118088ea0796118088e40795118088e3078f118088e0078d118088e3078e118088e9078f118088e80793118088e90797118088e4079f118088e407a9118088e307b1118088e507b9118088ea07c0118088f107c3118088f207ca118088f007d7118088e807e5118088dc07f1118088d507fa118088ca0701128088bf0704128088bd070a128088ba0713128088aa0722128088a007371280889707531280888f07781280888307a41280886407db128088310724138088c806941380882c060d1480886a057d1480887e040e1580886503c61580883e02a7168088fb00b6178088a3ffe51880882bfe351a8088a4fc921b808823fbdb1c808895f9f91d808812f8d01e808876f67d1f8088d8f40d2080882df37e2080887cf1d8208088c2ef0d21808808ee2d21808837ec532180883dea9121808840e8fe21808830e6782280881be4ee228088cde12b23808865df73238088d0dcf5238088f8d98f248088f8d98f24808000000080";
//		String address = getLastDevice();
//		DeviceObject device = mBufferDevices.get(address);
//		
//		byte[] data = new byte[testData.length() / 2];
//		for(int i = 0;i<testData.length() / 2;i++){
//			data[i] = (byte)Integer.parseInt(testData.substring(i*2, i*2+2), 16);
//		}
//		
//		handlerPenData(device, data);
	}
}
