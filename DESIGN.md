# SIGECLIN Design System

## Core Visual Tokens

### Colors
- **SIGECLIN Blue (Primary)**: `#1066e4` (used for active states, menu indicators, primary buttons).
- **Deep Navy**: `#0b4cb4` (used for dark mode card backdrops and logos).
- **Slate Text (Main)**: `#0f172a` (slate-900, primary high-contrast copy).
- **Slate Text (Muted)**: `#475569` (slate-600, secondary/supporting copy).
- **Clinical Teal (Success)**: `#0d9488` (used for successful checkout, finished appointments).
- **Amber Warning (Caution)**: `#d97706` (used for pending payments or scheduled appointments).
- **Rose Red (Danger)**: `#e11d48` (used for vital sign warnings, patient referrals, or errors).

### Typography
- **Primary Font Family**: `'Plus Jakarta Sans', sans-serif` (imported from Google Fonts).
- **Body Line Height**: `1.5` for density and quick data scanning.
- **Font Scale**: 
  - Title/Headers: `1.25rem` to `1.5rem` (`font-weight: 800`).
  - Subheaders: `1rem` to `1.125rem` (`font-weight: 700`).
  - Body Text: `0.9rem` (`font-weight: 600` for high legibility).
  - Micro Labels / Metadata: `0.75rem` (`font-weight: 800` uppercase).

### Spacing & Layout
- **Horizontal & Vertical Paddings**: Standardized on an 8px modular scale:
  - Tight: `8px` (`--spacing-xs`)
  - Normal: `16px` (`--spacing-sm`)
  - Card/Wrapper: `24px` (`--spacing-md`)
  - Section spacing: `32px` to `48px`
- **Max Containers**: Content blocks fit within a fluid dual-column layout with a fixed sidebar width of `260px` (`80px` when collapsed).

### Interactive Motion
- **Easing**: Exponential ease-out (`cubic-bezier(0.16, 1, 0.3, 1)`), no bounces.
- **Duration**: Standard `250ms` for color changes, hover translates, and active class triggers.

## Rules & Standards
- **No Side-Stripe borders**: Cards, alert panels, and lists must use clean full 1px borders instead of colored 4px left-borders.
- **No Decorative Glassmorphism**: Eliminate blurs (`backdrop-filter`) on standard interface containers; use solid tinted backgrounds for maximum contrast.
- **No Pure Black/White**: Use Slate-900 for dark text and Slate-50 / Tinted White for card backgrounds to reduce optical fatigue under bright clinic monitors.
