package com.example.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.example.R

object FloatingOverlayHelper {
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null

    fun checkAndRequestOverlayPermission(activity: Activity, requestCode: Int = 1234) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(activity)) {
                Toast.makeText(activity, "Izinkan 'Draw over other apps' untuk menggunakan Floating Overlay", Toast.LENGTH_LONG).show()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${activity.packageName}")
                )
                activity.startActivityForResult(intent, requestCode)
            }
        }
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun showOverlay(context: Context, responseText: String) {
        if (!hasOverlayPermission(context)) {
            Toast.makeText(context, "Izin Overlay belum diberikan!", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            if (windowManager == null) {
                windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            }

            if (overlayView != null) {
                // Update text if already showing
                val tv = overlayView?.findViewById<TextView>(R.id.tv_overlay_content)
                tv?.text = responseText
                return
            }

            val inflater = LayoutInflater.from(context)
            overlayView = inflater.inflate(R.layout.popup_layout, null)

            val tvContent = overlayView?.findViewById<TextView>(R.id.tv_overlay_content)
            val btnClose = overlayView?.findViewById<ImageButton>(R.id.btn_close_overlay)

            tvContent?.text = responseText

            btnClose?.setOnClickListener {
                hideOverlay()
            }

            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = 150
            }

            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hideOverlay() {
        try {
            if (overlayView != null && windowManager != null) {
                windowManager?.removeView(overlayView)
                overlayView = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            overlayView = null
        }
    }
}
