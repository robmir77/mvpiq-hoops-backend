# Documento di Analisi Tecnica - Backend MVPiQ Hoops

## 1. Panoramica del Progetto

### 1.1 Informazioni Generali

- **Nome Progetto**: MVPiQ Hoops Backend
- **Framework**: Quarkus 3.8.3 (Supersonic Subatomic Java)
- **Linguaggio**: Java 17
- **Build Tool**: Maven
- **Database**: PostgreSQL (Neon Cloud)
- **Porta**: 8082
- **Versione**: 1.0.0-SNAPSHOT

### 1.2 Architettura

Il backend segue un'architettura a strati tipica di applicazioni enterprise Java:

```
┌─────────────────────────────────────┐
│         REST API Layer              │
│  (Resource/Controller)              │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│         Service Layer               │
│  (Business Logic)                  │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│      Repository Layer               │
│  (Data Access - Panache)            │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│      Database Layer                 │
│  (PostgreSQL - Neon)                │
└─────────────────────────────────────┘
```

---

## 2. Stack Tecnologico

### 2.1 Core Framework

- **Quarkus 3.8.3**: Framework supersonico per applicazioni Java native
- **Hibernate ORM with Panache**: Semplificazione dell'accesso ai dati
- **RESTEasy Classic**: Framework REST JAX-RS
- **SmallRye OpenAPI**: Documentazione automatica API (Swagger)
- **SmallRye JWT**: Autenticazione e autorizzazione JWT

### 2.2 Database

- **PostgreSQL**: Database relazionale (hosting su Neon Cloud)
- **Hibernate ORM**: ORM per mapping oggetto-relazionale
- **Panache**: Repository pattern semplificato

### 2.3 AI/ML

- **DJL (Deep Java Library) 0.27.0**: Framework per deep learning in Java
- **ONNX Runtime**: Runtime per modelli ONNX
- **TensorFlow Engine**: Supporto per modelli TensorFlow
- **YOLOv5**: Modello per object detection (palla, canestro, giocatore)

### 2.4 Elaborazione Video

- **FFmpeg 6.1.1**: Elaborazione video
- **TwelveMonkeys ImageIO**: Manipolazione immagini
- **Apache Commons Math 3.6.1**: Calcoli matematici per traiettorie

### 2.5 Servizi Esterni

- **Firebase Admin SDK 9.2.0**: Push notifications (FCM)
- **Supabase**: Storage per video e frame di analisi
- **Ollama**: AI locale per generazione contenuti

### 2.6 Sicurezza

- **JBcrypt**: Hashing password
- **JWT**: Token-based authentication
- **Quarkus Security JPA**: Autorizzazione basata su ruoli

---

## 3. Modello Dati

### 3.1 Entità Principali per Tracking Tiri

#### WorkoutSession

```java
@Entity
@Table(name = "workout_sessions")
public class WorkoutSession {
    - id: UUID (PK)
    - player: Player (FK)
    - cameraMode: CameraMode (LATERAL, FRONTAL, ANGLE_45)
    - courtType: CourtType (HALF_COURT, FULL_COURT)
    - startTime: OffsetDateTime
    - endTime: OffsetDateTime
    - totalShots: Integer
    - madeShots: Integer
    - sessionStatus: String (ACTIVE, COMPLETED, PAUSED)
    - calibrationData: String (JSONB)
    - shots: List<ShotEvent> (One-to-Many)
    - createdAt: OffsetDateTime
    - updatedAt: OffsetDateTime
}
```

#### ShotEvent

```java
@Entity
@Table(name = "shot_events")
public class ShotEvent {
    - id: UUID (PK)
    - workoutSession: WorkoutSession (FK)
    - timestampMs: Long
    - shotResult: ShotResult (MADE, MISS, BLOCKED, AIRBALL)
    - courtX: Double
    - courtY: Double
    - distanceFromHoop: Double
    - releaseAngle: Double
    - releaseVelocity: Double
    - shotArcHeight: Double
    - videoTimestampMs: Long
    - detectionConfidence: Double
    - trackingData: String (JSONB)
    - videoClipPath: String
    - createdAt: OffsetDateTime
}
```

#### CourtCalibration

```java
@Entity
@Table(name = "court_calibrations")
public class CourtCalibration {
    - id: UUID (PK)
    - workoutSession: WorkoutSession (FK)
    - hoopCenterX: Double
    - hoopCenterY: Double
    - freeThrowLineX: Double
    - freeThrowLineY: Double
    - threePointLineTopX/Y: Double
    - threePointLineLeftX/Y: Double
    - threePointLineRightX/Y: Double
    - baselineX/Y: Double
    - sidelineLeftX/Y: Double
    - sidelineRightX/Y: Double
    - homographyMatrix: String (JSONB)
    - calibrationConfidence: Double
    - createdAt: OffsetDateTime
    - updatedAt: OffsetDateTime
}
```

#### WorkoutFrameData

```java
@Entity
@Table(name = "workout_frame_data")
public class WorkoutFrameData {
    - id: UUID (PK)
    - session: WorkoutSession (FK)
    - frameTimestamp: Long
    - ballX: Double
    - ballY: Double
    - ballConfidence: Double
    - hoopX: Double
    - hoopY: Double
    - hoopConfidence: Double
    - poseData: Map<String, Object> (JSONB)
    - trajectoryData: Map<String, Object> (JSONB)
    - ballVelocityX: Double
    - ballVelocityY: Double
    - shotDetected: Boolean
    - createdAt: OffsetDateTime
}
```

**Descrizione:** Memorizza dati frame video per analisi in tempo reale durante sessioni workout, inclusi tracking palla, canestro, pose giocatore e traiettorie.

### 3.2 Altre Entità Principali

- **Player**: Profilo giocatore (estende User, contiene attributi fisici e background)
- **User**: Utente del sistema (classe base per tutti gli utenti)
- **Role**: Catalogo ruoli applicativi (RBAC)
- **UserRoleAssignment**: Associazione molti-a-molti utenti-ruoli (RBAC)
- **JournalEntry**: Diario allenamenti/partite
- **TrainingSession**: Sessioni di training
- **Exercise**: Esercizi
- **Badge**: Badge gamification
- **AthleteGoal**: Obiettivi atleta
- **VideoAnalysisSession**: Sessioni analisi video
- **Conversation**: Conversazioni messaggistica
- **Notification**: Notifiche push

**Nota sulla Struttura del Database:**
- La tabella `players` estende `users` tramite table-per-type inheritance
- Foreign keys puntano a `users` per: athlete_badges, athlete_goals, athlete_points, player_cv, rankings, trainer_follows, training_sessions
- Foreign key punta a `players` per: workout_sessions
- L'entità `PlayerProfile` è stata rimossa in favore di `Player`
- **Sistema RBAC**: Ruoli gestiti tramite tabelle `roles` e `user_roles` (molti-a-molti)
- **Campi rimossi da `users`**: `role`, `is_creator`, `is_trainer` (gestiti tramite RBAC)

