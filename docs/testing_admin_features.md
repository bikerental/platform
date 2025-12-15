# Testing Admin Features Guide

This guide explains how to test the admin features of the rental service application.

## Overview

The admin features allow system administrators to:
1. **List all hotels** - View all registered hotels in the system
2. **Create new hotels** - Register new hotels with credentials
3. **Reset hotel passwords** - Reset passwords for existing hotels

All admin endpoints require authentication with `ROLE_ADMIN` authority.

---

## Prerequisites

1. **Start the application:**
   ```bash
   ./mvnw spring-boot:run
   ```
   The application will start on `http://localhost:8080`

2. **Default admin credentials** (from `application.properties`):
   - Username: `admin`
   - Password: `ChangeThisAdminPwd123!`

---

## Method 1: Automated Tests

### Run All Tests
```bash
./mvnw test
```

### Run Only Admin Tests
```bash
# Unit tests
./mvnw test -Dtest=AdminServiceTest
./mvnw test -Dtest=AdminControllerTest

# Integration tests (full flow with database)
./mvnw test -Dtest=AdminIntegrationTest
```

### Test Coverage

**Unit Tests:**
- ✅ `AdminServiceTest` - Tests business logic with mocked dependencies
- ✅ `AdminControllerTest` - Tests controller endpoints with mocked services

**Integration Tests:**
- ✅ `AdminIntegrationTest` - Tests full workflow with real database

---

## Method 2: Manual Testing with cURL

### Step 1: Login as Admin

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "hotelCode": "admin",
    "password": "ChangeThisAdminPwd123!"
  }'
```

**Expected Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "hotelName": "System Administrator"
}
```

**Save the token:**
```bash
ADMIN_TOKEN="<paste_token_here>"
```

### Step 2: List All Hotels

```bash
curl -X GET http://localhost:8080/api/admin/hotels \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Expected Response:**
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

### Step 3: Create a New Hotel

```bash
curl -X POST http://localhost:8080/api/admin/hotels \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "hotelCode": "NEWHOTEL",
    "hotelName": "My New Hotel",
    "password": "SecurePassword123"
  }'
```

**Expected Response (201 Created):**
```json
{
  "hotelId": 2,
  "hotelCode": "NEWHOTEL",
  "hotelName": "My New Hotel",
  "createdAt": "2025-12-11T22:13:42.918695Z"
}
```

**Validation Rules:**
- `hotelCode`: Required, 3-50 characters, must be unique
- `hotelName`: Required, max 255 characters
- `password`: Required, minimum 8 characters

### Step 4: Reset Hotel Password

```bash
curl -X POST http://localhost:8080/api/admin/hotels/2/reset-password \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "newPassword": "NewSecurePassword456"
  }'
```

**Expected Response (200 OK):**
```json
{
  "message": "Password reset successfully",
  "hotelId": 2
}
```

### Step 5: Verify Password Reset

Test that the hotel can login with the new password:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "hotelCode": "NEWHOTEL",
    "password": "NewSecurePassword456"
  }'
```

**Expected Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "hotelName": "My New Hotel"
}
```

---

## Method 3: Testing Error Cases

### Test 1: Duplicate Hotel Code (409 Conflict)

```bash
# Try to create a hotel with an existing code
curl -X POST http://localhost:8080/api/admin/hotels \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "hotelCode": "NEWHOTEL",
    "hotelName": "Duplicate Hotel",
    "password": "Password123"
  }'
```

**Expected Response (409 Conflict):**
```json
{
  "error": "CONFLICT",
  "message": "Hotel code 'NEWHOTEL' already exists",
  "details": null,
  "timestamp": "2025-12-11T22:13:43.406193Z"
}
```

### Test 2: Invalid Hotel Data (400 Bad Request)

```bash
# Test with empty hotel code
curl -X POST http://localhost:8080/api/admin/hotels \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "hotelCode": "",
    "hotelName": "Test Hotel",
    "password": "Password123"
  }'
```

**Expected Response (400 Bad Request):**
```json
{
  "error": "VALIDATION_ERROR",
  "message": "Validation failed",
  "details": {
    "hotelCode": "Hotel code is required"
  },
  "timestamp": "2025-12-11T22:13:43.406193Z"
}
```

### Test 3: Hotel Not Found (404 Not Found)

```bash
# Try to reset password for non-existent hotel
curl -X POST http://localhost:8080/api/admin/hotels/999/reset-password \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "newPassword": "NewPassword456"
  }'
