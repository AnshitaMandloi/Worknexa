# WorkNexa: AI-Assisted Workflow Automation Platform

> **Core philosophy**: *AI recommends, rules constrain, and humans remain in control of important decisions.*

WorkNexa is a configurable, multi-tenant workflow automation platform inspired by enterprise tools such as ServiceNow. It routes business requests (expenses today, any request type through configuration) through a three-layer decision engine:

1. **AI recommends**: a risk assessment (LOW / MEDIUM / HIGH) with a confidence score and reasons, produced by Google Gemini. If the AI service is unavailable, the platform falls back safely.
2. **Rules constrain**: priority-based business rules set by the tenant admin (amount limits, receipt requirements, risk level). **A mandatory rule always overrides the AI.**
3. **Humans decide**: multi-step approval chains with role-based access control, separation of duties, cascading cancellation and an append-only audit trail.

---

## 1. System Architecture

```
                    +---------------------------------------+
                    |   WorkNexa Web UI                     |
                    |  (HTML5 / CSS3 / Vanilla JS / Fetch)  |
                    +-------------------+-------------------+
                                        |
                           REST API + JWT Bearer token
                                        |
                                        v
                    +---------------------------------------+
                    |     Spring Security + RBAC Filter     |
                    |   (tenantId and role from JWT claims) |
                    +-------------------+-------------------+
                                        |
                                        v
                    +---------------------------------------+
                    |         WorkflowEngineService         |
                    +-------+---------------+---------------+
                            |               |
              +-------------+               +--------------+
              v                                            v
 +-------------------------------+        +-------------------------------+
 |       AiRoutingService        |        |       RuleEngineService       |
 |  - LlmRiskAnalyzer (Gemini)   |        |  - DB-driven business rules   |
 |  - MockRiskAnalyzer (fallback)|        |  - Priority & mandatory checks|
 +---------------+---------------+        +---------------+---------------+
                 |                                        |
                 +-------------+        +-----------------+
                               v        v
                    +---------------------------------------+
                    |            DecisionService            |
                    |  (blends AI + rules into the routing) |
                    +-------------------+-------------------+
                                        |
              +-------------------------+-------------------------+
              v                                                   v
   [Outcome: AUTO_APPROVE]                              [Outcome: HUMAN_REVIEW]
   - Request marked AUTO_APPROVED                       - Request marked PENDING_REVIEW
   - REQUEST_AUTO_APPROVED logged                       - Approval task chain created
   - Requester notified                                   (e.g. Manager → Finance)
                                                        - First reviewer notified
```

---

## 2. Core Engines & Governance

### A. Workflow Engine (`WorkflowEngineService`)
- Finds the active workflow definition for the tenant and request category.
- Coordinates the stages `SUBMISSION` → `AI_ANALYSIS` → `HUMAN_REVIEW` → `COMPLETION`.
- Creates sequential approval chains (`sequenceNo` 1 → 2 → 3).

### B. Rule Engine (`RuleEngineService`)
- Evaluates request attributes (`amount`, `documentAttached`, `riskLevel`) against the tenant's rules, ordered by priority.
- Operators: `LTE`, `LT`, `GT`, `GTE`, `EQ`, `NEQ`, `BETWEEN`.
- **Default rules seeded for the demo tenant:**
  1. `documentAttached == false` → `MANUAL_REVIEW` (*mandatory*)
  2. `amount <= 10000` → `AUTO_APPROVE` (optional)
  3. `riskLevel == HIGH` → `COMPLIANCE_REVIEW` (*mandatory*)
  4. `10000 < amount <= 50000` → `MANAGER_REVIEW` (optional)
  5. `amount > 50000` → `FINANCE_REVIEW` (*mandatory*)

