# Slagalica Notification Server

Minimal Node.js server for sending Firebase Cloud Messaging push notifications.

The server also listens to new Firestore notification documents:

```text
users/{uid}/notifications/{notificationId}
```

When the user is not currently active in the Android app, the server sends an FCM
data push to the saved `users/{uid}.fcmToken`. This covers notifications created
directly by the Android app through `NotificationRepository.create(...)`.

## Setup

1. Install dependencies:

```bash
npm install
```

2. Add Firebase Admin credentials.

Use one of these options:

```bash
set GOOGLE_APPLICATION_CREDENTIALS=C:\path\to\service-account.json
```

or put the full service account JSON in `FIREBASE_SERVICE_ACCOUNT_JSON`.

3. Start the server:

```bash
npm start
```

## Send a notification

```bash
curl -X POST http://localhost:3000/sendNotification ^
  -H "Content-Type: application/json" ^
  -d "{\"uid\":\"USER_UID\",\"type\":\"reward\",\"title\":\"Reward\",\"message\":\"You earned 3 tokens\",\"actionType\":\"OPEN_REWARDS\"}"
```

The server writes the notification to:

```text
users/{uid}/notifications/{notificationId}
```

Then it reads:

```text
users/{uid}.fcmToken
```

and sends an FCM push if the token exists.

## App presence

The Android app writes:

```text
users/{uid}.notificationPresence.active
users/{uid}.notificationPresence.updatedAt
```

The server uses this to avoid duplicate push popups while the user is actively
using the app. If the app is closed or the presence timestamp is stale, FCM is
sent.
