## wiremock-service

Standalone WireMock server used to mock downstream AMP endpoints for local/dev testing.

### Run

```bash
WIREMOCK_PORT=8089 ../gradlew :wiremock-service:run
```

Docker Compose builds this module (`wiremock-service/Dockerfile`); the stock `wiremock/wiremock` image does not run this Java.

### Endpoints mocked

- `GET /courthouses/{court_id}`
- `GET /courthouses/{court_id}/courtrooms/{court_room_id}`
- `GET /pcd/cases/{case_urn}`
- `GET /case/{case_urn}/courtschedule`
- `POST /{tenant_id}/oauth2/v2.0/token` (for local OAuth when `TMC_TOKEN_URL` points here)

### Court schedule mock URN format

The `case_urn` supports the same dynamic format you had in the in-process mocks:

- Prefix `TMC`
- Hearing type: `TR` (Trial) or `SE` (Sentence)
- Optional offset:
  - `{n}M` months in future, `N{n}M` months in past
  - `{n}D` days in future, `N{n}D` days in past
- Optional trailing digits \(\(\(\ge 2\)\)\) to set the number of sittings (multi-day hearing)

Examples:

- `TMCTR0D` (today, trial, 1 sitting)
- `TMCTRN1D2` (trial started 1 day ago, 2 sittings)
- `TMCSE2M3D5` (sentence in 2 months 3 days, 5 sittings)