### C. Decision Layer (`DecisionService`)
- A **mandatory rule always overrides the AI** (for example, a ₹65,000 request needs Finance review even if the AI rates it LOW).
- AI says `LOW` and the rules allow automation → `AUTO_APPROVE`.
- AI says `MEDIUM`, or a rule requires review → `HUMAN_REVIEW` (Manager).
- AI says `HIGH`, or a high-value rule triggers → multi-step chain (Manager → Finance → Compliance).

### D. AI Layer (`AiRoutingService`)
Which analyzer is used is decided per request:

| Situation | Analyzer |
| :--- | :--- |
| `AI_MOCK_ENABLED=true` or no `GEMINI_API_KEY` | `MockRiskAnalyzer` (fixed thresholds: up to ₹10,000 LOW, up to ₹50,000 MEDIUM, above that HIGH; missing receipt is HIGH) |
| API key present and Gemini responds | `LlmRiskAnalyzer` (real Gemini, strict JSON validation) |
| API key present but the call fails or the output is invalid | Safe fallback: MEDIUM risk, human review required |

The Gemini model is configurable through `GEMINI_MODEL` (default `gemini-3.5-flash-lite`), so it can be changed without touching the code when Google retires a model.

### E. Governance & Security Guards
- **Multi-tenant isolation**: `tenantId` always comes from the signed JWT. Every query is filtered by tenant, so cross-tenant access returns `403` / `404`.
- **Separation of duties**: nobody can approve a request they submitted (`403`).
- **Cascade cancellation**: if a reviewer rejects, all downstream pending tasks become `CANCELLED`.
- **Information request round-trip**: a reviewer can choose `REQUEST_INFO`; the request moves to `INFO_REQUESTED` and review resumes after the requester responds.
- **Append-only audit trail**: every event (`REQUEST_CREATED`, `AI_ANALYSIS_COMPLETED`, `RULE_TRIGGERED`, `DECISION_MADE`, `TASK_ASSIGNED`, `TASK_APPROVED`, `TASK_REJECTED`, ...) is recorded with actor and time.

---

## 3. Tech Stack

- **Backend**: Java 17, Spring Boot 3.2.5, Spring Web, Spring Data JPA, Hibernate, Bean Validation
- **Security**: Spring Security 6, JWT (`jjwt` 0.12.5), RBAC, BCrypt password hashing
- **Database**: PostgreSQL (16 in Docker, 14+ locally). H2 in-memory is used by the automated tests and by the optional `local` profile.
- **AI**: Google Gemini API through the `AiRiskAnalyzer` interface, with a mock analyzer as fallback
- **API docs**: SpringDoc OpenAPI / Swagger UI (`/swagger-ui.html`)
- **Testing**: JUnit 5, Mockito, Spring Boot Test, MockMvc (21 automated tests)
- **Deployment**: multi-stage `Dockerfile` and `docker-compose.yml` (PostgreSQL + app)
- **Frontend**: HTML5, CSS3 and vanilla JavaScript (ES modules, Fetch API). No build tools and no npm.

---

## 4. Database

Schema is generated by Hibernate (`ddl-auto: update`). Ten tables, each tenant-owned table carries a `tenant_id`:

| Table | Purpose |
| :--- | :--- |
| `tenants` | Organizations |
| `users` | Users per tenant (unique on tenant + email), role, BCrypt password hash, active flag |
| `workflow_definitions` | Workflow per request type |
| `workflow_stages` | Stages of a workflow (type, assigned role, order) |
| `business_rules` | Rules (field, operator, value, action, priority, mandatory) |
| `workflow_requests` | Submitted requests (title, amount, category, status, current stage) |
| `approval_tasks` | One row per review step (role, sequence, status, decided by, comment) |
| `ai_analyses` | AI result per request (risk level, confidence, recommendation, reasons) |
| `audit_logs` | Append-only event log |
| `notifications` | In-app notifications |

Tenant isolation is enforced in the application layer (tenant id from the JWT on every query).

---

## 5. Demo Tenants, Accounts & Sample Data

On first launch the `DataSeeder` creates two demo tenants (all passwords: **`Password@123`**).

