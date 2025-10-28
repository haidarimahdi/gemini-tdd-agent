# Gemini TDD Agent

## Description
This project is a Java-based autonomous AI agent built for a university project (ITE Project, 5 ECTS). It demonstrates the Model Context Protocol (MCP) by using Google's Gemini API to automate the Test-Driven Development (TDD) workflow.

When given a path to a JUnit test file, the agent:

- Analyzes the test cases to understand the requirements.

- Determines the correct source file path (e.g., src/main/java/...).

- Writes the Java source code to satisfy the tests.

- Executes mvn clean test in a secure sandbox to compile and verify its own work.

- Iterates by analyzing build failures or test errors and fixing its code until all tests pass.

The agent operates within a secure code-sandbox directory and uses a resilient API client with exponential backoff to handle network errors.

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
