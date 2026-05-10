package uzair.lightpods.android.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import uzair.lightpods.android.R
import uzair.lightpods.android.bluetooth.BleScanState

class OverlayPopupManager(
    private val context: Context
) {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isShowing = false
    private var isDismissedByUser = false
    private var lastDeviceAddress = ""
    private val handler = Handler(Looper.getMainLooper())

    fun showConnectionPopup(
        scan: BleScanState,
        deviceName: String = "AirPods Pro"
    ) {
        if (!canShowOverlay()) return
        if (isShowing) return
        if (isDismissedByUser &&
            scan.address == lastDeviceAddress
        ) return

        lastDeviceAddress = scan.address

        handler.post {
            try {
                createAndShowOverlay(scan, deviceName)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun dismissPopup() {
        handler.post {
            animateOut()
            isDismissedByUser = true
        }
    }

    fun resetDismissState() {
        isDismissedByUser = false
        lastDeviceAddress = ""
    }

    fun onDeviceLost() {
        handler.post { animateOut() }
        isDismissedByUser = false
    }

    private fun canShowOverlay(): Boolean {
        return if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.M
        ) {
            Settings.canDrawOverlays(context)
        } else true
    }

    private fun isDarkMode(): Boolean {
        val prefs = context.getSharedPreferences(
            "lightpods_settings",
            Context.MODE_PRIVATE
        )
        val theme = prefs.getString("theme_mode", null)
        return when (theme) {
            "DARK" -> true
            "LIGHT" -> false
            else -> {
                val uiMode = context.resources
                    .configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK
                uiMode == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createAndShowOverlay(
        scan: BleScanState,
        deviceName: String
    ) {
        if (isShowing) return

        windowManager = context.getSystemService(
            Context.WINDOW_SERVICE
        ) as WindowManager

        val layoutType = if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {
            WindowManager.LayoutParams
                .TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams
                .FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams
                    .FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
            x = 0
            y = 0
        }

        overlayView = buildSheet(scan, deviceName)

        var startY = 0f
        var dragY = 0f
        overlayView?.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.rawY
                    dragY = v.translationY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val delta = event.rawY - startY
                    if (delta > 0) {
                        v.translationY = dragY + delta
                    }
                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    val delta = event.rawY - startY
                    if (delta > dp(84)) {
                        dismissPopup()
                    } else {
                        v.animate()
                            .translationY(0f)
                            .setDuration(180)
                            .setInterpolator(
                                DecelerateInterpolator()
                            )
                            .start()
                    }
                    true
                }

                else -> false
            }
        }

        overlayView?.translationY = dp(420).toFloat()
        windowManager?.addView(overlayView, params)
        isShowing = true

        overlayView?.animate()
            ?.translationY(0f)
            ?.setDuration(460)
            ?.setInterpolator(
                DecelerateInterpolator(2.4f)
            )
            ?.start()

        handler.postDelayed({
            if (isShowing && !isDismissedByUser) {
                animateOut()
            }
        }, AUTO_DISMISS_MS)
    }

    private fun buildSheet(
        scan: BleScanState,
        deviceName: String
    ): View {
        val palette = overlayPalette()

        val root = FrameLayout(context).apply {
            setPadding(dp(12), 0, dp(12), dp(18))
            clipToPadding = false
        }

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    palette.surfaceHigh,
                    palette.surface
                )
            ).apply {
                cornerRadius = dp(34).toFloat()
            }
            setPadding(dp(18), dp(12), dp(18), dp(18))
            elevation = dp(18).toFloat()
            isClickable = true
            isFocusable = true
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
        }

        card.addView(buildHandle(palette))
        card.addView(buildHeader(deviceName, palette))
        card.addView(buildDeviceArt(palette))
        card.addView(buildBatteryGrid(scan, palette))
        card.addView(buildDoneButton(palette))

        root.addView(card)
        return root
    }

    private fun buildHandle(
        palette: OverlayPalette
    ): View {
        return View(context).apply {
            background = GradientDrawable().apply {
                setColor(palette.outline)
                cornerRadius = dp(999).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                dp(44),
                dp(5)
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(14)
            }
        }
    }

    private fun buildHeader(
        deviceName: String,
        palette: OverlayPalette
    ): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }
        }

        val icon = ImageView(context).apply {
            setImageResource(R.mipmap.ic_launcher_round)
            layoutParams = LinearLayout.LayoutParams(
                dp(46),
                dp(46)
            ).apply {
                rightMargin = dp(12)
            }
        }
        row.addView(icon)

        val titleColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        titleColumn.addView(
            TextView(context).apply {
                text = deviceName.ifBlank { "AirPods Pro" }
                setTextSize(
                    TypedValue.COMPLEX_UNIT_SP,
                    20f
                )
                setTextColor(palette.onSurface)
                typeface = Typeface.create(
                    "sans-serif",
                    Typeface.BOLD
                )
                maxLines = 1
            }
        )
        titleColumn.addView(
            TextView(context).apply {
                text = "Connected with LightPods"
                setTextSize(
                    TypedValue.COMPLEX_UNIT_SP,
                    13f
                )
                setTextColor(palette.onSurfaceVariant)
                maxLines = 1
            }
        )
        row.addView(titleColumn)

        val close = TextView(context).apply {
            text = "Done"
            setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                13f
            )
            setTextColor(palette.primary)
            typeface = Typeface.create(
                "sans-serif-medium",
                Typeface.NORMAL
            )
            gravity = Gravity.CENTER
            background = pillDrawable(
                palette.primaryContainer,
                dp(18).toFloat()
            )
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setOnClickListener { dismissPopup() }
        }
        row.addView(close)

        return row
    }

    private fun buildDeviceArt(
        palette: OverlayPalette
    ): View {
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(
                    palette.primaryContainer,
                    palette.tertiaryContainer
                )
            ).apply {
                cornerRadius = dp(28).toFloat()
            }
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(132)
            ).apply {
                bottomMargin = dp(12)
            }
        }

        panel.addView(
            deviceImage(
                res = R.drawable.pod_left,
                width = 78,
                height = 96
            )
        )
        panel.addView(
            deviceImage(
                res = R.drawable.case_closed,
                width = 116,
                height = 104
            )
        )
        panel.addView(
            deviceImage(
                res = R.drawable.pod_right,
                width = 78,
                height = 96
            )
        )
        return panel
    }

    private fun deviceImage(
        res: Int,
        width: Int,
        height: Int
    ): ImageView {
        return ImageView(context).apply {
            setImageResource(res)
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(
                dp(width),
                dp(height)
            )
        }
    }

    private fun buildBatteryGrid(
        scan: BleScanState,
        palette: OverlayPalette
    ): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }
        }

        row.addView(
            buildBatteryCard(
                label = "Left",
                percent = scan.battery.leftPercent,
                note = if (scan.isLeftMicrophone) {
                    "Mic"
                } else null,
                palette = palette
            )
        )
        row.addView(
            buildBatteryCard(
                label = "Right",
                percent = scan.battery.rightPercent,
                note = if (!scan.isLeftMicrophone) {
                    "Mic"
                } else null,
                palette = palette
            )
        )
        row.addView(
            buildBatteryCard(
                label = "Case",
                percent = scan.battery.casePercent,
                note = null,
                palette = palette
            )
        )

        return row
    }

    private fun buildBatteryCard(
        label: String,
        percent: Int,
        note: String?,
        palette: OverlayPalette
    ): View {
        val percentValid = percent in 0..100
        val color = batteryColor(percent)

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = pillDrawable(
                palette.surfaceContainer,
                dp(22).toFloat()
            )
            setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                leftMargin = dp(4)
                rightMargin = dp(4)
            }
        }

        val title = TextView(context).apply {
            text = label
            setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                12f
            )
            setTextColor(palette.onSurfaceVariant)
            maxLines = 1
        }
        card.addView(title)

        val value = TextView(context).apply {
            text = if (percentValid) "$percent%" else "NA"
            setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                18f
            )
            setTextColor(palette.onSurface)
            typeface = Typeface.create(
                "sans-serif",
                Typeface.BOLD
            )
            maxLines = 1
        }
        card.addView(value)

        if (note != null) {
            val noteView = TextView(context).apply {
                text = note
                setTextSize(
                    TypedValue.COMPLEX_UNIT_SP,
                    11f
                )
                setTextColor(palette.primary)
                typeface = Typeface.create(
                    "sans-serif-medium",
                    Typeface.NORMAL
                )
                maxLines = 1
            }
            card.addView(noteView)
        } else {
            card.addView(
                SpaceView(context, dp(14))
            )
        }

        val progress = ProgressBar(
            context,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 100
            progress = if (percentValid) percent else 0
            progressTintList = ColorStateList.valueOf(color)
            progressBackgroundTintList =
                ColorStateList.valueOf(palette.outlineVariant)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(6)
            ).apply {
                topMargin = dp(8)
            }
        }
        card.addView(progress)

        return card
    }

    private fun buildDoneButton(
        palette: OverlayPalette
    ): View {
        return TextView(context).apply {
            text = "Continue"
            setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                16f
            )
            setTextColor(palette.onPrimary)
            typeface = Typeface.create(
                "sans-serif-medium",
                Typeface.NORMAL
            )
            gravity = Gravity.CENTER
            background = pillDrawable(
                palette.primary,
                dp(18).toFloat()
            )
            setPadding(0, dp(15), 0, dp(15))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { dismissPopup() }
        }
    }

    private fun pillDrawable(
        color: Int,
        radius: Float
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
    }

    private fun overlayPalette(): OverlayPalette {
        val dark = isDarkMode()
        return if (dark) {
            OverlayPalette(
                primary = Color.parseColor("#52DBC8"),
                onPrimary = Color.parseColor("#003733"),
                primaryContainer = Color.parseColor("#005048"),
                tertiaryContainer = Color.parseColor("#2D4960"),
                surface = Color.parseColor("#101513"),
                surfaceHigh = Color.parseColor("#262B28"),
                surfaceContainer = Color.parseColor("#1C211E"),
                onSurface = Color.parseColor("#E0E4DE"),
                onSurfaceVariant = Color.parseColor("#BEC9C4"),
                outline = Color.parseColor("#5C6662"),
                outlineVariant = Color.parseColor("#3F4946")
            )
        } else {
            OverlayPalette(
                primary = Color.parseColor("#006A60"),
                onPrimary = Color.WHITE,
                primaryContainer = Color.parseColor("#C7FFF3"),
                tertiaryContainer = Color.parseColor("#D8EBFF"),
                surface = Color.parseColor("#F7FAF2"),
                surfaceHigh = Color.WHITE,
                surfaceContainer = Color.parseColor("#EEF2EA"),
                onSurface = Color.parseColor("#181D1A"),
                onSurfaceVariant = Color.parseColor("#3F4946"),
                outline = Color.parseColor("#BEC9C4"),
                outlineVariant = Color.parseColor("#D7E0DC")
            )
        }
    }

    private fun batteryColor(percent: Int): Int {
        return when {
            percent !in 0..100 ->
                Color.parseColor("#89938F")
            percent > 50 ->
                Color.parseColor("#21A65B")
            percent > 20 ->
                Color.parseColor("#B88700")
            else ->
                Color.parseColor("#BA1A1A")
        }
    }

    private fun animateOut() {
        overlayView?.animate()
            ?.translationY(dp(420).toFloat())
            ?.setDuration(300)
            ?.setInterpolator(DecelerateInterpolator())
            ?.withEndAction { removeOverlay() }
            ?.start()
            ?: removeOverlay()
    }

    private fun removeOverlay() {
        try {
            overlayView?.let {
                windowManager?.removeView(it)
            }
        } catch (_: Exception) {
        }
        overlayView = null
        isShowing = false
    }

    private data class OverlayPalette(
        val primary: Int,
        val onPrimary: Int,
        val primaryContainer: Int,
        val tertiaryContainer: Int,
        val surface: Int,
        val surfaceHigh: Int,
        val surfaceContainer: Int,
        val onSurface: Int,
        val onSurfaceVariant: Int,
        val outline: Int,
        val outlineVariant: Int
    )

    private class SpaceView(
        context: Context,
        height: Int
    ) : View(context) {
        init {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                height
            )
        }
    }

    companion object {
        private const val AUTO_DISMISS_MS = 30000L
    }
}