```

**Expected Response (404 Not Found):**
```json
{
  "error": "NOT_FOUND",
  "message": "Hotel not found with ID: 999",
  "details": null,
  "timestamp": "2025-12-11T22:13:43.406193Z"
}
```

### Test 4: Unauthorized Access (401 Unauthorized)

```bash
# Try to access without token
curl -X GET http://localhost:8080/api/admin/hotels
```

**Expected Response (401 Unauthorized):**
```json
{
  "timestamp": "2025-12-11T22:13:43.406193Z",
  "status": 401,
  "error": "Unauthorized",
  "path": "/api/admin/hotels"
}
```

### Test 5: Forbidden Access (403 Forbidden)

```bash
# Login as regular hotel (not admin)
HOTEL_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"hotelCode": "NEWHOTEL", "password": "NewSecurePassword456"}' \
  | python3 -c "import sys, json; print(json.load(sys.stdin)['accessToken'])")

# Try to access admin endpoint with hotel token
curl -X GET http://localhost:8080/api/admin/hotels \
  -H "Authorization: Bearer $HOTEL_TOKEN"
```

**Expected Response (403 Forbidden):**
```json
{
  "timestamp": "2025-12-11T22:13:43.406193Z",
  "status": 403,
  "error": "Forbidden",
  "path": "/api/admin/hotels"
}
```

---

## Method 4: Using Postman

See the detailed Postman guide in `docs/admin_api.md` for:
- Setting up a Postman collection
- Auto-saving admin token
- Testing all endpoints with examples

---

## Test Checklist

Use this checklist to ensure all admin features work correctly:

### Authentication
- [ ] Admin can login with correct credentials
- [ ] Admin receives JWT token with `ROLE_ADMIN`
- [ ] Invalid admin credentials return 401

### List Hotels
- [ ] Admin can list all hotels
- [ ] Returns empty array when no hotels exist
- [ ] Returns all hotels with correct data structure
- [ ] Requires admin authentication (401 without token)
- [ ] Rejects hotel tokens (403 with hotel token)

### Create Hotel
- [ ] Admin can create new hotel with valid data
- [ ] Hotel is saved to database with hashed password
- [ ] Returns 201 with hotel details
- [ ] Rejects duplicate hotel codes (409)
- [ ] Validates hotel code (3-50 chars, required)
- [ ] Validates hotel name (required, max 255 chars)
- [ ] Validates password (min 8 chars, required)
- [ ] Returns 400 for invalid data
- [ ] Requires admin authentication

### Reset Password
- [ ] Admin can reset hotel password
- [ ] Password is actually changed in database
- [ ] Hotel can login with new password
- [ ] Returns 200 with success message
- [ ] Returns 404 for non-existent hotel
- [ ] Validates new password (min 8 chars, required)
- [ ] Returns 400 for invalid password
- [ ] Requires admin authentication

---

## Troubleshooting

### Issue: "401 Unauthorized"
- **Solution:** Make sure you're using a valid admin token. Login again and get a fresh token.

### Issue: "403 Forbidden"
- **Solution:** You're using a hotel token instead of an admin token. Login as admin using `hotelCode: "admin"`.

### Issue: "409 Conflict" when creating hotel
- **Solution:** The hotel code already exists. Use a different hotel code or delete the existing hotel first.

### Issue: "404 Not Found" when resetting password
- **Solution:** The hotel ID doesn't exist. List hotels first to get the correct hotel ID.

### Issue: Tests fail with database errors
- **Solution:** Make sure your test database is properly configured. Integration tests use H2 in-memory database by default.

---

## Next Steps

After testing the admin features:

1. **Review test results:** Check that all tests pass
2. **Test edge cases:** Try unusual inputs and verify error handling
3. **Security testing:** Verify that non-admin users cannot access admin endpoints
4. **Performance testing:** Test with multiple hotels and concurrent requests
5. **Documentation:** Update API documentation if needed

For more details, see:
- `docs/admin_api.md` - Complete API documentation
- `src/test/java/com/bikerental/platform/rental/admin/` - Test source code
