.PHONY: run test build clean docker-up docker-down docker-build docker-logs docker-logs-backend docker-logs-frontend docker-mysql lint

# === Local Development ===

# Run backend locally (requires local MySQL)
run:
	./mvnw spring-boot:run

# Run frontend locally
run-frontend:
	cd frontend && npm run dev

# Run tests
test:
	./mvnw test

# Build the project
build:
	./mvnw clean package -DskipTests

# Clean build artifacts
clean:
	./mvnw clean
	rm -rf frontend/dist frontend/node_modules/.vite

# === Docker Development ===

# Start all containers (build if needed)
docker-up:
	docker compose up --build

# Start containers in background
docker-up-d:
	docker compose up --build -d

# Stop all containers
docker-down:
	docker compose down

# Stop containers and remove volumes (fresh start)
docker-clean:
	docker compose down -v

# Rebuild specific service
docker-build-backend:
	docker compose build backend

docker-build-frontend:
	docker compose build frontend

# === Docker Logs ===

docker-logs:
	docker compose logs -f

docker-logs-backend:
	docker compose logs -f backend

docker-logs-frontend:
	docker compose logs -f frontend

docker-logs-mysql:
	docker compose logs -f mysql

# === Database ===

# Connect to MySQL in Docker
docker-mysql:
	docker exec -it platform-mysql-1 mysql -ubikerental -pbikerental123 bikerental

# === Linting ===

lint:
	cd frontend && npm run lint

lint-fix:
	cd frontend && npm run lint -- --fix
