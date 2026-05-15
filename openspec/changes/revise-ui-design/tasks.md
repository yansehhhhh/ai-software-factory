## 1. Scope and Baseline Check

- [ ] 1.1 Inspect the current UI design specification at `docs/移动端应用：会议室预约系统/产品设计/03-UI设计规范/UI-Design-Spec.md` and all files under `docs/移动端应用：会议室预约系统/UI原型/` to establish the revision baseline.
- [ ] 1.2 Confirm the implementation will only change `docs/移动端应用：会议室预约系统/UI原型/` artifacts and `docs/移动端应用：会议室预约系统/产品设计/03-UI设计规范/UI-Design-Spec.md`.

## 2. Consistency Revision

- [ ] 2.1 Align page names, component names, action labels, state labels, and business rule wording across `组件库/组件清单.md`, `交互原型/交互说明.md`, and `产品设计/03-UI设计规范/UI-Design-Spec.md`.
- [ ] 2.2 Verify that booking, long-duration approval, VIP service confirmation, QR-code sign-in, automatic release after no-show, cancellation, transfer, violation handling, and room QR-code maintenance states are consistently represented.
- [ ] 2.3 Update artifact references in `产品设计/03-UI设计规范/UI-Design-Spec.md` so every listed UI prototype file path matches an existing file under `docs/移动端应用：会议室预约系统/UI原型/`.

## 3. Prototype and Responsive Artifact Revision

- [ ] 3.1 Review and lightly revise `设计稿/index.html`, `设计稿/desktop.svg`, and `设计稿/mobile.svg` for wording, state, and core-flow consistency without changing the approved business scope.
- [ ] 3.2 Review and lightly revise component library HTML/SVG artifacts so they match the Markdown component list and reuse the same component/state names.
- [ ] 3.3 Review and lightly revise responsive adaptation artifacts to preserve mobile-first H5 constraints, including safe-area handling, bottom fixed actions, readable status/error text, and 44px touch target guidance.

## 4. Verification

- [ ] 4.1 Check that no files outside `docs/移动端应用：会议室预约系统/UI原型/` and `docs/移动端应用：会议室预约系统/产品设计/03-UI设计规范/UI-Design-Spec.md` were modified by the apply operation.
- [ ] 4.2 Re-read the revised UI design specification, interaction notes, component list, and responsive guidance to confirm existing business rules remain intact.
- [ ] 4.3 Summarize the applied UI design revisions and explicitly note that no product requirement, architecture, database, code, or test artifacts were changed.
