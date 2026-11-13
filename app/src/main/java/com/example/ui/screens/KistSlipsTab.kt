package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AgencyDefaults
import com.example.data.model.AgencyProfile
import com.example.data.model.CustomerRecord
import com.example.data.model.KistSlip
import com.example.ui.UiState
import com.example.ui.components.SignaturePreviewField
import com.example.utils.FormatUtils
import com.example.utils.PdfGenerator

@Composable
fun KistSlipsTab(
  agency: AgencyProfile,
  slipsState: UiState<List<KistSlip>>,
  customers: List<CustomerRecord>,
  prefillCustomer: CustomerRecord?,
  onClearPrefill: () -> Unit,
  onSaveSlip: (KistSlip, onSaved: (KistSlip) -> Unit) -> Unit,
  onDeleteSlip: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var showCreateDialog by remember(prefillCustomer) { mutableStateOf(prefillCustomer != null) }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Temporary Kist Slips",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
              text = "Issue instant installment receipts with signatures & shareable PDF.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
            )
          }
          Button(
            onClick = { showCreateDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            modifier = Modifier.testTag("new_kist_slip_button"),
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("New Slip")
          }
        }
      }
    }

    when (slipsState) {
      is UiState.Loading -> {
        item {
          Box(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator()
          }
        }
      }
      is UiState.Error -> {
        item {
          Text(
            text = slipsState.message,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp),
          )
        }
      }
      is UiState.Success -> {
        if (slipsState.data.isEmpty()) {
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              shape = MaterialTheme.shapes.large,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  Icons.Default.ReceiptLong,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.tertiary,
                  modifier = Modifier.size(42.dp),
                )
                Text("No Temporary Kist Slips Yet", style = MaterialTheme.typography.titleMedium)
                Text(
                  text = "Create a temporary kist slip for any cash or EMI installment payment.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        } else {
          items(slipsState.data, key = { it.id }) { slip ->
            KistSlipCard(
              slip = slip,
              onPrint = {
                val file = PdfGenerator.generateKistSlipPdf(context, agency, slip)
                PdfGenerator.printPdfFile(context, file, "KistSlip_${slip.slipNumber}")
              },
              onShare = {
                val file = PdfGenerator.generateKistSlipPdf(context, agency, slip)
                PdfGenerator.sharePdfFile(context, file, "Temporary Kist Slip ${slip.slipNumber}")
              },
              onDelete = { onDeleteSlip(slip.id) },
            )
          }
        }
      }
    }

    item { Spacer(Modifier.height(76.dp)) }
  }

  if (showCreateDialog) {
    CreateKistSlipDialog(
      agency = agency,
      customers = customers,
      prefillCustomer = prefillCustomer,
      onDismiss = {
        showCreateDialog = false
        onClearPrefill()
      },
      onSaveAndShare = { slip, shareAfter ->
        onSaveSlip(slip) { saved ->
          showCreateDialog = false
          onClearPrefill()
          if (shareAfter) {
            val file = PdfGenerator.generateKistSlipPdf(context, agency, saved)
            PdfGenerator.sharePdfFile(context, file, "Temporary Kist Slip ${saved.slipNumber}")
          }
        }
      },
    )
  }
}

