package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

object AgencyDefaults {
  const val DEFAULT_AGENCY_NAME = "Ms Husaini Enterprises"
  const val DEFAULT_OWNER_NAMES = "Mohammad Anees, Mohammad Suhail"
  const val DEFAULT_CONTACT_NUMBERS = "7668732106, 7906599049"
  const val DEFAULT_ADDRESS = "E-Rickshaw Sales, Finance & Service Center"

  val E_RICKSHAW_MODELS =
    listOf(
      "Original Metro",
      "Gold Star",
      "Shivsakti",
      "Yatri 21",
      "Mini Metro",
      "Panther",
      "Pilot",
    )

  val PAYMENT_METHODS = listOf("Cash", "UPI", "Bank Transfer")
}

object PhotoTypes {
  const val CUSTOMER = "customer"
  const val VEHICLE = "vehicle"
  const val WARRANTY_CARD = "warranty_card"
}

@IgnoreExtraProperties
data class AgencyProfile(
  @DocumentId val id: String = "default",
  val userId: String = "",
  val agencyName: String = AgencyDefaults.DEFAULT_AGENCY_NAME,
  val ownerNames: String = AgencyDefaults.DEFAULT_OWNER_NAMES,
  val contactNumbers: String = AgencyDefaults.DEFAULT_CONTACT_NUMBERS,
  val agencyAddress: String = AgencyDefaults.DEFAULT_ADDRESS,
  val defaultOwnerSignatureBase64: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class CustomerRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val billNumber: String = "",
  val customerName: String = "",
  val keepNameBlankOnBill: Boolean = false,
  val phoneNumber: String = "",
  val aadharNumber: String = "",
  val customerAddress: String = "",
  val vehicleNo: String = "",
  val vehicleModel: String = AgencyDefaults.E_RICKSHAW_MODELS.first(),
  val purchaseDate: String = "",
  val keepDateBlankOnBill: Boolean = false,
  val totalPrice: Long = 0L,
  val keepPriceBlankOnBill: Boolean = false,
  val paymentMode: String = "cash", // "cash" or "finance"
  val downPayment: Long = 0L,
  val financeAmount: Long = 0L,
  val totalInstallments: Long = 0L,
  val installmentAmount: Long = 0L,
  val purchasedFrom: String = AgencyDefaults.DEFAULT_AGENCY_NAME,
  val batteryGuarantee: String = "",
  val batteryWarranty: String = "",
  val otherWarranty: String = "",
  val notes: String = "",
  val hasCustomerPhoto: Boolean = false,
  val hasVehiclePhoto: Boolean = false,
  val hasWarrantyPhoto: Boolean = false,
  val customerSignatureBase64: String = "",
  val ownerSignatureBase64: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class CustomerPhoto(
  @DocumentId val id: String = "",
  val userId: String = "",
  val customerId: String = "",
  val photoType: String = "",
  val imageBase64: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class KistSlip(
  @DocumentId val id: String = "",
  val userId: String = "",
  val customerId: String = "",
  val slipNumber: String = "",
  val customerName: String = "",
  val keepNameBlank: Boolean = false,
  val phoneNumber: String = "",
  val vehicleNo: String = "",
  val vehicleModel: String = "",
  val kistNumber: String = "",
  val amountPaid: Long = 0L,
  val keepAmountBlank: Boolean = false,
  val paymentDate: String = "",
  val keepDateBlank: Boolean = false,
  val paymentMethod: String = "Cash",
  val nextDueDate: String = "",
  val remarks: String = "",
  val customerSignatureBase64: String = "",
  val ownerSignatureBase64: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)
