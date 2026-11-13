const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_owner_123";
const BOB_UID = "bob_other_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTimestamp() {
  return new Date(Date.now() - 2000);
}

function validCustomerPayload(uid) {
  const ts = pastTimestamp();
  return {
    userId: uid,
    billNumber: "HE-2026-001",
    customerName: "Ramesh Kumar",
    keepNameBlankOnBill: false,
    phoneNumber: "9876543210",
    aadharNumber: "123456789012",
    customerAddress: "Rampur Road",
    vehicleNo: "UP21ER4091",
    vehicleModel: "Original Metro",
    purchaseDate: "12/11/2026",
    keepDateBlankOnBill: false,
    totalPrice: 145000,
    keepPriceBlankOnBill: false,
    paymentMode: "finance",
    downPayment: 35000,
    financeAmount: 110000,
    totalInstallments: 18,
    installmentAmount: 7200,
    purchasedFrom: "Ms Husaini Enterprises",
    batteryGuarantee: "12 Months",
    batteryWarranty: "6 Months",
    otherWarranty: "Motor 12M, Controller 6M",
    notes: "",
    hasCustomerPhoto: false,
    hasVehiclePhoto: false,
    hasWarrantyPhoto: false,
    customerSignatureBase64: "",
    ownerSignatureBase64: "",
    createdAt: ts,
    updatedAt: ts,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write customers", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("customers").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("customers").doc("c1").set(validCustomerPayload(ALICE_UID))
  );
});

test("Authenticated owner: can create, read, list, update customer (including blank name/date/price)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const custRef = aliceDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_1");
  const ts = pastTimestamp();

  await assertSucceeds(custRef.set({
    userId: ALICE_UID,
    billNumber: "HE-2026-002",
    customerName: "",
    keepNameBlankOnBill: true,
    purchaseDate: "",
    keepDateBlankOnBill: true,
    totalPrice: 0,
    keepPriceBlankOnBill: true,
    paymentMode: "cash",
    createdAt: ts,
    updatedAt: ts,
  }));

  await assertSucceeds(custRef.get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("customers").get());
  await assertSucceeds(custRef.update({
    customerName: "Suresh",
    keepNameBlankOnBill: false,
    updatedAt: new Date(Date.now() - 500),
  }));
});

test("Cross-user isolation: Bob cannot read or write Alice's customers", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_1").set(validCustomerPayload(ALICE_UID))
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("customers").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_2").set(validCustomerPayload(BOB_UID))
  );
});

test("Shadow update & schema validation: rejects ghost fields and invalid paymentMode", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const custRef = aliceDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_bad");

  await assertFails(custRef.set({
    ...validCustomerPayload(ALICE_UID),
    ghostField: "hacked",
  }));

  await assertFails(custRef.set({
    ...validCustomerPayload(ALICE_UID),
    paymentMode: "crypto",
  }));
});

test("Agency profile, photo subcollection, and kist slip validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastTimestamp();

  const profileRef = aliceDb.collection("users").doc(ALICE_UID).collection("agency_profiles").doc("default");
  await assertSucceeds(profileRef.set({
    userId: ALICE_UID,
    agencyName: "Ms Husaini Enterprises",
    ownerNames: "Mohammad Anees, Mohammad Suhail",
    contactNumbers: "7668732106, 7906599049",
    agencyAddress: "Main Showroom",
    defaultOwnerSignatureBase64: "",
    createdAt: ts,
    updatedAt: ts,
  }));

  const custRef = aliceDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_photo");
  await assertSucceeds(custRef.set(validCustomerPayload(ALICE_UID)));

  const photoRef = custRef.collection("photos").doc("warranty_card");
  await assertSucceeds(photoRef.set({
    userId: ALICE_UID,
    customerId: "cust_photo",
    photoType: "warranty_card",
    imageBase64: "AAAA",
    createdAt: ts,
    updatedAt: ts,
  }));

  const slipRef = aliceDb.collection("users").doc(ALICE_UID).collection("kist_slips").doc("slip_1");
  await assertSucceeds(slipRef.set({
    userId: ALICE_UID,
    customerId: "cust_photo",
    slipNumber: "KIST-001",
    customerName: "Ramesh Kumar",
    keepNameBlank: false,
    amountPaid: 7200,
    keepAmountBlank: false,
    paymentDate: "12/11/2026",
    keepDateBlank: false,
    createdAt: ts,
    updatedAt: ts,
  }));
});
