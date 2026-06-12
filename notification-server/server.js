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

    const userSnapshot = await db.collection("users").doc(uid).get();
    const fcmToken = userSnapshot.get("fcmToken");

    let fcmSent = false;
    if (fcmToken) {
      await admin.messaging().send({
        token: fcmToken,
        data: {
          notificationId: notificationRef.id,
          type: notification.type,
          title: notification.title,
          message: notification.message,
          actionType: notification.actionType,
          stored: "true",
        },
        android: {
          priority: "high",
        },
      });
      fcmSent = true;
    }

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
});
