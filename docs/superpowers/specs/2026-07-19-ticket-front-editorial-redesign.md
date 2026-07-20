# Ticket+ Editorial Redesign

## Design Read

Ticket+ is a Chinese event-discovery and ticket-purchase product for urban users. The redesign combines DICE-style purchase clarity, Resident Advisor-style editorial density, and familiar domestic ticket selection flows without retaining the old generic red-commerce visual language.

## Design System

- Design variance: 6; motion intensity: 3; visual density: 6.
- Use near-black text, cool white/gray surfaces, and vermilion `#d94732` as the only brand accent.
- Reserve green, amber, and blue for semantic states only.
- Use a 6px radius system. Cards are limited to repeated event items and framed tools.
- Use system Chinese sans fonts. Letter spacing remains zero.
- Page sections are unframed full-width bands. No gradients, decorative blobs, oversized pills, or nested cards.

## Information Architecture

- Keep all existing routes, labels, API contracts, and authentication behavior.
- Desktop navigation stays on one line. Mobile navigation exposes search and primary destinations without overlapping content.
- Home is an immediately usable event-discovery surface: featured show, time/category filters, on-sale editorial grid, seckill rail, and recommendations.
- Show detail uses poster, facts, and a clear ticket-selection column. Mobile uses a sticky purchase bar.
- Seckill detail presents status, countdown, stock, quantity, and processing feedback as one continuous workflow.
- Orders use compact scanning rows with explicit state and action hierarchy.
- Admin uses an operational shell with compact metrics, quick actions, and dense tables.

## Interaction And States

- Provide stable skeleton/loading, empty, error, disabled, sold-out, and processing states.
- Button active feedback uses a subtle scale or vertical offset.
- Motion is limited to hierarchy and feedback and respects reduced-motion preferences.
- Images use stable aspect ratios and object-fit to avoid layout shift.
- All controls remain keyboard reachable with visible focus states.

## Responsive Behavior

- Desktop content max width is 1280px with 24px gutters.
- Tablet layouts collapse multi-column grids without horizontal scrolling.
- Mobile uses 16px gutters, one-column content, fixed control dimensions, and sticky purchase actions where appropriate.

