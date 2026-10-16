# Towards

Towards is a public transport app for Android, iOS and desktop. It is built with Kotlin Multiplatform and Compose Multiplatform. Its Ktor backend gets routing, timetable and geocoding data from [MOTIS](https://github.com/motis-project/motis), using the public [Transitous](https://transitous.org) instance by default.

With the app you can:

- Browse a MapLibre map of nearby stops, live departures grouped by mode, and line previews.
- Search for places and plan trips, then see the itineraries on a map.
- Save favourite places as one-tap shortcuts.
- Switch between light and dark themes, and change the backend endpoint in Settings.



## How it works

```
Android / iOS / Desktop app  ──HTTP──▶  Ktor server  ──HTTP──▶  MOTIS/OTP 2
     (app/shared)                        (server)
          └── shared models & DTOs (core) ──┘
```

The clients never call MOTIS directly. The server turns MOTIS responses into Towards' own domain model. It also handles rate limiting, Prometheus metrics, structured logging, and an optional PostgreSQL audit log.

## Project layout


| Module                                                                                             | Contents                                                                            |
| -------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------- |
| `[core](core/src/commonMain/kotlin)`                                                               | Domain models, ports, and API DTOs shared by the server and the apps                |
| `[server](server/src/main/kotlin)`                                                                 | Ktor backend: HTTP routes, MOTIS client, config, and the optional database          |
| `[app/shared](app/shared/src/commonMain/kotlin)`                                                   | Shared Compose UI, view models, HTTP clients, and settings storage                  |
| `[app/androidApp](app/androidApp)`, `[app/desktopApp](app/desktopApp)`, `[app/iosApp](app/iosApp)` | Platform entry points                                                               |
| `[docs](docs)`                                                                                     | MOTIS OpenAPI spec, Bruno collections, and the [iOS build guide](docs/build-ios.md) |
| `[.bruno](.bruno)`                                                                                 | Bruno requests for the Towards API                                                  |




## Getting started

You need JDK 25. Gradle provisions it automatically through toolchains. You also need Android Studio or IntelliJ IDEA for the apps, and Xcode for iOS.

### 1. Run the backend

```sh
./gradlew :server:run
```

You can also use Docker:

```sh
cp .env.example .env
docker compose up --build
```

The server listens on `http://localhost:8081`. Run `curl localhost:8081/health` to check that it's up.

### 2. Run an app


| Target  | Command                                                                                                            |
| ------- | ------------------------------------------------------------------------------------------------------------------ |
| Desktop | `./gradlew :app:desktopApp:run`, or `:app:desktopApp:hotRun --auto` for hot reload                                 |
| Android | `./gradlew :app:androidApp:assembleDebug`, or use the IDE run configuration                                        |
| iOS     | Open `[app/iosApp](app/iosApp)` in Xcode and run it. For device builds, see [docs/build-ios.md](docs/build-ios.md) |


Each platform has its own default backend URL. You can change it in the app under **Settings → API endpoint**.


| Platform         | Default backend URL                                  |
| ---------------- | ---------------------------------------------------- |
| Desktop          | `http://127.0.0.1:8081`                              |
| Android emulator | `http://10.0.2.2:8081`                               |
| iOS              | A LAN address. Point it at your machine's IP address |




## Configuration

The server reads `[application.conf](server/src/main/resources/application.conf)`, and environment variables override its values:


| Variable                                             | Default                      | Purpose                                                             |
| ---------------------------------------------------- | ---------------------------- | ------------------------------------------------------------------- |
| `PORT`                                               | `8081`                       | HTTP port                                                           |
| `MOTIS_BASE_URL`                                     | `https://api.transitous.org` | MOTIS instance to query                                             |
| `MOTIS_REQUEST_TIMEOUT_MS`                           | `15000`                      | Timeout for upstream requests                                       |
| `RATE_LIMIT_REQUESTS` / `RATE_LIMIT_PERIOD_SECONDS`  | `120` / `60`                 | Rate limit for each client IP address                               |
| `DATABASE_ENABLED`                                   | `false`                      | Turns on the PostgreSQL audit log. Migrations run through Liquibase |
| `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD` | —                            | Database connection                                                 |
| `AUDIT_RETENTION_DAYS`                               | `30`                         | How long audit records are kept                                     |


To start PostgreSQL with Docker Compose, set `DATABASE_ENABLED=true` and `COMPOSE_PROFILES=database` in `.env`.

## API

All endpoints are under `/api/v1`. Coordinates use the `lat,lon[,level]` format.


| Method & path                                                 | Description                                                    |
| ------------------------------------------------------------- | -------------------------------------------------------------- |
| `GET /trips/plan?from=&to=`                                   | Plan a journey. `from` and `to` can be stop IDs or coordinates |
| `GET /trips/{tripId}`                                         | Get details for a trip                                         |
| `POST /itineraries/refresh`                                   | Refresh an itinerary with real-time data                       |
| `GET /stop-times`                                             | Get departures and arrivals for a stop, or around a point      |
| `GET /geocode?text=` / `GET /reverse-geocode?place=`          | Search for places                                              |
| `GET /map/initial`, `/map/stops`, `/map/trips`, `/map/levels` | Get map data                                                   |


The operational endpoints are `GET /health` and `GET /metrics` (Prometheus format). You can find ready-made requests in `[.bruno](.bruno)`.

## Tests

```sh
./gradlew :core:jvmTest :server:test         # Backend
./gradlew :app:shared:jvmTest                # Shared app logic (desktop JVM)
./gradlew :app:shared:testAndroidHostTest    # Android host tests
./gradlew :app:shared:iosSimulatorArm64Test  # iOS simulator tests
```

On every push and pull request that touches the backend, CI runs the backend tests and builds the Docker image (`[.github/workflows/backend.yml](.github/workflows/backend.yml)`). Pushes to `main` also publish the image to `ghcr.io`.