package ar.trenar.app.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import ar.trenar.app.R

// Marker art shared by the "Red en vivo" map and the station mini-map.

/** A marker drawable together with the vertical anchor that keeps its hotspot on the geo point. */
class MarkerArt(val drawable: Drawable, val anchorY: Float)

/** A circular marker: [fill] core + a [ringColor] ring + a white [glyphRes] glyph. */
fun buildMarker(
    ctx: Context,
    density: Float,
    fill: Int,
    sizeDp: Float,
    ringColor: Int,
    ringWidthDp: Float,
    glyphRes: Int,
): Drawable {
    val size = (sizeDp * density).toInt().coerceAtLeast(8)
    val ringW = ringWidthDp * density
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    drawCircleGlyph(canvas, ctx, size / 2f, size / 2f, size, ringW, fill, ringColor, glyphRes)
    return BitmapDrawable(ctx.resources, bmp)
}

/** Draws a filled circle + ring + centered white glyph onto [canvas] at ([cx],[cy]). */
private fun drawCircleGlyph(
    canvas: Canvas,
    ctx: Context,
    cx: Float,
    cy: Float,
    size: Int,
    ringW: Float,
    fill: Int,
    ringColor: Int,
    glyphRes: Int,
) {
    val r = size / 2f
    canvas.drawCircle(cx, cy, r - ringW, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fill })
    canvas.drawCircle(
        cx, cy, r - ringW / 2f - 0.5f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ringColor
            style = Paint.Style.STROKE
            strokeWidth = ringW
        },
    )
    ContextCompat.getDrawable(ctx, glyphRes)?.mutate()?.let { glyph ->
        glyph.setTint(AndroidColor.WHITE)
        val inset = (ringW + size * 0.2f)
        glyph.setBounds(
            (cx - r + inset).toInt(), (cy - r + inset).toInt(),
            (cx + r - inset).toInt(), (cy + r - inset).toInt(),
        )
        glyph.draw(canvas)
    }
}

/**
 * A circular station marker; when [label] is set (terminal stations), the name is drawn in bold
 * underneath the circle with a white halo so it stays readable over the map tiles.
 */
fun buildStationMarker(
    ctx: Context,
    density: Float,
    fill: Int,
    sizeDp: Float,
    ringColor: Int,
    ringWidthDp: Float,
    glyphRes: Int,
    label: String?,
): MarkerArt {
    val circle = (sizeDp * density).toInt().coerceAtLeast(8)
    val ringW = ringWidthDp * density
    if (label == null) {
        val bmp = Bitmap.createBitmap(circle, circle, Bitmap.Config.ARGB_8888)
        drawCircleGlyph(Canvas(bmp), ctx, circle / 2f, circle / 2f, circle, ringW, fill, ringColor, glyphRes)
        return MarkerArt(BitmapDrawable(ctx.resources, bmp), 0.5f)
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.BLACK
        textSize = 11f * density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    val haloPaint = Paint(textPaint).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        color = AndroidColor.WHITE
    }
    val fm = textPaint.fontMetrics
    val textH = fm.descent - fm.ascent
    val gap = 2f * density
    val textW = textPaint.measureText(label)
    val w = maxOf(circle.toFloat(), textW + 6f * density).toInt()
    val h = (circle + gap + textH).toInt() + 1
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val cx = w / 2f
    drawCircleGlyph(canvas, ctx, cx, circle / 2f, circle, ringW, fill, ringColor, glyphRes)
    val baseY = circle + gap - fm.ascent
    canvas.drawText(label, cx, baseY, haloPaint)
    canvas.drawText(label, cx, baseY, textPaint)
    return MarkerArt(BitmapDrawable(ctx.resources, bmp), (circle / 2f) / h)
}

/**
 * A side-view train sprite tinted in the line color with a white outline (no badge). The base
 * glyph faces right (eastbound); [flip] mirrors it horizontally so the nose points left for
 * westbound trains, matching the real direction of travel.
 */
fun buildTrainSprite(ctx: Context, density: Float, fillArgb: Int, sizeDp: Float, flip: Boolean): Drawable {
    val size = (sizeDp * density).toInt().coerceAtLeast(10)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val glyph = ContextCompat.getDrawable(ctx, R.drawable.ic_train_side)?.mutate()
        ?: return BitmapDrawable(ctx.resources, bmp)
    if (flip) {
        canvas.save()
        canvas.scale(-1f, 1f, size / 2f, size / 2f)
    }
    // white outline (full size) then the colored body slightly inset
    glyph.setTint(AndroidColor.WHITE)
    glyph.setBounds(0, 0, size, size)
    glyph.draw(canvas)
    val inset = (2f * density).toInt()
    glyph.setTint(fillArgb)
    glyph.setBounds(inset, inset, size - inset, size - inset)
    glyph.draw(canvas)
    if (flip) canvas.restore()
    return BitmapDrawable(ctx.resources, bmp)
}
