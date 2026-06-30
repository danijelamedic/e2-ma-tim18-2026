# e2-ma-tim18-2026

# BrainBattle – Mobile Applications Project

BrainBattle is an Android mobile application inspired by the Serbian TV quiz **Slagalica**.

The application enables registered and guest users to compete in real-time multiplayer quiz matches, track their progress, climb leagues, communicate with other players, participate in tournaments and challenges, and monitor detailed game statistics through a modern mobile interface.

---

# Technologies

- Java
- Android Studio
- XML Layouts
- Android SDK
- Firebase Authentication
- Firebase Firestore
- Firebase Storage
- Firebase Cloud Messaging
- Material Design Components
- OpenStreetMap (osmdroid)
- ZXing QR Code Scanner
- QR Code Generator
- Android Sensors (Shake Sensor)

---

# Main Features

## Authentication

- User registration
- Email verification
- Login using email or username
- Password reset
- Logout
- Guest mode

---

## User Profile

Each registered user has a personalized profile containing:

- Username
- Email address
- Avatar
- Avatar border
- League
- Region
- Total stars
- Tokens
- QR code for adding friends

Users can:

- Change avatar
- View detailed statistics
- View league progress
- Logout

---

## Statistics

The application stores detailed statistics for every player.

### Overall statistics

- Total matches played
- Wins
- Losses
- Win percentage
- Overall success percentage

### Game statistics

#### Quiz

- Correct answers
- Wrong answers
- Success percentage

#### Matching

- Correct matches
- Wrong matches
- Success percentage

#### Associations

- Solved associations
- Unsolved associations

#### Skocko

- Success percentage for every attempt

#### Step By Step

- Success percentage for every clue

#### My Number

- Percentage of exact solutions

---

# Mini Games

Every match consists of six games played in the following order.

## 1. Quiz (Ko zna zna)

- 5 questions
- 4 possible answers
- 5 seconds per question
- Multiplayer synchronized
- Score calculation according to specification

---

## 2. Matching (Spojnice)

- Two rounds
- 30 seconds each
- Five pairs per round
- Real-time synchronization

---

## 3. Associations

- Four columns
- Hidden clues
- Column solutions
- Final solution
- Multiplayer gameplay

---

## 4. Skocko

- Guess the four-symbol combination
- Six attempts
- Multiplayer synchronized rounds

Symbols:

- Skocko
- Circle
- Square
- Heart
- Triangle
- Star

---

## 5. Step By Step

- Seven clues
- One clue every 10 seconds
- Earlier solutions award more points

---

## 6. My Number

- Random target number
- Random available numbers
- Arithmetic expression editor
- Automatic validation
- Shake sensor for stopping number generation

Supported operators:

- +
- -
- *
- /
- ()

---

# Multiplayer Match System

Players can play:

- Random matchmaking
- Friendly matches

Features include:

- Real-time synchronization
- Live score updates
- Turn management
- Automatic game progression
- Match results
- Token system
- Star rewards
- Friendly match invitations

Friendly matches:

- Do not consume tokens
- Do not affect statistics
- Do not affect stars

---

# Leagues

Players progress through leagues by collecting stars.

Features:

- Automatic promotion
- Automatic demotion
- League icons
- Daily token bonuses
- League notifications

---

# Regional System

The application includes an interactive map of Serbia.

Features:

- OpenStreetMap integration
- Player locations
- Monthly regional rankings
- Region statistics
- Regional avatar borders
- Region icons

Each region displays:

- Monthly stars
- First places
- Second places
- Third places
- Active players
- Registered players

---

# Friends

Players can:

- Search users
- Add friends
- Remove friends
- Scan QR codes
- Send game invitations
- Cancel invitations
- Accept or decline invitations
- View online status

Friend profiles display:

- Avatar
- Username
- League
- Total stars
- Monthly rank

---

# Leaderboards

Weekly and monthly leaderboards include:

- Player rankings
- League icons
- Stars earned during the current cycle
- Automatic refresh
- Reward distribution
- Ranking notifications

---

# Tournament

Tournament mode allows:

- Four-player brackets
- Semi-finals
- Finals
- Tournament rewards
- Tournament visualization
- Token entry fee

---

# Challenges

Players can create regional challenges.

Features:

- Up to four players
- Token betting
- Star betting
- Independent gameplay
- Automatic reward distribution

---

# Regional Chat

Players from the same region can communicate through:

- Real-time chat
- Sender information
- Message timestamps
- Push notifications

---

# Notifications

Notification center includes:

- Match invitations
- Friend requests
- Chat notifications
- League notifications
- Reward notifications
- Ranking notifications
- Read / unread status
- Notification filters
- Notification history

---

# Daily Missions

Daily missions include tasks such as:

- Win a match
- Send a chat message
- Play a friendly match
- Win a tournament game

Completing missions awards:

- Stars
- Tokens
- Bonus rewards

---

# Economy System

Players use:

## Tokens

Used for:

- Starting matches
- Entering tournaments

Obtained through:

- Registration
- Daily rewards
- League bonuses
- Leaderboards
- Star milestones

## Stars

Used for:

- League progression
- Rankings
- Match rewards

---

# Firebase Integration

Firebase is used for:

- Authentication
- User management
- Multiplayer synchronization
- Game data
- Statistics
- Friends
- Regions
- Leaderboards
- Notifications
- Chat
- Challenges
- Tournament data

---

# Architecture

The project follows a three-layer architecture.

## Presentation Layer

- Activities
- Fragments
- XML layouts
- Adapters

## Business Logic Layer

- Managers
- Game engines
- Controllers
- Match logic

## Data Layer

- Firebase Firestore
- Firebase Authentication
- Firebase Storage
- Repository classes

---

# Application Flow

- Splash Screen
- Login / Registration
- Home Screen

Navigation includes:

- Profile
- Statistics
- Friends
- Regions
- Leaderboards
- Notifications
- Daily Missions

Gameplay:

- Random Match
- Friendly Match
- Tournament
- Challenge

Game sequence:

1. Quiz
2. Matching
3. Associations
4. Skocko
5. Step By Step
6. My Number

After the match:

- Results
- Updated statistics
- Updated stars
- League progression
- Rewards

---

# How to Run

1. Clone the repository

```bash
git clone <repository-link>
```

2. Open the project in Android Studio.

3. Wait for Gradle synchronization.

4. Configure Firebase using the provided `google-services.json`.

5. Build the project.

6. Run the application on an Android device or emulator.

---

# Authors

Developed as the final project for the course:

**Mobile Applications**

Faculty of Technical Sciences

University of Novi Sad

Academic Year 2025/2026