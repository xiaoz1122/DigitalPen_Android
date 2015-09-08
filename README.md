# DigitalPen For Android
这是易方物联为旗下产品**数码笔**提供的Android开源项目，用于让开发者快速的进行开发。


## 目录说明 ##
- <b>SmartPenCore：</b>数码笔SDK核心库。
- <b>SmartPenSample：</b>数码笔演示项⺫代码。


## 开发环境 ##
- UTF-8
- Eclipse
- Android 4.3
- JDK 1.6


## 集成SDK方式 ##
1. 使用项目依赖的方式导入SmartPenCore项目。
2. 引用jar包的方式导入SmartPenCore.jar包调用，jar文件在SmartPenCore/bin目录下。


## 配置AndroidManifest.xml ##
1. 打开您项目的“AndroidManifest.xml”，在其中添加如下的权限：
```
	<uses-permission android:name="android.permission.BLUETOOTH" />
	<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
```

2. BLE蓝牙最低支持Android版本为4.3，需添加如下版本支持：
```
	<uses-sdk android:minSdkVersion="18" android:targetSdkVersion="18" />
```

3. 添加数码笔蓝牙服务
```
	<service android:name="com.smart.pen.core.services.SmartPenService" android:enabled="true">
		<intent-filter android:priority="10000">  
		<action android:name="android.bluetooth.adapter.action.STATE_CHANGED"/>  
	  </intent-filter>
	</service>
```

## 启动并绑定数码笔服务 ##
参照SmartPenSample项目MainActivity类onCreate函数里的SmartPenApplication.getInstance().bindPenService();


## 扫描数码笔设备 ##
首先需要确认手机蓝牙是否已打开，然后执行以下步骤：

1. 定义com.smart.pen.core.common.Listeners.OnScanDeviceListener监听接口；
2. 执行SmartPenService服务里的scanDevice(onScanDeviceListener)函数；
3. 通过监听接口find(DeviceObject device)返回最新扫描到的设备；
4. 通过监听接口complete(HashMap<String, DeviceObject> list)告知扫描已完成，并返回所有扫描到的设备集合；
5. 执行SmartPenService服务里的stopScanDevice()可强制停止执行扫描。


其他方法：

**SmartPenService.scanDevice(OnScanDeviceListener listener,String prefix)**
- listener:扫描监听接口
- prefix:扫描设备的前缀，用于更改过蓝牙名称的OEM设备，默认为“null”全部显示。


## 连接数码笔设备 ##
**1. 获取需要连接设备的MAC地址**
取到扫描返回的设备对象DeviceObject后，可通过对象的address属性获取到MAC地址。

**2. 连接设备**
拿到MAC地址后可调用SmartPenService服务里的connectDevice(OnConnectStateListener listener,String address)方法进行设备连接。

**3.获取连接状态**
连接状态会通过OnConnectStateListener.stateChange(String address,ConnectState state)返回。

**4.连接完成**
当返回值为PEN_INIT_COMPLETE时表示笔已连接成功并初始化完成，可以进行后续操作。

## 获取笔坐标信息**
连接成功后，可通过SmartPenService服务里的setOnPointChangeListener(OnPointChangeListener listener)方法，监听笔的坐标数据。
OnPointChangeListener.change方法实时返回PointObject对象，公开属性有：

- originalX：笔相对于接收器的实际X轴坐标，单位px；
- originalY：笔相对于接收器的实际Y轴坐标，单位px；
- isRoute：表示当前输出的坐标对象是否为笔迹，false表示当前笔为悬空状态；
- isSw1：表示当前数码笔上的按键1是否被按下，false表示没有被按下；
- battery：表示电量信息。当数码笔电量过低时，会间隔发送BatteryState.LOW信号。


#####PointObject对象公开方法：#####
- 设置场景类型，目前支持A4、A5和自定义，本SDK默认选择为A5；
```	setSceneType(SceneType type) ```
	
- 自定义场景宽度和高度，设置后sceneType自动被切换为SceneType.CUSTOM，且笔迹只能在这个范围内被输出。
```	setCustomScene(short width,short height,short offsetX,short offsetY) ```
> 参数说明：
> width			纸张场景宽度  
> height		纸张场景高度  
> offsetX		自定义区域中心点离接收器X中心的偏移量  
> offsetY		自定义区域顶部坐标与接收器Y起点的偏移量  

- 获取当前场景的宽，单位px。
```	getWidth() ```
		
- 获取当前场景的高，单位px。
```	getHeight() ```
	
- 笔相对于当前场景的X轴坐标，单位px；
```	getSceneX() ```

- 笔坐标相对于showWidth等比缩放后的X轴坐标，单位px；	
```	getSceneX(int showWidth) ```
	
- 笔相对于当前场景的Y轴坐标，单位px；
```	getSceneY() ```
	
- 笔坐标相对于showHeight等比缩放后的Y轴坐标，单位px；
```	getSceneY(int showHeight) ```
	

提示：
> 当isRoute由false变为true时，可视为Down；  
> 当isRoute由true变为false时，可视为Up；  
> 当isRoute持续为true时，可视为Move。  

## 自定义画布 ##
自定义画布是指用户在非标准尺寸（非A4、A5等未知尺寸）的纸张上书写时，使用数码笔定位纸张的左上角和右下角坐标，SDK就能自动适配当前画布输出按照当前屏幕尺寸转换后的坐标。


需要使用自定义画布功能，首先需要设置监听SmartPenService服务的以下两个事件：

- OnPointChangeListener 笔坐标更改事件监听，用于获取定位的坐标信息。
- OnFixedPointListener 固定坐标事件监听，用于确定当前是否已完成坐标确认。

OnFixedPointListener会通过location(PointObject first, PointObject second,LocationState state)方法返回定位状态。
> 参数说明：
> first		定位的第一个点（左上角）
> second	定位的第二个点（右下角）
> state		定位状态

我们只需要根据state返回的定位状态，做出相应的动作即可：

- LocationState.FirstComp		已完成第一个点，开始定位第二个点；
- LocationState.SecondComp		已完成第二个点，定位完成；
- LocationState.LocationSmall	定位范围太小，重新定位第二个点；
- LocationState.DontLocation	没有定位信息，开始定位第一个点。

详细操作请参考SmartPenSample项目里的SettingSize类。