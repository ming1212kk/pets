# Pet Social MVP - Stage 1

This repository now contains a Spring Boot 3 backend for phase 1 of the pet social mini program, backed by MySQL and Redis.

## Covered in stage 1

- WeChat login (mock code exchange)
- Phone login with SMS verification code and Redis-based send-frequency limiting
- JWT-based authentication
- Pet profile create/edit/default selection
- Public post publishing with text, images, and one video URL
- Image/text synchronous review
- Video asynchronous review simulation
- Post list/detail
- Like / unlike
- First-level comments and comment deletion
- Basic admin APIs for viewing posts, deleting posts, and banning users

## Infrastructure

Start local MySQL and Redis:

```bash
docker compose up -d
```

Default local connections:

- MySQL: `jdbc:mysql://localhost:3306/pet_social`
- Redis: `localhost:6379`

Environment variables:

- `MYSQL_URL`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD`
- `REDIS_DATABASE`

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

- Core business data is persisted in MySQL.
- SMS verification codes and rate-limit cooldowns are stored in Redis.
- Object storage, real WeChat login, and production content-safety integrations are still mocked at the business layer.

## Frontend deliverables

- WeChat mini program source: `miniapp/`
- WeChat DevTools config: `project.config.json`
- Admin page: `http://localhost:8080/admin/index.html`

If `8080` is occupied locally, use:

```bash
MYSQL_USERNAME=root MYSQL_PASSWORD=zgm585880 SERVER_PORT=8081 mvn spring-boot:run
```

Then open `http://localhost:8081/admin/index.html` and use the same base URL in the mini program login page.
