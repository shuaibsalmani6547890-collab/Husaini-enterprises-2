package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AgencyDefaults
import com.example.data.model.AgencyProfile
import com.example.data.model.CustomerRecord
import com.example.data.model.PhotoTypes
import com.example.utils.FormatUtils
import com.example.utils.ImageUtils
import com.example.utils.PdfGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillPreviewScreen(
  agency: AgencyProfile,
  customer: CustomerRecord,
  photos: Map<String, String>,
  onEditCustomer: () -> Unit,
  onCreateKistSlip: () -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val context = LocalContext.current
  val agencyName = agency.agencyName.ifBlank { AgencyDefaults.DEFAULT_AGENCY_NAME }
  val ownerNames = agency.ownerNames.ifBlank { AgencyDefaults.DEFAULT_OWNER_NAMES }
  val contacts = agency.contactNumbers.ifBlank { AgencyDefaults.DEFAULT_CONTACT_NUMBERS }

  val nameDisplay =
    if (customer.keepNameBlankOnBill || customer.customerName.isBlank()) "________________________"
    else customer.customerName
  val dateDisplay =
    if (customer.keepDateBlankOnBill || customer.purchaseDate.isBlank()) "____ / ____ / ______"
    else customer.purchaseDate
  val priceDisplay =
    if (customer.keepPriceBlankOnBill || customer.totalPrice <= 0L) "₹ __________________"
    else FormatUtils.formatInr(customer.totalPrice)

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("1-Click Bill Preview") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = onEditCustomer) {
            Icon(Icons.Default.Edit, contentDescription = "Edit bill")
          }
          IconButton(onClick = onCreateKistSlip) {
            Icon(Icons.Default.ReceiptLong, contentDescription = "Create Kist Slip")
          }
        },
      )
    },
    bottomBar = {
      Surface(
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .navigationBarsPadding()
              .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = {
              val file = PdfGenerator.generateCustomerBillPdf(context, agency, customer, photos)
              PdfGenerator.printPdfFile(context, file, "Bill_${customer.billNumber}")
            },
            modifier = Modifier.weight(1f).height(50.dp).testTag("print_bill_button"),
          ) {
            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Print")
          }
          Button(
            onClick = {
              val file = PdfGenerator.generateCustomerBillPdf(context, agency, customer, photos)
              PdfGenerator.sharePdfFile(
                context,
                file,
                "Share Bill ${customer.billNumber} - $agencyName",
              )
            },
            colors =
              ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier.weight(1.3f).height(50.dp).testTag("share_bill_pdf_button"),
          ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Share Bill PDF")
          }
        }
      }
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Invoice Sheet Card
      Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          // Agency Banner
          Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = agencyName.uppercase(),
                  style = MaterialTheme.typography.titleLarge,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.weight(1f),
                )
                Surface(
                  color = MaterialTheme.colorScheme.secondary,
                  shape = MaterialTheme.shapes.small,
                ) {
                  Text(
                    text = "BILL",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  )
                }
              }
              Text(
                text = "Owners: $ownerNames",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
              )
              Text(
                text = "Mob: $contacts",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.9f),
              )
              if (agency.agencyAddress.isNotBlank()) {
                Text(
                  text = agency.agencyAddress,
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.White.copy(alpha = 0.8f),
                )
              }
            }
          }

          // Bill No & Date
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            BillKeyValue("Bill No", customer.billNumber)
            BillKeyValue("Date", dateDisplay)
          }

          HorizontalDivider()

          // Customer Info
          Text(
            text = "CUSTOMER DETAILS",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
          )
          BillRow("Name", nameDisplay)
          BillRow("Mobile", customer.phoneNumber.ifBlank { "____________________" })
          BillRow("Aadhar No", customer.aadharNumber.ifBlank { "____________________" })
          BillRow("Address", customer.customerAddress.ifBlank { "____________________" })
          BillRow("Taken From", customer.purchasedFrom.ifBlank { agencyName })

          HorizontalDivider()

          // Vehicle & Price
          Text(
            text = "E-RICKSHAW & PAYMENT",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
          )
          BillRow("Model", customer.vehicleModel.ifBlank { "____________________" })
          BillRow("Vehicle No", customer.vehicleNo.ifBlank { "____________________" })
          BillRow("Total Price", priceDisplay)
          BillRow("Payment Mode", if (customer.paymentMode == "finance") "FINANCE / KIST" else "CASH")

          if (customer.paymentMode == "finance") {
            BillRow(
              "Down Payment",
              if (customer.downPayment > 0) FormatUtils.formatInr(customer.downPayment) else "₹ ________",
            )
            BillRow(
              "Finance Amount",
              if (customer.financeAmount > 0) FormatUtils.formatInr(customer.financeAmount) else "₹ ________",
            )
            BillRow(
              "Total Kist",
              if (customer.totalInstallments > 0) "${customer.totalInstallments} Months" else "____ Months",
            )
            BillRow(
              "Monthly Kist",
              if (customer.installmentAmount > 0) FormatUtils.formatInr(customer.installmentAmount)
              else "₹ ________",
            )
          }

          HorizontalDivider()

          // Warranty
          Text(
            text = "GUARANTEE & WARRANTY",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
          )
          BillRow("Battery Guarantee", customer.batteryGuarantee.ifBlank { "As per company terms" })
          BillRow("Battery Warranty", customer.batteryWarranty.ifBlank { "As per warranty card" })
          BillRow("Other Parts", customer.otherWarranty.ifBlank { "As applicable" })

          // Photos
          val custBmp = photos[PhotoTypes.CUSTOMER]?.let { ImageUtils.decodeBase64ToBitmap(it) }
          val vehBmp = photos[PhotoTypes.VEHICLE]?.let { ImageUtils.decodeBase64ToBitmap(it) }
          val warBmp = photos[PhotoTypes.WARRANTY_CARD]?.let { ImageUtils.decodeBase64ToBitmap(it) }
          if (custBmp != null || vehBmp != null || warBmp != null) {
            HorizontalDivider()
            Text(
              text = "ATTACHED PHOTOS",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              PhotoPreviewTile("Customer", custBmp, Modifier.weight(1f))
              PhotoPreviewTile("E-Rickshaw", vehBmp, Modifier.weight(1f))
              PhotoPreviewTile("Warranty", warBmp, Modifier.weight(1f))
            }
          }

          HorizontalDivider()

          // Signatures
          val custSigBmp = ImageUtils.decodeBase64ToBitmap(customer.customerSignatureBase64)
          val ownerSigBmp =
            ImageUtils.decodeBase64ToBitmap(
              customer.ownerSignatureBase64.ifBlank { agency.defaultOwnerSignatureBase64 }
            )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            SignaturePreviewBox("Customer Signature", custSigBmp, Modifier.weight(1f))
            SignaturePreviewBox("Owner Signature", ownerSigBmp, Modifier.weight(1f))
          }
        }
      }

      Spacer(Modifier.height(12.dp))
    }
  }
}

