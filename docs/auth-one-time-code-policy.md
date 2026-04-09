# One-Time Code Policy (Single Instance)

## Scope
- `web login ticket`
- `mobile auth code`

## Current Operation Policy
- The current implementation uses in-memory storage.
- A restart can invalidate issued one-time codes.
- The client must treat one-time code exchange as retry-safe for a short replay window.

## Reissue Policy
- If exchange fails due to expiration or restart loss, the client must request a new login flow.
- No server-side automatic reissue is performed in this version.
- Reissue is user-driven by retrying the social login entry point.

## UX Guidance
- Web: show "login expired, please sign in again" and redirect to social login.
- App: show "session setup expired, please retry login" and reopen browser login.

## Future Extension Notes
- Keep `ExpiringCodeStore` abstraction intact for later Redis-backed implementation.
- Add request-level rate limiting when one-time code abuse risk increases.