@Composable
private fun KistSlipCard(
  slip: KistSlip,
  onPrint: () -> Unit,
  onShare: () -> Unit,
  onDelete: () -> Unit,
) {
  val nameDisplay =
    if (slip.keepNameBlank || slip.customerName.isBlank()) "Blank Name (On Slip)"
    else slip.customerName
  val amountDisplay =
    if (slip.keepAmountBlank || slip.amountPaid <= 0L) "Blank Amount"
    else FormatUtils.formatInr(slip.amountPaid)
  val dateDisplay =
    if (slip.keepDateBlank || slip.paymentDate.isBlank()) "Blank Date"
    else slip.paymentDate

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = MaterialTheme.shapes.large,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "${slip.slipNumber} • $dateDisplay",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.tertiary,
          )
          Text(
            text = nameDisplay,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
        Text(
          text = amountDisplay,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
        )
      }

      Text(
        text =
          listOfNotNull(
              slip.vehicleModel.takeIf { it.isNotBlank() },
              slip.vehicleNo.takeIf { it.isNotBlank() }?.let { "No: $it" },
              slip.kistNumber.takeIf { it.isNotBlank() }?.let { "Kist: $it" },
              slip.paymentMethod.takeIf { it.isNotBlank() },
            )
            .joinToString(" • "),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedButton(onClick = onPrint, modifier = Modifier.weight(1f)) {
          Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Print")
        }
        Button(
          onClick = onShare,
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
          modifier = Modifier.weight(1f),
        ) {
          Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Share PDF")
        }
        IconButton(onClick = onDelete) {
          Icon(
            Icons.Default.Delete,
            contentDescription = "Delete slip",
            tint = MaterialTheme.colorScheme.error,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateKistSlipDialog(
  agency: AgencyProfile,
  customers: List<CustomerRecord>,
  prefillCustomer: CustomerRecord?,
  onDismiss: () -> Unit,
  onSaveAndShare: (KistSlip, shareAfter: Boolean) -> Unit,
) {
  var selectedCustomer by remember { mutableStateOf(prefillCustomer) }
  var dropdownExpanded by remember { mutableStateOf(false) }

  var slipNumber by remember {
    mutableStateOf("KIST-${System.currentTimeMillis().toString().takeLast(6)}")
  }
  var customerName by remember { mutableStateOf(prefillCustomer?.customerName ?: "") }
  var keepNameBlank by remember { mutableStateOf(prefillCustomer?.keepNameBlankOnBill ?: false) }
  var phoneNumber by remember { mutableStateOf(prefillCustomer?.phoneNumber ?: "") }
  var vehicleNo by remember { mutableStateOf(prefillCustomer?.vehicleNo ?: "") }
  var vehicleModel by remember {
    mutableStateOf(prefillCustomer?.vehicleModel ?: AgencyDefaults.E_RICKSHAW_MODELS.first())
  }
  var kistNumber by remember {
    mutableStateOf(
      if ((prefillCustomer?.totalInstallments ?: 0L) > 0L)
        "1 / ${prefillCustomer!!.totalInstallments}"
      else ""
    )
  }
  var amountText by remember {
    mutableStateOf(
      if ((prefillCustomer?.installmentAmount ?: 0L) > 0L)
        prefillCustomer!!.installmentAmount.toString()
      else ""
    )
  }
  var keepAmountBlank by remember { mutableStateOf(false) }
  var paymentDate by remember { mutableStateOf(FormatUtils.todayDateString()) }
  var keepDateBlank by remember { mutableStateOf(false) }
  var paymentMethod by remember { mutableStateOf("Cash") }
  var nextDueDate by remember { mutableStateOf("") }
  var remarks by remember { mutableStateOf("") }
  var customerSig by remember { mutableStateOf(prefillCustomer?.customerSignatureBase64 ?: "") }
  var ownerSig by remember {
    mutableStateOf(
      prefillCustomer?.ownerSignatureBase64?.ifBlank { agency.defaultOwnerSignatureBase64 }
        ?: agency.defaultOwnerSignatureBase64
    )
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier.fillMaxWidth().padding(14.dp),
    ) {
      Column(
        modifier = Modifier.padding(18.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text("Temporary Kist Slip", style = MaterialTheme.typography.titleLarge)

        if (customers.isNotEmpty()) {
          ExposedDropdownMenuBox(
            expanded = dropdownExpanded,
            onExpandedChange = { dropdownExpanded = it },
          ) {
            OutlinedTextField(
              value = selectedCustomer?.customerName?.ifBlank { selectedCustomer?.billNumber }
                ?: "Select Customer (Optional)",
              onValueChange = {},
              readOnly = true,
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
              modifier =
                Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                  .fillMaxWidth(),
            )
            ExposedDropdownMenu(
              expanded = dropdownExpanded,
              onDismissRequest = { dropdownExpanded = false },
            ) {
              customers.forEach { c ->
                DropdownMenuItem(
                  text = {
                    Text("${c.customerName.ifBlank { c.billNumber }} (${c.vehicleModel})")
                  },
                  onClick = {
                    selectedCustomer = c
                    customerName = c.customerName
                    keepNameBlank = c.keepNameBlankOnBill
                    phoneNumber = c.phoneNumber
                    vehicleNo = c.vehicleNo
                    vehicleModel = c.vehicleModel
                    if (c.totalInstallments > 0) kistNumber = "1 / ${c.totalInstallments}"
                    if (c.installmentAmount > 0) amountText = c.installmentAmount.toString()
                    if (c.customerSignatureBase64.isNotBlank()) customerSig = c.customerSignatureBase64
                    dropdownExpanded = false
                  },
                )
              }
            }
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = slipNumber,
            onValueChange = { slipNumber = it },
            label = { Text("Slip No") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = kistNumber,
            onValueChange = { kistNumber = it },
            label = { Text("Kist No (e.g. 3/18)") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }

        OutlinedTextField(
          value = customerName,
          onValueChange = { customerName = it },
          label = { Text("Customer Name") },
          enabled = !keepNameBlank,
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = keepNameBlank, onCheckedChange = { keepNameBlank = it })
          Text("Keep Name blank on slip", style = MaterialTheme.typography.bodyMedium)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it.take(15) },
            label = { Text("Mobile") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = vehicleNo,
            onValueChange = { vehicleNo = it.uppercase() },
            label = { Text("Vehicle No") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }

        OutlinedTextField(
          value = vehicleModel,
          onValueChange = { vehicleModel = it },
          label = { Text("E-Rickshaw Model") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
          label = { Text("Kist Amount Received ₹") },
          enabled = !keepAmountBlank,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = keepAmountBlank, onCheckedChange = { keepAmountBlank = it })
          Text("Keep Amount blank on slip", style = MaterialTheme.typography.bodyMedium)
        }

        OutlinedTextField(
          value = paymentDate,
          onValueChange = { paymentDate = it },
          label = { Text("Payment Date") },
          enabled = !keepDateBlank,
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = keepDateBlank, onCheckedChange = { keepDateBlank = it })
          Text("Keep Date blank on slip", style = MaterialTheme.typography.bodyMedium)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          AgencyDefaults.PAYMENT_METHODS.forEach { method ->
            FilterChip(
              selected = paymentMethod == method,
              onClick = { paymentMethod = method },
              label = { Text(method) },
            )
          }
        }

        OutlinedTextField(
          value = nextDueDate,
          onValueChange = { nextDueDate = it },
          label = { Text("Next Kist Due Date (Optional)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
          value = remarks,
          onValueChange = { remarks = it },
          label = { Text("Remarks") },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )

        SignaturePreviewField(
          label = "Customer Signature",
          signatureBase64 = customerSig,
          onSignatureChanged = { customerSig = it },
        )
        SignaturePreviewField(
          label = "Agency Owner Signature",
          signatureBase64 = ownerSig,
          onSignatureChanged = { ownerSig = it },
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(onClick = onDismiss) { Text("Cancel") }
          Spacer(Modifier.width(8.dp))
          OutlinedButton(
            onClick = {
              onSaveAndShare(
                KistSlip(
                  customerId = selectedCustomer?.id ?: "",
                  slipNumber = slipNumber,
                  customerName = if (keepNameBlank) "" else customerName,
                  keepNameBlank = keepNameBlank,
                  phoneNumber = phoneNumber,
                  vehicleNo = vehicleNo,
                  vehicleModel = vehicleModel,
                  kistNumber = kistNumber,
                  amountPaid = if (keepAmountBlank) 0L else amountText.toLongOrNull() ?: 0L,
                  keepAmountBlank = keepAmountBlank,
                  paymentDate = if (keepDateBlank) "" else paymentDate,
                  keepDateBlank = keepDateBlank,
                  paymentMethod = paymentMethod,
                  nextDueDate = nextDueDate,
                  remarks = remarks,
                  customerSignatureBase64 = customerSig,
                  ownerSignatureBase64 = ownerSig,
                ),
                false,
              )
            }
          ) {
            Text("Save")
          }
          Spacer(Modifier.width(8.dp))
          Button(
            onClick = {
              onSaveAndShare(
                KistSlip(
                  customerId = selectedCustomer?.id ?: "",
                  slipNumber = slipNumber,
                  customerName = if (keepNameBlank) "" else customerName,
                  keepNameBlank = keepNameBlank,
                  phoneNumber = phoneNumber,
                  vehicleNo = vehicleNo,
                  vehicleModel = vehicleModel,
                  kistNumber = kistNumber,
                  amountPaid = if (keepAmountBlank) 0L else amountText.toLongOrNull() ?: 0L,
                  keepAmountBlank = keepAmountBlank,
                  paymentDate = if (keepDateBlank) "" else paymentDate,
                  keepDateBlank = keepDateBlank,
                  paymentMethod = paymentMethod,
                  nextDueDate = nextDueDate,
                  remarks = remarks,
                  customerSignatureBase64 = customerSig,
                  ownerSignatureBase64 = ownerSig,
                ),
                true,
              )
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier.testTag("save_and_share_kist_button"),
          ) {
            Text("Save & Share")
          }
        }
      }
    }
  }
}
