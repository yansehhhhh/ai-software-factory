## ADDED Requirements

### Requirement: Restrict UI revision scope
The revision MUST only modify UI design stage artifacts for the meeting room reservation project, limited to the `UI原型/` directory and the UI design specification file.

#### Scenario: Change stays within allowed paths
- **WHEN** the UI design revision is implemented
- **THEN** all changed files MUST be under `docs/移动端应用：会议室预约系统/UI原型/` or exactly `docs/移动端应用：会议室预约系统/产品设计/03-UI设计规范/UI-Design-Spec.md`

#### Scenario: Non-UI files remain unchanged
- **WHEN** the UI design revision is implemented
- **THEN** files under `apps/`, `packages/`, `tests/`, product requirement documents, architecture design, database design, and other stage artifact directories MUST NOT be changed

### Requirement: Preserve existing business rules
The revised UI artifacts MUST preserve the existing meeting room reservation business rules already documented in the current UI design stage.

#### Scenario: Reservation constraints remain visible
- **WHEN** users review the revised prototype or interaction documents
- **THEN** the 30-day reservation window, 15-minute time granularity, end-time validation, capacity risk warning, and permission restriction states MUST remain represented

#### Scenario: Time-sensitive flows remain visible
- **WHEN** users review the revised prototype or interaction documents
- **THEN** the long-duration booking approval, VIP service confirmation, QR-code sign-in, automatic release after no-show, booking cancellation, booking transfer, violation handling, and room QR-code maintenance flows MUST remain represented

### Requirement: Keep UI artifacts internally consistent
The revised UI artifacts MUST use consistent page names, component names, state labels, action labels, business rule wording, and artifact references across the UI design specification, prototype, component library, interaction notes, and responsive adaptation documents.

#### Scenario: State labels are consistent
- **WHEN** a booking, approval, service, sign-in, transfer, violation, or room QR-code state appears in multiple UI artifacts
- **THEN** the state label and meaning MUST be consistent across those artifacts

#### Scenario: Artifact references are accurate
- **WHEN** the UI design specification lists generated UI prototype files
- **THEN** each listed artifact path MUST correspond to an existing file in the UI prototype delivery package under `docs/移动端应用：会议室预约系统/UI原型/`

### Requirement: Maintain mobile-first usability
The revised UI artifacts MUST maintain mobile-first usability for the embedded H5 meeting room reservation experience.

#### Scenario: Touch and safe-area guidance is retained
- **WHEN** mobile pages or responsive guidance are revised
- **THEN** tappable controls MUST continue to target at least 44px touch areas and bottom fixed actions MUST account for safe-area padding

#### Scenario: Status and errors are understandable without color alone
- **WHEN** status, warning, error, empty, or result states are shown in revised artifacts
- **THEN** they MUST combine text with visual styling so the meaning is not conveyed by color alone

### Requirement: Keep revision lightweight without explicit feedback
When user feedback is empty, the revision MUST remain a lightweight quality pass and MUST NOT introduce new product scope.

#### Scenario: No new capabilities are introduced from empty feedback
- **WHEN** the UI design revision is implemented without additional user feedback
- **THEN** the revised artifacts MUST NOT add new roles, new business flows, new backend-dependent behavior, or new data requirements beyond the existing meeting room reservation UI stage scope
