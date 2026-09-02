# AGENTS.md

## What this is

Spring Boot 4.1.0 / Java 21 REST app. Single Maven module, wrapper-only (`./mvnw` — no system Maven). Flat package `com.voidirl.jobscheduler`: Controller → Service → Spring Data JPA repository → PostgreSQL. Currently a job-store CRUD API under `/api/jobs`; there is no actual scheduling/execution engine yet.

## Commands

- All tests: `./mvnw test`
- One test class/method: `./mvnw test -Dtest=ClassName` (append `#method`)
- Run app: `./mvnw spring-boot:run`
- Verification = `./mvnw test`. No lint/format/typecheck step exists.

## Gotchas

- **Tests need live PostgreSQL.** `@SpringBootTest` boots the full context against `localhost:5432/job_scheduler_db` (user `jobscheduler_user`). No H2/embedded fallback — tests fail without the DB running.
- **Engine concurrency model:** `JobExecutionEngine.runDueJobs` runs in one transaction and claims due jobs with `FOR UPDATE` (pessimistic lock), flipping them to `TRIGGERED` before handing them to a fixed-size `ExecutorService` (size = `jobs.executor.pool-size`, default 4). Workers then own each row exclusively (`RUNNING` → terminal). Don't move the claim outside the transaction without re-adding the lock story.
- **Dead-letter:** jobs that exhaust `maxRetries` end in `DEAD_LETTERED` (exposed via `GET /api/jobs/dead-letter`); `FAILED` remains a reserved enum state but the engine no longer produces it.
- **Enum changes need manual DB work:** `ddl-auto=update` never alters an existing PostgreSQL CHECK constraint on an `@Enumerated(STRING)` column. When `DEAD_LETTERED` was added, writes failed with `jobs_status_check` violations until `ALTER TABLE jobs DROP CONSTRAINT jobs_status_check` was run manually. Do not drop it again; Hibernate won't recreate it automatically.
- **Never commit `src/main/resources/application.properties`.** It holds local DB credentials and is gitignored on purpose. Do not remove the ignore rule.
- **Schema is Hibernate-managed** via `spring.jpa.hibernate.ddl-auto=update`. No Flyway/Liquibase — entity annotation changes alter the schema on next boot.
- `HELP.md` is gitignored Spring Initializr boilerplate; ignore it.

## Working agreement (learning mode)

The user directs design; the agent writes code. The user is learning system design and architecture, not syntax.

- Before implementing, briefly state what will be built, which library/tool, and why vs alternatives.
- Explain architectural concepts as they come up (caching, auth flow, DB choices, etc.) — assume the user knows the concept name, not the mechanics.
- Do not show raw code unless explicitly asked.
- After finishing a feature, give a 3–5 bullet plain-English summary of what was built and how the pieces connect.
- Frame feature summaries for a placement interview: say what was built, which library/tool and why vs alternatives, and the core concept (e.g. thread pool sizing, claim-based locking, dead-letter queue).
