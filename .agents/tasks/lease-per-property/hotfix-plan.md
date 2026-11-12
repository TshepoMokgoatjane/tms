# Implementation Plan — Hotfix: Room/Tenant JSON serialization of LAZY Property proxy

Branch: `hotfix/room-property-serialization`
Worktree root (run ALL commands from here): `C:\devWork\tms\.worktrees\hotfix-room-property-serialization`

## Problem recap

`Room.property` is `@ManyToOne(fetch = FetchType.LAZY)`. `RoomController` (`/rooms`) returns raw `Room`
entities and `TenantController` (`/tenants`) returns raw `Tenant` entities (Tenant has `@OneToOne(EAGER) Room`).
When Hibernate has not initialized `room.property`, it hands Jackson a `Property$HibernateProxy` (ByteBuddy)
whose `hibernateLazyInitializer`/`handler` internals have no serializer, so every `/rooms` and `/tenants`
response fails with `InvalidDefinitionException` → HTTP 500. The data is intact; this is purely a
serialization failure. The DB must NOT be touched.

## Design decisions (grounded in the codebase)

1. **Follow the existing per-entity annotation pattern — do NOT add a new mechanism.** The codebase already
   solves this exact problem in two places with the same annotation:
   - `src/main/java/za/co/tms/domain/GateRemote.java` — `@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})` on the lazy `@ManyToOne Tenant` field.
   - `src/main/java/za/co/tms/domain/TicketComment.java` — same annotation on the lazy `@ManyToOne Ticket` field.
   There is **no** `jackson-datatype-hibernate` dependency in `pom.xml`, **no** `Hibernate5Module`/`Hibernate6Module`
   registration, and **no** custom `ObjectMapper`/`Jackson2ObjectMapperBuilder` bean anywhere in `src/main`. So
   the established, minimal, targeted fix is the `@JsonIgnoreProperties` annotation. We will NOT introduce the
   hibernate module and will NOT globally disable `FAIL_ON_EMPTY_BEANS` (that only hides the symptom).

2. **Where to annotate: type-level on `Property`.** The failing object in both reported reference chains is the
   `Property` proxy (`...Room["property"]->Property$HibernateProxy...["hibernateLazyInitializer"]`). Annotating
   the `Property` *type* fixes every serialization path that reaches a `Property` proxy at once — both
   `Room["property"]` and `Tenant["room"]["property"]` — rather than having to annotate each referencing field
   separately. This also matches the user's required fix. (Field-level on `Room.property`, GateRemote-style,
   would also work for the Room path, but type-level on `Property` is strictly broader and covers the Tenant→Room→Property
   path and any future lazy reference to `Property` with one annotation. Chosen for correctness + minimal surface.)
   `@JsonIgnoreProperties` is additive with the inherited `AuditModel` annotation: the two named properties are
   merged, so `createdAt`/`updatedAt` handling on `Property` is unaffected.

3. **No change needed to `Room` or `Tenant` themselves.** `Room.property` is the only LAZY association that gets
   serialized raw through `/rooms` and `/tenants`; fixing `Property` at the type level covers it. `Tenant.room`
   is `EAGER` (never a proxy). No other entity reachable from these two endpoints exposes an uninitialized LAZY
   proxy (verified: `GateRemote` and `TicketComment` already carry the annotation; they are not in the `/rooms`
   or `/tenants` response graphs).

4. **The lease blob stays hidden.** `Property.leasePdfData` keeps its existing `@JsonIgnore` + `@Schema(hidden=true)`.
   `@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})` only suppresses the two Hibernate proxy
   internal accessors; it does not alter `@JsonIgnore` on any real field. `leaseFilename`/`leaseContentType`/
   `leaseFileSize`/`streetAddress`/`erfNumber` continue to serialize; `leasePdfData` continues to be omitted.

## Plan

