package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firestore.OperationType
import com.example.data.firestore.handleFirestoreError
import com.example.data.model.AgencyDefaults
import com.example.data.model.AgencyProfile
import com.example.data.model.CustomerPhoto
import com.example.data.model.CustomerRecord
import com.example.data.model.KistSlip
import com.example.data.model.PhotoTypes
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AgencyRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(
    context: Context
  ) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    Firebase.auth,
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must sign in with Google before accessing Firestore.")

  private fun userDoc(uid: String) = db.collection("users").document(uid)

  fun observeAgencyProfile(uid: String): Flow<AgencyProfile> = flow {
    val docRef = userDoc(uid).collection("agency_profiles").document("default")
    emitAll(
      docRef
        .snapshots()
        .map { snapshot ->
          snapshot.toObject(
            AgencyProfile::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
            ?: AgencyProfile(
              id = "default",
              userId = uid,
              agencyName = AgencyDefaults.DEFAULT_AGENCY_NAME,
              ownerNames = AgencyDefaults.DEFAULT_OWNER_NAMES,
              contactNumbers = AgencyDefaults.DEFAULT_CONTACT_NUMBERS,
              agencyAddress = AgencyDefaults.DEFAULT_ADDRESS,
            )
        }
        .catch { error ->
          logAndRethrowIfActive(error, OperationType.GET, docRef.path, uid)
        }
    )
  }

  suspend fun saveAgencyProfile(profile: AgencyProfile): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("agency_profiles").document("default")
    val existing = docRef.get().await()
    if (!existing.exists()) {
      val payload =
        mapOf(
          "userId" to uid,
          "agencyName" to profile.agencyName.trim().ifBlank { AgencyDefaults.DEFAULT_AGENCY_NAME },
          "ownerNames" to profile.ownerNames.trim(),
          "contactNumbers" to profile.contactNumbers.trim(),
          "agencyAddress" to profile.agencyAddress.trim(),
          "defaultOwnerSignatureBase64" to profile.defaultOwnerSignatureBase64,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      try {
        docRef.set(payload).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, docRef.path)
        throw e
      }
    } else {
      val payload =
        mapOf(
          "agencyName" to profile.agencyName.trim().ifBlank { AgencyDefaults.DEFAULT_AGENCY_NAME },
          "ownerNames" to profile.ownerNames.trim(),
          "contactNumbers" to profile.contactNumbers.trim(),
          "agencyAddress" to profile.agencyAddress.trim(),
          "defaultOwnerSignatureBase64" to profile.defaultOwnerSignatureBase64,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      try {
        docRef.update(payload).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        throw e
      }
    }
  }

  fun observeCustomers(uid: String): Flow<List<CustomerRecord>> = flow {
    val colRef = userDoc(uid).collection("customers")
    emitAll(
      colRef
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              CustomerRecord::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          logAndRethrowIfActive(error, OperationType.LIST, colRef.path, uid)
        }
    )
  }

  suspend fun getCustomerById(customerId: String): Result<CustomerRecord?> = runCatching {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("customers").document(customerId)
    try {
      docRef
        .get()
        .await()
        .toObject(
          CustomerRecord::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      throw e
    }
  }

  suspend fun saveCustomer(
    record: CustomerRecord,
    photos: Map<String, String> = emptyMap(),
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val isNew = record.id.isBlank()
    val customerId = if (isNew) UUID.randomUUID().toString() else record.id
    val docRef = userDoc(uid).collection("customers").document(customerId)

    val hasCustomerPhoto = photos.containsKey(PhotoTypes.CUSTOMER) || record.hasCustomerPhoto
    val hasVehiclePhoto = photos.containsKey(PhotoTypes.VEHICLE) || record.hasVehiclePhoto
    val hasWarrantyPhoto = photos.containsKey(PhotoTypes.WARRANTY_CARD) || record.hasWarrantyPhoto

    val billNumber =
      record.billNumber.trim().ifBlank {
        "HE-${System.currentTimeMillis().toString().takeLast(6)}"
      }
    val mode = if (record.paymentMode == "finance") "finance" else "cash"

    if (isNew) {
      val payload =
        mapOf(
          "userId" to uid,
          "billNumber" to billNumber,
          "customerName" to record.customerName.trim(),
          "keepNameBlankOnBill" to record.keepNameBlankOnBill,
          "phoneNumber" to record.phoneNumber.trim(),
          "aadharNumber" to record.aadharNumber.trim(),
          "customerAddress" to record.customerAddress.trim(),
          "vehicleNo" to record.vehicleNo.trim(),
          "vehicleModel" to record.vehicleModel.trim(),
          "purchaseDate" to record.purchaseDate.trim(),
          "keepDateBlankOnBill" to record.keepDateBlankOnBill,
          "totalPrice" to record.totalPrice.coerceIn(0L, 10_000_000L),
          "keepPriceBlankOnBill" to record.keepPriceBlankOnBill,
          "paymentMode" to mode,
          "downPayment" to record.downPayment.coerceIn(0L, 10_000_000L),
          "financeAmount" to record.financeAmount.coerceIn(0L, 10_000_000L),
          "totalInstallments" to record.totalInstallments.coerceIn(0L, 120L),
          "installmentAmount" to record.installmentAmount.coerceIn(0L, 1_000_000L),
          "purchasedFrom" to record.purchasedFrom.trim(),
          "batteryGuarantee" to record.batteryGuarantee.trim(),
          "batteryWarranty" to record.batteryWarranty.trim(),
          "otherWarranty" to record.otherWarranty.trim(),
          "notes" to record.notes.trim(),
          "hasCustomerPhoto" to hasCustomerPhoto,
          "hasVehiclePhoto" to hasVehiclePhoto,
          "hasWarrantyPhoto" to hasWarrantyPhoto,
          "customerSignatureBase64" to record.customerSignatureBase64,
          "ownerSignatureBase64" to record.ownerSignatureBase64,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      try {
        docRef.set(payload).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, docRef.path)
        throw e
      }
    } else {
      val payload =
        mapOf(
          "billNumber" to billNumber,
          "customerName" to record.customerName.trim(),
          "keepNameBlankOnBill" to record.keepNameBlankOnBill,
          "phoneNumber" to record.phoneNumber.trim(),
          "aadharNumber" to record.aadharNumber.trim(),
          "customerAddress" to record.customerAddress.trim(),
          "vehicleNo" to record.vehicleNo.trim(),
          "vehicleModel" to record.vehicleModel.trim(),
          "purchaseDate" to record.purchaseDate.trim(),
          "keepDateBlankOnBill" to record.keepDateBlankOnBill,
          "totalPrice" to record.totalPrice.coerceIn(0L, 10_000_000L),
          "keepPriceBlankOnBill" to record.keepPriceBlankOnBill,
          "paymentMode" to mode,
          "downPayment" to record.downPayment.coerceIn(0L, 10_000_000L),
          "financeAmount" to record.financeAmount.coerceIn(0L, 10_000_000L),
          "totalInstallments" to record.totalInstallments.coerceIn(0L, 120L),
          "installmentAmount" to record.installmentAmount.coerceIn(0L, 1_000_000L),
          "purchasedFrom" to record.purchasedFrom.trim(),
          "batteryGuarantee" to record.batteryGuarantee.trim(),
          "batteryWarranty" to record.batteryWarranty.trim(),
          "otherWarranty" to record.otherWarranty.trim(),
          "notes" to record.notes.trim(),
          "hasCustomerPhoto" to hasCustomerPhoto,
          "hasVehiclePhoto" to hasVehiclePhoto,
          "hasWarrantyPhoto" to hasWarrantyPhoto,
          "customerSignatureBase64" to record.customerSignatureBase64,
          "ownerSignatureBase64" to record.ownerSignatureBase64,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      try {
        docRef.update(payload).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        throw e
      }
    }

    // Upload any modified/new photos
    for ((photoType, base64) in photos) {
      if (base64.isNotBlank()) {
        saveCustomerPhoto(customerId, photoType, base64).getOrThrow()
      }
    }

    customerId
  }

  suspend fun deleteCustomer(customerId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("customers").document(customerId)
    try {
      // Delete photos subcollection docs if present
      val photoSnaps = docRef.collection("photos").get().await()
      for (photoDoc in photoSnaps.documents) {
        photoDoc.reference.delete().await()
      }
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  suspend fun saveCustomerPhoto(
    customerId: String,
    photoType: String,
    imageBase64: String,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val photoRef =
      userDoc(uid).collection("customers").document(customerId).collection("photos").document(photoType)
    val existing = photoRef.get().await()
    if (!existing.exists()) {
      val payload =
        mapOf(
          "userId" to uid,
          "customerId" to customerId,
          "photoType" to photoType,
          "imageBase64" to imageBase64,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      try {
        photoRef.set(payload).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, photoRef.path)
        throw e
      }
    } else {
      val payload =
        mapOf(
          "imageBase64" to imageBase64,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      try {
        photoRef.update(payload).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, photoRef.path)
        throw e
      }
    }
  }

  suspend fun getCustomerPhotos(customerId: String): Result<Map<String, String>> = runCatching {
    val uid = requireUserId()
    val colRef = userDoc(uid).collection("customers").document(customerId).collection("photos")
    try {
      val snap = colRef.get().await()
      snap
        .toObjects(
          CustomerPhoto::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
        .associate { it.photoType to it.imageBase64 }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, colRef.path)
      throw e
    }
  }

  suspend fun removeCustomerPhoto(customerId: String, photoType: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val customerRef = userDoc(uid).collection("customers").document(customerId)
    val photoRef = customerRef.collection("photos").document(photoType)
    val flagField =
      when (photoType) {
        PhotoTypes.CUSTOMER -> "hasCustomerPhoto"
        PhotoTypes.VEHICLE -> "hasVehiclePhoto"
        PhotoTypes.WARRANTY_CARD -> "hasWarrantyPhoto"
        else -> throw IllegalArgumentException("Unknown photoType: $photoType")
      }
    try {
      photoRef.delete().await()
      customerRef.update(mapOf(flagField to false, "updatedAt" to FieldValue.serverTimestamp())).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, photoRef.path)
      throw e
    }
  }

  fun observeKistSlips(uid: String): Flow<List<KistSlip>> = flow {
    val colRef = userDoc(uid).collection("kist_slips")
    emitAll(
      colRef
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(KistSlip::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          logAndRethrowIfActive(error, OperationType.LIST, colRef.path, uid)
        }
    )
  }

  suspend fun saveKistSlip(slip: KistSlip): Result<String> = runCatching {
    val uid = requireUserId()
    val isNew = slip.id.isBlank()
    val slipId = if (isNew) UUID.randomUUID().toString() else slip.id
    val docRef = userDoc(uid).collection("kist_slips").document(slipId)
    val slipNumber =
      slip.slipNumber.trim().ifBlank {
        "KIST-${System.currentTimeMillis().toString().takeLast(6)}"
      }

    if (isNew) {
      val payload =
        mapOf(
          "userId" to uid,
          "customerId" to slip.customerId,
          "slipNumber" to slipNumber,
          "customerName" to slip.customerName.trim(),
          "keepNameBlank" to slip.keepNameBlank,
          "phoneNumber" to slip.phoneNumber.trim(),
          "vehicleNo" to slip.vehicleNo.trim(),
          "vehicleModel" to slip.vehicleModel.trim(),
          "kistNumber" to slip.kistNumber.trim(),
          "amountPaid" to slip.amountPaid.coerceIn(0L, 10_000_000L),
          "keepAmountBlank" to slip.keepAmountBlank,
          "paymentDate" to slip.paymentDate.trim(),
          "keepDateBlank" to slip.keepDateBlank,
          "paymentMethod" to slip.paymentMethod.trim(),
          "nextDueDate" to slip.nextDueDate.trim(),
          "remarks" to slip.remarks.trim(),
          "customerSignatureBase64" to slip.customerSignatureBase64,
          "ownerSignatureBase64" to slip.ownerSignatureBase64,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      try {
        docRef.set(payload).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, docRef.path)
        throw e
      }
    } else {
      val payload =
        mapOf(
          "customerId" to slip.customerId,
          "slipNumber" to slipNumber,
          "customerName" to slip.customerName.trim(),
          "keepNameBlank" to slip.keepNameBlank,
          "phoneNumber" to slip.phoneNumber.trim(),
          "vehicleNo" to slip.vehicleNo.trim(),
          "vehicleModel" to slip.vehicleModel.trim(),
          "kistNumber" to slip.kistNumber.trim(),
          "amountPaid" to slip.amountPaid.coerceIn(0L, 10_000_000L),
          "keepAmountBlank" to slip.keepAmountBlank,
          "paymentDate" to slip.paymentDate.trim(),
          "keepDateBlank" to slip.keepDateBlank,
          "paymentMethod" to slip.paymentMethod.trim(),
          "nextDueDate" to slip.nextDueDate.trim(),
          "remarks" to slip.remarks.trim(),
          "customerSignatureBase64" to slip.customerSignatureBase64,
          "ownerSignatureBase64" to slip.ownerSignatureBase64,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      try {
        docRef.update(payload).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        throw e
      }
    }
    slipId
  }

  suspend fun deleteKistSlip(slipId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("kist_slips").document(slipId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  private fun logAndRethrowIfActive(
    error: Throwable,
    operationType: OperationType,
    path: String,
    uid: String,
  ): Nothing {
    val firestoreError =
      generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val exceptionToLog = firestoreError ?: (error as? Exception)
    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
      handleFirestoreError(exceptionToLog, operationType, path)
    }
    throw error
  }
}
