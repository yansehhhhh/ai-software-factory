## 1. Inspect current real API wiring

- [x] 1.1 Inspect `generated/HX-Meeting/frontend` API client, mock/demo switch, router/bootstrap logic, and Vite proxy to confirm default requests use `/api/hx-meeting/v1` instead of mock data.
- [x] 1.2 Inspect `generated/HX-Meeting/backend` controllers and services for all endpoints required by verification: `/auth/me`, `/rooms`, `/bookings`, `/bookings/my`, `/notifications`, `/violations/my-summary`, `/transfers/pending-mine`, `/admin/dashboard`, and admin approvals.
- [x] 1.3 Identify and document any frontend request, test helper, route interception, or fixture that still bypasses the generated backend.

## 2. Update generated frontend/backend if needed

- [x] 2.1 Update frontend API usage so acceptance mode defaults to real HTTP requests and only uses demo data when `VITE_USE_DEMO=true` is explicitly set.
- [x] 2.2 Update frontend role/token handling so `?token=user` and `?token=admin` reliably exercise backend authorization with real bearer tokens.
- [x] 2.3 Update frontend pages or stores so normal-user startup and booking page behavior do not trigger unrelated admin or room-list requests before the user action being tested.
- [x] 2.4 Update generated backend implementation only where an endpoint required by verification is missing or returns an implementation error.

## 3. Strengthen completion-before-verification E2E

- [x] 3.1 Revise `docs/移动端应用：会议室预约系统/测试/e2e/dev-integration.spec.js` to assert real HTTP responses for identity, room search, booking creation, and conflict/error behavior.
- [x] 3.2 Add/maintain assertions for supporting user endpoints: my bookings, notifications, violation summary, and pending transfer APIs.
- [x] 3.3 Add/maintain assertions for role-sensitive admin endpoints: normal-user rejection and admin-token dashboard/approval success.
- [x] 3.4 Ensure the E2E suite does not use Playwright route fulfillment, local fixture response bodies, or mock-only environment variables for business API assertions.

## 4. Update verification artifacts and reports

- [x] 4.1 Update `docs/移动端应用：会议室预约系统/测试/测试报告/dev-integration-report.md` with real backend URL/proxy target, interface coverage table, mock-disabled evidence, commands, and results.
- [x] 4.2 Update `docs/移动端应用：会议室预约系统/测试/测试报告/test-report.md` with acceptance conclusion, covered API list, pass/fail results, and remaining risks.
- [x] 4.3 Update `docs/移动端应用：会议室预约系统/测试/测试用例/test-case-summary.md` and `功能测试用例.md` if the expanded real-backend coverage changes the documented cases.

## 5. Run verification

- [x] 5.1 Run frontend build for `generated/HX-Meeting/frontend` and record the result.
- [x] 5.2 Start or connect to `generated/HX-Meeting/backend`, run the Playwright E2E suite with `API_BASE_URL` and `PLAYWRIGHT_BASE_URL` pointing to the generated backend/frontend, and record the result.
- [x] 5.3 If verification fails because a request is mocked, misrouted, or unsupported by the backend, fix the allowed-scope artifact and rerun the relevant command before marking complete.