---

## 4. API REST - Tracking Tiri (Punto 21)

### 4.1 Workout Sessions

#### POST /api/workouts/sessions
Crea una nuova sessione di workout

**Request:**
```json
{
  "cameraMode": "ANGLE_45",
  "courtType": "HALF_COURT",
  "calibrationData": "{\"homographyMatrix\": [...], \"hoopCenter\": {\"x\": 320, \"y\": 240}}"
}
```

**Response:**
```json
{
  "id": "session-123",
  "player": {...},
  "cameraMode": "ANGLE_45",
  "courtType": "HALF_COURT",
  "startTime": "2026-05-12T10:00:00Z",
  "totalShots": 0,
  "madeShots": 0,
  "sessionStatus": "ACTIVE"
}
```

#### GET /api/workouts/sessions/{sessionId}
Recupera i dettagli di una sessione

#### GET /api/workouts/sessions
Recupera tutte le sessioni di un giocatore

#### POST /api/workouts/sessions/{sessionId}/end
Termina una sessione di workout

#### POST /api/workouts/sessions/{sessionId}/pause
Mette in pausa una sessione

#### POST /api/workouts/sessions/{sessionId}/resume
Riprende una sessione in pausa

#### GET /api/workouts/sessions/active
Recupera la sessione attiva di un giocatore

#### DELETE /api/workouts/sessions/{sessionId}
Elimina una sessione di workout

**Response:** 204 No Content

#### POST /api/workouts/sessions/{sessionId}/frames
Salva dati frame video per analisi in tempo reale

**Request:**
```json
{
  "frameTimestamp": 123456,
  "ballX": 320.5,
  "ballY": 240.2,
  "ballConfidence": 0.95,
  "hoopX": 640.0,
  "hoopY": 480.0,
  "hoopConfidence": 0.98,
  "poseData": {...},
  "trajectoryData": {...},
  "ballVelocityX": 5.2,
  "ballVelocityY": -8.1,
  "shotDetected": false
}
```

**Response:**
```json
{
  "id": "frame-123",
  "sessionId": "session-123",
  "frameTimestamp": 123456,
  "ballX": 320.5,
  "ballY": 240.2,
  "ballConfidence": 0.95,
  "hoopX": 640.0,
  "hoopY": 480.0,
  "hoopConfidence": 0.98,
  "poseData": {...},
  "trajectoryData": {...},
  "ballVelocityX": 5.2,
  "ballVelocityY": -8.1,
  "shotDetected": false,
  "createdAt": "2026-05-19T10:00:00Z"
}
```

#### POST /api/workouts/sessions/{sessionId}/pose-analysis
Salva dati di analisi pose del giocatore

**Request:**
```json
{
  "timestampMs": 123456,
  "poseLandmarks": [...],
  "poseConfidence": 0.92
}
```

**Response:**
```json
{
  "id": "pose-123",
  "sessionId": "session-123",
  "timestampMs": 123456,
  "poseLandmarks": [...],
  "poseConfidence": 0.92,
  "createdAt": "2026-05-19T10:00:00Z"
}
```

#### GET /api/workouts/sessions/{sessionId}/realtime-stats
Recupera statistiche in tempo reale della sessione

**Response:**
```json
{
  "sessionId": "session-123",
  "shotCount": 25,
  "fieldGoalPercentage": 68.0,
  "shotStreak": 5,
  "releaseAngleAvg": 47.5,
  "releaseVelocityAvg": 12.3,
  "heatZones": {
    "PAINT": 8,
    "MID_RANGE": 10,
    "THREE_POINT": 7
  },
  "recentShots": [
    {
      "courtX": 4.2,
      "courtY": 6.8,
      "result": "MADE",
      "timestamp": 123456
    }
  ],
  "sessionDuration": 1800
}
```

### 4.2 WebSocket Live Updates

#### WS /api/workouts/live/{sessionId}?userId={userId}
WebSocket per aggiornamenti in tempo reale delle statistiche

**Funzionalità:**
- Connessione WebSocket per streaming statistiche live
- Broadcast automatico ogni secondo delle statistiche
- Push manuale dopo ogni tiro
- Supporto multi-client per stessa sessione

**Messaggi:** JSON con struttura `RealtimeStatsResponse`

### 4.3 Shot Events

#### GET /api/workouts/sessions/{sessionId}/shots
Recupera tutti i tiri di una sessione

**Response:**
```json
[
  {
    "id": "shot-1",
    "timestampMs": 123456,
    "shotResult": "MADE",
    "courtX": 4.2,
    "courtY": 6.8,
    "distanceFromHoop": 7.1,
    "releaseAngle": 48.2,
    "releaseVelocity": 12.5,
    "detectionConfidence": 0.91
  }
]
```

#### POST /api/workouts/sessions/{sessionId}/shots
Aggiunge un evento tiro a una sessione

**Request:**
```json
{
  "timestampMs": 123456,
  "shotResult": "MADE",
  "courtX": 5.2,
  "courtY": 7.4,
  "distanceFromHoop": 7.1,
  "releaseAngle": 48.2,
  "releaseVelocity": 12.5,
  "detectionConfidence": 0.91,
  "trackingData": "{\"ballPosition\": {\"x\": 120, \"y\": 330}}"
}
```

### 4.3 Court Calibration

#### POST /api/workouts/sessions/{sessionId}/calibration
Salva i dati di calibrazione del campo

**Request:**
```json
{
  "hoopCenterX": 320,
  "hoopCenterY": 240,
  "homographyMatrix": [...],
  "calibrationConfidence": 0.95
}
```

### 4.4 Analytics & Statistics

#### GET /api/workouts/{sessionId}/analytics/shot-chart
Recupera lo shot chart di una sessione

**Response:**
```json
{
  "shots": [
    {
      "x": 4.3,
      "y": 5.6,
      "made": true,
      "distance": 7.2,
      "zone": "THREE_POINT"
    }
  ],
  "sessionStats": {
    "totalShots": 100,
    "madeShots": 63,
    "missedShots": 37,
    "shootingPercentage": 63.0,
    "averageDistance": 6.8,
    "bestZone": "PAINT",
    "worstZone": "THREE_POINT"
  },
  "zoneStats": {
    "paint": {"attempts": 30, "made": 24, "percentage": 80.0},
    "midRange": {"attempts": 25, "made": 15, "percentage": 60.0},
    "threePoint": {"attempts": 35, "made": 18, "percentage": 51.4},
    "corner": {"attempts": 10, "made": 6, "percentage": 60.0}
  }
}
```