### Tenant 1: Acme Corp
| Role | Email | Capabilities |
| :--- | :--- | :--- |
| **ADMIN** | `admin@acme.com` | Manage workflows, stages, rules and users |
| **EMPLOYEE** | `employee@acme.com` | Submit and track requests, answer information requests |
| **MANAGER** | `manager@acme.com` | Stage 1 approvals |
| **FINANCE** | `finance@acme.com` | Stage 2 high-value approvals |
| **COMPLIANCE** | `compliance@acme.com` | Stage 3 risk and regulatory approvals |

### Tenant 2: Globex Corp
| Role | Email | Purpose |
| :--- | :--- | :--- |
| **ADMIN** | `admin@globex.com` | Verifies multi-tenant isolation |
| **EMPLOYEE** | `employee@globex.com` | Verifies cross-tenant access is blocked |

### Sample requests
When the requests table is empty, `DemoRequestSeeder` pushes seven sample requests for Acme Corp through the real engine (AI analysis, rules, approvals, audit trail), so the dashboard is not empty. Results below are with the mock analyzer:

| Request | Amount | Result |
| :--- | :--- | :--- |
| Team lunch - sprint demo | ₹4,500 | Auto-approved |
| Client dinner - Q3 review | ₹18,500 | Approved by Manager |
| Design software licence renewal | ₹35,000 | Information requested |
| Vendor advance payment | ₹1,20,000 | Rejected by Manager |
| Cloud infrastructure credits | ₹72,000 | Pending (Finance) |
| Data centre storage upgrade | ₹65,000 | Pending (Manager) |
| DevSummit conference registration | ₹28,000 | Pending (Manager) |

Turn this off with `SEED_DEMO_REQUESTS=false`.

---

## 6. Frontend

Served directly by Spring Boot from `src/main/resources/static/`:

- **`/`** is the landing page (`index.html`). **`/app.html`** is the application.
- **Sidebar navigation**: Dashboard, My Requests, Approvals (with a live task counter), All Requests, Audit Trail, and for admins Workflows & Rules and Team Directory. Record search by number (`REQ0000007`).
- **Header**: notification bell with unread badge, *New Request* button, and a persona switcher in the account menu to move between admin, manager, finance, compliance and employee without logging out.
- **Dashboard**: KPI cards (awaiting approval, total requests, automation rate), status breakdown bars, state donut chart and a latest-requests table.
- **Request page**: progress stepper (Submitted → AI analysis → Review → Completed), a three-column verdict (*AI recommends / Rules constrain / Humans decide*), fields card, approvals table, activity stream and an AI card with confidence meter and reasons.
- **Admin screens**: workflow, stage and rule management, and user management.

---

## 7. REST API Reference

Swagger UI is available at: **`http://localhost:8080/swagger-ui.html`**

