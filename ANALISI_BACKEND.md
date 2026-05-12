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

### 4.2 Shot Events

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
- Follow/Unfollow giocatori
- Feedback e progress
- Statistiche allenatore

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

---

## 18. Stato Attuale Sistema

### 18.1 Funzionalità Verificate

✅ **Autenticazione**: Login con JWT funzionante  
✅ **RBAC**: Sistema ruoli multipli operativo  
✅ **Navigazione**: Sezioni correttamente configurate per PLAYER  
✅ **Player Profile**: Caricamento e aggiornamento profili  
✅ **Player CV**: Creazione automatica e gestione CV  
✅ **Positions**: Gestione posizioni giocatori  
✅ **Goals**: Sistema obiettivi atleta  

### 18.2 Architettura Stabile

- **Backend Quarkus**: Performance ottimali
- **Database PostgreSQL**: Schema consistente
- **Sicurezza**: JWT + RBAC completo
- **API REST**: Endpoints allineati e funzionanti

---

## 19. Conclusioni

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

Il backend è pronto per l'integrazione con l'app mobile React Native già sviluppata.
