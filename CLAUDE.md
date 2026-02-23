# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

VibeConnector Backend is a Spring Boot 3.5.x application using Java 17. This is the backend component of the VibeConnector system, built as a REST API service.

**Base Package:** `com.link.vibe`
**Build Tool:** Gradle with wrapper included

## Build and Development Commands

### Build
```bash
./gradlew build
```

### Run Application
```bash
./gradlew bootRun
```

### Run Tests
```bash
./gradlew test
```

### Run Single Test
```bash
./gradlew test --tests com.link.vibe.VibeApplicationTests
./gradlew test --tests "com.link.vibe.*SpecificTest"
```

### Clean Build
```bash
./gradlew clean build
```

## Architecture

### Technology Stack
- **Framework:** Spring Boot 3.5.11-SNAPSHOT
- **Java Version:** 17
- **Dependencies:**
  - Spring Web (REST API)
  - Lombok (boilerplate reduction)
  - Spring DevTools (development hot reload)
  - JUnit 5 (testing)

### Project Structure
```
src/main/java/com/link/vibe/    - Main application code
src/main/resources/             - Configuration files
src/test/java/com/link/vibe/    - Test code
```

### Entry Point
`VibeApplication.java` contains the main Spring Boot application class with `@SpringBootApplication` annotation.

### Configuration
Application configuration is in `src/main/resources/application.properties`. The application name is set to "Backend".

## Development Notes

- The project uses Gradle wrapper (`./gradlew`), so no local Gradle installation is required
- Lombok annotations are available for reducing boilerplate code
- DevTools is enabled for automatic restart during development
- The project uses Spring Boot snapshot versions from `repo.spring.io/snapshot`
