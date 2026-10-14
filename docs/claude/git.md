# Git rules

| Rule | Reason | Example |
|---|---|---|
| Conventional commits, scope inline after colon, no parentheses | Owner's style | `feat: history - search by URL` |
| No trailers or tool attribution in commits or PR descriptions | Owner's rule | no `Co-Authored-By` |
| Commit locally; never `git push` unless asked | Owner reviews and pushes | - |
| Work on a feature branch, not `main` | Clean history | `feature/sms-api-trigger` |
| Changes to `docs/claude/*` get their own commit | Rules stay traceable | `docs: update docs - detail is <what changed>` |
| AI tool files (`CLAUDE.md`, `AGENTS.md`, `.claude/`, `.gitnexus/`, ...) are never committed: create them in the ignored `tooling/<path>`, symlink the original path, add its name to `.git/info/exclude` | Tool config stays on the machine | `CLAUDE.md -> tooling/CLAUDE.md` |
| PR description: one-line summary + short bullets | Owner's style | - |
