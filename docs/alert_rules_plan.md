# Alert Rules System — Implementation Plan

## Goal

Build the Alert Rules engine that lets users define custom rules (e.g., "alert me when a WHALE buys >$100K of any token") and automatically generates `Alert` records when detected signals match those rules. This completes the **detect → match → notify** loop that is the core value proposition of Argus.

## User Review Required

> [!IMPORTANT]
> The `alert_rules` table (V5 migration) already exists with `conditions JSONB` and `channels JSONB`. We will **not** alter this table — only add a new `alerts` table for matched alert instances.

> [!WARNING]
> This plan does **not** include the Telegram notification delivery. It builds the rules engine and matching logic only. Telegram integration should be a follow-up feature.

---

## Proposed Changes

### Domain Layer

#### [NEW] [AlertRule.java](file:///mnt/apps/argus/src/main/java/com/argus/domain/model/AlertRule.java)

Domain model mapping to the existing `alert_rules` table:

```java
public class AlertRule {
    private UUID id;
    private UUID userId;
    private String name;
    private AlertConditions conditions; // parsed JSONB
    private List<String> channels;      // parsed JSONB
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### [NEW] [AlertConditions.java](file:///mnt/apps/argus/src/main/java/com/argus/domain/model/AlertConditions.java)

Typed representation of the `conditions` JSONB column:

```java
public class AlertConditions {
    private List<String> signalTypes;       // e.g. ["WHALE_BUY", "SNIPER_ALPHA"]
    private BigDecimal minAmountUsd;        // e.g. 100000
    private List<String> walletArchetypes;  // e.g. ["WHALE", "SNIPER"]
    private List<String> tokenAddresses;    // optional: specific tokens
    private List<String> chains;            // optional: specific chains
}
```

Matching method `matches(Signal signal)` lives on this class — keeps logic in the domain.

#### [NEW] [Alert.java](file:///mnt/apps/argus/src/main/java/com/argus/domain/model/Alert.java)

A triggered alert instance — created when a signal matches a user's rule:

```java
public class Alert {
    private UUID id;
    private UUID userId;
    private UUID alertRuleId;
    private Long signalId;
    private String status;      // PENDING, SENT, DELIVERED, FAILED
    private boolean read;
    private LocalDateTime createdAt;
}
```

---

### Port Layer

#### [NEW] [AlertRulePersistencePort.java](file:///mnt/apps/argus/src/main/java/com/argus/domain/port/persistence/AlertRulePersistencePort.java)

```java
public interface AlertRulePersistencePort {
    AlertRule save(AlertRule rule);
    Optional<AlertRule> findByIdAndUserId(UUID id, UUID userId);
    List<AlertRule> findByUserId(UUID userId);
    List<AlertRule> findAllEnabled();
    void deleteByIdAndUserId(UUID id, UUID userId);
}
```

#### [NEW] [AlertPersistencePort.java](file:///mnt/apps/argus/src/main/java/com/argus/domain/port/persistence/AlertPersistencePort.java)

```java
public interface AlertPersistencePort {
    Alert save(Alert alert);
    List<Alert> findByUserId(UUID userId, int limit);
    Optional<Alert> findById(UUID id);
    void markAsRead(UUID id, UUID userId);
    long countUnreadByUserId(UUID userId);
}
```

---

### Service Layer

#### [NEW] [AlertRuleService.java](file:///mnt/apps/argus/src/main/java/com/argus/domain/service/AlertRuleService.java)

CRUD for user alert rules + default rule provisioning:

- `createRule(AlertRule rule, UUID userId)` — validate + persist
- `getRules(UUID userId)` — list user's rules
- `getRule(UUID id, UUID userId)` — single rule (ownership checked)
- `updateRule(UUID id, AlertRule updated, UUID userId)` — partial update
- `deleteRule(UUID id, UUID userId)`
- `createDefaultRulesForUser(UUID userId)` — whale buy > $50K default

#### [NEW] [RuleMatcherService.java](file:///mnt/apps/argus/src/main/java/com/argus/domain/service/RuleMatcherService.java)

The core matching engine — called after every signal is saved:

- `matchSignal(Signal signal)` — load all enabled rules, check `AlertConditions.matches(signal)`, create `Alert` records for matches
- Uses Redis cache for enabled rules (5-min TTL) to avoid DB hits on every signal

---

### Infrastructure Layer — Persistence

#### [NEW] [AlertRuleEntity.java](file:///mnt/apps/argus/src/main/java/com/argus/infra/persistence/entity/AlertRuleEntity.java)

JPA entity for existing `alert_rules` table. `conditions` and `channels` stored as `String` with `columnDefinition = "jsonb"`, converted via Jackson in the adapter.

#### [NEW] [AlertEntity.java](file:///mnt/apps/argus/src/main/java/com/argus/infra/persistence/entity/AlertEntity.java)

JPA entity for new `alerts` table.

#### [NEW] [AlertRuleRepository.java](file:///mnt/apps/argus/src/main/java/com/argus/infra/persistence/repository/AlertRuleRepository.java)

Spring Data JPA repository.

#### [NEW] [AlertRepository.java](file:///mnt/apps/argus/src/main/java/com/argus/infra/persistence/repository/AlertRepository.java)

Spring Data JPA repository.

#### [NEW] [AlertRulePersistenceAdapter.java](file:///mnt/apps/argus/src/main/java/com/argus/infra/persistence/adapter/AlertRulePersistenceAdapter.java)

Implements `AlertRulePersistencePort`. Handles entity↔domain mapping and JSONB serialization via Jackson `ObjectMapper`.

#### [NEW] [AlertPersistenceAdapter.java](file:///mnt/apps/argus/src/main/java/com/argus/infra/persistence/adapter/AlertPersistenceAdapter.java)

Implements `AlertPersistencePort`.

---

### Database Migration

#### [NEW] [V21__create_alerts_table.sql](file:///mnt/apps/argus/src/main/resources/db/migration/V21__create_alerts_table.sql)

```sql
CREATE TABLE alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    alert_rule_id UUID REFERENCES alert_rules(id) ON DELETE CASCADE,
    signal_id BIGINT REFERENCES signals(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_alerts_user ON alerts(user_id);
CREATE INDEX idx_alerts_rule ON alerts(alert_rule_id);
CREATE INDEX idx_alerts_user_unread ON alerts(user_id, read) WHERE read = false;
```

---

### API Layer

#### [NEW] [AlertRuleApi.java](file:///mnt/apps/argus/src/main/java/com/argus/api/spec/AlertRuleApi.java)

OpenAPI spec interface (follows existing pattern in `api/spec/`).

#### [NEW] [AlertRuleController.java](file:///mnt/apps/argus/src/main/java/com/argus/api/AlertRuleController.java)

REST controller implementing `AlertRuleApi`:

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/alert-rules` | Create a new alert rule |
| `GET` | `/api/v1/alert-rules` | List user's alert rules |
| `GET` | `/api/v1/alert-rules/{id}` | Get single rule |
| `PUT` | `/api/v1/alert-rules/{id}` | Update rule |
| `DELETE` | `/api/v1/alert-rules/{id}` | Delete rule |

#### [NEW] [AlertController.java](file:///mnt/apps/argus/src/main/java/com/argus/api/AlertController.java)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/alerts` | List user's triggered alerts |
| `PATCH` | `/api/v1/alerts/{id}/read` | Mark alert as read |
| `GET` | `/api/v1/alerts/unread-count` | Get unread alert count |

#### [NEW] DTOs

- [AlertRuleRequest.java](file:///mnt/apps/argus/src/main/java/com/argus/api/dto/request/AlertRuleRequest.java) — with Jakarta validation
- [AlertRuleResponse.java](file:///mnt/apps/argus/src/main/java/com/argus/api/dto/response/AlertRuleResponse.java)
- [AlertResponse.java](file:///mnt/apps/argus/src/main/java/com/argus/api/dto/response/AlertResponse.java)

---

### Exception Handling

#### [NEW] [AlertRuleNotFoundException.java](file:///mnt/apps/argus/src/main/java/com/argus/core/exception/AlertRuleNotFoundException.java)

#### [MODIFY] [GlobalExceptionHandler.java](file:///mnt/apps/argus/src/main/java/com/argus/core/exception/GlobalExceptionHandler.java)

Add `handleAlertRuleNotFound()` handler — follows existing pattern (returns 404 + `ErrorResponse`).

---

### Configuration Wiring

#### [MODIFY] [DomainServiceConfig.java](file:///mnt/apps/argus/src/main/java/com/argus/config/DomainServiceConfig.java)

Add beans:
- `alertRuleService(AlertRulePersistencePort)` → `AlertRuleService`
- `ruleMatcherService(AlertRulePersistencePort, AlertPersistencePort)` → `RuleMatcherService`

---

### Pipeline Integration

#### [MODIFY] [WhaleDetectorService.java](file:///mnt/apps/argus/src/main/java/com/argus/domain/service/WhaleDetectorService.java)

After saving a signal, call `ruleMatcherService.matchSignal(signal)` to check against all enabled rules and generate alerts.

This is the single integration point — all signal types (WHALE_BUY, SNIPER_ALPHA, MULTI_WHALE, etc.) flow through `detectAndSaveWhaleSignal()`, so wiring here covers all signals.

---

## New Files Summary

| # | File | Layer | Purpose |
|---|------|-------|---------|
| 1 | `AlertRule.java` | Domain Model | Alert rule definition |
| 2 | `AlertConditions.java` | Domain Model | Typed JSONB conditions with `matches()` |
| 3 | `Alert.java` | Domain Model | Triggered alert instance |
| 4 | `AlertRulePersistencePort.java` | Port | Rule persistence interface |
| 5 | `AlertPersistencePort.java` | Port | Alert persistence interface |
| 6 | `AlertRuleService.java` | Service | Rule CRUD + defaults |
| 7 | `RuleMatcherService.java` | Service | Signal→rule matching engine |
| 8 | `AlertRuleEntity.java` | Infra/Entity | JPA entity for `alert_rules` |
| 9 | `AlertEntity.java` | Infra/Entity | JPA entity for `alerts` |
| 10 | `AlertRuleRepository.java` | Infra/Repo | Spring Data JPA |
| 11 | `AlertRepository.java` | Infra/Repo | Spring Data JPA |
| 12 | `AlertRulePersistenceAdapter.java` | Infra/Adapter | Port implementation |
| 13 | `AlertPersistenceAdapter.java` | Infra/Adapter | Port implementation |
| 14 | `V21__create_alerts_table.sql` | Migration | New alerts table |
| 15 | `AlertRuleApi.java` | API Spec | OpenAPI interface |
| 16 | `AlertRuleController.java` | API | Rule CRUD endpoints |
| 17 | `AlertController.java` | API | Alert list/read endpoints |
| 18 | `AlertRuleRequest.java` | DTO | Request validation |
| 19 | `AlertRuleResponse.java` | DTO | Response mapping |
| 20 | `AlertResponse.java` | DTO | Response mapping |
| 21 | `AlertRuleNotFoundException.java` | Exception | 404 handling |

## Modified Files

| File | Change |
|------|--------|
| `DomainServiceConfig.java` | Add `alertRuleService` and `ruleMatcherService` beans |
| `GlobalExceptionHandler.java` | Add `handleAlertRuleNotFound()` |
| `WhaleDetectorService.java` | Call `ruleMatcherService.matchSignal()` after saving signal |

---

## Verification Plan

### Automated Tests

**1. Unit test: `AlertConditions.matches()` logic**
```bash
./mvnw test -Dtest=AlertConditionsTest
```
- Test WHALE_BUY matches rule with `signalTypes: ["WHALE_BUY"]`
- Test signal below `minAmountUsd` does NOT match
- Test empty conditions matches everything
- Test archetype filter

**2. Unit test: `RuleMatcherService.matchSignal()`**
```bash
./mvnw test -Dtest=RuleMatcherServiceTest
```
- Mock `AlertRulePersistencePort` to return rules, verify `Alert` is created when conditions match
- Verify NO alert when conditions don't match
- Verify multiple rules can match the same signal

**3. Controller test: `AlertRuleController`**
```bash
./mvnw test -Dtest=AlertRuleControllerTest
```
- Test CRUD endpoints return correct HTTP status codes
- Test validation (empty name, null conditions)
- Test user ownership enforcement

**4. Full suite regression**
```bash
./mvnw test
```
- Verify no existing tests break

### Manual Verification

Test the full API flow using curl or Swagger UI once the app is running (`./mvnw spring-boot:run`):

1. **Create a rule:** `POST /api/v1/alert-rules` with body:
   ```json
   {
     "name": "Whale Buys >100K",
     "conditions": {
       "signalTypes": ["WHALE_BUY"],
       "minAmountUsd": 100000
     },
     "channels": ["in_app"],
     "enabled": true
   }
   ```
2. **List rules:** `GET /api/v1/alert-rules` — verify rule appears
3. **Check alerts:** `GET /api/v1/alerts` — verify alerts appear after signal detection runs
