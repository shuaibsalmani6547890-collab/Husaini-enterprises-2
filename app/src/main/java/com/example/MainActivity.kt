package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.data.model.AgencyProfile
import com.example.data.model.CustomerRecord
import com.example.data.repository.AgencyRepository
import com.example.ui.AgencyViewModel
import com.example.ui.UiState
import com.example.ui.screens.AgencySettingsTab
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BillPreviewScreen
import com.example.ui.screens.CustomerFormScreen
import com.example.ui.screens.CustomersTab
import com.example.ui.screens.KistSlipsTab
import com.example.ui.theme.HusainiAgencyTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

private const val ROUTE_HOME = "home"
private const val ROUTE_CUSTOMER_FORM = "customer_form"
private const val ROUTE_BILL_PREVIEW = "bill_preview"

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      HusainiAgencyTheme {
        AppRoot()
      }
    }
  }
}

@Composable
private fun AppRoot() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val auth = Firebase.auth
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

  LaunchedEffect(Unit) {
    if (auth.currentUser == null) {
      val credentialManager = CredentialManager.create(context)
      attemptAutoSignIn(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = {},
        onUnauthenticated = {},
        scope = scope,
      )
    }
  }

  val user = currentUser
  if (user == null) {
    AuthScreen(onAuthSuccess = {})
  } else {
    key(user.uid) {
      MainAppShell(
        currentUserId = user.uid,
        userEmail = user.email,
      )
    }
  }
}

