# mvpiq

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/mvpiq-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.

## Related Guides

- Hibernate ORM with Panache ([guide](https://quarkus.io/guides/hibernate-orm-panache)): Simplify your persistence code for Hibernate ORM via the active record or the repository pattern
- RESTEasy Classic JSON-B ([guide](https://quarkus.io/guides/rest-json)): JSON-B serialization support for RESTEasy Classic
- Security JPA ([guide](https://quarkus.io/guides/security-getting-started)): Secure your applications with username/password stored in a database via Jakarta Persistence
- SmallRye OpenAPI ([guide](https://quarkus.io/guides/openapi-swaggerui)): Document your REST APIs with OpenAPI - comes with Swagger UI
- RESTEasy Classic ([guide](https://quarkus.io/guides/resteasy)): REST endpoint framework implementing Jakarta REST and more

## Provided Code

### Hibernate ORM

Create your first JPA entity

[Related guide section...](https://quarkus.io/guides/hibernate-orm)

[Related Hibernate with Panache section...](https://quarkus.io/guides/hibernate-orm-panache)


### RESTEasy JAX-RS

Easily start your RESTful Web Services

[Related guide section...](https://quarkus.io/guides/getting-started#the-jax-rs-resources)

---

## API Documentation

Base URL: `/api`

All APIs use JSON format (`application/json`) unless otherwise specified.

### 1. Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/register` | Register new user |
| `POST` | `/api/auth/login` | User login |
| `POST` | `/api/auth/logout` | User logout |

### 2. Users

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/users/me/{userId}` | Get current user data |
| `GET` | `/api/users/online?minutesAgo={n}` | Get online users (admin only) |

### 3. Player Profiles

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/player-profiles?userId={id}` | Create player profile |
| `PUT` | `/api/player-profiles/{id}` | Update profile |
| `GET` | `/api/player-profiles/{id}` | Get profile by ID |
| `GET` | `/api/player-profiles/user/{userId}` | Get profile by user ID |
| `PUT` | `/api/player-profiles/user/{userId}` | Update profile by user ID |
| `DELETE` | `/api/player-profiles/{id}` | Delete profile |
| `GET` | `/api/player-profiles/country/{country}` | Filter by country |
| `GET` | `/api/player-profiles/level/{level}` | Filter by level |
| `GET` | `/api/player-profiles/position/{position}` | Filter by position |
| `GET` | `/api/player-profiles/age-range?minAge={n}&maxAge={n}` | Filter by age range |
| `GET` | `/api/player-profiles/public` | Get public profiles |
| `POST` | `/api/player-profiles/search` | Advanced search with filters |
| `GET` | `/api/player-profiles/{id}/stats` | Player statistics |
| `GET` | `/api/player-profiles/{id}/rankings` | Player rankings |

**Athlete Resource:**
- `GET /api/athletes` - Get all athletes
- `GET /api/athlet/{id}` - Get profile by ID
- `GET /api/athlet/user/{userId}` - Get profile by user ID
- `PUT /api/athlet/{id}` - Update profile

**Player CV:**
- `GET /api/players/{playerId}/cv` - Get CV
- `PUT /api/players/{playerId}/cv` - Update CV

### 4. Trainer

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/trainer/follow` | Follow a player |
| `DELETE` | `/api/trainer/follow?trainerId={id}&playerId={id}` | Unfollow player |
| `GET` | `/api/trainer/follows/{trainerId}` | List followed players |
| `GET` | `/api/trainer/followers/{playerId}` | List followers |
| `GET` | `/api/trainer/follow/check` | Check if following |
| `GET` | `/api/trainer/stats/{trainerId}` | Trainer statistics |
| `GET` | `/api/trainer/players/{trainerId}/progress` | Players progress |
| `GET` | `/api/trainer/players/{playerId}/details` | Player details for trainer |
| `POST` | `/api/trainer/feedback` | Add feedback |
| `GET` | `/api/trainer/follow-count/{trainerId}` | Follow count |
| `GET` | `/api/trainer/follower-count/{playerId}` | Follower count |

### 5. Journal

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/players/{playerId}/journal?entryType={type}` | List journal entries |
| `GET` | `/api/players/{playerId}/journal/{entryId}` | Get single entry |
| `POST` | `/api/players/{playerId}/journal` | Create journal entry |
| `DELETE` | `/api/players/{playerId}/journal/{entryId}` | Delete entry |

### 6. Training

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/training/programs` | Public programs |
| `GET` | `/api/training/sessions/{userId}` | User sessions |
| `GET` | `/api/training/stats/{userId}` | User statistics |

**Exercises:**
- `POST /api/exercises` - Create exercise
- `PUT /api/exercises/{id}` - Update exercise
- `DELETE /api/exercises/{id}` - Delete exercise
- `GET /api/exercises/{id}` - Get exercise details
- `GET /api/exercises/owner/{ownerId}` - Exercises by owner
- `GET /api/exercises/public` - Public exercises
- `GET /api/exercises/category/{category}` - Filter by category
- `GET /api/exercises/difficulty/{difficulty}` - Filter by difficulty
- `GET /api/exercises/search?title={q}` - Search by title
- `GET /api/exercises/media-type/{mediaType}` - Filter by media type

### 7. Goals

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/goals/{athleteId}` | Get athlete goals |
| `POST` | `/api/goals/{athleteId}` | Create goal |
| `PUT` | `/api/goals/{goalId}` | Update goal |

### 8. Gamification

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/players/{playerId}/gamification/achievements/check` | Check achievements |
| `GET` | `/api/players/{playerId}/gamification/badges` | Get player badges |
| `POST` | `/api/players/{playerId}/gamification/badges/{badgeId}/award` | Award badge |
| `GET` | `/api/players/{playerId}/gamification/streak` | Current streak |
| `POST` | `/api/players/{playerId}/gamification/daily-progress` | Calculate daily progress |
| `GET` | `/api/players/{playerId}/gamification/weekly-stats` | Weekly statistics |
| `GET` | `/api/players/{playerId}/gamification/monthly-stats` | Monthly statistics |
| `GET` | `/api/players/{playerId}/gamification/progress-summary` | Progress summary |
| `GET` | `/api/players/{playerId}/gamification/goals/{goalId}/progress` | Goal progress |
| `PUT` | `/api/players/{playerId}/gamification/goals/{goalId}/progress` | Update goal progress |
| `POST` | `/api/players/{playerId}/gamification/points/update` | Update points |
| `POST` | `/api/players/{playerId}/gamification/achievements/initialize` | Initialize system badges |

### 9. Subscriptions

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/users/{userId}/subscription` | Get subscription details |
| `GET` | `/api/users/{userId}/subscription/premium` | Check premium access |
| `GET` | `/api/users/{userId}/subscription/scout-access` | Check scout access |
| `GET` | `/api/users/{userId}/subscription/creator-access` | Check creator access |
| `GET` | `/api/users/{userId}/subscription/plan` | Get subscription plan |
| `POST` | `/api/users/{userId}/subscription/upgrade-premium` | Upgrade to premium |
| `GET` | `/api/users/{userId}/subscription/features/{feature}` | Check feature access |
| `GET` | `/api/users/{userId}/subscription/video-upload-limits` | Video upload limits |
| `GET` | `/api/users/{userId}/subscription/video-analysis-limits` | Video analysis limits |
| `GET` | `/api/users/{userId}/subscription/can-create-official-content` | Can create official content |
| `GET` | `/api/users/{userId}/subscription/can-access-analytics` | Advanced analytics access |
| `GET` | `/api/users/{userId}/subscription/can-use-advanced-filters` | Advanced filters access |

### 10. Scout

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/scout/filters` | Save filter |
| `PUT` | `/api/scout/filters/{id}` | Update filter |
| `DELETE` | `/api/scout/filters/{id}` | Delete filter |
| `GET` | `/api/scout/filters/{id}` | Get filter |
| `GET` | `/api/scout/filters/scout/{scoutId}` | Saved filters |
| `GET` | `/api/scout/filters/search?scoutId={id}&name={n}` | Search filters |
| `POST` | `/api/scout/search` | Search players |
| `GET` | `/api/scout/rankings?scope={s}&scopeValue={v}` | Rankings |
| `GET` | `/api/scout/players/{playerId}/profile` | Scout profile view |

### 11. Conversations & Messages

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/conversations` | Create conversation |
| `POST` | `/api/conversations/{id}/messages` | Send message |
| `GET` | `/api/conversations/{id}` | Get conversation |
| `GET` | `/api/conversations/{id}/messages` | Get messages |
| `GET` | `/api/conversations/{id}/participants` | Get participants |
| `GET` | `/api/conversations/user/{userId}` | User conversations |
| `POST` | `/api/conversations/{id}/participants` | Add participant |
| `DELETE` | `/api/conversations/{id}/participants/{userId}` | Remove participant |

**Legacy Messages:**
- `GET /api/messages/{userId}` - Messages by user
- `POST /api/message` - Send message

### 12. Video Analysis

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/analysis/types` | Available analysis types |
| `POST` | `/api/analysis/sessions` | Create analysis session |
| `POST` | `/api/analysis/analyze` | Analyze video |
| `GET` | `/api/analysis/sessions/{id}/result` | Get analysis result |

### 13. Media

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/videos/{athleteId}` | Videos by athlete |
| `POST` | `/api/videos/upload` | Upload video (multipart/form-data) |

### 14. Rankings

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/ranking/{role}` | Ranking by role (PG,SG,SF,PF,C) |
| `GET` | `/api/ranking/global` | Global ranking |

### 15. Checklist Templates

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/checklist-templates?entryType={type}` | Templates by type |
| `GET` | `/api/checklist-templates/all` | All templates (admin) |
| `GET` | `/api/checklist-templates/{id}` | Single template (admin) |
| `POST` | `/api/checklist-templates` | Create template (admin) |
| `PUT` | `/api/checklist-templates/{id}` | Update template (admin) |
| `DELETE` | `/api/checklist-templates/{id}` | Delete template (admin) |
| `GET` | `/api/checklist-templates/dynamic-options` | Dynamic options |

### 16. Notifications

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/notifications/user/{userId}?limit={n}&unreadOnly={bool}` | User notifications |
| `GET` | `/api/notifications/user/{userId}/unread-count` | Unread count |
| `PUT` | `/api/notifications/{notificationId}/read` | Mark as read |
| `PUT` | `/api/notifications/user/{userId}/read-all` | Mark all as read |
| `DELETE` | `/api/notifications/user/{userId}` | Delete all notifications |
| `POST` | `/api/notifications/test/{userId}` | Send test notification |

**Device Tokens:**
- `POST /api/device-tokens/register` - Register device token
- `GET /api/device-tokens/user/{userId}?platform={p}` - Tokens by user
- `PUT /api/device-tokens/deactivate?token={t}` - Deactivate token
- `PUT /api/device-tokens/user/{userId}/deactivate-all` - Deactivate all
- `PUT /api/device-tokens/device/{deviceId}/deactivate` - Deactivate by device
- `DELETE /api/device-tokens/cleanup` - Cleanup inactive tokens

### 17. AI Training

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/ai/training-programs/generate` | Generate AI program |
| `GET` | `/api/ai/training-programs/{athleteId}` | Programs by athlete |
| `GET` | `/api/ai/training-programs/program/{programId}` | Program details |
| `POST` | `/api/ai/training-programs/{programId}/regenerate` | Regenerate program |
| `GET` | `/api/ai/training-programs/status/{programId}` | Generation status |

### 18. Navigation

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/navigation/sections` | Accessible sections |
| `GET` | `/api/navigation/sections/{sectionId}/access` | Check section access |
| `GET` | `/api/navigation/sections/all` | All sections (admin) |

### 19. Position Metadata

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/positions` | List basketball positions |

### 20. Health Check

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/mvpiq` | Basic health check |

---

## Technical Notes

- **Base URL**: All APIs start with `/api/` (except `/mvpiq`)
- **Authentication**: Some APIs require authentication (`@Authenticated`)
- **Format**: All APIs use JSON (`application/json`)
- **Upload**: Video upload uses `multipart/form-data`
- **IDs**: All IDs are UUID format (e.g., `550e8400-e29b-41d4-a716-446655440000`)
