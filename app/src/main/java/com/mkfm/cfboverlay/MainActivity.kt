package com.mkfm.cfboverlay

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

/**
 * This screen only exists to get the Accessibility Service enabled once.
 * After that, this app never needs to be opened again — the service runs
 * in the background and the overlay is triggered entirely by the remote
 * button, from inside whatever app happens to be on screen.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }
}
