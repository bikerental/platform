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

**Backend:**
```bash
./mvnw clean install
```

**Frontend:**
```bash
cd frontend
npm install
```

### 4. Run the Application

**Backend** (from project root):
```bash
./mvnw spring-boot:run
```

**Frontend** (in separate terminal):
```bash
cd frontend
npm run dev
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

- Backend runs on port 8080
- Frontend dev server runs on port 5173
- API base path: `/api`
- Frontend uses `VITE_API_BASE_URL` environment variable

## Deployment

### Backend (Production)

1. Build the JAR:
```bash
./mvnw clean package -DskipTests
```

2. Run with production settings:
```bash
java -jar target/rental-service-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --server.port=8080
```

### Frontend (Production)

1. Update `frontend/.env` with production API URL:
```bash
VITE_API_BASE_URL=https://your-api-domain.com/api
```

2. Build for production:
```bash
cd frontend
npm run build
```

3. Deploy the `frontend/dist` directory to your web server (Nginx, Apache, etc.)

### Environment Variables

All sensitive configuration should use environment variables in production. See section 2 (Database Setup) for required variables.

## License

This project is private and proprietary.
