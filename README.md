# Android USB Camera & Jetpack Compose 示例

本项目展示了如何在 Android Jetpack Compose 中集成并使用 [AndroidUSBCamera](https://github.com/jiangdongguo/AndroidUSBCamera) 库，以实现 USB 摄像头接入功能。

## 使用说明

- 在使用 `usbMonitor.register()` 和 `usbMonitor.unregister()` 方法时，请注意，这些方法用于注册和注销 USB 设备的插拔事件，具体通过 `USBMonitor.OnDeviceConnectListener` 中的 `onAttach` 和 `onDetach` 回调进行处理。

- 无论是否已获取权限，在调用 `cameraUVC.openCamera()` 之前，都需要先调用一次 `usbMonitor.requestPermission()` 以确保设备权限。

- 当设备没有权限（`usbMonitor.hasPermission` 为 `false`）时，调用 `usbMonitor.requestPermission()` 会触发系统弹出对话框，询问用户是否允许应用访问 USB 视频设备。
- 若用户点击“确定”，则会触发 `USBMonitor.OnDeviceConnectListener.onConnect` 回调；
- 若点击“取消”，则会触发 `USBMonitor.OnDeviceConnectListener.onDisconnect` 回调。

- 若设备已经拥有权限（`usbMonitor.hasPermission` 为 `true`），直接调用 `usbMonitor.requestPermission()` 会直接触发 `onConnect` 回调。

- 设置系统签名和`android:sharedUserId="android.uid.system"`后，可以无需询问用户是否允许应用访问 USB 视频设备。