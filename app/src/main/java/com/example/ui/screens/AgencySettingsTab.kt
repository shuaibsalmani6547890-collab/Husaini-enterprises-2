package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import com.example.data.model.AgencyDefaults
import com.example.data.model.AgencyProfile
import com.example.ui.UiState
import com.example.ui.components.SignaturePreviewField

@Composable
fun AgencySettingsTab(
  agencyState: UiState<AgencyProfile>,
  userEmail: String?,
  isSaving: Boolean,
  onSaveProfile: (AgencyProfile) -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val loaded =
    when (agencyState) {
      is UiState.Success -> agencyState.data
      else -> AgencyProfile()
    }

  var agencyName by remember(loaded) {
    mutableStateOf(loaded.agencyName.ifBlank { AgencyDefaults.DEFAULT_AGENCY_NAME })
  }
  var ownerNames by remember(loaded) {
    mutableStateOf(loaded.ownerNames.ifBlank { AgencyDefaults.DEFAULT_OWNER_NAMES })
  }
  var contactNumbers by remember(loaded) {
    mutableStateOf(loaded.contactNumbers.ifBlank { AgencyDefaults.DEFAULT_CONTACT_NUMBERS })
  }
  var agencyAddress by remember(loaded) {
    mutableStateOf(loaded.agencyAddress.ifBlank { AgencyDefaults.DEFAULT_ADDRESS })
  }
  var defaultOwnerSig by remember(loaded) { mutableStateOf(loaded.defaultOwnerSignatureBase64) }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              Icons.Default.Store,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "Agency & Owner Details (Editable)",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }

          OutlinedTextField(
            value = agencyName,
            onValueChange = { agencyName = it },
            label = { Text("Agency Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("agency_name_input"),
          )

          OutlinedTextField(
            value = ownerNames,
            onValueChange = { ownerNames = it },
            label = { Text("Owner Names") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("agency_owners_input"),
          )

          OutlinedTextField(
            value = contactNumbers,
            onValueChange = { contactNumbers = it },
            label = { Text("Contact Numbers") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("agency_contacts_input"),
          )

          OutlinedTextField(
            value = agencyAddress,
            onValueChange = { agencyAddress = it },
            label = { Text("Agency Address / Branch") },
            maxLines = 2,
            modifier = Modifier.fillMaxWidth().testTag("agency_address_input"),
          )

          SignaturePreviewField(
            label = "Default Agency Owner Signature",
            signatureBase64 = defaultOwnerSig,
            onSignatureChanged = { defaultOwnerSig = it },
            testTag = "default_owner_signature_field",
          )

          Button(
            onClick = {
              onSaveProfile(
                loaded.copy(
                  agencyName = agencyName.trim().ifBlank { AgencyDefaults.DEFAULT_AGENCY_NAME },
                  ownerNames = ownerNames.trim(),
                  contactNumbers = contactNumbers.trim(),
                  agencyAddress = agencyAddress.trim(),
                  defaultOwnerSignatureBase64 = defaultOwnerSig,
                )
              )
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_agency_button"),
          ) {
            if (isSaving) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
              )
            } else {
              Icon(Icons.Default.Save, contentDescription = null)
              Spacer(Modifier.width(8.dp))
              Text("Save Agency Details")
            }
          }
        }
      }
    }

    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              Icons.Default.CloudSync,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
              text = "Multi-Mobile Cloud Sync",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          }
          Text(
            text =
              "Signed in as: ${userEmail ?: "Google Account"}\nTo view all customer details, bills, photos & kist slips on another mobile phone, install this app and sign in with the exact same Google account.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
          )
          Spacer(Modifier.height(4.dp))
          Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
            shape = MaterialTheme.shapes.small,
          ) {
            OutlinedButton(
              onClick = onSignOut,
              modifier = Modifier.fillMaxWidth().testTag("sign_out_button"),
            ) {
              Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
              Spacer(Modifier.width(8.dp))
              Text("Sign Out")
            }
          }
        }
      }
    }

    item { Spacer(Modifier.height(76.dp)) }
  }
}
