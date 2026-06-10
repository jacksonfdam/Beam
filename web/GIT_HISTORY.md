# Importing the commit history

The web layer was built as a series of atomic, build-green micro-commits. They
were authored in a sandbox whose mount cannot delete git lock files, so the
history was recorded outside the mount and exported as a git bundle:

```
../beam-web-history.bundle   (at the repo root, alongside files/ and web/)
```

The bundle contains the full 14-commit history on `main`. To bring it into the
repository on your machine (where git can write normally):

**If this folder has no real history yet (recommended):**

```bash
cd /path/to/Beam
rm -f .git/index.lock .git/HEAD.lock      # clear any stale sandbox locks
git fetch beam-web-history.bundle main:main-imported
git checkout main-imported                # inspect, then fast-forward main
# git branch -f main main-imported && git checkout main
```

**Or clone a fresh copy from the bundle to inspect it:**

```bash
git clone beam-web-history.bundle beam-clone
cd beam-clone && git log --oneline
```

Once imported you can delete `beam-web-history.bundle`.

## Commit list

```
chore: add build prompts and shared protocol reference
chore(web): scaffold Next.js + TypeScript + Tailwind project
feat(web): build landing page — hero, features, how-to-connect, downloads, footer
feat(web): mirror BeamProtocol.kt in lib/protocol.ts
feat(web): add TTL signaling store (Redis REST + in-memory fallback)
feat(web): add rate limiter and signaling HTTP/validation helpers
feat(web): add WebRTC signaling endpoints (offer, answer, ice)
feat(web): add WebRTC peer (BeamRemote) and signaling client
feat(web): add useBeamRemote hook tracking connection and presentation state
feat(web): build /remote — pairing, deck picker, nav, notes, timer, drawing
test(web): protocol round-trip, endpoint, store, validation, rate-limit
docs(web): document pairing, privacy, config, and milestone status
chore(web): use eslint-config-next flat config; drop @eslint/eslintrc
fix(web): keep timer display pure via a shared useSyncExternalStore clock
```
