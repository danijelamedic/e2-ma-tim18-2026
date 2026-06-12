# Slagalica Notification Server

Minimal Node.js server for sending Firebase Cloud Messaging push notifications.

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
