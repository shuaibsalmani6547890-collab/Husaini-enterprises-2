package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.utils.ImageUtils
import com.example.utils.SignatureStroke

@Composable
fun SignaturePreviewField(
  label: String,
  signatureBase64: String,
  onSignatureChanged: (String) -> Unit,
  modifier: Modifier = Modifier,
  testTag: String = "signature_field",
) {
  var showDialog by remember { mutableStateOf(false) }
  val bitmap: Bitmap? =
    remember(signatureBase64) { ImageUtils.decodeBase64ToBitmap(signatureBase64) }

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      if (bitmap != null) {
        TextButton(onClick = { onSignatureChanged("") }) {
          Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Clear")
        }
      }
    }
    Surface(
      shape = MaterialTheme.shapes.medium,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      color = Color.White,
      modifier =
        Modifier.fillMaxWidth()
          .height(92.dp)
          .clip(MaterialTheme.shapes.medium)
          .clickable { showDialog = true }
          .testTag(testTag),
    ) {
      if (bitmap != null) {
        Image(
          bitmap = bitmap.asImageBitmap(),
          contentDescription = label,
          contentScale = ContentScale.Fit,
          modifier = Modifier.fillMaxSize().padding(6.dp),
        )
      } else {
        Row(
          modifier = Modifier.fillMaxSize(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Default.Draw,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Spacer(Modifier.width(8.dp))
          Text(
            text = "Tap to sign ($label)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }

  if (showDialog) {
    SignaturePadDialog(
      title = label,
      onDismiss = { showDialog = false },
      onSave = { base64 ->
        onSignatureChanged(base64)
        showDialog = false
      },
    )
  }
}

@Composable
fun SignaturePadDialog(
  title: String,
  onDismiss: () -> Unit,
  onSave: (String) -> Unit,
) {
  val strokes = remember { mutableStateListOf<SignatureStroke>() }
  var currentPoints by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
  var canvasWidth by remember { mutableFloatStateOf(0f) }
  var canvasHeight by remember { mutableFloatStateOf(0f) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
          text = "Draw signature clearly inside the box below",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))

        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(210.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(Color.White)
              .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
              .onSizeChanged { size ->
                canvasWidth = size.width.toFloat()
                canvasHeight = size.height.toFloat()
              }
              .pointerInput(Unit) {
                detectDragGestures(
                  onDragStart = { offset ->
                    currentPoints = listOf(offset.x to offset.y)
                  },
                  onDrag = { change, _ ->
                    change.consume()
                    val x = change.position.x.coerceIn(0f, canvasWidth)
                    val y = change.position.y.coerceIn(0f, canvasHeight)
                    currentPoints = currentPoints + (x to y)
                  },
                  onDragEnd = {
                    if (currentPoints.isNotEmpty()) {
                      strokes.add(SignatureStroke(currentPoints))
                      currentPoints = emptyList()
                    }
                  },
                  onDragCancel = { currentPoints = emptyList() },
                )
              }
              .testTag("signature_canvas")
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            // Baseline guide
            val lineY = size.height * 0.78f
            drawLine(
              color = Color(0xFFE2E8F0),
              start = Offset(24f, lineY),
              end = Offset(size.width - 24f, lineY),
              strokeWidth = 2f,
            )

            val allStrokes =
              if (currentPoints.isNotEmpty()) strokes + SignatureStroke(currentPoints) else strokes
            for (stroke in allStrokes) {
              if (stroke.points.isEmpty()) continue
              val path = Path()
              val first = stroke.points.first()
              path.moveTo(first.first, first.second)
              if (stroke.points.size == 1) {
                path.lineTo(first.first + 1f, first.second + 1f)
              } else {
                for (i in 1 until stroke.points.size) {
                  val pt = stroke.points[i]
                  path.lineTo(pt.first, pt.second)
                }
              }
              drawPath(
                path = path,
                color = Color(0xFF0F2942),
                style =
                  Stroke(
                    width = 6f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                  ),
              )
            }
          }
        }

        Spacer(Modifier.height(16.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          OutlinedButton(
            onClick = {
              strokes.clear()
              currentPoints = emptyList()
            }
          ) {
            Text("Clear")
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Button(
              onClick = {
                val base64 = ImageUtils.renderStrokesToBase64(strokes, canvasWidth, canvasHeight)
                onSave(base64)
              },
              enabled = strokes.isNotEmpty(),
              modifier = Modifier.testTag("save_signature_button"),
            ) {
              Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("Save Sign")
            }
          }
        }
      }
    }
  }
}