| Module | Method | Endpoint | Access Role | Description |
| :--- | :--- | :--- | :--- | :--- |
| **Auth** | `POST` | `/api/auth/register-organization` | Public | Create new tenant & first admin user |
| **Auth** | `POST` | `/api/auth/login` | Public | Authenticate user & return JWT token |
| **Auth** | `GET` | `/api/auth/me` | Authenticated | Get current authenticated user details |
| **Requests** | `POST` | `/api/requests` | `EMPLOYEE`, `ADMIN` | Submit a new workflow request |
| **Requests** | `GET` | `/api/requests/my` | Authenticated | View requests submitted by current user |
| **Requests** | `GET` | `/api/requests` | Reviewers / Admin | View all tenant requests (paginated) |
| **Requests** | `GET` | `/api/requests/{id}` | Tenant Members | Get record details, AI analysis, tasks, timeline |
| **Requests** | `POST` | `/api/requests/{id}/respond-info` | Request Owner | Provide requested information to resume review |
| **Approvals**| `GET` | `/api/tasks/pending` | Reviewers | View actionable pending tasks for current role |
| **Approvals**| `POST` | `/api/tasks/{id}/decision` | Assigned Role / Admin | Submit decision (`APPROVE`, `REJECT`, `REQUEST_INFO`) |
| **Workflows**| `GET/POST`| `/api/workflows` | `ADMIN` | List and create workflow definitions |
| **Workflows**| `POST` | `/api/workflows/{id}/stages` | `ADMIN` | Add stages to a workflow |
| **Rules** | `GET/POST`| `/api/workflows/{id}/rules` | `ADMIN` | List and create business rules |
| **Users** | `GET/POST`| `/api/users` | `ADMIN` | List and invite tenant users |
| **Users** | `PATCH` | `/api/users/{id}/status` | `ADMIN` | Activate or deactivate a user |
| **Audit** | `GET` | `/api/audit` | Reviewers / Admin | Query append-only audit trail |
| **Notes** | `GET` | `/api/notifications` | Authenticated | View in-app notifications |
| **Notes** | `PATCH` | `/api/notifications/{id}/read` | Authenticated | Mark notification as read |
| **Dashboard**| `GET` | `/api/dashboard/stats` | Authenticated | High-level KPI and automation metrics |

---

## 8. Configuration

Settings are read from environment variables. For local development put them in a `.env` file in the project root (Spring Boot loads it automatically). **Never commit `.env`**; it is listed in `.gitignore`. Copy `.env.example` to `.env` to start.

| Variable | Required | Description |
| :--- | :--- | :--- |
| `JWT_SECRET` | **Yes** | At least 32 characters. The app refuses to start without it. |
| `SPRING_DATASOURCE_URL` | No | Default `jdbc:postgresql://localhost:5432/worknexa` |
| `SPRING_DATASOURCE_USERNAME` | No | Default `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | **Yes** (Postgres) | Your PostgreSQL password |
| `POSTGRES_PASSWORD` | Docker only | Password for the Postgres container (use the same value) |
| `GEMINI_API_KEY` | No | Key from Google AI Studio. Empty means the mock analyzer is used. |
| `GEMINI_MODEL` | No | Default `gemini-3.5-flash-lite` |
| `AI_MOCK_ENABLED` | No | `true` forces the mock analyzer (predictable demos) |
| `SEED_DEMO_REQUESTS` | No | Default `true` |
| `CORS_ALLOWED_ORIGINS` | No | Comma-separated list of allowed origins |
| `PORT` | No | Default `8080` |

Generate a JWT secret on Windows PowerShell:
```powershell
$b = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

---

## 9. How to Run

### Option A: Local with Maven and PostgreSQL
Prerequisites: Java 17+, Maven, PostgreSQL.

1. Create the database (pgAdmin, or `psql -U postgres`, then `CREATE DATABASE worknexa;`).
2. Copy `.env.example` to `.env` and fill in `JWT_SECRET` and `SPRING_DATASOURCE_PASSWORD`.
3. From the project root (where `pom.xml` is):
```bash
mvn spring-boot:run
```
4. Open **http://localhost:8080** (landing page) or **http://localhost:8080/app.html** (the app).

To use another port, set `PORT` in `.env` (for example `PORT=8081`).

### Option B: Docker Compose
Needs Docker Desktop. Set `POSTGRES_PASSWORD` and `JWT_SECRET` in `.env`, then:
```bash
docker compose up --build
```
- App: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- PostgreSQL: `127.0.0.1:5432` (user `postgres`, database `worknexa`)

If the Postgres volume was created earlier with a different password, reset it with `docker compose down -v` (this deletes the data).

