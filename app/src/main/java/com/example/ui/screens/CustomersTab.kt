package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AgencyDefaults
import com.example.data.model.AgencyProfile
import com.example.data.model.CustomerRecord
import com.example.ui.UiState
import com.example.utils.FormatUtils

@Composable
fun CustomersTab(
  agencyState: UiState<AgencyProfile>,
  customersState: UiState<List<CustomerRecord>>,
  onAddCustomer: () -> Unit,
  onEditCustomer: (CustomerRecord) -> Unit,
  onOneClickBill: (CustomerRecord) -> Unit,
  onCreateKistSlip: (CustomerRecord) -> Unit,
  onEditAgencyClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var searchQuery by remember { mutableStateOf("") }
  var modeFilter by remember { mutableStateOf("all") } // all, cash, finance
  var modelFilter by remember { mutableStateOf<String?>(null) }

  val agency =
    when (agencyState) {
      is UiState.Success -> agencyState.data
      else -> AgencyProfile()
    }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Showroom Summary Header Card
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              color = MaterialTheme.colorScheme.secondary,
              shape = MaterialTheme.shapes.small,
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(
                  Icons.Default.CloudDone,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSecondary,
                  modifier = Modifier.size(14.dp),
                )
                Text(
                  text = "LIVE SYNCED",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSecondary,
                )
              }
            }
            IconButton(
              onClick = onEditAgencyClick,
              modifier = Modifier.testTag("edit_agency_quick_button"),
            ) {
              Icon(
                Icons.Default.Edit,
                contentDescription = "Edit agency details",
                tint = MaterialTheme.colorScheme.onPrimary,
              )
            }
          }

          Text(
            text = agency.agencyName.ifBlank { AgencyDefaults.DEFAULT_AGENCY_NAME },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onPrimary,
          )
          Text(
            text = agency.ownerNames.ifBlank { AgencyDefaults.DEFAULT_OWNER_NAMES },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f),
          )
          Text(
            text = "Mob: ${agency.contactNumbers.ifBlank { AgencyDefaults.DEFAULT_CONTACT_NUMBERS }}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
          )

          if (customersState is UiState.Success) {
            val all = customersState.data
            val cashCount = all.count { it.paymentMode == "cash" }
            val financeCount = all.count { it.paymentMode == "finance" }
            Spacer(Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              SummaryPill("Total: ${all.size}", Modifier.weight(1f))
              SummaryPill("Cash: $cashCount", Modifier.weight(1f))
              SummaryPill("Finance: $financeCount", Modifier.weight(1f))
            }
          }
        }
      }
    }

    // Search Bar
    item {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search name, mobile, vehicle no, model...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().testTag("customer_search_input"),
      )
    }

    // Mode & Model Filter Chips
    item {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = modeFilter == "all" && modelFilter == null,
            onClick = {
              modeFilter = "all"
              modelFilter = null
            },
            label = { Text("All") },
          )
          FilterChip(
            selected = modeFilter == "cash",
            onClick = { modeFilter = if (modeFilter == "cash") "all" else "cash" },
            label = { Text("Cash") },
          )
          FilterChip(
            selected = modeFilter == "finance",
            onClick = { modeFilter = if (modeFilter == "finance") "all" else "finance" },
            label = { Text("Finance / Kist") },
          )
          AgencyDefaults.E_RICKSHAW_MODELS.forEach { model ->
            FilterChip(
              selected = modelFilter == model,
              onClick = { modelFilter = if (modelFilter == model) null else model },
              label = { Text(model) },
            )
          }
        }
      }
    }

    when (customersState) {
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
          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = customersState.message,
              color = MaterialTheme.colorScheme.error,
              modifier = Modifier.padding(16.dp),
            )
          }
        }
      }
      is UiState.Success -> {
        val filtered =
          customersState.data.filter { c ->
            val matchesMode = modeFilter == "all" || c.paymentMode == modeFilter
            val matchesModel =
              modelFilter == null || c.vehicleModel.equals(modelFilter, ignoreCase = true)
            val q = searchQuery.trim().lowercase()
            val matchesQuery =
              q.isEmpty() ||
                c.customerName.lowercase().contains(q) ||
                c.phoneNumber.lowercase().contains(q) ||
                c.vehicleNo.lowercase().contains(q) ||
                c.vehicleModel.lowercase().contains(q) ||
                c.billNumber.lowercase().contains(q) ||
                c.aadharNumber.lowercase().contains(q)
            matchesMode && matchesModel && matchesQuery
          }

        if (filtered.isEmpty()) {
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              shape = MaterialTheme.shapes.large,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(
                  Icons.Default.DirectionsCar,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(44.dp),
                )
                Text(
                  text = if (customersState.data.isEmpty()) "No Customer Bills Yet" else "No Matching Records",
                  style = MaterialTheme.typography.titleMedium,
                )
                Text(
                  text =
                    if (customersState.data.isEmpty())
                      "Tap below to add your first e-rickshaw customer, photos & signatures and generate a 1-click bill."
                    else "Try clearing your search or filter.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (customersState.data.isEmpty()) {
                  Button(
                    onClick = onAddCustomer,
                    modifier = Modifier.testTag("empty_add_customer_button"),
                  ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("New Customer & Bill")
                  }
                }
              }
            }
          }
        } else {
          items(filtered, key = { it.id }) { customer ->
            CustomerCard(
              customer = customer,
              onClick = { onEditCustomer(customer) },
              onOneClickBill = { onOneClickBill(customer) },
              onCreateKistSlip = { onCreateKistSlip(customer) },
            )
          }
        }
      }
    }

    item { Spacer(Modifier.height(76.dp)) }
  }
}

