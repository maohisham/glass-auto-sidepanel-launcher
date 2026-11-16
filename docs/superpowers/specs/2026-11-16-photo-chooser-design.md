# A photo chooser inside Glass Panel (car picture)

Short design, approved 2026-11-16 ("Yes, build it").

## Why

On the Eonon, "Use my own picture" still fails after 0.6.1. The unit's gallery (likely ES File Explorer) hands back a
link only that gallery can open; no permission on Glass Panel's side unlocks it. "All files access" doesn't apply:
Glass Panel only uses the photo permission.

## Design

- "Use my own picture" opens Glass Panel's own chooser: a grid of the unit's pictures, newest first, read from Android's
  photo library (MediaStore) with the photo permission (READ_MEDIA_IMAGES, or READ_EXTERNAL_STORAGE before Android 13).
  It doesn't depend on any gallery app.
- No permission yet: it asks first. Refused: the unit's own picker opens as before.
- At the top: "Other apps…" opens the unit's own picker (the 0.6.1 path, kept), and a close button.
- No pictures: "No pictures on this unit yet. Download one, or use Other apps…".
- A tap saves that picture for the car (same crop and save as today) and closes the chooser.
- Thumbnails load in the background; up to 300 newest pictures.

## Approaches

1. Own chooser from MediaStore (picked): works on any unit whose photo library is normal.
2. Try to turn the gallery's link into a file path — rejected: guessing per gallery app.
3. Keep only the unit's picker — rejected: that is what fails.
