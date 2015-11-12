package com.smart.pen.core.services;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import com.smart.pen.core.common.Listeners.OnScanDeviceListener;
import com.smart.pen.core.model.DeviceObject;
import com.smart.pen.core.model.PointObject;
import com.smart.pen.core.symbol.ConnectState;
import com.smart.pen.core.symbol.Keys;
import com.smart.pen.core.symbol.LocationState;
import com.smart.pen.core.utils.UsbPenUtil;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.AsyncTask;
import android.os.Binder;
import android.util.Log;

/**
 * 
 * @author Xiaoz
 * @date 2015年9月30日 上午10:04:57
 *
 * Description
 */
public class UsbPenService extends PenService{
	public static final String TAG = UsbPenService.class.getSimpleName();

	private UsbManager mUsbManager;
	private UsbEndpoint mUsbEndpoint;
	private UsbInterface mUsbInterface;
	private UsbDeviceConnection mUsbDeviceConnection;
	/**当前连接USB设备**/
	private UsbDevice currUsbDevice;
	private UsbStateReceiver mUsbStateReceiver;
	
	private boolean isReadData;

	
	@Override
	public void onCreate() {
		super.onCreate();

		mUsbStateReceiver = new UsbStateReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		intentFilter.addAction(Keys.ACTION_USB_PERMISSION);
		registerReceiver(mUsbStateReceiver, intentFilter);
		
		setScanTime(5000);
	}
	
	@Override
	public void onDestroy() {
		disconnectDevice();
		unregisterReceiver(mUsbStateReceiver);
		super.onDestroy();
	}

	@Override
	public String getSvrTag() {
		return Keys.APP_USB_SERVICE_NAME;
	}
	
	@Override
	public short getReceiverGapHeight() {
		return 880;
	}

	@Override
	public DeviceObject getConnectDevice() {
		if(currUsbDevice != null){
			DeviceObject device = new DeviceObject(currUsbDevice);
			return device;
		}
		return null;
	}

	@Override
	public ConnectState checkDeviceConnect(){
		ConnectState result = ConnectState.NOTHING;
		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		for(UsbDevice device :mUsbManager.getDeviceList().values()){
			
			if(device.getVendorId() == 3616 && device.getProductId() == 257){
				currUsbDevice = device;
				break;
			}
		}
		if(currUsbDevice != null){
			boolean hasPermission = mUsbManager.hasPermission(currUsbDevice);
			boolean hasClaim = false;
			if(hasPermission){
				mUsbDeviceConnection = mUsbManager.openDevice(currUsbDevice);
				if(mUsbDeviceConnection != null){
					mUsbInterface = currUsbDevice.getInterface(0);
					mUsbEndpoint = mUsbInterface.getEndpoint(0);
					hasClaim = mUsbDeviceConnection.claimInterface(mUsbInterface, true);
					
					if(hasClaim){
						UsbPenUtil.clearDataBuffer();
						result = ConnectState.CONNECTED;
					} 
				}			
			}else{
				result = ConnectState.CONNECT_FAIL_PERMISSION;
			}
		}
		
		if(result != ConnectState.CONNECTED){
			if(mUsbDeviceConnection != null){
				mUsbDeviceConnection.releaseInterface(mUsbInterface);
				mUsbDeviceConnection.close();
			}
			mUsbDeviceConnection = null;
			mUsbInterface = null;
		}
		
		return result;
	}

	@Override
	public ConnectState disconnectDevice(){
		stopReadData();
		UsbPenUtil.clearDataBuffer();
		if(mUsbDeviceConnection != null){
			mUsbDeviceConnection.releaseInterface(mUsbInterface);
			mUsbDeviceConnection.close();
		}
		mUsbDeviceConnection = null;
		mUsbInterface = null;
		currUsbDevice = null;
		sendConnectState(null,ConnectState.DISCONNECTED);
		return ConnectState.DISCONNECTED;
	}


