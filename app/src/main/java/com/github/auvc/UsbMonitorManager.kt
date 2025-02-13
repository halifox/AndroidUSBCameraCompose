package com.github.auvc

import android.content.Context
import android.hardware.usb.UsbDevice
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.jiangdg.ausbc.camera.CameraUVC
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.render.env.RotateType
import com.jiangdg.ausbc.utils.CameraUtils.isFilterDevice
import com.jiangdg.ausbc.utils.CameraUtils.isUsbCamera
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.usb.USBMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class UsbMonitorManager(context: Context) {
    companion object {
        private const val TAG = "UsbMonitorManager"
    }

    private val _cameras = mutableMapOf<Int, CameraUVC>()
    val cameras = MutableStateFlow(emptyList<CameraUVC>())


    private val listeners = mutableListOf<OnDeviceConnectListener>()

    fun addOnDeviceConnectListener(listener: OnDeviceConnectListener) {
        listeners += listener
    }

    fun removeOnDeviceConnectListener(listener: OnDeviceConnectListener) {
        listeners -= listener
    }


    val usbMonitor = USBMonitor(context, object : USBMonitor.OnDeviceConnectListener {
        /**
         * 当usb设备插入
         */
        override fun onAttach(device: UsbDevice?) {
            Log.d(TAG, "onAttach: ${device?.deviceName}")
            if (checkCamera(context, device)) {
                _cameras.putIfAbsent(device.deviceId, CameraUVC(context, device))
                cameras.value = _cameras.values.toList()
            }
            listeners.forEach { it.onAttach(device) }
        }

        /**
         * 当usb设备退出
         */
        override fun onDetach(device: UsbDevice?) {
            Log.d(TAG, "onDetach: ${device?.deviceName}")
            _cameras[device?.deviceId]?.setUsbControlBlock(null)
            _cameras[device?.deviceId]?.closeCamera()
            _cameras.remove(device?.deviceId)
            cameras.value = _cameras.values.toList()
            listeners.forEach { it.onDetach(device) }
        }

        /**
         * 当请求USB摄像头权限 被允许
         */
        override fun onConnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?, createNew: Boolean) {
            Log.d(TAG, "onConnect: ${device?.deviceName}")
            _cameras[device?.deviceId]?.setUsbControlBlock(ctrlBlock)
            cameras.value = _cameras.values.toList()
            listeners.forEach { it.onConnect(device, ctrlBlock, createNew) }
        }

        /**
         * 当请求USB摄像头权限 被拒接
         */
        override fun onDisconnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
            Log.d(TAG, "onDisconnect: ${device?.deviceName}")
            _cameras[device?.deviceId]?.closeCamera()
            listeners.forEach { it.onDisconnect(device, ctrlBlock) }
        }


        /**
         * 被未授权权限或请求权限异常调用
         */
        override fun onCancel(device: UsbDevice?) {
            Log.d(TAG, "onCancel: ${device?.deviceName}")
            _cameras[device?.deviceId]?.closeCamera()
            listeners.forEach { it.onCancel(device) }
        }
    })

    @OptIn(ExperimentalContracts::class)
    fun checkCamera(ctx: Context, device: UsbDevice?): Boolean {
        contract {
            returns(true) implies (device is UsbDevice)
        }
        return isUsbCamera(device) || isFilterDevice(ctx, device);
    }


    open class OnDeviceConnectListener : USBMonitor.OnDeviceConnectListener {
        override fun onAttach(device: UsbDevice?) {

        }

        override fun onDetach(device: UsbDevice?) {

        }

        override fun onConnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?, createNew: Boolean) {

        }

        override fun onDisconnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {

        }

        override fun onCancel(device: UsbDevice?) {

        }

    }
}

@Composable
fun UsbCameraView(
    usbMonitorManager: UsbMonitorManager,
    cameraUVC: CameraUVC,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
    ) {
        val context = LocalContext.current
        val cameraView = remember { AspectRatioTextureView(context) }
        var hasPermission by remember { mutableStateOf(usbMonitorManager.usbMonitor.hasPermission(cameraUVC.device)) }
        DisposableEffect(cameraUVC) {
            if (usbMonitorManager.usbMonitor.hasPermission(cameraUVC.device)) {
                //如果有权限 就不会弹出用户手动授权的对话框 直接`requestPermission`然后进入`onConnect`回调
                //如果没有权限 就显示一个按钮 让用户手动申请 因为每一个摄像头都需要用户手动点击'允许'一次 一次申请多个摄像头的权限会出现异常
                //  或者直接实现一个请求列队 回调为 `onConnect`或`onDisconnect`
                usbMonitorManager.usbMonitor.requestPermission(cameraUVC.device)
            }
            val listener = object : UsbMonitorManager.OnDeviceConnectListener() {
                override fun onConnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?, createNew: Boolean) {
                    if (device?.deviceId == cameraUVC.device.deviceId) {
                        hasPermission = usbMonitorManager.usbMonitor.hasPermission(cameraUVC.device)

                        val cameraRequest = CameraRequest.Builder()
                            .setPreviewWidth(1280)
                            .setPreviewHeight(720)
                            .setRenderMode(CameraRequest.RenderMode.OPENGL)
                            .setDefaultRotateType(RotateType.ANGLE_0)
                            .setAudioSource(CameraRequest.AudioSource.SOURCE_AUTO)
                            .setAspectRatioShow(true)
                            .setCaptureRawImage(false)
                            .setRawPreviewData(false)
                            .create()
                        cameraView.post {
                            cameraUVC.openCamera(cameraView, cameraRequest)
                        }
                    }
                }
            }
            usbMonitorManager.addOnDeviceConnectListener(listener)
            onDispose {
                usbMonitorManager.removeOnDeviceConnectListener(listener)
            }
        }
        AndroidView(
            factory = {
                cameraView
            },
            onRelease = {
                cameraUVC.closeCamera()
            },
        )

        if (!hasPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("需要授权才能访问")
                Button({
                    usbMonitorManager.usbMonitor.requestPermission(cameraUVC.device)
                }) {
                    Text("点击请求权限")
                }
            }
        }
        Text(cameraUVC.device.deviceName, color = Color.Green)
    }
}
