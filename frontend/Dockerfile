# Build stage
FROM node:20-alpine AS build
WORKDIR /app

# Build arg for API URL (defaults to local Docker backend)
ARG VITE_API_BASE_URL=http://localhost:8080/api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

# Copy package files first (better layer caching)
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy source and build
COPY . .
RUN npm run build

# Runtime stage
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Remove default nginx static content
RUN rm -rf ./*

# Copy built assets from build stage
COPY --from=build /app/dist .

# Copy nginx config for SPA routing
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
