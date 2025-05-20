# Android USB Camera & Jetpack Compose 示例

## 快速接入并使用

1. 在 build.gradle 中添加依赖：

```kotlin
    implementation("com.herohan:UVCAndroid:1.0.9")
```

2. 使用

```kotlin
@Composable
fun AndroidUsbCamera(usbDevice: UsbDevice?, modifier: Modifier = Modifier) {
    if (usbDevice == null) return
    val context = LocalContext.current
    val surfaceView = remember { SurfaceView(context) }
    DisposableEffect(usbDevice) {
        val mCameraHelper = CameraHelper()
        mCameraHelper.setStateCallback(object : ICameraHelper.StateCallback {
            override fun onAttach(device: UsbDevice?) {}
            override fun onDeviceOpen(device: UsbDevice?, isFirstOpen: Boolean) {}
            override fun onCameraOpen(device: UsbDevice?) {}
            override fun onCameraClose(device: UsbDevice?) {}
            override fun onDeviceClose(device: UsbDevice?) {}
            override fun onDetach(device: UsbDevice?) {}
            override fun onCancel(device: UsbDevice?) {}
        })
        mCameraHelper.selectDevice(usbDevice)
        mCameraHelper.openCamera()
        mCameraHelper.addSurface(surfaceView.holder.surface, false)
        mCameraHelper.startPreview()
        onDispose {
            mCameraHelper.removeSurface(surfaceView.holder.surface)
            mCameraHelper.release()
        }
    }
    AndroidView({ surfaceView }, modifier)
}
```

## 示例

```kotlin
@Composable
fun AndroidUsbCameraDemo(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val mCameraHelper = CameraHelper()
    var usbDevice by remember { mutableStateOf<UsbDevice?>(null) }
    DisposableEffect(Unit) {
        //
        fun setUsbDevice() {
            val deviceList = mCameraHelper.deviceList.filter { it.productName == "USB Video" }
            usbDevice = deviceList.getOrNull(0)
        }


        setUsbDevice()
        //息屏后 亮屏前对应的USB设备的deviceName会发生改变 所以监听亮屏广播 重新获取设备列表
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_SCREEN_ON) {
                    MainScope().launch {
                        delay(2000)//亮屏后 重新获取usb设备需要时间
                        setUsbDevice()
                    }
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_SCREEN_ON))
        onDispose {
            context.unregisterReceiver(receiver)
            mCameraHelper.release()
        }
    }
    AndroidUsbCamera(usbDevice, modifier)
}
```

## 资源

- [shiyinghan/UVCAndroid](https://github.com/shiyinghan/UVCAndroid)
- [UVCAndroid，安卓UVC相机通用开发库（支持多预览和多摄像头）](https://blog.csdn.net/hanshiying007/article/details/124118486)
- [Android相机调用-libusbCamera【外接摄像头】【USB摄像头】 【多摄像头预览】](https://blog.csdn.net/huahua520amy/article/details/136261508)
