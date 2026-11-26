# -*- coding: utf-8 -*-
"""
Compress all photos in the cloned photos repo that are larger than 3 MB
down to under 3 MB (target ~2.5 MB for safety).

Strategy per image:
  1. Open with Pillow, convert to RGB (JPEG has no alpha).
  2. Try lowering JPEG quality (85 -> 80 -> 75 -> ... -> 40) at original size.
  3. If still too big, progressively downscale (0.85x, 0.7x, 0.55x, 0.4x)
     while keeping quality in a reasonable band.
  4. Write back in place only if the new file is smaller AND under the limit.

Only files that are currently > LIMIT are touched. Everything else is left
byte-for-byte identical.
"""
import os
import sys
from PIL import Image

REPO = sys.argv[1] if len(sys.argv) > 1 else os.environ.get("PHOTOS_REPO")
if not REPO:
    print("Usage: compress_photos.py <repo_dir> [limit_mb]")
    sys.exit(1)

# Hard limit (MB). Default 1.5 MB — Telegram reliably downloads photos up to
# ~1.7 MB but fails on ~2.3-2.5 MB (WEBPAGE_CURL_FAILED), so keep a margin.
LIMIT_MB = float(sys.argv[2]) if len(sys.argv) > 2 else 1.5
LIMIT = int(LIMIT_MB * 1024 * 1024)
TARGET = int(LIMIT * 0.85)          # aim a bit under the limit
EXTS = {".jpg", ".jpeg", ".png", ".webp"}

def file_size(path):
    return os.path.getsize(path)

def try_save_jpeg(img, path, quality, max_dim=None):
    """Save img as JPEG to a temp path; return (tmp_path, size) or None."""
    tmp = path + ".tmp.jpg"
    im = img
    if max_dim is not None:
        w, h = im.size
        scale = min(1.0, max_dim / max(w, h))
        if scale < 1.0:
            im = im.resize((max(1, int(w * scale)), max(1, int(h * scale))),
                           Image.LANCZOS)
    im.save(tmp, "JPEG", quality=quality, optimize=True, progressive=True)
    return tmp, os.path.getsize(tmp)

def compress_one(path):
    orig = file_size(path)
    try:
        with Image.open(path) as im:
            im.load()
            if im.mode not in ("RGB", "L"):
                im = im.convert("RGB")
            base = im
    except Exception as e:
        return ("skip", path, orig, orig, f"open error: {e}")

    # Fast path: downscale to a max dimension (enough for Telegram) and pick a
    # quality. Working on a ~1600px image is far cheaper than the original
    # (which can be 6000px+), so this is much faster than quality-only loops.
    for max_dim in (1600, 1280, 1024, 800):
        for q in (85, 80, 75, 70, 65, 60):
            tmp, size = try_save_jpeg(base, path, q, max_dim=max_dim)
            if size <= TARGET:
                os.replace(tmp, path)
                return ("ok", path, orig, size, f"dim={max_dim} q={q}")
            os.remove(tmp)

    # Last resort: smallest reasonable output.
    tmp, size = try_save_jpeg(base, path, 50, max_dim=640)
    if size <= LIMIT:
        os.replace(tmp, path)
        return ("ok", path, orig, size, "fallback")
    os.remove(tmp)
    return ("fail", path, orig, size, "could not get under limit")

def main():
    files = []
    for root, _dirs, names in os.walk(REPO):
        if os.path.basename(root) == ".git":
            continue
        for n in names:
            ext = os.path.splitext(n)[1].lower()
            if ext in EXTS:
                p = os.path.join(root, n)
                if file_size(p) > LIMIT:
                    files.append(p)

    files.sort()
    print(f"Files > {LIMIT_MB} MB: {len(files)}")
    ok = fail = skip = 0
    saved_total = 0
    for p in files:
        status, _path, before, after, info = compress_one(p)
        rel = os.path.relpath(p, REPO)
        if status == "ok":
            ok += 1
            saved_total += before - after
            print(f"  OK   {rel}  {before/1048576:.1f} -> {after/1048576:.2f} MB  ({info})")
        elif status == "fail":
            fail += 1
            print(f"  FAIL {rel}  {before/1048576:.1f} MB  ({info})")
        else:
            skip += 1
            print(f"  SKIP {rel}  ({info})")

    print("-" * 60)
    print(f"Compressed: {ok}   Failed: {fail}   Skipped: {skip}")
    print(f"Total saved: {saved_total/1048576:.1f} MB")

if __name__ == "__main__":
    main()