#### GET /api/workouts/{sessionId}/analytics/stats
Recupera le statistiche di una sessione

#### GET /api/workouts/{sessionId}/analytics/zones
Recupera le statistiche per zona

#### GET /api/workouts/{sessionId}/analytics/hot-zones?limit=10
Recupera i tiri nelle hot zones

#### GET /api/workouts/{sessionId}/analytics/cold-zones?limit=10
Recupera i tiri nelle cold zones

#### GET /api/workouts/{sessionId}/analytics/career-stats
Recupera le statistiche carriera di un giocatore

---

## 5. Servizi Business Logic

### 5.1 WorkoutService

Gestisce la logica di business per le sessioni di workout:

- **createWorkoutSession**: Crea nuova sessione, verifica sessione attiva esistente
- **endWorkoutSession**: Termina sessione, calcola durata
- **addShotEvent**: Aggiunge tiro, aggiorna statistiche sessione
- **saveCalibration**: Salva dati calibrazione
- **pauseSession/resumeSession**: Gestisce stati sessione
- **updateSessionStatistics**: Aggiorna contatori tiri in tempo reale
- **deleteSession**: Elimina una sessione di workout
- **saveFrameData**: Salva dati frame video per analisi real-time (tracking palla, canestro, pose)
- **savePoseAnalysis**: Salva dati di analisi pose del giocatore
- **getRealtimeStats**: Calcola statistiche in tempo reale (tiri, percentuale, streak, heat zones)
- **getSessionShots**: Recupera tutti i tiri di una sessione

### 5.2 ShotAnalyticsService

Gestisce l'analisi dei dati dei tiri:

- **getShotChart**: Genera shot chart completo
- **calculateSessionStats**: Calcola statistiche sessione
- **calculateZoneStats**: Calcola statistiche per zona (PAINT, MID_RANGE, THREE_POINT, CORNER)
- **getHotZones/getColdZones**: Identifica zone performanti/critiche
- **getPlayerCareerStats**: Calcola statistiche carriera

**Logica Zone:**
- PAINT: distanza ≤ 4.0m
- MID_RANGE: 4.0m < distanza ≤ 7.0m
- CORNER: 7.0m < distanza ≤ 8.0m
- THREE_POINT: distanza > 8.0m

### 5.3 ShotDetectionService (AI)

Servizio per rilevamento automatico tiri tramite AI:

- **initializeModel**: Inizializza modello YOLOv5 per object detection
- **detectObjects**: Rileva oggetti (palla, canestro, giocatore) da frame video
- **analyzeForShotEvent**: Analizza sequenza frame per identificare tiri
- **createShotEventFromDetection**: Converte rilevamento in evento tiro

**Algoritmo Rilevamento Tiro:**
1. Tracking palla e giocatore
2. Rilevamento palla vicino mano giocatore
3. Calcolo velocità verso l'alto
4. Rilevamento separazione palla-giocatore
5. Analisi traiettoria verso canestro
6. Determinazione MADE/MISS basata su posizione finale

### 5.4 Altri Servizi AI

- **BallTrackingService**: Tracking palla con Kalman filter
- **HoopDetectionService**: Rilevamento canestro
- **PoseTrackingService**: Tracking pose giocatore (MoveNet)
- **TrajectoryService**: Calcolo traiettoria palla
- **ShotMetricsService**: Calcolo metriche tiro (angolo, velocità, arco)
- **VideoStabilizationService**: Stabilizzazione video
- **OverlayDrawerService**: Disegno overlay campo

---

## 6. Repository Layer

### 6.1 WorkoutSessionRepository

Query personalizzate per sessioni workout:

- `findByPlayer`: Sessioni giocatore ordinate per data
- `findByIdAndPlayer`: Sessione specifica con verifica proprietà
- `findActiveSessionByPlayer`: Sessione attiva corrente
- `findCompletedSessionsByPlayer`: Sessioni completate
- `countByPlayer/countCompletedSessionsByPlayer`: Statistiche

### 6.2 ShotEventRepository

Query personalizzate per eventi tiro:

- `findByWorkoutSession`: Tiri di una sessione
- `findByWorkoutSessionAndResult`: Tiri filtrati per risultato
- `findByWorkoutSessionWithCoordinates`: Tiri con coordinate valide
- `findRecentShotsByPlayer`: Tiri recenti per hot/cold zones
- `calculateAverageDistance`: Media distanza tiri
- `getShotDistributionByZone`: Distribuzione tiri per zona (SQL CASE)

### 6.3 Altri Repository

- **CourtCalibrationRepository**: Dati calibrazione
- **WorkoutFrameDataRepository**: Dati frame video per analisi real-time
- **PlayerRepository**: Profili giocatori (Player entity)
- **UserRepository**: Utenti sistema
- **RoleRepository**: Catalogo ruoli RBAC
- **UserRoleRepository**: Assegnazioni ruoli utenti (RBAC)
- **JournalEntryRepository**: Diario
- **VideoAnalysisSessionRepository**: Sessioni analisi video
- **BadgeRepository**: Gamification

---

## 7. Configurazione

### 7.1 Application Properties

```properties
# Server
quarkus.http.host=0.0.0.0
quarkus.http.port=8082

# Database (Neon PostgreSQL)
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://ep-blue-hat-abs1hipi-pooler.eu-west-2.aws.neon.tech/neondb
quarkus.hibernate-orm.schema-management.strategy=validate

# JWT
mp.jwt.verify.issuer=mvpiq-hoops
mp.jwt.verify.publickey.location=publicKey.pem
smallrye.jwt.sign.key.location=privateKey.pem

# Supabase Storage
supabase.url=https://gnjwgcronnnzxokmuqlw.supabase.co
supabase.bucket.videos=videos
supabase.bucket.frames=analysis-frames

# Video Processing
mvpiq.video.frame-width=960
mvpiq.video.frame-height=544
mvpiq.hoop.fallback-radius=18
mvpiq.hoop.search-frames=10

# Ollama AI
quarkus.rest-client.ollama-api.url=http://localhost:11434

# Firebase
firebase.project.id=mvpiq-hoops
firebase.database.url=https://mvpiq-hoops-default-rtdb.firebaseio.com
```

### 7.2 CORS

```properties
quarkus.http.cors=true
quarkus.http.cors.origins=*
quarkus.http.cors.methods=GET,PUT,POST,DELETE
```

---

## 8. Sicurezza

### 8.1 Autenticazione

- **JWT Token-based**: SmallRye JWT
- **Ruoli**: ADMIN, TRAINER, PLAYER, SCOUT, CREATOR, GUEST
- **Public/Private Key**: Chiavi RSA per firma/verifica

