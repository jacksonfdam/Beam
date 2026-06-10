# Beam landing page

A self-contained, **static** marketing page for Beam — single `index.html` (inline
CSS + JS), no build step, no dependencies. Multilingual: **EN, PT-BR, ES, SV**, with
a language switcher (top right) that persists the choice and seeds from the
browser language.

## Run locally

Just open `index.html` in a browser, or serve the folder:

```bash
cd landing
python3 -m http.server 8080   # http://localhost:8080
```

## Deploy to Vercel

This folder is a static site — Vercel needs no framework detection.

- **Dashboard:** New Project → import the repo → set **Root Directory** to `landing`
  → Framework Preset **Other** → Deploy. No build command, output is the folder.
- **CLI:**
  ```bash
  cd landing
  vercel        # preview
  vercel --prod # production
  ```

## Add your demo videos

Drop MP4 files into `landing/videos/` using these names (already referenced in
`index.html` → `DEMO_VIDEOS`):

| File                        | Card |
|-----------------------------|------|
| `videos/open-project.mp4`   | Open & project a PDF |
| `videos/phone-remote.mp4`   | Drive it from your phone |
| `videos/draw-spotlight.mp4` | Draw & spotlight live |
| `videos/notes-timer.mp4`    | Speaker notes & timer |

Until a file exists, that card shows a labelled placeholder — so the page looks
complete before the recordings are ready. To add, rename, or reorder clips, edit
the `DEMO_VIDEOS` array and the `demoItems` copy near the bottom of `index.html`.

## Edit copy / add a language

All visible text lives in the `I18N` object at the bottom of `index.html`. To add
a language: add a locale key with the same shape, then add it to `LOCALES` and
`LABELS`.
