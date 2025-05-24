# MDental Platform Launcher

A comprehensive launcher for the MDental microservices platform. This launcher provides a terminal-based GUI for configuring and deploying all components of the platform.

## Features

- Terminal-based UI for easy configuration and management
- Docker network isolation for secure deployments
- Postgres database setup with automatic initialization
- Keycloak identity provider configuration
- Service discovery with Eureka
- Microservices deployment with proper dependencies
- Flyway migrations execution
- Log management and viewing
- Dynamic realm creation in Keycloak

## Prerequisites

- Linux environment
- Dialog package (`apt-get install dialog`)
- Docker and Docker Compose
- Java 17 or higher
- PostgreSQL client (for migrations)

## Getting Started

1. Clone the repository
2. Run the launcher:

```bash
./launch.sh