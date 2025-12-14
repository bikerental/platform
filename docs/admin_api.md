# Admin API Documentation

## Overview

The Admin API provides endpoints for managing hotels (creating new hotels and resetting passwords). These endpoints require admin authentication.

---

## Authentication

### Admin Credentials

The admin credentials are configured in `application.properties`:

```properties
admin.username=${ADMIN_USERNAME:admin}
admin.password-hash=${ADMIN_PASSWORD_HASH:$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy}
```

**Default credentials (development):**
- Username: `admin`
- Password: `admin123` (hashed with bcrypt)

⚠️ **Important:** Change the admin password hash in production by setting the environment variable:
```bash
# Generate a bcrypt hash for your password (cost factor 10)
# Then set:
export ADMIN_PASSWORD_HASH=your_bcrypt_hash_here
```

**Note:** The admin password is stored as a bcrypt hash, not plaintext. To generate a new hash, use a bcrypt password encoder with cost factor 10.

### Login as Admin

```http
POST /api/auth/login
Content-Type: application/json

{
  "hotelCode": "admin",
  "password": "admin123"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "hotelName": "System Administrator"
}
```

The returned `accessToken` contains `ROLE_ADMIN` and can be used to access `/api/admin/**` endpoints.

---

## Admin Endpoints

All admin endpoints require the `Authorization: Bearer <token>` header with an admin JWT.

### List All Hotels

```http
GET /api/admin/hotels
Authorization: Bearer <admin_token>
```

**Response:**
```json
[
  {
    "hotelId": 1,
    "hotelCode": "STEELHOUSE",
    "hotelName": "SteelHouse Copenhagen",
    "createdAt": "2025-12-11T00:55:53Z"
  }
]
```

### Get Hotel by ID

```http
GET /api/admin/hotels/{hotelId}
Authorization: Bearer <admin_token>
```

**Response (200):**
```json
{
  "hotelId": 1,
  "hotelCode": "STEELHOUSE",
  "hotelName": "SteelHouse Copenhagen",
  "createdAt": "2025-12-11T00:55:53Z"
}
```

**Response (404):**
```json
{
  "error": "HOTEL_NOT_FOUND",
  "message": "Hotel not found with ID: 999",
  "details": null,
  "timestamp": "2025-12-11T22:13:43.406193Z"
}
```

### Create New Hotel

```http
POST /api/admin/hotels
Authorization: Bearer <admin_token>
Content-Type: application/json

{
  "hotelCode": "NEWHOTEL",
  "hotelName": "My New Hotel",
  "password": "SecurePassword123"
}
```

**Validation:**
- `hotelCode`: Required, 3-50 characters, must be unique
- `hotelName`: Required, max 255 characters
- `password`: Required, minimum 8 characters

**Response (201):**
```json
{
  "hotelId": 2,
  "hotelCode": "NEWHOTEL",
  "hotelName": "My New Hotel",
  "createdAt": "2025-12-11T22:13:42.918695Z"
}
```

**Response (409 - Hotel code exists):**
```json
{
  "error": "HOTEL_CODE_EXISTS",
  "message": "Hotel code 'NEWHOTEL' already exists",
  "details": null,
  "timestamp": "2025-12-11T22:13:43.406193Z"
}
```

### Reset Hotel Password

```http
POST /api/admin/hotels/{hotelId}/reset-password
Authorization: Bearer <admin_token>
Content-Type: application/json

{
  "newPassword": "NewSecurePassword456"
}
```

**Validation:**
- `newPassword`: Required, minimum 8 characters

**Response (200):**
```json
{
  "message": "Password reset successfully",
  "hotelId": 2
}
```

**Response (404 - Hotel not found):**
```json
{
  "error": "HOTEL_NOT_FOUND",
  "message": "Hotel not found with ID: 999",
  "details": null,
  "timestamp": "2025-12-11T22:13:43.406193Z"
}
```

---

## Postman Usage Guide

### Setup Collection

1. Create a new Postman collection called "Bike Rental Admin"
2. Add a collection variable `baseUrl` = `http://localhost:8080`
3. Add a collection variable `adminToken` (leave empty for now)

### Request 1: Admin Login

- **Method:** POST
- **URL:** `{{baseUrl}}/api/auth/login`
- **Headers:** `Content-Type: application/json`
- **Body (raw JSON):**
```json
{
  "hotelCode": "admin",
  "password": "admin123"
}
```
- **Tests tab (to auto-save token):**
```javascript
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    pm.collectionVariables.set("adminToken", jsonData.accessToken);
}
```

### Request 2: List Hotels

- **Method:** GET
- **URL:** `{{baseUrl}}/api/admin/hotels`
- **Headers:** 
  - `Authorization: Bearer {{adminToken}}`

### Request 3: Create Hotel

- **Method:** POST
- **URL:** `{{baseUrl}}/api/admin/hotels`
- **Headers:** 
  - `Authorization: Bearer {{adminToken}}`
  - `Content-Type: application/json`
- **Body (raw JSON):**
```json
{
  "hotelCode": "MYHOTEL",
  "hotelName": "My Hotel Name",
  "password": "SecurePassword123"
}
```

### Request 4: Reset Password

- **Method:** POST
- **URL:** `{{baseUrl}}/api/admin/hotels/1/reset-password`
- **Headers:** 
  - `Authorization: Bearer {{adminToken}}`
  - `Content-Type: application/json`
- **Body (raw JSON):**
```json
{
  "newPassword": "NewSecurePassword456"
}
```

---

## cURL Examples

### Complete Workflow

```bash
# Step 1: Login as admin and save token
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"hotelCode": "admin", "password": "admin123"}' \
  | python3 -c "import sys, json; print(json.load(sys.stdin)['accessToken'])")

echo "Admin token: $ADMIN_TOKEN"

# Step 2: List existing hotels
curl -s -X GET http://localhost:8080/api/admin/hotels \
  -H "Authorization: Bearer $ADMIN_TOKEN" | python3 -m json.tool

# Step 3: Create a new hotel
curl -s -X POST http://localhost:8080/api/admin/hotels \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "hotelCode": "NEWHOTEL",
    "hotelName": "New Hotel",
    "password": "HotelPassword123"
  }' | python3 -m json.tool

# Step 4: Reset a hotel password
curl -s -X POST http://localhost:8080/api/admin/hotels/1/reset-password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"newPassword": "NewPassword456"}' | python3 -m json.tool

# Step 5: Verify the new hotel can login
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"hotelCode": "NEWHOTEL", "password": "HotelPassword123"}' | python3 -m json.tool
```

---

## Security Considerations

1. **Change default admin password** in production via environment variables
2. **Use HTTPS** in production - never transmit credentials over plain HTTP
3. **Protect admin credentials** - store them in a secret management system
4. **Audit admin actions** - consider adding logging for admin operations
5. **Token expiration** - Admin tokens expire after 10 hours (configurable)

---

## Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `INVALID_CREDENTIALS` | 401 | Wrong username or password |
| `HOTEL_NOT_FOUND` | 404 | Hotel ID does not exist |
| `HOTEL_CODE_EXISTS` | 409 | Hotel code already in use |
| `VALIDATION_ERROR` | 400 | Request validation failed |
| `Forbidden` | 403 | Missing admin role (not logged in as admin) |