	@Override
	public boolean scanDevice(OnScanDeviceListener listener) {
		this.onScanDeviceListener = listener;
		if(!isScanning){
			new ScanDeviceTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		return true;
	}

	@Override
	public void stopScanDevice(){
		if(isScanning){
			isScanning = false;
		}
	}

	@Override
	public void sendFixedPointState(LocationState state) {
		if(onFixedPointListener != null)
			onFixedPointListener.location(mFirstPointObject, mSecondPointObject, state);
	}

	@Override
	public void handlerPointInfo(PointObject point) {
		//USB过来的不是异步数据，所以不需要Handler send处理
		sendPointInfo(point);
	}
	
	public void startReadData(){
		if(!isReadData)
			new ReadDataTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
	
	public void stopReadData(){
		isReadData = false;
	}

	public class LocalBinder extends Binder {
		/**获取服务对象**/
		public UsbPenService getService() {
			return UsbPenService.this;
		}
	}
	
	private class ScanDeviceTask extends AsyncTask<Void, Integer, ConnectState> {
        @Override
        protected ConnectState doInBackground(Void... params) {
			isScanning = true;
        	ConnectState result = ConnectState.NOTHING;
            int timer = 0;
            //检查连接状态
            while ((result = checkDeviceConnect()) != ConnectState.CONNECTED){
            	timer++;
            	if(timer >= mScanTime / 100)break;
            	if(result == ConnectState.CONNECT_FAIL_PERMISSION)break;
            	if(!isScanning)break;
            	
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return result;
        }
        @Override
        protected void onPostExecute(ConnectState result) {
        	isScanning = false;
			sendConnectState(null,result);
			
			if(result == ConnectState.CONNECTED){
				startReadData();
			}else if(result == ConnectState.CONNECT_FAIL_PERMISSION){
				PendingIntent intent = PendingIntent.getBroadcast(
											UsbPenService.this, 
											Activity.RESULT_OK, 
											new Intent(Keys.ACTION_USB_PERMISSION), 
											0);
				mUsbManager.requestPermission(currUsbDevice, intent);
			}
        }
    }
	
	private class ReadDataTask extends AsyncTask<Void, ByteBuffer, ConnectState> {
        @Override
        protected ConnectState doInBackground(Void... params) {
        	ConnectState result = ConnectState.CONNECTED;
			
			int inmax = mUsbEndpoint.getMaxPacketSize();
			ByteBuffer buffer = ByteBuffer.allocate(inmax);
			buffer.order(ByteOrder.nativeOrder());
			UsbRequest usbRequest = new UsbRequest();
			boolean isOpen = usbRequest.initialize(mUsbDeviceConnection, mUsbEndpoint);
			if(isOpen){
	        	isReadData = true;
				byte[] modeBuffer = {0x02,0x04,(byte)0x80,(byte)0xB5,0x01,0x01};
				int value = mUsbDeviceConnection.controlTransfer(0x21, 0x9, 0x200, 0,modeBuffer,6, 10);
				Log.v(TAG,"transfer:"+value);
				while(isReadData){
					if(currUsbDevice == null
							|| mUsbDeviceConnection == null){
						result = ConnectState.CONNECT_FAIL;
						break;
					}
									
					usbRequest.setClientData(UsbPenService.this);
					boolean isrequest = usbRequest.queue(buffer, inmax);
					if(isrequest){
						if(mUsbDeviceConnection.requestWait() == usbRequest){
		                    publishProgress(buffer);			
						}
					}
					
					try {
						Thread.sleep(10);
					} catch (InterruptedException e){
						e.printStackTrace();
					}
				}
			}
			buffer.clear();
			usbRequest.close();
            return result;
        }
        @Override
        protected void onProgressUpdate(ByteBuffer... progresses) {
        	byte[] usbData = progresses[0].array();
        	
    		List<PointObject> pointList = UsbPenUtil.getPointList(usbData);
    		handlerPointList(pointList);
        }
        @Override
        protected void onPostExecute(ConnectState result) {
			if(result != ConnectState.CONNECTED){
				//异常断开，尝试重新连接设备
				scanDevice(null);
			}
        }
    }
	
	private class UsbStateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)){
				//USB插入
				scanDevice(null);
				
			}else if(UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)){
				//USB拔出
				disconnectDevice();
			}else if(Keys.ACTION_USB_PERMISSION.equals(action)){
				//授权完成
				scanDevice(null);
			}
		}
	}
}
