package com.iobits.photo_to_video_slides_maker.utils

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.TextView
import androidx.media3.ui.PlayerView

class DraggablePlayerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : PlayerView(context, attrs, defStyleAttr) {

    private var dX = 0f
    private var dY = 0f
    val overlayTextView: TextView

    init {
        // Initialize overlay TextView with dynamic properties
        overlayTextView = TextView(context).apply {
            setBackgroundColor(0x80000000.toInt()) // Semi-transparent background
            setTextColor(0xFFFFFFFF.toInt()) // White text color for contrast
            textSize = getDynamicTextSize() // Set initial text size dynamically
            text = "Sample Text" // Default text
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
        }

        // Add overlay TextView to PlayerView
        addView(overlayTextView)

        // Set touch listener on overlayTextView for dragging
        overlayTextView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    val newX = event.rawX + dX
                    val newY = event.rawY + dY

                    // Constrain X within the PlayerView bounds
                    view.x = newX.coerceIn(0f, (width - view.width).toFloat())

                    // Constrain Y within the PlayerView bounds
                    view.y = newY.coerceIn(0f, (height - view.height).toFloat())
                }
                MotionEvent.ACTION_UP -> {
                    // Final position, can retrieve coordinates here if needed
                }
            }
            true
        }
    }

    // Method to set the text content of overlay TextView
    fun setOverlayText(text: String) {
        overlayTextView.text = text
    }

    // Method to get the current position of the overlay text
    fun getOverlayTextPosition(): Pair<Float, Float> {
        return Pair(overlayTextView.x, overlayTextView.y)
    }

    // Adjust the text size based on PlayerView dimensions
    private fun getDynamicTextSize(): Float {
        val baseTextSize = 30f // Increased base text size for visibility
        val scalingFactor = (width + height) / 1500f // Adjust scaling factor for more balanced scaling
        return (baseTextSize * scalingFactor).coerceAtLeast(16f) // Ensure a minimum size
    }

    // Update text size dynamically when layout changes
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        overlayTextView.textSize = getDynamicTextSize()
    }
}
