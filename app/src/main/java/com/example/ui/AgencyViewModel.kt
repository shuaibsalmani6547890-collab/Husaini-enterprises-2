package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AgencyProfile
import com.example.data.model.CustomerRecord
import com.example.data.model.KistSlip
import com.example.data.repository.AgencyRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>

  data class Success<T>(val data: T) : UiState<T>

  data class Error(val message: String) : UiState<Nothing>
}

class AgencyViewModel(
  private val repository: AgencyRepository,
  val currentUserId: String,
) : ViewModel() {

  val agencyProfile: StateFlow<UiState<AgencyProfile>> =
    repository
      .observeAgencyProfile(currentUserId)
      .map<AgencyProfile, UiState<AgencyProfile>> { UiState.Success(it) }
      .catch { error ->
        val rootError = unwrapFirestore(error) ?: error
        Log.w(TAG, "Error observing agency profile", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to load agency details"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading,
      )

  val customers: StateFlow<UiState<List<CustomerRecord>>> =
    repository
      .observeCustomers(currentUserId)
      .map<List<CustomerRecord>, UiState<List<CustomerRecord>>> { UiState.Success(it) }
      .catch { error ->
        val rootError = unwrapFirestore(error) ?: error
        Log.w(TAG, "Error observing customers", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to load customer records"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading,
      )

  val kistSlips: StateFlow<UiState<List<KistSlip>>> =
    repository
      .observeKistSlips(currentUserId)
      .map<List<KistSlip>, UiState<List<KistSlip>>> { UiState.Success(it) }
      .catch { error ->
        val rootError = unwrapFirestore(error) ?: error
        Log.w(TAG, "Error observing kist slips", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to load kist slips"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading,
      )

  private val _isSaving = MutableStateFlow(false)
  val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

  private val _activePhotos = MutableStateFlow<Map<String, String>>(emptyMap())
  val activePhotos: StateFlow<Map<String, String>> = _activePhotos.asStateFlow()

  private val _photosLoading = MutableStateFlow(false)
  val photosLoading: StateFlow<Boolean> = _photosLoading.asStateFlow()

  fun loadCustomerPhotos(customerId: String) {
    if (customerId.isBlank()) {
      _activePhotos.value = emptyMap()
      return
    }
    viewModelScope.launch {
      _photosLoading.value = true
      repository.getCustomerPhotos(customerId).onSuccess { map -> _activePhotos.value = map }
      _photosLoading.value = false
    }
  }

  fun clearActivePhotos() {
    _activePhotos.value = emptyMap()
  }

  fun saveCustomer(
    record: CustomerRecord,
    photos: Map<String, String>,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit,
  ) {
    viewModelScope.launch {
      _isSaving.value = true
      val result = repository.saveCustomer(record, photos)
      _isSaving.value = false
      result
        .onSuccess { id -> onSuccess(id) }
        .onFailure { e -> onError(e.localizedMessage ?: "Failed to save customer") }
    }
  }

  fun deleteCustomer(
    customerId: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
  ) {
    viewModelScope.launch {
      _isSaving.value = true
      val result = repository.deleteCustomer(customerId)
      _isSaving.value = false
      result
        .onSuccess { onSuccess() }
        .onFailure { e -> onError(e.localizedMessage ?: "Failed to delete customer") }
    }
  }

  fun removeCustomerPhoto(
    customerId: String,
    photoType: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
  ) {
    viewModelScope.launch {
      repository
        .removeCustomerPhoto(customerId, photoType)
        .onSuccess {
          _activePhotos.value = _activePhotos.value - photoType
          onSuccess()
        }
        .onFailure { e -> onError(e.localizedMessage ?: "Failed to remove photo") }
    }
  }

  fun saveKistSlip(
    slip: KistSlip,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit,
  ) {
    viewModelScope.launch {
      _isSaving.value = true
      val result = repository.saveKistSlip(slip)
      _isSaving.value = false
      result
        .onSuccess { id -> onSuccess(id) }
        .onFailure { e -> onError(e.localizedMessage ?: "Failed to save kist slip") }
    }
  }

  fun deleteKistSlip(
    slipId: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
  ) {
    viewModelScope.launch {
      repository
        .deleteKistSlip(slipId)
        .onSuccess { onSuccess() }
        .onFailure { e -> onError(e.localizedMessage ?: "Failed to delete kist slip") }
    }
  }

  fun saveAgencyProfile(
    profile: AgencyProfile,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
  ) {
    viewModelScope.launch {
      _isSaving.value = true
      val result = repository.saveAgencyProfile(profile)
      _isSaving.value = false
      result
        .onSuccess { onSuccess() }
        .onFailure { e -> onError(e.localizedMessage ?: "Failed to save agency settings") }
    }
  }

  suspend fun fetchPhotosDirect(customerId: String): Map<String, String> {
    if (customerId.isBlank()) return emptyMap()
    return repository.getCustomerPhotos(customerId).getOrDefault(emptyMap())
  }

  private fun unwrapFirestore(error: Throwable): FirebaseFirestoreException? {
    val firestoreError =
      generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    return firestoreError
  }

  private companion object {
    const val TAG = "AgencyVM"
  }
}
