# UI Foundation: JobStack.ma
**UX Reference**: docs/ux-jobstack.md
**Version**: 1.0 | **Date**: 2026-07-04 | **Author**: UI Designer

**HANDOFF: UX Designer → UI Designer**
Context: UX approved — sitemap, 3 core flows, wireframes + screen states for search/profile/dashboard/checkout/moderation screens.
Need: Visual system sized for an MVP (existing component library, not a custom design system — YAGNI).

## 1. Design Approach
- **Strategy**: Angular Material (official Angular component library, version-aligned with Angular 22)
- **Rationale**: MVP stage — an existing, accessible, actively-maintained library beats building a custom design system for ~10 screens. Angular Material also gives WCAG-conscious components out of the box, supporting NFR-3 (PRD).

## 2. Design Tokens (Material theme overrides only)
```css
/* Colors */
--color-primary:     #0F4C81;  /* deep industrial blue — trust, engineering */
--color-secondary:   #F2A63A;  /* amber accent — CTAs, highlights */
--color-background:  #FFFFFF;
--color-surface:     #F5F7FA;
--color-error:       #DC2626;
--color-success:     #16A34A;
--color-text:        #111827;
--color-text-muted:  #6B7280;

/* Typography */
--font-family:   Inter, system-ui, sans-serif;
--font-size-sm:  0.875rem;
--font-size-md:  1rem;
--font-size-lg:  1.25rem;
--font-size-xl:  1.5rem;

/* Spacing scale (4px base, Angular Material default grid) */
--spacing-xs: 4px;   --spacing-sm: 8px;
--spacing-md: 16px;  --spacing-lg: 24px;
--spacing-xl: 32px;
```
Dark mode: deferred (YAGNI — no user demand signal yet; Angular Material's built-in dark theme can be enabled later with no structural rework).

## 3. Component Inventory
| Component | Reuse Existing | Build New | Notes |
|---|---|---|---|
| Button | mat-button / mat-raised-button | No | Primary = raised + brand color, secondary = outlined |
| Text input / textarea | mat-form-field | No | Used for profile, job form |
| Select (sector/city/contract) | mat-select | No | |
| Card (job listing) | mat-card | No | Custom content layout only |
| Table (applicants, admin queue) | mat-table | No | With mat-paginator |
| File upload (CV) | — | Yes | Angular Material has no native file input; thin wrapper around `<input type="file">` + drag-drop, styled to match |
| Toast/snackbar | MatSnackBar | No | Success/error feedback |
| Status badge (job status, payment status) | — | Yes | Small custom chip component (mat-chip base + semantic color mapping) |
| Navigation bar | mat-toolbar | No | |

## 4. Responsive Breakpoints
| Breakpoint | Width | Layout Notes |
|---|---|---|
| Mobile | < 768px | Single column, job cards stack, filters collapse into a drawer |
| Tablet | 768–1024px | 2-column job grid, dashboard tables scroll horizontally |
| Desktop | > 1024px | 3-column job grid, full dashboard tables, max content width 1280px |

## 5. Accessibility Baseline
- Color contrast: AA minimum (4.5:1 normal text, 3:1 large text) — verified for primary/secondary against background/surface
- Focus indicators: Angular Material's default focus ring retained (not overridden away)
- Semantic HTML first; ARIA only where Material components don't already provide it (e.g., custom status badge gets `role="status"`)
- File upload: keyboard-operable (not drag-and-drop only), with visible error text on invalid type/size (per security baseline's PDF/5MB rule)