### 8.2 Sistema RBAC (Role-Based Access Control)

**Architettura RBAC:**
- **Tabella `roles`**: Catalogo ruoli applicativi (PLAYER, TRAINER, SCOUT, CREATOR, ADMIN, GUEST)
- **Tabella `user_roles`**: Associazioni molti-a-molti utenti-ruoli
- **Multi-role support**: Un utente può avere più ruoli contemporaneamente
- **Fine boolean flags**: Campi `is_creator` e `is_trainer` rimossi, gestiti tramite ruoli

**Entità RBAC:**
- `Role`: Entità per catalogo ruoli (code, name, description)
- `UserRoleAssignment`: Entità per assegnazioni utente-ruolo (user_id, role_id, assigned_at)

**Servizi RBAC:**
- `RoleRepository`: Repository per gestione ruoli
- `UserRoleRepository`: Repository per gestione assegnazioni ruoli
- `SecurityIdentityRoleMapper`: Mapper per recuperare ruoli dal database in JWT
- `RoleBasedSecurityService`: Servizio per verifiche permessi basate su ruoli

### 8.3 Autorizzazione

Le API per workout richiedono ruolo:
- `@RolesAllowed({"PLAYER", "TRAINER"})`

**Verifiche Ruoli:**
- `hasRole(UserRole.ROLE)`: Verifica ruolo specifico
- `hasAnyRole(UserRole...)`: Verifica uno tra più ruoli
- `canTrain()`: Verifica permessi training (TRAINER o ADMIN)
- `canScout()`: Verifica permessi scouting (SCOUT, TRAINER o ADMIN)
- `canCreateContent()`: Verifica permessi creazione contenuti (CREATOR o ADMIN)

### 8.4 Protezione Endpoints

- **@Authenticated**: Richiede autenticazione
- **Verifica proprietà**: Le query verificano che l'utente abbia accesso alle risorse
- **RBAC integration**: Ruoli recuperati dal database per ogni richiesta

---

## 9. Altre API Implementate

### 9.1 Autenticazione
- POST /api/auth/register
- POST /api/auth/login
- POST /api/auth/logout

### 9.2 Profili Giocatori
- CRUD Player (entità principale per giocatori)
- Filtri per paese, livello, posizione, età
- Statistiche e rankings
- Gestione posizioni (main position, secondary positions)

### 9.3 Trainer

Il sistema Trainer permette agli allenatori di seguire i giocatori, monitorare i loro progressi e fornire feedback.

#### Entità TrainerFollows

```java
@Entity
@Table(name = "trainer_follows")
public class TrainerFollows {
    - id: UUID (PK)
    - trainer: User (FK)
    - player: User (FK)
    - createdAt: OffsetDateTime
}
```

#### API Trainer

**POST /api/trainer/follow**
- Segue un giocatore
- Verifica che l'utente abbia ruolo TRAINER tramite RBAC
- Previene duplicazioni

**Request:**
```json
{
  "trainerId": "uuid",
  "playerId": "uuid"
}
```

**DELETE /api/trainer/follow?trainerId={uuid}&playerId={uuid}**
- Smette di seguire un giocatore

**GET /api/trainer/follows/{trainerId}**
- Recupera tutti i giocatori seguiti da un allenatore

**GET /api/trainer/followers/{playerId}**
- Recupera tutti gli allenatori che seguono un giocatore

**GET /api/trainer/follow/check?trainerId={uuid}&playerId={uuid}**
- Verifica se un allenatore segue un giocatore

**Response:**
```json
{
  "isFollowing": true
}
```

**GET /api/trainer/stats/{trainerId}**
- Recupera statistiche allenatore

**Response:**
```json
{
  "followedPlayers": 15,
  "trainerId": "uuid"
}
```

**GET /api/trainer/players/{trainerId}/progress**
- Recupera il progresso dei giocatori seguiti (placeholder)

**GET /api/trainer/players/{playerId}/details?trainerId={uuid}**
- Recupera dettagli giocatore per allenatore (verifica follow)

**POST /api/trainer/feedback**
- Aggiunge feedback per un giocatore

**Request:**
```json
{
  "trainerId": "uuid",
  "playerId": "uuid",
  "feedback": "Ottimo progresso nel tiro da 3 punti"
}
```

**GET /api/trainer/follow-count/{trainerId}**
- Conta giocatori seguiti

**GET /api/trainer/follower-count/{playerId}**
- Conta follower di un giocatore

#### TrainerService

Servizio business logic per gestione trainer:

- **followPlayer**: Segue giocatore con verifica RBAC
- **unfollowPlayer**: Smette di seguire
- **getTrainerFollows**: Lista giocatori seguiti
- **getPlayerFollowers**: Lista allenatori follower
- **isFollowingPlayer**: Verifica stato follow
- **getTrainerFollowCount**: Conta follows
- **getPlayerFollowerCount**: Conta follower
- **getTrainerStats**: Statistiche allenatore
- **getTrainerPlayersProgress**: Progresso giocatori (placeholder)
- **getPlayerDetailsForTrainer**: Dettagli giocatore (placeholder)
- **addPlayerFeedback**: Aggiunge feedback (placeholder)

#### TrainerFollowRepository

Query personalizzate:

- `findByTrainerId`: Follows di un allenatore
- `findByPlayerId`: Followers di un giocatore
- `findByTrainerAndPlayer`: Relazione specifica
- `existsByTrainerAndPlayer`: Verifica esistenza
- `countByTrainerId`: Conta follows allenatore
- `countByPlayerId`: Conta follower giocatore

#### Integrazione RBAC

Il sistema verifica che l'utente abbia ruolo TRAINER prima di permettere operazioni di follow, utilizzando `UserRoleRepository` per controllare le assegnazioni ruoli.

### 9.4 Journal
- CRUD diario allenamenti/partite
- Checklist templates
- Opzioni dinamiche

### 9.5 Training
- Programmi di allenamento
- Esercizi
- Sessioni training

### 9.6 Gamification
- Badge e achievements
- Punti e streak
- Progress obiettivi

### 9.7 Video Analysis
- Upload video
- Analisi AI
- Frame-by-frame analysis

### 9.8 Messaggistica
- Conversazioni
- Messaggi
- Partecipanti

### 9.9 Notifiche
- Push notifications (Firebase)
- Device token management
- Unread count

### 9.10 Scout
- Ricerca giocatori
- Filtri salvati
- Rankings

### 9.11 Abbonamenti
- Piano utente
- Feature access
- Limits

---

## 10. Pipeline Computer Vision

### 10.1 Object Detection

**Input:** Frame video (30/60 fps)
**Modello:** YOLOv5
**Output:** Bounding boxes per:
- basketball/ball
- hoop/basket
- player/person

