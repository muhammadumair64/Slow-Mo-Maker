package com.iobits.photo_to_video_slides_maker.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaMetadataRetriever
import android.util.AttributeSet
import android.view.View


class VideoFrameView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private val retriever = MediaMetadataRetriever()
    private var frames = mutableListOf<Bitmap>()

    fun setVideoPath(path: String) {
        retriever.setDataSource(path)

        // extract video frames and add them to the frames list
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
        var time = 0L
        while (time < duration) {
            val frame = retriever.getFrameAtTime(time, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            if (frame != null) {
                frames.add(frame)
            }
            time += 1000000 // 1 second
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        frames.forEachIndexed { index, frame ->
            // draw the frame at the appropriate position
            val x = (index * width) / frames.size
            canvas.drawBitmap(frame, x.toFloat(), 0f, null)
        }
    }
    fun displayFrames(frames: HashSet<Bitmap>) {
        // clear the existing frames
        this.frames.clear()

        // add the new frames
        this.frames.addAll(frames)

        // redraw the view
        invalidate()
    }
}
