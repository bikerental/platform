# BikeRental Platform

A full-stack bike rental management system for hotels, built with Spring Boot and React.

## Tech Stack

### Backend

- Java 21
- Spring Boot 3.4 (Web, Security JWT, Data JPA)
- MySQL
- Maven
- Apache POI (Excel Export)


### Frontend

- React 19 + TypeScript
- Vite
- Tailwind CSS 4.1
- React Router 7

## Prerequisites

- Java 21 or higher
- Node.js 20.x
- MySQL 8.0+
- Maven (wrapper included)

## Getting Started

### 1. Clone the Repository

```bash
git clone git@github.com:bikerental/platform.git
cd platform
```

### 2. Database Setup

Create the MySQL database:

```sql
CREATE DATABASE bikerental_platform;
```

Configure your database credentials in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/bikerental_platform
spring.datasource.username=root
spring.datasource.password=yourpassword
```

**For production deployments**, use environment variables:

```bash
export SPRING_DATASOURCE_URL=jdbc:mysql://your-db-host:3306/bikerental_platform
export SPRING_DATASOURCE_USERNAME=your-db-user
export SPRING_DATASOURCE_PASSWORD=your-secure-password
export JWT_SECRET=your-production-jwt-secret-256-bits-minimum
export ADMIN_USERNAME=your-admin-username
export ADMIN_PASSWORD_HASH=your-bcrypt-hashed-password
```

### 3. Install Dependencies

```bash
./mvnw clean install
cd frontend && npm install
```

### 4. Run the Application

Use the Makefile for common commands:

```bash
make run            # Start backend (port 8080)
make run-frontend   # Start frontend (port 5173)
make test           # Run backend tests
make build          # Build production JAR
```

Or use Docker for the full stack:

```bash
make docker-up      # Start all services (MySQL + backend + frontend)
make docker-down    # Stop all services
make docker-logs    # View logs
```

The application will be available at:
- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8080/api`

## Default Credentials (Development Only)

**Default admin credentials:**
- Username: `admin`
- Password: `admin123`

**Production:** Set secure credentials using environment variables `ADMIN_USERNAME` and `ADMIN_PASSWORD_HASH` (BCrypt hash).

## Project Structure

```text
platform/
├── src/                    # Backend source code
│   └── main/
│       ├── java/          # Java source files
│       └── resources/     # Application properties
├── frontend/              # React frontend
│   ├── src/              # Frontend source code
│   └── .env              # Frontend environment variables
├── docs/                 # Documentation
└── pom.xml              # Maven configuration
```

## Development

| Command | Description |
|---------|-------------|
| `make run` | Start backend locally |
| `make run-frontend` | Start frontend dev server |
| `make test` | Run backend tests |
| `make lint` | Lint frontend code |
| `make docker-up` | Start full stack with Docker |
| `make docker-mysql` | Connect to MySQL shell |

- Backend runs on port 8080
- Frontend dev server runs on port 5173
- API base path: `/api`
- Frontend uses `VITE_API_BASE_URL` environment variable

## Deployment

The application is deployed to Azure using GitHub Actions CI/CD.

### Architecture

- **Backend**: Azure Web App (Docker container) - `app-bikerental-api`
- **Frontend**: Azure Web App (Docker container) - `app-bikerental-frontend`
- **Container Registry**: Azure Container Registry (ACR)
- **Database**: Azure Database for MySQL

### CI/CD Pipeline

Deployment is triggered manually via GitHub Actions workflow dispatch:

```bash
# From GitHub Actions UI, run the "Deploy to Azure" workflow
# Select which components to deploy (backend, frontend, or both)
```

The workflow:
1. Builds Docker images for backend and frontend
2. Pushes images to Azure Container Registry
3. Deploys to Azure Web Apps

### Local Build

```bash
make build                    # Build backend JAR
make docker-build-backend     # Build backend Docker image
make docker-build-frontend    # Build frontend Docker image
```

### Environment Variables

All sensitive configuration should use environment variables in production. See section 2 (Database Setup) for required variables.

## License

This project is private and proprietary.
