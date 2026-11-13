package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.data.model.AgencyDefaults
import com.example.data.model.AgencyProfile
import com.example.data.model.CustomerRecord
import com.example.data.model.KistSlip
import com.example.data.model.PhotoTypes
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object PdfGenerator {

  private const val PAGE_WIDTH = 595
  private const val PAGE_HEIGHT = 842
  private const val MARGIN = 32f

  fun generateCustomerBillPdf(
    context: Context,
    agency: AgencyProfile,
    customer: CustomerRecord,
    photos: Map<String, String>,
  ): File {
    val pdf = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
    val page = pdf.startPage(pageInfo)
    val c = page.canvas

    val navyPaint = Paint().apply { color = Color.rgb(15, 41, 66); isAntiAlias = true }
    val emeraldPaint = Paint().apply { color = Color.rgb(13, 148, 104); isAntiAlias = true }
    val borderPaint =
      Paint().apply {
        color = Color.rgb(203, 213, 225)
        style = Paint.Style.STROKE
        strokeWidth = 1f
        isAntiAlias = true
      }
    val boxFillPaint = Paint().apply { color = Color.rgb(246, 248, 251); isAntiAlias = true }

    val titlePaint =
      Paint().apply {
        color = Color.WHITE
        textSize = 20f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
      }
    val subtitleWhite =
      Paint().apply {
        color = Color.rgb(220, 235, 248)
        textSize = 10.5f
        isAntiAlias = true
      }
    val sectionTitlePaint =
      Paint().apply {
        color = Color.rgb(15, 41, 66)
        textSize = 11.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
      }
    val labelPaint =
      Paint().apply {
        color = Color.rgb(71, 85, 105)
        textSize = 9.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
      }
    val valuePaint =
      Paint().apply {
        color = Color.rgb(15, 23, 42)
        textSize = 10.5f
        isAntiAlias = true
      }
    val valueBoldPaint =
      Paint().apply {
        color = Color.rgb(15, 23, 42)
        textSize = 11f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
      }

    // Header banner
    c.drawRoundRect(RectF(MARGIN, MARGIN, PAGE_WIDTH - MARGIN, 118f), 10f, 10f, navyPaint)
    val agencyName = agency.agencyName.ifBlank { AgencyDefaults.DEFAULT_AGENCY_NAME }
    c.drawText(agencyName.uppercase(), MARGIN + 16f, MARGIN + 30f, titlePaint)
    c.drawText("Owners: ${agency.ownerNames.ifBlank { AgencyDefaults.DEFAULT_OWNER_NAMES }}", MARGIN + 16f, MARGIN + 50f, subtitleWhite)
    c.drawText("Mob: ${agency.contactNumbers.ifBlank { AgencyDefaults.DEFAULT_CONTACT_NUMBERS }}", MARGIN + 16f, MARGIN + 65f, subtitleWhite)
    if (agency.agencyAddress.isNotBlank()) {
      c.drawText(agency.agencyAddress, MARGIN + 16f, MARGIN + 80f, subtitleWhite)
    }

    // Badge on right of header
    val badgeRect = RectF(PAGE_WIDTH - MARGIN - 150f, MARGIN + 16f, PAGE_WIDTH - MARGIN - 14f, MARGIN + 44f)
    c.drawRoundRect(badgeRect, 6f, 6f, emeraldPaint)
    val badgeTextPaint =
      Paint().apply {
        color = Color.WHITE
        textSize = 11f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
      }
    c.drawText("E-RICKSHAW BILL", badgeRect.centerX(), badgeRect.centerY() + 4f, badgeTextPaint)

    var y = 136f

    // Bill No & Date row
    val dateDisplay =
      if (customer.keepDateBlankOnBill || customer.purchaseDate.isBlank()) "____ / ____ / ______"
      else customer.purchaseDate
    c.drawText("BILL NO:", MARGIN, y, labelPaint)
    c.drawText(customer.billNumber.ifBlank { "____________" }, MARGIN + 56f, y, valueBoldPaint)
    c.drawText("DATE:", 385f, y, labelPaint)
    c.drawText(dateDisplay, 425f, y, valueBoldPaint)
    y += 16f

    // Customer Details Section
    c.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 114f), 8f, 8f, boxFillPaint)
    c.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 114f), 8f, 8f, borderPaint)
    c.drawText("CUSTOMER DETAILS", MARGIN + 12f, y + 18f, sectionTitlePaint)

    val nameDisplay =
      if (customer.keepNameBlankOnBill || customer.customerName.isBlank()) "________________________________"
      else customer.customerName
    c.drawText("Name:", MARGIN + 12f, y + 38f, labelPaint)
    c.drawText(nameDisplay, MARGIN + 85f, y + 38f, valueBoldPaint)

    c.drawText("Mobile:", MARGIN + 12f, y + 58f, labelPaint)
    c.drawText(customer.phoneNumber.ifBlank { "____________________" }, MARGIN + 85f, y + 58f, valuePaint)

    c.drawText("Aadhar No:", 310f, y + 58f, labelPaint)
    c.drawText(customer.aadharNumber.ifBlank { "____________________" }, 375f, y + 58f, valuePaint)

    c.drawText("Address:", MARGIN + 12f, y + 78f, labelPaint)
    c.drawText(
      customer.customerAddress.ifBlank { "____________________________________________" }.take(68),
      MARGIN + 85f,
      y + 78f,
      valuePaint,
    )

    c.drawText("Taken From:", MARGIN + 12f, y + 98f, labelPaint)
    c.drawText(customer.purchasedFrom.ifBlank { agencyName }.take(64), MARGIN + 85f, y + 98f, valuePaint)
    y += 126f

    // Vehicle & Pricing Section
    val isFinance = customer.paymentMode == "finance"
    val boxH = if (isFinance) 142f else 98f
    c.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + boxH), 8f, 8f, boxFillPaint)
    c.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + boxH), 8f, 8f, borderPaint)
    c.drawText("E-RICKSHAW & PAYMENT DETAILS", MARGIN + 12f, y + 18f, sectionTitlePaint)

    c.drawText("Model:", MARGIN + 12f, y + 38f, labelPaint)
    c.drawText(customer.vehicleModel.ifBlank { "____________________" }, MARGIN + 85f, y + 38f, valueBoldPaint)

    c.drawText("Vehicle No:", 310f, y + 38f, labelPaint)
    c.drawText(customer.vehicleNo.ifBlank { "____________________" }, 375f, y + 38f, valueBoldPaint)

    val priceDisplay =
      if (customer.keepPriceBlankOnBill || customer.totalPrice <= 0L) "₹ __________________"
      else FormatUtils.formatInr(customer.totalPrice)
    c.drawText("Total Price:", MARGIN + 12f, y + 60f, labelPaint)
    c.drawText(priceDisplay, MARGIN + 85f, y + 60f, valueBoldPaint)

    c.drawText("Payment:", 310f, y + 60f, labelPaint)
    c.drawText(if (isFinance) "FINANCE / EMI" else "CASH", 375f, y + 60f, valueBoldPaint)

    if (isFinance) {
      c.drawText("Down Payment:", MARGIN + 12f, y + 84f, labelPaint)
      c.drawText(
        if (customer.downPayment > 0) FormatUtils.formatInr(customer.downPayment) else "₹ ________",
        MARGIN + 100f,
        y + 84f,
        valuePaint,
      )
      c.drawText("Finance Amt:", 310f, y + 84f, labelPaint)
      c.drawText(
        if (customer.financeAmount > 0) FormatUtils.formatInr(customer.financeAmount) else "₹ ________",
        385f,
        y + 84f,
        valueBoldPaint,
      )

      c.drawText("Total Kist:", MARGIN + 12f, y + 106f, labelPaint)
      c.drawText(
        if (customer.totalInstallments > 0) "${customer.totalInstallments} Months" else "____ Months",
        MARGIN + 100f,
        y + 106f,
        valuePaint,
      )
      c.drawText("Monthly Kist:", 310f, y + 106f, labelPaint)
      c.drawText(
        if (customer.installmentAmount > 0) FormatUtils.formatInr(customer.installmentAmount) else "₹ ________",
        385f,
        y + 106f,
        valueBoldPaint,
      )
    } else {
      c.drawText("Status:", MARGIN + 12f, y + 82f, labelPaint)
      c.drawText("Full Cash Payment", MARGIN + 85f, y + 82f, valuePaint)
    }
    y += boxH + 12f

    // Guarantee & Warranty Section
    c.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 96f), 8f, 8f, boxFillPaint)
    c.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 96f), 8f, 8f, borderPaint)
    c.drawText("GUARANTEE & WARRANTY TERMS", MARGIN + 12f, y + 18f, sectionTitlePaint)

    c.drawText("Battery Guarantee:", MARGIN + 12f, y + 38f, labelPaint)
    c.drawText(customer.batteryGuarantee.ifBlank { "As per company terms" }.take(52), MARGIN + 130f, y + 38f, valuePaint)

    c.drawText("Battery Warranty:", MARGIN + 12f, y + 58f, labelPaint)
    c.drawText(customer.batteryWarranty.ifBlank { "As per warranty card" }.take(52), MARGIN + 130f, y + 58f, valuePaint)

    c.drawText("Other Parts:", MARGIN + 12f, y + 78f, labelPaint)
    c.drawText(customer.otherWarranty.ifBlank { "Motor / Controller / Charger as applicable" }.take(56), MARGIN + 130f, y + 78f, valuePaint)
    y += 108f

    // Photos strip if any photo exists
    val customerBmp = photos[PhotoTypes.CUSTOMER]?.let { ImageUtils.decodeBase64ToBitmap(it) }
    val vehicleBmp = photos[PhotoTypes.VEHICLE]?.let { ImageUtils.decodeBase64ToBitmap(it) }
    val warrantyBmp = photos[PhotoTypes.WARRANTY_CARD]?.let { ImageUtils.decodeBase64ToBitmap(it) }
    if (customerBmp != null || vehicleBmp != null || warrantyBmp != null) {
      c.drawText("ATTACHED PHOTOS", MARGIN, y + 10f, sectionTitlePaint)
      y += 18f
      val boxW = 165f
      val boxImgH = 95f
      drawPhotoBox(c, MARGIN, y, boxW, boxImgH, "Customer Photo", customerBmp, borderPaint, labelPaint)
      drawPhotoBox(c, MARGIN + boxW + 18f, y, boxW, boxImgH, "E-Rickshaw Photo", vehicleBmp, borderPaint, labelPaint)
      drawPhotoBox(c, MARGIN + (boxW + 18f) * 2f, y, boxW, boxImgH, "Warranty Card", warrantyBmp, borderPaint, labelPaint)
      y += boxImgH + 22f
    } else if (customer.notes.isNotBlank()) {
      c.drawText("Notes: ${customer.notes.take(80)}", MARGIN, y + 14f, valuePaint)
      y += 28f
    }

    // Signatures Footer (pinned near bottom)
    val sigTop = maxOf(y + 8f, PAGE_HEIGHT - 132f)
    val custSigBmp = ImageUtils.decodeBase64ToBitmap(customer.customerSignatureBase64)
    val ownerSigBase64 =
      customer.ownerSignatureBase64.ifBlank { agency.defaultOwnerSignatureBase64 }
    val ownerSigBmp = ImageUtils.decodeBase64ToBitmap(ownerSigBase64)

    drawSignatureBox(c, MARGIN, sigTop, 220f, 68f, "Customer Signature", custSigBmp, borderPaint, labelPaint)
    drawSignatureBox(
      c,
      PAGE_WIDTH - MARGIN - 220f,
      sigTop,
      220f,
      68f,
      "Agency Owner Signature (${agencyName.take(22)})",
      ownerSigBmp,
      borderPaint,
      labelPaint,
    )

    // Footer text
    val footerPaint =
      Paint().apply {
        color = Color.rgb(100, 116, 139)
        textSize = 8.5f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
      }
    c.drawText(
      "Thank you for choosing $agencyName • Subject to local jurisdiction",
      PAGE_WIDTH / 2f,
      PAGE_HEIGHT - 20f,
      footerPaint,
    )

    pdf.finishPage(page)
    val safeBill = customer.billNumber.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "bill" }
    val file = File(context.cacheDir, "Bill_$safeBill.pdf")
    FileOutputStream(file).use { pdf.writeTo(it) }
    pdf.close()
    return file
  }

  fun generateKistSlipPdf(
    context: Context,
    agency: AgencyProfile,
    slip: KistSlip,
  ): File {
    val pdf = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, 520, 1).create()
    val page = pdf.startPage(pageInfo)
    val c = page.canvas

    val navyPaint = Paint().apply { color = Color.rgb(15, 41, 66); isAntiAlias = true }
    val amberPaint = Paint().apply { color = Color.rgb(217, 119, 6); isAntiAlias = true }
    val borderPaint =
      Paint().apply {
        color = Color.rgb(203, 213, 225)
        style = Paint.Style.STROKE
        strokeWidth = 1f
        isAntiAlias = true
      }
    val boxFillPaint = Paint().apply { color = Color.rgb(246, 248, 251); isAntiAlias = true }

    val titlePaint =
      Paint().apply {
        color = Color.WHITE
        textSize = 19f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
      }
    val subtitleWhite = Paint().apply { color = Color.rgb(220, 235, 248); textSize = 10f; isAntiAlias = true }
    val labelPaint =
      Paint().apply {
        color = Color.rgb(71, 85, 105)
        textSize = 10f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
      }
    val valueBoldPaint =
      Paint().apply {
        color = Color.rgb(15, 23, 42)
        textSize = 11.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
      }
    val valuePaint = Paint().apply { color = Color.rgb(15, 23, 42); textSize = 10.5f; isAntiAlias = true }

    val agencyName = agency.agencyName.ifBlank { AgencyDefaults.DEFAULT_AGENCY_NAME }
    c.drawRoundRect(RectF(MARGIN, MARGIN, PAGE_WIDTH - MARGIN, 114f), 10f, 10f, navyPaint)
    c.drawText(agencyName.uppercase(), MARGIN + 16f, MARGIN + 28f, titlePaint)
    c.drawText("Owners: ${agency.ownerNames.ifBlank { AgencyDefaults.DEFAULT_OWNER_NAMES }}", MARGIN + 16f, MARGIN + 47f, subtitleWhite)
    c.drawText("Mob: ${agency.contactNumbers.ifBlank { AgencyDefaults.DEFAULT_CONTACT_NUMBERS }}", MARGIN + 16f, MARGIN + 62f, subtitleWhite)

    val badgeRect = RectF(PAGE_WIDTH - MARGIN - 178f, MARGIN + 16f, PAGE_WIDTH - MARGIN - 14f, MARGIN + 44f)
    c.drawRoundRect(badgeRect, 6f, 6f, amberPaint)
    val badgeText =
      Paint().apply {
        color = Color.WHITE
        textSize = 10.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
      }
    c.drawText("TEMPORARY KIST SLIP", badgeRect.centerX(), badgeRect.centerY() + 4f, badgeText)

    var y = 134f
    val dateStr =
      if (slip.keepDateBlank || slip.paymentDate.isBlank()) "____ / ____ / ______"
      else slip.paymentDate
    c.drawText("SLIP NO:", MARGIN, y, labelPaint)
    c.drawText(slip.slipNumber.ifBlank { "____________" }, MARGIN + 58f, y, valueBoldPaint)
    c.drawText("DATE:", 385f, y, labelPaint)
    c.drawText(dateStr, 425f, y, valueBoldPaint)
    y += 16f

    c.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 188f), 8f, 8f, boxFillPaint)
    c.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 188f), 8f, 8f, borderPaint)

    val nameStr =
      if (slip.keepNameBlank || slip.customerName.isBlank()) "________________________________"
      else slip.customerName
    c.drawText("Customer Name:", MARGIN + 14f, y + 26f, labelPaint)
    c.drawText(nameStr, MARGIN + 120f, y + 26f, valueBoldPaint)

    c.drawText("Mobile:", MARGIN + 14f, y + 52f, labelPaint)
    c.drawText(slip.phoneNumber.ifBlank { "____________________" }, MARGIN + 120f, y + 52f, valuePaint)

    c.drawText("Vehicle No:", 320f, y + 52f, labelPaint)
    c.drawText(slip.vehicleNo.ifBlank { "________________" }, 390f, y + 52f, valueBoldPaint)

    c.drawText("E-Rickshaw Model:", MARGIN + 14f, y + 78f, labelPaint)
    c.drawText(slip.vehicleModel.ifBlank { "____________________" }, MARGIN + 120f, y + 78f, valuePaint)

    c.drawText("Kist / EMI No:", 320f, y + 78f, labelPaint)
    c.drawText(slip.kistNumber.ifBlank { "____ / ____" }, 390f, y + 78f, valueBoldPaint)

    val amountStr =
      if (slip.keepAmountBlank || slip.amountPaid <= 0L) "₹ __________________"
      else FormatUtils.formatInr(slip.amountPaid)
    c.drawText("Amount Received:", MARGIN + 14f, y + 108f, labelPaint)
    c.drawText(amountStr, MARGIN + 120f, y + 108f, valueBoldPaint)

    c.drawText("Payment Mode:", 320f, y + 108f, labelPaint)
    c.drawText(slip.paymentMethod.ifBlank { "Cash" }, 405f, y + 108f, valuePaint)

    c.drawText("Next Due Date:", MARGIN + 14f, y + 136f, labelPaint)
    c.drawText(slip.nextDueDate.ifBlank { "____ / ____ / ______" }, MARGIN + 120f, y + 136f, valuePaint)

    c.drawText("Remarks:", MARGIN + 14f, y + 162f, labelPaint)
    c.drawText(slip.remarks.ifBlank { "Temporary receipt valid until final ledger entry." }.take(65), MARGIN + 120f, y + 162f, valuePaint)

    val sigTop = 368f
    val custSig = ImageUtils.decodeBase64ToBitmap(slip.customerSignatureBase64)
    val ownerSig =
      ImageUtils.decodeBase64ToBitmap(
        slip.ownerSignatureBase64.ifBlank { agency.defaultOwnerSignatureBase64 }
      )
    drawSignatureBox(c, MARGIN, sigTop, 220f, 68f, "Customer Signature", custSig, borderPaint, labelPaint)
    drawSignatureBox(
      c,
      PAGE_WIDTH - MARGIN - 220f,
      sigTop,
      220f,
      68f,
      "Agency Owner Signature",
      ownerSig,
      borderPaint,
      labelPaint,
    )

    pdf.finishPage(page)
    val safeSlip = slip.slipNumber.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "kist_slip" }
    val file = File(context.cacheDir, "KistSlip_$safeSlip.pdf")
    FileOutputStream(file).use { pdf.writeTo(it) }
    pdf.close()
    return file
  }

  private fun drawPhotoBox(
    c: Canvas,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    caption: String,
    bitmap: Bitmap?,
    borderPaint: Paint,
    labelPaint: Paint,
  ) {
    val rect = RectF(x, y, x + w, y + h)
    c.drawRoundRect(rect, 6f, 6f, borderPaint)
    if (bitmap != null) {
      val dst = RectF(x + 4f, y + 4f, x + w - 4f, y + h - 18f)
      c.drawBitmap(bitmap, Rect(0, 0, bitmap.width, bitmap.height), dst, null)
    } else {
      c.drawText("No Photo", x + 52f, y + h / 2f, labelPaint)
    }
    c.drawText(caption, x + 8f, y + h - 5f, labelPaint)
  }

  private fun drawSignatureBox(
    c: Canvas,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    caption: String,
    bitmap: Bitmap?,
    borderPaint: Paint,
    labelPaint: Paint,
  ) {
    val rect = RectF(x, y, x + w, y + h)
    c.drawRoundRect(rect, 6f, 6f, borderPaint)
    if (bitmap != null) {
      val dst = RectF(x + 8f, y + 4f, x + w - 8f, y + h - 6f)
      c.drawBitmap(bitmap, Rect(0, 0, bitmap.width, bitmap.height), dst, null)
    }
    c.drawText(caption, x + 6f, y + h + 14f, labelPaint)
  }

  fun sharePdfFile(context: Context, pdfFile: File, title: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
    val intent =
      Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    context.startActivity(Intent.createChooser(intent, title))
  }

  fun printPdfFile(context: Context, pdfFile: File, jobName: String) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
    val adapter =
      object : PrintDocumentAdapter() {
        override fun onLayout(
          oldAttributes: PrintAttributes?,
          newAttributes: PrintAttributes?,
          cancellationSignal: CancellationSignal?,
          callback: LayoutResultCallback?,
          extras: Bundle?,
        ) {
          if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
          }
          val info =
            PrintDocumentInfo.Builder(pdfFile.name)
              .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
              .build()
          callback?.onLayoutFinished(info, true)
        }

        override fun onWrite(
          pages: Array<out PageRange>?,
          destination: ParcelFileDescriptor?,
          cancellationSignal: CancellationSignal?,
          callback: WriteResultCallback?,
        ) {
          try {
            FileInputStream(pdfFile).use { input ->
              FileOutputStream(destination?.fileDescriptor).use { output -> input.copyTo(output) }
            }
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
          } catch (e: Exception) {
            callback?.onWriteFailed(e.message)
          }
        }
      }
    printManager.print(jobName, adapter, PrintAttributes.Builder().build())
  }
}