- [ ] 1. Add the Hibernate-proxy ignore annotation to the `Property` entity, following the existing
      `GateRemote`/`TicketComment` pattern, placed at the class (type) level.
      Add import `com.fasterxml.jackson.annotation.JsonIgnoreProperties` and annotate the class with
      `@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})`. Do NOT remove or alter the existing
      `@JsonIgnore` on `leasePdfData`, the `@Schema` annotations, or anything inherited from `AuditModel`.
      Files: `src/main/java/za/co/tms/domain/Property.java`
      Verify: `mvnw.cmd -q compile` from the worktree root completes with no errors.
      (Expected exact class header after edit:)
      ```java
      import com.fasterxml.jackson.annotation.JsonIgnore;
      import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
      ...
      @Data
      @Entity
      @AllArgsConstructor
      @NoArgsConstructor
      @EqualsAndHashCode(callSuper = true)
      @Table(name = "property")
      @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
      @Schema(description = "Property/address owning a shared lease agreement PDF reused across its units")
      public class Property extends AuditModel implements Serializable {
      ```

- [ ] 2. Add a regression test that genuinely reproduces the ByteBuddy proxy and serializes it through the real
      Jackson `ObjectMapper`. Use the `@DataJpaTest` approach (preferred over a hand-built entity, which can never
      be a proxy, and over a pure MockMvc mock, where a Mockito mock would not be a real Hibernate proxy). The test
      must: save a `Property` and a `Room` linked to it, flush, then **clear the persistence context** so the
      reload produces an uninitialized LAZY proxy for `room.property`, reload the `Room` by id, and serialize it
      with an `ObjectMapper` **without first touching `getProperty()`** (touching it would initialize the proxy
      and hide the bug). Assert serialization succeeds and the JSON contains the property's `streetAddress` and
      does NOT contain `hibernateLazyInitializer` or `leasePdfData`.
      Mirror the existing `@DataJpaTest` setup in `src/test/java/za/co/tms/repository/PropertyRepositoryTest.java`
      (`@DataJpaTest` + `@EntityScan("za.co.tms.domain")` + `@EnableJpaRepositories("za.co.tms.repository")` +
      `@AutoConfigureTestDatabase(replace = ANY)`), and inject `TestEntityManager` to flush/clear. Build the
      `ObjectMapper` the same way Spring Boot does (`new ObjectMapper()` is sufficient here because the fix is a
      Jackson annotation on the entity, not an ObjectMapper feature; register `JavaTimeModule` only if a
      serialized field needs it — `Room` has `LocalDate`/`LocalDateTime`, so register `JavaTimeModule` and
      disable `WRITE_DATES_AS_TIMESTAMPS` is optional; simplest is `new ObjectMapper().findAndRegisterModules()`).
      Suggested test skeleton:
      ```java
      @DataJpaTest
      @EntityScan(basePackages = "za.co.tms.domain")
      @EnableJpaRepositories(basePackages = "za.co.tms.repository")
      @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
      class RoomLazyPropertySerializationTest {
          @Autowired RoomRepository roomRepository;
          @Autowired PropertyRepository propertyRepository;
          @Autowired TestEntityManager em;

          private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

          @Test
          void serializesRoomWithUninitializedLazyPropertyProxy() throws Exception {
              Property p = new Property();
              p.setStreetAddress("57 Caledon Drive");
              p.setErfNumber("1866");
              p = propertyRepository.save(p);

              Room r = new Room();
              r.setCode("A1");
              r.setDescription("Unit A1");
              r.setRentalAmount(java.math.BigDecimal.valueOf(5000));
              r.setOccupied(false);
              r.setProperty(p);
              r = roomRepository.save(r);

              em.flush();
              em.clear(); // force LAZY property to be a ByteBuddy proxy on reload

              Room reloaded = roomRepository.findById(r.getId()).orElseThrow();
              // IMPORTANT: do NOT call reloaded.getProperty().getStreetAddress() before serializing

              String json = mapper.writeValueAsString(reloaded); // throws InvalidDefinitionException WITHOUT the fix
              assertThat(json).contains("57 Caledon Drive");
              assertThat(json).doesNotContain("hibernateLazyInitializer");
              assertThat(json).doesNotContain("leasePdfData");
          }
      }
      ```
      Files: `src/test/java/za/co/tms/repository/RoomLazyPropertySerializationTest.java`
      (or `src/test/java/za/co/tms/domain/RoomLazyPropertySerializationTest.java` — keep it beside the existing
      `@DataJpaTest` classes in the `repository` package for consistency).
      Verify: run only this test — `mvnw.cmd -q -Dtest=RoomLazyPropertySerializationTest test` (with the TLS
      workaround flags below if Nexus blocks resolution). Expected: PASSES with the fix from step 1 applied.

