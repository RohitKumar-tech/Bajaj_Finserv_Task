# Quiz Leaderboard System — Bajaj Finserv Health Internship

## Problem

Build a backend application that:
1. Polls an external quiz API 10 times (poll 0–9)
2. Deduplicates events using `(roundId + participant)` as unique key
3. Aggregates scores per participant
4. Generates a leaderboard sorted by total score (descending)
5. Submits the leaderboard exactly once

## Key Challenge

The API deliberately returns duplicate events across polls. Processing duplicates causes an incorrect total score. The solution uses a `HashSet` of `roundId|participant` keys to ignore already-seen events.

## Tech Stack

- Java 11+
- Maven
- Gson (JSON parsing)
- `java.net.http.HttpClient` (built-in HTTP client)

## Setup & Run

### Prerequisites
- Java 11 or higher
- Maven 3.6+

### Configuration

Open `src/main/java/com/bajaj/QuizLeaderboard.java` and set your registration number:

```java
private static final String REG_NO = "YOUR_REG_NO_HERE";
```

### Build

```bash
mvn clean package -q
```

### Run

```bash
java -jar target/quiz-leaderboard.jar
```

The program will:
1. Poll `/quiz/messages` 10 times with a **5-second delay** between each poll
2. Print new vs. duplicate events per poll
3. Display the final leaderboard
4. Submit to `/quiz/submit` and print the validator response

## Expected Output

```
=== Quiz Leaderboard System ===
RegNo: YOUR_REG_NO

[Poll 0] Fetching...
[Poll 0] HTTP 200
[Poll 0] New events: 5, Duplicates ignored: 0
Waiting 5s...
...
=== Leaderboard ===
Alice: 150
Bob: 120
Charlie: 90
Total Score: 360

=== Submitting ===
Submit HTTP 200
Response: {"isCorrect":true,"isIdempotent":true,...}
```

## API Reference

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/quiz/messages?regNo=X&poll=N` | GET | Fetch events for poll N (0–9) |
| `/quiz/submit` | POST | Submit final leaderboard |

Base URL: `https://devapigw.vidalhealthtpa.com/srm-quiz-task`

## Deduplication Logic

```
key = roundId + "|" + participant
if key not in seen → process event, add to seen
if key in seen     → skip (duplicate)
```
