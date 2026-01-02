# Password Hashing

This project uses **bcrypt** with cost factor 10 for password hashing via Spring Security's `BCryptPasswordEncoder`.

## Hash Format

```
$2a$10$iOURAF1RHhsMCdRr2U1n/.JTfyq4cyuokEyEn.E/YwIsWWRX8biAi
```

- Algorithm: `$2a$` (bcrypt)
- Cost factor: `10` (2^10 = 1,024 iterations)
- Total length: 60 characters

## Creating Hotels

### Via Admin API (Recommended)

```bash
# Login as admin
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"hotelCode": "admin", "password": "admin123"}'

# Create hotel (password will be hashed automatically)
curl -X POST http://localhost:8080/api/admin/hotels \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"hotelCode": "MYHOTEL", "hotelName": "My Hotel", "password": "SecurePass123"}'
```

### Via Direct SQL

Generate hash first:

```java
new BCryptPasswordEncoder(10).encode("yourPassword")
```

Then insert:

```sql
INSERT INTO hotels (hotel_code, hotel_name, password_hash, is_admin, created_at)
VALUES ('HOTEL1', 'Hotel Name', '$2a$10$...hash...', false, NOW());
```

## Security Notes

- Never store plain passwords
- Never log passwords or hashes
- Production passwords should come from environment variables or secret management
- Use HTTPS in production
