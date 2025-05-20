package com.github.auvc

import android.annotation.SuppressLint
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.auvc.ui.theme.AndroidUSBCameraCompose2Theme
import com.herohan.uvcapp.CameraHelper
import com.herohan.uvcapp.ICameraHelper

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
class MainActivity : ComponentActivity() {
    val mCameraHelper = CameraHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val mCameraViewMain = SurfaceView(this)
        mCameraHelper.setStateCallback(object : ICameraHelper.StateCallback {
            //插入UVC设备
            override fun onAttach(device: UsbDevice?) {
                Log.d("TAG", "onAttach:${device?.deviceName} ")
                //设置为当前设备（如果没有权限，会显示授权对话框）
//                mCameraHelper.selectDevice(device)
                if (device?.deviceName=="/dev/bus/usb/001/036"){
                    mCameraHelper.selectDevice(device)
                }
            }

            //打开UVC设备成功（也就是已经获取到UVC设备的权限）
            override fun onDeviceOpen(device: UsbDevice?, isFirstOpen: Boolean) {
                Log.d("TAG", "onDeviceOpen:${device} ${isFirstOpen}")
                //打开UVC摄像头
                mCameraHelper.openCamera()
            }

            //打开摄像头成功
            override fun onCameraOpen(device: UsbDevice?) {
                Log.d("TAG", "onCameraOpen:${device}")
                //开始预览
                mCameraHelper.startPreview()
                mCameraHelper.addSurface(mCameraViewMain.getHolder().surface, false)
            }


            //关闭摄像头成功
            override fun onCameraClose(device: UsbDevice?) {
                Log.d("TAG", "onCameraClose:${device}")

            }

            //关闭UVC设备成功
            override fun onDeviceClose(device: UsbDevice?) {
                Log.d("TAG", "onDeviceClose:${device}")

            }

            //断开UVC设备
            override fun onDetach(device: UsbDevice?) {
                Log.d("TAG", "onDetach:${device}")

            }

            //用户没有授予访问UVC设备的权限
            override fun onCancel(device: UsbDevice?) {
                Log.d("TAG", "onCancel:${device}")

            }

        })


//        Log.d("TAG", "mCameraHelper.deviceList:${mCameraHelper.deviceList} ")
//        mCameraHelper.selectDevice(mCameraHelper.deviceList[1])
        mCameraViewMain.getHolder().addCallback(object : SurfaceHolder.Callback {
            //创建了新的Surface
            override fun surfaceCreated(holder: SurfaceHolder) {
                mCameraHelper.addSurface(holder.surface, false)
            }

            //Surface发生了改变
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            }

            //销毁了原来的Surface
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                mCameraHelper.removeSurface(holder.surface)
            }
        })

        setContent {
            AndroidUSBCameraCompose2Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AndroidView({ mCameraViewMain })

//                    AndroidExternalSurface(
//                        modifier = Modifier
//                            .fillMaxSize()
//                            .padding(innerPadding),
//                        zOrder = AndroidExternalSurfaceZOrder.Behind,
//                        isOpaque = true,
//                        surfaceSize = IntSize.Zero,
//                        onInit = {
//                            onSurface { surface, width, height ->
//                                mCameraHelper.addSurface(surface, false)
//                            }
//                        }
//                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mCameraHelper.release();
    }
}

