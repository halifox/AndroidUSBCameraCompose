package com.github.auvc

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.github.auvc.ui.theme.AndroidUSBCameraCompose2Theme
import com.herohan.uvcapp.CameraHelper
import com.herohan.uvcapp.ICameraHelper
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


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

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidUSBCameraCompose2Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AndroidUsbCameraDemo()
                }
            }
        }
    }
}

