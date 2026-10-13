# Image and font attributions

Every third-party asset bundled in this app, with the licence it was taken under and where it
came from. Nothing here was found by scraping a search result: each file was resolved from
Wikimedia Commons by name and its licence read off the file page.

---

## `res/drawable-nodpi/splash_chess_king.xml`

| | |
|---|---|
| File | `Chess white king.svg` |
| Author | CyrilKato |
| Source | https://commons.wikimedia.org/wiki/File:Chess_white_king.svg |
| Licence | **CC0 1.0 Universal (Public Domain Dedication)** |
| Changes | Filename only. Vector path data is unmodified; it is used as an Android vector drawable. |

The uploader released this to the public domain worldwide, so no attribution is legally required.
It is credited here anyway, because a dependency nobody recorded is a dependency nobody can audit.

## `res/drawable-xxhdpi/portrait_machiavelli.jpg`

| | |
|---|---|
| File | `Portrait of Niccolò Machiavelli by Santi di Tito.jpg` |
| Artist | Santi di Tito (1536–1603), c. 1550–1600 |
| Source | https://commons.wikimedia.org/wiki/File:Portrait_of_Niccol%C3%B2_Machiavelli_by_Santi_di_Tito.jpg |
| Licence | **Public domain** (PD-Art; artist died 1603, and the photograph is a faithful reproduction of a two-dimensional PD work) |
| Changes | Resized to 480px wide, JPEG quality 82, metadata stripped. No retouching, recolouring or cropping of the subject. |

## Fonts

None yet. The design calls for Playfair Display (headings) and Inter (body); both are SIL Open Font
License 1.1 and redistributable, and both must be added to this file, with their OFL text, at the
point the font files are committed. Do not bundle a font without recording it here.

---

## Not used, and why

The reference design showed portrait photography on its law cards. Several candidates were
considered and rejected:

- **Modern photographs of businesspeople** — rights vary per photographer, and a stock-photo
  licence rarely permits redistribution inside an APK.
- **AI-generated portraits** — licence status is unsettled, and several providers grant rights only
  for non-commercial use, which would not cover this app's release.
- **Portraits of living people used to stand in for a historical law** — a misattribution risk
  that outlives any single licence.

Portraiture of figures centuries dead is used instead: the subjects are long out of copyright and
the captions can be factually checked.