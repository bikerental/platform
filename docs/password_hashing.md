# Password Hashing Implementation Guide

## Overview

The bike rental system uses **bcrypt** for password hashing, which is a secure, one-way cryptographic hash function specifically designed for passwords. This document explains how password hashing works in the system and how to properly add hotels to the database in production.

---

## How It Works

### Architecture

1. **Password Storage**: Passwords are **never stored in plain text**. Only bcrypt hashes are stored in the `hotels.password_hash` column.

2. **Hashing on Creation**: When a hotel is created, the plain password is hashed using bcrypt with a cost factor of 10 before storing.

3. **Verification on Login**: When a user logs in, the provided password is hashed and compared against the stored hash using bcrypt's built-in comparison function.

### Implementation Details

#### Backend Components

**Location**: `com.bikerental.platform.rental.config.SecurityConfig`

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10); // Cost factor 10
}
```

- **Cost Factor 10**: This means 2^10 = 1,024 iterations of the hashing algorithm. Higher values = more secure but slower.
- **Spring Security BCryptPasswordEncoder**: Handles both hashing and verification automatically.

**Authentication Flow** (`AuthService.authenticate()`):

1. Lookup hotel by `hotel_code`
2. Use `passwordEncoder.matches(plainPassword, storedHash)` to verify
3. If match succeeds, generate JWT token
4. If match fails, return empty (generic error - no hints)

#### Hash Format

Bcrypt hashes follow this format:
```
$2a$10$iOURAF1RHhsMCdRr2U1n/.JTfyq4cyuokEyEn.E/YwIsWWRX8biAi
 │  │  │                                    │
 │  │  │                                    └─ 31 characters (salt + hash)
 │  │  └─ Cost factor (2^10 = 1,024 iterations)
 │  └─ Algorithm version (2a = bcrypt)
 └─ Format identifier
```

**Total length**: 60 characters

---

## Adding Hotels in Production

### Option 1: Using Admin API (Recommended)

The easiest way to create hotels and reset passwords is through the Admin API endpoints.

**Quick Start:**

```bash
# 1. Login as admin
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"hotelCode": "admin", "password": "ChangeThisAdminPwd123!"}'

# 2. Use the returned token to create a hotel
curl -X POST http://localhost:8080/api/admin/hotels \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"hotelCode": "MYHOTEL", "hotelName": "My Hotel", "password": "SecurePass123"}'

# 3. Reset a password
curl -X POST http://localhost:8080/api/admin/hotels/{hotelId}/reset-password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"newPassword": "NewSecurePass456"}'
```

See `docs/admin_api.md` for full API documentation.

### Option 2: Using Spring Boot Application (Programmatic)

Create a service method or admin endpoint to add hotels securely:

```java
@Service
@RequiredArgsConstructor
public class HotelManagementService {
    
    private final HotelRepository hotelRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Transactional
    public Hotel createHotel(String hotelCode, String hotelName, String plainPassword) {
        // Hash the password before storing
        String passwordHash = passwordEncoder.encode(plainPassword);
        
        Hotel hotel = new Hotel();
        hotel.setHotelCode(hotelCode);
        hotel.setHotelName(hotelName);
        hotel.setPasswordHash(passwordHash);
        
        return hotelRepository.save(hotel);
    }
}
```

**Usage**:
```java
hotelManagementService.createHotel("HOTEL123", "My Hotel Name", "securePassword123");
```

### Option 2: Using SQL (Manual Insert)

If you need to insert hotels directly via SQL, you must generate the bcrypt hash first.

#### Step 1: Generate the Hash

**Using Java (recommended)**:

Create a temporary class or use the test utility:

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
String hash = encoder.encode("yourPlainPassword");
System.out.println(hash);
```

**Using Python** (if Java not available):

```python
import bcrypt

password = b"yourPlainPassword"
salt = bcrypt.gensalt(rounds=10)
hashed = bcrypt.hashpw(password, salt)
print(hashed.decode())
```

**Using Online Tools** (⚠️ **NOT RECOMMENDED FOR PRODUCTION**):
- Only use for development/testing
- Never use online tools for production passwords

#### Step 2: Insert into Database

```sql
INSERT INTO hotels (hotel_code, hotel_name, password_hash, created_at)
VALUES (
    'HOTEL123',
    'My Hotel Name',
    '$2a$10$iOURAF1RHhsMCdRr2U1n/.JTfyq4cyuokEyEn.E/YwIsWWRX8biAi', -- Generated hash
    NOW()
);
```

**⚠️ Important**: Replace the hash above with the one you generated for your specific password.

### Option 3: Using Database Migration Scripts

For production deployments, use Flyway or Liquibase migration scripts:

**Example Flyway migration** (`V1__create_initial_hotels.sql`):

