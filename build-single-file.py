from pathlib import Path
import base64
import re
import sys

ROOT = Path(__file__).parent
FRONTEND = ROOT / 'frontend'
source = (FRONTEND / 'index.html').read_text(encoding='utf-8')
preview_theme = sys.argv[1] if len(sys.argv) > 1 else ''
if preview_theme:
    source = source.replace('<html lang="zh-CN">', f'<html lang="zh-CN" data-preview-theme="{preview_theme}">', 1)

# Embed local assets so the portable HTML opens without a sibling assets folder.
app_icon = base64.b64encode((FRONTEND / 'icon.svg').read_bytes()).decode('ascii')
source = source.replace('href="icon.svg"', f'href="data:image/svg+xml;base64,{app_icon}"')
source = source.replace('src="icon.svg"', f'src="data:image/svg+xml;base64,{app_icon}"')
assets = {
    'icons/home.svg': 'image/svg+xml',
    'icons/anime.svg': 'image/svg+xml',
    'icons/book.svg': 'image/svg+xml',
    'icons/settings.svg': 'image/svg+xml',
    'icons/search.svg': 'image/svg+xml',
    'icons/movie.png': 'image/png',
    'icons/stats.png': 'image/png',
    'icons/tv.png': 'image/png',
    'fonts/sarasa-gothic-sc-regular.woff2': 'font/woff2',
    'fonts/sarasa-gothic-sc-bold.woff2': 'font/woff2',
    'vendor/jszip.min.js': 'text/javascript',
}
inline_assets = {}
for relative, mime in assets.items():
    data = base64.b64encode((FRONTEND / 'assets' / relative).read_bytes()).decode('ascii')
    uri = f'data:{mime};base64,{data}'
    inline_assets[relative] = uri
    source = source.replace(f'assets/{relative}', uri)

# The navigation chooses an icon at runtime, so replace that dynamic path with the embedded map.
source = source.replace('assets/icons/${i[2]}', '${INLINE_ICONS[i[2]]}')
inline_icons = {name.split('/')[-1]: uri for name, uri in inline_assets.items() if name.startswith('icons/')}
icon_object = ','.join(f'{name!r}:{uri!r}' for name, uri in inline_icons.items())
source = source.replace('<script>', f'<script>\nconst INLINE_ICONS={{ {icon_object} }};\n', 1)

# A standalone file cannot register a relative service worker from file://.
source = re.sub(r"\n\s*if\('serviceWorker' in navigator && location\.protocol\.startsWith\('http'\)\)\{\s*navigator\.serviceWorker\.register\('sw\.js'\)\.catch\(e=>console\.error\('FolioFrame service worker registration failed',e\)\);\s*\}", '', source)
# A portable file should not advertise an external manifest that is not beside it.
source = source.replace('<link rel="manifest" href="manifest.webmanifest">\n', '')

out = ROOT / ({'dark': 'FolioFrame-dark-forest-preview.html', 'dark-contrast': 'FolioFrame-dark-ink-preview.html'}.get(preview_theme, 'FolioFrame-standalone.html'))
out.write_text(source, encoding='utf-8')
print(out)
