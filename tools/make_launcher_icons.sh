#!/usr/bin/env bash
#
# Regenerates the launcher icons from tools/icon-source/favicon.ico.
#
# The source is a 128x128 .ico whose artwork is a book. The book is trimmed out of the
# transparent padding once, then composited onto an obsidian (#0A0A0B, the app's
# ObsidianBlack) field at every density.
#
# Outputs:
#   mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher.webp          legacy square
#   mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher_round.webp    legacy round
#   mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher_foreground.png adaptive foreground
#
# The themed-icon layer is hand-written instead: see drawable/ic_launcher_monochrome.xml.
#
# Requires ImageMagick 7 (webp delegate).
set -euo pipefail

RES="$(cd "$(dirname "$0")/.." && pwd)/app/src/main/res"
SRC="$(cd "$(dirname "$0")" && pwd)/icon-source/favicon.ico"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

BG='#0A0A0B'

# Legacy launcher icon sizes, and adaptive-icon foreground canvases, per density bucket.
declare -A LEGACY=( [mdpi]=48 [hdpi]=72 [xhdpi]=96 [xxhdpi]=144 [xxxhdpi]=192 )
declare -A ADAPT=( [mdpi]=108 [hdpi]=162 [xhdpi]=216 [xxhdpi]=324 [xxxhdpi]=432 )

# Largest frame in the .ico, trimmed to the artwork's bounding box.
magick "$SRC[4]" -trim +repage "$TMP/book.png"

upscale() { # <src> <w> <h> <out>  - Lanczos plus a light unsharp, since the source is only 128px.
  magick "$1" -filter Lanczos -resize "${2}x${3}!" -unsharp 0x0.75+0.6+0.02 "$4"
}

# ---------- legacy: square ----------
for d in "${!LEGACY[@]}"; do
  s="${LEGACY[$d]}"
  bh=$(( s * 72 / 100 ))                       # book fills 72% of the frame
  bw=$(magick "$TMP/book.png" -format '%[fx:w/h]' info:)
  bw=$(awk -v h="$bh" -v r="$bw" 'BEGIN{printf "%d", h*r}')
  upscale "$TMP/book.png" "$bw" "$bh" "$TMP/b.png"
  magick -size "${s}x${s}" "xc:$BG" "$TMP/b.png" -gravity center -composite \
    -define webp:method=6 "$RES/mipmap-$d/ic_launcher.webp"
done

# ---------- legacy: round ----------
for d in "${!LEGACY[@]}"; do
  s="${LEGACY[$d]}"
  bh=$(( s * 66 / 100 ))
  bw=$(magick "$TMP/book.png" -format '%[fx:w/h]' info:)
  bw=$(awk -v h="$bh" -v r="$bw" 'BEGIN{printf "%d", h*r}')
  upscale "$TMP/book.png" "$bw" "$bh" "$TMP/b.png"
  magick -size "${s}x${s}" "xc:$BG" "$TMP/b.png" -gravity center -composite \
    \( -size "${s}x${s}" xc:none -fill white -draw "circle $((s/2)),$((s/2)) $((s/2)),1" \) \
    -alpha off -compose copy_opacity -composite \
    -define webp:method=6 "$RES/mipmap-$d/ic_launcher_round.webp"
done

# ---------- adaptive: foreground ----------
for d in "${!ADAPT[@]}"; do
  c="${ADAPT[$d]}"
  bh=$(( c * 59 / 100 ))                       # stays inside the 66dp safe circle
  bw=$(magick "$TMP/book.png" -format '%[fx:w/h]' info:)
  bw=$(awk -v h="$bh" -v r="$bw" 'BEGIN{printf "%d", h*r}')
  upscale "$TMP/book.png" "$bw" "$bh" "$TMP/b.png"
  magick -size "${c}x${c}" xc:none "$TMP/b.png" -gravity center -compose over -composite \
    -strip "$RES/mipmap-$d/ic_launcher_foreground.png"
done

echo "Launcher icons regenerated."