```sql
-- Migration: Create initial hotels
-- Note: In production, generate hashes securely before committing

INSERT INTO hotels (hotel_code, hotel_name, password_hash, created_at)
VALUES 
    ('HOTEL1', 'Hotel One', '$2a$10$...', NOW()),
    ('HOTEL2', 'Hotel Two', '$2a$10$...', NOW());
```

**Best Practice**: Generate hashes during deployment, not in source control.

---

## Security Best Practices

### ✅ DO

1. **Always hash passwords** - Never store plain text passwords
2. **Use strong passwords** - Enforce minimum length (8+ characters), complexity
3. **Use cost factor 10+** - Balance between security and performance
4. **Generate hashes securely** - Use the application's PasswordEncoder, not external tools
5. **Rotate passwords** - Provide mechanism for password changes
6. **Use HTTPS** - Always transmit passwords over encrypted connections
7. **Log security events** - Log failed login attempts (without exposing details)

### ❌ DON'T

1. **Never store plain passwords** - Even temporarily
2. **Don't use weak cost factors** - Below 10 is not recommended
3. **Don't reuse hashes** - Each password gets a unique hash (salt is random)
4. **Don't expose hash generation** - Keep password hashing server-side only
5. **Don't log passwords** - Never log plain passwords or hashes in production logs
6. **Don't use predictable salts** - BCrypt handles this automatically

---

## Password Verification Flow

When a user logs in:

```
1. User submits: hotelCode + plainPassword
   ↓
2. Backend looks up hotel by hotelCode
   ↓
3. Backend calls: passwordEncoder.matches(plainPassword, storedHash)
   ↓
4. BCrypt internally:
   a. Extracts salt from stored hash
   b. Hashes plainPassword with that salt
   c. Compares result with stored hash
   ↓
5. Returns true/false
   ↓
6. If true: Generate JWT token
   If false: Return generic error (no hints)
```

**Key Point**: The `matches()` method handles all the complexity. You never manually extract salts or compare hashes.

---

## Troubleshooting

### Issue: "Invalid hotel code or password"

**Possible causes**:
1. Wrong password hash in database
2. Hotel code doesn't exist
3. Case sensitivity (hotel codes should be case-sensitive)

**Debug steps**:
1. Verify hotel exists: `SELECT * FROM hotels WHERE hotel_code = 'YOUR_CODE';`
2. Verify hash format: Should start with `$2a$10$` and be 60 characters
3. Test hash manually:
   ```java
   BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
   boolean matches = encoder.matches("testPassword", storedHash);
   ```

### Issue: Hash doesn't match

**Common mistakes**:
- Using wrong cost factor (must be 10)
- Truncating the hash (must be full 60 characters)
- Using wrong algorithm version (must be `$2a$`)

**Solution**: Regenerate the hash using the application's PasswordEncoder.

---

## Example: Complete Hotel Creation Script

For production deployment, here's a complete example:

```java
@Service
@RequiredArgsConstructor
public class HotelSeeder {
    
    private final HotelRepository hotelRepository;
    private final PasswordEncoder passwordEncoder;
    
    @PostConstruct
    public void seedHotels() {
        // Only seed if no hotels exist
        if (hotelRepository.count() == 0) {
            createHotel("HOTEL1", "Production Hotel 1", "SecurePass123!");
            createHotel("HOTEL2", "Production Hotel 2", "AnotherSecurePass456!");
        }
    }
    
    private void createHotel(String code, String name, String password) {
        if (!hotelRepository.existsByHotelCode(code)) {
            Hotel hotel = new Hotel();
            hotel.setHotelCode(code);
            hotel.setHotelName(name);
            hotel.setPasswordHash(passwordEncoder.encode(password));
            hotelRepository.save(hotel);
        }
    }
}
```

**⚠️ Security Note**: In production, passwords should come from:
- Environment variables
- Secret management systems (AWS Secrets Manager, HashiCorp Vault)
- Secure configuration files (not in source control)

---

## Development vs Production

### Development
- Seed data in `data.sql` is acceptable
- Use simple passwords for testing
- Hash can be committed to source control (it's a test password)

### Production
- **Never commit production passwords or hashes to source control**
- Use environment variables or secret management
- Generate hashes during deployment, not in code
- Use strong, unique passwords for each hotel
- Consider password rotation policies

---

## References

- [Spring Security BCryptPasswordEncoder](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html#authentication-password-storage-bcrypt)
- [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
- [Bcrypt Algorithm](https://en.wikipedia.org/wiki/Bcrypt)

---

## Summary

1. **Hashing**: Passwords are hashed using bcrypt (cost factor 10) before storage
2. **Verification**: Spring's `BCryptPasswordEncoder.matches()` handles comparison
3. **Adding Hotels**: Use the application's PasswordEncoder to generate hashes
4. **Security**: Never store plain passwords; always use HTTPS; generate hashes securely
5. **Production**: Use secret management; never commit production passwords/hashes
