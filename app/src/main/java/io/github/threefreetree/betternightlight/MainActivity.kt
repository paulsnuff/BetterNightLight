package io.github.threefreetree.betternightlight

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.threefreetree.betternightlight.shizuku.ShizukuPermissionManager
import io.github.threefreetree.betternightlight.ui.AppRoot
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var shizukuPermissionManager: ShizukuPermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                shizukuPermissionManager.tryGrantSilentlyInBackground()
            }
        }

        enableEdgeToEdge()
        setContent {
            AppRoot()
        }
    }
}
