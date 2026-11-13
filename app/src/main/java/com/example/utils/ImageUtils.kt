package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SignatureStroke(val points: List<Pair<Float, Float>>)

object ImageUtils {

  fun encodeUriToCompressedBase64(context: Context, uri: Uri, maxDim: Int = 640): String? {
    return try {
      val inputStream = context.contentResolver.openInputStream(uri) ?: return null
      val original = inputStream.use { BitmapFactory.decodeStream(it) } ?: return null
      val scaled = scaleBitmap(original, maxDim)
      if (scaled !== original) original.recycle()
      val baos = ByteArrayOutputStream()
      scaled.compress(Bitmap.CompressFormat.JPEG, 65, baos)
      scaled.recycle()
      Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    } catch (_: Exception) {
      null
    }
  }

  fun decodeBase64ToBitmap(base64: String): Bitmap? {
    if (base64.isBlank()) return null
    return try {
      val bytes = Base64.decode(base64, Base64.DEFAULT)
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
      null
    }
  }

  fun renderStrokesToBase64(
    strokes: List<SignatureStroke>,
    canvasWidth: Float,
    canvasHeight: Float,
  ): String {
    if (strokes.isEmpty() || canvasWidth <= 0f || canvasHeight <= 0f) return ""
    val outW = 480
    val outH = 180
    val bitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.WHITE)

    val paint =
      Paint().apply {
        color = Color.rgb(15, 41, 66)
        strokeWidth = 4.5f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
      }

    val scaleX = outW / canvasWidth
    val scaleY = outH / canvasHeight

    for (stroke in strokes) {
      if (stroke.points.isEmpty()) continue
      val path = Path()
      val first = stroke.points.first()
      path.moveTo(first.first * scaleX, first.second * scaleY)
      if (stroke.points.size == 1) {
        path.lineTo(first.first * scaleX + 1f, first.second * scaleY + 1f)
      } else {
        for (i in 1 until stroke.points.size) {
          val pt = stroke.points[i]
          path.lineTo(pt.first * scaleX, pt.second * scaleY)
        }
      }
      canvas.drawPath(path, paint)
    }

    val baos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 90, baos)
    bitmap.recycle()
    return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
  }

  fun createTempCameraUri(context: Context): Uri {
    val file = File(context.cacheDir, "cam_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
  }

  private fun scaleBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= maxDim && h <= maxDim) return bitmap
    val ratio = minOf(maxDim.toFloat() / w, maxDim.toFloat() / h)
    val newW = (w * ratio).toInt().coerceAtLeast(1)
    val newH = (h * ratio).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
  }
}

object FormatUtils {
  private val inrFormat = NumberFormat.getNumberInstance(Locale("en", "IN"))

  fun formatInr(amount: Long): String = "₹${inrFormat.format(amount)}"

  fun todayDateString(): String = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
}
