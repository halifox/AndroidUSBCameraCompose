package com.github.auvc

import android.Manifest.permission.CAMERA
import android.Manifest.permission.RECORD_AUDIO
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private val context = this
    private val usbMonitorManager by lazy { UsbMonitorManager(context) }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val cameras by usbMonitorManager.cameras.collectAsState()
                    LazyVerticalGrid(
                        GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        items(cameras.size, key = { cameras[it].device.deviceId }) {
                            val cameraUVC = cameras[it]
                            UsbCameraView(usbMonitorManager, cameraUVC, Modifier.padding(8.dp))
                        }
                    }
                }
            }
        }
        //注册 BroadcastReceiver 以监控 USB 事件
        usbMonitorManager.usbMonitor.register()
        launcher.launch(arrayOf(CAMERA, RECORD_AUDIO))
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {

    }


    override fun onDestroy() {
        super.onDestroy()
        usbMonitorManager.usbMonitor.unregister()
        usbMonitorManager.usbMonitor.destroy()
    }
}




