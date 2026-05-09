package uzair.lightpods.android.service

import android.annotation.SuppressLint
import android.content.Context
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

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
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
                    .FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams
                    .FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
            x = 0
            y = 0
        }

        overlayView = buildSheet(scan, deviceName)

        // ── Swipe-down to dismiss ──
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
                    if (delta > dp(80)) {
                        dismissPopup()
                    } else {
                        v.animate()
                            .translationY(0f)
                            .setDuration(200)
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

        overlayView?.translationY =
            dp(400).toFloat()
        windowManager?.addView(overlayView, params)
        isShowing = true

        overlayView?.animate()
            ?.translationY(0f)
            ?.setDuration(500)
            ?.setInterpolator(
                DecelerateInterpolator(2.5f)
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
        val dark = isDarkMode()

        val surfaceColor = if (dark)
            Color.parseColor("#1C1C1E")
        else
            Color.parseColor("#F2F2F7")

        val onSurface = if (dark)
            Color.parseColor("#FFFFFF")
        else
            Color.parseColor("#1C1C1E")

        val onSurfaceVariant = if (dark)
            Color.parseColor("#8E8E93")
        else
            Color.parseColor("#6C6C70")

        val primaryColor =
            Color.parseColor("#0A84FF")

        val dividerColor = if (dark)
            Color.parseColor("#38383A")
        else
            Color.parseColor("#D1D1D6")

        // shadow tint behind case in light mode
        val caseShadowColor = if (dark)
            Color.parseColor("#2C2C2E")
        else
            Color.parseColor("#E0E0E5")

        // ── Card: rounded top, flat bottom ──
        val radius = dp(28).toFloat()
        val cardBg = GradientDrawable().apply {
            setColor(surfaceColor)
            cornerRadii = floatArrayOf(
                radius, radius,
                radius, radius,
                0f, 0f,
                0f, 0f
            )
        }

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBg
            setPadding(
                dp(24), dp(24), dp(24), dp(36)
            )
            elevation = dp(16).toFloat()
            isClickable = true
            isFocusable = true
        }

        // ── Handle bar ──
        val handleBg = GradientDrawable().apply {
            setColor(dividerColor)
            cornerRadius = dp(3).toFloat()
        }
        val handle = View(context).apply {
            background = handleBg
            layoutParams = LinearLayout.LayoutParams(
                dp(40), dp(5)
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(20)
            }
        }
        card.addView(handle)

        // ── App name ──
        val appLabel = TextView(context).apply {
            text = "LightPods"
            setTextSize(
                TypedValue.COMPLEX_UNIT_SP, 12f
            )
            setTextColor(onSurfaceVariant)
            typeface = Typeface.create(
                "sans-serif-medium",
                Typeface.NORMAL
            )
            gravity = Gravity.CENTER
            letterSpacing = 0.08f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(4) }
        }
        card.addView(appLabel)

        // ── Device name ──
        val nameLabel = TextView(context).apply {
            text = deviceName
            setTextSize(
                TypedValue.COMPLEX_UNIT_SP, 22f
            )
            setTextColor(onSurface)
            typeface = Typeface.create(
                "sans-serif", Typeface.BOLD
            )
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(4) }
        }
        card.addView(nameLabel)

        // ── Subtitle ──
        val subtitle = TextView(context).apply {
            text = "AirPods Pro · Connected"
            setTextSize(
                TypedValue.COMPLEX_UNIT_SP, 13f
            )
            setTextColor(onSurfaceVariant)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(24) }
        }
        card.addView(subtitle)

        // ── Device row ──
        val deviceRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(20) }
        }

        deviceRow.addView(
            buildPodColumn(
                "Left",
                scan.battery.leftPercent,
                scan.isLeftMicrophone,
                R.drawable.pod_left,
                onSurface,
                onSurfaceVariant
            )
        )

        deviceRow.addView(
            buildPodColumn(
                "Right",
                scan.battery.rightPercent,
                !scan.isLeftMicrophone,
                R.drawable.pod_right,
                onSurface,
                onSurfaceVariant
            )
        )

        // Vertical separator
        val sep = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                dp(1), dp(70)
            ).apply {
                leftMargin = dp(8)
                rightMargin = dp(8)
                gravity = Gravity.CENTER_VERTICAL
            }
            setBackgroundColor(dividerColor)
        }
        deviceRow.addView(sep)

        deviceRow.addView(
            buildCaseColumn(
                scan.battery.casePercent,
                onSurface,
                onSurfaceVariant,
                caseShadowColor
            )
        )

        card.addView(deviceRow)

        // ── Done button ──
        val btnBg = GradientDrawable().apply {
            setColor(primaryColor)
            cornerRadius = dp(14).toFloat()
        }

        val doneBtn = TextView(context).apply {
            text = "Done"
            setTextSize(
                TypedValue.COMPLEX_UNIT_SP, 16f
            )
            setTextColor(Color.WHITE)
            typeface = Typeface.create(
                "sans-serif-medium",
                Typeface.NORMAL
            )
            gravity = Gravity.CENTER
            background = btnBg
            setPadding(0, dp(14), 0, dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { dismissPopup() }
        }
        card.addView(doneBtn)

        return card
    }

    private fun buildPodColumn(
        label: String,
        percent: Int,
        hasMic: Boolean,
        iconRes: Int,
        textColor: Int,
        subtextColor: Int
    ): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

            val icon = ImageView(context).apply {
                setImageResource(iconRes)
                layoutParams = LinearLayout.LayoutParams(
                    dp(72), dp(72)
                ).apply { bottomMargin = dp(6) }
                scaleType =
                    ImageView.ScaleType.FIT_CENTER
            }
            addView(icon)

            if (hasMic) {
                val mic = TextView(context).apply {
                    text = "🎤"
                    setTextSize(
                        TypedValue.COMPLEX_UNIT_SP, 10f
                    )
                    gravity = Gravity.CENTER
                }
                addView(mic)
            }

            val bText = TextView(context).apply {
                text = if (percent in 0..100) {
                    "$percent%"
                } else "—"
                setTextSize(
                    TypedValue.COMPLEX_UNIT_SP, 16f
                )
                setTextColor(batteryColor(percent))
                typeface = Typeface.create(
                    "sans-serif", Typeface.BOLD
                )
                gravity = Gravity.CENTER
            }
            addView(bText)

            val lbl = TextView(context).apply {
                text = label
                setTextSize(
                    TypedValue.COMPLEX_UNIT_SP, 12f
                )
                setTextColor(subtextColor)
                gravity = Gravity.CENTER
            }
            addView(lbl)
        }
    }

    // ╔══════════════════════════════════════════╗
    // ║  CASE SIZE: Change dp(64) below to      ║
    // ║  adjust the case image size in popup.    ║
    // ║  e.g. dp(72) for bigger, dp(48) smaller ║
    // ╚══════════════════════════════════════════╝
    private fun buildCaseColumn(
        percent: Int,
        textColor: Int,
        subtextColor: Int,
        shadowColor: Int
    ): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

            // Case with shadow circle behind it
            val caseWrapper = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    dp(80), dp(80)
                ).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    bottomMargin = dp(6)
                }
            }

            // Shadow circle behind case
            val shadowBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(shadowColor)
            }
            val shadow = View(context).apply {
                background = shadowBg
                layoutParams = FrameLayout.LayoutParams(
                    dp(68), dp(68)
                ).apply {
                    gravity = Gravity.CENTER
                }
            }
            caseWrapper.addView(shadow)

            val icon = ImageView(context).apply {
                setImageResource(R.drawable.case_closed)
                layoutParams = FrameLayout.LayoutParams(
                    dp(80), dp(80)
                ).apply {
                    gravity = Gravity.CENTER
                }
                scaleType =
                    ImageView.ScaleType.FIT_CENTER
            }
            caseWrapper.addView(icon)

            addView(caseWrapper)

            val bText = TextView(context).apply {
                text = if (percent in 0..100) {
                    "$percent%"
                } else "—"
                setTextSize(
                    TypedValue.COMPLEX_UNIT_SP, 16f
                )
                setTextColor(batteryColor(percent))
                typeface = Typeface.create(
                    "sans-serif", Typeface.BOLD
                )
                gravity = Gravity.CENTER
            }
            addView(bText)

            val lbl = TextView(context).apply {
                text = "Case"
                setTextSize(
                    TypedValue.COMPLEX_UNIT_SP, 12f
                )
                setTextColor(subtextColor)
                gravity = Gravity.CENTER
            }
            addView(lbl)
        }
    }

    private fun batteryColor(percent: Int): Int {
        return when {
            percent !in 0..100 ->
                Color.parseColor("#48484A")
            percent > 50 ->
                Color.parseColor("#34C759")
            percent > 20 ->
                Color.parseColor("#FFCC00")
            else ->
                Color.parseColor("#FF3B30")
        }
    }

    private fun animateOut() {
        overlayView?.animate()
            ?.translationY(dp(400).toFloat())
            ?.setDuration(350)
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
        } catch (_: Exception) { }
        overlayView = null
        isShowing = false
    }

    companion object {
        private const val AUTO_DISMISS_MS = 30000L
    }
}
