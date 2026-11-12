# Hotfix notes — Room/Tenant JSON serialization of LAZY Property proxy

Branch: `hotfix/room-property-serialization`
Worktree root: `C:\devWork\tms\.worktrees\hotfix-room-property-serialization`

## Problem

`Room.property` is `@ManyToOne(fetch = FetchType.LAZY)`. `RoomController` (`/rooms`) returns raw
`Room` entities and `TenantController` (`/tenants`) returns raw `Tenant` entities (Tenant has an
`@OneToOne Room`). When Hibernate has not initialized `room.property`, it hands Jackson a
`Property$HibernateProxy` (ByteBuddy). Jackson has no serializer for the proxy's
`hibernateLazyInitializer`/`handler` internals and throws `InvalidDefinitionException`, producing an
HTTP 500 on every `/rooms` and `/tenants` response. The data is intact — this is purely a
serialization failure. No DB changes were made.

## Change

Followed the existing per-entity annotation pattern already used in the codebase on
`GateRemote` and `TicketComment` (both annotate their lazy `@ManyToOne` fields with
`@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})`). There is no
`jackson-datatype-hibernate` dependency, no `Hibernate6Module` registration, and no custom
`ObjectMapper` bean in `src/main`, so the annotation is the established, minimal fix.

- `src/main/java/za/co/tms/domain/Property.java`
  - Added import `com.fasterxml.jackson.annotation.JsonIgnoreProperties`.
  - Added type-level annotation `@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})`.
  - Annotated the `Property` **type** rather than each referencing field so every serialization path
    that reaches a `Property` proxy is covered at once — both `Room["property"]` and
    `Tenant["room"]["property"]`.
  - `leasePdfData` keeps its existing `@JsonIgnore` + `@Schema(hidden=true)`; the lease blob stays
    hidden. `@JsonIgnoreProperties` only suppresses the two Hibernate proxy internal accessors.

No changes to `Room`, `Tenant`, `pom.xml`, the database, or any global ObjectMapper config.
`FAIL_ON_EMPTY_BEANS` was NOT disabled.

## Regression test

- `src/test/java/za/co/tms/repository/RoomLazyPropertySerializationTest.java`
  - `@DataJpaTest` (mirrors the existing `PropertyRepositoryTest` setup) that saves a `Property` and a
    linked `Room`, flushes, then `em.clear()`s the persistence context so the reloaded `Room` carries a
    genuine uninitialized ByteBuddy proxy for `room.property`.
  - Serializes the reloaded `Room` with a real Jackson `ObjectMapper`
    (`new ObjectMapper().findAndRegisterModules()`) WITHOUT touching `getProperty()` first (touching it
    would initialize the proxy and hide the bug).
  - Asserts serialization succeeds, the JSON contains the property `streetAddress`, and does NOT
    contain `hibernateLazyInitializer` or `leasePdfData`.
  - This closes the gap that let the original bug ship: prior tests used DTOs / hand-built entities and
    never serialized a real Hibernate proxy.

## Verification

Commands run from the worktree root (corporate Nexus TLS workaround was NOT needed — dependency
resolution succeeded directly).

- `mvnw.cmd -q compile` → BUILD SUCCESS (exit 0).
- `mvnw.cmd -q -Dtest=RoomLazyPropertySerializationTest test` (with fix applied) → PASS.
- Fail-without-fix proof: temporarily removed the `@JsonIgnoreProperties` annotation from `Property`,
  re-ran the test → FAILED with:
  `com.fasterxml.jackson.databind.exc.InvalidDefinitionException: No serializer found for class
  org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor ... (through reference chain:
  za.co.tms.domain.Room["property"]->za.co.tms.domain.Property$HibernateProxy$...["hibernateLazyInitializer"])`
  — the exact reported error. Annotation restored, test passes again.
- `mvnw.cmd -q test` (full suite) → **100 tests, 0 failures, 0 errors, 0 skipped** across 19 test
  classes, including `TenantControllerTest`, `PropertyRepositoryTest`, `LeaseAgreement`-related tests,
  and the new `RoomLazyPropertySerializationTest`. No regressions.

## Commit scope

Two source files only: `Property.java` (one import + one annotation) and the new test. No DB changes,
no `pom.xml` changes, no settings/workaround files committed.
