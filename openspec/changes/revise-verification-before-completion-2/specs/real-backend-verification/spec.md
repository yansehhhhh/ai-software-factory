## ADDED Requirements

### Requirement: Verification uses generated backend APIs
完成前验证 MUST use HTTP APIs implemented by `generated/HX-Meeting/backend` for all business interface assertions, and MUST NOT use frontend mock responses, Playwright route fulfillment, static fixture responses, or demo data as acceptance evidence.

#### Scenario: Frontend requests reach generated backend
- **WHEN** the E2E suite opens the generated frontend and performs identity, room search, booking, notification, transfer, violation, or admin flows
- **THEN** each asserted business request MUST be observed as a real HTTP request under `/api/hx-meeting/v1` and MUST receive a response from the generated backend service

#### Scenario: Mock mode is not enabled during acceptance
- **WHEN** completion-before-verification commands are executed
- **THEN** `VITE_USE_DEMO=true` MUST NOT be required for the tests to pass and the report MUST state that frontend mock/demo mode is disabled for acceptance

### Requirement: Core user booking flow is verified end to end
The verification suite MUST validate the normal user booking path through the generated frontend and generated backend, including user identity lookup, available room query, booking submission, and resulting page state.

#### Scenario: User creates a valid booking
- **WHEN** a normal user selects a future date, chooses a time range within two hours, queries available rooms, selects a room, and submits a booking
- **THEN** the suite MUST assert successful backend responses for `/auth/me`, GET `/rooms`, and POST `/bookings`, and MUST assert the frontend shows the successful booking result

#### Scenario: User sees backend conflict state
- **WHEN** a booking conflict is created through the real backend and the same slot is requested again
- **THEN** the suite MUST assert the backend returns `TIME_CONFLICT` or disables the conflicting room based on real availability data, and MUST NOT simulate the conflict in frontend code

### Requirement: Supporting user and admin APIs are verified
The verification suite MUST cover supporting APIs used by the mobile application home, notification, violation, transfer, and admin approval experiences with real backend responses and role-appropriate tokens.

#### Scenario: User supporting APIs return real responses
- **WHEN** the suite calls normal-user supporting endpoints such as `/bookings/my`, `/notifications`, `/violations/my-summary`, and `/transfers/pending-mine`
- **THEN** each endpoint MUST return a successful response from the generated backend with the expected API envelope or data shape

#### Scenario: Admin permissions are verified with real backend authorization
- **WHEN** a normal-user token calls `/admin/dashboard` and an admin token calls admin dashboard or approval endpoints
- **THEN** the normal-user request MUST be rejected by backend authorization and the admin-token request MUST return successful backend data

### Requirement: Verification reports include real-backend evidence
Completion-before-verification reports MUST document the real backend environment and provide enough evidence to distinguish real backend verification from frontend mock validation.

#### Scenario: Report records commands and interface coverage
- **WHEN** the verification artifacts are updated
- **THEN** `dev-integration-report.md` and `test-report.md` MUST include the backend URL or proxy target, executed commands, covered interface list, pass/fail results, and a statement that frontend mock responses were not used
