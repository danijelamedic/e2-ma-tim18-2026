# e2-ma-tim18-2026

# BrainBattle - Mobile Applications Project

BrainBattle is an Android mobile application inspired by the Serbian TV quiz **Slagalica**.

The application allows players to compete in multiple quiz mini-games, customize their profiles, track game statistics and receive notifications through a unified game interface.

---

## Technologies

* Java
* Android Studio
* XML Layouts
* Android SDK
* Firebase Authentication
* Firebase Firestore
* Firebase Storage
* Material Design Components
* QR Code Generator

---

## Implemented Features

### Authentication

* User registration
* Email verification
* Login using email or username
* Password reset
* Logout

---

### User Profile

Each registered user has a profile containing:

* Username
* Email address
* Avatar
* Region
* Number of tokens
* Total stars
* League name and icon
* QR code for adding friends

Additional features:

* Change avatar
* Logout

---

### Statistics

The application keeps track of player statistics.

#### Overall statistics

* Total games played
* Win percentage
* Loss percentage
* Overall success percentage

#### Quiz statistics

* Games played
* Correct answers
* Wrong answers
* Success percentage

#### Matching statistics

* Games played
* Correct matches
* Wrong matches
* Success percentage

#### Associations statistics

* Solved vs unsolved associations

#### Skocko statistics

* Success percentage by attempt

#### Step By Step statistics

* Success percentage by step

#### My Number statistics

* Percentage of exact solutions

---

## Games

A complete match consists of six mini-games played in sequence.

### 1. Quiz (Ko zna zna)

* 5 questions
* 4 possible answers
* 5 seconds per question
* Correct answer: +10 points
* Wrong answer: -5 points

---

### 2. Matching (Spojnice)

* Two rounds
* 30 seconds per round
* Match terms from left and right columns
* Each correct match gives 2 points

---

### 3. Associations

* Four columns with hidden clues
* Guess column solutions
* Guess the final solution

---

### 4. Skocko

* Guess a sequence of four symbols
* Maximum six attempts

Symbols:

* Skocko
* Circle
* Square
* Heart
* Triangle
* Star

---

### 5. Step By Step

* Up to seven clues
* New clue every 10 seconds
* Earlier guesses bring more points

---

### 6. My Number

* Randomly generated numbers
* Create an arithmetic expression to obtain the target number

Allowed operators:

* *
* *
* *
* /
* ()
* Shake sensor for stopping number generation

---

## Match System

Players use tokens to start matches.

* One token = one match
* New users receive 5 tokens
* Players receive additional tokens every day

A match can be started:

* Against a random player
* Against a friend

After a match:

* Winners gain stars
* Losers lose stars
* Stars contribute to league progress
* Every 50 stars awards one additional token

Friendly matches:

* Do not consume tokens
* Do not affect statistics
* Do not affect stars

---

## Notifications

Implemented notification system with:

* Notification history
* Read/unread status
* Filtering notifications
* Reward notifications
* Match invitation notifications

---

## Application Flow

* Splash Screen
* Login / Registration
* Home Screen
* Game Session
* Quiz
* Matching
* Associations
* Skocko
* Step By Step
* My Number
* Profile
* Statistics
* Notifications

---

## Project Architecture

The application follows a three-layer architecture:

* Presentation Layer
* Business Logic Layer
* Data Layer

Firebase is used for:

* Authentication
* User profiles
* Game data
* Statistics
* Notifications
* Multiplayer synchronization

---

## How to Run the Application

1. Clone the repository:

```bash
git clone <repository-link>
```

2. Open the project in Android Studio.

3. Wait for Gradle synchronization.

4. Connect an Android device or start an emulator.

5. Run the application using the green **Run** button.

---

## Authors

Project developed as part of the course:

**Mobile Applications**
Faculty of Technical Sciences, Novi Sad
Academic year 2025/2026