@Composable
private fun SummaryPill(text: String, modifier: Modifier = Modifier) {
  Surface(
    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
    shape = MaterialTheme.shapes.small,
    modifier = modifier,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onPrimary,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun CustomerCard(
  customer: CustomerRecord,
  onClick: () -> Unit,
  onOneClickBill: () -> Unit,
  onCreateKistSlip: () -> Unit,
) {
  val isFinance = customer.paymentMode == "finance"
  val displayName =
    if (customer.keepNameBlankOnBill || customer.customerName.isBlank()) "Blank Name (On Bill)"
    else customer.customerName
  val displayDate =
    if (customer.keepDateBlankOnBill || customer.purchaseDate.isBlank()) "Blank Date"
    else customer.purchaseDate
  val displayPrice =
    if (customer.keepPriceBlankOnBill || customer.totalPrice <= 0L) "Blank Price"
    else FormatUtils.formatInr(customer.totalPrice)

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = MaterialTheme.shapes.large,
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    modifier =
      Modifier.fillMaxWidth()
        .clickable(onClick = onClick)
        .testTag("customer_card_${customer.id}"),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = customer.billNumber,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
            Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
              text = displayDate,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Spacer(Modifier.height(2.dp))
          Text(
            text = displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (customer.phoneNumber.isNotBlank()) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                Icons.Default.Phone,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = customer.phoneNumber,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        Surface(
          color =
            if (isFinance) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.secondaryContainer,
          shape = MaterialTheme.shapes.small,
        ) {
          Text(
            text = if (isFinance) "FINANCE" else "CASH",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color =
              if (isFinance) MaterialTheme.colorScheme.onTertiaryContainer
              else MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }

      // Vehicle & Price Row
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = customer.vehicleModel.ifBlank { "E-Rickshaw" },
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text =
                if (customer.vehicleNo.isNotBlank()) "No: ${customer.vehicleNo}"
                else "Vehicle No: Pending",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = displayPrice,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
            if (isFinance && customer.totalInstallments > 0) {
              Text(
                text =
                  "Fin: ${FormatUtils.formatInr(customer.financeAmount)} • ${customer.totalInstallments} Kist",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
              )
            }
          }
        }
      }

      // Attachments & Signatures status indicators
      val photoCount =
        listOf(customer.hasCustomerPhoto, customer.hasVehiclePhoto, customer.hasWarrantyPhoto)
          .count { it }
      val sigCount =
        listOf(
            customer.customerSignatureBase64.isNotBlank(),
            customer.ownerSignatureBase64.isNotBlank(),
          )
          .count { it }
      if (photoCount > 0 || sigCount > 0) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          if (photoCount > 0) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.secondary,
              )
              Text(
                text = "$photoCount Photo(s)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
              )
            }
          }
          if (sigCount > 0) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                Icons.Default.Draw,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = "$sigCount Sign(s)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }

      // 1-Click Bill & Kist Slip Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(
          onClick = onOneClickBill,
          colors =
            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.weight(1f).testTag("one_click_bill_${customer.id}"),
        ) {
          Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("1-Click Bill")
        }
        OutlinedButton(
          onClick = onCreateKistSlip,
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.weight(1f).testTag("kist_slip_btn_${customer.id}"),
        ) {
          Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("Kist Slip")
        }
      }
    }
  }
}