@Composable
private fun MainAppShell(
  currentUserId: String,
  userEmail: String?,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val navController = rememberNavController()

  val viewModel: AgencyViewModel =
    viewModel(
      key = currentUserId,
      factory =
        viewModelFactory {
          initializer {
            val app =
              checkNotNull(this[APPLICATION_KEY]) {
                "APPLICATION_KEY missing from CreationExtras"
              }
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            AgencyViewModel(AgencyRepository(db), currentUserId)
          }
        },
    )

  val agencyState by viewModel.agencyProfile.collectAsStateWithLifecycle()
  val customersState by viewModel.customers.collectAsStateWithLifecycle()
  val kistSlipsState by viewModel.kistSlips.collectAsStateWithLifecycle()
  val activePhotos by viewModel.activePhotos.collectAsStateWithLifecycle()
  val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

  val agencyProfile =
    when (val s = agencyState) {
      is UiState.Success -> s.data
      else -> AgencyProfile(userId = currentUserId)
    }
  val customerList =
    when (val s = customersState) {
      is UiState.Success -> s.data
      else -> emptyList()
    }

  var selectedTab by remember { mutableIntStateOf(0) }
  var editingCustomer by remember { mutableStateOf<CustomerRecord?>(null) }
  var previewCustomer by remember { mutableStateOf<CustomerRecord?>(null) }
  var kistPrefillCustomer by remember { mutableStateOf<CustomerRecord?>(null) }

  NavHost(navController = navController, startDestination = ROUTE_HOME) {
    composable(ROUTE_HOME) {
      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
          snackbarHost = { SnackbarHost(snackbarHostState) },
          contentWindowInsets = WindowInsets(0, 0, 0, 0),
          floatingActionButton = {
            if (selectedTab == 0) {
              FloatingActionButton(
                onClick = {
                  editingCustomer = null
                  viewModel.clearActivePhotos()
                  navController.navigate(ROUTE_CUSTOMER_FORM)
                },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.testTag("add_customer_fab"),
              ) {
                Icon(Icons.Default.Add, contentDescription = "Add new customer")
              }
            }
          },
          bottomBar = {
            if (!useRail) {
              NavigationBar {
                NavigationBarItem(
                  selected = selectedTab == 0,
                  onClick = { selectedTab = 0 },
                  icon = { Icon(Icons.Default.People, contentDescription = null) },
                  label = { Text("Customers") },
                )
                NavigationBarItem(
                  selected = selectedTab == 1,
                  onClick = { selectedTab = 1 },
                  icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                  label = { Text("Kist Slips") },
                )
                NavigationBarItem(
                  selected = selectedTab == 2,
                  onClick = { selectedTab = 2 },
                  icon = { Icon(Icons.Default.Store, contentDescription = null) },
                  label = { Text("Agency") },
                )
              }
            }
          },
        ) { innerPadding ->
          Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (useRail) {
              NavigationRail(
                header = {
                  Icon(
                    Icons.Default.ElectricRickshaw,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 12.dp),
                  )
                }
              ) {
                NavigationRailItem(
                  selected = selectedTab == 0,
                  onClick = { selectedTab = 0 },
                  icon = { Icon(Icons.Default.People, contentDescription = null) },
                  label = { Text("Customers") },
                )
                NavigationRailItem(
                  selected = selectedTab == 1,
                  onClick = { selectedTab = 1 },
                  icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                  label = { Text("Kist Slips") },
                )
                NavigationRailItem(
                  selected = selectedTab == 2,
                  onClick = { selectedTab = 2 },
                  icon = { Icon(Icons.Default.Store, contentDescription = null) },
                  label = { Text("Agency") },
                )
              }
            }

            when (selectedTab) {
              0 ->
                CustomersTab(
                  agencyState = agencyState,
                  customersState = customersState,
                  onAddCustomer = {
                    editingCustomer = null
                    viewModel.clearActivePhotos()
                    navController.navigate(ROUTE_CUSTOMER_FORM)
                  },
                  onEditCustomer = { customer ->
                    editingCustomer = customer
                    viewModel.loadCustomerPhotos(customer.id)
                    navController.navigate(ROUTE_CUSTOMER_FORM)
                  },
                  onOneClickBill = { customer ->
                    previewCustomer = customer
                    viewModel.loadCustomerPhotos(customer.id)
                    navController.navigate(ROUTE_BILL_PREVIEW)
                  },
                  onCreateKistSlip = { customer ->
                    kistPrefillCustomer = customer
                    selectedTab = 1
                  },
                  onEditAgencyClick = { selectedTab = 2 },
                  modifier = Modifier.weight(1f),
                )
              1 ->
                KistSlipsTab(
                  agency = agencyProfile,
                  slipsState = kistSlipsState,
                  customers = customerList,
                  prefillCustomer = kistPrefillCustomer,
                  onClearPrefill = { kistPrefillCustomer = null },
                  onSaveSlip = { slip, onSaved ->
                    viewModel.saveKistSlip(
                      slip = slip,
                      onSuccess = { id ->
                        onSaved(slip.copy(id = id))
                        scope.launch { snackbarHostState.showSnackbar("Temporary Kist Slip saved") }
                      },
                      onError = { err ->
                        scope.launch { snackbarHostState.showSnackbar(err) }
                      },
                    )
                  },
                  onDeleteSlip = { slipId ->
                    viewModel.deleteKistSlip(
                      slipId = slipId,
                      onSuccess = {
                        scope.launch { snackbarHostState.showSnackbar("Kist slip deleted") }
                      },
                      onError = { err -> scope.launch { snackbarHostState.showSnackbar(err) } },
                    )
                  },
                  modifier = Modifier.weight(1f),
                )
              2 ->
                AgencySettingsTab(
                  agencyState = agencyState,
                  userEmail = userEmail,
                  isSaving = isSaving,
                  onSaveProfile = { updated ->
                    viewModel.saveAgencyProfile(
                      profile = updated,
                      onSuccess = {
                        scope.launch { snackbarHostState.showSnackbar("Agency details updated") }
                      },
                      onError = { err -> scope.launch { snackbarHostState.showSnackbar(err) } },
                    )
                  },
                  onSignOut = {
                    val credentialManager = CredentialManager.create(context)
                    signOut(credentialManager, scope)
                  },
                  modifier = Modifier.weight(1f),
                )
            }
          }
        }
      }
    }

    composable(ROUTE_CUSTOMER_FORM) {
      CustomerFormScreen(
        initialCustomer = editingCustomer,
        agencyProfile = agencyProfile,
        loadedPhotos = activePhotos,
        isSaving = isSaving,
        onSaveCustomer = { record, newPhotos, openBill ->
          viewModel.saveCustomer(
            record = record,
            photos = newPhotos,
            onSuccess = { savedId ->
              val savedRecord = record.copy(id = savedId)
              if (openBill) {
                previewCustomer = savedRecord
                viewModel.loadCustomerPhotos(savedId)
                navController.navigate(ROUTE_BILL_PREVIEW) {
                  popUpTo(ROUTE_HOME)
                }
              } else {
                navController.popBackStack()
                scope.launch { snackbarHostState.showSnackbar("Customer saved & synced") }
              }
            },
            onError = { err -> scope.launch { snackbarHostState.showSnackbar(err) } },
          )
        },
        onDeleteCustomer =
          editingCustomer?.let { existing ->
            { id ->
              viewModel.deleteCustomer(
                customerId = id,
                onSuccess = {
                  navController.popBackStack()
                  scope.launch { snackbarHostState.showSnackbar("Customer deleted") }
                },
                onError = { err -> scope.launch { snackbarHostState.showSnackbar(err) } },
              )
            }
          },
        onRemovePhoto = { photoType ->
          editingCustomer?.id?.let { cid ->
            viewModel.removeCustomerPhoto(
              customerId = cid,
              photoType = photoType,
              onSuccess = {},
              onError = { err -> scope.launch { snackbarHostState.showSnackbar(err) } },
            )
          }
        },
        onBack = { navController.popBackStack() },
      )
    }

    composable(ROUTE_BILL_PREVIEW) {
      val target = previewCustomer ?: customerList.firstOrNull()
      if (target != null) {
        val liveCustomer = customerList.firstOrNull { it.id == target.id } ?: target
        BillPreviewScreen(
          agency = agencyProfile,
          customer = liveCustomer,
          photos = activePhotos,
          onEditCustomer = {
            editingCustomer = liveCustomer
            navController.navigate(ROUTE_CUSTOMER_FORM)
          },
          onCreateKistSlip = {
            kistPrefillCustomer = liveCustomer
            selectedTab = 1
            navController.popBackStack(ROUTE_HOME, inclusive = false)
          },
          onBack = { navController.popBackStack() },
        )
      }
    }
  }
}