### 10.2 Tracking

**Algoritmo:** Kalman Filter per tracking palla
**Output:** Posizione continua palla nel tempo

### 10.3 Shot Detection

**Regole:**
1. Palla vicino mano giocatore
2. Velocità verso l'alto > 2.0 px/s
3. Separazione palla-giocatore
4. Traietoria verso canestro

### 10.4 Made/Miss Detection

**Made:**
- Palla sopra ferro
- Attraversa area ferro
- Scende sotto ferro

**Miss:**
- Traietoria non attraversa ferro
- Rimbalzo esterno

### 10.5 Coordinate Mapping

**Omografia Prospettica:**
- Coordinate video → Coordinate campo reali
- Matrice omografia calcolata da calibrazione

---

## 11. Performance e Scalabilità

### 11.1 Requisiti Performance

- **API Response Time**: < 100ms per operazioni CRUD
- **AI Inference**: < 30ms/frame
- **Database Queries**: Indici su player_id, session_id
- **Concurrent Sessions**: Supporto multi-utente

### 11.2 Ottimizzazioni

- **Panache Repository**: Query efficienti con Hibernate
- **Lazy Loading**: Caricamento differito relazioni
- **JSONB**: Dati complessi in PostgreSQL
- **Connection Pooling**: Quarkus datasource pooling

---

## 12. Deployment

### 12.1 Build

```bash
./mvnw clean package
```

### 12.2 Dev Mode

```bash
./mvnw compile quarkus:dev
```

### 12.3 Native Image

```bash
./mvnw package -Pnative
```

### 12.4 Docker

```dockerfile
FROM quay.io/quarkus/quarkus-mandrel:22.3-java17
COPY target/*-runner /application
EXPOSE 8082
CMD ["/application/run-application.sh"]
```

---

## 13. Monitoraggio e Logging

### 13.1 Logging

- **Java Util Logging**: Logger standard
- **Log Levels**: SEVERE, WARNING, INFO
- **Structured Logging**: Messaggi informativi operazioni

### 13.2 Health Check

- **Endpoint**: GET /mvpiq
- **Status**: Basic health check

---

## 14. Criticità e Soluzioni

### 14.1 Motion Blur Palla

**Problema:** Palla sfocata in movimento rapido
**Soluzione:** 
- FPS alti (60 fps)
- Tracking predittivo con Kalman filter
- Interpolazione traiettoria

### 14.2 Luce Palestra

**Problema:** Illuminazione variabile
**Soluzione:**
- Augment dataset con diverse condizioni
- Preprocessing immagine (normalizzazione)
- Modello robusto a variazioni luce

### 14.3 Occlusioni

**Problema:** Palla/ferro occluso da giocatore
**Soluzione:**
- Tracking temporale multi-frame
- Interpolazione traiettoria
- Predizione posizione

---

## 15. Estensioni Future

### 15.1 Multiplayer

- Tracking multi-giocatori
- Eventi multipli simultanei
- Analytics comparativi

### 15.2 Coach Dashboard

- Web app per statistiche squadra
- Comparazione giocatori
- Report dettagliati

### 15.3 AI Coaching

- Suggerimenti miglioramento
- Analisi pattern
- Allenamenti personalizzati

---

## 17. Fix Applicati (Maggio 2026)

### 17.1 Correzioni Database e Entità

**PlayerPosition Entity:**
- **Problema**: Errore SQL `column p1_0.profile_id does not exist`
- **Causa**: Mismatch tra nome colonna database (`player_id`) e definizione entità (`profile_id`)
- **Soluzione**: Aggiornato `@JoinColumn(name = "player_id")` e unique constraint

### 17.2 Correzioni API Endpoints

**Player CV Endpoint:**
- **Problema**: Errore 404 su `/api/athlet/{playerId}/cv`
- **Causa**: Path mismatch tra client (`/api/athlet/`) e server (`/api/players/`)
- **Soluzione**: Allineato client a usare `/api/players/{playerId}/cv`

**Auto-creazione CV:**
- **Problema**: 404 quando CV non esiste
- **Soluzione**: Modificato `PlayerCvService.getCv()` per creare CV vuoto automaticamente

### 17.3 Correzioni Query Hibernate

**PlayerCvRepository:**
- **Problema**: `Could not interpret path expression 'player.id'`
- **Causa**: Query Panache non funzionava con path expression complessi
- **Soluzione**: Aggiunto metodo `findByPlayerIdColumn()` con query diretta su colonna

**PlayerCvTeamRepository:**
- **Problema**: Path expression errata per accedere al player
- **Causa**: `PlayerCvTeam` → `PlayerCv` → `Player` richiede path `cv.player.id`
- **Soluzione**: Aggiornato query a `"cv.player.id"` e aggiunto metodo alternativo

### 17.4 Correzioni Navigazione

**Sezioni PLAYER:**
- **Problema**: Utenti PLAYER vedevano 0 sezioni di navigazione
- **Causa**: Database non conteneva sezioni configurate per ruolo PLAYER
- **Soluzione**: Creato migration `V1.7__Add_Player_Navigation_Sections.sql` con sezioni base (Home, Profile, Goals, Training, Journal, Statistics)

### 17.5 Correzioni Enum RBAC

**UserRole Constants:**
- **Problema**: `No enum constant com.mvpiq.enums.UserRole.GUEST`
- **Causa**: Enum constants in minuscolo non corrispondevano a valori database
- **Soluzione**: Convertito tutti i valori enum in maiuscolo (ADMIN, TRAINER, PLAYER, SCOUT, CREATOR, GUEST)

### 17.6 Correzioni DTO

**WorkoutSessionResponse Package:**
- **Problema**: `Cannot resolve symbol 'WorkoutSessionResponse'` in WorkoutService.java
- **Causa**: WorkoutSessionResponse.java mancava della dichiarazione del package
- **Soluzione**: Aggiunto `package com.mvpiq.dto;` all'inizio del file

**PlayerCvHighlightDTO ExternalUrl:**
- **Problema**: `Cannot resolve method 'getExternalUrl' in 'MediaAsset'` a linea 28
- **Causa**: Il codice cercava di ottenere `externalUrl` da `MediaAsset`, ma questo campo esiste sull'entità `PlayerCvHighlight`, non su `MediaAsset`
- **Soluzione**: Spostato `externalUrl = h.getExternalUrl()` fuori dal check null di media, poiché è un campo diretto di PlayerCvHighlight

---

## 18. Migrazione Database - Maggio 2026

### 18.1 Panoramica Modifiche

Migration SQL applicata per migliorare lo schema del database con nuove funzionalità, indici, constraint e campi di audit.

**Estensioni PostgreSQL:**
- `pgcrypto`: Funzioni crittografiche
- `citext`: Case-insensitive text

