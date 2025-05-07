# OpenAI Chat Function Call (Java)

## Description
Calls the OpenAI Chat Completion API (GPT-4 or GPT-3.5) with function calling enabled.

## Requirements
- Java 11+
- Maven
- Environment variable `OPENAI_API_KEY` set

## Compile
```bash
mvn clean compile
```

## Usage
```bash
export OPENAI_API_KEY=sk-...
mvn exec:java
```
