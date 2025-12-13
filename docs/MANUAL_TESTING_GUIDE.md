# Manual Testing Guide - Admin Features

This guide will help you manually test the admin features by running both the backend and frontend, then hitting the endpoints yourself.

---

## Prerequisites

1. **MySQL Database** - Make sure MySQL is running and accessible
2. **Java 21** - Required for the backend
3. **Node.js & npm** - Required for the frontend
4. **Browser** - For testing (Chrome/Firefox recommended with DevTools)

---

## Step 1: Start the Backend

### Option A: Using Maven Wrapper (Recommended)

```bash
cd /path/to/rental-service
./mvnw spring-boot:run
```

### Option B: Using IDE

1. Open the project in your IDE (IntelliJ IDEA, Eclipse, etc.)
2. Run `RentalServiceApplication.java`
3. The backend will start on `http://localhost:8080`

### Verify Backend is Running

Open your browser and go to:
```
http://localhost:8080/api/auth/login
```

You should see an error (since it's a POST endpoint), but this confirms the server is running.

**Expected:** You'll see a 405 Method Not Allowed or similar error, which means the server is up!

---

## Step 2: Start the Frontend

Open a **new terminal window** (keep the backend running):

```bash
cd /path/to/rental-service/frontend
npm install  # Only needed first time or after dependency changes
npm run dev
```

The frontend will start on `http://localhost:5173` (or another port if 5173 is taken).

**Verify Frontend is Running:**
- Open `http://localhost:5173` in your browser
- You should see the login page

---

## Step 3: Test Admin Features Manually

You have several options for testing:

### Method 1: Browser Developer Tools (Easiest)

This is the easiest way to test if you're comfortable with browser DevTools.

#### Step 3.1: Login as Admin

1. Open your browser and go to `http://localhost:5173`
2. Open Developer Tools (F12 or Cmd+Option+I on Mac, Ctrl+Shift+I on Windows)
3. Go to the **Console** tab
4. Paste this code to login as admin:

```javascript
// Login as admin
fetch('http://localhost:8080/api/auth/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    hotelCode: 'admin',
    password: 'ChangeThisAdminPwd123!'
  })
})
.then(response => response.json())
.then(data => {
  console.log('Login successful!');
  console.log('Token:', data.accessToken);
  // Save token to window for later use
  window.adminToken = data.accessToken;
  console.log('Token saved to window.adminToken');
})
.catch(error => console.error('Error:', error));
```

5. Press Enter. You should see:
   - "Login successful!"
   - The JWT token
   - Token saved message

#### Step 3.2: List All Hotels

In the same browser console, run:

```javascript
// List all hotels
fetch('http://localhost:8080/api/admin/hotels', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${window.adminToken}`,
    'Content-Type': 'application/json',
  }
})
.then(response => response.json())
.then(data => {
  console.log('Hotels:', data);
  console.table(data); // Nice table view
})
.catch(error => console.error('Error:', error));
```

**Expected Result:** You'll see an array of hotels (might be empty if no hotels exist yet).

#### Step 3.3: Create a New Hotel

```javascript
// Create a new hotel
fetch('http://localhost:8080/api/admin/hotels', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${window.adminToken}`,
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    hotelCode: 'TESTHOTEL',
    hotelName: 'Test Hotel',
    password: 'TestPassword123'
  })
})
.then(response => response.json())
.then(data => {
  console.log('Hotel created:', data);
  window.createdHotelId = data.hotelId; // Save for next step
})
.catch(error => {
  error.json().then(err => console.error('Error:', err));
});
```

**Expected Result:** You'll see the created hotel with an ID, code, name, and createdAt timestamp.

#### Step 3.4: List Hotels Again (Verify Creation)

```javascript
// List hotels again to see the new one
fetch('http://localhost:8080/api/admin/hotels', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${window.adminToken}`,
    'Content-Type': 'application/json',
  }
})
.then(response => response.json())
.then(data => {
  console.log('All hotels:', data);
  console.table(data);
})
.catch(error => console.error('Error:', error));
```

**Expected Result:** You should now see your newly created hotel in the list.

#### Step 3.5: Reset Hotel Password

```javascript
// Reset password for the hotel we just created
fetch(`http://localhost:8080/api/admin/hotels/${window.createdHotelId}/reset-password`, {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${window.adminToken}`,
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    newPassword: 'NewPassword456'
  })
})
.then(response => response.json())
.then(data => {
  console.log('Password reset successful:', data);
})
.catch(error => {
  error.json().then(err => console.error('Error:', err));
});
```

**Expected Result:** You'll see a success message with the hotel ID.

#### Step 3.6: Verify Password Reset (Test Login)

```javascript
// Try to login with the new password
fetch('http://localhost:8080/api/auth/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    hotelCode: 'TESTHOTEL',
    password: 'NewPassword456'
  })
})
.then(response => response.json())
.then(data => {
  console.log('Login with new password successful!', data);
})
.catch(error => {
  error.json().then(err => console.error('Error:', err));
});
```

**Expected Result:** You should be able to login successfully with the new password.

---

### Method 2: Using cURL (Command Line)

If you prefer command line, here are the commands:

#### Step 1: Login as Admin

```bash
# Login and save token
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"hotelCode": "admin", "password": "ChangeThisAdminPwd123!"}' \
  | python3 -c "import sys, json; print(json.load(sys.stdin)['accessToken'])")

echo "Admin token: $ADMIN_TOKEN"
```

#### Step 2: List Hotels

```bash
curl -X GET http://localhost:8080/api/admin/hotels \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" | python3 -m json.tool
```

#### Step 3: Create Hotel

```bash
curl -X POST http://localhost:8080/api/admin/hotels \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "hotelCode": "MYHOTEL",
    "hotelName": "My Test Hotel",
    "password": "SecurePassword123"
  }' | python3 -m json.tool
```

#### Step 4: Reset Password

```bash
# Replace 1 with the actual hotel ID from the create response
curl -X POST http://localhost:8080/api/admin/hotels/1/reset-password \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"newPassword": "NewPassword456"}' | python3 -m json.tool
```

---

### Method 3: Using Postman

1. **Import the Collection:**
   - Open Postman
   - Create a new collection called "Rental Service Admin"
   - Add collection variables:
     - `baseUrl` = `http://localhost:8080`
     - `adminToken` = (leave empty, will be set automatically)

2. **Create Requests:**

   **Request 1: Admin Login**
   - Method: `POST`
   - URL: `{{baseUrl}}/api/auth/login`
   - Body (raw JSON):
     ```json
     {
       "hotelCode": "admin",
       "password": "ChangeThisAdminPwd123!"
     }
     ```
   - Tests tab (to auto-save token):
     ```javascript
     if (pm.response.code === 200) {
         var jsonData = pm.response.json();
         pm.collectionVariables.set("adminToken", jsonData.accessToken);
     }
     ```

   **Request 2: List Hotels**
   - Method: `GET`
   - URL: `{{baseUrl}}/api/admin/hotels`
   - Headers:
     - `Authorization: Bearer {{adminToken}}`

   **Request 3: Create Hotel**
   - Method: `POST`
   - URL: `{{baseUrl}}/api/admin/hotels`
   - Headers:
     - `Authorization: Bearer {{adminToken}}`
     - `Content-Type: application/json`
   - Body (raw JSON):
     ```json
     {
       "hotelCode": "NEWHOTEL",
       "hotelName": "New Hotel",
       "password": "Password123"
     }
     ```

   **Request 4: Reset Password**
   - Method: `POST`
   - URL: `{{baseUrl}}/api/admin/hotels/1/reset-password`
   - Headers:
     - `Authorization: Bearer {{adminToken}}`
     - `Content-Type: application/json`
   - Body (raw JSON):
     ```json
     {
       "newPassword": "NewPassword456"
     }
     ```

---

## Step 4: Test Error Cases

### Test 1: Try to Access Admin Endpoints Without Token

In browser console:
```javascript
fetch('http://localhost:8080/api/admin/hotels', {
  method: 'GET',
  headers: {
    'Content-Type': 'application/json',
  }
})
.then(response => response.json())
.then(data => console.log('Response:', data))
.catch(error => console.error('Error:', error));
```

+**Expected:** 403 Forbidden error (Spring Security default for unauthenticated requests)

### Test 2: Try to Create Hotel with Duplicate Code

```javascript
fetch('http://localhost:8080/api/admin/hotels', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${window.adminToken}`,
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    hotelCode: 'TESTHOTEL', // Same code as before
    hotelName: 'Duplicate Hotel',
    password: 'Password123'
  })
})
.then(response => response.json())
.then(data => console.log('Response:', data))
.catch(error => {
  error.json().then(err => console.error('Error:', err));
});
```

**Expected:** 409 Conflict error with message about duplicate hotel code

### Test 3: Try to Create Hotel with Invalid Data

```javascript
fetch('http://localhost:8080/api/admin/hotels', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${window.adminToken}`,
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    hotelCode: '', // Empty - invalid
    hotelName: 'Test',
    password: 'short' // Too short - invalid
  })
})
.then(response => response.json())
.then(data => console.log('Response:', data))
.catch(error => {
  error.json().then(err => console.error('Error:', err));
});
```

**Expected:** 400 Bad Request with validation errors

### Test 4: Try to Reset Password for Non-Existent Hotel

```javascript
fetch('http://localhost:8080/api/admin/hotels/999/reset-password', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${window.adminToken}`,
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    newPassword: 'NewPassword456'
  })
})
.then(response => response.json())
.then(data => console.log('Response:', data))
.catch(error => {
  error.json().then(err => console.error('Error:', err));
});
```

**Expected:** 404 Not Found error

---

## Quick Reference: All Admin Endpoints

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| `/api/auth/login` | POST | Login as admin (use `hotelCode: "admin"`) | No |
| `/api/admin/hotels` | GET | List all hotels | Yes (Admin) |
| `/api/admin/hotels` | POST | Create new hotel | Yes (Admin) |
| `/api/admin/hotels/{id}/reset-password` | POST | Reset hotel password | Yes (Admin) |

---

## Troubleshooting

### Backend won't start
- **Check MySQL:** Make sure MySQL is running: `mysql -u root -p`
- **Check port 8080:** Make sure nothing else is using port 8080
- **Check logs:** Look at the console output for error messages

### Frontend won't start
- **Install dependencies:** Run `npm install` in the frontend directory
- **Check Node version:** Make sure you have Node.js installed: `node --version`
- **Check port 5173:** If 5173 is taken, Vite will use the next available port

### Can't login as admin
- **Check credentials:** Default is `admin` / `ChangeThisAdminPwd123!`
- **Check application.properties:** Verify admin credentials in `src/main/resources/application.properties`

### Getting 401 Unauthorized
- **Token expired:** Login again to get a new token
- **Wrong token:** Make sure you're using the admin token, not a hotel token
- **Token format:** Make sure the Authorization header is: `Bearer <token>`

### Getting 403 Forbidden
- **Not admin:** You're logged in as a hotel, not as admin. Login with `hotelCode: "admin"`

### CORS errors in browser
- **Check backend CORS config:** Make sure `SecurityConfig.java` allows your frontend origin
- **Check API URL:** Make sure frontend is calling `http://localhost:8080/api`

---

## Next Steps

After testing manually:

1. **Check the database:** Verify hotels are actually being created:
   ```sql
   mysql -u root -p
   USE bikerental_platform;
   SELECT * FROM hotels;
   ```

2. **Test the full workflow:**
   - Create a hotel
   - Login as that hotel
   - Verify the hotel can access hotel endpoints (not admin endpoints)

3. **Test edge cases:**
   - Very long hotel codes/names
   - Special characters
   - Unicode characters

4. **Performance testing:**
   - Create multiple hotels
   - List hotels with many entries
   - Test concurrent requests

---

## Summary

You now have three ways to test the admin features:

1. **Browser DevTools** - Quick and easy, great for interactive testing
2. **cURL** - Command line, good for scripts and automation
3. **Postman** - GUI tool, great for saving and organizing requests

Choose the method that works best for you! The browser DevTools method is probably the easiest to get started with.