@Composable
private fun BillKeyValue(label: String, value: String) {
  Column {
    Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569))
    Text(
      text = value,
      style = MaterialTheme.typography.labelLarge,
      color = Color(0xFF0F172A),
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun BillRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = "$label:",
      style = MaterialTheme.typography.bodyMedium,
      color = Color(0xFF475569),
      modifier = Modifier.weight(0.42f),
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      color = Color(0xFF0F172A),
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.weight(0.58f),
    )
  }
}

@Composable
private fun PhotoPreviewTile(title: String, bitmap: Bitmap?, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(78.dp)
          .clip(MaterialTheme.shapes.small)
          .background(Color(0xFFF1F5F9)),
      contentAlignment = Alignment.Center,
    ) {
      if (bitmap != null) {
        Image(
          bitmap = bitmap.asImageBitmap(),
          contentDescription = title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      } else {
        Text("—", color = Color(0xFF94A3B8))
      }
    }
    Text(text = title, style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569))
  }
}

@Composable
private fun SignaturePreviewBox(title: String, bitmap: Bitmap?, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Surface(
      shape = MaterialTheme.shapes.small,
      border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
      color = Color.White,
      modifier = Modifier.fillMaxWidth().height(64.dp),
    ) {
      if (bitmap != null) {
        Image(
          bitmap = bitmap.asImageBitmap(),
          contentDescription = title,
          contentScale = ContentScale.Fit,
          modifier = Modifier.fillMaxSize().padding(4.dp),
        )
      } else {
        Box(contentAlignment = Alignment.Center) {
          Text("Blank", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
        }
      }
    }
    Text(text = title, style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569))
  }
}
