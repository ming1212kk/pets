# Pet Social MVP - Stage 1

This repository now contains a runnable Spring Boot 3 backend for phase 1 of the pet social mini program.

## Covered in stage 1

- WeChat login (mock code exchange)
- Phone login with SMS verification code and send-frequency limiting
- JWT-based authentication
- Pet profile create/edit/default selection
- Public post publishing with text, images, and one video URL
- Image/text synchronous review
- Video asynchronous review simulation
- Post list/detail
- Like / unlike
- First-level comments and comment deletion
- Basic admin APIs for viewing posts, deleting posts, and banning users

## How to run

```bash
mvn spring-boot:run
```

The service starts on `http://localhost:8080`.

## Demo login flow

1. Send SMS code:

```bash
curl -X POST http://localhost:8080/api/v1/auth/sms/send \
  -H 'Content-Type: application/json' \
  -d '{"phone":"13800000000"}'
```

The response includes `debugCode` because there is no real SMS provider in this MVP.

2. Login with the code:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login/phone \
  -H 'Content-Type: application/json' \
  -d '{"phone":"13800000000","code":"123456"}'
```

3. Use the returned JWT:

```bash
curl http://localhost:8080/api/v1/posts \
  -H 'Authorization: Bearer <token>'
```

## Demo admin login

```bash
curl -X POST http://localhost:8080/api/v1/auth/admin/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
```

## Notes

- Data is stored in memory for fast local acceptance testing.
- The current implementation mirrors the phase-1 product behavior, while leaving MySQL, Redis, object storage, and real WeChat/content-safety integrations for the next iteration.