- [ ] 3. Prove the test actually reproduces the bug (fail-without-fix check). Temporarily revert step 1 (remove the
      `@JsonIgnoreProperties` annotation from `Property`), re-run the step-2 test, and confirm it FAILS with
      `com.fasterxml.jackson.databind.exc.InvalidDefinitionException` naming `hibernateLazyInitializer`. Then
      re-apply the step-1 annotation and confirm the test PASSES again. This closes the gap that let the original
      bug ship (prior tests used DTOs/hand-built entities and never serialized a real proxy). Do NOT commit the
      reverted state.
      Files: none committed (temporary toggle of `src/main/java/za/co/tms/domain/Property.java` only).
      Verify: without annotation → test fails with `InvalidDefinitionException` (`hibernateLazyInitializer`);
      with annotation restored → test passes.

- [ ] 4. Run the full test suite to confirm no regressions, especially the `/rooms` and `/tenants` controller
      slices and the existing `PropertyRepositoryTest`/`TenantRepositoryTest`.
      Files: none.
      Verify: `mvnw.cmd -q test` from the worktree root — BUILD SUCCESS, all tests pass (including
      `TenantControllerTest`, `PropertyRepositoryTest`, and the new `RoomLazyPropertySerializationTest`).

## Build / test commands and environment workaround

From the worktree root `C:\devWork\tms\.worktrees\hotfix-room-property-serialization`:
- Compile: `mvnw.cmd -q compile`
- Test (all): `mvnw.cmd -q test`
- Test (single): `mvnw.cmd -q -Dtest=RoomLazyPropertySerializationTest test`

Corporate Nexus Maven mirror has an untrusted TLS cert that can break dependency resolution. If resolution
fails, use the previously-proven offline/direct-central workaround, e.g.:
`mvnw.cmd -o -s <temp-settings.xml> -q test`
where `<temp-settings.xml>` mirrors `*` to `https://repo.maven.apache.org/maven2` with id `central-direct`
(alternatively `-Dmaven.legacyLocalRepo=true` against an already-populated local repo). This is an
**environment-only** workaround — the temp settings file and any such flags MUST NOT be committed.

## Commit scope

Only two files change in the commit: `src/main/java/za/co/tms/domain/Property.java` (one import + one annotation)
and the new test `src/test/java/za/co/tms/repository/RoomLazyPropertySerializationTest.java`. No DB changes, no
`pom.xml` changes, no settings files. Commit locally on `hotfix/room-property-serialization`; do not push.

## Assumptions / gaps

- Assumed `new ObjectMapper().findAndRegisterModules()` adequately mirrors the production mapper for the purpose
  of this test; the fix is a Jackson annotation on the entity and is mapper-config-independent, so this is safe.
  If the team prefers end-to-end fidelity, an alternative/companion is a `@SpringBootTest` + `MockMvc` slice on
  `/rooms/find/all` with the service returning a repository-loaded (proxied) `Room`; the `@DataJpaTest` approach
  above is preferred because it most directly and reliably creates the real ByteBuddy proxy.
