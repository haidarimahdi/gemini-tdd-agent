# Gemini TDD Agent

## Description
A Java-based AI agent that autonomously writes and refactors code based on Test-Driven Development (TDD) principles.

This agent uses Google's Gemini model to read Java test files from a `code-sandbox` directory, write the corresponding 
source code, and then compile and run the tests using Maven to verify its work.

## Requirements
- Java 11+
- Maven
- A `code-sandbox` directory (see `code-sandbox/pom.xml`)
- Environment variable `GEMINI_API_KEY` set

## Compile
```bash
mvn clean compile
```

## Usage
```bash
export GEMINI_API_KEY=AIzaSyBww8YT7sQ9acjac5_b1EBPrObunne9Zeo
mvn exec:java -Dexec.args="<relative-test-path>"
```
