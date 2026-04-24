#!/bin/bash
set -e

echo "Compiling..."
mkdir -p out
javac -d out src/main/java/com/bajaj/QuizLeaderboard.java

echo "Running..."
java -cp out com.bajaj.QuizLeaderboard