### 18.2 Dettaglio Modifiche per Entità

#### User
**Nuovi campi:**
- `updated_at`: Timestamp ultimo aggiornamento (con trigger automatico)
- `deleted_at`: Timestamp soft delete
- `status`: Stato utente (ACTIVE, SUSPENDED, DELETED) con default ACTIVE

**Constraint aggiunti:**
- `users_status_ck`: Verifica valori status validi

**Indici aggiunti:**
- `idx_users_email`, `idx_users_username`, `idx_users_status`, `idx_users_created_at`

#### Player
**Nuovi campi:**
- `wingspan_cm`: Apertura alare in cm (Short)
- `vertical_jump_cm`: Verticale in cm (Short)
- `preferred_position_id`: FK a player_position_metadata

**Constraint aggiunti:**
- `players_preferred_position_fk`: Foreign key a position_metadata
- `players_height_ck`: Verifica altezza tra 50-300cm
- `players_weight_ck`: Verifica peso tra 20-300kg
- `players_age_ck`: Verifica età tra 1-100 anni

**Indici aggiunti:**
- `idx_players_city`, `idx_players_level`, `idx_players_preferred_position`

#### JournalEntry
**Nuovi campi:**
- `checklist_completed`: Boolean per completamento checklist (default false)
- `tags`: JSONB per tagging flessibile
- `deleted_at`: Timestamp soft delete

**Constraint aggiunti:**
- `journal_entries_mood_ck`: Verifica mood rating 1-5
- `journal_entries_performance_ck`: Verifica performance rating 1-5
- `journal_entries_duration_ck`: Verifica durata >= 0

**Indici aggiunti:**
- `idx_journal_entries_tags`: GIN index per query JSONB

#### ChecklistTemplateItem
**Nuovi campi:**
- `placeholder`: Placeholder per input (varchar 255)
- `help_text`: Testo di aiuto (TEXT)
- `validation_rules`: JSONB per regole di validazione dinamiche

**Indici aggiunti:**
- `idx_checklist_template_items_template`

#### AthleteGoal
**Nuovi campi:**
- `priority`: Priorità obiettivo (LOW, MEDIUM, HIGH) con default MEDIUM
- `progress_percentage`: Progresso percentuale (numeric 5,2) con default 0

**Constraint aggiunti:**
- `athlete_goals_priority_ck`: Verifica valori priority validi
- `athlete_goals_progress_ck`: Verifica progresso 0-100

**Indici aggiunti:**
- `idx_athlete_goals_status`, `idx_athlete_goals_due_date`

#### TrainingProgram
**Nuovi campi:**
- `estimated_duration_minutes`: Durata stimata in minuti
- `difficulty`: Difficoltà (BEGINNER, INTERMEDIATE, ADVANCED)
- `tags`: JSONB per tagging programmi
- `published_at`: Timestamp pubblicazione

**Constraint aggiunti:**
- `training_programs_difficulty_ck`: Verifica valori difficulty validi

**Indici aggiunti:**
- `idx_training_programs_tags`: GIN index per query JSONB

