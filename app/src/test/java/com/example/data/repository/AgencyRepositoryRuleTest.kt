package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AgencyProfile
import com.example.data.model.CustomerRecord
import com.example.data.model.KistSlip
import com.example.data.model.PhotoTypes
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgencyRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun saveAndObserveCustomer_withOptionalBlankFields_succeeds() = runBlocking {
    val uid = signInTestUser(OWNER_EMAIL)
    val repo = AgencyRepository(firestore, auth)

    val customer =
      CustomerRecord(
        billNumber = "HE-TEST-001",
        customerName = "",
        keepNameBlankOnBill = true,
        phoneNumber = "7668732106",
        aadharNumber = "123456789012",
        vehicleNo = "UP21ER0001",
        vehicleModel = "Original Metro",
        purchaseDate = "",
        keepDateBlankOnBill = true,
        totalPrice = 0L,
        keepPriceBlankOnBill = true,
        paymentMode = "finance",
        financeAmount = 95000L,
        totalInstallments = 18L,
        installmentAmount = 6200L,
        purchasedFrom = "Ms Husaini Enterprises",
        batteryGuarantee = "12 Months",
        batteryWarranty = "6 Months",
        otherWarranty = "Motor 12M, Charger 6M",
      )

    val createdId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.saveCustomer(
          record = customer,
          photos = mapOf(PhotoTypes.WARRANTY_CARD to "dGVzdF9pbWFnZV9iYXNlNjQ="),
        ).getOrThrow()
      }
    assertTrue(createdId.isNotBlank())

    val observed =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeCustomers(uid).first { list -> list.any { it.id == createdId } }
      }
    val saved = observed.first { it.id == createdId }
    assertTrue(saved.keepNameBlankOnBill)
    assertTrue(saved.keepDateBlankOnBill)
    assertTrue(saved.keepPriceBlankOnBill)
    assertTrue(saved.hasWarrantyPhoto)

    val photos = withTimeout(DEFAULT_TIMEOUT_MS) { repo.getCustomerPhotos(createdId).getOrThrow() }
    assertEquals("dGVzdF9pbWFnZV9iYXNlNjQ=", photos[PhotoTypes.WARRANTY_CARD])
  }

  @Test
  fun saveAgencyProfile_andKistSlip_succeeds() = runBlocking {
    val uid = signInTestUser(OWNER_EMAIL)
    val repo = AgencyRepository(firestore, auth)

    val profile =
      AgencyProfile(
        agencyName = "Ms Husaini Enterprises",
        ownerNames = "Mohammad Anees, Mohammad Suhail",
        contactNumbers = "7668732106, 7906599049",
        agencyAddress = "Showroom Main Road",
      )
    withTimeout(DEFAULT_TIMEOUT_MS) { repo.saveAgencyProfile(profile).getOrThrow() }

    val observedProfile =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeAgencyProfile(uid).first { it.agencyName == "Ms Husaini Enterprises" }
      }
    assertEquals("7668732106, 7906599049", observedProfile.contactNumbers)

    val slip =
      KistSlip(
        slipNumber = "KIST-101",
        customerName = "Imran",
        vehicleNo = "UP21ER4091",
        vehicleModel = "Yatri 21",
        kistNumber = "2 / 18",
        amountPaid = 6500L,
        paymentDate = "12/11/2026",
        paymentMethod = "Cash",
      )
    val slipId = withTimeout(DEFAULT_TIMEOUT_MS) { repo.saveKistSlip(slip).getOrThrow() }
    val slips =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeKistSlips(uid).first { list -> list.any { it.id == slipId } }
      }
    assertTrue(slips.any { it.id == slipId && it.amountPaid == 6500L })
  }

  @Test
  fun observeCustomers_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(OWNER_EMAIL)
    auth.signOut()
    val repo = AgencyRepository(firestore, auth)

    var thrown: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeCustomers(aliceUid).first() }
    } catch (e: Exception) {
      thrown = e
    }
    assertNotNull("Expected PERMISSION_DENIED exception for unauthenticated read", thrown)
    val firestoreException = checkNotNull(thrown!!.unwrapFirestoreException()) {
      "Expected nested FirebaseFirestoreException, got $thrown"
    }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
  }

  @Test
  fun crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(OWNER_EMAIL)
    val aliceRepo = AgencyRepository(firestore, auth)
    val customerId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        aliceRepo.saveCustomer(CustomerRecord(billNumber = "HE-999", customerName = "Alice Customer")).getOrThrow()
      }

    signInTestUser(OTHER_EMAIL)
    val bobRepo = AgencyRepository(firestore, auth)

    var thrown: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) { bobRepo.observeCustomers(aliceUid).first() }
    } catch (e: Exception) {
      thrown = e
    }
    assertNotNull("Expected PERMISSION_DENIED for cross-user read", thrown)
    val firestoreException = checkNotNull(thrown!!.unwrapFirestoreException()) {
      "Expected nested FirebaseFirestoreException, got $thrown"
    }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    assertTrue(customerId.isNotBlank())
  }

  private companion object {
    const val OWNER_EMAIL = "owner@husaini.test"
    const val OTHER_EMAIL = "other@husaini.test"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
