require("dotenv").config();

const express = require("express");
const cors = require("cors");
const admin = require("firebase-admin");

const app = express();
app.use(cors());
app.use(express.json());

function initFirebase() {
  if (admin.apps.length > 0) return;

  if (process.env.FIREBASE_SERVICE_ACCOUNT_JSON) {
    admin.initializeApp({
      credential: admin.credential.cert(JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON)),
    });
    return;
  }

  admin.initializeApp({
    credential: admin.credential.applicationDefault(),
  });
}

initFirebase();

const db = admin.firestore();

const VALID_TYPES = new Set(["chat", "ranking", "reward", "other"]);
const ACTIVE_PRESENCE_TTL_MS = 2 * 60 * 1000;
const SERVER_STARTED_AT = Date.now();
let notificationWatcherStarted = false;

function stringValue(value, fallback = "") {
  if (value === undefined || value === null) return fallback;
  return String(value);
}

function timestampToMillis(value) {
  if (!value) return 0;
  if (typeof value.toMillis === "function") return value.toMillis();
  if (typeof value === "number") return value;
  return 0;
}

function isUserActive(userSnapshot) {
  const presence = userSnapshot.get("notificationPresence") || {};
  const active = presence.active === true;
  const updatedAt = timestampToMillis(presence.updatedAt);
  const fresh = updatedAt > 0 && Date.now() - updatedAt < ACTIVE_PRESENCE_TTL_MS;
  return active && fresh;
}

function notificationIntentData(notificationId, notification) {
  return {
    notificationId,
    type: stringValue(notification.type, "other"),
    title: stringValue(notification.title, "Slagalica"),
    message: stringValue(notification.message),
    actionType: stringValue(notification.actionType, "NONE"),
    actionData: JSON.stringify(notification.actionData || {}),
    stored: "true",
  };
}

async function sendStoredNotificationPush(uid, notificationId, notification) {
  if (!uid || !notificationId || !notification) return false;

  const notificationRef = db
    .collection("users")
    .doc(uid)
    .collection("notifications")
    .doc(notificationId);

  const claimed = await db.runTransaction(async transaction => {
    const latest = await transaction.get(notificationRef);
    if (!latest.exists) return false;

    const latestData = latest.data() || {};
    if (latestData.pushSentAt || latestData.pushProcessingAt) return false;

    transaction.set(notificationRef, {
      pushProcessingAt: admin.firestore.FieldValue.serverTimestamp(),
      pushSkippedReason: admin.firestore.FieldValue.delete(),
    }, { merge: true });
    return true;
  });

  if (!claimed) return false;

  const userSnapshot = await db.collection("users").doc(uid).get();
  if (!userSnapshot.exists) {
    await notificationRef.set({
      pushSkippedReason: "missing_user",
      pushCheckedAt: admin.firestore.FieldValue.serverTimestamp(),
      pushProcessingAt: admin.firestore.FieldValue.delete(),
    }, { merge: true });
    return false;
  }

  if (isUserActive(userSnapshot)) {
    await notificationRef.set({
      pushSkippedReason: "user_active",
      pushCheckedAt: admin.firestore.FieldValue.serverTimestamp(),
      pushProcessingAt: admin.firestore.FieldValue.delete(),
    }, { merge: true });
    return false;
  }

  const fcmToken = userSnapshot.get("fcmToken");
  if (!fcmToken) {
    await notificationRef.set({
      pushSkippedReason: "missing_fcm_token",
      pushCheckedAt: admin.firestore.FieldValue.serverTimestamp(),
      pushProcessingAt: admin.firestore.FieldValue.delete(),
    }, { merge: true });
    return false;
  }

  try {
    await admin.messaging().send({
      token: fcmToken,
      data: notificationIntentData(notificationId, notification),
      android: {
        priority: "high",
      },
    });

    await notificationRef.set({
      pushSentAt: admin.firestore.FieldValue.serverTimestamp(),
      pushProcessingAt: admin.firestore.FieldValue.delete(),
      pushSkippedReason: admin.firestore.FieldValue.delete(),
    }, { merge: true });
    return true;
  } catch (error) {
    console.error(`Failed to send FCM to ${uid}`, error);
    await notificationRef.set({
      pushSkippedReason: error.code || "fcm_error",
      pushCheckedAt: admin.firestore.FieldValue.serverTimestamp(),
      pushProcessingAt: admin.firestore.FieldValue.delete(),
    }, { merge: true });

    if (error.code === "messaging/registration-token-not-registered"
      || error.code === "messaging/invalid-registration-token") {
      await db.collection("users").doc(uid).update({
        fcmToken: admin.firestore.FieldValue.delete(),
      });
    }
    return false;
  }
}

function startNotificationWatcher() {
  if (notificationWatcherStarted) return;
  notificationWatcherStarted = true;

  db.collectionGroup("notifications").onSnapshot(snapshot => {
    snapshot.docChanges().forEach(change => {
      if (change.type !== "added") return;
      const notification = change.doc.data();
      const createdAt = Number(notification.createdAt || 0);
      if (createdAt > 0 && createdAt < SERVER_STARTED_AT) return;

      const notificationRef = change.doc.ref;
      const userRef = notificationRef.parent.parent;
      if (!userRef) return;

      sendStoredNotificationPush(userRef.id, change.doc.id, notification)
        .catch(error => console.error("Notification watcher error", error));
    });
  }, error => {
    notificationWatcherStarted = false;
    console.error("Notification watcher stopped", error);
  });
}

app.get("/health", (req, res) => {
  res.json({ ok: true });
});

app.post("/sendNotification", async (req, res) => {
  try {
    const {
      uid,
      type = "other",
      title,
      message,
      actionType = "NONE",
      actionData = {},
    } = req.body;

    if (!uid || !title || !message) {
      return res.status(400).json({
        error: "uid, title and message are required",
      });
    }

    const notificationType = VALID_TYPES.has(type) ? type : "other";
    const notification = {
      userId: uid,
      type: notificationType,
      title,
      message,
      actionType,
      actionData,
      read: false,
      createdAt: Date.now(),
    };

    const notificationRef = await db
      .collection("users")
      .doc(uid)
      .collection("notifications")
      .add(notification);

    const fcmSent = await sendStoredNotificationPush(uid, notificationRef.id, notification);

    res.json({
      ok: true,
      notificationId: notificationRef.id,
      fcmSent,
    });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: error.message });
  }
});

const port = process.env.PORT || 3000;
app.listen(port, () => {
  console.log(`Notification server listening on port ${port}`);
  startNotificationWatcher();
});