#### TrainingSession
**Nuovi campi:**
- `calories_burned`: Calorie bruciate
- `average_heart_rate`: Frequenza cardiaca media
- `perceived_effort': Sforzo percepito 1-10 (Short)

**Constraint aggiunti:**
- `training_sessions_effort_ck`: Verifica sforzo 1-10

**Indici aggiunti:**
- `idx_training_sessions_program`, `idx_training_sessions_date`

#### MediaAsset
**Nuovi campi:**
- `mime_type`: Tipo MIME (varchar 100)
- `file_size_bytes`: Dimensione file in bytes
- `visibility`: Visibilità (PRIVATE, PUBLIC, UNLISTED) con default PRIVATE

**Constraint aggiunti:**
- `media_assets_visibility_ck`: Verifica valori visibility validi

**Indici aggiunti:**
- `idx_media_visibility`

#### Exercise
**Nuovi campi:**
- `equipment`: JSONB per lista attrezzature
- `tags`: JSONB per tagging esercizi
- `calories_estimate`: Stima calorie

**Indici aggiunti:**
- `idx_exercises_tags`: GIN index per query JSONB

#### Message
**Nuovi campi:**
- `edited_at`: Timestamp ultima modifica
- `deleted_at`: Timestamp soft delete
- `reply_to_message_id`: FK self-reference per thread messaggi

**Constraint aggiunti:**
- `messages_reply_fk`: Foreign key self-reference con ON DELETE SET NULL

**Indici aggiunti:**
- `idx_messages_conversation_created`

#### Notification
**Modifiche:**
- `created_at`: Tipo cambiato a timestamptz
- `is_read`: Boolean per lettura (default false)

**Indici aggiunti:**
- `idx_notifications_user`, `idx_notifications_read`

#### VideoAnalysisSession
**Nuovi campi:**
- `error_message`: Messaggio di errore (TEXT)
- `retry_count`: Contatore retry (default 0)

**Constraint aggiunti:**
- `video_analysis_sessions_status_ck`: Verifica status (UPLOADED, PROCESSING, COMPLETED, FAILED)

**Indici aggiunti:**
- `idx_video_analysis_sessions_status`

#### ShotEvent
**Nuovi campi:**
- `shot_zone`: Zona tiro (varchar 50)
- `release_time_ms`: Tempo rilascio in ms

**Indici aggiunti:**
- `idx_shot_events_zone`

#### WorkoutSession
**Nuovi campi:**
- `notes`: Note sessione (TEXT)
- `average_shot_distance`: Distanza media tiri
- `workout_score`: Punteggio sessione (numeric 5,2)

**Indici aggiunti:**
- `idx_workout_sessions_player_status`

#### Badge
**Nuovi campi:**
- `rarity`: Rarità (COMMON, RARE, EPIC, LEGENDARY) con default COMMON
- `active`: Boolean per attivazione (default true)

**Constraint aggiunti:**
- `badges_rarity_ck`: Verifica valori rarity validi

**Indici aggiunti:**
- `idx_badges_category`

### 18.3 Miglioramenti Architetturali

**Uniformazione Audit:**
- `updated_at` aggiunto dove mancante con trigger automatico
- `deleted_at` per soft delete su entità principali
- `status` per gestione stati applicativi

**Performance Query:**
- Indici su tutte le foreign keys principali
- Indici su campi di ricerca frequenti
- GIN indexes su campi JSONB per query efficienti

**Integrità Dati:**
- CHECK constraints su tutti i campi numerici
- Foreign key constraints con appropriate cascade rules
- Default values per campi obbligatori

**Tagging System:**
- Supporto JSONB + GIN per tagging flessibile
- Applicato a: JournalEntry, TrainingProgram, Exercise

**Messaging:**
- Supporto thread con reply_to_message_id
- Tracking edit/delete con timestamp

**Video Analysis:**
- Miglioramento tracking errori con error_message e retry_count
- Status constraint per stati validi

### 18.4 Trigger Automatici

Trigger `update_updated_at_column` applicati a:
- `users`
- `training_programs`
- `training_sessions`
- `messages`

Questi trigger aggiornano automaticamente `updated_at` su ogni UPDATE.

### 18.5 Note Architetturali

**Scalabilità:**
- Indici ottimizzati per query dashboard e feed
- JSONB per dati flessibili senza alterare schema
- Soft delete per preservare dati storici

**Mantenibilità:**
- Constraint database per validazione a livello di DB
- Uniformazione naming e tipi
- Documentazione inline tramite commenti SQL

---

## 19. Stato Attuale Sistema

### 19.1 Funzionalità Verificate

✅ **Autenticazione**: Login con JWT funzionante  
✅ **RBAC**: Sistema ruoli multipli operativo  
✅ **Navigazione**: Sezioni correttamente configurate per PLAYER  
✅ **Player Profile**: Caricamento e aggiornamento profili  
✅ **Player CV**: Creazione automatica e gestione CV  
✅ **Positions**: Gestione posizioni giocatori  
✅ **Goals**: Sistema obiettivi atleta  
✅ **Database Migration**: Migrazione Maggio 2026 applicata con successo  

### 19.2 Architettura Stabile

- **Backend Quarkus**: Performance ottimali
- **Database PostgreSQL**: Schema migliorato con indici e constraint
- **Sicurezza**: JWT + RBAC completo
- **API REST**: Endpoints allineati e funzionanti
- **Entità Java**: Tutte aggiornate per riflettere migration database

---

## 20. CV Sportivo Pubblico Condivisibile con Highlights

### 20.1 Obiettivo della Funzionalità

Consentire ai giocatori di creare un CV sportivo digitale professionale, condivisibile tramite link pubblico permanente, arricchito con highlights video e informazioni sportive rilevanti.

La funzionalità trasforma il profilo atleta da semplice area personale interna ad uno strumento reale di recruiting sportivo.

Il CV deve poter essere:
- visualizzato senza autenticazione
- condiviso via link
- consultabile da mobile e desktop
- professionale nella presentazione
- facilmente aggiornabile dal giocatore

### 20.2 Obiettivi Business

- Aumentare retention utenti
- Incentivare compilazione profilo
- Favorire condivisione organica
- Creare valore recruiting
- Migliorare percezione premium della piattaforma

### 20.3 Attori Coinvolti

| Attore | Descrizione |
|--------|-------------|
| Giocatore | Gestisce il proprio CV |
| Scout | Consulta il CV pubblico |
| Coach | Analizza atleta e highlights |
| Squadra | Riceve CV condiviso |
| Sistema | Genera link e serve contenuti pubblici |

### 20.4 Scope MVP

**Incluso:**
- Profilo pubblico (dati anagrafici, fisico, ruolo, carriera squadre, statistiche, highlights video)
- Condivisione (generazione link pubblico, copia link, condivisione mail/social)
- Highlights (upload video piccoli, supporto link YouTube/Vimeo, gestione multipla clip)
- Pagina pubblica (responsive, mobile first, accessibile senza login)

**Escluso MVP:**
- Analytics recruiter
- Tracking visualizzazioni
- PDF export
- AI tagging highlights
- Commenti recruiter
- Chat diretta scout/player

### 20.5 User Journey

**5.1 Creazione CV**
Il giocatore accede alla sezione CV, inserisce informazioni sportive, salva il CV.

**5.2 Gestione Highlights**
Il giocatore può caricare video MP4, aggiungere link YouTube/Vimeo, eliminare highlights, riordinare highlights.

**5.3 Condivisione**
Il giocatore preme "Condividi CV", il sistema genera token pubblico, viene mostrato link condivisibile.

Esempio: `https://app.domain.com/public/cv/uuid-token`

**5.4 Visualizzazione Pubblica**
Lo scout apre il link, visualizza il profilo atleta, guarda highlights, consulta statistiche e carriera. Senza login.

### 20.6 Requisiti Funzionali

| ID | Requisito |
|----|-----------|
| RF-01 | Il giocatore può creare/modificare il CV |
| RF-02 | Il giocatore può condividere il CV |
| RF-03 | Il sistema genera un token pubblico univoco |
| RF-04 | Il link pubblico è accessibile senza autenticazione |
| RF-05 | Il giocatore può revocare la condivisione |
| RF-06 | Il giocatore può caricare highlights |
| RF-07 | Il giocatore può usare link esterni |
| RF-08 | Il CV pubblico mostra highlights |
| RF-09 | Il sistema supporta più highlights |
| RF-10 | La pagina pubblica è responsive |
| RF-11 | Gli highlights possono essere ordinati |
| RF-12 | Il link pubblico è permanente |

### 20.7 Requisiti Non Funzionali

| Categoria | Requisito |
|-----------|-----------|
| Performance | Pagina caricata < 2 sec |
| Sicurezza | Token UUID non predicibili |
| Mobile | UI ottimizzata smartphone |
| Scalabilità | Supporto storage CDN |
| UX | Esperienza professionale |
| Compatibilità | Browser mobile e desktop |

### 20.8 Architettura Generale

**Backend:**
- Quarkus REST API
- PostgreSQL (Supabase)
- Supabase Storage

**Frontend App:**
- Gestione CV
- Upload highlights
- Condivisione link

**Frontend Pubblico:**
- Pagina web pubblica
- Rendering CV condivisibile

### 20.9 Modello Dati

#### Tabelle Coinvolte

| Tabella | Stato |
|---------|-------|
| users | esistente |
| players | esistente |
| player_cv | estesa |
| player_cv_teams | esistente |
| player_cv_highlights | estesa |
| media_assets | estesa |

### 20.10 Evoluzione Schema Database

#### player_cv

**Nuovi campi:**
- `share_token` (uuid)
- `share_enabled` (boolean)
- `public_updated_at` (timestamptz)
- `public_slug` (varchar 100)

**Responsabilità:**
- Gestione pubblicazione CV
- Accesso pubblico
- Invalidazione link

#### player_cv_highlights

**Nuovi campi:**
- `external_url` (text)
- `sort_order` (int)
- `thumbnail_url` (text)

**Modello Supportato:**
- Upload interno: media_id != null, external_url = null
- Video esterno: media_id = null, external_url != null

#### media_assets

**Nuovi metadata storage:**
- storage_provider
- storage_bucket
- storage_path
- external_url: URL esterno per video (YouTube, Vimeo, Hudl) - aggiunto con migration CV

