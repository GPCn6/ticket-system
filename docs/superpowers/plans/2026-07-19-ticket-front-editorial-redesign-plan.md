# Ticket+ Editorial Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the legacy generic commerce frontend with a coherent editorial ticketing experience while preserving routes and APIs.

**Architecture:** Centralize visual tokens and shared layout behavior in global CSS, then rebuild route-level Vue views on top of the existing API and Pinia layers. Keep Element Plus for proven controls and admin tables, but override its visual defaults through the shared design system.

**Tech Stack:** Vue 3, Vue Router, Pinia, Element Plus, Vite, native CSS.

## Global Constraints

- Preserve all existing routes and backend API contracts.
- Use `#d94732` as the only brand accent and 6px as the shared radius.
- Do not add new runtime dependencies.
- Do not use gradients, decorative blobs, nested cards, emoji icons, or client-generated identity headers.
- Desktop and mobile layouts must remain stable without text or control overlap.

---

### Task 1: Global design system and application shell

**Files:** `src/assets/styles/main.css`, `src/App.vue`, `src/components/Header.vue`, `src/components/Footer.vue`, `index.html`

- [ ] Replace legacy and Vite styles with tokens, resets, typography, focus, buttons, states, and responsive containers.
- [ ] Rebuild the header as a single-line desktop navigation with search and an accessible mobile menu.
- [ ] Reduce the footer to useful product and support information.
- [ ] Verify with `npm run build`.

### Task 2: Discovery home

**Files:** `src/views/home/Home.vue`

- [ ] Replace the carousel with one real-image featured stage.
- [ ] Add compact time/category discovery controls and an editorial show grid.
- [ ] Rebuild the seckill area as a high-contrast information band without gradients.
- [ ] Preserve loading, API enrichment, countdown cleanup, and route actions.

### Task 3: Purchase views

**Files:** `src/views/show/ShowDetail.vue`, `src/views/seckill/SeckillDetail.vue`

- [ ] Build stable poster/facts/ticket layouts with explicit stock and submission states.
- [ ] Add responsive sticky purchase behavior without changing order or seckill API payloads.
- [ ] Preserve request-id polling and authentication redirects.

### Task 4: Account flow

**Files:** `src/views/order/Order.vue`, `src/views/order/OrderDetail.vue`, `src/views/user/Login.vue`, `src/views/user/Register.vue`, `src/views/user/UserCenter.vue`

- [ ] Rebuild orders for fast status scanning and safe repeated actions.
- [ ] Rebuild authentication as focused forms with clear inline validation.
- [ ] Align user center styling with the shared shell.

### Task 5: Operational admin

**Files:** `src/views/admin/AdminDashboard.vue`, shared admin view styles as needed.

- [ ] Replace colorful metric cards with compact operational metrics.
- [ ] Provide consistent quick actions and table framing without nested cards.
- [ ] Keep existing Element Plus behavior and routes.

### Task 6: Verification

- [ ] Run `npm run build` and resolve all template or CSS errors.
- [ ] Scan for legacy purple tokens, gradients, direct service URLs, and identity headers.
- [ ] Check the responsive CSS at desktop, tablet, and mobile breakpoints.

