## ADDED Requirements

### Requirement: Revision scope remains within product design artifacts
The revision MUST modify only files under `docs/公司内部使用的会议室预约系统/产品设计/` and MUST NOT modify runtime code, application configuration, dependencies, or unrelated documentation.

#### Scenario: Product design files are revised
- **WHEN** the change is applied
- **THEN** updates are limited to PRD, version notes, UI design spec, flow diagrams, information architecture, page flow, appendix, and PRD docx under the product design directory

#### Scenario: Runtime directories are untouched
- **WHEN** the change is applied
- **THEN** files under `apps/`, `packages/`, `.claude/`, `tests/`, and other non-product-design directories remain unchanged

### Requirement: Transfer timeout rule is explicit
The PRD and related artifacts MUST state that a pending transfer automatically expires if the receiver has not confirmed before the meeting start time, and the booking owner remains unchanged after rejection or timeout.

#### Scenario: Receiver does not confirm before meeting start
- **WHEN** a transfer remains pending at the meeting start time
- **THEN** the transfer status becomes timed out and the booking remains assigned to the original owner

#### Scenario: Transfer succeeds before meeting start
- **WHEN** the receiver accepts the transfer before the meeting start time
- **THEN** booking ownership, check-in responsibility, cancellation/release authority, and violation responsibility transfer to the receiver

### Requirement: Recurring booking special resources use group approval
For recurring bookings containing special resources, the artifacts MUST define a group-level approval rule: the administrator approves or rejects the recurring request as a group, and rejection cancels the associated group booking instances.

#### Scenario: Administrator approves recurring special resources
- **WHEN** an administrator approves the group special-resource request for a recurring booking
- **THEN** all associated booking instances remain valid and their special-resource status becomes confirmed

#### Scenario: Administrator rejects recurring special resources
- **WHEN** an administrator rejects the group special-resource request for a recurring booking
- **THEN** the associated group booking instances are cancelled and their meeting-room time slots are released

### Requirement: Over-capacity booking is allowed with warning
The artifacts MUST state that attendee count exceeding meeting-room capacity does not block booking submission, but the UI and validation messages MUST prominently warn about over-capacity risk before submission.

#### Scenario: Attendee count exceeds room capacity
- **WHEN** a user submits a booking with attendee count greater than room capacity
- **THEN** the system allows submission after displaying a clear over-capacity warning

#### Scenario: Attendee count is within capacity
- **WHEN** a user submits a booking with attendee count less than or equal to room capacity
- **THEN** no capacity risk warning is required

### Requirement: Administrator manages fixed QR code presentation and output
The artifacts MUST specify that administrators can view, download, and print each meeting room's fixed QR code from administrator meeting-room management pages.

#### Scenario: Administrator opens QR code page
- **WHEN** an administrator views a meeting room's QR code page
- **THEN** the page displays the fixed QR code and provides download and print actions

#### Scenario: QR code scope remains fixed
- **WHEN** QR code behavior is described
- **THEN** the QR code remains fixed to the meeting room and dynamic QR code or QR reset capability remains out of scope

### Requirement: Special-resource notification wording is unambiguous
Notification wording MUST distinguish booking submission from special-resource confirmation: bookings containing special resources are described as submitted and pending resource confirmation until approval succeeds.

#### Scenario: Booking contains special resources and awaits approval
- **WHEN** a user submits a booking containing special resources
- **THEN** notifications and result pages state that the booking has been submitted and special resources are pending confirmation, not that special resources are already successful

#### Scenario: Special resources are approved
- **WHEN** an administrator approves the special-resource request
- **THEN** the notification states that the special resources have been confirmed and the booking remains valid

### Requirement: Active release and automatic release have distinct semantics
The artifacts MUST distinguish active release from automatic release. Active release is initiated by the booking owner before the meeting is no longer used. Automatic release is initiated by the system after the check-in window ends without check-in and records a violation.

#### Scenario: User actively releases a booking
- **WHEN** the booking owner actively releases a not-started eligible booking
- **THEN** the booking is marked as actively released, the time slot becomes bookable, and no violation is recorded

#### Scenario: System automatically releases a booking
- **WHEN** the check-in window ends and the booking was not cancelled and not checked in
- **THEN** the booking is marked as automatically released, the time slot becomes bookable, and one violation is recorded for the responsible user

### Requirement: Cancellation and release boundaries are consistent
The artifacts MUST define cancellation and release consistently: cancellation is pre-meeting withdrawal of a booking, recurring cancellation may affect one instance or current-and-future instances, active release is the owner's explicit release of an unused booking time slot, and automatic release is the system's no-show handling.

#### Scenario: User cancels before meeting start
- **WHEN** a booking owner cancels an eligible booking before the meeting starts
- **THEN** the booking becomes cancelled and its time slot is released without violation

#### Scenario: User releases before meeting start
- **WHEN** a booking owner actively releases an eligible booking before the meeting starts
- **THEN** the booking becomes actively released and its time slot is released without violation

#### Scenario: User attempts operation after sign-in or end
- **WHEN** a booking is already signed in or ended
- **THEN** cancellation and active release are not allowed unless explicitly defined by a later product version

### Requirement: Page flow structure is consistent
The page flow diagram and UI design spec MUST use consistent page naming and transitions for home, my page, message center, booking detail, transfer confirmation, administrator management, and QR code operations.

#### Scenario: User navigates from bottom tabs
- **WHEN** bottom navigation is represented
- **THEN** the flow consistently distinguishes booking home, my page, and message center without conflicting home-to-my-booking shortcuts

#### Scenario: Administrator manages QR code
- **WHEN** administrator QR code flow is represented
- **THEN** the QR code page returns to the meeting-room edit or management flow and exposes view, download, and print actions consistently

### Requirement: Booking time boundary is explicit
The artifacts MUST define booking time intervals as left-closed and right-open `[start_time, end_time)`, require end time to be later than start time, require 30-minute granularity, and allow 24:00 only as an end time.

#### Scenario: Adjacent bookings share a boundary
- **WHEN** one booking ends at 10:00 and another booking starts at 10:00 for the same room
- **THEN** they do not conflict solely because of the shared boundary

#### Scenario: End time is not after start time
- **WHEN** a user selects an end time that is equal to or earlier than the start time
- **THEN** the booking is invalid and the user is prompted to correct the time range

#### Scenario: 24:00 is selected
- **WHEN** a user selects 24:00
- **THEN** 24:00 is valid only as an end time and not as a start time

### Requirement: PRD and version notes align on filtering scope
The PRD and version notes MUST align that first-version meeting-room filtering supports date, start time, and end time only; meeting-room name, location, and capacity are display fields or basic record information, not advanced filters.

#### Scenario: Employee filters meeting rooms
- **WHEN** first-version employee meeting-room search scope is described
- **THEN** supported filters are date, start time, and end time

#### Scenario: Meeting room attributes are shown
- **WHEN** meeting-room cards or records are described
- **THEN** name, location, and capacity are presented as display fields rather than filtering criteria
