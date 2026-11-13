package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AgencyDefaults
import com.example.data.model.AgencyProfile
import com.example.data.model.CustomerRecord
import com.example.data.model.PhotoTypes
import com.example.ui.components.SignaturePreviewField
import com.example.utils.FormatUtils
import com.example.utils.ImageUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomerFormScreen(
  initialCustomer: CustomerRecord?,
  agencyProfile: AgencyProfile,
  loadedPhotos: Map<String, String>,
  isSaving: Boolean,
  onSaveCustomer: (CustomerRecord, Map<String, String>, openBillAfterSave: Boolean) -> Unit,
  onDeleteCustomer: ((String) -> Unit)?,
  onRemovePhoto: (String) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val context = LocalContext.current
  val isEditing = initialCustomer != null

  var billNumber by remember(initialCustomer) {
    mutableStateOf(
      initialCustomer?.billNumber?.ifBlank { "HE-${System.currentTimeMillis().toString().takeLast(6)}" }
        ?: "HE-${System.currentTimeMillis().toString().takeLast(6)}"
    )
  }
  var customerName by remember(initialCustomer) { mutableStateOf(initialCustomer?.customerName ?: "") }
  var keepNameBlank by remember(initialCustomer) {
    mutableStateOf(initialCustomer?.keepNameBlankOnBill ?: false)
  }
  var phoneNumber by remember(initialCustomer) { mutableStateOf(initialCustomer?.phoneNumber ?: "") }
  var aadharNumber by remember(initialCustomer) { mutableStateOf(initialCustomer?.aadharNumber ?: "") }
  var customerAddress by remember(initialCustomer) {
    mutableStateOf(initialCustomer?.customerAddress ?: "")
  }
  var vehicleNo by remember(initialCustomer) { mutableStateOf(initialCustomer?.vehicleNo ?: "") }

  val initialModel = initialCustomer?.vehicleModel ?: AgencyDefaults.E_RICKSHAW_MODELS.first()
  val isStandardModel = AgencyDefaults.E_RICKSHAW_MODELS.any { it.equals(initialModel, ignoreCase = true) }
  var selectedModelChip by remember(initialCustomer) {
    mutableStateOf(if (isStandardModel) initialModel else "Other")
  }
  var customModelText by remember(initialCustomer) {
    mutableStateOf(if (isStandardModel) "" else initialModel)
  }

  var purchaseDate by remember(initialCustomer) {
    mutableStateOf(initialCustomer?.purchaseDate ?: FormatUtils.todayDateString())
  }
  var keepDateBlank by remember(initialCustomer) {
    mutableStateOf(initialCustomer?.keepDateBlankOnBill ?: false)
  }
  var showDatePicker by remember { mutableStateOf(false) }

  var totalPriceText by remember(initialCustomer) {
    mutableStateOf(
      if ((initialCustomer?.totalPrice ?: 0L) > 0L) initialCustomer!!.totalPrice.toString() else ""
    )
  }
  var keepPriceBlank by remember(initialCustomer) {
    mutableStateOf(initialCustomer?.keepPriceBlankOnBill ?: false)
  }

  var paymentMode by remember(initialCustomer) {
    mutableStateOf(initialCustomer?.paymentMode ?: "cash")
  }
  var downPaymentText by remember(initialCustomer) {
    mutableStateOf(
      if ((initialCustomer?.downPayment ?: 0L) > 0L) initialCustomer!!.downPayment.toString() else ""
    )
  }
  var financeAmountText by remember(initialCustomer) {
    mutableStateOf(
      if ((initialCustomer?.financeAmount ?: 0L) > 0L) initialCustomer!!.financeAmount.toString() else ""
    )
  }
  var totalInstallmentsText by remember(initialCustomer) {
    mutableStateOf(
      if ((initialCustomer?.totalInstallments ?: 0L) > 0L)
        initialCustomer!!.totalInstallments.toString()
      else ""
    )
  }
  var installmentAmountText by remember(initialCustomer) {
    mutableStateOf(
      if ((initialCustomer?.installmentAmount ?: 0L) > 0L)
        initialCustomer!!.installmentAmount.toString()
      else ""
    )
  }
  var purchasedFrom by remember(initialCustomer) {
    mutableStateOf(
      initialCustomer?.purchasedFrom?.ifBlank { agencyProfile.agencyName }
        ?: agencyProfile.agencyName.ifBlank { AgencyDefaults.DEFAULT_AGENCY_NAME }
    )
  }

  var batteryGuarantee by remember(initialCustomer) {
    mutableStateOf(initialCustomer?.batteryGuarantee ?: "")
  }
  var batteryWarranty by remember(initialCustomer) {
    mutableStateOf(initialCustomer?.batteryWarranty ?: "")
  }
  var otherWarranty by remember(initialCustomer) {
    mutableStateOf(initialCustomer?.otherWarranty ?: "")
  }
  var notes by remember(initialCustomer) { mutableStateOf(initialCustomer?.notes ?: "") }

  var customerSignature by remember(initialCustomer) {
    mutableStateOf(initialCustomer?.customerSignatureBase64 ?: "")
  }
  var ownerSignature by remember(initialCustomer) {
    mutableStateOf(
      initialCustomer?.ownerSignatureBase64?.ifBlank { agencyProfile.defaultOwnerSignatureBase64 }
        ?: agencyProfile.defaultOwnerSignatureBase64
    )
  }

  // Local photo map seeded from Firestore loadedPhotos + newly picked photos
  val pendingPhotos = remember { mutableStateMapOf<String, String>() }
  val combinedPhotos = remember(loadedPhotos, pendingPhotos.toMap()) { loadedPhotos + pendingPhotos }

  var activePhotoTarget by remember { mutableStateOf<String?>(null) }
  var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
  var showDeleteConfirm by remember { mutableStateOf(false) }

  LaunchedEffect(loadedPhotos) {
    // Keep newly added local photos intact while refreshing loaded ones
  }

  val galleryLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      val target = activePhotoTarget
      if (uri != null && target != null) {
        val encoded = ImageUtils.encodeUriToCompressedBase64(context, uri)
        if (encoded != null) {
          pendingPhotos[target] = encoded
        }
      }
      activePhotoTarget = null
    }

  val cameraLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
      val target = activePhotoTarget
      val uri = pendingCameraUri
      if (success && target != null && uri != null) {
        val encoded = ImageUtils.encodeUriToCompressedBase64(context, uri)
        if (encoded != null) {
          pendingPhotos[target] = encoded
        }
      }
      activePhotoTarget = null
      pendingCameraUri = null
    }

  fun buildCustomerRecord(): CustomerRecord {
    val resolvedModel =
      if (selectedModelChip == "Other") customModelText.trim().ifBlank { "Custom E-Rickshaw" }
      else selectedModelChip
    return CustomerRecord(
      id = initialCustomer?.id ?: "",
      userId = initialCustomer?.userId ?: "",
      billNumber = billNumber.trim(),
      customerName = if (keepNameBlank) "" else customerName.trim(),
      keepNameBlankOnBill = keepNameBlank,
      phoneNumber = phoneNumber.trim(),
      aadharNumber = aadharNumber.trim(),
      customerAddress = customerAddress.trim(),
      vehicleNo = vehicleNo.trim(),
      vehicleModel = resolvedModel,
      purchaseDate = if (keepDateBlank) "" else purchaseDate.trim(),
      keepDateBlankOnBill = keepDateBlank,
      totalPrice = if (keepPriceBlank) 0L else totalPriceText.toLongOrNull() ?: 0L,
      keepPriceBlankOnBill = keepPriceBlank,
      paymentMode = paymentMode,
      downPayment = downPaymentText.toLongOrNull() ?: 0L,
      financeAmount = financeAmountText.toLongOrNull() ?: 0L,
      totalInstallments = totalInstallmentsText.toLongOrNull() ?: 0L,
      installmentAmount = installmentAmountText.toLongOrNull() ?: 0L,
      purchasedFrom = purchasedFrom.trim(),
      batteryGuarantee = batteryGuarantee.trim(),
      batteryWarranty = batteryWarranty.trim(),
      otherWarranty = otherWarranty.trim(),
      notes = notes.trim(),
      hasCustomerPhoto = combinedPhotos.containsKey(PhotoTypes.CUSTOMER),
      hasVehiclePhoto = combinedPhotos.containsKey(PhotoTypes.VEHICLE),
      hasWarrantyPhoto = combinedPhotos.containsKey(PhotoTypes.WARRANTY_CARD),
      customerSignatureBase64 = customerSignature,
      ownerSignatureBase64 = ownerSignature,
      createdAt = initialCustomer?.createdAt,
      updatedAt = initialCustomer?.updatedAt,
    )
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(if (isEditing) "Edit Customer & Bill" else "New Customer Bill") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          if (isEditing && onDeleteCustomer != null) {
            IconButton(onClick = { showDeleteConfirm = true }) {
              Icon(
                Icons.Default.Delete,
                contentDescription = "Delete customer",
                tint = MaterialTheme.colorScheme.error,
              )
            }
          }
        },
      )
    },
    bottomBar = {
      Surface(
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
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
            onClick = { onSaveCustomer(buildCustomerRecord(), pendingPhotos.toMap(), false) },
            enabled = !isSaving,
            modifier = Modifier.weight(1f).height(50.dp).testTag("save_customer_button"),
          ) {
            if (isSaving) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
              Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("Save")
            }
          }
          Button(
            onClick = { onSaveCustomer(buildCustomerRecord(), pendingPhotos.toMap(), true) },
            enabled = !isSaving,
            colors =
              ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier.weight(1.3f).height(50.dp).testTag("save_and_bill_button"),
          ) {
            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Save & 1-Click Bill")
          }
        }
      }
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .imePadding()
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Section 1: Customer Details
      FormSectionCard(title = "Customer Details") {
        OutlinedTextField(
          value = billNumber,
          onValueChange = { billNumber = it },
          label = { Text("Bill No") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
          value = customerName,
          onValueChange = { customerName = it },
          label = { Text("Customer Name") },
          enabled = !keepNameBlank,
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("customer_name_input"),
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Checkbox(
            checked = keepNameBlank,
            onCheckedChange = { keepNameBlank = it },
            modifier = Modifier.testTag("keep_name_blank_checkbox"),
          )
          Text(
            text = "Keep Customer Name column blank on bill",
            style = MaterialTheme.typography.bodyMedium,
          )
        }

        OutlinedTextField(
          value = phoneNumber,
          onValueChange = { phoneNumber = it.filter { ch -> ch.isDigit() || ch == '+' }.take(15) },
          label = { Text("Mobile Number") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("customer_phone_input"),
        )

        OutlinedTextField(
          value = aadharNumber,
          onValueChange = { aadharNumber = it.filter { ch -> ch.isDigit() }.take(12) },
          label = { Text("Aadhar Number (12 digits)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("customer_aadhar_input"),
        )

        OutlinedTextField(
          value = customerAddress,
          onValueChange = { customerAddress = it },
          label = { Text("Customer Address") },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
      }

      // Section 2: E-Rickshaw Details
      FormSectionCard(title = "E-Rickshaw Details (Konsi Gadi Hai)") {
        Text("Select E-Rickshaw Model", style = MaterialTheme.typography.labelLarge)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          AgencyDefaults.E_RICKSHAW_MODELS.forEach { model ->
            FilterChip(
              selected = selectedModelChip == model,
              onClick = { selectedModelChip = model },
              label = { Text(model) },
            )
          }
          FilterChip(
            selected = selectedModelChip == "Other",
            onClick = { selectedModelChip = "Other" },
            label = { Text("Other") },
          )
        }

        AnimatedVisibility(visible = selectedModelChip == "Other") {
          OutlinedTextField(
            value = customModelText,
            onValueChange = { customModelText = it },
            label = { Text("Enter Custom Model Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }

        OutlinedTextField(
          value = vehicleNo,
          onValueChange = { vehicleNo = it.uppercase() },
          label = { Text("Vehicle No / Chassis No") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("vehicle_no_input"),
        )

        OutlinedTextField(
          value = purchasedFrom,
          onValueChange = { purchasedFrom = it },
          label = { Text("Kaha Se Lee Hai (Showroom / Branch)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        // Purchase Date + Blank option
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = purchaseDate,
            onValueChange = { purchaseDate = it },
            label = { Text("Purchase Date (Kab Li Hai)") },
            enabled = !keepDateBlank,
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("purchase_date_input"),
          )
          IconButton(
            onClick = { showDatePicker = true },
            enabled = !keepDateBlank,
          ) {
            Icon(Icons.Default.DateRange, contentDescription = "Pick date")
          }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(
            checked = keepDateBlank,
            onCheckedChange = { keepDateBlank = it },
            modifier = Modifier.testTag("keep_date_blank_checkbox"),
          )
          Text(
            text = "Keep Date column blank on bill",
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }

      // Section 3: Price & Cash / Finance
      FormSectionCard(title = "Price & Cash / Finance Details") {
        OutlinedTextField(
          value = totalPriceText,
          onValueChange = { totalPriceText = it.filter { ch -> ch.isDigit() } },
          label = { Text("Total Price (Kitne Ki Lee Hai ₹)") },
          enabled = !keepPriceBlank,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("total_price_input"),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(
            checked = keepPriceBlank,
            onCheckedChange = { keepPriceBlank = it },
            modifier = Modifier.testTag("keep_price_blank_checkbox"),
          )
          Text(
            text = "Keep Price column blank on bill",
            style = MaterialTheme.typography.bodyMedium,
          )
        }

        Text("Payment Type", style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          SegmentedButton(
            selected = paymentMode == "cash",
            onClick = { paymentMode = "cash" },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
          ) {
            Text("Cash")
          }
          SegmentedButton(
            selected = paymentMode == "finance",
            onClick = { paymentMode = "finance" },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
          ) {
            Text("Finance / Kist")
          }
        }

        AnimatedVisibility(visible = paymentMode == "finance") {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = downPaymentText,
                onValueChange = { downPaymentText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Down Payment ₹") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
              OutlinedTextField(
                value = financeAmountText,
                onValueChange = { financeAmountText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Finance Amt ₹") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("finance_amount_input"),
              )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = totalInstallmentsText,
                onValueChange = { totalInstallmentsText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Kitni Kist (Months)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("total_kist_input"),
              )
              OutlinedTextField(
                value = installmentAmountText,
                onValueChange = { installmentAmountText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Monthly Kist ₹") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }

      // Section 4: Guarantee & Warranty
      FormSectionCard(title = "Guarantee & Warranty") {
        OutlinedTextField(
          value = batteryGuarantee,
          onValueChange = { batteryGuarantee = it },
          label = { Text("Battery Guarantee (e.g. 12 Months Replacement)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("battery_guarantee_input"),
        )
        OutlinedTextField(
          value = batteryWarranty,
          onValueChange = { batteryWarranty = it },
          label = { Text("Battery Warranty (e.g. 6 Months Service/Pro-Rata)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("battery_warranty_input"),
        )
        OutlinedTextField(
          value = otherWarranty,
          onValueChange = { otherWarranty = it },
          label = { Text("Other Parts Guarantee / Warranty (Motor, Controller, Charger)") },
          maxLines = 3,
          modifier = Modifier.fillMaxWidth().testTag("other_warranty_input"),
        )
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Additional Notes") },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
      }

      // Section 5: Photos (Customer, E-Rickshaw, Warranty Card)
      FormSectionCard(title = "Photos (Customer, E-Rickshaw & Warranty Card)") {
        PhotoPickerSlot(
          label = "Customer Photo",
          base64 = combinedPhotos[PhotoTypes.CUSTOMER],
          onPickGallery = {
            activePhotoTarget = PhotoTypes.CUSTOMER
            galleryLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          onCaptureCamera = {
            activePhotoTarget = PhotoTypes.CUSTOMER
            val uri = ImageUtils.createTempCameraUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
          },
          onRemove = {
            pendingPhotos.remove(PhotoTypes.CUSTOMER)
            if (loadedPhotos.containsKey(PhotoTypes.CUSTOMER)) {
              onRemovePhoto(PhotoTypes.CUSTOMER)
            }
          },
        )
        PhotoPickerSlot(
          label = "E-Rickshaw Photo",
          base64 = combinedPhotos[PhotoTypes.VEHICLE],
          onPickGallery = {
            activePhotoTarget = PhotoTypes.VEHICLE
            galleryLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          onCaptureCamera = {
            activePhotoTarget = PhotoTypes.VEHICLE
            val uri = ImageUtils.createTempCameraUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
          },
          onRemove = {
            pendingPhotos.remove(PhotoTypes.VEHICLE)
            if (loadedPhotos.containsKey(PhotoTypes.VEHICLE)) {
              onRemovePhoto(PhotoTypes.VEHICLE)
            }
          },
        )
        PhotoPickerSlot(
          label = "Warranty Card Photo",
          base64 = combinedPhotos[PhotoTypes.WARRANTY_CARD],
          onPickGallery = {
            activePhotoTarget = PhotoTypes.WARRANTY_CARD
            galleryLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          onCaptureCamera = {
            activePhotoTarget = PhotoTypes.WARRANTY_CARD
            val uri = ImageUtils.createTempCameraUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
          },
          onRemove = {
            pendingPhotos.remove(PhotoTypes.WARRANTY_CARD)
            if (loadedPhotos.containsKey(PhotoTypes.WARRANTY_CARD)) {
              onRemovePhoto(PhotoTypes.WARRANTY_CARD)
            }
          },
        )
      }

      // Section 6: Signatures
      FormSectionCard(title = "Signatures (Customer & Agency Owner)") {
        SignaturePreviewField(
          label = "Customer Signature",
          signatureBase64 = customerSignature,
          onSignatureChanged = { customerSignature = it },
          testTag = "customer_signature_field",
        )
        SignaturePreviewField(
          label = "Agency Owner Signature",
          signatureBase64 = ownerSignature,
          onSignatureChanged = { ownerSignature = it },
          testTag = "owner_signature_field",
        )
        if (agencyProfile.defaultOwnerSignatureBase64.isNotBlank() &&
            ownerSignature != agencyProfile.defaultOwnerSignatureBase64
        ) {
          TextButton(onClick = { ownerSignature = agencyProfile.defaultOwnerSignatureBase64 }) {
            Text("Use Saved Agency Default Signature")
          }
        }
      }

      Spacer(Modifier.height(16.dp))
    }
  }

  if (showDatePicker) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { millis ->
              purchaseDate =
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis))
            }
            showDatePicker = false
          }
        ) {
          Text("OK")
        }
      },
      dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
    ) {
      DatePicker(state = datePickerState)
    }
  }

  if (showDeleteConfirm && initialCustomer != null && onDeleteCustomer != null) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text("Delete Customer Record?") },
      text = { Text("This will remove the customer bill and photos across all synced devices.") },
      confirmButton = {
        TextButton(
          onClick = {
            showDeleteConfirm = false
            onDeleteCustomer(initialCustomer.id)
          }
        ) {
          Text("Delete", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
    )
  }
}

@Composable
private fun FormSectionCard(title: String, content: @Composable () -> Unit) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = MaterialTheme.shapes.large,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
      )
      content()
    }
  }
}

@Composable
private fun PhotoPickerSlot(
  label: String,
  base64: String?,
  onPickGallery: () -> Unit,
  onCaptureCamera: () -> Unit,
  onRemove: () -> Unit,
) {
  val bitmap: Bitmap? = remember(base64) { base64?.let { ImageUtils.decodeBase64ToBitmap(it) } }
  Surface(
    shape = MaterialTheme.shapes.medium,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        modifier =
          Modifier.size(64.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
      ) {
        if (bitmap != null) {
          Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Icon(
            Icons.Default.PhotoLibrary,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          OutlinedButton(
            onClick = onPickGallery,
            modifier = Modifier.height(36.dp),
          ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Gallery", style = MaterialTheme.typography.labelMedium)
          }
          OutlinedButton(
            onClick = onCaptureCamera,
            modifier = Modifier.height(36.dp),
          ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Camera", style = MaterialTheme.typography.labelMedium)
          }
          if (bitmap != null) {
            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
              Icon(
                Icons.Default.Delete,
                contentDescription = "Remove $label",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }
      }
    }
  }
}
