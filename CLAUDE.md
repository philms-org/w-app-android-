# CLAUDE.md

Guidance for any Claude Code session (yours or another agent's) working in this repo.

This is one of five repos in the W app family — `w-app-web`, `w-app-ios`,
`w-app-android-`, `w.app`, `wapp-landing-page` — sharing a Supabase backend and
frequently worked by several concurrent Claude Code sessions at once. This
policy is mirrored across all five; `w-app-web`'s copy has the fullest detail
on the incident referenced below.

## Founder approval required

Before acting autonomously, stop and ask the founder first for any of the
following — "small" or "obviously correct" does not exempt a change in these
categories:

1. **Auth / RLS / security-sensitive changes.** Anything touching the shared
   Supabase schema/RLS policies, admin flags, CAPTCHA config, or auth/session
   handling. The schema has a documented history of RLS misconfigurations
   reaching prod (see `w-app-web/supabase/migrations/0006_profiles_rls_lockdown.sql`
   — a policy that let any authenticated user self-promote to admin) — don't
   autonomously "fix" anything in this area even if it looks small.
2. **Anything already flagged by another session as founder-gated.** If a peer
   session's notes/summary say "pending founder go-ahead" or similar, a new
   session should not silently proceed past that just because it wasn't the
   one that wrote the note. Check `list_sessions` / recent session summaries
   across the W app repos before assuming you're the only one working on an
   area.
3. **Production data or schema changes** — anything beyond editing a
   migration file in the repo (backfills, running something directly against
   the live DB, etc.).
4. **Merge conflicts where both sides changed the same logic**, or two agent
   sessions independently touching the same feature/branch. Check for a live
   sibling session on the same branch before resolving a conflict
   unilaterally.
5. **Anything customer-facing at scale** — pricing, legal/privacy copy, Play
   Store listing copy, or other content/policy decisions (not ordinary
   bug-fix copy changes).
6. **Destructive or hard-to-reverse git ops** — force-push, `reset --hard`,
   rewriting another session's branch history, etc. (standard practice,
   called out here for emphasis given how many branches are in flight across
   these repos at once.)

Everything else — bug fixes, lint/CI fixes, small reviewer nits, non-destructive
local changes — can proceed autonomously as usual.

## Working alongside other agent sessions

Multiple Claude Code sessions are frequently active across the W app repos at
the same time, often on overlapping branches or the same feature from
different angles. Before starting substantial work:

- Check for other running/recent sessions touching the same area, in this
  repo and the sibling W app repos.
- Don't assume a PR you didn't open is unwatched — check whether another
  session or the PR Steward is already subscribed before taking it over.