### Option C: Quick demo with no database setup
Uses an in-memory H2 database and the mock AI. Data is lost on restart.
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```
On Windows PowerShell, quote the argument: `mvn spring-boot:run "-Dspring-boot.run.profiles=local"`.

### Using real Gemini
Get a key from Google AI Studio (aistudio.google.com, *Get API key*), set `GEMINI_API_KEY` and `AI_MOCK_ENABLED=false` in `.env`, and restart. Submit a request: the AI card should show Gemini's reasons instead of "AI unavailable". The free tier has rate limits, and Google may use free-tier inputs to improve its products, so use demo data only.

---

## 10. Security Notes

- No secrets are stored in the repository. `JWT_SECRET` and database credentials come from the environment, and the app will not start with a missing or short JWT secret.
- Every endpoint requires a valid JWT except login, organization registration, Swagger and the static pages. New endpoints are protected by default.
- CORS allows only the origins listed in `CORS_ALLOWED_ORIGINS`.
- Passwords are hashed with BCrypt. The demo password `Password@123` is for demo accounts only.
- Swagger UI is public to make the API easy to explore. Restrict or disable it for a real production deployment.

---

## 11. Automated Tests

```bash
mvn test
```

The suite has 21 tests and runs on an in-memory H2 database with the mock AI. Demo request seeding is switched off for tests.

- **`DecisionServiceTest`** (5): low risk with automation, mandatory rule override, high risk chain, medium risk, rule-based rejection.
- **`RuleEngineServiceTest`** (5): operators (`LTE`, `GT`, `BETWEEN`, `EQ`), mandatory flag, rule ordering.
- **`MultiTenantIsolationTest`** (2): one company cannot read another company's requests or audit logs.
- **`ApprovalGuardsTest`** (5): requester cannot approve own request, wrong role blocked, rejection cancels downstream tasks, info-request round trip, employees blocked from the approval inbox.
- **`EndToEndWorkflowTest`** (4): ₹8,000 auto-approved; ₹35,000 manager approval; ₹200,000 three-step chain; ₹65,000 mandatory Finance review despite a LOW AI rating.

---

## 12. Known Limitations & Roadmap

- **Documents**: only the document name and an "attached" flag are stored. Real file upload and storage is not implemented yet.
- **Schema management**: tables are created by Hibernate (`ddl-auto: update`). Flyway migrations, foreign keys and indexes on `tenant_id` and `request_id` are planned.
- **AI call**: the Gemini request runs synchronously inside the submit transaction (10 second timeout). Moving it out of the transaction is planned.
- **Request types**: the engine is generic, but the UI and seeded data focus on expense-style requests.

---

## 13. Example Workflow Walkthrough (cURL)

### Step 1: Log In as Employee
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"employee@acme.com","password":"Password@123","organizationName":"Acme Corp"}'
```

### Step 2: Submit a High-Value Expense Request (₹75,000)
```bash
curl -X POST http://localhost:8080/api/requests \
  -H "Authorization: Bearer <EMPLOYEE_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Data Center High-Capacity Storage Arrays",
    "amount": 75000.00,
    "category": "EXPENSE",
    "description": "Critical SAN storage expansion",
    "documentAttached": true,
    "documentName": "vendor-quote.pdf"
  }'
```
*Result: Status will be `PENDING_REVIEW`. Since amount > ₹50,000, two approval tasks are created in sequence: Step 1 (Manager) → Step 2 (Finance).*

### Step 3: Approve Step 1 as Manager
```bash
# Log in as manager@acme.com to obtain MANAGER_TOKEN
curl -X POST http://localhost:8080/api/tasks/1/decision \
  -H "Authorization: Bearer <MANAGER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"decision":"APPROVE","comment":"Approved for storage upgrade."}'
```
*Result: Step 1 is `APPROVED`. Step 2 is now actionable for the Finance role.*

### Step 4: Approve Step 2 as Finance
```bash
# Log in as finance@acme.com to obtain FINANCE_TOKEN
curl -X POST http://localhost:8080/api/tasks/2/decision \
  -H "Authorization: Bearer <FINANCE_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"decision":"APPROVE","comment":"Capital expenditure verified and approved."}'
```
*Result: The entire approval chain is complete. The request is now `APPROVED`.*