### 20.11 Storage Video

**Strategia scelta:** Supabase Storage

**Bucket:** cv-highlights

**Limiti upload:**
| Parametro | Valore |
|-----------|--------|
| Max size | 20 MB |
| Max durata | 90 sec |
| Formato | MP4 |
| Codec | H264 |
| Max highlights | 5 |

**Motivazioni:**
- Controllo costi
- Upload rapidi
- UX mobile
- Storage sostenibile

### 20.12 Gestione Highlights

#### Upload Video

**Flow:**
1. Frontend richiede upload
2. Backend genera metadata
3. Frontend upload diretto Supabase
4. Backend salva media asset

#### Link Esterni

**Supportati:**
- YouTube
- Vimeo
- Hudl

### 20.13 API Backend

#### Condivisione CV

**POST /players/{id}/cv/share**

Genera token pubblico e URL condivisibile.

#### Revoca condivisione

**DELETE /players/{id}/cv/share**

Disabilita accesso pubblico.

#### CV pubblico

**GET /public/cv/{shareToken}**

Pubblico, senza auth. Restituisce dati player, statistiche, carriera, highlights.

#### Upload Highlight

**POST /players/{id}/cv/highlights**

Upload metadata.

#### Highlight esterno

**POST /players/{id}/cv/highlights/link**

Aggiunge URL esterno.

#### Eliminazione Highlight

**DELETE /players/{id}/cv/highlights/{highlightId}**

### 20.14 Sicurezza

**Token pubblico:**
- Usare UUID v4
- Mai ID incrementali

**Validazioni:**
- Verificare ownership CV
- Verificare mime type
- Verificare file size
- Verificare max numero highlights

**Privacy:**
Il CV pubblico espone dati personali. Serve:
- Consenso esplicito
- Toggle pubblicazione

### 20.15 Frontend Mobile

#### CvScreen

**Nuove sezioni:**
- Preview CV
- Highlights
- Stato condivisione
- Share button

#### EditCvScreen

**Supporta:**
- Upload
- Link esterni
- Reorder
- Delete

#### Share Sheet

**Azioni:**
- Copia link
- Mail
- WhatsApp
- Share OS native

### 20.16 Pagina Pubblica Web

**Obiettivo UX:** Esperienza simile a LinkedIn athlete profile, Hudl profile, recruiting card.

**Contenuti:**

**Header:**
- Foto
- Nome
- Età
- Ruolo
- Fisico

**Carriera:**
- Timeline squadre

**Statistiche:**
- Card responsive

**Highlights:**
- Player video embedded

### 20.17 Performance

**Ottimizzazioni:**

**Video:**
- Thumbnail preview
- Preload metadata
- Lazy loading

**API:**
- Cache-control
- ETag
- Query ottimizzate

### 20.18 SEO e Sharing

Aggiungere:
- OpenGraph
- Twitter cards

Per preview su WhatsApp, LinkedIn, Telegram.

### 20.19 Architettura Consigliata MVP

**Backend:**
- Quarkus
- PostgreSQL Supabase
- Supabase Storage

**Frontend:**
- App mobile esistente
- Pagina pubblica React/web

**Video:**
- Upload piccoli
- Supporto link esterni

### 20.20 Roadmap Implementativa

**FASE 1 — Database:**
- Migration schema
- Indici
- Constraint

**FASE 2 — Backend:**
- Endpoint sharing
- Endpoint pubblico
- Gestione highlights

**FASE 3 — Frontend App:**
- UI CV
- Upload highlights
- Sharing

**FASE 4 — Pagina Pubblica:**
- Rendering CV
- Player video
- Responsive UI

### 20.21 Evoluzioni Future

**Recruiting:**
- Contatta atleta
- Invito tryout

**Analytics:**
- Visualizzazioni CV
- Click highlights

**Media:**
- Thumbnail automatica
- Compressione server-side

**Export:**
- PDF CV
- QR Code

### 20.22 Conclusione Funzionalità

La funzionalità introduce un vero profilo atleta pubblico professionale, trasformando il CV da semplice archivio interno a strumento concreto di scouting e recruiting.

L'architettura scelta:
- Riutilizza il dominio esistente
- Minimizza complessità
- Sfrutta Supabase Storage
- Consente evoluzioni future senza refactor strutturali

---

## 21. Conclusioni

Il backend MVPiQ Hoops è un'applicazione Quarkus completa e ben strutturata che implementa tutte le funzionalità richieste per il tracking dei tiri di basket, incluse le API del punto 21 della specifica tecnica.

**Punti di Forza:**
- Architettura a strati chiara e manutenibile
- Integrazione AI/ML avanzata (DJL, YOLOv5)
- API REST complete e documentate
- Sicurezza robusta (JWT, RBAC-based)
- Performance ottimizzate
- Database PostgreSQL con JSONB per flessibilità
- Sistema RBAC completo per gestione ruoli multipli

**Stato Implementazione:**
- ✅ Tutte le API tracking tiri implementate
- ✅ Servizi AI per rilevamento automatico
- ✅ Analytics e statistiche complete
- ✅ Calibrazione campo
- ✅ Integrazione mobile pronta
- ✅ Sistema RBAC completo implementato
- ✅ Multi-role support per utenti

**Migrazioni Recenti (Maggio 2026):**
- ✅ Rimozione entità `PlayerProfile` in favore di `Player`
- ✅ Rimozione campi `role`, `is_creator`, `is_trainer` da `users`
- ✅ Implementazione sistema RBAC con tabelle `roles` e `user_roles`
- ✅ Nuove entità `Role` e `UserRoleAssignment`
- ✅ Aggiornamento tutti i servizi per RBAC
- ✅ Aggiornamento enum `UserRole` a valori maiuscoli
- ✅ Rinomino tabella `player_profile_positions` in `player_positions`
- ✅ Fix PlayerPosition entity per usare `player_id` invece di `profile_id`
- ✅ Fix endpoint path CV da `/api/athlet/{id}/cv` a `/api/players/{id}/cv`
- ✅ Implementazione auto-creazione CV vuoto quando non esiste
- ✅ Fix query Hibernate per CV e CV teams
- ✅ Creazione sezioni navigazione per ruolo PLAYER
- ✅ Migration database miglioramenti: nuovi campi, indici, constraint
- ✅ Aggiornamento 15 entità Java per riflettere migration database
- ✅ Implementazione tagging system con JSONB + GIN indexes
- ✅ Aggiunta soft delete e audit fields su entità principali
- ✅ Miglioramento sistema messaging con thread support
- ✅ Implementazione sistema rarità badge e gamification avanzata

Il backend è pronto per l'integrazione con l'app mobile React Native già sviluppata.
