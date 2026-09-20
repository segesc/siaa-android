package com.siaa.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.siaa.app.media.SiaaPlaybackService
import com.siaa.app.ui.SiaaApp
import com.siaa.core.model.SessionMode

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        setContent {
            SiaaApp(
                viewModel = viewModel,
                onStartMode = ::startMode,
                onStop = ::stopSession,
                onStartCalibration = ::startCalibration,
                onStopCalibration = ::stopCalibration
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun startMode(mode: SessionMode) {
        val preferences = (application as SiaaApplication).graph.preferenceStore.current()
        val intent = Intent(this, SiaaPlaybackService::class.java).apply {
            action = SiaaPlaybackService.ACTION_START_SESSION
            putExtra(SiaaPlaybackService.EXTRA_MODE, mode.name)
            putExtra(SiaaPlaybackService.EXTRA_MAX_ITEMS, preferences.maxItems)
            putExtra(SiaaPlaybackService.EXTRA_ANNOUNCE_CONTROLS, preferences.announceControls)
            putExtra(SiaaPlaybackService.EXTRA_FEEDBACK_EXPLANATIONS, preferences.feedbackExplanations)
            putExtra(SiaaPlaybackService.EXTRA_SPEECH_RATE, preferences.speechRate)
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopSession() {
        startService(Intent(this, SiaaPlaybackService::class.java).apply {
            action = SiaaPlaybackService.ACTION_STOP_SESSION
        })
    }


    private fun startCalibration() {
        ContextCompat.startForegroundService(this, Intent(this, SiaaPlaybackService::class.java).apply {
            action = SiaaPlaybackService.ACTION_START_CALIBRATION
        })
    }

    private fun stopCalibration() {
        startService(Intent(this, SiaaPlaybackService::class.java).apply {
            action = SiaaPlaybackService.ACTION_STOP_CALIBRATION
        })
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
